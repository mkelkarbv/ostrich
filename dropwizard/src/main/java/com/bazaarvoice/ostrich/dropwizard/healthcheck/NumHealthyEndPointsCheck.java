package com.bazaarvoice.ostrich.dropwizard.healthcheck;

import com.bazaarvoice.ostrich.ServicePool;
import com.bazaarvoice.ostrich.pool.ServicePoolProxies;
import com.google.common.base.Strings;
import com.yammer.metrics.core.HealthCheck;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class NumHealthyEndPointsCheck extends HealthCheck {
    private ServicePool<?> _pool;

    public NumHealthyEndPointsCheck(ServicePool<?> pool, String name) {
        super(name);
        checkArgument(!Strings.isNullOrEmpty(name));
        _pool = checkNotNull(pool);
    }

    public NumHealthyEndPointsCheck(Object proxy, String name) {
        this(ServicePoolProxies.getPool(proxy), name);
    }

    @Override
    protected Result check()
            throws Exception {
        int numValidEndPoints = _pool.getNumValidEndPoints();
        int numBadEndPoints = _pool.getNumBadEndPoints();

        if(numValidEndPoints == 0 && numBadEndPoints == 0) {
            return Result.unhealthy("No end points.");
        }

        String unhealthyMessage = numBadEndPoints + " unhealthy instances";
        if(numValidEndPoints == 0 && numBadEndPoints > 0) {
            return Result.unhealthy(unhealthyMessage);
        }

        return Result.healthy(numValidEndPoints + " healthy instances; " + unhealthyMessage);
    }
}
