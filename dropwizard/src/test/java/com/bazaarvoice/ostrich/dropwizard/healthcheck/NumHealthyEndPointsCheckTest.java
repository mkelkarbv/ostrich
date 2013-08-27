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
    private final String name = "test";
    @SuppressWarnings("unchecked") private final ServicePool<Service> pool = mock(ServicePool.class);
    InvocationHandler invocationHandler = mock(InvocationHandler.class);
    private final Object proxy = Proxy.newProxyInstance(Service.class.getClassLoader(), new Class[] {Service.class}, invocationHandler);

    @Test (expected = NullPointerException.class)
    public void testNullPool() {
        new NumHealthyEndPointsCheck(null, name);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullServiceName() {
        new NumHealthyEndPointsCheck(pool, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyServiceName() {
        new NumHealthyEndPointsCheck(pool, "");
    }

    //todo - how to test this
    @Test
    public void testPoolProxy() {
        new NumHealthyEndPointsCheck(proxy, name);
    }

    @Test
    public void testEmptyResult() {
        when(pool.getNumValidEndPoints()).thenReturn(0);
        when(pool.getNumBadEndPoints()).thenReturn(0);
        HealthCheck check = new NumHealthyEndPointsCheck(pool, name);

        assertFalse(check.execute().isHealthy());
    }

    @Test
    public void testOnlyUnhealthyResult() {
        when(pool.getNumValidEndPoints()).thenReturn(0);
        when(pool.getNumBadEndPoints()).thenReturn(2);

        HealthCheck check = new NumHealthyEndPointsCheck(pool, name);

        assertFalse(check.execute().isHealthy());
    }

    @Test
    public void testOnlyHealthyResult() {
        when(pool.getNumValidEndPoints()).thenReturn(2);
        when(pool.getNumBadEndPoints()).thenReturn(0);

        HealthCheck check = new NumHealthyEndPointsCheck(pool, name);

        assertTrue(check.execute().isHealthy());
    }

    @Test
    public void testBothResults() {
        when(pool.getNumValidEndPoints()).thenReturn(1);
        when(pool.getNumBadEndPoints()).thenReturn(1);

        HealthCheck check = new NumHealthyEndPointsCheck(pool, name);

        assertTrue(check.execute().isHealthy());
    }

    interface Service {}
}
