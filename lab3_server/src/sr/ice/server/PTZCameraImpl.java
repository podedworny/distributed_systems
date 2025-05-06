package sr.ice.server;

import SmartHome.DeviceError;
import SmartHome.DeviceState;
import SmartHome.InvalidParameter;
import SmartHome.PTZCamera;
import com.zeroc.Ice.Current;

public class PTZCameraImpl implements PTZCamera {
    private int pan = 0;
    private int tilt = 0;
    private int zoom = 0;
    private boolean state = false;
    private boolean working = true;
    @Override
    public void pan(int degrees, Current current) throws DeviceError, InvalidParameter {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }

        if (degrees < -180 || degrees > 180) {
            InvalidParameter ex = new InvalidParameter();
            ex.reason = "Pan degrees must be between -180 and 180";
            ex.parameter = Integer.toString(degrees);
            throw ex;
        }
        this.pan = degrees;
    }

    @Override
    public void tilt(int degrees, Current current) throws DeviceError, InvalidParameter {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }

        if (degrees < -90 || degrees > 90) {
            InvalidParameter ex = new InvalidParameter();
            ex.reason = "Tilt degrees must be between -90 and 90";
            ex.parameter = Integer.toString(degrees);
            throw ex;
        }
        this.tilt = degrees;

    }

    @Override
    public void zoom(int factor, Current current) throws DeviceError, InvalidParameter {
        if (!working) {
            throw new DeviceError("Camera is not working");
        }

        if (factor < 1 || factor > 10) {
            InvalidParameter ex = new InvalidParameter();
            ex.reason = "Zoom factor must be between 1 and 10";
            ex.parameter = Integer.toString(factor);
            throw ex;
        }
        this.zoom = factor;

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
        deviceState.statusMessage = "Camera is " + (state ? "on" : "off") + " with pan: " + pan + " degrees, tilt: " + tilt + " degrees, and zoom: " + zoom + ".";
        return deviceState;
    }
}
