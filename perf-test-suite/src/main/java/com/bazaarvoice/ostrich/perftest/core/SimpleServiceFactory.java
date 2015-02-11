package com.bazaarvoice.ostrich.perftest.core;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.ServiceFactory;
import com.bazaarvoice.ostrich.perftest.utils.HashFunction;
import com.bazaarvoice.ostrich.pool.ServicePoolBuilder;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

import java.util.concurrent.TimeUnit;

public class SimpleServiceFactory implements ServiceFactory<Service<String, String>> {

    private final Meter serviceCreated;
    private final Meter serviceDestroyed;
    private final Timer serviceTimer;


    private SimpleServiceFactory() {
        serviceCreated = Metrics.newMeter(this.getClass(), "Created", "created", TimeUnit.SECONDS);
        serviceDestroyed = Metrics.newMeter(this.getClass(), "Destroyed", "destroyed", TimeUnit.SECONDS);
        serviceTimer = Metrics.newTimer(this.getClass(), "Timer");
    }

    public static SimpleServiceFactory newInstance() {
        return new SimpleServiceFactory();
    }

    @Override
    public String getServiceName() {
        return "SimpleService";
    }

    @Override
    public void configure(ServicePoolBuilder<Service<String, String>> servicePoolBuilder) { }

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
        throw new RuntimeException("This should not get executed");
    }

    @Override
    public boolean isRetriableException(Exception exception) {
        throw new RuntimeException("This should not get executed");
    }

    public Meter getServiceCreated() {
        return serviceCreated;
    }

    public Meter getServiceDestroyed() {
        return serviceDestroyed;
    }

    public Timer getServiceTimer() {
        return serviceTimer;
    }

    private Service<String, String> createService(final HashFunction hashFunction) {

        return new Service<String, String>() {

            @Override
            public String process(String work) {
                TimerContext serviceTime = serviceTimer.time();
                String result = hashFunction.process(work);
                serviceTime.stop();
                return result;
            }

            @Override
            public void initialize() {
                serviceCreated.mark();
            }

            @Override
            public void destroy() {
                serviceDestroyed.mark();
            }
        };
    }
}
