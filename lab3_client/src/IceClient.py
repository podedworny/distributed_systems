import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../generated')))
from SmartHome import *


class SmartHomeClient:
    def __init__(self, communicator):
        self.communicator = communicator
        self.device_managers = {}
        self.device_to_manager = {}
        servers = {
            "Server1": "manager/deviceManager:tcp -h 127.0.0.2 -p 10000 -z : udp -h 127.0.0.2 -p 10000 -z",
            "Server2": "manager/deviceManager:tcp -h 127.0.0.2 -p 10001 -z : udp -h 127.0.0.2 -p 10001 -z"
        }
        self.connect_to_servers(servers)
        self.get_list_of_devices()


    def connect_to_servers(self, server_configs):
        for name, proxy_str in server_configs.items():
            try:
                proxy = self.communicator.stringToProxy(proxy_str)
                device_manager = DeviceManagerPrx.checkedCast(proxy)
                if not device_manager:
                    print(f"Failed to get proxy for server {name}")
                    continue
                self.device_managers[name] = device_manager
                print(f"Connected to server {name}")
            except Ice.Exception as e:
                print(f"Error connecting to server {name}: {e}")

    def get_list_of_devices(self):
        for server_name, manager in self.device_managers.items():
            try:
                device_ids = manager.listDevices()
                for device_id in device_ids:
                    self.device_to_manager[device_id] = server_name
            except Ice.Exception as e:
                print(f"Error fetching device list from {server_name}: {e}")

    def list_devices(self):
        all_devices = []
        device_index = 1

        for server_name, manager in self.device_managers.items():
            try:
                device_ids = manager.listDevices()
                print(f"\nUrządzenia na serwerze {server_name}:")

                for device_id in device_ids:
                    print(f"{device_index}. {device_id}")
                    all_devices.append(device_id)
                    device_index += 1

            except Ice.Exception as e:
                print(f"Błąd podczas pobierania listy urządzeń z {server_name}: {e}")

        return all_devices

    def get_device(self, device_id):
        try:
            if device_id not in self.device_to_manager:
                print(f"Nie znaleziono urządzenia o ID: {device_id}")
                return None

            server_name = self.device_to_manager[device_id]
            manager = self.device_managers[server_name]

            device = manager.getDevice(device_id)
            if not device:
                raise Exception(f"Nie znaleziono urządzenia o ID: {device_id}")
            return device

        except Ice.Exception as e:
            print(f"Błąd podczas pobierania urządzenia: {e}")
            return None

    def run(self):
        print("Wybierz opcję:")
        print("list -  Lista urządzeń")
        print("<device_name> <function> <parameters> - wywołanie funkcji na urządzeniu")
        print("quit - Wyjdź")
        while True:
            choice = input("==>: ").strip().split()

            op = choice[0]

            if op == "list":
                self.list_devices()
                continue
            elif op == "quit":
                print("Zamykam klienta...")
                break

            if len(choice) < 2:
                print("Niepoprawne dane wejściowe.")
                continue

            device = self.get_device(choice[0])
            if not device:
                print("Niepoprawne ID urządzenia.")
                continue

            func = choice[1]

            if func in ("on", "off", "state"):
                try:
                    if func == "on":
                        device.turnOn()
                        print(f"Urządzenie {choice[0]} włączone.")
                    elif func == "off":
                        device.turnOff()
                        print(f"Urządzenie {choice[0]} wyłączone.")
                    elif func == "state":
                        state = device.getState()
                        print(f"Stan urządzenia {choice[0]}: {state}")
                except DeviceError as e:
                    print(f"Błąd podczas wywoływania funkcji: {e}")

            elif func == "snapshot":
                cam = MonitoringCameraPrx.checkedCast(device)
                if not cam:
                    print("Niepoprawne urządzenie kamery.")
                    continue

                try:
                    data = cam.getSnapshot()
                    print(f"Zrzut ekranu z kamery {choice[0]}:\n{data}")
                except DeviceError as e:
                    print(f"Błąd podczas pobierania zrzutu ekranu: {e}")

            elif func in ("pan", "tilt", "zoom"):
                if len(choice) < 3:
                    print("Niepoprawne dane wejściowe.")
                    continue

                val = int(choice[2])
                ptz = PTZCameraPrx.checkedCast(device)
                if not ptz:
                    print("Niepoprawne urządzenie kamery.")
                    continue

                try:
                    if func == "pan":
                        ptz.pan(val)
                        print(f"Pan kamery {choice[0]} ustawiony na {val}.")
                    elif func == "tilt":
                        ptz.tilt(val)
                        print(f"Tilt kamery {choice[0]} ustawiony na {val}.")
                    elif func == "zoom":
                        ptz.zoom(val)
                        print(f"Zoom kamery {choice[0]} ustawiony na {val}.")
                except InvalidParameter as e:
                    print("Zły parametr.", e.parameter, e.reason)
                except DeviceError as e:
                    print(f"Błąd podczas ustawiania kamery: {e}")

            elif func == "changeirmode":
                nv = NightVisionCameraPrx.checkedCast(device)

                if not nv:
                    print("Niepoprawne urządzenie kamery.")
                    continue

                try:
                    nv.changeIRMode()
                    print(f"Tryb IR kamery {choice[0]} zmieniony.")
                except DeviceError as e:
                    print(f"Błąd podczas zmiany trybu IR: {e}")

            elif func in ("fovset", "fovget"):
                fov = WideAngleCameraPrx.checkedCast(device)

                if not fov:
                    print("Niepoprawne urządzenie kamery.")
                    continue

                try:
                    if op == "fovset":
                        val = int(choice[2])
                        fov.setFieldOfView(val)
                        print(f"Ustawiono FOV kamery {choice[0]} na {val}.")
                    elif op == "fovget":
                        val = fov.getFieldOfView()
                        print(f"FOV kamery {choice[0]}: {val}.")
                except InvalidParameter as e:
                    print("Zły parametr.", e.parameter, e.reason)
                except DeviceError as e:
                    print(f"Błąd podczas ustawiania FOV: {e}")

            elif func in ("settemp", "gettemp"):
                th = ThermostatPrx.checkedCast(device)

                if not th:
                    print("Niepoprawne urządzenie termostatu.")
                    continue

                try:
                    if func == "settemp":
                        if len(choice) < 3:
                            print("Niepoprawne dane wejściowe.")
                            continue
                        val = int(choice[2])
                        th.setTemperature(val)
                        print(f"Ustawiono temperaturę termostatu {choice[0]} na {val}.")
                    elif func == "gettemp":
                        val = th.getTemperature()
                        print(f"Temperatura termostatu {choice[0]}: {val}.")
                except InvalidParameter as e:
                    print("Zły parametr.", e.parameter, e.reason)
                except DeviceError as e:
                    print(f"Błąd podczas ustawiania temperatury: {e}")

            elif func in ("power", "reset"):
                sp = SmartPlugPrx.checkedCast(device)
                if not sp:
                    print("Niepoprawne urządzenie smart plug.")
                    continue

                try:
                    if func == "power":
                        powerConsumption = sp.getPowerConsumption()
                        print(f"Zużycie energii przez smart plug {choice[0]}: {powerConsumption} W.")
                    elif func == "reset":
                        sp.resetPowerConsumption()
                        print(f"Smart plug {choice[0]} zresetowany.")
                except DeviceError as e:
                    print(f"Błąd podczas pobierania zużycia energii: {e}")

            elif func in ("outlet", "resetoutlet"):
                ps = PowerStripPlugPrx.checkedCast(device)
                if not ps:
                    print("Niepoprawne urządzenie power strip.")
                    continue

                if len(choice) < 3:
                    print("Niepoprawne dane wejściowe.")
                    continue

                idx = int(choice[2])

                try:
                    if func == "outlet":
                        state = ps.getOutletConsumption(idx)
                        print(f"Stan gniazdka {idx} w power stripie {choice[0]}: {state}.")
                    elif func == "resetoutlet":
                        ps.resetOutletConsumption(idx)
                        print(f"Gniazdko {idx} w power stripie {choice[0]} zresetowane.")
                except InvalidParameter as e:
                    print("Zły parametr.", e.parameter, e.reason)
                except DeviceError as e:
                    print(f"Błąd podczas pobierania stanu gniazdka: {e}")

            elif func in ("autooff", "canceloff"):
                tp = TimerPlugPrx.checkedCast(device)

                if not tp:
                    print("Niepoprawne urządzenie timer plug.")
                    continue

                try:
                    if op == "autooff":
                        if len(choice) < 3:
                            print("Niepoprawne dane wejściowe.")
                            continue
                        tp.setAutoOff(int(choice[2]))
                        print(f"Ustawiono automatyczne wyłączenie timer plug {choice[0]}.")
                    elif op == "canceloff":
                        tp.cancelAutoOff()
                        print(f"Anulowano automatyczne wyłączenie timer plug {choice[0]}.")
                except InvalidParameter as e:
                    print("Zły parametr.", e.parameter, e.reason)
                except DeviceError as e:
                    print(f"Błąd podczas ustawiania automatycznego wyłączenia: {e}")

            else:
                print("Niepoprawna funkcja.")


def main():
    try:
        communicator = Ice.initialize(sys.argv)
        client = SmartHomeClient(communicator)


        client.run()

    except Ice.Exception as e:
        print(f"Błąd: {e}")
    finally:
        communicator.destroy()

if __name__ == "__main__":
    main()