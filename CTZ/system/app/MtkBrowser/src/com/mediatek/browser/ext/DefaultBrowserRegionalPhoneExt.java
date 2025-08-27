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

/* loaded from: classes.dex */
public class DefaultBrowserRegionalPhoneExt implements IBrowserRegionalPhoneExt {
    public static final boolean DEBUG;

    static {
        DEBUG = !Build.TYPE.equals("user") ? true : SystemProperties.getBoolean("ro.mtk_browser_debug_enablelog", false);
    }

    @Override // com.mediatek.browser.ext.IBrowserRegionalPhoneExt
    public String getSearchEngine(SharedPreferences sharedPreferences, Context context) {
        Log.i("@M_DefaultBrowserRegionalPhoneExt", "Enter: updateSearchEngine --default implement");
        return null;
    }

    @Override // com.mediatek.browser.ext.IBrowserRegionalPhoneExt
    public void updateBookmarks(Context context) {
        if (!needUpdateBookmarks(context)) {
            Log.i("@M_DefaultBrowserRegionalPhoneExt", "Enter: updateBookmarks --default implement");
        } else {
            new UpdateBookmarkTask(context).execute(new Void[0]);
        }
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

    private static class UpdateBookmarkTask extends AsyncTask<Void, Void, Void> {
        Context mContext;

        public UpdateBookmarkTask(Context context) {
            this.mContext = context;
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected Void doInBackground(Void... voidArr) throws Resources.NotFoundException, PackageManager.NameNotFoundException, IOException {
            removeOperatorBookmarks(this.mContext);
            addOMBookmarks(this.mContext);
            return null;
        }

        private void removeOperatorBookmarks(Context context) throws PackageManager.NameNotFoundException {
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
                        strArr[i] = textArray[(2 * i) + 1].toString();
                    }
                    Log.d("DefaultBrowserRegionalPhoneExt", "Delete count = " + context.getContentResolver().delete(uri, "url" + makeInQueryString(length), strArr));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void addOMBookmarks(Context context) throws Resources.NotFoundException, PackageManager.NameNotFoundException, IOException {
            CharSequence[] textArray;
            TypedArray typedArrayObtainTypedArray;
            CharSequence[] textArray2;
            try {
                Resources resourcesForApplication = context.getPackageManager().getResourcesForApplication("com.android.browser");
                int identifier = resourcesForApplication.getIdentifier("com.android.browser:array/bookmarks_for_yahoo", null, null);
                Log.d("DefaultBrowserRegionalPhoneExt", "addOMBookmarks(), first resourceId = " + identifier);
                if (identifier != 0) {
                    textArray = resourcesForApplication.getTextArray(identifier);
                } else {
                    textArray = null;
                }
                int identifier2 = resourcesForApplication.getIdentifier("com.android.browser:array/bookmark_preloads_for_yahoo", null, null);
                Log.d("DefaultBrowserRegionalPhoneExt", "addOMBookmarks(), first Preload resourceId = " + identifier2);
                if (identifier2 != 0) {
                    typedArrayObtainTypedArray = resourcesForApplication.obtainTypedArray(identifier2);
                } else {
                    typedArrayObtainTypedArray = null;
                }
                if (textArray != null && typedArrayObtainTypedArray != null) {
                    int iAddDefaultBookmarks = addDefaultBookmarks(context, textArray, typedArrayObtainTypedArray, 2);
                    int identifier3 = resourcesForApplication.getIdentifier("com.android.browser:array/bookmarks", null, null);
                    Log.d("DefaultBrowserRegionalPhoneExt", "addOMBookmarks(), Other resourceId = " + identifier3);
                    if (identifier3 != 0) {
                        textArray2 = resourcesForApplication.getTextArray(identifier3);
                    } else {
                        textArray2 = null;
                    }
                    int identifier4 = resourcesForApplication.getIdentifier("com.android.browser:array/bookmark_preloads", null, null);
                    Log.d("DefaultBrowserRegionalPhoneExt", "addOMBookmarks(), other Preload resourceId = " + identifier4);
                    TypedArray typedArrayObtainTypedArray2 = identifier4 != 0 ? resourcesForApplication.obtainTypedArray(identifier4) : null;
                    if (textArray2 != null && typedArrayObtainTypedArray2 != null) {
                        addDefaultBookmarks(context, textArray2, typedArrayObtainTypedArray2, iAddDefaultBookmarks);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        /* JADX WARN: Removed duplicated region for block: B:25:0x00c0  */
        /* JADX WARN: Removed duplicated region for block: B:26:0x00c2  */
        /* JADX WARN: Removed duplicated region for block: B:29:0x00c7 A[Catch: all -> 0x00ea, ArrayIndexOutOfBoundsException -> 0x00ec, TRY_LEAVE, TryCatch #3 {ArrayIndexOutOfBoundsException -> 0x00ec, blocks: (B:6:0x0027, B:8:0x0039, B:12:0x004f, B:14:0x0059, B:15:0x005d, B:23:0x0073, B:27:0x00c3, B:29:0x00c7, B:22:0x006b, B:11:0x004d), top: B:43:0x0027, outer: #2 }] */
        /* JADX WARN: Removed duplicated region for block: B:47:0x00e5 A[SYNTHETIC] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private int addDefaultBookmarks(Context context, CharSequence[] charSequenceArr, TypedArray typedArray, int i) throws Resources.NotFoundException, IOException {
            int i2;
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
                    int i3 = 0;
                    int i4 = 0;
                    while (i4 < length) {
                        int i5 = i4 + 1;
                        CharSequence charSequence = charSequenceArr[i5];
                        if (!"http://www.google.com/".equals(charSequence.toString())) {
                            i2 = i + i4;
                        } else {
                            i2 = 1;
                        }
                        int resourceId = typedArray.getResourceId(i5, i3);
                        int resourceId2 = typedArray.getResourceId(i4, i3);
                        byte[] raw2 = null;
                        try {
                            raw = readRaw(resources, resourceId);
                            try {
                                raw2 = readRaw(resources, resourceId2);
                            } catch (IOException e) {
                                Log.i("DefaultBrowserRegionalPhoneExt", "IOException for thumb");
                                byte[] bArr = raw;
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("title", charSequenceArr[i4].toString());
                                contentValues.put("url", charSequence.toString());
                                contentValues.put("folder", (Integer) 0);
                                contentValues.put("thumbnail", bArr);
                                contentValues.put("favicon", raw2);
                                contentValues.put("parent", string);
                                contentValues.put("position", Integer.toString(i2));
                                contentValues.put("created", string2);
                                if (context.getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, contentValues) == null) {
                                }
                                if (!DefaultBrowserRegionalPhoneExt.DEBUG) {
                                }
                                i4 += 2;
                                i3 = 0;
                            }
                        } catch (IOException e2) {
                            raw = null;
                        }
                        byte[] bArr2 = raw;
                        ContentValues contentValues2 = new ContentValues();
                        contentValues2.put("title", charSequenceArr[i4].toString());
                        contentValues2.put("url", charSequence.toString());
                        contentValues2.put("folder", (Integer) 0);
                        contentValues2.put("thumbnail", bArr2);
                        contentValues2.put("favicon", raw2);
                        contentValues2.put("parent", string);
                        contentValues2.put("position", Integer.toString(i2));
                        contentValues2.put("created", string2);
                        boolean z = context.getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, contentValues2) == null;
                        if (!DefaultBrowserRegionalPhoneExt.DEBUG) {
                            Log.i("DefaultBrowserRegionalPhoneExt", "for " + i4 + "update result = " + z);
                        }
                        i4 += 2;
                        i3 = 0;
                    }
                } catch (ArrayIndexOutOfBoundsException e3) {
                    Log.i("DefaultBrowserRegionalPhoneExt", "ArrayIndexOutOfBoundsException is caught");
                }
                return length;
            } finally {
                typedArray.recycle();
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

        private byte[] readRaw(Resources resources, int i) throws Resources.NotFoundException, IOException {
            if (i == 0) {
                return null;
            }
            InputStream inputStreamOpenRawResource = resources.openRawResource(i);
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[4096];
                while (true) {
                    int i2 = inputStreamOpenRawResource.read(bArr);
                    if (i2 > 0) {
                        byteArrayOutputStream.write(bArr, 0, i2);
                    } else {
                        byteArrayOutputStream.flush();
                        return byteArrayOutputStream.toByteArray();
                    }
                }
            } finally {
                inputStreamOpenRawResource.close();
            }
        }
    }
}
