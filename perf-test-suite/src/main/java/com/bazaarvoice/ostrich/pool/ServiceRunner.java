package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.MultiThreadedServiceFactory;
import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.exceptions.NoCachedInstancesAvailableException;
import com.bazaarvoice.ostrich.metrics.Metrics;
import com.bazaarvoice.ostrich.perftest.core.Result;
import com.bazaarvoice.ostrich.perftest.core.ResultFactory;
import com.bazaarvoice.ostrich.perftest.core.Service;
import com.bazaarvoice.ostrich.perftest.core.SimpleResultFactory;
import com.bazaarvoice.ostrich.perftest.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.utils.HashFunction;
import com.bazaarvoice.ostrich.perftest.utils.Utilities;
import com.google.common.collect.ImmutableList;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
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
    private final Meter _cacheMissMeter;
    private final Meter _failureMeter;
    private final Timer _checkoutTimer;
    private final Timer _checkinTimer;
    private final Timer _totalExecTimer;

    /**
     * @param serviceFactory the service factory
     * @param arguments      the command line arguments
     */
    public ServiceRunner(MultiThreadedServiceFactory<Service<String, String>> serviceFactory, Arguments arguments) {
        String serviceName = "ServiceRunner";
        _workSize = arguments.getWorkSize();
        _threadSize = arguments.getThreadSize();
        _resultFactory = SimpleResultFactory.newInstance();

        ServiceCachingPolicy cachingPolicy;
        if(arguments.isRunSingletonMode()) {
            cachingPolicy = ServiceCachingPolicyBuilder.getMultiThreadedClientPolicy();
        }
        else {
            cachingPolicy = new ServiceCachingPolicyBuilder()
                    .withCacheExhaustionAction(arguments.getExhaustionAction())
                    .withMaxNumServiceInstancesPerEndPoint(arguments.getMaxInstance())
                    .withMaxServiceInstanceIdleTime(arguments.getIdleTimeSecond(), TimeUnit.SECONDS)
                    .build();
        }

        _serviceCache = new ServiceCacheBuilder<Service<String, String>>()
                .withServiceFactory(serviceFactory)
                .withCachingPolicy(cachingPolicy).build();

        Metrics _metrics = Metrics.forClass(this.getClass());
        _serviceMeter = _metrics.newMeter(serviceName, "Service-Executed", "Service-Executed", TimeUnit.SECONDS);
        _cacheMissMeter = _metrics.newMeter(serviceName, "Cache-Miss", "Cache-Miss", TimeUnit.SECONDS);
        _failureMeter = _metrics.newMeter(serviceName, "Service-Failure", "Service-Failure", TimeUnit.SECONDS);
        _checkoutTimer = _metrics.newTimer(serviceName, "Checkout", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        _checkinTimer = _metrics.newTimer(serviceName, "Checkin", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        _totalExecTimer = _metrics.newTimer(serviceName, "Total-Exec", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    }

    public ServiceCache<Service<String, String>> getServiceCache() {
        return _serviceCache;
    }

    /**
     * Generates worker threads that will make requests of the ServiceCache.
     * Each worker thread will request a "Client" from the ServiceCache, and
     * when it has a client will do some "busywork". The "busywork" is running
     * some cryptographic hashes on a random string.
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
                        String hashName = HashFunction.getRandomHashName();
                        ServiceEndPoint serviceEndPoint = Utilities.buildServiceEndPoint(hashName);
                        serviceExecution(serviceEndPoint, work);
                    }
                }
            };
            Thread thread = new Thread(runnable);
            threadListBuilder.add(thread);
        }
        return threadListBuilder.build();
    }

    private Result<String> serviceExecution(ServiceEndPoint serviceEndPoint, String work) {
        TimerContext totalTimeContext = _totalExecTimer.time();
        try {
            TimerContext checkoutTimeContext = _checkoutTimer.time();
            ServiceHandle<Service<String, String>> serviceHandle = _serviceCache.checkOut(serviceEndPoint);
            Service<String, String> service = serviceHandle.getService();
            checkoutTimeContext.stop();

            String result = service.process(work);

            TimerContext checkinTimeContext = _checkinTimer.time();
            _serviceCache.checkIn(serviceHandle);
            checkinTimeContext.stop();
            _serviceMeter.mark();

            return _resultFactory.createResponse(result);
        } catch (NoCachedInstancesAvailableException exception) {
            _cacheMissMeter.mark();
            return _resultFactory.createResponse(exception);
        } catch (Exception exception) {
            exception.printStackTrace();
            _failureMeter.mark();
            return _resultFactory.createResponse(exception);
        } finally {
            totalTimeContext.stop();
        }
    }
}
