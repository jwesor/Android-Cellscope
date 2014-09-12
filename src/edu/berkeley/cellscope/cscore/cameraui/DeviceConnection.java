package edu.berkeley.cellscope.cscore.cameraui;

import android.content.Intent;

public interface DeviceConnection {
	// Intent request codes
	static final int REQUEST_CONNECT_DEVICE = 1;
	static final int REQUEST_ENABLE_BT = 2;

	public void startConnection();
	public void stopConnection();
	public boolean isEnabled();
	public void write(byte[] b);
	public void queryResultConnect(int resultCode, Intent data);
	public void queryResultEnabled(int resultCode, Intent data);
	public String getDeviceName();
}
