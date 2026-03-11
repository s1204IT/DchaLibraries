package com.android.browser.sitenavigation;

import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.browser.R;
import com.android.browser.sitenavigation.TemplateSiteNavigation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandlerSiteNavigation extends Thread {
    private static final UriMatcher S_URI_MATCHER = new UriMatcher(-1);
    Context mContext;
    OutputStream mOutput;
    Uri mUri;

    static {
        S_URI_MATCHER.addURI("com.android.browser.site_navigation", "websites/res/*/*", 2);
        S_URI_MATCHER.addURI("com.android.browser.site_navigation", "websites", 1);
    }

    public RequestHandlerSiteNavigation(Context context, Uri uri, OutputStream outputStream) {
        this.mUri = uri;
        this.mContext = context.getApplicationContext();
        this.mOutput = outputStream;
    }

    private void writeTemplatedIndex() throws Throwable {
        Cursor cursorQuery;
        TemplateSiteNavigation cachedTemplate = TemplateSiteNavigation.getCachedTemplate(this.mContext, 2131165210);
        try {
            cursorQuery = this.mContext.getContentResolver().query(Uri.parse("content://com.android.browser.site_navigation/websites"), new String[]{"url", "title", "thumbnail"}, null, null, null);
        } catch (Throwable th) {
            th = th;
            cursorQuery = null;
        }
        try {
            cachedTemplate.assignLoop("site_navigation", new TemplateSiteNavigation.CursorListEntityWrapper(this, cursorQuery) {
                final RequestHandlerSiteNavigation this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void writeValue(OutputStream outputStream, String str) throws IOException {
                    Cursor cursor = getCursor();
                    if (str.equals("url")) {
                        outputStream.write(this.this$0.htmlEncode(cursor.getString(0)));
                        return;
                    }
                    if (!str.equals("title")) {
                        if (str.equals("thumbnail")) {
                            outputStream.write("data:image/png;base64,".getBytes());
                            outputStream.write(Base64.encode(cursor.getBlob(2), 0));
                            return;
                        }
                        return;
                    }
                    String string = cursor.getString(1);
                    if (string == null || string.length() == 0) {
                        string = this.this$0.mContext.getString(2131492880);
                    }
                    outputStream.write(this.this$0.htmlEncode(string));
                }
            });
            cachedTemplate.write(this.mOutput);
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th2) {
            th = th2;
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            throw th;
        }
    }

    void cleanup() {
        try {
            this.mOutput.close();
        } catch (IOException e) {
            Log.e("RequestHandlerSiteNavigation", "Failed to close pipe!", e);
        }
    }

    void doHandleRequest() throws Throwable {
        switch (S_URI_MATCHER.match(this.mUri)) {
            case 1:
                writeTemplatedIndex();
                break;
            case 2:
                writeResource(getUriResourcePath());
                break;
        }
    }

    String getUriResourcePath() {
        Matcher matcher = Pattern.compile("/?res/([\\w/]+)").matcher(this.mUri.getPath());
        return matcher.matches() ? matcher.group(1) : this.mUri.getPath();
    }

    byte[] htmlEncode(String str) {
        return TextUtils.htmlEncode(str).getBytes();
    }

    @Override
    public void run() {
        super.run();
        try {
            try {
                doHandleRequest();
            } catch (IOException e) {
                Log.e("RequestHandlerSiteNavigation", "Failed to handle request: " + this.mUri, e);
            }
        } finally {
            cleanup();
        }
    }

    void writeResource(String str) throws IOException {
        Resources resources = this.mContext.getResources();
        int identifier = resources.getIdentifier(str, null, R.class.getPackage().getName());
        if (identifier == 0) {
            return;
        }
        InputStream inputStreamOpenRawResource = resources.openRawResource(identifier);
        byte[] bArr = new byte[4096];
        while (true) {
            int i = inputStreamOpenRawResource.read(bArr);
            if (i <= 0) {
                inputStreamOpenRawResource.close();
                return;
            }
            this.mOutput.write(bArr, 0, i);
        }
    }
}
