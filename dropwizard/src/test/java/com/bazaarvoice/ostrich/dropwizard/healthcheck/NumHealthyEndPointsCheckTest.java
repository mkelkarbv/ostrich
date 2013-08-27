package com.bazaarvoice.ostrich.dropwizard.healthcheck;

import com.bazaarvoice.ostrich.ServicePool;
import com.yammer.metrics.core.HealthCheck;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NumHealthyEndPointsCheckTest {
    private final String _name = "test";
    @SuppressWarnings("unchecked") private final ServicePool<Service> _pool = mock(ServicePool.class);
    private final InvocationHandler _invocationHandler = mock(InvocationHandler.class);
    private final Service _proxy = (Service)Proxy.newProxyInstance(Service.class.getClassLoader(), new Class[] {Service.class}, _invocationHandler);

    @Test (expected = NullPointerException.class)
    public void testNullPool() {
        new NumHealthyEndPointsCheck(null, _name);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullServiceName() {
        new NumHealthyEndPointsCheck(_pool, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyServiceName() {
        new NumHealthyEndPointsCheck(_pool, "");
    }

    //todo - how to test this
    @Test
    public void testPoolProxy() {
        new NumHealthyEndPointsCheck(_proxy, _name);
    }

    @Test
    public void testEmptyResult() {
        when(_pool.getNumValidEndPoints()).thenReturn(0);
        when(_pool.getNumBadEndPoints()).thenReturn(0);
        HealthCheck check = new NumHealthyEndPointsCheck(_pool, _name);

        assertFalse(check.execute().isHealthy());
    }

    @Test
    public void testOnlyUnhealthyResult() {
        when(_pool.getNumValidEndPoints()).thenReturn(0);
        when(_pool.getNumBadEndPoints()).thenReturn(2);

        HealthCheck check = new NumHealthyEndPointsCheck(_pool, _name);

        assertFalse(check.execute().isHealthy());
    }

    @Test
    public void testOnlyHealthyResult() {
        when(_pool.getNumValidEndPoints()).thenReturn(2);
        when(_pool.getNumBadEndPoints()).thenReturn(0);

        HealthCheck check = new NumHealthyEndPointsCheck(_pool, _name);

        assertTrue(check.execute().isHealthy());
    }

    @Test
    public void testBothResults() {
        when(_pool.getNumValidEndPoints()).thenReturn(1);
        when(_pool.getNumBadEndPoints()).thenReturn(1);

        HealthCheck check = new NumHealthyEndPointsCheck(_pool, _name);

        assertTrue(check.execute().isHealthy());
    }

    interface Service {}
}
