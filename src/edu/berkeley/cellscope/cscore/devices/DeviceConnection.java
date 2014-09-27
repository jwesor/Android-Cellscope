package edu.berkeley.cellscope.cscore.devices;

import android.content.Intent;

public interface DeviceConnection {
	// Intent request codes
	static final int REQUEST_CONNECT_DEVICE = 1;
	static final int REQUEST_ENABLE_BT = 2;

	public void open();
	public void close();
	public boolean isOpen();
	public void write(byte[] b);
	public Object read();
	public void queryResultConnect(int resultCode, Intent data);
	public void queryResultEnabled(int resultCode, Intent data);
	public String getDeviceName();
	public String getDeviceAddress();
}
