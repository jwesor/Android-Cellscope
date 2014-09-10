package edu.berkeley.cellscope.cscore.cameraui;

import edu.berkeley.cellscope.cscore.bluetooth.BluetoothActivity;

public class IlluminationControl {
	private BluetoothControllable scope;
	private boolean on;

	private static int lightOn = BluetoothActivity.lightOn;
	private static int lightOff = BluetoothActivity.lightOff;

	public IlluminationControl(BluetoothControllable bt) {
		scope = bt;
		on = false;
	}

	public void enableIllumination() {
		if (scope.controlReady()) {
			on = true;
			write(lightOn);
		}
	}

	public void disableIllumination() {
		if (scope.controlReady()) {
			on = false;
			write(lightOff);
		}
	}

	public void toggleIllumination() {
		if (on)
			disableIllumination();
		else
			enableIllumination();
	}

	private void write(int val) {
		BluetoothConnector bt = scope.getBluetooth();
		byte[] buffer = new byte[1];
		buffer[0] = (byte)val;
		bt.write(buffer);
	}
}
