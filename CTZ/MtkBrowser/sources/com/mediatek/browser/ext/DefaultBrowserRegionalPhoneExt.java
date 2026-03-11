package com.mediatek.browser.ext;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.browser.provider.BrowserContract;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DefaultBrowserRegionalPhoneExt implements IBrowserRegionalPhoneExt {
    public static final boolean DEBUG;

    private static class UpdateBookmarkTask extends AsyncTask<Void, Void, Void> {
        Context mContext;

        public UpdateBookmarkTask(Context context) {
            this.mContext = context;
        }

        private int addDefaultBookmarks(Context context, CharSequence[] charSequenceArr, TypedArray typedArray, int i) {
            byte[] raw;
            Resources resources = context.getResources();
            int length = charSequenceArr.length;
            if (DefaultBrowserRegionalPhoneExt.DEBUG) {
                Log.i("DefaultBrowserRegionalPhoneExt", "bookmarks count = " + length);
            }
            try {
                try {
                    String string = Long.toString(1L);
                    String string2 = Long.toString(System.currentTimeMillis());
                    for (int i2 = 0; i2 < length; i2 += 2) {
                        int i3 = i2 + 1;
                        CharSequence charSequence = charSequenceArr[i3];
                        int i4 = "http://www.google.com/".equals(charSequence.toString()) ? 1 : i + i2;
                        int resourceId = typedArray.getResourceId(i3, 0);
                        int resourceId2 = typedArray.getResourceId(i2, 0);
                        byte[] raw2 = null;
                        try {
                            raw = readRaw(resources, resourceId);
                        } catch (IOException e) {
                            raw = null;
                        }
                        try {
                            raw2 = readRaw(resources, resourceId2);
                        } catch (IOException e2) {
                            Log.i("DefaultBrowserRegionalPhoneExt", "IOException for thumb");
                        }
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("title", charSequenceArr[i2].toString());
                        contentValues.put("url", charSequence.toString());
                        contentValues.put("folder", (Integer) 0);
                        contentValues.put("thumbnail", raw);
                        contentValues.put("favicon", raw2);
                        contentValues.put("parent", string);
                        contentValues.put("position", Integer.toString(i4));
                        contentValues.put("created", string2);
                        boolean z = context.getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, contentValues) != null;
                        if (DefaultBrowserRegionalPhoneExt.DEBUG) {
                            Log.i("DefaultBrowserRegionalPhoneExt", "for " + i2 + "update result = " + z);
                        }
                    }
                } finally {
                    typedArray.recycle();
                }
            } catch (ArrayIndexOutOfBoundsException e3) {
                Log.i("DefaultBrowserRegionalPhoneExt", "ArrayIndexOutOfBoundsException is caught");
            }
            return length;
        }

        private void addOMBookmarks(Context context) {
            try {
                Resources resourcesForApplication = context.getPackageManager().getResourcesForApplication("com.android.browser");
                int identifier = resourcesForApplication.getIdentifier("com.android.browser:array/bookmarks_for_yahoo", null, null);
                Log.d("DefaultBrowserRegionalPhoneExt", "addOMBookmarks(), first resourceId = " + identifier);
                CharSequence[] textArray = identifier != 0 ? resourcesForApplication.getTextArray(identifier) : null;
                int identifier2 = resourcesForApplication.getIdentifier("com.android.browser:array/bookmark_preloads_for_yahoo", null, null);
                Log.d("DefaultBrowserRegionalPhoneExt", "addOMBookmarks(), first Preload resourceId = " + identifier2);
                TypedArray typedArrayObtainTypedArray = identifier2 != 0 ? resourcesForApplication.obtainTypedArray(identifier2) : null;
                if (textArray == null || typedArrayObtainTypedArray == null) {
                    return;
                }
                int iAddDefaultBookmarks = addDefaultBookmarks(context, textArray, typedArrayObtainTypedArray, 2);
                int identifier3 = resourcesForApplication.getIdentifier("com.android.browser:array/bookmarks", null, null);
                Log.d("DefaultBrowserRegionalPhoneExt", "addOMBookmarks(), Other resourceId = " + identifier3);
                CharSequence[] textArray2 = identifier3 != 0 ? resourcesForApplication.getTextArray(identifier3) : null;
                int identifier4 = resourcesForApplication.getIdentifier("com.android.browser:array/bookmark_preloads", null, null);
                Log.d("DefaultBrowserRegionalPhoneExt", "addOMBookmarks(), other Preload resourceId = " + identifier4);
                TypedArray typedArrayObtainTypedArray2 = identifier4 != 0 ? resourcesForApplication.obtainTypedArray(identifier4) : null;
                if (textArray2 == null || typedArrayObtainTypedArray2 == null) {
                    return;
                }
                addDefaultBookmarks(context, textArray2, typedArrayObtainTypedArray2, iAddDefaultBookmarks);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        private String makeInQueryString(int i) {
            StringBuilder sb = new StringBuilder();
            if (i > 0) {
                sb.append(" IN ( ");
                String str = "";
                for (int i2 = 0; i2 < i; i2++) {
                    sb.append(str);
                    sb.append("?");
                    str = ",";
                }
                sb.append(" )");
            }
            return sb.toString();
        }

        private byte[] readRaw(Resources resources, int i) throws IOException {
            if (i == 0) {
                return null;
            }
            InputStream inputStreamOpenRawResource = resources.openRawResource(i);
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[4096];
                while (true) {
                    int i2 = inputStreamOpenRawResource.read(bArr);
                    if (i2 <= 0) {
                        byteArrayOutputStream.flush();
                        return byteArrayOutputStream.toByteArray();
                    }
                    byteArrayOutputStream.write(bArr, 0, i2);
                }
            } finally {
                inputStreamOpenRawResource.close();
            }
        }

        private void removeOperatorBookmarks(Context context) {
            Uri uri = BrowserContract.Bookmarks.CONTENT_URI;
            try {
                Resources resourcesForApplication = context.getPackageManager().getResourcesForApplication("com.android.browser");
                int identifier = resourcesForApplication.getIdentifier("com.android.browser:array/bookmarks_for_op03", null, null);
                Log.d("DefaultBrowserRegionalPhoneExt", "OP03 resourceId = " + identifier);
                CharSequence[] textArray = identifier != 0 ? resourcesForApplication.getTextArray(identifier) : null;
                if (textArray != null) {
                    Log.d("DefaultBrowserRegionalPhoneExt", " OP03 bookmarks size = " + textArray.length);
                    int length = textArray.length / 2;
                    String[] strArr = new String[length];
                    for (int i = 0; i < length; i++) {
                        strArr[i] = textArray[(i * 2) + 1].toString();
                    }
                    Log.d("DefaultBrowserRegionalPhoneExt", "Delete count = " + context.getContentResolver().delete(uri, "url" + makeInQueryString(length), strArr));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Void doInBackground(Void... voidArr) {
            removeOperatorBookmarks(this.mContext);
            addOMBookmarks(this.mContext);
            return null;
        }
    }

    static {
        DEBUG = !Build.TYPE.equals("user") ? true : SystemProperties.getBoolean("ro.mtk_browser_debug_enablelog", false);
    }

    private boolean needUpdateBookmarks(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String str = SystemProperties.get("persist.vendor.operator.optr");
        Log.d("DefaultBrowserRegionalPhoneExt", "system property = " + str);
        String string = defaultSharedPreferences.getString("operator_bookmarks", "OPNONE");
        Log.d("DefaultBrowserRegionalPhoneExt", "currentOperator = " + string);
        boolean zEquals = str.equals(string) ^ true;
        if (zEquals) {
            zEquals = string.equals("OP03");
        }
        SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
        editorEdit.putString("operator_bookmarks", str);
        editorEdit.commit();
        return zEquals;
    }

    @Override
    public String getSearchEngine(SharedPreferences sharedPreferences, Context context) {
        Log.i("@M_DefaultBrowserRegionalPhoneExt", "Enter: updateSearchEngine --default implement");
        return null;
    }

    @Override
    public void updateBookmarks(Context context) {
        if (needUpdateBookmarks(context)) {
            new UpdateBookmarkTask(context).execute(new Void[0]);
        } else {
            Log.i("@M_DefaultBrowserRegionalPhoneExt", "Enter: updateBookmarks --default implement");
        }
    }
}
