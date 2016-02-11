package com.bazaarvoice.ostrich.spi;

import java.util.concurrent.TimeUnit;

public interface Timer {

    TimerContext time();

    void update(long duration, TimeUnit nanoseconds);

}
