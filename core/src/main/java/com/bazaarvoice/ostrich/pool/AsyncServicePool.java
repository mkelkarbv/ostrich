package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.metrics.yammer.YammerMetrics;
import com.google.common.base.Ticker;
import yammercom.bazaarvoice.ostrich.pool.ServicePool;

import java.util.concurrent.ExecutorService;

class AsyncServicePool<S> extends CoreAsyncServicePool<S> {

    AsyncServicePool(Ticker ticker, ServicePool<S> pool, boolean shutdownPoolOnClose,
                     ExecutorService executor, boolean shutdownExecutorOnClose) {
        super(ticker, pool, shutdownPoolOnClose, executor, shutdownExecutorOnClose, YammerMetrics.forClass(AsyncServicePool.class));
        _metrics.addInstance(this, pool.getServiceName());
    }
}
