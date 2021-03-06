package edu.berkeley.cellscope.cscore.devices.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.devices.DeviceConnection;

/**
 *  Handles user interface for enabling bluetooth and connecting to devices,
 *  as well as providing a simple interface for communication with the device
 *  via BluetoothSerialService
 */
public class BluetoothDeviceConnection implements DeviceConnection {
	private Activity activity;
	private BluetoothDeviceConnectable connectable;

	//Bluetooth stuff
	private static BluetoothSerialService mSerialService = null;
	private boolean mEnablingBT;
	private BluetoothHandler mHandlerBT;

	// Name of the connected device
	private String mConnectedDeviceName = null;

	private BluetoothAdapter mBluetoothAdapter = null;
	private boolean bluetoothEnabled = false;
	private boolean proceedWithConnection = true;

	private Message message;

	// Message types sent from the BluetoothReadService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	static final int REQUEST_CONNECT_DEVICE = 1;
	static final int REQUEST_ENABLE_BT = 2;

	public BluetoothDeviceConnection(Activity c, BluetoothDeviceConnectable b) {
		activity = c;
		addReceiver(b);
		mHandlerBT = new BluetoothHandler();
		mSerialService = new BluetoothSerialService(activity, mHandlerBT/*, mEmulatorView*/);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		//BluetoothAdapter myBluetoothAdapter = null; //This was to test to see what the noBluetoothAdapter() method did
		if (mBluetoothAdapter == null){
			connectable.deviceUnavailable();
		}
		mEnablingBT = false;
	}

	public void addReceiver(BluetoothDeviceConnectable receiver) {
		connectable = receiver;
	}

	private void bluetoothConnected() {
		connectable.deviceConnected();
		bluetoothEnabled = true;

	}

	private void bluetoothDisconnected() {
		connectable.deviceDisconnected();
	}


	private class BluetoothHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothSerialService.STATE_CONNECTED:
					bluetoothConnected();
					break;

				case BluetoothSerialService.STATE_CONNECTING:
					connectable.updateStatusMessage(R.string.title_connecting);
					break;

				case BluetoothSerialService.STATE_NONE:
					bluetoothDisconnected();
					connectable.updateStatusMessage(R.string.title_not_connected);
					bluetoothEnabled = false;

					break;
				}
				break;
			case MESSAGE_WRITE:
				break;

			case MESSAGE_READ:
				message = msg;
				connectable.pushData(message);
				break;

			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(activity, "Connected to "
						+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(activity, msg.getData().getString(TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			}
		}
	}

	public void close() {
		if (mSerialService != null) {
			mSerialService.stop();
		}
	}

	public void open() {
		proceedWithConnection = true;
		if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
			if (!mEnablingBT) { // If we are turning on the BT we cannot check if it's enable
				if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
					proceedWithConnection = false;
					AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					builder.setMessage(R.string.alert_dialog_turn_on_bt)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.alert_dialog_warning_title)
					.setCancelable( false )
					.setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mEnablingBT = true;
							Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
							activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
						}
					})
					.setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}

				if (mSerialService != null) {
					// Only if the state is STATE_NONE, do we know that we haven't started already
					if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
						// Start the Bluetooth chat services
						mSerialService.start();
					}
				}
			}
			if (proceedWithConnection) {
				// Launch the DeviceListActivity to see devices and do scan
				Intent serverIntent = new Intent(activity, DeviceListActivity.class);
				activity.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			}
		}
		else if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
			mSerialService.stop();
			mSerialService.start();
		}
	}

	// When DeviceListActivity returns with a device to connect
	public void queryResultConnect(int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			// Get the device MAC address
			String address = data.getExtras()
					.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
			// Get the BLuetoothDevice object
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address); //I deleted "m." before the method getRemoteDevice()
			// Attempt to connect to the device
			mSerialService.connect(device);
		}
	}

	// When the request to enable Bluetooth returns
	public void queryResultEnabled(int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			mEnablingBT = false;
			//finishDialogNoBluetooth();
		}
		else {
			Intent serverIntent = new Intent(activity, DeviceListActivity.class);
			activity.startActivityForResult(serverIntent, DeviceConnection.REQUEST_CONNECT_DEVICE);
		}
	}


	public int getConnectionState() {
		return mSerialService.getState();
	}

	public boolean isOpen() {
		return bluetoothEnabled;
	}

	public void write(byte[] buffer) {
		if (bluetoothEnabled) {
			System.out.println("write  " + buffer[0]);
			mSerialService.write(buffer);
		}
	}

	public Message read() {
		return message;
	}

	public String getDeviceName() {
		if (mConnectedDeviceName == null)
			return "";
		return mConnectedDeviceName;
	}

	public String getDeviceAddress() {
		return "";
	}
}
