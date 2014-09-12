package edu.berkeley.cellscope.cscore.cameraui;

import android.view.MotionEvent;

/*
 * Touch listener that responds to single-finger gestures.
 */

public class TouchSwipeControl extends TouchControl {
	protected DeviceConnectable stage;
	private double touchX, touchY;
	private int lastX, lastY;

	private static final double SENSITIVITY = 0.1;

	public TouchSwipeControl(DeviceConnectable s, int w, int h) {
		super(w, h);
		stage = s;
		touchX = touchY = firstTouchEvent;
		lastX = lastY = stopMotor;
	}

	@Override
	public boolean touch(MotionEvent event) {
		int pointers = event.getPointerCount();
		int action = event.getActionMasked();

		if (pointers == 1) {
			if (action == MotionEvent.ACTION_DOWN) {
				touchX = event.getX();
				touchY = event.getY();
			}
			else if (action == MotionEvent.ACTION_UP) {
				if (touchX != firstTouchEvent && touchY != firstTouchEvent) {
					swipeStage(event.getX() - touchX, event.getY() - touchY);
				}
				touchX = touchY = firstTouchEvent;
			}
		} else {
			touchX = touchY = firstTouchEvent;
		}
		return true;
	}

	public void swipeStage(double x, double y) {
		if (!stage.isReadyForWrite())
			return;
		int dir = 0, dist = 0;
		if (Math.abs(x) > Math.abs(y)) {
			dir = x > 0 ? TouchPanControl.xPositive : TouchPanControl.xNegative;
			dist = (int)(Math.abs(x) * SENSITIVITY);
		}
		else {
			dir = y < 0 ? TouchPanControl.yPositive : TouchPanControl.yNegative;
			dist = (int)(Math.abs(y) * SENSITIVITY);
		}
		swipe(dir, dist);
	}


	public void swipe(int dir, int dist) {
		stage.writeByte((byte)dir);
		stage.writeByte((byte)dist);
		if (dir == xPositive || dir == xNegative) {
			lastX = dir;
		} else if (dir == yPositive || dir == yNegative) {
			lastY = dir;
		}
	}

	public void swipeX(int steps) {
		if (steps > 0) {
			swipe(TouchControl.xPositive, steps);
		} else if (steps < 0) {
			swipe(TouchControl.xNegative, -steps);
		}
	}

	public void swipeY(int steps) {
		if (steps > 0) {
			swipe(TouchControl.yPositive, steps);
		} else if (steps < 0) {
			swipe(TouchControl.yNegative, -steps);
		}
	}

	public boolean bluetoothConnected() {
		return stage.isReadyForWrite();
	}

	public boolean backlashOccurs(int dir) {
		if (dir == xPositive || dir == xNegative)
			return lastX != dir;
		if (dir == yPositive || dir == yNegative)
			return lastY != dir;
		return false;
	}

}
