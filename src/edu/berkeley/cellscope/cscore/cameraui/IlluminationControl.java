package edu.berkeley.cellscope.cscore.cameraui;

import edu.berkeley.cellscope.cscore.devices.DeviceConnection;
import edu.berkeley.cellscope.cscore.devices.bluetooth.BluetoothActivity;
import edu.berkeley.cellscope.cscore.devices.bluetooth.BluetoothDeviceConnectable;

public class IlluminationControl {
	private BluetoothDeviceConnectable scope;
	private boolean on;

	private static int lightOn = BluetoothActivity.lightOn;
	private static int lightOff = BluetoothActivity.lightOff;

	public IlluminationControl(BluetoothDeviceConnectable bt) {
		scope = bt;
		on = false;
	}

	public void enableIllumination() {
		if (scope.isReadyForWrite()) {
			on = true;
			write(lightOn);
		}
	}

	public void disableIllumination() {
		if (scope.isReadyForWrite()) {
			on = false;
			write(lightOff);
		}
	}

	public void toggleIllumination() {
		if (on) {
			disableIllumination();
		} else {
			enableIllumination();
		}
	}

	private void write(int val) {
		DeviceConnection bt = scope.getDeviceConnection();
		byte[] buffer = new byte[1];
		buffer[0] = (byte)val;
		bt.write(buffer);
	}
}
