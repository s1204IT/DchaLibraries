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

    public RequestHandlerSiteNavigation(Context context, Uri uri, OutputStream out) {
        this.mUri = uri;
        this.mContext = context.getApplicationContext();
        this.mOutput = out;
    }

    @Override
    public void run() {
        super.run();
        try {
            doHandleRequest();
        } catch (IOException e) {
            Log.e("RequestHandlerSiteNavigation", "Failed to handle request: " + this.mUri, e);
        } finally {
            cleanup();
        }
    }

    void doHandleRequest() throws IOException {
        int match = S_URI_MATCHER.match(this.mUri);
        switch (match) {
            case 1:
                writeTemplatedIndex();
                break;
            case 2:
                writeResource(getUriResourcePath());
                break;
        }
    }

    private void writeTemplatedIndex() throws IOException {
        TemplateSiteNavigation t = TemplateSiteNavigation.getCachedTemplate(this.mContext, R.raw.site_navigation);
        Cursor cursor = null;
        try {
            cursor = this.mContext.getContentResolver().query(Uri.parse("content://com.android.browser.site_navigation/websites"), new String[]{"url", "title", "thumbnail"}, null, null, null);
            t.assignLoop("site_navigation", new TemplateSiteNavigation.CursorListEntityWrapper(cursor) {
                @Override
                public void writeValue(OutputStream stream, String key) throws IOException {
                    Cursor cursor2 = getCursor();
                    if (key.equals("url")) {
                        stream.write(RequestHandlerSiteNavigation.this.htmlEncode(cursor2.getString(0)));
                        return;
                    }
                    if (key.equals("title")) {
                        String title = cursor2.getString(1);
                        if (title == null || title.length() == 0) {
                            title = RequestHandlerSiteNavigation.this.mContext.getString(R.string.sitenavigation_add);
                        }
                        stream.write(RequestHandlerSiteNavigation.this.htmlEncode(title));
                        return;
                    }
                    if (!key.equals("thumbnail")) {
                        return;
                    }
                    stream.write("data:image/png;base64,".getBytes());
                    byte[] thumb = cursor2.getBlob(2);
                    stream.write(Base64.encode(thumb, 0));
                }
            });
            t.write(this.mOutput);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    byte[] htmlEncode(String s) {
        return TextUtils.htmlEncode(s).getBytes();
    }

    String getUriResourcePath() {
        Pattern pattern = Pattern.compile("/?res/([\\w/]+)");
        Matcher m = pattern.matcher(this.mUri.getPath());
        if (m.matches()) {
            return m.group(1);
        }
        return this.mUri.getPath();
    }

    void writeResource(String fileName) throws IOException {
        Resources res = this.mContext.getResources();
        String packageName = R.class.getPackage().getName();
        int id = res.getIdentifier(fileName, null, packageName);
        if (id == 0) {
            return;
        }
        InputStream in = res.openRawResource(id);
        byte[] buf = new byte[4096];
        while (true) {
            int read = in.read(buf);
            if (read > 0) {
                this.mOutput.write(buf, 0, read);
            } else {
                in.close();
                return;
            }
        }
    }

    void cleanup() {
        try {
            this.mOutput.close();
        } catch (IOException e) {
            Log.e("RequestHandlerSiteNavigation", "Failed to close pipe!", e);
        }
    }
}
