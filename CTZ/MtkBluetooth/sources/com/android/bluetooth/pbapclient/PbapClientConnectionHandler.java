package com.android.bluetooth.pbapclient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.SdpPseRecord;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.CallLog;
import android.util.Log;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.bluetooth.R;
import java.io.IOException;
import java.util.HashMap;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;

class PbapClientConnectionHandler extends Handler {
    static final boolean DBG = true;
    public static final String ICH_PATH = "telecom/ich.vcf";
    private static final int L2CAP_INVALID_PSM = -1;
    public static final String MCH_PATH = "telecom/mch.vcf";
    static final int MSG_CONNECT = 1;
    static final int MSG_DISCONNECT = 2;
    static final int MSG_DOWNLOAD = 3;
    public static final String OCH_PATH = "telecom/och.vcf";
    private static final int PBAP_FEATURE_BROWSING = 2;
    private static final int PBAP_FEATURE_DEFAULT_IMAGE_FORMAT = 512;
    private static final int PBAP_FEATURE_DOWNLOADING = 1;
    private static final long PBAP_FILTER_ADR = 32;
    private static final long PBAP_FILTER_EMAIL = 256;
    private static final long PBAP_FILTER_FN = 2;
    private static final long PBAP_FILTER_N = 4;
    private static final long PBAP_FILTER_NICKNAME = 8388608;
    private static final long PBAP_FILTER_PHOTO = 8;
    private static final long PBAP_FILTER_TEL = 128;
    private static final long PBAP_FILTER_VERSION = 1;
    private static final long PBAP_REQUESTED_FIELDS = 8389039;
    private static final int PBAP_SUPPORTED_FEATURE = 515;
    private static final byte[] PBAP_TARGET = {121, 97, 53, -16, -16, -59, 17, -40, 9, 102, 8, 0, 32, 12, -102, 102};
    private static final int PBAP_V1_2 = 258;
    public static final String PB_PATH = "telecom/pb.vcf";
    static final String TAG = "PBAP PCE handler";
    public static final byte VCARD_TYPE_21 = 0;
    public static final byte VCARD_TYPE_30 = 1;
    private Account mAccount;
    private boolean mAccountCreated;
    private AccountManager mAccountManager;
    private final BluetoothAdapter mAdapter;
    private BluetoothPbapObexAuthenticator mAuth;
    private Context mContext;
    private final BluetoothDevice mDevice;
    private ClientSession mObexSession;
    private final PbapClientStateMachine mPbapClientStateMachine;
    private SdpPseRecord mPseRec;
    private BluetoothSocket mSocket;

    PbapClientConnectionHandler(Looper looper, Context context, PbapClientStateMachine pbapClientStateMachine, BluetoothDevice bluetoothDevice) {
        super(looper);
        this.mPseRec = null;
        this.mAuth = null;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mDevice = bluetoothDevice;
        this.mContext = context;
        this.mPbapClientStateMachine = pbapClientStateMachine;
        this.mAuth = new BluetoothPbapObexAuthenticator(this);
        this.mAccountManager = AccountManager.get(this.mPbapClientStateMachine.getContext());
        this.mAccount = new Account(this.mDevice.getAddress(), this.mContext.getString(R.string.pbap_account_type));
    }

    PbapClientConnectionHandler(Builder builder) {
        super(builder.mLooper);
        this.mPseRec = null;
        this.mAuth = null;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mDevice = builder.mDevice;
        this.mContext = builder.mContext;
        this.mPbapClientStateMachine = builder.mClientStateMachine;
        this.mAuth = new BluetoothPbapObexAuthenticator(this);
        this.mAccountManager = AccountManager.get(this.mPbapClientStateMachine.getContext());
        this.mAccount = new Account(this.mDevice.getAddress(), this.mContext.getString(R.string.pbap_account_type));
    }

    public static class Builder {
        private PbapClientStateMachine mClientStateMachine;
        private Context mContext;
        private BluetoothDevice mDevice;
        private Looper mLooper;

        public Builder setLooper(Looper looper) {
            this.mLooper = looper;
            return this;
        }

        public Builder setClientSM(PbapClientStateMachine pbapClientStateMachine) {
            this.mClientStateMachine = pbapClientStateMachine;
            return this;
        }

        public Builder setRemoteDevice(BluetoothDevice bluetoothDevice) {
            this.mDevice = bluetoothDevice;
            return this;
        }

        public Builder setContext(Context context) {
            this.mContext = context;
            return this;
        }

        public PbapClientConnectionHandler build() {
            return new PbapClientConnectionHandler(this);
        }
    }

    @Override
    public void handleMessage(Message message) {
        Log.d(TAG, "Handling Message = " + message.what);
        switch (message.what) {
            case 1:
                this.mPseRec = (SdpPseRecord) message.obj;
                if (connectSocket()) {
                    Log.d(TAG, "Socket connected");
                    if (connectObexSession()) {
                        this.mPbapClientStateMachine.obtainMessage(5).sendToTarget();
                    } else {
                        this.mPbapClientStateMachine.obtainMessage(6).sendToTarget();
                    }
                } else {
                    Log.w(TAG, "Socket CONNECT Failure ");
                    this.mPbapClientStateMachine.obtainMessage(6).sendToTarget();
                }
                break;
            case 2:
                Log.d(TAG, "Starting Disconnect");
                try {
                    if (this.mObexSession != null) {
                        Log.d(TAG, "obexSessionDisconnect" + this.mObexSession);
                        this.mObexSession.disconnect((HeaderSet) null);
                        this.mObexSession.close();
                    }
                    Log.d(TAG, "Closing Socket");
                    closeSocket();
                } catch (IOException e) {
                    Log.w(TAG, "DISCONNECT Failure ", e);
                }
                Log.d(TAG, "Completing Disconnect");
                removeAccount(this.mAccount);
                removeCallLog(this.mAccount);
                this.mPbapClientStateMachine.obtainMessage(7).sendToTarget();
                break;
            case 3:
                try {
                    this.mAccountCreated = addAccount(this.mAccount);
                    if (!this.mAccountCreated) {
                        Log.e(TAG, "Account creation failed.");
                    } else {
                        BluetoothPbapRequestPullPhoneBook bluetoothPbapRequestPullPhoneBook = new BluetoothPbapRequestPullPhoneBook(PB_PATH, this.mAccount, PBAP_REQUESTED_FIELDS, (byte) 1, 0, 1);
                        bluetoothPbapRequestPullPhoneBook.execute(this.mObexSession);
                        PhonebookPullRequest phonebookPullRequest = new PhonebookPullRequest(this.mPbapClientStateMachine.getContext(), this.mAccount);
                        phonebookPullRequest.setResults(bluetoothPbapRequestPullPhoneBook.getList());
                        phonebookPullRequest.onPullComplete();
                        HashMap<String, Integer> map = new HashMap<>();
                        downloadCallLog(MCH_PATH, map);
                        downloadCallLog(ICH_PATH, map);
                        downloadCallLog(OCH_PATH, map);
                    }
                } catch (IOException e2) {
                    Log.w(TAG, "DOWNLOAD_CONTACTS Failure" + e2.toString());
                    return;
                }
                break;
            default:
                Log.w(TAG, "Received Unexpected Message");
                break;
        }
    }

    private boolean connectSocket() {
        try {
            if (this.mPseRec == null) {
                Log.v(TAG, "connectSocket: UUID: " + BluetoothUuid.PBAP_PSE.getUuid());
                this.mSocket = this.mDevice.createRfcommSocketToServiceRecord(BluetoothUuid.PBAP_PSE.getUuid());
            } else if (this.mPseRec.getL2capPsm() != -1) {
                Log.v(TAG, "connectSocket: PSM: " + this.mPseRec.getL2capPsm());
                this.mSocket = this.mDevice.createL2capSocket(this.mPseRec.getL2capPsm());
            } else {
                Log.v(TAG, "connectSocket: channel: " + this.mPseRec.getRfcommChannelNumber());
                this.mSocket = this.mDevice.createRfcommSocket(this.mPseRec.getRfcommChannelNumber());
            }
            if (this.mSocket != null) {
                this.mSocket.connect();
                return true;
            }
            Log.w(TAG, "Could not create socket");
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error while connecting socket", e);
            return false;
        }
    }

    private boolean connectObexSession() {
        try {
            Log.v(TAG, "Start Obex Client Session");
            this.mObexSession = new ClientSession(new BluetoothObexTransport(this.mSocket));
            this.mObexSession.setAuthenticator(this.mAuth);
            HeaderSet headerSet = new HeaderSet();
            headerSet.setHeader(70, PBAP_TARGET);
            if (this.mPseRec != null) {
                Log.d(TAG, "Remote PbapSupportedFeatures " + this.mPseRec.getSupportedFeatures());
                ObexAppParameters obexAppParameters = new ObexAppParameters();
                if (this.mPseRec.getProfileVersion() >= 258) {
                    obexAppParameters.add((byte) 16, PBAP_SUPPORTED_FEATURE);
                }
                obexAppParameters.addToHeaderSet(headerSet);
            }
            z = this.mObexSession.connect(headerSet).getResponseCode() == 160;
            Log.d(TAG, "Success = " + Boolean.toString(z));
        } catch (IOException e) {
            Log.w(TAG, "CONNECT Failure " + e.toString());
            closeSocket();
        }
        return z;
    }

    public void abort() {
        closeSocket();
        getLooper().getThread().interrupt();
    }

    private void closeSocket() {
        try {
            if (this.mSocket != null) {
                Log.d(TAG, "Closing socket" + this.mSocket);
                this.mSocket.close();
                this.mSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error when closing socket", e);
            this.mSocket = null;
        }
    }

    void downloadCallLog(String str, HashMap<String, Integer> map) {
        try {
            BluetoothPbapRequestPullPhoneBook bluetoothPbapRequestPullPhoneBook = new BluetoothPbapRequestPullPhoneBook(str, this.mAccount, 0L, (byte) 1, 0, 0);
            bluetoothPbapRequestPullPhoneBook.execute(this.mObexSession);
            CallLogPullRequest callLogPullRequest = new CallLogPullRequest(this.mPbapClientStateMachine.getContext(), str, map, this.mAccount);
            callLogPullRequest.setResults(bluetoothPbapRequestPullPhoneBook.getList());
            callLogPullRequest.onPullComplete();
        } catch (IOException e) {
            Log.w(TAG, "Download call log failure");
        }
    }

    private boolean addAccount(Account account) {
        if (this.mAccountManager.addAccountExplicitly(account, null, null)) {
            Log.d(TAG, "Added account " + this.mAccount);
            return true;
        }
        return false;
    }

    private void removeAccount(Account account) {
        if (this.mAccountManager.removeAccountExplicitly(account)) {
            Log.d(TAG, "Removed account " + account);
            return;
        }
        Log.e(TAG, "Failed to remove account " + this.mAccount);
    }

    private void removeCallLog(Account account) {
        try {
            if (this.mContext.getContentResolver() == null) {
                Log.d(TAG, "CallLog ContentResolver is not found");
                return;
            }
            this.mContext.getContentResolver().delete(CallLog.Calls.CONTENT_URI, "subscription_id=" + account.hashCode(), null);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Call Logs could not be deleted, they may not exist yet.");
        }
    }
}
