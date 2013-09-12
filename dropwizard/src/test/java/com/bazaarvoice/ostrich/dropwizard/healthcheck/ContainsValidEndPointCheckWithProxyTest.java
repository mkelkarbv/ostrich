package com.bazaarvoice.ostrich.dropwizard.healthcheck;

import com.bazaarvoice.ostrich.ServicePool;
import com.bazaarvoice.ostrich.pool.ServicePoolProxyHelper;
import com.yammer.metrics.core.HealthCheck;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContainsValidEndPointCheckWithProxyTest {
    private final String _name = "test";
    @SuppressWarnings("unchecked") private final ServicePool<Service> _pool = mock(ServicePool.class);
    private Service _proxy;

    @Before
    public void setUp() {
        _proxy = ServicePoolProxyHelper.createMock(Service.class, _pool);
    }

    @Test (expected = NullPointerException.class)
    public void testNullPool() {
        ContainsValidEndPointCheck.forProxy(null, _name);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullServiceName() {
        ContainsValidEndPointCheck.forProxy(_proxy, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyServiceName() {
        ContainsValidEndPointCheck.forProxy(_proxy, "");
    }

    @Test
    public void testEmptyResult() {
        when(_pool.getNumValidEndPoints()).thenReturn(0);
        when(_pool.getNumBadEndPoints()).thenReturn(0);
        HealthCheck check = ContainsValidEndPointCheck.forProxy(_proxy, _name);

        assertFalse(check.execute().isHealthy());
    }

    @Test
    public void testOnlyUnhealthyResult() {
        when(_pool.getNumValidEndPoints()).thenReturn(0);
        when(_pool.getNumBadEndPoints()).thenReturn(2);

        HealthCheck check = ContainsValidEndPointCheck.forProxy(_proxy, _name);

        assertFalse(check.execute().isHealthy());
    }

    @Test
    public void testOnlyHealthyResult() {
        when(_pool.getNumValidEndPoints()).thenReturn(2);
        when(_pool.getNumBadEndPoints()).thenReturn(0);

        HealthCheck check = ContainsValidEndPointCheck.forProxy(_proxy, _name);

        assertTrue(check.execute().isHealthy());
    }

    @Test
    public void testBothResults() {
        when(_pool.getNumValidEndPoints()).thenReturn(1);
        when(_pool.getNumBadEndPoints()).thenReturn(1);

        HealthCheck check = ContainsValidEndPointCheck.forProxy(_proxy, _name);

        assertTrue(check.execute().isHealthy());
    }

    interface Service {}
}
