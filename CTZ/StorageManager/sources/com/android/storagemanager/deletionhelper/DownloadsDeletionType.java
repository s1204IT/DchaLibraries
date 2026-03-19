package com.android.storagemanager.deletionhelper;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.ArraySet;
import com.android.internal.logging.MetricsLogger;
import com.android.storagemanager.deletionhelper.DeletionType;
import com.android.storagemanager.deletionhelper.FetchDownloadsLoader;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class DownloadsDeletionType implements LoaderManager.LoaderCallbacks<FetchDownloadsLoader.DownloadsResult>, DeletionType {
    private long mBytes;
    private Context mContext;
    private DeletionType.FreeableChangedListener mListener;
    private long mMostRecent;
    private HashMap<File, Bitmap> mThumbnails;
    private int mLoadingStatus = 0;
    private ArraySet<File> mFiles = new ArraySet<>();
    private ArraySet<String> mUncheckedFiles = new ArraySet<>();

    public DownloadsDeletionType(Context context, String[] strArr) {
        this.mContext = context;
        if (strArr != null) {
            Collections.addAll(this.mUncheckedFiles, strArr);
        }
    }

    @Override
    public void registerFreeableChangedListener(DeletionType.FreeableChangedListener freeableChangedListener) {
        this.mListener = freeableChangedListener;
        if (this.mFiles != null) {
            maybeUpdateListener();
        }
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onSaveInstanceStateBundle(Bundle bundle) {
        bundle.putStringArray("uncheckedFiles", (String[]) this.mUncheckedFiles.toArray(new String[this.mUncheckedFiles.size()]));
    }

    @Override
    public void clearFreeableData(final Activity activity) {
        if (this.mFiles != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    boolean z = true;
                    for (File file : DownloadsDeletionType.this.mFiles) {
                        if (DownloadsDeletionType.this.isChecked(file)) {
                            if (!z || !file.delete()) {
                                z = false;
                            } else {
                                z = true;
                            }
                        }
                    }
                    if (!z) {
                        MetricsLogger.action(activity, 472);
                    }
                }
            });
        }
    }

    @Override
    public int getLoadingStatus() {
        return this.mLoadingStatus;
    }

    @Override
    public int getContentCount() {
        return this.mFiles.size();
    }

    @Override
    public void setLoadingStatus(int i) {
        this.mLoadingStatus = i;
    }

    @Override
    public Loader<FetchDownloadsLoader.DownloadsResult> onCreateLoader(int i, Bundle bundle) {
        return new FetchDownloadsLoader(this.mContext, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
    }

    @Override
    public void onLoadFinished(Loader<FetchDownloadsLoader.DownloadsResult> loader, FetchDownloadsLoader.DownloadsResult downloadsResult) {
        this.mMostRecent = downloadsResult.youngestLastModified;
        Iterator<File> it = downloadsResult.files.iterator();
        while (it.hasNext()) {
            this.mFiles.add(it.next());
        }
        this.mBytes = downloadsResult.totalSize;
        this.mThumbnails = downloadsResult.thumbnails;
        updateLoadingStatus();
        maybeUpdateListener();
    }

    @Override
    public void onLoaderReset(Loader<FetchDownloadsLoader.DownloadsResult> loader) {
    }

    public long getMostRecentLastModified() {
        return this.mMostRecent;
    }

    public Set<File> getFiles() {
        if (this.mFiles == null) {
            return null;
        }
        return this.mFiles;
    }

    public void setFileChecked(File file, boolean z) {
        if (z) {
            this.mUncheckedFiles.remove(file.getPath());
        } else {
            this.mUncheckedFiles.add(file.getPath());
        }
    }

    public long getFreeableBytes(boolean z) {
        long length = 0;
        for (File file : this.mFiles) {
            if (isChecked(file) || z) {
                length += file.length();
            }
        }
        return length;
    }

    public Bitmap getCachedThumbnail(File file) {
        if (this.mThumbnails == null) {
            return null;
        }
        return this.mThumbnails.get(file);
    }

    public boolean isChecked(File file) {
        return !this.mUncheckedFiles.contains(file.getPath());
    }

    private void maybeUpdateListener() {
        if (this.mListener != null) {
            this.mListener.onFreeableChanged(this.mFiles.size(), this.mBytes);
        }
    }
}
