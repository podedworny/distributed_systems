package sr.ice.server;

import SmartHome.DeviceError;
import SmartHome.DeviceState;
import SmartHome.InvalidParameter;
import SmartHome.Thermostat;
import com.zeroc.Ice.Current;

public class ThermostatImpl implements Thermostat {
    private final boolean working = true;
    private float setTemperature;
    private boolean state = false;
    @Override
    public void setTemperature(float temperature, Current current) throws DeviceError, InvalidParameter {
        if (!working) {
            throw new DeviceError("Thermostat is not working");
        }
        if (temperature < 0 || temperature > 30) {
            InvalidParameter ex = new InvalidParameter();
            ex.reason = "Temperature must be between 0 and 30";
            ex.parameter = Float.toString(temperature);
            throw ex;
        }
        this.setTemperature = temperature;
    }

    @Override
    public float getTemperature(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Thermostat is not working");
        }
        return setTemperature;
    }

    @Override
    public void turnOn(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Thermostat is not working");
        }
        state = true;
    }

    @Override
    public void turnOff(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Thermostat is not working");
        }
        state = false;

    }

    @Override
    public DeviceState getState(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Thermostat is not working");
        }
        DeviceState deviceState = new DeviceState();
        deviceState.isOn = state;
        deviceState.statusMessage = "Thermostat is " + (state ? "on" : "off") + " with set temperature " + setTemperature + ".";
        return deviceState;
    }
}
