package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.switching.Switch;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingTest {

    @Test
    void testPingSameSubnet() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
        Host h2 = new Host("h2", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 1, 2), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
        topology.addHost(h1);
        topology.addHost(h2);
        // Connect hosts directly (via Connection) - for same-subnet we don't need router
        topology.addConnection(new Connection(h1.getHostInterface(), h2.getHostInterface()));

        PingStatistics stats = h1.ping("192.168.1.2", topology);
        assertEquals(4, stats.getSent());
        assertEquals(4, stats.getReceived());
        assertEquals(0.0, stats.getLossPercent());
    }

    @Test
    void testPingViaRouter() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 1, 2), new SubnetMask(24)), new IPAddress(192, 168, 1, 1)));
        Host h2 = new Host("h2", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 2, 2), new SubnetMask(24)), new IPAddress(192, 168, 2, 1)));

        Router r = new Router("R1", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
        // configure router interfaces
        r.setMode(RouterMode.CONFIGURATION);
        r.configureInterface("eth0", InterfaceAddress.fromString("192.168.1.1/24"));
        r.configureInterface("eth1", InterfaceAddress.fromString("192.168.2.1/24"));
        r.commitChanges();

        topology.addHost(h1);
        topology.addHost(h2);
        topology.addRouter(r);

        // Connect h1 <-> r.eth0 and h2 <-> r.eth1
        topology.addConnection(new Connection(h1.getHostInterface(), r.getInterfaces().get(0)));
        topology.addConnection(new Connection(h2.getHostInterface(), r.getInterfaces().get(1)));

        // Add static routes (router has connected routes automatically via interfaces)

        PingStatistics stats = h1.ping("192.168.2.2", topology);
        assertEquals(4, stats.getSent());
        assertEquals(4, stats.getReceived());
    }

    @Test
    void testPingViaRouterAndSwitch() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 1, 2), new SubnetMask(24)), new IPAddress(192, 168, 1, 1)));
        Host h2 = new Host("h2", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 2, 2), new SubnetMask(24)), new IPAddress(192, 168, 2, 1)));

        Switch sw1 = new Switch("SW1");
        // Register switch in topology so topology traversal can treat it as a hub
        topology.addSwitch(sw1);

        Router r = new Router("R1", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
        // configure router interfaces
        r.setMode(RouterMode.CONFIGURATION);
        r.configureInterface("eth0", InterfaceAddress.fromString("192.168.1.1/24"));
        r.configureInterface("eth1", InterfaceAddress.fromString("192.168.2.1/24"));
        r.commitChanges();

        topology.addHost(h1);
        topology.addHost(h2);
        topology.addRouter(r);

        // Connect h1 <-> r.eth0 and h2 <-> r.eth1
        topology.addConnection(new Connection(h1.getHostInterface(), r.getInterfaces().get(0)));
        topology.addConnection(new Connection(r.getInterfaces().get(1), sw1.getPorts().getFirst()));
        topology.addConnection(new Connection(sw1.getPorts().get(1), h2.getHostInterface()));


        // Add static routes (router has connected routes automatically via interfaces)

        PingStatistics stats = h1.ping("192.168.2.2", topology);
        assertEquals(4, stats.getSent());
        assertEquals(4, stats.getReceived());
    }

    @Test
    void testPingBasicRouting() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
        Host h2 = new Host("h2", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 3, 1), new SubnetMask(24)), new IPAddress(192, 168, 3, 254)));

        Router r1 = new Router("R1", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
        Router r2 = new Router("R2", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
        // configure router interfaces
        r1.setMode(RouterMode.CONFIGURATION);
        r1.configureInterface("eth0", InterfaceAddress.fromString("192.168.1.254/24"));
        r1.configureInterface("eth1", InterfaceAddress.fromString("192.168.2.1/24"));
        r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 3, 0), new SubnetMask(24)), r1.findFromName("eth1")));
        r1.commitChanges();

        r2.setMode(RouterMode.CONFIGURATION);
        r2.configureInterface("eth0", InterfaceAddress.fromString("192.168.2.2/24"));
        r2.configureInterface("eth1", InterfaceAddress.fromString("192.168.3.254/24"));
        r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), r2.findFromName("eth0")));
        r2.commitChanges();

        topology.addHost(h1);
        topology.addHost(h2);
        topology.addRouter(r1);
        topology.addRouter(r2);

        // H1 -> R1 <-> R2 -> H2
        topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
        topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
        topology.addConnection(new Connection(r2.getInterfaces().get(1), h2.getHostInterface()));

        PingStatistics stats = h1.ping("192.168.3.1", topology);
        assertEquals(4, stats.getSent());
        assertEquals(4, stats.getReceived());
    }

    @Test
    void testPingViaRouterUnknownHost() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 1, 2), new SubnetMask(24)), new IPAddress(192, 168, 1, 1)));
        Host h2 = new Host("h2", new HostInterface("eth0", new Subnet(new IPAddress(192, 168, 2, 2), new SubnetMask(24)), new IPAddress(192, 168, 2, 1)));

        Router r = new Router("R1", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
        // configure router interfaces
        r.setMode(RouterMode.CONFIGURATION);
        r.configureInterface("eth0", InterfaceAddress.fromString("192.168.1.1/24"));
        r.configureInterface("eth1", InterfaceAddress.fromString("192.168.2.1/24"));
        r.commitChanges();

        topology.addHost(h1);
        topology.addHost(h2);
        topology.addRouter(r);

        // Connect h1 <-> r.eth0 and h2 <-> r.eth1
        topology.addConnection(new Connection(h1.getHostInterface(), r.getInterfaces().get(0)));
        topology.addConnection(new Connection(h2.getHostInterface(), r.getInterfaces().get(1)));

        // Add static routes (router has connected routes automatically via interfaces)

        PingStatistics stats = h1.ping("192.168.2.3", topology);
        assertEquals(4, stats.getSent());
        // This subnet exists but no host with .3, so we expect 0 received
        assertEquals(0, stats.getReceived());
    }

    @Test
    void testPingUnreachable() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new Subnet(new IPAddress(10, 0, 0, 1), new SubnetMask(24)), new IPAddress(10, 0, 0, 254)));
        topology.addHost(h1);

        PingStatistics stats = h1.ping("192.168.99.1", topology);
        assertEquals(4, stats.getSent());
        assertEquals(0, stats.getReceived());
    }
}
