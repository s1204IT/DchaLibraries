package com.android.gallery3d.gadget;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.base.MediaFilterSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class LocalPhotoSource implements WidgetSource {
    private static String SELECTION;
    private ContentListener mContentListener;
    private ContentObserver mContentObserver;
    private Context mContext;
    private DataManager mDataManager;
    private HandlerThread mHandlerThread;
    private static final Uri CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static final String[] PROJECTION = {BookmarkEnhance.COLUMN_ID};
    private static final String[] COUNT_PROJECTION = {"count(*)"};
    private static final String ORDER = String.format("%s DESC", "datetaken");
    private static final Path LOCAL_IMAGE_ROOT = Path.fromString("/local/image/item");
    private ArrayList<Long> mPhotos = new ArrayList<>();
    private boolean mContentDirty = true;

    public LocalPhotoSource(Context context) {
        this.mHandlerThread = null;
        SELECTION = MediaFilterSetting.getExtWhereClause(String.format("%s != %s", "bucket_id", Integer.valueOf(getDownloadBucketId())));
        this.mContext = context;
        this.mDataManager = ((GalleryApp) context.getApplicationContext()).getDataManager();
        this.mHandlerThread = new HandlerThread("LocalPhotoSource-HandlerThread", 10);
        this.mHandlerThread.start();
        Looper looper = this.mHandlerThread.getLooper();
        while (looper == null) {
            Log.d("Gallery2/LocalPhotoSource", "<LocalPhotoSource> looper is null, wait 5 ms");
            try {
                Thread.sleep(5L);
            } catch (InterruptedException e) {
                Log.d("Gallery2/LocalPhotoSource", "<LocalPhotoSource> Thread.sleep InterruptedException");
            }
            looper = this.mHandlerThread.getLooper();
        }
        this.mContentObserver = new ContentObserver(new Handler(looper)) {
            @Override
            public void onChange(boolean z) {
                Log.d("Gallery2/LocalPhotoSource", "<LocalPhotoSource> ContentObserver.onChange: selfchange=" + z + ", listener=" + LocalPhotoSource.this.mContentListener);
                LocalPhotoSource.this.mContentDirty = true;
                if (LocalPhotoSource.this.mContentListener != null) {
                    LocalPhotoSource.this.mContentListener.onContentDirty();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(CONTENT_URI, true, this.mContentObserver);
        Log.d("Gallery2/LocalPhotoSource", "<LocalPhotoSource>content observer registered!!");
    }

    @Override
    public void close() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
        this.mHandlerThread.quit();
    }

    @Override
    public Uri getContentUri(int i) {
        synchronized (this.mPhotos) {
            if (i < this.mPhotos.size()) {
                return CONTENT_URI.buildUpon().appendPath(String.valueOf(this.mPhotos.get(i))).build();
            }
            return null;
        }
    }

    @Override
    public Bitmap getImage(int i) {
        synchronized (this.mPhotos) {
            if (i >= this.mPhotos.size()) {
                Log.e("Gallery2/LocalPhotoSource", "getImage: index out of range: " + i + ", size=" + this.mPhotos.size());
                return null;
            }
            long jLongValue = this.mPhotos.get(i).longValue();
            ?? r10 = (MediaItem) this.mDataManager.getMediaObject(LOCAL_IMAGE_ROOT.getChild(jLongValue));
            StringBuilder sb = new StringBuilder();
            sb.append("getImage: id=");
            sb.append(jLongValue);
            sb.append(", mediaitem=");
            sb.append(r10 == 0 ? "null" : r10.getName());
            Log.d("Gallery2/LocalPhotoSource", sb.toString());
            if (r10 == 0) {
                return null;
            }
            if (r10 instanceof LocalImage) {
                try {
                    Cursor cursorQuery = this.mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, LocalImage.getProjection(), "_id = ?", new String[]{String.valueOf(r10.id)}, null);
                    if (cursorQuery != null && cursorQuery.moveToFirst()) {
                        r10.updateContent(cursorQuery);
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (SecurityException e) {
                    Log.d("Gallery2/LocalPhotoSource", "<getImage> SecurityException", e);
                    return null;
                }
            }
            return WidgetUtils.createWidgetBitmap(r10);
        }
    }

    private int[] getExponentialIndice(int i, int i2) {
        Random random = new Random();
        if (i2 > i) {
            i2 = i;
        }
        HashSet hashSet = new HashSet(i2);
        while (hashSet.size() < i2) {
            int i3 = (int) (((-Math.log(random.nextDouble())) * ((double) i)) / 2.0d);
            if (i3 < i) {
                hashSet.add(Integer.valueOf(i3));
            }
        }
        int[] iArr = new int[i2];
        int i4 = 0;
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            iArr[i4] = ((Integer) it.next()).intValue();
            i4++;
        }
        return iArr;
    }

    private int getPhotoCount(ContentResolver contentResolver) {
        try {
            Cursor cursorQuery = contentResolver.query(CONTENT_URI, COUNT_PROJECTION, SELECTION, null, null);
            if (cursorQuery == null) {
                return 0;
            }
            try {
                Utils.assertTrue(cursorQuery.moveToNext());
                return cursorQuery.getInt(0);
            } finally {
                cursorQuery.close();
            }
        } catch (SecurityException e) {
            Log.d("Gallery2/LocalPhotoSource", "<getPhotoCount> SecurityException", e);
            return 0;
        }
    }

    private boolean isContentSound(int i) {
        if (this.mPhotos.size() < Math.min(i, 128)) {
            return false;
        }
        if (this.mPhotos.size() == 0) {
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (Long l : this.mPhotos) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(l);
        }
        try {
            Cursor cursorQuery = this.mContext.getContentResolver().query(CONTENT_URI, COUNT_PROJECTION, String.format("%s in (%s)", BookmarkEnhance.COLUMN_ID, sb.toString()), null, null);
            if (cursorQuery == null) {
                return false;
            }
            try {
                Utils.assertTrue(cursorQuery.moveToNext());
                return cursorQuery.getInt(0) == this.mPhotos.size();
            } finally {
                cursorQuery.close();
            }
        } catch (SecurityException e) {
            Log.d("Gallery2/LocalPhotoSource", "<isContentSound> SecurityException", e);
            return false;
        }
    }

    @Override
    public synchronized void reload() {
        if (this.mContentDirty) {
            this.mContentDirty = false;
            int photoCount = getPhotoCount(this.mContext.getContentResolver());
            Log.d("Gallery2/LocalPhotoSource", "<reload> DB photo count=" + photoCount);
            synchronized (this.mPhotos) {
                if (isContentSound(photoCount)) {
                    Log.w("Gallery2/LocalPhotoSource", "<reload> content is sound, return and do nothing...");
                    return;
                }
                int[] exponentialIndice = getExponentialIndice(photoCount, 128);
                Log.d("Gallery2/LocalPhotoSource", "<reload> random ids count=" + exponentialIndice.length);
                Arrays.sort(exponentialIndice);
                ArrayList arrayList = new ArrayList();
                try {
                    Cursor cursorQuery = this.mContext.getContentResolver().query(CONTENT_URI, PROJECTION, SELECTION, null, ORDER);
                    if (cursorQuery == null) {
                        Log.e("Gallery2/LocalPhotoSource", "<reload> query returns null");
                        return;
                    }
                    try {
                        for (int i : exponentialIndice) {
                            if (cursorQuery.moveToPosition(i)) {
                                arrayList.add(Long.valueOf(cursorQuery.getLong(0)));
                            }
                        }
                        synchronized (this.mPhotos) {
                            this.mPhotos.clear();
                            this.mPhotos.addAll(arrayList);
                            arrayList.clear();
                        }
                        Log.d("Gallery2/LocalPhotoSource", "<reload>reload result: new photo count=" + this.mPhotos.size());
                    } finally {
                        cursorQuery.close();
                    }
                } catch (SecurityException e) {
                    Log.d("Gallery2/LocalPhotoSource", "<reload> SecurityException", e);
                }
            }
        }
    }

    @Override
    public synchronized int size() {
        int size;
        reload();
        synchronized (this.mPhotos) {
            size = this.mPhotos.size();
        }
        return size;
    }

    private static int getDownloadBucketId() {
        return GalleryUtils.getBucketId(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
    }

    @Override
    public void setContentListener(ContentListener contentListener) {
        this.mContentListener = contentListener;
    }

    @Override
    public void forceNotifyDirty() {
        this.mContentObserver.onChange(true);
    }
}
