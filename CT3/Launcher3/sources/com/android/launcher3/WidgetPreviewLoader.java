package com.android.launcher3;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.util.LongSparseArray;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.SQLiteCacheHelper;
import com.android.launcher3.widget.WidgetCell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class WidgetPreviewLoader {
    private final Context mContext;
    private final CacheDb mDb;
    private final IconCache mIconCache;
    private final int mProfileBadgeMargin;
    private final UserManagerCompat mUserManager;
    private final AppWidgetManagerCompat mWidgetManager;
    private final HashMap<String, long[]> mPackageVersions = new HashMap<>();
    final Set<Bitmap> mUnusedBitmaps = Collections.newSetFromMap(new WeakHashMap());
    private final MainThreadExecutor mMainThreadExecutor = new MainThreadExecutor();
    final Handler mWorkerHandler = new Handler(LauncherModel.getWorkerLooper());

    public WidgetPreviewLoader(Context context, IconCache iconCache) {
        this.mContext = context;
        this.mIconCache = iconCache;
        this.mWidgetManager = AppWidgetManagerCompat.getInstance(context);
        this.mUserManager = UserManagerCompat.getInstance(context);
        this.mDb = new CacheDb(context);
        this.mProfileBadgeMargin = context.getResources().getDimensionPixelSize(R.dimen.profile_badge_margin);
    }

    public PreviewLoadRequest getPreview(Object o, int previewWidth, int previewHeight, WidgetCell caller) {
        String size = previewWidth + "x" + previewHeight;
        WidgetCacheKey key = getObjectKey(o, size);
        PreviewLoadTask task = new PreviewLoadTask(key, o, previewWidth, previewHeight, caller);
        task.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
        return new PreviewLoadRequest(task);
    }

    private static class CacheDb extends SQLiteCacheHelper {
        public CacheDb(Context context) {
            super(context, "widgetpreviews.db", 4, "shortcut_and_widget_previews");
        }

        @Override
        public void onCreateTable(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS shortcut_and_widget_previews (componentName TEXT NOT NULL, profileId INTEGER NOT NULL, size TEXT NOT NULL, packageName TEXT NOT NULL, lastUpdated INTEGER NOT NULL DEFAULT 0, version INTEGER NOT NULL DEFAULT 0, preview_bitmap BLOB, PRIMARY KEY (componentName, profileId, size) );");
        }
    }

    private WidgetCacheKey getObjectKey(Object o, String size) {
        if (o instanceof LauncherAppWidgetProviderInfo) {
            LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) o;
            return new WidgetCacheKey(info.provider, this.mWidgetManager.getUser(info), size);
        }
        ResolveInfo info2 = (ResolveInfo) o;
        return new WidgetCacheKey(new ComponentName(info2.activityInfo.packageName, info2.activityInfo.name), UserHandleCompat.myUserHandle(), size);
    }

    void writeToDb(WidgetCacheKey key, long[] versions, Bitmap preview) {
        ContentValues values = new ContentValues();
        values.put("componentName", key.componentName.flattenToShortString());
        values.put("profileId", Long.valueOf(this.mUserManager.getSerialNumberForUser(key.user)));
        values.put("size", key.size);
        values.put("packageName", key.componentName.getPackageName());
        values.put("version", Long.valueOf(versions[0]));
        values.put("lastUpdated", Long.valueOf(versions[1]));
        values.put("preview_bitmap", Utilities.flattenBitmap(preview));
        this.mDb.insertOrReplace(values);
    }

    public void removePackage(String packageName, UserHandleCompat user) {
        removePackage(packageName, user, this.mUserManager.getSerialNumberForUser(user));
    }

    private void removePackage(String packageName, UserHandleCompat user, long userSerial) {
        synchronized (this.mPackageVersions) {
            this.mPackageVersions.remove(packageName);
        }
        this.mDb.delete("packageName = ? AND profileId = ?", new String[]{packageName, Long.toString(userSerial)});
    }

    public void removeObsoletePreviews(ArrayList<Object> list) {
        UserHandleCompat user;
        String pkg;
        Utilities.assertWorkerThread();
        LongSparseArray<HashSet<String>> validPackages = new LongSparseArray<>();
        for (Object obj : list) {
            if (obj instanceof ResolveInfo) {
                user = UserHandleCompat.myUserHandle();
                pkg = ((ResolveInfo) obj).activityInfo.packageName;
            } else {
                LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) obj;
                user = this.mWidgetManager.getUser(info);
                pkg = info.provider.getPackageName();
            }
            long userId = this.mUserManager.getSerialNumberForUser(user);
            HashSet<String> packages = validPackages.get(userId);
            if (packages == null) {
                packages = new HashSet<>();
                validPackages.put(userId, packages);
            }
            packages.add(pkg);
        }
        LongSparseArray<HashSet<String>> packagesToDelete = new LongSparseArray<>();
        Cursor c = null;
        try {
            try {
                c = this.mDb.query(new String[]{"profileId", "packageName", "lastUpdated", "version"}, null, null);
                while (c.moveToNext()) {
                    long userId2 = c.getLong(0);
                    String pkg2 = c.getString(1);
                    long lastUpdated = c.getLong(2);
                    long version = c.getLong(3);
                    HashSet<String> packages2 = validPackages.get(userId2);
                    if (packages2 != null && packages2.contains(pkg2)) {
                        long[] versions = getPackageVersion(pkg2);
                        if (versions[0] != version || versions[1] != lastUpdated) {
                        }
                    }
                    HashSet<String> packages3 = packagesToDelete.get(userId2);
                    if (packages3 == null) {
                        packages3 = new HashSet<>();
                        packagesToDelete.put(userId2, packages3);
                    }
                    packages3.add(pkg2);
                }
                for (int i = 0; i < packagesToDelete.size(); i++) {
                    long userId3 = packagesToDelete.keyAt(i);
                    UserHandleCompat user2 = this.mUserManager.getUserForSerialNumber(userId3);
                    Iterator pkg$iterator = packagesToDelete.valueAt(i).iterator();
                    while (pkg$iterator.hasNext()) {
                        removePackage((String) pkg$iterator.next(), user2, userId3);
                    }
                }
                if (c == null) {
                    return;
                }
                c.close();
            } catch (SQLException e) {
                Log.e("WidgetPreviewLoader", "Error updating widget previews", e);
                if (c == null) {
                    return;
                }
                c.close();
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    Bitmap readFromDb(WidgetCacheKey key, Bitmap recycle, PreviewLoadTask loadTask) {
        Cursor cursor = null;
        try {
            try {
                cursor = this.mDb.query(new String[]{"preview_bitmap"}, "componentName = ? AND profileId = ? AND size = ?", new String[]{key.componentName.flattenToString(), Long.toString(this.mUserManager.getSerialNumberForUser(key.user)), key.size});
                if (loadTask.isCancelled()) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    return null;
                }
                if (cursor.moveToNext()) {
                    byte[] blob = cursor.getBlob(0);
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inBitmap = recycle;
                    try {
                        if (!loadTask.isCancelled()) {
                            Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(blob, 0, blob.length, opts);
                            if (cursor != null) {
                                cursor.close();
                            }
                            return bitmapDecodeByteArray;
                        }
                    } catch (Exception e) {
                        if (cursor != null) {
                            cursor.close();
                        }
                        return null;
                    }
                }
                if (cursor == null) {
                    return null;
                }
                cursor.close();
                return null;
            } catch (SQLException e2) {
                Log.w("WidgetPreviewLoader", "Error loading preview from DB", e2);
                if (cursor == null) {
                    return null;
                }
                cursor.close();
                return null;
            }
        } catch (Throwable th) {
            if (cursor != null) {
            }
            throw th;
        }
        if (cursor != null) {
            cursor.close();
        }
        throw th;
    }

    Bitmap generatePreview(Launcher launcher, Object info, Bitmap recycle, int previewWidth, int previewHeight) {
        if (info instanceof LauncherAppWidgetProviderInfo) {
            return generateWidgetPreview(launcher, (LauncherAppWidgetProviderInfo) info, previewWidth, recycle, null);
        }
        return generateShortcutPreview(launcher, (ResolveInfo) info, previewWidth, previewHeight, recycle);
    }

    public Bitmap generateWidgetPreview(Launcher launcher, LauncherAppWidgetProviderInfo info, int maxPreviewWidth, Bitmap preview, int[] preScaledWidthOut) {
        int previewWidth;
        int previewHeight;
        if (maxPreviewWidth < 0) {
            maxPreviewWidth = Integer.MAX_VALUE;
        }
        Drawable drawable = null;
        if (info.previewImage != 0) {
            drawable = this.mWidgetManager.loadPreview(info);
            if (drawable != null) {
                drawable = mutateOnMainThread(drawable);
            } else {
                Log.w("WidgetPreviewLoader", "Can't load widget preview drawable 0x" + Integer.toHexString(info.previewImage) + " for provider: " + info.provider);
            }
        }
        boolean widgetPreviewExists = drawable != null;
        int spanX = info.spanX;
        int spanY = info.spanY;
        Bitmap tileBitmap = null;
        if (widgetPreviewExists) {
            previewWidth = drawable.getIntrinsicWidth();
            previewHeight = drawable.getIntrinsicHeight();
        } else {
            tileBitmap = ((BitmapDrawable) this.mContext.getResources().getDrawable(R.drawable.widget_tile)).getBitmap();
            previewWidth = tileBitmap.getWidth() * spanX;
            previewHeight = tileBitmap.getHeight() * spanY;
        }
        if (preScaledWidthOut != null) {
            preScaledWidthOut[0] = previewWidth;
        }
        float scale = previewWidth > maxPreviewWidth ? (maxPreviewWidth - (this.mProfileBadgeMargin * 2)) / previewWidth : 1.0f;
        if (scale != 1.0f) {
            previewWidth = (int) (previewWidth * scale);
            previewHeight = (int) (previewHeight * scale);
        }
        Canvas c = new Canvas();
        if (preview == null) {
            preview = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
            c.setBitmap(preview);
        } else {
            c.setBitmap(preview);
            c.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        int x = (preview.getWidth() - previewWidth) / 2;
        if (widgetPreviewExists) {
            drawable.setBounds(0, 0, previewWidth, previewHeight);
            drawable.draw(c);
        } else {
            Paint p = new Paint();
            p.setFilterBitmap(true);
            int appIconSize = launcher.getDeviceProfile().iconSizePx;
            Rect src = new Rect(0, 0, tileBitmap.getWidth(), tileBitmap.getHeight());
            float tileW = scale * tileBitmap.getWidth();
            float tileH = scale * tileBitmap.getHeight();
            RectF dst = new RectF(0.0f, 0.0f, tileW, tileH);
            float tx = x;
            int i = 0;
            while (i < spanX) {
                float ty = 0.0f;
                int j = 0;
                while (j < spanY) {
                    dst.offsetTo(tx, ty);
                    c.drawBitmap(tileBitmap, src, dst, p);
                    j++;
                    ty += tileH;
                }
                i++;
                tx += tileW;
            }
            int minOffset = (int) (appIconSize * 0.25f);
            int smallestSide = Math.min(previewWidth, previewHeight);
            float iconScale = Math.min(smallestSide / ((minOffset * 2) + appIconSize), scale);
            try {
                Drawable icon = this.mWidgetManager.loadIcon(info, this.mIconCache);
                if (icon != null) {
                    Drawable icon2 = mutateOnMainThread(icon);
                    int hoffset = ((int) ((tileW - (appIconSize * iconScale)) / 2.0f)) + x;
                    int yoffset = (int) ((tileH - (appIconSize * iconScale)) / 2.0f);
                    icon2.setBounds(hoffset, yoffset, ((int) (appIconSize * iconScale)) + hoffset, ((int) (appIconSize * iconScale)) + yoffset);
                    icon2.draw(c);
                }
            } catch (Resources.NotFoundException e) {
            }
            c.setBitmap(null);
        }
        int imageWidth = Math.min(preview.getWidth(), this.mProfileBadgeMargin + previewWidth);
        int imageHeight = Math.min(preview.getHeight(), this.mProfileBadgeMargin + previewHeight);
        return this.mWidgetManager.getBadgeBitmap(info, preview, imageWidth, imageHeight);
    }

    private Bitmap generateShortcutPreview(Launcher launcher, ResolveInfo info, int maxWidth, int maxHeight, Bitmap preview) {
        Canvas c = new Canvas();
        if (preview == null) {
            preview = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
            c.setBitmap(preview);
        } else {
            if (preview.getWidth() != maxWidth || preview.getHeight() != maxHeight) {
                throw new RuntimeException("Improperly sized bitmap passed as argument");
            }
            c.setBitmap(preview);
            c.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        Drawable icon = mutateOnMainThread(this.mIconCache.getFullResIcon(info.activityInfo));
        icon.setFilterBitmap(true);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0.0f);
        icon.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        icon.setAlpha(15);
        Resources res = this.mContext.getResources();
        int paddingTop = res.getDimensionPixelOffset(R.dimen.shortcut_preview_padding_top);
        int paddingLeft = res.getDimensionPixelOffset(R.dimen.shortcut_preview_padding_left);
        int paddingRight = res.getDimensionPixelOffset(R.dimen.shortcut_preview_padding_right);
        int scaledIconWidth = (maxWidth - paddingLeft) - paddingRight;
        icon.setBounds(paddingLeft, paddingTop, paddingLeft + scaledIconWidth, paddingTop + scaledIconWidth);
        icon.draw(c);
        int appIconSize = launcher.getDeviceProfile().iconSizePx;
        icon.setAlpha(255);
        icon.setColorFilter(null);
        icon.setBounds(0, 0, appIconSize, appIconSize);
        icon.draw(c);
        c.setBitmap(null);
        return preview;
    }

    private Drawable mutateOnMainThread(final Drawable drawable) {
        try {
            return (Drawable) this.mMainThreadExecutor.submit(new Callable<Drawable>() {
                @Override
                public Drawable call() throws Exception {
                    return drawable.mutate();
                }
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e2) {
            throw new RuntimeException(e2);
        }
    }

    long[] getPackageVersion(String packageName) {
        long[] versions;
        synchronized (this.mPackageVersions) {
            versions = this.mPackageVersions.get(packageName);
            if (versions == null) {
                versions = new long[2];
                try {
                    PackageInfo info = this.mContext.getPackageManager().getPackageInfo(packageName, 0);
                    versions[0] = info.versionCode;
                    versions[1] = info.lastUpdateTime;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("WidgetPreviewLoader", "PackageInfo not found", e);
                }
                this.mPackageVersions.put(packageName, versions);
            }
        }
        return versions;
    }

    public class PreviewLoadRequest {
        final PreviewLoadTask mTask;

        public PreviewLoadRequest(PreviewLoadTask task) {
            this.mTask = task;
        }

        public void cleanup() {
            if (this.mTask != null) {
                this.mTask.cancel(true);
            }
            if (this.mTask.mBitmapToRecycle == null) {
                return;
            }
            WidgetPreviewLoader.this.mWorkerHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                        WidgetPreviewLoader.this.mUnusedBitmaps.add(PreviewLoadRequest.this.mTask.mBitmapToRecycle);
                    }
                    PreviewLoadRequest.this.mTask.mBitmapToRecycle = null;
                }
            });
        }
    }

    public class PreviewLoadTask extends AsyncTask<Void, Void, Bitmap> {
        Bitmap mBitmapToRecycle;
        private final WidgetCell mCaller;
        private final Object mInfo;
        final WidgetCacheKey mKey;
        private final int mPreviewHeight;
        private final int mPreviewWidth;
        long[] mVersions;

        PreviewLoadTask(WidgetCacheKey key, Object info, int previewWidth, int previewHeight, WidgetCell caller) {
            this.mKey = key;
            this.mInfo = info;
            this.mPreviewHeight = previewHeight;
            this.mPreviewWidth = previewWidth;
            this.mCaller = caller;
        }

        @Override
        public Bitmap doInBackground(Void... params) {
            Bitmap unusedBitmap = null;
            if (isCancelled()) {
                return null;
            }
            synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                Iterator candidate$iterator = WidgetPreviewLoader.this.mUnusedBitmaps.iterator();
                while (true) {
                    if (!candidate$iterator.hasNext()) {
                        break;
                    }
                    Bitmap candidate = (Bitmap) candidate$iterator.next();
                    if (candidate != null && candidate.isMutable() && candidate.getWidth() == this.mPreviewWidth && candidate.getHeight() == this.mPreviewHeight) {
                        break;
                    }
                }
            }
            if (unusedBitmap == null) {
                unusedBitmap = Bitmap.createBitmap(this.mPreviewWidth, this.mPreviewHeight, Bitmap.Config.ARGB_8888);
            }
            if (isCancelled()) {
                return unusedBitmap;
            }
            Bitmap preview = WidgetPreviewLoader.this.readFromDb(this.mKey, unusedBitmap, this);
            if (!isCancelled() && preview == null) {
                this.mVersions = WidgetPreviewLoader.this.getPackageVersion(this.mKey.componentName.getPackageName());
                Launcher launcher = (Launcher) this.mCaller.getContext();
                return WidgetPreviewLoader.this.generatePreview(launcher, this.mInfo, unusedBitmap, this.mPreviewWidth, this.mPreviewHeight);
            }
            return preview;
        }

        @Override
        public void onPostExecute(final Bitmap preview) {
            this.mCaller.applyPreview(preview);
            if (this.mVersions != null) {
                WidgetPreviewLoader.this.mWorkerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!PreviewLoadTask.this.isCancelled()) {
                            WidgetPreviewLoader.this.writeToDb(PreviewLoadTask.this.mKey, PreviewLoadTask.this.mVersions, preview);
                            PreviewLoadTask.this.mBitmapToRecycle = preview;
                        } else {
                            synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                                WidgetPreviewLoader.this.mUnusedBitmaps.add(preview);
                            }
                        }
                    }
                });
            } else {
                this.mBitmapToRecycle = preview;
            }
        }

        @Override
        public void onCancelled(final Bitmap preview) {
            if (preview == null) {
                return;
            }
            WidgetPreviewLoader.this.mWorkerHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                        WidgetPreviewLoader.this.mUnusedBitmaps.add(preview);
                    }
                }
            });
        }
    }

    private static final class WidgetCacheKey extends ComponentKey {
        final String size;

        public WidgetCacheKey(ComponentName componentName, UserHandleCompat user, String size) {
            super(componentName, user);
            this.size = size;
        }

        @Override
        public int hashCode() {
            return super.hashCode() ^ this.size.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (super.equals(o)) {
                return ((WidgetCacheKey) o).size.equals(this.size);
            }
            return false;
        }
    }
}
