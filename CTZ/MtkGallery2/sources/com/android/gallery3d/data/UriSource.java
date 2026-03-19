package com.android.gallery3d.data;

import android.net.Uri;
import android.webkit.MimeTypeMap;
import com.android.gallery3d.app.GalleryApp;
import com.mediatek.omadrm.OmaDrmStore;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

class UriSource extends MediaSource {
    private GalleryApp mApplication;

    public UriSource(GalleryApp galleryApp) {
        super("uri");
        this.mApplication = galleryApp;
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        String[] strArrSplit = path.split();
        if (strArrSplit.length != 3) {
            throw new RuntimeException("bad path: " + path);
        }
        try {
            return new UriImage(this.mApplication, path, Uri.parse(URLDecoder.decode(strArrSplit[1], "utf-8")), URLDecoder.decode(strArrSplit[2], "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    private String getMimeType(Uri uri) {
        if ("file".equals(uri.getScheme())) {
            String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()).toLowerCase());
            if (mimeTypeFromExtension != null) {
                return mimeTypeFromExtension;
            }
        }
        String type = this.mApplication.getContentResolver().getType(uri);
        return type == null ? "image/*" : type;
    }

    @Override
    public Path findPathByUri(Uri uri, String str) {
        String mimeType = getMimeType(uri);
        if (str == null || ("image/*".equals(str) && mimeType.startsWith(OmaDrmStore.MimePrefix.IMAGE))) {
            str = mimeType;
        }
        if (str.startsWith(OmaDrmStore.MimePrefix.IMAGE)) {
            try {
                return Path.fromString("/uri/" + URLEncoder.encode(uri.toString(), "utf-8") + "/" + URLEncoder.encode(str, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
        }
        return null;
    }
}
