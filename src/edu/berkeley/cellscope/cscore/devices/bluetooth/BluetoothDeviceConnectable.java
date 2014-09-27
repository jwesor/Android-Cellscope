package edu.berkeley.cellscope.cscore.devices.bluetooth;

import android.os.Message;
import edu.berkeley.cellscope.cscore.devices.DeviceConnectable;

public interface BluetoothDeviceConnectable extends DeviceConnectable<Message> {

	public void deviceUnavailable();
	public void deviceConnected();
	public void deviceDisconnected();
	public void updateStatusMessage(int id);
	public void writeByte(byte b);
	public boolean isReadyForWrite();
}
