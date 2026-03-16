package com.android.gallery3d.app;

import android.content.Context;
import android.net.Uri;
import com.android.gallery3d.common.BlobCache;
import com.android.gallery3d.util.CacheManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

class Bookmarker {
    private final Context mContext;

    public Bookmarker(Context context) {
        this.mContext = context;
    }

    public void setBookmark(Uri uri, int bookmark, int duration) {
        try {
            BlobCache cache = CacheManager.getCache(this.mContext, "bookmark", 100, 10240, 1);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(uri.toString());
            dos.writeInt(bookmark);
            dos.writeInt(duration);
            dos.flush();
            cache.insert(uri.hashCode(), bos.toByteArray());
        } catch (Throwable t) {
            Log.w("Bookmarker", "setBookmark failed", t);
        }
    }

    public Integer getBookmark(Uri uri) {
        try {
            BlobCache cache = CacheManager.getCache(this.mContext, "bookmark", 100, 10240, 1);
            byte[] data = cache.lookup(uri.hashCode());
            if (data == null) {
                return null;
            }
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            String uriString = DataInputStream.readUTF(dis);
            int bookmark = dis.readInt();
            int duration = dis.readInt();
            if (!uriString.equals(uri.toString()) || bookmark < 30000 || duration < 120000 || bookmark > duration - 30000) {
                return null;
            }
            return Integer.valueOf(bookmark);
        } catch (Throwable t) {
            Log.w("Bookmarker", "getBookmark failed", t);
            return null;
        }
    }
}
