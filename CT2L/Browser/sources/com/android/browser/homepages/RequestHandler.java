package com.android.browser.homepages;

import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.BrowserContract;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.browser.R;
import com.android.browser.homepages.Template;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandler extends Thread {
    private static final String[] PROJECTION;
    private static final Comparator<File> sFileComparator;
    private static final UriMatcher sUriMatcher = new UriMatcher(-1);
    Context mContext;
    OutputStream mOutput;
    Uri mUri;

    static {
        sUriMatcher.addURI("com.android.browser.home", "/", 1);
        sUriMatcher.addURI("com.android.browser.home", "res/*/*", 2);
        PROJECTION = new String[]{"url", "title", "thumbnail"};
        sFileComparator = new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                if (lhs.isDirectory() != rhs.isDirectory()) {
                    return lhs.isDirectory() ? -1 : 1;
                }
                return lhs.getName().compareTo(rhs.getName());
            }
        };
    }

    public RequestHandler(Context context, Uri uri, OutputStream out) {
        this.mUri = uri;
        this.mContext = context.getApplicationContext();
        this.mOutput = out;
    }

    @Override
    public void run() {
        super.run();
        try {
            doHandleRequest();
        } catch (Exception e) {
            Log.e("RequestHandler", "Failed to handle request: " + this.mUri, e);
        } finally {
            cleanup();
        }
    }

    void doHandleRequest() throws IOException {
        if ("file".equals(this.mUri.getScheme())) {
            writeFolderIndex();
        }
        int match = sUriMatcher.match(this.mUri);
        switch (match) {
            case 1:
                writeTemplatedIndex();
                break;
            case 2:
                writeResource(getUriResourcePath());
                break;
        }
    }

    byte[] htmlEncode(String s) {
        return TextUtils.htmlEncode(s).getBytes();
    }

    void writeTemplatedIndex() throws IOException {
        Template t = Template.getCachedTemplate(this.mContext, R.raw.most_visited);
        Cursor historyResults = this.mContext.getContentResolver().query(BrowserContract.History.CONTENT_URI, PROJECTION, "url NOT LIKE 'content:%' AND thumbnail IS NOT NULL", null, "visits DESC LIMIT 12");
        Cursor cursor = historyResults;
        try {
            if (cursor.getCount() < 12) {
                Cursor bookmarkResults = this.mContext.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, PROJECTION, "url NOT LIKE 'content:%' AND thumbnail IS NOT NULL", null, "created DESC LIMIT 12");
                cursor = new MergeCursor(new Cursor[]{historyResults, bookmarkResults}) {
                    @Override
                    public int getCount() {
                        return Math.min(12, super.getCount());
                    }
                };
            }
            t.assignLoop("most_visited", new Template.CursorListEntityWrapper(cursor) {
                @Override
                public void writeValue(OutputStream stream, String key) throws IOException {
                    Cursor cursor2 = getCursor();
                    if (key.equals("url")) {
                        stream.write(RequestHandler.this.htmlEncode(cursor2.getString(0)));
                        return;
                    }
                    if (key.equals("title")) {
                        stream.write(RequestHandler.this.htmlEncode(cursor2.getString(1)));
                    } else if (key.equals("thumbnail")) {
                        stream.write("data:image/png;base64,".getBytes());
                        byte[] thumb = cursor2.getBlob(2);
                        stream.write(Base64.encode(thumb, 0));
                    }
                }
            });
            t.write(this.mOutput);
        } finally {
            cursor.close();
        }
    }

    void writeFolderIndex() throws IOException {
        File f = new File(this.mUri.getPath());
        final File[] files = f.listFiles();
        Arrays.sort(files, sFileComparator);
        Template t = Template.getCachedTemplate(this.mContext, R.raw.folder_view);
        t.assign("path", this.mUri.getPath());
        t.assign("parent_url", f.getParent() != null ? f.getParent() : f.getPath());
        t.assignLoop("files", new Template.ListEntityIterator() {
            int index = -1;

            @Override
            public void writeValue(OutputStream stream, String key) throws IOException {
                File f2 = files[this.index];
                if ("name".equals(key)) {
                    stream.write(f2.getName().getBytes());
                }
                if ("url".equals(key)) {
                    stream.write(("file://" + f2.getAbsolutePath()).getBytes());
                }
                if ("type".equals(key)) {
                    stream.write((f2.isDirectory() ? "dir" : "file").getBytes());
                }
                if ("size".equals(key) && f2.isFile()) {
                    stream.write(RequestHandler.readableFileSize(f2.length()).getBytes());
                }
                if ("last_modified".equals(key)) {
                    String date = DateFormat.getDateTimeInstance(3, 3).format(Long.valueOf(f2.lastModified()));
                    stream.write(date.getBytes());
                }
                if ("alt".equals(key) && this.index % 2 == 0) {
                    stream.write("alt".getBytes());
                }
            }

            @Override
            public Template.ListEntityIterator getListIterator(String key) {
                return null;
            }

            @Override
            public void reset() {
                this.index = -1;
            }

            @Override
            public boolean moveToNext() {
                int i = this.index + 1;
                this.index = i;
                return i < files.length;
            }
        });
        t.write(this.mOutput);
    }

    static String readableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024.0d));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024.0d, digitGroups)) + " " + units[digitGroups];
    }

    String getUriResourcePath() {
        Pattern pattern = Pattern.compile("/?res/([\\w/]+)");
        Matcher m = pattern.matcher(this.mUri.getPath());
        return m.matches() ? m.group(1) : this.mUri.getPath();
    }

    void writeResource(String fileName) throws IOException {
        Resources res = this.mContext.getResources();
        String packageName = R.class.getPackage().getName();
        int id = res.getIdentifier(fileName, null, packageName);
        if (id != 0) {
            InputStream in = res.openRawResource(id);
            byte[] buf = new byte[4096];
            while (true) {
                int read = in.read(buf);
                if (read > 0) {
                    this.mOutput.write(buf, 0, read);
                } else {
                    return;
                }
            }
        }
    }

    void cleanup() {
        try {
            this.mOutput.close();
        } catch (Exception e) {
            Log.e("RequestHandler", "Failed to close pipe!", e);
        }
    }
}
