package com.bazaarvoice.ostrich.perftest.utils;

import com.bazaarvoice.ostrich.perftest.core.SimpleServiceFactory;
import com.bazaarvoice.ostrich.pool.ServiceRunner;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class creates the service runner and runs the requested load as parsed via Arguments
 * This also prints/writes the report log and/or the statistics as requested in the Arguments
 */
public class LoadRunner {

    private final PrintStream _out;
    private final boolean _doPrintStats;
    private final long _totalRuntime;
    private final SimpleServiceFactory _serviceFactory;
    private final ServiceRunner _serviceRunner;
    private final List<Thread> _workers;
    private final long _startTime;
    private final int _reportingIntervalSeconds;
    private final Arguments _arguments;
    private long _counter = 0;

    public LoadRunner(Arguments arguments) {

        MetricRegistry metricRegistry = new MetricRegistry();
        _arguments = arguments;

        this._serviceFactory = SimpleServiceFactory.newInstance(metricRegistry);
        this._serviceRunner = new ServiceRunner(_serviceFactory, metricRegistry, _arguments);
        this._workers = _serviceRunner.generateWorkers();
        this._out = arguments.getOutput();
        this._doPrintStats = arguments.doPrintStats();
        this._totalRuntime = arguments.getRunTimeSecond();
        this._reportingIntervalSeconds = arguments.getReportingIntervalSeconds();

        for (Thread thread : _workers) {
            thread.start();
        }

        this._startTime = currentTimeSeconds();
    }

    public void printLog() {

        long currentRuntime = currentTimeSeconds() - _startTime;

        Meter serviceCreated = _serviceFactory.getServiceCreated();
        Meter serviceDestroyed = _serviceFactory.getServiceDestroyed();
        Meter serviceCalled = _serviceRunner.getServiceMeter();
        Timer checkoutTimer = _serviceRunner.getCheckoutTimer();
        Timer checkinTimer = _serviceRunner.getCheckinTimer();
        Timer serviceTimer = _serviceFactory.getServiceTimer();
        Timer totalTimer = _serviceRunner.getTotalExecTimer();

        _out.print(String.format("%d,", _counter++));

        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,",
                serviceCreated.getCount(), serviceCreated.getMeanRate(), serviceCreated.getOneMinuteRate(),
                serviceCreated.getFiveMinuteRate(), serviceCreated.getFifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,",
                serviceDestroyed.getCount(), serviceDestroyed.getMeanRate(), serviceDestroyed.getOneMinuteRate(),
                serviceDestroyed.getFiveMinuteRate(), serviceDestroyed.getFifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,",
                serviceCalled.getCount(), serviceCalled.getMeanRate(), serviceCalled.getOneMinuteRate(),
                serviceCalled.getFiveMinuteRate(), serviceCalled.getFifteenMinuteRate()));

        Snapshot checkoutTimerSnapshot = checkoutTimer.getSnapshot();
        Snapshot checkinTimerSnapshot = checkinTimer.getSnapshot();
        Snapshot serviceTimerSnapshot = serviceTimer.getSnapshot();
        Snapshot totalTimerSnapshot = totalTimer.getSnapshot();

        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                checkoutTimer.getCount(), nsToMs(checkoutTimerSnapshot.getMin()),
                nsToMs(checkoutTimerSnapshot.getMax()), nsToMs(checkoutTimerSnapshot.getMean()),
                checkoutTimer.getOneMinuteRate(), checkoutTimer.getFiveMinuteRate(), checkoutTimer.getFifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                checkinTimer.getCount(), nsToMs(checkinTimerSnapshot.getMin()),
                nsToMs(checkinTimerSnapshot.getMax()), nsToMs(checkinTimerSnapshot.getMean()),
                checkinTimer.getOneMinuteRate(), checkinTimer.getFiveMinuteRate(), checkinTimer.getFifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                serviceTimer.getCount(), nsToMs(serviceTimerSnapshot.getMin()),
                nsToMs(serviceTimerSnapshot.getMax()), nsToMs(serviceTimerSnapshot.getMean()),
                serviceTimer.getOneMinuteRate(), serviceTimer.getFiveMinuteRate(), serviceTimer.getFifteenMinuteRate()));
        _out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                totalTimer.getCount(), nsToMs(totalTimerSnapshot.getMin()),
                nsToMs(totalTimerSnapshot.getMax()), nsToMs(totalTimerSnapshot.getMean()),
                totalTimer.getOneMinuteRate(), totalTimer.getFiveMinuteRate(), totalTimer.getFifteenMinuteRate()));

        _out.println();
        _out.flush();

        if (_doPrintStats) {
            System.out.print("\u001b[2J");

            System.out.println(String.format("Running %d seconds of %s with threads: %d, work size: %d, idle time: %d, " +
                            "max instance: %d, exhaust action: %s",
                    currentRuntime, _arguments.getRunTimeSecond(), _arguments.getThreadSize(), _arguments.getWorkSize(),
                    _arguments.getIdleTimeSecond(), _arguments.getMaxInstance(), _arguments.getExhaustionAction().name()));

            System.out.println();

            System.out.println(String.format("\tcreated / destroyed\t-- 1-min: %3.2f/s / %3.2f/s" +
                            "\t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %3.2f/s / %3.2f/s",
                    serviceCreated.getOneMinuteRate(), serviceDestroyed.getOneMinuteRate(),
                    serviceCreated.getFiveMinuteRate(), serviceDestroyed.getFiveMinuteRate(),
                    serviceCreated.getFifteenMinuteRate(), serviceDestroyed.getFifteenMinuteRate(),
                    serviceCreated.getMeanRate(), serviceDestroyed.getMeanRate()));

            System.out.println(String.format("\tservice / total\t\t-- 1-min: %3.2f/s / %3.2f/s" +
                            "\t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %3.2f/s / %3.2f/s",
                    serviceTimer.getOneMinuteRate(), totalTimer.getOneMinuteRate(),
                    serviceTimer.getFiveMinuteRate(), totalTimer.getFiveMinuteRate(),
                    serviceTimer.getFifteenMinuteRate(), totalTimer.getFifteenMinuteRate(),
                    serviceTimer.getMeanRate(), totalTimer.getMeanRate()));

            System.out.println(String.format("\tcheckout / checkin\t-- 1-min: %3.2f/s / %3.2f/s" +
                            "\t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %3.2f/s / %3.2f/s",
                    checkoutTimer.getOneMinuteRate(), checkinTimer.getOneMinuteRate(),
                    checkoutTimer.getFiveMinuteRate(), checkinTimer.getFiveMinuteRate(),
                    checkoutTimer.getFifteenMinuteRate(), checkinTimer.getFifteenMinuteRate(),
                    checkoutTimer.getMeanRate(), checkinTimer.getMeanRate()));

            System.out.println();

            System.out.println(String.format("\tService\t -- mean: %3.2fms\tmin: %3.2fms\tmax: %3.2fms\t75th: %3.2fms" +
                            "\t95th: %3.2fms\t98th: %3.2fms\t99th: %3.2fms\t999th: %3.2fms",
                    nsToMs(serviceTimerSnapshot.getMean()),
                    nsToMs(serviceTimerSnapshot.getMin()),
                    nsToMs(serviceTimerSnapshot.getMax()),
                    nsToMs(serviceTimerSnapshot.get75thPercentile()),
                    nsToMs(serviceTimerSnapshot.get95thPercentile()),
                    nsToMs(serviceTimerSnapshot.get98thPercentile()),
                    nsToMs(serviceTimerSnapshot.get99thPercentile()),
                    nsToMs(serviceTimerSnapshot.get999thPercentile())
            ));

            System.out.println(String.format("\tCheckout -- mean: %3.2fms\tmin: %3.2fms\tmax: %3.2fms\t75th: %3.2fms" +
                            "\t95th: %3.2fms\t98th: %3.2fms\t99th: %3.2fms\t999th: %3.2fms",
                    nsToMs(checkoutTimerSnapshot.getMean()),
                    nsToMs(checkoutTimerSnapshot.getMin()),
                    nsToMs(checkoutTimerSnapshot.getMax()),
                    nsToMs(checkoutTimerSnapshot.get75thPercentile()),
                    nsToMs(checkoutTimerSnapshot.get95thPercentile()),
                    nsToMs(checkoutTimerSnapshot.get98thPercentile()),
                    nsToMs(checkoutTimerSnapshot.get99thPercentile()),
                    nsToMs(checkoutTimerSnapshot.get999thPercentile())
            ));

            System.out.println(String.format("\tCheckin\t -- mean: %3.2fms\tmin: %3.2fms\tmax: %3.2fms\t75th: %3.2fms" +
                            "\t95th: %3.2fms\t98th: %3.2fms\t99th: %3.2fms\t999th: %3.2fms",
                    nsToMs(checkinTimerSnapshot.getMean()),
                    nsToMs(checkinTimerSnapshot.getMin()),
                    nsToMs(checkinTimerSnapshot.getMax()),
                    nsToMs(checkinTimerSnapshot.get75thPercentile()),
                    nsToMs(checkinTimerSnapshot.get95thPercentile()),
                    nsToMs(checkinTimerSnapshot.get98thPercentile()),
                    nsToMs(checkinTimerSnapshot.get99thPercentile()),
                    nsToMs(checkinTimerSnapshot.get999thPercentile())
            ));

            System.out.println(String.format("\tTotal\t -- mean: %3.2fms\tmin: %3.2fms\tmax: %3.2fms\t75th: %3.2fms" +
                            "\t95th: %3.2fms\t98th: %3.2fms\t99th: %3.2fms\t999th: %3.2fms",
                    nsToMs(totalTimerSnapshot.getMean()),
                    nsToMs(totalTimerSnapshot.getMin()),
                    nsToMs(totalTimerSnapshot.getMax()),
                    nsToMs(totalTimerSnapshot.get75thPercentile()),
                    nsToMs(totalTimerSnapshot.get95thPercentile()),
                    nsToMs(totalTimerSnapshot.get98thPercentile()),
                    nsToMs(totalTimerSnapshot.get99thPercentile()),
                    nsToMs(totalTimerSnapshot.get999thPercentile())
            ));

            System.out.flush();
        }

        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(_reportingIntervalSeconds));
        } catch (InterruptedException ignored) {
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
