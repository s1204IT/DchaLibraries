package com.android.gallery3d.ingest;

import android.R;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.mtp.MtpDevice;
import android.mtp.MtpDeviceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.SparseBooleanArray;
import android.widget.Adapter;
import com.android.gallery3d.ingest.data.ImportTask;
import com.android.gallery3d.ingest.data.IngestObjectInfo;
import com.android.gallery3d.ingest.data.MtpClient;
import com.android.gallery3d.ingest.data.MtpDeviceIndex;
import com.mediatek.gallery3d.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import mf.org.apache.xerces.impl.xpath.XPath;

@TargetApi(12)
public class IngestService extends Service implements ImportTask.Listener, MtpClient.Listener, MtpDeviceIndex.ProgressListener {
    private MtpClient mClient;
    private IngestActivity mClientActivity;
    private MtpDevice mDevice;
    private String mDevicePrettyName;
    private MtpDeviceIndex mIndex;
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private Collection<IngestObjectInfo> mRedeliverObjectsNotImported;
    private ScannerClient mScannerClient;
    private final IBinder mBinder = new LocalBinder();
    private boolean mRedeliverImportFinish = false;
    private int mRedeliverImportFinishCount = 0;
    private boolean mRedeliverNotifyIndexChanged = false;
    private boolean mRedeliverIndexFinish = false;
    private long mLastProgressIndexTime = 0;
    private boolean mNeedRelaunchNotification = false;

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        IngestService getService() {
            return IngestService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mScannerClient = new ScannerClient(this);
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
        this.mNotificationBuilder = new NotificationCompat.Builder(this);
        this.mNotificationBuilder.setSmallIcon(R.drawable.stat_notify_sync).setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, (Class<?>) IngestActivity.class), 0));
        createChannel(this.mNotificationBuilder);
        this.mIndex = MtpDeviceIndex.getInstance();
        this.mIndex.setProgressListener(this);
        this.mClient = new MtpClient(getApplicationContext());
        List<MtpDevice> deviceList = this.mClient.getDeviceList();
        if (!deviceList.isEmpty()) {
            setDevice(deviceList.get(0));
        }
        this.mClient.addListener(this);
    }

    @Override
    public void onDestroy() {
        this.mClient.close();
        this.mIndex.unsetProgressListener(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    private void setDevice(MtpDevice mtpDevice) {
        if (this.mDevice == mtpDevice) {
            return;
        }
        this.mRedeliverImportFinish = false;
        this.mRedeliverObjectsNotImported = null;
        this.mRedeliverNotifyIndexChanged = false;
        this.mRedeliverIndexFinish = false;
        this.mDevice = mtpDevice;
        this.mIndex.setDevice(this.mDevice);
        if (this.mDevice != null) {
            MtpDeviceInfo deviceInfo = this.mDevice.getDeviceInfo();
            if (deviceInfo == null) {
                setDevice(null);
                return;
            } else {
                this.mDevicePrettyName = deviceInfo.getModel();
                this.mNotificationBuilder.setContentTitle(this.mDevicePrettyName);
                new Thread(this.mIndex.getIndexRunnable()).start();
            }
        } else {
            this.mDevicePrettyName = null;
        }
        if (this.mClientActivity != null) {
            this.mClientActivity.notifyIndexChanged();
        } else {
            this.mRedeliverNotifyIndexChanged = true;
        }
    }

    protected MtpDeviceIndex getIndex() {
        return this.mIndex;
    }

    protected void setClientActivity(IngestActivity ingestActivity) {
        if (this.mClientActivity == ingestActivity) {
            return;
        }
        this.mClientActivity = ingestActivity;
        if (this.mClientActivity == null) {
            if (this.mNeedRelaunchNotification) {
                this.mNotificationBuilder.setProgress(0, 0, false).setContentText(getResources().getText(com.android.gallery3d.R.string.ingest_scanning_done));
                this.mNotificationManager.notify(com.android.gallery3d.R.id.ingest_notification_scanning, this.mNotificationBuilder.build());
                return;
            }
            return;
        }
        this.mNotificationManager.cancel(com.android.gallery3d.R.id.ingest_notification_importing);
        this.mNotificationManager.cancel(com.android.gallery3d.R.id.ingest_notification_scanning);
        if (this.mRedeliverImportFinish) {
            this.mClientActivity.onImportFinish(this.mRedeliverObjectsNotImported, this.mRedeliverImportFinishCount);
            this.mRedeliverImportFinish = false;
            this.mRedeliverObjectsNotImported = null;
        }
        if (this.mRedeliverNotifyIndexChanged) {
            this.mClientActivity.notifyIndexChanged();
            this.mRedeliverNotifyIndexChanged = false;
        }
        if (this.mRedeliverIndexFinish) {
            this.mClientActivity.onIndexingFinished();
            this.mRedeliverIndexFinish = false;
        }
        if (this.mDevice != null) {
            this.mNeedRelaunchNotification = true;
        }
    }

    protected void importSelectedItems(SparseBooleanArray sparseBooleanArray, Adapter adapter) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < sparseBooleanArray.size(); i++) {
            if (sparseBooleanArray.valueAt(i)) {
                Object item = adapter.getItem(sparseBooleanArray.keyAt(i));
                if (item instanceof IngestObjectInfo) {
                    arrayList.add(item);
                }
            }
        }
        ImportTask importTask = new ImportTask(this.mDevice, arrayList, this.mDevicePrettyName, this);
        importTask.setListener(this);
        this.mNotificationBuilder.setProgress(0, 0, true).setContentText(getResources().getText(com.android.gallery3d.R.string.ingest_importing));
        startForeground(com.android.gallery3d.R.id.ingest_notification_importing, this.mNotificationBuilder.build());
        new Thread(importTask).start();
    }

    @Override
    public void deviceAdded(MtpDevice mtpDevice) {
        if (this.mDevice == null) {
            setDevice(mtpDevice);
        }
    }

    @Override
    public void deviceRemoved(MtpDevice mtpDevice) {
        if (mtpDevice == this.mDevice) {
            this.mNotificationManager.cancel(com.android.gallery3d.R.id.ingest_notification_scanning);
            this.mNotificationManager.cancel(com.android.gallery3d.R.id.ingest_notification_importing);
            setDevice(null);
            this.mNeedRelaunchNotification = false;
        }
    }

    @Override
    public void onImportProgress(int i, int i2, String str) {
        if (str != null) {
            this.mScannerClient.scanPath(str);
        }
        this.mNeedRelaunchNotification = false;
        if (this.mClientActivity != null) {
            this.mClientActivity.onImportProgress(i, i2, str);
        }
        this.mNotificationBuilder.setProgress(i2, i, false).setContentText(getResources().getText(com.android.gallery3d.R.string.ingest_importing));
        this.mNotificationManager.notify(com.android.gallery3d.R.id.ingest_notification_importing, this.mNotificationBuilder.build());
    }

    @Override
    public void onImportFinish(Collection<IngestObjectInfo> collection, int i) {
        stopForeground(true);
        this.mNeedRelaunchNotification = true;
        if (this.mClientActivity != null) {
            this.mClientActivity.onImportFinish(collection, i);
            return;
        }
        this.mRedeliverImportFinish = true;
        this.mRedeliverObjectsNotImported = collection;
        this.mRedeliverImportFinishCount = i;
        this.mNotificationBuilder.setProgress(0, 0, false).setContentText(getResources().getText(com.android.gallery3d.R.string.ingest_import_complete));
        this.mNotificationManager.notify(com.android.gallery3d.R.id.ingest_notification_importing, this.mNotificationBuilder.build());
    }

    @Override
    public void onObjectIndexed(IngestObjectInfo ingestObjectInfo, int i) {
        this.mNeedRelaunchNotification = false;
        if (this.mClientActivity != null) {
            this.mClientActivity.onObjectIndexed(ingestObjectInfo, i);
            return;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (jUptimeMillis > this.mLastProgressIndexTime + 180) {
            this.mLastProgressIndexTime = jUptimeMillis;
            this.mNotificationBuilder.setProgress(0, i, true).setContentText(getResources().getText(com.android.gallery3d.R.string.ingest_scanning));
            this.mNotificationManager.notify(com.android.gallery3d.R.id.ingest_notification_scanning, this.mNotificationBuilder.build());
        }
    }

    @Override
    public void onSortingStarted() {
        if (this.mClientActivity != null) {
            this.mClientActivity.onSortingStarted();
        }
    }

    @Override
    public void onIndexingFinished() {
        this.mNeedRelaunchNotification = true;
        if (this.mClientActivity != null) {
            this.mClientActivity.onIndexingFinished();
            return;
        }
        this.mNotificationBuilder.setProgress(0, 0, false).setContentText(getResources().getText(com.android.gallery3d.R.string.ingest_scanning_done));
        this.mNotificationManager.notify(com.android.gallery3d.R.id.ingest_notification_scanning, this.mNotificationBuilder.build());
        this.mRedeliverIndexFinish = true;
    }

    private static final class ScannerClient implements MediaScannerConnection.MediaScannerConnectionClient {
        boolean mConnected;
        MediaScannerConnection mScannerConnection;
        ArrayList<String> mPaths = new ArrayList<>();
        Object mLock = new Object();

        public ScannerClient(Context context) {
            this.mScannerConnection = new MediaScannerConnection(context, this);
        }

        public void scanPath(String str) {
            synchronized (this.mLock) {
                if (this.mConnected) {
                    this.mScannerConnection.scanFile(str, null);
                } else {
                    this.mPaths.add(str);
                    this.mScannerConnection.connect();
                }
            }
        }

        @Override
        public void onMediaScannerConnected() {
            synchronized (this.mLock) {
                this.mConnected = true;
                if (!this.mPaths.isEmpty()) {
                    Iterator<String> it = this.mPaths.iterator();
                    while (it.hasNext()) {
                        this.mScannerConnection.scanFile(it.next(), null);
                    }
                    this.mPaths.clear();
                }
            }
        }

        @Override
        public void onScanCompleted(String str, Uri uri) {
        }
    }

    @TargetApi(XPath.Tokens.EXPRTOKEN_OPERATOR_EQUAL)
    private void createChannel(NotificationCompat.Builder builder) {
        Log.i("Gallery2/IngestService", "<createChannel> SDK: " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= 26) {
            this.mNotificationManager.createNotificationChannel(new NotificationChannel("mtp_channel", "mtp", 2));
            builder.setChannelId("mtp_channel");
        }
    }
}
