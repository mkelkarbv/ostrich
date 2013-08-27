package com.bazaarvoice.ostrich.dropwizard.healthcheck;

import com.yammer.metrics.core.HealthCheck;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachedHealthCheckTest {
    @Test
    public void testCheckHealthy() {
        HealthCheck healthyCheck = Mockito.mock(HealthCheck.class);
        when(healthyCheck.execute()).thenReturn(HealthCheck.Result.healthy());
        CachedHealthCheck cachedCheck = new CachedHealthCheck(healthyCheck);

        // Execute check twice, should only call inner check once
        HealthCheck.Result check = cachedCheck.execute();
        assertTrue(check.isHealthy());
        check = cachedCheck.execute();
        assertTrue(check.isHealthy());

        verify(healthyCheck).execute();
    }

    @Test
    public void testCheckUnhealthy() {
        HealthCheck unhealthyCheck = Mockito.mock(HealthCheck.class);
        when(unhealthyCheck.execute()).thenReturn(HealthCheck.Result.unhealthy("test"));
        CachedHealthCheck cachedCheck = new CachedHealthCheck(unhealthyCheck);

        // Execute check twice, should call inner check twice since it's unhealthy
        HealthCheck.Result check = cachedCheck.execute();
        assertFalse(check.isHealthy());
        check = cachedCheck.execute();
        assertFalse(check.isHealthy());

        verify(unhealthyCheck).execute();
    }

    interface Service {}
}
