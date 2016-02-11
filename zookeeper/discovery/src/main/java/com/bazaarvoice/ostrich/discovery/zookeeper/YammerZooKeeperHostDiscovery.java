package com.bazaarvoice.ostrich.discovery.zookeeper;

import com.bazaarvoice.ostrich.discovery.CoreZooKeeperHostDiscovery;
import com.bazaarvoice.ostrich.spi.Metrics;
import com.google.common.annotations.VisibleForTesting;
import org.apache.curator.framework.CuratorFramework;

/**
 * The <code>HostDiscovery</code> class encapsulates a ZooKeeper backed NodeDiscovery which watches a specific service
 * path in ZooKeeper and will monitor which end points are known to exist.  As end pionts come and go the results of
 * calling the {@link #getHosts} method change.
 */
public class YammerZooKeeperHostDiscovery extends CoreZooKeeperHostDiscovery {

    public YammerZooKeeperHostDiscovery(CuratorFramework curator, String serviceName, Metrics metrics) {
        this(new NodeDiscoveryFactory(), curator, serviceName, metrics);
    }

    @VisibleForTesting YammerZooKeeperHostDiscovery(NodeDiscoveryFactory factory, CuratorFramework curator, String serviceName, Metrics metrics) {
        super(factory, curator, serviceName, metrics);
    }

}
