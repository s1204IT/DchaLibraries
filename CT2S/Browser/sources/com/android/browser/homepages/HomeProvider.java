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
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        try {
            ParcelFileDescriptor[] pipes = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor write = pipes[1];
            AssetFileDescriptor afd = new AssetFileDescriptor(write, 0L, -1L);
            new RequestHandler(getContext(), uri, afd.createOutputStream()).start();
            return pipes[0];
        } catch (IOException e) {
            Log.e("HomeProvider", "Failed to handle request: " + uri, e);
            return null;
        }
    }

    public static WebResourceResponse shouldInterceptRequest(Context context, String url) {
        WebResourceResponse webResourceResponse;
        boolean useMostVisited;
        try {
            useMostVisited = BrowserSettings.getInstance().useMostVisitedHomepage();
        } catch (Exception e) {
        }
        if (useMostVisited && url.startsWith("content://")) {
            Uri uri = Uri.parse(url);
            if ("com.android.browser.home".equals(uri.getAuthority())) {
                webResourceResponse = new WebResourceResponse("text/html", "utf-8", context.getContentResolver().openInputStream(uri));
            }
        } else {
            boolean listFiles = BrowserSettings.getInstance().isDebugEnabled();
            if (listFiles && interceptFile(url)) {
                PipedInputStream ins = new PipedInputStream();
                PipedOutputStream outs = new PipedOutputStream(ins);
                new RequestHandler(context, Uri.parse(url), outs).start();
                webResourceResponse = new WebResourceResponse("text/html", "utf-8", ins);
            } else {
                webResourceResponse = null;
            }
        }
        return webResourceResponse;
    }

    private static boolean interceptFile(String url) {
        if (!url.startsWith("file:///")) {
            return false;
        }
        String fpath = url.substring(7);
        File f = new File(fpath);
        return f.isDirectory();
    }
}
