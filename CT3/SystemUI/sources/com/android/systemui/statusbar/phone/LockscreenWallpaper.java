package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.IWallpaperManager;
import android.app.IWallpaperManagerCallback;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
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
import libcore.io.IoUtils;

public class LockscreenWallpaper extends IWallpaperManagerCallback.Stub implements Runnable {
    private final PhoneStatusBar mBar;
    private Bitmap mCache;
    private boolean mCached;
    private int mCurrentUserId = ActivityManager.getCurrentUser();
    private final Handler mH;
    private AsyncTask<Void, Void, LoaderResult> mLoader;
    private UserHandle mSelectedUser;
    private final WallpaperManager mWallpaperManager;

    public LockscreenWallpaper(Context ctx, PhoneStatusBar bar, Handler h) {
        this.mBar = bar;
        this.mH = h;
        this.mWallpaperManager = (WallpaperManager) ctx.getSystemService("wallpaper");
        IWallpaperManager service = IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper"));
        try {
            service.setLockWallpaperCallback(this);
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
        LoaderResult result = loadBitmap(this.mCurrentUserId, this.mSelectedUser);
        if (result.success) {
            this.mCached = true;
            this.mCache = result.bitmap;
        }
        return this.mCache;
    }

    public LoaderResult loadBitmap(int currentUserId, UserHandle selectedUser) {
        int lockWallpaperUserId = selectedUser != null ? selectedUser.getIdentifier() : currentUserId;
        ParcelFileDescriptor fd = this.mWallpaperManager.getWallpaperFile(2, lockWallpaperUserId);
        if (fd != null) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                return LoaderResult.success(BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options));
            } catch (OutOfMemoryError e) {
                Log.w("LockscreenWallpaper", "Can't decode file", e);
                return LoaderResult.fail();
            } finally {
                IoUtils.closeQuietly(fd);
            }
        }
        if (selectedUser != null && selectedUser.getIdentifier() != currentUserId) {
            return LoaderResult.success(this.mWallpaperManager.getBitmapAsUser(selectedUser.getIdentifier()));
        }
        return LoaderResult.success(null);
    }

    public void setCurrentUser(int user) {
        if (user == this.mCurrentUserId) {
            return;
        }
        this.mCached = false;
        this.mCurrentUserId = user;
    }

    public void onWallpaperChanged() {
        postUpdateWallpaper();
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
        final int currentUser = this.mCurrentUserId;
        final UserHandle selectedUser = this.mSelectedUser;
        this.mLoader = new AsyncTask<Void, Void, LoaderResult>() {
            @Override
            public LoaderResult doInBackground(Void... params) {
                return LockscreenWallpaper.this.loadBitmap(currentUser, selectedUser);
            }

            @Override
            public void onPostExecute(LoaderResult result) {
                super.onPostExecute(result);
                if (isCancelled()) {
                    return;
                }
                if (result.success) {
                    LockscreenWallpaper.this.mCached = true;
                    LockscreenWallpaper.this.mCache = result.bitmap;
                    LockscreenWallpaper.this.mBar.updateMediaMetaData(true, true);
                }
                LockscreenWallpaper.this.mLoader = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private static class LoaderResult {
        public final Bitmap bitmap;
        public final boolean success;

        LoaderResult(boolean success, Bitmap bitmap) {
            this.success = success;
            this.bitmap = bitmap;
        }

        static LoaderResult success(Bitmap b) {
            return new LoaderResult(true, b);
        }

        static LoaderResult fail() {
            return new LoaderResult(false, null);
        }
    }

    public static class WallpaperDrawable extends DrawableWrapper {
        private final ConstantState mState;
        private final Rect mTmpRect;

        WallpaperDrawable(Resources r, ConstantState state, WallpaperDrawable wallpaperDrawable) {
            this(r, state);
        }

        public WallpaperDrawable(Resources r, Bitmap b) {
            this(r, new ConstantState(b));
        }

        private WallpaperDrawable(Resources r, ConstantState state) {
            super(new BitmapDrawable(r, state.mBackground));
            this.mTmpRect = new Rect();
            this.mState = state;
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
        protected void onBoundsChange(Rect bounds) {
            float scale;
            int vwidth = getBounds().width();
            int vheight = getBounds().height();
            int dwidth = this.mState.mBackground.getWidth();
            int dheight = this.mState.mBackground.getHeight();
            if (dwidth * vheight > vwidth * dheight) {
                scale = vheight / dheight;
            } else {
                scale = vwidth / dwidth;
            }
            if (scale <= 1.0f) {
                scale = 1.0f;
            }
            float dy = (vheight - (dheight * scale)) * 0.5f;
            this.mTmpRect.set(bounds.left, bounds.top + Math.round(dy), bounds.left + Math.round(dwidth * scale), bounds.top + Math.round((dheight * scale) + dy));
            super.onBoundsChange(this.mTmpRect);
        }

        @Override
        public ConstantState getConstantState() {
            return this.mState;
        }

        static class ConstantState extends Drawable.ConstantState {
            private final Bitmap mBackground;

            ConstantState(Bitmap background) {
                this.mBackground = background;
            }

            @Override
            public Drawable newDrawable() {
                return newDrawable(null);
            }

            @Override
            public Drawable newDrawable(Resources res) {
                return new WallpaperDrawable(res, this, null);
            }

            @Override
            public int getChangingConfigurations() {
                return 0;
            }
        }
    }
}
