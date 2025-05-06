package sr.ice.server;

import SmartHome.DeviceError;
import SmartHome.DeviceState;
import SmartHome.InvalidParameter;
import SmartHome.WideAngleCamera;
import com.zeroc.Ice.Current;

public class WideAngleCameraImpl implements WideAngleCamera {
    private int fieldOfView = 90;
    private final boolean working = true;
    private boolean state = false;
    @Override
    public void setFieldOfView(int degrees, Current current) throws DeviceError, InvalidParameter {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }
        if (degrees < 0 || degrees > 180) {
            InvalidParameter ex = new InvalidParameter();
            ex.reason = "Field of view must be between 0 and 180 degrees";
            ex.parameter = Integer.toString(degrees);
            throw ex;
        }
        this.fieldOfView = degrees;
    }

    @Override
    public int getFieldOfView(Current current) throws DeviceError {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }
        return fieldOfView;
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
        deviceState.statusMessage = "Camera is " + (state ? "on" : "off") + " with field of view " + fieldOfView + " degrees.";
        return deviceState;
    }
}
