package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Point;

import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.cameraui.IlluminationControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

/* Has all stage basics.*/

/*
 * Class for testing the stepper counter.
 *
 * Observations: commands do not queue. Sending multiple commands at once
 * to the stage will cause the first command to be executed, but all commands' steps
 * to count down. When the first command is done executing (i.e. no more steps remaining),
 * the next command will be executed if it has remaining steps.
 */
public class StageCameraActivity extends OpenCVCameraActivity implements Autofocus.AutofocusCallback, FovTracker.MotionCallback {
	private MenuItem mMenuItemAutofocus, mMenuItemIllumination;

	protected Autofocus autofocus;
	protected IlluminationControl illumination;

	@Override
	protected void createAddons(int width, int height) {
		super.createAddons(width, height);
		touchPan.setEnabled(true);
		TouchSwipeControl swipeDriver = new TouchSwipeControl(this, width, height);
		autofocus = new Autofocus(swipeDriver);
		autofocus.addCallback(this);
		realtimeProcessors.add(autofocus);
		illumination = new IlluminationControl(this);
	}

	@Override
	public void readMessage(Message msg) {
		super.readMessage(msg);
		byte[] buffer = (byte[])(msg.obj);
		if (buffer.length > 0) {
			if (autofocus.isRunning()) {
				autofocus.continueRunning();
			}
		}
	}


	/* Override this to perform post-calculation operations
	 * in subclasses.
	 */
	public void motionResult(Point result) {
		//System.out.println(result);
	}

	@Override
	public void deviceConnected() {
		super.deviceConnected();
		if (mMenuItemAutofocus != null) {
			mMenuItemAutofocus.setEnabled(true);
		}
		if (mMenuItemIllumination != null) {
			mMenuItemIllumination.setEnabled(true);
			illumination.enableIllumination();
		}
	}

	@Override
	public void deviceDisconnected() {
		super.deviceDisconnected();
		if (mMenuItemAutofocus != null) {
			mMenuItemAutofocus.setEnabled(false);
		}
		if (mMenuItemIllumination != null) {
			mMenuItemIllumination.setEnabled(false);
			illumination.disableIllumination();
		}
	}

	@Override
	public boolean isReadyForWrite() {
		return super.isReadyForWrite() && !autofocus.isRunning();
	}

	public void hideControls() {
		takePicture.setVisibility(View.INVISIBLE);
		toggleRecord.setVisibility(View.INVISIBLE);
	}

	public void showControls() {
		takePicture.setVisibility(View.VISIBLE);
		toggleRecord.setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_autofocus, menu);
		mMenuItemAutofocus = menu.getItem(menuItems++);
		mMenuItemAutofocus.setEnabled(false);
		inflater.inflate(R.menu.menu_illumination, menu);
		mMenuItemIllumination = menu.getItem(menuItems++);
		mMenuItemIllumination.setEnabled(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (super.onOptionsItemSelected(item))
			return true;
		int id = item.getItemId();
		if (id == R.id.autofocus) {
			autofocus.start();
			return true;
		} else if (id == R.id.illuminate) {
			illumination.toggleIllumination();
			return true;
		}
		return false;
	}

	public void focusComplete(boolean success) {
		if (success) {
			toast(Autofocus.SUCCESS_MESSAGE);
		} else {
			toast(Autofocus.FAILURE_MESSAGE);
		}
	}
}
