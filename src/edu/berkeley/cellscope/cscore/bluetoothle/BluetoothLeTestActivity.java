package edu.berkeley.cellscope.cscore.bluetoothle;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import edu.berkeley.cellscope.cscore.R;

public class BluetoothLeTestActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_le_test);

		Button button = (Button)findViewById(R.id.bluetooth_test_button);

		button.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				System.out.println("Attempting to create fragment....");
				String bluetoothFragment = "BluetoothConnectionFragment";
				FragmentManager fragmentManager = getFragmentManager();
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
				BluetoothConnectionFragment fragment = new BluetoothConnectionFragment();
				fragmentTransaction.add(fragment, bluetoothFragment);
				fragmentTransaction.commit();
				fragmentManager.executePendingTransactions();
				System.out.println("Fragment created and committed....");
				((BluetoothConnectionFragment) fragmentManager.findFragmentByTag(bluetoothFragment)).connectToDevice();
				System.out.println("Attempting to connect to device...");
			}

		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
			return true;
		return super.onOptionsItemSelected(item);
	}
}
