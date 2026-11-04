package org.uj.routingemulator.gui;

import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.NetworkInterface;
import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkTopologyControllerTest {

	@Test
	void cableChoicesExcludeVifDummyAndLoopbackRouterInterfaces() throws ReflectiveOperationException {
		RouterInterface ethernet = new RouterInterface("eth0");
		Router router = new Router("R1", List.of(
				ethernet,
				new RouterInterface("eth0.10"),
				new RouterInterface("dum0"),
				new RouterInterface("lo")
		));
		NetworkTopologyController controller = new NetworkTopologyController();

		Field topologyField = NetworkTopologyController.class.getDeclaredField("topology");
		topologyField.setAccessible(true);
		topologyField.set(controller, new NetworkTopology());

		Method getAvailableInterfaces = NetworkTopologyController.class
				.getDeclaredMethod("getAvailableInterfaces", Object.class);
		getAvailableInterfaces.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<NetworkInterface> available = (List<NetworkInterface>) getAvailableInterfaces.invoke(controller, router);

		assertEquals(List.of(ethernet), available);
	}
}