package yammercom.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.HostDiscovery;
import com.bazaarvoice.ostrich.LoadBalanceAlgorithm;
import com.bazaarvoice.ostrich.ServiceFactory;
import com.bazaarvoice.ostrich.metrics.yammer.YammerMetrics;
import com.bazaarvoice.ostrich.partition.PartitionFilter;
import com.bazaarvoice.ostrich.pool.CoreServicePool;
import com.bazaarvoice.ostrich.pool.ServiceCachingPolicy;
import com.bazaarvoice.ostrich.spi.InstanceGauge;
import com.bazaarvoice.ostrich.spi.ScopedMetrics;
import com.google.common.base.Ticker;

import java.util.concurrent.ScheduledExecutorService;

public class ServicePool<S> extends CoreServicePool<S> {

    public ServicePool(Ticker ticker, HostDiscovery hostDiscovery, boolean cleanupHostDiscoveryOnClose,
                       ServiceFactory<S> serviceFactory, ServiceCachingPolicy cachingPolicy,
                       PartitionFilter partitionFilter, LoadBalanceAlgorithm loadBalanceAlgorithm,
                       ScheduledExecutorService healthCheckExecutor, boolean shutdownHealthCheckExecutorOnClose) {
        super(ticker, hostDiscovery, cleanupHostDiscoveryOnClose, serviceFactory, cachingPolicy, partitionFilter, loadBalanceAlgorithm,
            healthCheckExecutor, shutdownHealthCheckExecutorOnClose,
            new ScopedMetrics(ServicePool.class.getCanonicalName(), new YammerMetrics(com.yammer.metrics.Metrics.defaultRegistry())));

        InstanceGauge instanceGauge = new InstanceGauge();
        instanceGauge = (InstanceGauge) _metrics.registerGauge(serviceFactory.getServiceName(), "num-instances", instanceGauge);
        instanceGauge.add(this);
    }
}
