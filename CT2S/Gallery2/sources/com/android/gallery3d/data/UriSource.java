package com.android.gallery3d.data;

import android.net.Uri;
import android.webkit.MimeTypeMap;
import com.android.gallery3d.app.GalleryApp;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

class UriSource extends MediaSource {
    private GalleryApp mApplication;

    public UriSource(GalleryApp context) {
        super("uri");
        this.mApplication = context;
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        String[] segment = path.split();
        if (segment.length != 3) {
            throw new RuntimeException("bad path: " + path);
        }
        try {
            String uri = URLDecoder.decode(segment[1], "utf-8");
            String type = URLDecoder.decode(segment[2], "utf-8");
            return new UriImage(this.mApplication, path, Uri.parse(uri), type);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    private String getMimeType(Uri uri) {
        if ("file".equals(uri.getScheme())) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (type != null) {
                return type;
            }
        }
        String type2 = this.mApplication.getContentResolver().getType(uri);
        return type2 == null ? "image/*" : type2;
    }

    @Override
    public Path findPathByUri(Uri uri, String type) {
        String mimeType = getMimeType(uri);
        if (type == null || ("image/*".equals(type) && mimeType.startsWith("image/"))) {
            type = mimeType;
        }
        if (type.startsWith("image/")) {
            try {
                return Path.fromString("/uri/" + URLEncoder.encode(uri.toString(), "utf-8") + "/" + URLEncoder.encode(type, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
        }
        return null;
    }
}
