package com.bazaarvoice.ostrich.dropwizard.healthcheck;

import com.bazaarvoice.ostrich.ServicePool;
import com.yammer.metrics.core.HealthCheck;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContainsValidEndPointCheckTest {
    private final String _name = "test";
    @SuppressWarnings("unchecked") private final ServicePool<Service> _pool = mock(ServicePool.class);

    @Test (expected = NullPointerException.class)
    public void testNullPool() {
        new ContainsValidEndPointCheck(null, _name);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullServiceName() {
        new ContainsValidEndPointCheck(_pool, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyServiceName() {
        new ContainsValidEndPointCheck(_pool, "");
    }

    @Test
    public void testEmptyResult() {
        when(_pool.getNumValidEndPoints()).thenReturn(0);
        when(_pool.getNumBadEndPoints()).thenReturn(0);
        HealthCheck check = new ContainsValidEndPointCheck(_pool, _name);

        assertFalse(check.execute().isHealthy());
    }

    @Test
    public void testOnlyUnhealthyResult() {
        when(_pool.getNumValidEndPoints()).thenReturn(0);
        when(_pool.getNumBadEndPoints()).thenReturn(2);

        HealthCheck check = new ContainsValidEndPointCheck(_pool, _name);

        assertFalse(check.execute().isHealthy());
    }

    @Test
    public void testOnlyHealthyResult() {
        when(_pool.getNumValidEndPoints()).thenReturn(2);
        when(_pool.getNumBadEndPoints()).thenReturn(0);

        HealthCheck check = new ContainsValidEndPointCheck(_pool, _name);

        assertTrue(check.execute().isHealthy());
    }

    @Test
    public void testBothResults() {
        when(_pool.getNumValidEndPoints()).thenReturn(1);
        when(_pool.getNumBadEndPoints()).thenReturn(1);

        HealthCheck check = new ContainsValidEndPointCheck(_pool, _name);

        assertTrue(check.execute().isHealthy());
    }

    interface Service {}
}
