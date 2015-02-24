package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.ServiceEndPointBuilder;
import com.bazaarvoice.ostrich.ServiceFactory;
import com.bazaarvoice.ostrich.metrics.Metrics;
import com.bazaarvoice.ostrich.perftest.core.Result;
import com.bazaarvoice.ostrich.perftest.core.ResultFactory;
import com.bazaarvoice.ostrich.perftest.core.Service;
import com.bazaarvoice.ostrich.perftest.core.SimpleResultFactory;
import com.bazaarvoice.ostrich.perftest.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.utils.HashFunction;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This needs to be in the com.bazaarvoice.ostrich.pool package so that it can have direct access to ServiceCache
 * <p/>
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
     * @param serviceFactory the service factory
     * @param arguments      the command line arguments
     */
    public ServiceRunner(ServiceFactory<Service<String, String>> serviceFactory, MetricRegistry metricRegistry, Arguments arguments) {
        _workSize = arguments.getWorkSize();
        _threadSize = arguments.getThreadSize();
        _resultFactory = SimpleResultFactory.newInstance();

        ServiceCachingPolicy _cachingPolicy = new ServiceCachingPolicyBuilder()
                .withCacheExhaustionAction(arguments.getExhaustionAction())
                .withMaxNumServiceInstancesPerEndPoint(arguments.getMaxInstance())
                .withMaxServiceInstanceIdleTime(arguments.getIdleTimeSecond(), TimeUnit.SECONDS)
                .build();
        _serviceCache = new ServiceCache<>(_cachingPolicy, serviceFactory, new MetricRegistry());

        Metrics.InstanceMetrics _metrics = Metrics.forInstance(metricRegistry, this, "ServiceRunner");
        _serviceMeter = _metrics.meter("Executed");
        _checkoutTimer = _metrics.timer("Checkout");
        _checkinTimer = _metrics.timer("Checkin");
        _totalExecTimer = _metrics.timer("Total");
    }

    /**
     * Creates a service endpoint to hash a string with a given hash function
     *
     * @param hashFunction to delegate the work
     * @param payload      the string to hash
     * @return an appropriate service endpoint for the job
     */
    private static ServiceEndPoint buildServiceEndPoint(HashFunction hashFunction, String payload) {
        return new ServiceEndPointBuilder()
                .withServiceName(hashFunction.name())
                .withId(hashFunction.name())
                .withPayload(payload)
                .build();
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

    /**
     * Generates worker threads that will make requests of the ServiceCache.
     * Each worker thread will request a "Client" from the ServiceCache, and
     * when it has a client will do some "busywork". The "busywork" will be
     * to run some cryptographic hashes across a random string.
     * The size of the random String to be hashed can be configured from the command line.
     */
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

    private Result<String> serviceExecution(ServiceEndPoint serviceEndPoint) {
        Timer.Context totalTimeContext = _totalExecTimer.time();
        try {
            Timer.Context checkoutTimeContext = _checkoutTimer.time();
            ServiceHandle<Service<String, String>> serviceHandle = _serviceCache.checkOut(serviceEndPoint);
            Service<String, String> service = serviceHandle.getService();
            checkoutTimeContext.stop();

            String work = serviceEndPoint.getPayload();
            String result = service.process(work);

            Timer.Context checkinTimeContext = _checkinTimer.time();
            _serviceCache.checkIn(serviceHandle);
            checkinTimeContext.stop();

            return _resultFactory.createResponse(result);
        } catch (Exception exception) {
            return _resultFactory.createResponse(exception);
        } finally {
            _serviceMeter.mark();
            totalTimeContext.stop();
        }
    }
}
