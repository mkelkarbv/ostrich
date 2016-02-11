package com.bazaarvoice.ostrich.metrics.yammer;

import com.bazaarvoice.ostrich.spi.TimerContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A thin wrapper implementation around Yammer YammerMetrics for use by SOA.  This wrapper adds the following functionality:
 */
public class YammerMetrics implements com.bazaarvoice.ostrich.spi.Metrics {
    private MetricsRegistry _registry;

    public YammerMetrics(MetricsRegistry registry) {
        this._registry = registry;
    }

    public <T> com.bazaarvoice.ostrich.spi.Gauge<T> registerGauge(final String group, final String type, final String name, final com.bazaarvoice.ostrich.spi.Gauge<T> metric) {
        return new YammerGage<T>(_registry.newGauge(new MetricName(group, type, name), new Gauge<T>() {
            @Override public T value() {
                return metric.value();
            }
        }));
    }

    @Override public void close() throws IOException {

    }

    public static class YammerCounter implements com.bazaarvoice.ostrich.spi.Counter {
        private Counter delegate;

        public YammerCounter(final com.yammer.metrics.core.Counter delegate) {
            this.delegate = delegate;
        }

        @Override public void dec(final int size) {
            delegate.dec(size);
        }

        @Override public void inc() {
            delegate.inc();
        }

        @Override public void dec() {
            delegate.dec();
        }
    }


    private static class YammerGage<T> implements com.bazaarvoice.ostrich.spi.Gauge<T> {

        private Gauge<T> delegate;

        public YammerGage(final com.yammer.metrics.core.Gauge<T> delegate) {
            this.delegate = delegate;
        }

        @Override public T value() {
            return delegate.value();
        }
    }

    /** @see MetricsRegistry#newCounter(com.yammer.metrics.core.MetricName) */
    public com.bazaarvoice.ostrich.spi.Counter newCounter(String group, String type, String name) {
        return new YammerCounter(_registry.newCounter(new MetricName(group, type, name)));
    }

    private class YammerHistogram implements com.bazaarvoice.ostrich.spi.Histogram {
        public YammerHistogram(final Histogram delegate) {}
    }

    /** @see MetricsRegistry#newHistogram(MetricName, boolean) */
    public com.bazaarvoice.ostrich.spi.Histogram newHistogram(String group, String type, String name, boolean biased) {
        return new YammerHistogram(_registry.newHistogram(new MetricName(group, type, name), biased));
    }


    private class YammerMeter implements com.bazaarvoice.ostrich.spi.Meter {
        private Meter delegate;

        public YammerMeter(final Meter delegate) {this.delegate = delegate;}

        @Override public void mark() {
            delegate.mark();
        }
    }

    /** @see MetricsRegistry#newMeter(MetricName, String, TimeUnit) */
    public com.bazaarvoice.ostrich.spi.Meter newMeter(String group, String type, String name, String eventType, TimeUnit unit) {
        checkNotNullOrEmpty(eventType);
        Preconditions.checkNotNull(unit);
        return new YammerMeter(_registry.newMeter(new MetricName(group, type, name), eventType, unit));
    }

    /** @see MetricsRegistry#newTimer(MetricName, TimeUnit, TimeUnit) */
    public com.bazaarvoice.ostrich.spi.Timer newTimer(String group, String type, String name, TimeUnit durationUnit, TimeUnit rateUnit) {
        Preconditions.checkNotNull(durationUnit);
        Preconditions.checkNotNull(rateUnit);
        return new YammerTimer(_registry.newTimer(new MetricName(group, type, name), durationUnit, rateUnit));
    }

    @VisibleForTesting
    public MetricsRegistry getRegistry() {
        return _registry;
    }

    private static void checkNotNullOrEmpty(String string) {
        Preconditions.checkNotNull(string);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(string));
    }

    private class YammerTimer implements com.bazaarvoice.ostrich.spi.Timer {
        private Timer delegate;

        public YammerTimer(final Timer delegate) {this.delegate = delegate;}

        @Override public TimerContext time() {
            return new YammerTimerContext(delegate.time());
        }

        @Override public void update(final long duration, final TimeUnit nanoseconds) {
            delegate.update(duration, nanoseconds);
        }

        private class YammerTimerContext implements TimerContext {
            private com.yammer.metrics.core.TimerContext timerContextDelegate;

            public YammerTimerContext(final com.yammer.metrics.core.TimerContext timerContextDelegate) {this.timerContextDelegate = timerContextDelegate;}

            @Override public void stop() {
                timerContextDelegate.stop();
            }
        }
    }
}
