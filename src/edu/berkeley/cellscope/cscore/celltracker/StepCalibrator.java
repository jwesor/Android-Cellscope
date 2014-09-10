package edu.berkeley.cellscope.cscore.celltracker;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import edu.berkeley.cellscope.cscore.cameraui.TouchControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

/*
 * Determines the number of motor steps that backlash will consume.
 */
public class StepCalibrator implements RealtimeImageProcessor {
	private boolean busy, calibrated;
	private Point backlash;					//backlash experienced on the stepper motor, in number of steps
	private Point xStep, yStep;				//number of pixels per step
	private int[] backlashResults;			//temporarily holds backlash results from each step of calibration
	private Point[] partialStepResults;		//temporarily holds the partial step that occurs when coming off backlash
	private Point[] stepResults;			//temporarily holds step size results from each step of calibration
	private boolean continueCalibration;	//false if calibration is to be stopped
	private int currentState, currentDir;	//the current direction the stage is calibrating, and the step it is on
	private int trackerResponses;			//counts the number of FovTrackers that have finished calculating
	private int moves;						//counts the number of strides that have been taken
	private int wait;						//counts frames to wait
	private FovTracker[] trackers;
	private TrackerResult[] trackerResults;
	private TouchSwipeControl stage;		//controls the stage
	private List<CalibrationCallback> callbacks;
	
	private static final int STATE_RESET = 0;		//Stage moves continuously in one direction to maximize backlash in the other
	private static final int STATE_WAIT = 1;		//Calibrator is waiting several frames for screen to update
	private static final int STATE_BACKLASH = 2;	//Calibrator is calculating the stage's backlash
	private static final int STATE_STEP = 3;		//Calibrator is calculating the stage's step size
	
	private static final int[] MOVE_DIR = new int[]{TouchControl.xPositive, TouchControl.xNegative,
			TouchControl.yPositive, TouchControl.yNegative};	//Order of directions to calibrate the stage in
	private static final int[] RESET_DIR = new int[]{TouchControl.xNegative, TouchControl.xPositive,
			TouchControl.yNegative, TouchControl.yPositive};	//Order of directions the stage to move to reset, opposite of MOVE_DIR
	
	private static final int REQUIRED_BACKLASH_MOVES = 6; 	//If the screen moves for this many consecutive strides, there is no more backlash
	public static final int STRIDE_SIZE = 3;				//Number of steps the stage moves at once, per "stride"
	public static final int REQUIRED_STRIDES = 6;			//Number of strides the stage takes to calibrate
	
	private static final double TRACKER_SIZE = 0.05;		//Size of each FovTracker relative to the smallest screen dimen.
	private static final double TRACKER_SPACING = 0.07;		//Distance between each FovTracker
	private static final int TRACKER_COUNT = 4;				//Number of FovTrackers
	
	private static final int BACKLASH_LIMIT = 42;			//Calibration will fail if the backlash is greater than this many steps
	private static final int WAIT_FRAMES = 2;				//Number of frames to wait after each movement of the stage for the screen to update
	public static final String SUCCESS_MESSAGE = "Calibration successful";
	public static final String FAILURE_MESSAGE = "Calibration failed";

	
	/* StepCalibrator MUST use the same TouchSwipeControl as StepNavigator or anything else that uses these results
	 * to correctly account for backlash.
	 */
	public StepCalibrator(TouchSwipeControl s, int w, int h) {
        calibrated = false;
        busy = false;
        stage = s;
        trackers = new FovTracker[TRACKER_COUNT];
        int dimen = w < h ? w: h;
        int spacing = (int)(dimen * TRACKER_SPACING);
        int size = (int)(dimen * TRACKER_SIZE);
        Size rectSize = new Size(size, size);
        Point loc = new Point();
        MathUtils.set(loc, w / 2 - spacing, h / 2 - spacing);
        trackers[0] = new FovTracker(w, h, MathUtils.createCenteredRect(loc, rectSize));
        trackers[0].setPause(2);
       
        MathUtils.set(loc, w / 2 + spacing, h / 2 - spacing);
        trackers[1] = new FovTracker(w, h, MathUtils.createCenteredRect(loc, rectSize));
        trackers[1].setPause(2);
       
        MathUtils.set(loc, w / 2 - spacing, h / 2 + spacing);
        trackers[2] = new FovTracker(w, h, MathUtils.createCenteredRect(loc, rectSize));
        trackers[2].setPause(2);
        
        MathUtils.set(loc, w / 2 + spacing, h / 2 + spacing);
        trackers[3] = new FovTracker(w, h, MathUtils.createCenteredRect(loc, rectSize));
        trackers[3].setPause(2);
        
        trackerResults = new TrackerResult[TRACKER_COUNT];
        for (int i = 0; i < TRACKER_COUNT; i ++)
        	trackerResults[i] = new TrackerResult(trackers[i]);
        
        xStep = new Point();
        yStep = new Point();
        backlash = new Point();
        backlashResults = new int[4];
        Point xPosStep = new Point();
        Point xNegStep = new Point();
        Point yPosStep = new Point();
        Point yNegStep = new Point();
        stepResults = new Point[]{xPosStep, xNegStep, yPosStep, yNegStep};
        partialStepResults = new Point[]{new Point(), new Point(), new Point(), new Point()};
        callbacks = new ArrayList<CalibrationCallback>();
	}
	
	public void start() {
		if (busy || !stage.bluetoothConnected())
			return;
		busy = true;
		System.out.println("begin calibration");
		currentState = STATE_RESET;
		currentDir = 0;
		calibrated = false;
		continueCalibration = false;
		moves = 0;
		for (int i = 0; i < backlashResults.length; i ++) {
			backlashResults[i] = 0;
			MathUtils.set(stepResults[i], 0, 0);
			MathUtils.set(partialStepResults[i], 0, 0);
		}
		for (FovTracker tracker: trackers)
			tracker.start();
	}
	
	/** 
	 * Records values and sends commands to the stage as necessary
	 */
	private void executeCalibration() {
		if (currentState == STATE_RESET) {
			if (RESET_DIR[currentDir] == TouchSwipeControl.stopMotor)
				continueRunning();
			else
				stage.swipe(RESET_DIR[currentDir], BACKLASH_LIMIT);
			wait = WAIT_FRAMES;
			toNextStep();
		} else if (currentState == STATE_WAIT) {
			wait --;
			if (wait <= 0)
				toNextStep();
			continueRunning();
		} else if (currentState == STATE_BACKLASH) {
			if (!continueCalibration)
				moves ++;
			else {
				moves = 0;
				MathUtils.set(partialStepResults[currentDir], 0, 0);
			}
			if (moves == REQUIRED_BACKLASH_MOVES) {
				backlashResults[currentDir] -= moves * STRIDE_SIZE;
				moves = 0;
				toNextStep();
				stage.swipe(MOVE_DIR[currentDir], STRIDE_SIZE);
			} else {
				continueCalibration = false;
				backlashResults[currentDir] += STRIDE_SIZE;
				if (backlashResults[currentDir] >= BACKLASH_LIMIT && moves == 0)
					calibrationFailed();
				else
					stage.swipe(MOVE_DIR[currentDir], STRIDE_SIZE);
			}
		} else if (currentState == STATE_STEP) {
			for (int i = 0; i < TRACKER_COUNT; i ++)
				MathUtils.add(stepResults[currentDir], trackerResults[i].movement);
			moves ++;
			if (moves == REQUIRED_STRIDES) {
				moves = 0;
				if (toNextStep())
					calibrationComplete();
				else
					stage.swipe(MOVE_DIR[currentDir], STRIDE_SIZE);
			} else
				stage.swipe(MOVE_DIR[currentDir], STRIDE_SIZE);
		}
	}
	
	/* Advances to the next step of calibration.
	 * If calibration is already on the last step, return true.
	 */
	private boolean toNextStep() {
		currentState ++;
		if (currentState > STATE_STEP) {
			currentState = STATE_RESET;
			currentDir ++;
			if (currentDir >= MOVE_DIR.length)
				return true;
		}
		return false;
	}
	
	public void processFrame(Mat mat) {
		for (FovTracker tracker: trackers)
			tracker.processFrame(mat);
	}

	public void displayFrame(Mat mat) {
		for (FovTracker tracker: trackers)
			tracker.displayFrame(mat);
	}
	
	//All FovTrackers to resume
	public void continueRunning() {
		for (FovTracker tracker: trackers)
			tracker.resume();
	}
	
	
	public boolean isCalibrated() {
		return calibrated;
	}
	
	public boolean isRunning() {
		return busy;
	}
	
	private void calibrationComplete() {
		System.out.println("calibration complete");
		for (FovTracker tracker: trackers)
			tracker.stop();
		for (int i = 0; i < stepResults.length; i ++)
			MathUtils.divide(stepResults[i], TRACKER_COUNT * REQUIRED_STRIDES * STRIDE_SIZE);
		backlash.x = (backlashResults[0] + backlashResults[1]) / 2;
		backlash.y = (backlashResults[2] + backlashResults[3]) / 2;
		MathUtils.set(xStep, stepResults[0]);
		MathUtils.subtract(xStep, stepResults[1]);
		MathUtils.divide(xStep, 2);
		MathUtils.set(yStep, stepResults[3]);
		MathUtils.subtract(yStep, stepResults[4]);
		MathUtils.divide(yStep, 2);
		busy = false;
		calibrated = true;
		for (CalibrationCallback c: callbacks)
			c.calibrationComplete(true);
		System.out.println("Backlash: ");
		System.out.println(backlash);
		System.out.println("Step sizes: ");
		System.out.println("x " + xStep);
		System.out.println("y " + yStep);
	}
	
	
	public void stop() {
		calibrationFailed();
	}
	
	public void calibrationFailed() {
		System.out.println("calibration failed");
		for (FovTracker tracker: trackers)
			tracker.stop();
		busy = false;
		calibrated = false;
		for (CalibrationCallback c: callbacks)
			c.calibrationComplete(false);
	}
	
	public void addCallback(CalibrationCallback c) {
		callbacks.add(c);
	}
	
	public void removeCallback(CalibrationCallback c) {
		callbacks.remove(c);
	}
	
	private synchronized void continueCalibration(boolean result) {
		continueCalibration = continueCalibration || result;
	}
	
	private synchronized boolean trackerResponsesComplete() {
		trackerResponses ++;
		if (trackerResponses == TRACKER_COUNT) {
			trackerResponses = 0;
			return true;
		}
		return false;
	}
	
	public static interface CalibrationCallback {
		public void calibrationComplete(boolean success);
	}
	
	private class TrackerResult implements FovTracker.MotionCallback {
		private FovTracker tracker;
		Point movement;
		
		private TrackerResult(FovTracker ft) {
			tracker = ft;
			tracker.addCallback(this);
			movement = new Point();
		}
		
		public synchronized void motionResult(Point result) {
			continueCalibration(result.x == 0 && result.y == 0);
			tracker.pause();
			MathUtils.set(movement, result);
			if (trackerResponsesComplete())
				executeCalibration();
		}
	}

	/* Convert a target location in the screen's x-y to the number of steps in the motor's x-y. */
	public Point getRequiredSteps(Point target) {
		if (!calibrated)
			return new Point(0, 0);
		double a1 = xStep.x, a2 = xStep.y, b1 = yStep.x, b2 = yStep.y;
		double det = (a1 * b2 - b1 * a2);
		double x1 = target.x, x2 = target.y;
		double c1 = (x1 * b2) + (-b1 * x2);
		double c2 = (x1 * -a2) + (b2 * x2);
		c1 /= det;
		c2 /= det;
		return new Point(c1, c2);
	}
	
	public TouchSwipeControl getStageController() {
		return stage;
	}
	
	/* Adds steps to account for backlash in either direction. */
	public Point adjustBacklash(Point steps) {
		if (steps.x > 0) {
			if (stage.backlashOccurs(TouchControl.xPositive))
				steps.x += backlash.x;
		} else if (steps.x < 0) {
			if (stage.backlashOccurs(TouchControl.xNegative))
				steps.x -= backlash.x;
		}
		if (steps.y > 0) {
			if (stage.backlashOccurs(TouchControl.yPositive))
				steps.y += backlash.y;
		} else if (steps.y < 0) {
			if (stage.backlashOccurs(TouchControl.yNegative))
				steps.y -= backlash.y;
		}
		return steps;
	}
}
