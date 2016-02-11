package com.bazaarvoice.ostrich.discovery.zookeeper;

import com.bazaarvoice.ostrich.discovery.CoreZooKeeperHostDiscovery;
import com.bazaarvoice.ostrich.metrics.notimplemented.NotImplementedMetrics;
import com.google.common.annotations.VisibleForTesting;
import org.apache.curator.framework.CuratorFramework;

public class NoOpMetricsZookeeperHostDiscovery extends CoreZooKeeperHostDiscovery {

    public NoOpMetricsZookeeperHostDiscovery(CuratorFramework curator, String serviceName) {
        this(new CoreZooKeeperHostDiscovery.NodeDiscoveryFactory(), curator, serviceName);
    }

    @VisibleForTesting NoOpMetricsZookeeperHostDiscovery(NodeDiscoveryFactory factory, CuratorFramework curator, String serviceName) {
        super(factory, curator, serviceName, NotImplementedMetrics.forClass(ZooKeeperHostDiscovery.class));
    }
}
