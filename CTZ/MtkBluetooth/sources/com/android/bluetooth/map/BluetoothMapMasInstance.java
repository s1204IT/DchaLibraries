package com.android.bluetooth.map;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.bluetooth.IObexConnectionHandler;
import com.android.bluetooth.ObexServerSockets;
import com.android.bluetooth.map.BluetoothMapContentObserver;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.bluetooth.sdp.SdpManager;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.obex.Authenticator;
import javax.obex.ServerSession;

public class BluetoothMapMasInstance implements IObexConnectionHandler {
    static final int SDP_MAP_MAS_FEATURES = 127;
    private static final int SDP_MAP_MAS_VERSION = 258;
    private static final int SDP_MAP_MSG_TYPE_EMAIL = 1;
    private static final int SDP_MAP_MSG_TYPE_IM = 16;
    private static final int SDP_MAP_MSG_TYPE_MMS = 8;
    private static final int SDP_MAP_MSG_TYPE_SMS_CDMA = 4;
    private static final int SDP_MAP_MSG_TYPE_SMS_GSM = 2;
    public static final String TYPE_EMAIL_STR = "EMAIL";
    public static final String TYPE_IM_STR = "IM";
    public static final String TYPE_SMS_MMS_STR = "SMS/MMS";
    private static final boolean V = false;
    private volatile boolean mAcceptNewConnections;
    private BluetoothMapAccountItem mAccount;
    private BluetoothAdapter mAdapter;
    private String mBaseUri;
    private BluetoothSocket mConnSocket;
    private Map<String, BluetoothMapConvoContactElement> mContactList;
    private Context mContext;
    private AtomicLong mDbIndetifier;
    private boolean mEnableSmsMms;
    private AtomicLong mFolderVersionCounter;
    private HashMap<Long, BluetoothMapConvoListingElement> mImEmailConvoList;
    private AtomicLong mImEmailConvoListVersionCounter;
    private volatile boolean mInterrupted;
    private BluetoothMapService mMapService;
    private int mMasInstanceId;
    private BluetoothMnsObexClient mMnsClient;
    private Map<Long, BluetoothMapContentObserver.Msg> mMsgListMms;
    private Map<Long, BluetoothMapContentObserver.Msg> mMsgListMsg;
    private Map<Long, BluetoothMapContentObserver.Msg> mMsgListSms;
    BluetoothMapContentObserver mObserver;
    private BluetoothDevice mRemoteDevice;
    private int mRemoteFeatureMask;
    private int mSdpHandle;
    private ServerSession mServerSession;
    private ObexServerSockets mServerSockets;
    private Handler mServiceHandler;
    private volatile boolean mShutdown;
    private HashMap<Long, BluetoothMapConvoListingElement> mSmsMmsConvoList;
    private AtomicLong mSmsMmsConvoListVersionCounter;
    private final String mTag;
    private static volatile int sInstanceCounter = 0;
    private static final boolean D = BluetoothMapService.DEBUG;

    public BluetoothMapMasInstance(BluetoothMapService bluetoothMapService, Context context, BluetoothMapAccountItem bluetoothMapAccountItem, int i, boolean z) {
        this.mServerSession = null;
        this.mServerSockets = null;
        this.mSdpHandle = -1;
        this.mConnSocket = null;
        this.mRemoteDevice = null;
        this.mShutdown = false;
        this.mAcceptNewConnections = false;
        this.mServiceHandler = null;
        this.mMapService = null;
        this.mContext = null;
        this.mMnsClient = null;
        this.mAccount = null;
        this.mBaseUri = null;
        this.mMasInstanceId = -1;
        this.mEnableSmsMms = false;
        this.mDbIndetifier = new AtomicLong();
        this.mFolderVersionCounter = new AtomicLong(0L);
        this.mSmsMmsConvoListVersionCounter = new AtomicLong(0L);
        this.mImEmailConvoListVersionCounter = new AtomicLong(0L);
        this.mMsgListSms = null;
        this.mMsgListMms = null;
        this.mMsgListMsg = null;
        this.mSmsMmsConvoList = new HashMap<>();
        this.mImEmailConvoList = new HashMap<>();
        this.mRemoteFeatureMask = 31;
        StringBuilder sb = new StringBuilder();
        sb.append("BluetoothMapMasInstance");
        int i2 = sInstanceCounter;
        sInstanceCounter = i2 + 1;
        sb.append(i2);
        this.mTag = sb.toString();
        this.mMapService = bluetoothMapService;
        this.mServiceHandler = bluetoothMapService.getHandler();
        this.mContext = context;
        this.mAccount = bluetoothMapAccountItem;
        if (bluetoothMapAccountItem != null) {
            this.mBaseUri = bluetoothMapAccountItem.mBase_uri;
        }
        this.mMasInstanceId = i;
        this.mEnableSmsMms = z;
        init();
    }

    private void removeSdpRecord() {
        if (this.mAdapter != null && this.mSdpHandle >= 0 && SdpManager.getDefaultManager() != null) {
            boolean zRemoveSdpRecord = SdpManager.getDefaultManager().removeSdpRecord(this.mSdpHandle);
            Log.d(this.mTag, "RemoveSDPrecord returns " + zRemoveSdpRecord);
            this.mSdpHandle = -1;
        }
    }

    protected BluetoothMapMasInstance() {
        this.mServerSession = null;
        this.mServerSockets = null;
        this.mSdpHandle = -1;
        this.mConnSocket = null;
        this.mRemoteDevice = null;
        this.mShutdown = false;
        this.mAcceptNewConnections = false;
        this.mServiceHandler = null;
        this.mMapService = null;
        this.mContext = null;
        this.mMnsClient = null;
        this.mAccount = null;
        this.mBaseUri = null;
        this.mMasInstanceId = -1;
        this.mEnableSmsMms = false;
        this.mDbIndetifier = new AtomicLong();
        this.mFolderVersionCounter = new AtomicLong(0L);
        this.mSmsMmsConvoListVersionCounter = new AtomicLong(0L);
        this.mImEmailConvoListVersionCounter = new AtomicLong(0L);
        this.mMsgListSms = null;
        this.mMsgListMms = null;
        this.mMsgListMsg = null;
        this.mSmsMmsConvoList = new HashMap<>();
        this.mImEmailConvoList = new HashMap<>();
        this.mRemoteFeatureMask = 31;
        StringBuilder sb = new StringBuilder();
        sb.append("BluetoothMapMasInstance");
        int i = sInstanceCounter;
        sInstanceCounter = i + 1;
        sb.append(i);
        this.mTag = sb.toString();
    }

    public String toString() {
        return "MasId: " + this.mMasInstanceId + " Uri:" + this.mBaseUri + " SMS/MMS:" + this.mEnableSmsMms;
    }

    private void init() {
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void updateDbIdentifier() {
        this.mDbIndetifier.set(Calendar.getInstance().getTime().getTime());
    }

    void updateFolderVersionCounter() {
        this.mFolderVersionCounter.incrementAndGet();
    }

    void updateSmsMmsConvoListVersionCounter() {
        this.mSmsMmsConvoListVersionCounter.incrementAndGet();
    }

    void updateImEmailConvoListVersionCounter() {
        this.mImEmailConvoListVersionCounter.incrementAndGet();
    }

    Map<Long, BluetoothMapContentObserver.Msg> getMsgListSms() {
        return this.mMsgListSms;
    }

    void setMsgListSms(Map<Long, BluetoothMapContentObserver.Msg> map) {
        this.mMsgListSms = map;
    }

    Map<Long, BluetoothMapContentObserver.Msg> getMsgListMms() {
        return this.mMsgListMms;
    }

    void setMsgListMms(Map<Long, BluetoothMapContentObserver.Msg> map) {
        this.mMsgListMms = map;
    }

    Map<Long, BluetoothMapContentObserver.Msg> getMsgListMsg() {
        return this.mMsgListMsg;
    }

    void setMsgListMsg(Map<Long, BluetoothMapContentObserver.Msg> map) {
        this.mMsgListMsg = map;
    }

    Map<String, BluetoothMapConvoContactElement> getContactList() {
        return this.mContactList;
    }

    void setContactList(Map<String, BluetoothMapConvoContactElement> map) {
        this.mContactList = map;
    }

    HashMap<Long, BluetoothMapConvoListingElement> getSmsMmsConvoList() {
        return this.mSmsMmsConvoList;
    }

    void setSmsMmsConvoList(HashMap<Long, BluetoothMapConvoListingElement> map) {
        this.mSmsMmsConvoList = map;
    }

    HashMap<Long, BluetoothMapConvoListingElement> getImEmailConvoList() {
        return this.mImEmailConvoList;
    }

    void setImEmailConvoList(HashMap<Long, BluetoothMapConvoListingElement> map) {
        this.mImEmailConvoList = map;
    }

    int getMasId() {
        return this.mMasInstanceId;
    }

    long getDbIdentifier() {
        return this.mDbIndetifier.get();
    }

    long getFolderVersionCounter() {
        return this.mFolderVersionCounter.get();
    }

    long getCombinedConvoListVersionCounter() {
        return this.mSmsMmsConvoListVersionCounter.get() + this.mImEmailConvoListVersionCounter.get();
    }

    public synchronized void startSocketListeners() {
        if (D) {
            Log.d(this.mTag, "Map Service startSocketListeners");
        }
        if (this.mServerSession != null) {
            if (D) {
                Log.d(this.mTag, "mServerSession exists - shutting it down...");
            }
            this.mServerSession.close();
            this.mServerSession = null;
        }
        if (this.mObserver != null) {
            if (D) {
                Log.d(this.mTag, "mObserver exists - shutting it down...");
            }
            this.mObserver.deinit();
            this.mObserver = null;
        }
        closeConnectionSocket();
        if (this.mServerSockets != null) {
            this.mAcceptNewConnections = true;
        } else {
            this.mServerSockets = ObexServerSockets.createWithFixedChannels(this, 26, SdpManager.MAP_L2CAP_PSM);
            this.mAcceptNewConnections = true;
            if (this.mServerSockets == null) {
                Log.e(this.mTag, "Failed to start the listeners");
            } else {
                removeSdpRecord();
                this.mSdpHandle = createMasSdpRecord(this.mServerSockets.getRfcommChannel(), this.mServerSockets.getL2capPsm());
                updateDbIdentifier();
            }
        }
    }

    private int createMasSdpRecord(int i, int i2) {
        int i3;
        String name = "";
        if (this.mEnableSmsMms) {
            name = TYPE_SMS_MMS_STR;
            i3 = 14;
        } else {
            i3 = 0;
        }
        if (this.mBaseUri != null) {
            if (this.mEnableSmsMms) {
                if (this.mAccount.getType() == BluetoothMapUtils.TYPE.EMAIL) {
                    name = name + "/EMAIL";
                } else if (this.mAccount.getType() == BluetoothMapUtils.TYPE.IM) {
                    name = name + "/IM";
                }
            } else {
                name = this.mAccount.getName();
            }
            if (this.mAccount.getType() == BluetoothMapUtils.TYPE.EMAIL) {
                i3 |= 1;
            } else if (this.mAccount.getType() == BluetoothMapUtils.TYPE.IM) {
                i3 |= 16;
            }
        }
        return SdpManager.getDefaultManager().createMapMasRecord(name, this.mMasInstanceId, i, i2, 258, i3, 127);
    }

    public boolean startObexServerSession(BluetoothMnsObexClient bluetoothMnsObexClient) throws IOException, RemoteException {
        if (D) {
            Log.d(this.mTag, "Map Service startObexServerSession masid = " + this.mMasInstanceId);
        }
        if (this.mConnSocket != null) {
            if (this.mServerSession != null) {
                return true;
            }
            this.mMnsClient = bluetoothMnsObexClient;
            this.mObserver = new BluetoothMapContentObserver(this.mContext, this.mMnsClient, this, this.mAccount, this.mEnableSmsMms);
            this.mObserver.init();
            this.mServerSession = new ServerSession(new BluetoothObexTransport(this.mConnSocket), new BluetoothMapObexServer(this.mServiceHandler, this.mContext, this.mObserver, this, this.mAccount, this.mEnableSmsMms), (Authenticator) null);
            if (D) {
                Log.d(this.mTag, "    ServerSession started.");
            }
            return true;
        }
        if (D) {
            Log.d(this.mTag, "    No connection for this instance");
            return false;
        }
        return false;
    }

    public boolean handleSmsSendIntent(Context context, Intent intent) {
        if (this.mObserver != null) {
            return this.mObserver.handleSmsSendIntent(context, intent);
        }
        return false;
    }

    public boolean isStarted() {
        return this.mConnSocket != null;
    }

    public void shutdown() {
        if (D) {
            Log.d(this.mTag, "MAP Service shutdown");
        }
        if (this.mServerSession != null) {
            this.mServerSession.close();
            this.mServerSession = null;
        }
        if (this.mObserver != null) {
            this.mObserver.deinit();
            this.mObserver = null;
        }
        removeSdpRecord();
        closeConnectionSocket();
        closeServerSockets(false);
    }

    public void restartObexServerSession() {
        if (D) {
            Log.d(this.mTag, "MAP Service restartObexServerSession()");
        }
        startSocketListeners();
    }

    private synchronized void closeServerSockets(boolean z) {
        ObexServerSockets obexServerSockets = this.mServerSockets;
        if (obexServerSockets != null) {
            obexServerSockets.shutdown(z);
            this.mServerSockets = null;
        }
    }

    private synchronized void closeConnectionSocket() {
        if (this.mConnSocket != null) {
            try {
                try {
                    this.mConnSocket.close();
                } catch (IOException e) {
                    Log.e(this.mTag, "Close Connection Socket error: ", e);
                }
            } finally {
                this.mConnSocket = null;
            }
        }
    }

    public void setRemoteFeatureMask(int i) {
        this.mRemoteFeatureMask = i & 127;
        if (this.mObserver != null) {
            this.mObserver.setObserverRemoteFeatureMask(this.mRemoteFeatureMask);
        }
    }

    public int getRemoteFeatureMask() {
        return this.mRemoteFeatureMask;
    }

    @Override
    public synchronized boolean onConnect(BluetoothDevice bluetoothDevice, BluetoothSocket bluetoothSocket) {
        if (!this.mAcceptNewConnections) {
            return false;
        }
        boolean zOnConnect = this.mMapService.onConnect(bluetoothDevice, this);
        if (zOnConnect) {
            this.mRemoteDevice = bluetoothDevice;
            this.mConnSocket = bluetoothSocket;
            this.mAcceptNewConnections = false;
        }
        return zOnConnect;
    }

    @Override
    public synchronized void onAcceptFailed() {
        this.mServerSockets = null;
        if (this.mShutdown) {
            Log.e(this.mTag, "Failed to accept incomming connection - shutdown");
        } else {
            Log.e(this.mTag, "Failed to accept incomming connection - restarting");
            startSocketListeners();
        }
    }
}
