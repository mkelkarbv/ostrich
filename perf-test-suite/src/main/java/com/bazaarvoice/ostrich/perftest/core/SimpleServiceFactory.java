package com.bazaarvoice.ostrich.perftest.core;

import com.bazaarvoice.ostrich.MultiThreadedServiceFactory;
import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.metrics.Metrics;
import com.bazaarvoice.ostrich.perftest.utils.HashFunction;
import com.bazaarvoice.ostrich.pool.ServicePoolBuilder;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

import java.util.concurrent.TimeUnit;


/**
 * A service factory, as needed in the ServiceCache
 */
public class SimpleServiceFactory implements MultiThreadedServiceFactory<Service<String, String>> {

    private final Meter _serviceCreated;
    private final Meter _serviceDestroyed;
    private final Timer _serviceTimer;

    /**
     * private constructor
     */
    private SimpleServiceFactory() {
        String serviceName = this.getServiceName();
        Metrics metrics = Metrics.forClass(this.getClass());
        _serviceCreated = metrics.newMeter(serviceName, "Service-Created", "Service-Created", TimeUnit.SECONDS);
        _serviceDestroyed = metrics.newMeter(serviceName, "Service-Destroyed", "Service-Destroyed", TimeUnit.SECONDS);
        _serviceTimer = metrics.newTimer(serviceName, "Service-Time", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    }

    /**
     * Static instantiator to get a handle of a service factory
     *
     * @return a simple service factory
     */
    public static SimpleServiceFactory newInstance() {
        return new SimpleServiceFactory();
    }

    @Override
    public String getServiceName() {
        return "SimpleService";
    }

    @Override
    public void configure(ServicePoolBuilder<Service<String, String>> servicePoolBuilder) {
    }

    /**
     * Creates a service for a given service endpoint
     *
     * @param serviceEndPoint created with id of a random HashFunction
     * @return a service instance
     */
    @Override
    public Service<String, String> create(final ServiceEndPoint serviceEndPoint) {
        final HashFunction hashFunction = HashFunction.valueOf(serviceEndPoint.getId());
        final Service<String, String> service = createService(hashFunction);
        service.initialize();
        return service;
    }

    @Override
    public void destroy(ServiceEndPoint serviceEndPoint, Service<String, String> service) {
        service.destroy();
    }

    @Override
    public boolean isHealthy(ServiceEndPoint endPoint) {
        throw new RuntimeException("isHealthy() should not get executed as part the performance test suite");
    }

    @Override
    public boolean isRetriableException(Exception exception) {
        throw new RuntimeException("isRetriableException() should not get executed as part the performance test suite");
    }

    /**
     * Create a simple service wrapper as and when required by Service Cache
     *
     * @param hashFunction the hash function to use in the service
     * @return a service wrapping the hash function
     */
    private Service<String, String> createService(final HashFunction hashFunction) {

        return new Service<String, String>() {

            @Override
            public String process(String work) {
                TimerContext serviceTime = _serviceTimer.time();
                String result = hashFunction.process(work);
                serviceTime.stop();
                return result;
            }

            @Override
            public void initialize() {
                _serviceCreated.mark();
            }

            @Override
            public void destroy() {
                _serviceDestroyed.mark();
            }
        };
    }
}
