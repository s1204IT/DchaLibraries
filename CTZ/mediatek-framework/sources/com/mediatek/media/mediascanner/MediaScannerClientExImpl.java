package com.mediatek.media.mediascanner;

import android.content.ContentValues;
import android.content.Context;
import android.drm.DrmManagerClient;
import android.media.MediaFile;
import android.media.MediaScanner;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.media.MtkMediaStore;
import com.mediatek.mmsdk.BaseParameters;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MediaScannerClientExImpl extends MediaScannerClientEx {
    private static final int APP1 = 65505;
    private static final int APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT = 4;
    private static final String CLASS_NAME_MY_MEDIA_SCANNER_CLIENT = "MyMediaScannerClient";
    private static final boolean DEBUG;
    private static final String FIELD_NAME_CONTEXT = "mContext";
    private static final String FIELD_NAME_FILE_TYPE = "mFileType";
    private static final String FIELD_NAME_IS_DRM = "mIsDrm";
    private static final String FIELD_NAME_MIME_TYPE = "mMimeType";
    private static final String FIELD_NAME_MY_MEDIA_SCANNER_CLIENT = "mClient";
    private static final String METHOD_IS_DRM_ENABLED = "isDrmEnabled";
    private static final String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String MTK_REFOCUS_PREFIX = "MRefocus";
    private static final String NS_GDEPTH = "http://ns.google.com/photos/1.0/depthmap/";
    private static final int SOI = 65496;
    private static final int SOS = 65498;
    private static final String TAG = "MediaScannerClientExImpl";
    private static final String XMP_EXT_MAIN_HEADER1 = "http://ns.adobe.com/xmp/extension/";
    private static final String XMP_HEADER_START = "http://ns.adobe.com/xap/1.0/\u0000";
    private static Class sClassMyMediaScannerClient;
    private static Field sFieldClient;
    private static Field sFieldContext;
    private static Field sFieldFileType;
    private static Field sFieldIsDrm;
    private static Field sFieldMimeType;
    private static Method sMethodIsDrmEnabled;
    private ExMetaData mExMetaData;

    public static class ExMetaData {
        public String mDrmContentUr = null;
        public long mDrmOffset = -1;
        public long mDrmDataLen = -1;
        public String mDrmRightsIssuer = null;
        public String mDrmContentName = null;
        public String mDrmContentDescriptioin = null;
        public String mDrmContentVendor = null;
        public String mDrmIconUri = null;
        public long mDrmMethod = -1;
        public int mOrientation = 0;
    }

    static {
        DEBUG = Log.isLoggable(TAG, 3) || "eng".equals(Build.TYPE);
        sFieldClient = getField(MediaScanner.class, FIELD_NAME_MY_MEDIA_SCANNER_CLIENT);
        sFieldContext = getField(MediaScanner.class, FIELD_NAME_CONTEXT);
        sMethodIsDrmEnabled = getMethod(MediaScanner.class, METHOD_IS_DRM_ENABLED, new Class[0]);
        for (Class<?> cls : MediaScanner.class.getDeclaredClasses()) {
            if (cls.getSimpleName().equals(CLASS_NAME_MY_MEDIA_SCANNER_CLIENT)) {
                sClassMyMediaScannerClient = cls;
                sFieldIsDrm = getField(sClassMyMediaScannerClient, FIELD_NAME_IS_DRM);
                sFieldIsDrm.setAccessible(true);
                sFieldFileType = getField(sClassMyMediaScannerClient, FIELD_NAME_FILE_TYPE);
                sFieldFileType.setAccessible(true);
                sFieldMimeType = getField(sClassMyMediaScannerClient, FIELD_NAME_MIME_TYPE);
                sFieldMimeType.setAccessible(true);
                return;
            }
        }
    }

    public void init() {
        this.mExMetaData = new ExMetaData();
    }

    public void parseExMetaDataFromStringTag(String str, String str2, MediaScanner mediaScanner) {
        if (str.equalsIgnoreCase(MtkMediaStore.MediaColumns.DRM_CONTENT_URI)) {
            this.mExMetaData.mDrmContentUr = str2.trim();
            return;
        }
        if (str.equalsIgnoreCase(MtkMediaStore.MediaColumns.DRM_OFFSET)) {
            this.mExMetaData.mDrmOffset = parseSubstring(str2, 0, 0);
            return;
        }
        if (str.equalsIgnoreCase(MtkMediaStore.MediaColumns.DRM_DATA_LEN)) {
            this.mExMetaData.mDrmDataLen = parseSubstring(str2, 0, 0);
            return;
        }
        if (str.equalsIgnoreCase(MtkMediaStore.MediaColumns.DRM_RIGHTS_ISSUER)) {
            this.mExMetaData.mDrmRightsIssuer = str2.trim();
            return;
        }
        if (str.equalsIgnoreCase(MtkMediaStore.MediaColumns.DRM_CONTENT_NAME)) {
            this.mExMetaData.mDrmContentName = str2.trim();
            return;
        }
        if (str.equalsIgnoreCase(MtkMediaStore.MediaColumns.DRM_CONTENT_DESCRIPTION)) {
            this.mExMetaData.mDrmContentDescriptioin = str2.trim();
            return;
        }
        if (str.equalsIgnoreCase(MtkMediaStore.MediaColumns.DRM_CONTENT_VENDOR)) {
            this.mExMetaData.mDrmContentVendor = str2.trim();
            return;
        }
        if (str.equalsIgnoreCase(MtkMediaStore.MediaColumns.DRM_ICON_URI)) {
            this.mExMetaData.mDrmIconUri = str2.trim();
        } else if (str.equalsIgnoreCase(MtkMediaStore.MediaColumns.DRM_METHOD)) {
            this.mExMetaData.mDrmMethod = parseSubstring(str2, 0, 0);
        } else if (str.equalsIgnoreCase(BaseParameters.KEY_PICTURE_ROTATION)) {
            this.mExMetaData.mOrientation = parseSubstring(str2, 0, 0);
        }
    }

    public void addExMetaDataToContentValues(ContentValues contentValues, MediaScanner mediaScanner) {
        Object fieldOnObject = getFieldOnObject(sFieldClient, mediaScanner);
        if (fieldOnObject == null) {
            Log.e(TAG, "[addExMetaDataToContentValues] client is null, return");
            return;
        }
        if (((Boolean) getFieldOnObject(sFieldIsDrm, fieldOnObject)).booleanValue()) {
            contentValues.put(MtkMediaStore.MediaColumns.DRM_CONTENT_DESCRIPTION, this.mExMetaData.mDrmContentDescriptioin);
            contentValues.put(MtkMediaStore.MediaColumns.DRM_CONTENT_NAME, this.mExMetaData.mDrmContentName);
            contentValues.put(MtkMediaStore.MediaColumns.DRM_CONTENT_URI, this.mExMetaData.mDrmContentUr);
            contentValues.put(MtkMediaStore.MediaColumns.DRM_CONTENT_VENDOR, this.mExMetaData.mDrmContentVendor);
            contentValues.put(MtkMediaStore.MediaColumns.DRM_DATA_LEN, Long.valueOf(this.mExMetaData.mDrmDataLen));
            contentValues.put(MtkMediaStore.MediaColumns.DRM_ICON_URI, this.mExMetaData.mDrmIconUri);
            contentValues.put(MtkMediaStore.MediaColumns.DRM_OFFSET, Long.valueOf(this.mExMetaData.mDrmOffset));
            contentValues.put(MtkMediaStore.MediaColumns.DRM_RIGHTS_ISSUER, this.mExMetaData.mDrmRightsIssuer);
            contentValues.put(MtkMediaStore.MediaColumns.DRM_METHOD, Long.valueOf(this.mExMetaData.mDrmMethod));
        }
        if (MediaFile.isVideoFileType(((Integer) getFieldOnObject(sFieldFileType, fieldOnObject)).intValue())) {
            contentValues.put(MtkMediaStore.VideoColumns.ORIENTATION, Integer.valueOf(this.mExMetaData.mOrientation));
        }
    }

    public void correctFileType(String str, String str2, MediaScanner mediaScanner) {
        int iLastIndexOf;
        Object fieldOnObject = getFieldOnObject(sFieldClient, mediaScanner);
        if (fieldOnObject == null) {
            Log.e(TAG, "[correctFileType] client is null, return");
            return;
        }
        if (MediaFile.isImageFileType(((Integer) getFieldOnObject(sFieldFileType, fieldOnObject)).intValue()) && (iLastIndexOf = str.lastIndexOf(".")) > 0 && str.substring(iLastIndexOf + 1).toUpperCase().equals("DCF")) {
            if (DEBUG) {
                Log.v(TAG, "[correctFileType] detect a *.DCF file with input mime type = " + str2);
            }
            if (sFieldFileType != null) {
                try {
                    sFieldFileType.setInt(fieldOnObject, 0);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "[correctFileType] IllegalAccessException", e);
                }
            }
        }
    }

    public void correctMetaData(String str, MediaScanner mediaScanner) {
        Object fieldOnObject = getFieldOnObject(sFieldClient, mediaScanner);
        if (fieldOnObject == null) {
            Log.e(TAG, "[correctFileType] client is null, return");
            return;
        }
        if (((Boolean) callMethodOnObject(mediaScanner, sMethodIsDrmEnabled, new Object[0])).booleanValue() && str.endsWith(".mudp")) {
            DrmManagerClient drmManagerClient = new DrmManagerClient((Context) getFieldOnObject(sFieldContext, mediaScanner));
            if (drmManagerClient.canHandle(str, (String) null)) {
                if (sFieldMimeType != null) {
                    try {
                        sFieldMimeType.set(fieldOnObject, drmManagerClient.getOriginalMimeType(str));
                    } catch (IllegalAccessException e) {
                        Log.e(TAG, "[correctMetaData] IllegalAccessException", e);
                    }
                }
                if (sFieldIsDrm != null) {
                    try {
                        sFieldIsDrm.setBoolean(fieldOnObject, true);
                    } catch (IllegalAccessException e2) {
                        Log.e(TAG, "[correctMetaData] IllegalAccessException", e2);
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "[correctMetaData] get cta file " + str + " with original mimetype = " + getFieldOnObject(sFieldMimeType, fieldOnObject));
                }
            }
        }
    }

    public void putExtensionContentValuesForImage(ContentValues contentValues, String str) {
        contentValues.put(MtkMediaStore.ImageColumns.CAMERA_REFOCUS, Integer.valueOf(isStereoPhoto(str) ? 1 : 0));
    }

    public static boolean isStereoPhoto(String str) throws Throwable {
        RandomAccessFile randomAccessFile;
        if (str == null) {
            if (DEBUG) {
                Log.d(TAG, "<isStereoPhoto> filePath is null!!");
            }
            return false;
        }
        if (!new File(str).exists()) {
            if (DEBUG) {
                Log.d(TAG, "<isStereoPhoto> " + str + " not exists!!!");
            }
            return false;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        ArrayList<Section> app1Info = parseApp1Info(str);
        if (app1Info == null || app1Info.size() < 0) {
            if (DEBUG) {
                Log.d(TAG, "<isStereoPhoto> " + str + ", no app1 sections");
            }
            return false;
        }
        RandomAccessFile randomAccessFile2 = null;
        try {
            try {
                randomAccessFile = new RandomAccessFile(str, "r");
                for (int i = 0; i < app1Info.size(); i++) {
                    try {
                        if (isStereo(app1Info.get(i), randomAccessFile)) {
                            if (DEBUG) {
                                Log.d(TAG, "<isStereoPhoto> " + str + " is stereo photo");
                            }
                            try {
                                randomAccessFile.close();
                            } catch (IOException e) {
                                Log.e(TAG, "<isStereoPhoto> IOException:", e);
                            }
                            if (!DEBUG) {
                                return true;
                            }
                            Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - jCurrentTimeMillis));
                            return true;
                        }
                    } catch (FileNotFoundException e2) {
                        e = e2;
                        randomAccessFile2 = randomAccessFile;
                        Log.e(TAG, "<isStereoPhoto> FileNotFoundException:", e);
                        if (randomAccessFile2 != null) {
                            try {
                                randomAccessFile2.close();
                            } catch (IOException e3) {
                                Log.e(TAG, "<isStereoPhoto> IOException:", e3);
                            }
                        }
                        if (DEBUG) {
                            Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - jCurrentTimeMillis));
                        }
                        return false;
                    } catch (IllegalArgumentException e4) {
                        e = e4;
                        randomAccessFile2 = randomAccessFile;
                        Log.e(TAG, "<isStereoPhoto> IllegalArgumentException:", e);
                        if (randomAccessFile2 != null) {
                            try {
                                randomAccessFile2.close();
                            } catch (IOException e5) {
                                Log.e(TAG, "<isStereoPhoto> IOException:", e5);
                            }
                        }
                        if (DEBUG) {
                            Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - jCurrentTimeMillis));
                        }
                        return false;
                    } catch (Throwable th) {
                        th = th;
                        if (randomAccessFile != null) {
                            try {
                                randomAccessFile.close();
                            } catch (IOException e6) {
                                Log.e(TAG, "<isStereoPhoto> IOException:", e6);
                            }
                        }
                        if (!DEBUG) {
                            throw th;
                        }
                        Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - jCurrentTimeMillis));
                        throw th;
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "<isStereoPhoto> " + str + " is not stereo photo");
                }
                try {
                    randomAccessFile.close();
                } catch (IOException e7) {
                    Log.e(TAG, "<isStereoPhoto> IOException:", e7);
                }
                if (DEBUG) {
                    Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - jCurrentTimeMillis));
                }
                return false;
            } catch (Throwable th2) {
                th = th2;
                randomAccessFile = null;
            }
        } catch (FileNotFoundException e8) {
            e = e8;
        } catch (IllegalArgumentException e9) {
            e = e9;
        }
    }

    private static boolean isStereo(Section section, RandomAccessFile randomAccessFile) {
        try {
            if (section.mIsXmpMain) {
                randomAccessFile.seek(section.mOffset + 2);
                int unsignedShort = randomAccessFile.readUnsignedShort() - 2;
                randomAccessFile.skipBytes(XMP_HEADER_START.length());
                byte[] bArr = new byte[unsignedShort - XMP_HEADER_START.length()];
                randomAccessFile.read(bArr, 0, bArr.length);
                if (new String(bArr).contains(MTK_REFOCUS_PREFIX)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "<isStereo> IOException:", e);
            return false;
        }
    }

    private static ArrayList<Section> parseApp1Info(String str) throws Throwable {
        Throwable th;
        RandomAccessFile randomAccessFile;
        RandomAccessFile randomAccessFile2 = null;
        try {
            try {
                randomAccessFile = new RandomAccessFile(str, "r");
                try {
                    if (randomAccessFile.readUnsignedShort() != SOI) {
                        if (DEBUG) {
                            Log.d(TAG, "<parseApp1Info> error, find no SOI");
                        }
                        ArrayList<Section> arrayList = new ArrayList<>();
                        try {
                            randomAccessFile.close();
                        } catch (IOException e) {
                            Log.e(TAG, "<parseApp1Info> IOException, path " + str, e);
                        }
                        return arrayList;
                    }
                    ArrayList<Section> arrayList2 = new ArrayList<>();
                    while (true) {
                        int unsignedShort = randomAccessFile.readUnsignedShort();
                        if (unsignedShort != -1 && unsignedShort != SOS) {
                            long filePointer = randomAccessFile.getFilePointer() - 2;
                            int unsignedShort2 = randomAccessFile.readUnsignedShort();
                            if (unsignedShort == APP1) {
                                Section section = new Section(unsignedShort, filePointer, unsignedShort2);
                                long filePointer2 = randomAccessFile.getFilePointer();
                                Section sectionCheckIfMainXmpInApp1 = checkIfMainXmpInApp1(randomAccessFile, section);
                                if (sectionCheckIfMainXmpInApp1 != null && sectionCheckIfMainXmpInApp1.mIsXmpMain) {
                                    arrayList2.add(sectionCheckIfMainXmpInApp1);
                                    break;
                                }
                                randomAccessFile.seek(filePointer2);
                            }
                            randomAccessFile.skipBytes(unsignedShort2 - 2);
                        }
                    }
                    try {
                        randomAccessFile.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "<parseApp1Info> IOException, path " + str, e2);
                    }
                    return arrayList2;
                } catch (IOException e3) {
                    e = e3;
                    Log.e(TAG, "<parseApp1Info> IOException, path " + str, e);
                    if (randomAccessFile != null) {
                        try {
                            randomAccessFile.close();
                        } catch (IOException e4) {
                            Log.e(TAG, "<parseApp1Info> IOException, path " + str, e4);
                        }
                    }
                    return null;
                }
            } catch (Throwable th2) {
                th = th2;
                if (0 != 0) {
                    try {
                        randomAccessFile2.close();
                    } catch (IOException e5) {
                        Log.e(TAG, "<parseApp1Info> IOException, path " + str, e5);
                    }
                }
                throw th;
            }
        } catch (IOException e6) {
            e = e6;
            randomAccessFile = null;
        } catch (Throwable th3) {
            th = th3;
            if (0 != 0) {
            }
            throw th;
        }
    }

    private static Section checkIfMainXmpInApp1(RandomAccessFile randomAccessFile, Section section) {
        if (section == null) {
            if (DEBUG) {
                Log.d(TAG, "<checkIfMainXmpInApp1> section is null!!!");
            }
            return null;
        }
        try {
            if (section.mMarker == APP1) {
                randomAccessFile.seek(section.mOffset + 4);
                byte[] bArr = new byte[XMP_EXT_MAIN_HEADER1.length()];
                randomAccessFile.read(bArr, 0, bArr.length);
                if (XMP_HEADER_START.equals(new String(bArr, 0, XMP_HEADER_START.length()))) {
                    section.mIsXmpMain = true;
                }
            }
            return section;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "<checkIfMainXmpInApp1> UnsupportedEncodingException" + e);
            return null;
        } catch (IOException e2) {
            Log.e(TAG, "<checkIfMainXmpInApp1> IOException" + e2);
            return null;
        }
    }

    private static class Section {
        public boolean mIsXmpMain;
        public int mLength;
        public int mMarker;
        public long mOffset;

        public Section(int i, long j, int i2) {
            this.mMarker = i;
            this.mOffset = j;
            this.mLength = i2;
        }
    }

    private static int parseSubstring(String str, int i, int i2) {
        int length = str.length();
        if (i == length) {
            return i2;
        }
        int i3 = i + 1;
        char cCharAt = str.charAt(i);
        if (cCharAt < '0' || cCharAt > '9') {
            return i2;
        }
        int i4 = cCharAt - '0';
        while (i3 < length) {
            int i5 = i3 + 1;
            char cCharAt2 = str.charAt(i3);
            if (cCharAt2 < '0' || cCharAt2 > '9') {
                return i4;
            }
            i4 = (i4 * 10) + (cCharAt2 - '0');
            i3 = i5;
        }
        return i4;
    }

    private static Field getField(Class<?> cls, String str) {
        try {
            Field declaredField = cls.getDeclaredField(str);
            declaredField.setAccessible(true);
            return declaredField;
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "[getField]", e);
            return null;
        }
    }

    private static Object getFieldOnObject(Field field, Object obj) {
        if (field != null) {
            try {
                return field.get(obj);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "[getFieldOnObject]", e);
                return null;
            }
        }
        return null;
    }

    public static Method getMethod(Class<?> cls, String str, Class<?>... clsArr) {
        try {
            Method declaredMethod = cls.getDeclaredMethod(str, clsArr);
            declaredMethod.setAccessible(true);
            return declaredMethod;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "[getMethod]", e);
            return null;
        }
    }

    public static Object callMethodOnObject(Object obj, Method method, Object... objArr) {
        try {
            return method.invoke(obj, objArr);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "[callMethodOnObject]", e);
            return null;
        } catch (InvocationTargetException e2) {
            Log.e(TAG, "[callMethodOnObject]", e2);
            return null;
        }
    }

    public boolean doesSettingEmpty(String str, Context context) {
        if (TextUtils.isEmpty(Settings.System.getString(context.getContentResolver(), str))) {
            return true;
        }
        return false;
    }

    public void setSettingFlag(String str, Context context) {
        Log.d(TAG, "setSettingFlag set:" + str);
        Settings.System.putString(context.getContentResolver(), str, "yes");
    }

    public boolean isValueslessMimeType(String str) {
        if (MIME_APPLICATION_OCTET_STREAM.equalsIgnoreCase(str)) {
            if (!DEBUG) {
                return true;
            }
            Log.v(TAG, "isValueslessMimeType: mimetype=" + str);
            return true;
        }
        return false;
    }
}
