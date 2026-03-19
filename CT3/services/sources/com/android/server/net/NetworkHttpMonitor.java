package com.android.server.net;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.am.EventLogTags;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.FlpHardwareProvider;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NetworkHttpMonitor {
    private static final String ACTION_POLL = "com.android.server.net.NetworkHttpMonitor.action.POLL";
    private static final String ACTION_ROUTING_UPDATE = "com.android.server.net.NetworkHttpMonitor.action.routing";
    private static final boolean DBG = true;
    private static final String DEFAULT_SERVER = "connectivitycheck.android.com";
    private static final int EVENT_DISABLE_FIREWALL = 2;
    private static final int EVENT_ENABLE_FIREWALL = 1;
    private static final int EVENT_KEEP_ALIVE = 3;
    private static final int EXPIRE_TIME = 1200000;
    private static final String HTTP_FIREWALL_UID = "net.http.browser.uid";
    private static final int KEEP_ALIVE_INTERVAL = 120000;
    private static final int MAX_REDIRECT_CONNECTION = 3;
    private static final int MOBILE = 0;
    private static final int SOCKET_TIMEOUT_MS = 10000;
    private static final String TAG = "NetworkHttpMonitor";
    private static Context mContext;
    private static INetworkManagementService mNetd;
    private static PackageManager mPackageManager;
    private ConnectivityManager cm;
    private AlarmManager mAlarmManager;
    private Handler mHandler;
    private PendingIntent mPendingPollIntent;
    final Object mRulesLock = new Object();
    private String mServer;
    private static int mHttpRedirectCount = 0;
    private static String WEB_LOCATION = "pcautivo.telcel.com";
    private static ArrayList<Integer> mBrowserAppUids = new ArrayList<>();
    private static ArrayList<String> mBrowserAppList = new ArrayList<>();
    private static boolean mIsFirewallEnabled = false;

    public NetworkHttpMonitor(Context context, INetworkManagementService netd) {
        mContext = context;
        mNetd = netd;
        mPackageManager = mContext.getPackageManager();
        this.mAlarmManager = (AlarmManager) mContext.getSystemService("alarm");
        this.mPendingPollIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_POLL), 0);
        registerForAlarms();
        registerForRougingUpdate();
        registerForRoutingUpdate();
        registerWifiEvent();
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new MyHandler(thread.getLooper());
        this.mServer = Settings.Global.getString(mContext.getContentResolver(), "captive_portal_server");
        if (this.mServer == null) {
            this.mServer = DEFAULT_SERVER;
        }
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            Slog.w(NetworkHttpMonitor.TAG, "msg:" + msg.what);
            switch (msg.what) {
                case 1:
                    NetworkHttpMonitor.this.enableFirewallPolicy();
                    break;
                case 2:
                    NetworkHttpMonitor.this.disableFirewall();
                    break;
                case 3:
                    NetworkHttpMonitor.this.sendKeepAlive();
                    break;
            }
        }
    }

    private void sendKeepAlive() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    HttpURLConnection httpURLConnection = null;
                    try {
                        String checkUrl = "http://" + NetworkHttpMonitor.this.mServer + "/generate_204";
                        Slog.w(NetworkHttpMonitor.TAG, "Checking:" + checkUrl);
                        try {
                            URL url = new URL(checkUrl);
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.setInstanceFollowRedirects(false);
                            urlConnection.setConnectTimeout(10000);
                            urlConnection.setReadTimeout(10000);
                            urlConnection.setUseCaches(false);
                            urlConnection.getInputStream();
                            int status = urlConnection.getResponseCode();
                            boolean isConnected = status == 204;
                            Slog.w(NetworkHttpMonitor.TAG, "Checking status:" + status);
                            if (isConnected) {
                                if (NetworkHttpMonitor.this.isFirewallEnabled()) {
                                    NetworkHttpMonitor.this.resetFirewallStatus();
                                }
                            } else if (status == 302 || status == 301 || status == 303) {
                                String loc = urlConnection.getHeaderField(FlpHardwareProvider.LOCATION);
                                Slog.w(NetworkHttpMonitor.TAG, "new loc:" + loc);
                                if (loc.contains(NetworkHttpMonitor.this.getWebLocation())) {
                                    if (!NetworkHttpMonitor.this.isFirewallEnabled()) {
                                        NetworkHttpMonitor.this.mHandler.obtainMessage(1).sendToTarget();
                                        boolean unused = NetworkHttpMonitor.mIsFirewallEnabled = true;
                                    } else {
                                        NetworkHttpMonitor.this.mHandler.sendMessageDelayed(NetworkHttpMonitor.this.mHandler.obtainMessage(3), JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                                    }
                                }
                            }
                            if (urlConnection != null) {
                                urlConnection.disconnect();
                            }
                        } catch (IOException e) {
                            Slog.w(NetworkHttpMonitor.TAG, "ioe:" + e);
                            NetworkHttpMonitor.this.mHandler.sendMessageDelayed(NetworkHttpMonitor.this.mHandler.obtainMessage(3), JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                            if (0 != 0) {
                                httpURLConnection.disconnect();
                            }
                        }
                    } catch (Throwable th) {
                        if (0 != 0) {
                            httpURLConnection.disconnect();
                        }
                        throw th;
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void clearFirewallRule() {
        resetFirewallStatus();
    }

    private void resetFirewallStatus() {
        synchronized (this.mRulesLock) {
            if (mIsFirewallEnabled) {
                Slog.w(TAG, "resetFirewallStatus");
                mIsFirewallEnabled = false;
                mHttpRedirectCount = 0;
                SystemProperties.set(HTTP_FIREWALL_UID, "");
                this.mAlarmManager.cancel(this.mPendingPollIntent);
                this.mHandler.obtainMessage(2).sendToTarget();
            }
        }
    }

    private void registerForAlarms() {
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Slog.w(NetworkHttpMonitor.TAG, "onReceive: registerForAlarms");
                NetworkHttpMonitor.this.resetFirewallStatus();
            }
        }, new IntentFilter(ACTION_POLL));
    }

    private void registerForRoutingUpdate() {
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Slog.d(NetworkHttpMonitor.TAG, "onReceive: registerForRoutingUpdate");
                NetworkHttpMonitor.this.mAlarmManager.cancel(NetworkHttpMonitor.this.mPendingPollIntent);
                NetworkHttpMonitor.this.resetFirewallStatus();
            }
        }, new IntentFilter(ACTION_ROUTING_UPDATE));
    }

    private void registerForRougingUpdate() {
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Slog.w(NetworkHttpMonitor.TAG, "onReceive: registerForRougingUpdate");
                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    return;
                }
                int event_type = bundle.getInt("eventType");
                if (event_type != 1 && event_type != 2) {
                    return;
                }
                NetworkHttpMonitor.this.mAlarmManager.cancel(NetworkHttpMonitor.this.mPendingPollIntent);
                NetworkHttpMonitor.this.resetFirewallStatus();
            }
        }, new IntentFilter("android.intent.action.ACTION_NETWORK_EVENT"));
    }

    private void registerWifiEvent() {
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo info;
                Bundle bundle = intent.getExtras();
                Slog.w(NetworkHttpMonitor.TAG, "onReceive: CONNECTIVITY_ACTION");
                if (bundle == null || (info = (NetworkInfo) bundle.get("networkInfo")) == null || info.getType() != 1 || !info.isConnected()) {
                    return;
                }
                NetworkHttpMonitor.this.mAlarmManager.cancel(NetworkHttpMonitor.this.mPendingPollIntent);
                Slog.w(NetworkHttpMonitor.TAG, "onReceive: resetFirewallStatus");
                NetworkHttpMonitor.this.resetFirewallStatus();
            }
        }, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    public boolean isFirewallEnabled() {
        Slog.w(TAG, "isFirewallEnabled:" + mIsFirewallEnabled);
        return mIsFirewallEnabled;
    }

    public String getWebLocation() {
        String web = WEB_LOCATION;
        String testWeb = SystemProperties.get("net.http.web.location", "");
        if (testWeb.length() != 0) {
            web = testWeb;
        }
        Slog.w(TAG, "getWebLocation:" + web);
        return web;
    }

    public void monitorHttpRedirect(String location, int appUid) {
        Slog.w(TAG, "HttpRedirect:" + mHttpRedirectCount + ":" + appUid + "\r\nloc:" + location);
        if ("1".equals(SystemProperties.get("ro.mtk_pre_sim_wo_bal_support", "0"))) {
            Slog.w(TAG, "test 1");
            if (!location.contains(getWebLocation())) {
                return;
            }
            if (mIsFirewallEnabled) {
                Slog.w(TAG, "Http Firewall is enabled");
                return;
            }
            Slog.w(TAG, "Non-app id:" + appUid);
            Slog.w(TAG, "Non-app id:" + appUid);
            if (appUid < 10000) {
                Slog.w(TAG, "Non-app id:" + appUid);
                return;
            }
            getBrowserAppList();
            if (isBrowsrAppByUid(appUid)) {
                return;
            }
            mHttpRedirectCount++;
            Slog.w(TAG, "mHttpRedirectCount add");
            Slog.w(TAG, "mHttpRedirectCount add");
            if (mHttpRedirectCount >= 3) {
                Slog.w(TAG, "Enable firewall");
                synchronized (this.mRulesLock) {
                    this.mHandler.obtainMessage(1).sendToTarget();
                }
            }
        }
        Slog.w(TAG, "test 2");
    }

    private void enableFirewallWithUid(int appUid, boolean isEnabled) {
        try {
            if (isEnabled) {
                mNetd.setFirewallUidRule(0, appUid, 1);
            } else {
                mNetd.setFirewallUidRule(0, appUid, 2);
            }
            Slog.w(TAG, "Test:" + appUid + ":" + isEnabled);
        } catch (Exception e) {
            Slog.w(TAG, "e:" + e + "\r\n" + appUid + ":" + isEnabled);
        }
    }

    private void enableFirewall() {
        try {
            mNetd.setFirewallEnabled(true);
            mNetd.setFirewallUidRule(0, 0, 1);
            mNetd.setFirewallUidRule(0, 1000, 1);
            mNetd.setFirewallEgressDestRule("0.0.0.0/0", 53, true);
            mNetd.setFirewallEgressProtoRule("icmp", true);
            mNetd.setFirewallInterfaceRule("lo", true);
            mNetd.setFirewallEgressDestRule("0.0.0.0/0", EventLogTags.AM_LOW_MEMORY, true);
            mNetd.setFirewallEgressDestRule("0.0.0.0/0", 5037, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disableFirewall() {
        try {
            mNetd.setFirewallEnabled(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Slog.w(TAG, "disableFirewall");
        sendFirewallIntent(false);
        if (this.mHandler.hasMessages(3)) {
            this.mHandler.removeMessages(3);
        }
        Slog.w(TAG, "Keep alive after the disableFirewall");
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
    }

    private void enableFirewallPolicy() {
        StringBuffer sb = new StringBuffer();
        Slog.w(TAG, "enableFirewallPolicy");
        enableFirewall();
        for (int i = 0; i < mBrowserAppUids.size(); i++) {
            if (i != 0) {
                sb.append("," + mBrowserAppUids.get(i));
                Slog.w(TAG, "mBrowserAppUids.get 1");
            } else {
                sb.append(mBrowserAppUids.get(i));
                Slog.w(TAG, "mBrowserAppUids.get 2");
            }
            enableFirewallWithUid(mBrowserAppUids.get(i).intValue(), true);
        }
        sendFirewallIntent(true);
        Slog.w(TAG, "new property:" + sb.toString());
        SystemProperties.set(HTTP_FIREWALL_UID, sb.toString());
        Slog.w(TAG, "start 20 minutes timer");
        mIsFirewallEnabled = true;
        long now = SystemClock.elapsedRealtime();
        long next = now + 1200000;
        this.mAlarmManager.set(3, next, this.mPendingPollIntent);
        if (this.mHandler.hasMessages(3)) {
            return;
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
    }

    private boolean isBrowsrAppByUid(int appUid) {
        for (int i = 0; i < mBrowserAppUids.size(); i++) {
            Slog.w(TAG, "isBrowsrAppByUid");
            if (appUid == mBrowserAppUids.get(i).intValue()) {
                return true;
            }
        }
        return false;
    }

    private void sendFirewallIntent(boolean isEnabled) {
        Slog.w(TAG, "sendFirewallIntent");
        Binder.clearCallingIdentity();
        Intent intent = new Intent("com.android.server.net.NetworkHttpMonitor.action.firewall");
        intent.addFlags(536870912);
        intent.putExtra("isEnabled", isEnabled);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private ArrayList getBrowserAppList() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.BROWSABLE");
        intent.setData(Uri.parse("http://www.google.com"));
        Slog.w(TAG, "getBrowserAppList");
        List<ResolveInfo> infos = mPackageManager.queryIntentActivities(intent, 64);
        Slog.w(TAG, "getBrowserAppList:" + infos.size());
        mBrowserAppList.clear();
        mBrowserAppUids.clear();
        for (ResolveInfo info : infos) {
            mBrowserAppList.add(info.activityInfo.packageName);
            mBrowserAppUids.add(new Integer(info.activityInfo.applicationInfo.uid));
        }
        return mBrowserAppList;
    }
}
