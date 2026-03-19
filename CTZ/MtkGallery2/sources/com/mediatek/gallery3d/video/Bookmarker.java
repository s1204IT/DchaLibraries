package com.mediatek.gallery3d.video;

import android.content.Context;
import android.net.Uri;
import com.android.gallery3d.common.BlobCache;
import com.android.gallery3d.util.CacheManager;
import com.mediatek.gallery3d.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Bookmarker {
    private static final String BOOKMARK_CACHE_FILE = "bookmark";
    private static final int BOOKMARK_CACHE_MAX_BYTES = 10240;
    private static final int BOOKMARK_CACHE_MAX_ENTRIES = 100;
    private static final int BOOKMARK_CACHE_VERSION = 1;
    private static final int HALF_MINUTE = 30000;
    private static final String TAG = "VP_Bookmarker";
    private static final int TWO_MINUTES = 120000;
    private final Context mContext;

    public Bookmarker(Context context) {
        this.mContext = context;
    }

    public void setBookmark(Uri uri, int i, int i2) {
        Log.v(TAG, "setBookmark(" + uri + ", " + i + ", " + i2 + ")");
        if (i2 < 0) {
            return;
        }
        try {
            BlobCache cache = CacheManager.getCache(this.mContext, BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES, BOOKMARK_CACHE_MAX_BYTES, 1);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeUTF(uri.toString());
            dataOutputStream.writeInt(i);
            dataOutputStream.writeInt(Math.abs(i2));
            dataOutputStream.flush();
            cache.insert(uri.hashCode(), byteArrayOutputStream.toByteArray());
        } catch (Throwable th) {
            Log.w(TAG, "setBookmark failed", th);
        }
    }

    public BookmarkerInfo getBookmark(Uri uri) {
        try {
            byte[] bArrLookup = CacheManager.getCache(this.mContext, BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES, BOOKMARK_CACHE_MAX_BYTES, 1).lookup(uri.hashCode());
            if (bArrLookup == null) {
                Log.v(TAG, "getBookmark(" + uri + ") data=null. uri.hashCode()=" + uri.hashCode());
                return null;
            }
            DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArrLookup));
            String utf = DataInputStream.readUTF(dataInputStream);
            int i = dataInputStream.readInt();
            int i2 = dataInputStream.readInt();
            Log.v(TAG, "getBookmark(" + uri + ") uriString=" + utf + ", bookmark=" + i + ", duration=" + i2);
            if (utf.equals(uri.toString()) && i >= HALF_MINUTE && i2 >= TWO_MINUTES && i <= i2 - 30000) {
                return new BookmarkerInfo(i, i2);
            }
            return null;
        } catch (Throwable th) {
            Log.w(TAG, "getBookmark failed", th);
            return null;
        }
    }

    public class BookmarkerInfo {
        public final int mBookmark;
        public final int mDuration;

        public BookmarkerInfo(int i, int i2) {
            this.mBookmark = i;
            this.mDuration = i2;
        }

        public String toString() {
            return "BookmarkInfo(bookmark=" + this.mBookmark + ", duration=" + this.mDuration + ")";
        }
    }
}
