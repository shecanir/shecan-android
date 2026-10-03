package ir.shecan.data.modelDto.monitoring;

public class MonitoringNetworkInfo {
    private final String type;
    private final String carrier;
    private final String country;

    public MonitoringNetworkInfo(String type, String carrier, String country) {
        this.type = type;
        this.carrier = carrier;
        this.country = country;
    }
}
