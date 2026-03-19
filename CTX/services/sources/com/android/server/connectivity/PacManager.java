package com.android.server.connectivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.net.IProxyCallback;
import com.android.net.IProxyPortListener;
import com.android.net.IProxyService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class PacManager {
    private static final String ACTION_PAC_REFRESH = "android.net.proxy.PAC_REFRESH";
    private static final String DEFAULT_DELAYS = "8 32 120 14400 43200";
    private static final int DELAY_1 = 0;
    private static final int DELAY_4 = 3;
    private static final int DELAY_LONG = 4;
    private static final long MAX_PAC_SIZE = 20000000;
    private static final String PAC_PACKAGE = "com.android.pacprocessor";
    private static final String PAC_SERVICE = "com.android.pacprocessor.PacService";
    private static final String PAC_SERVICE_NAME = "com.android.net.IProxyService";
    private static final String PROXY_PACKAGE = "com.android.proxyhandler";
    private static final String PROXY_SERVICE = "com.android.proxyhandler.ProxyService";
    private static final String TAG = "PacManager";
    private AlarmManager mAlarmManager;
    private ServiceConnection mConnection;
    private Handler mConnectivityHandler;
    private Context mContext;
    private int mCurrentDelay;
    private String mCurrentPac;
    private volatile boolean mHasDownloaded;
    private volatile boolean mHasSentBroadcast;
    private final Handler mNetThreadHandler;
    private PendingIntent mPacRefreshIntent;
    private ServiceConnection mProxyConnection;
    private int mProxyMessage;

    @GuardedBy("mProxyLock")
    private IProxyService mProxyService;

    @GuardedBy("mProxyLock")
    private volatile Uri mPacUrl = Uri.EMPTY;
    private final Object mProxyLock = new Object();
    private Runnable mPacDownloader = new Runnable() {
        @Override
        public void run() {
            String str;
            Uri uri = PacManager.this.mPacUrl;
            if (Uri.EMPTY.equals(uri)) {
                return;
            }
            int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-187);
            try {
                try {
                    str = PacManager.get(uri);
                } catch (IOException e) {
                    Log.w(PacManager.TAG, "Failed to load PAC file: " + e);
                    TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                    str = null;
                }
                if (str == null) {
                    PacManager.this.reschedule();
                    return;
                }
                synchronized (PacManager.this.mProxyLock) {
                    if (!str.equals(PacManager.this.mCurrentPac)) {
                        PacManager.this.setCurrentProxyScript(str);
                    }
                }
                PacManager.this.mHasDownloaded = true;
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                PacManager.this.sendProxyIfNeeded();
                PacManager.this.longSchedule();
            } finally {
                TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
            }
        }
    };
    private final HandlerThread mNetThread = new HandlerThread("android.pacmanager", 0);
    private int mLastPort = -1;

    class PacRefreshIntentReceiver extends BroadcastReceiver {
        PacRefreshIntentReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            PacManager.this.mNetThreadHandler.post(PacManager.this.mPacDownloader);
        }
    }

    public PacManager(Context context, Handler handler, int i) {
        this.mContext = context;
        this.mNetThread.start();
        this.mNetThreadHandler = new Handler(this.mNetThread.getLooper());
        this.mPacRefreshIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_PAC_REFRESH), 0);
        context.registerReceiver(new PacRefreshIntentReceiver(), new IntentFilter(ACTION_PAC_REFRESH));
        this.mConnectivityHandler = handler;
        this.mProxyMessage = i;
    }

    private AlarmManager getAlarmManager() {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        }
        return this.mAlarmManager;
    }

    public synchronized boolean setCurrentProxyScriptUrl(ProxyInfo proxyInfo) {
        if (!Uri.EMPTY.equals(proxyInfo.getPacFileUrl())) {
            if (proxyInfo.getPacFileUrl().equals(this.mPacUrl) && proxyInfo.getPort() > 0) {
                return false;
            }
            this.mPacUrl = proxyInfo.getPacFileUrl();
            this.mCurrentDelay = 0;
            this.mHasSentBroadcast = false;
            this.mHasDownloaded = false;
            getAlarmManager().cancel(this.mPacRefreshIntent);
            bind();
            return true;
        }
        getAlarmManager().cancel(this.mPacRefreshIntent);
        synchronized (this.mProxyLock) {
            this.mPacUrl = Uri.EMPTY;
            this.mCurrentPac = null;
            if (this.mProxyService != null) {
                try {
                    try {
                        this.mProxyService.stopPacSystem();
                    } catch (RemoteException e) {
                        Log.w(TAG, "Failed to stop PAC service", e);
                    }
                } finally {
                    unbind();
                }
            }
        }
        return false;
    }

    private static String get(Uri uri) throws IOException {
        long j;
        URLConnection uRLConnectionOpenConnection = new URL(uri.toString()).openConnection(Proxy.NO_PROXY);
        try {
            j = Long.parseLong(uRLConnectionOpenConnection.getHeaderField("Content-Length"));
        } catch (NumberFormatException e) {
            j = -1;
        }
        if (j > MAX_PAC_SIZE) {
            throw new IOException("PAC too big: " + j + " bytes");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[1024];
        do {
            int i = uRLConnectionOpenConnection.getInputStream().read(bArr);
            if (i != -1) {
                byteArrayOutputStream.write(bArr, 0, i);
            } else {
                return byteArrayOutputStream.toString();
            }
        } while (byteArrayOutputStream.size() <= MAX_PAC_SIZE);
        throw new IOException("PAC too big");
    }

    private int getNextDelay(int i) {
        int i2 = i + 1;
        if (i2 > 3) {
            return 3;
        }
        return i2;
    }

    private void longSchedule() {
        this.mCurrentDelay = 0;
        setDownloadIn(4);
    }

    private void reschedule() {
        this.mCurrentDelay = getNextDelay(this.mCurrentDelay);
        setDownloadIn(this.mCurrentDelay);
    }

    private String getPacChangeDelay() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str = SystemProperties.get("conn.pac_change_delay", DEFAULT_DELAYS);
        String string = Settings.Global.getString(contentResolver, "pac_change_delay");
        return string == null ? str : string;
    }

    private long getDownloadDelay(int i) {
        String[] strArrSplit = getPacChangeDelay().split(" ");
        if (i < strArrSplit.length) {
            return Long.parseLong(strArrSplit[i]);
        }
        return 0L;
    }

    private void setDownloadIn(int i) {
        getAlarmManager().set(3, (1000 * getDownloadDelay(i)) + SystemClock.elapsedRealtime(), this.mPacRefreshIntent);
    }

    private boolean setCurrentProxyScript(String str) {
        if (this.mProxyService == null) {
            Log.e(TAG, "setCurrentProxyScript: no proxy service");
            return false;
        }
        try {
            this.mProxyService.setPacFile(str);
            this.mCurrentPac = str;
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to set PAC file", e);
            return true;
        }
    }

    private void bind() {
        if (this.mContext == null) {
            Log.e(TAG, "No context for binding");
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(PAC_PACKAGE, PAC_SERVICE);
        if (this.mProxyConnection != null && this.mConnection != null) {
            this.mNetThreadHandler.post(this.mPacDownloader);
            return;
        }
        this.mConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                synchronized (PacManager.this.mProxyLock) {
                    PacManager.this.mProxyService = null;
                }
            }

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                synchronized (PacManager.this.mProxyLock) {
                    try {
                        Log.d(PacManager.TAG, "Adding service com.android.net.IProxyService " + iBinder.getInterfaceDescriptor());
                    } catch (RemoteException e) {
                        Log.e(PacManager.TAG, "Remote Exception", e);
                    }
                    ServiceManager.addService(PacManager.PAC_SERVICE_NAME, iBinder);
                    PacManager.this.mProxyService = IProxyService.Stub.asInterface(iBinder);
                    if (PacManager.this.mProxyService != null) {
                        try {
                            PacManager.this.mProxyService.startPacSystem();
                        } catch (RemoteException e2) {
                            Log.e(PacManager.TAG, "Unable to reach ProxyService - PAC will not be started", e2);
                        }
                        PacManager.this.mNetThreadHandler.post(PacManager.this.mPacDownloader);
                    } else {
                        Log.e(PacManager.TAG, "No proxy service");
                    }
                }
            }
        };
        this.mContext.bindService(intent, this.mConnection, 1073741829);
        Intent intent2 = new Intent();
        intent2.setClassName(PROXY_PACKAGE, PROXY_SERVICE);
        this.mProxyConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                IProxyCallback iProxyCallbackAsInterface = IProxyCallback.Stub.asInterface(iBinder);
                if (iProxyCallbackAsInterface != null) {
                    try {
                        iProxyCallbackAsInterface.getProxyPort(new IProxyPortListener.Stub() {
                            public void setProxyPort(int i) throws RemoteException {
                                if (PacManager.this.mLastPort != -1) {
                                    PacManager.this.mHasSentBroadcast = false;
                                }
                                PacManager.this.mLastPort = i;
                                if (i != -1) {
                                    Log.d(PacManager.TAG, "Local proxy is bound on " + i);
                                    PacManager.this.sendProxyIfNeeded();
                                    return;
                                }
                                Log.e(PacManager.TAG, "Received invalid port from Local Proxy, PAC will not be operational");
                            }
                        });
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        this.mContext.bindService(intent2, this.mProxyConnection, 1073741829);
    }

    private void unbind() {
        if (this.mConnection != null) {
            this.mContext.unbindService(this.mConnection);
            this.mConnection = null;
        }
        if (this.mProxyConnection != null) {
            this.mContext.unbindService(this.mProxyConnection);
            this.mProxyConnection = null;
        }
        this.mProxyService = null;
        this.mLastPort = -1;
    }

    private void sendPacBroadcast(ProxyInfo proxyInfo) {
        this.mConnectivityHandler.sendMessage(this.mConnectivityHandler.obtainMessage(this.mProxyMessage, proxyInfo));
    }

    private synchronized void sendProxyIfNeeded() {
        if (this.mHasDownloaded && this.mLastPort != -1) {
            if (!this.mHasSentBroadcast) {
                sendPacBroadcast(new ProxyInfo(this.mPacUrl, this.mLastPort));
                this.mHasSentBroadcast = true;
            }
        }
    }
}
