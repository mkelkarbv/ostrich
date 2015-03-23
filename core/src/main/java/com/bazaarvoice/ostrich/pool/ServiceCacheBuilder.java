package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.MultiThreadedServiceFactory;
import com.bazaarvoice.ostrich.ServiceFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ServiceCacheBuilder<S> {

    private ServiceCachingPolicy _cachingPolicy;
    private ServiceFactory<S> _serviceFactory;

    public ServiceCacheBuilder<S> withCachingPolicy(ServiceCachingPolicy cachingPolicy) {
        _cachingPolicy = cachingPolicy;
        return this;
    }

    public ServiceCacheBuilder<S> withServiceFactory(ServiceFactory<S> serviceFactory) {
        _serviceFactory = serviceFactory;
        return this;
    }

    public ServiceCache<S> build() {
        checkNotNull(_cachingPolicy, "cachingPolicy");
        if (_cachingPolicy.useMultiThreadedClientPolicy()) {
            checkNotNull(_serviceFactory, "serviceFactory");
            checkArgument((_serviceFactory instanceof MultiThreadedServiceFactory), "Please implement MultiThreadedServiceFactory to construct MultiThreadedClientServiceCache");
            return new MultiThreadedClientServiceCache<S>((MultiThreadedServiceFactory<S>) _serviceFactory);
        }
        else {
            checkNotNull(_serviceFactory, "serviceFactory");
            return new SingleThreadedClientServiceCache<S>(_cachingPolicy, _serviceFactory);
        }
    }

    /**
     * This ensures the {@link java.util.concurrent.ScheduledExecutorService} in not loaded onto jvm
     * until the class is loaded by explicitly calling the constructor.
     */
    public static ScheduledExecutorService buildDefaultExecutor() {
        return Executors.newScheduledThreadPool(1,
                new ThreadFactoryBuilder().setNameFormat("ServiceCache-CleanupThread-%d").setDaemon(true).build());
    }
}
