package com.android.phone;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.NetworkStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.UserManager;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.carrier.CarrierIdentifier;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.CellInfo;
import android.telephony.ClientRequestStats;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.LocationAccessPolicy;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyHistogram;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.telephony.UssdResponse;
import android.telephony.VisualVoicemailSmsFilterSettings;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import com.android.ims.ImsManager;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CarrierInfoManager;
import com.android.internal.telephony.CellNetworkScanResult;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.LocaleTracker;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.NetworkScanRequestTracker;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstantConversions;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyPermissions;
import com.android.internal.telephony.euicc.EuiccConnector;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.telephony.uicc.UiccSlot;
import com.android.internal.telephony.util.VoicemailNotificationSettingsUtil;
import com.android.phone.vvm.PhoneAccountHandleConverter;
import com.android.phone.vvm.RemoteVvmTaskManager;
import com.android.phone.vvm.VisualVoicemailSettingsUtil;
import com.android.phone.vvm.VisualVoicemailSmsFilterConfig;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.phone.MtkPhoneInterfaceManagerEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhoneInterfaceManager extends ITelephony.Stub {
    private static final int CMD_ANSWER_RINGING_CALL = 4;
    private static final int CMD_CLOSE_CHANNEL = 11;
    private static final int CMD_END_CALL = 5;
    private static final int CMD_EXCHANGE_SIM_IO = 31;
    private static final int CMD_GET_ALLOWED_CARRIERS = 45;
    private static final int CMD_GET_FORBIDDEN_PLMNS = 48;
    private static final int CMD_GET_MODEM_ACTIVITY_INFO = 37;
    private static final int CMD_GET_PREFERRED_NETWORK_TYPE = 21;
    private static final int CMD_HANDLE_NEIGHBORING_CELL = 2;
    private static final int CMD_HANDLE_PIN_MMI = 1;
    private static final int CMD_HANDLE_USSD_REQUEST = 47;
    private static final int CMD_INVOKE_OEM_RIL_REQUEST_RAW = 27;
    private static final int CMD_NV_READ_ITEM = 13;
    private static final int CMD_NV_RESET_CONFIG = 19;
    private static final int CMD_NV_WRITE_CDMA_PRL = 17;
    private static final int CMD_NV_WRITE_ITEM = 15;
    private static final int CMD_OPEN_CHANNEL = 9;
    private static final int CMD_PERFORM_NETWORK_SCAN = 39;
    private static final int CMD_SEND_ENVELOPE = 25;
    private static final int CMD_SET_ALLOWED_CARRIERS = 43;
    private static final int CMD_SET_NETWORK_SELECTION_MODE_AUTOMATIC = 35;
    private static final int CMD_SET_NETWORK_SELECTION_MODE_MANUAL = 41;
    private static final int CMD_SET_PREFERRED_NETWORK_TYPE = 23;
    private static final int CMD_SET_VOICEMAIL_NUMBER = 33;
    private static final int CMD_SWITCH_SLOTS = 50;
    private static final int CMD_TRANSMIT_APDU_BASIC_CHANNEL = 29;
    private static final int CMD_TRANSMIT_APDU_LOGICAL_CHANNEL = 7;
    private static final boolean DBG = false;
    private static final boolean DBG_LOC = false;
    private static final boolean DBG_MERGE = false;
    private static final String DEFAULT_DATA_ROAMING_PROPERTY_NAME = "ro.com.android.dataroaming";
    private static final String DEFAULT_MOBILE_DATA_PROPERTY_NAME = "ro.com.android.mobiledata";
    private static final String DEFAULT_NETWORK_MODE_PROPERTY_NAME = "ro.telephony.default_network";
    private static final int EVENT_CLOSE_CHANNEL_DONE = 12;
    private static final int EVENT_EXCHANGE_SIM_IO_DONE = 32;
    private static final int EVENT_GET_ALLOWED_CARRIERS_DONE = 46;
    private static final int EVENT_GET_FORBIDDEN_PLMNS_DONE = 49;
    private static final int EVENT_GET_MODEM_ACTIVITY_INFO_DONE = 38;
    private static final int EVENT_GET_PREFERRED_NETWORK_TYPE_DONE = 22;
    private static final int EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE = 28;
    private static final int EVENT_NEIGHBORING_CELL_DONE = 3;
    private static final int EVENT_NV_READ_ITEM_DONE = 14;
    private static final int EVENT_NV_RESET_CONFIG_DONE = 20;
    private static final int EVENT_NV_WRITE_CDMA_PRL_DONE = 18;
    private static final int EVENT_NV_WRITE_ITEM_DONE = 16;
    private static final int EVENT_OPEN_CHANNEL_DONE = 10;
    private static final int EVENT_PERFORM_NETWORK_SCAN_DONE = 40;
    private static final int EVENT_SEND_ENVELOPE_DONE = 26;
    private static final int EVENT_SET_ALLOWED_CARRIERS_DONE = 44;
    private static final int EVENT_SET_NETWORK_SELECTION_MODE_AUTOMATIC_DONE = 36;
    private static final int EVENT_SET_NETWORK_SELECTION_MODE_MANUAL_DONE = 42;
    private static final int EVENT_SET_PREFERRED_NETWORK_TYPE_DONE = 24;
    private static final int EVENT_SET_VOICEMAIL_NUMBER_DONE = 34;
    private static final int EVENT_SWITCH_SLOTS_DONE = 51;
    private static final int EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE = 30;
    private static final int EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE = 8;
    private static final String ISDR_AID = "A0000005591010FFFFFFFF8900000100";
    private static final String LOG_TAG = "PhoneInterfaceManager";
    private static final String PREF_CARRIERS_ALPHATAG_PREFIX = "carrier_alphtag_";
    private static final String PREF_CARRIERS_NUMBER_PREFIX = "carrier_number_";
    private static final String PREF_CARRIERS_SUBSCRIBER_PREFIX = "carrier_subscriber_";
    private static final int SELECT_COMMAND = 164;
    private static final int SELECT_P1 = 4;
    private static final int SELECT_P2 = 0;
    private static final int SELECT_P3 = 16;
    private static PhoneInterfaceManager sInstance;
    private PhoneGlobals mApp;
    private AppOpsManager mAppOps;
    private Phone mPhone;
    private SharedPreferences mTelephonySharedPreferences;
    private UserManager mUserManager;
    private final ModemActivityInfo mLastModemActivityInfo = new ModemActivityInfo(0, 0, 0, new int[0], 0, 0);
    private CallManager mCM = PhoneGlobals.getInstance().mCM;
    private MainThreadHandler mMainThreadHandler = new MainThreadHandler(this, null);
    private SubscriptionController mSubscriptionController = SubscriptionController.getInstance();
    private NetworkScanRequestTracker mNetworkScanRequestTracker = new NetworkScanRequestTracker();

    private static final class IccAPDUArgument {
        public int channel;
        public int cla;
        public int command;
        public String data;
        public int p1;
        public int p2;
        public int p3;

        public IccAPDUArgument(int i, int i2, int i3, int i4, int i5, int i6, String str) {
            this.channel = i;
            this.cla = i2;
            this.command = i3;
            this.p1 = i4;
            this.p2 = i5;
            this.p3 = i6;
            this.data = str;
        }
    }

    private static final class ManualNetworkSelectionArgument {
        public OperatorInfo operatorInfo;
        public boolean persistSelection;

        public ManualNetworkSelectionArgument(OperatorInfo operatorInfo, boolean z) {
            this.operatorInfo = operatorInfo;
            this.persistSelection = z;
        }
    }

    private static final class MainThreadRequest {
        public Object argument;
        public Object result;
        public Integer subId;

        public MainThreadRequest(Object obj) {
            this.subId = -1;
            this.argument = obj;
        }

        public MainThreadRequest(Object obj, Integer num) {
            this.subId = -1;
            this.argument = obj;
            if (num != null) {
                this.subId = num;
            }
        }
    }

    private static final class IncomingThirdPartyCallArgs {
        public final String callId;
        public final String callerDisplayName;
        public final ComponentName component;

        public IncomingThirdPartyCallArgs(ComponentName componentName, String str, String str2) {
            this.component = componentName;
            this.callId = str;
            this.callerDisplayName = str2;
        }
    }

    private final class MainThreadHandler extends Handler {
        private MainThreadHandler() {
        }

        MainThreadHandler(PhoneInterfaceManager phoneInterfaceManager, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void handleMessage(Message message) {
            boolean zHangup;
            IccOpenLogicalChannelResponse iccOpenLogicalChannelResponse;
            CellNetworkScanResult cellNetworkScanResult;
            boolean zHandleUssdRequest;
            int i = 4;
            byte[] bArr = null;
            switch (message.what) {
                case 1:
                    MainThreadRequest mainThreadRequest = (MainThreadRequest) message.obj;
                    mainThreadRequest.result = Boolean.valueOf(PhoneInterfaceManager.this.getPhoneFromRequest(mainThreadRequest) != null ? PhoneInterfaceManager.this.getPhoneFromRequest(mainThreadRequest).handlePinMmi((String) mainThreadRequest.argument) : false);
                    synchronized (mainThreadRequest) {
                        mainThreadRequest.notifyAll();
                        break;
                    }
                    return;
                case 2:
                    MainThreadRequest mainThreadRequest2 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.mPhone.getNeighboringCids(obtainMessage(3, mainThreadRequest2), (WorkSource) mainThreadRequest2.argument);
                    return;
                case 3:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest3 = (MainThreadRequest) asyncResult.userObj;
                    if (asyncResult.exception == null && asyncResult.result != null) {
                        mainThreadRequest3.result = asyncResult.result;
                    } else {
                        mainThreadRequest3.result = new ArrayList(0);
                    }
                    synchronized (mainThreadRequest3) {
                        mainThreadRequest3.notifyAll();
                        break;
                    }
                    return;
                case 4:
                    MainThreadRequest mainThreadRequest4 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.answerRingingCallInternal(mainThreadRequest4.subId.intValue());
                    synchronized (mainThreadRequest4) {
                        mainThreadRequest4.result = "OK";
                        mainThreadRequest4.notifyAll();
                        break;
                    }
                    return;
                case 5:
                    MainThreadRequest mainThreadRequest5 = (MainThreadRequest) message.obj;
                    int iIntValue = mainThreadRequest5.subId.intValue();
                    Phone phone = PhoneInterfaceManager.this.getPhone(iIntValue);
                    if (phone == null) {
                        synchronized (mainThreadRequest5) {
                            mainThreadRequest5.result = false;
                            mainThreadRequest5.notifyAll();
                            break;
                        }
                        return;
                    }
                    int phoneType = phone.getPhoneType();
                    if (phoneType == 2) {
                        zHangup = PhoneUtils.hangupRingingAndActive(PhoneInterfaceManager.this.getPhone(iIntValue));
                    } else if (phoneType == 1) {
                        zHangup = PhoneUtils.hangup(PhoneInterfaceManager.this.mCM);
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }
                    mainThreadRequest5.result = Boolean.valueOf(zHangup);
                    synchronized (mainThreadRequest5) {
                        mainThreadRequest5.notifyAll();
                        break;
                    }
                    return;
                case 6:
                default:
                    Log.w(PhoneInterfaceManager.LOG_TAG, "MainThreadHandler: unexpected message code: " + message.what);
                    return;
                case 7:
                    MainThreadRequest mainThreadRequest6 = (MainThreadRequest) message.obj;
                    IccAPDUArgument iccAPDUArgument = (IccAPDUArgument) mainThreadRequest6.argument;
                    UiccCard uiccCardFromRequest = PhoneInterfaceManager.this.getUiccCardFromRequest(mainThreadRequest6);
                    if (uiccCardFromRequest == null) {
                        PhoneInterfaceManager.loge("iccTransmitApduLogicalChannel: No UICC");
                        mainThreadRequest6.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (mainThreadRequest6) {
                            mainThreadRequest6.notifyAll();
                            break;
                        }
                        return;
                    }
                    uiccCardFromRequest.iccTransmitApduLogicalChannel(iccAPDUArgument.channel, iccAPDUArgument.cla, iccAPDUArgument.command, iccAPDUArgument.p1, iccAPDUArgument.p2, iccAPDUArgument.p3, iccAPDUArgument.data, obtainMessage(8, mainThreadRequest6));
                    return;
                case 8:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest7 = (MainThreadRequest) asyncResult2.userObj;
                    if (asyncResult2.exception == null && asyncResult2.result != null) {
                        mainThreadRequest7.result = asyncResult2.result;
                    } else {
                        mainThreadRequest7.result = new IccIoResult(111, 0, (byte[]) null);
                        if (asyncResult2.result == null) {
                            PhoneInterfaceManager.loge("iccTransmitApduLogicalChannel: Empty response");
                        } else if (!(asyncResult2.exception instanceof CommandException)) {
                            PhoneInterfaceManager.loge("iccTransmitApduLogicalChannel: Unknown exception");
                        } else {
                            PhoneInterfaceManager.loge("iccTransmitApduLogicalChannel: CommandException: " + asyncResult2.exception);
                        }
                    }
                    synchronized (mainThreadRequest7) {
                        mainThreadRequest7.notifyAll();
                        break;
                    }
                    return;
                case 9:
                    MainThreadRequest mainThreadRequest8 = (MainThreadRequest) message.obj;
                    UiccCard uiccCardFromRequest2 = PhoneInterfaceManager.this.getUiccCardFromRequest(mainThreadRequest8);
                    Pair pair = (Pair) mainThreadRequest8.argument;
                    if (uiccCardFromRequest2 == null) {
                        PhoneInterfaceManager.loge("iccOpenLogicalChannel: No UICC");
                        mainThreadRequest8.result = new IccOpenLogicalChannelResponse(-1, 2, null);
                        synchronized (mainThreadRequest8) {
                            mainThreadRequest8.notifyAll();
                            break;
                        }
                        return;
                    }
                    uiccCardFromRequest2.iccOpenLogicalChannel((String) pair.first, ((Integer) pair.second).intValue(), obtainMessage(10, mainThreadRequest8));
                    return;
                case 10:
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest9 = (MainThreadRequest) asyncResult3.userObj;
                    if (asyncResult3.exception == null && asyncResult3.result != null) {
                        int[] iArr = (int[]) asyncResult3.result;
                        int i2 = iArr[0];
                        if (iArr.length > 1) {
                            bArr = new byte[iArr.length - 1];
                            for (int i3 = 1; i3 < iArr.length; i3++) {
                                bArr[i3 - 1] = (byte) iArr[i3];
                            }
                        }
                        iccOpenLogicalChannelResponse = new IccOpenLogicalChannelResponse(i2, 1, bArr);
                    } else {
                        if (asyncResult3.result == null) {
                            PhoneInterfaceManager.loge("iccOpenLogicalChannel: Empty response");
                        }
                        if (asyncResult3.exception != null) {
                            PhoneInterfaceManager.loge("iccOpenLogicalChannel: Exception: " + asyncResult3.exception);
                        }
                        if (asyncResult3.exception instanceof CommandException) {
                            CommandException.Error commandError = asyncResult3.exception.getCommandError();
                            if (commandError != CommandException.Error.MISSING_RESOURCE) {
                                if (commandError == CommandException.Error.NO_SUCH_ELEMENT) {
                                    i = 3;
                                }
                            } else {
                                i = 2;
                            }
                        }
                        iccOpenLogicalChannelResponse = new IccOpenLogicalChannelResponse(-1, i, null);
                    }
                    mainThreadRequest9.result = iccOpenLogicalChannelResponse;
                    synchronized (mainThreadRequest9) {
                        mainThreadRequest9.notifyAll();
                        break;
                    }
                    return;
                case 11:
                    MainThreadRequest mainThreadRequest10 = (MainThreadRequest) message.obj;
                    UiccCard uiccCardFromRequest3 = PhoneInterfaceManager.this.getUiccCardFromRequest(mainThreadRequest10);
                    if (uiccCardFromRequest3 == null) {
                        PhoneInterfaceManager.loge("iccCloseLogicalChannel: No UICC");
                        mainThreadRequest10.result = false;
                        synchronized (mainThreadRequest10) {
                            mainThreadRequest10.notifyAll();
                            break;
                        }
                        return;
                    }
                    uiccCardFromRequest3.iccCloseLogicalChannel(((Integer) mainThreadRequest10.argument).intValue(), obtainMessage(12, mainThreadRequest10));
                    return;
                case 12:
                    handleNullReturnEvent(message, "iccCloseLogicalChannel");
                    return;
                case 13:
                    MainThreadRequest mainThreadRequest11 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.mPhone.nvReadItem(((Integer) mainThreadRequest11.argument).intValue(), obtainMessage(14, mainThreadRequest11));
                    return;
                case 14:
                    AsyncResult asyncResult4 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest12 = (MainThreadRequest) asyncResult4.userObj;
                    if (asyncResult4.exception == null && asyncResult4.result != null) {
                        mainThreadRequest12.result = asyncResult4.result;
                    } else {
                        mainThreadRequest12.result = "";
                        if (asyncResult4.result == null) {
                            PhoneInterfaceManager.loge("nvReadItem: Empty response");
                        } else if (!(asyncResult4.exception instanceof CommandException)) {
                            PhoneInterfaceManager.loge("nvReadItem: Unknown exception");
                        } else {
                            PhoneInterfaceManager.loge("nvReadItem: CommandException: " + asyncResult4.exception);
                        }
                    }
                    synchronized (mainThreadRequest12) {
                        mainThreadRequest12.notifyAll();
                        break;
                    }
                    return;
                case 15:
                    MainThreadRequest mainThreadRequest13 = (MainThreadRequest) message.obj;
                    Message messageObtainMessage = obtainMessage(16, mainThreadRequest13);
                    Pair pair2 = (Pair) mainThreadRequest13.argument;
                    PhoneInterfaceManager.this.mPhone.nvWriteItem(((Integer) pair2.first).intValue(), (String) pair2.second, messageObtainMessage);
                    return;
                case 16:
                    handleNullReturnEvent(message, "nvWriteItem");
                    return;
                case 17:
                    MainThreadRequest mainThreadRequest14 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.mPhone.nvWriteCdmaPrl((byte[]) mainThreadRequest14.argument, obtainMessage(18, mainThreadRequest14));
                    return;
                case 18:
                    handleNullReturnEvent(message, "nvWriteCdmaPrl");
                    return;
                case 19:
                    MainThreadRequest mainThreadRequest15 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.mPhone.nvResetConfig(((Integer) mainThreadRequest15.argument).intValue(), obtainMessage(20, mainThreadRequest15));
                    return;
                case 20:
                    handleNullReturnEvent(message, "nvResetConfig");
                    return;
                case 21:
                    MainThreadRequest mainThreadRequest16 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.getPhoneFromRequest(mainThreadRequest16).getPreferredNetworkType(obtainMessage(22, mainThreadRequest16));
                    return;
                case 22:
                    AsyncResult asyncResult5 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest17 = (MainThreadRequest) asyncResult5.userObj;
                    if (asyncResult5.exception == null && asyncResult5.result != null) {
                        mainThreadRequest17.result = asyncResult5.result;
                    } else {
                        mainThreadRequest17.result = null;
                        if (asyncResult5.result == null) {
                            PhoneInterfaceManager.loge("getPreferredNetworkType: Empty response");
                        } else if (!(asyncResult5.exception instanceof CommandException)) {
                            PhoneInterfaceManager.loge("getPreferredNetworkType: Unknown exception");
                        } else {
                            PhoneInterfaceManager.loge("getPreferredNetworkType: CommandException: " + asyncResult5.exception);
                        }
                    }
                    synchronized (mainThreadRequest17) {
                        mainThreadRequest17.notifyAll();
                        break;
                    }
                    return;
                case 23:
                    MainThreadRequest mainThreadRequest18 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.getPhoneFromRequest(mainThreadRequest18).setPreferredNetworkType(((Integer) mainThreadRequest18.argument).intValue(), obtainMessage(24, mainThreadRequest18));
                    return;
                case 24:
                    handleNullReturnEvent(message, "setPreferredNetworkType");
                    return;
                case 25:
                    MainThreadRequest mainThreadRequest19 = (MainThreadRequest) message.obj;
                    UiccCard uiccCardFromRequest4 = PhoneInterfaceManager.this.getUiccCardFromRequest(mainThreadRequest19);
                    if (uiccCardFromRequest4 == null) {
                        PhoneInterfaceManager.loge("sendEnvelopeWithStatus: No UICC");
                        mainThreadRequest19.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (mainThreadRequest19) {
                            mainThreadRequest19.notifyAll();
                            break;
                        }
                        return;
                    }
                    uiccCardFromRequest4.sendEnvelopeWithStatus((String) mainThreadRequest19.argument, obtainMessage(26, mainThreadRequest19));
                    return;
                case 26:
                    AsyncResult asyncResult6 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest20 = (MainThreadRequest) asyncResult6.userObj;
                    if (asyncResult6.exception == null && asyncResult6.result != null) {
                        mainThreadRequest20.result = asyncResult6.result;
                    } else {
                        mainThreadRequest20.result = new IccIoResult(111, 0, (byte[]) null);
                        if (asyncResult6.result == null) {
                            PhoneInterfaceManager.loge("sendEnvelopeWithStatus: Empty response");
                        } else if (asyncResult6.exception instanceof CommandException) {
                            PhoneInterfaceManager.loge("sendEnvelopeWithStatus: CommandException: " + asyncResult6.exception);
                        } else {
                            PhoneInterfaceManager.loge("sendEnvelopeWithStatus: exception:" + asyncResult6.exception);
                        }
                    }
                    synchronized (mainThreadRequest20) {
                        mainThreadRequest20.notifyAll();
                        break;
                    }
                    return;
                case 27:
                    MainThreadRequest mainThreadRequest21 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.mPhone.invokeOemRilRequestRaw((byte[]) mainThreadRequest21.argument, obtainMessage(28, mainThreadRequest21));
                    return;
                case 28:
                    AsyncResult asyncResult7 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest22 = (MainThreadRequest) asyncResult7.userObj;
                    mainThreadRequest22.result = asyncResult7;
                    synchronized (mainThreadRequest22) {
                        mainThreadRequest22.notifyAll();
                        break;
                    }
                    return;
                case 29:
                    MainThreadRequest mainThreadRequest23 = (MainThreadRequest) message.obj;
                    IccAPDUArgument iccAPDUArgument2 = (IccAPDUArgument) mainThreadRequest23.argument;
                    UiccCard uiccCardFromRequest5 = PhoneInterfaceManager.this.getUiccCardFromRequest(mainThreadRequest23);
                    if (uiccCardFromRequest5 == null) {
                        PhoneInterfaceManager.loge("iccTransmitApduBasicChannel: No UICC");
                        mainThreadRequest23.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (mainThreadRequest23) {
                            mainThreadRequest23.notifyAll();
                            break;
                        }
                        return;
                    }
                    uiccCardFromRequest5.iccTransmitApduBasicChannel(iccAPDUArgument2.cla, iccAPDUArgument2.command, iccAPDUArgument2.p1, iccAPDUArgument2.p2, iccAPDUArgument2.p3, iccAPDUArgument2.data, obtainMessage(30, mainThreadRequest23));
                    return;
                case 30:
                    AsyncResult asyncResult8 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest24 = (MainThreadRequest) asyncResult8.userObj;
                    if (asyncResult8.exception == null && asyncResult8.result != null) {
                        mainThreadRequest24.result = asyncResult8.result;
                    } else {
                        mainThreadRequest24.result = new IccIoResult(111, 0, (byte[]) null);
                        if (asyncResult8.result == null) {
                            PhoneInterfaceManager.loge("iccTransmitApduBasicChannel: Empty response");
                        } else if (!(asyncResult8.exception instanceof CommandException)) {
                            PhoneInterfaceManager.loge("iccTransmitApduBasicChannel: Unknown exception");
                        } else {
                            PhoneInterfaceManager.loge("iccTransmitApduBasicChannel: CommandException: " + asyncResult8.exception);
                        }
                    }
                    synchronized (mainThreadRequest24) {
                        mainThreadRequest24.notifyAll();
                        break;
                    }
                    return;
                case 31:
                    MainThreadRequest mainThreadRequest25 = (MainThreadRequest) message.obj;
                    IccAPDUArgument iccAPDUArgument3 = (IccAPDUArgument) mainThreadRequest25.argument;
                    UiccCard uiccCardFromRequest6 = PhoneInterfaceManager.this.getUiccCardFromRequest(mainThreadRequest25);
                    if (uiccCardFromRequest6 == null) {
                        PhoneInterfaceManager.loge("iccExchangeSimIO: No UICC");
                        mainThreadRequest25.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (mainThreadRequest25) {
                            mainThreadRequest25.notifyAll();
                            break;
                        }
                        return;
                    }
                    uiccCardFromRequest6.iccExchangeSimIO(iccAPDUArgument3.cla, iccAPDUArgument3.command, iccAPDUArgument3.p1, iccAPDUArgument3.p2, iccAPDUArgument3.p3, iccAPDUArgument3.data, obtainMessage(32, mainThreadRequest25));
                    return;
                case 32:
                    AsyncResult asyncResult9 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest26 = (MainThreadRequest) asyncResult9.userObj;
                    if (asyncResult9.exception == null && asyncResult9.result != null) {
                        mainThreadRequest26.result = asyncResult9.result;
                    } else {
                        mainThreadRequest26.result = new IccIoResult(111, 0, (byte[]) null);
                    }
                    synchronized (mainThreadRequest26) {
                        mainThreadRequest26.notifyAll();
                        break;
                    }
                    return;
                case 33:
                    MainThreadRequest mainThreadRequest27 = (MainThreadRequest) message.obj;
                    Message messageObtainMessage2 = obtainMessage(34, mainThreadRequest27);
                    Pair pair3 = (Pair) mainThreadRequest27.argument;
                    PhoneInterfaceManager.this.getPhoneFromRequest(mainThreadRequest27).setVoiceMailNumber((String) pair3.first, (String) pair3.second, messageObtainMessage2);
                    return;
                case 34:
                    handleNullReturnEvent(message, "setVoicemailNumber");
                    return;
                case 35:
                    MainThreadRequest mainThreadRequest28 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.getPhoneFromRequest(mainThreadRequest28).setNetworkSelectionModeAutomatic(obtainMessage(36, mainThreadRequest28));
                    return;
                case 36:
                    handleNullReturnEvent(message, "setNetworkSelectionModeAutomatic");
                    return;
                case 37:
                    PhoneInterfaceManager.this.mPhone.getModemActivityInfo(obtainMessage(38, (MainThreadRequest) message.obj));
                    return;
                case 38:
                    AsyncResult asyncResult10 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest29 = (MainThreadRequest) asyncResult10.userObj;
                    if (asyncResult10.exception == null && asyncResult10.result != null) {
                        mainThreadRequest29.result = asyncResult10.result;
                    } else if (asyncResult10.result == null) {
                        PhoneInterfaceManager.loge("queryModemActivityInfo: Empty response");
                    } else if (!(asyncResult10.exception instanceof CommandException)) {
                        PhoneInterfaceManager.loge("queryModemActivityInfo: Unknown exception");
                    } else {
                        PhoneInterfaceManager.loge("queryModemActivityInfo: CommandException: " + asyncResult10.exception);
                    }
                    if (mainThreadRequest29.result == null) {
                        mainThreadRequest29.result = new ModemActivityInfo(0L, 0, 0, (int[]) null, 0, 0);
                    }
                    synchronized (mainThreadRequest29) {
                        mainThreadRequest29.notifyAll();
                        break;
                    }
                    return;
                case 39:
                    MainThreadRequest mainThreadRequest30 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.getPhoneFromRequest(mainThreadRequest30).getAvailableNetworks(obtainMessage(40, mainThreadRequest30));
                    return;
                case 40:
                    AsyncResult asyncResult11 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest31 = (MainThreadRequest) asyncResult11.userObj;
                    if (asyncResult11.exception == null && asyncResult11.result != null) {
                        cellNetworkScanResult = new CellNetworkScanResult(1, (List) asyncResult11.result);
                    } else {
                        if (asyncResult11.result == null) {
                            PhoneInterfaceManager.loge("getCellNetworkScanResults: Empty response");
                        }
                        if (asyncResult11.exception != null) {
                            PhoneInterfaceManager.loge("getCellNetworkScanResults: Exception: " + asyncResult11.exception);
                        }
                        if (asyncResult11.exception instanceof CommandException) {
                            CommandException.Error commandError2 = asyncResult11.exception.getCommandError();
                            if (commandError2 != CommandException.Error.RADIO_NOT_AVAILABLE) {
                                if (commandError2 == CommandException.Error.GENERIC_FAILURE) {
                                    i = 3;
                                }
                            } else {
                                i = 2;
                            }
                        }
                        cellNetworkScanResult = new CellNetworkScanResult(i, (List) null);
                    }
                    mainThreadRequest31.result = cellNetworkScanResult;
                    synchronized (mainThreadRequest31) {
                        mainThreadRequest31.notifyAll();
                        break;
                    }
                    return;
                case 41:
                    MainThreadRequest mainThreadRequest32 = (MainThreadRequest) message.obj;
                    ManualNetworkSelectionArgument manualNetworkSelectionArgument = (ManualNetworkSelectionArgument) mainThreadRequest32.argument;
                    PhoneInterfaceManager.this.getPhoneFromRequest(mainThreadRequest32).selectNetworkManually(manualNetworkSelectionArgument.operatorInfo, manualNetworkSelectionArgument.persistSelection, obtainMessage(42, mainThreadRequest32));
                    return;
                case 42:
                    handleNullReturnEvent(message, "setNetworkSelectionModeManual");
                    return;
                case 43:
                    MainThreadRequest mainThreadRequest33 = (MainThreadRequest) message.obj;
                    PhoneInterfaceManager.this.mPhone.setAllowedCarriers((List) mainThreadRequest33.argument, obtainMessage(44, mainThreadRequest33));
                    return;
                case 44:
                    AsyncResult asyncResult12 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest34 = (MainThreadRequest) asyncResult12.userObj;
                    if (asyncResult12.exception == null && asyncResult12.result != null) {
                        mainThreadRequest34.result = asyncResult12.result;
                    } else if (asyncResult12.result == null) {
                        PhoneInterfaceManager.loge("setAllowedCarriers: Empty response");
                    } else if (!(asyncResult12.exception instanceof CommandException)) {
                        PhoneInterfaceManager.loge("setAllowedCarriers: Unknown exception");
                    } else {
                        PhoneInterfaceManager.loge("setAllowedCarriers: CommandException: " + asyncResult12.exception);
                    }
                    if (mainThreadRequest34.result == null) {
                        mainThreadRequest34.result = new int[]{-1};
                    }
                    synchronized (mainThreadRequest34) {
                        mainThreadRequest34.notifyAll();
                        break;
                    }
                    return;
                case 45:
                    PhoneInterfaceManager.this.mPhone.getAllowedCarriers(obtainMessage(46, (MainThreadRequest) message.obj));
                    return;
                case 46:
                    AsyncResult asyncResult13 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest35 = (MainThreadRequest) asyncResult13.userObj;
                    if (asyncResult13.exception == null && asyncResult13.result != null) {
                        mainThreadRequest35.result = asyncResult13.result;
                    } else if (asyncResult13.result == null) {
                        PhoneInterfaceManager.loge("getAllowedCarriers: Empty response");
                    } else if (!(asyncResult13.exception instanceof CommandException)) {
                        PhoneInterfaceManager.loge("getAllowedCarriers: Unknown exception");
                    } else {
                        PhoneInterfaceManager.loge("getAllowedCarriers: CommandException: " + asyncResult13.exception);
                    }
                    if (mainThreadRequest35.result == null) {
                        mainThreadRequest35.result = new ArrayList(0);
                    }
                    synchronized (mainThreadRequest35) {
                        mainThreadRequest35.notifyAll();
                        break;
                    }
                    return;
                case 47:
                    MainThreadRequest mainThreadRequest36 = (MainThreadRequest) message.obj;
                    Phone phoneFromRequest = PhoneInterfaceManager.this.getPhoneFromRequest(mainThreadRequest36);
                    Pair pair4 = (Pair) mainThreadRequest36.argument;
                    String str = (String) pair4.first;
                    ResultReceiver resultReceiver = (ResultReceiver) pair4.second;
                    if (!PhoneInterfaceManager.this.isUssdApiAllowed(mainThreadRequest36.subId.intValue())) {
                        Rlog.w(PhoneInterfaceManager.LOG_TAG, "handleUssdRequest: carrier does not support USSD apis.");
                        UssdResponse ussdResponse = new UssdResponse(str, (CharSequence) null);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("USSD_RESPONSE", ussdResponse);
                        resultReceiver.send(-1, bundle);
                        mainThreadRequest36.result = true;
                        synchronized (mainThreadRequest36) {
                            mainThreadRequest36.notifyAll();
                            break;
                        }
                        return;
                    }
                    if (phoneFromRequest == null) {
                        zHandleUssdRequest = false;
                    } else {
                        try {
                            zHandleUssdRequest = phoneFromRequest.handleUssdRequest(str, resultReceiver);
                        } catch (CallStateException e) {
                            mainThreadRequest36.result = false;
                        }
                    }
                    mainThreadRequest36.result = Boolean.valueOf(zHandleUssdRequest);
                    synchronized (mainThreadRequest36) {
                        mainThreadRequest36.notifyAll();
                        break;
                    }
                    return;
                case 48:
                    MainThreadRequest mainThreadRequest37 = (MainThreadRequest) message.obj;
                    UiccCard uiccCardFromRequest7 = PhoneInterfaceManager.this.getUiccCardFromRequest(mainThreadRequest37);
                    if (uiccCardFromRequest7 == null) {
                        PhoneInterfaceManager.loge("getForbiddenPlmns() UiccCard is null");
                        mainThreadRequest37.result = new IllegalArgumentException("getForbiddenPlmns() UiccCard is null");
                        synchronized (mainThreadRequest37) {
                            mainThreadRequest37.notifyAll();
                            break;
                        }
                        return;
                    }
                    Integer num = (Integer) mainThreadRequest37.argument;
                    UiccCardApplication applicationByType = uiccCardFromRequest7.getApplicationByType(num.intValue());
                    if (applicationByType == null) {
                        PhoneInterfaceManager.loge("getForbiddenPlmns() no app with specified type -- " + num);
                        mainThreadRequest37.result = new IllegalArgumentException("Failed to get UICC App");
                        synchronized (mainThreadRequest37) {
                            mainThreadRequest37.notifyAll();
                            break;
                        }
                        return;
                    }
                    applicationByType.getIccRecords().getForbiddenPlmns(obtainMessage(49, mainThreadRequest37));
                    return;
                case 49:
                    AsyncResult asyncResult14 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest38 = (MainThreadRequest) asyncResult14.userObj;
                    if (asyncResult14.exception == null && asyncResult14.result != null) {
                        mainThreadRequest38.result = asyncResult14.result;
                    } else {
                        mainThreadRequest38.result = new IllegalArgumentException("Failed to retrieve Forbidden Plmns");
                        if (asyncResult14.result == null) {
                            PhoneInterfaceManager.loge("getForbiddenPlmns: Empty response");
                        } else {
                            PhoneInterfaceManager.loge("getForbiddenPlmns: Unknown exception");
                        }
                    }
                    synchronized (mainThreadRequest38) {
                        mainThreadRequest38.notifyAll();
                        break;
                    }
                    return;
                case 50:
                    MainThreadRequest mainThreadRequest39 = (MainThreadRequest) message.obj;
                    UiccController.getInstance().switchSlots((int[]) mainThreadRequest39.argument, obtainMessage(51, mainThreadRequest39));
                    return;
                case 51:
                    AsyncResult asyncResult15 = (AsyncResult) message.obj;
                    MainThreadRequest mainThreadRequest40 = (MainThreadRequest) asyncResult15.userObj;
                    mainThreadRequest40.result = Boolean.valueOf(asyncResult15.exception == null);
                    synchronized (mainThreadRequest40) {
                        mainThreadRequest40.notifyAll();
                        break;
                    }
                    return;
            }
        }

        private void handleNullReturnEvent(Message message, String str) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            MainThreadRequest mainThreadRequest = (MainThreadRequest) asyncResult.userObj;
            if (asyncResult.exception == null) {
                mainThreadRequest.result = true;
            } else {
                mainThreadRequest.result = false;
                if (asyncResult.exception instanceof CommandException) {
                    PhoneInterfaceManager.loge(str + ": CommandException: " + asyncResult.exception);
                } else {
                    PhoneInterfaceManager.loge(str + ": Unknown exception");
                }
            }
            synchronized (mainThreadRequest) {
                mainThreadRequest.notifyAll();
            }
        }
    }

    private Object sendRequest(int i, Object obj) {
        return sendRequest(i, obj, -1);
    }

    private Object sendRequest(int i, Object obj, Integer num) {
        if (Looper.myLooper() == this.mMainThreadHandler.getLooper()) {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }
        MainThreadRequest mainThreadRequest = new MainThreadRequest(obj, num);
        this.mMainThreadHandler.obtainMessage(i, mainThreadRequest).sendToTarget();
        synchronized (mainThreadRequest) {
            while (mainThreadRequest.result == null) {
                try {
                    mainThreadRequest.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mainThreadRequest.result;
    }

    private void sendRequestAsync(int i) {
        this.mMainThreadHandler.sendEmptyMessage(i);
    }

    private void sendRequestAsync(int i, Object obj) {
        this.mMainThreadHandler.obtainMessage(i, new MainThreadRequest(obj)).sendToTarget();
    }

    static PhoneInterfaceManager init(PhoneGlobals phoneGlobals, Phone phone) {
        PhoneInterfaceManager phoneInterfaceManager;
        synchronized (PhoneInterfaceManager.class) {
            if (sInstance == null) {
                sInstance = new PhoneInterfaceManager(phoneGlobals, phone);
                MtkPhoneInterfaceManagerEx.init(phoneGlobals, phone);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            phoneInterfaceManager = sInstance;
        }
        return phoneInterfaceManager;
    }

    private PhoneInterfaceManager(PhoneGlobals phoneGlobals, Phone phone) {
        this.mApp = phoneGlobals;
        this.mPhone = phone;
        this.mUserManager = (UserManager) phoneGlobals.getSystemService("user");
        this.mAppOps = (AppOpsManager) phoneGlobals.getSystemService("appops");
        this.mTelephonySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        publish();
    }

    private void publish() {
        ServiceManager.addService("phone", this);
    }

    private Phone getPhoneFromRequest(MainThreadRequest mainThreadRequest) {
        return mainThreadRequest.subId.intValue() == -1 ? this.mPhone : getPhone(mainThreadRequest.subId.intValue());
    }

    private UiccCard getUiccCardFromRequest(MainThreadRequest mainThreadRequest) {
        Phone phoneFromRequest = getPhoneFromRequest(mainThreadRequest);
        if (phoneFromRequest == null) {
            return null;
        }
        return UiccController.getInstance().getUiccCard(phoneFromRequest.getPhoneId());
    }

    private Phone getPhone(int i) {
        return PhoneFactory.getPhone(this.mSubscriptionController.getPhoneId(i));
    }

    public void dial(String str) {
        dialForSubscriber(getPreferredVoiceSubscription(), str);
    }

    public void dialForSubscriber(int i, String str) {
        PhoneConstants.State state;
        String strCreateTelUrl = createTelUrl(str);
        if (strCreateTelUrl != null && (state = this.mCM.getState(i)) != PhoneConstants.State.OFFHOOK && state != PhoneConstants.State.RINGING) {
            Intent intent = new Intent("android.intent.action.DIAL", Uri.parse(strCreateTelUrl));
            intent.addFlags(268435456);
            this.mApp.startActivity(intent);
        }
    }

    public void call(String str, String str2) {
        callForSubscriber(getPreferredVoiceSubscription(), str, str2);
    }

    public void callForSubscriber(int i, String str, String str2) {
        String strCreateTelUrl;
        enforceCallPermission();
        if (this.mAppOps.noteOp(13, Binder.getCallingUid(), str) != 0 || (strCreateTelUrl = createTelUrl(str2)) == null) {
            return;
        }
        boolean z = false;
        List<SubscriptionInfo> activeSubscriptionInfoList = getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                } else if (it.next().getSubscriptionId() == i) {
                    z = true;
                    break;
                }
            }
        }
        if (!z) {
            return;
        }
        Intent intent = new Intent("android.intent.action.CALL", Uri.parse(strCreateTelUrl));
        intent.putExtra("subscription", i);
        intent.addFlags(268435456);
        this.mApp.startActivity(intent);
    }

    public boolean endCall() {
        return endCallForSubscriber(getDefaultSubscription());
    }

    public boolean endCallForSubscriber(int i) {
        if (this.mApp.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            Log.i(LOG_TAG, "endCall: called without modify phone state.");
            EventLog.writeEvent(1397638484, "67862398", -1, "");
            throw new SecurityException("MODIFY_PHONE_STATE permission required.");
        }
        return ((Boolean) sendRequest(5, null, new Integer(i))).booleanValue();
    }

    public void answerRingingCall() {
        answerRingingCallForSubscriber(getDefaultSubscription());
    }

    public void answerRingingCallForSubscriber(int i) {
        enforceModifyPermission();
        sendRequest(4, null, new Integer(i));
    }

    private void answerRingingCallInternal(int i) {
        if (!getPhone(i).getRingingCall().isIdle()) {
            boolean z = !getPhone(i).getForegroundCall().isIdle();
            boolean z2 = !getPhone(i).getBackgroundCall().isIdle();
            if (z && z2) {
                PhoneUtils.answerAndEndActive(this.mCM, this.mCM.getFirstActiveRingingCall());
            } else {
                PhoneUtils.answerCall(this.mCM.getFirstActiveRingingCall());
            }
        }
    }

    public void silenceRinger() {
        Log.e(LOG_TAG, "silenseRinger not supported");
    }

    public boolean isOffhook(String str) {
        return isOffhookForSubscriber(getDefaultSubscription(), str);
    }

    public boolean isOffhookForSubscriber(int i, String str) {
        Phone phone;
        return TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "isOffhookForSubscriber") && (phone = getPhone(i)) != null && phone.getState() == PhoneConstants.State.OFFHOOK;
    }

    public boolean isRinging(String str) {
        return isRingingForSubscriber(getDefaultSubscription(), str);
    }

    public boolean isRingingForSubscriber(int i, String str) {
        Phone phone;
        return TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "isRingingForSubscriber") && (phone = getPhone(i)) != null && phone.getState() == PhoneConstants.State.RINGING;
    }

    public boolean isIdle(String str) {
        return isIdleForSubscriber(getDefaultSubscription(), str);
    }

    public boolean isIdleForSubscriber(int i, String str) {
        Phone phone;
        return TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "isIdleForSubscriber") && (phone = getPhone(i)) != null && phone.getState() == PhoneConstants.State.IDLE;
    }

    public boolean supplyPin(String str) {
        return supplyPinForSubscriber(getDefaultSubscription(), str);
    }

    public boolean supplyPinForSubscriber(int i, String str) {
        return supplyPinReportResultForSubscriber(i, str)[0] == 0;
    }

    public boolean supplyPuk(String str, String str2) {
        return supplyPukForSubscriber(getDefaultSubscription(), str, str2);
    }

    public boolean supplyPukForSubscriber(int i, String str, String str2) {
        return supplyPukReportResultForSubscriber(i, str, str2)[0] == 0;
    }

    public int[] supplyPinReportResult(String str) {
        return supplyPinReportResultForSubscriber(getDefaultSubscription(), str);
    }

    public int[] supplyPinReportResultForSubscriber(int i, String str) {
        enforceModifyPermission();
        UnlockSim unlockSim = new UnlockSim(getPhone(i).getIccCard());
        unlockSim.start();
        return unlockSim.unlockSim(null, str);
    }

    public int[] supplyPukReportResult(String str, String str2) {
        return supplyPukReportResultForSubscriber(getDefaultSubscription(), str, str2);
    }

    public int[] supplyPukReportResultForSubscriber(int i, String str, String str2) {
        enforceModifyPermission();
        UnlockSim unlockSim = new UnlockSim(getPhone(i).getIccCard());
        unlockSim.start();
        return unlockSim.unlockSim(str, str2);
    }

    private static class UnlockSim extends Thread {
        private static final int SUPPLY_PIN_COMPLETE = 100;
        private Handler mHandler;
        private final IccCard mSimCard;
        private boolean mDone = false;
        private int mResult = 2;
        private int mRetryCount = -1;

        public UnlockSim(IccCard iccCard) {
            this.mSimCard = iccCard;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (this) {
                this.mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        if (message.what == 100) {
                            Log.d(PhoneInterfaceManager.LOG_TAG, "SUPPLY_PIN_COMPLETE");
                            synchronized (UnlockSim.this) {
                                UnlockSim.this.mRetryCount = message.arg1;
                                if (asyncResult.exception == null) {
                                    UnlockSim.this.mResult = 0;
                                } else if (!(asyncResult.exception instanceof CommandException) || asyncResult.exception.getCommandError() != CommandException.Error.PASSWORD_INCORRECT) {
                                    UnlockSim.this.mResult = 2;
                                } else {
                                    UnlockSim.this.mResult = 1;
                                }
                                UnlockSim.this.mDone = true;
                                UnlockSim.this.notifyAll();
                            }
                        }
                    }
                };
                notifyAll();
            }
            Looper.loop();
        }

        synchronized int[] unlockSim(String str, String str2) {
            while (this.mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Message messageObtain = Message.obtain(this.mHandler, 100);
            if (str == null) {
                this.mSimCard.supplyPin(str2, messageObtain);
            } else {
                this.mSimCard.supplyPuk(str, str2, messageObtain);
            }
            while (!this.mDone) {
                try {
                    Log.d(PhoneInterfaceManager.LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(PhoneInterfaceManager.LOG_TAG, "done");
            return new int[]{this.mResult, this.mRetryCount};
        }
    }

    public void updateServiceLocation() {
        updateServiceLocationForSubscriber(getDefaultSubscription());
    }

    public void updateServiceLocationForSubscriber(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            phone.updateServiceLocation();
        }
    }

    public boolean isRadioOn(String str) {
        return isRadioOnForSubscriber(getDefaultSubscription(), str);
    }

    public boolean isRadioOnForSubscriber(int i, String str) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "isRadioOnForSubscriber")) {
            return false;
        }
        return isRadioOnForSubscriber(i);
    }

    private boolean isRadioOnForSubscriber(int i) {
        log("SubscriptionManager.getPhoneId(" + i + ")=" + SubscriptionManager.getPhoneId(i));
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.isRadioOn();
        }
        log("getPhone is null");
        return false;
    }

    public void toggleRadioOnOff() {
        toggleRadioOnOffForSubscriber(getDefaultSubscription());
    }

    public void toggleRadioOnOffForSubscriber(int i) {
        enforceModifyPermission();
        log("toggleRadioOnOffForSubscriber handled by RadioManager");
        RadioManager.getInstance().notifySimModeChange(!isRadioOnForSubscriber(i), this.mSubscriptionController.getPhoneId(i));
    }

    public boolean setRadio(boolean z) {
        return setRadioForSubscriber(getDefaultSubscription(), z);
    }

    public boolean setRadioForSubscriber(int i, boolean z) {
        enforceModifyPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            log("getPhone is null");
            return false;
        }
        if (!phone.isRadioAvailable()) {
            log("setRadioForSubscriber, radio unavailable.");
            return false;
        }
        log("setRadioForSubscriber, Radio=" + phone.isRadioOn() + "Request=" + z);
        if (phone.isRadioOn() != z) {
            toggleRadioOnOffForSubscriber(i);
            return true;
        }
        return true;
    }

    public boolean needMobileRadioShutdown() {
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            Phone phone = PhoneFactory.getPhone(i);
            RadioManager.getInstance();
            if (!RadioManager.isModemPowerOff(i) && phone != null && phone.isRadioAvailable()) {
                return true;
            }
        }
        logv(TelephonyManager.getDefault().getPhoneCount() + " Phones are shutdown.");
        return false;
    }

    public void shutdownMobileRadios() {
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            logv("Shutting down Phone " + i);
            shutdownRadioUsingPhoneId(i);
        }
    }

    private void shutdownRadioUsingPhoneId(int i) {
        enforceModifyPermission();
        Phone phone = PhoneFactory.getPhone(i);
        if (phone != null && phone.isRadioAvailable()) {
            phone.shutdownRadio();
        }
    }

    public boolean setRadioPower(boolean z) {
        enforceModifyPermission();
        Phone defaultPhone = PhoneFactory.getDefaultPhone();
        if (defaultPhone != null) {
            defaultPhone.setRadioPower(z);
            return true;
        }
        loge("There's no default phone.");
        return false;
    }

    public boolean setRadioPowerForSubscriber(int i, boolean z) {
        enforceModifyPermission();
        Phone phone = getPhone(i);
        if (phone != null) {
            phone.setRadioPower(z);
            return true;
        }
        return false;
    }

    public boolean enableDataConnectivity() {
        enforceModifyPermission();
        Phone phone = getPhone(this.mSubscriptionController.getDefaultDataSubId());
        if (phone != null) {
            phone.setUserDataEnabled(true);
            return true;
        }
        return false;
    }

    public boolean disableDataConnectivity() {
        enforceModifyPermission();
        Phone phone = getPhone(this.mSubscriptionController.getDefaultDataSubId());
        if (phone == null) {
            return false;
        }
        phone.setUserDataEnabled(false);
        return true;
    }

    public boolean isDataConnectivityPossible(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.isDataAllowed();
        }
        return false;
    }

    public boolean handlePinMmi(String str) {
        return handlePinMmiForSubscriber(getDefaultSubscription(), str);
    }

    public void handleUssdRequest(int i, String str, ResultReceiver resultReceiver) {
        enforceCallPermission();
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            return;
        }
        sendRequest(47, new Pair(str, resultReceiver), Integer.valueOf(i));
    }

    public boolean handlePinMmiForSubscriber(int i, String str) {
        enforceModifyPermission();
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            return false;
        }
        return ((Boolean) sendRequest(1, str, Integer.valueOf(i))).booleanValue();
    }

    public int getCallState() {
        return getCallStateForSlot(getSlotForDefaultSubscription());
    }

    public int getCallStateForSlot(int i) {
        Phone phone = PhoneFactory.getPhone(i);
        if (phone == null) {
            return 0;
        }
        return PhoneConstantConversions.convertCallState(phone.getState());
    }

    public int getDataState() {
        Phone phone = getPhone(this.mSubscriptionController.getDefaultDataSubId());
        if (phone != null) {
            return PhoneConstantConversions.convertDataState(phone.getDataConnectionState());
        }
        return PhoneConstantConversions.convertDataState(PhoneConstants.DataState.DISCONNECTED);
    }

    public int getDataActivity() {
        Phone phone = getPhone(this.mSubscriptionController.getDefaultDataSubId());
        if (phone != null) {
            return DefaultPhoneNotifier.convertDataActivityState(phone.getDataActivityState());
        }
        return 0;
    }

    public Bundle getCellLocation(String str) {
        ((AppOpsManager) this.mPhone.getContext().getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), str);
        if (!LocationAccessPolicy.canAccessCellLocation(this.mPhone.getContext(), str, Binder.getCallingUid(), Binder.getCallingPid(), true)) {
            return null;
        }
        Bundle bundle = new Bundle();
        Phone phone = getPhone(this.mSubscriptionController.getDefaultDataSubId());
        if (phone == null) {
            return null;
        }
        phone.getCellLocation(getWorkSource(Binder.getCallingUid())).fillInNotifierBundle(bundle);
        return bundle;
    }

    public String getNetworkCountryIsoForPhone(int i) {
        ServiceStateTracker serviceStateTracker;
        LocaleTracker localeTracker;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (18 == getVoiceNetworkTypeForSubscriber(this.mSubscriptionController.getSubIdUsingPhoneId(i), this.mApp.getPackageName())) {
                return "";
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null && (serviceStateTracker = phone.getServiceStateTracker()) != null && (localeTracker = serviceStateTracker.getLocaleTracker()) != null) {
                return localeTracker.getCurrentCountry();
            }
            return "";
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void enableLocationUpdates() {
        enableLocationUpdatesForSubscriber(getDefaultSubscription());
    }

    public void enableLocationUpdatesForSubscriber(int i) {
        this.mApp.enforceCallingOrSelfPermission("android.permission.CONTROL_LOCATION_UPDATES", null);
        Phone phone = getPhone(i);
        if (phone != null) {
            phone.enableLocationUpdates();
        }
    }

    public void disableLocationUpdates() {
        disableLocationUpdatesForSubscriber(getDefaultSubscription());
    }

    public void disableLocationUpdatesForSubscriber(int i) {
        this.mApp.enforceCallingOrSelfPermission("android.permission.CONTROL_LOCATION_UPDATES", null);
        Phone phone = getPhone(i);
        if (phone != null) {
            phone.disableLocationUpdates();
        }
    }

    public List<NeighboringCellInfo> getNeighboringCellInfo(String str) {
        ((AppOpsManager) this.mPhone.getContext().getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), str);
        if (!LocationAccessPolicy.canAccessCellLocation(this.mPhone.getContext(), str, Binder.getCallingUid(), Binder.getCallingPid(), true) || this.mAppOps.noteOp(12, Binder.getCallingUid(), str) != 0) {
            return null;
        }
        try {
            return (ArrayList) sendRequest(2, getWorkSource(Binder.getCallingUid()), -1);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "getNeighboringCellInfo " + e);
            return null;
        }
    }

    public List<CellInfo> getAllCellInfo(String str) {
        ((AppOpsManager) this.mPhone.getContext().getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), str);
        if (!LocationAccessPolicy.canAccessCellLocation(this.mPhone.getContext(), str, Binder.getCallingUid(), Binder.getCallingPid(), true)) {
            return null;
        }
        WorkSource workSource = getWorkSource(Binder.getCallingUid());
        ArrayList arrayList = new ArrayList();
        for (Phone phone : PhoneFactory.getPhones()) {
            List allCellInfo = phone.getAllCellInfo(workSource);
            if (allCellInfo != null) {
                arrayList.addAll(allCellInfo);
            }
        }
        return arrayList;
    }

    public void setCellInfoListRate(int i) {
        enforceModifyPermission();
        this.mPhone.setCellInfoListRate(i, getWorkSource(Binder.getCallingUid()));
    }

    public String getImeiForSlot(int i, String str) {
        Phone phone = PhoneFactory.getPhone(i);
        if (phone == null) {
            return null;
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, phone.getSubId(), str, "getImeiForSlot")) {
            return null;
        }
        return phone.getImei();
    }

    public String getMeidForSlot(int i, String str) {
        Phone phone = PhoneFactory.getPhone(i);
        if (phone == null) {
            return null;
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, phone.getSubId(), str, "getMeidForSlot")) {
            return null;
        }
        return phone.getMeid();
    }

    public String getDeviceSoftwareVersionForSlot(int i, String str) {
        Phone phone = PhoneFactory.getPhone(i);
        if (phone == null) {
            return null;
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, phone.getSubId(), str, "getDeviceSoftwareVersionForSlot")) {
            return null;
        }
        return phone.getDeviceSvn();
    }

    public int getSubscriptionCarrierId(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            return -1;
        }
        return phone.getCarrierId();
    }

    public String getSubscriptionCarrierName(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            return null;
        }
        return phone.getCarrierName();
    }

    private void enforceModifyPermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
    }

    private void enforceCallPermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.CALL_PHONE", null);
    }

    private void enforceConnectivityInternalPermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", "ConnectivityService");
    }

    private String createTelUrl(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return "tel:" + str;
    }

    private static void log(String str) {
        Log.d(LOG_TAG, "[PhoneIntfMgr] " + str);
    }

    private static void logv(String str) {
        Log.v(LOG_TAG, "[PhoneIntfMgr] " + str);
    }

    private static void loge(String str) {
        Log.e(LOG_TAG, "[PhoneIntfMgr] " + str);
    }

    public int getActivePhoneType() {
        return getActivePhoneTypeForSlot(getSlotForDefaultSubscription());
    }

    public int getActivePhoneTypeForSlot(int i) {
        Phone phone = PhoneFactory.getPhone(i);
        if (phone == null) {
            return 0;
        }
        return phone.getPhoneType();
    }

    public int getCdmaEriIconIndex(String str) {
        return getCdmaEriIconIndexForSubscriber(getDefaultSubscription(), str);
    }

    public int getCdmaEriIconIndexForSubscriber(int i, String str) {
        Phone phone;
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getCdmaEriIconIndexForSubscriber") && (phone = getPhone(i)) != null) {
            return phone.getCdmaEriIconIndex();
        }
        return -1;
    }

    public int getCdmaEriIconMode(String str) {
        return getCdmaEriIconModeForSubscriber(getDefaultSubscription(), str);
    }

    public int getCdmaEriIconModeForSubscriber(int i, String str) {
        Phone phone;
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getCdmaEriIconModeForSubscriber") && (phone = getPhone(i)) != null) {
            return phone.getCdmaEriIconMode();
        }
        return -1;
    }

    public String getCdmaEriText(String str) {
        return getCdmaEriTextForSubscriber(getDefaultSubscription(), str);
    }

    public String getCdmaEriTextForSubscriber(int i, String str) {
        Phone phone;
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getCdmaEriIconTextForSubscriber") && (phone = getPhone(i)) != null) {
            return phone.getCdmaEriText();
        }
        return null;
    }

    public String getCdmaMdn(int i) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "getCdmaMdn");
        Phone phone = getPhone(i);
        if (this.mPhone.getPhoneType() == 2 && phone != null) {
            return phone.getLine1Number();
        }
        return null;
    }

    public String getCdmaMin(int i) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "getCdmaMin");
        Phone phone = getPhone(i);
        if (phone != null && phone.getPhoneType() == 2) {
            return phone.getCdmaMin();
        }
        return null;
    }

    public boolean needsOtaServiceProvisioning() {
        return this.mPhone.needsOtaServiceProvisioning();
    }

    public boolean setVoiceMailNumber(int i, String str, String str2) {
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(i, "setVoiceMailNumber");
        return ((Boolean) sendRequest(33, new Pair(str, str2), new Integer(i))).booleanValue();
    }

    public Bundle getVisualVoicemailSettings(String str, int i) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        if (!TextUtils.equals(str, TelecomManager.from(this.mPhone.getContext()).getSystemDialerPackage())) {
            throw new SecurityException("caller must be system dialer");
        }
        PhoneAccountHandle phoneAccountHandleFromSubId = PhoneAccountHandleConverter.fromSubId(i);
        if (phoneAccountHandleFromSubId == null) {
            return null;
        }
        return VisualVoicemailSettingsUtil.dump(this.mPhone.getContext(), phoneAccountHandleFromSubId);
    }

    public String getVisualVoicemailPackageName(String str, int i) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getVisualVoicemailPackageName")) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return RemoteVvmTaskManager.getRemotePackage(this.mPhone.getContext(), i).getPackageName();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void enableVisualVoicemailSmsFilter(String str, int i, VisualVoicemailSmsFilterSettings visualVoicemailSmsFilterSettings) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        VisualVoicemailSmsFilterConfig.enableVisualVoicemailSmsFilter(this.mPhone.getContext(), str, i, visualVoicemailSmsFilterSettings);
    }

    public void disableVisualVoicemailSmsFilter(String str, int i) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        VisualVoicemailSmsFilterConfig.disableVisualVoicemailSmsFilter(this.mPhone.getContext(), str, i);
    }

    public VisualVoicemailSmsFilterSettings getVisualVoicemailSmsFilterSettings(String str, int i) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        return VisualVoicemailSmsFilterConfig.getVisualVoicemailSmsFilterSettings(this.mPhone.getContext(), str, i);
    }

    public VisualVoicemailSmsFilterSettings getActiveVisualVoicemailSmsFilterSettings(int i) {
        enforceReadPrivilegedPermission();
        return VisualVoicemailSmsFilterConfig.getActiveVisualVoicemailSmsFilterSettings(this.mPhone.getContext(), i);
    }

    public void sendVisualVoicemailSmsForSubscriber(String str, int i, String str2, int i2, String str3, PendingIntent pendingIntent) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        enforceVisualVoicemailPackage(str, i);
        enforceSendSmsPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            SmsManager smsManagerForSubscriptionId = SmsManager.getSmsManagerForSubscriptionId(i);
            if (i2 == 0) {
                smsManagerForSubscriptionId.sendTextMessageWithSelfPermissions(str2, null, str3, pendingIntent, null, false);
            } else {
                smsManagerForSubscriptionId.sendDataMessageWithSelfPermissions(str2, null, (short) i2, str3.getBytes(StandardCharsets.UTF_8), pendingIntent, null);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setVoiceActivationState(int i, int i2) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "setVoiceActivationState");
        Phone phone = getPhone(i);
        if (phone != null) {
            phone.setVoiceActivationState(i2);
            return;
        }
        loge("setVoiceActivationState fails with invalid subId: " + i);
    }

    public void setDataActivationState(int i, int i2) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "setDataActivationState");
        Phone phone = getPhone(i);
        if (phone != null) {
            phone.setDataActivationState(i2);
            return;
        }
        loge("setVoiceActivationState fails with invalid subId: " + i);
    }

    public int getVoiceActivationState(int i, String str) {
        enforceReadPrivilegedPermission();
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.getVoiceActivationState();
        }
        return 0;
    }

    public int getDataActivationState(int i, String str) {
        enforceReadPrivilegedPermission();
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.getDataActivationState();
        }
        return 0;
    }

    public int getVoiceMessageCount() {
        return getVoiceMessageCountForSubscriber(getDefaultSubscription());
    }

    public int getVoiceMessageCountForSubscriber(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.getVoiceMessageCount();
        }
        return 0;
    }

    public boolean isConcurrentVoiceAndDataAllowed(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            return false;
        }
        return phone.isConcurrentVoiceAndDataAllowed();
    }

    public void sendDialerSpecialCode(String str, String str2) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        if (!TextUtils.equals(str, TelecomManager.from(this.mPhone.getContext()).getDefaultDialerPackage())) {
            TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(getDefaultSubscription(), "sendDialerSpecialCode");
        }
        this.mPhone.sendDialerSpecialCode(str2);
    }

    public int getNetworkType() {
        Phone phone = getPhone(getDefaultSubscription());
        if (phone != null) {
            return phone.getServiceState().getDataNetworkType();
        }
        return 0;
    }

    public int getNetworkTypeForSubscriber(int i, String str) {
        Phone phone;
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getNetworkTypeForSubscriber") && (phone = getPhone(i)) != null) {
            return phone.getServiceState().getDataNetworkType();
        }
        return 0;
    }

    public int getDataNetworkType(String str) {
        return getDataNetworkTypeForSubscriber(getDefaultSubscription(), str);
    }

    public int getDataNetworkTypeForSubscriber(int i, String str) {
        Phone phone;
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getDataNetworkTypeForSubscriber") && (phone = getPhone(i)) != null) {
            return phone.getServiceState().getDataNetworkType();
        }
        return 0;
    }

    public int getVoiceNetworkTypeForSubscriber(int i, String str) {
        Phone phone;
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getDataNetworkTypeForSubscriber") && (phone = getPhone(i)) != null) {
            return phone.getServiceState().getVoiceNetworkType();
        }
        return 0;
    }

    public boolean hasIccCard() {
        return hasIccCardUsingSlotIndex(this.mSubscriptionController.getSlotIndex(getDefaultSubscription()));
    }

    public boolean hasIccCardUsingSlotIndex(int i) {
        Phone phone = PhoneFactory.getPhone(i);
        if (phone != null) {
            return phone.getIccCard().hasIccCard();
        }
        return false;
    }

    public int getLteOnCdmaMode(String str) {
        return getLteOnCdmaModeForSubscriber(getDefaultSubscription(), str);
    }

    public int getLteOnCdmaModeForSubscriber(int i, String str) {
        Phone phone;
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getLteOnCdmaModeForSubscriber") && (phone = getPhone(i)) != null) {
            return phone.getLteOnCdmaMode();
        }
        return -1;
    }

    public void setPhone(Phone phone) {
        this.mPhone = phone;
    }

    private int getDefaultSubscription() {
        return this.mSubscriptionController.getDefaultSubId();
    }

    private int getSlotForDefaultSubscription() {
        return this.mSubscriptionController.getPhoneId(getDefaultSubscription());
    }

    private int getPreferredVoiceSubscription() {
        return this.mSubscriptionController.getDefaultVoiceSubId();
    }

    public int getWhenToMakeWifiCalls() {
        return Settings.System.getInt(this.mPhone.getContext().getContentResolver(), "when_to_make_wifi_calls", getWhenToMakeWifiCallsDefaultPreference());
    }

    public void setWhenToMakeWifiCalls(int i) {
        Settings.System.putInt(this.mPhone.getContext().getContentResolver(), "when_to_make_wifi_calls", i);
    }

    private static int getWhenToMakeWifiCallsDefaultPreference() {
        return 0;
    }

    public IccOpenLogicalChannelResponse iccOpenLogicalChannel(int i, String str, String str2, int i2) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "iccOpenLogicalChannel");
        if (TextUtils.equals(ISDR_AID, str2)) {
            this.mAppOps.checkPackage(Binder.getCallingUid(), str);
            ComponentInfo componentInfoFindBestComponent = EuiccConnector.findBestComponent(this.mPhone.getContext().getPackageManager());
            if (componentInfoFindBestComponent == null || !TextUtils.equals(str, componentInfoFindBestComponent.packageName)) {
                loge("The calling package is not allowed to access ISD-R.");
                throw new SecurityException("The calling package is not allowed to access ISD-R.");
            }
        }
        return (IccOpenLogicalChannelResponse) sendRequest(9, new Pair(str2, Integer.valueOf(i2)), Integer.valueOf(i));
    }

    public boolean iccCloseLogicalChannel(int i, int i2) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "iccCloseLogicalChannel");
        if (i2 < 0) {
            return false;
        }
        return ((Boolean) sendRequest(11, Integer.valueOf(i2), Integer.valueOf(i))).booleanValue();
    }

    public String iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, int i7, String str) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "iccTransmitApduLogicalChannel");
        if (i2 < 0) {
            return "";
        }
        IccIoResult iccIoResult = (IccIoResult) sendRequest(7, new IccAPDUArgument(i2, i3, i4, i5, i6, i7, str), Integer.valueOf(i));
        String strSubstring = Integer.toHexString((iccIoResult.sw1 << 8) + iccIoResult.sw2 + 65536).substring(1);
        if (iccIoResult.payload != null) {
            return IccUtils.bytesToHexString(iccIoResult.payload) + strSubstring;
        }
        return strSubstring;
    }

    public String iccTransmitApduBasicChannel(int i, String str, int i2, int i3, int i4, int i5, int i6, String str2) {
        int i7;
        int i8;
        String str3;
        IccIoResult iccIoResult;
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "iccTransmitApduBasicChannel");
        if (i3 == SELECT_COMMAND) {
            i7 = i4;
            if (i7 == 4 && i5 == 0) {
                i8 = i6;
                if (i8 == 16) {
                    str3 = str2;
                    if (TextUtils.equals(ISDR_AID, str3)) {
                        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
                        ComponentInfo componentInfoFindBestComponent = EuiccConnector.findBestComponent(this.mPhone.getContext().getPackageManager());
                        if (componentInfoFindBestComponent == null || !TextUtils.equals(str, componentInfoFindBestComponent.packageName)) {
                            loge("The calling package is not allowed to select ISD-R.");
                            throw new SecurityException("The calling package is not allowed to select ISD-R.");
                        }
                    }
                }
                iccIoResult = (IccIoResult) sendRequest(29, new IccAPDUArgument(0, i2, i3, i7, i5, i8, str3), Integer.valueOf(i));
                String strSubstring = Integer.toHexString((iccIoResult.sw1 << 8) + iccIoResult.sw2 + 65536).substring(1);
                if (iccIoResult.payload != null) {
                    return IccUtils.bytesToHexString(iccIoResult.payload) + strSubstring;
                }
                return strSubstring;
            }
            str3 = str2;
            iccIoResult = (IccIoResult) sendRequest(29, new IccAPDUArgument(0, i2, i3, i7, i5, i8, str3), Integer.valueOf(i));
            String strSubstring2 = Integer.toHexString((iccIoResult.sw1 << 8) + iccIoResult.sw2 + 65536).substring(1);
            if (iccIoResult.payload != null) {
            }
        } else {
            i7 = i4;
        }
        i8 = i6;
        str3 = str2;
        iccIoResult = (IccIoResult) sendRequest(29, new IccAPDUArgument(0, i2, i3, i7, i5, i8, str3), Integer.valueOf(i));
        String strSubstring22 = Integer.toHexString((iccIoResult.sw1 << 8) + iccIoResult.sw2 + 65536).substring(1);
        if (iccIoResult.payload != null) {
        }
    }

    public byte[] iccExchangeSimIO(int i, int i2, int i3, int i4, int i5, int i6, String str) {
        byte[] bArr;
        int length;
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "iccExchangeSimIO");
        IccIoResult iccIoResult = (IccIoResult) sendRequest(31, new IccAPDUArgument(-1, i2, i3, i4, i5, i6, str), Integer.valueOf(i));
        if (iccIoResult.payload != null) {
            length = iccIoResult.payload.length + 2;
            bArr = new byte[length];
            System.arraycopy(iccIoResult.payload, 0, bArr, 0, iccIoResult.payload.length);
        } else {
            bArr = new byte[2];
            length = 2;
        }
        bArr[length - 1] = (byte) iccIoResult.sw2;
        bArr[length - 2] = (byte) iccIoResult.sw1;
        return bArr;
    }

    public String[] getForbiddenPlmns(int i, int i2, String str) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getForbiddenPlmns")) {
            return null;
        }
        if (i2 != 2 && i2 != 1) {
            loge("getForbiddenPlmnList(): App Type must be USIM or SIM");
            return null;
        }
        Object objSendRequest = sendRequest(48, new Integer(i2), Integer.valueOf(i));
        if (objSendRequest instanceof String[]) {
            return (String[]) objSendRequest;
        }
        return null;
    }

    public String sendEnvelopeWithStatus(int i, String str) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "sendEnvelopeWithStatus");
        IccIoResult iccIoResult = (IccIoResult) sendRequest(25, str, Integer.valueOf(i));
        if (iccIoResult.payload == null) {
            return "";
        }
        return IccUtils.bytesToHexString(iccIoResult.payload) + Integer.toHexString((iccIoResult.sw1 << 8) + iccIoResult.sw2 + 65536).substring(1);
    }

    public String nvReadItem(int i) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, getDefaultSubscription(), "nvReadItem");
        return (String) sendRequest(13, Integer.valueOf(i));
    }

    public boolean nvWriteItem(int i, String str) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, getDefaultSubscription(), "nvWriteItem");
        return ((Boolean) sendRequest(15, new Pair(Integer.valueOf(i), str))).booleanValue();
    }

    public boolean nvWriteCdmaPrl(byte[] bArr) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, getDefaultSubscription(), "nvWriteCdmaPrl");
        return ((Boolean) sendRequest(17, bArr)).booleanValue();
    }

    public boolean nvResetConfig(int i) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, getDefaultSubscription(), "nvResetConfig");
        return ((Boolean) sendRequest(19, Integer.valueOf(i))).booleanValue();
    }

    public int getDefaultSim() {
        return 0;
    }

    public String[] getPcscfAddress(String str, String str2) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, this.mPhone.getSubId(), str2, "getPcscfAddress")) {
            return new String[0];
        }
        return this.mPhone.getPcscfAddress(str);
    }

    public void enableIms(int i) {
        enforceModifyPermission();
        PhoneFactory.getImsResolver().enableIms(i);
    }

    public void disableIms(int i) {
        enforceModifyPermission();
        PhoneFactory.getImsResolver().disableIms(i);
    }

    public IImsMmTelFeature getMmTelFeatureAndListen(int i, IImsServiceFeatureCallback iImsServiceFeatureCallback) {
        enforceModifyPermission();
        return PhoneFactory.getImsResolver().getMmTelFeatureAndListen(i, iImsServiceFeatureCallback);
    }

    public IImsRcsFeature getRcsFeatureAndListen(int i, IImsServiceFeatureCallback iImsServiceFeatureCallback) {
        enforceModifyPermission();
        return PhoneFactory.getImsResolver().getRcsFeatureAndListen(i, iImsServiceFeatureCallback);
    }

    public IImsRegistration getImsRegistration(int i, int i2) throws RemoteException {
        enforceModifyPermission();
        return PhoneFactory.getImsResolver().getImsRegistration(i, i2);
    }

    public IImsConfig getImsConfig(int i, int i2) throws RemoteException {
        enforceModifyPermission();
        return PhoneFactory.getImsResolver().getImsConfig(i, i2);
    }

    public boolean isResolvingImsBinding() {
        enforceModifyPermission();
        return PhoneFactory.getImsResolver().isResolvingBinding();
    }

    public boolean setImsService(int i, boolean z, String str) {
        int[] subId = SubscriptionManager.getSubId(i);
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, subId != null ? subId[0] : -1, "setImsService");
        return PhoneFactory.getImsResolver().overrideImsServiceConfiguration(i, z, str);
    }

    public String getImsService(int i, boolean z) {
        int[] subId = SubscriptionManager.getSubId(i);
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, subId != null ? subId[0] : -1, "getImsService");
        return PhoneFactory.getImsResolver().getImsServiceConfiguration(i, z);
    }

    public void setImsRegistrationState(boolean z) {
        enforceModifyPermission();
        this.mPhone.setImsRegistrationState(z);
    }

    public void setNetworkSelectionModeAutomatic(int i) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "setNetworkSelectionModeAutomatic");
        sendRequest(35, null, Integer.valueOf(i));
    }

    public boolean setNetworkSelectionModeManual(int i, String str, boolean z) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "setNetworkSelectionModeManual");
        return ((Boolean) sendRequest(41, new ManualNetworkSelectionArgument(new OperatorInfo("", "", str), z), Integer.valueOf(i))).booleanValue();
    }

    public CellNetworkScanResult getCellNetworkScanResults(int i) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "getCellNetworkScanResults");
        return (CellNetworkScanResult) sendRequest(39, null, Integer.valueOf(i));
    }

    public int requestNetworkScan(int i, NetworkScanRequest networkScanRequest, Messenger messenger, IBinder iBinder) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "requestNetworkScan");
        return this.mNetworkScanRequestTracker.startNetworkScan(networkScanRequest, messenger, iBinder, getPhone(i));
    }

    public void stopNetworkScan(int i, int i2) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "stopNetworkScan");
        this.mNetworkScanRequestTracker.stopNetworkScan(i2);
    }

    public int getCalculatedPreferredNetworkType(String str) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, this.mPhone.getSubId(), str, "getCalculatedPreferredNetworkType")) {
            return RILConstants.PREFERRED_NETWORK_MODE;
        }
        return PhoneFactory.calculatePreferredNetworkType(this.mPhone.getContext(), 0);
    }

    public int getPreferredNetworkType(int i) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "getPreferredNetworkType");
        int[] iArr = (int[]) sendRequest(21, null, Integer.valueOf(i));
        if (iArr != null) {
            return iArr[0];
        }
        return -1;
    }

    public boolean setPreferredNetworkType(int i, int i2) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "setPreferredNetworkType");
        Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + i, i2);
        return ((Boolean) sendRequest(23, Integer.valueOf(i2), Integer.valueOf(i))).booleanValue();
    }

    public int getTetherApnRequired() {
        enforceModifyPermission();
        int i = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "tether_dun_required", 2);
        int defaultDataSubId = this.mSubscriptionController.getDefaultDataSubId();
        int phoneId = this.mSubscriptionController.getPhoneId(defaultDataSubId);
        log("getTetherApnRequired: subId=" + defaultDataSubId + " phoneId=" + phoneId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            if (phone.hasMatchedTetherApnSetting()) {
                return 1;
            }
            return 0;
        }
        return i;
    }

    public void setUserDataEnabled(int i, boolean z) {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "setUserDataEnabled");
        Phone phone = PhoneFactory.getPhone(this.mSubscriptionController.getPhoneId(i));
        if (phone != null) {
            phone.setUserDataEnabled(z);
            return;
        }
        loge("setUserDataEnabled: no phone for subId=" + i);
    }

    public boolean getDataEnabled(int i) {
        return isUserDataEnabled(i);
    }

    public boolean isUserDataEnabled(int i) {
        try {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", null);
        } catch (Exception e) {
            TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "isUserDataEnabled");
        }
        Phone phone = PhoneFactory.getPhone(this.mSubscriptionController.getPhoneId(i));
        if (phone != null) {
            return phone.isUserDataEnabled();
        }
        return false;
    }

    public boolean isDataEnabled(int i) {
        try {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", null);
        } catch (Exception e) {
            TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, i, "isDataEnabled");
        }
        Phone phone = PhoneFactory.getPhone(this.mSubscriptionController.getPhoneId(i));
        if (phone != null) {
            return phone.isDataEnabled();
        }
        return false;
    }

    public int getCarrierPrivilegeStatus(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            loge("getCarrierPrivilegeStatus: Invalid subId");
            return 0;
        }
        UiccCard uiccCard = UiccController.getInstance().getUiccCard(phone.getPhoneId());
        if (uiccCard == null) {
            loge("getCarrierPrivilegeStatus: No UICC");
            return -1;
        }
        return uiccCard.getCarrierPrivilegeStatusForCurrentTransaction(phone.getContext().getPackageManager());
    }

    public int getCarrierPrivilegeStatusForUid(int i, int i2) {
        Phone phone = getPhone(i);
        if (phone == null) {
            loge("getCarrierPrivilegeStatus: Invalid subId");
            return 0;
        }
        UiccProfile uiccProfileForPhone = UiccController.getInstance().getUiccProfileForPhone(phone.getPhoneId());
        if (uiccProfileForPhone == null) {
            loge("getCarrierPrivilegeStatus: No UICC");
            return -1;
        }
        return uiccProfileForPhone.getCarrierPrivilegeStatusForUid(phone.getContext().getPackageManager(), i2);
    }

    public int checkCarrierPrivilegesForPackage(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        UiccCard uiccCard = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
        if (uiccCard == null) {
            loge("checkCarrierPrivilegesForPackage: No UICC");
            return -1;
        }
        return uiccCard.getCarrierPrivilegeStatus(this.mPhone.getContext().getPackageManager(), str);
    }

    public int checkCarrierPrivilegesForPackageAnyPhone(String str) {
        UiccCard uiccCard;
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        int carrierPrivilegeStatus = -1;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount() && ((uiccCard = UiccController.getInstance().getUiccCard(i)) == null || (carrierPrivilegeStatus = uiccCard.getCarrierPrivilegeStatus(this.mPhone.getContext().getPackageManager(), str)) != 1); i++) {
        }
        return carrierPrivilegeStatus;
    }

    public List<String> getCarrierPackageNamesForIntentAndPhone(Intent intent, int i) {
        if (!SubscriptionManager.isValidPhoneId(i)) {
            loge("phoneId " + i + " is not valid.");
            return null;
        }
        UiccCard uiccCard = UiccController.getInstance().getUiccCard(i);
        if (uiccCard == null) {
            loge("getCarrierPackageNamesForIntent: No UICC");
            return null;
        }
        return uiccCard.getCarrierPackageNamesForIntent(this.mPhone.getContext().getPackageManager(), intent);
    }

    public List<String> getPackagesWithCarrierPrivileges() {
        PackageManager packageManager = this.mPhone.getContext().getPackageManager();
        ArrayList arrayList = new ArrayList();
        List installedPackagesAsUser = null;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            UiccCard uiccCard = UiccController.getInstance().getUiccCard(i);
            if (uiccCard != null && uiccCard.hasCarrierPrivilegeRules()) {
                if (installedPackagesAsUser == null) {
                    installedPackagesAsUser = packageManager.getInstalledPackagesAsUser(33344, 0);
                }
                for (int size = installedPackagesAsUser.size() - 1; size >= 0; size--) {
                    PackageInfo packageInfo = (PackageInfo) installedPackagesAsUser.get(size);
                    if (packageInfo != null && packageInfo.packageName != null && uiccCard.getCarrierPrivilegeStatus(packageInfo) == 1) {
                        arrayList.add(packageInfo.packageName);
                    }
                }
            }
        }
        return arrayList;
    }

    private String getIccId(int i) {
        UiccCard uiccCard;
        Phone phone = getPhone(i);
        if (phone != null) {
            uiccCard = phone.getUiccCard();
        } else {
            uiccCard = null;
        }
        if (uiccCard == null) {
            loge("getIccId: No UICC");
            return null;
        }
        String iccId = uiccCard.getIccId();
        if (TextUtils.isEmpty(iccId)) {
            loge("getIccId: ICC ID is null or empty.");
            return null;
        }
        return iccId;
    }

    public boolean setLine1NumberForDisplayForSubscriber(int i, String str, String str2) {
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(i, "setLine1NumberForDisplayForSubscriber");
        String iccId = getIccId(i);
        Phone phone = getPhone(i);
        if (phone == null) {
            return false;
        }
        String subscriberId = phone.getSubscriberId();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }
        SharedPreferences.Editor editorEdit = this.mTelephonySharedPreferences.edit();
        String str3 = PREF_CARRIERS_ALPHATAG_PREFIX + iccId;
        if (str == null) {
            editorEdit.remove(str3);
        } else {
            editorEdit.putString(str3, str);
        }
        String str4 = PREF_CARRIERS_NUMBER_PREFIX + iccId;
        String str5 = PREF_CARRIERS_SUBSCRIBER_PREFIX + iccId;
        if (str2 == null) {
            editorEdit.remove(str4);
            editorEdit.remove(str5);
        } else {
            editorEdit.putString(str4, str2);
            editorEdit.putString(str5, subscriberId);
        }
        editorEdit.commit();
        return true;
    }

    public String getLine1NumberForDisplay(int i, String str) {
        String iccId;
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneNumber(this.mApp, i, str, "getLine1NumberForDisplay") || (iccId = getIccId(i)) == null) {
            return null;
        }
        return this.mTelephonySharedPreferences.getString(PREF_CARRIERS_NUMBER_PREFIX + iccId, null);
    }

    public String getLine1AlphaTagForDisplay(int i, String str) {
        String iccId;
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getLine1AlphaTagForDisplay") || (iccId = getIccId(i)) == null) {
            return null;
        }
        return this.mTelephonySharedPreferences.getString(PREF_CARRIERS_ALPHATAG_PREFIX + iccId, null);
    }

    public String[] getMergedSubscriberIds(String str) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, -1, str, "getMergedSubscriberIds")) {
            return null;
        }
        Context context = this.mPhone.getContext();
        TelephonyManager telephonyManagerFrom = TelephonyManager.from(context);
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(context);
        ArraySet arraySet = new ArraySet();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            for (int i : subscriptionManagerFrom.getActiveSubscriptionIdList()) {
                arraySet.add(telephonyManagerFrom.getSubscriberId(i));
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Map<String, ?> all = this.mTelephonySharedPreferences.getAll();
            String str2 = null;
            for (String str3 : all.keySet()) {
                if (str3.startsWith(PREF_CARRIERS_SUBSCRIBER_PREFIX) && arraySet.contains((String) all.get(str3))) {
                    str2 = (String) all.get(PREF_CARRIERS_NUMBER_PREFIX + str3.substring(PREF_CARRIERS_SUBSCRIBER_PREFIX.length()));
                    if (!TextUtils.isEmpty(str2)) {
                        break;
                    }
                }
            }
            if (TextUtils.isEmpty(str2)) {
                return null;
            }
            ArraySet arraySet2 = new ArraySet();
            for (String str4 : all.keySet()) {
                if (str4.startsWith(PREF_CARRIERS_NUMBER_PREFIX) && str2.equals((String) all.get(str4))) {
                    String str5 = (String) all.get(PREF_CARRIERS_SUBSCRIBER_PREFIX + str4.substring(PREF_CARRIERS_NUMBER_PREFIX.length()));
                    if (!TextUtils.isEmpty(str5)) {
                        arraySet2.add(str5);
                    }
                }
            }
            String[] strArr = (String[]) arraySet2.toArray(new String[arraySet2.size()]);
            Arrays.sort(strArr);
            return strArr;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public boolean setOperatorBrandOverride(int i, String str) {
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(i, "setOperatorBrandOverride");
        Phone phone = getPhone(i);
        if (phone == null) {
            return false;
        }
        return phone.setOperatorBrandOverride(str);
    }

    public boolean setRoamingOverride(int i, List<String> list, List<String> list2, List<String> list3, List<String> list4) {
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(i, "setRoamingOverride");
        Phone phone = getPhone(i);
        if (phone == null) {
            return false;
        }
        return phone.setRoamingOverride(list, list2, list3, list4);
    }

    @Deprecated
    public int invokeOemRilRequestRaw(byte[] bArr, byte[] bArr2) {
        enforceModifyPermission();
        try {
            AsyncResult asyncResult = (AsyncResult) sendRequest(27, bArr);
            if (asyncResult.exception == null) {
                if (asyncResult.result == null) {
                    return 0;
                }
                byte[] bArr3 = (byte[]) asyncResult.result;
                if (bArr3.length > bArr2.length) {
                    Log.w(LOG_TAG, "Buffer to copy response too small: Response length is " + bArr3.length + "bytes. Buffer Size is " + bArr2.length + "bytes.");
                }
                System.arraycopy(bArr3, 0, bArr2, 0, bArr3.length);
                return bArr3.length;
            }
            int iOrdinal = asyncResult.exception.getCommandError().ordinal();
            return iOrdinal > 0 ? iOrdinal * (-1) : iOrdinal;
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "sendOemRilRequestRaw: Runtime Exception");
            int iOrdinal2 = CommandException.Error.GENERIC_FAILURE.ordinal();
            return iOrdinal2 > 0 ? iOrdinal2 * (-1) : iOrdinal2;
        }
    }

    public void setRadioCapability(RadioAccessFamily[] radioAccessFamilyArr) {
        try {
            ProxyController.getInstance().setRadioCapability(radioAccessFamilyArr);
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "setRadioCapability: Runtime Exception");
        }
    }

    public int getRadioAccessFamily(int i, String str) {
        Phone phone = PhoneFactory.getPhone(i);
        if (phone == null) {
            return 1;
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, phone.getSubId(), str, "getRadioAccessFamily")) {
            return 1;
        }
        return ProxyController.getInstance().getRadioAccessFamily(i);
    }

    public void enableVideoCalling(boolean z) {
        enforceModifyPermission();
        ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId()).setVtSetting(z);
    }

    public boolean isVideoCallingEnabled(String str) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, this.mPhone.getSubId(), str, "isVideoCallingEnabled")) {
            return false;
        }
        ImsManager imsManager = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
        return imsManager.isVtEnabledByPlatform() && imsManager.isEnhanced4gLteModeSettingEnabledByUser() && imsManager.isVtEnabledByUser();
    }

    public boolean canChangeDtmfToneLength() {
        return this.mApp.getCarrierConfig().getBoolean("dtmf_type_enabled_bool");
    }

    public boolean isWorldPhone() {
        return this.mApp.getCarrierConfig().getBoolean("world_phone_bool");
    }

    public boolean isTtyModeSupported() {
        TelecomManager telecomManagerFrom = TelecomManager.from(this.mPhone.getContext());
        return telecomManagerFrom.isTtySupported();
    }

    public boolean isHearingAidCompatibilitySupported() {
        return this.mPhone.getContext().getResources().getBoolean(R.bool.hac_enabled);
    }

    public boolean isRttSupported() {
        return this.mApp.getCarrierConfig().getBoolean("rtt_supported_bool") && this.mPhone.getContext().getResources().getBoolean(R.bool.config_support_rtt);
    }

    public boolean isRttEnabled() {
        return isRttSupported() && Settings.Secure.getInt(this.mPhone.getContext().getContentResolver(), "rtt_calling_mode", 0) != 0;
    }

    public String getDeviceId(String str) {
        Phone phone = PhoneFactory.getPhone(0);
        if (phone == null) {
            return null;
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, phone.getSubId(), str, "getDeviceId")) {
            return null;
        }
        return phone.getDeviceId();
    }

    public boolean isImsRegistered(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.isImsRegistered();
        }
        return false;
    }

    public int getSubIdForPhoneAccount(PhoneAccount phoneAccount) {
        return PhoneUtils.getSubIdForPhoneAccount(phoneAccount);
    }

    public boolean isWifiCallingAvailable(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.isWifiCallingEnabled();
        }
        return false;
    }

    public boolean isVolteAvailable(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.isVolteEnabled();
        }
        return false;
    }

    public boolean isVideoTelephonyAvailable(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.isVideoEnabled();
        }
        return false;
    }

    public int getImsRegTechnologyForMmTel(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.getImsRegistrationTech();
        }
        return -1;
    }

    public void factoryReset(int i) {
        enforceConnectivityInternalPermission();
        if (this.mUserManager.hasUserRestriction("no_network_reset")) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (SubscriptionManager.isUsableSubIdValue(i) && !this.mUserManager.hasUserRestriction("no_config_mobile_networks")) {
                setUserDataEnabled(i, getDefaultDataEnabled());
                MtkGsmCdmaPhone phone = getPhone(i);
                if (phone instanceof MtkGsmCdmaPhone) {
                    phone.setMtkNetworkSelection(1, (OperatorInfo) null, (Message) null);
                } else {
                    setNetworkSelectionModeAutomatic(i);
                }
                setPreferredNetworkType(i, getDefaultNetworkType(i));
                if (phone != null) {
                    phone.setDataRoamingEnabled(getDefaultDataRoamingEnabled(i));
                }
                CarrierInfoManager.deleteAllCarrierKeysForImsiEncryption(this.mPhone.getContext());
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public String getLocaleFromDefaultSim() {
        SubscriptionInfo subscriptionInfo;
        String language;
        Locale localeFromSimAndCarrierPrefs;
        List<SubscriptionInfo> allSubscriptionInfoList = getAllSubscriptionInfoList();
        if (allSubscriptionInfoList == null || allSubscriptionInfoList.isEmpty()) {
            return null;
        }
        int defaultSubscription = getDefaultSubscription();
        if (defaultSubscription == -1) {
            subscriptionInfo = allSubscriptionInfoList.get(0);
        } else {
            Iterator<SubscriptionInfo> it = allSubscriptionInfoList.iterator();
            while (true) {
                if (it.hasNext()) {
                    SubscriptionInfo next = it.next();
                    if (next.getSubscriptionId() == defaultSubscription) {
                        subscriptionInfo = next;
                        break;
                    }
                } else {
                    subscriptionInfo = null;
                    break;
                }
            }
            if (subscriptionInfo == null) {
                return null;
            }
        }
        int mcc = subscriptionInfo.getMcc();
        Phone phone = getPhone(subscriptionInfo.getSubscriptionId());
        if (phone != null && (localeFromSimAndCarrierPrefs = phone.getLocaleFromSimAndCarrierPrefs()) != null) {
            if (!localeFromSimAndCarrierPrefs.getCountry().isEmpty()) {
                return localeFromSimAndCarrierPrefs.toLanguageTag();
            }
            language = localeFromSimAndCarrierPrefs.getLanguage();
        } else {
            language = null;
        }
        Locale localeFromMcc = MccTable.getLocaleFromMcc(this.mPhone.getContext(), mcc, language);
        if (localeFromMcc == null) {
            return null;
        }
        return localeFromMcc.toLanguageTag();
    }

    private List<SubscriptionInfo> getAllSubscriptionInfoList() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mSubscriptionController.getAllSubInfoList(this.mPhone.getContext().getOpPackageName());
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private List<SubscriptionInfo> getActiveSubscriptionInfoList() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mSubscriptionController.getActiveSubscriptionInfoList(this.mPhone.getContext().getOpPackageName());
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void requestModemActivityInfo(ResultReceiver resultReceiver) {
        ModemActivityInfo modemActivityInfo;
        enforceModifyPermission();
        synchronized (this.mLastModemActivityInfo) {
            ModemActivityInfo modemActivityInfo2 = (ModemActivityInfo) sendRequest(37, null);
            if (modemActivityInfo2 != null) {
                int[] iArr = new int[5];
                for (int i = 0; i < iArr.length; i++) {
                    iArr[i] = modemActivityInfo2.getTxTimeMillis()[i] + this.mLastModemActivityInfo.getTxTimeMillis()[i];
                }
                this.mLastModemActivityInfo.setTimestamp(modemActivityInfo2.getTimestamp());
                this.mLastModemActivityInfo.setSleepTimeMillis(modemActivityInfo2.getSleepTimeMillis() + this.mLastModemActivityInfo.getSleepTimeMillis());
                this.mLastModemActivityInfo.setIdleTimeMillis(modemActivityInfo2.getIdleTimeMillis() + this.mLastModemActivityInfo.getIdleTimeMillis());
                this.mLastModemActivityInfo.setTxTimeMillis(iArr);
                this.mLastModemActivityInfo.setRxTimeMillis(modemActivityInfo2.getRxTimeMillis() + this.mLastModemActivityInfo.getRxTimeMillis());
                this.mLastModemActivityInfo.setEnergyUsed(modemActivityInfo2.getEnergyUsed() + this.mLastModemActivityInfo.getEnergyUsed());
            }
            modemActivityInfo = new ModemActivityInfo(this.mLastModemActivityInfo.getTimestamp(), this.mLastModemActivityInfo.getSleepTimeMillis(), this.mLastModemActivityInfo.getIdleTimeMillis(), this.mLastModemActivityInfo.getTxTimeMillis(), this.mLastModemActivityInfo.getRxTimeMillis(), this.mLastModemActivityInfo.getEnergyUsed());
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable("controller_activity", modemActivityInfo);
        resultReceiver.send(0, bundle);
    }

    public ServiceState getServiceStateForSubscriber(int i, String str) {
        Phone phone;
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getServiceStateForSubscriber") && (phone = getPhone(i)) != null) {
            return phone.getServiceState();
        }
        return null;
    }

    public Uri getVoicemailRingtoneUri(PhoneAccountHandle phoneAccountHandle) {
        Phone phoneForPhoneAccountHandle = PhoneUtils.getPhoneForPhoneAccountHandle(phoneAccountHandle);
        if (phoneForPhoneAccountHandle == null) {
            phoneForPhoneAccountHandle = this.mPhone;
        }
        return VoicemailNotificationSettingsUtil.getRingtoneUri(phoneForPhoneAccountHandle.getContext());
    }

    public void setVoicemailRingtoneUri(String str, PhoneAccountHandle phoneAccountHandle, Uri uri) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        if (!TextUtils.equals(str, TelecomManager.from(this.mPhone.getContext()).getDefaultDialerPackage())) {
            TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccountHandle), "setVoicemailRingtoneUri");
        }
        Phone phoneForPhoneAccountHandle = PhoneUtils.getPhoneForPhoneAccountHandle(phoneAccountHandle);
        if (phoneForPhoneAccountHandle == null) {
            phoneForPhoneAccountHandle = this.mPhone;
        }
        VoicemailNotificationSettingsUtil.setRingtoneUri(phoneForPhoneAccountHandle.getContext(), uri);
    }

    public boolean isVoicemailVibrationEnabled(PhoneAccountHandle phoneAccountHandle) {
        Phone phoneForPhoneAccountHandle = PhoneUtils.getPhoneForPhoneAccountHandle(phoneAccountHandle);
        if (phoneForPhoneAccountHandle == null) {
            phoneForPhoneAccountHandle = this.mPhone;
        }
        return VoicemailNotificationSettingsUtil.isVibrationEnabled(phoneForPhoneAccountHandle.getContext());
    }

    public void setVoicemailVibrationEnabled(String str, PhoneAccountHandle phoneAccountHandle, boolean z) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        if (!TextUtils.equals(str, TelecomManager.from(this.mPhone.getContext()).getDefaultDialerPackage())) {
            TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccountHandle), "setVoicemailVibrationEnabled");
        }
        Phone phoneForPhoneAccountHandle = PhoneUtils.getPhoneForPhoneAccountHandle(phoneAccountHandle);
        if (phoneForPhoneAccountHandle == null) {
            phoneForPhoneAccountHandle = this.mPhone;
        }
        VoicemailNotificationSettingsUtil.setVibrationEnabled(phoneForPhoneAccountHandle.getContext(), z);
    }

    private void enforceReadPrivilegedPermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", null);
    }

    private void enforceSendSmsPermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.SEND_SMS", null);
    }

    private void enforceVisualVoicemailPackage(String str, int i) {
        ComponentName remotePackage = RemoteVvmTaskManager.getRemotePackage(this.mPhone.getContext(), i);
        if (remotePackage == null) {
            throw new SecurityException("Caller not current active visual voicemail package[null]");
        }
        String packageName = remotePackage.getPackageName();
        if (!str.equals(packageName)) {
            throw new SecurityException("Caller not current active visual voicemail package[" + packageName + "]");
        }
    }

    public String getAidForAppType(int i, int i2) {
        enforceReadPrivilegedPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            return null;
        }
        try {
            return UiccController.getInstance().getUiccCard(phone.getPhoneId()).getApplicationByType(i2).getAid();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Not getting aid. Exception ex=" + e);
            return null;
        }
    }

    public String getEsn(int i) {
        enforceReadPrivilegedPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            return null;
        }
        try {
            return phone.getEsn();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Not getting ESN. Exception ex=" + e);
            return null;
        }
    }

    public String getCdmaPrlVersion(int i) {
        enforceReadPrivilegedPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            return null;
        }
        try {
            return phone.getCdmaPrlVersion();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Not getting PRLVersion", e);
            return null;
        }
    }

    public List<TelephonyHistogram> getTelephonyHistograms() {
        TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(this.mApp, getDefaultSubscription(), "getTelephonyHistograms");
        return RIL.getTelephonyRILTimingHistograms();
    }

    public int setAllowedCarriers(int i, List<CarrierIdentifier> list) {
        enforceModifyPermission();
        if (list == null) {
            throw new NullPointerException("carriers cannot be null");
        }
        return ((int[]) sendRequest(43, list, Integer.valueOf(SubscriptionManager.getSubId(i)[0])))[0];
    }

    public List<CarrierIdentifier> getAllowedCarriers(int i) {
        enforceReadPrivilegedPermission();
        return (List) sendRequest(45, null, Integer.valueOf(SubscriptionManager.getSubId(i)[0]));
    }

    public void carrierActionSetMeteredApnsEnabled(int i, boolean z) {
        enforceModifyPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            loge("carrierAction: SetMeteredApnsEnabled fails with invalid subId: " + i);
            return;
        }
        try {
            phone.carrierActionSetMeteredApnsEnabled(z);
        } catch (Exception e) {
            Log.e(LOG_TAG, "carrierAction: SetMeteredApnsEnabled fails. Exception ex=" + e);
        }
    }

    public void carrierActionSetRadioEnabled(int i, boolean z) {
        enforceModifyPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            loge("carrierAction: SetRadioEnabled fails with invalid sibId: " + i);
            return;
        }
        try {
            phone.carrierActionSetRadioEnabled(z);
        } catch (Exception e) {
            Log.e(LOG_TAG, "carrierAction: SetRadioEnabled fails. Exception ex=" + e);
        }
    }

    public void carrierActionReportDefaultNetworkStatus(int i, boolean z) {
        enforceModifyPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            loge("carrierAction: ReportDefaultNetworkStatus fails with invalid sibId: " + i);
            return;
        }
        try {
            phone.carrierActionReportDefaultNetworkStatus(z);
        } catch (Exception e) {
            Log.e(LOG_TAG, "carrierAction: ReportDefaultNetworkStatus fails. Exception ex=" + e);
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            printWriter.println("Permission Denial: can't dump Phone from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + "without permission android.permission.DUMP");
            return;
        }
        DumpsysHandler.dump(this.mPhone.getContext(), fileDescriptor, printWriter, strArr);
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) throws RemoteException {
        new TelephonyShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    public NetworkStats getVtDataUsage(int i, boolean z) {
        this.mApp.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", null);
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.getVtDataUsage(z);
        }
        return null;
    }

    public void setPolicyDataEnabled(boolean z, int i) {
        enforceModifyPermission();
        Phone phone = getPhone(i);
        if (phone != null) {
            phone.setPolicyDataEnabled(z);
        }
    }

    public List<ClientRequestStats> getClientRequestStats(String str, int i) {
        Phone phone;
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mApp, i, str, "getClientRequestStats") && (phone = getPhone(i)) != null) {
            return phone.getClientRequestStats();
        }
        return null;
    }

    private WorkSource getWorkSource(int i) {
        return new WorkSource(i, this.mPhone.getContext().getPackageManager().getNameForUid(i));
    }

    public void setSimPowerStateForSlot(int i, int i2) {
        enforceModifyPermission();
        Phone phone = PhoneFactory.getPhone(i);
        if (phone != null) {
            phone.setSimPowerState(i2);
        }
    }

    private boolean isUssdApiAllowed(int i) {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager == null || (configForSubId = carrierConfigManager.getConfigForSubId(i)) == null) {
            return false;
        }
        return configForSubId.getBoolean("allow_ussd_requests_via_telephony_manager_bool");
    }

    public boolean getEmergencyCallbackMode(int i) {
        enforceReadPrivilegedPermission();
        Phone phone = getPhone(i);
        if (phone != null) {
            return phone.isInEcm();
        }
        return false;
    }

    public SignalStrength getSignalStrength(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            return null;
        }
        return phone.getSignalStrength();
    }

    public UiccSlotInfo[] getUiccSlotsInfo() {
        String iccId;
        int i;
        int i2;
        enforceReadPrivilegedPermission();
        UiccSlot[] uiccSlots = UiccController.getInstance().getUiccSlots();
        if (uiccSlots == null) {
            Rlog.i(LOG_TAG, "slots is null.");
            return null;
        }
        UiccSlotInfo[] uiccSlotInfoArr = new UiccSlotInfo[uiccSlots.length];
        for (int i3 = 0; i3 < uiccSlots.length; i3++) {
            UiccSlot uiccSlot = uiccSlots[i3];
            if (uiccSlot != null) {
                UiccCard uiccCard = uiccSlot.getUiccCard();
                if (uiccCard != null) {
                    iccId = uiccCard.getCardId();
                } else {
                    iccId = uiccSlot.getIccId();
                }
                String str = iccId;
                switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$uicc$IccCardStatus$CardState[uiccSlot.getCardState().ordinal()]) {
                    case 1:
                        i = 1;
                        i2 = i;
                        break;
                    case 2:
                        i = 2;
                        i2 = i;
                        break;
                    case 3:
                        i = 3;
                        i2 = i;
                        break;
                    case 4:
                        i = 4;
                        i2 = i;
                        break;
                    default:
                        i2 = 0;
                        break;
                }
                uiccSlotInfoArr[i3] = new UiccSlotInfo(uiccSlot.isActive(), uiccSlot.isEuicc(), str, i2, uiccSlot.getPhoneId(), uiccSlot.isExtendedApduSupported());
            }
        }
        return uiccSlotInfoArr;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$internal$telephony$uicc$IccCardStatus$CardState = new int[IccCardStatus.CardState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardStatus$CardState[IccCardStatus.CardState.CARDSTATE_ABSENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardStatus$CardState[IccCardStatus.CardState.CARDSTATE_PRESENT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardStatus$CardState[IccCardStatus.CardState.CARDSTATE_ERROR.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardStatus$CardState[IccCardStatus.CardState.CARDSTATE_RESTRICTED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public boolean switchSlots(int[] iArr) {
        enforceModifyPermission();
        return ((Boolean) sendRequest(50, iArr)).booleanValue();
    }

    public void setRadioIndicationUpdateMode(int i, int i2, int i3) {
        enforceModifyPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            loge("setRadioIndicationUpdateMode fails with invalid subId: " + i);
            return;
        }
        phone.setRadioIndicationUpdateMode(i2, i3);
    }

    public void refreshUiccProfile(int i) {
        enforceModifyPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Phone phone = getPhone(i);
            if (phone == null) {
                return;
            }
            UiccCard uiccCard = phone.getUiccCard();
            if (uiccCard == null) {
                return;
            }
            UiccProfile uiccProfile = uiccCard.getUiccProfile();
            if (uiccProfile == null) {
                return;
            }
            uiccProfile.refresh();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean getDefaultDataEnabled() {
        return "true".equalsIgnoreCase(SystemProperties.get(DEFAULT_MOBILE_DATA_PROPERTY_NAME, "true"));
    }

    private boolean getDefaultDataRoamingEnabled(int i) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        return carrierConfigManager.getConfigForSubId(i).getBoolean("carrier_default_data_roaming_enabled_bool") | "true".equalsIgnoreCase(SystemProperties.get(DEFAULT_DATA_ROAMING_PROPERTY_NAME, "false"));
    }

    private int getDefaultNetworkType(int i) {
        return Integer.parseInt(TelephonyManager.getTelephonyProperty(this.mSubscriptionController.getPhoneId(i), DEFAULT_NETWORK_MODE_PROPERTY_NAME, String.valueOf(Phone.PREFERRED_NT_MODE)));
    }

    public void setCarrierTestOverride(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7) {
        enforceModifyPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            loge("setCarrierTestOverride fails with invalid subId: " + i);
            return;
        }
        phone.setCarrierTestOverride(str, str2, str3, str4, str5, str6, str7);
    }

    public int getCarrierIdListVersion(int i) {
        enforceReadPrivilegedPermission();
        Phone phone = getPhone(i);
        if (phone == null) {
            loge("getCarrierIdListVersion fails with invalid subId: " + i);
            return -1;
        }
        return phone.getCarrierIdListVersion();
    }
}
