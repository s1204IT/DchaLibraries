package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.IWallpaperManager;
import android.app.IWallpaperManagerCallback;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import libcore.io.IoUtils;

public class LockscreenWallpaper extends IWallpaperManagerCallback.Stub implements Runnable {
    private final StatusBar mBar;
    private Bitmap mCache;
    private boolean mCached;
    private int mCurrentUserId = ActivityManager.getCurrentUser();
    private final Handler mH;
    private AsyncTask<Void, Void, LoaderResult> mLoader;
    private UserHandle mSelectedUser;
    private final KeyguardUpdateMonitor mUpdateMonitor;
    private final WallpaperManager mWallpaperManager;

    public LockscreenWallpaper(Context context, StatusBar statusBar, Handler handler) {
        this.mBar = statusBar;
        this.mH = handler;
        this.mWallpaperManager = (WallpaperManager) context.getSystemService("wallpaper");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        try {
            IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper")).setLockWallpaperCallback(this);
        } catch (RemoteException e) {
            Log.e("LockscreenWallpaper", "System dead?" + e);
        }
    }

    public Bitmap getBitmap() {
        if (this.mCached) {
            return this.mCache;
        }
        if (!this.mWallpaperManager.isWallpaperSupported()) {
            this.mCached = true;
            this.mCache = null;
            return null;
        }
        LoaderResult loaderResultLoadBitmap = loadBitmap(this.mCurrentUserId, this.mSelectedUser);
        if (loaderResultLoadBitmap.success) {
            this.mCached = true;
            this.mUpdateMonitor.setHasLockscreenWallpaper(loaderResultLoadBitmap.bitmap != null);
            this.mCache = loaderResultLoadBitmap.bitmap;
        }
        return this.mCache;
    }

    public LoaderResult loadBitmap(int i, UserHandle userHandle) {
        if (userHandle != null) {
            i = userHandle.getIdentifier();
        }
        ParcelFileDescriptor wallpaperFile = this.mWallpaperManager.getWallpaperFile(2, i);
        if (wallpaperFile == null) {
            return userHandle != null ? LoaderResult.success(this.mWallpaperManager.getBitmapAsUser(userHandle.getIdentifier(), true)) : LoaderResult.success(null);
        }
        try {
            return LoaderResult.success(BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor(), null, new BitmapFactory.Options()));
        } catch (OutOfMemoryError e) {
            Log.w("LockscreenWallpaper", "Can't decode file", e);
            return LoaderResult.fail();
        } finally {
            IoUtils.closeQuietly(wallpaperFile);
        }
    }

    public void setCurrentUser(int i) {
        if (i != this.mCurrentUserId) {
            if (this.mSelectedUser == null || i != this.mSelectedUser.getIdentifier()) {
                this.mCached = false;
            }
            this.mCurrentUserId = i;
        }
    }

    public void onWallpaperChanged() {
        postUpdateWallpaper();
    }

    public void onWallpaperColorsChanged(WallpaperColors wallpaperColors, int i, int i2) {
    }

    private void postUpdateWallpaper() {
        this.mH.removeCallbacks(this);
        this.mH.post(this);
    }

    @Override
    public void run() {
        if (this.mLoader != null) {
            this.mLoader.cancel(false);
        }
        final int i = this.mCurrentUserId;
        final UserHandle userHandle = this.mSelectedUser;
        this.mLoader = new AsyncTask<Void, Void, LoaderResult>() {
            @Override
            public LoaderResult doInBackground(Void... voidArr) {
                return LockscreenWallpaper.this.loadBitmap(i, userHandle);
            }

            @Override
            public void onPostExecute(LoaderResult loaderResult) {
                super.onPostExecute(loaderResult);
                if (isCancelled()) {
                    return;
                }
                if (loaderResult.success) {
                    LockscreenWallpaper.this.mCached = true;
                    LockscreenWallpaper.this.mCache = loaderResult.bitmap;
                    LockscreenWallpaper.this.mUpdateMonitor.setHasLockscreenWallpaper(loaderResult.bitmap != null);
                    LockscreenWallpaper.this.mBar.updateMediaMetaData(true, true);
                }
                LockscreenWallpaper.this.mLoader = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private static class LoaderResult {
        public final Bitmap bitmap;
        public final boolean success;

        LoaderResult(boolean z, Bitmap bitmap) {
            this.success = z;
            this.bitmap = bitmap;
        }

        static LoaderResult success(Bitmap bitmap) {
            return new LoaderResult(true, bitmap);
        }

        static LoaderResult fail() {
            return new LoaderResult(false, null);
        }
    }

    public static class WallpaperDrawable extends DrawableWrapper {
        private final ConstantState mState;
        private final Rect mTmpRect;

        public WallpaperDrawable(Resources resources, Bitmap bitmap) {
            this(resources, new ConstantState(bitmap));
        }

        private WallpaperDrawable(Resources resources, ConstantState constantState) {
            super(new BitmapDrawable(resources, constantState.mBackground));
            this.mTmpRect = new Rect();
            this.mState = constantState;
        }

        public void setXfermode(Xfermode xfermode) {
            getDrawable().setXfermode(xfermode);
        }

        @Override
        public int getIntrinsicWidth() {
            return -1;
        }

        @Override
        public int getIntrinsicHeight() {
            return -1;
        }

        @Override
        protected void onBoundsChange(Rect rect) {
            float f;
            int iWidth = getBounds().width();
            int iHeight = getBounds().height();
            int width = this.mState.mBackground.getWidth();
            int height = this.mState.mBackground.getHeight();
            if (width * iHeight > iWidth * height) {
                f = iHeight / height;
            } else {
                f = iWidth / width;
            }
            if (f <= 1.0f) {
                f = 1.0f;
            }
            float f2 = height * f;
            float f3 = (iHeight - f2) * 0.5f;
            this.mTmpRect.set(rect.left, rect.top + Math.round(f3), rect.left + Math.round(width * f), rect.top + Math.round(f2 + f3));
            super.onBoundsChange(this.mTmpRect);
        }

        @Override
        public ConstantState getConstantState() {
            return this.mState;
        }

        static class ConstantState extends Drawable.ConstantState {
            private final Bitmap mBackground;

            ConstantState(Bitmap bitmap) {
                this.mBackground = bitmap;
            }

            @Override
            public Drawable newDrawable() {
                return newDrawable(null);
            }

            @Override
            public Drawable newDrawable(Resources resources) {
                return new WallpaperDrawable(resources, this);
            }

            @Override
            public int getChangingConfigurations() {
                return 0;
            }
        }
    }
}
