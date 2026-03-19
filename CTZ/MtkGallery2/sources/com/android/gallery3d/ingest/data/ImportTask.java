package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.mtp.MtpDevice;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import com.mediatek.gallery3d.util.Log;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

@TargetApi(12)
public class ImportTask implements Runnable {
    private String mDestAlbumName;
    private MtpDevice mDevice;
    private Listener mListener;
    private Collection<IngestObjectInfo> mObjectsToImport;
    private PowerManager.WakeLock mWakeLock;

    public interface Listener {
        void onImportFinish(Collection<IngestObjectInfo> collection, int i);

        void onImportProgress(int i, int i2, String str);
    }

    public ImportTask(MtpDevice mtpDevice, Collection<IngestObjectInfo> collection, String str, Context context) {
        this.mDestAlbumName = str;
        this.mObjectsToImport = collection;
        this.mDevice = mtpDevice;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(6, "Google Photos MTP Import Task");
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void run() {
        String absolutePath;
        this.mWakeLock.acquire();
        try {
            LinkedList linkedList = new LinkedList();
            int i = 0;
            int size = this.mObjectsToImport.size();
            this.mListener.onImportProgress(0, size, null);
            File file = new File(Environment.getExternalStorageDirectory(), this.mDestAlbumName);
            file.mkdirs();
            for (IngestObjectInfo ingestObjectInfo : this.mObjectsToImport) {
                i++;
                if (ingestObjectInfo != null && hasSpaceForSize(ingestObjectInfo.getCompressedSize())) {
                    try {
                        absolutePath = new File(file, ingestObjectInfo.getName(this.mDevice)).getAbsolutePath();
                    } catch (NullPointerException e) {
                        Log.w("Gallery2/ImportTask", "can not get name fom mtp device, catch null pointer exception, " + e);
                        absolutePath = null;
                    }
                    if (absolutePath != null && !this.mDevice.importFile(ingestObjectInfo.getObjectHandle(), absolutePath)) {
                    }
                } else {
                    absolutePath = null;
                }
                if (absolutePath == null) {
                    linkedList.add(ingestObjectInfo);
                }
                if (this.mListener != null) {
                    this.mListener.onImportProgress(i, size, absolutePath);
                }
            }
            if (this.mListener != null) {
                this.mListener.onImportFinish(linkedList, i);
            }
        } finally {
            this.mListener = null;
            this.mWakeLock.release();
        }
    }

    private static boolean hasSpaceForSize(long j) {
        if (!"mounted".equals(Environment.getExternalStorageState())) {
            return false;
        }
        try {
            StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return ((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize()) > j;
        } catch (Exception e) {
            Log.i("Gallery2/ImportTask", "Fail to access external storage", e);
            return false;
        }
    }
}
