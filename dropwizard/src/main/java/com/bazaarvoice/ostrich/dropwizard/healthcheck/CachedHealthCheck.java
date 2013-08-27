package com.bazaarvoice.ostrich.dropwizard.healthcheck;

import com.yammer.metrics.core.HealthCheck;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Random;

public class CachedHealthCheck extends HealthCheck {
    private static Random RANDOM = new Random();

    private HealthCheck _healthCheck;
    private DateTime _nextCheckTime;
    private Result _lastResult;

    public CachedHealthCheck(HealthCheck healthCheck) {
        super(healthCheck.getName());
        _healthCheck = healthCheck;
        _nextCheckTime = DateTime.now();
    }

    @Override
    public Result check() throws Exception {
        DateTime now = DateTime.now();
        if(_lastResult == null || now.isAfter(_nextCheckTime)){
            _lastResult = _healthCheck.execute();
            // If we're healthy, back off for a random amount of time (30-60 seconds) so that we don't overwhelm or swarm the dependency
            _nextCheckTime = now.plusSeconds(30 + RANDOM.nextInt(30));
        }

        Seconds seconds = Seconds.secondsBetween(now, _nextCheckTime);
        String resultMessage = _lastResult.getMessage();
        String cachedMessage = " [cached - refresh " + seconds.getSeconds() +  "s]";
        if(resultMessage == null) {
            resultMessage = "";
        }
        String message = resultMessage + cachedMessage;

        if(_lastResult.isHealthy()) {
            return Result.healthy(message);
        } else {
            return Result.unhealthy(message);
        }
    }
}
