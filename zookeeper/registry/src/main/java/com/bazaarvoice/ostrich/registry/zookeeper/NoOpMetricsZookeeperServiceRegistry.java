package com.bazaarvoice.ostrich.registry.zookeeper;

import com.bazaarvoice.ostrich.metrics.notimplemented.NotImplementedMetrics;
import com.bazaarvoice.ostrich.registry.CoreZooKeeperServiceRegistry;
import com.google.common.annotations.VisibleForTesting;
import org.apache.curator.framework.CuratorFramework;

public class NoOpMetricsZookeeperServiceRegistry extends CoreZooKeeperServiceRegistry {

    public NoOpMetricsZookeeperServiceRegistry(CuratorFramework curator) {
        this(new NodeFactory(curator));
    }

    @VisibleForTesting NoOpMetricsZookeeperServiceRegistry(NodeFactory nodeFactory) {
        super(nodeFactory, NotImplementedMetrics.forClass(ZooKeeperServiceRegistry.class));
    }

}
