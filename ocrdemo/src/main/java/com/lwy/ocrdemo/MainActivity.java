package com.lwy.ocrdemo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.lwy.ocrdemo.utils.BitmapUtil;
import com.lwy.ocrdemo.utils.CameraUtil;
import com.lwy.ocrdemo.view.PreviewBorderView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by lwy on 2018/7/4.
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.AutoFocusCallback {
    private static final String TAG = MainActivity.class.getSimpleName();

    private SurfaceView mSvCamera;
    private float mPointX, mPointY;
    static final int FOCUS = 1;            // 聚焦
    static final int ZOOM = 2;            // 缩放
    private int mMode;                      //0是聚焦 1是放大
    //放大缩小
    int curZoomValue = 0;

    private int mCameraPosition = 0; // 0表示后置，1表示前置

    private SurfaceHolder mSvHolder;
    private Camera mCamera;
    //    private Camera.CameraInfo mCameraInfo;
    private MediaPlayer mShootMP;
    private float mDist;
    private int mScreenOrientation;
    private ImageView mCapturePhoto;
    private PreviewBorderView mBorderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSvCamera = findViewById(R.id.svCamera);
        mCapturePhoto = findViewById(R.id.ivCapturePhoto);
        mBorderView = findViewById(R.id.borderView);
        CameraUtil.init(this);
        initData();
    }

    @Override
    public void onPause() {
        super.onPause();
        /**
         * 记得释放camera，方便其他应用调用
         */
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = getCamera(mCameraPosition);
            startPreview(mCamera, mSvHolder);
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        FrameLayout.LayoutParams imgParams = (FrameLayout.LayoutParams) mCapturePhoto.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//             横屏
            mScreenOrientation = Configuration.ORIENTATION_LANDSCAPE;
            imgParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        } else {
//            竖屏
            mScreenOrientation = Configuration.ORIENTATION_PORTRAIT;
            imgParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        }
        mCapturePhoto.setLayoutParams(imgParams);
        startPreview(mCamera, mSvHolder);
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 初始化相关data
     */
    private void initData() {
        // 获得句柄
        mSvHolder = mSvCamera.getHolder(); // 获得句柄
        mSvHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // 添加回调
        mSvHolder.addCallback(this);
        mSvCamera.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    // 主点按下
                    case MotionEvent.ACTION_DOWN:
                        mPointX = event.getX();
                        mPointY = event.getY();
                        mMode = FOCUS;
                        break;
                    // 副点按下
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mDist = spacing(event);
                        // 如果连续两点距离大于10，则判定为多点模式
                        if (spacing(event) > 10f) {
                            mMode = ZOOM;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mMode = FOCUS;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d("Camera", "Is support Zoom mMode:" + mMode);
                        if (mMode == FOCUS) {
                        } else if (mMode == ZOOM) {
                            float newDist = spacing(event);
                            if (newDist > 10f) {
                                float tScale = (newDist - mDist) / mDist;
                                if (tScale < 0) {
                                    tScale = tScale * 10;
                                }
                                addZoomIn((int) tScale);
                            }
                        }
                        break;
                }
                return false;
            }
        });

        mSvCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    pointFocus((int) mPointX, (int) mPointY);
                } catch (Exception e) {
                    Log.d(this.getClass().getSimpleName(), e.toString());
                }
//                RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(focusIndex.getLayoutParams());
//                layout.setMargins((int) mPointX - 60, (int) mPointY - 60, 0, 0);
//                focusIndex.setLayoutParams(layout);
//                focusIndex.setVisibility(View.VISIBLE);
//                ScaleAnimation sa = new ScaleAnimation(3f, 1f, 3f, 1f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
//                sa.setDuration(800);
//                focusIndex.startAnimation(sa);
//
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        focusIndex.setVisibility(View.INVISIBLE);
//                    }
//                }, 700);
            }
        });
    }


    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    private void addZoomIn(int delta) {
        try {
            Camera.Parameters params = mCamera.getParameters();
            Log.d("Camera", "Is support Zoom " + params.isZoomSupported());
            if (!params.isZoomSupported()) {
                return;
            }
            curZoomValue += delta;
            if (curZoomValue < 0) {
                curZoomValue = 0;
            } else if (curZoomValue > params.getMaxZoom()) {
                curZoomValue = params.getMaxZoom();
            }

            if (!params.isSmoothZoomSupported()) {
                params.setZoom(curZoomValue);
                mCamera.setParameters(params);
                return;
            } else {
                mCamera.startSmoothZoom(curZoomValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 两点的距离
     */
    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 获取Camera实例
     */
    private Camera getCamera(int id) {
        Camera camera = null;
        try {
            camera = Camera.open(id);
        } catch (Exception e) {
            Log.d(this.getClass().getSimpleName(), e.toString());
        }
        return camera;
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ivCapturePhoto:
                // 拍照,设置相关参数
                try {
                    mCamera.takePicture(shutterCallback, null, jpgPictureCallback);
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    //定点对焦的代码
    private void pointFocus(int x, int y) {
        mCamera.cancelAutoFocus();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//            parameters = mCamera.getParameters();
//            showPoint(x, y);
//            mCamera.setParameters(parameters);
//        }
        autoFocus();
    }

    //实现自动对焦
    private void autoFocus() {
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mCamera == null) {
                    return;
                }
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            setupCamera(camera);//实现相机的参数初始化
                            camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。
                        }
                    }
                });
            }
        }.start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        releaseCamera();
        mCamera = getCamera(mCameraPosition);
        startPreview(mCamera, holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();
        startPreview(mCamera, holder);
        autoFocus();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 当surfaceview关闭时，关闭预览并释放资源
        /**
         * 记得释放camera，方便其他应用调用
         */
        releaseCamera();
    }

    /**
     * TakePicture回调
     */
    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            shootSound();
            mCamera.setOneShotPreviewCallback(previewCallback);
        }
    };

    Camera.PictureCallback rawPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.startPreview();
        }
    };

    Camera.PictureCallback jpgPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                camera.startPreview();
                Bitmap bitmap = Bytes2Bimap(data);
                if (mScreenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                } else {
                    bitmap = BitmapUtil.createRotateBitmap(bitmap);
                }
                Bitmap sizeBitmap = Bitmap.createScaledBitmap(bitmap, mSvCamera.getWidth(), mSvCamera.getHeight(), true);
                bitmap.recycle();
                bitmap = sizeBitmap;
                int[] location = new int[2];
                float rate = 2 / 3f;
                int reactHeight = 0;
                int reactWidth = 0;
                if (mScreenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    reactHeight = (int) (mSvCamera.getHeight() * rate);
                    reactWidth = (int) (reactHeight * 1.6);
                    float left = mSvCamera.getWidth() / 2 - reactWidth / 2;
                    float top = mSvCamera.getHeight() / 5;
                    location[0] = (int) left;
                    location[1] = (int) top;
                } else {
                    reactWidth = (int) (mSvCamera.getWidth() * rate);
                    reactHeight = (int) (reactWidth / 1.6);
                    float left = mSvCamera.getWidth() / 2 - reactWidth / 2;
                    float top = mSvCamera.getHeight() / 2 - reactHeight / 2;
                    location[0] = (int) left;
                    location[1] = (int) top;
                }
                Bitmap normalBitmap = Bitmap.createBitmap(bitmap, location[0], location[1], reactWidth, reactHeight);
                bitmap.recycle();
                bitmap = normalBitmap;
                goNext(Bitmap2Bytes(bitmap));
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }
    };


//    public static void setPictureDegreeZero(String path) {
//        try {
//            ExifInterface exifInterface = new ExifInterface(path);
//            // 修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
//            // 例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
//            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
//            exifInterface.saveAttributes();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    /**
     * 预览相机
     */
    private void startPreview(Camera camera, SurfaceHolder holder) {
        try {
            setupCamera(camera);
            camera.setPreviewDisplay(holder);
            CameraUtil.getInstance().setCameraDisplayOrientation(this, mCameraPosition, camera);
            camera.startPreview();
        } catch (IOException e) {
            Log.d(this.getClass().getSimpleName(), e.toString());
        }
    }

    private void setupCamera(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setJpegQuality(100);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            // Autofocus mMode is supported 自动对焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        int rootViewW = CameraUtil.screenWidth;
        int rootViewH = CameraUtil.screenHeight;
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            rootViewW = CameraUtil.screenHeight;
            rootViewH = CameraUtil.screenWidth;
        }

        int picHeight = rootViewH - getStatusBarHeight(this);
        Camera.Size previewSize = CameraUtil.findBestPreviewResolution(camera);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        Camera.Size pictrueSize = CameraUtil.getInstance().getPropPictureSize(parameters.getSupportedPictureSizes(), 1000);
        parameters.setPictureSize(pictrueSize.width, pictrueSize.height);

        camera.setParameters(parameters);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(rootViewW, picHeight);
        mBorderView.setLayoutParams(params);
        mSvCamera.setLayoutParams(params);

    }

    /**
     * 播放系统拍照声音
     */
    private void shootSound() {
        AudioManager meng = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        if (volume != 0) {
            if (mShootMP == null)
                mShootMP = MediaPlayer.create(this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (mShootMP != null)
                mShootMP.start();
        }
    }

    /**
     * 获取Preview界面的截图，并存储
     */
    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

        }
    };

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    //Get status bar height
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            result = context.getResources().getDimensionPixelOffset(resId);
        }
        return result;
    }


    /**
     * 打开闪光灯
     */
    public synchronized void openLight() {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
        }
    }

    /**
     * 关闭闪光灯
     */
    public synchronized void offLight() {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
        }
    }


    //byte转Bitmap
    public Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    //bitmap转byte
    public byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    private void goNext(byte[] data) {
        StaticValue.sBitmaData = data;
        Intent intent = new Intent(this, ResultActivity.class);

        startActivity(intent);
    }

}


