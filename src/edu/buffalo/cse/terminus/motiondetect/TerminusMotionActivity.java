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
                    learningRate 	= .05;
                    mogBgSub 		= new BackgroundSubtractorMOG(3, 4, 0.8);
                    
                    mRgba 			= new Mat();
                    mRgb 			= new Mat();
                    mFGMask			= new Mat();
                    
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

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.motiondetect_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        
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
    	
    	// ********* background subtraction **********
    	// the apply function will throw an error if you don't feed it an RGB image
    	// but it exports a gray image, so we need to convert the gray MAT
    	// into RGB before we apply it to the foreground mask
        Imgproc.cvtColor(inputFrame.gray(), mRgb, Imgproc.COLOR_GRAY2RGB);

        //apply() exports a gray image by definition
		mogBgSub.apply(mRgb, mFGMask, learningRate);
		
		// re-init or the old contours will stay on screen 
		contours = new ArrayList<MatOfPoint>();
		
		Imgproc.findContours(mFGMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		mRgba = inputFrame.rgba();
		
		// will draw the outlines on the regions...
		//Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
		
		for (int i = 0; i < contours.size(); i++) {
			// more efficient - how can i do this?
			//Rect r = Imgproc.boundingRect(contours.get(i));
			Core.rectangle(mRgba, Imgproc.boundingRect(contours.get(i)).tl(), Imgproc.boundingRect(contours.get(i)).br(), CONTOUR_COLOR, 3);
		}
		
		// to show the undelying mask for tuning...
		//return mFGMask;
		
        return mRgba;
    }
    
    public native void FindFeatures(long matAddrGr, long matAddrRgba);
}
