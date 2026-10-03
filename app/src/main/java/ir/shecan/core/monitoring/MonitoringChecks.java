package ir.shecan.core.monitoring;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import ir.shecan.data.modelDto.monitoring.MonitoringLog;
import ir.shecan.data.modelDto.monitoring.MonitoringTarget;

class MonitoringChecks {
    private static final String DEFAULT_DNS_DOMAIN = "cp.cloudflare.com";
    private static final AtomicLong EVENT_COUNTER = new AtomicLong(1);

    private final MonitoringConnectivity connectivity;
    private final MonitoringIdentity identity;

    MonitoringChecks(MonitoringConnectivity connectivity, MonitoringIdentity identity) {
        this.connectivity = connectivity;
        this.identity = identity;
    }

    MonitoringLog run(MonitoringTarget target) {
        String type = lower(target.getType());
        if ("dns".equals(type)) return dns(target);
        if ("proxy".equals(type)) return proxy(target);
        if ("http".equals(type)) return http(target);

        MonitoringLog log = baseLog(target, type);
        log.success = false;
        log.errorType = "unsupported_target_type";
        log.error = "Unsupported target type";
        return log;
    }

    private MonitoringLog dns(MonitoringTarget target) {
        MonitoringLog log = baseLog(target, "dns");
        if (safe(target.getIp()).isEmpty()) {
            log.success = false;
            log.errorType = "invalid_target";
            log.error = "DNS target ip is empty";
            return log;
        }

        String domain = safe(target.getDomain()).isEmpty() ? DEFAULT_DNS_DOMAIN : target.getDomain();
        log.domain = domain;
        int timeout = target.getTimeoutMs(3000);
        long start = System.nanoTime();

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeout);
            byte[] query = buildDnsQuery(domain);
            DatagramPacket request = new DatagramPacket(
                    query,
                    query.length,
                    InetAddress.getByName(target.getIp()),
                    53
            );
            socket.send(request);

            byte[] response = new byte[512];
            DatagramPacket packet = new DatagramPacket(response, response.length);
            socket.receive(packet);

            log.latencyMs = elapsedMillis(start);
            int rCode = response[3] & 0x0F;
            log.success = rCode == 0;
            log.error = dnsRCode(rCode);
            log.resolvedIp = firstARecord(response, packet.getLength(), query.length);
        } catch (Exception e) {
            log.latencyMs = elapsedMillis(start);
            log.success = false;
            log.errorType = e.getClass().getSimpleName();
            log.error = safe(e.getMessage());
        }
        return log;
    }

    private MonitoringLog proxy(MonitoringTarget target) {
        MonitoringLog log = baseLog(target, "proxy");
        log.proxyNode = target.getProxyNode();
        log.datacenter = target.getDatacenter();
        log.targetDomain = safe(target.getTargetDomain()).isEmpty()
                ? target.getDomain()
                : target.getTargetDomain();
        if (safe(target.getIp()).isEmpty()) {
            log.success = false;
            log.errorType = "invalid_target";
            log.error = "Proxy target ip is empty";
            return log;
        }

        int timeout = target.getTimeoutMs(5000);
        int port = target.getPort() != null ? target.getPort() : 443;
        long start = System.nanoTime();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.getIp(), port), timeout);
            long elapsed = elapsedMillis(start);
            log.success = true;
            log.latencyMs = elapsed;
            log.tcpConnectMs = elapsed;
        } catch (Exception e) {
            long elapsed = elapsedMillis(start);
            log.success = false;
            log.latencyMs = elapsed;
            log.tcpConnectMs = elapsed;
            log.errorType = e.getClass().getSimpleName();
            log.error = safe(e.getMessage());
        }
        return log;
    }

    private MonitoringLog http(MonitoringTarget target) {
        MonitoringLog log = baseLog(target, "http");
        if (safe(target.getUrl()).isEmpty()) {
            log.success = false;
            log.errorType = "invalid_target";
            log.error = "HTTP target url is empty";
            return log;
        }

        int timeout = target.getTimeoutMs(5000);
        long start = System.nanoTime();
        HttpURLConnection connection = null;

        try {
            URL url = new URL(target.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);

            int status = connection.getResponseCode();
            long elapsed = elapsedMillis(start);
            log.httpStatus = status;
            log.responseTimeMs = elapsed;
            log.latencyMs = elapsed;
            log.tlsSuccess = "https".equalsIgnoreCase(url.getProtocol());
            log.success = status >= 200 && status < 400;
            log.responseBody = readSmallJsonBody(connection);
        } catch (Exception e) {
            long elapsed = elapsedMillis(start);
            log.success = false;
            log.responseTimeMs = elapsed;
            log.latencyMs = elapsed;
            log.tlsSuccess = false;
            log.errorType = e.getClass().getSimpleName();
            log.error = safe(e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
        return log;
    }

    private MonitoringLog baseLog(MonitoringTarget target, String type) {
        MonitoringLog log = new MonitoringLog();
        log.targetId = target.getId();
        log.eventId = EVENT_COUNTER.getAndIncrement();
        log.testType = type;
        log.userId = identity.hashedUserId();
        log.sessionId = identity.sessionId();
        log.isp = connectivity.carrierName();
        log.timestamp = utcTimestamp();
        log.payload = new HashMap<>();
        return log;
    }

    private byte[] buildDnsQuery(String domain) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        for (String label : domain.split("\\.")) {
            byte[] labelBytes = label.getBytes("UTF-8");
            out.write(labelBytes.length);
            out.write(labelBytes);
        }
        out.write(0x00);
        out.write(new byte[]{0x00, 0x01, 0x00, 0x01});
        return out.toByteArray();
    }

    private String firstARecord(byte[] response, int length, int questionEnd) {
        try {
            int answerCount = ((response[6] & 0xFF) << 8) | (response[7] & 0xFF);
            int offset = questionEnd;
            for (int i = 0; i < answerCount && offset + 12 <= length; i++) {
                offset = skipDnsName(response, offset, length);
                int type = ((response[offset] & 0xFF) << 8) | (response[offset + 1] & 0xFF);
                int dataLength = ((response[offset + 8] & 0xFF) << 8) | (response[offset + 9] & 0xFF);
                offset += 10;
                if (type == 1 && dataLength == 4 && offset + 4 <= length) {
                    return (response[offset] & 0xFF) + "."
                            + (response[offset + 1] & 0xFF) + "."
                            + (response[offset + 2] & 0xFF) + "."
                            + (response[offset + 3] & 0xFF);
                }
                offset += dataLength;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private int skipDnsName(byte[] data, int offset, int length) {
        while (offset < length) {
            int value = data[offset] & 0xFF;
            if ((value & 0xC0) == 0xC0) return offset + 2;
            if (value == 0) return offset + 1;
            offset += value + 1;
        }
        return offset;
    }

    private Map<String, Object> readSmallJsonBody(HttpURLConnection connection) {
        try {
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.US).contains("json")) {
                return null;
            }
            InputStream stream = connection.getInputStream();
            byte[] buffer = new byte[256];
            int read = stream.read(buffer);
            if (read <= 0) return null;
            Map<String, Object> body = new HashMap<>();
            body.put("raw", new String(Arrays.copyOf(buffer, read), "UTF-8"));
            return body;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String dnsRCode(int rCode) {
        switch (rCode) {
            case 0:
                return "NOERROR";
            case 1:
                return "FORMERR";
            case 2:
                return "SERVFAIL";
            case 3:
                return "NXDOMAIN";
            case 5:
                return "REFUSED";
            default:
                return "RCODE_" + rCode;
        }
    }

    private long elapsedMillis(long startNanos) {
        return Math.max(0, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private String utcTimestamp() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String lower(String value) {
        return value != null ? value.toLowerCase(Locale.US) : "";
    }
}
