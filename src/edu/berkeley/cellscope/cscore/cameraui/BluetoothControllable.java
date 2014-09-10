package edu.berkeley.cellscope.cscore.cameraui;

//Interface for stuff controllable via bluetooth
public interface BluetoothControllable {
	public boolean controlReady();
	public BluetoothConnector getBluetooth();

	public static final int PROCEED = 0;
	public static final int FAILED = 1;
}

