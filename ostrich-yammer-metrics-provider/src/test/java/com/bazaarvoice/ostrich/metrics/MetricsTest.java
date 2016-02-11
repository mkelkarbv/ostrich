package com.bazaarvoice.ostrich.metrics;

import com.bazaarvoice.ostrich.metrics.yammer.YammerMetrics;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Mockito.mock;

public class MetricsTest {
    private static final Service INSTANCE_ONE = new Service();
    private static final Service INSTANCE_TWO = new Service();

    private final YammerMetrics _metrics = YammerMetrics.forClass(Service.class);

    @After
    public void teardown() {
        _metrics.close();
    }

    @Test(expected = NullPointerException.class)
    public void testNullDomain() {
        YammerMetrics.forClass(null);
    }

    @Test(expected = NullPointerException.class)
    public void testInstanceNullInstance() {
        YammerMetrics.forInstance(null, "scope");
    }

    @Test(expected = NullPointerException.class)
    public void testInstanceNullScope() {
        YammerMetrics.forInstance(this, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstanceEmptyScope() {
        YammerMetrics.forInstance(this, "");
    }

    @Test
    public void testInstanceInfersDomain() {
        YammerMetrics metrics = YammerMetrics.forInstance(this, "scope");

        assertEquals(new MetricName(getClass(), "name", "scope"), metrics.newName("scope", "name"));

        metrics.close();
    }

    @Test
    public void testNamesWithDifferentScopes() {
        MetricName name1 = _metrics.newName("scope1", "name");
        MetricName name2 = _metrics.newName("scope2", "name");
        assertNotEquals(name1, name2);
    }

    @Test
    public void testNamesWithIdenticalScopes() {
        MetricName name1 = _metrics.newName("scope", "name");
        MetricName name2 = _metrics.newName("scope", "name");
        Assert.assertEquals(name1, name2);
    }

    @Test(expected = NullPointerException.class)
    public void testNullGauge() {
        _metrics.newGauge("scope", "name", null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void testNewGaugeNullScope() {
        _metrics.newGauge(null, "name", mock(Gauge.class));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testNewGaugeEmptyScope() {
        _metrics.newGauge("", "name", mock(Gauge.class));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void testNewGaugeNullName() {
        _metrics.newGauge("scope", null, mock(Gauge.class));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testNewGaugeEmptyName() {
        _metrics.newGauge("scope", "", mock(Gauge.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNewGauge() {
        _metrics.newGauge("scope", "name", mock(Gauge.class));
        assertRegistered("scope", "name");
    }

    @Test(expected = NullPointerException.class)
    public void testNewCounterNullScope() {
        _metrics.newCounter(null, "name");
    }

    @Test(expected = NullPointerException.class)
    public void testNewCounterNullName() {
        _metrics.newCounter("scope", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewCounterEmptyScope() {
        _metrics.newCounter("", "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewCounterEmptyName() {
        _metrics.newCounter("scope", "");
    }

    @Test
    public void testNewCounter() {
        _metrics.newCounter("scope", "name");
        assertRegistered("scope", "name");
    }

    @Test(expected = NullPointerException.class)
    public void testNewHistogramNullScope() {
        _metrics.newHistogram(null, "name", false);
    }

    @Test(expected = NullPointerException.class)
    public void testNewHistogramNullName() {
        _metrics.newHistogram("scope", null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHistogramEmptyScope() {
        _metrics.newHistogram("", "name", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHistogramEmptyName() {
        _metrics.newHistogram("scope", "", false);
    }

    @Test
    public void testNewHistogram() {
        _metrics.newHistogram("scope", "name", false);
        assertRegistered("scope", "name");
    }

    @Test(expected = NullPointerException.class)
    public void testNewMeterNullScope() {
        _metrics.newMeter(null, "name", "event", TimeUnit.MICROSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testNewMeterNullName() {
        _metrics.newMeter("scope", null, "event", TimeUnit.MICROSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testNewMeterNullEventType() {
        _metrics.newMeter("scope", "name", null, TimeUnit.MICROSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testNewMeterNullTimeUnit() {
        _metrics.newMeter("scope", "name", "event", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterEmptyScope() {
        _metrics.newMeter("", "name", "events", TimeUnit.MICROSECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterEmptyName() {
        _metrics.newMeter("scope", "", "events", TimeUnit.MICROSECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterEmptyEventType() {
        _metrics.newMeter("scope", "name", "", TimeUnit.MICROSECONDS);
    }

    @Test
    public void testNewMeter() {
        _metrics.newMeter("scope", "name", "event", TimeUnit.MICROSECONDS);
        assertRegistered("scope", "name");
    }

    @Test(expected = NullPointerException.class)
    public void testNewTimerNullScope() {
        _metrics.newTimer(null, "name", TimeUnit.MICROSECONDS, TimeUnit.MICROSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testNewTimerNullName() {
        _metrics.newTimer("scope", null, TimeUnit.MICROSECONDS, TimeUnit.MICROSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testNewTimerNullDurationUnit() {
        _metrics.newTimer("scope", "name", null, TimeUnit.MICROSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testNewTimerNullRateUnit() {
        _metrics.newTimer("scope", "name", TimeUnit.MICROSECONDS, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewTimerEmptyScope() {
        _metrics.newTimer("", "name", TimeUnit.MICROSECONDS, TimeUnit.MICROSECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewTimerEmptyName() {
        _metrics.newTimer("scope", "", TimeUnit.MICROSECONDS, TimeUnit.MICROSECONDS);
    }

    @Test
    public void testNewTimer() {
        _metrics.newTimer("scope", "name", TimeUnit.MICROSECONDS, TimeUnit.MICROSECONDS);
        assertRegistered("scope", "name");
    }

    @Test(expected = NullPointerException.class)
    public void testAddInstanceNullScope() {
        _metrics.addInstance(this, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInstanceEmptyScope() {
        _metrics.addInstance(this, "");
    }

    @Test
    public void testAddInstance() {
        assertEquals(1, _metrics.addInstance(this, "scope").value().intValue());
    }

    @Test
    public void testInstanceGaugeReused() {
        YammerMetrics instancedMetrics = YammerMetrics.forInstance(INSTANCE_ONE, "scope");
        Gauge<Integer> gauge = _metrics.addInstance(INSTANCE_TWO, "scope");

        Assert.assertEquals(2, gauge.value().intValue());

        instancedMetrics.close();
    }

    @Test
    public void testCloseUnregisters() {
        _metrics.newCounter("scope", "name");
        assertRegistered("scope", "name");

        _metrics.close();
        assertNotRegistered("scope", "name");
    }

    @Test
    public void testCloseUnregistersInstanceGauge() {
        YammerMetrics metrics = YammerMetrics.forInstance(this, "scope");

        metrics.close();
        assertNotRegistered("scope", "num-instances");
    }

    @Test
    public void testCloseKeepsActiveInstanceGauge() {
        YammerMetrics metrics = YammerMetrics.forInstance(INSTANCE_ONE, "scope");
        YammerMetrics moreMetrics = YammerMetrics.forInstance(INSTANCE_TWO, "scope");

        metrics.close();
        assertRegistered("scope", "num-instances");

        moreMetrics.close();
    }

    @Test
    public void testCloseDecreasesInstanceCount() {
        Gauge<Integer> gauge = _metrics.addInstance(this, "scope");

        _metrics.close();
        Assert.assertEquals(0, gauge.value().intValue());
    }

    @Test
    public void testCloseKeepsWhenActiveInstancesExist() {
        YammerMetrics metrics = YammerMetrics.forInstance(INSTANCE_ONE, "scope");
        YammerMetrics moreMetrics = YammerMetrics.forInstance(INSTANCE_TWO, "scope");

        metrics.newCounter("scope", "name");
        moreMetrics.newCounter("scope", "name");

        metrics.close();
        assertRegistered("scope", "name");

        moreMetrics.close();
    }

    @Test
    public void testCloseUnregistersDifferentScopeWhenActiveInstancesExist() {
        YammerMetrics metrics = YammerMetrics.forInstance(INSTANCE_ONE, "scope");
        YammerMetrics moreMetrics = YammerMetrics.forInstance(INSTANCE_TWO, "scope");

        metrics.newCounter("different", "name");

        metrics.close();
        assertNotRegistered("different", "name");

        moreMetrics.close();
    }

    @Test
    public void testCloseUnregistersWhenLastActiveInstance() {
        YammerMetrics metrics = YammerMetrics.forInstance(INSTANCE_ONE, "scope");
        YammerMetrics moreMetrics = YammerMetrics.forInstance(INSTANCE_TWO, "scope");

        metrics.newCounter("scope", "name");
        moreMetrics.newCounter("scope", "name");

        metrics.close();
        moreMetrics.close();
        assertNotRegistered("scope", "name");
    }

    @Test
    public void testPathologicalScope() {
        // ,=:*? and newline are invalid characters in an ObjectName. Backslash and quote need to be escaped.
        String scope = "\"\\,:=?*\n";
        _metrics.newName(scope, "name");  // Make a metric with a non-JMX friendly name can be constructed without an exception
    }

    @Test
    public void testPathologicalName() {
        // ,=:*? and newline are invalid characters in an ObjectName. Backslash and quote need to be escaped.
        String name = "\"\\,:=?*\n";
        _metrics.newName("scope", name);  // Make a metric with a non-JMX friendly name can be constructed without an exception
    }

    private void assertRegistered(String scope, String name) {
        MetricName metricName = _metrics.newName(scope, name);
        Metric metric = _metrics.getRegistry().allMetrics().get(metricName);
        Assert.assertNotNull(metric);
    }

    private void assertNotRegistered(String scope, String name) {
        MetricName metricName = _metrics.newName(scope, name);
        Metric metric = _metrics.getRegistry().allMetrics().get(metricName);
        Assert.assertNull(metric);
    }

    private static <T> void assertNotEquals(T a, T b) {
        Assert.assertThat(a, not(equalTo(b)));
    }

    // Dummy class for testing.
    private static class Service {}
}
