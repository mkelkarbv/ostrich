package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.MultiThreadedServiceFactory;
import com.bazaarvoice.ostrich.metrics.yammer.YammerMetrics;
import com.bazaarvoice.ostrich.spi.ScopedMetrics;

import java.util.concurrent.ScheduledExecutorService;

/**
 * A ServiceCache for "heavy weight" client instances, i.e. ones that are already thread safe.
 * Therefore unlike {@link com.bazaarvoice.ostrich.pool.SingleThreadedClientServiceCache}, we
 * can just map EndPoints to a single shared instance of a "heavy weight" client.
 * <p/>
 * This applies to third party client libraries for connecting to generic or specialized
 * services, i.e. {@code HttpClient}, {@code JestClient}, {@code ElasticSearchClient} etc.
 * <p/>
 * If your client library is multi-thread safe, this ServiceCache should provide better
 * performance than the {@link com.bazaarvoice.ostrich.pool.SingleThreadedClientServiceCache}.
 *
 * @param <S> the Service type
 */
class MultiThreadedClientServiceCache<S> extends CoreMultiThreadedClientServiceCache<S> {

    public MultiThreadedClientServiceCache(final MultiThreadedServiceFactory<S> serviceFactory) {

        super(serviceFactory,
            new ScopedMetrics(MultiThreadedClientServiceCache.class.getCanonicalName(), new YammerMetrics(com.yammer.metrics.Metrics.defaultRegistry())));
    }

    public MultiThreadedClientServiceCache(final MultiThreadedServiceFactory<S> serviceFactory,
                                           final ScheduledExecutorService executor,
                                           final int evictionDelayInSeconds,
                                           final int cleanUpDelayInSeconds) {

        super(serviceFactory, executor, evictionDelayInSeconds, cleanUpDelayInSeconds,
            new ScopedMetrics(MultiThreadedClientServiceCache.class.getCanonicalName(), new YammerMetrics(com.yammer.metrics.Metrics.defaultRegistry())));

    }

}
