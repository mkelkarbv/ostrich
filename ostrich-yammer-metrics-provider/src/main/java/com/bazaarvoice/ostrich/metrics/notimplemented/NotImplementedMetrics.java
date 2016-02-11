package com.bazaarvoice.ostrich.metrics.notimplemented;

import com.bazaarvoice.ostrich.spi.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;

import java.util.concurrent.TimeUnit;

public class NotImplementedMetrics implements Metrics {

    public static NotImplementedMetrics forClass(Class<?> owner) {
        return new NotImplementedMetrics();
    }

    @Override
    public <T> Gauge<T> addInstance(final Object instance, final String scope) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public <T> Gauge<T> newGauge(final String scope, final String name, final Gauge<T> metric) {
        return null;
    }

    @Override
    public Counter newCounter(final String scope, final String name) {
        return null;
    }

    @Override
    public Histogram newHistogram(final String scope, final String name, final boolean biased) {
        return null;
    }

    @Override
    public Meter newMeter(final String scope, final String name, final String eventType, final TimeUnit unit) {
        return null;
    }

    @Override
    public Timer newTimer(final String scope, final String name, final TimeUnit durationUnit, final TimeUnit rateUnit) {
        return null;
    }
}
