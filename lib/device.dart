class Device {
  final String name;
  final String address;

  const Device(this.name, this.address);

  @override
  String toString() => 'Device(name: $name, address: $address)';
}
