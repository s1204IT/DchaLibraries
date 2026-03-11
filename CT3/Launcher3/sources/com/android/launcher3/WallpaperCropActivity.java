package com.android.launcher3;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Toast;
import com.android.gallery3d.common.BitmapCropTask;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.launcher3.base.BaseActivity;
import com.android.launcher3.util.WallpaperUtils;
import com.android.photos.BitmapRegionTileSource;
import com.android.photos.views.TiledImageRenderer;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class WallpaperCropActivity extends BaseActivity implements Handler.Callback {
    protected CropView mCropView;
    LoadRequest mCurrentLoadRequest;
    private Handler mLoaderHandler;
    private HandlerThread mLoaderThread;
    protected View mProgressView;
    protected View mSetWallpaperButton;
    private byte[] mTempStorageForDecoding = new byte[16384];
    Set<Bitmap> mReusableBitmaps = Collections.newSetFromMap(new WeakHashMap());
    private final DialogInterface.OnCancelListener mOnDialogCancelListener = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            WallpaperCropActivity.this.getActionBar().show();
            View wallpaperStrip = WallpaperCropActivity.this.findViewById(R.id.wallpaper_strip);
            if (wallpaperStrip != null) {
                wallpaperStrip.setVisibility(0);
            }
            if (WallpaperCropActivity.this.mProgressView == null) {
                return;
            }
            WallpaperCropActivity.this.mProgressView.setVisibility(8);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mLoaderThread = new HandlerThread("wallpaper_loader");
        this.mLoaderThread.start();
        this.mLoaderHandler = new Handler(this.mLoaderThread.getLooper(), this);
        init();
        if (enableRotation()) {
            return;
        }
        setRequestedOrientation(1);
    }

    protected void init() {
        setContentView(R.layout.wallpaper_cropper);
        this.mCropView = (CropView) findViewById(R.id.cropView);
        this.mProgressView = findViewById(R.id.loading);
        Intent cropIntent = getIntent();
        final Uri imageUri = cropIntent.getData();
        if (imageUri == null) {
            Log.e("Launcher3.CropActivity", "No URI passed in intent, exiting WallpaperCropActivity");
            finish();
            return;
        }
        final ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.actionbar_set_wallpaper);
        actionBar.getCustomView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionBar.hide();
                WallpaperCropActivity.this.cropImageAndSetWallpaper(imageUri, (BitmapCropTask.OnBitmapCroppedHandler) null, true, false);
            }
        });
        this.mSetWallpaperButton = findViewById(R.id.set_wallpaper_button);
        final BitmapRegionTileSource.UriBitmapSource bitmapSource = new BitmapRegionTileSource.UriBitmapSource(getContext(), imageUri);
        this.mSetWallpaperButton.setEnabled(false);
        Runnable onLoad = new Runnable() {
            @Override
            public void run() {
                if (bitmapSource.getLoadingState() != BitmapRegionTileSource.BitmapSource.State.LOADED) {
                    Toast.makeText(WallpaperCropActivity.this.getContext(), R.string.wallpaper_load_fail, 1).show();
                    WallpaperCropActivity.this.finish();
                } else {
                    WallpaperCropActivity.this.mSetWallpaperButton.setEnabled(true);
                }
            }
        };
        setCropViewTileSource(bitmapSource, true, false, null, onLoad);
    }

    @Override
    public void onDestroy() {
        if (this.mCropView != null) {
            this.mCropView.destroy();
        }
        if (this.mLoaderThread != null) {
            this.mLoaderThread.quit();
        }
        super.onDestroy();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == 1) {
            final LoadRequest req = (LoadRequest) msg.obj;
            try {
                req.src.loadInBackground(new BitmapRegionTileSource.BitmapSource.InBitmapProvider() {
                    @Override
                    public Bitmap forPixelCount(int count) {
                        Bitmap bitmapToReuse = null;
                        synchronized (WallpaperCropActivity.this.mReusableBitmaps) {
                            int currentBitmapSize = Integer.MAX_VALUE;
                            for (Bitmap b : WallpaperCropActivity.this.mReusableBitmaps) {
                                int bitmapSize = b.getWidth() * b.getHeight();
                                if (bitmapSize >= count && bitmapSize < currentBitmapSize) {
                                    bitmapToReuse = b;
                                    currentBitmapSize = bitmapSize;
                                }
                            }
                            if (bitmapToReuse != null) {
                                WallpaperCropActivity.this.mReusableBitmaps.remove(bitmapToReuse);
                            }
                        }
                        return bitmapToReuse;
                    }
                });
                req.result = new BitmapRegionTileSource(getContext(), req.src, this.mTempStorageForDecoding);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (req == WallpaperCropActivity.this.mCurrentLoadRequest) {
                            WallpaperCropActivity.this.onLoadRequestComplete(req, req.src.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED);
                        } else {
                            WallpaperCropActivity.this.addReusableBitmap(req.result);
                        }
                    }
                });
                return true;
            } catch (SecurityException securityException) {
                if (isActivityDestroyed()) {
                    return true;
                }
                throw securityException;
            }
        }
        return false;
    }

    @TargetApi(17)
    protected boolean isActivityDestroyed() {
        if (Utilities.ATLEAST_JB_MR1) {
            return isDestroyed();
        }
        return false;
    }

    void addReusableBitmap(TiledImageRenderer.TileSource src) {
        Bitmap preview;
        synchronized (this.mReusableBitmaps) {
            if (Utilities.ATLEAST_KITKAT && (src instanceof BitmapRegionTileSource) && (preview = ((BitmapRegionTileSource) src).getBitmap()) != null && preview.isMutable()) {
                this.mReusableBitmaps.add(preview);
            }
        }
    }

    public DialogInterface.OnCancelListener getOnDialogCancelListener() {
        return this.mOnDialogCancelListener;
    }

    protected void onLoadRequestComplete(LoadRequest req, boolean success) {
        this.mCurrentLoadRequest = null;
        if (success) {
            TiledImageRenderer.TileSource oldSrc = this.mCropView.getTileSource();
            this.mCropView.setTileSource(req.result, null);
            this.mCropView.setTouchEnabled(req.touchEnabled);
            if (req.moveToLeft) {
                this.mCropView.moveToLeft();
            }
            if (req.scaleAndOffsetProvider != null) {
                req.scaleAndOffsetProvider.updateCropView(this, req.result);
            }
            if (oldSrc != null) {
                oldSrc.getPreview().yield();
            }
            addReusableBitmap(oldSrc);
        }
        if (req.postExecute != null) {
            req.postExecute.run();
        }
        this.mProgressView.setVisibility(8);
    }

    public final void setCropViewTileSource(BitmapRegionTileSource.BitmapSource bitmapSource, boolean touchEnabled, boolean moveToLeft, CropViewScaleAndOffsetProvider scaleProvider, Runnable postExecute) {
        final LoadRequest req = new LoadRequest();
        req.moveToLeft = moveToLeft;
        req.src = bitmapSource;
        req.touchEnabled = touchEnabled;
        req.postExecute = postExecute;
        req.scaleAndOffsetProvider = scaleProvider;
        this.mCurrentLoadRequest = req;
        this.mLoaderHandler.removeMessages(1);
        Message.obtain(this.mLoaderHandler, 1, req).sendToTarget();
        this.mProgressView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (WallpaperCropActivity.this.mCurrentLoadRequest != req) {
                    return;
                }
                WallpaperCropActivity.this.mProgressView.setVisibility(0);
            }
        }, 1000L);
    }

    public boolean enableRotation() {
        return getResources().getBoolean(R.bool.allow_rotation);
    }

    protected void setWallpaper(Uri uri, final boolean finishActivityWhenDone, final boolean shouldFadeOutOnFinish) {
        int rotation = BitmapUtils.getRotationFromExif(getContext(), uri);
        BitmapCropTask cropTask = new BitmapCropTask(getContext(), uri, null, rotation, 0, 0, true, false, null);
        final Point bounds = cropTask.getImageBounds();
        BitmapCropTask.OnEndCropHandler onEndCrop = new BitmapCropTask.OnEndCropHandler() {
            @Override
            public void run(boolean cropSucceeded) {
                WallpaperCropActivity.this.updateWallpaperDimensions(bounds.x, bounds.y);
                if (!finishActivityWhenDone) {
                    return;
                }
                WallpaperCropActivity.this.setResult(-1);
                WallpaperCropActivity.this.finish();
                if (!cropSucceeded || !shouldFadeOutOnFinish) {
                    return;
                }
                WallpaperCropActivity.this.overridePendingTransition(0, R.anim.fade_out);
            }
        };
        cropTask.setOnEndRunnable(onEndCrop);
        cropTask.setNoCrop(true);
        NycWallpaperUtils.executeCropTaskAfterPrompt(this, cropTask, getOnDialogCancelListener());
    }

    protected void cropImageAndSetWallpaper(Resources res, int resId, final boolean finishActivityWhenDone, final boolean shouldFadeOutOnFinish) {
        int rotation = BitmapUtils.getRotationFromExif(res, resId);
        Point inSize = this.mCropView.getSourceDimensions();
        Point outSize = WallpaperUtils.getDefaultWallpaperSize(getResources(), getWindowManager());
        RectF crop = Utils.getMaxCropRect(inSize.x, inSize.y, outSize.x, outSize.y, false);
        BitmapCropTask.OnEndCropHandler onEndCrop = new BitmapCropTask.OnEndCropHandler() {
            @Override
            public void run(boolean cropSucceeded) {
                WallpaperCropActivity.this.updateWallpaperDimensions(0, 0);
                if (!finishActivityWhenDone) {
                    return;
                }
                WallpaperCropActivity.this.setResult(-1);
                WallpaperCropActivity.this.finish();
                if (!cropSucceeded || !shouldFadeOutOnFinish) {
                    return;
                }
                WallpaperCropActivity.this.overridePendingTransition(0, R.anim.fade_out);
            }
        };
        BitmapCropTask cropTask = new BitmapCropTask(getContext(), res, resId, crop, rotation, outSize.x, outSize.y, true, false, onEndCrop);
        NycWallpaperUtils.executeCropTaskAfterPrompt(this, cropTask, getOnDialogCancelListener());
    }

    @TargetApi(17)
    protected void cropImageAndSetWallpaper(Uri uri, BitmapCropTask.OnBitmapCroppedHandler onBitmapCroppedHandler, final boolean finishActivityWhenDone, final boolean shouldFadeOutOnFinish) {
        float extraSpace;
        this.mProgressView.setVisibility(0);
        boolean centerCrop = getResources().getBoolean(R.bool.center_crop);
        boolean ltr = this.mCropView.getLayoutDirection() == 0;
        Display d = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        d.getSize(displaySize);
        boolean isPortrait = displaySize.x < displaySize.y;
        Point defaultWallpaperSize = WallpaperUtils.getDefaultWallpaperSize(getResources(), getWindowManager());
        RectF cropRect = this.mCropView.getCrop();
        Point inSize = this.mCropView.getSourceDimensions();
        int cropRotation = this.mCropView.getImageRotation();
        float cropScale = this.mCropView.getWidth() / cropRect.width();
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(cropRotation);
        float[] rotatedInSize = {inSize.x, inSize.y};
        rotateMatrix.mapPoints(rotatedInSize);
        rotatedInSize[0] = Math.abs(rotatedInSize[0]);
        rotatedInSize[1] = Math.abs(rotatedInSize[1]);
        cropRect.left = Math.max(0.0f, cropRect.left);
        cropRect.right = Math.min(rotatedInSize[0], cropRect.right);
        cropRect.top = Math.max(0.0f, cropRect.top);
        cropRect.bottom = Math.min(rotatedInSize[1], cropRect.bottom);
        if (centerCrop) {
            extraSpace = 2.0f * Math.min(rotatedInSize[0] - cropRect.right, cropRect.left);
        } else {
            extraSpace = ltr ? rotatedInSize[0] - cropRect.right : cropRect.left;
        }
        float maxExtraSpace = (defaultWallpaperSize.x / cropScale) - cropRect.width();
        float extraSpace2 = Math.min(extraSpace, maxExtraSpace);
        if (centerCrop) {
            cropRect.left -= extraSpace2 / 2.0f;
            cropRect.right += extraSpace2 / 2.0f;
        } else if (ltr) {
            cropRect.right += extraSpace2;
        } else {
            cropRect.left -= extraSpace2;
        }
        if (isPortrait) {
            cropRect.bottom = cropRect.top + (defaultWallpaperSize.y / cropScale);
        } else {
            float extraPortraitHeight = (defaultWallpaperSize.y / cropScale) - cropRect.height();
            float expandHeight = Math.min(Math.min(rotatedInSize[1] - cropRect.bottom, cropRect.top), extraPortraitHeight / 2.0f);
            cropRect.top -= expandHeight;
            cropRect.bottom += expandHeight;
        }
        final int outWidth = Math.round(cropRect.width() * cropScale);
        final int outHeight = Math.round(cropRect.height() * cropScale);
        BitmapCropTask.OnEndCropHandler onEndCrop = new BitmapCropTask.OnEndCropHandler() {
            @Override
            public void run(boolean cropSucceeded) {
                WallpaperCropActivity.this.updateWallpaperDimensions(outWidth, outHeight);
                if (finishActivityWhenDone) {
                    WallpaperCropActivity.this.setResult(-1);
                    WallpaperCropActivity.this.finish();
                }
                if (!cropSucceeded || !shouldFadeOutOnFinish) {
                    return;
                }
                WallpaperCropActivity.this.overridePendingTransition(0, R.anim.fade_out);
            }
        };
        BitmapCropTask cropTask = new BitmapCropTask(getContext(), uri, cropRect, cropRotation, outWidth, outHeight, true, false, onEndCrop);
        if (onBitmapCroppedHandler != null) {
            cropTask.setOnBitmapCropped(onBitmapCroppedHandler);
        }
        NycWallpaperUtils.executeCropTaskAfterPrompt(this, cropTask, getOnDialogCancelListener());
    }

    protected void updateWallpaperDimensions(int width, int height) {
        SharedPreferences sp = getContext().getSharedPreferences("com.android.launcher3.WallpaperCropActivity", 4);
        SharedPreferences.Editor editor = sp.edit();
        if (width != 0 && height != 0) {
            editor.putInt("wallpaper.width", width);
            editor.putInt("wallpaper.height", height);
        } else {
            editor.remove("wallpaper.width");
            editor.remove("wallpaper.height");
        }
        editor.apply();
        WallpaperUtils.suggestWallpaperDimension(getResources(), sp, getWindowManager(), WallpaperManager.getInstance(getContext()), true);
    }

    static class LoadRequest {
        boolean moveToLeft;
        Runnable postExecute;
        TiledImageRenderer.TileSource result;
        CropViewScaleAndOffsetProvider scaleAndOffsetProvider;
        BitmapRegionTileSource.BitmapSource src;
        boolean touchEnabled;

        LoadRequest() {
        }
    }

    public static class CropViewScaleAndOffsetProvider {
        public float getScale(Point wallpaperSize, RectF crop) {
            return 1.0f;
        }

        public float getParallaxOffset() {
            return 0.5f;
        }

        public void updateCropView(WallpaperCropActivity a, TiledImageRenderer.TileSource src) {
            Point wallpaperSize = WallpaperUtils.getDefaultWallpaperSize(a.getResources(), a.getWindowManager());
            RectF crop = Utils.getMaxCropRect(src.getImageWidth(), src.getImageHeight(), wallpaperSize.x, wallpaperSize.y, false);
            float scale = getScale(wallpaperSize, crop);
            PointF center = a.mCropView.getCenter();
            float offset = Math.max(0.0f, Math.min(getParallaxOffset(), 1.0f));
            float screenWidth = a.mCropView.getWidth() / scale;
            center.x = (screenWidth / 2.0f) + ((crop.width() - screenWidth) * offset) + crop.left;
            a.mCropView.setScaleAndCenter(scale, center.x, center.y);
        }
    }
}
