package com.bazaarvoice.ostrich.perftest.utils;

import com.bazaarvoice.ostrich.perftest.core.SimpleResultFactory;
import com.bazaarvoice.ostrich.perftest.core.SimpleServiceFactory;
import com.bazaarvoice.ostrich.pool.ServiceRunner;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * This class creates the service runner and runs the requested load as parsed via Arguments
 * This also prints/writes the report log and/or the statistics as requested in the Arguments
 */
public class LoadRunner {

    private final PrintStream out;
    private final boolean doPrintStats;
    private final long totalRuntime;
    private final SimpleServiceFactory serviceFactory;
    private final ServiceRunner serviceRunner;
    private final List<Thread> workers;
    private final long startTime;
    private final int reportingIntervalSeconds;
    private final Arguments _arguments;
    private long counter = 0;

    public LoadRunner(Arguments arguments) {

        _arguments = arguments;

        SimpleResultFactory resultFactory = SimpleResultFactory.newInstance();
        this.serviceFactory = SimpleServiceFactory.newInstance();
        this.serviceRunner = ServiceRunner.builder()
                .withThreadSize(arguments.getThreadSize())
                .withWorkSize(arguments.getWorkSize())
                .withMaxServiceInstances(arguments.getMaxInstance())
                .withMaxServiceIdleTimeSeconds(arguments.getIdleTimeSecond())
                .withExhaustionAction(arguments.getExhaustionAction())
                .withServiceFactory(serviceFactory)
                .withResultFactory(resultFactory)
                .build();
        this.workers = serviceRunner.generateWorkers();
        this.out = arguments.getOutput();
        this.doPrintStats = arguments.doPrintStats();
        this.totalRuntime = arguments.getRunTimeSecond();
        this.reportingIntervalSeconds = arguments.getReportingIntervalSeconds();

        for (Thread thread : workers) {
            thread.start();
        }

        this.startTime = currentTimeSeconds();
    }

    public void printLog() {

        long currentRuntime = currentTimeSeconds() - startTime;

        Meter serviceCreated = serviceFactory.getServiceCreated();
        Meter serviceDestroyed = serviceFactory.getServiceDestroyed();
        Meter serviceCalled = serviceRunner.getServiceMeter();
        Timer checkoutTimer = serviceRunner.getCheckoutTimer();
        Timer checkinTimer = serviceRunner.getCheckinTimer();
        Timer serviceTimer = serviceFactory.getServiceTimer();
        Timer totalTimer = serviceRunner.getTotalExecTimer();

        out.print(String.format("%d,", counter++));
        out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,",
                serviceCreated.count(), serviceCreated.meanRate(), serviceCreated.oneMinuteRate(), serviceCreated.fiveMinuteRate(), serviceCreated.fifteenMinuteRate()));
        out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,",
                serviceDestroyed.count(), serviceDestroyed.meanRate(), serviceDestroyed.oneMinuteRate(), serviceDestroyed.fiveMinuteRate(), serviceDestroyed.fifteenMinuteRate()));
        out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,",
                serviceCalled.count(), serviceCalled.meanRate(), serviceCalled.oneMinuteRate(), serviceCalled.fiveMinuteRate(), serviceCalled.fifteenMinuteRate()));

        out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                checkoutTimer.count(), checkoutTimer.min(), checkoutTimer.max(), checkoutTimer.mean(), checkoutTimer.oneMinuteRate(), checkoutTimer.fiveMinuteRate(), checkoutTimer.fifteenMinuteRate()));
        out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                checkinTimer.count(), checkinTimer.min(), checkinTimer.max(), checkinTimer.mean(), checkinTimer.oneMinuteRate(), checkinTimer.fiveMinuteRate(), checkinTimer.fifteenMinuteRate()));
        out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                serviceTimer.count(), serviceTimer.min(), serviceTimer.max(), serviceTimer.mean(), serviceTimer.oneMinuteRate(), serviceTimer.fiveMinuteRate(), serviceTimer.fifteenMinuteRate()));
        out.print(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                totalTimer.count(), totalTimer.min(), totalTimer.max(), totalTimer.mean(), totalTimer.oneMinuteRate(), totalTimer.fiveMinuteRate(), totalTimer.fifteenMinuteRate()));

        out.println();
        out.flush();

        if (doPrintStats) {
            System.out.print("\u001b[2J");

            System.out.println(String.format("Running %d seconds of %s with threads: %d, work size: %d, idle time: %d, max instance: %d, exhaust action: %s",
                    currentRuntime, _arguments.getRunTimeSecond(), _arguments.getThreadSize(), _arguments.getWorkSize(), _arguments.getIdleTimeSecond(), _arguments.getMaxInstance(), _arguments.getExhaustionAction().name()));
            System.out.println(String.format("\tcreated / destroyed\t\t-- total: %12d / %12d\t1-min: %3.2f/s / %3.2f/s  \t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %3.2f/s / %3.2f/s",
                    serviceCreated.count(), serviceDestroyed.count(), serviceCreated.oneMinuteRate(), serviceDestroyed.oneMinuteRate(),
                    serviceCreated.fiveMinuteRate(), serviceDestroyed.fiveMinuteRate(), serviceCreated.fifteenMinuteRate(), serviceDestroyed.fifteenMinuteRate(),
                    serviceCreated.meanRate(), serviceDestroyed.meanRate()));

            System.out.println(String.format("\tservice / total  \t\t-- total: %12d / %12d\t1-min: %3.2f/s / %3.2f/s  \t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %.2fms / %.2fms",
                    serviceTimer.count(), totalTimer.count(), serviceTimer.oneMinuteRate(), totalTimer.oneMinuteRate(),
                    serviceTimer.fiveMinuteRate(), totalTimer.fiveMinuteRate(), serviceTimer.fifteenMinuteRate(), totalTimer.fifteenMinuteRate(),
                    serviceTimer.mean(), totalTimer.mean()));

            System.out.println(String.format("\tcheckout / checkin\t\t-- total: %12d / %12d\t1-min: %3.2f/s / %3.2f/s  \t5-min: %3.2f/s / %3.2f/s  \t15-min: %3.2f/s / %3.2f/s\tmean: %.2fms / %.2fms",
                    checkoutTimer.count(), checkinTimer.count(), checkoutTimer.oneMinuteRate(), checkinTimer.oneMinuteRate(),
                    checkoutTimer.fiveMinuteRate(), checkinTimer.fiveMinuteRate(), checkoutTimer.fifteenMinuteRate(), checkinTimer.fifteenMinuteRate(),
                    checkoutTimer.mean(), checkinTimer.mean()));

            System.out.flush();
        }

        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(reportingIntervalSeconds));
        } catch(InterruptedException ignored) {}
    }

    public void printHeaders() {
        out.print("counter,");
        out.print(String.format("%s,%s,%s,%s,%s,", "cr-totl", "cr-mnrt", "cr-1mrt", "cr-5mrt", "cr-15rt"));
        out.print(String.format("%s,%s,%s,%s,%s,", "dt-totl", "dt-mnrt", "dt-1mrt", "dt-5mrt", "dt-15rt"));
        out.print(String.format("%s,%s,%s,%s,%s,", "sc-totl", "sc-mnrt", "sc-1mrt", "sc-5mrt", "sc-15rt"));
        out.print(String.format("%s,%s,%s,%s,%s,%s,%s,", "co-totl", "co-min", "co-max", "co-mean", "co-1mrt", "co-5mrt", "co-15rt"));
        out.print(String.format("%s,%s,%s,%s,%s,%s,%s,", "ci-totl", "ci-min", "ci-max", "ci-mean", "ci-1mrt", "ci-5mrt", "ci-15rt"));
        out.print(String.format("%s,%s,%s,%s,%s,%s,%s,", "st-totl", "st-min", "st-max", "st-mean", "st-1mrt", "st-5mrt", "st-15rt"));
        out.print(String.format("%s,%s,%s,%s,%s,%s,%s,", "tt-totl", "tt-min", "tt-max", "tt-mean", "tt-1mrt", "tt-5mrt", "tt-15rt"));
        out.println();
    }

    public boolean shouldContinue() {

        long spent = currentTimeSeconds() - startTime;
        boolean shouldContinue;

        if (spent >= totalRuntime) {
            for (Thread t : workers) {
                if (t.isAlive()) {
                    try {
                        t.interrupt();
                        t.join();
                    } catch (InterruptedException ignored) { }
                }
            }
            shouldContinue = false;
        } else {
            int total = workers.size();
            int done = 0;
            for (Thread t : workers) {
                if (!t.isAlive()) done++;
            }
            shouldContinue = !(done == total);
        }

        if(!shouldContinue) {
            out.close();
        }
        return shouldContinue;
    }

    public long currentTimeSeconds() {
        return TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
}
