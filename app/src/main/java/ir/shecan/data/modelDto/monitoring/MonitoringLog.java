package ir.shecan.data.modelDto.monitoring;

import java.util.Map;

public class MonitoringLog {
    public String targetId;
    public long eventId;
    public String testType;
    public boolean success;
    public String error;
    public String errorType;
    public String userId;
    public String sessionId;
    public String isp;
    public String region;
    public String domain;
    public Long latencyMs;
    public String publicIp;
    public String resolvedIp;
    public String proxyNode;
    public String datacenter;
    public String targetDomain;
    public Long tcpConnectMs;
    public Long tlsHandshakeMs;
    public Integer httpStatus;
    public Long responseTimeMs;
    public Boolean tlsSuccess;
    public Map<String, Object> responseBody;
    public String timestamp;
    public Map<String, Object> payload;
}
