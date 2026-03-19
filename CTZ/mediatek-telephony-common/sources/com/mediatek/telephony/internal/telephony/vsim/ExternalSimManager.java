package com.mediatek.telephony.internal.telephony.vsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkProxyController;
import com.mediatek.internal.telephony.MtkSubscriptionController;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.MtkUiccController;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mediatek.telephony.MtkServiceState;

public class ExternalSimManager {
    private static final int AUTO_RETRY_DURATION = 2000;
    private static final int EVENT_VSIM_INDICATION = 1;
    private static final int MAX_VSIM_UICC_CMD_LEN = 269;
    private static final byte NO_RESPONSE_STATUS_WORD_BYTE1 = 0;
    private static final byte NO_RESPONSE_STATUS_WORD_BYTE2 = 0;
    private static final int NO_RESPONSE_TIMEOUT_DURATION = 13000;
    private static final int PLATFORM_READY_CATEGORY_RADIO = 3;
    private static final int PLATFORM_READY_CATEGORY_SIM_SWITCH = 2;
    private static final int PLATFORM_READY_CATEGORY_SUB = 1;
    private static final int PLUG_IN_AUTO_RETRY_TIMEOUT = 40000;
    private static final String PREFERED_AKA_SIM_SLOT = "vendor.gsm.prefered.aka.sim.slot";
    private static final String PREFERED_RSIM_SLOT = "vendor.gsm.prefered.rsim.slot";
    private static final int RECOVERY_TO_REAL_SIM_TIMEOUT = 300000;
    private static final int SET_CAPABILITY_DONE = 2;
    private static final int SET_CAPABILITY_FAILED = 3;
    private static final int SET_CAPABILITY_NONE = 0;
    private static final int SET_CAPABILITY_ONGOING = 1;
    private static final int SIM_STATE_RETRY_DURATION = 20000;
    private static final int SOCKET_OPEN_RETRY_MILLIS = 4000;
    private static final String TAG = "ExternalSimMgr";
    private static final int TRY_RESET_MODEM_DURATION = 2000;
    private CommandsInterface[] mCi;
    private Context mContext;
    private VsimEvenHandler mEventHandler;
    private VsimIndEventHandler mIndHandler;
    private final Object mLock;
    private final Object mLockForEventReq;
    private final BroadcastReceiver mReceiver;
    private Timer mRecoveryTimer;
    private int mSetCapabilityDone;
    private int mUserMainPhoneId;
    private boolean mUserRadioOn;
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static boolean PLUG_IN_AUTO_RETRY = true;
    private static ExternalSimManager sInstance = null;
    static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
    private static int sPreferedRsimSlot = -1;
    private static int sPreferedAkaSlot = -1;

    public ExternalSimManager() {
        this.mRecoveryTimer = null;
        this.mContext = null;
        this.mCi = null;
        this.mIndHandler = null;
        this.mEventHandler = null;
        this.mLockForEventReq = new Object();
        this.mLock = new Object();
        this.mSetCapabilityDone = 0;
        this.mUserMainPhoneId = -1;
        this.mUserRadioOn = false;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Rlog.d(ExternalSimManager.TAG, "[Receiver]+");
                String action = intent.getAction();
                Rlog.d(ExternalSimManager.TAG, "Action: " + action);
                if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE") && ExternalSimManager.this.mSetCapabilityDone == 1) {
                    if (ExternalSimManager.this.mEventHandler.getVsimSlotId(2) == RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()) {
                        synchronized (ExternalSimManager.this.mLock) {
                            ExternalSimManager.this.mSetCapabilityDone = 2;
                            ExternalSimManager.this.sendCapabilityDoneEvent();
                        }
                        Rlog.d(ExternalSimManager.TAG, "SET_CAPABILITY_DONE, notify all");
                    }
                } else if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED") && ExternalSimManager.this.mSetCapabilityDone == 1) {
                    synchronized (ExternalSimManager.this.mLock) {
                        ExternalSimManager.this.mSetCapabilityDone = 3;
                        ExternalSimManager.this.sendCapabilityDoneEvent();
                    }
                    Rlog.d(ExternalSimManager.TAG, "SET_CAPABILITY_FAILED, notify all");
                }
                Rlog.d(ExternalSimManager.TAG, "[Receiver]-");
            }
        };
        Rlog.d(TAG, "construtor 0 parameter is called - done");
    }

    private ExternalSimManager(Context context, CommandsInterface[] commandsInterfaceArr) {
        this.mRecoveryTimer = null;
        this.mContext = null;
        this.mCi = null;
        this.mIndHandler = null;
        this.mEventHandler = null;
        this.mLockForEventReq = new Object();
        this.mLock = new Object();
        this.mSetCapabilityDone = 0;
        this.mUserMainPhoneId = -1;
        this.mUserRadioOn = false;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Rlog.d(ExternalSimManager.TAG, "[Receiver]+");
                String action = intent.getAction();
                Rlog.d(ExternalSimManager.TAG, "Action: " + action);
                if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE") && ExternalSimManager.this.mSetCapabilityDone == 1) {
                    if (ExternalSimManager.this.mEventHandler.getVsimSlotId(2) == RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()) {
                        synchronized (ExternalSimManager.this.mLock) {
                            ExternalSimManager.this.mSetCapabilityDone = 2;
                            ExternalSimManager.this.sendCapabilityDoneEvent();
                        }
                        Rlog.d(ExternalSimManager.TAG, "SET_CAPABILITY_DONE, notify all");
                    }
                } else if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED") && ExternalSimManager.this.mSetCapabilityDone == 1) {
                    synchronized (ExternalSimManager.this.mLock) {
                        ExternalSimManager.this.mSetCapabilityDone = 3;
                        ExternalSimManager.this.sendCapabilityDoneEvent();
                    }
                    Rlog.d(ExternalSimManager.TAG, "SET_CAPABILITY_FAILED, notify all");
                }
                Rlog.d(ExternalSimManager.TAG, "[Receiver]-");
            }
        };
        Rlog.d(TAG, "construtor 1 parameter is called - start");
        initVsimConfiguration();
        startRecoveryTimer();
        this.mContext = context;
        this.mCi = commandsInterfaceArr;
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                ExternalSimManager.this.mEventHandler = ExternalSimManager.this.new VsimEvenHandler();
                ExternalSimManager.this.mIndHandler = ExternalSimManager.this.new VsimIndEventHandler();
                for (int i = 0; i < ExternalSimManager.this.mCi.length; i++) {
                    ExternalSimManager.this.mCi[i].registerForVsimIndication(ExternalSimManager.this.mIndHandler, 1, new Integer(i));
                }
                Looper.loop();
            }
        }.start();
        new Thread() {
            @Override
            public void run() throws Throwable {
                while (true) {
                    if (ExternalSimManager.this.mEventHandler == null || ExternalSimManager.this.mIndHandler == null) {
                        try {
                            Thread.sleep(10L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        ExternalSimManager.this.new ServerTask().listenConnection();
                        return;
                    }
                }
            }
        }.start();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        context.registerReceiver(this.mReceiver, intentFilter);
        Rlog.d(TAG, "construtor is called - end");
    }

    public static ExternalSimManager make(Context context, CommandsInterface[] commandsInterfaceArr) {
        if (sInstance == null) {
            sInstance = new ExternalSimManager(context, commandsInterfaceArr);
        }
        return sInstance;
    }

    private static String truncateString(String str) {
        if (str == null || str.length() < 6) {
            return str;
        }
        return str.substring(0, 2) + "***" + str.substring(str.length() - 4);
    }

    private static IMtkTelephonyEx getITelephonyEx() {
        return IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
    }

    private void sendCapabilityDoneEvent() {
        VsimEvent vsimEvent = new VsimEvent(0, ExternalSimConstants.MSG_ID_CAPABILITY_SWITCH_DONE, -1);
        Message message = new Message();
        message.obj = vsimEvent;
        this.mEventHandler.sendMessage(message);
        Rlog.d(TAG, "sendCapabilityDoneEvent....");
    }

    public boolean initializeService(byte[] bArr) {
        Rlog.d(TAG, "initializeService() - start");
        if (SystemProperties.getInt("ro.vendor.mtk_external_sim_support", 0) == 0) {
            Rlog.d(TAG, "initializeService() - mtk_external_sim_support didn't support");
            return false;
        }
        SystemProperties.set("ctl.start", "osi");
        Rlog.d(TAG, "initializeService() - end");
        return true;
    }

    public boolean finalizeService(byte[] bArr) {
        Rlog.d(TAG, "finalizeService() - start");
        if (SystemProperties.getInt("ro.vendor.mtk_external_sim_support", 0) == 0) {
            Rlog.d(TAG, "initializeService() - mtk_external_sim_support didn't support");
            return false;
        }
        SystemProperties.set("ctl.stop", "osi");
        Rlog.d(TAG, "finalizeService() - end");
        return true;
    }

    public void setVsimProperty(int i, String str, String str2) {
        TelephonyManager.getDefault();
        TelephonyManager.setTelephonyProperty(i, str, str2);
        TelephonyManager.getDefault();
        TelephonyManager.getTelephonyProperty(i, str, "");
        do {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TelephonyManager.getDefault();
        } while (!TelephonyManager.getTelephonyProperty(i, str, "").equals(str2));
    }

    public void initVsimConfiguration() {
        sPreferedRsimSlot = SystemProperties.getInt(PREFERED_RSIM_SLOT, -1);
        sPreferedAkaSlot = SystemProperties.getInt(PREFERED_AKA_SIM_SLOT, -1);
    }

    public static boolean isNonDsdaRemoteSimSupport() {
        return SystemProperties.getInt("ro.vendor.mtk_non_dsda_rsim_support", 0) == 1;
    }

    public static boolean isSupportVsimHotPlugOut() {
        for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
            TelephonyManager.getDefault();
            String telephonyProperty = TelephonyManager.getTelephonyProperty(i, "vendor.gsm.modem.vsim.capability", "0");
            if (telephonyProperty != null && telephonyProperty.length() > 0 && !"0".equals(telephonyProperty) && (Integer.parseInt(telephonyProperty) & 2) > 0) {
                return true;
            }
        }
        return false;
    }

    public static int getPreferedRsimSlot() {
        return sPreferedRsimSlot;
    }

    public static boolean isAnyVsimEnabled() {
        for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
            TelephonyManager.getDefault();
            String telephonyProperty = TelephonyManager.getTelephonyProperty(i, "vendor.gsm.external.sim.enabled", "0");
            if (telephonyProperty != null && telephonyProperty.length() > 0 && !"0".equals(telephonyProperty)) {
                return true;
            }
        }
        return false;
    }

    public void startRecoveryTimer() {
        for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
            TelephonyManager.getDefault();
            String telephonyProperty = TelephonyManager.getTelephonyProperty(i, "persist.vendor.radio.external.sim", "0");
            if (telephonyProperty != null && telephonyProperty.length() > 0 && String.valueOf(2).equals(telephonyProperty)) {
                if (this.mRecoveryTimer == null) {
                    this.mRecoveryTimer = new Timer();
                    TelephonyManager.getDefault();
                    int i2 = RECOVERY_TO_REAL_SIM_TIMEOUT;
                    String telephonyProperty2 = TelephonyManager.getTelephonyProperty(i, "persist.vendor.radio.vsim.timeout", String.valueOf(RECOVERY_TO_REAL_SIM_TIMEOUT));
                    try {
                        if (Integer.parseInt(telephonyProperty2) > 0) {
                            i2 = Integer.parseInt(telephonyProperty2) * 1000;
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    this.mRecoveryTimer.schedule(new RecoveryRealSimTask(), i2);
                    Rlog.i(TAG, "startRecoveryTimer: " + i2 + " ms.");
                    return;
                }
                return;
            }
        }
        Rlog.i(TAG, "No need to startRecoveryTimer since didn't set persist VSIM.");
    }

    public void stopRecoveryTimer() {
        if (this.mRecoveryTimer != null) {
            this.mRecoveryTimer.cancel();
            this.mRecoveryTimer.purge();
            this.mRecoveryTimer = null;
            Rlog.i(TAG, "stopRecoveryTimer.");
        }
    }

    public class RecoveryRealSimTask extends TimerTask {
        public RecoveryRealSimTask() {
        }

        @Override
        public void run() {
            int i = 0;
            for (int i2 = 0; i2 < TelephonyManager.getDefault().getSimCount(); i2++) {
                TelephonyManager.getDefault();
                String telephonyProperty = TelephonyManager.getTelephonyProperty(i2, "vendor.gsm.external.sim.enabled", "0");
                if (telephonyProperty != null && telephonyProperty.length() > 0 && !"0".equals(telephonyProperty)) {
                    Rlog.i(ExternalSimManager.TAG, "Auto recovery time out, disable VSIM...");
                    ExternalSimManager.this.sendDisableEvent(1 << i2, 1);
                }
            }
            if (!ExternalSimManager.isSupportVsimHotPlugOut()) {
                while (i < TelephonyManager.getDefault().getSimCount()) {
                    TelephonyManager.getDefault();
                    String telephonyProperty2 = TelephonyManager.getTelephonyProperty(i, "vendor.gsm.external.sim.enabled", "0");
                    if (telephonyProperty2 != null && telephonyProperty2.length() > 0 && !"0".equals(telephonyProperty2)) {
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        i++;
                    }
                }
                ExternalSimManager.this.disableAllVsimWithResetModem();
            }
        }
    }

    public void disableAllVsimWithResetModem() {
        for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
            waitRildSetDisabledProperty(i);
        }
        if (this.mEventHandler != null) {
            this.mEventHandler.retryIfRadioUnavailable(null);
        }
        RadioManager.getInstance().setSilentRebootPropertyForAllModem("1");
        ((MtkUiccController) UiccController.getInstance()).resetRadioForVsim();
        Rlog.i(TAG, "disableAllVsimWithResetModem...");
    }

    public void sendDisableEvent(int i, int i2) {
        VsimEvent vsimEvent = new VsimEvent(0, 3, i);
        vsimEvent.putInt(2);
        vsimEvent.putInt(i2);
        Message message = new Message();
        message.obj = vsimEvent;
        this.mEventHandler.sendMessage(message);
        Rlog.i(TAG, "sendDisableEvent[" + i + "]....");
    }

    private void sendExternalSimConnectedEvent(int i) {
        VsimEvent vsimEvent = new VsimEvent(0, 3, 0);
        vsimEvent.putInt(ExternalSimConstants.EVENT_TYPE_EXTERNAL_SIM_CONNECTED);
        vsimEvent.putInt(i);
        Message message = new Message();
        message.obj = vsimEvent;
        this.mEventHandler.sendMessage(message);
        Rlog.i(TAG, "sendExternalSimConnectedEvent connected=" + i);
    }

    private void waitRildSetDisabledProperty(int i) {
        TelephonyManager.getDefault();
        String telephonyProperty = TelephonyManager.getTelephonyProperty(i, "vendor.gsm.external.sim.enabled", "0");
        while (telephonyProperty != null && telephonyProperty.length() > 0 && !"0".equals(telephonyProperty)) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TelephonyManager.getDefault();
            telephonyProperty = TelephonyManager.getTelephonyProperty(i, "vendor.gsm.external.sim.enabled", "0");
        }
    }

    public class ServerTask {
        public static final String HOST_NAME = "vsim-adaptor";
        private VsimIoThread ioThread = null;

        public ServerTask() {
        }

        public void listenConnection() throws Throwable {
            LocalServerSocket localServerSocket;
            Exception e;
            IOException e2;
            Rlog.d(ExternalSimManager.TAG, "listenConnection() - start");
            ExecutorService executorServiceNewCachedThreadPool = Executors.newCachedThreadPool();
            LocalServerSocket localServerSocket2 = null;
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                try {
                    localServerSocket = new LocalServerSocket(HOST_NAME);
                    while (true) {
                        try {
                            LocalSocket localSocketAccept = localServerSocket.accept();
                            Rlog.i(ExternalSimManager.TAG, "There is a client is accpted: " + localSocketAccept.toString());
                            ExternalSimManager.this.stopRecoveryTimer();
                            ExternalSimManager.this.sendExternalSimConnectedEvent(1);
                            executorServiceNewCachedThreadPool.execute(ExternalSimManager.this.new ConnectionHandler(localSocketAccept, ExternalSimManager.this.mEventHandler));
                        } catch (IOException e3) {
                            e2 = e3;
                            Rlog.w(ExternalSimManager.TAG, "listenConnection catch IOException");
                            e2.printStackTrace();
                            Rlog.d(ExternalSimManager.TAG, "listenConnection finally!!");
                            if (executorServiceNewCachedThreadPool != null) {
                                executorServiceNewCachedThreadPool.shutdown();
                            }
                            if (localServerSocket != null) {
                                localServerSocket.close();
                            }
                            Rlog.d(ExternalSimManager.TAG, "listenConnection() - end");
                        } catch (Exception e4) {
                            e = e4;
                            Rlog.w(ExternalSimManager.TAG, "listenConnection catch Exception");
                            e.printStackTrace();
                            Rlog.d(ExternalSimManager.TAG, "listenConnection finally!!");
                            if (executorServiceNewCachedThreadPool != null) {
                                executorServiceNewCachedThreadPool.shutdown();
                            }
                            if (localServerSocket != null) {
                                localServerSocket.close();
                            }
                            Rlog.d(ExternalSimManager.TAG, "listenConnection() - end");
                        }
                    }
                } catch (IOException e5) {
                    e5.printStackTrace();
                    Rlog.d(ExternalSimManager.TAG, "listenConnection() - end");
                }
            } catch (IOException e6) {
                localServerSocket = null;
                e2 = e6;
            } catch (Exception e7) {
                localServerSocket = null;
                e = e7;
            } catch (Throwable th2) {
                th = th2;
                Rlog.d(ExternalSimManager.TAG, "listenConnection finally!!");
                if (executorServiceNewCachedThreadPool != null) {
                    executorServiceNewCachedThreadPool.shutdown();
                }
                if (0 != 0) {
                    try {
                        localServerSocket2.close();
                    } catch (IOException e8) {
                        e8.printStackTrace();
                    }
                }
                throw th;
            }
        }
    }

    public class ConnectionHandler implements Runnable {
        public static final String RILD_SERVER_NAME = "rild-vsim";
        private VsimEvenHandler mEventHandler;
        private LocalSocket mSocket;

        public ConnectionHandler(LocalSocket localSocket, VsimEvenHandler vsimEvenHandler) {
            this.mSocket = localSocket;
            this.mEventHandler = vsimEvenHandler;
        }

        @Override
        public void run() {
            Rlog.i(ExternalSimManager.TAG, "New connection: " + this.mSocket.toString());
            try {
                VsimIoThread vsimIoThread = ExternalSimManager.this.new VsimIoThread(ServerTask.HOST_NAME, this.mSocket.getInputStream(), this.mSocket.getOutputStream(), this.mEventHandler);
                this.mEventHandler.setDataStream(vsimIoThread, null);
                vsimIoThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class VsimEvent {
        public static final int DEFAULT_MAX_DATA_LENGTH = 512;
        private byte[] mData;
        private int mDataLen;
        private int mEventMaxDataLen;
        private int mMessageId;
        private int mReadOffset;
        private int mSlotId;
        private int mTransactionId;

        public VsimEvent(int i, int i2) {
            this(i, i2, 0);
        }

        public VsimEvent(int i, int i2, int i3) {
            this(i, i2, 512, i3);
        }

        public VsimEvent(int i, int i2, int i3, int i4) {
            this.mEventMaxDataLen = 512;
            this.mTransactionId = i;
            this.mMessageId = i2;
            this.mSlotId = i4;
            this.mEventMaxDataLen = i3;
            this.mData = new byte[this.mEventMaxDataLen];
            this.mDataLen = 0;
            this.mReadOffset = 0;
        }

        public void resetOffset() {
            synchronized (this) {
                this.mReadOffset = 0;
            }
        }

        public int putInt(int i) {
            synchronized (this) {
                if (this.mDataLen > this.mEventMaxDataLen - 4) {
                    return -1;
                }
                for (int i2 = 0; i2 < 4; i2++) {
                    this.mData[this.mDataLen] = (byte) ((i >> (8 * i2)) & 255);
                    this.mDataLen++;
                }
                return 0;
            }
        }

        public int putShort(int i) {
            synchronized (this) {
                if (this.mDataLen > this.mEventMaxDataLen - 2) {
                    return -1;
                }
                for (int i2 = 0; i2 < 2; i2++) {
                    this.mData[this.mDataLen] = (byte) ((i >> (8 * i2)) & 255);
                    this.mDataLen++;
                }
                return 0;
            }
        }

        public int putByte(int i) {
            synchronized (this) {
                if (this.mDataLen > this.mEventMaxDataLen - 1) {
                    return -1;
                }
                this.mData[this.mDataLen] = (byte) (i & 255);
                this.mDataLen++;
                return 0;
            }
        }

        public int putString(String str, int i) {
            synchronized (this) {
                if (this.mDataLen > this.mEventMaxDataLen - i) {
                    return -1;
                }
                byte[] bytes = str.getBytes();
                if (i < str.length()) {
                    System.arraycopy(bytes, 0, this.mData, this.mDataLen, i);
                    this.mDataLen += i;
                } else {
                    int length = i - str.length();
                    System.arraycopy(bytes, 0, this.mData, this.mDataLen, str.length());
                    this.mDataLen += str.length();
                    for (int i2 = 0; i2 < length; i2++) {
                        this.mData[this.mDataLen] = 0;
                        this.mDataLen++;
                    }
                }
                return 0;
            }
        }

        public int putBytes(byte[] bArr) {
            synchronized (this) {
                int length = bArr.length;
                if (length > this.mEventMaxDataLen) {
                    return -1;
                }
                System.arraycopy(bArr, 0, this.mData, this.mDataLen, length);
                this.mDataLen += length;
                return 0;
            }
        }

        public byte[] getData() {
            byte[] bArr;
            synchronized (this) {
                bArr = new byte[this.mDataLen];
                System.arraycopy(this.mData, 0, bArr, 0, this.mDataLen);
            }
            return bArr;
        }

        public int getDataLen() {
            int i;
            synchronized (this) {
                i = this.mDataLen;
            }
            return i;
        }

        public byte[] getDataByReadOffest() {
            byte[] bArr;
            synchronized (this) {
                bArr = new byte[this.mDataLen - this.mReadOffset];
                System.arraycopy(this.mData, this.mReadOffset, bArr, 0, this.mDataLen - this.mReadOffset);
            }
            return bArr;
        }

        public int getMessageId() {
            return this.mMessageId;
        }

        public int getSlotBitMask() {
            return this.mSlotId;
        }

        public int getFirstSlotId() {
            int simCount = TelephonyManager.getDefault().getSimCount();
            if (getSlotBitMask() > (1 << (simCount - 1))) {
                Rlog.w(ExternalSimManager.TAG, "getFirstSlotId, invalid slot id: " + getSlotBitMask());
                return 0;
            }
            for (int i = 0; i < simCount; i++) {
                if ((getSlotBitMask() & (1 << i)) != 0) {
                    return i;
                }
            }
            Rlog.w(ExternalSimManager.TAG, "getFirstSlotId, invalid slot id: " + getSlotBitMask());
            return 0;
        }

        public int getTransactionId() {
            return this.mTransactionId;
        }

        public int getInt() {
            int i;
            synchronized (this) {
                if (this.mData.length >= 4) {
                    i = ((this.mData[this.mReadOffset + 3] & PplMessageManager.Type.INVALID) << 24) | ((this.mData[this.mReadOffset + 2] & PplMessageManager.Type.INVALID) << 16) | ((this.mData[this.mReadOffset + 1] & PplMessageManager.Type.INVALID) << 8) | (this.mData[this.mReadOffset] & PplMessageManager.Type.INVALID);
                    this.mReadOffset += 4;
                } else {
                    i = 0;
                }
            }
            return i;
        }

        public int getShort() {
            int i;
            synchronized (this) {
                i = ((this.mData[this.mReadOffset + 1] & PplMessageManager.Type.INVALID) << 8) | (this.mData[this.mReadOffset] & PplMessageManager.Type.INVALID);
                this.mReadOffset += 2;
            }
            return i;
        }

        public int getByte() {
            int i;
            synchronized (this) {
                i = this.mData[this.mReadOffset] & PplMessageManager.Type.INVALID;
                this.mReadOffset++;
            }
            return i;
        }

        public byte[] getBytes(int i) {
            synchronized (this) {
                if (i > this.mDataLen - this.mReadOffset) {
                    return null;
                }
                byte[] bArr = new byte[i];
                for (int i2 = 0; i2 < i; i2++) {
                    bArr[i2] = this.mData[this.mReadOffset];
                    this.mReadOffset++;
                }
                return bArr;
            }
        }

        public String getString(int i) {
            byte[] bArr = new byte[i];
            synchronized (this) {
                System.arraycopy(this.mData, this.mReadOffset, bArr, 0, i);
                this.mReadOffset += i;
            }
            return new String(bArr).trim();
        }

        public String toString() {
            return new String("dumpEvent: transaction_id: " + getTransactionId() + ", message_id:" + getMessageId() + ", slot_id:" + getSlotBitMask() + ", data_len:" + getDataLen() + ", event:" + ExternalSimManager.truncateString(IccUtils.bytesToHexString(getData())));
        }
    }

    class VsimIoThread extends Thread {
        private static final int MAX_DATA_LENGTH = 20480;
        private VsimEvenHandler mEventHandler;
        private DataInputStream mInput;
        private String mName;
        private DataOutputStream mOutput;
        private boolean mIsContinue = true;
        private String mServerName = "";
        private byte[] readBuffer = null;

        public VsimIoThread(String str, InputStream inputStream, OutputStream outputStream, VsimEvenHandler vsimEvenHandler) {
            this.mName = "";
            this.mInput = null;
            this.mOutput = null;
            this.mEventHandler = null;
            this.mName = str;
            this.mInput = new DataInputStream(inputStream);
            this.mOutput = new DataOutputStream(outputStream);
            this.mEventHandler = vsimEvenHandler;
            logd("VsimIoThread constructor is called.");
        }

        public void terminate() {
            logd("VsimIoThread terminate.");
            this.mIsContinue = false;
        }

        @Override
        public void run() {
            logd("VsimIoThread running.");
            while (this.mIsContinue) {
                try {
                    VsimEvent event = readEvent();
                    if (event != null) {
                        Message message = new Message();
                        message.obj = event;
                        this.mEventHandler.sendMessage(message);
                    }
                } catch (IOException e) {
                    logw("VsimIoThread IOException.");
                    e.printStackTrace();
                    if (this.mServerName.equals("")) {
                        for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
                            TelephonyManager.getDefault();
                            String telephonyProperty = TelephonyManager.getTelephonyProperty(i, "vendor.gsm.external.sim.enabled", "0");
                            TelephonyManager.getDefault();
                            String telephonyProperty2 = TelephonyManager.getTelephonyProperty(i, "vendor.gsm.external.sim.inserted", "0");
                            if (telephonyProperty != null && telephonyProperty.length() > 0 && !"0".equals(telephonyProperty)) {
                                if (telephonyProperty2 == null || telephonyProperty2.length() <= 0) {
                                    telephonyProperty2 = "0";
                                }
                                ExternalSimManager.this.sendDisableEvent(1 << i, Integer.valueOf(telephonyProperty2).intValue());
                                logi("Disable VSIM and reset modem since socket disconnected.");
                            }
                        }
                        ExternalSimManager.this.sendExternalSimConnectedEvent(0);
                        logw("Socket disconnected and vsim is disabled.");
                        terminate();
                    }
                } catch (Exception e2) {
                    logw("VsimIoThread Exception.");
                    e2.printStackTrace();
                }
            }
        }

        private void writeBytes(byte[] bArr, int i) throws IOException {
            this.mOutput.write(bArr, 0, i);
        }

        private void writeInt(int i) throws IOException {
            for (int i2 = 0; i2 < 4; i2++) {
                this.mOutput.write((i >> (8 * i2)) & 255);
            }
        }

        public int writeEvent(VsimEvent vsimEvent) {
            return writeEvent(vsimEvent, false);
        }

        public int writeEvent(VsimEvent vsimEvent, boolean z) {
            int i;
            logd("writeEvent Enter, isBigEndian:" + z);
            try {
                synchronized (this) {
                    if (this.mOutput != null) {
                        dumpEvent(vsimEvent);
                        writeInt(vsimEvent.getTransactionId());
                        writeInt(vsimEvent.getMessageId());
                        writeInt(vsimEvent.getSlotBitMask());
                        writeInt(vsimEvent.getDataLen());
                        writeBytes(vsimEvent.getData(), vsimEvent.getDataLen());
                        this.mOutput.flush();
                        i = 0;
                    } else {
                        loge("mOut is null, socket is not setup");
                        i = -1;
                    }
                }
                return i;
            } catch (Exception e) {
                loge("writeEvent Exception");
                e.printStackTrace();
                return -1;
            }
        }

        private int readInt() throws IOException {
            byte[] bArr = new byte[8];
            if (this.mInput.read(bArr, 0, 4) < 0) {
                loge("readInt(), fail to read and throw exception");
                throw new IOException("fail to read");
            }
            return ((bArr[1] & PplMessageManager.Type.INVALID) << 8) | (bArr[3] << 24) | ((bArr[2] & PplMessageManager.Type.INVALID) << 16) | (bArr[0] & PplMessageManager.Type.INVALID);
        }

        private VsimEvent readEvent() throws IOException {
            logd("readEvent Enter");
            int i = readInt();
            int i2 = readInt();
            int i3 = readInt();
            int i4 = readInt();
            logd("readEvent transaction_id: " + i + ", msgId: " + i2 + ", slot_id: " + i3 + ", len: " + i4);
            if (i4 > ExternalSimManager.MAX_VSIM_UICC_CMD_LEN) {
                loge("readEvent(), data_len large than 269");
                throw new IOException("unreasonable data length");
            }
            this.readBuffer = new byte[i4];
            int i5 = 0;
            int i6 = i4;
            do {
                int i7 = this.mInput.read(this.readBuffer, i5, i6);
                if (i7 < 0) {
                    loge("readEvent(), fail to read and throw exception");
                    throw new IOException("fail to read");
                }
                i5 += i7;
                i6 -= i7;
            } while (i6 > 0);
            VsimEvent vsimEvent = new VsimEvent(i, i2, i4, i3);
            vsimEvent.putBytes(this.readBuffer);
            dumpEvent(vsimEvent);
            return vsimEvent;
        }

        private void dumpEvent(VsimEvent vsimEvent) {
            if (ExternalSimManager.ENG) {
                logd("dumpEvent: transaction_id: " + vsimEvent.getTransactionId() + ", message_id:" + vsimEvent.getMessageId() + ", slot_id:" + vsimEvent.getSlotBitMask() + ", data_len:" + vsimEvent.getDataLen() + ", event:" + ExternalSimManager.truncateString(IccUtils.bytesToHexString(vsimEvent.getData())));
                return;
            }
            logd("dumpEvent: transaction_id: " + vsimEvent.getTransactionId() + ", message_id:" + vsimEvent.getMessageId() + ", slot_id:" + vsimEvent.getSlotBitMask() + ", data_len:" + vsimEvent.getDataLen());
        }

        private void logd(String str) {
            Rlog.d(ExternalSimManager.TAG, "[" + this.mName + "] " + str);
        }

        private void logi(String str) {
            Rlog.i(ExternalSimManager.TAG, "[" + this.mName + "] " + str);
        }

        private void logw(String str) {
            Rlog.w(ExternalSimManager.TAG, "[" + this.mName + "] " + str);
        }

        private void loge(String str) {
            Rlog.e(ExternalSimManager.TAG, "[" + this.mName + "] " + str);
        }
    }

    public class VsimIndEventHandler extends Handler {
        public VsimIndEventHandler() {
        }

        protected Integer getCiIndex(Message message) {
            Integer num = new Integer(0);
            if (message != null) {
                if (message.obj != null && (message.obj instanceof Integer)) {
                    return (Integer) message.obj;
                }
                if (message.obj != null && (message.obj instanceof AsyncResult)) {
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.userObj != null && (asyncResult.userObj instanceof Integer)) {
                        return (Integer) asyncResult.userObj;
                    }
                    return num;
                }
                return num;
            }
            return num;
        }

        @Override
        public void handleMessage(Message message) {
            Integer ciIndex = getCiIndex(message);
            if (ciIndex.intValue() < 0 || ciIndex.intValue() >= ExternalSimManager.this.mCi.length) {
                Rlog.e(ExternalSimManager.TAG, "Invalid index : " + ciIndex + " received with event " + message.what);
                return;
            }
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (message.what == 1) {
                if (ExternalSimManager.ENG) {
                    Rlog.d(ExternalSimManager.TAG, "Received EVENT_VSIM_INDICATION...");
                }
                VsimEvent vsimEvent = (VsimEvent) asyncResult.result;
                dumpEvent(vsimEvent);
                Message message2 = new Message();
                message2.obj = vsimEvent;
                ExternalSimManager.this.mEventHandler.sendMessage(message2);
                return;
            }
            Rlog.e(ExternalSimManager.TAG, " Unknown Event " + message.what);
        }

        private void dumpEvent(VsimEvent vsimEvent) {
            if (ExternalSimManager.ENG) {
                Rlog.d(ExternalSimManager.TAG, "dumpEvent: transaction_id: " + vsimEvent.getTransactionId() + ", message_id:" + vsimEvent.getMessageId() + ", slot_id:" + vsimEvent.getSlotBitMask() + ", data_len:" + vsimEvent.getDataLen() + ", event:" + ExternalSimManager.truncateString(IccUtils.bytesToHexString(vsimEvent.getData())));
                return;
            }
            Rlog.d(ExternalSimManager.TAG, "dumpEvent: transaction_id: " + vsimEvent.getTransactionId() + ", message_id:" + vsimEvent.getMessageId() + ", slot_id:" + vsimEvent.getSlotBitMask() + ", data_len:" + vsimEvent.getDataLen());
        }
    }

    public class VsimEvenHandler extends Handler {
        private eventHandlerTread[] mEventHandlingThread;
        private boolean[] mIsMdWaitingResponse;
        private boolean[] mIsWaitingAuthRsp;
        private int[] mNoResponseTimeOut;
        private Timer[] mNoResponseTimer;
        private VsimEvent[] mWaitingEvent;
        private VsimIoThread mVsimAdaptorIo = null;
        private VsimIoThread mVsimRilIo = null;
        private boolean mHasNotifyEnableEvnetToModem = false;
        private boolean mIsSwitchRfSuccessful = false;
        private long mLastDisableEventTime = 0;
        private Runnable mTryResetModemRunnable = new Runnable() {
            @Override
            public void run() {
                MtkUiccController mtkUiccController = (MtkUiccController) UiccController.getInstance();
                if (!mtkUiccController.isAllRadioAvailable()) {
                    VsimEvenHandler.this.postDelayed(VsimEvenHandler.this.mTryResetModemRunnable, 2000L);
                    return;
                }
                RadioManager.getInstance().setSilentRebootPropertyForAllModem("1");
                mtkUiccController.resetRadioForVsim();
                Rlog.i(ExternalSimManager.TAG, "mTryResetModemRunnable reset modem done.");
            }
        };

        public VsimEvenHandler() {
            this.mIsMdWaitingResponse = null;
            this.mWaitingEvent = null;
            this.mNoResponseTimer = null;
            this.mIsWaitingAuthRsp = null;
            this.mNoResponseTimeOut = null;
            this.mEventHandlingThread = null;
            int simCount = TelephonyManager.getDefault().getSimCount();
            this.mIsMdWaitingResponse = new boolean[simCount];
            this.mNoResponseTimer = new Timer[simCount];
            this.mWaitingEvent = new VsimEvent[simCount];
            this.mIsWaitingAuthRsp = new boolean[simCount];
            this.mNoResponseTimeOut = new int[simCount];
            this.mEventHandlingThread = new eventHandlerTread[simCount];
            for (int i = 0; i < simCount; i++) {
                this.mIsMdWaitingResponse[i] = false;
                this.mNoResponseTimer[i] = null;
                this.mWaitingEvent[i] = null;
                this.mIsWaitingAuthRsp[i] = false;
                this.mNoResponseTimeOut[i] = ExternalSimManager.NO_RESPONSE_TIMEOUT_DURATION;
                this.mEventHandlingThread[i] = null;
            }
        }

        @Override
        public void handleMessage(Message message) {
            VsimEvent vsimEvent;
            if (message.obj instanceof AsyncResult) {
                vsimEvent = (VsimEvent) ((AsyncResult) message.obj).userObj;
            } else {
                vsimEvent = (VsimEvent) message.obj;
            }
            int firstSlotId = vsimEvent.getFirstSlotId();
            if (firstSlotId >= 0 && firstSlotId < TelephonyManager.getDefault().getSimCount()) {
                while (this.mEventHandlingThread[firstSlotId] != null && this.mEventHandlingThread[firstSlotId].isWaiting()) {
                    Rlog.d(ExternalSimManager.TAG, "handleMessage[" + firstSlotId + "] thread running, delay 100 ms...");
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                this.mEventHandlingThread[firstSlotId] = new eventHandlerTread(vsimEvent);
                this.mEventHandlingThread[firstSlotId].start();
                return;
            }
            new eventHandlerTread((VsimEvent) message.obj).start();
        }

        private void setDataStream(VsimIoThread vsimIoThread, VsimIoThread vsimIoThread2) {
            this.mVsimAdaptorIo = vsimIoThread;
            this.mVsimRilIo = vsimIoThread2;
            Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler setDataStream done.");
        }

        private int getRspMessageId(int i) {
            switch (i) {
                case 1:
                    return 1001;
                case 2:
                    return 1002;
                case 3:
                    return 1003;
                case 7:
                    return 1007;
                case 8:
                    return 1008;
                case 1004:
                    return 4;
                case 1005:
                case 2001:
                    return 5;
                case 1006:
                    return 6;
                default:
                    Rlog.d(ExternalSimManager.TAG, "getRspMessageId: " + i + "no support.");
                    return -1;
            }
        }

        public class eventHandlerTread extends Thread {
            boolean isWaiting = true;
            VsimEvent mEvent;

            public eventHandlerTread(VsimEvent vsimEvent) {
                this.mEvent = null;
                this.mEvent = vsimEvent;
            }

            public boolean isWaiting() {
                return this.isWaiting;
            }

            public void setWaiting(boolean z) {
                this.isWaiting = z;
            }

            @Override
            public void run() {
                Rlog.d(ExternalSimManager.TAG, "eventHandlerTread[ " + this.mEvent.getFirstSlotId() + "]: run...");
                VsimEvenHandler.this.dispatchCallback(this.mEvent);
                this.isWaiting = false;
            }
        }

        public class TimeOutTimerTask extends TimerTask {
            int mSlotId;

            public TimeOutTimerTask(int i) {
                this.mSlotId = 0;
                this.mSlotId = i;
            }

            @Override
            public void run() {
                synchronized (ExternalSimManager.this.mLock) {
                    if (VsimEvenHandler.this.mWaitingEvent[this.mSlotId] != null) {
                        VsimEvenHandler.this.sendNoResponseError(VsimEvenHandler.this.mWaitingEvent[this.mSlotId]);
                    }
                    Rlog.i(ExternalSimManager.TAG, "TimeOutTimerTask[" + this.mSlotId + "] time out and send response to modem directly.");
                }
            }
        }

        private void sendNoResponseError(VsimEvent vsimEvent) {
            if (this.mIsWaitingAuthRsp[vsimEvent.getFirstSlotId()]) {
                sendRsimAuthProgressEvent(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP);
            }
            if (getMdWaitingFlag(vsimEvent.getFirstSlotId())) {
                setMdWaitingFlag(false, vsimEvent.getFirstSlotId());
                VsimEvent vsimEvent2 = new VsimEvent(vsimEvent.getTransactionId(), getRspMessageId(vsimEvent.getMessageId()), vsimEvent.getSlotBitMask());
                vsimEvent2.putInt(-1);
                vsimEvent2.putInt(2);
                vsimEvent2.putByte(0);
                vsimEvent2.putByte(0);
                ExternalSimManager.this.mCi[vsimEvent.getFirstSlotId()].sendVsimOperation(vsimEvent2.getTransactionId(), vsimEvent2.getMessageId(), vsimEvent2.getInt(), vsimEvent2.getInt(), vsimEvent2.getDataByReadOffest(), null);
            }
        }

        private void sendVsimNotification(int i, int i2, int i3, int i4, Message message) {
            boolean zSendVsimNotification = ExternalSimManager.this.mCi[i].sendVsimNotification(i2, i3, i4, message);
            Rlog.d(ExternalSimManager.TAG, "sendVsimNotification result = " + zSendVsimNotification);
            if (message == null) {
                int i5 = 0;
                while (!zSendVsimNotification && i5 < ExternalSimManager.PLUG_IN_AUTO_RETRY_TIMEOUT) {
                    try {
                        Thread.sleep(2000L);
                        i5 += MtkGsmCdmaPhone.EVENT_IMS_UT_DONE;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    zSendVsimNotification = ExternalSimManager.this.mCi[i].sendVsimNotification(i2, i3, i4, message);
                }
            }
            if (!zSendVsimNotification) {
                Rlog.e(ExternalSimManager.TAG, "sendVsimNotification fail until 40000");
            }
        }

        private void sendPlugOutEvent(VsimEvent vsimEvent) {
            TelephonyManager.getDefault();
            String telephonyProperty = TelephonyManager.getTelephonyProperty(vsimEvent.getFirstSlotId(), "vendor.gsm.external.sim.inserted", "0");
            if ("0".equals(telephonyProperty)) {
                Rlog.d(ExternalSimManager.TAG, "sendPlugOutEvent: " + telephonyProperty);
                return;
            }
            VsimEvent vsimEvent2 = new VsimEvent(vsimEvent.getTransactionId(), 3, vsimEvent.getSlotBitMask());
            vsimEvent2.putInt(3);
            vsimEvent2.putInt(1);
            setMdWaitingFlag(false, vsimEvent.getFirstSlotId());
            sendVsimNotification(vsimEvent.getFirstSlotId(), vsimEvent2.mTransactionId, 3, 1, null);
        }

        private void sendHotPlugEvent(VsimEvent vsimEvent, boolean z) {
            int i;
            if (!z) {
                i = 3;
            } else {
                i = 4;
            }
            sendVsimNotification(vsimEvent.getFirstSlotId(), vsimEvent.getTransactionId(), i, 1, null);
        }

        private int getVsimSlotId(int i) {
            switch (i) {
                case 2:
                    int i2 = SystemProperties.getInt(ExternalSimManager.PREFERED_RSIM_SLOT, -1);
                    if (i2 == -1) {
                        return ExternalSimManager.sPreferedRsimSlot;
                    }
                    return i2;
                case 3:
                    int i3 = SystemProperties.getInt(ExternalSimManager.PREFERED_AKA_SIM_SLOT, -1);
                    if (i3 == -1) {
                        return ExternalSimManager.sPreferedAkaSlot;
                    }
                    return i3;
                default:
                    for (int i4 = 0; i4 < TelephonyManager.getDefault().getSimCount(); i4++) {
                        TelephonyManager.getDefault();
                        String telephonyProperty = TelephonyManager.getTelephonyProperty(i4, "vendor.gsm.external.sim.enabled", "0");
                        if (telephonyProperty != null && telephonyProperty.length() > 0 && !"0".equals(telephonyProperty)) {
                            TelephonyManager.getDefault();
                            String telephonyProperty2 = TelephonyManager.getTelephonyProperty(i4, "vendor.gsm.external.sim.inserted", "0");
                            if (telephonyProperty2 != null && telephonyProperty2.length() > 0 && String.valueOf(i).equals(telephonyProperty2)) {
                                return i4;
                            }
                        }
                    }
                    return -1;
            }
        }

        private void sendRsimAuthProgressEvent(int i) {
            this.mIsSwitchRfSuccessful = false;
            int vsimSlotId = getVsimSlotId(3);
            int vsimSlotId2 = getVsimSlotId(2);
            if (vsimSlotId < 0 || vsimSlotId > TelephonyManager.getDefault().getSimCount() || vsimSlotId2 < 0 || vsimSlotId2 > TelephonyManager.getDefault().getSimCount()) {
                Rlog.d(ExternalSimManager.TAG, "sendRsimAuthProgressEvent aka sim: " + vsimSlotId + ", rsim: " + vsimSlotId2);
                this.mIsSwitchRfSuccessful = true;
                return;
            }
            if (i == 201) {
                this.mIsWaitingAuthRsp[vsimSlotId2] = true;
            } else if (i == 202) {
                this.mIsWaitingAuthRsp[vsimSlotId2] = false;
            }
            Rlog.d(ExternalSimManager.TAG, "sendRsimAuthProgressEvent mIsWaitingAuthRsp[" + vsimSlotId2 + "]: " + this.mIsWaitingAuthRsp[vsimSlotId2]);
            VsimEvent vsimEvent = new VsimEvent(0, 1003, 1 << vsimSlotId);
            vsimEvent.putInt(i);
            vsimEvent.putInt(1);
            Message message = new Message();
            message.obj = vsimEvent;
            message.setTarget(ExternalSimManager.this.mEventHandler);
            sendVsimNotification(vsimEvent.getFirstSlotId(), vsimEvent.mTransactionId, i, 1, message);
            Rlog.d(ExternalSimManager.TAG, "sendRsimAuthProgressEvent eventId: " + i);
            try {
                ExternalSimManager.this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.w(ExternalSimManager.TAG, "sendRsimAuthProgressEvent InterruptedException.");
            }
        }

        private void sendActiveAkaSimEvent(int i, boolean z) {
            int i2;
            Rlog.d(ExternalSimManager.TAG, "sendActiveAkaSimEvent[" + i + "]: " + z);
            int vsimSlotId = getVsimSlotId(2);
            if (vsimSlotId >= 0 && this.mIsWaitingAuthRsp[vsimSlotId] && !z) {
                sendRsimAuthProgressEvent(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP);
            }
            VsimEvent vsimEvent = new VsimEvent(0, 3, 1 << i);
            if (z) {
                int unused = ExternalSimManager.sPreferedAkaSlot = vsimEvent.getFirstSlotId();
                vsimEvent.putInt(6);
                i2 = 6;
            } else {
                int unused2 = ExternalSimManager.sPreferedAkaSlot = -1;
                vsimEvent.putInt(ExternalSimConstants.EVENT_TYPE_RSIM_AUTH_DONE);
                i2 = 203;
            }
            vsimEvent.putInt(3);
            sendVsimNotification(vsimEvent.getFirstSlotId(), vsimEvent.mTransactionId, i2, 3, null);
            if (vsimSlotId >= 0 && this.mIsWaitingAuthRsp[vsimSlotId] && z) {
                sendRsimAuthProgressEvent(201);
            }
        }

        private void setMdWaitingFlag(boolean z, int i) {
            setMdWaitingFlag(z, null, i);
        }

        private void setMdWaitingFlag(boolean z, VsimEvent vsimEvent, int i) {
            Rlog.d(ExternalSimManager.TAG, "setMdWaitingFlag[" + i + "]: " + z);
            this.mIsMdWaitingResponse[i] = z;
            if (z) {
                this.mWaitingEvent[i] = vsimEvent;
                if (this.mNoResponseTimer[i] == null) {
                    this.mNoResponseTimer[i] = new Timer(true);
                }
                TelephonyManager.getDefault();
                String telephonyProperty = TelephonyManager.getTelephonyProperty(vsimEvent != null ? vsimEvent.getFirstSlotId() : -1, "vendor.gsm.external.sim.enabled", "0");
                if ("".equals(telephonyProperty) || "0".equals(telephonyProperty)) {
                    this.mNoResponseTimer[i].schedule(new TimeOutTimerTask(i), 500L);
                    if (System.currentTimeMillis() > this.mLastDisableEventTime + 5000) {
                        postDelayed(this.mTryResetModemRunnable, 2000L);
                    }
                    Rlog.i(ExternalSimManager.TAG, "recevice modem event under vsim disabled state. lastDisableTime:" + this.mLastDisableEventTime);
                    return;
                }
                this.mNoResponseTimer[i].schedule(new TimeOutTimerTask(i), this.mNoResponseTimeOut[i]);
                return;
            }
            if (this.mNoResponseTimer[i] != null) {
                this.mNoResponseTimer[i].cancel();
                this.mNoResponseTimer[i].purge();
                this.mNoResponseTimer[i] = null;
            }
            this.mWaitingEvent[i] = null;
        }

        private boolean getMdWaitingFlag(int i) {
            Rlog.d(ExternalSimManager.TAG, "getMdWaitingFlag[" + i + "]: " + this.mIsMdWaitingResponse[i]);
            return this.mIsMdWaitingResponse[i];
        }

        private boolean isPlatformReady(int i) {
            switch (i) {
                case 1:
                    return ((MtkSubscriptionController) SubscriptionController.getInstance()).isReady();
                case 2:
                    return true ^ ((MtkProxyController) ProxyController.getInstance()).isCapabilitySwitching();
                case 3:
                    return ((MtkUiccController) UiccController.getInstance()).isAllRadioAvailable();
                default:
                    Rlog.d(ExternalSimManager.TAG, "isPlatformReady invalid category: " + i);
                    return true;
            }
        }

        private int retryIfPlatformNotReady(VsimEvent vsimEvent, int i) {
            boolean zIsPlatformReady;
            boolean zIsPlatformReady2 = isPlatformReady(i);
            Rlog.d(ExternalSimManager.TAG, "retryIfPlatformNotReady category= " + i + ", isReady= " + zIsPlatformReady2);
            if (!zIsPlatformReady2) {
                int i2 = 0;
                do {
                    try {
                        Thread.sleep(2000L);
                        i2 += MtkGsmCdmaPhone.EVENT_IMS_UT_DONE;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    zIsPlatformReady = isPlatformReady(i);
                    if (zIsPlatformReady) {
                        break;
                    }
                } while (i2 < ExternalSimManager.PLUG_IN_AUTO_RETRY_TIMEOUT);
                zIsPlatformReady2 = zIsPlatformReady;
            }
            if (zIsPlatformReady2) {
                return 0;
            }
            Rlog.d(ExternalSimManager.TAG, "retryIfPlatformNotReady return not ready");
            return -2;
        }

        private int retryIfRadioUnavailable(VsimEvent vsimEvent) {
            return retryIfPlatformNotReady(vsimEvent, 3);
        }

        private int retryIfSubNotReady(VsimEvent vsimEvent) {
            return retryIfPlatformNotReady(vsimEvent, 1);
        }

        private int retryIfCapabilitySwitching(VsimEvent vsimEvent) {
            return retryIfPlatformNotReady(vsimEvent, 2);
        }

        private void changeRadioSetting(boolean z) {
            int simCount = TelephonyManager.getDefault().getSimCount();
            if (ExternalSimManager.isNonDsdaRemoteSimSupport() && simCount > 2) {
                int i = SystemProperties.getInt(ExternalSimManager.PREFERED_RSIM_SLOT, -1);
                int i2 = SystemProperties.getInt(ExternalSimManager.PREFERED_AKA_SIM_SLOT, -1);
                for (int i3 = 0; i3 < simCount; i3++) {
                    if (-1 != i && i3 != i && -1 != i2 && i3 != i2) {
                        int subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(i3);
                        ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                        if (iTelephonyAsInterface == null) {
                            Rlog.d(ExternalSimManager.TAG, "telephony is null");
                        } else if (!z) {
                            try {
                                if (iTelephonyAsInterface.isRadioOnForSubscriber(subIdUsingPhoneId, ExternalSimManager.this.mContext.getOpPackageName())) {
                                    ExternalSimManager.this.mUserRadioOn = true;
                                    iTelephonyAsInterface.setRadioForSubscriber(subIdUsingPhoneId, false);
                                    Rlog.i(ExternalSimManager.TAG, "changeRadioSetting trun off radio subId:" + subIdUsingPhoneId);
                                } else if (true == z && ExternalSimManager.this.mUserRadioOn) {
                                    ExternalSimManager.this.mUserRadioOn = false;
                                    iTelephonyAsInterface.setRadioForSubscriber(subIdUsingPhoneId, true);
                                    Rlog.i(ExternalSimManager.TAG, "changeRadioSetting trun on radio subId:" + subIdUsingPhoneId);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        private void handleEventRequest(int i, VsimEvent vsimEvent) {
            int i2;
            int iRetryIfRadioUnavailable;
            int iRetryIfCapabilitySwitching;
            Rlog.i(ExternalSimManager.TAG, "VsimEvenHandler eventHandlerByType: type[" + i + "] start");
            int firstSlotId = vsimEvent.getFirstSlotId();
            int i3 = vsimEvent.getInt();
            Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler First slotId:" + firstSlotId + ", simType:" + i3);
            int i4 = 0;
            if (firstSlotId >= 0 && firstSlotId < TelephonyManager.getDefault().getSimCount() && !((MtkUiccController) UiccController.getInstance()).ignoreGetSimStatus()) {
                if (i == 204) {
                    sendVsimNotification(firstSlotId, vsimEvent.mTransactionId, i, i3, null);
                    Rlog.i(ExternalSimManager.TAG, "VsimEvenHandler eventHandlerByType: type[" + i + "] end");
                    return;
                }
                switch (i) {
                    case 1:
                        iRetryIfRadioUnavailable = retryIfRadioUnavailable(vsimEvent);
                        if (iRetryIfRadioUnavailable >= 0) {
                            int iRetryIfSubNotReady = retryIfSubNotReady(vsimEvent);
                            if (iRetryIfSubNotReady >= 0) {
                                MtkSubscriptionController mtkSubscriptionController = (MtkSubscriptionController) SubscriptionController.getInstance();
                                int subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(firstSlotId);
                                if (i3 == 2) {
                                    mtkSubscriptionController.setDefaultDataSubIdWithoutCapabilitySwitch(subIdUsingPhoneId);
                                    if (ExternalSimManager.isNonDsdaRemoteSimSupport()) {
                                        int unused = ExternalSimManager.sPreferedRsimSlot = firstSlotId;
                                    }
                                    Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler set default data to subId: " + subIdUsingPhoneId);
                                }
                                if (ExternalSimManager.isSupportVsimHotPlugOut() && !ExternalSimManager.isNonDsdaRemoteSimSupport()) {
                                    sendHotPlugEvent(vsimEvent, false);
                                }
                                sendVsimNotification(firstSlotId, vsimEvent.mTransactionId, i, i3, null);
                            }
                            iRetryIfRadioUnavailable = iRetryIfSubNotReady;
                        }
                        i2 = -1;
                        break;
                    case 2:
                        TelephonyManager.getDefault();
                        String telephonyProperty = TelephonyManager.getTelephonyProperty(firstSlotId, "vendor.gsm.external.sim.enabled", "0");
                        if (telephonyProperty == null || telephonyProperty.length() == 0 || "0".equals(telephonyProperty)) {
                            Rlog.w(ExternalSimManager.TAG, "VsimEvenHandler didn't not enabled before.");
                            iRetryIfRadioUnavailable = 0;
                            i2 = -1;
                        } else {
                            iRetryIfCapabilitySwitching = retryIfCapabilitySwitching(vsimEvent);
                            if (iRetryIfCapabilitySwitching >= 0) {
                                if (this.mWaitingEvent[firstSlotId] != null) {
                                    sendNoResponseError(this.mWaitingEvent[firstSlotId]);
                                }
                                sendPlugOutEvent(vsimEvent);
                                if (ExternalSimManager.isNonDsdaRemoteSimSupport() || ExternalSimManager.isSupportVsimHotPlugOut()) {
                                    SubscriptionController subscriptionController = SubscriptionController.getInstance();
                                    IccCardConstants.State state = IccCardConstants.State.NOT_READY;
                                    do {
                                        try {
                                            Thread.sleep(200L);
                                            i4 += 200;
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        IccCardConstants.State stateIntToState = IccCardConstants.State.intToState(subscriptionController.getSimStateForSlotIndex(firstSlotId));
                                        if (stateIntToState == IccCardConstants.State.ABSENT || stateIntToState == IccCardConstants.State.NOT_READY || stateIntToState == IccCardConstants.State.UNKNOWN) {
                                        }
                                        Rlog.i(ExternalSimManager.TAG, "VsimEvenHandler DISABLE_EXTERNAL_SIM state: " + stateIntToState);
                                    } while (i4 < ExternalSimManager.SIM_STATE_RETRY_DURATION);
                                    Rlog.i(ExternalSimManager.TAG, "VsimEvenHandler DISABLE_EXTERNAL_SIM state: " + stateIntToState);
                                }
                                if (getVsimSlotId(2) == firstSlotId) {
                                    int unused2 = ExternalSimManager.sPreferedRsimSlot = -1;
                                    int unused3 = ExternalSimManager.sPreferedAkaSlot = -1;
                                    SystemProperties.set("vendor.gsm.disable.sim.dialog", "0");
                                }
                                sendVsimNotification(firstSlotId, vsimEvent.mTransactionId, i, i3, null);
                                this.mLastDisableEventTime = System.currentTimeMillis();
                                if (ExternalSimManager.isNonDsdaRemoteSimSupport() || ExternalSimManager.isSupportVsimHotPlugOut()) {
                                    Rlog.d(ExternalSimManager.TAG, "Disable VSIM without reset modem, sim switch:" + ((MtkSubscriptionController) SubscriptionController.getInstance()).setDefaultDataSubIdWithResult(SubscriptionManager.getDefaultDataSubscriptionId()));
                                } else {
                                    ExternalSimManager.this.waitRildSetDisabledProperty(firstSlotId);
                                    RadioManager.getInstance().setSilentRebootPropertyForAllModem("1");
                                    ((MtkUiccController) UiccController.getInstance()).resetRadioForVsim();
                                }
                                if (ExternalSimManager.isSupportVsimHotPlugOut() && !ExternalSimManager.isNonDsdaRemoteSimSupport()) {
                                    sendHotPlugEvent(vsimEvent, true);
                                }
                            }
                            i2 = -1;
                            iRetryIfRadioUnavailable = iRetryIfCapabilitySwitching;
                        }
                        break;
                    case 3:
                        if (this.mWaitingEvent[firstSlotId] != null) {
                            sendNoResponseError(this.mWaitingEvent[firstSlotId]);
                        }
                        sendVsimNotification(firstSlotId, vsimEvent.mTransactionId, i, i3, null);
                        iRetryIfRadioUnavailable = 0;
                        i2 = -1;
                        break;
                    case 4:
                        iRetryIfCapabilitySwitching = retryIfCapabilitySwitching(vsimEvent);
                        if (iRetryIfCapabilitySwitching >= 0) {
                            if (ExternalSimManager.isNonDsdaRemoteSimSupport() || ExternalSimManager.isSupportVsimHotPlugOut()) {
                                SubscriptionController subscriptionController2 = SubscriptionController.getInstance();
                                IccCardConstants.State state2 = IccCardConstants.State.NOT_READY;
                                int i5 = 0;
                                do {
                                    try {
                                        Thread.sleep(200L);
                                        i5 += 200;
                                    } catch (InterruptedException e2) {
                                        e2.printStackTrace();
                                    }
                                    IccCardConstants.State stateIntToState2 = IccCardConstants.State.intToState(subscriptionController2.getSimStateForSlotIndex(firstSlotId));
                                    if (stateIntToState2 == IccCardConstants.State.ABSENT || stateIntToState2 == IccCardConstants.State.NOT_READY || stateIntToState2 == IccCardConstants.State.UNKNOWN) {
                                    }
                                    Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler REQUEST_TYPE_PLUG_IN state: " + stateIntToState2);
                                } while (i5 < ExternalSimManager.SIM_STATE_RETRY_DURATION);
                                Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler REQUEST_TYPE_PLUG_IN state: " + stateIntToState2);
                            }
                            MtkSubscriptionController mtkSubscriptionController2 = (MtkSubscriptionController) SubscriptionController.getInstance();
                            if (firstSlotId != RadioCapabilitySwitchUtil.getMainCapabilityPhoneId() && i3 != 1) {
                                Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler need to do capablity switch");
                                if (mtkSubscriptionController2.isReady()) {
                                    iRetryIfRadioUnavailable = mtkSubscriptionController2.setDefaultDataSubIdWithResult(MtkSubscriptionManager.getSubIdUsingPhoneId(firstSlotId)) ? 0 : -1;
                                    i2 = -1;
                                }
                                i2 = -1;
                                iRetryIfRadioUnavailable = -2;
                                break;
                            } else {
                                Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler no need to do capablity switch");
                                sendVsimNotification(firstSlotId, vsimEvent.mTransactionId, i, i3, null);
                                if (ExternalSimManager.isNonDsdaRemoteSimSupport() || ExternalSimManager.isSupportVsimHotPlugOut()) {
                                    Rlog.d(ExternalSimManager.TAG, "VSIM allow to enable without reset modem");
                                } else {
                                    RadioManager.getInstance().setSilentRebootPropertyForAllModem("1");
                                    ((MtkUiccController) UiccController.getInstance()).resetRadioForVsim();
                                }
                            }
                        }
                        i2 = -1;
                        iRetryIfRadioUnavailable = iRetryIfCapabilitySwitching;
                        break;
                    case 5:
                        sendVsimNotification(firstSlotId, vsimEvent.mTransactionId, i, i3, null);
                        iRetryIfRadioUnavailable = 0;
                        i2 = -1;
                        break;
                    case 6:
                        if (i3 == 2) {
                            int mainCapabilityPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
                            MtkSubscriptionController mtkSubscriptionController3 = (MtkSubscriptionController) SubscriptionController.getInstance();
                            int subIdUsingPhoneId2 = MtkSubscriptionManager.getSubIdUsingPhoneId(firstSlotId);
                            Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler REQUEST_TYPE_SET_MAPPING_INFO:mainPhoneId= " + mainCapabilityPhoneId + ", subId= " + subIdUsingPhoneId2);
                            iRetryIfCapabilitySwitching = retryIfCapabilitySwitching(vsimEvent);
                            if (iRetryIfCapabilitySwitching >= 0) {
                                try {
                                    Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler isCapabilitySwitching: false.");
                                    ExternalSimManager.this.mSetCapabilityDone = 1;
                                    int unused4 = ExternalSimManager.sPreferedRsimSlot = firstSlotId;
                                    Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler before setDefaultDataSubIdWithResult.");
                                } catch (InterruptedException e3) {
                                    Rlog.w(ExternalSimManager.TAG, "VsimEvenHandler InterruptedException.");
                                }
                                if (!mtkSubscriptionController3.setDefaultDataSubIdWithResult(subIdUsingPhoneId2)) {
                                    ExternalSimManager.this.mSetCapabilityDone = 0;
                                    int unused5 = ExternalSimManager.sPreferedRsimSlot = -1;
                                    i2 = -1;
                                    iRetryIfRadioUnavailable = -2;
                                } else {
                                    if (ExternalSimManager.this.mSetCapabilityDone == 1) {
                                        Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler before mLock.wait");
                                        ExternalSimManager.this.mLock.wait();
                                    }
                                    Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler after mLock.wait");
                                    if (ExternalSimManager.this.mSetCapabilityDone == 3) {
                                        int unused6 = ExternalSimManager.sPreferedRsimSlot = -1;
                                        iRetryIfCapabilitySwitching = -2;
                                    } else {
                                        sendVsimNotification(firstSlotId, vsimEvent.mTransactionId, i, i3, null);
                                    }
                                    ExternalSimManager.this.mSetCapabilityDone = 0;
                                    iRetryIfRadioUnavailable = iRetryIfCapabilitySwitching;
                                    i2 = -1;
                                }
                            }
                            i2 = -1;
                            iRetryIfRadioUnavailable = iRetryIfCapabilitySwitching;
                            break;
                        } else if (i3 == 3) {
                            if (firstSlotId >= 0 && firstSlotId < TelephonyManager.getDefault().getSimCount()) {
                                sendActiveAkaSimEvent(firstSlotId, true);
                                iRetryIfRadioUnavailable = 0;
                                i2 = -1;
                            } else {
                                i2 = SystemProperties.getInt(ExternalSimManager.PREFERED_AKA_SIM_SLOT, -1);
                                if (i2 != -1) {
                                    sendActiveAkaSimEvent(i2, false);
                                } else {
                                    i2 = -1;
                                }
                                Rlog.d(ExternalSimManager.TAG, "Reset PREFERED_AKA_SIM_SLOT");
                                iRetryIfRadioUnavailable = 0;
                            }
                            break;
                        }
                        break;
                    case 7:
                        if (i3 == 3) {
                            int i6 = SystemProperties.getInt(ExternalSimManager.PREFERED_AKA_SIM_SLOT, -1);
                            if (i6 != -1) {
                                sendActiveAkaSimEvent(i6, false);
                            }
                            Rlog.d(ExternalSimManager.TAG, "Reset PREFERED_AKA_SIM_SLOT");
                        }
                        iRetryIfRadioUnavailable = 0;
                        i2 = -1;
                        break;
                    case 8:
                        this.mNoResponseTimeOut[firstSlotId] = i3 * 1000;
                        sendVsimNotification(firstSlotId, vsimEvent.mTransactionId, i, i3, null);
                        iRetryIfRadioUnavailable = 0;
                        i2 = -1;
                        break;
                    case 9:
                        SystemProperties.set("vendor.gsm.disable.sim.dialog", "1");
                        iRetryIfRadioUnavailable = 0;
                        i2 = -1;
                        break;
                    case 10:
                        SystemProperties.set("vendor.gsm.disable.sim.dialog", "0");
                        iRetryIfRadioUnavailable = 0;
                        i2 = -1;
                        break;
                    case 11:
                        sendVsimNotification(firstSlotId, vsimEvent.mTransactionId, i, i3, null);
                        iRetryIfRadioUnavailable = 0;
                        i2 = -1;
                        break;
                    default:
                        Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler invalid event id.");
                        i2 = -1;
                        iRetryIfRadioUnavailable = -1;
                        break;
                }
            } else {
                i2 = -1;
                iRetryIfRadioUnavailable = -1;
            }
            VsimEvent vsimEvent2 = new VsimEvent(vsimEvent.getTransactionId(), 1003, i2 == -1 ? vsimEvent.getSlotBitMask() : 1 << i2);
            vsimEvent2.putInt(iRetryIfRadioUnavailable);
            if (this.mVsimAdaptorIo != null) {
                this.mVsimAdaptorIo.writeEvent(vsimEvent2);
            }
            Rlog.i(ExternalSimManager.TAG, "VsimEvenHandler eventHandlerByType: type[" + i + "] end");
        }

        private void handleGetPlatformCapability(VsimEvent vsimEvent) {
            vsimEvent.getInt();
            int i = vsimEvent.getInt();
            VsimEvent vsimEvent2 = new VsimEvent(vsimEvent.getTransactionId(), 1002, vsimEvent.getSlotBitMask());
            if (((MtkSubscriptionController) SubscriptionController.getInstance()).isReady()) {
                vsimEvent2.putInt(0);
            } else {
                vsimEvent2.putInt(-2);
            }
            TelephonyManager.MultiSimVariants multiSimConfiguration = TelephonyManager.getDefault().getMultiSimConfiguration();
            if (multiSimConfiguration == TelephonyManager.MultiSimVariants.DSDS) {
                vsimEvent2.putInt(1);
            } else if (multiSimConfiguration == TelephonyManager.MultiSimVariants.DSDA) {
                vsimEvent2.putInt(2);
            } else if (multiSimConfiguration == TelephonyManager.MultiSimVariants.TSTS) {
                vsimEvent2.putInt(3);
            } else {
                vsimEvent2.putInt(0);
            }
            if (SystemProperties.getInt("ro.vendor.mtk_external_sim_support", 0) > 0) {
                vsimEvent2.putInt(ExternalSimManager.isNonDsdaRemoteSimSupport() ? 7 : 3);
            } else {
                vsimEvent2.putInt(0);
            }
            int simCount = TelephonyManager.getDefault().getSimCount();
            Rlog.d(ExternalSimManager.TAG, "handleGetPlatformCapability simType: " + i + ", simCount: " + simCount);
            if (i == 1) {
                int i2 = SystemProperties.getInt(ExternalSimManager.PREFERED_RSIM_SLOT, -1);
                if (i2 == -1) {
                    vsimEvent2.putInt((1 << simCount) - 1);
                } else if (i2 == 1 || i2 == 4) {
                    vsimEvent2.putInt(2);
                } else if (i2 == 2) {
                    vsimEvent2.putInt(1);
                }
            } else if (multiSimConfiguration == TelephonyManager.MultiSimVariants.DSDA) {
                int i3 = 0;
                int i4 = 0;
                for (int i5 = 0; i5 < simCount; i5++) {
                    String str = SystemProperties.get(ExternalSimManager.PROPERTY_RIL_FULL_UICC_TYPE[i5], "");
                    if (!str.equals("")) {
                        i4 |= 1 << i5;
                    }
                    if (str.contains("CSIM") || str.contains("RUIM") || str.contains("UIM")) {
                        i3 |= 1 << i5;
                    }
                }
                Rlog.d(ExternalSimManager.TAG, "handleGetPlatformCapability isCdmaCard: " + i3 + ", isHasCard: " + i4);
                if (i4 == 0 || i3 == 0) {
                    vsimEvent2.putInt(0);
                } else {
                    vsimEvent2.putInt(((1 << simCount) - 1) ^ i3);
                }
            } else if (ExternalSimManager.isNonDsdaRemoteSimSupport()) {
                if (multiSimConfiguration == TelephonyManager.MultiSimVariants.DSDS) {
                    vsimEvent2.putInt((1 << simCount) - 1);
                } else if (multiSimConfiguration == TelephonyManager.MultiSimVariants.TSTS) {
                    int i6 = SystemProperties.getInt("ro.vendor.mtk_external_sim_only_slots", 0);
                    if (i6 != 0) {
                        vsimEvent2.putInt(i6);
                    } else {
                        vsimEvent2.putInt((1 << simCount) - 1);
                    }
                }
            } else {
                vsimEvent2.putInt(0);
            }
            this.mVsimAdaptorIo.writeEvent(vsimEvent2);
        }

        private void handleServiceStateRequest(VsimEvent vsimEvent) {
            int i;
            int dataRejectCause;
            VsimEvent vsimEvent2 = new VsimEvent(vsimEvent.getTransactionId(), 1007, vsimEvent.getSlotBitMask());
            int i2 = -1;
            if (((MtkSubscriptionController) SubscriptionController.getInstance()).isReady()) {
                int subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(vsimEvent.getFirstSlotId());
                MtkServiceState serviceStateForSubscriber = TelephonyManager.getDefault().getServiceStateForSubscriber(subIdUsingPhoneId);
                if (serviceStateForSubscriber != null) {
                    MtkServiceState mtkServiceState = serviceStateForSubscriber;
                    Rlog.d(ExternalSimManager.TAG, "handleServiceStateRequest subId: " + subIdUsingPhoneId + ", ss = " + mtkServiceState.toString());
                    int voiceRejectCause = mtkServiceState.getVoiceRejectCause();
                    dataRejectCause = mtkServiceState.getDataRejectCause();
                    i2 = voiceRejectCause;
                } else {
                    dataRejectCause = -1;
                }
                i = 0;
            } else {
                i = -2;
                dataRejectCause = -1;
            }
            vsimEvent2.putInt(i);
            vsimEvent2.putInt(i2);
            vsimEvent2.putInt(dataRejectCause);
            this.mVsimAdaptorIo.writeEvent(vsimEvent2);
        }

        private Object getLock(int i) {
            switch (i) {
                case 3:
                case 1003:
                case 1009:
                case ExternalSimConstants.MSG_ID_UICC_AUTHENTICATION_ABORT_IND:
                case 2001:
                case ExternalSimConstants.MSG_ID_CAPABILITY_SWITCH_DONE:
                    break;
            }
            return ExternalSimManager.this.mLock;
        }

        private void dispatchCallback(VsimEvent vsimEvent) {
            synchronized (getLock(vsimEvent.getMessageId())) {
                boolean z = false;
                if (this.mEventHandlingThread[vsimEvent.getFirstSlotId()] != null) {
                    this.mEventHandlingThread[vsimEvent.getFirstSlotId()].setWaiting(false);
                }
                int messageId = vsimEvent.getMessageId();
                Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler handleMessage[" + vsimEvent.getFirstSlotId() + "]: msgId[" + messageId + "] start");
                if (messageId != 5001) {
                    switch (messageId) {
                        case 1:
                            break;
                        case 2:
                            handleGetPlatformCapability(vsimEvent);
                            break;
                        case 3:
                            handleEventRequest(vsimEvent.getInt(), vsimEvent);
                            break;
                        case 4:
                            if (getMdWaitingFlag(vsimEvent.getFirstSlotId())) {
                                setMdWaitingFlag(false, vsimEvent.getFirstSlotId());
                                ExternalSimManager.this.mCi[vsimEvent.getFirstSlotId()].sendVsimOperation(vsimEvent.getTransactionId(), vsimEvent.getMessageId(), vsimEvent.getInt(), vsimEvent.getInt(), vsimEvent.getDataByReadOffest(), null);
                            }
                            break;
                        case 5:
                            if (this.mIsWaitingAuthRsp[vsimEvent.getFirstSlotId()]) {
                                sendRsimAuthProgressEvent(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP);
                            }
                            if (getMdWaitingFlag(vsimEvent.getFirstSlotId())) {
                                setMdWaitingFlag(false, vsimEvent.getFirstSlotId());
                                TelephonyManager.getDefault();
                                String telephonyProperty = TelephonyManager.getTelephonyProperty(vsimEvent.getFirstSlotId(), "vendor.gsm.external.sim.inserted", "0");
                                if (telephonyProperty != null && telephonyProperty.length() > 0 && !"0".equals(telephonyProperty)) {
                                    ExternalSimManager.this.mCi[vsimEvent.getFirstSlotId()].sendVsimOperation(vsimEvent.getTransactionId(), vsimEvent.getMessageId(), vsimEvent.getInt(), vsimEvent.getInt(), vsimEvent.getDataByReadOffest(), null);
                                } else {
                                    Rlog.d(ExternalSimManager.TAG, "ignore UICC_APDU_RESPONSE since vsim plug out.");
                                }
                            }
                            break;
                        case 6:
                            break;
                        case 7:
                            handleServiceStateRequest(vsimEvent);
                            break;
                        case 8:
                            break;
                        default:
                            switch (messageId) {
                                case 1003:
                                    int i = vsimEvent.getInt();
                                    if (i == 201 || i == 202) {
                                        if (vsimEvent.getInt() >= 0) {
                                            z = true;
                                        }
                                        this.mIsSwitchRfSuccessful = z;
                                        ExternalSimManager.this.mLock.notifyAll();
                                    }
                                    break;
                                case 1004:
                                    setMdWaitingFlag(true, vsimEvent, vsimEvent.getFirstSlotId());
                                    TelephonyManager.getDefault();
                                    String telephonyProperty2 = TelephonyManager.getTelephonyProperty(vsimEvent.getFirstSlotId(), "vendor.gsm.external.sim.inserted", "0");
                                    if (this.mVsimAdaptorIo != null && telephonyProperty2 != null && telephonyProperty2.length() > 0 && !"0".equals(telephonyProperty2)) {
                                        this.mVsimAdaptorIo.writeEvent(vsimEvent);
                                    }
                                    break;
                                case 1005:
                                    setMdWaitingFlag(true, vsimEvent, vsimEvent.getFirstSlotId());
                                    TelephonyManager.getDefault();
                                    String telephonyProperty3 = TelephonyManager.getTelephonyProperty(vsimEvent.getFirstSlotId(), "vendor.gsm.external.sim.inserted", "0");
                                    if (this.mVsimAdaptorIo != null && telephonyProperty3 != null && telephonyProperty3.length() > 0 && !"0".equals(telephonyProperty3)) {
                                        this.mVsimAdaptorIo.writeEvent(vsimEvent);
                                    } else {
                                        Rlog.d(ExternalSimManager.TAG, "ignore UICC_APDU_REQUEST since vsim plug out.");
                                        sendNoResponseError(vsimEvent);
                                    }
                                    break;
                                case 1006:
                                    TelephonyManager.getDefault();
                                    String telephonyProperty4 = TelephonyManager.getTelephonyProperty(vsimEvent.getFirstSlotId(), "vendor.gsm.external.sim.inserted", "0");
                                    if (this.mVsimAdaptorIo != null && telephonyProperty4 != null && telephonyProperty4.length() > 0 && !"0".equals(telephonyProperty4)) {
                                        this.mVsimAdaptorIo.writeEvent(vsimEvent);
                                    }
                                    break;
                                default:
                                    switch (messageId) {
                                        case 1009:
                                            TelephonyManager.getDefault();
                                            String telephonyProperty5 = TelephonyManager.getTelephonyProperty(vsimEvent.getFirstSlotId(), "vendor.gsm.external.sim.inserted", "0");
                                            if (telephonyProperty5 != null && telephonyProperty5.length() > 0 && !"0".equals(telephonyProperty5)) {
                                                this.mVsimAdaptorIo.writeEvent(vsimEvent);
                                            }
                                            break;
                                        case ExternalSimConstants.MSG_ID_UICC_AUTHENTICATION_ABORT_IND:
                                            sendNoResponseError(new VsimEvent(0, 1005, 1 << getVsimSlotId(2)));
                                            TelephonyManager.getDefault();
                                            String telephonyProperty6 = TelephonyManager.getTelephonyProperty(1 << getVsimSlotId(2), "vendor.gsm.external.sim.inserted", "0");
                                            if (telephonyProperty6 != null && telephonyProperty6.length() > 0 && !"0".equals(telephonyProperty6)) {
                                                this.mVsimAdaptorIo.writeEvent(vsimEvent);
                                            }
                                            break;
                                        default:
                                            switch (messageId) {
                                                case 2001:
                                                    setMdWaitingFlag(true, vsimEvent, vsimEvent.getFirstSlotId());
                                                    this.mIsWaitingAuthRsp[vsimEvent.getFirstSlotId()] = true;
                                                    sendRsimAuthProgressEvent(201);
                                                    vsimEvent.mMessageId = 1005;
                                                    TelephonyManager.getDefault();
                                                    String telephonyProperty7 = TelephonyManager.getTelephonyProperty(vsimEvent.getFirstSlotId(), "vendor.gsm.external.sim.inserted", "0");
                                                    if (this.mIsSwitchRfSuccessful) {
                                                        if (telephonyProperty7 != null && telephonyProperty7.length() > 0 && !"0".equals(telephonyProperty7)) {
                                                            this.mVsimAdaptorIo.writeEvent(vsimEvent);
                                                        }
                                                    } else {
                                                        sendNoResponseError(vsimEvent);
                                                    }
                                                    break;
                                                case ExternalSimConstants.MSG_ID_CAPABILITY_SWITCH_DONE:
                                                    ExternalSimManager.this.mLock.notifyAll();
                                                    break;
                                                default:
                                                    Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler handleMessage: default");
                                                    break;
                                            }
                                            break;
                                    }
                                    break;
                            }
                            break;
                    }
                } else {
                    ExternalSimManager.this.mCi[vsimEvent.getFirstSlotId()].sendVsimOperation(vsimEvent.getTransactionId(), vsimEvent.getMessageId(), vsimEvent.getInt(), vsimEvent.getInt(), vsimEvent.getDataByReadOffest(), null);
                }
                Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler handleMessage[" + vsimEvent.getFirstSlotId() + "]: msgId[" + messageId + "] end");
            }
        }
    }
}
