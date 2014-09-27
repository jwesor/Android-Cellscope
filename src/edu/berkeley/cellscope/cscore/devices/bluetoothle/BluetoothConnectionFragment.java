package edu.berkeley.cellscope.cscore.devices.bluetoothle;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.redbear.bleframework.RBLService;

import edu.berkeley.cellscope.cscore.devices.DeviceConnectable;
import edu.berkeley.cellscope.cscore.devices.DeviceConnection;

/**
 * Background fragment that maintains the bluetooth connection between a device and
 * the containing activity. Can also initiate the device discovery activity.
 */
public class BluetoothConnectionFragment extends Fragment implements DeviceConnection {
	private final static String TAG = BluetoothConnectionFragment.class.getSimpleName();

	private RBLService mBtLeService;
	private BluetoothAdapter mBtAdapter;
	private String mDeviceName, mDeviceAddress;
	private List<DeviceConnectable<byte[]>> mReceiversList;
	private byte[] mLastMessage;
	private int mLastMessageId;
	private boolean mConnectionOpen;

	/* Monitors the RBLService */
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName compoentName,
				IBinder service) {
			mBtLeService = ((RBLService.LocalBinder) service)
					.getService();
			if (!mBtLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				disconnectSelf();
			}
			System.out.println("attempting to connect to " + mDeviceAddress);
			mBtLeService.connect(mDeviceAddress);
		}

		public void onServiceDisconnected(ComponentName componentName) {
			mBtLeService.disconnect();
		}
	};

	/* Receives status updates from connected bluetooth */
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			System.out.println("Received update! " + action);

			if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
				onDeviceDisconnected();
			} else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				onDeviceConnected();
			} else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
				mLastMessage = intent.getByteArrayExtra(RBLService.EXTRA_DATA);
				mLastMessageId ++;
				pushData();
			}
		}
	};

	public BluetoothConnectionFragment() {
		mReceiversList = new ArrayList<DeviceConnectable<byte[]>>();
		mLastMessageId = -1;
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
		getActivity().registerReceiver(mGattUpdateReceiver, intentFilter);
	}

	@Override
	public void onStop() {
		super.onStop();
		getActivity().unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mConnectionOpen) {
			close();
		}
		mBtLeService.close();
	}

	public void open() {
		Activity activity = getActivity();
		final BluetoothManager mBluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
		mBtAdapter = mBluetoothManager.getAdapter();

		if (mBtAdapter == null) {
			Toast.makeText(activity, "Bluetooth LE is not supported", Toast.LENGTH_SHORT).show();
			return;
		}

		Intent intent = new Intent(activity, DeviceScanActivity.class);
		startActivityForResult(intent, DeviceScanActivity.REQUEST_SCAN_DEVICES);
	}

	private void onDeviceConnected() {
		BluetoothGattService gattService = mBtLeService.getSupportedGattService();;
		BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
		mBtLeService.setCharacteristicNotification(characteristicRx, true);
		mBtLeService.readCharacteristic(characteristicRx);
	}

	private void onDeviceDisconnected() {

	}

	public void close() {
		mConnectionOpen = false;
		getActivity().unbindService(mServiceConnection);
	}

	public boolean isOpen() {
		return mConnectionOpen;
	}

	public void write(byte[] b) {
		// TODO Auto-generated method stub

	}

	private void pushData() {
		for (DeviceConnectable<byte[]> r: mReceiversList) {
			r.pushData(mLastMessage);
		}
	}

	public byte[] read() {
		return mLastMessage;
	}

	public int readId() {
		return mLastMessageId;
	}

	public String getDeviceName() {
		return mDeviceName;
	}

	public String getDeviceAddress() {
		return mDeviceAddress;
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
				openConnection();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void openConnection() {
		Activity activity = getActivity();
		Intent gattServiceIntent = new Intent(activity, RBLService.class);
		activity.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
		mConnectionOpen = true;
	}

	public void addReceiver(DeviceConnectable<byte[]> receiver) {
		mReceiversList.add(receiver);
	}

	public void queryResultConnect(int resultCode, Intent data) {}

	public void queryResultEnabled(int resultCode, Intent data) {}

}
