package com.android.wallpaperpicker;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Toast;
import com.android.gallery3d.common.Utils;
import com.android.photos.BitmapRegionTileSource;
import com.android.photos.views.TiledImageRenderer;
import com.android.wallpaperpicker.common.CropAndSetWallpaperTask;
import com.android.wallpaperpicker.common.DialogUtils;
import com.android.wallpaperpicker.common.InputStreamProvider;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class WallpaperCropActivity extends Activity implements Handler.Callback {
    protected CropView mCropView;
    private LoadRequest mCurrentLoadRequest;
    private Handler mLoaderHandler;
    private HandlerThread mLoaderThread;
    protected View mProgressView;
    protected View mSetWallpaperButton;
    private byte[] mTempStorageForDecoding = new byte[16384];
    private Set<Bitmap> mReusableBitmaps = Collections.newSetFromMap(new WeakHashMap());
    private final DialogInterface.OnCancelListener mOnDialogCancelListener = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialogInterface) {
            WallpaperCropActivity.this.showActionBarAndTiles();
        }
    };

    public interface CropViewScaleAndOffsetProvider {
        float getParallaxOffset();

        float getScale(Point point, RectF rectF);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mLoaderThread = new HandlerThread("wallpaper_loader");
        this.mLoaderThread.start();
        this.mLoaderHandler = new Handler(this.mLoaderThread.getLooper(), this);
        init();
        if (!enableRotation()) {
            setRequestedOrientation(1);
        }
    }

    protected void init() {
        setContentView(R.layout.wallpaper_cropper);
        this.mCropView = (CropView) findViewById(R.id.cropView);
        this.mProgressView = findViewById(R.id.loading);
        final Uri data = getIntent().getData();
        if (data == null) {
            Log.e("WallpaperCropActivity", "No URI passed in intent, exiting WallpaperCropActivity");
            finish();
            return;
        }
        final ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.actionbar_set_wallpaper);
        actionBar.getCustomView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionBar.hide();
                WallpaperCropActivity.this.cropImageAndSetWallpaper(data, (CropAndSetWallpaperTask.OnBitmapCroppedHandler) null, false);
            }
        });
        this.mSetWallpaperButton = findViewById(R.id.set_wallpaper_button);
        final BitmapRegionTileSource.InputStreamSource inputStreamSource = new BitmapRegionTileSource.InputStreamSource(this, data);
        this.mSetWallpaperButton.setEnabled(false);
        setCropViewTileSource(inputStreamSource, true, false, null, new Runnable() {
            @Override
            public void run() {
                if (inputStreamSource.getLoadingState() != BitmapRegionTileSource.BitmapSource.State.LOADED) {
                    Toast.makeText(WallpaperCropActivity.this, R.string.wallpaper_load_fail, 1).show();
                    WallpaperCropActivity.this.finish();
                } else {
                    WallpaperCropActivity.this.mSetWallpaperButton.setEnabled(true);
                }
            }
        });
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
    @TargetApi(19)
    public boolean handleMessage(Message message) {
        final boolean z = false;
        if (message.what != 1) {
            return false;
        }
        final LoadRequest loadRequest = (LoadRequest) message.obj;
        if (loadRequest.src == null) {
            Drawable builtInDrawable = WallpaperManager.getInstance(this).getBuiltInDrawable(this.mCropView.getWidth(), this.mCropView.getHeight(), false, 0.5f, 0.5f);
            if (builtInDrawable == null) {
                Log.w("WallpaperCropActivity", "Null default wallpaper encountered.");
            } else {
                loadRequest.result = new DrawableTileSource(this, builtInDrawable, 1024);
                z = true;
            }
        } else {
            try {
                loadRequest.src.loadInBackground(new BitmapRegionTileSource.BitmapSource.InBitmapProvider() {
                    @Override
                    public Bitmap forPixelCount(int i) {
                        Bitmap bitmap;
                        synchronized (WallpaperCropActivity.this.mReusableBitmaps) {
                            int i2 = Integer.MAX_VALUE;
                            bitmap = null;
                            for (Bitmap bitmap2 : WallpaperCropActivity.this.mReusableBitmaps) {
                                int width = bitmap2.getWidth() * bitmap2.getHeight();
                                if (width >= i && width < i2) {
                                    bitmap = bitmap2;
                                    i2 = width;
                                }
                            }
                            if (bitmap != null) {
                                WallpaperCropActivity.this.mReusableBitmaps.remove(bitmap);
                            }
                        }
                        return bitmap;
                    }
                });
                loadRequest.result = new BitmapRegionTileSource(this, loadRequest.src, this.mTempStorageForDecoding);
                if (loadRequest.src.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED) {
                    z = true;
                }
            } catch (SecurityException e) {
                if (isActivityDestroyed()) {
                    return true;
                }
                throw e;
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loadRequest != WallpaperCropActivity.this.mCurrentLoadRequest) {
                    WallpaperCropActivity.this.addReusableBitmap(loadRequest.result);
                } else {
                    WallpaperCropActivity.this.onLoadRequestComplete(loadRequest, z);
                }
            }
        });
        return true;
    }

    @TargetApi(17)
    public boolean isActivityDestroyed() {
        return Build.VERSION.SDK_INT >= 17 && isDestroyed();
    }

    private void addReusableBitmap(TiledImageRenderer.TileSource tileSource) {
        Bitmap bitmap;
        synchronized (this.mReusableBitmaps) {
            if (Build.VERSION.SDK_INT >= 19 && (tileSource instanceof BitmapRegionTileSource) && (bitmap = tileSource.getBitmap()) != null && bitmap.isMutable()) {
                this.mReusableBitmaps.add(bitmap);
            }
        }
    }

    public DialogInterface.OnCancelListener getOnDialogCancelListener() {
        return this.mOnDialogCancelListener;
    }

    private void showActionBarAndTiles() {
        getActionBar().show();
        View viewFindViewById = findViewById(R.id.wallpaper_strip);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(0);
        }
    }

    protected void onLoadRequestComplete(LoadRequest loadRequest, boolean z) {
        this.mCurrentLoadRequest = null;
        if (z) {
            TiledImageRenderer.TileSource tileSource = this.mCropView.getTileSource();
            this.mCropView.setTileSource(loadRequest.result, null);
            this.mCropView.setTouchEnabled(loadRequest.touchEnabled);
            if (loadRequest.moveToLeft) {
                this.mCropView.moveToLeft();
            }
            if (loadRequest.scaleAndOffsetProvider != null) {
                TiledImageRenderer.TileSource tileSource2 = loadRequest.result;
                Point defaultWallpaperSize = WallpaperUtils.getDefaultWallpaperSize(getResources(), getWindowManager());
                RectF maxCropRect = Utils.getMaxCropRect(tileSource2.getImageWidth(), tileSource2.getImageHeight(), defaultWallpaperSize.x, defaultWallpaperSize.y, false);
                this.mCropView.setScale(loadRequest.scaleAndOffsetProvider.getScale(defaultWallpaperSize, maxCropRect));
                this.mCropView.setParallaxOffset(loadRequest.scaleAndOffsetProvider.getParallaxOffset(), maxCropRect);
            }
            if (tileSource != null) {
                tileSource.getPreview().yield();
            }
            addReusableBitmap(tileSource);
        }
        if (loadRequest.postExecute != null) {
            loadRequest.postExecute.run();
        }
        this.mProgressView.setVisibility(8);
    }

    @TargetApi(19)
    public final void setCropViewTileSource(BitmapRegionTileSource.BitmapSource bitmapSource, boolean z, boolean z2, CropViewScaleAndOffsetProvider cropViewScaleAndOffsetProvider, Runnable runnable) {
        final LoadRequest loadRequest = new LoadRequest();
        loadRequest.moveToLeft = z2;
        loadRequest.src = bitmapSource;
        loadRequest.touchEnabled = z;
        loadRequest.postExecute = runnable;
        loadRequest.scaleAndOffsetProvider = cropViewScaleAndOffsetProvider;
        this.mCurrentLoadRequest = loadRequest;
        this.mLoaderHandler.removeMessages(1);
        Message.obtain(this.mLoaderHandler, 1, loadRequest).sendToTarget();
        this.mProgressView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (WallpaperCropActivity.this.mCurrentLoadRequest == loadRequest) {
                    WallpaperCropActivity.this.mProgressView.setVisibility(0);
                }
            }
        }, 1000L);
    }

    public boolean enableRotation() {
        return true;
    }

    public void cropImageAndSetWallpaper(Resources resources, int i, boolean z) {
        InputStreamProvider inputStreamProviderFromResource = InputStreamProvider.fromResource(resources, i);
        Point sourceDimensions = this.mCropView.getSourceDimensions();
        Point defaultWallpaperSize = WallpaperUtils.getDefaultWallpaperSize(getResources(), getWindowManager());
        DialogUtils.executeCropTaskAfterPrompt(this, new CropAndSetWallpaperTask(inputStreamProviderFromResource, this, Utils.getMaxCropRect(sourceDimensions.x, sourceDimensions.y, defaultWallpaperSize.x, defaultWallpaperSize.y, false), inputStreamProviderFromResource.getRotationFromExif(this), defaultWallpaperSize.x, defaultWallpaperSize.y, new CropAndFinishHandler(new Point(0, 0), z)), getOnDialogCancelListener());
    }

    @TargetApi(17)
    public void cropImageAndSetWallpaper(Uri uri, CropAndSetWallpaperTask.OnBitmapCroppedHandler onBitmapCroppedHandler, boolean z) {
        boolean z2 = this.mCropView.getLayoutDirection() == 0;
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        boolean z3 = point.x < point.y;
        Point defaultWallpaperSize = WallpaperUtils.getDefaultWallpaperSize(getResources(), getWindowManager());
        RectF crop = this.mCropView.getCrop();
        Point sourceDimensions = this.mCropView.getSourceDimensions();
        int imageRotation = this.mCropView.getImageRotation();
        float width = this.mCropView.getWidth() / crop.width();
        Matrix matrix = new Matrix();
        matrix.setRotate(imageRotation);
        float[] fArr = {sourceDimensions.x, sourceDimensions.y};
        matrix.mapPoints(fArr);
        fArr[0] = Math.abs(fArr[0]);
        fArr[1] = Math.abs(fArr[1]);
        crop.left = Math.max(0.0f, crop.left);
        crop.right = Math.min(fArr[0], crop.right);
        crop.top = Math.max(0.0f, crop.top);
        crop.bottom = Math.min(fArr[1], crop.bottom);
        float fMin = Math.min(z2 ? fArr[0] - crop.right : crop.left, (defaultWallpaperSize.x / width) - crop.width());
        if (z2) {
            crop.right += fMin;
        } else {
            crop.left -= fMin;
        }
        if (z3) {
            crop.bottom = crop.top + (defaultWallpaperSize.y / width);
        } else {
            float fMin2 = Math.min(Math.min(fArr[1] - crop.bottom, crop.top), ((defaultWallpaperSize.y / width) - crop.height()) / 2.0f);
            crop.top -= fMin2;
            crop.bottom += fMin2;
        }
        int iRound = Math.round(crop.width() * width);
        int iRound2 = Math.round(crop.height() * width);
        CropAndSetWallpaperTask cropAndSetWallpaperTask = new CropAndSetWallpaperTask(InputStreamProvider.fromUri(this, uri), this, crop, imageRotation, iRound, iRound2, new CropAndFinishHandler(new Point(iRound, iRound2), z)) {
            @Override
            protected void onPreExecute() {
                WallpaperCropActivity.this.mProgressView.setVisibility(0);
            }
        };
        if (onBitmapCroppedHandler != null) {
            cropAndSetWallpaperTask.setOnBitmapCropped(onBitmapCroppedHandler);
        }
        DialogUtils.executeCropTaskAfterPrompt(this, cropAndSetWallpaperTask, getOnDialogCancelListener());
    }

    public void setBoundsAndFinish(Point point, boolean z) {
        WallpaperUtils.saveWallpaperDimensions(point.x, point.y, this);
        setResult(-1);
        finish();
        if (z) {
            overridePendingTransition(0, R.anim.fade_out);
        }
    }

    public class CropAndFinishHandler implements CropAndSetWallpaperTask.OnEndCropHandler {
        private final Point mBounds;
        private boolean mShouldFadeOutOnFinish;

        public CropAndFinishHandler(Point point, boolean z) {
            this.mBounds = point;
            this.mShouldFadeOutOnFinish = z;
        }

        @Override
        public void run(boolean z) {
            WallpaperCropActivity.this.setBoundsAndFinish(this.mBounds, z && this.mShouldFadeOutOnFinish);
        }
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
}
