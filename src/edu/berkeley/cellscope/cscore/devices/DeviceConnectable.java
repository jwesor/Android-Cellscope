package edu.berkeley.cellscope.cscore.devices;

public interface DeviceConnectable<T> {
	public DeviceConnection getDeviceConnection();
	public void pushData(T dat);
}
