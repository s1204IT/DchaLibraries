package com.mediatek.gallery3d.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.support.v4.os.EnvironmentCompat;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.ThumbType;
import com.mediatek.galleryportable.PerfServiceUtils;
import com.mediatek.galleryportable.StorageManagerUtils;
import com.mediatek.omadrm.OmaDrmStore;
import com.mediatek.plugin.preload.SoOperater;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FeatureHelper {
    static final boolean $assertionsDisabled = false;
    private static final AtomicInteger sNextGeneratedViewId;
    private static StorageManager sStorageManager = null;
    private static final HashMap<ExtItem.SupportOperation, Integer> sSpMap = new HashMap<>();

    static {
        sSpMap.put(ExtItem.SupportOperation.DELETE, 1);
        sSpMap.put(ExtItem.SupportOperation.ROTATE, 2);
        sSpMap.put(ExtItem.SupportOperation.SHARE, 4);
        sSpMap.put(ExtItem.SupportOperation.CROP, 8);
        sSpMap.put(ExtItem.SupportOperation.SHOW_ON_MAP, 16);
        sSpMap.put(ExtItem.SupportOperation.SETAS, 32);
        sSpMap.put(ExtItem.SupportOperation.FULL_IMAGE, 64);
        sSpMap.put(ExtItem.SupportOperation.PLAY, 128);
        sSpMap.put(ExtItem.SupportOperation.CACHE, 256);
        sSpMap.put(ExtItem.SupportOperation.EDIT, 512);
        sSpMap.put(ExtItem.SupportOperation.INFO, Integer.valueOf(SoOperater.STEP));
        sSpMap.put(ExtItem.SupportOperation.TRIM, 2048);
        sSpMap.put(ExtItem.SupportOperation.UNLOCK, 4096);
        sSpMap.put(ExtItem.SupportOperation.BACK, 8192);
        sSpMap.put(ExtItem.SupportOperation.ACTION, 16384);
        sSpMap.put(ExtItem.SupportOperation.CAMERA_SHORTCUT, 32768);
        sSpMap.put(ExtItem.SupportOperation.MUTE, 65536);
        sSpMap.put(ExtItem.SupportOperation.PRINT, 131072);
        sNextGeneratedViewId = new AtomicInteger(1);
    }

    public static int mergeSupportOperations(int i, ArrayList<ExtItem.SupportOperation> arrayList, ArrayList<ExtItem.SupportOperation> arrayList2) {
        if (arrayList != null && arrayList.size() != 0) {
            int size = arrayList.size();
            int iIntValue = i;
            for (int i2 = 0; i2 < size; i2++) {
                iIntValue |= sSpMap.get(arrayList.get(i2)).intValue();
            }
            i = iIntValue;
        }
        if (arrayList2 != null && arrayList2.size() != 0) {
            int size2 = arrayList2.size();
            for (int i3 = 0; i3 < size2; i3++) {
                i &= ~sSpMap.get(arrayList2.get(i3)).intValue();
            }
        }
        return i;
    }

    public static ThumbType convertToThumbType(int i) {
        switch (i) {
            case 1:
                return ThumbType.MIDDLE;
            case 2:
                return ThumbType.MICRO;
            case 3:
                return ThumbType.FANCY;
            case 4:
                return ThumbType.HIGHQUALITY;
            default:
                Log.e("MtkGallery2/FeatureHelper", "<covertToThumbType> not support type");
                return null;
        }
    }

    public static void setExtBundle(AbstractGalleryActivity abstractGalleryActivity, Intent intent, Bundle bundle, Path path) {
        bundle.putBoolean("isCamera", intent.getBooleanExtra("isCamera", false));
        ?? mediaObject = abstractGalleryActivity.getDataManager().getMediaObject(path);
        if (mediaObject instanceof MediaItem) {
            mediaObject.getMediaData();
            if (intent.getExtras() != null && intent.getBooleanExtra("isSecureCamera", false) && intent.getExtras().getSerializable("secureAlbum") != null) {
                bundle.putSerializable("secureAlbum", intent.getExtras().getSerializable("secureAlbum"));
                bundle.putString("media-set-path", intent.getStringExtra("securePath"));
                bundle.putBoolean("isSecureCamera", intent.getBooleanExtra("isSecureCamera", false));
            }
        }
    }

    public static Uri tryContentMediaUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String scheme = uri.getScheme();
        UriMatcher uriMatcher = new UriMatcher(-1);
        uriMatcher.addURI("media", "external/file/#", 1);
        if (uriMatcher.match(uri) == 1) {
            return getUriById(context, uri);
        }
        if (!"file".equals(scheme)) {
            return uri;
        }
        String path = uri.getPath();
        Log.d("MtkGallery2/FeatureHelper", "<tryContentMediaUri> for " + path);
        if (!new File(path).exists()) {
            return null;
        }
        return getUriByPath(context, uri);
    }

    private static Uri getUriByPath(Context context, Uri uri) throws Throwable {
        Cursor cursorQuery;
        String string;
        try {
            cursorQuery = MediaStore.Images.Media.query(context.getContentResolver(), MediaStore.Files.getContentUri("external"), new String[]{BookmarkEnhance.COLUMN_ID, BookmarkEnhance.COLUMN_MEDIA_TYPE, "bucket_id"}, "_data=(?)", new String[]{uri.getPath()}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        long j = cursorQuery.getLong(0);
                        String string2 = cursorQuery.getString(1);
                        if (string2 == null) {
                            Log.e("MtkGallery2/FeatureHelper", "<getUriByPath> mimeType == null, Please check  the uri" + uri);
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return null;
                        }
                        if (string2.startsWith(OmaDrmStore.MimePrefix.IMAGE)) {
                            string = MediaStore.Images.Media.getContentUri("external").toString();
                        } else {
                            if (!string2.startsWith(OmaDrmStore.MimePrefix.VIDEO)) {
                                Log.d("MtkGallery2/FeatureHelper", "<getUriByPath> id = " + j + ", mimeType = " + string2 + ", not begin with image/ or video/, return uri " + uri);
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                return uri;
                            }
                            string = MediaStore.Video.Media.getContentUri("external").toString();
                        }
                        Uri uri2 = Uri.parse(string + "/" + j);
                        StringBuilder sb = new StringBuilder();
                        sb.append("<getUriByPath> got ");
                        sb.append(uri2);
                        Log.d("MtkGallery2/FeatureHelper", sb.toString());
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return uri2;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            Log.w("MtkGallery2/FeatureHelper", "<getUriByPath> fail to convert " + uri);
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return uri;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private static Uri getUriById(Context context, Uri uri) throws Throwable {
        Cursor cursorQuery;
        String string;
        Cursor cursor = null;
        try {
            cursorQuery = MediaStore.Images.Media.query(context.getContentResolver(), MediaStore.Files.getContentUri("external"), new String[]{BookmarkEnhance.COLUMN_ID, BookmarkEnhance.COLUMN_MEDIA_TYPE, "bucket_id"}, "_id=(?)", new String[]{uri.getLastPathSegment()}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        long j = cursorQuery.getLong(0);
                        String string2 = cursorQuery.getString(1);
                        if (string2.startsWith(OmaDrmStore.MimePrefix.IMAGE)) {
                            string = MediaStore.Images.Media.getContentUri("external").toString();
                        } else {
                            if (!string2.startsWith(OmaDrmStore.MimePrefix.VIDEO)) {
                                Log.d("MtkGallery2/FeatureHelper", "<getUriById> id = " + j + ", mimeType = " + string2 + ", not begin with image/ or video/, return uri " + uri);
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                return uri;
                            }
                            string = MediaStore.Video.Media.getContentUri("external").toString();
                        }
                        Uri uri2 = Uri.parse(string + "/" + j);
                        Cursor cursorQuery2 = context.getContentResolver().query(uri2, null, null, null, null);
                        if (cursorQuery2 != null) {
                            try {
                                if (cursorQuery2.moveToNext()) {
                                    Log.d("MtkGallery2/FeatureHelper", "<getUriById> got " + uri2);
                                    if (cursorQuery != null) {
                                        cursorQuery.close();
                                    }
                                    if (cursorQuery2 != null) {
                                        cursorQuery2.close();
                                    }
                                    return uri2;
                                }
                            } catch (Throwable th) {
                                cursor = cursorQuery2;
                                th = th;
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                if (cursor != null) {
                                    cursor.close();
                                }
                                throw th;
                            }
                        }
                        Log.w("MtkGallery2/FeatureHelper", "<getUriById> fail to convert " + uri);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        if (cursorQuery2 != null) {
                            cursorQuery2.close();
                        }
                        return uri;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            Log.w("MtkGallery2/FeatureHelper", "<getUriById> fail to convert " + uri);
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return uri;
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = null;
        }
    }

    public static File getExternalCacheDir(Context context) {
        if (context == null) {
            Log.e("MtkGallery2/FeatureHelper", "<getExternalCacheDir> context is null, return null");
            return null;
        }
        String storageForCache = StorageManagerUtils.getStorageForCache(context);
        if (storageForCache == null || storageForCache.equals("")) {
            Log.e("MtkGallery2/FeatureHelper", "<getExternalCacheDir> internalStoragePath is null, return null");
            return null;
        }
        String str = storageForCache + "/Android/data/com.android.gallery3d/cache";
        Log.d("MtkGallery2/FeatureHelper", "<getExternalCacheDir> return external cache dir is " + str);
        File file = new File(str);
        if (file.exists() || file.mkdirs()) {
            return file;
        }
        Log.e("MtkGallery2/FeatureHelper", "<getExternalCacheDir> Fail to create external cache dir, return null");
        return null;
    }

    public static String getDefaultPath() {
        return StorageManagerUtils.getDefaultPath();
    }

    public static String getDefaultStorageState(Context context) {
        String storageState = null;
        if (sStorageManager == null && context == null) {
            return null;
        }
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context.getSystemService("storage");
        }
        String defaultPath = StorageManagerUtils.getDefaultPath();
        if (defaultPath == null || defaultPath.equals("")) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 24) {
            StorageVolume storageVolume = sStorageManager.getStorageVolume(new File(defaultPath));
            if (storageVolume != null) {
                storageState = storageVolume.getState();
            }
        } else if (Build.VERSION.SDK_INT >= 19) {
            storageState = Environment.getStorageState(new File(defaultPath));
        } else {
            storageState = EnvironmentCompat.getStorageState(new File(defaultPath));
        }
        Log.v("MtkGallery2/FeatureHelper", "<getDefaultStorageState> default path = " + defaultPath + ", state = " + storageState);
        return storageState;
    }

    public static boolean isLocalUri(Uri uri) {
        boolean z = false;
        if (uri == null) {
            return false;
        }
        boolean zEquals = "file".equals(uri.getScheme());
        if ("content".equals(uri.getScheme()) && "media".equals(uri.getAuthority())) {
            z = true;
        }
        return zEquals | z;
    }

    public static MediaDetails convertStringArrayToDetails(String[] strArr) {
        if (strArr == null || strArr.length < 1) {
            return null;
        }
        MediaDetails mediaDetails = new MediaDetails();
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i] != null) {
                mediaDetails.addDetail(i + 1, strArr[i]);
            }
        }
        return mediaDetails;
    }

    public static boolean isJpegOutOfLimit(String str, int i, int i2) {
        if (!str.equals("image/jpeg")) {
            return false;
        }
        if (i > 8192 || i2 > 8192) {
            return true;
        }
        return false;
    }

    public static boolean isDefaultStorageMounted(Context context) {
        String defaultStorageState = getDefaultStorageState(context);
        if (defaultStorageState == null) {
            defaultStorageState = Environment.getExternalStorageState();
        }
        return "mounted".equalsIgnoreCase(defaultStorageState);
    }

    public static int getOrientationFromExif(String str, InputStream inputStream) {
        ExifInterface exifInterface;
        int i = 0;
        if (str != null) {
            try {
                if (!str.equals("")) {
                    exifInterface = new ExifInterface(str);
                } else if (inputStream != null && Build.VERSION.SDK_INT >= 24) {
                    exifInterface = new ExifInterface(inputStream);
                } else {
                    Log.d("MtkGallery2/FeatureHelper", "<getOrientationFromExif> sdk version issue, return 0");
                    return 0;
                }
            } catch (IOException e) {
                Log.e("MtkGallery2/FeatureHelper", "<getOrientationFromExif> IOException", e);
                return 0;
            }
        }
        int attributeInt = exifInterface.getAttributeInt("Orientation", 0);
        Log.d("MtkGallery2/FeatureHelper", "<getOrientationFromExif> exif orientation: " + attributeInt);
        if (attributeInt != 1) {
            if (attributeInt == 3) {
                i = 180;
            } else if (attributeInt == 6) {
                i = 90;
            } else if (attributeInt == 8) {
                i = 270;
            }
        }
        Log.d("MtkGallery2/FeatureHelper", "<getOrientationFromExif> rotation: " + i);
        return i;
    }

    public static int generateViewId() {
        int i;
        int i2;
        do {
            i = sNextGeneratedViewId.get();
            i2 = i + 1;
            if (i2 > 16777215) {
                i2 = 1;
            }
        } while (!sNextGeneratedViewId.compareAndSet(i, i2));
        return i;
    }

    public static void modifyBoostPolicy(Context context) {
        if (!isCacheFileExists(context)) {
            Log.i("MtkGallery2/FeatureHelper", "<modifyBoostPolicy> gallery cache not exists, and execute boost policy");
            PerfServiceUtils.boostEnableTimeoutMs(1500);
        }
    }

    public static boolean isCacheFileExists(Context context) {
        File[] fileArrListFiles;
        File externalCacheDir = getExternalCacheDir(context);
        if (externalCacheDir == null || (fileArrListFiles = externalCacheDir.listFiles()) == null || fileArrListFiles.length == 0) {
            return false;
        }
        for (File file : fileArrListFiles) {
            if (file.getName().endsWith("idx")) {
                Log.d("MtkGallery2/FeatureHelper", "<isCacheFileExists> File cache exists!");
                return true;
            }
        }
        return false;
    }
}
