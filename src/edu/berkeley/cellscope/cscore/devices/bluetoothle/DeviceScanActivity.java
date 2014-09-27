/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.berkeley.cellscope.cscore.devices.bluetoothle;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import edu.berkeley.cellscope.cscore.R;

/**
 * This Activity appears as a dialog. If Bluetooth is not enabled,
 * it will attempt to enable. It then searches for and displays
 * nearby Bluetooth LE devices and provides UI for selecting one for connection.
 */
public class DeviceScanActivity extends Activity {
	// Intent request codes
	private static final int REQUEST_ENABLE_BT = 1;
	public static final int REQUEST_SCAN_DEVICES = 2;

	// Debugging
	private final static String TAG = BluetoothConnectionFragment.class.getSimpleName();
	private static final boolean D = true;

	public static final String DEVICE_ADDRESS = "deviceAddress";
	public static final String DEVICE_NAME = "deviceName";

	private static final long SCAN_DURATION = 3000;

	// Member fields
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mDevicesAdapter;
	private ArrayList<BluetoothDevice> mDevices;
	private Button mScanButton;
	private boolean mScanningState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup the window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_device_dialog);

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Bluetooth LE is not supported", Toast.LENGTH_SHORT).show();
			finish();
		}

		mDevices = new ArrayList<BluetoothDevice>();

		// Set listeners for button
		mScanButton = (Button) findViewById(R.id.device_dialog_scan);
		mScanButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				startDeviceScan();
			}
		});

		// Initialize array adapter for list of discovered devices
		mDevicesAdapter = new ArrayAdapter<String>(this, R.layout.device_dialog_item);
		ListView deviceListView = (ListView) findViewById(R.id.device_dialog_list);
		deviceListView.setAdapter(mDevicesAdapter);
		deviceListView.setOnItemClickListener(mDeviceClickListener);

		setTitle(R.string.device_dialog_title);
	}

	@Override
	protected void onResume() {
		super.onResume();
		//Get the local Bluetooth adapter and attempt to enable Bluetooth if it's not enabled
		final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBtAdapter = mBluetoothManager.getAdapter();
		if (mBtAdapter == null) {
			Toast.makeText(this, "Bluetooth LE is not supported", Toast.LENGTH_SHORT).show();
			setResult(Activity.RESULT_CANCELED);
			finish();
			return;
		} else if (!mBtAdapter.isEnabled()) {
			mScanButton.setEnabled(false);
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mScanningState) {
			stopScan.run();
		}
	}

	// The on-click listener for all devices in the ListViews
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (mScanningState) {
				stopScan.run();
			}
			BluetoothDevice device = mDevices.get(position);
			Intent intent = new Intent();
			intent.putExtra(DEVICE_ADDRESS, device.getAddress());
			intent.putExtra(DEVICE_NAME, device.getName());
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};

	/**
	 * Begin scanning for nearby LE devices.
	 */
	private void startDeviceScan() {
		if (D) {
			Log.d(TAG, "Scanning for devices...");
		}


		if (mScanningState)
			return;
		mScanningState = mBtAdapter.startLeScan(mScanCallback);
		if (mScanningState) {
			mDevices.clear();
			mDevicesAdapter.clear();
			setProgressBarIndeterminateVisibility(true);
			mScanButton.setText(R.string.device_dialog_scanning);
			mScanButton.setEnabled(false);
			ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
			scheduler.schedule(stopScan, SCAN_DURATION, TimeUnit.MILLISECONDS);
		}

	}

	private Runnable stopScan = new Runnable() {
		public void run() {
			mBtAdapter.stopLeScan(mScanCallback);
			mScanningState = false;

			runOnUiThread(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(false);
					mScanButton.setText(R.string.device_dialog_scan);
					mScanButton.setEnabled(true);
				}
			});
		}
	};


	private BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {

		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {

				public void run() {
					if (device != null) {
						if (mDevices.indexOf(device) == -1) {
							mDevices.add(device);
							String label = String.format("%s\n%s", device.getName(), device.getAddress());
							mDevicesAdapter.add(label);
						}
					}
				}
			});
		}
	};


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == Activity.RESULT_OK) {
				mScanButton.setEnabled(true);
			} else {
				setResult(Activity.RESULT_CANCELED);
				finish();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
