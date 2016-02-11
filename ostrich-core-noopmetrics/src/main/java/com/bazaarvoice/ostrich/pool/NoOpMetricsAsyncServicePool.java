package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.metrics.notimplemented.NotImplementedMetrics;
import com.google.common.base.Ticker;
import yammercom.bazaarvoice.ostrich.pool.ServicePool;

import java.util.concurrent.ExecutorService;

public class NoOpMetricsAsyncServicePool<S> extends CoreAsyncServicePool<S> {

    public NoOpMetricsAsyncServicePool(Ticker ticker, ServicePool<S> pool, boolean shutdownPoolOnClose,
                                       ExecutorService executor, boolean shutdownExecutorOnClose) {

        super(ticker, pool, shutdownPoolOnClose, executor, shutdownExecutorOnClose, NotImplementedMetrics.forClass(NoOpMetricsAsyncServicePool.class));
    }
}
