package ir.shecan.data.modelDto.monitoring;

import java.util.List;

public class MonitoringLogsRequest {
    private final String deviceId;
    private final String appVersion;
    private final String platform;
    private final MonitoringNetworkInfo network;
    private final List<MonitoringLog> logs;

    public MonitoringLogsRequest(
            String deviceId,
            String appVersion,
            String platform,
            MonitoringNetworkInfo network,
            List<MonitoringLog> logs
    ) {
        this.deviceId = deviceId;
        this.appVersion = appVersion;
        this.platform = platform;
        this.network = network;
        this.logs = logs;
    }
}
