package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.*;
import org.uj.routingemulator.switching.Switch;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingTest {
    private final RouterConfigurationService routerConfigurationService = new RouterConfigurationService();

    @Test
    void testPingSameSubnet() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
        Host h2 = new Host("h2", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 2), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
        topology.addDevice(h1);
        topology.addDevice(h2);
        topology.addConnection(new Connection(h1.getHostInterface(), h2.getHostInterface()));
        PingStatistics stats = new PingService().ping(h1, "192.168.1.2", 4, topology);
        assertEquals(4, stats.getSent());
        assertEquals(4, stats.getReceived());
        assertEquals(0.0, stats.getLossPercent());
    }

    @Test
    void testPingViaRouter() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 2), new SubnetMask(24)), new IPAddress(192, 168, 1, 1)));
        Host h2 = new Host("h2", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 2, 2), new SubnetMask(24)), new IPAddress(192, 168, 2, 1)));
        Router r = new Router("R1", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
        RouterModeController.setMode(r, RouterMode.CONFIGURATION);
        routerConfigurationService.configureInterface(r, "eth0", InterfaceAddress.fromString("192.168.1.1/24"));
        routerConfigurationService.configureInterface(r, "eth1", InterfaceAddress.fromString("192.168.2.1/24"));
        r.getConfigSession().commit();

        topology.addDevice(h1);
        topology.addDevice(h2);
        topology.addDevice(r);
        topology.addConnection(new Connection(h1.getHostInterface(), r.getInterfaces().get(0)));
        topology.addConnection(new Connection(h2.getHostInterface(), r.getInterfaces().get(1)));
        PingStatistics stats = new PingService().ping(h1, "192.168.2.2", 4, topology);
        assertEquals(4, stats.getSent());
        assertEquals(4, stats.getReceived());
    }

    @Test
    void testPingViaRouterAndSwitch() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 2), new SubnetMask(24)), new IPAddress(192, 168, 1, 1)));
        Host h2 = new Host("h2", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 2, 2), new SubnetMask(24)), new IPAddress(192, 168, 2, 1)));
        Switch sw1 = new Switch("SW1");
        topology.addDevice(sw1);
        Router r = new Router("R1", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
        RouterModeController.setMode(r, RouterMode.CONFIGURATION);
        routerConfigurationService.configureInterface(r, "eth0", InterfaceAddress.fromString("192.168.1.1/24"));
        routerConfigurationService.configureInterface(r, "eth1", InterfaceAddress.fromString("192.168.2.1/24"));
        r.getConfigSession().commit();

        topology.addDevice(h1);
        topology.addDevice(h2);
        topology.addDevice(r);
        topology.addConnection(new Connection(h1.getHostInterface(), r.getInterfaces().get(0)));
        topology.addConnection(new Connection(r.getInterfaces().get(1), sw1.getPorts().getFirst()));
        topology.addConnection(new Connection(sw1.getPorts().get(1), h2.getHostInterface()));
        PingStatistics stats = new PingService().ping(h1, "192.168.2.2", 4, topology);
        assertEquals(4, stats.getSent());
        assertEquals(4, stats.getReceived());
    }

    @Test
    void testPingBasicRouting() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
        Host h2 = new Host("h2", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 3, 1), new SubnetMask(24)), new IPAddress(192, 168, 3, 254)));
        Router r1 = new Router("R1", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
        Router r2 = new Router("R2", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

        RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
        routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("192.168.1.254/24"));
        routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.2.1/24"));
        routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 3, 0), new SubnetMask(24)), r1.findFromName("eth1")));
        r1.getConfigSession().commit();

        RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
        routerConfigurationService.configureInterface(r2, "eth0", InterfaceAddress.fromString("192.168.2.2/24"));
        routerConfigurationService.configureInterface(r2, "eth1", InterfaceAddress.fromString("192.168.3.254/24"));
        routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), r2.findFromName("eth0")));
        r2.getConfigSession().commit();

        topology.addDevice(h1);
        topology.addDevice(h2);
        topology.addDevice(r1);
        topology.addDevice(r2);
        topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
        topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
        topology.addConnection(new Connection(r2.getInterfaces().get(1), h2.getHostInterface()));
        PingStatistics stats = new PingService().ping(h1, "192.168.3.1", 4, topology);
        assertEquals(4, stats.getSent());
        assertEquals(4, stats.getReceived());
    }

    @Test
    void testPingViaRouterUnknownHost() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 2), new SubnetMask(24)), new IPAddress(192, 168, 1, 1)));
        Host h2 = new Host("h2", new HostInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 2, 2), new SubnetMask(24)), new IPAddress(192, 168, 2, 1)));
        Router r = new Router("R1", java.util.List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

        RouterModeController.setMode(r, RouterMode.CONFIGURATION);
        routerConfigurationService.configureInterface(r, "eth0", InterfaceAddress.fromString("192.168.1.1/24"));
        routerConfigurationService.configureInterface(r, "eth1", InterfaceAddress.fromString("192.168.2.1/24"));
        r.getConfigSession().commit();

        topology.addDevice(h1);
        topology.addDevice(h2);
        topology.addDevice(r);
        topology.addConnection(new Connection(h1.getHostInterface(), r.getInterfaces().get(0)));
        topology.addConnection(new Connection(h2.getHostInterface(), r.getInterfaces().get(1)));
        PingStatistics stats = new PingService().ping(h1, "192.168.2.3", 4, topology);
        assertEquals(4, stats.getSent());
        assertEquals(0, stats.getReceived());
    }

    @Test
    void testPingUnreachable() {
        NetworkTopology topology = new NetworkTopology();
        Host h1 = new Host("h1", new HostInterface("eth0", new InterfaceAddress(new IPAddress(10, 0, 0, 1), new SubnetMask(24)), new IPAddress(10, 0, 0, 254)));
        topology.addDevice(h1);
        PingStatistics stats = new PingService().ping(h1, "192.168.99.1", 4, topology);
        assertEquals(4, stats.getSent());
        assertEquals(0, stats.getReceived());
    }
}