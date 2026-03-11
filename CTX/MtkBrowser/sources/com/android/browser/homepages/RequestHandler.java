package com.android.browser.homepages;

import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.browser.R;
import com.android.browser.homepages.Template;
import com.android.browser.provider.BrowserContract;
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
        sUriMatcher.addURI("com.android.browser.home", null, 1);
        sUriMatcher.addURI("com.android.browser.home", "res/*/*", 2);
        PROJECTION = new String[]{"url", "title", "thumbnail"};
        sFileComparator = new Comparator<File>() {
            @Override
            public int compare(File file, File file2) {
                return file.isDirectory() != file2.isDirectory() ? file.isDirectory() ? -1 : 1 : file.getName().compareTo(file2.getName());
            }
        };
    }

    public RequestHandler(Context context, Uri uri, OutputStream outputStream) {
        this.mUri = uri;
        this.mContext = context.getApplicationContext();
        this.mOutput = outputStream;
    }

    static String readableFileSize(long j) {
        if (j <= 0) {
            return "0";
        }
        double d = j;
        int iLog10 = (int) (Math.log10(d) / Math.log10(1024.0d));
        return new DecimalFormat("#,##0.#").format(d / Math.pow(1024.0d, iLog10)) + " " + new String[]{"B", "KB", "MB", "GB", "TB"}[iLog10];
    }

    void cleanup() {
        try {
            this.mOutput.close();
        } catch (Exception e) {
            Log.e("RequestHandler", "Failed to close pipe!", e);
        }
    }

    void doHandleRequest() throws Throwable {
        if ("file".equals(this.mUri.getScheme())) {
            writeFolderIndex();
        }
        switch (sUriMatcher.match(this.mUri)) {
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
            } catch (Exception e) {
                Log.e("RequestHandler", "Failed to handle request: " + this.mUri, e);
            }
        } finally {
            cleanup();
        }
    }

    void writeFolderIndex() throws IOException {
        File file = new File(this.mUri.getPath());
        File[] fileArrListFiles = file.listFiles();
        Arrays.sort(fileArrListFiles, sFileComparator);
        Template cachedTemplate = Template.getCachedTemplate(this.mContext, 2131165203);
        cachedTemplate.assign("path", this.mUri.getPath());
        cachedTemplate.assign("parent_url", file.getParent() != null ? file.getParent() : file.getPath());
        cachedTemplate.assignLoop("files", new Template.ListEntityIterator(this, fileArrListFiles) {
            int index = -1;
            final RequestHandler this$0;
            final File[] val$files;

            {
                this.this$0 = this;
                this.val$files = fileArrListFiles;
            }

            @Override
            public Template.ListEntityIterator getListIterator(String str) {
                return null;
            }

            @Override
            public boolean moveToNext() {
                int i = this.index + 1;
                this.index = i;
                return i < this.val$files.length;
            }

            @Override
            public void reset() {
                this.index = -1;
            }

            @Override
            public void writeValue(OutputStream outputStream, String str) throws IOException {
                File file2 = this.val$files[this.index];
                if ("name".equals(str)) {
                    outputStream.write(file2.getName().getBytes());
                }
                if ("url".equals(str)) {
                    outputStream.write(("file://" + file2.getAbsolutePath()).getBytes());
                }
                if ("type".equals(str)) {
                    outputStream.write((file2.isDirectory() ? "dir" : "file").getBytes());
                }
                if ("size".equals(str) && file2.isFile()) {
                    outputStream.write(RequestHandler.readableFileSize(file2.length()).getBytes());
                }
                if ("last_modified".equals(str)) {
                    outputStream.write(DateFormat.getDateTimeInstance(3, 3).format(Long.valueOf(file2.lastModified())).getBytes());
                }
                if ("alt".equals(str) && this.index % 2 == 0) {
                    outputStream.write("alt".getBytes());
                }
            }
        });
        cachedTemplate.write(this.mOutput);
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
                return;
            } else {
                this.mOutput.write(bArr, 0, i);
            }
        }
    }

    void writeTemplatedIndex() throws Throwable {
        Throwable th;
        Cursor cursor;
        Template cachedTemplate = Template.getCachedTemplate(this.mContext, 2131165204);
        Cursor cursorQuery = this.mContext.getContentResolver().query(BrowserContract.History.CONTENT_URI, PROJECTION, "url NOT LIKE 'content:%' AND thumbnail IS NOT NULL", null, "visits DESC LIMIT 12");
        try {
            cursor = cursorQuery.getCount() < 12 ? new MergeCursor(this, new Cursor[]{cursorQuery, this.mContext.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, PROJECTION, "url NOT LIKE 'content:%' AND thumbnail IS NOT NULL", null, "created DESC LIMIT 12")}) {
                final RequestHandler this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public int getCount() {
                    return Math.min(12, super.getCount());
                }
            } : cursorQuery;
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            cachedTemplate.assignLoop("most_visited", new Template.CursorListEntityWrapper(this, cursor) {
                final RequestHandler this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void writeValue(OutputStream outputStream, String str) throws IOException {
                    Cursor cursor2 = getCursor();
                    if (str.equals("url")) {
                        outputStream.write(this.this$0.htmlEncode(cursor2.getString(0)));
                        return;
                    }
                    if (str.equals("title")) {
                        outputStream.write(this.this$0.htmlEncode(cursor2.getString(1)));
                    } else if (str.equals("thumbnail")) {
                        outputStream.write("data:image/png;base64,".getBytes());
                        outputStream.write(Base64.encode(cursor2.getBlob(2), 0));
                    }
                }
            });
            cachedTemplate.write(this.mOutput);
            cursor.close();
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = cursor;
            cursorQuery.close();
            throw th;
        }
    }
}
