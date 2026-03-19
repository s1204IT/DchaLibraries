package com.mediatek.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.LocationManagerService;
import com.android.server.location.LocationFudger;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class NlpUtils {
    private static final boolean DEBUG = LocationManagerService.D;
    private static final int LAST_LOCATION_EXPIRED_TIMEOUT = 600000;
    private static final boolean NIJ_ON_GPS_START_DEFAULT = false;
    private static final int NLPS_CMD_GPS_NIJ_REQ = 101;
    private static final int NLPS_CMD_QUIT = 100;
    private static final int NLPS_MAX_CLIENTS = 2;
    private static final int NLPS_MSG_CLEAR_LAST_LOC = 4;
    private static final int NLPS_MSG_GPS_NIJ_REQ = 2;
    private static final int NLPS_MSG_GPS_STARTED = 0;
    private static final int NLPS_MSG_GPS_STOPPED = 1;
    private static final int NLPS_MSG_NLP_UPDATED = 3;
    protected static final String SOCKET_ADDRESS = "com.mediatek.nlpservice.NlpService";
    private static final int UPDATE_LOCATION = 7;
    private Context mContext;
    private Handler mGpsHandler;
    private NlpsMsgHandler mHandler;
    private LocationManager mLocationManager;
    private Thread mServerThread;
    private boolean mIsNlpRequested = false;
    private volatile boolean mIsStopping = false;
    private AtomicInteger mClientCount = new AtomicInteger();
    private LocalServerSocket mNlpServerSocket = null;
    private Location mLastLocation = null;
    private LocationListener mPassiveLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (!NlpUtils.this.mIsNlpRequested || !"network".equals(location.getProvider())) {
                return;
            }
            synchronized (this) {
                if (NlpUtils.this.mLastLocation == null) {
                    NlpUtils.this.mLastLocation = new Location(location);
                } else {
                    NlpUtils.this.mLastLocation.set(location);
                }
            }
            NlpUtils.this.mHandler.removeMessages(4);
            NlpUtils.this.sendCommandDelayed(4, LocationFudger.FASTEST_INTERVAL_MS);
            NlpUtils.this.sendCommand(3);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };
    private LocationListener mNetworkLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public NlpUtils(Context context, Handler gpsHandler) {
        if (DEBUG) {
            log("onCreate");
        }
        this.mLocationManager = (LocationManager) context.getSystemService("location");
        this.mContext = context;
        this.mGpsHandler = gpsHandler;
        HandlerThread handlerThread = new HandlerThread("[NlpUtils]");
        handlerThread.start();
        this.mHandler = new NlpsMsgHandler(handlerThread.getLooper());
        this.mServerThread = new Thread() {
            @Override
            public void run() {
                if (NlpUtils.DEBUG) {
                    NlpUtils.log("mServerThread.run()");
                }
                NlpUtils.this.doServerTask();
            }
        };
        this.mServerThread.start();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (!action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    return;
                }
                NlpUtils.this.connectivityAction(intent);
            }
        }, intentFilter);
        this.mLocationManager.requestLocationUpdates("passive", 0L, 0.0f, this.mPassiveLocationListener);
    }

    private void connectivityAction(Intent intent) {
        NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
        ConnectivityManager connManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        NetworkInfo info2 = connManager.getNetworkInfo(info.getType());
        if (!intent.getBooleanExtra("noConnectivity", false) && (info2 == null || info2.isConnected())) {
            return;
        }
        log("Connectivity set unConnected");
        clearLastLocation();
    }

    private boolean isNlpEnabled() {
        return this.mLocationManager.isProviderEnabled("network");
    }

    private void startNlpQuery() {
        if (this.mIsNlpRequested) {
            stopNlpQuery();
        }
        boolean isNlpEnabled = isNlpEnabled();
        log("startNlpQuery isNlpEnabled=" + isNlpEnabled + " ver=1.00");
        if (!isNlpEnabled) {
            return;
        }
        this.mLocationManager.requestLocationUpdates("network", 1000L, 0.0f, this.mNetworkLocationListener);
        this.mIsNlpRequested = true;
    }

    private void stopNlpQuery() {
        if (!this.mIsNlpRequested) {
            return;
        }
        this.mLocationManager.removeUpdates(this.mNetworkLocationListener);
        this.mIsNlpRequested = false;
    }

    public static void log(String msg) {
        Log.d("NlpUtils", msg);
    }

    private static void close(LocalServerSocket lss) {
        try {
            lss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void close(LocalSocket ls) {
        try {
            ls.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void requestNlp() {
        try {
            startNlpQuery();
            if (this.mLastLocation != null) {
                if (DEBUG) {
                    log("inject NLP location");
                }
                this.mGpsHandler.obtainMessage(7, 0, 0, this.mLastLocation).sendToTarget();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void releaseNlp() {
        try {
            stopNlpQuery();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void closeServerSocket() {
        if (this.mNlpServerSocket == null) {
            return;
        }
        close(this.mNlpServerSocket);
        this.mNlpServerSocket = null;
    }

    private synchronized void clearLastLocation() {
        if (DEBUG) {
            log("clearLastLocation");
        }
        this.mLastLocation = null;
    }

    private void doServerTask() {
        try {
            if (DEBUG) {
                log("NlpUtilsSocket+");
            }
            synchronized (this) {
                this.mNlpServerSocket = new LocalServerSocket(SOCKET_ADDRESS);
                if (DEBUG) {
                    log("NlpServerSocket: " + this.mNlpServerSocket);
                }
            }
            while (!this.mIsStopping) {
                if (DEBUG) {
                    log("NlpUtilsSocket, wait client");
                }
                LocalSocket instanceSocket = this.mNlpServerSocket.accept();
                if (DEBUG) {
                    log("NlpUtilsSocket, instance: " + instanceSocket);
                }
                if (!this.mIsStopping) {
                    if (this.mClientCount.get() < 2) {
                        new ServerInstanceThread(instanceSocket).start();
                    } else {
                        log("no resource, client count: " + this.mClientCount.get());
                        close(instanceSocket);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeServerSocket();
        if (DEBUG) {
            log("NlpUtilsSocket-");
        }
    }

    private class ServerInstanceThread extends Thread {
        LocalSocket mSocket;

        public ServerInstanceThread(LocalSocket instanceSocket) {
            this.mSocket = instanceSocket;
            NlpUtils.this.mClientCount.getAndIncrement();
            if (NlpUtils.DEBUG) {
                NlpUtils.log("client count+: " + NlpUtils.this.mClientCount.get());
            }
        }

        @Override
        public void run() {
            try {
                if (NlpUtils.DEBUG) {
                    NlpUtils.log("NlpInstanceSocket+");
                }
                DataInputStream dins = new DataInputStream(this.mSocket.getInputStream());
                while (true) {
                    if (NlpUtils.this.mIsStopping) {
                        break;
                    }
                    int cmd = DataCoder.getInt(dins);
                    DataCoder.getInt(dins);
                    DataCoder.getInt(dins);
                    DataCoder.getInt(dins);
                    if (cmd == 101) {
                        NlpUtils.log("ClientCmd: NLP_INJECT_REQ");
                        NlpUtils.this.sendCommand(2);
                    } else if (cmd == 100) {
                        if (NlpUtils.DEBUG) {
                            NlpUtils.log("ClientCmd: QUIT");
                        }
                    } else {
                        NlpUtils.log("ClientCmd, unknown: " + cmd);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            closeInstanceSocket();
            if (NlpUtils.DEBUG) {
                NlpUtils.log("NlpInstanceSocket-");
            }
        }

        private void closeInstanceSocket() {
            NlpUtils.close(this.mSocket);
            this.mSocket = null;
            NlpUtils.this.mClientCount.getAndDecrement();
            if (NlpUtils.DEBUG) {
                NlpUtils.log("client count-: " + NlpUtils.this.mClientCount.get());
            }
        }
    }

    private void sendCommand(int cmd) {
        Message msg = Message.obtain();
        msg.what = cmd;
        this.mHandler.sendMessage(msg);
    }

    private void sendCommandDelayed(int cmd, long delayMs) {
        Message msg = Message.obtain();
        msg.what = cmd;
        this.mHandler.sendMessageDelayed(msg, delayMs);
    }

    private class NlpsMsgHandler extends Handler {
        public NlpsMsgHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    if (NlpUtils.DEBUG) {
                        NlpUtils.log("handle NLPS_MSG_GPS_NIJ_REQ");
                    }
                    NlpUtils.this.requestNlp();
                    break;
                case 3:
                    if (NlpUtils.DEBUG) {
                        NlpUtils.log("handle NLPS_MSG_NLP_UPDATED");
                    }
                    NlpUtils.this.releaseNlp();
                    break;
                case 4:
                    if (NlpUtils.DEBUG) {
                        NlpUtils.log("handle NLPS_MSG_CLEAR_LAST_LOC");
                    }
                    NlpUtils.this.clearLastLocation();
                    break;
                default:
                    NlpUtils.log("Undefined message");
                    break;
            }
        }
    }
}
