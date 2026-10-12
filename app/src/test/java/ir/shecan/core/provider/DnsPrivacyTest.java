package ir.shecan.core.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import ir.shecan.Shecan;
import ir.shecan.ConfigurationPersistenceTest;
import ir.shecan.core.util.Logger;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = ConfigurationPersistenceTest.TestApplication.class, sdk = 28)
public class DnsPrivacyTest {
    @Test
    public void debugResponseDoesNotRecordBrowsedDomain() throws Exception {
        Shecan.getPrefs().edit().putBoolean("settings_debug_output", true).commit();
        Logger.init();
        String domain = "private-browsing.example";
        DNSMessage response = DNSMessage.builder().addQuestion(new Question(domain, Record.TYPE.A))
                .setQrFlag(true).build();
        new UdpProvider(null, null).handleDnsResponse(null, response.toArray());
        assertFalse(Logger.getLog().contains(domain));
        assertTrue(Logger.getLog().contains("bytes"));
    }
}
