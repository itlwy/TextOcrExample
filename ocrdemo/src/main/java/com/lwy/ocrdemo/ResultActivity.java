package com.lwy.ocrdemo;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.lwy.ocrdemo.utils.FileUtil;
import com.lwy.ocrdemo.utils.PictureHandler;
import com.lwy.ocrdemo.utils.ThreadManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lwy on 2018/7/4.
 */

public class ResultActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ResultActivity";
    private Handler mHandler;
    private ProgressDialog mDialog;
    private TextView mTextView2;
    private TextView mTextView3;
    private TextView mTextView4;
    private TextView mTextView5;
    private TextView mTextView6;
    private TextView mTextView7;
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString()
            + "/Tess";
    /**
     * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    private static final String tessdata = DATA_PATH + File.separator + "tessdata";
    public static String language = "chi_sim";
    private ImageView mImageView1;
    private ImageView mImageView2;
    private ImageView mImageView3;
    private ImageView mImageView4;
    private ImageView mImageView5;
    private ImageView mImageView6;
    private ImageView mImageView7;
    private TextView mPicTimetv;
    private TextView mRecTimetv;
    private long mPicTimeStart;
    private long mPicTimeEnd;
    private long mRecTimeStart;
    private long mRecTimeEnd;
    private Bitmap[] mBitmaps;
    private int mRecogRunningCount;

    static class MyHandler extends Handler {

        private WeakReference<ResultActivity> wf;

        public MyHandler(ResultActivity activity) {
            wf = new WeakReference(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (wf.get() != null) {
                ResultActivity activity = wf.get();
                switch (msg.what) {
                    case 1:
                        activity.handleRecogResult(msg);
                        if (--activity.mRecogRunningCount == 0) {
                            activity.mRecTimeEnd = System.currentTimeMillis();
                            activity.mRecTimetv.setText("文字识别耗时:" + (activity.mRecTimeStart - activity.mRecTimeEnd));
                        }
                        break;
                    case 2:
                        activity.handleBitmap();
                        break;
                    case 0:
                        Toast.makeText(activity, String.format("识别失败:%s", msg.obj), Toast.LENGTH_SHORT).show();
                        break;
                }
                if (activity.mDialog != null && activity.mDialog.isShowing()) {
                    activity.mDialog.cancel();
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        initView();
        init();
    }

    private void init() {
        File dataDir = new File(tessdata);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            mDialog = new ProgressDialog(ResultActivity.this);
            mDialog.setMessage("拷贝训练数据中......");
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.show();
            ThreadManager.getInstance().createLongPool().execute(new Runnable() {
                @Override
                public void run() {

                    Message msg;
                    try {
                        FileUtil.assets2SDCard(ResultActivity.this, "chi_sim.traineddata", tessdata + File.separator +
                                language + ".traineddata");
                        msg = Message.obtain();
                        msg.what = 2;
                    } catch (IOException e) {
                        msg = Message.obtain();
                        msg.what = 0;
                        msg.obj = e.getMessage();
                    }
                    mHandler.sendMessage(msg);
                }
            });
        } else {
            handleBitmap();
        }
    }

    private void initView() {
        mImageView1 = findViewById(R.id.image1);
        mImageView2 = findViewById(R.id.image2);
        mImageView3 = findViewById(R.id.image3);
        mImageView4 = findViewById(R.id.image4);
        mImageView5 = findViewById(R.id.image5);
        mImageView6 = findViewById(R.id.image6);
        mImageView7 = findViewById(R.id.image7);
        mTextView2 = findViewById(R.id.text2);
        mTextView3 = findViewById(R.id.text3);
        mTextView4 = findViewById(R.id.text4);
        mTextView5 = findViewById(R.id.text5);
        mTextView6 = findViewById(R.id.text6);
        mTextView7 = findViewById(R.id.text7);
        mPicTimetv = findViewById(R.id.pic_time_tv);
        mRecTimetv = findViewById(R.id.rec_time_tv);
        findViewById(R.id.button).setOnClickListener(this);
        mHandler = new MyHandler(this);
    }

    private void handleBitmap() {
        new AsyncTask<Void, Void, Bitmap[]>() {
            @Override
            protected void onPreExecute() {
                mDialog = new ProgressDialog(ResultActivity.this);
                mDialog.setMessage("图片处理中......");
                mDialog.setCanceledOnTouchOutside(false);
                mDialog.show();
            }

            @Override
            protected Bitmap[] doInBackground(Void... voids) {
                Bitmap[] bitmaps = new Bitmap[7];
                mPicTimeStart = System.currentTimeMillis();
                Bitmap bitmap;
                try {
                    // 得到身份证
                    bitmap = Bytes2Bimap(StaticValue.sBitmaData);
//                    FileInputStream bitmapInputStream = new FileInputStream(DATA_PATH + "/1.jpg");  //取本地图片做测试
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//                    bitmap = BitmapFactory.decodeStream(bitmapInputStream);
                    bitmaps[0] = bitmap;
//                    bitmap = PictureHandler.getBinaryImage(bitmap, PictureHandler.TYPE_MATRIX);
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    // 得到姓名
                    bitmaps[1] = Bitmap.createBitmap(bitmap, width * 16 / 85, height * 2 / 55, width / 5,
                            height * 11 / 55);
                    bitmaps[1] = PictureHandler.getBinaryImage(bitmaps[1], PictureHandler.TYPE_MATRIX);
                    // 得到性别
                    bitmaps[2] = Bitmap.createBitmap(bitmap, width * 16 / 85, height * 13 / 55, width / 10,
                            height * 7 / 55);
                    bitmaps[2] = PictureHandler.getBinaryImage(bitmaps[2], PictureHandler.TYPE_MATRIX);


                    // 得到民族
                    bitmaps[3] = Bitmap.createBitmap(bitmap, width * 67 / 168, height * 13 / 55, width / 11,
                            height * 7 / 55);
                    bitmaps[3] = PictureHandler.getBinaryImage(bitmaps[3], PictureHandler.TYPE_MATRIX);

                    // 得到出生年月
                    bitmaps[4] = Bitmap.createBitmap(bitmap, width * 16 / 85, height * 20 / 55, width
                            * 30 / 85, height * 8 / 56);
                    bitmaps[4] = PictureHandler.getBinaryImage(bitmaps[4], PictureHandler.TYPE_MATRIX);

                    // 得到住址
                    bitmaps[5] = Bitmap.createBitmap(bitmap, width * 15 / 85, height * 27 / 55, width
                            * 38 / 85, height * 15 / 55);
                    bitmaps[5] = PictureHandler.getBinaryImage(bitmaps[5], PictureHandler.TYPE_MATRIX);

                    // 得到身份证号码
                    bitmaps[6] = Bitmap.createBitmap(bitmap, width * 27 / 85, height * 44 / 55, width * 50
                            / 85, height * 7 / 55);
                    bitmaps[6] = PictureHandler.getBinaryImage(bitmaps[6], PictureHandler.TYPE_MATRIX);
                } catch (final Exception e) {
                    Log.d(this.getClass().getSimpleName(), e.toString());
                    Message msg = Message.obtain();
                    msg.what = 0;
                    msg.obj = e.getMessage();
                    mHandler.sendMessage(msg);
                }
                return bitmaps;
            }

            @Override
            protected void onPostExecute(Bitmap[] bitmaps) {
                mBitmaps = bitmaps;
                mPicTimeEnd = System.currentTimeMillis();
                mPicTimetv.setText("图像处理时间:" + (mPicTimeEnd - mPicTimeStart));
                if (mBitmaps != null && mBitmaps.length > 00) {
                    mImageView1.setImageBitmap(bitmaps[0]);
                    mImageView2.setImageBitmap(bitmaps[1]);
                    mImageView3.setImageBitmap(bitmaps[2]);
                    mImageView4.setImageBitmap(bitmaps[3]);
                    mImageView5.setImageBitmap(bitmaps[4]);
                    mImageView6.setImageBitmap(bitmaps[5]);
                    mImageView7.setImageBitmap(bitmaps[6]);
                    mDialog.cancel();
                }

                super.onPostExecute(bitmaps);
            }
        }.execute();
    }

    private void handleRecogResult(Message msg) {
        int type = msg.arg1;
        switch (type) {
            case 1:
                mTextView2.setText((CharSequence) msg.obj);
                break;
            case 2:
                mTextView3.setText((CharSequence) msg.obj);
                break;
            case 3:
                mTextView4.setText((CharSequence) msg.obj);
                break;
            case 4:
                String time = (String) msg.obj;
                if (!TextUtils.isEmpty(time)) {
                    Pattern pattern = Pattern.compile("^(\\d{4}).+(\\d{1,2}).+(\\d{1,2}).+$");
                    Matcher matcher = pattern.matcher(time);
                    if (matcher.find()) {
                        time = String.format("%s年%s月%s日", matcher.group(1), matcher.group(2), matcher.group(3));
                    }
                }
                mTextView5.setText(time);
                break;
            case 5:
                mTextView6.setText((CharSequence) msg.obj);
                break;
            case 6:
                mTextView7.setText((CharSequence) msg.obj);
                break;
        }
    }

    /**
     * 改变亮度
     */
    public Bitmap changeLight(Bitmap bitmap, int brightness) {
        ColorMatrix cMatrix = new ColorMatrix();
        cMatrix.set(new float[]{1, 0, 0, 0, brightness, 0, 1,
                0, 0, brightness,// 改变亮度
                0, 0, 1, 0, brightness, 0, 0, 0, 1, 0});

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cMatrix));
        Canvas canvas = new Canvas(bitmap);
        // 在Canvas上绘制一个已经存在的Bitmap。这样，dstBitmap就和srcBitmap一摸一样了
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmap;
    }


    /**
     * 放大图片
     *
     * @param bitmap
     * @return
     */
    private static Bitmap big(Bitmap bitmap, float num) {
        Matrix matrix = new Matrix();
        matrix.postScale(num, num); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight
                (), matrix, true);
        return resizeBmp;
    }


    /**
     * byte转Bitmap
     *
     * @param b
     * @return
     */
    public Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//            options.inMutable = true;
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length, options);
            return bitmap;
        } else {
            return null;
        }
    }

    @Override
    public void onClick(final View view) {
        mDialog = new ProgressDialog(view.getContext());
        mDialog.setMessage("识别中......");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
        mRecTimeStart = System.currentTimeMillis();
        ThreadManager.getInstance().createLongPool().execute(new RecRunnable(mBitmaps[1], 1, mHandler));
        mRecogRunningCount++;
        ThreadManager.getInstance().createLongPool().execute(new RecRunnable(mBitmaps[2], 2, mHandler));
        mRecogRunningCount++;
        ThreadManager.getInstance().createLongPool().execute(new RecRunnable(mBitmaps[3], 3, mHandler));
        mRecogRunningCount++;
        ThreadManager.getInstance().createLongPool().execute(new RecRunnable(mBitmaps[4], 4, mHandler));
        mRecogRunningCount++;
        ThreadManager.getInstance().createLongPool().execute(new RecRunnable(mBitmaps[5], 5, mHandler));
        mRecogRunningCount++;
        ThreadManager.getInstance().createLongPool().execute(new RecRunnable(mBitmaps[6], 6, mHandler));
        mRecogRunningCount++;
    }

    public static class RecRunnable implements Runnable {

        private int mType;
        private Handler mHandler;
        private Bitmap mBitmap;


        public RecRunnable(Bitmap bitmap, int type, Handler handler) {
            mBitmap = bitmap;
            mHandler = handler;
            mType = type;
        }

        @Override
        public void run() {
            String text = getText(mBitmap);
            Message msg = Message.obtain();
            msg.what = 1;
            msg.obj = text;
            msg.arg1 = mType;
            mHandler.sendMessage(msg);
        }


        /**
         * 获取图片上的文字
         *
         * @param bitmap
         * @return
         */
        private String getText(Bitmap bitmap) {
            String retStr = "No result";
            try {
                TessBaseAPI tessBaseAPI = new TessBaseAPI();
                tessBaseAPI.init(DATA_PATH, language);
                // 识别黑名单
//            tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=÷-[]}{;:'\"\\|~`," +
//                    "./<>?" + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                tessBaseAPI.setImage(bitmap);
                retStr = tessBaseAPI.getUTF8Text();
                tessBaseAPI.clear();
                tessBaseAPI.end();
            } catch (Exception e) {
                Message msg = Message.obtain();
                msg.what = 0;
                msg.obj = e.getMessage();
                mHandler.sendMessage(msg);
            }
            return retStr;
        }
    }
}
