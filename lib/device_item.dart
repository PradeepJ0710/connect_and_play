import 'package:bluetooth/device.dart';
import 'package:flutter/material.dart';

class DeviceItem extends StatelessWidget {
  final Device device;
  final VoidCallback onTap;
  final bool isConnected;

  const DeviceItem(
      {required this.device,
      required this.onTap,
      required this.isConnected,
      super.key});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(device.name),
      subtitle: Text(device.address),
      trailing: Icon(isConnected ? Icons.check : Icons.add),
      onTap: onTap,
    );
  }
}
