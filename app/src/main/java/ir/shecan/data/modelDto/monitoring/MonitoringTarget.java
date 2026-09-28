package ir.shecan.data.modelDto.monitoring;

import com.google.gson.annotations.SerializedName;

public class MonitoringTarget {
    private String id;
    private String type;
    private String ip;
    private Integer port;
    private String domain;
    private String url;
    @SerializedName(value = "timeoutMs", alternate = {"timeout_ms"})
    private Integer timeoutMs;
    @SerializedName(value = "proxyNode", alternate = {"proxy_node"})
    private String proxyNode;
    private String datacenter;
    @SerializedName(value = "targetDomain", alternate = {"target_domain"})
    private String targetDomain;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public String getDomain() {
        return domain;
    }

    public String getUrl() {
        return url;
    }

    public int getTimeoutMs(int fallback) {
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : fallback;
    }

    public String getProxyNode() {
        return proxyNode;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public String getTargetDomain() {
        return targetDomain;
    }
}
