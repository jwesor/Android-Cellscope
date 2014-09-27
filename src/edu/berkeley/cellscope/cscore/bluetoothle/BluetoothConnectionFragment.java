package edu.berkeley.cellscope.cscore.bluetoothle;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.redbear.bleframework.RBLService;

import edu.berkeley.cellscope.cscore.cameraui.DeviceConnection;

/**
 * Background fragment that maintains the bluetooth connection between a device and
 * the containing activity. Can also initiate the device discovery activity.
 */
public class BluetoothConnectionFragment extends Fragment implements DeviceConnection {
	private final static String TAG = BluetoothConnectionFragment.class.getSimpleName();

	private RBLService mBtLeService;
	private BluetoothAdapter mBtAdapter;
	private String mDeviceName, mDeviceAddress;

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBtLeService = ((RBLService.LocalBinder) service)
					.getService();
			if (!mBtLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				disconnectSelf();
			}
		}

		public void onServiceDisconnected(ComponentName componentName) {
			mBtLeService = null;
		}
	};

	public BluetoothConnectionFragment() {
	}

	public void connectToDevice() {
		Activity activity = getActivity();
		final BluetoothManager mBluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
		mBtAdapter = mBluetoothManager.getAdapter();

		if (mBtAdapter == null) {
			Toast.makeText(activity, "Bluetooth LE is not supported", Toast.LENGTH_SHORT).show();
			return;
		}

		Intent intent = new Intent(activity, DeviceScanActivity.class);
		startActivityForResult(intent, DeviceScanActivity.REQUEST_SCAN_DEVICES);

		Intent gattServiceIntent = new Intent(activity, RBLService.class);
		activity.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}



	public void disconnectFromDevice() {
		// TODO Auto-generated method stub

	}

	public boolean isConnectedToDevice() {
		// TODO Auto-generated method stub
		return false;
	}

	public void write(byte[] b) {
		// TODO Auto-generated method stub

	}


	public String getDeviceName() {
		// TODO Auto-generated method stub
		return null;
	}

	private void disconnectSelf() {
		getActivity().getFragmentManager().beginTransaction().remove(this).commit();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DeviceScanActivity.REQUEST_SCAN_DEVICES) {
			if (resultCode == Activity.RESULT_OK) {
				mDeviceName = data.getStringExtra(DeviceScanActivity.DEVICE_NAME);
				mDeviceAddress = data.getStringExtra(DeviceScanActivity.DEVICE_ADDRESS);
				System.out.printf("Got device %s %s\n", mDeviceName, mDeviceAddress);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public String getDeviceAddress() {
		return null;
	}

	public void queryResultConnect(int resultCode, Intent data) {}

	public void queryResultEnabled(int resultCode, Intent data) {}
}
