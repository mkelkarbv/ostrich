package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.HostDiscovery;
import com.bazaarvoice.ostrich.LoadBalanceAlgorithm;
import com.bazaarvoice.ostrich.ServiceFactory;
import com.bazaarvoice.ostrich.metrics.notimplemented.NotImplementedMetrics;
import com.bazaarvoice.ostrich.partition.PartitionFilter;
import com.google.common.base.Ticker;
import yammercom.bazaarvoice.ostrich.pool.ServicePool;

import java.util.concurrent.ScheduledExecutorService;

public class NoOpMetricsServicePool<S> extends ServicePool<S> {

    public NoOpMetricsServicePool(Ticker ticker, HostDiscovery hostDiscovery, boolean cleanupHostDiscoveryOnClose,
                                  ServiceFactory<S> serviceFactory, ServiceCachingPolicy cachingPolicy,
                                  PartitionFilter partitionFilter, LoadBalanceAlgorithm loadBalanceAlgorithm,
                                  ScheduledExecutorService healthCheckExecutor, boolean shutdownHealthCheckExecutorOnClose) {


        super(ticker, hostDiscovery, cleanupHostDiscoveryOnClose, serviceFactory, cachingPolicy, partitionFilter, loadBalanceAlgorithm,
            healthCheckExecutor, shutdownHealthCheckExecutorOnClose, NotImplementedMetrics.forClass(ServicePool.class));

    }
}
