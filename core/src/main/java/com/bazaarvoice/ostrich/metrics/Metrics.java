package com.bazaarvoice.ostrich.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A thin wrapper implementation around Yammer Metrics for use by SOA.  This wrapper adds the following functionality:
 * <p/>
 * The ability to control what {@link MetricRegistry} instance is used for SOA metrics via
 * {@link #setMetricRegistry(MetricRegistry)}.  This gives us the ability in the future to isolate the SOA metrics
 * from the end user's application metrics as well as publish them somewhere differently from the end user's application
 * metrics.
 */
public abstract class Metrics implements Closeable {
    private static MetricRegistry METRICS_REGISTRY = new MetricRegistry();

    /** Set the metric registry that should be used by the SOA library for creating and registering metrics. */
    public static void setMetricRegistry(MetricRegistry registry) {
        METRICS_REGISTRY = checkNotNull(registry);
    }

    /** Return the metric registry that is currently being used. */
    public static MetricRegistry getMetricsRegistry() {
        return METRICS_REGISTRY;
    }

    /**
     * Create a metrics instance that corresponds to a class.  This is useful for cases where a single instance handles
     * many different services at the same time.  For example a ServiceRegistry.
     */
    public static ClassMetrics forClass(Class<?> cls) {
        return new ClassMetrics(cls);
    }

    /**
     * Create a metrics instance that corresponds to a single instance of a class.  This is useful for cases where there
     * exists one instance per service.  For example in a ServicePool.
     */
    public static InstanceMetrics forInstance(Object instance, String serviceName) {
        return new InstanceMetrics(instance, serviceName);
    }

    public static final class ClassMetrics implements Closeable {
        private final MetricRegistry _metrics = METRICS_REGISTRY;
        private final Class<?> _class;
        private final String _prefix;
        private final Set<String> _names = Sets.newHashSet();

        ClassMetrics(Class<?> cls) {
            _class = checkNotNull(cls);
            _prefix = MetricRegistry.name(_class);
        }

        public synchronized <T> Gauge<T> gauge(String serviceName, String name, Gauge<T> metric) {
            checkNotNullOrEmpty(serviceName);
            checkNotNullOrEmpty(name);
            checkNotNull(metric);
            return _metrics.register(name(serviceName, name), metric);
        }

        public synchronized Counter counter(String serviceName, String name) {
            checkNotNullOrEmpty(serviceName);
            checkNotNullOrEmpty(name);
            return _metrics.counter(name(serviceName, name));
        }

        public synchronized Histogram histogram(String serviceName, String name) {
            checkNotNullOrEmpty(serviceName);
            checkNotNullOrEmpty(name);
            return _metrics.histogram(name(serviceName, name));
        }

        public synchronized Meter meter(String serviceName, String name) {
            checkNotNullOrEmpty(serviceName);
            checkNotNullOrEmpty(name);
            return _metrics.meter(name(serviceName, name));
        }

        public synchronized Timer timer(String serviceName, String name) {
            checkNotNullOrEmpty(serviceName);
            checkNotNullOrEmpty(name);
            return _metrics.timer(name(serviceName, name));
        }

        @Override
        public synchronized void close() {
            for (String name : _names) {
                _metrics.remove(name);
            }
            _names.clear();
        }

        @VisibleForTesting
        String name(String serviceName, String name) {
            String fullName = MetricRegistry.name(_prefix, serviceName, name);
            _names.add(fullName);
            return fullName;
        }
    }

    public static class InstanceMetrics implements Closeable {
        private final MetricRegistry _registry = METRICS_REGISTRY;
        private final Map<String, Metric> _metrics = _registry.getMetrics();
        private final Object _instance;
        private final String _serviceName;
        private final String _prefix;

        private final String _instanceCounterName;
        private final Counter _instanceCounter;
        private final Set<String> _names = Sets.newHashSet();

        InstanceMetrics(Object instance, String serviceName) {
            _instance = checkNotNull(instance);
            _serviceName = checkNotNullOrEmpty(serviceName);
            _prefix = MetricRegistry.name(_instance.getClass());
            _instanceCounterName = name("num-instances");
            _instanceCounter = counter("num-instances");
            _instanceCounter.inc();
        }

        @SuppressWarnings("unchecked")
        public synchronized <T> Gauge<T> gauge(String name, Gauge<T> gauge) {
            checkNotNullOrEmpty(name);
            checkNotNull(gauge);

            // Unfortunately register doesn't call getOrAdd (probably a bug in metrics), and instead goes through
            // a code path that throws an exception if this metric already exists.
            String fullName = name(name);
            Gauge<T> metric = (Gauge<T>) _metrics.get(fullName);
            if (metric == null) {
                metric = _registry.register(fullName, gauge);
            }

            return metric;
        }

        public synchronized Counter counter(String name) {
            checkNotNullOrEmpty(name);
            return _registry.counter(name(name));
        }

        public synchronized Histogram histogram(String name) {
            checkNotNullOrEmpty(name);
            return _registry.histogram(name(name));
        }

        public synchronized Meter meter(String name) {
            checkNotNullOrEmpty(name);
            return _registry.meter(name(name));
        }

        public synchronized Timer timer(String name) {
            checkNotNullOrEmpty(name);
            return _registry.timer(name(name));
        }

        @Override
        public synchronized void close() {
            _instanceCounter.dec();
            if (_instanceCounter.getCount() == 0) {
                _registry.remove(_instanceCounterName);
            }

            for (String name : _names) {
                _registry.remove(name);
            }
            _names.clear();
        }
        
        @VisibleForTesting
        Counter getInstanceCounter() {
            return _instanceCounter;
        }

        @VisibleForTesting
        String name(String name) {
            String fullName = MetricRegistry.name(_prefix, _serviceName, name);
            _names.add(fullName);
            return fullName;
        }
    }

    private static String checkNotNullOrEmpty(String string) {
        checkNotNull(string);
        checkArgument(!Strings.isNullOrEmpty(string));
        return string;
    }
}