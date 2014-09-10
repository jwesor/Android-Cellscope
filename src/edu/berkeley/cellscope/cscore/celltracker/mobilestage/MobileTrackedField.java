package edu.berkeley.cellscope.cscore.celltracker.mobilestage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import edu.berkeley.cellscope.cscore.celltracker.MathUtils;
import edu.berkeley.cellscope.cscore.celltracker.RealtimeImageProcessor;
import edu.berkeley.cellscope.cscore.celltracker.StepNavigator;

/**
 * Track objects across several fovs.
 * The travelling-salesman problem is solved using a greedy algorithm.
 * 1. 	Image of starting fov and initial set of objects
 * 2. 	Move to the closest unchecked object
 * 3. 	Record new fov
 * 4. 	Calculate new fov's offset from old fov (current object's fov)
 * 5. 	Calculate the expected positions of the old fov's objects
 * 		in the new fov
 * 6.	Ignore objects that are not expected to be found in the new fov.
 * 		Ignore objects that are expected to be close to the edge of the new fov,
 * 			in case the object has moved offscren.
 * 		Ignore objects that have already been found.
 * 7. 	Of remaining objects, locate their positions in the new fov.
 * 		Marked these objects as found.
 * 8.	Move to the next closest unfound object and repeat steps 3-8.
 * 9.	When all objects are found, mark all objects as not found and begin again
 * 			at step 2.
 */
public class MobileTrackedField implements RealtimeImageProcessor, StepNavigator.NavigationCallback {
	private List<MobileObj> objects;	//objects currently being tracked
	private List<MobileObj> notFound; //lists objects that are found and not found
	private List<MobileObj> toProcess;	//list of objects to process
	private boolean active; 			//true if actively tracking objecsts
	private MobileFov currentFov;	//fov used in the last update
	private double radius;
	private StepNavigator navigator;
	private int interval;
	private Mat currentFrame; //the frame inside currentFov
	private Mat nextFrame;	//queue the next frame to use
	private long nextTime;
	private boolean waitForNavigation, waitForFrame;
	private final Point navigationMoved, navigationError;
		//stores where the navigator moved to, and how far off it was from its original intended target
	
	
	private ScheduledExecutorService updateThread; //This thread will run image processing separately from the main UI thread
	private final Object frameLock; //used to synchronize frame updates and navigation callback

	private static final int INITIAL_DELAY = 500;
	private static final int DEFAULT_INTERVAL = 250;
	
	
	public MobileTrackedField(StepNavigator nav) {
		objects = new ArrayList<MobileObj>(); 
		notFound = new ArrayList<MobileObj>();
		toProcess = new ArrayList<MobileObj>();
		active = false;
		navigator = nav;
		navigator.addCallback(this);
		frameLock = new Object();
		navigationMoved = new Point();
		navigationError = new Point();
	}
	
	public void setStartingFov(Mat img, Point center, double radius, long time) {
		currentFrame = img;
		Point start = new Point(0, 0);
		this.radius = radius;
		currentFov = new MobileFov(center, radius, img, start, time);
	}
	
	public void addStartingObject(Rect roi) {
		if (!active && currentFov != null) {
			MobileObj obj = new MobileObj(roi, currentFov);
			objects.add(obj);
			notFound.add(obj);
		}
	}
	
	public void addStartingObject(List<Rect> roi) {
		for (Rect r: roi) {
			addStartingObject(r);
		}
	}
	
	/**
	 * Travelling salesman problem: given the position of the current fov,
	 * calculate the position of the next object to move to.
	 * Greedy: finds the closest object not yet found to move to.
	 */
	private Point nextTargetLocation() {
		Point currentLoc = currentFov.getAbsoluteLocation();
		double bestDistSqr = -1;
		Point bestTargetLoc = null;
		for (MobileObj obj: notFound) {
			Point objLoc = obj.getAbsoluteLocation();
			double distSqr = MathUtils.distSqr(currentLoc, objLoc);
			if (bestDistSqr == -1 || distSqr < bestDistSqr) {
				bestTargetLoc = objLoc;
				bestDistSqr = distSqr;
			}
		}
		return bestTargetLoc;
	}
	
	//Return a list of objects that we expect to find in this (absolute) location's field of vision.
	public List<MobileObj> expectedObjects(Point location) {
		toProcess.clear();
		double r2 = radius * radius;
		for (MobileObj obj: objects) {
			Point objPos = obj.getAbsoluteLocation();
			if (MathUtils.distSqr(objPos, location) <= r2) {
				toProcess.add(obj);
			}
		}
		return toProcess;
	}

	//Update the current frame being stored.
	public void processFrame(Mat mat) {
		synchronized (frameLock) {
			currentFrame = mat;
			nextTime = System.currentTimeMillis();
			if (waitForFrame && !waitForNavigation)
				waitForFrame = false;
		}
	}

	public boolean isRunning() {
		return active;
	}

	public void start() {
		active = true;
	}

	public void stop() {
	}

	public void displayFrame(Mat mat) {
	}

	public void continueRunning() {
	}
	
	public void setInterval(int i) {
		interval = i;
	}
	
	//Update all object's positions, moving as necessary.
	private void update() {
		//Reset the list of objects that have been updated.
		notFound.clear();
		notFound.addAll(objects);
		
		//Continue while there are objects that have not been updated.
		while (!notFound.isEmpty()) {
			Point location = nextTargetLocation();
			navigator.setTarget(location);
			navigator.start();
			waitForMotion();
			synchronized (frameLock) {
				//currentFov = new MobileFov();;
			}
		}
	}
	
	private void initiateUpdateThread() {
		updateThread = Executors.newSingleThreadScheduledExecutor();
		Runnable updater = new Runnable() {
			public void run() {
				update();
			}
		};
		updateThread.scheduleAtFixedRate(updater, INITIAL_DELAY, interval, TimeUnit.MILLISECONDS);
	}
	
	public void navigationComplete(Point target, Point moved, Point error) {
		synchronized (frameLock) {
			waitForNavigation = false;
			MathUtils.set(navigationError, error);
			MathUtils.set(navigationMoved, moved);
		}
	}

	//Wait for the navigator to finish moving and the frame to update.
	private void waitForMotion() {
		Lock lock = new ReentrantLock();
		Condition condition = lock.newCondition();
		lock.lock();
		try {
			while (waitForNavigation || waitForFrame) {
				condition.await();
			}
		} catch (InterruptedException e) {
			stop();
		} finally {
			lock.unlock();
		}
	}
}
