package com.bazaarvoice.ostrich.spi;

import java.util.concurrent.TimeUnit;

public class ScopedMetrics {

    private final String group;
    private final Metrics metrics;

    public ScopedMetrics(String group, Metrics metrics) {
        this.group = group;
        this.metrics = metrics;
    }

    public <T> Gauge<T> registerGauge(String type, String name, Gauge<T> gauge) {
        return metrics.registerGauge(group, type, name, gauge);
    }

    public Counter newCounter(String type, String name) {
        return metrics.newCounter(group, type, name);
    }

    public Histogram newHistogram(String type, String name, boolean biased) {
        return metrics.newHistogram(group, type, name, biased);
    }

    public Meter newMeter(String type, String name, String eventType, TimeUnit unit) {
        return metrics.newMeter(group, type, name, eventType, unit);
    }

    public Timer newTimer(String type, String name, TimeUnit durationUnit, TimeUnit rateUnit) {
        return metrics.newTimer(group, type, name, durationUnit, rateUnit);
    }
}
