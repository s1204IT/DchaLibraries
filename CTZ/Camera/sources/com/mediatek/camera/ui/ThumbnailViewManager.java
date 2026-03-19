package com.mediatek.camera.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.widget.RotateImageView;
import com.mediatek.camera.portability.storage.StorageManagerExt;
import java.lang.reflect.Method;

class ThumbnailViewManager extends AbstractViewManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ThumbnailViewManager.class.getSimpleName());
    private static Method sGetDefaultPath;
    private static String sMountPoint;
    private RoundedBitmapDrawable mAnimationDrawable;
    private RotateImageView mAnimationView;
    private final Context mContext;
    private boolean mIsNeedQueryDB;
    private AsyncTask<Void, Void, Bitmap> mLoadBitmapTask;
    private Object mLock;
    private IAppUiListener.OnThumbnailClickedListener mOnClickListener;
    private final BroadcastReceiver mReceiver;
    private RoundedBitmapDrawable mRoundDrawable;
    private Bitmap mRoundedBitmap;
    private RotateImageView mThumbnailView;
    private int mThumbnailViewWidth;

    static {
        try {
            Class<?> cls = Class.forName("com.mediatek.storage.StorageManagerEx");
            if (cls != null) {
                sGetDefaultPath = cls.getDeclaredMethod("getDefaultPath", new Class[0]);
            }
            if (sGetDefaultPath != null) {
                sGetDefaultPath.setAccessible(true);
            }
        } catch (ClassNotFoundException e) {
            LogHelper.e(TAG, "ClassNotFoundException: com.mediatek.storage.StorageManagerEx");
        } catch (NoSuchMethodException e2) {
            LogHelper.e(TAG, "NoSuchMethodException: getDefaultPath");
        }
    }

    public ThumbnailViewManager(IApp iApp, ViewGroup viewGroup) {
        super(iApp, viewGroup);
        this.mRoundedBitmap = null;
        this.mLock = new Object();
        this.mIsNeedQueryDB = true;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LogHelper.i(ThumbnailViewManager.TAG, "mReceiver.onReceive(" + intent + ")");
                if (intent.getAction() == null) {
                    LogHelper.d(ThumbnailViewManager.TAG, "[mReceiver.onReceive] action is null");
                }
                String action = intent.getAction();
                byte b = -1;
                int iHashCode = action.hashCode();
                if (iHashCode != -1142424621) {
                    if (iHashCode != -963871873) {
                        if (iHashCode == -625887599 && action.equals("android.intent.action.MEDIA_EJECT")) {
                            b = 0;
                        }
                    } else if (action.equals("android.intent.action.MEDIA_UNMOUNTED")) {
                        b = 2;
                    }
                } else if (action.equals("android.intent.action.MEDIA_SCANNER_FINISHED")) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                    case Camera2Proxy.TEMPLATE_PREVIEW:
                        if ("android.media.action.STILL_IMAGE_CAMERA_SECURE".equals(ThumbnailViewManager.this.mApp.getActivity().getIntent().getAction())) {
                            LogHelper.d(ThumbnailViewManager.TAG, "[mReceiver.onReceive] security camera");
                            if (ThumbnailViewManager.this.isSameStorage(intent)) {
                                LogHelper.d(ThumbnailViewManager.TAG, "[mReceiver.onReceive] the eject media is same storage.");
                                ThumbnailViewManager.this.updateThumbnail(null);
                            }
                        } else {
                            ThumbnailViewManager.this.getLastThumbnail();
                        }
                        break;
                    case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                        String unused = ThumbnailViewManager.sMountPoint = StorageManagerExt.getDefaultPath();
                        break;
                }
            }
        };
        this.mContext = iApp.getActivity();
    }

    @Override
    protected View getView() {
        LogHelper.d(TAG, "[getView]...");
        this.mThumbnailView = (RotateImageView) this.mParentView.findViewById(R.id.thumbnail);
        this.mAnimationView = (RotateImageView) this.mParentView.findViewById(R.id.thumbnail_animation);
        this.mRoundDrawable = createRoundDrawable(null, -13619152);
        this.mAnimationDrawable = createRoundDrawable(null, -1);
        this.mThumbnailView.setImageDrawable(this.mRoundDrawable);
        this.mAnimationView.setImageDrawable(this.mAnimationDrawable);
        this.mThumbnailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ThumbnailViewManager.this.mOnClickListener != null) {
                    ThumbnailViewManager.this.mOnClickListener.onThumbnailClicked();
                }
            }
        });
        this.mThumbnailViewWidth = Math.min(this.mThumbnailView.getLayoutParams().width, this.mThumbnailView.getLayoutParams().height);
        return this.mThumbnailView;
    }

    public void setThumbnailClickedListener(IAppUiListener.OnThumbnailClickedListener onThumbnailClickedListener) {
        this.mOnClickListener = onThumbnailClickedListener;
    }

    @Override
    public void onResume() {
        LogHelper.d(TAG, "[onResume]");
        super.onResume();
        if (isExtendStorageCanUsed()) {
            registerIntentFilter();
            sMountPoint = StorageManagerExt.getDefaultPath();
        }
        if (this.mIsNeedQueryDB) {
            getLastThumbnail();
        }
        this.mIsNeedQueryDB = true;
    }

    @Override
    public void onPause() {
        LogHelper.d(TAG, "[onPause]");
        super.onPause();
        if (isExtendStorageCanUsed()) {
            unregisterIntentFilter();
        }
    }

    @Override
    public void setEnabled(boolean z) {
        LogHelper.d(TAG, "[setEnabled] enabled = " + z);
        if (this.mThumbnailView != null) {
            this.mThumbnailView.setClickable(z);
        }
    }

    public int getThumbnailViewWidth() {
        return this.mThumbnailViewWidth;
    }

    public void updateThumbnail(Bitmap bitmap) {
        updateThumbnailView(bitmap);
        if (bitmap != null) {
            doAnimation(this.mAnimationView);
        } else {
            this.mThumbnailView.setImageDrawable(this.mRoundDrawable);
            this.mIsNeedQueryDB = false;
        }
    }

    private RoundedBitmapDrawable createRoundDrawable(Bitmap bitmap, int i) {
        Bitmap bitmapCreateBitmap;
        int i2 = this.mThumbnailView.getLayoutParams().width;
        int i3 = this.mThumbnailView.getLayoutParams().height;
        this.mThumbnailView.setContentDescription("Has Content");
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(i2, i3, Bitmap.Config.ARGB_8888);
            this.mThumbnailView.setContentDescription("No Content");
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (height > width) {
            bitmapCreateBitmap = Bitmap.createBitmap(bitmap, 0, (height - width) / 2, width, width);
        } else {
            bitmapCreateBitmap = height < width ? Bitmap.createBitmap(bitmap, (width - height) / 2, 0, height, height) : bitmap;
        }
        Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmapCreateBitmap, i2, i3, true);
        int width2 = bitmapCreateScaledBitmap.getWidth();
        int height2 = bitmapCreateScaledBitmap.getHeight();
        int iMin = Math.min(width2, height2) / 2;
        int iMin2 = Math.min(width2, height2) - 4;
        this.mRoundedBitmap = Bitmap.createBitmap(iMin2, iMin2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(this.mRoundedBitmap);
        canvas.drawColor(i);
        canvas.drawBitmap(bitmapCreateScaledBitmap, iMin2 - width2, iMin2 - height2, (Paint) null);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(1291845632);
        canvas.drawCircle(canvas.getWidth() / 2, canvas.getWidth() / 2, iMin2 / 2, paint);
        RoundedBitmapDrawable roundedBitmapDrawableCreate = RoundedBitmapDrawableFactory.create(this.mApp.getActivity().getResources(), this.mRoundedBitmap);
        roundedBitmapDrawableCreate.setCornerRadius(iMin);
        roundedBitmapDrawableCreate.setAntiAlias(true);
        bitmap.recycle();
        bitmapCreateBitmap.recycle();
        bitmapCreateScaledBitmap.recycle();
        return roundedBitmapDrawableCreate;
    }

    private void cancelLoadThumbnail() {
        synchronized (this.mLock) {
            if (this.mLoadBitmapTask != null) {
                LogHelper.d(TAG, "[cancelLoadThumbnail]...");
                this.mLoadBitmapTask.cancel(true);
                this.mLoadBitmapTask = null;
            }
        }
    }

    private void updateThumbnailView(Bitmap bitmap) {
        LogHelper.d(TAG, "[updateThumbnailView]...");
        if (this.mThumbnailView != null) {
            if (bitmap != null) {
                LogHelper.d(TAG, "[updateThumbnailView] set created thumbnail");
                this.mRoundDrawable = createRoundDrawable(bitmap, -13619152);
            } else {
                LogHelper.d(TAG, "[updateThumbnailView] set default thumbnail");
                this.mRoundDrawable = createRoundDrawable(null, -13619152);
            }
        }
    }

    private void getLastThumbnail() {
        cancelLoadThumbnail();
        synchronized (this.mLock) {
            this.mLoadBitmapTask = new LoadBitmapTask().execute(new Void[0]);
        }
    }

    private class LoadBitmapTask extends AsyncTask<Void, Void, Bitmap> {
        public LoadBitmapTask() {
        }

        @Override
        protected Bitmap doInBackground(Void... voidArr) throws Throwable {
            LogHelper.d(ThumbnailViewManager.TAG, "[doInBackground]begin.");
            try {
                if (isCancelled()) {
                    LogHelper.w(ThumbnailViewManager.TAG, "[doInBackground]task is cancel,return.");
                    return null;
                }
                Bitmap lastBitmapFromDatabase = BitmapCreator.getLastBitmapFromDatabase(ThumbnailViewManager.this.mApp.getActivity().getContentResolver());
                ThumbnailViewManager.this.mApp.notifyNewMedia(BitmapCreator.getUriAfterQueryDb(), false);
                LogHelper.d(ThumbnailViewManager.TAG, "getLastBitmapFromDatabase bitmap = " + lastBitmapFromDatabase);
                return lastBitmapFromDatabase;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            LogHelper.d(ThumbnailViewManager.TAG, "[onPostExecute]isCancelled()=" + isCancelled());
            if (!isCancelled()) {
                ThumbnailViewManager.this.updateThumbnailView(bitmap);
                ThumbnailViewManager.this.mThumbnailView.setImageDrawable(ThumbnailViewManager.this.mRoundDrawable);
            }
        }
    }

    private void doAnimation(RotateImageView rotateImageView) {
        rotateImageView.clearAnimation();
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(0L);
        alphaAnimation.setInterpolator(new AccelerateInterpolator());
        alphaAnimation.setRepeatMode(2);
        alphaAnimation.setRepeatCount(1);
        rotateImageView.startAnimation(alphaAnimation);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                ThumbnailViewManager.this.mThumbnailView.setImageDrawable(ThumbnailViewManager.this.mRoundDrawable);
            }
        });
    }

    private void registerIntentFilter() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_EJECT");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addAction("android.intent.action.MEDIA_CHECKING");
        intentFilter.addDataScheme("file");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    private void unregisterIntentFilter() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    boolean isSameStorage(Intent intent) {
        if (isExtendStorageCanUsed()) {
            return StorageManagerExt.isSameStorage(intent, sMountPoint);
        }
        return false;
    }

    private boolean isExtendStorageCanUsed() {
        return Build.VERSION.SDK_INT >= 23 && isDefaultPathCanUsed();
    }

    private boolean isDefaultPathCanUsed() {
        if (sGetDefaultPath != null) {
            return true;
        }
        return false;
    }
}
