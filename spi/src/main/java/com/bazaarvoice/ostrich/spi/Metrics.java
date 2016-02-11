package com.bazaarvoice.ostrich.spi;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public interface Metrics extends Closeable {

    <T> Gauge<T> registerGauge(String group, String type, String name, Gauge<T> metric);

    Counter newCounter(String group, String type, String name);

    Histogram newHistogram(String group, String type, String name, boolean biased);

    Meter newMeter(String group, String type, String name, String eventType, TimeUnit unit);

    Timer newTimer(String group, String type, String name, TimeUnit durationUnit, TimeUnit rateUnit);
}