package ir.shecan.data.modelDto;

import static org.junit.Assert.assertEquals;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

public class HomePageConfigTest {

    @Test
    public void parsesDynamicDataAndMonitoringUrls() {
        String json = "{"
                + "\"dynamic_data\":{"
                + "\"banner\":\"https://n8n.coolify.shcn.ir/webhook/api/banner/match\","
                + "\"dialog\":{"
                + "\"match\":\"https://n8n.coolify.shcn.ir/webhook/api/dialog/match\","
                + "\"dismiss\":\"https://n8n.coolify.shcn.ir/webhook/api/dialog/dismiss\","
                + "\"action\":\"https://n8n.coolify.shcn.ir/webhook/api/dialog/action\""
                + "}"
                + "},"
                + "\"monitoring\":{"
                + "\"target\":\"https://n8n.coolify.shcn.ir/webhook/api/monitoring/targets\","
                + "\"logs\":\"https://n8n.coolify.shcn.ir/webhook/api/monitoring/logs\""
                + "},"
                + "\"sentry\":{"
                + "\"desktop\":{\"sample_rate\":25,\"url\":\"https://sentry.example/desktop\"},"
                + "\"android\":{\"sample_rate\":100,\"url\":\"https://sentry.example/android\"}"
                + "},"
                + "\"sentry_sample_rate\":1"
                + "}";

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        HomePage homePage = gson.fromJson(json, HomePage.class);

        assertEquals(
                "https://n8n.coolify.shcn.ir/webhook/api/banner/match",
                homePage.getDynamicData().getBanner()
        );
        assertEquals(
                "https://n8n.coolify.shcn.ir/webhook/api/dialog/match",
                homePage.getDynamicData().getDialog().getMatch()
        );
        assertEquals(
                "https://n8n.coolify.shcn.ir/webhook/api/dialog/dismiss",
                homePage.getDynamicData().getDialog().getDismiss()
        );
        assertEquals(
                "https://n8n.coolify.shcn.ir/webhook/api/dialog/action",
                homePage.getDynamicData().getDialog().getAction()
        );
        assertEquals(
                "https://n8n.coolify.shcn.ir/webhook/api/monitoring/targets",
                homePage.getMonitoring().getTarget()
        );
        assertEquals(
                "https://n8n.coolify.shcn.ir/webhook/api/monitoring/logs",
                homePage.getMonitoring().getLogs()
        );
        assertEquals(Integer.valueOf(100), homePage.getSentrySampleRate());
        assertEquals("https://sentry.example/android", homePage.getSentry().getAndroid().getUrl());
    }

    @Test
    public void fallsBackToLegacySentrySampleRate() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        HomePage homePage = gson.fromJson("{\"sentry_sample_rate\":25}", HomePage.class);

        assertEquals(Integer.valueOf(25), homePage.getSentrySampleRate());
    }
}
