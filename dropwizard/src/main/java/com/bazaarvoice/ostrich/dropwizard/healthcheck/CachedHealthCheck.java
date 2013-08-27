package com.bazaarvoice.ostrich.dropwizard.healthcheck;

import com.yammer.metrics.core.HealthCheck;
import org.joda.time.DateTime;

import java.util.Random;

public class CachedHealthCheck extends HealthCheck {
    private HealthCheck healthCheck;
    private static Random RANDOM = new Random();
    private DateTime nextCheckTime;
    private Result lastResult;

    public CachedHealthCheck(HealthCheck healthCheck) {
        super(healthCheck.getName());
        this.healthCheck = healthCheck;
        nextCheckTime = DateTime.now();
    }

    @Override
    public Result check() throws Exception {
        DateTime now = DateTime.now();
        if(lastResult == null || !lastResult.isHealthy() || now.isAfter(nextCheckTime)){
            lastResult = healthCheck.execute();
            // If we're healthy, back off for a random amount of time (30-60 seconds) so that we don't overwhelm or swarm the dependency
            nextCheckTime = now.plusSeconds(30 + RANDOM.nextInt(30));
        }
        return lastResult;
    }
}
