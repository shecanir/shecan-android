package ir.shecan.provider;

import android.os.ParcelFileDescriptor;
import androidx.core.util.Pair;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;

import ir.shecan.Shecan;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.util.Logger;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import de.measite.minidns.DNSMessage;

/**
 * Shecan Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class UdpProvider extends Provider {
    private static final String TAG = "UdpProvider";

    private final WospList dnsIn = new WospList();
    FileDescriptor mBlockFd = null;
    FileDescriptor mInterruptFd = null;
    final Queue<byte[]> deviceWrites = new LinkedList<>();

    public UdpProvider(ParcelFileDescriptor descriptor, ShecanVpnService service) {
        super(descriptor, service);
    }

    public void stop() {
        try {
            if (mInterruptFd != null) {
                Os.close(mInterruptFd);
            }
            if (mBlockFd != null) {
                Os.close(mBlockFd);
            }
            if (this.descriptor != null) {
                this.descriptor.close();
                this.descriptor = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void queueDeviceWrite(IpPacket ipOutPacket) {
        dnsQueryTimes++;
        deviceWrites.add(ipOutPacket.getRawData());
    }

    public void process() {
        try {
            Log.d(TAG, "Starting advanced DNS proxy.");
            FileDescriptor[] pipes = Os.pipe();
            mInterruptFd = pipes[0];
            mBlockFd = pipes[1];
            FileInputStream inputStream = new FileInputStream(descriptor.getFileDescriptor());
            FileOutputStream outputStream = new FileOutputStream(descriptor.getFileDescriptor());

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
                {
                    int i = -1;
                    for (WaitingOnSocketPacket wosp : dnsIn) {
                        i++;
                        StructPollfd pollFd = polls[2 + i] = new StructPollfd();
                        pollFd.fd = ParcelFileDescriptor.fromDatagramSocket(wosp.socket).getFileDescriptor();
                        pollFd.events = (short) OsConstants.POLLIN;
                    }
                }

                Log.d(TAG, "doOne: Polling " + polls.length + " file descriptors");
                Os.poll(polls, -1);
                if (blockFd.revents != 0) {
                    Log.i(TAG, "Told to stop VPN");
                    running = false;
                    return;
                }

                // Need to do this before reading from the device, otherwise a new insertion there could
                // invalidate one of the sockets we want to read from either due to size or time out
                // constraints
                {
                    int i = -1;
                    Iterator<WaitingOnSocketPacket> iter = dnsIn.iterator();
                    while (iter.hasNext()) {
                        i++;
                        WaitingOnSocketPacket wosp = iter.next();
                        if ((polls[i + 2].revents & OsConstants.POLLIN) != 0) {
                            Log.d(TAG, "Read from UDP DNS socket" + wosp.socket);
                            iter.remove();
                            handleRawDnsResponse(wosp.packet, wosp.socket);
                            wosp.socket.close();
                        }
                    }
                }
                if ((deviceFd.revents & OsConstants.POLLOUT) != 0) {
                    Log.d(TAG, "Write to device");
                    writeToDevice(outputStream);
                }
                if ((deviceFd.revents & OsConstants.POLLIN) != 0) {
                    Log.d(TAG, "Read from device");
                    readPacketFromDevice(inputStream, packet);
                }
                service.providerLoopCallback();
            }
        } catch (Exception e) {
            Logger.logException(e);
        }
    }

    void writeToDevice(FileOutputStream outFd) throws ShecanVpnService.VpnNetworkException {
        try {
            outFd.write(deviceWrites.poll());
        } catch (IOException e) {
            throw new ShecanVpnService.VpnNetworkException("Outgoing VPN output stream closed");
        }
    }

    void readPacketFromDevice(FileInputStream inputStream, byte[] packet) throws ShecanVpnService.VpnNetworkException, SocketException {
        // Read the outgoing packet from the input stream.
        int length;

        try {
            length = inputStream.read(packet);
        } catch (IOException e) {
            throw new ShecanVpnService.VpnNetworkException("Cannot read from device", e);
        }


        if (length == 0) {
            Log.w(TAG, "Got empty packet!");
            return;
        }

        final byte[] readPacket = Arrays.copyOfRange(packet, 0, length);

        handleDnsRequest(readPacket);
    }

    void forwardPacket(DatagramPacket outPacket, IpPacket parsedPacket) throws ShecanVpnService.VpnNetworkException {
        DatagramSocket dnsSocket;
        try {
            // Packets to be sent to the real DNS server will need to be protected from the VPN
            dnsSocket = new DatagramSocket();

            service.protect(dnsSocket);

            dnsSocket.send(outPacket);

            if (parsedPacket != null) {
                dnsIn.add(new WaitingOnSocketPacket(dnsSocket, parsedPacket));
            } else {
                dnsSocket.close();
            }
        } catch (IOException e) {
            handleDnsResponse(parsedPacket, outPacket.getData());
            Logger.warning("DNSProvider: Could not send packet to upstream, forwarding packet directly");
        }
    }

    private void handleRawDnsResponse(IpPacket parsedPacket, DatagramSocket dnsSocket) {
        try {
            byte[] datagramData = new byte[1024];
            DatagramPacket replyPacket = new DatagramPacket(datagramData, datagramData.length);
            dnsSocket.receive(replyPacket);
            handleDnsResponse(parsedPacket, datagramData);
        } catch (Exception e) {
            Logger.logException(e);
        }
    }


    /**
     * Handles a responsePayload from an upstream DNS server
     *
     * @param requestPacket   The original request packet
     * @param responsePayload The payload of the response
     */
    void handleDnsResponse(IpPacket requestPacket, byte[] responsePayload) {
        if (Shecan.getPrefs().getBoolean("settings_debug_output", false)) {
            try {
                Logger.debug(new DNSMessage(responsePayload).toString());
            } catch (IOException e) {
                Logger.logException(e);
            }
        }
        UdpPacket udpOutPacket = (UdpPacket) requestPacket.getPayload();
        UdpPacket.Builder payLoadBuilder = new UdpPacket.Builder(udpOutPacket)
                .srcPort(udpOutPacket.getHeader().getDstPort())
                .dstPort(udpOutPacket.getHeader().getSrcPort())
                .srcAddr(requestPacket.getHeader().getDstAddr())
                .dstAddr(requestPacket.getHeader().getSrcAddr())
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .payloadBuilder(
                        new UnknownPacket.Builder()
                                .rawData(responsePayload)
                );


        IpPacket ipOutPacket;
        if (requestPacket instanceof IpV4Packet) {
            ipOutPacket = new IpV4Packet.Builder((IpV4Packet) requestPacket)
                    .srcAddr((Inet4Address) requestPacket.getHeader().getDstAddr())
                    .dstAddr((Inet4Address) requestPacket.getHeader().getSrcAddr())
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true)
                    .payloadBuilder(payLoadBuilder)
                    .build();

        } else {
            ipOutPacket = new IpV6Packet.Builder((IpV6Packet) requestPacket)
                    .srcAddr((Inet6Address) requestPacket.getHeader().getDstAddr())
                    .dstAddr((Inet6Address) requestPacket.getHeader().getSrcAddr())
                    .correctLengthAtBuild(true)
                    .payloadBuilder(payLoadBuilder)
                    .build();
        }

        queueDeviceWrite(ipOutPacket);
    }

    /**
     * Handles a DNS request, by either blocking it or forwarding it to the remote location.
     *
     * @param packetData The packet data to read
     * @throws ShecanVpnService.VpnNetworkException If some network error occurred
     */
    private void handleDnsRequest(byte[] packetData) throws ShecanVpnService.VpnNetworkException {

        IpPacket parsedPacket;
        try {
            parsedPacket = (IpPacket) IpSelector.newPacket(packetData, 0, packetData.length);
        } catch (Exception e) {
            Log.i(TAG, "handleDnsRequest: Discarding invalid IP packet", e);
            return;
        }

        if (!(parsedPacket.getPayload() instanceof UdpPacket)) {
            Log.i(TAG, "handleDnsRequest: Discarding unknown packet type " + parsedPacket.getPayload());
            return;
        }

        InetAddress destAddr = parsedPacket.getHeader().getDstAddr();
        int destPort;
        Pair<String, Integer> destination = service.dnsServers.get(destAddr.getHostAddress());
        if (destAddr == null)
            return;
        try {
            destAddr = InetAddress.getByName(destination.first);
            destPort = destination.second;
        } catch (Exception e) {
            Logger.logException(e);
            Logger.error("handleDnsRequest: DNS server alias query failed for " + destAddr.getHostAddress());
            return;
        }

        UdpPacket parsedUdp = (UdpPacket) parsedPacket.getPayload();

        if (parsedUdp.getPayload() == null) {
            Log.i(TAG, "handleDnsRequest: Sending UDP packet without payload: " + parsedUdp);

            // Let's be nice to Firefox. Firefox uses an empty UDP packet to
            // the gateway to reduce the RTT. For further details, please see
            // https://bugzilla.mozilla.org/show_bug.cgi?id=888268
            DatagramPacket outPacket = new DatagramPacket(new byte[0], 0, 0, destAddr,
                     destPort);
            forwardPacket(outPacket, null);
            return;
        }

        byte[] dnsRawData = (parsedUdp).getPayload().getRawData();
        DNSMessage dnsMsg;
        try {
            dnsMsg = new DNSMessage(dnsRawData);
            if (Shecan.getPrefs().getBoolean("settings_debug_output", false)) {
                Logger.debug(dnsMsg.toString());
            }
        } catch (IOException e) {
            Log.i(TAG, "handleDnsRequest: Discarding non-DNS or invalid packet", e);
            return;
        }
        if (dnsMsg.getQuestion() == null) {
            Log.i(TAG, "handleDnsRequest: Discarding DNS packet with no query " + dnsMsg);
            return;
        }
        String dnsQueryName = dnsMsg.getQuestion().name.toString();

        try {
            Logger.info("Provider: Resolving " + dnsQueryName + " Type: " + dnsMsg.getQuestion().type.name() + " Sending to " + destAddr + ":" + destPort);
            DatagramPacket outPacket = new DatagramPacket(dnsRawData, 0, dnsRawData.length, destAddr,
                    destPort);
            forwardPacket(outPacket, parsedPacket);
        } catch (Exception e) {
            Logger.logException(e);
        }
    }

    /**
     * Helper class holding a socket, the packet we are waiting the answer for, and a time
     */
    private static class WaitingOnSocketPacket {
        final DatagramSocket socket;
        final IpPacket packet;
        private final long time;

        WaitingOnSocketPacket(DatagramSocket socket, IpPacket packet) {
            this.socket = socket;
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        long ageSeconds() {
            return (System.currentTimeMillis() - time) / 1000;
        }
    }

    /**
     * Queue of WaitingOnSocketPacket, bound on time and space.
     */
    private static class WospList implements Iterable<WaitingOnSocketPacket> {
        private final LinkedList<WaitingOnSocketPacket> list = new LinkedList<>();

        void add(WaitingOnSocketPacket wosp) {
            if (list.size() > 1024) {
                Log.d(TAG, "Dropping socket due to space constraints: " + list.element().socket);
                list.element().socket.close();
                list.remove();
            }
            while (!list.isEmpty() && list.element().ageSeconds() > 10) {
                Log.d(TAG, "Timeout on socket " + list.element().socket);
                list.element().socket.close();
                list.remove();
            }
            list.add(wosp);
        }

        public Iterator<WaitingOnSocketPacket> iterator() {
            return list.iterator();
        }

        int size() {
            return list.size();
        }

    }
}
