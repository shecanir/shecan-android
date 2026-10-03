package ir.shecan.data.modelDto.monitoring;

import static org.junit.Assert.assertEquals;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

public class MonitoringTargetsResponseTest {

    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    @Test
    public void parsesCamelCaseTargetsResponse() {
        String json = "{"
                + "\"version\":1,"
                + "\"intervalSeconds\":127,"
                + "\"samplingPercent\":37,"
                + "\"targets\":[{"
                + "\"id\":\"de1-proxy\","
                + "\"type\":\"proxy\","
                + "\"ip\":\"144.76.1.88\","
                + "\"proxyNode\":\"de1\","
                + "\"datacenter\":\"Germany\","
                + "\"targetDomain\":\"gemini.google.com\","
                + "\"timeoutMs\":3210"
                + "}]}";

        MonitoringTargetsResponse response = gson.fromJson(json, MonitoringTargetsResponse.class);
        MonitoringTarget target = response.getTargets().get(0);

        assertEquals(127, response.getIntervalSeconds());
        assertEquals(37, response.getSamplingPercent());
        assertEquals("de1", target.getProxyNode());
        assertEquals("gemini.google.com", target.getTargetDomain());
        assertEquals(3210, target.getTimeoutMs(5000));
    }

    @Test
    public void keepsSnakeCaseBackwardCompatibility() {
        String json = "{"
                + "\"interval_seconds\":180,"
                + "\"sampling_percent\":25,"
                + "\"targets\":[{"
                + "\"type\":\"proxy\","
                + "\"proxy_node\":\"de2\","
                + "\"target_domain\":\"example.com\","
                + "\"timeout_ms\":2500"
                + "}]}";

        MonitoringTargetsResponse response = gson.fromJson(json, MonitoringTargetsResponse.class);
        MonitoringTarget target = response.getTargets().get(0);

        assertEquals(180, response.getIntervalSeconds());
        assertEquals(25, response.getSamplingPercent());
        assertEquals("de2", target.getProxyNode());
        assertEquals("example.com", target.getTargetDomain());
        assertEquals(2500, target.getTimeoutMs(5000));
    }
}
