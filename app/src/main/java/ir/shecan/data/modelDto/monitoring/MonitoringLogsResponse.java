package ir.shecan.data.modelDto.monitoring;

public class MonitoringLogsResponse {
    private int code;
    private boolean success;
    private int acceptedLogs;
    private int rejectedLogs;

    public int getCode() {
        return code;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getAcceptedLogs() {
        return acceptedLogs;
    }

    public int getRejectedLogs() {
        return rejectedLogs;
    }
}
