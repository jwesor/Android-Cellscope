package edu.berkeley.cellscope.cscore.celltracker;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;
import edu.berkeley.cellscope.cscore.devices.bluetooth.BluetoothDeviceConnectable;

/**
 *  Moves the stage.
 */
public class StepNavigator implements RealtimeImageProcessor, FovTracker.MotionCallback {
	private StepCalibrator calibrator;
	private TouchSwipeControl stage;
	private Autofocus autofocus;
	private boolean processSub;
	private Point target, steps;	//Target location and the distance moved so far
	private Point offtarget; 		//How off-target the final movement was--how many pixels the stage needs to move by to be on target
	private boolean moving, targetSet;
	private FovTracker tracker;
	private List<NavigationCallback> callbacks;

	private static final int STRIDE_SIZE = StepCalibrator.STRIDE_SIZE;

	public StepNavigator(BluetoothDeviceConnectable bt, int w, int h) {
		TouchSwipeControl ctrl = new TouchSwipeControl(bt, w, h);
		StepCalibrator calib = new StepCalibrator(ctrl, w, h);
		Autofocus focus = new Autofocus(ctrl);
		FovTracker track = new FovTracker(w, h);
		processSub = true;
		init(calib, focus, track);
	}

	public StepNavigator(StepCalibrator calib, Autofocus focus, FovTracker track) {
		processSub = false;
		init(calib, focus, track);
	}

	private void init(StepCalibrator calib, Autofocus focus, FovTracker track) {
		stage = calib.getStageController();
		calibrator = calib;
		autofocus = focus;
		tracker = track;
		tracker.addCallback(this);
		target = new Point();
		offtarget = new Point();
		callbacks = new ArrayList<NavigationCallback>();
	}

	/** Sets the navigator to move by displacement p */
	public void setTarget(Point p) {
		setTarget(p.x, p.y);
	}

	/** Sets the navigator to move along x and y by X and Y.*/
	public void setTarget(double x, double y) {
		if (!calibrator.isCalibrated())
			return;
		MathUtils.set(target, x, y);
		MathUtils.set(offtarget, target);
		calibrator.getRequiredSteps(target);
		targetSet = true;
		steps.x = (steps.x > 0) ? (int)(steps.x + 0.5) : (int)(steps.x - 0.5); //round no. of steps
		steps.y = (steps.y > 0) ? (int)(steps.y + 0.5) : (int)(steps.y - 0.5);

		calibrator.adjustBacklash(steps);
	}

	/** The navigator will move to the target specified by setTarget() */
	public void start() {
		System.out.println("initiate navigation");
		if (!calibrator.isCalibrated() || !stage.bluetoothConnected() || calibrator.isRunning()
				|| autofocus.isRunning() || isRunning() || !targetSet)
			return;
		System.out.println("prerequisites fulfilled");
		targetSet = false;
		moving = true;
		tracker.start();
	}

	/** Halt the navigator. Movement stops. */
	public void stop() {
		moving = false;
		tracker.stop();
	}

	/** Returns true if the navigator is currently moving to a target. */
	public boolean isRunning() {
		return moving;
	}

	/** If this navigator was made using an external Autofocus and external StepCalibrator,
	 *  then it is assumed that these will be updated by whatever activity made them.
	 *  If the navigator constructed its own AutoFocus and StepCalibrator, then they must
	 *  be updated when the navigator is updated.
	 */
	public void processFrame(Mat mat) {
		if (processSub) {
			if (autofocus.isRunning()) {
				autofocus.processFrame(mat);
			}
			if (calibrator.isRunning()) {
				calibrator.processFrame(mat);
			}
		}
	}

	public void displayFrame(Mat mat) {
		if (processSub) {
			if (autofocus.isRunning()) {
				autofocus.displayFrame(mat);
			}
			if (calibrator.isRunning()) {
				calibrator.displayFrame(mat);
			}
		}
	}

	/** Called when motor completes stride. */
	public void continueRunning() {
		tracker.pause();
	}

	/** Called when the FovTracker has a result on exactly how much
	 * the screen moved by.
	 * Calculates which direction the stage should move for the next stride,
	 * then executes the move.
	 */
	public void motionResult(Point result) {
		tracker.pause();
		MathUtils.subtract(offtarget, result);

		if (targetReached()) {
			stop();
		} else if (steps.y > steps.x) {
			steps.y -= STRIDE_SIZE;
			stage.swipeY(STRIDE_SIZE);
		} else if (steps.x > steps.y) {
			steps.x -= STRIDE_SIZE;
			stage.swipeX(STRIDE_SIZE);
		}
	}

	/** Return true if the navigator cannot move any closer to the target. */
	private boolean targetReached() {
		return steps.x < STRIDE_SIZE && steps.y < STRIDE_SIZE;
	}

	/** Get the displacement between the intended target and the actual location
	 * that we moved to.
	 */
	public Point getErrorDistance() {
		return offtarget;
	}

	public void addCallback(NavigationCallback n) {
		callbacks.add(n);
	}

	public void removeCallback(NavigationCallback n) {
		callbacks.remove(n);
	}

	public static interface NavigationCallback {
		public void navigationComplete(Point target, Point moved, Point error);
	}
}
