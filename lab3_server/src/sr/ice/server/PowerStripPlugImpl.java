package sr.ice.server;

import SmartHome.DeviceError;
import SmartHome.DeviceState;
import SmartHome.InvalidParameter;
import SmartHome.PowerStripPlug;
import com.zeroc.Ice.Current;

import java.util.List;

public class PowerStripPlugImpl implements PowerStripPlug {
    private final boolean working = true;
    private boolean state = false;
    private List<Float> outletConsumptions = List.of(0.0f, 0.0f, 0.0f, 0.0f);
    @Override
    public float getOutletConsumption(int index, Current current) throws DeviceError, InvalidParameter {
        if (!working) {
            throw new DeviceError("PowerStripPlug is not working");
        }
        if (index < 0 || index >= outletConsumptions.size()) {
            InvalidParameter ex = new InvalidParameter();
            ex.reason = "Index out of bounds";
            ex.parameter = Integer.toString(index);
            throw ex;
        }
        outletConsumptions.set(index, (float) (Math.random() * 100));
        return outletConsumptions.get(index);
    }

    @Override
    public void resetOutletConsumption(int index, Current current) throws DeviceError, InvalidParameter {
        if (!working) {
            throw new DeviceError("PowerStripPlug is not working");
        }
        if (index < 0 || index >= outletConsumptions.size()) {
            InvalidParameter ex = new InvalidParameter();
            ex.reason = "Index out of bounds";
            ex.parameter = Integer.toString(index);
            throw ex;
        }
        outletConsumptions.set(index, 0.0f);
    }

    @Override
    public float getPowerConsumption(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("PowerStripPlug is not working");
        }
        outletConsumptions.replaceAll(ignored -> (float) (Math.random() * 100));
        return (float) outletConsumptions.stream().mapToDouble(Float::doubleValue).sum();
    }

    @Override
    public void resetPowerConsumption(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("PowerStripPlug is not working");
        }
        outletConsumptions.replaceAll(ignored -> 0.0f);

    }

    @Override
    public void turnOn(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("PowerStripPlug is not working");
        }
        state = true;

    }

    @Override
    public void turnOff(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("PowerStripPlug is not working");
        }
        state = false;

    }

    @Override
    public DeviceState getState(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("PowerStripPlug is not working");
        }
        DeviceState deviceState = new DeviceState();
        deviceState.isOn = state;
        deviceState.statusMessage = "PowerStripPlug is " + (state ? "on" : "off") + " with outlet consumptions " + outletConsumptions + ".";
        return deviceState;
    }
}
