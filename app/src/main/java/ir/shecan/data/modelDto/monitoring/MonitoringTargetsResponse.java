package ir.shecan.data.modelDto.monitoring;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MonitoringTargetsResponse {
    private int version;
    @SerializedName(value = "intervalSeconds", alternate = {"interval_seconds"})
    private Integer intervalSeconds;
    @SerializedName(value = "samplingPercent", alternate = {"sampling_percent"})
    private Integer samplingPercent;
    private List<MonitoringTarget> targets;

    public int getVersion() {
        return version;
    }

    public int getIntervalSeconds() {
        return intervalSeconds != null && intervalSeconds > 0 ? intervalSeconds : 300;
    }

    public int getSamplingPercent() {
        if (samplingPercent == null) return 100;
        return Math.max(0, Math.min(100, samplingPercent));
    }

    public List<MonitoringTarget> getTargets() {
        return targets != null ? targets : new ArrayList<>();
    }
}
