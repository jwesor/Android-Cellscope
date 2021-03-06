package edu.berkeley.cellscope.cscore.cameraui;



public class TouchExposureControl extends TouchPinchControl {
	private ManualExposure screen;
	private int maxExposure, minExposure;
	
	public TouchExposureControl(ManualExposure m, int w, int h) {
		super(w, h);
		screen = m;
	}
	
	@Override
	public boolean pinch(double amount) {
		if (maxExposure == 0 && minExposure == 0) {
			maxExposure = screen.getMaxExposure();
			minExposure = screen.getMinExposure();
		}
		int exposure = (int)(amount * (maxExposure - minExposure));
		if (exposure == 0)
			return false;
		screen.adjustExposure(exposure);
		return true;
	}
	
	public interface ManualExposure extends TouchControllable {
		public int getMinExposure();
		public int getMaxExposure();
		public void adjustExposure(int amount);
	}
}
