package edu.berkeley.cellscope.cscore.cameraui;

import android.os.Message;

public interface DeviceConnectable {

	public void deviceUnavailable();
	public void deviceConnected();
	public void deviceDisconnected();
	public void updateStatusMessage(int id);
	public void readMessage(Message msg);
	public void writeByte(byte b);
	public boolean isReadyForWrite();
	public DeviceConnection getDeviceConnection();
}
