module SmartHome {

    exception DeviceError {
        string errorMessage;
    };

    exception DeviceNotFound {
        string errorMessage;
        string deviceId;
    };

    exception InvalidParameter {
        string parameter;
        string reason;
    };


    struct DeviceState {
        bool isOn;
        string statusMessage;
    };

    sequence<byte> photoSeq;

    sequence<string> stringSeq;

    interface Device {
        void turnOn()  throws DeviceError;
        void turnOff() throws DeviceError;
        DeviceState getState() throws DeviceError;
    };

    interface MonitoringCamera extends Device {
        photoSeq getSnapshot() throws DeviceError;
    };

    interface PTZCamera extends MonitoringCamera  {
        void pan(int degrees)  throws DeviceError, InvalidParameter;
        void tilt(int degrees) throws DeviceError, InvalidParameter;
        void zoom(int factor)  throws DeviceError, InvalidParameter;
    };

    interface NightVisionCamera extends MonitoringCamera {
        void changeIRMode() throws DeviceError;
    };

    interface WideAngleCamera extends MonitoringCamera {
        void setFieldOfView(int degrees) throws DeviceError, InvalidParameter;
        int getFieldOfView() throws DeviceError;
    };

    interface Thermostat extends Device {
        void setTemperature(float temperature) throws DeviceError, InvalidParameter;
        float getTemperature() throws DeviceError;
    };

    interface SmartPlug extends Device {
        float getPowerConsumption() throws DeviceError;
        void resetPowerConsumption() throws DeviceError;
    };

    interface PowerStripPlug extends SmartPlug {
        float getOutletConsumption(int index) throws DeviceError, InvalidParameter;
        void resetOutletConsumption(int index) throws DeviceError, InvalidParameter;
    };

    interface TimerPlug extends SmartPlug {
        void setAutoOff(int seconds) throws DeviceError, InvalidParameter;
        void cancelAutoOff() throws DeviceError;
    };

    interface DeviceManager {
        stringSeq listDevices();
        Device* getDevice(string id) throws DeviceNotFound;
        void addDevice(string id, Device* device) throws DeviceError;
    };
};