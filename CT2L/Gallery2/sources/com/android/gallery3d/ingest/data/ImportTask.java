package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.mtp.MtpDevice;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.util.Log;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

    public ImportTask(MtpDevice device, Collection<IngestObjectInfo> objectsToImport, String destAlbumName, Context context) {
        this.mDestAlbumName = destAlbumName;
        this.mObjectsToImport = objectsToImport;
        this.mDevice = device;
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(6, "Google Photos MTP Import Task");
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void run() {
        this.mWakeLock.acquire();
        try {
            List<IngestObjectInfo> objectsNotImported = new LinkedList<>();
            int visited = 0;
            int total = this.mObjectsToImport.size();
            this.mListener.onImportProgress(0, total, null);
            File dest = new File(Environment.getExternalStorageDirectory(), this.mDestAlbumName);
            dest.mkdirs();
            for (IngestObjectInfo object : this.mObjectsToImport) {
                visited++;
                String importedPath = null;
                if (hasSpaceForSize(object.getCompressedSize())) {
                    importedPath = new File(dest, object.getName(this.mDevice)).getAbsolutePath();
                    if (!this.mDevice.importFile(object.getObjectHandle(), importedPath)) {
                        importedPath = null;
                    }
                }
                if (importedPath == null) {
                    objectsNotImported.add(object);
                }
                if (this.mListener != null) {
                    this.mListener.onImportProgress(visited, total, importedPath);
                }
            }
            if (this.mListener != null) {
                this.mListener.onImportFinish(objectsNotImported, visited);
            }
        } finally {
            this.mListener = null;
            this.mWakeLock.release();
        }
    }

    private static boolean hasSpaceForSize(long size) {
        String state = Environment.getExternalStorageState();
        if (!"mounted".equals(state)) {
            return false;
        }
        String path = Environment.getExternalStorageDirectory().getPath();
        try {
            StatFs stat = new StatFs(path);
            return ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize()) > size;
        } catch (Exception e) {
            Log.i("ImportTask", "Fail to access external storage", e);
            return false;
        }
    }
}
