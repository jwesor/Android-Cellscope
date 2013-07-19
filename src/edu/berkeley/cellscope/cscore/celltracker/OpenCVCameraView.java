package edu.berkeley.cellscope.cscore.celltracker;

import java.io.File;
import java.io.FileOutputStream;

import org.opencv.android.JavaCameraView;

import edu.berkeley.cellscope.cscore.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

public class OpenCVCameraView extends JavaCameraView {
	private static final String TAG = "OpenCvCameraView";
	
	private OpenCVCameraActivity activity;
	private int minExposure, maxExposure;
	int width, height;

	public OpenCVCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setActivity(OpenCVCameraActivity activity) {
		this.activity = activity;
	}
	
	public void disableAutoFocus() {
		System.out.println(mCamera);
		Camera.Parameters params = mCamera.getParameters();
		if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_INFINITY))
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		params.setExposureCompensation(0);
		minExposure = params.getMinExposureCompensation();
		maxExposure = params.getMaxExposureCompensation();
		mCamera.setParameters(params);
		Camera.Size size = params.getPreviewSize();
		width = size.width;
		height = size.height;
	}
	
	@Override
	public void enableView() {
		super.enableView();
		disableAutoFocus();
	}

	public void takePicture(final File fileName) {
        Log.i(TAG, "Taking picture");
        PictureCallback callback = new PictureCallback() {

            public void onPictureTaken(byte[] data, Camera camera) {
                activity.savePicture(fileName, data);
                //mCamera.stopPreview();
                mCamera.startPreview();
            }
        };
        Camera.Parameters params = mCamera.getParameters();
        Camera.Size size = params.getPreviewSize();
        params.setPictureSize(size.width, size.height);
        mCamera.setParameters(params);
        mCamera.takePicture(null, null, callback);
    }
    
	public String zoom(int step) {
		Camera.Parameters parameters = mCamera.getParameters();
		if (!parameters.isZoomSupported())
			return "100%";
		int zoom = parameters.getZoom() + step;
		if (zoom > parameters.getMaxZoom())
			zoom = parameters.getMaxZoom();
		else if (zoom < 0)
			zoom = 0;
		parameters.setZoom(zoom);
		mCamera.setParameters(parameters);
		return parameters.getZoomRatios().get(zoom) + "%";
	}
	
	public int getMaxZoom() {
		Camera.Parameters parameters = mCamera.getParameters();
		if (!parameters.isZoomSupported())
			return 0;
		return parameters.getMaxZoom();
	}
	
	public int getMaxExposure() {
		Camera.Parameters parameters = mCamera.getParameters();
		return parameters.getMaxExposureCompensation();
	}
	
	public int getMinExposure() {
		Camera.Parameters parameters = mCamera.getParameters();
		return parameters.getMinExposureCompensation();
	}
	
	public String adjustExposure(int amount) {
		Camera.Parameters parameters = mCamera.getParameters();
		int current = parameters.getExposureCompensation();
		current += amount;
		if (current < minExposure)
			current = minExposure;
		else if (current > maxExposure)
			current = maxExposure;
		parameters.setExposureCompensation(current);
		mCamera.setParameters(parameters);
		double exposure = parameters.getExposureCompensationStep() * parameters.getExposureCompensation();
		exposure = (int)(exposure * 100) / 100d; //limit to two decimal places
		return "" + exposure;
	}
	
	public int getCurrentZoom() {
		return mCamera.getParameters().getZoom();
	}
	
	public int getCUrrentExposure() {
		return mCamera.getParameters().getExposureCompensation();
	}
}
