package com.android.documentsui.base;

import android.R;
import android.content.ContentProviderClient;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;
import com.android.documentsui.ScopedAccessMetrics;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public final class SharedMinimal {
    public static final boolean DEBUG = Log.isLoggable("Documents", 2);
    public static final boolean VERBOSE;

    public interface GetUriPermissionCallback {
        boolean onResult(File file, String str, boolean z, boolean z2, Uri uri, Uri uri2);
    }

    static {
        VERBOSE = DEBUG && Log.isLoggable("Documents", 2);
    }

    public static String getInternalDirectoryName(String str) {
        return str == null ? "ROOT_DIRECTORY" : str;
    }

    public static String getExternalDirectoryName(String str) {
        if (str.equals("ROOT_DIRECTORY")) {
            return null;
        }
        return str;
    }

    public static boolean getUriPermission(Context context, ContentProviderClient contentProviderClient, StorageVolume storageVolume, String str, int i, boolean z, GetUriPermissionCallback getUriPermissionCallback) {
        File canonicalFile;
        String absolutePath;
        String str2;
        File internalPathForUser;
        File file;
        String bestVolumeDescription;
        Uri uriPermission;
        if (DEBUG) {
            Log.d("Documents", "getUriPermission() for volume " + storageVolume.dump() + ", directory " + str + ", and user " + i);
        }
        boolean zEquals = str.equals("ROOT_DIRECTORY");
        boolean zIsPrimary = storageVolume.isPrimary();
        if (zEquals && zIsPrimary) {
            if (DEBUG) {
                Log.d("Documents", "root access requested on primary volume");
            }
            return false;
        }
        File pathFile = storageVolume.getPathFile();
        if (!zEquals) {
            try {
                canonicalFile = new File(pathFile, str).getCanonicalFile();
            } catch (IOException e) {
                Log.e("Documents", "Could not get canonical file for volume " + storageVolume.dump() + " and directory " + str);
                if (z) {
                    ScopedAccessMetrics.logInvalidScopedAccessRequest(context, "docsui_scoped_directory_access_error");
                }
                return false;
            }
        } else {
            canonicalFile = pathFile;
        }
        StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
        if (zEquals) {
            absolutePath = pathFile.getAbsolutePath();
            str2 = ".";
        } else {
            String parent = canonicalFile.getParent();
            String name = canonicalFile.getName();
            if (TextUtils.isEmpty(name) || !Environment.isStandardDirectory(name)) {
                if (DEBUG) {
                    Log.d("Documents", "Directory '" + name + "' is not standard (full path: '" + canonicalFile.getAbsolutePath() + "')");
                }
                if (z) {
                    ScopedAccessMetrics.logInvalidScopedAccessRequest(context, "docsui_scoped_directory_access_invalid_dir");
                }
                return false;
            }
            absolutePath = parent;
            str2 = name;
        }
        List volumes = storageManager.getVolumes();
        if (DEBUG) {
            Log.d("Documents", "Number of volumes: " + volumes.size());
        }
        Iterator it = volumes.iterator();
        while (true) {
            internalPathForUser = null;
            if (it.hasNext()) {
                VolumeInfo volumeInfo = (VolumeInfo) it.next();
                if (isRightVolume(volumeInfo, absolutePath, i)) {
                    internalPathForUser = volumeInfo.getInternalPathForUser(i);
                    if (DEBUG) {
                        Log.d("Documents", "Converting " + absolutePath + " to " + internalPathForUser);
                    }
                    if (!zEquals) {
                        file = new File(internalPathForUser, str2);
                    } else {
                        file = internalPathForUser;
                    }
                    bestVolumeDescription = storageManager.getBestVolumeDescription(volumeInfo);
                    if (TextUtils.isEmpty(bestVolumeDescription)) {
                        bestVolumeDescription = storageVolume.getDescription(context);
                    }
                    if (TextUtils.isEmpty(bestVolumeDescription)) {
                        bestVolumeDescription = context.getString(R.string.unknownName);
                        Log.w("Documents", "No volume description  for " + volumeInfo + "; using " + bestVolumeDescription);
                    }
                }
            } else {
                file = canonicalFile;
                bestVolumeDescription = null;
                break;
            }
        }
        if (internalPathForUser == null) {
            Log.e("Documents", "Didn't find right volume for '" + storageVolume.dump() + "' on " + volumes);
            return false;
        }
        Uri uriPermission2 = getUriPermission(context, contentProviderClient, file);
        if (!internalPathForUser.equals(file)) {
            uriPermission = getUriPermission(context, contentProviderClient, internalPathForUser);
        } else {
            uriPermission = uriPermission2;
        }
        return getUriPermissionCallback.onResult(file, bestVolumeDescription, zEquals, zIsPrimary, uriPermission2, uriPermission);
    }

    public static Uri getUriPermission(Context context, ContentProviderClient contentProviderClient, File file) {
        String string;
        try {
            Bundle bundleCall = contentProviderClient.call("getDocIdForFileCreateNewDir", file.getPath(), null);
            if (bundleCall != null) {
                string = bundleCall.getString("DOC_ID");
            } else {
                string = null;
            }
            if (string == null) {
                Log.e("Documents", "Did not get doc id from External Storage provider for " + file);
                ScopedAccessMetrics.logInvalidScopedAccessRequest(context, "docsui_scoped_directory_access_error");
                return null;
            }
            if (DEBUG) {
                Log.d("Documents", "doc id for " + file + ": " + string);
            }
            Uri uriBuildTreeDocumentUri = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", string);
            if (uriBuildTreeDocumentUri == null) {
                Log.e("Documents", "Could not get URI for doc id " + string);
                return null;
            }
            if (DEBUG) {
                Log.d("Documents", "URI for " + file + ": " + uriBuildTreeDocumentUri);
            }
            return uriBuildTreeDocumentUri;
        } catch (RemoteException e) {
            Log.e("Documents", "Did not get doc id from External Storage provider for " + file, e);
            ScopedAccessMetrics.logInvalidScopedAccessRequest(context, "docsui_scoped_directory_access_error");
            return null;
        }
    }

    private static boolean isRightVolume(VolumeInfo volumeInfo, String str, int i) {
        File pathForUser = volumeInfo.getPathForUser(i);
        String path = pathForUser == null ? null : volumeInfo.getPathForUser(i).getPath();
        boolean zIsMountedReadable = volumeInfo.isMountedReadable();
        if (DEBUG) {
            Log.d("Documents", "Volume: " + volumeInfo + "\n\tuserId: " + i + "\n\tuserPath: " + pathForUser + "\n\troot: " + str + "\n\tpath: " + path + "\n\tisMounted: " + zIsMountedReadable);
        }
        return zIsMountedReadable && str.equals(path);
    }
}
