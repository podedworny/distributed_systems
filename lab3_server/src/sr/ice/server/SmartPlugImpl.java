package sr.ice.server;

import SmartHome.DeviceError;
import SmartHome.DeviceState;
import SmartHome.SmartPlug;
import com.zeroc.Ice.Current;

public class SmartPlugImpl implements SmartPlug {
    private final boolean working = true;
    private boolean state = false;
    private float powerConsumption = 0.0f;
    @Override
    public float getPowerConsumption(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("SmartPlug is not working");
        }
        powerConsumption = (float) (Math.random() * 100);
        return powerConsumption;
    }

    @Override
    public void resetPowerConsumption(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("SmartPlug is not working");
        }
        powerConsumption = 0.0f;

    }

    @Override
    public void turnOn(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("SmartPlug is not working");
        }
        state = true;
    }

    @Override
    public void turnOff(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("SmartPlug is not working");
        }
        state = false;
    }

    @Override
    public DeviceState getState(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("SmartPlug is not working");
        }
        DeviceState deviceState = new DeviceState();
        deviceState.isOn = state;
        deviceState.statusMessage = "SmartPlug is " + (state ? "on" : "off") + " with power consumption " + powerConsumption + ".";
        return deviceState;
    }
}
