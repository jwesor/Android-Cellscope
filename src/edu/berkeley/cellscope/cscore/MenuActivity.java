package edu.berkeley.cellscope.cscore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import edu.berkeley.cellscope.cscore.bluetooth.BluetoothActivity;
import edu.berkeley.cellscope.cscore.bluetoothle.BluetoothLeTestActivity;
import edu.berkeley.cellscope.cscore.celltracker.OpenCVCameraActivity;
import edu.berkeley.cellscope.cscore.celltracker.PanTrackActivity;
import edu.berkeley.cellscope.cscore.celltracker.StageCameraActivity;
import edu.berkeley.cellscope.cscore.celltracker.SwipePanActivity;
import edu.berkeley.cellscope.cscore.celltracker.TrackedCameraActivity;
import edu.berkeley.cellscope.cscore.celltracker.tracker.TrackerSettingsActivity;
import edu.berkeley.cellscope.cscore.mediaviewer.PhotoLibraryActivity;
import edu.berkeley.cellscope.cscore.mediaviewer.VideoLibraryActivity;

public class MenuActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
	}

	/*
	 * This is automatically called when the application is opened
	 * or resumed.
	 */
	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void goToCamera(View v) {
		Intent intent = new Intent(this, CameraActivity.class);
		startActivity(intent);
	}

	public void goToPhotoLibrary(View v) {
		Intent intent = new Intent(this, PhotoLibraryActivity.class);
		startActivity(intent);
	}

	public void goToVideoLibrary(View v) {
		Intent intent = new Intent(this, VideoLibraryActivity.class);
		startActivity(intent);
	}

	public void goToBluetooth(View v) {
		Intent intent = new Intent(this, BluetoothActivity.class);
		startActivity(intent);
	}

	public void goToOpenCVCamera(View v) {
		Intent intent = new Intent(this, OpenCVCameraActivity.class);
		startActivity(intent);
	}

	public void goToPanTrack(View v) {
		Intent intent = new Intent(this, PanTrackActivity.class);
		startActivity(intent);
	}

	public void goToTrackCam(View v) {
		Intent intent = new Intent(this, TrackedCameraActivity.class);
		startActivity(intent);
	}

	public void goToCellTracker(View v) {
		Intent intent = new Intent(this, TrackerSettingsActivity.class);
		startActivity(intent);
	}

	public void goToSwipePan(View v) {
		Intent intent = new Intent(this, SwipePanActivity.class);
		startActivity(intent);
	}

	public void goToStageCam(View v) {
		Intent intent = new Intent(this, StageCameraActivity.class);
		startActivity(intent);
	}

	public void goToBluetoothTest(View v) {
		Intent intent = new Intent(this, BluetoothLeTestActivity.class);
		startActivity(intent);
	}
}
