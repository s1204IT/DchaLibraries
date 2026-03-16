package com.android.contacts.common.vcard;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.contacts.R;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class VCardService extends Service {
    private MyBinder mBinder;
    private String mCallingActivity;
    private int mCurrentJobId;
    private String mErrorReason;
    private Set<String> mExtensionsToConsider;
    private int mFileIndexMaximum;
    private int mFileIndexMinimum;
    private String mFileNameExtension;
    private String mFileNamePrefix;
    private String mFileNameSuffix;
    private File mTargetDirectory;
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private final SparseArray<ProcessorBase> mRunningJobMap = new SparseArray<>();
    private final List<CustomMediaScannerConnectionClient> mRemainingScannerConnections = new ArrayList();
    private final Set<String> mReservedDestination = new HashSet();

    private class CustomMediaScannerConnectionClient implements MediaScannerConnection.MediaScannerConnectionClient {
        final MediaScannerConnection mConnection;
        final String mPath;

        public CustomMediaScannerConnectionClient(String path) {
            this.mConnection = new MediaScannerConnection(VCardService.this, this);
            this.mPath = path;
        }

        public void start() {
            this.mConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            this.mConnection.scanFile(this.mPath, null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            this.mConnection.disconnect();
            VCardService.this.removeConnectionClient(this);
        }
    }

    public class MyBinder extends Binder {
        public MyBinder() {
        }

        public VCardService getService() {
            return VCardService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBinder = new MyBinder();
        initExporterParams();
    }

    private void initExporterParams() {
        this.mTargetDirectory = Environment.getExternalStorageDirectory();
        this.mFileNamePrefix = getString(R.string.config_export_file_prefix);
        this.mFileNameSuffix = getString(R.string.config_export_file_suffix);
        this.mFileNameExtension = getString(R.string.config_export_file_extension);
        this.mExtensionsToConsider = new HashSet();
        this.mExtensionsToConsider.add(this.mFileNameExtension);
        String additionalExtensions = getString(R.string.config_export_extensions_to_consider);
        if (!TextUtils.isEmpty(additionalExtensions)) {
            String[] arr$ = additionalExtensions.split(",");
            for (String extension : arr$) {
                String trimed = extension.trim();
                if (trimed.length() > 0) {
                    this.mExtensionsToConsider.add(trimed);
                }
            }
        }
        Resources resources = getResources();
        this.mFileIndexMinimum = resources.getInteger(R.integer.config_export_file_min_index);
        this.mFileIndexMaximum = resources.getInteger(R.integer.config_export_file_max_index);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && intent.getExtras() != null) {
            this.mCallingActivity = intent.getExtras().getString("CALLING_ACTIVITY");
            return 1;
        }
        this.mCallingActivity = null;
        return 1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    @Override
    public void onDestroy() {
        cancelAllRequestsAndShutdown();
        clearCache();
        super.onDestroy();
    }

    public synchronized void handleImportRequest(List<ImportRequest> requests, VCardImportExportListener listener) {
        int size = requests.size();
        int i = 0;
        while (true) {
            if (i >= size) {
                break;
            }
            ImportRequest request = requests.get(i);
            if (!tryExecute(new ImportProcessor(this, listener, request, this.mCurrentJobId))) {
                break;
            }
            if (listener != null) {
                listener.onImportProcessed(request, this.mCurrentJobId, i);
            }
            this.mCurrentJobId++;
            i++;
        }
    }

    public synchronized void handleExportRequest(ExportRequest request, VCardImportExportListener listener) {
        if (tryExecute(new ExportProcessor(this, request, this.mCurrentJobId, this.mCallingActivity))) {
            String path = request.destUri.getEncodedPath();
            if (!this.mReservedDestination.add(path)) {
                Log.w("VCardService", String.format("The path %s is already reserved. Reject export request", path));
                if (listener != null) {
                    listener.onExportFailed(request);
                }
            } else {
                if (listener != null) {
                    listener.onExportProcessed(request, this.mCurrentJobId);
                }
                this.mCurrentJobId++;
            }
        } else if (listener != null) {
            listener.onExportFailed(request);
        }
    }

    private synchronized boolean tryExecute(ProcessorBase processor) {
        boolean z;
        try {
            this.mExecutorService.execute(processor);
            this.mRunningJobMap.put(this.mCurrentJobId, processor);
            z = true;
        } catch (RejectedExecutionException e) {
            Log.w("VCardService", "Failed to excetute a job.", e);
            z = false;
        }
        return z;
    }

    public synchronized void handleCancelRequest(CancelRequest request, VCardImportExportListener listener) {
        int jobId = request.jobId;
        ProcessorBase processor = this.mRunningJobMap.get(jobId);
        this.mRunningJobMap.remove(jobId);
        if (processor != null) {
            processor.cancel(true);
            int type = processor.getType();
            if (listener != null) {
                listener.onCancelRequest(request, type);
            }
            if (type == 2) {
                String path = ((ExportProcessor) processor).getRequest().destUri.getEncodedPath();
                Log.i("VCardService", String.format("Cancel reservation for the path %s if appropriate", path));
                if (!this.mReservedDestination.remove(path)) {
                    Log.w("VCardService", "Not reserved.");
                }
            }
        } else {
            Log.w("VCardService", String.format("Tried to remove unknown job (id: %d)", Integer.valueOf(jobId)));
        }
        stopServiceIfAppropriate();
    }

    public synchronized void handleRequestAvailableExportDestination(Messenger messenger) {
        Message message;
        String path = getAppropriateDestination(this.mTargetDirectory);
        if (path != null) {
            message = Message.obtain(null, 5, 0, 0, path);
        } else {
            message = Message.obtain(null, 5, R.id.dialog_fail_to_export_with_reason, 0, this.mErrorReason);
        }
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            Log.w("VCardService", "Failed to send reply for available export destination request.", e);
        }
    }

    private synchronized void stopServiceIfAppropriate() {
        if (this.mRunningJobMap.size() > 0) {
            int size = this.mRunningJobMap.size();
            int[] toBeRemoved = new int[size];
            for (int i = 0; i < size; i++) {
                int jobId = this.mRunningJobMap.keyAt(i);
                ProcessorBase processor = this.mRunningJobMap.valueAt(i);
                if (!processor.isDone()) {
                    Log.i("VCardService", String.format("Found unfinished job (id: %d)", Integer.valueOf(jobId)));
                    for (int j = 0; j < i; j++) {
                        this.mRunningJobMap.remove(toBeRemoved[j]);
                    }
                } else {
                    toBeRemoved[i] = jobId;
                }
            }
            this.mRunningJobMap.clear();
            if (this.mRemainingScannerConnections.isEmpty()) {
                Log.i("VCardService", "MediaScanner update is in progress.");
            } else {
                Log.i("VCardService", "No unfinished job. Stop this service.");
                this.mExecutorService.shutdown();
                stopSelf();
            }
        } else if (this.mRemainingScannerConnections.isEmpty()) {
        }
    }

    synchronized void updateMediaScanner(String path) {
        if (this.mExecutorService.isShutdown()) {
            Log.w("VCardService", "MediaScanner update is requested after executor's being shut down. Ignoring the update request");
        } else {
            CustomMediaScannerConnectionClient client = new CustomMediaScannerConnectionClient(path);
            this.mRemainingScannerConnections.add(client);
            client.start();
        }
    }

    private synchronized void removeConnectionClient(CustomMediaScannerConnectionClient client) {
        this.mRemainingScannerConnections.remove(client);
        stopServiceIfAppropriate();
    }

    synchronized void handleFinishImportNotification(int jobId, boolean successful) {
        this.mRunningJobMap.remove(jobId);
        stopServiceIfAppropriate();
    }

    synchronized void handleFinishExportNotification(int jobId, boolean successful) {
        ProcessorBase job = this.mRunningJobMap.get(jobId);
        this.mRunningJobMap.remove(jobId);
        if (job == null) {
            Log.w("VCardService", String.format("Tried to remove unknown job (id: %d)", Integer.valueOf(jobId)));
        } else if (!(job instanceof ExportProcessor)) {
            Log.w("VCardService", String.format("Removed job (id: %s) isn't ExportProcessor", Integer.valueOf(jobId)));
        } else {
            String path = ((ExportProcessor) job).getRequest().destUri.getEncodedPath();
            this.mReservedDestination.remove(path);
        }
        stopServiceIfAppropriate();
    }

    private synchronized void cancelAllRequestsAndShutdown() {
        for (int i = 0; i < this.mRunningJobMap.size(); i++) {
            this.mRunningJobMap.valueAt(i).cancel(true);
        }
        this.mRunningJobMap.clear();
        this.mExecutorService.shutdown();
    }

    private void clearCache() {
        String[] arr$ = fileList();
        for (String fileName : arr$) {
            if (fileName.startsWith("import_tmp_")) {
                Log.i("VCardService", "Remove a temporary file: " + fileName);
                deleteFile(fileName);
            }
        }
    }

    private String getAppropriateDestination(File destDirectory) {
        int fileIndexDigit = 0;
        for (int tmp = this.mFileIndexMaximum; tmp > 0; tmp /= 10) {
            fileIndexDigit++;
        }
        String bodyFormat = "%s%0" + fileIndexDigit + "d%s";
        String possibleBody = String.format(Locale.US, bodyFormat, this.mFileNamePrefix, 1, this.mFileNameSuffix);
        if (possibleBody.length() > 8 || this.mFileNameExtension.length() > 3) {
            Log.e("VCardService", "This code does not allow any long file name.");
            this.mErrorReason = getString(R.string.fail_reason_too_long_filename, new Object[]{String.format("%s.%s", possibleBody, this.mFileNameExtension)});
            Log.w("VCardService", "File name becomes too long.");
            return null;
        }
        for (int i = this.mFileIndexMinimum; i <= this.mFileIndexMaximum; i++) {
            boolean numberIsAvailable = true;
            String body = String.format(Locale.US, bodyFormat, this.mFileNamePrefix, Integer.valueOf(i), this.mFileNameSuffix);
            Iterator<String> it = this.mExtensionsToConsider.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                String possibleExtension = it.next();
                File file = new File(destDirectory, body + "." + possibleExtension);
                String path = file.getAbsolutePath();
                synchronized (this) {
                    if (this.mReservedDestination.contains(path)) {
                        numberIsAvailable = false;
                        break;
                    }
                    if (file.exists()) {
                        numberIsAvailable = false;
                        break;
                    }
                }
            }
            if (numberIsAvailable) {
                return new File(destDirectory, body + "." + this.mFileNameExtension).getAbsolutePath();
            }
        }
        Log.w("VCardService", "Reached vCard number limit. Maybe there are too many vCard in the storage");
        this.mErrorReason = getString(R.string.fail_reason_too_many_vcard);
        return null;
    }
}
