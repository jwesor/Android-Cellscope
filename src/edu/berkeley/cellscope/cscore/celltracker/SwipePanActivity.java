package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Point;

import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.cameraui.DeviceConnectable;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

/*
 * Class for testing the stepper counter.
 *
 * Observations: commands do not queue. Sending multiple commands at once
 * to the stage will cause the first command to be executed, but all commands' steps
 * to count down. When the first command is done executing (i.e. no more steps remaining),
 * the next command will be executed if it has remaining steps.
 */
public class SwipePanActivity extends OpenCVCameraActivity implements Autofocus.AutofocusCallback, StepCalibrator.CalibrationCallback, FovTracker.MotionCallback {
	private MenuItem mMenuItemCalibrate, mMenuItemAutofocus, mMenuItemIllumination;

	protected TouchSwipeControl touchSwipe;
	private StepCalibrator calibrator;
	private StepNavigator navigator;
	private FovTracker positionTracker;
	protected Autofocus autofocus;

	@Override
	protected void createAddons(int width, int height) {
		super.createAddons(width, height);
		touchPan.setEnabled(false);
		touchSwipe = new NavigationSwipeControl(this, width, height);
		touchSwipe.setEnabled(true);
		compoundTouch.addTouchListener(touchSwipe);
		TouchSwipeControl swipeDriver = new TouchSwipeControl(this, width, height);
		autofocus = new Autofocus(swipeDriver);
		autofocus.addCallback(this);
		realtimeProcessors.add(autofocus);
		calibrator = new StepCalibrator(swipeDriver, width, height);
		calibrator.addCallback(this);
		positionTracker = new FovTracker(width, height);
		navigator = new StepNavigator(calibrator, autofocus, positionTracker);
		realtimeProcessors.add(calibrator);
		realtimeProcessors.add(navigator);
	}

	@Override
	public void readMessage(Message msg) {
		super.readMessage(msg);
		byte[] buffer = (byte[])(msg.obj);
		if (buffer.length > 0) {
			//			int message = (int)(buffer[0]);
			if (calibrator.isRunning()) {
				//				if (message == BluetoothControllable.PROCEED)
				calibrator.continueRunning();
				//				else if (message == BluetoothControllable.FAILED)
				//					calibrator.calibrationFailed();
			}
			else if (navigator.isRunning()) {
				//				if (message == BluetoothControllable.PROCEED)
				navigator.continueRunning();
				//				else if (message == BluetoothControllable.FAILED)
				//					navigator.stop();
			}
			else if (autofocus.isRunning()) {
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
		if (mMenuItemCalibrate != null) {
			mMenuItemCalibrate.setEnabled(true);
		}
		if (mMenuItemAutofocus != null) {
			mMenuItemAutofocus.setEnabled(true);
		}
		if (mMenuItemIllumination != null) {
			mMenuItemIllumination.setEnabled(true);
		}
	}

	@Override
	public void deviceDisconnected() {
		super.deviceDisconnected();
		if (mMenuItemCalibrate != null) {
			mMenuItemCalibrate.setEnabled(false);
		}
		if (mMenuItemAutofocus != null) {
			mMenuItemAutofocus.setEnabled(false);
		}
		if (mMenuItemIllumination != null) {
			mMenuItemIllumination.setEnabled(false);
		}
	}

	@Override
	public boolean isReadyForWrite() {
		return super.isReadyForWrite() && !calibrator.isRunning();
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
		inflater.inflate(R.menu.menu_calibrate, menu);
		mMenuItemCalibrate = menu.getItem(menuItems++);
		mMenuItemCalibrate.setEnabled(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (super.onOptionsItemSelected(item))
			return true;
		int id = item.getItemId();
		if (id == R.id.calibrate) {
			runStageCalibration();
			return true;
		}
		else if (id == R.id.autofocus) {
			autofocus.start();
			return true;
		}
		return false;
	}

	public void runStageCalibration() {
		calibrator.start();
	}

	public void calibrationComplete(boolean success) {
		if (success) {
			toast(StepCalibrator.SUCCESS_MESSAGE);
		} else {
			toast(StepCalibrator.FAILURE_MESSAGE);
		}
	}

	/* Redirects all input to StepNavigator instead of carrying out the stage motions itself.
	 */
	private class NavigationSwipeControl extends TouchSwipeControl {

		public NavigationSwipeControl(DeviceConnectable s, int w, int h) {
			super(s, w, h);
		}

		@Override
		public void swipeStage(double x, double y) {
			System.out.println("test " + x + " " + y);
			if (!stage.isReadyForWrite() || navigator.isRunning())
				return;
			System.out.println("pass");
			navigator.setTarget((int)x, (int)y);
			navigator.start();
		}

		@Override
		public void swipe(int dir, int steps) {
			return;
		}

	}


	public void focusComplete(boolean success) {
		if (success) {
			toast(Autofocus.SUCCESS_MESSAGE);
		} else {
			toast(Autofocus.FAILURE_MESSAGE);
		}
	}
}
