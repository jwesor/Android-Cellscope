package edu.berkeley.cellscope.cscore.devices.bluetoothle;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.devices.DeviceConnectable;

public class BluetoothLeTestActivity extends Activity {

	private static final String BLUETOOTH_FRAGMENT = "BluetoothConnectionFragment";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_le_test);

		Button button = (Button)findViewById(R.id.bluetooth_test_button);

		button.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				System.out.println("Attempting to create fragment....");
				FragmentManager fragmentManager = getFragmentManager();
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
				BluetoothConnectionFragment fragment = new BluetoothConnectionFragment();
				fragmentTransaction.add(fragment, BLUETOOTH_FRAGMENT);
				fragmentTransaction.commit();
				fragmentManager.executePendingTransactions();
				System.out.println("Fragment created and committed....");
				((BluetoothConnectionFragment) fragmentManager.findFragmentByTag(BLUETOOTH_FRAGMENT)).open();
				System.out.println("Attempting to connect to device...");
				((BluetoothConnectionFragment) fragmentManager.findFragmentByTag(BLUETOOTH_FRAGMENT)).addReceiver(receiver);
			}

		});
	}

	private DeviceConnectable<byte[]> receiver = new DeviceConnectable<byte[]>() {

		public BluetoothConnectionFragment getDeviceConnection() {
			return ((BluetoothConnectionFragment) getFragmentManager().findFragmentByTag(BLUETOOTH_FRAGMENT));
		}

		public void pushData(byte[] dat) {
			if (dat != null) {
				String data = new String(dat);
				System.out.println(data);
			}
		}

	};
}
