package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.ServiceFactory;
import com.bazaarvoice.ostrich.metrics.notimplemented.NotImplementedMetrics;
import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.ScheduledExecutorService;

import static com.bazaarvoice.ostrich.pool.ServiceCacheBuilder.buildDefaultExecutor;

public class NoOpMetricsSingleThreadedClientServiceCache<S> extends CoreSingleThreadedClientServiceCache<S> {

    /**
     * Builds a basic service cache.
     *
     * @param policy         The configuration for this cache.
     * @param serviceFactory The factory to fall back to on cache misses.
     */
    NoOpMetricsSingleThreadedClientServiceCache(ServiceCachingPolicy policy, ServiceFactory<S> serviceFactory) {
        this(policy, serviceFactory, buildDefaultExecutor());
    }

    /**
     * Builds a basic service cache.
     *
     * @param policy         The configuration for this cache.
     * @param serviceFactory The factory to fall back to on cache misses.
     * @param executor       The executor to use for checking for idle instances to evict.
     */
    @VisibleForTesting NoOpMetricsSingleThreadedClientServiceCache(ServiceCachingPolicy policy, ServiceFactory<S> serviceFactory, ScheduledExecutorService executor) {
        super(policy, serviceFactory, executor, NotImplementedMetrics.forClass(SingleThreadedClientServiceCache.class));
    }
}
