package com.bazaarvoice.ostrich.registry.zookeeper;

import com.bazaarvoice.ostrich.metrics.yammer.YammerMetrics;
import com.bazaarvoice.ostrich.registry.CoreZooKeeperServiceRegistry;
import com.google.common.annotations.VisibleForTesting;
import org.apache.curator.framework.CuratorFramework;

/**
 * A <code>ServiceRegistry</code> implementation that uses ZooKeeper as its backing data store.
 */
public class ZooKeeperServiceRegistry extends CoreZooKeeperServiceRegistry {


    public ZooKeeperServiceRegistry(CuratorFramework curator) {
        this(new NodeFactory(curator));
    }

    @VisibleForTesting ZooKeeperServiceRegistry(NodeFactory nodeFactory) {
        super(nodeFactory, YammerMetrics.forClass(ZooKeeperServiceRegistry.class));
    }

}
