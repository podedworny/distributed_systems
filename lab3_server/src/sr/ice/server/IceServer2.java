package sr.ice.server;

import SmartHome.DevicePrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public class IceServer2 {
    public void t2(String[] args) {
        int status = 0;
        Communicator communicator = null;

        try {
            communicator = Util.initialize(args);

            ObjectAdapter adapter = communicator.createObjectAdapter("Adapter2");

            DeviceManagerImpl deviceManager = new DeviceManagerImpl();
            adapter.add(deviceManager, new Identity("deviceManager", "manager"));


            Identity identity = new Identity("thermostat", "thermostat");

            Identity identity2 = new Identity("smartPlug", "plug");
            Identity identity2_1 = new Identity("smartPlug2", "plug");
            Identity identity2_2 = new Identity("smartPlug3", "plug");
            Identity identity2_3 = new Identity("smartPlug4", "plug");
            Identity identity3 = new Identity("powerStripPlug", "plug");
            Identity identity3_1 = new Identity("powerStripPlug2", "plug");
            Identity identity3_2 = new Identity("powerStripPlug3", "plug");
            Identity identity4 = new Identity("timerPlug", "plug");

            adapter.add(new ThermostatImpl(), identity);

            adapter.add(new SmartPlugImpl(), identity2);
            adapter.add(new SmartPlugImpl(), identity2_1);
            adapter.add(new SmartPlugImpl(), identity2_2);
            adapter.add(new SmartPlugImpl(), identity2_3);
            adapter.add(new PowerStripPlugImpl(), identity3);
            adapter.add(new PowerStripPlugImpl(), identity3_1);
            adapter.add(new PowerStripPlugImpl(), identity3_2);
            adapter.add(new TimerPlugImpl(), identity4);


            DevicePrx thermostatPrx = DevicePrx.checkedCast(adapter.createProxy(identity));

            DevicePrx smartPlugPrx = DevicePrx.checkedCast(adapter.createProxy(identity2));
            DevicePrx smartPlug2Prx = DevicePrx.checkedCast(adapter.createProxy(identity2_1));
            DevicePrx smartPlug3Prx = DevicePrx.checkedCast(adapter.createProxy(identity2_2));
            DevicePrx smartPlug4Prx = DevicePrx.checkedCast(adapter.createProxy(identity2_3));
            DevicePrx powerStripPlugPrx = DevicePrx.checkedCast(adapter.createProxy(identity3));
            DevicePrx powerStripPlug2Prx = DevicePrx.checkedCast(adapter.createProxy(identity3_1));
            DevicePrx powerStripPlug3Prx = DevicePrx.checkedCast(adapter.createProxy(identity3_2));
            DevicePrx timerPlugPrx = DevicePrx.checkedCast(adapter.createProxy(identity4));


            deviceManager.addDevice("thermostat", thermostatPrx, null);

            deviceManager.addDevice("smartPlug", smartPlugPrx, null);
            deviceManager.addDevice("smartPlug2", smartPlug2Prx, null);
            deviceManager.addDevice("smartPlug3", smartPlug3Prx, null);
            deviceManager.addDevice("smartPlug4", smartPlug4Prx, null);
            deviceManager.addDevice("powerStripPlug", powerStripPlugPrx, null);
            deviceManager.addDevice("powerStripPlug2", powerStripPlug2Prx, null);
            deviceManager.addDevice("powerStripPlug3", powerStripPlug3Prx, null);
            deviceManager.addDevice("timerPlug", timerPlugPrx, null);

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
        IceServer2 app = new IceServer2();
        app.t2(args);
    }
}