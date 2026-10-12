package ir.shecan.core.provider;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;

import org.pcap4j.packet.IpPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.core.util.Logger;

public class TcpProvider extends UdpProvider {

    private static final String TAG = "TcpProvider";
    private final CopyOnWriteArrayList<WaitingOnSocketPacket> dnsIn = new CopyOnWriteArrayList<>();

    public TcpProvider(ParcelFileDescriptor descriptor, ShecanVpnService service) {
        super(descriptor, service);
    }

    @Override
    public void process() {
        if (descriptor == null) {
            Logger.warning("TcpProvider: descriptor is null, aborting process()");
            return;
        }

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            Log.d(TAG, "Starting TCP DNS proxy.");
            FileDescriptor[] pipes = Os.pipe();
            mInterruptFd = pipes[0];
            mBlockFd = pipes[1];

            inputStream = new FileInputStream(descriptor.getFileDescriptor());
            outputStream = new FileOutputStream(descriptor.getFileDescriptor());

            byte[] packet = new byte[32767];

            while (running) {
                StructPollfd deviceFd = new StructPollfd();
                deviceFd.fd = inputStream.getFD();
                deviceFd.events = (short) OsConstants.POLLIN;

                StructPollfd blockFd = new StructPollfd();
                blockFd.fd = mBlockFd;
                blockFd.events = (short) (OsConstants.POLLHUP | OsConstants.POLLERR);

                if (!deviceWrites.isEmpty())
                    deviceFd.events |= (short) OsConstants.POLLOUT;

                StructPollfd[] polls = new StructPollfd[2 + dnsIn.size()];
                polls[0] = deviceFd;
                polls[1] = blockFd;

                int index = 2;
                for (WaitingOnSocketPacket wosp : dnsIn) {
                    try (ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(wosp.socket)) {
                        StructPollfd pollFd = new StructPollfd();
                        pollFd.fd = pfd.getFileDescriptor();
                        pollFd.events = (short) OsConstants.POLLIN;
                        polls[index++] = pollFd;
                    } catch (IOException e) {
                        Logger.logException(e);
                    }
                }

                Os.poll(polls, -1);

                if (blockFd.revents != 0) {
                    Log.i(TAG, "Received blockFd event, continuing...");
                    continue; // حلقه متوقف نشه
                }

                int i = -1;
                Iterator<WaitingOnSocketPacket> iter = dnsIn.iterator();
                while (iter.hasNext()) {
                    i++;
                    WaitingOnSocketPacket wosp = iter.next();
                    if (i + 2 < polls.length && (polls[i + 2].revents & OsConstants.POLLIN) != 0) {
                        try {
                            handleRawDnsResponse(wosp.packet, wosp.socket);
                        } catch (Exception e) {
                            Logger.logException(e);
                        } finally {
                            try { wosp.socket.close(); } catch (IOException ignored) {}
                            iter.remove();
                        }
                    }
                }

                if ((deviceFd.revents & OsConstants.POLLOUT) != 0) {
                    writeToDevice(outputStream);
                }
                if ((deviceFd.revents & OsConstants.POLLIN) != 0) {
                    readPacketFromDevice(inputStream, packet);
                }

                service.providerLoopCallback();
            }

        } catch (Exception e) {
            Logger.logException(e);
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                service.providerLoopCallback(); // حتماً callback اجرا شود
            } catch (IOException e) {
                Logger.logException(e);
            }
        }
    }

    @Override
    void forwardPacket(DatagramPacket outPacket, IpPacket parsedPacket)
            throws ShecanVpnService.VpnNetworkException {
        Socket dnsSocket = null;
        try {
            dnsSocket = SocketChannel.open().socket();
            service.protect(dnsSocket);

            SocketAddress address = new InetSocketAddress(outPacket.getAddress(), outPacket.getPort());
            dnsSocket.connect(address, 5000);
            dnsSocket.setSoTimeout(5000);

            try (DataOutputStream dos = new DataOutputStream(dnsSocket.getOutputStream())) {
                byte[] packet = outPacket.getData();
                dos.writeShort(packet.length);
                dos.write(packet);
                dos.flush();
            }

            if (parsedPacket != null) {
                dnsIn.add(new WaitingOnSocketPacket(dnsSocket, parsedPacket));
            } else {
                dnsSocket.close();
            }
        } catch (IOException e) {
            if (e.getCause() instanceof ErrnoException) {
                ErrnoException errno = (ErrnoException) e.getCause();
                if (errno.errno == OsConstants.ENETUNREACH || errno.errno == OsConstants.EPERM) {
                    throw new ShecanVpnService.VpnNetworkException("Cannot send message:", e);
                }
            }
            Logger.warning("TcpProvider: Could not send packet to upstream");
            Logger.logException(e);
        } finally {
            if (parsedPacket == null && dnsSocket != null) {
                try { dnsSocket.close(); } catch (IOException ignored) {}
            }
        }
    }

    private void handleRawDnsResponse(IpPacket parsedPacket, Socket dnsSocket) {
        try (Socket socket = dnsSocket;
             DataInputStream stream = new DataInputStream(socket.getInputStream())) {

            int length = stream.readUnsignedShort();
            if (length <= 0 || length > 4096) {
                Log.w(TAG, "Invalid DNS response length: " + length);
                return;
            }

            byte[] data = new byte[length];
            int readBytes = stream.read(data);
            if (readBytes != length) {
                Log.w(TAG, "Incomplete DNS response");
            }
            handleDnsResponse(parsedPacket, data);
        } catch (Exception e) {
            Logger.logException(e);
        }
    }

    private static class WaitingOnSocketPacket {
        final Socket socket;
        final IpPacket packet;
        private final long time;

        WaitingOnSocketPacket(Socket socket, IpPacket packet) {
            this.socket = socket;
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        long ageSeconds() {
            return (System.currentTimeMillis() - time) / 1000;
        }
    }
}
