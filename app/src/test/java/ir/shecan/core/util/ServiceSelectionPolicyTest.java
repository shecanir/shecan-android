package ir.shecan.core.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.junit.Test;
import ir.shecan.core.constant.RequestStatus;
import ir.shecan.data.modelDto.ServiceItem;

public class ServiceSelectionPolicyTest {
    private ServiceItem service(String order, RequestStatus status) {
        ServiceItem item = new ServiceItem(order, "silver", "1m", "active", "link", 0, 0, null);
        item.statusId = status.getValue();
        return item;
    }

    @Test
    public void prefersActivePaidServiceOverFreeClosedAndInactiveServices() {
        ServiceItem free = service("0", RequestStatus.ACTIVE);
        ServiceItem closed = service("1", RequestStatus.ACTIVE);
        closed.closedOn = "2020-01-01";
        ServiceItem inactive = service("2", RequestStatus.WAITING_FOR_ACTIVATION);
        ServiceItem active = service("3", RequestStatus.IN_USE);
        assertSame(active, ServiceSelectionPolicy.findPreferredActivePaidService(
                Arrays.asList(free, closed, inactive, active)));
    }

    @Test
    public void renewsExpiredServiceButHonorsRenewalBlock() {
        ServiceItem expired = service("1", RequestStatus.ACTIVE);
        expired.dueDate = "2000-01-01T00:00:00Z";
        assertTrue(ServiceSelectionPolicy.shouldOpenRenewal(expired));
        expired.statusId = RequestStatus.SUSPENDED_FOR_VIOLATION.getValue();
        assertFalse(ServiceSelectionPolicy.shouldOpenRenewal(expired));
    }

    @Test
    public void activeServiceWithFutureDueDateCanConnect() {
        ServiceItem active = service("1", RequestStatus.ACTIVE);
        active.dueDate = "2999-01-01";
        assertFalse(ServiceSelectionPolicy.shouldOpenRenewal(active));
    }
}
