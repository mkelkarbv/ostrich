package com.bazaarvoice.ostrich.perftest.utils;

import com.bazaarvoice.ostrich.MultiThreadedServiceFactory;
import com.bazaarvoice.ostrich.perftest.core.Service;
import com.bazaarvoice.ostrich.perftest.core.SimpleServiceFactory;
import com.bazaarvoice.ostrich.pool.ServiceRunner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;

import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class creates the service runner and runs the requested load as parsed via Arguments
 * This also prints/writes the report log and/or the statistics as requested in the Arguments
 */
public class LoadRunner {

    private final PrintStream _out;
    private final boolean _doPrintStats;
    private final long _totalRuntime;
    private final List<Thread> _workers;
    private final long _startTime;
    private final int _reportingIntervalSeconds;
    private final Arguments _arguments;

    private final Meter _serviceCreated;
    private final Meter _serviceDestroyed;
    private final Meter _serviceCalled;
    private final Meter _cacheMissed;
    private final Meter _serviceFailed;
    private final Meter _chaosCreated;
    private final Meter _stableCreated;
    private final Timer _checkoutTimer;
    private final Timer _checkinTimer;
    private final Timer _serviceTimer;
    private final Timer _totalTimer;
    private final Timer _evictionTimer;
    private final Timer _registerTimer;
    private final Timer _loadTimer;

    private long _counter = 0;

    public LoadRunner(Arguments arguments) {

        MetricsRegistry metricRegistry = com.yammer.metrics.Metrics.defaultRegistry();
        MultiThreadedServiceFactory<Service<String, String>> serviceFactory = SimpleServiceFactory.newInstance();
        ServiceRunner serviceRunner = new ServiceRunner(serviceFactory, arguments);
        ChaosRunner chaosRunner = new ChaosRunner(serviceRunner.getServiceCache(), arguments);

        _arguments = arguments;
        _out = arguments.getOutput();
        _doPrintStats = arguments.doPrintStats();
        _totalRuntime = arguments.getRunTimeSecond();
        _reportingIntervalSeconds = arguments.getReportingIntervalSeconds();

        _workers = Lists.newArrayList();
        _workers.addAll(serviceRunner.generateWorkers());
        _workers.addAll(chaosRunner.generateChaosWorkers());

        for (Thread thread : _workers) {
            thread.start();
        }

        Map<String, MetricName> allMetrics = Maps.newHashMap();
        for(MetricName metricName: metricRegistry.allMetrics().keySet()) {
            allMetrics.put(metricName.getName(), metricName);
        }

        _startTime = currentTimeSeconds();

        _serviceCreated = metricRegistry.newMeter(allMetrics.get("Service-Created"), null, null);
        _serviceDestroyed = metricRegistry.newMeter(allMetrics.get("Service-Destroyed"), null, null);
        _serviceCalled = metricRegistry.newMeter(allMetrics.get("Service-Executed"), null, null);
        _cacheMissed = metricRegistry.newMeter(allMetrics.get("Cache-Miss"), null, null);
        _serviceFailed = metricRegistry.newMeter(allMetrics.get("Service-Failure"), null, null);
        _chaosCreated = metricRegistry.newMeter(allMetrics.get("Chaos"), null, null);
        _stableCreated = metricRegistry.newMeter(allMetrics.get("Stable"), null, null);
        _checkoutTimer = metricRegistry.newTimer(allMetrics.get("Checkout"), null, null);
        _checkinTimer = metricRegistry.newTimer(allMetrics.get("Checkin"), null, null);
        _serviceTimer = metricRegistry.newTimer(allMetrics.get("Service-Time"), null, null);
        _totalTimer = metricRegistry.newTimer(allMetrics.get("Total-Exec"), null, null);
        if(arguments.isRunSingletonMode()) {
            _evictionTimer = metricRegistry.newTimer(allMetrics.get("eviction-time"), null, null);
            _registerTimer = metricRegistry.newTimer(allMetrics.get("register-time"), null, null);
            _loadTimer = metricRegistry.newTimer(new MetricName(this.getClass(), "dummy"), TimeUnit.SECONDS, TimeUnit.SECONDS);
        }
        else {
            _loadTimer = metricRegistry.newTimer(allMetrics.get("load-time"), null, null);
            _evictionTimer = _registerTimer = metricRegistry.newTimer(new MetricName(this.getClass(), "dummy"), TimeUnit.SECONDS, TimeUnit.SECONDS);
        }
    }

    public void printLog() {

        long currentRuntime = currentTimeSeconds() - _startTime;

        _out.print(String.format("%d,", _counter++));

        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,",
                _serviceCreated.count(), _serviceCreated.meanRate(), _serviceCreated.oneMinuteRate(),
                _serviceCreated.fiveMinuteRate(), _serviceCreated.fifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,",
                _serviceDestroyed.count(), _serviceDestroyed.meanRate(), _serviceDestroyed.oneMinuteRate(),
                _serviceDestroyed.fiveMinuteRate(), _serviceDestroyed.fifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,",
                _serviceCalled.count(), _serviceCalled.meanRate(), _serviceCalled.oneMinuteRate(),
                _serviceCalled.fiveMinuteRate(), _serviceCalled.fifteenMinuteRate()));

        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                _checkoutTimer.count(), nsToMs(_checkoutTimer.min()),
                nsToMs(_checkoutTimer.max()), nsToMs(_checkoutTimer.mean()),
                _checkoutTimer.oneMinuteRate(), _checkoutTimer.fiveMinuteRate(), _checkoutTimer.fifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                _checkinTimer.count(), nsToMs(_checkinTimer.min()),
                nsToMs(_checkinTimer.max()), nsToMs(_checkinTimer.mean()),
                _checkinTimer.oneMinuteRate(), _checkinTimer.fiveMinuteRate(), _checkinTimer.fifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                _serviceTimer.count(), nsToMs(_serviceTimer.min()),
                nsToMs(_serviceTimer.max()), nsToMs(_serviceTimer.mean()),
                _serviceTimer.oneMinuteRate(), _serviceTimer.fiveMinuteRate(), _serviceTimer.fifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                _totalTimer.count(), nsToMs(_totalTimer.min()),
                nsToMs(_totalTimer.max()), nsToMs(_totalTimer.mean()),
                _totalTimer.oneMinuteRate(), _totalTimer.fiveMinuteRate(), _totalTimer.fifteenMinuteRate()));


        _out.println();
        _out.flush();

        if (_doPrintStats) {
            System.out.print("\u001b[2J");

            System.out.println(new Date());

            System.out.println(String.format("Running %d seconds of %s with threads: %d, work size: %d, idle time: %d, " +
                            "max instance: %d, exhaust action: %s, singleton-mode: %s, chaos-worker: %d, chaos-interval: %d",
                    currentRuntime, _arguments.getRunTimeSecond(), _arguments.getThreadSize(), _arguments.getWorkSize(),
                    _arguments.getIdleTimeSecond(), _arguments.getMaxInstance(), _arguments.getExhaustionAction().name(),
                    _arguments.isRunSingletonMode(),
                    _arguments.getChaosWorkers(), _arguments.getChaosInterval()));

            System.out.println(String.format("Called count: %d\tCache Miss: %d\tFailed Count: %d\tService Created: %d" +
                            "\tService Destroyed: %d\tChaos: %d\tStable: %d\tRegister: %d\tEvict: %d\tLoad: %d",
                    _serviceCalled.count(), _cacheMissed.count(), _serviceFailed.count(),
                    _serviceCreated.count(), _serviceDestroyed.count(),
                    _chaosCreated.count(), _stableCreated.count(),
                    _registerTimer.count(), _evictionTimer.count(), _loadTimer.count()));

            System.out.println();

            System.out.println(String.format("\tcreated / destroyed\t-- 1-min: %3.2f/s / %3.2f/s" +
                            "\t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %3.2f/s / %3.2f/s",
                    _serviceCreated.oneMinuteRate(), _serviceDestroyed.oneMinuteRate(),
                    _serviceCreated.fiveMinuteRate(), _serviceDestroyed.fiveMinuteRate(),
                    _serviceCreated.fifteenMinuteRate(), _serviceDestroyed.fifteenMinuteRate(),
                    _serviceCreated.meanRate(), _serviceDestroyed.meanRate()));

            System.out.println(String.format("\tchaos / stable\t\t-- 1-min: %3.2f/s / %3.2f/s" +
                            "\t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %3.2f/s / %3.2f/s",
                    _chaosCreated.oneMinuteRate(), _stableCreated.oneMinuteRate(),
                    _chaosCreated.fiveMinuteRate(), _stableCreated.fiveMinuteRate(),
                    _chaosCreated.fifteenMinuteRate(), _stableCreated.fifteenMinuteRate(),
                    _chaosCreated.meanRate(), _stableCreated.meanRate()));

            System.out.println(String.format("\texecuted / failure\t-- 1-min: %3.2f/s / %3.2f/s" +
                            "\t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %3.2f/s / %3.2f/s",
                    _serviceCalled.oneMinuteRate(), _serviceFailed.oneMinuteRate(),
                    _serviceCalled.fiveMinuteRate(), _serviceFailed.fiveMinuteRate(),
                    _serviceCalled.fifteenMinuteRate(), _serviceFailed.fifteenMinuteRate(),
                    _serviceCalled.meanRate(), _serviceFailed.meanRate()));

            System.out.println(String.format("\tservice / total\t\t-- 1-min: %3.2f/s / %3.2f/s" +
                            "\t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %3.2f/s / %3.2f/s",
                    _serviceTimer.oneMinuteRate(), _totalTimer.oneMinuteRate(),
                    _serviceTimer.fiveMinuteRate(), _totalTimer.fiveMinuteRate(),
                    _serviceTimer.fifteenMinuteRate(), _totalTimer.fifteenMinuteRate(),
                    _serviceTimer.meanRate(), _totalTimer.meanRate()));

            System.out.println(String.format("\tcheckout / checkin\t-- 1-min: %3.2f/s / %3.2f/s" +
                            "\t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %3.2f/s / %3.2f/s",
                    _checkoutTimer.oneMinuteRate(), _checkinTimer.oneMinuteRate(),
                    _checkoutTimer.fiveMinuteRate(), _checkinTimer.fiveMinuteRate(),
                    _checkoutTimer.fifteenMinuteRate(), _checkinTimer.fifteenMinuteRate(),
                    _checkoutTimer.meanRate(), _checkinTimer.meanRate()));

            System.out.println();
            System.out.flush();
        }

        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(_reportingIntervalSeconds));
        }
        catch (InterruptedException ignored) {
        }
    }

    public void printHeaders() {
        _out.print("counter,");
        _out.print(String.format("%s,%s,%s,%s,%s,", "cr-totl", "cr-mnrt", "cr-1mrt", "cr-5mrt", "cr-15rt"));
        _out.print(String.format("%s,%s,%s,%s,%s,", "dt-totl", "dt-mnrt", "dt-1mrt", "dt-5mrt", "dt-15rt"));
        _out.print(String.format("%s,%s,%s,%s,%s,", "sc-totl", "sc-mnrt", "sc-1mrt", "sc-5mrt", "sc-15rt"));
        _out.print(String.format("%s,%s,%s,%s,%s,%s,%s,", "co-totl", "co-min", "co-max", "co-mean", "co-1mrt", "co-5mrt", "co-15rt"));
        _out.print(String.format("%s,%s,%s,%s,%s,%s,%s,", "ci-totl", "ci-min", "ci-max", "ci-mean", "ci-1mrt", "ci-5mrt", "ci-15rt"));
        _out.print(String.format("%s,%s,%s,%s,%s,%s,%s,", "st-totl", "st-min", "st-max", "st-mean", "st-1mrt", "st-5mrt", "st-15rt"));
        _out.print(String.format("%s,%s,%s,%s,%s,%s,%s,", "tt-totl", "tt-min", "tt-max", "tt-mean", "tt-1mrt", "tt-5mrt", "tt-15rt"));
        _out.println();
    }

    public boolean shouldContinue() {

        long spent = currentTimeSeconds() - _startTime;
        boolean shouldContinue;

        if (spent >= _totalRuntime) {
            for (Thread t : _workers) {
                if (t.isAlive()) {
                    try {
                        t.interrupt();
                        t.join();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            shouldContinue = false;
        } else {
            int total = _workers.size();
            int done = 0;
            for (Thread t : _workers) {
                if (!t.isAlive()) {
                    done++;
                }
            }
            shouldContinue = (done != total);
        }

        if (!shouldContinue) {
            _out.close();
        }
        return shouldContinue;
    }

    public long currentTimeSeconds() {
        return TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    // Snapshot returns times in NS, this converts them to ms as we need
    private double nsToMs(double ns) {
        return ns / 1000000;
    }
}
