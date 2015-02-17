package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.ServiceEndPointBuilder;
import com.bazaarvoice.ostrich.ServiceFactory;
import com.bazaarvoice.ostrich.perftest.core.Result;
import com.bazaarvoice.ostrich.perftest.core.ResultFactory;
import com.bazaarvoice.ostrich.perftest.core.Service;
import com.bazaarvoice.ostrich.perftest.utils.HashFunction;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This needs to be in the com.bazaarvoice.ostrich.pool package so that it can have direct access to ServiceCache
 *
 * This instantiates a service cache, creates threads to run test on and exposes various metrics for monitoring
 */
@SuppressWarnings ("deprecation")
public class ServiceRunner {

    private final int _threadSize;
    private final int _workSize;
    private final ServiceCache<Service<String, String>> _serviceCache;
    private final ResultFactory<String> _resultFactory;

    private final Meter _serviceMeter;
    private final Timer _checkoutTimer;
    private final Timer _checkinTimer;
    private final Timer _totalExecTimer;

    /**
     * private constructor to prohibit instantiation
     * @param builder for the service runner
     */
    private ServiceRunner(Builder builder) {
        _workSize = builder._workSize;
        _threadSize = builder._threadSize;

        _resultFactory = builder._resultFactory;

        ServiceCachingPolicy _cachingPolicy = new ServiceCachingPolicyBuilder()
                .withCacheExhaustionAction(builder._exhaustionAction)
                .withMaxNumServiceInstancesPerEndPoint(builder._maxServiceInstances)
                .withMaxServiceInstanceIdleTime(builder._maxServiceIdleTimeSeconds, TimeUnit.SECONDS)
                .build();

        _serviceCache = new ServiceCache<>(_cachingPolicy, builder._serviceFactory, new MetricRegistry());

        _serviceMeter = Metrics.newMeter(this.getClass(), "Succeeded", "succeeded", TimeUnit.SECONDS);
        _checkoutTimer = Metrics.newTimer(this.getClass(), "Checkout");
        _checkinTimer = Metrics.newTimer(this.getClass(), "Checkin");
        _totalExecTimer = Metrics.newTimer(this.getClass(), "Total");
    }

    public Meter getServiceMeter() {
        return _serviceMeter;
    }

    public Timer getCheckoutTimer() {
        return _checkoutTimer;
    }

    public Timer getCheckinTimer() {
        return _checkinTimer;
    }

    public Timer getTotalExecTimer() {
        return _totalExecTimer;
    }

    public List<Thread> generateWorkers() {

        ImmutableList.Builder<Thread> threadListBuilder = ImmutableList.builder();

        // generate workers
        for (int i = 0; i < _threadSize; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while (!Thread.interrupted()) {
                        String work = RandomStringUtils.random(_workSize);
                        HashFunction hashFunction = HashFunction.getRandomHashFunction();
                        ServiceEndPoint serviceEndPoint = buildServiceEndPoint(hashFunction, work);
                        serviceExecution(serviceEndPoint);
                    }
                }
            };
            Thread thread = new Thread(runnable);
            threadListBuilder.add(thread);
        }
        return threadListBuilder.build();
    }


    /**
     * private helpers
     */

    private Result<String> serviceExecution(ServiceEndPoint serviceEndPoint) {
        TimerContext totalTimeContext = _totalExecTimer.time();
        try {
            TimerContext checkoutTimeContext = _checkoutTimer.time();
            ServiceHandle<Service<String, String>> serviceHandle = _serviceCache.checkOut(serviceEndPoint);
            Service<String, String> service = serviceHandle.getService();
            checkoutTimeContext.stop();

            String work = serviceEndPoint.getPayload();
            String result = service.process(work);

            TimerContext checkinTimeContext = _checkinTimer.time();
            _serviceCache.checkIn(serviceHandle);
            checkinTimeContext.stop();

            _serviceMeter.mark();
            totalTimeContext.stop();

            return _resultFactory.createResponse(result);
        } catch (Exception exception) {
            _serviceMeter.mark();
            totalTimeContext.stop();

            return _resultFactory.createResponse(exception);
        }
    }


    /**
     * static helpers
     */

    /**
     * Creates a service endpoint to hash a string with a given hash function
     *
     * @param hashFunction to delegate the work
     * @param payload the string to hash
     * @return an appropriate service endpoint for the job
     */
    private static ServiceEndPoint buildServiceEndPoint(HashFunction hashFunction, String payload) {
        return new ServiceEndPointBuilder()
                .withServiceName(hashFunction.name())
                .withId(hashFunction.name())
                .withPayload(payload)
                .build();
    }

    /**
     * Builder
     */

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Integer _workSize;
        private Integer _threadSize;
        private ServiceCachingPolicy.ExhaustionAction _exhaustionAction;
        private Integer _maxServiceInstances;
        private Integer _maxServiceIdleTimeSeconds;
        private ResultFactory<String> _resultFactory;
        private ServiceFactory<Service<String, String>> _serviceFactory;

        private Builder() {
        }

        public Builder withWorkSize(int workSize) {
            this._workSize = workSize;
            return this;
        }

        public Builder withThreadSize(int threadSize) {
            this._threadSize = threadSize;
            return this;
        }

        public Builder withExhaustionAction(ServiceCachingPolicy.ExhaustionAction exhaustionAction) {
            this._exhaustionAction = exhaustionAction;
            return this;
        }

        public Builder withMaxServiceInstances(int maxServiceInstances) {
            this._maxServiceInstances = maxServiceInstances;
            return this;
        }

        public Builder withMaxServiceIdleTimeSeconds(int maxServiceIdleTimeSeconds) {
            this._maxServiceIdleTimeSeconds = maxServiceIdleTimeSeconds;
            return this;
        }

        public Builder withServiceFactory(ServiceFactory<Service<String, String>> serviceFactory) {
            this._serviceFactory = serviceFactory;
            return this;
        }

        public Builder withResultFactory(ResultFactory<String> resultFactory) {
            this._resultFactory = resultFactory;
            return this;
        }

        public ServiceRunner build() {
            Preconditions.checkNotNull(_workSize);
            Preconditions.checkNotNull(_threadSize);
            Preconditions.checkNotNull(_exhaustionAction);
            Preconditions.checkNotNull(_maxServiceInstances);
            Preconditions.checkNotNull(_maxServiceIdleTimeSeconds);
            Preconditions.checkNotNull(_serviceFactory);
            Preconditions.checkNotNull(_resultFactory);

            return new ServiceRunner(this);
        }
    }
}
