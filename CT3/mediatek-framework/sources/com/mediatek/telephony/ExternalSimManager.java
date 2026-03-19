package com.mediatek.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.mmsdk.BaseParameters;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExternalSimManager {
    private static final int NOT_SUPPORT = 0;
    private static final byte NO_RESPONSE_STATUS_WORD_BYTE1 = 0;
    private static final byte NO_RESPONSE_STATUS_WORD_BYTE2 = 0;
    private static final int NO_RESPONSE_TIMEOUT_DURATION = 10000;
    private static final int PLUG_IN_AUTO_RETRY_TIMEOUT = 10000;
    private static final int SOCKET_OPEN_RETRY_MILLIS = 4000;
    private static final int SUPPORT_VERSION = 2;
    private static final String TAG = "ExternalSimMgr";
    private static final int TRY_RESET_MODEM_DURATION = 1000;
    private boolean isIpoShutdown;
    private boolean isMdWaitingResponse;
    private VsimEvenHandler mEventHandler;
    private int mRetryCounter;
    private VsimIoThread mRilIoThread;
    private final BroadcastReceiver sReceiver;
    private static boolean PLUG_IN_AUTO_RETRY = true;
    private static ExternalSimManager sInstance = null;
    static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"gsm.ril.fulluicctype", "gsm.ril.fulluicctype.2", "gsm.ril.fulluicctype.3", "gsm.ril.fulluicctype.4"};

    public ExternalSimManager() {
        this.mRetryCounter = 0;
        this.mEventHandler = null;
        this.mRilIoThread = null;
        this.isMdWaitingResponse = false;
        this.isIpoShutdown = false;
        this.sReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Rlog.d(ExternalSimManager.TAG, "[Receiver]+");
                String action = intent.getAction();
                Rlog.d(ExternalSimManager.TAG, "Action: " + action);
                if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                    ExternalSimManager.this.isIpoShutdown = true;
                    SystemProperties.set("gsm.external.sim.enabled", "");
                    SystemProperties.set("gsm.external.sim.inserted", "");
                }
                Rlog.d(ExternalSimManager.TAG, "[Receiver]-");
            }
        };
        Rlog.d(TAG, "construtor 0 parameter is called - done");
    }

    private ExternalSimManager(Context context) {
        this.mRetryCounter = 0;
        this.mEventHandler = null;
        this.mRilIoThread = null;
        this.isMdWaitingResponse = false;
        this.isIpoShutdown = false;
        this.sReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Rlog.d(ExternalSimManager.TAG, "[Receiver]+");
                String action = intent.getAction();
                Rlog.d(ExternalSimManager.TAG, "Action: " + action);
                if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                    ExternalSimManager.this.isIpoShutdown = true;
                    SystemProperties.set("gsm.external.sim.enabled", "");
                    SystemProperties.set("gsm.external.sim.inserted", "");
                }
                Rlog.d(ExternalSimManager.TAG, "[Receiver]-");
            }
        };
        Rlog.d(TAG, "construtor 1 parameter is called - start");
        for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
            TelephonyManager.getDefault();
            String persist = TelephonyManager.getTelephonyProperty(i, "persist.radio.external.sim", BaseParameters.FEATURE_MASK_3DNR_OFF);
            if (persist != null && persist.length() > 0 && !BaseParameters.FEATURE_MASK_3DNR_OFF.equals(persist)) {
                TelephonyManager.getDefault();
                TelephonyManager.setTelephonyProperty(i, "gsm.external.sim.enabled", BaseParameters.FEATURE_MASK_3DNR_ON);
            }
        }
        this.mEventHandler = new VsimEvenHandler();
        new Thread() {
            @Override
            public void run() throws Throwable {
                ServerTask server = ExternalSimManager.this.new ServerTask();
                server.listenConnection(ExternalSimManager.this.mEventHandler);
            }
        }.start();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN_IPO");
        context.registerReceiver(this.sReceiver, intentFilter);
        Rlog.d(TAG, "construtor is called - end");
    }

    public static ExternalSimManager getDefault(Context context) {
        Rlog.i(TAG, "getDefault()");
        if (sInstance == null) {
            sInstance = new ExternalSimManager(context);
        }
        return sInstance;
    }

    private static ITelephonyEx getITelephonyEx() {
        return ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
    }

    public boolean initializeService(byte[] userData) {
        Rlog.d(TAG, "initializeService() - start");
        if (SystemProperties.getInt("ro.mtk_external_sim_support", 0) == 0) {
            Rlog.d(TAG, "initializeService() - mtk_external_sim_support didn't support");
            return false;
        }
        try {
            getITelephonyEx().initializeService("osi");
            Rlog.d(TAG, "initialize() - end");
            return true;
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean finalizeService(byte[] userData) {
        Rlog.d(TAG, "finalizeService() - start");
        if (SystemProperties.getInt("ro.mtk_external_sim_support", 0) == 0) {
            Rlog.d(TAG, "initializeService() - mtk_external_sim_support didn't support");
            return false;
        }
        try {
            getITelephonyEx().finalizeService("osi");
            Rlog.d(TAG, "finalizeService() - end");
            return true;
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public class ServerTask {
        public static final String HOST_NAME = "vsim-adaptor";
        private VsimIoThread ioThread = null;

        public ServerTask() {
        }

        public void listenConnection(VsimEvenHandler eventHandler) throws Throwable {
            Rlog.d(ExternalSimManager.TAG, "listenConnection() - start");
            LocalServerSocket serverSocket = null;
            ExecutorService threadExecutor = Executors.newCachedThreadPool();
            try {
                try {
                    LocalServerSocket serverSocket2 = new LocalServerSocket(HOST_NAME);
                    while (true) {
                        try {
                            LocalSocket socket = serverSocket2.accept();
                            Rlog.i(ExternalSimManager.TAG, "There is a client is accpted: " + socket.toString());
                            threadExecutor.execute(ExternalSimManager.this.new ConnectionHandler(socket, eventHandler));
                        } catch (IOException e) {
                            e = e;
                            serverSocket = serverSocket2;
                            Rlog.w(ExternalSimManager.TAG, "listenConnection catch IOException");
                            e.printStackTrace();
                            Rlog.d(ExternalSimManager.TAG, "listenConnection finally!!");
                            if (threadExecutor != null) {
                                threadExecutor.shutdown();
                            }
                            if (serverSocket != null) {
                                try {
                                    serverSocket.close();
                                } catch (IOException e2) {
                                    e2.printStackTrace();
                                }
                            }
                            Rlog.d(ExternalSimManager.TAG, "listenConnection() - end");
                            return;
                        } catch (Exception e3) {
                            e = e3;
                            serverSocket = serverSocket2;
                            Rlog.w(ExternalSimManager.TAG, "listenConnection catch Exception");
                            e.printStackTrace();
                            Rlog.d(ExternalSimManager.TAG, "listenConnection finally!!");
                            if (threadExecutor != null) {
                                threadExecutor.shutdown();
                            }
                            if (serverSocket != null) {
                                try {
                                    serverSocket.close();
                                } catch (IOException e4) {
                                    e4.printStackTrace();
                                }
                            }
                            Rlog.d(ExternalSimManager.TAG, "listenConnection() - end");
                            return;
                        } catch (Throwable th) {
                            th = th;
                            serverSocket = serverSocket2;
                            Rlog.d(ExternalSimManager.TAG, "listenConnection finally!!");
                            if (threadExecutor != null) {
                                threadExecutor.shutdown();
                            }
                            if (serverSocket != null) {
                                try {
                                    serverSocket.close();
                                } catch (IOException e5) {
                                    e5.printStackTrace();
                                }
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e6) {
                e = e6;
            } catch (Exception e7) {
                e = e7;
            }
        }
    }

    public class ConnectionHandler implements Runnable {
        public static final String RILD_SERVER_NAME = "rild-vsim";
        private VsimEvenHandler mEventHandler;
        private LocalSocket mSocket;

        public ConnectionHandler(LocalSocket clientSocket, VsimEvenHandler eventHandler) {
            this.mSocket = clientSocket;
            this.mEventHandler = eventHandler;
        }

        @Override
        public void run() {
            Rlog.i(ExternalSimManager.TAG, "New connection: " + this.mSocket.toString());
            try {
                VsimIoThread ioThread = ExternalSimManager.this.new VsimIoThread(ServerTask.HOST_NAME, this.mSocket.getInputStream(), this.mSocket.getOutputStream(), this.mEventHandler);
                if (ExternalSimManager.this.mRilIoThread == null) {
                    ExternalSimManager.this.mRilIoThread = ExternalSimManager.this.new VsimIoThread(RILD_SERVER_NAME, RILD_SERVER_NAME, this.mEventHandler);
                    ExternalSimManager.this.mRilIoThread.start();
                }
                this.mEventHandler.setDataStream(ioThread, ExternalSimManager.this.mRilIoThread);
                if (ioThread != null) {
                    ioThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class VsimEvent {
        public static final int DEFAULT_MAX_DATA_LENGTH = 20480;
        private byte[] mData;
        private int mDataLen;
        private int mEventMaxDataLen;
        private int mMessageId;
        private int mReadOffset;
        private int mSlotId;
        private int mTransactionId;

        public VsimEvent(int transactionId, int messageId) {
            this(transactionId, messageId, 0);
        }

        public VsimEvent(int transactionId, int messageId, int slotId) {
            this(transactionId, messageId, DEFAULT_MAX_DATA_LENGTH, slotId);
        }

        public VsimEvent(int transactionId, int messageId, int length, int slotId) {
            this.mEventMaxDataLen = DEFAULT_MAX_DATA_LENGTH;
            this.mTransactionId = transactionId;
            this.mMessageId = messageId;
            this.mSlotId = slotId;
            this.mEventMaxDataLen = length;
            this.mData = new byte[this.mEventMaxDataLen];
            this.mDataLen = 0;
            this.mReadOffset = 0;
        }

        public void resetOffset() {
            synchronized (this) {
                this.mReadOffset = 0;
            }
        }

        public int putInt(int value) {
            synchronized (this) {
                if (this.mDataLen > this.mEventMaxDataLen - 4) {
                    return -1;
                }
                for (int i = 0; i < 4; i++) {
                    this.mData[this.mDataLen] = (byte) ((value >> (i * 8)) & 255);
                    this.mDataLen++;
                }
                return 0;
            }
        }

        public int putShort(int value) {
            synchronized (this) {
                if (this.mDataLen > this.mEventMaxDataLen - 2) {
                    return -1;
                }
                for (int i = 0; i < 2; i++) {
                    this.mData[this.mDataLen] = (byte) ((value >> (i * 8)) & 255);
                    this.mDataLen++;
                }
                return 0;
            }
        }

        public int putByte(int value) {
            synchronized (this) {
                if (this.mDataLen > this.mEventMaxDataLen - 1) {
                    return -1;
                }
                this.mData[this.mDataLen] = (byte) (value & 255);
                this.mDataLen++;
                return 0;
            }
        }

        public int putString(String str, int len) {
            synchronized (this) {
                if (this.mDataLen > this.mEventMaxDataLen - len) {
                    return -1;
                }
                byte[] s = str.getBytes();
                if (len < str.length()) {
                    System.arraycopy(s, 0, this.mData, this.mDataLen, len);
                    this.mDataLen += len;
                } else {
                    int remain = len - str.length();
                    System.arraycopy(s, 0, this.mData, this.mDataLen, str.length());
                    this.mDataLen += str.length();
                    for (int i = 0; i < remain; i++) {
                        this.mData[this.mDataLen] = 0;
                        this.mDataLen++;
                    }
                }
                return 0;
            }
        }

        public int putBytes(byte[] value) {
            synchronized (this) {
                int len = value.length;
                if (len > this.mEventMaxDataLen) {
                    return -1;
                }
                System.arraycopy(value, 0, this.mData, this.mDataLen, len);
                this.mDataLen += len;
                return 0;
            }
        }

        public byte[] getData() {
            byte[] tempData;
            synchronized (this) {
                tempData = new byte[this.mDataLen];
                System.arraycopy(this.mData, 0, tempData, 0, this.mDataLen);
            }
            return tempData;
        }

        public int getDataLen() {
            int i;
            synchronized (this) {
                i = this.mDataLen;
            }
            return i;
        }

        public int getMessageId() {
            return this.mMessageId;
        }

        public int getSlotBitMask() {
            return this.mSlotId;
        }

        public int getFirstSlotId() {
            int simCount = TelephonyManager.getDefault().getSimCount();
            for (int i = 0; i < simCount; i++) {
                if ((getSlotBitMask() & (1 << i)) != 0) {
                    Rlog.d(ExternalSimManager.TAG, "getFirstSlotId, slotId = " + i + ", slot bit mapping = " + getSlotBitMask());
                    return i;
                }
            }
            return -1;
        }

        public int getTransactionId() {
            return this.mTransactionId;
        }

        public int getInt() {
            int ret = 0;
            synchronized (this) {
                if (this.mData.length >= 4) {
                    ret = ((this.mData[this.mReadOffset + 3] & 255) << 24) | ((this.mData[this.mReadOffset + 2] & 255) << 16) | ((this.mData[this.mReadOffset + 1] & 255) << 8) | (this.mData[this.mReadOffset] & 255);
                    this.mReadOffset += 4;
                }
            }
            return ret;
        }

        public int getShort() {
            int ret;
            synchronized (this) {
                ret = ((this.mData[this.mReadOffset + 1] & 255) << 8) | (this.mData[this.mReadOffset] & 255);
                this.mReadOffset += 2;
            }
            return ret;
        }

        public int getByte() {
            int ret;
            synchronized (this) {
                ret = this.mData[this.mReadOffset] & 255;
                this.mReadOffset++;
            }
            return ret;
        }

        public byte[] getBytes(int length) {
            synchronized (this) {
                if (length > this.mDataLen - this.mReadOffset) {
                    return null;
                }
                byte[] ret = new byte[length];
                for (int i = 0; i < length; i++) {
                    ret[i] = this.mData[this.mReadOffset];
                    this.mReadOffset++;
                }
                return ret;
            }
        }

        public String getString(int len) {
            byte[] buf = new byte[len];
            synchronized (this) {
                System.arraycopy(this.mData, this.mReadOffset, buf, 0, len);
                this.mReadOffset += len;
            }
            return new String(buf).trim();
        }
    }

    class VsimIoThread extends Thread {
        private static final int MAX_DATA_LENGTH = 20480;
        private VsimEvenHandler mEventHandler;
        private DataInputStream mInput;
        private boolean mIsContinue;
        private String mName;
        private DataOutputStream mOutput;
        private String mServerName;
        private LocalSocket mSocket;
        private byte[] readBuffer;

        public VsimIoThread(String name, InputStream inputStream, OutputStream outputStream, VsimEvenHandler eventHandler) {
            this.mName = "";
            this.mIsContinue = true;
            this.mInput = null;
            this.mOutput = null;
            this.mEventHandler = null;
            this.mSocket = null;
            this.mServerName = "";
            this.readBuffer = null;
            this.mName = name;
            this.mInput = new DataInputStream(inputStream);
            this.mOutput = new DataOutputStream(outputStream);
            this.mEventHandler = eventHandler;
            logd("VsimIoThread constructor is called.");
        }

        public VsimIoThread(String name, String serverName, VsimEvenHandler eventHandler) {
            this.mName = "";
            this.mIsContinue = true;
            this.mInput = null;
            this.mOutput = null;
            this.mEventHandler = null;
            this.mSocket = null;
            this.mServerName = "";
            this.readBuffer = null;
            this.mServerName = serverName;
            createClientSocket(this.mServerName);
            this.mName = name;
            this.mEventHandler = eventHandler;
            logd("VsimIoThread constructor with creating socket is called.");
        }

        private void createClientSocket(String serverName) {
            int retryCount = 0;
            logd("createClientSocket() - start");
            while (true) {
                if (retryCount >= 10) {
                    break;
                }
                try {
                    logi("createClientSocket() - before, serverName: " + serverName);
                    this.mSocket = new LocalSocket();
                    LocalSocketAddress addr = new LocalSocketAddress(serverName, LocalSocketAddress.Namespace.RESERVED);
                    this.mSocket.connect(addr);
                    this.mInput = new DataInputStream(this.mSocket.getInputStream());
                    this.mOutput = new DataOutputStream(this.mSocket.getOutputStream());
                    logi("createClientSocket() - after, mSocket:" + this.mSocket.toString());
                } catch (IOException e) {
                    logw("createClientSocket catch IOException");
                    e.printStackTrace();
                    if (!ExternalSimManager.this.isIpoShutdown && this.mSocket != null && !this.mSocket.isConnected()) {
                        retryCount++;
                        try {
                            this.mSocket.close();
                            this.mSocket = null;
                            Thread.sleep(4000L);
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        logi("createClientSocket retry later, retry count: " + retryCount);
                    }
                }
                if (this.mSocket != null && this.mSocket.isConnected()) {
                    logd("createClientSocket connected!");
                    break;
                }
            }
            logd("createClientSocket() - end");
        }

        public void closeSocket() {
            try {
                if (this.mSocket == null) {
                    return;
                }
                this.mSocket.close();
                this.mSocket = null;
                logd("closeSocket.");
            } catch (IOException e) {
                logd("closeSocket IOException.");
                e.printStackTrace();
            }
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
                        Message msg = new Message();
                        msg.obj = event;
                        this.mEventHandler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    logw("VsimIoThread IOException.");
                    e.printStackTrace();
                    try {
                        if (this.mSocket != null) {
                            this.mSocket.close();
                            this.mSocket = null;
                        }
                        if (!this.mServerName.equals("")) {
                            createClientSocket(this.mServerName);
                        } else {
                            for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
                                TelephonyManager.getDefault();
                                String enabled = TelephonyManager.getTelephonyProperty(i, "gsm.external.sim.inserted", BaseParameters.FEATURE_MASK_3DNR_OFF);
                                if (enabled != null && enabled.length() > 0 && !BaseParameters.FEATURE_MASK_3DNR_OFF.equals(enabled)) {
                                    SystemProperties.set("gsm.external.sim.enabled", "");
                                    SystemProperties.set("gsm.external.sim.inserted", "");
                                    RadioManager.getInstance().setSilentRebootPropertyForAllModem(BaseParameters.FEATURE_MASK_3DNR_ON);
                                    UiccController.getInstance().resetRadioForVsim();
                                    logi("Disable VSIM and reset modem since socket disconnected.");
                                }
                            }
                            logw("Socket disconnected and vsim is disabled.");
                            terminate();
                        }
                    } catch (IOException e2) {
                        logw("VsimIoThread IOException 2.");
                        e2.printStackTrace();
                    }
                } catch (Exception e3) {
                    logw("VsimIoThread Exception.");
                    e3.printStackTrace();
                }
            }
        }

        private void writeBytes(byte[] value, int len) throws IOException {
            this.mOutput.write(value, 0, len);
        }

        private void writeInt(int value) throws IOException {
            for (int i = 0; i < 4; i++) {
                this.mOutput.write((value >> (i * 8)) & 255);
            }
        }

        public int writeEvent(VsimEvent event) {
            return writeEvent(event, false);
        }

        public int writeEvent(VsimEvent event, boolean isBigEndian) {
            logd("writeEvent Enter, isBigEndian:" + isBigEndian);
            int ret = -1;
            try {
                synchronized (this) {
                    if (this.mOutput != null) {
                        dumpEvent(event);
                        writeInt(event.getTransactionId());
                        writeInt(event.getMessageId());
                        writeInt(event.getSlotBitMask());
                        writeInt(event.getDataLen());
                        writeBytes(event.getData(), event.getDataLen());
                        this.mOutput.flush();
                        ret = 0;
                    } else {
                        loge("mOut is null, socket is not setup");
                    }
                }
                return ret;
            } catch (Exception e) {
                loge("writeEvent Exception");
                e.printStackTrace();
                return -1;
            }
        }

        private int readInt() throws IOException {
            byte[] tempBuf = new byte[8];
            int readCount = this.mInput.read(tempBuf, 0, 4);
            if (readCount >= 0) {
                return (tempBuf[3] << 24) | ((tempBuf[2] & 255) << 16) | ((tempBuf[1] & 255) << 8) | (tempBuf[0] & 255);
            }
            loge("readInt(), fail to read and throw exception");
            throw new IOException("fail to read");
        }

        private VsimEvent readEvent() throws IOException {
            logd("readEvent Enter");
            int transaction_id = readInt();
            int msg_id = readInt();
            int slot_id = readInt();
            int data_len = readInt();
            logd("readEvent transaction_id: " + transaction_id + ", msgId: " + msg_id + ", slot_id: " + slot_id + ", len: " + data_len);
            this.readBuffer = new byte[data_len];
            int offset = 0;
            int remaining = data_len;
            do {
                int countRead = this.mInput.read(this.readBuffer, offset, remaining);
                if (countRead < 0) {
                    loge("readEvent(), fail to read and throw exception");
                    throw new IOException("fail to read");
                }
                offset += countRead;
                remaining -= countRead;
            } while (remaining > 0);
            VsimEvent event = new VsimEvent(transaction_id, msg_id, data_len, slot_id);
            event.putBytes(this.readBuffer);
            dumpEvent(event);
            return event;
        }

        private void dumpEvent(VsimEvent event) {
            logi("dumpEvent: transaction_id: " + event.getTransactionId() + ", message_id:" + event.getMessageId() + ", slot_id:" + event.getSlotBitMask() + ", data_len:" + event.getDataLen() + ", event:" + IccUtils.bytesToHexString(event.getData()));
        }

        private void logd(String s) {
            Rlog.d(ExternalSimManager.TAG, "[" + this.mName + "] " + s);
        }

        private void logi(String s) {
            Rlog.i(ExternalSimManager.TAG, "[" + this.mName + "] " + s);
        }

        private void logw(String s) {
            Rlog.w(ExternalSimManager.TAG, "[" + this.mName + "] " + s);
        }

        private void loge(String s) {
            Rlog.e(ExternalSimManager.TAG, "[" + this.mName + "] " + s);
        }
    }

    public class VsimEvenHandler extends Handler {
        private VsimIoThread mVsimAdaptorIo = null;
        private VsimIoThread mVsimRilIo = null;
        private boolean mHasNotifyEnableEvnetToModem = false;
        private Timer mNoResponseTimer = null;
        private VsimEvent mWaitingEvent = null;
        private Runnable mTryResetModemRunnable = new Runnable() {
            @Override
            public void run() {
                if (UiccController.getInstance().isAllRadioAvailable()) {
                    RadioManager.getInstance().setSilentRebootPropertyForAllModem(BaseParameters.FEATURE_MASK_3DNR_ON);
                    UiccController.getInstance().resetRadioForVsim();
                    Rlog.i(ExternalSimManager.TAG, "mTryResetModemRunnable reset modem done.");
                    return;
                }
                VsimEvenHandler.this.postDelayed(VsimEvenHandler.this.mTryResetModemRunnable, 1000L);
            }
        };

        public VsimEvenHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            dispatchCallback((VsimEvent) msg.obj);
        }

        private void setDataStream(VsimIoThread vsimAdpatorIo, VsimIoThread vsimRilIo) {
            this.mVsimAdaptorIo = vsimAdpatorIo;
            this.mVsimRilIo = vsimRilIo;
            Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler setDataStream done.");
        }

        private int getRspMessageId(int requestMsgId) {
            switch (requestMsgId) {
                case 1:
                    return ExternalSimConstants.MSG_ID_INITIALIZATION_RESPONSE;
                case 2:
                    return ExternalSimConstants.MSG_ID_GET_PLATFORM_CAPABILITY_RESPONSE;
                case 3:
                    return ExternalSimConstants.MSG_ID_EVENT_RESPONSE;
                case 7:
                    return ExternalSimConstants.MSG_ID_GET_SERVICE_STATE_RESPONSE;
                case 8:
                    return ExternalSimConstants.MSG_ID_FINALIZATION_RESPONSE;
                case ExternalSimConstants.MSG_ID_UICC_RESET_REQUEST:
                    return 4;
                case ExternalSimConstants.MSG_ID_UICC_APDU_REQUEST:
                    return 5;
                case ExternalSimConstants.MSG_ID_UICC_POWER_DOWN_REQUEST:
                    return 6;
                default:
                    Rlog.d(ExternalSimManager.TAG, "getRspMessageId: " + requestMsgId + "no support.");
                    return -1;
            }
        }

        public class TimeOutTimerTask extends TimerTask {
            public TimeOutTimerTask() {
            }

            @Override
            public void run() {
                VsimEvenHandler.this.sendNoResponseError(VsimEvenHandler.this.mWaitingEvent);
                Rlog.i(ExternalSimManager.TAG, "TimeOutTimerTask time out and send response to modem directly.");
            }
        }

        private void sendNoResponseError(VsimEvent event) {
            VsimEvent response = new VsimEvent(event.getTransactionId(), getRspMessageId(event.getMessageId()), event.getSlotBitMask());
            response.putInt(-1);
            response.putInt(2);
            response.putByte(0);
            response.putByte(0);
            setMdWaitingFlag(false);
            if (this.mVsimRilIo == null) {
                return;
            }
            this.mVsimRilIo.writeEvent(response);
        }

        private void sendPlugOutEvent(VsimEvent event) {
            TelephonyManager.getDefault();
            String isInserted = TelephonyManager.getTelephonyProperty(event.getFirstSlotId(), "gsm.external.sim.inserted", BaseParameters.FEATURE_MASK_3DNR_OFF);
            if (BaseParameters.FEATURE_MASK_3DNR_OFF.equals(isInserted)) {
                Rlog.d(ExternalSimManager.TAG, "sendPlugOutEvent: " + isInserted);
                return;
            }
            TelephonyManager.getDefault();
            TelephonyManager.setTelephonyProperty(event.getFirstSlotId(), "gsm.external.sim.inserted", BaseParameters.FEATURE_MASK_3DNR_OFF);
            VsimEvent plugOutEvent = new VsimEvent(event.getTransactionId(), 3, event.getSlotBitMask());
            plugOutEvent.putInt(3);
            plugOutEvent.putInt(1);
            setMdWaitingFlag(false);
            if (this.mVsimRilIo == null) {
                return;
            }
            this.mVsimRilIo.writeEvent(plugOutEvent);
        }

        private void setMdWaitingFlag(boolean isWaiting) {
            setMdWaitingFlag(isWaiting, null);
        }

        private void setMdWaitingFlag(boolean isWaiting, VsimEvent event) {
            Rlog.d(ExternalSimManager.TAG, "setMdWaitingFlag: " + isWaiting);
            ExternalSimManager.this.isMdWaitingResponse = isWaiting;
            if (isWaiting) {
                this.mWaitingEvent = event;
                if (this.mNoResponseTimer == null) {
                    this.mNoResponseTimer = new Timer(true);
                }
                TelephonyManager.getDefault();
                String isVsimEnabled = TelephonyManager.getTelephonyProperty(event != null ? event.getFirstSlotId() : -1, "gsm.external.sim.enabled", BaseParameters.FEATURE_MASK_3DNR_OFF);
                if ("".equals(isVsimEnabled) || BaseParameters.FEATURE_MASK_3DNR_OFF.equals(isVsimEnabled)) {
                    this.mNoResponseTimer.schedule(new TimeOutTimerTask(), 0L);
                    if (!ExternalSimManager.this.isIpoShutdown) {
                        postDelayed(this.mTryResetModemRunnable, 1000L);
                    }
                    Rlog.i(ExternalSimManager.TAG, "recevice modem event under vsim disabled state. (isIpoShutdown: " + ExternalSimManager.this.isIpoShutdown + ")");
                    return;
                }
                this.mNoResponseTimer.schedule(new TimeOutTimerTask(), 10000L);
                return;
            }
            this.mWaitingEvent = null;
            if (this.mNoResponseTimer == null) {
                return;
            }
            this.mNoResponseTimer.cancel();
            this.mNoResponseTimer.purge();
            this.mNoResponseTimer = null;
        }

        private boolean getMdWaitingFlag() {
            Rlog.d(ExternalSimManager.TAG, "getMdWaitingFlag: " + ExternalSimManager.this.isMdWaitingResponse);
            return ExternalSimManager.this.isMdWaitingResponse;
        }

        private void handleEventRequest(int type, VsimEvent event) {
            Rlog.i(ExternalSimManager.TAG, "VsimEvenHandler eventHandlerByType: type[" + type + "] start");
            int slotId = event.getFirstSlotId();
            int simType = event.getInt();
            int result = 0;
            Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler First slotId:" + slotId + ", simType:" + simType);
            switch (type) {
                case 1:
                    if (SubscriptionController.getInstance().isReady()) {
                        result = 0;
                    } else {
                        result = -2;
                    }
                    int subId = SubscriptionManager.getSubIdUsingPhoneId(slotId);
                    SubscriptionController ctrl = SubscriptionController.getInstance();
                    if (simType != 1) {
                        ctrl.setDefaultDataSubIdWithoutCapabilitySwitch(subId);
                        Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler set default data to subId: " + subId);
                    }
                    TelephonyManager.getDefault();
                    TelephonyManager.setTelephonyProperty(slotId, "gsm.external.sim.enabled", BaseParameters.FEATURE_MASK_3DNR_ON);
                    break;
                case 2:
                    TelephonyManager.getDefault();
                    TelephonyManager.setTelephonyProperty(slotId, "gsm.external.sim.enabled", BaseParameters.FEATURE_MASK_3DNR_OFF);
                    TelephonyManager.getDefault();
                    TelephonyManager.setTelephonyProperty(slotId, "gsm.external.sim.inserted", BaseParameters.FEATURE_MASK_3DNR_OFF);
                    TelephonyManager.getDefault();
                    TelephonyManager.setTelephonyProperty(slotId, "persist.radio.external.sim", String.valueOf(0));
                    if (getMdWaitingFlag() && this.mWaitingEvent != null) {
                        sendNoResponseError(this.mWaitingEvent);
                    }
                    setMdWaitingFlag(false);
                    sendPlugOutEvent(event);
                    this.mVsimRilIo.writeEvent(event);
                    RadioManager.getInstance().setSilentRebootPropertyForAllModem(BaseParameters.FEATURE_MASK_3DNR_ON);
                    UiccController.getInstance().resetRadioForVsim();
                    break;
                case 3:
                    TelephonyManager.getDefault();
                    TelephonyManager.setTelephonyProperty(slotId, "gsm.external.sim.inserted", BaseParameters.FEATURE_MASK_3DNR_OFF);
                    setMdWaitingFlag(false);
                    this.mVsimRilIo.writeEvent(event);
                    break;
                case 4:
                    ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
                    if (iTelEx != null) {
                        try {
                        } catch (RemoteException e) {
                            Rlog.w(ExternalSimManager.TAG, "VsimEvenHandler fail to query isCapabilitySwitching.");
                        }
                        if (iTelEx.isCapabilitySwitching()) {
                            Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler isCapabilitySwitching: true.");
                            if (ExternalSimManager.PLUG_IN_AUTO_RETRY && ExternalSimManager.this.mRetryCounter < 10000) {
                                ExternalSimManager.this.mRetryCounter += ExternalSimManager.TRY_RESET_MODEM_DURATION;
                                Message msg = new Message();
                                event.resetOffset();
                                msg.obj = event;
                                sendMessageDelayed(msg, 1000L);
                                Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler sendMessageDelayed:1000, mRetryCounter:" + ExternalSimManager.this.mRetryCounter);
                                return;
                            }
                            result = -2;
                            Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler platform not ready.");
                        } else {
                            TelephonyManager.getDefault();
                            TelephonyManager.setTelephonyProperty(slotId, "gsm.external.sim.inserted", String.valueOf(simType));
                            SubscriptionController ctrl2 = SubscriptionController.getInstance();
                            int mCPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
                            if (slotId == mCPhoneId || simType == 1) {
                                Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler no need to do capablity switch");
                                this.mVsimRilIo.writeEvent(event);
                                RadioManager.getInstance().setSilentRebootPropertyForAllModem(BaseParameters.FEATURE_MASK_3DNR_ON);
                                UiccController.getInstance().resetRadioForVsim();
                            } else {
                                Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler need to do capablity switch");
                                if (SubscriptionController.getInstance().isReady()) {
                                    boolean success = ctrl2.setDefaultDataSubIdWithResult(SubscriptionManager.getSubIdUsingPhoneId(slotId));
                                    result = success ? 0 : -1;
                                } else {
                                    result = -2;
                                }
                            }
                        }
                        break;
                    }
                    break;
                case 5:
                    TelephonyManager.getDefault();
                    TelephonyManager.setTelephonyProperty(slotId, "persist.radio.external.sim", String.valueOf(simType));
                    break;
                default:
                    result = -1;
                    Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler invalid event id.");
                    break;
            }
            VsimEvent eventResponse = new VsimEvent(event.getTransactionId(), ExternalSimConstants.MSG_ID_EVENT_RESPONSE, event.getSlotBitMask());
            eventResponse.putInt(result);
            this.mVsimAdaptorIo.writeEvent(eventResponse);
            Rlog.i(ExternalSimManager.TAG, "VsimEvenHandler eventHandlerByType: type[" + type + "] end");
        }

        private void handleGetPlatformCapability(VsimEvent event) {
            event.getInt();
            int simType = event.getInt();
            VsimEvent response = new VsimEvent(event.getTransactionId(), ExternalSimConstants.MSG_ID_GET_PLATFORM_CAPABILITY_RESPONSE, event.getSlotBitMask());
            if (SubscriptionController.getInstance().isReady()) {
                response.putInt(0);
            } else {
                response.putInt(-2);
            }
            TelephonyManager.MultiSimVariants config = TelephonyManager.getDefault().getMultiSimConfiguration();
            if (config == TelephonyManager.MultiSimVariants.DSDS) {
                response.putInt(1);
            } else if (config == TelephonyManager.MultiSimVariants.DSDA) {
                response.putInt(2);
            } else if (config == TelephonyManager.MultiSimVariants.TSTS) {
                response.putInt(3);
            } else {
                response.putInt(0);
            }
            if (SystemProperties.getInt("ro.mtk_external_sim_support", 0) > 0) {
                response.putInt(2);
            } else {
                response.putInt(0);
            }
            int simCount = TelephonyManager.getDefault().getSimCount();
            Rlog.d(ExternalSimManager.TAG, "handleGetPlatformCapability simType: " + simType + ", simCount: " + simCount);
            if (simType == 1) {
                response.putInt((1 << simCount) - 1);
            } else if (config == TelephonyManager.MultiSimVariants.DSDA) {
                int isCdmaCard = 0;
                int isHasCard = 0;
                for (int i = 0; i < simCount; i++) {
                    String cardType = SystemProperties.get(ExternalSimManager.PROPERTY_RIL_FULL_UICC_TYPE[i], "");
                    if (!cardType.equals("")) {
                        isHasCard |= 1 << i;
                    }
                    if (cardType.contains("CSIM") || cardType.contains("RUIM") || cardType.contains("UIM")) {
                        isCdmaCard |= 1 << i;
                    }
                }
                Rlog.d(ExternalSimManager.TAG, "handleGetPlatformCapability isCdmaCard: " + isCdmaCard + ", isHasCard: " + isHasCard);
                if (isHasCard == 0 || isCdmaCard == 0) {
                    response.putInt(0);
                } else {
                    response.putInt(((1 << simCount) - 1) ^ isCdmaCard);
                }
            } else {
                response.putInt(0);
            }
            this.mVsimAdaptorIo.writeEvent(response);
        }

        private void handleServiceStateRequest(VsimEvent event) {
            int result = 0;
            int voiceRejectCause = -1;
            int dataRejectCause = -1;
            VsimEvent response = new VsimEvent(event.getTransactionId(), ExternalSimConstants.MSG_ID_GET_SERVICE_STATE_RESPONSE, event.getSlotBitMask());
            if (SubscriptionController.getInstance().isReady()) {
                ITelephonyEx telEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
                if (telEx != null) {
                    try {
                        int subId = SubscriptionManager.getSubIdUsingPhoneId(event.getFirstSlotId());
                        Bundle bundle = telEx.getServiceState(subId);
                        if (bundle != null) {
                            ServiceState ss = ServiceState.newFromBundle(bundle);
                            Rlog.d(ExternalSimManager.TAG, "handleServiceStateRequest subId: " + subId + ", ss = " + ss.toString());
                            voiceRejectCause = ss.getVoiceRejectCause();
                            dataRejectCause = ss.getDataRejectCause();
                        }
                    } catch (RemoteException e) {
                        Rlog.w(ExternalSimManager.TAG, "RemoteException!!");
                        result = -1;
                        e.printStackTrace();
                    }
                }
            } else {
                result = -2;
            }
            response.putInt(result);
            response.putInt(voiceRejectCause);
            response.putInt(dataRejectCause);
            this.mVsimAdaptorIo.writeEvent(response);
        }

        private void dispatchCallback(VsimEvent event) {
            synchronized (this) {
                int msgId = event.getMessageId();
                Rlog.i(ExternalSimManager.TAG, "VsimEvenHandler handleMessage: msgId[" + msgId + "]");
                switch (msgId) {
                    case 1:
                    case 8:
                    case ExternalSimConstants.MSG_ID_EVENT_RESPONSE:
                        break;
                    case 2:
                        handleGetPlatformCapability(event);
                        break;
                    case 3:
                        handleEventRequest(event.getInt(), event);
                        break;
                    case 4:
                        if (getMdWaitingFlag()) {
                            this.mVsimRilIo.writeEvent(event);
                            setMdWaitingFlag(false);
                        }
                        break;
                    case 5:
                        if (getMdWaitingFlag()) {
                            this.mVsimRilIo.writeEvent(event);
                            setMdWaitingFlag(false);
                        }
                        break;
                    case 6:
                        this.mVsimRilIo.writeEvent(event);
                        break;
                    case 7:
                        handleServiceStateRequest(event);
                        break;
                    case ExternalSimConstants.MSG_ID_UICC_RESET_REQUEST:
                        setMdWaitingFlag(true, event);
                        TelephonyManager.getDefault();
                        String inserted = TelephonyManager.getTelephonyProperty(event.getFirstSlotId(), "gsm.external.sim.inserted", BaseParameters.FEATURE_MASK_3DNR_OFF);
                        if (inserted != null && inserted.length() > 0 && !BaseParameters.FEATURE_MASK_3DNR_OFF.equals(inserted)) {
                            this.mVsimAdaptorIo.writeEvent(event);
                        }
                        break;
                    case ExternalSimConstants.MSG_ID_UICC_APDU_REQUEST:
                        setMdWaitingFlag(true, event);
                        TelephonyManager.getDefault();
                        String inserted2 = TelephonyManager.getTelephonyProperty(event.getFirstSlotId(), "gsm.external.sim.inserted", BaseParameters.FEATURE_MASK_3DNR_OFF);
                        if (inserted2 != null && inserted2.length() > 0 && !BaseParameters.FEATURE_MASK_3DNR_OFF.equals(inserted2)) {
                            this.mVsimAdaptorIo.writeEvent(event);
                        }
                        break;
                    case ExternalSimConstants.MSG_ID_UICC_POWER_DOWN_REQUEST:
                        TelephonyManager.getDefault();
                        String inserted3 = TelephonyManager.getTelephonyProperty(event.getFirstSlotId(), "gsm.external.sim.inserted", BaseParameters.FEATURE_MASK_3DNR_OFF);
                        if (inserted3 != null && inserted3.length() > 0 && !BaseParameters.FEATURE_MASK_3DNR_OFF.equals(inserted3)) {
                            this.mVsimAdaptorIo.writeEvent(event);
                        }
                        break;
                    default:
                        Rlog.d(ExternalSimManager.TAG, "VsimEvenHandler handleMessage: default");
                        break;
                }
            }
        }
    }
}
