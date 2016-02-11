package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.ServiceFactory;
import com.bazaarvoice.ostrich.metrics.yammer.YammerMetrics;
import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.ScheduledExecutorService;

import static com.bazaarvoice.ostrich.pool.ServiceCacheBuilder.buildDefaultExecutor;

/**
 * A cache for service instances. Useful if there's more than insignificant overhead in creating service connections
 * from a {@link com.bazaarvoice.ostrich.ServiceEndPoint}.  Will spawn one thread (shared by all
 * {@link com.bazaarvoice.ostrich.pool.ServiceCache}s) to handle evictions of
 */
class SingleThreadedClientServiceCache<S> extends CoreSingleThreadedClientServiceCache {

    /**
     * Builds a basic service cache.
     *
     * @param policy         The configuration for this cache.
     * @param serviceFactory The factory to fall back to on cache misses.
     */
    SingleThreadedClientServiceCache(ServiceCachingPolicy policy, ServiceFactory<S> serviceFactory) {
        this(policy, serviceFactory, buildDefaultExecutor());
    }

    /**
     * Builds a basic service cache.
     *
     * @param policy         The configuration for this cache.
     * @param serviceFactory The factory to fall back to on cache misses.
     * @param executor       The executor to use for checking for idle instances to evict.
     */
    @VisibleForTesting SingleThreadedClientServiceCache(ServiceCachingPolicy policy, ServiceFactory<S> serviceFactory, ScheduledExecutorService executor) {
        super(policy, serviceFactory, executor, YammerMetrics.forClass(SingleThreadedClientServiceCache.class));
        _metrics.addInstance(this, serviceFactory.getServiceName());
    }

}
