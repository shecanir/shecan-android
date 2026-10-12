package ir.shecan.core.provider;

import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

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
import java.util.List;
import java.util.Map;
import java.util.Queue;

import de.measite.minidns.DNSMessage;
import ir.shecan.Shecan;
import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.core.util.Logger;

/**
 * Refactored UdpProvider
 * - Prevents PFD/socket leaks by creating ParcelFileDescriptor once per socket and closing it.
 * - Uses snapshots when building poll arrays to avoid concurrent modification issues.
 * - Null checks for deviceWrites.poll()
 * - Proper handling of InputStream.read() == -1 (EOF)
 * - Uses actual DatagramPacket.getLength() when reading replies
 * - Protects DatagramSocket with setSoTimeout to avoid infinite blocking on receive
 */
public class UdpProvider extends Provider {
    private static final String TAG = "UdpProvider";

    private final WospList dnsIn = new WospList();
    public FileDescriptor mBlockFd = null;
    public FileDescriptor mInterruptFd = null;
    // deviceWrites should be thread-safe; use synchronized access via dnsIn/deviceWrites monitors
    final Queue<byte[]> deviceWrites = new LinkedList<>();

    public UdpProvider(ParcelFileDescriptor descriptor, ShecanVpnService service) {
        super(descriptor, service);
    }

    public void stop() {
        try {
            if (mInterruptFd != null) {
                try {
                    Os.close(mInterruptFd);
                } catch (Exception ignored) {}
                mInterruptFd = null;
            }
            if (mBlockFd != null) {
                try {
                    Os.close(mBlockFd);
                } catch (Exception ignored) {}
                mBlockFd = null;
            }
            dnsIn.closeAll();
            // clear deviceWrites
            synchronized (deviceWrites) {
                deviceWrites.clear();
            }
        } catch (Exception ignored) {
        }
    }

    private void queueDeviceWrite(IpPacket ipOutPacket) {
        dnsQueryTimes++;
        if (ipOutPacket == null) return;
        byte[] raw = ipOutPacket.getRawData();
        if (raw == null) return;
        synchronized (deviceWrites) {
            deviceWrites.add(raw);
        }
    }

    public void process() {
        try {
            Log.d(TAG, "Starting advanced DNS proxy.");
            FileDescriptor[] pipes = Os.pipe();
            mInterruptFd = pipes[0];
            mBlockFd = pipes[1];

            try (FileInputStream inputStream = new FileInputStream(descriptor.getFileDescriptor());
                 FileOutputStream outputStream = new FileOutputStream(descriptor.getFileDescriptor())) {

                byte[] packet = new byte[32767];

                while (running) {
                    // Build poll fds: device, block, and snapshot of dns sockets
                    StructPollfd deviceFd = new StructPollfd();
                    deviceFd.fd = inputStream.getFD();
                    deviceFd.events = (short) OsConstants.POLLIN;

                    StructPollfd blockFd = new StructPollfd();
                    blockFd.fd = mBlockFd;
                    blockFd.events = (short) (OsConstants.POLLHUP | OsConstants.POLLERR);

                    boolean hasDeviceWrites;
                    synchronized (deviceWrites) {
                        hasDeviceWrites = !deviceWrites.isEmpty();
                    }
                    if (hasDeviceWrites) {
                        deviceFd.events |= (short) OsConstants.POLLOUT;
                    }

                    // Create snapshot of WaitingOnSocketPacket to avoid concurrent modification and to reuse stored PFD
                    List<WaitingOnSocketPacket> snapshot = dnsIn.snapshot();

                    StructPollfd[] polls = new StructPollfd[2 + snapshot.size()];
                    polls[0] = deviceFd;
                    polls[1] = blockFd;

                    for (int i = 0; i < snapshot.size(); i++) {
                        WaitingOnSocketPacket wosp = snapshot.get(i);
                        StructPollfd pollFd = new StructPollfd();
                        // use stored ParcelFileDescriptor's FileDescriptor to avoid creating a new PFD here
                        pollFd.fd = wosp.getParcelFileDescriptor().getFileDescriptor();
                        pollFd.events = (short) OsConstants.POLLIN;
                        polls[2 + i] = pollFd;
                    }

                    Log.d(TAG, "doOne: Polling " + polls.length + " file descriptors");
                    // blocking poll; rely on blockFd to wake us up on stop
                    Os.poll(polls, -1);

                    if (blockFd.revents != 0) {
                        Log.i(TAG, "Told to stop VPN");
                        running = false;
                        return;
                    }

                    // First handle any sockets that became readable — iterate over snapshot and remove from dnsIn properly
                    for (int i = 0; i < snapshot.size(); i++) {
                        WaitingOnSocketPacket wosp = snapshot.get(i);
                        StructPollfd p = polls[2 + i];
                        if ((p.revents & OsConstants.POLLIN) != 0) {
                            Log.d(TAG, "Read from UDP DNS socket " + wosp.socket);
                            // remove the specific wosp from dnsIn (it may have aged out already)
                            if (dnsIn.removeSpecific(wosp)) {
                                // handle response; close socket & pfd inside
                                handleRawDnsResponse(wosp);
                            } else {
                                // if couldn't remove, still try to close resources to be safe
                                wosp.closeQuietly();
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
            } // try-with-resources closes streams
        } catch (Exception e) {
            Logger.logException(e);
        } finally {
            // Try to clean up if process exits
            try {
                dnsIn.closeAll();
            } catch (Exception ignored) {}
            try {
                if (mInterruptFd != null) {
                    Os.close(mInterruptFd);
                    mInterruptFd = null;
                }
            } catch (Exception ignored) {}
            try {
                if (mBlockFd != null) {
                    Os.close(mBlockFd);
                    mBlockFd = null;
                }
            } catch (Exception ignored) {}
        }
    }

    void writeToDevice(FileOutputStream outFd) throws ShecanVpnService.VpnNetworkException {
        byte[] data = null;
        synchronized (deviceWrites) {
            data = deviceWrites.poll();
        }
        if (data == null) {
            // nothing to write
            return;
        }
        try {
            outFd.write(data);
        } catch (IOException e) {
            throw new ShecanVpnService.VpnNetworkException("Outgoing VPN output stream closed", e);
        }
    }

    void readPacketFromDevice(FileInputStream inputStream, byte[] packet) throws ShecanVpnService.VpnNetworkException, SocketException {
        // Read the outgoing packet from the input stream.
        int length;

        try {
            length = inputStream.read(packet);
        } catch (IOException e) {
            Logger.logException(e);
            running = false;
            throw new ShecanVpnService.VpnNetworkException("Cannot read from device", e);
        }

        if (length == -1) {
            // EOF — treat as stop condition or skip
            Log.w(TAG, "Device read returned EOF (-1). Stopping provider.");
            running = false;
            return;
        }

        if (length == 0) {
            Log.w(TAG, "Got empty packet!");
            return;
        }

        final byte[] readPacket = Arrays.copyOfRange(packet, 0, length);

        handleDnsRequest(readPacket);
    }

    void forwardPacket(DatagramPacket outPacket, IpPacket parsedPacket) throws ShecanVpnService.VpnNetworkException {
        DatagramSocket dnsSocket = null;
        ParcelFileDescriptor pfd = null;
        try {
            dnsSocket = new DatagramSocket();
            // set a short timeout so receive won't block forever later
            dnsSocket.setSoTimeout(3000);
            service.protect(dnsSocket);

            dnsSocket.send(outPacket);

            if (parsedPacket != null) {
                // create ParcelFileDescriptor once and store it with the wosp
                pfd = ParcelFileDescriptor.fromDatagramSocket(dnsSocket);
                WaitingOnSocketPacket wosp = new WaitingOnSocketPacket(dnsSocket, parsedPacket, pfd);
                dnsIn.add(wosp);
            } else {
                dnsSocket.close();
            }
        } catch (IOException e) {
            // If sending failed, attempt to handle locally
            try {
                handleDnsResponse(parsedPacket, outPacket.getData());
            } catch (Exception ex) {
                Logger.logException(ex);
            }
            Logger.warning("DNSProvider: Could not send packet to upstream, forwarding packet directly");
            if (dnsSocket != null && !dnsSocket.isClosed()) {
                try {
                    dnsSocket.close();
                } catch (Exception ignored) {}
            }
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private void handleRawDnsResponse(WaitingOnSocketPacket wosp) {
        DatagramSocket dnsSocket = wosp.socket;
        try {
            // Use a sufficiently large buffer; DatagramPacket will tell us actual length
            byte[] datagramData = new byte[4096];
            DatagramPacket replyPacket = new DatagramPacket(datagramData, datagramData.length);
            dnsSocket.receive(replyPacket);
            // use the actual returned length
            int len = replyPacket.getLength();
            byte[] actual = Arrays.copyOf(replyPacket.getData(), len);
            handleDnsResponse(wosp.packet, actual);
        } catch (SocketException se) {
            // socket timeout or closed
            Logger.logException(se);
        } catch (Exception e) {
            Logger.logException(e);
        } finally {
            // ensure socket and pfd closed
            wosp.closeQuietly();
        }
    }


    /**
     * Handles a responsePayload from an upstream DNS server
     *
     * @param requestPacket   The original request packet
     * @param responsePayload The payload of the response
     */
    void handleDnsResponse(IpPacket requestPacket, byte[] responsePayload) {
        if (responsePayload == null) return;
        if (Shecan.getPrefs().getBoolean("settings_debug_output", false)) {
            Logger.debug("DNS response received: " + responsePayload.length + " bytes");
        }

        if (requestPacket == null) return;
        if (!(requestPacket.getPayload() instanceof UdpPacket)) {
            Log.i(TAG, "handleDnsResponse: requestPacket payload is not UDP");
            return;
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

        if (packetData == null || packetData.length == 0) return;

        IpPacket parsedPacket;
        try {
            parsedPacket = (IpPacket) IpSelector.newPacket(packetData, 0, packetData.length);
        } catch (Exception e) {
            Log.w(TAG, "handleDnsRequest: Discarding invalid IP packet (" + e.getClass().getSimpleName() + ")");
            return;
        }

        if (!(parsedPacket.getPayload() instanceof UdpPacket)) {
            Log.i(TAG, "handleDnsRequest: Discarding non-UDP packet");
            return;
        }

        InetAddress destAddr = parsedPacket.getHeader().getDstAddr();
        if (destAddr == null) {
            Log.i(TAG, "handleDnsRequest: destination address is null");
            return;
        }

        // defensive snapshot of dnsServers to avoid NPE if service clears it concurrently
        Pair<String, Integer> destination;
        try {
            // grab reference once (avoid race where service.dnsServers becomes null)
            final Map<String, Pair<String,Integer>> dnsServersRef = service.dnsServers;
            if (dnsServersRef == null) {
                Logger.error("handleDnsRequest: dnsServers map is null (VPN shutting down or not initialized). Dropping packet for " + destAddr.getHostAddress());
                return;
            }

            destination = dnsServersRef.get(destAddr.getHostAddress());
        } catch (ClassCastException cce) {
            Logger.logException(cce);
            Logger.error("handleDnsRequest: unexpected dnsServers type. Dropping packet for " + destAddr.getHostAddress());
            return;
        } catch (Exception ex) {
            Logger.logException(ex);
            Logger.error("handleDnsRequest: error reading dnsServers for " + destAddr.getHostAddress());
            return;
        }

        if (destination == null) {
            Logger.error("handleDnsRequest: No DNS mapping for " + destAddr.getHostAddress());
            return;
        }
        InetAddress mappedAddr;
        int destPort;
        try {
            mappedAddr = InetAddress.getByName(destination.first);
            destPort = destination.second;
        } catch (Exception e) {
            Logger.logException(e);
            Logger.error("handleDnsRequest: DNS server alias query failed for " + destAddr.getHostAddress());
            return;
        }

        UdpPacket parsedUdp = (UdpPacket) parsedPacket.getPayload();
        byte[] dnsRawData;

        // ✅ جلوگیری از RuntimeException هنگام استخراج RawData
        try {
            if (parsedUdp.getPayload() == null) {
                Log.w(TAG, "handleDnsRequest: UDP payload is null, sending empty packet");
                DatagramPacket outPacket = new DatagramPacket(new byte[0], 0, 0, mappedAddr, destPort);
                forwardPacket(outPacket, null);
                return;
            }
            dnsRawData = parsedUdp.getPayload().getRawData();
        } catch (RuntimeException re) {
            Logger.logException(re);
            Log.w(TAG, "handleDnsRequest: Dropping packet due to RuntimeException while extracting UDP payload");
            return;
        }

        if (dnsRawData == null || dnsRawData.length == 0) {
            Log.i(TAG, "handleDnsRequest: empty DNS raw data");
            return;
        }

        // ✅ چک اندازه هدر DNS
        if (dnsRawData.length < 12) {
            Log.w(TAG, "handleDnsRequest: DNS packet too small (" + dnsRawData.length + " bytes). Dropping.");
            if (Shecan.getPrefs().getBoolean("settings_debug_output", false)) {
                Logger.debug("DNS payload too small: " + dnsRawData.length + " bytes");
            }
            return;
        }

        DNSMessage dnsMsg;
        try {
            dnsMsg = new DNSMessage(dnsRawData);
            if (Shecan.getPrefs().getBoolean("settings_debug_output", false)) {
                Logger.debug("DNS query received: " + dnsRawData.length + " bytes");
            }
        } catch (IOException | RuntimeException e) {
            // گرفتن خطاهای minidns و pcap4j (مثلاً ArrayIndexOutOfBounds)
            Log.w(TAG, "handleDnsRequest: Discarding malformed DNS packet: " + e.getClass().getSimpleName());
            if (Shecan.getPrefs().getBoolean("settings_debug_output", false)) {
                Logger.debug("Malformed DNS payload: " + dnsRawData.length + " bytes");
            }
            return;
        }

        if (dnsMsg.getQuestion() == null) {
            Log.i(TAG, "handleDnsRequest: Discarding DNS packet with no query");
            return;
        }

        try {
            if (Shecan.getPrefs().getBoolean("settings_debug_output", false)) {
                Logger.debug("Forwarding DNS query type: " + dnsMsg.getQuestion().type.name());
            }

            DatagramPacket outPacket = new DatagramPacket(dnsRawData, 0, dnsRawData.length, mappedAddr, destPort);
            forwardPacket(outPacket, parsedPacket);
        } catch (Exception e) {
            Logger.logException(e);
            Log.e(TAG, "handleDnsRequest: Failed to forward DNS packet: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Helper class holding a socket, the packet we are waiting the answer for, and a time
     * Now also holds a ParcelFileDescriptor for safe polling, and a close helper.
     */
    private static class WaitingOnSocketPacket {
        final DatagramSocket socket;
        final IpPacket packet;
        private final long time;
        private final ParcelFileDescriptor pfd; // may be null if unavailable

        WaitingOnSocketPacket(DatagramSocket socket, IpPacket packet, ParcelFileDescriptor pfd) {
            this.socket = socket;
            this.packet = packet;
            this.time = System.currentTimeMillis();
            this.pfd = pfd;
        }

        ParcelFileDescriptor getParcelFileDescriptor() {
            return pfd;
        }

        long ageSeconds() {
            return (System.currentTimeMillis() - time) / 1000;
        }

        void closeQuietly() {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
            try {
                if (pfd != null) pfd.close();
            } catch (Exception ignored) {}
        }

        @NonNull
        @Override
        public String toString() {
            return "Wosp[" + socket + ", age=" + ageSeconds() + "]";
        }
    }

    /**
     * Queue of WaitingOnSocketPacket, bound on time and space.
     * Thread-safe basic operations; iterator() returns iterator on snapshot to avoid concurrency issues.
     */
    private static class WospList implements Iterable<WaitingOnSocketPacket> {
        private final LinkedList<WaitingOnSocketPacket> list = new LinkedList<>();

        synchronized void add(WaitingOnSocketPacket wosp) {
            if (list.size() > 1024) {
                Log.d(TAG, "Dropping socket due to space constraints: " + list.element().socket);
                try {
                    list.element().closeQuietly();
                } catch (Exception ignored) {}
                list.remove();
            }
            while (!list.isEmpty() && list.element().ageSeconds() > 10) {
                Log.d(TAG, "Timeout on socket " + list.element().socket);
                try {
                    list.element().closeQuietly();
                } catch (Exception ignored) {}
                list.remove();
            }
            list.add(wosp);
        }

        /**
         * Remove a specific WaitingOnSocketPacket (by object equality).
         * @return true if removed
         */
        synchronized boolean removeSpecific(WaitingOnSocketPacket wosp) {
            return list.remove(wosp);
        }

        /**
         * Create a snapshot (copy) of the current list for safe iteration.
         */
        synchronized List<WaitingOnSocketPacket> snapshot() {
            return new LinkedList<>(list);
        }

        public Iterator<WaitingOnSocketPacket> iterator() {
            // Return iterator on a copy to avoid concurrent modification externally.
            return snapshot().iterator();
        }

        synchronized int size() {
            return list.size();
        }

        synchronized void closeAll() {
            for (WaitingOnSocketPacket w : list) {
                try {
                    w.closeQuietly();
                } catch (Exception ignored) {}
            }
            list.clear();
        }
    }
}
