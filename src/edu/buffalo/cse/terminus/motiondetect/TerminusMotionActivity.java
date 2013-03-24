package edu.buffalo.cse.terminus.motiondetect;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import org.opencv.video.BackgroundSubtractorMOG;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

public class TerminusMotionActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;

    double learningRate;   
    private BackgroundSubtractorMOG mogBgSub;
    
    private Mat mGray;
    private Mat mRgba;
	private Mat mRgb;
	private Mat mFGMask;
	private Mat mHierarchy;
	private List<MatOfPoint> contours;
	private Scalar CONTOUR_COLOR;
	private Size ksize;
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                 
                    // for background removal...
                    // need to tweak these
                  //learningRate 	= .05;
                    learningRate	= .1;
                  //mogBgSub 		= new BackgroundSubtractorMOG();
                    mogBgSub 		= new BackgroundSubtractorMOG(3, 4, 0.8);
                        
                    // try backgroundsubtractormog2 ?
                    
                    mGray 			= new Mat();
                    mRgba 			= new Mat();
                    mRgb 			= new Mat();
                    mFGMask			= new Mat();
                    
                    ksize			= new Size(25,25);
                    
                    // for contours
                    mHierarchy  	= new Mat();
                    contours 		= new ArrayList<MatOfPoint>();
                    CONTOUR_COLOR 	= new Scalar(255,0,0,255);
                    
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public TerminusMotionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.motiondetect_surface_view);

        //mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.motiondetect_activity_java_surface_view);
        
        // native camera is slightly faster...
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.motiondetect_activity_native_surface_view);
        
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        
        mOpenCvCameraView.setMaxFrameSize(800, 800);  

        
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	
		mRgba = inputFrame.rgba();
    	mGray = inputFrame.gray();
    	
    	// 6.2 fps here
    	
    	// ********* background subtraction **********
    	
    	// the apply function will throw an error if you don't feed it an RGB image
    	// but it exports a gray image, so we need to convert the gray MAT
    	// into RGB before we apply it to the foreground mask
        Imgproc.cvtColor(mGray, mRgb, Imgproc.COLOR_GRAY2RGB);
        // 5.2 fps here
        
        
        //apply() exports a gray image by definition
		mogBgSub.apply(mRgb, mFGMask, learningRate);
		
		// 0.27 fps here, ugh.
				
		// blur the fgmask to reduce the number of contours
        Imgproc.GaussianBlur(mFGMask, mFGMask, ksize, 0);
		
		// debug
		//if(true) return mRgb;
		//if(true) return mFGMask;
		
		// re-init or the old contours will stay on screen 
		contours = new ArrayList<MatOfPoint>();
		
		Imgproc.findContours(mFGMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		// will draw the outlines on the regions - debug
		//Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
		
		Rect r;
		for (int i = 0; i < contours.size(); i++) {
			r = Imgproc.boundingRect(contours.get(i));
			// if bounding rect larger than min area, draw rect on screen
			if(r.area()>1200) {
				// we have motion!
				Core.rectangle(mRgba, r.tl(), r.br(), CONTOUR_COLOR, 3);
			}
		}
		
		return mRgba;
        
    }
    
    public native void FindFeatures(long matAddrGr, long matAddrRgba);
}
