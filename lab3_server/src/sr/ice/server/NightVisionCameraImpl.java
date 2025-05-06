package sr.ice.server;

import SmartHome.DeviceError;
import SmartHome.DeviceState;
import SmartHome.NightVisionCamera;
import com.zeroc.Ice.Current;

public class NightVisionCameraImpl implements NightVisionCamera {
    private boolean state = false;
    private boolean working = true;
    private boolean irMode = false;

    @Override
    public void changeIRMode(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }
        irMode = !irMode;

    }

    @Override
    public byte[] getSnapshot(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }
        String snapshotData = "SampleSnapshot";
        return snapshotData.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    @Override
    public void turnOn(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }
        state = true;
    }

    @Override
    public void turnOff(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }
        state = false;
    }

    @Override
    public DeviceState getState(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }
        DeviceState deviceState = new DeviceState();
        deviceState.isOn = state;
        deviceState.statusMessage = "Camera is " + (state ? "on" : "off") + " with IR mode " + (irMode ? "enabled" : "disabled") + ".";
        return deviceState;
    }
}
