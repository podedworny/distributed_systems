package sr.ice.server;

import SmartHome.*;
import com.zeroc.Ice.Current;

import java.util.HashMap;
import java.util.Map;

public class DeviceManagerImpl implements DeviceManager {
    private final Map<String, DevicePrx> devices = new HashMap<>();
    @Override
    public String[] listDevices(Current current) {
        return devices.keySet().toArray(new String[0]);
    }

    @Override
    public DevicePrx getDevice(String id, Current current) throws DeviceNotFound {
        DevicePrx device = devices.get(id);
        if (device == null) {
            DeviceNotFound deviceNotFound = new DeviceNotFound();
            deviceNotFound.deviceId = id;
            deviceNotFound.errorMessage = "Device not found";
            throw deviceNotFound;
        }
        return device;
    }

    @Override
    public void addDevice(String id, DevicePrx device, Current current) throws DeviceError {
        if (devices.containsKey(id)) {
            DeviceError deviceError = new DeviceError();
            deviceError.errorMessage = "Device already exists";
            throw deviceError;
        }
        devices.put(id, device);
    }
}
