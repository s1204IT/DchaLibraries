package jp.co.benesse.dcha.databox.plugin;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import java.io.IOException;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class PackageSignatureProvider extends ContentProvider {
    private static final String TAG = PackageSignatureProvider.class.getSimpleName();

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return null;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        return false;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return null;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public AssetFileDescriptor openAssetFile(Uri uri, String str) throws IOException {
        try {
            AssetManager assets = getContext().getResources().getAssets();
            String[] list = assets.list("certs");
            String lastPathSegment = uri.getLastPathSegment();
            Logger.d(TAG, "openAssetFile dstFileName:" + lastPathSegment);
            for (String str2 : list) {
                Logger.d(TAG, "openAssetFile assetFile:" + str2);
                if (str2.startsWith(lastPathSegment)) {
                    return assets.openFd("certs/" + str2);
                }
            }
            return null;
        } catch (Exception e) {
            Logger.e(TAG, "openAssetFile", e);
            return null;
        }
    }
}
