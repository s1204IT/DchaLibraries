package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneSwitcher;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.RadioCapability;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.devreg.DeviceRegisterController;
import com.mediatek.internal.telephony.phb.MtkUiccPhoneBookController;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MtkProxyController extends ProxyController {
    private static final int EVENT_RADIO_AVAILABLE = 6;
    private static final int EVENT_RIL_CONNECTED = 7;
    private static final String PROPERTY_CAPABILITY_SWITCH = "persist.vendor.radio.simswitch";
    private static final String PROPERTY_CAPABILITY_SWITCH_STATE = "persist.vendor.radio.simswitchstate";
    private static final int RC_CANNOT_SWITCH = 2;
    private static final int RC_DO_SWITCH = 0;
    private static final int RC_NO_NEED_SWITCH = 1;
    private static final int RC_RETRY_CAUSE_AIRPLANE_MODE = 5;
    private static final int RC_RETRY_CAUSE_CAPABILITY_SWITCHING = 2;
    private static final int RC_RETRY_CAUSE_IN_CALL = 3;
    private static final int RC_RETRY_CAUSE_NONE = 0;
    private static final int RC_RETRY_CAUSE_RADIO_UNAVAILABLE = 4;
    private static final int RC_RETRY_CAUSE_RESULT_ERROR = 6;
    private static final int RC_RETRY_CAUSE_WORLD_MODE_SWITCHING = 1;
    private DeviceRegisterController mDeviceRegisterController;
    private BroadcastReceiver mEccStateReceiver;
    private boolean mHasRegisterEccStateReceiver;
    private boolean mHasRegisterPhoneStateReceiver;
    private boolean mHasRegisterWorldModeReceiver;
    private boolean mIsCapSwitching;
    private boolean mIsRildReconnected;
    private Handler mMtkHandler;
    private MtkPhoneSubInfoControllerEx mMtkPhoneSubInfoControllerEx;
    protected MtkUiccPhoneBookController mMtkUiccPhoneBookController;
    private MtkUiccSmsController mMtkUiccSmsController;
    RadioAccessFamily[] mNextRafs;
    private BroadcastReceiver mPhoneStateReceiver;
    private IMtkProxyControllerExt mProxyControllerExt;
    private int mSetRafRetryCause;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory;
    private BroadcastReceiver mWorldModeReceiver;
    private int onExceptionCount;

    public MtkProxyController(Context context, Phone[] phoneArr, UiccController uiccController, CommandsInterface[] commandsInterfaceArr, PhoneSwitcher phoneSwitcher) {
        super(context, phoneArr, uiccController, commandsInterfaceArr, phoneSwitcher);
        this.mIsCapSwitching = false;
        this.mHasRegisterWorldModeReceiver = false;
        this.mHasRegisterPhoneStateReceiver = false;
        this.mHasRegisterEccStateReceiver = false;
        this.mIsRildReconnected = false;
        this.mNextRafs = null;
        this.onExceptionCount = 0;
        this.mTelephonyCustomizationFactory = null;
        this.mProxyControllerExt = null;
        this.mMtkHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                MtkProxyController.this.mtkLogd("mtkHandleMessage msg.what=" + message.what);
                switch (message.what) {
                    case 6:
                        MtkProxyController.this.onRetryWhenRadioAvailable(message);
                        break;
                    case 7:
                        MtkProxyController.this.mIsRildReconnected = true;
                        break;
                }
            }
        };
        this.mWorldModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                MtkProxyController.this.mtkLogd("mWorldModeReceiver: action = " + action);
                if (!WorldPhoneUtil.isWorldModeSupport() && WorldPhoneUtil.isWorldPhoneSupport() && ModemSwitchHandler.ACTION_MODEM_SWITCH_DONE.equals(action) && MtkProxyController.this.mNextRafs != null && MtkProxyController.this.mSetRafRetryCause == 1) {
                    try {
                        if (!MtkProxyController.this.setRadioCapability(MtkProxyController.this.mNextRafs)) {
                            MtkProxyController.this.sendCapabilityFailBroadcast();
                        }
                    } catch (RuntimeException e) {
                        MtkProxyController.this.sendCapabilityFailBroadcast();
                    }
                }
            }
        };
        this.mPhoneStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                String str = TelephonyManager.EXTRA_STATE_OFFHOOK;
                MtkProxyController.this.mtkLogd("mPhoneStateReceiver: action = " + action);
                if ("android.intent.action.PHONE_STATE".equals(action)) {
                    String stringExtra = intent.getStringExtra("state");
                    MtkProxyController.this.mtkLogd("phoneState: " + stringExtra);
                    if (TelephonyManager.EXTRA_STATE_IDLE.equals(stringExtra) && MtkProxyController.this.mNextRafs != null && MtkProxyController.this.mSetRafRetryCause == 3) {
                        MtkProxyController.this.unRegisterPhoneStateReceiver();
                        try {
                            if (!MtkProxyController.this.setRadioCapability(MtkProxyController.this.mNextRafs)) {
                                MtkProxyController.this.sendCapabilityFailBroadcast();
                            }
                        } catch (RuntimeException e) {
                            MtkProxyController.this.sendCapabilityFailBroadcast();
                        }
                    }
                }
            }
        };
        this.mEccStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                MtkProxyController.this.mtkLogd("mEccStateReceiver, received " + intent.getAction());
                if (!MtkProxyController.this.isEccInProgress() && MtkProxyController.this.mNextRafs != null && MtkProxyController.this.mSetRafRetryCause == 3) {
                    MtkProxyController.this.unRegisterEccStateReceiver();
                    try {
                        if (!MtkProxyController.this.setRadioCapability(MtkProxyController.this.mNextRafs)) {
                            MtkProxyController.this.sendCapabilityFailBroadcast();
                        }
                    } catch (RuntimeException e) {
                        MtkProxyController.this.sendCapabilityFailBroadcast();
                    }
                }
            }
        };
        mtkLogd("Constructor - Enter");
        this.mMtkUiccPhoneBookController = new MtkUiccPhoneBookController(this.mPhones);
        this.mMtkPhoneSubInfoControllerEx = new MtkPhoneSubInfoControllerEx(this.mContext, this.mPhones);
        this.mMtkUiccSmsController = new MtkUiccSmsController(this.mPhones);
        mtkLogd("Constructor - Exit");
        this.mDeviceRegisterController = new DeviceRegisterController(this.mContext, this.mPhones, this.mMtkUiccSmsController);
        try {
            this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(context);
            this.mProxyControllerExt = this.mTelephonyCustomizationFactory.makeMtkProxyControllerExt(context);
        } catch (Exception e) {
            mtkLogd("mProxyControllerExt init fail");
            e.printStackTrace();
        }
    }

    public DeviceRegisterController getDeviceRegisterController() {
        return this.mDeviceRegisterController;
    }

    public boolean setRadioCapability(RadioAccessFamily[] radioAccessFamilyArr) {
        if (radioAccessFamilyArr.length != this.mPhones.length) {
            throw new RuntimeException("Length of input rafs must equal to total phone count");
        }
        int i = 0;
        while (true) {
            if (i >= radioAccessFamilyArr.length) {
                break;
            }
            if ((radioAccessFamilyArr[i].getRadioAccessFamily() & 2) <= 0) {
                i++;
            } else {
                SystemProperties.set(PROPERTY_CAPABILITY_SWITCH_STATE, String.valueOf(i));
                break;
            }
        }
        int iCheckRadioCapabilitySwitchConditions = checkRadioCapabilitySwitchConditions(radioAccessFamilyArr);
        if (iCheckRadioCapabilitySwitchConditions == 1) {
            return true;
        }
        if (iCheckRadioCapabilitySwitchConditions == 2) {
            return false;
        }
        return super.setRadioCapability(radioAccessFamilyArr);
    }

    protected boolean doSetRadioCapabilities(RadioAccessFamily[] radioAccessFamilyArr) {
        synchronized (this) {
            this.mIsCapSwitching = true;
        }
        this.onExceptionCount = 0;
        this.mCi[0].registerForRilConnected(this.mMtkHandler, 7, (Object) null);
        this.mIsRildReconnected = false;
        return super.doSetRadioCapabilities(radioAccessFamilyArr);
    }

    protected void onStartRadioCapabilityResponse(Message message) {
        CommandException.Error commandError;
        synchronized (this.mSetRadioAccessFamilyStatus) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                if (this.onExceptionCount == 0) {
                    this.onExceptionCount = 1;
                    if (asyncResult.exception instanceof CommandException) {
                        commandError = asyncResult.exception.getCommandError();
                    } else {
                        commandError = null;
                    }
                    if (commandError == CommandException.Error.RADIO_NOT_AVAILABLE) {
                        this.mSetRafRetryCause = 4;
                        for (int i = 0; i < this.mPhones.length; i++) {
                            this.mCi[i].registerForAvailable(this.mMtkHandler, 6, (Object) null);
                        }
                        mtkLoge("onStartRadioCapabilityResponse: Retry later due to modem off");
                    }
                }
                mtkLogd("onStartRadioCapabilityResponse got exception=" + asyncResult.exception);
                this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
                this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED"));
                resetSimSwitchState();
                return;
            }
            RadioCapability radioCapability = (RadioCapability) ((AsyncResult) message.obj).result;
            if (radioCapability != null && radioCapability.getSession() == this.mRadioCapabilitySessionId) {
                this.mRadioAccessFamilyStatusCounter--;
                int phoneId = radioCapability.getPhoneId();
                if (((AsyncResult) message.obj).exception != null) {
                    mtkLogd("onStartRadioCapabilityResponse: Error response session=" + radioCapability.getSession());
                    mtkLogd("onStartRadioCapabilityResponse: phoneId=" + phoneId + " status=FAIL");
                    this.mSetRadioAccessFamilyStatus[phoneId] = 5;
                    this.mTransactionFailed = true;
                } else {
                    mtkLogd("onStartRadioCapabilityResponse: phoneId=" + phoneId + " status=STARTED");
                    this.mSetRadioAccessFamilyStatus[phoneId] = 2;
                }
                if (this.mRadioAccessFamilyStatusCounter == 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onStartRadioCapabilityResponse: success=");
                    sb.append(!this.mTransactionFailed);
                    mtkLogd(sb.toString());
                    if (this.mTransactionFailed) {
                        issueFinish(this.mRadioCapabilitySessionId);
                    } else {
                        resetRadioAccessFamilyStatusCounter();
                        for (int i2 = 0; i2 < this.mPhones.length; i2++) {
                            sendRadioCapabilityRequest(i2, this.mRadioCapabilitySessionId, 2, this.mNewRadioAccessFamily[i2], this.mNewLogicalModemIds[i2], 0, 3);
                            mtkLogd("onStartRadioCapabilityResponse: phoneId=" + i2 + " status=APPLYING");
                            this.mSetRadioAccessFamilyStatus[i2] = 3;
                        }
                    }
                }
                return;
            }
            mtkLogd("onStartRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + radioCapability);
        }
    }

    protected void onApplyRadioCapabilityErrorHandler(Message message) {
        CommandException.Error commandError;
        RadioCapability radioCapability = (RadioCapability) ((AsyncResult) message.obj).result;
        AsyncResult asyncResult = (AsyncResult) message.obj;
        if (radioCapability == null && asyncResult.exception != null && this.onExceptionCount == 0) {
            this.onExceptionCount = 1;
            if (asyncResult.exception instanceof CommandException) {
                commandError = asyncResult.exception.getCommandError();
            } else {
                commandError = null;
            }
            if (commandError == CommandException.Error.RADIO_NOT_AVAILABLE) {
                this.mSetRafRetryCause = 4;
                for (int i = 0; i < this.mPhones.length; i++) {
                    this.mCi[i].registerForAvailable(this.mMtkHandler, 6, (Object) null);
                }
                mtkLoge("onApplyRadioCapabilityResponse: Retry due to RADIO_NOT_AVAILABLE");
            } else {
                mtkLoge("onApplyRadioCapabilityResponse: exception=" + asyncResult.exception);
            }
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED"));
            resetSimSwitchState();
        }
    }

    protected void onApplyExceptionHandler(Message message) {
        CommandException.Error commandError;
        RadioCapability radioCapability = (RadioCapability) ((AsyncResult) message.obj).result;
        AsyncResult asyncResult = (AsyncResult) message.obj;
        int phoneId = radioCapability.getPhoneId();
        if (asyncResult.exception instanceof CommandException) {
            commandError = asyncResult.exception.getCommandError();
        } else {
            commandError = null;
        }
        if (commandError == CommandException.Error.RADIO_NOT_AVAILABLE) {
            this.mSetRafRetryCause = 4;
            this.mCi[phoneId].registerForAvailable(this.mMtkHandler, 6, (Object) null);
            mtkLoge("onApplyRadioCapabilityResponse: Retry later due to modem off");
        } else {
            mtkLoge("onApplyRadioCapabilityResponse: exception=" + asyncResult.exception);
        }
    }

    protected void onNotificationRadioCapabilityChanged(Message message) {
        if (!isCapabilitySwitching()) {
            RadioCapability radioCapability = (RadioCapability) ((AsyncResult) message.obj).result;
            mtkLogd("radio change is not triggered by sim switch, notification should be ignore");
            if (radioCapability == null) {
                logd("onNotificationRadioCapabilityChanged: rc == null");
                return;
            }
            logd("onNotificationRadioCapabilityChanged: rc=" + radioCapability);
            int phoneId = radioCapability.getPhoneId();
            if (((AsyncResult) message.obj).exception == null) {
                logd("onNotificationRadioCapabilityChanged: update phone capability");
                this.mPhones[phoneId].radioCapabilityUpdated(radioCapability);
            }
            resetSimSwitchState();
            return;
        }
        super.onNotificationRadioCapabilityChanged(message);
    }

    protected void onFinishRadioCapabilityResponse(Message message) {
        RadioCapability radioCapability = (RadioCapability) ((AsyncResult) message.obj).result;
        int i = SystemProperties.getInt(PROPERTY_CAPABILITY_SWITCH_STATE, -1);
        if ((radioCapability == null || radioCapability.getSession() != this.mRadioCapabilitySessionId) && radioCapability == null && ((AsyncResult) message.obj).exception != null) {
            synchronized (this.mSetRadioAccessFamilyStatus) {
                mtkLogd("onFinishRadioCapabilityResponse C2K mRadioAccessFamilyStatusCounter=" + this.mRadioAccessFamilyStatusCounter);
                this.mRadioAccessFamilyStatusCounter = this.mRadioAccessFamilyStatusCounter - 1;
                if (this.mRadioAccessFamilyStatusCounter == 0) {
                    completeRadioCapabilityTransaction();
                }
            }
            return;
        }
        if (i >= 0 && i < this.mPhones.length && this.mRadioAccessFamilyStatusCounter == 1) {
            int radioAccessFamily = this.mPhones[i].getRadioAccessFamily();
            if ((radioAccessFamily & 2) == 0) {
                mtkLogd("onFinishRadioCapabilityResponse, main phone raf[" + i + "]=" + radioAccessFamily);
                this.mSetRafRetryCause = 6;
            }
        }
        super.onFinishRadioCapabilityResponse(message);
    }

    protected void onTimeoutRadioCapability(Message message) {
        boolean z;
        synchronized (this.mSetRadioAccessFamilyStatus) {
            z = true;
            for (int i = 0; i < this.mPhones.length; i++) {
                if (this.mIsRildReconnected && this.mSetRadioAccessFamilyStatus[i] < 2) {
                    z = false;
                }
            }
        }
        mtkLogd("onTimeoutRadioCapability mIsRildReconnected=" + this.mIsRildReconnected + ", isNeedDelay=" + z);
        if (z) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, this.mRadioCapabilitySessionId, 0), 45000L);
        } else {
            super.onTimeoutRadioCapability(message);
        }
    }

    protected void issueFinish(int i) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            resetRadioAccessFamilyStatusCounter();
            for (int i2 = 0; i2 < this.mPhones.length; i2++) {
                mtkLogd("issueFinish: phoneId=" + i2 + " sessionId=" + i + " mTransactionFailed=" + this.mTransactionFailed);
                sendRadioCapabilityRequest(i2, i, 4, this.mOldRadioAccessFamily[i2], this.mCurrentLogicalModemIds[i2], this.mTransactionFailed ? 2 : 1, 4);
                if (this.mTransactionFailed) {
                    mtkLogd("issueFinish: phoneId: " + i2 + " status: FAIL");
                    this.mSetRadioAccessFamilyStatus[i2] = 5;
                }
            }
        }
    }

    protected void completeRadioCapabilityTransaction() {
        Intent intent;
        StringBuilder sb = new StringBuilder();
        sb.append("onFinishRadioCapabilityResponse: success=");
        sb.append(!this.mTransactionFailed);
        mtkLogd(sb.toString());
        SystemProperties.set(PROPERTY_CAPABILITY_SWITCH_STATE, "-1");
        if (!this.mTransactionFailed) {
            ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
            for (int i = 0; i < this.mPhones.length; i++) {
                int radioAccessFamily = this.mPhones[i].getRadioAccessFamily();
                mtkLogd("radioAccessFamily[" + i + "]=" + radioAccessFamily);
                arrayList.add(new RadioAccessFamily(i, radioAccessFamily));
            }
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
            intent.putParcelableArrayListExtra("rafs", arrayList);
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            resetSimSwitchState();
        } else {
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
            this.mTransactionFailed = false;
            resetSimSwitchState();
        }
        this.mCi[0].unregisterForRilConnected(this.mMtkHandler);
        this.mIsRildReconnected = false;
        this.mContext.sendBroadcast(intent, "android.permission.READ_PHONE_STATE");
        if (this.mNextRafs != null) {
            if (this.mSetRafRetryCause == 2 || this.mSetRafRetryCause == 6) {
                mtkLogd("has next capability switch request,trigger it, cause = " + this.mSetRafRetryCause);
                try {
                    if (!setRadioCapability(this.mNextRafs)) {
                        sendCapabilityFailBroadcast();
                    } else {
                        this.mSetRafRetryCause = 0;
                        this.mNextRafs = null;
                    }
                } catch (RuntimeException e) {
                    sendCapabilityFailBroadcast();
                }
            }
        }
    }

    private void resetSimSwitchState() {
        if (isCapabilitySwitching()) {
            this.mHandler.removeMessages(5);
        }
        synchronized (this) {
            this.mIsCapSwitching = false;
        }
        clearTransaction();
    }

    protected void sendRadioCapabilityRequest(int i, int i2, int i3, int i4, String str, int i5, int i6) {
        if (str == null || str.equals("")) {
            str = "modem_sys3";
        }
        super.sendRadioCapabilityRequest(i, i2, i3, i4, str, i5, i6);
    }

    public int getMaxRafSupported() {
        int[] iArr = new int[this.mPhones.length];
        int radioAccessFamily = 1;
        for (int i = 0; i < this.mPhones.length; i++) {
            if ((this.mPhones[i].getRadioAccessFamily() & 2) == 2) {
                radioAccessFamily = this.mPhones[i].getRadioAccessFamily();
            }
        }
        mtkLogd("getMaxRafSupported: maxRafBit=0 maxRaf=" + radioAccessFamily + " flag=" + (radioAccessFamily & 2));
        if (radioAccessFamily == 1) {
            return radioAccessFamily | 2;
        }
        return radioAccessFamily;
    }

    public int getMinRafSupported() {
        int[] iArr = new int[this.mPhones.length];
        int radioAccessFamily = 1;
        for (int i = 0; i < this.mPhones.length; i++) {
            if ((this.mPhones[i].getRadioAccessFamily() & 2) == 0) {
                radioAccessFamily = this.mPhones[i].getRadioAccessFamily();
            }
        }
        mtkLogd("getMinRafSupported: minRafBit=0 minRaf=" + radioAccessFamily + " flag=" + (radioAccessFamily & 2));
        return radioAccessFamily;
    }

    protected void mtkLogd(String str) {
        Rlog.d("MtkProxyController", str);
    }

    protected void mtkLoge(String str) {
        Rlog.e("MtkProxyController", str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        try {
            this.mPhoneSwitcher.dump(fileDescriptor, printWriter, strArr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isCapabilitySwitching() {
        boolean z;
        synchronized (this) {
            z = this.mIsCapSwitching;
        }
        return z;
    }

    private int checkRadioCapabilitySwitchConditions(RadioAccessFamily[] radioAccessFamilyArr) {
        synchronized (this) {
            this.mNextRafs = radioAccessFamilyArr;
            if (this.mIsCapSwitching) {
                mtkLogd("keep it and return,because capability swithing");
                this.mSetRafRetryCause = 2;
                return 1;
            }
            if (this.mSetRafRetryCause == 2) {
                mtkLogd("setCapability, mIsCapSwitching is not switching, can switch");
                this.mSetRafRetryCause = 0;
            }
            this.mIsCapSwitching = true;
            if (SystemProperties.getBoolean("ro.vendor.mtk_disable_cap_switch", false)) {
                this.mNextRafs = null;
                completeRadioCapabilityTransaction();
                mtkLogd("skip switching because mtk_disable_cap_switch is true");
                return 1;
            }
            if (SystemProperties.getInt("vendor.gsm.gcf.testmode", 0) == 2) {
                this.mNextRafs = null;
                completeRadioCapabilityTransaction();
                mtkLogd("skip switching because FTA mode");
                return 1;
            }
            if (SystemProperties.getInt("persist.vendor.radio.simswitch.emmode", 1) == 0) {
                this.mNextRafs = null;
                completeRadioCapabilityTransaction();
                mtkLogd("skip switching because EM disable mode");
                return 1;
            }
            if (WorldPhoneUtil.isWorldPhoneSupport()) {
                if (!WorldPhoneUtil.isWorldModeSupport()) {
                    if (ModemSwitchHandler.isModemTypeSwitching()) {
                        logd("world mode switching.");
                        if (!this.mHasRegisterWorldModeReceiver) {
                            registerWorldModeReceiverFor90Modem();
                        }
                        this.mSetRafRetryCause = 1;
                        synchronized (this) {
                            this.mIsCapSwitching = false;
                        }
                        return 2;
                    }
                } else if (this.mSetRafRetryCause == 1 && this.mHasRegisterWorldModeReceiver) {
                    unRegisterWorldModeReceiver();
                    this.mSetRafRetryCause = 0;
                }
            }
            if (TelephonyManager.getDefault().getCallState() != 0) {
                mtkLogd("setCapability in calling, fail to set RAT for phones");
                if (!this.mHasRegisterPhoneStateReceiver) {
                    registerPhoneStateReceiver();
                }
                this.mSetRafRetryCause = 3;
                synchronized (this) {
                    this.mIsCapSwitching = false;
                }
                return 2;
            }
            if (isEccInProgress()) {
                mtkLogd("setCapability in ECC, fail to set RAT for phones");
                if (!this.mHasRegisterEccStateReceiver) {
                    registerEccStateReceiver();
                }
                this.mSetRafRetryCause = 3;
                synchronized (this) {
                    this.mIsCapSwitching = false;
                }
                return 2;
            }
            if (this.mSetRafRetryCause == 3) {
                if (this.mHasRegisterPhoneStateReceiver) {
                    unRegisterPhoneStateReceiver();
                    this.mSetRafRetryCause = 0;
                }
                if (this.mHasRegisterEccStateReceiver) {
                    unRegisterEccStateReceiver();
                    this.mSetRafRetryCause = 0;
                }
            }
            for (int i = 0; i < this.mPhones.length; i++) {
                if (!this.mPhones[i].isRadioAvailable()) {
                    this.mSetRafRetryCause = 4;
                    this.mCi[i].registerForAvailable(this.mMtkHandler, 6, (Object) null);
                    mtkLogd("setCapability fail,Phone" + i + " is not available");
                    synchronized (this) {
                        this.mIsCapSwitching = false;
                    }
                    return 2;
                }
                if (this.mSetRafRetryCause == 4) {
                    this.mCi[i].unregisterForAvailable(this.mMtkHandler);
                    if (i == this.mPhones.length - 1) {
                        this.mSetRafRetryCause = 0;
                    }
                }
            }
            int iIntValue = Integer.valueOf(SystemProperties.get("persist.vendor.radio.simswitch", "1")).intValue();
            boolean z = false;
            int phoneId = 0;
            for (int i2 = 0; i2 < radioAccessFamilyArr.length; i2++) {
                if ((radioAccessFamilyArr[i2].getRadioAccessFamily() & 2) > 0) {
                    phoneId = radioAccessFamilyArr[i2].getPhoneId();
                    if (phoneId == iIntValue - 1) {
                        mtkLogd("no change, skip setRadioCapability");
                        this.mSetRafRetryCause = 0;
                        this.mNextRafs = null;
                        completeRadioCapabilityTransaction();
                        return 1;
                    }
                    if (!z) {
                        z = true;
                    } else {
                        mtkLogd("set more than one 3G phone, fail");
                        synchronized (this) {
                            this.mIsCapSwitching = false;
                        }
                        throw new RuntimeException("input parameter is incorrect");
                    }
                }
            }
            if (!z) {
                synchronized (this) {
                    this.mIsCapSwitching = false;
                }
                throw new RuntimeException("input parameter is incorrect - no 3g phone");
            }
            if (SystemProperties.getInt("ro.vendor.mtk_external_sim_support", 0) == 1) {
                for (int i3 = 0; i3 < this.mPhones.length; i3++) {
                    TelephonyManager.getDefault();
                    String telephonyProperty = TelephonyManager.getTelephonyProperty(i3, "vendor.gsm.external.sim.enabled", "0");
                    TelephonyManager.getDefault();
                    String telephonyProperty2 = TelephonyManager.getTelephonyProperty(i3, "vendor.gsm.external.sim.inserted", "0");
                    int phoneId2 = MtkSubscriptionController.getMtkInstance().getPhoneId(MtkSubscriptionController.getMtkInstance().getDefaultDataSubId());
                    if ("1".equals(telephonyProperty) && (("0".equals(telephonyProperty2) || "".equals(telephonyProperty2)) && phoneId != phoneId2)) {
                        synchronized (this) {
                            this.mIsCapSwitching = false;
                        }
                        return 1;
                    }
                }
                int mainCapabilityPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
                TelephonyManager.getDefault();
                String telephonyProperty3 = TelephonyManager.getTelephonyProperty(mainCapabilityPhoneId, "vendor.gsm.external.sim.enabled", "0");
                TelephonyManager.getDefault();
                String telephonyProperty4 = TelephonyManager.getTelephonyProperty(mainCapabilityPhoneId, "vendor.gsm.external.sim.inserted", "0");
                int preferedRsimSlot = ExternalSimManager.getPreferedRsimSlot();
                if ((telephonyProperty3.equals("1") && telephonyProperty4.equals(MtkGsmCdmaPhone.ACT_TYPE_UTRAN)) || (preferedRsimSlot != -1 && phoneId != preferedRsimSlot)) {
                    synchronized (this) {
                        this.mIsCapSwitching = false;
                    }
                    return 1;
                }
                if (SystemProperties.getInt("ro.vendor.mtk_non_dsda_rsim_support", 0) == 1 && preferedRsimSlot != -1 && preferedRsimSlot == phoneId) {
                    return 0;
                }
            }
            if (!this.mProxyControllerExt.isNeedSimSwitch(phoneId, this.mPhones.length)) {
                logd("check sim card type and skip setRadioCapability");
                this.mSetRafRetryCause = 0;
                this.mNextRafs = null;
                completeRadioCapabilityTransaction();
                return 1;
            }
            if (!WorldPhoneUtil.isWorldModeSupport() && WorldPhoneUtil.isWorldPhoneSupport()) {
                WorldPhoneUtil.getWorldPhone().notifyRadioCapabilityChange(phoneId);
            }
            mtkLogd("checkRadioCapabilitySwitchConditions, do switch");
            return 0;
        }
    }

    private void onRetryWhenRadioAvailable(Message message) {
        mtkLogd("onRetryWhenRadioAvailable,mSetRafRetryCause:" + this.mSetRafRetryCause);
        for (int i = 0; i < this.mPhones.length; i++) {
            if (RadioManager.isModemPowerOff(i)) {
                mtkLogd("onRetryWhenRadioAvailable, Phone" + i + " modem off");
                return;
            }
        }
        if (this.mNextRafs != null && this.mSetRafRetryCause == 4) {
            try {
                setRadioCapability(this.mNextRafs);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendCapabilityFailBroadcast() {
        if (this.mContext != null) {
            this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED"));
        }
    }

    private void registerWorldModeReceiverFor90Modem() {
        if (this.mContext == null) {
            logd("registerWorldModeReceiverFor90Modem, context is null => return");
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ModemSwitchHandler.ACTION_MODEM_SWITCH_DONE);
        this.mContext.registerReceiver(this.mWorldModeReceiver, intentFilter);
        this.mHasRegisterWorldModeReceiver = true;
    }

    private void unRegisterWorldModeReceiver() {
        if (this.mContext == null) {
            mtkLogd("unRegisterWorldModeReceiver, context is null => return");
        } else {
            this.mContext.unregisterReceiver(this.mWorldModeReceiver);
            this.mHasRegisterWorldModeReceiver = false;
        }
    }

    private void registerPhoneStateReceiver() {
        if (this.mContext == null) {
            mtkLogd("registerPhoneStateReceiver, context is null => return");
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        this.mContext.registerReceiver(this.mPhoneStateReceiver, intentFilter);
        this.mHasRegisterPhoneStateReceiver = true;
    }

    private void unRegisterPhoneStateReceiver() {
        if (this.mContext == null) {
            mtkLogd("unRegisterPhoneStateReceiver, context is null => return");
        } else {
            this.mContext.unregisterReceiver(this.mPhoneStateReceiver);
            this.mHasRegisterPhoneStateReceiver = false;
        }
    }

    private void registerEccStateReceiver() {
        if (this.mContext == null) {
            mtkLogd("registerEccStateReceiver, context is null => return");
            return;
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ECC_IN_PROGRESS");
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        this.mContext.registerReceiver(this.mEccStateReceiver, intentFilter);
        this.mHasRegisterEccStateReceiver = true;
    }

    private void unRegisterEccStateReceiver() {
        if (this.mContext == null) {
            mtkLogd("unRegisterEccStateReceiver, context is null => return");
        } else {
            this.mContext.unregisterReceiver(this.mEccStateReceiver);
            this.mHasRegisterEccStateReceiver = false;
        }
    }

    private boolean isEccInProgress() {
        boolean zIsEccInProgress;
        String str = SystemProperties.get("ril.cdma.inecmmode", "");
        boolean zContains = str.contains("true");
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (iMtkTelephonyExAsInterface != null) {
            try {
                zIsEccInProgress = iMtkTelephonyExAsInterface.isEccInProgress();
            } catch (RemoteException e) {
                loge("Exception of isEccInProgress");
                zIsEccInProgress = false;
            }
        } else {
            zIsEccInProgress = false;
        }
        logd("isEccInProgress, value:" + str + ", inEcm:" + zContains + ", isInEcc:" + zIsEccInProgress);
        return zContains || zIsEccInProgress;
    }
}
