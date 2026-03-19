package com.mediatek.internal.content;

import android.content.Context;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FileSystemProviderExt {
    private static final String MIMETYPE_OCTET_STREAM = "application/octet-stream";
    private static final String TAG = "FileSystemProviderExt";
    private static final String classFileSystemProviderHelper = "com.mediatek.internal.content.MtkFileSystemProviderHelper";
    private Context mContext;
    private String[] mDefaultProjection;
    private static final boolean DEBUG = "eng".equals(Build.TYPE);
    private static FileSystemProviderExt sInstance = null;
    private static Object sFSPHelper = null;
    private static Method sAddSupportDRMMethod = null;
    private static Method sGetTypeForNameMethod = null;
    private static Method sGetDefaultProjectionMethod = null;
    private static final Uri BASE_URI = new Uri.Builder().scheme("content").authority(DocumentsContract.EXTERNAL_STORAGE_PROVIDER_AUTHORITY).build();

    public static FileSystemProviderExt getInstance(Context context) {
        if (context == null) {
            return null;
        }
        if (sInstance == null) {
            sInstance = new FileSystemProviderExt(context);
        }
        return sInstance;
    }

    private FileSystemProviderExt(Context context) {
        if (DEBUG) {
            Log.d(TAG, "[DRM]- Contructor is called FileSystemProviderExt");
        }
        this.mContext = context;
        onCreateMTKHelper();
    }

    private void onCreateMTKHelper() {
        String[] strArr;
        if (DEBUG) {
            Log.d(TAG, "onCreateMTKHelper");
        }
        try {
            Class<?> cls = Class.forName(classFileSystemProviderHelper);
            try {
                Constructor<?> constructor = cls.getConstructor(Context.class);
                if (constructor != null) {
                    try {
                        sFSPHelper = constructor.newInstance(this.mContext);
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                        Log.d(TAG, "[DRM] - Failed to create constructor for helper class.");
                        return;
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "onCreate sFSPHelper : " + sFSPHelper);
                }
                Class<?>[] clsArr = {File.class, String.class};
                Class<?>[] clsArr2 = new Class[0];
                try {
                    sAddSupportDRMMethod = cls.getDeclaredMethod("supportDRM", File.class, MatrixCursor.RowBuilder.class, String.class, String.class, File.class);
                } catch (NoSuchMethodException e2) {
                    sAddSupportDRMMethod = null;
                }
                try {
                    sGetTypeForNameMethod = cls.getDeclaredMethod("getTypeForNameMtk", clsArr);
                } catch (NoSuchMethodException e3) {
                    sGetTypeForNameMethod = null;
                }
                try {
                    sGetDefaultProjectionMethod = cls.getDeclaredMethod("getDefaultProjection", clsArr2);
                } catch (NoSuchMethodException e4) {
                    sGetDefaultProjectionMethod = null;
                }
                if (sGetDefaultProjectionMethod != null) {
                    try {
                        strArr = (String[]) sGetDefaultProjectionMethod.invoke(sFSPHelper, new Object[0]);
                    } catch (IllegalAccessException | InvocationTargetException e5) {
                        Log.d(TAG, "[DRM]-Unable to access GetDefaultProjectionMethod()");
                        strArr = null;
                    }
                    this.mDefaultProjection = strArr;
                }
            } catch (NoSuchMethodException e6) {
                Log.d(TAG, "[DRM]- onCreate Helper Class constructor not found");
            }
        } catch (ClassNotFoundException e7) {
            Log.d(TAG, "[DRM]- onCreate Helper Class not found");
        }
    }

    public String[] resolveProjection(String[] strArr) {
        return this.mDefaultProjection == null ? strArr : this.mDefaultProjection;
    }

    public static void addSupportDRMMethod(File file, MatrixCursor.RowBuilder rowBuilder, String str, String str2, File file2) {
        if (sAddSupportDRMMethod != null) {
            try {
                sAddSupportDRMMethod.invoke(sFSPHelper, file, rowBuilder, str, str2, file2);
            } catch (IllegalAccessException | InvocationTargetException e) {
                Log.d(TAG, "[DRM]-Unable to access AddSupportDRMMethod()");
            }
        }
    }

    public static String getTypeForNameMethod(File file) {
        String mimeTypeFromExtension;
        String name = file.getName();
        if (sGetTypeForNameMethod != null) {
            try {
                String str = (String) sGetTypeForNameMethod.invoke(sFSPHelper, file, name);
                Log.d(TAG, "getTypeForNameMethod" + str);
                if (str == null) {
                    str = MIMETYPE_OCTET_STREAM;
                }
                return str;
            } catch (IllegalAccessException | InvocationTargetException e) {
                Log.d(TAG, "[DRM]-Unable to access GetTypeForNameMethod()");
                return MIMETYPE_OCTET_STREAM;
            }
        }
        int iLastIndexOf = name.lastIndexOf(46);
        if (iLastIndexOf < 0 || (mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(iLastIndexOf + 1).toLowerCase())) == null) {
            return MIMETYPE_OCTET_STREAM;
        }
        return mimeTypeFromExtension;
    }
}
