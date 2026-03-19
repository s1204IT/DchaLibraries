package com.android.launcher3;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LongSparseArray;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.ShortcutConfigActivityInfo;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.graphics.ShadowGenerator;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.Preconditions;
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
    private static final boolean DEBUG = false;
    private static final String TAG = "WidgetPreviewLoader";
    private final Context mContext;
    private final CacheDb mDb;
    private final IconCache mIconCache;
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
    }

    public CancellationSignal getPreview(WidgetItem widgetItem, int i, int i2, WidgetCell widgetCell) {
        PreviewLoadTask previewLoadTask = new PreviewLoadTask(new WidgetCacheKey(widgetItem.componentName, widgetItem.user, i + "x" + i2), widgetItem, i, i2, widgetCell);
        previewLoadTask.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(previewLoadTask);
        return cancellationSignal;
    }

    private static class CacheDb extends SQLiteCacheHelper {
        private static final String COLUMN_COMPONENT = "componentName";
        private static final String COLUMN_LAST_UPDATED = "lastUpdated";
        private static final String COLUMN_PACKAGE = "packageName";
        private static final String COLUMN_PREVIEW_BITMAP = "preview_bitmap";
        private static final String COLUMN_SIZE = "size";
        private static final String COLUMN_USER = "profileId";
        private static final String COLUMN_VERSION = "version";
        private static final int DB_VERSION = 9;
        private static final String TABLE_NAME = "shortcut_and_widget_previews";

        public CacheDb(Context context) {
            super(context, LauncherFiles.WIDGET_PREVIEWS_DB, 9, TABLE_NAME);
        }

        @Override
        public void onCreateTable(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS shortcut_and_widget_previews (componentName TEXT NOT NULL, profileId INTEGER NOT NULL, size TEXT NOT NULL, packageName TEXT NOT NULL, lastUpdated INTEGER NOT NULL DEFAULT 0, version INTEGER NOT NULL DEFAULT 0, preview_bitmap BLOB, PRIMARY KEY (componentName, profileId, size) );");
        }
    }

    void writeToDb(WidgetCacheKey widgetCacheKey, long[] jArr, Bitmap bitmap) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("componentName", widgetCacheKey.componentName.flattenToShortString());
        contentValues.put(LauncherSettings.Favorites.PROFILE_ID, Long.valueOf(this.mUserManager.getSerialNumberForUser(widgetCacheKey.user)));
        contentValues.put("size", widgetCacheKey.size);
        contentValues.put("packageName", widgetCacheKey.componentName.getPackageName());
        contentValues.put("version", Long.valueOf(jArr[0]));
        contentValues.put("lastUpdated", Long.valueOf(jArr[1]));
        contentValues.put("preview_bitmap", Utilities.flattenBitmap(bitmap));
        this.mDb.insertOrReplace(contentValues);
    }

    public void removePackage(String str, UserHandle userHandle) {
        removePackage(str, userHandle, this.mUserManager.getSerialNumberForUser(userHandle));
    }

    private void removePackage(String str, UserHandle userHandle, long j) {
        synchronized (this.mPackageVersions) {
            this.mPackageVersions.remove(str);
        }
        this.mDb.delete("packageName = ? AND profileId = ?", new String[]{str, Long.toString(j)});
    }

    public void removeObsoletePreviews(ArrayList<? extends ComponentKey> arrayList, @Nullable PackageUserKey packageUserKey) throws Throwable {
        Cursor cursorQuery;
        int i;
        Preconditions.assertWorkerThread();
        LongSparseArray longSparseArray = new LongSparseArray();
        for (ComponentKey componentKey : arrayList) {
            long serialNumberForUser = this.mUserManager.getSerialNumberForUser(componentKey.user);
            HashSet hashSet = (HashSet) longSparseArray.get(serialNumberForUser);
            if (hashSet == null) {
                hashSet = new HashSet();
                longSparseArray.put(serialNumberForUser, hashSet);
            }
            hashSet.add(componentKey.componentName.getPackageName());
        }
        LongSparseArray longSparseArray2 = new LongSparseArray();
        long serialNumberForUser2 = packageUserKey == null ? 0L : this.mUserManager.getSerialNumberForUser(packageUserKey.mUser);
        Cursor cursor = null;
        try {
            try {
                cursorQuery = this.mDb.query(new String[]{LauncherSettings.Favorites.PROFILE_ID, "packageName", "lastUpdated", "version"}, null, null);
                while (true) {
                    try {
                        if (!cursorQuery.moveToNext()) {
                            break;
                        }
                        long j = cursorQuery.getLong(0);
                        String string = cursorQuery.getString(1);
                        long j2 = cursorQuery.getLong(2);
                        long j3 = cursorQuery.getLong(3);
                        if (packageUserKey == null || (string.equals(packageUserKey.mPackageName) && j == serialNumberForUser2)) {
                            HashSet hashSet2 = (HashSet) longSparseArray.get(j);
                            if (hashSet2 != null && hashSet2.contains(string)) {
                                long[] packageVersion = getPackageVersion(string);
                                if (packageVersion[0] != j3 || packageVersion[1] != j2) {
                                }
                            }
                            HashSet hashSet3 = (HashSet) longSparseArray2.get(j);
                            if (hashSet3 == null) {
                                hashSet3 = new HashSet();
                                longSparseArray2.put(j, hashSet3);
                            }
                            hashSet3.add(string);
                        }
                    } catch (SQLException e) {
                        e = e;
                        cursor = cursorQuery;
                        Log.e(TAG, "Error updating widget previews", e);
                        if (cursor != null) {
                            cursor.close();
                            return;
                        }
                        return;
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                for (i = 0; i < longSparseArray2.size(); i++) {
                    long jKeyAt = longSparseArray2.keyAt(i);
                    UserHandle userForSerialNumber = this.mUserManager.getUserForSerialNumber(jKeyAt);
                    Iterator it = ((HashSet) longSparseArray2.valueAt(i)).iterator();
                    while (it.hasNext()) {
                        removePackage((String) it.next(), userForSerialNumber, jKeyAt);
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (SQLException e2) {
                e = e2;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = cursor;
        }
    }

    Bitmap readFromDb(WidgetCacheKey widgetCacheKey, Bitmap bitmap, PreviewLoadTask previewLoadTask) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            cursorQuery = this.mDb.query(new String[]{"preview_bitmap"}, "componentName = ? AND profileId = ? AND size = ?", new String[]{widgetCacheKey.componentName.flattenToShortString(), Long.toString(this.mUserManager.getSerialNumberForUser(widgetCacheKey.user)), widgetCacheKey.size});
            try {
                try {
                } catch (SQLException e) {
                    e = e;
                    Log.w(TAG, "Error loading preview from DB", e);
                    if (cursorQuery != null) {
                    }
                }
            } catch (Throwable th) {
                th = th;
                cursor = cursorQuery;
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        } catch (SQLException e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            if (cursor != null) {
            }
            throw th;
        }
        if (previewLoadTask.isCancelled()) {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return null;
        }
        if (cursorQuery.moveToNext()) {
            byte[] blob = cursorQuery.getBlob(0);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inBitmap = bitmap;
            try {
                if (!previewLoadTask.isCancelled()) {
                    Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(blob, 0, blob.length, options);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return bitmapDecodeByteArray;
                }
            } catch (Exception e3) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return null;
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return null;
    }

    private Bitmap generatePreview(BaseActivity baseActivity, WidgetItem widgetItem, Bitmap bitmap, int i, int i2) {
        if (widgetItem.widgetInfo != null) {
            return generateWidgetPreview(baseActivity, widgetItem.widgetInfo, i, bitmap, null);
        }
        return generateShortcutPreview(baseActivity, widgetItem.activityInfo, i, i2, bitmap);
    }

    public Bitmap generateWidgetPreview(BaseActivity baseActivity, LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo, int i, Bitmap bitmap, int[] iArr) {
        Drawable drawableMutateOnMainThread;
        int iMax;
        int iMax2;
        float f;
        Bitmap bitmapCreateBitmap = bitmap;
        int i2 = i < 0 ? Integer.MAX_VALUE : i;
        if (launcherAppWidgetProviderInfo.previewImage != 0) {
            try {
                drawableMutateOnMainThread = launcherAppWidgetProviderInfo.loadPreviewImage(this.mContext, 0);
            } catch (OutOfMemoryError e) {
                Log.w(TAG, "Error loading widget preview for: " + launcherAppWidgetProviderInfo.provider, e);
                drawableMutateOnMainThread = null;
            }
            if (drawableMutateOnMainThread != null) {
                drawableMutateOnMainThread = mutateOnMainThread(drawableMutateOnMainThread);
            } else {
                Log.w(TAG, "Can't load widget preview drawable 0x" + Integer.toHexString(launcherAppWidgetProviderInfo.previewImage) + " for provider: " + launcherAppWidgetProviderInfo.provider);
            }
        } else {
            drawableMutateOnMainThread = null;
        }
        boolean z = drawableMutateOnMainThread != null;
        int i3 = launcherAppWidgetProviderInfo.spanX;
        int i4 = launcherAppWidgetProviderInfo.spanY;
        if (z && drawableMutateOnMainThread.getIntrinsicWidth() > 0 && drawableMutateOnMainThread.getIntrinsicHeight() > 0) {
            iMax2 = drawableMutateOnMainThread.getIntrinsicWidth();
            iMax = drawableMutateOnMainThread.getIntrinsicHeight();
        } else {
            DeviceProfile deviceProfile = baseActivity.getDeviceProfile();
            int iMin = Math.min(deviceProfile.cellWidthPx, deviceProfile.cellHeightPx);
            iMax = iMin * i4;
            iMax2 = iMin * i3;
        }
        if (iArr != null) {
            iArr[0] = iMax2;
        }
        if (iMax2 > i2) {
            f = i2 / iMax2;
        } else {
            f = 1.0f;
        }
        if (f != 1.0f) {
            iMax2 = Math.max((int) (iMax2 * f), 1);
            iMax = Math.max((int) (iMax * f), 1);
        }
        Canvas canvas = new Canvas();
        if (bitmapCreateBitmap == null) {
            bitmapCreateBitmap = Bitmap.createBitmap(iMax2, iMax, Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmapCreateBitmap);
        } else {
            if (bitmap.getHeight() > iMax) {
                bitmapCreateBitmap.reconfigure(bitmap.getWidth(), iMax, bitmap.getConfig());
            }
            canvas.setBitmap(bitmapCreateBitmap);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        int width = (bitmapCreateBitmap.getWidth() - iMax2) / 2;
        if (z) {
            drawableMutateOnMainThread.setBounds(width, 0, iMax2 + width, iMax);
            drawableMutateOnMainThread.draw(canvas);
        } else {
            RectF rectFDrawBoxWithShadow = drawBoxWithShadow(canvas, iMax2, iMax);
            Paint paint = new Paint(1);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(this.mContext.getResources().getDimension(R.dimen.widget_preview_cell_divider_width));
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            float f2 = rectFDrawBoxWithShadow.left;
            float fWidth = rectFDrawBoxWithShadow.width() / i3;
            float f3 = f2;
            int i5 = 1;
            while (i5 < i3) {
                float f4 = f3 + fWidth;
                canvas.drawLine(f4, 0.0f, f4, iMax, paint);
                i5++;
                f3 = f4;
            }
            float f5 = rectFDrawBoxWithShadow.top;
            float fHeight = rectFDrawBoxWithShadow.height() / i4;
            for (int i6 = 1; i6 < i4; i6++) {
                f5 += fHeight;
                canvas.drawLine(0.0f, f5, iMax2, f5, paint);
            }
            try {
                Drawable fullResIcon = this.mIconCache.getFullResIcon(launcherAppWidgetProviderInfo.provider.getPackageName(), launcherAppWidgetProviderInfo.icon);
                if (fullResIcon != null) {
                    int iMin2 = (int) Math.min(baseActivity.getDeviceProfile().iconSizePx * f, Math.min(rectFDrawBoxWithShadow.width(), rectFDrawBoxWithShadow.height()));
                    Drawable drawableMutateOnMainThread2 = mutateOnMainThread(fullResIcon);
                    int i7 = (iMax2 - iMin2) / 2;
                    int i8 = (iMax - iMin2) / 2;
                    drawableMutateOnMainThread2.setBounds(i7, i8, i7 + iMin2, iMin2 + i8);
                    drawableMutateOnMainThread2.draw(canvas);
                }
            } catch (Resources.NotFoundException e2) {
            }
            canvas.setBitmap(null);
        }
        return bitmapCreateBitmap;
    }

    private RectF drawBoxWithShadow(Canvas canvas, int i, int i2) {
        Resources resources = this.mContext.getResources();
        ShadowGenerator.Builder builder = new ShadowGenerator.Builder(-1);
        builder.shadowBlur = resources.getDimension(R.dimen.widget_preview_shadow_blur);
        builder.radius = resources.getDimension(R.dimen.widget_preview_corner_radius);
        builder.keyShadowDistance = resources.getDimension(R.dimen.widget_preview_key_shadow_distance);
        builder.bounds.set(builder.shadowBlur, builder.shadowBlur, i - builder.shadowBlur, (i2 - builder.shadowBlur) - builder.keyShadowDistance);
        builder.drawShadow(canvas);
        return builder.bounds;
    }

    private Bitmap generateShortcutPreview(BaseActivity baseActivity, ShortcutConfigActivityInfo shortcutConfigActivityInfo, int i, int i2, Bitmap bitmap) {
        int i3 = baseActivity.getDeviceProfile().iconSizePx;
        int dimensionPixelSize = baseActivity.getResources().getDimensionPixelSize(R.dimen.widget_preview_shortcut_padding);
        int i4 = (2 * dimensionPixelSize) + i3;
        if (i2 < i4 || i < i4) {
            throw new RuntimeException("Max size is too small for preview");
        }
        Canvas canvas = new Canvas();
        if (bitmap == null || bitmap.getWidth() < i4 || bitmap.getHeight() < i4) {
            bitmap = Bitmap.createBitmap(i4, i4, Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmap);
        } else {
            if (bitmap.getWidth() > i4 || bitmap.getHeight() > i4) {
                bitmap.reconfigure(i4, i4, bitmap.getConfig());
            }
            canvas.setBitmap(bitmap);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        RectF rectFDrawBoxWithShadow = drawBoxWithShadow(canvas, i4, i4);
        LauncherIcons launcherIconsObtain = LauncherIcons.obtain(this.mContext);
        Bitmap bitmapCreateScaledBitmapWithoutShadow = launcherIconsObtain.createScaledBitmapWithoutShadow(mutateOnMainThread(shortcutConfigActivityInfo.getFullResIcon(this.mIconCache)), 0);
        launcherIconsObtain.recycle();
        Rect rect = new Rect(0, 0, bitmapCreateScaledBitmapWithoutShadow.getWidth(), bitmapCreateScaledBitmapWithoutShadow.getHeight());
        float f = i3;
        rectFDrawBoxWithShadow.set(0.0f, 0.0f, f, f);
        float f2 = dimensionPixelSize;
        rectFDrawBoxWithShadow.offset(f2, f2);
        canvas.drawBitmap(bitmapCreateScaledBitmapWithoutShadow, rect, rectFDrawBoxWithShadow, new Paint(3));
        canvas.setBitmap(null);
        return bitmap;
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

    long[] getPackageVersion(String str) {
        long[] jArr;
        synchronized (this.mPackageVersions) {
            jArr = this.mPackageVersions.get(str);
            if (jArr == null) {
                jArr = new long[2];
                try {
                    PackageInfo packageInfo = this.mContext.getPackageManager().getPackageInfo(str, 8192);
                    jArr[0] = packageInfo.versionCode;
                    jArr[1] = packageInfo.lastUpdateTime;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "PackageInfo not found", e);
                }
                this.mPackageVersions.put(str, jArr);
            }
        }
        return jArr;
    }

    public class PreviewLoadTask extends AsyncTask<Void, Void, Bitmap> implements CancellationSignal.OnCancelListener {
        private final BaseActivity mActivity;
        Bitmap mBitmapToRecycle;
        private final WidgetCell mCaller;
        private final WidgetItem mInfo;
        final WidgetCacheKey mKey;
        private final int mPreviewHeight;
        private final int mPreviewWidth;
        long[] mVersions;

        PreviewLoadTask(WidgetCacheKey widgetCacheKey, WidgetItem widgetItem, int i, int i2, WidgetCell widgetCell) {
            this.mKey = widgetCacheKey;
            this.mInfo = widgetItem;
            this.mPreviewHeight = i2;
            this.mPreviewWidth = i;
            this.mCaller = widgetCell;
            this.mActivity = BaseActivity.fromContext(this.mCaller.getContext());
        }

        @Override
        protected Bitmap doInBackground(Void... voidArr) throws Throwable {
            Bitmap bitmapCreateBitmap;
            if (isCancelled()) {
                return null;
            }
            synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                Iterator<Bitmap> it = WidgetPreviewLoader.this.mUnusedBitmaps.iterator();
                while (true) {
                    if (it.hasNext()) {
                        bitmapCreateBitmap = it.next();
                        if (bitmapCreateBitmap != null && bitmapCreateBitmap.isMutable() && bitmapCreateBitmap.getWidth() == this.mPreviewWidth && bitmapCreateBitmap.getHeight() == this.mPreviewHeight) {
                            WidgetPreviewLoader.this.mUnusedBitmaps.remove(bitmapCreateBitmap);
                            break;
                        }
                    } else {
                        bitmapCreateBitmap = null;
                        break;
                    }
                }
            }
            if (bitmapCreateBitmap == null) {
                bitmapCreateBitmap = Bitmap.createBitmap(this.mPreviewWidth, this.mPreviewHeight, Bitmap.Config.ARGB_8888);
            }
            Bitmap bitmap = bitmapCreateBitmap;
            if (isCancelled()) {
                return bitmap;
            }
            Bitmap fromDb = WidgetPreviewLoader.this.readFromDb(this.mKey, bitmap, this);
            if (!isCancelled() && fromDb == null) {
                this.mVersions = this.mInfo.activityInfo == null || this.mInfo.activityInfo.isPersistable() ? WidgetPreviewLoader.this.getPackageVersion(this.mKey.componentName.getPackageName()) : null;
                return WidgetPreviewLoader.this.generatePreview(this.mActivity, this.mInfo, bitmap, this.mPreviewWidth, this.mPreviewHeight);
            }
            return fromDb;
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            this.mCaller.applyPreview(bitmap);
            if (this.mVersions != null) {
                WidgetPreviewLoader.this.mWorkerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!PreviewLoadTask.this.isCancelled()) {
                            WidgetPreviewLoader.this.writeToDb(PreviewLoadTask.this.mKey, PreviewLoadTask.this.mVersions, bitmap);
                            PreviewLoadTask.this.mBitmapToRecycle = bitmap;
                        } else {
                            synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                                WidgetPreviewLoader.this.mUnusedBitmaps.add(bitmap);
                            }
                        }
                    }
                });
            } else {
                this.mBitmapToRecycle = bitmap;
            }
        }

        @Override
        protected void onCancelled(final Bitmap bitmap) {
            if (bitmap != null) {
                WidgetPreviewLoader.this.mWorkerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                            WidgetPreviewLoader.this.mUnusedBitmaps.add(bitmap);
                        }
                    }
                });
            }
        }

        @Override
        public void onCancel() {
            cancel(true);
            if (this.mBitmapToRecycle != null) {
                WidgetPreviewLoader.this.mWorkerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                            WidgetPreviewLoader.this.mUnusedBitmaps.add(PreviewLoadTask.this.mBitmapToRecycle);
                        }
                        PreviewLoadTask.this.mBitmapToRecycle = null;
                    }
                });
            }
        }
    }

    private static final class WidgetCacheKey extends ComponentKey {
        final String size;

        public WidgetCacheKey(ComponentName componentName, UserHandle userHandle, String str) {
            super(componentName, userHandle);
            this.size = str;
        }

        @Override
        public int hashCode() {
            return super.hashCode() ^ this.size.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj) && ((WidgetCacheKey) obj).size.equals(this.size);
        }
    }
}
