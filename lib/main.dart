import 'package:bluetooth/bluetooth_controller.dart';
import 'package:bluetooth/device.dart';
import 'package:bluetooth/device_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:just_audio/just_audio.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Connect & Play'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final BluetoothController _bCtrl = BluetoothController();
  bool _bluetoothStatus = false;
  List<Device> _listOfDevices = [];
  final List<Device> _connectedDevices = [];
  final AudioPlayer _player = AudioPlayer();

  Future<void> _connectDevice(Device device) async {
    debugPrint('Connecting device: ${device.address}');
    final bool connected = await _bCtrl.connect(device.address);
    if (connected) {
      _player.setAudioSources(<AudioSource>[AudioSource.asset('assets/qa.mp3')]);
      _player.setLoopMode(LoopMode.one);
      setState(() {
        _connectedDevices.add(device);
      });
    }
  }

  Future<void> _checkBluetoothStatus() async {
    debugPrint('Checking bluetooth connection status');
    final bool status = await _bCtrl.checkStatus();
    setState(() {
      _bluetoothStatus = status;
    });
  }

  // TODO: Attach this function to a bluetooth status stream
  Future _scanForDevices() async {
    await _checkBluetoothStatus();
    debugPrint('Scanning for bluetooth devices');
    if (_bluetoothStatus) {
      setState(() {
        _listOfDevices = [];
      });
      final List<Device> devices = await _bCtrl.scan();
      setState(() {
        _listOfDevices = devices;
      });
    }
  }

  @override
  void initState() {
    super.initState();

    SchedulerBinding.instance.addPostFrameCallback((_) {
      _checkBluetoothStatus();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
        actions: [
          IconButton(
            onPressed: _checkBluetoothStatus,
            icon: Icon(Icons.refresh),
          ),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Bluetooth is ${_bluetoothStatus ? 'ON' : 'OFF'}'),
            const SizedBox(height: 8.0),
            if (_bluetoothStatus)
              ElevatedButton(
                onPressed: _scanForDevices,
                child: Text('Scan'),
              ),
            if (_listOfDevices.isNotEmpty) const SizedBox(height: 8.0),
            if (_listOfDevices.isNotEmpty)
              Expanded(
                child: ListView.separated(
                  itemBuilder: (_, index) => DeviceItem(
                    device: _listOfDevices[index],
                    onTap: () {
                      _connectDevice(_listOfDevices[index]);
                    },
                    isConnected: _connectedDevices.contains(_listOfDevices[index]),
                  ),
                  separatorBuilder: (_, __) => const SizedBox(height: 8.0),
                  itemCount: _listOfDevices.length,
                ),
              ),
          ],
        ),
      ),
      floatingActionButton: _connectedDevices.isNotEmpty
          ? FloatingActionButton.extended(
              onPressed: () {
                _player.playing ? _player.pause() : _player.play();
                setState(() {});
              },
              label: SizedBox(
                width: 40.0,
                child: Text(
                  _player.playing ? 'Pause' : 'Play',
                  textAlign: TextAlign.center,
                ),
              ),
              icon: Icon(_player.playing ? Icons.pause : Icons.play_arrow_outlined),
            )
          : null,
    );
  }
}
