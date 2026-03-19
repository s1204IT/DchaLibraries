package com.android.browser.homepages;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.WebResourceResponse;
import com.android.browser.BrowserSettings;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class HomeProvider extends ContentProvider {
    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) {
        try {
            ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
            new RequestHandler(getContext(), uri, new AssetFileDescriptor(parcelFileDescriptorArrCreatePipe[1], 0L, -1L).createOutputStream()).start();
            return parcelFileDescriptorArrCreatePipe[0];
        } catch (IOException e) {
            Log.e("HomeProvider", "Failed to handle request: " + uri, e);
            return null;
        }
    }

    public static WebResourceResponse shouldInterceptRequest(Context context, String str) {
        try {
            if (!str.equals("content://com.android.browser.site_navigation/websites") && !str.equals("content://com.android.browser.home/")) {
                if (BrowserSettings.getInstance().isDebugEnabled() && interceptFile(str)) {
                    PipedInputStream pipedInputStream = new PipedInputStream();
                    new RequestHandler(context, Uri.parse(str), new PipedOutputStream(pipedInputStream)).start();
                    return new WebResourceResponse("text/html", "utf-8", pipedInputStream);
                }
                return null;
            }
            return new WebResourceResponse("text/html", "utf-8", context.getContentResolver().openInputStream(Uri.parse(str)));
        } catch (IOException e) {
            Log.e("HomeProvider", "Failed to create WebResourceResponse: " + e.getMessage());
            return null;
        }
    }

    private static boolean interceptFile(String str) {
        return str.startsWith("file:///") && new File(str.substring(7)).isDirectory();
    }
}
