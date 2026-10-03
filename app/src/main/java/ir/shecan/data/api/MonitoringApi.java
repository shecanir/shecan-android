package ir.shecan.data.api;

import android.content.Context;

import ir.shecan.Shecan;
import ir.shecan.data.modelDto.monitoring.MonitoringLogsRequest;
import ir.shecan.data.modelDto.monitoring.MonitoringLogsResponse;
import ir.shecan.data.modelDto.monitoring.MonitoringTargetsResponse;

public class MonitoringApi {
    private final ApiRepository repo;

    public MonitoringApi(Context context) {
        repo = new ApiRepository(context);
    }

    public void targets(ApiCallback<MonitoringTargetsResponse> callback) {
        repo.request(
                "monitoring_targets",
                null,
                Shecan.ShecanInfo.getMonitoringTargetUrl(),
                HttpMethod.GET,
                false,
                callback,
                MonitoringTargetsResponse.class
        );
    }

    public void sendLogs(MonitoringLogsRequest request, ApiCallback<MonitoringLogsResponse> callback) {
        repo.request(
                "monitoring_logs_" + System.currentTimeMillis(),
                request,
                Shecan.ShecanInfo.getMonitoringLogsUrl(),
                HttpMethod.POST,
                false,
                callback,
                MonitoringLogsResponse.class
        );
    }
}
