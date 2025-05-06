package sr.ice.server;

import SmartHome.DevicePrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public class IceServer {
	public void t1(String[] args) {
		int status = 0;
		Communicator communicator = null;

		try {
			communicator = Util.initialize(args);

			ObjectAdapter adapter = communicator.createObjectAdapter("Adapter1");

			DeviceManagerImpl deviceManager = new DeviceManagerImpl();
			adapter.add(deviceManager, new Identity("deviceManager", "manager"));


			Identity identity = new Identity("monitoringCamera", "camera");
			Identity identity_1 = new Identity("monitoringCamera2", "camera");
			Identity identity2 = new Identity("ptzCamera", "camera");
			Identity identity3 = new Identity("wideAngleCamera", "camera");
			Identity identity3_1 = new Identity("wideAngleCamera2", "camera");
			Identity identity4 = new Identity("nightVisionCamera", "camera");


			adapter.add(new MonitoringCameraImpl(), identity);
			adapter.add(new MonitoringCameraImpl(), identity_1);
			adapter.add(new PTZCameraImpl(), identity2);
			adapter.add(new WideAngleCameraImpl(), identity3);
			adapter.add(new WideAngleCameraImpl(), identity3_1);
			adapter.add(new NightVisionCameraImpl(), identity4);



			DevicePrx monitoringCameraPrx = DevicePrx.checkedCast(adapter.createProxy(identity));
			DevicePrx monitoringCamera2Prx = DevicePrx.checkedCast(adapter.createProxy(identity_1));
			DevicePrx ptzCameraPrx = DevicePrx.checkedCast(adapter.createProxy(identity2));
			DevicePrx wideAngleCameraPrx = DevicePrx.checkedCast(adapter.createProxy(identity3));
			DevicePrx wideAngleCamera2Prx = DevicePrx.checkedCast(adapter.createProxy(identity3_1));
			DevicePrx nightVisionCameraPrx = DevicePrx.checkedCast(adapter.createProxy(identity4));



			deviceManager.addDevice("monitoringCamera", monitoringCameraPrx, null);
			deviceManager.addDevice("monitoringCamera2", monitoringCamera2Prx, null);
			deviceManager.addDevice("ptzCamera", ptzCameraPrx, null);
			deviceManager.addDevice("wideAngleCamera", wideAngleCameraPrx, null);
			deviceManager.addDevice("wideAngleCamera2", wideAngleCamera2Prx, null);
			deviceManager.addDevice("nightVisionCamera", nightVisionCameraPrx, null);



			adapter.activate();

			System.out.println("Entering event processing loop...");

			communicator.waitForShutdown();

		} catch (Exception e) {
			e.printStackTrace(System.err);
			status = 1;
		}
		if (communicator != null) {
			try {
				communicator.destroy();
			} catch (Exception e) {
				e.printStackTrace(System.err);
				status = 1;
			}
		}
		System.exit(status);
	}


	public static void main(String[] args) {
		IceServer app = new IceServer();
		app.t1(args);
	}
}