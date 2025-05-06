package sr.ice.server;

import SmartHome.DeviceError;
import SmartHome.DeviceState;
import SmartHome.InvalidParameter;
import SmartHome.TimerPlug;
import com.zeroc.Ice.Current;

public class TimerPlugImpl implements TimerPlug {
    private final boolean working = true;
    private boolean state = false;
    private int autoOffTime = 0;
    private int powerConsumption = 0;
    @Override
    public void setAutoOff(int seconds, Current current) throws DeviceError, InvalidParameter {
        if (!working) {
            throw new DeviceError("Device is not working");
        }
        if (seconds < 0) {
            InvalidParameter invalidParameter = new InvalidParameter();
            invalidParameter.reason = "Time must be a positive integer";
            invalidParameter.parameter = String.valueOf(seconds);
        }
        this.autoOffTime = seconds;

    }

    @Override
    public void cancelAutoOff(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Device is not working");
        }
        this.autoOffTime = 0;
    }

    @Override
    public float getPowerConsumption(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Device is not working");
        }
        return powerConsumption;
    }

    @Override
    public void resetPowerConsumption(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Device is not working");
        }
        this.powerConsumption = 0;
    }

    @Override
    public void turnOn(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Device is not working");
        }
        this.state = true;
    }

    @Override
    public void turnOff(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Device is not working");
        }
        this.state = false;
    }

    @Override
    public DeviceState getState(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Device is not working");
        }
        DeviceState deviceState = new DeviceState();
        deviceState.isOn = state;
        deviceState.statusMessage = "Device is " + (state ? "on" : "off") + ". Auto-off time: " + autoOffTime + " seconds.";
        return deviceState;
    }
}
