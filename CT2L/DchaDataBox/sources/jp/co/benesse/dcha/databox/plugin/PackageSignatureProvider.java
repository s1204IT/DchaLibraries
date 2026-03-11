package jp.co.benesse.dcha.databox.plugin;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import java.io.FileNotFoundException;
import java.io.IOException;
import jp.co.benesse.dcha.util.Logger;

public class PackageSignatureProvider extends ContentProvider {
    private static final String TAG = PackageSignatureProvider.class.getSimpleName();

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        return null;
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        AssetManager manager = getContext().getResources().getAssets();
        try {
            String[] fileList = manager.list("certs");
            String dstFileName = uri.getLastPathSegment();
            Logger.d(TAG, "openAssetFile dstFileName:" + dstFileName);
            for (String assetFile : fileList) {
                Logger.d(TAG, "openAssetFile assetFile:" + assetFile);
                if (assetFile.startsWith(dstFileName)) {
                    AssetFileDescriptor fd = manager.openFd("certs/" + assetFile);
                    return fd;
                }
            }
            return null;
        } catch (IOException e) {
            Logger.e(TAG, "openAssetFile", e);
            return null;
        }
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
