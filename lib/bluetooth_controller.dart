import 'package:bluetooth/device.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class BluetoothController {
  final MethodChannel channel = MethodChannel('bluetooth_channel');

  Future<bool> checkStatus() async {
    try {
      return await channel.invokeMethod('checkBluetoothStatus');
    } on PlatformException catch (e, s) {
      debugPrint('Platform error caught: $e\n$s');
      return false;
    } catch (e, s) {
      debugPrint('Error caught: $e\n$s');
      return false;
    }
  }

  Future<List<Device>> scan() async {
    try {
      debugPrint('Scanning...');
      final List<dynamic> result = await channel.invokeMethod('scanForDevices');
      final List<Device> devices = result.map((e) {
        final Map<Object?, Object?> map = e as Map<Object?, Object?>;
        return Device(map['name'].toString(), map['address'].toString());
      }).toList();
      debugPrint('Scan complete: $devices');
      return devices;
    } on PlatformException catch (e, s) {
      debugPrint('Platform error caught: $e\n$s');
      return [];
    } catch (e, s) {
      debugPrint('Error caught: $e\n$s');
      return [];
    }
  }

  Future<bool> connect(String address) async {
    try {
      return await channel
          .invokeMethod('connectToDevice', {'address': address});
    } on PlatformException catch (e, s) {
      debugPrint('Platform error caught: $e\n$s');
      return false;
    } catch (e, s) {
      debugPrint('Error caught: $e\n$s');
      return false;
    }
  }
}
