package com.android.internal.telephony.cat;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import com.android.ims.ImsManager;
import com.android.ims.ImsSms;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.CatCmdMessage;
import com.android.internal.telephony.cat.Duration;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

public class CatService extends Handler implements AppInterface {
    private static final boolean DBG = false;
    private static final int DELAY_TIME = 1000;
    private static final int DEV_ID_DISPLAY = 2;
    private static final int DEV_ID_KEYPAD = 1;
    private static final int DEV_ID_NETWORK = 131;
    private static final int DEV_ID_TERMINAL = 130;
    private static final int DEV_ID_UICC = 129;
    protected static final int MSG_ID_ALPHA_NOTIFY = 9;
    protected static final int MSG_ID_CALL_SETUP = 4;
    static final int MSG_ID_CALL_SETUP_RESULT = 52;
    static final int MSG_ID_CALL_SETUP_STATUS = 50;
    private static final int MSG_ID_DELAY_SEND_SM_STATUS = 55;
    static final int MSG_ID_EVENT_DTMF_DONE = 42;
    static final int MSG_ID_EVENT_MMI_COMPLETE = 41;
    protected static final int MSG_ID_EVENT_NOTIFY = 3;
    static final int MSG_ID_EVENT_PAUSE_DONE = 43;
    static final int MSG_ID_EVENT_RESPONSE = 54;
    static final int MSG_ID_EXT_BASE = 50;
    protected static final int MSG_ID_ICC_CHANGED = 8;
    private static final int MSG_ID_ICC_RECORDS_LOADED = 20;
    private static final int MSG_ID_ICC_REFRESH = 30;
    protected static final int MSG_ID_PROACTIVE_COMMAND = 2;
    static final int MSG_ID_REFRESH = 5;
    static final int MSG_ID_RESPONSE = 6;
    static final int MSG_ID_RIL_MSG_DECODED = 10;
    static final int MSG_ID_SEND_SMS_DONE = 40;
    static final int MSG_ID_SEND_SM_STATUS = 51;
    static final int MSG_ID_SEND_SS_USSD_RESULT = 53;
    protected static final int MSG_ID_SESSION_END = 1;
    static final int MSG_ID_SIM_READY = 7;
    static final int PAUSE_DELAY_MILLIS = 3000;
    static final String STK_DEFAULT = "Default Message";
    private static IccRecords mIccRecords;
    private static UiccCardApplication mUiccApplication;
    private BipProxy mBipProxy;
    private CommandsInterface mCmdIf;
    private Context mContext;
    private HandlerThread mHandlerThread;
    private RilMessageDecoder mMsgDecoder;
    private Phone mPhone;
    private int mSlotId;
    private boolean mStkAppInstalled;
    private UiccController mUiccController;
    private static final Object sInstanceLock = new Object();
    private static CatService[] sInstance = null;
    private CatCmdMessage mCurrntCmd = null;
    private CatCmdMessage mMenuCmd = null;
    private CatCmdMessage mEventCmd = null;
    private IccCardStatus.CardState mCardState = IccCardStatus.CardState.CARDSTATE_ABSENT;
    private ImsSms mImsSms = null;

    private CatService(CommandsInterface ci, UiccCardApplication ca, IccRecords ir, Context context, IccFileHandler fh, UiccCard ic, int slotId) {
        this.mBipProxy = null;
        this.mMsgDecoder = null;
        this.mStkAppInstalled = false;
        if (ci == null || ca == null || ir == null || context == null || fh == null || ic == null) {
            throw new NullPointerException("Service: Input parameters must not be null");
        }
        this.mCmdIf = ci;
        this.mContext = context;
        this.mSlotId = slotId;
        this.mPhone = PhoneFactory.getPhone(this.mSlotId);
        this.mMsgDecoder = RilMessageDecoder.getInstance(this, fh, slotId);
        if (this.mMsgDecoder == null) {
            CatLog.d(this, "Null RilMessageDecoder instance");
            return;
        }
        this.mMsgDecoder.start();
        this.mCmdIf.setOnCatSessionEnd(this, 1, null);
        this.mCmdIf.setOnCatProactiveCmd(this, 2, null);
        this.mCmdIf.setOnCatEvent(this, 3, null);
        this.mCmdIf.setOnCatCallSetUp(this, 4, null);
        this.mCmdIf.setOnCatCallSetUpStatus(this, 50, null);
        this.mCmdIf.setOnCatSendSmStatus(this, 51, null);
        this.mCmdIf.setOnCatCallSetUpResult(this, 52, null);
        this.mCmdIf.setOnCatSendUssdResult(this, 53, null);
        this.mCmdIf.registerForIccRefresh(this, 30, null);
        this.mCmdIf.setOnCatCcAlphaNotify(this, 9, null);
        mIccRecords = ir;
        mUiccApplication = ca;
        mIccRecords.registerForRecordsLoaded(this, 20, null);
        CatLog.d(this, "registerForRecordsLoaded slotid=" + this.mSlotId + " instance:" + this);
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 8, null);
        this.mBipProxy = new BipProxy(this.mSlotId, this, this.mCmdIf, this.mContext);
        this.mStkAppInstalled = isStkAppInstalled();
        CatLog.d(this, "Running CAT service on Slotid: " + this.mSlotId + ". STK app installed:" + this.mStkAppInstalled);
    }

    public static CatService getInstance(CommandsInterface ci, Context context, UiccCard ic, int slotId) {
        CatService catService = null;
        UiccCardApplication ca = null;
        IccFileHandler fh = null;
        IccRecords ir = null;
        if (ic != null && (ca = ic.getApplicationIndex(0)) != null) {
            fh = ca.getIccFileHandler();
            ir = ca.getIccRecords();
        }
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                int simCount = TelephonyManager.getDefault().getSimCount();
                sInstance = new CatService[simCount];
                for (int i = 0; i < simCount; i++) {
                    sInstance[i] = null;
                }
            }
            if (sInstance[slotId] == null) {
                if (ci != null && ca != null && ir != null && context != null && fh != null && ic != null) {
                    sInstance[slotId] = new CatService(ci, ca, ir, context, fh, ic, slotId);
                }
            } else if (ir != null && mIccRecords != ir) {
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(sInstance[slotId]);
                }
                mIccRecords = ir;
                mUiccApplication = ca;
                mIccRecords.registerForRecordsLoaded(sInstance[slotId], 20, null);
                CatLog.d(sInstance[slotId], "registerForRecordsLoaded slotid=" + slotId + " instance:" + sInstance[slotId]);
            }
            catService = sInstance[slotId];
        }
        return catService;
    }

    public void dispose() {
        synchronized (sInstanceLock) {
            CatLog.d(this, "Disposing CatService object");
            mIccRecords.unregisterForRecordsLoaded(this);
            broadcastCardStateAndIccRefreshResp(IccCardStatus.CardState.CARDSTATE_ABSENT, null);
            this.mCmdIf.unSetOnCatSessionEnd(this);
            this.mCmdIf.unSetOnCatProactiveCmd(this);
            this.mCmdIf.unSetOnCatEvent(this);
            this.mCmdIf.unSetOnCatCallSetUp(this);
            this.mCmdIf.unSetOnCatCcAlphaNotify(this);
            this.mCmdIf.unregisterForIccRefresh(this);
            this.mCmdIf.unSetOnCatCallSetUpStatus(this);
            this.mCmdIf.unSetOnCatSendSmStatus(this);
            this.mCmdIf.unSetOnCatCallSetUpResult(this);
            this.mCmdIf.unSetOnCatSendUssdResult(this);
            if (this.mUiccController != null) {
                this.mUiccController.unregisterForIccChanged(this);
                this.mUiccController = null;
            }
            this.mMsgDecoder.dispose();
            this.mMsgDecoder = null;
            removeCallbacksAndMessages(null);
            if (sInstance != null) {
                if (SubscriptionManager.isValidSlotId(this.mSlotId)) {
                    sInstance[this.mSlotId] = null;
                } else {
                    CatLog.d(this, "error: invaild slot id: " + this.mSlotId);
                }
            }
        }
    }

    protected void finalize() {
        CatLog.d(this, "Service finalized");
    }

    private void handleRilMsg(RilMessage rilMsg) {
        CommandParams cmdParams;
        if (rilMsg != null) {
            switch (rilMsg.mId) {
                case 1:
                    handleSessionEnd();
                    break;
                case 2:
                    try {
                        CommandParams cmdParams2 = (CommandParams) rilMsg.mData;
                        if (cmdParams2 != null) {
                            if (rilMsg.mResCode == ResultCode.OK) {
                                handleCommand(cmdParams2, true);
                            } else {
                                sendTerminalResponse(cmdParams2.mCmdDet, rilMsg.mResCode, false, 0, null);
                            }
                        }
                    } catch (ClassCastException e) {
                        CatLog.d(this, "Fail to parse proactive command");
                        if (this.mCurrntCmd != null) {
                            sendTerminalResponse(this.mCurrntCmd.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                            return;
                        }
                        return;
                    }
                    break;
                case 3:
                    if (rilMsg.mResCode == ResultCode.OK && (cmdParams = (CommandParams) rilMsg.mData) != null) {
                        handleCommand(cmdParams, false);
                        break;
                    }
                    break;
                case 5:
                    CommandParams cmdParams3 = (CommandParams) rilMsg.mData;
                    if (cmdParams3 != null) {
                        handleCommand(cmdParams3, false);
                    }
                    break;
            }
        }
    }

    private boolean isSupportedSetupEventCommand(CatCmdMessage cmdMsg) {
        boolean flag = true;
        int[] arr$ = cmdMsg.getSetEventList().eventList;
        for (int eventVal : arr$) {
            CatLog.d(this, "Event: " + eventVal);
            switch (eventVal) {
                case 5:
                case 7:
                    break;
                case 6:
                default:
                    flag = false;
                    break;
            }
        }
        return flag;
    }

    private void handleCommand(CommandParams cmdParams, boolean isProactiveCmd) {
        CatLog.d(this, cmdParams.getCommandType().name());
        CatCmdMessage cmdMsg = new CatCmdMessage(cmdParams);
        switch (cmdParams.getCommandType()) {
            case SET_UP_MENU:
                if (removeMenu(cmdMsg.getMenu())) {
                    this.mMenuCmd = null;
                } else {
                    this.mMenuCmd = cmdMsg;
                    if (this.mMenuCmd.getMenu() != null) {
                        try {
                            SystemProperties.set("gsm.SET_UP_MENU", this.mMenuCmd.getMenu().title);
                        } catch (IllegalArgumentException e) {
                            CatLog.d(this, "The menu title is too long to save in properties: " + this.mMenuCmd.getMenu().title);
                        }
                    }
                }
                if (this.mMenuCmd != null && (cmdMsg.getMenu().disableTitleIcon || cmdMsg.getMenu().disableItemsIcon)) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.PRFRMD_ICON_NOT_DISPLAYED, false, 0, null);
                } else {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                }
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case DISPLAY_TEXT:
            case SELECT_ITEM:
            case GET_INPUT:
            case GET_INKEY:
            case PLAY_TONE:
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case REFRESH:
                sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                if (cmdMsg.getRefreshSettings().fileList != null && cmdMsg.getRefreshSettings().fileChanged) {
                    Toast.makeText(this.mContext, "Refreshing... SIM data has changed.", 1).show();
                } else if (!cmdMsg.getRefreshSettings().fileChanged) {
                    Toast.makeText(this.mContext, "Refreshing SIM data", 1).show();
                }
                cmdParams.mCmdDet.typeOfCommand = AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.value();
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case SET_UP_IDLE_MODE_TEXT:
                if (cmdMsg.geTextMessage().disableIcon) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.PRFRMD_ICON_NOT_DISPLAYED, false, 0, null);
                } else {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                }
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case PROVIDE_LOCAL_INFORMATION:
                switch (cmdParams.mCmdDet.commandQualifier) {
                    case 3:
                        ResponseData resp = new DTTZResponseData(null);
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, resp);
                        break;
                    case 4:
                        ResponseData resp2 = new LanguageResponseData(Locale.getDefault().getLanguage());
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, resp2);
                        break;
                    default:
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                        break;
                }
                break;
            case SET_UP_EVENT_LIST:
                sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                this.mEventCmd = cmdMsg;
                if (this.mEventCmd.getSetEventList().eventList.length == 1 && this.mEventCmd.getSetEventList().eventList[0] == EventList.REMOVE_EVENT.value()) {
                    CatLog.d(this, "Remove Event");
                    this.mEventCmd = null;
                    if (this.mBipProxy != null) {
                        BipProxy bipProxy = this.mBipProxy;
                        BipProxy.dataAvailableEvent = false;
                        BipProxy bipProxy2 = this.mBipProxy;
                        BipProxy.channelStatusEvent = false;
                    }
                } else {
                    CatCmdMessage.SetupEventListSettings settings = this.mEventCmd.getSetEventList();
                    for (int i = 0; i < settings.eventList.length; i++) {
                        int event = settings.eventList[i];
                        switch (EventList.values()[event]) {
                            case DATA_AVAILABLE:
                                BipProxy bipProxy3 = this.mBipProxy;
                                BipProxy.dataAvailableEvent = true;
                                break;
                            case CHANNEL_STATUS:
                                BipProxy bipProxy4 = this.mBipProxy;
                                BipProxy.channelStatusEvent = true;
                                break;
                            default:
                                break;
                        }
                    }
                    Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
                    intent.putExtra("slot", this.mSlotId);
                    CatLog.d(this, "Sending CmdMsg: " + cmdMsg + " on slotid:" + this.mSlotId);
                    intent.putExtra("STK CMD", cmdMsg);
                    this.mContext.sendBroadcast(intent);
                }
                break;
            case LAUNCH_BROWSER:
                if (((LaunchBrowserParams) cmdParams).mConfirmMsg.text != null && ((LaunchBrowserParams) cmdParams).mConfirmMsg.text.equals(STK_DEFAULT)) {
                    CharSequence message = this.mContext.getText(R.string.low_internal_storage_view_text);
                    ((LaunchBrowserParams) cmdParams).mConfirmMsg.text = message.toString();
                }
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case SEND_DTMF:
                if (cmdParams instanceof SendDTMFParams) {
                    SendDTMFParams dtmf = (SendDTMFParams) cmdParams;
                    CatLog.d(this, "dtmf: " + dtmf.mTextMsg.title + ", " + dtmf.mTextMsg.text + ", " + dtmf.mDTMFString);
                    if (this.mPhone.getState() != PhoneConstants.State.OFFHOOK) {
                        sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 7, null);
                    } else {
                        Toast.makeText(this.mContext, dtmf.mTextMsg.text, 1).show();
                        handleDTMFString(cmdParams, 0);
                    }
                } else if ((cmdParams instanceof DisplayTextParams) && ((DisplayTextParams) cmdParams).mTextMsg.text != null && ((DisplayTextParams) cmdParams).mTextMsg.text.equals(STK_DEFAULT)) {
                    CharSequence message2 = this.mContext.getText(R.string.lockscreen_unlock_label);
                    ((DisplayTextParams) cmdParams).mTextMsg.text = message2.toString();
                }
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case SEND_SMS:
                if (cmdParams instanceof SMSTPDUParams) {
                    SMSTPDUParams smsTPDU = (SMSTPDUParams) cmdParams;
                    CatLog.d(this, "sms: " + smsTPDU.mTextMsg.title + ", " + smsTPDU.mTextMsg.text + ", " + smsTPDU.mSMSAdress + ", " + smsTPDU.mLength);
                    CatLog.d(this, "data2string = " + IccUtils.bytesToHexString(smsTPDU.mData));
                    if (smsTPDU.mTextMsg.text != null) {
                        Toast.makeText(this.mContext, smsTPDU.mTextMsg.text, 1).show();
                    }
                    Message reply = obtainMessage(40, cmdMsg);
                    String encodedScAddrString = null;
                    if (smsTPDU.mSMSAdress != null) {
                        byte[] encodedScAddr = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(smsTPDU.mSMSAdress);
                        encodedScAddrString = IccUtils.bytesToHexString(encodedScAddr);
                    }
                    if (checkImsStatus()) {
                        this.mImsSms.sendImsGsmSms(encodedScAddrString, IccUtils.bytesToHexString(smsTPDU.mData), 0, 0, reply);
                    } else {
                        this.mCmdIf.sendSMS(encodedScAddrString, IccUtils.bytesToHexString(smsTPDU.mData), reply);
                    }
                } else if ((cmdParams instanceof DisplayTextParams) && ((DisplayTextParams) cmdParams).mTextMsg.text != null && ((DisplayTextParams) cmdParams).mTextMsg.text.equals(STK_DEFAULT)) {
                    CharSequence message3 = this.mContext.getText(R.string.lockscreen_unlock_label);
                    ((DisplayTextParams) cmdParams).mTextMsg.text = message3.toString();
                }
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case SEND_SS:
                if (cmdParams instanceof SendSSParams) {
                    SendSSParams ss = (SendSSParams) cmdParams;
                    CatLog.d(this, "ss: " + ss.mTextMsg.title + ", " + ss.mTextMsg.text + ", " + ss.mSSString);
                    Toast.makeText(this.mContext, ss.mTextMsg.text, 1).show();
                    this.mPhone.registerForMmiComplete(this, 41, cmdMsg);
                    try {
                        this.mPhone.dial(ss.mSSString, 0);
                    } catch (CallStateException e2) {
                        this.mPhone.unregisterForMmiComplete(this);
                        sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.SS_RETURN_ERROR, false, 0, null);
                    }
                } else if ((cmdParams instanceof DisplayTextParams) && ((DisplayTextParams) cmdParams).mTextMsg.text != null && ((DisplayTextParams) cmdParams).mTextMsg.text.equals(STK_DEFAULT)) {
                    CharSequence message4 = this.mContext.getText(R.string.lockscreen_unlock_label);
                    ((DisplayTextParams) cmdParams).mTextMsg.text = message4.toString();
                }
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case SEND_USSD:
                if (cmdParams instanceof SendUSSDParams) {
                    SendUSSDParams ussd = (SendUSSDParams) cmdParams;
                    CatLog.d(this, "ussd: " + ussd.mTextMsg.title + ", " + ussd.mTextMsg.text + ", " + ussd.mUSSDString);
                    Toast.makeText(this.mContext, ussd.mTextMsg.text, 1).show();
                    this.mPhone.registerForMmiComplete(this, 41, cmdMsg);
                    this.mPhone.sendUssdResponse(ussd.mUSSDString);
                } else if ((cmdParams instanceof DisplayTextParams) && ((DisplayTextParams) cmdParams).mTextMsg.text != null && ((DisplayTextParams) cmdParams).mTextMsg.text.equals(STK_DEFAULT)) {
                    CharSequence message5 = this.mContext.getText(R.string.lockscreen_unlock_label);
                    ((DisplayTextParams) cmdParams).mTextMsg.text = message5.toString();
                }
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case SET_UP_CALL:
                if (((CallSetupParams) cmdParams).mConfirmMsg.text != null && ((CallSetupParams) cmdParams).mConfirmMsg.text.equals(STK_DEFAULT)) {
                    CharSequence message6 = this.mContext.getText(R.string.low_internal_storage_view_text_no_boot);
                    ((CallSetupParams) cmdParams).mConfirmMsg.text = message6.toString();
                }
                this.mCurrntCmd = cmdMsg;
                broadcastCatCmdIntent(cmdMsg);
                break;
            case LANGUAGE_NOTIFICATION:
                sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                break;
            case OPEN_CHANNEL:
                CatCmdMessage.ChannelSettings newChannel = cmdMsg.getChannelSettings();
                if (newChannel == null) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                } else if (!this.mBipProxy.canHandleNewChannel()) {
                    ResponseData resp3 = new OpenChannelResponseData(newChannel.bufSize, null, newChannel.bearerDescription);
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.BIP_ERROR, true, 1, resp3);
                } else {
                    if (cmdMsg.geTextMessage() != null) {
                    }
                    this.mCurrntCmd = cmdMsg;
                    this.mBipProxy.handleBipCommand(cmdMsg);
                }
                break;
            case CLOSE_CHANNEL:
            case RECEIVE_DATA:
            case SEND_DATA:
            case GET_CHANNEL_STATUS:
                this.mCurrntCmd = cmdMsg;
                this.mBipProxy.handleBipCommand(cmdMsg);
                break;
            default:
                CatLog.d(this, "Unsupported command");
                break;
        }
    }

    private void broadcastCatCmdIntent(CatCmdMessage cmdMsg) {
        Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
        intent.putExtra("STK CMD", cmdMsg);
        intent.putExtra("SLOT_ID", this.mSlotId);
        CatLog.d(this, "Sending CmdMsg: " + cmdMsg + " on slotid:" + this.mSlotId);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    private void handleSessionEnd() {
        CatLog.d(this, "SESSION END on " + this.mSlotId);
        this.mCurrntCmd = this.mMenuCmd;
        Intent intent = new Intent(AppInterface.CAT_SESSION_END_ACTION);
        intent.putExtra("SLOT_ID", this.mSlotId);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
        this.mBipProxy.handleBipCommand(null);
    }

    protected void sendTerminalResponse(CommandDetails cmdDet, ResultCode resultCode, boolean includeAdditionalInfo, int additionalInfo, ResponseData resp) {
        if (cmdDet != null) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            Input cmdInput = null;
            if (this.mCurrntCmd != null) {
                cmdInput = this.mCurrntCmd.geInput();
            }
            int tag = ComprehensionTlvTag.COMMAND_DETAILS.value();
            if (cmdDet.compRequired) {
                tag |= 128;
            }
            buf.write(tag);
            buf.write(3);
            buf.write(cmdDet.commandNumber);
            buf.write(cmdDet.typeOfCommand);
            buf.write(cmdDet.commandQualifier);
            buf.write(ComprehensionTlvTag.DEVICE_IDENTITIES.value());
            buf.write(2);
            buf.write(130);
            buf.write(129);
            int tag2 = ComprehensionTlvTag.RESULT.value();
            if (cmdDet.compRequired) {
                tag2 |= 128;
            }
            buf.write(tag2);
            int length = includeAdditionalInfo ? 2 : 1;
            buf.write(length);
            buf.write(resultCode.value());
            if (includeAdditionalInfo) {
                buf.write(additionalInfo);
            }
            if (resp != null) {
                resp.format(buf);
            } else {
                encodeOptionalTags(cmdDet, resultCode, cmdInput, buf);
            }
            byte[] rawData = buf.toByteArray();
            String hexString = IccUtils.bytesToHexString(rawData);
            this.mCmdIf.sendTerminalResponse(hexString, null);
        }
    }

    private void encodeOptionalTags(CommandDetails cmdDet, ResultCode resultCode, Input cmdInput, ByteArrayOutputStream buf) {
        AppInterface.CommandType cmdType = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
        if (cmdType != null) {
            switch (cmdType) {
                case PROVIDE_LOCAL_INFORMATION:
                    if (cmdDet.commandQualifier == 4 && resultCode.value() == ResultCode.OK.value()) {
                        getPliResponse(buf);
                        break;
                    }
                    break;
                case GET_INKEY:
                    if (resultCode.value() == ResultCode.NO_RESPONSE_FROM_USER.value() && cmdInput != null && cmdInput.duration != null) {
                        getInKeyResponse(buf, cmdInput);
                        break;
                    }
                    break;
                default:
                    CatLog.d(this, "encodeOptionalTags() Unsupported Cmd details=" + cmdDet);
                    break;
            }
            return;
        }
        CatLog.d(this, "encodeOptionalTags() bad Cmd details=" + cmdDet);
    }

    private void getInKeyResponse(ByteArrayOutputStream buf, Input cmdInput) {
        int tag = ComprehensionTlvTag.DURATION.value();
        buf.write(tag);
        buf.write(2);
        Duration.TimeUnit timeUnit = cmdInput.duration.timeUnit;
        buf.write(Duration.TimeUnit.SECOND.value());
        buf.write(cmdInput.duration.timeInterval);
    }

    private void getPliResponse(ByteArrayOutputStream buf) {
        String lang = SystemProperties.get("persist.sys.language");
        if (lang != null) {
            int tag = ComprehensionTlvTag.LANGUAGE.value();
            buf.write(tag);
            ResponseData.writeLength(buf, lang.length());
            buf.write(lang.getBytes(), 0, lang.length());
        }
    }

    private void sendMenuSelection(int menuId, boolean helpRequired) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(211);
        buf.write(0);
        int tag = ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128;
        buf.write(tag);
        buf.write(2);
        buf.write(1);
        buf.write(129);
        int tag2 = ComprehensionTlvTag.ITEM_ID.value() | 128;
        buf.write(tag2);
        buf.write(1);
        buf.write(menuId);
        if (helpRequired) {
            int tag3 = ComprehensionTlvTag.HELP_REQUEST.value();
            buf.write(tag3);
            buf.write(0);
        }
        byte[] rawData = buf.toByteArray();
        int len = rawData.length - 2;
        rawData[1] = (byte) len;
        String hexString = IccUtils.bytesToHexString(rawData);
        this.mCmdIf.sendEnvelope(hexString, null);
    }

    private boolean checkImsStatus() {
        boolean ret = false;
        ImsManager imsManager = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
        Phone imsPhone = this.mPhone.getImsPhone();
        if (imsManager != null) {
            this.mImsSms = imsManager.getSmsInterface();
        }
        if (this.mImsSms != null && imsPhone != null && imsPhone.getServiceState().getState() == 0) {
            ret = true;
        }
        CatLog.d(this, "checkImsStatus ret= " + ret);
        return ret;
    }

    public void onEventDownload(CatEventMessage eventMsg) {
        CatLog.d(this, "Download event: " + eventMsg.getEvent());
        eventDownload(eventMsg.getEvent(), eventMsg.getSourceId(), eventMsg.getDestId(), eventMsg.getAdditionalInfo(), eventMsg.isOneShot());
    }

    protected void eventDownload(int event, int sourceId, int destinationId, byte[] additionalInfo, boolean oneShot) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(BerTlv.BER_EVENT_DOWNLOAD_TAG);
        buf.write(0);
        int tag = ComprehensionTlvTag.EVENT_LIST.value() | 128;
        buf.write(tag);
        buf.write(1);
        buf.write(event);
        int tag2 = ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128;
        buf.write(tag2);
        buf.write(2);
        buf.write(sourceId);
        buf.write(destinationId);
        switch (event) {
            case 5:
                CatLog.d(sInstance, " Sending Idle Screen Available event download to ICC");
                break;
            case 7:
                CatLog.d(sInstance, " Sending Language Selection event download to ICC");
                int tag3 = ComprehensionTlvTag.LANGUAGE.value() | 128;
                buf.write(tag3);
                buf.write(2);
                break;
        }
        if (additionalInfo != null) {
            for (byte b : additionalInfo) {
                buf.write(b);
            }
        }
        byte[] rawData = buf.toByteArray();
        int len = rawData.length - 2;
        rawData[1] = (byte) len;
        String hexString = IccUtils.bytesToHexString(rawData);
        CatLog.d(this, "ENVELOPE COMMAND: " + hexString);
        this.mCmdIf.sendEnvelope(hexString, null);
    }

    public static UiccCardApplication getUiccApplication() {
        return mUiccApplication;
    }

    public static AppInterface getInstance() {
        int slotId = 0;
        SubscriptionController sControl = SubscriptionController.getInstance();
        if (sControl != null) {
            slotId = sControl.getSlotId(sControl.getDefaultSubId());
        }
        return getInstance(null, null, null, slotId);
    }

    public static AppInterface getInstance(int slotId) {
        return getInstance(null, null, null, slotId);
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        AsyncResult ar2;
        AsyncResult ar3;
        AsyncResult ar4;
        AsyncResult ar5;
        CatLog.d(this, "handleMessage[" + msg.what + "]");
        switch (msg.what) {
            case 1:
            case 2:
            case 3:
            case 5:
                CatLog.d(this, "ril message arrived,slotid:" + this.mSlotId);
                String data = null;
                if (msg.obj != null && (ar5 = (AsyncResult) msg.obj) != null && ar5.result != null) {
                    try {
                        data = (String) ar5.result;
                    } catch (ClassCastException e) {
                        return;
                    }
                    break;
                }
                this.mMsgDecoder.sendStartDecodingMessageParams(new RilMessage(msg.what, data));
                return;
            case 4:
                this.mMsgDecoder.sendStartDecodingMessageParams(new RilMessage(msg.what, null));
                return;
            case 6:
                handleCmdResponse((CatResponseMessage) msg.obj);
                return;
            case 7:
                CatLog.d(this, "SIM ready. Reporting STK service running now...");
                this.mCmdIf.reportStkServiceIsRunning(null);
                return;
            case 8:
                CatLog.d(this, "MSG_ID_ICC_CHANGED");
                updateIccAvailability();
                return;
            case 9:
                CatLog.d(this, "Received CAT CC Alpha message from card");
                if (msg.obj != null) {
                    AsyncResult ar6 = (AsyncResult) msg.obj;
                    if (ar6 != null && ar6.result != null) {
                        broadcastAlphaMessage((String) ar6.result);
                        return;
                    } else {
                        CatLog.d(this, "CAT Alpha message: ar.result is null");
                        return;
                    }
                }
                CatLog.d(this, "CAT Alpha message: msg.obj is null");
                return;
            case 10:
                handleRilMsg((RilMessage) msg.obj);
                return;
            case 20:
                return;
            case 30:
                if (msg.obj != null) {
                    AsyncResult ar7 = (AsyncResult) msg.obj;
                    if (ar7 != null && ar7.result != null) {
                        broadcastCardStateAndIccRefreshResp(IccCardStatus.CardState.CARDSTATE_PRESENT, (IccRefreshResponse) ar7.result);
                        return;
                    } else {
                        CatLog.d(this, "Icc REFRESH with exception: " + ar7.exception);
                        return;
                    }
                }
                CatLog.d(this, "IccRefresh Message is null");
                return;
            case 40:
                handleSendSMSComplete((AsyncResult) msg.obj);
                return;
            case 41:
                handleSendSSComplete((AsyncResult) msg.obj);
                return;
            case 42:
                AsyncResult ar8 = (AsyncResult) msg.obj;
                if (ar8.exception == null) {
                    handleDTMFString((CommandParams) ar8.userObj, msg.arg1);
                    return;
                } else {
                    sendTerminalResponse(((SendDTMFParams) ar8.userObj).mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 7, null);
                    return;
                }
            case MSG_ID_EVENT_PAUSE_DONE:
                handleDTMFString((CommandParams) msg.obj, msg.arg1);
                return;
            case 50:
                if (msg.obj != null && (ar4 = (AsyncResult) msg.obj) != null && ar4.result != null) {
                    try {
                        String data2 = (String) ar4.result;
                        CatLog.d(this, "data len = " + data2.length());
                        byte[] bArr = new byte[data2.length() / 2];
                        byte[] Buf = IccUtils.hexStringToBytes(data2);
                        if (Buf.length < 5) {
                            CatLog.d(this, "call setup dataus data error");
                        } else if (Buf[4] == 0 && Buf[6] != 0) {
                            Toast.makeText(this.mContext, "Allowed, no modification", 1).show();
                        } else if (Buf[4] == 2) {
                            Toast.makeText(this.mContext, "not Allowed", 1).show();
                        } else if (Buf[4] == 1) {
                            Toast.makeText(this.mContext, "Allowed, with modification", 1).show();
                        } else {
                            CatLog.d(this, "result is " + data2);
                        }
                        return;
                    } catch (ClassCastException e2) {
                        CatLog.d(this, "call setup status get exception");
                        return;
                    }
                }
                return;
            case 51:
                if (msg.obj != null) {
                    Message delayMsg = obtainMessage(55, msg.obj);
                    sendMessageDelayed(delayMsg, 1000L);
                    return;
                }
                return;
            case 52:
                if (msg.obj != null && (ar3 = (AsyncResult) msg.obj) != null && ar3.result != null) {
                    try {
                        String data3 = (String) ar3.result;
                        CatLog.d(this, "call setup result data is =" + data3 + "data len = " + data3.length());
                        byte[] bArr2 = new byte[data3.length() / 2];
                        byte[] Buf2 = IccUtils.hexStringToBytes(data3);
                        if (Buf2.length < 5) {
                            CatLog.d(this, "call setup status data error");
                        } else if (Buf2[4] == 0) {
                            Toast.makeText(this.mContext, "The user's choice is rejected", 1).show();
                        } else if (Buf2[4] == 1) {
                            Toast.makeText(this.mContext, "The user's choice is accepted", 1).show();
                        }
                        return;
                    } catch (ClassCastException e3) {
                        CatLog.d(this, "call setup status get exception");
                        return;
                    }
                }
                return;
            case 53:
                if (msg.obj != null && (ar2 = (AsyncResult) msg.obj) != null && ar2.result != null) {
                    try {
                        String data4 = (String) ar2.result;
                        CatLog.d(this, "send ss ussd result data is =" + data4 + "data len = " + data4.length());
                        byte[] bArr3 = new byte[data4.length() / 2];
                        byte[] Buf3 = IccUtils.hexStringToBytes(data4);
                        if (Buf3.length < 6) {
                            CatLog.d(this, "send ss ussd result data error");
                        } else if (Buf3[4] == 0) {
                            Toast.makeText(this.mContext, "Not Done", 1).show();
                        } else if (Buf3[4] == 1) {
                            Toast.makeText(this.mContext, "Done", 1).show();
                        }
                        return;
                    } catch (ClassCastException e4) {
                        CatLog.d(this, "send ss ussd status get exception");
                        return;
                    }
                }
                return;
            case 54:
                handleEventResponse((CatResponseMessage) msg.obj);
                return;
            case 55:
                if (msg.obj != null && (ar = (AsyncResult) msg.obj) != null && ar.result != null) {
                    try {
                        String data5 = (String) ar.result;
                        CatLog.d(this, "data len = " + data5.length());
                        byte[] bArr4 = new byte[data5.length() / 2];
                        byte[] Buf4 = IccUtils.hexStringToBytes(data5);
                        if (Buf4.length < 5) {
                            CatLog.d(this, "send sms status data error");
                        } else {
                            int status = Buf4[4] & 255;
                            CatLog.d(this, "status = " + status);
                            Intent intent = new Intent(AppInterface.CAT_EVENT_SEND_SM_STATUS);
                            intent.putExtra("STK SendSmStatus", status);
                            this.mContext.sendBroadcast(intent);
                        }
                        return;
                    } catch (ClassCastException e5) {
                        CatLog.d(this, "send sm status get exception");
                        return;
                    }
                }
                return;
            default:
                throw new AssertionError("Unrecognized CAT command: " + msg.what);
        }
    }

    private void broadcastCardStateAndIccRefreshResp(IccCardStatus.CardState cardState, IccRefreshResponse iccRefreshState) {
        Intent intent = new Intent(AppInterface.CAT_ICC_STATUS_CHANGE);
        boolean cardPresent = cardState == IccCardStatus.CardState.CARDSTATE_PRESENT;
        if (iccRefreshState != null) {
            intent.putExtra(AppInterface.REFRESH_RESULT, iccRefreshState.refreshResult);
            CatLog.d(this, "Sending IccResult with Result: " + iccRefreshState.refreshResult);
        }
        intent.putExtra(AppInterface.CARD_STATUS, cardPresent);
        CatLog.d(this, "Sending Card Status: " + cardState + " cardPresent: " + cardPresent);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    private void broadcastAlphaMessage(String alphaString) {
        CatLog.d(this, "Broadcasting CAT Alpha message from card: " + alphaString);
        Intent intent = new Intent(AppInterface.CAT_ALPHA_NOTIFY_ACTION);
        intent.addFlags(268435456);
        intent.putExtra(AppInterface.ALPHA_STRING, alphaString);
        intent.putExtra("SLOT_ID", this.mSlotId);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    @Override
    public synchronized void onCmdResponse(CatResponseMessage resMsg) {
        if (resMsg != null) {
            Message msg = obtainMessage(6, resMsg);
            msg.sendToTarget();
        }
    }

    @Override
    public synchronized void onEventResponse(CatResponseMessage resMsg) {
        if (resMsg != null) {
            Message msg = obtainMessage(54, resMsg);
            msg.sendToTarget();
        }
    }

    private boolean validateResponse(CatResponseMessage resMsg) {
        if (resMsg.mCmdDet.typeOfCommand == AppInterface.CommandType.SET_UP_EVENT_LIST.value() || resMsg.mCmdDet.typeOfCommand == AppInterface.CommandType.SET_UP_MENU.value()) {
            CatLog.d(this, "CmdType: " + resMsg.mCmdDet.typeOfCommand);
            return true;
        }
        if (this.mCurrntCmd == null) {
            return false;
        }
        boolean validResponse = resMsg.mCmdDet.compareTo(this.mCurrntCmd.mCmdDet);
        CatLog.d(this, "isResponse for last valid cmd: " + validResponse);
        return validResponse;
    }

    private boolean removeMenu(Menu menu) {
        try {
            if (menu.items.size() == 1) {
                if (menu.items.get(0) == null) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException e) {
            CatLog.d(this, "Unable to get Menu's items size");
            return true;
        }
    }

    private void handleEventResponse(CatResponseMessage eventMsg) {
        if (this.mEventCmd != null) {
            eventMsg.getCmdDetails();
            switch (AppInterface.CommandType.fromInt(cmdDet.typeOfCommand)) {
                case SET_UP_EVENT_LIST:
                    if (eventMsg.mEventValue == 5) {
                        eventDownload(eventMsg.mEventValue, 2, 129, eventMsg.mAddedInfo, false);
                    } else {
                        eventDownload(eventMsg.mEventValue, 130, 129, eventMsg.mAddedInfo, false);
                    }
                    this.mEventCmd = null;
                    break;
                default:
                    CatLog.d(this, "here must control event");
                    break;
            }
        }
    }

    private void handleCmdResponse(CatResponseMessage resMsg) {
        if (!validateResponse(resMsg)) {
            CatLog.d(this, "the response is not valid");
        }
        ResponseData resp = null;
        boolean helpRequired = false;
        CommandDetails cmdDet = resMsg.getCmdDetails();
        AppInterface.CommandType type = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
        CatLog.d(this, "resMsg.mResCode=" + resMsg.mResCode + ",TypeOfCommand=" + cmdDet.typeOfCommand);
        switch (resMsg.mResCode) {
            case HELP_INFO_REQUIRED:
                helpRequired = true;
            case OK:
            case PRFRMD_WITH_PARTIAL_COMPREHENSION:
            case PRFRMD_WITH_MISSING_INFO:
            case PRFRMD_WITH_ADDITIONAL_EFS_READ:
            case PRFRMD_ICON_NOT_DISPLAYED:
            case PRFRMD_MODIFIED_BY_NAA:
            case PRFRMD_LIMITED_SERVICE:
            case PRFRMD_WITH_MODIFICATION:
            case PRFRMD_NAA_NOT_ACTIVE:
            case PRFRMD_TONE_NOT_PLAYED:
            case TERMINAL_CRNTLY_UNABLE_TO_PROCESS:
                switch (type) {
                    case SET_UP_MENU:
                        boolean helpRequired2 = resMsg.mResCode == ResultCode.HELP_INFO_REQUIRED;
                        sendMenuSelection(resMsg.mUsersMenuSelection, helpRequired2);
                        break;
                    case DISPLAY_TEXT:
                        if (this.mCurrntCmd.geTextMessage().responseNeeded) {
                            if (resMsg.mResCode == ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS) {
                                resMsg.setAdditionalInfo(1);
                            } else {
                                resMsg.mIncludeAdditionalInfo = false;
                                resMsg.mAdditionalInfo = 0;
                            }
                        }
                        break;
                    case SET_UP_EVENT_LIST:
                        if (5 == resMsg.mEventValue) {
                            eventDownload(resMsg.mEventValue, 2, 129, resMsg.mAddedInfo, false);
                        } else {
                            eventDownload(resMsg.mEventValue, 130, 129, resMsg.mAddedInfo, false);
                        }
                        break;
                    case SELECT_ITEM:
                        resp = new SelectItemResponseData(resMsg.mUsersMenuSelection);
                        break;
                    case GET_INPUT:
                    case GET_INKEY:
                        Input input = this.mCurrntCmd.geInput();
                        if (!input.yesNo) {
                            if (!helpRequired) {
                                resp = new GetInkeyInputResponseData(resMsg.mUsersInput, input.ucs2, input.packed);
                            }
                        } else {
                            resp = new GetInkeyInputResponseData(resMsg.mUsersYesNoSelection);
                        }
                        break;
                    case SET_UP_CALL:
                        this.mCmdIf.handleCallSetupRequestFromSim(resMsg.mUsersConfirm, null);
                        this.mCurrntCmd = null;
                        break;
                    case OPEN_CHANNEL:
                        if (resMsg.mResCode == ResultCode.OK && resMsg.mUsersConfirm) {
                            this.mBipProxy.handleBipCommand(this.mCurrntCmd);
                        }
                        break;
                }
                sendTerminalResponse(cmdDet, resMsg.mResCode, resMsg.mIncludeAdditionalInfo, resMsg.mAdditionalInfo, resp);
                if (AppInterface.CommandType.fromInt(cmdDet.typeOfCommand) == AppInterface.CommandType.SET_UP_MENU) {
                    this.mCurrntCmd = this.mMenuCmd;
                } else {
                    this.mCurrntCmd = null;
                }
                break;
            case BACKWARD_MOVE_BY_USER:
            case USER_NOT_ACCEPT:
                switch (type) {
                    case SET_UP_MENU:
                        break;
                    case OPEN_CHANNEL:
                        if (!resMsg.mUsersConfirm && this.mCurrntCmd.geTextMessage().responseNeeded) {
                            CatCmdMessage.ChannelSettings params = this.mCurrntCmd.getChannelSettings();
                            resMsg.mResCode = ResultCode.USER_NOT_ACCEPT;
                            resp = new OpenChannelResponseData(params.bufSize, null, params.bearerDescription);
                        }
                        sendTerminalResponse(cmdDet, resMsg.mResCode, false, 0, resp);
                        break;
                    default:
                        resp = null;
                        break;
                }
                sendTerminalResponse(cmdDet, resMsg.mResCode, resMsg.mIncludeAdditionalInfo, resMsg.mAdditionalInfo, resp);
                if (AppInterface.CommandType.fromInt(cmdDet.typeOfCommand) == AppInterface.CommandType.SET_UP_MENU) {
                }
                break;
            case NO_RESPONSE_FROM_USER:
            case UICC_SESSION_TERM_BY_USER:
                resp = null;
                sendTerminalResponse(cmdDet, resMsg.mResCode, resMsg.mIncludeAdditionalInfo, resMsg.mAdditionalInfo, resp);
                if (AppInterface.CommandType.fromInt(cmdDet.typeOfCommand) == AppInterface.CommandType.SET_UP_MENU) {
                }
                break;
        }
    }

    private boolean isStkAppInstalled() {
        Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
        PackageManager pm = this.mContext.getPackageManager();
        List<ResolveInfo> broadcastReceivers = pm.queryBroadcastReceivers(intent, 128);
        int numReceiver = broadcastReceivers == null ? 0 : broadcastReceivers.size();
        return numReceiver > 0;
    }

    public void update(CommandsInterface ci, Context context, UiccCard ic) {
        UiccCardApplication ca = null;
        IccRecords ir = null;
        if (ic != null && (ca = ic.getApplicationIndex(0)) != null) {
            ir = ca.getIccRecords();
        }
        synchronized (sInstanceLock) {
            if (ir != null) {
                if (mIccRecords != ir) {
                    if (mIccRecords != null) {
                        mIccRecords.unregisterForRecordsLoaded(this);
                    }
                    CatLog.d(this, "Reinitialize the Service with SIMRecords and UiccCardApplication");
                    mIccRecords = ir;
                    mUiccApplication = ca;
                    mIccRecords.registerForRecordsLoaded(this, 20, null);
                    CatLog.d(this, "registerForRecordsLoaded slotid=" + this.mSlotId + " instance:" + this);
                }
            }
        }
    }

    void updateIccAvailability() {
        if (this.mUiccController != null) {
            IccCardStatus.CardState newState = IccCardStatus.CardState.CARDSTATE_ABSENT;
            UiccCard newCard = this.mUiccController.getUiccCard(this.mSlotId);
            if (newCard != null) {
                newState = newCard.getCardState();
            }
            IccCardStatus.CardState oldState = this.mCardState;
            this.mCardState = newState;
            CatLog.d(this, "New Card State = " + newState + " Old Card State = " + oldState);
            if (oldState == IccCardStatus.CardState.CARDSTATE_PRESENT && newState != IccCardStatus.CardState.CARDSTATE_PRESENT) {
                broadcastCardStateAndIccRefreshResp(newState, null);
            } else if (oldState != IccCardStatus.CardState.CARDSTATE_PRESENT && newState == IccCardStatus.CardState.CARDSTATE_PRESENT) {
                this.mCmdIf.reportStkServiceIsRunning(null);
            }
        }
    }

    private void handleSendSMSComplete(AsyncResult ar) {
        if (ar != null) {
            CatCmdMessage cmdMsg = (CatCmdMessage) ar.userObj;
            if (ar.exception == null) {
                sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, null);
            } else {
                sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.SMS_RP_ERROR, false, 0, null);
                CatLog.d(this, "MSG_ID_SEND_SMS_DONE error!!");
            }
        }
    }

    private void handleSendSSComplete(AsyncResult ar) {
        this.mPhone.unregisterForMmiComplete(this);
        ResultCode err = null;
        if (ar != null) {
            CatCmdMessage cmdMsg = (CatCmdMessage) ar.userObj;
            if (cmdMsg.getCmdType() == AppInterface.CommandType.SEND_SS) {
                err = ResultCode.SS_RETURN_ERROR;
            } else if (cmdMsg.getCmdType() == AppInterface.CommandType.SEND_USSD) {
                err = ResultCode.USSD_RETURN_ERROR;
            }
            if (ar.exception == null) {
                MmiCode mmiCode = (MmiCode) ar.result;
                CatLog.d(this, "mmiCode = " + mmiCode);
                if (MmiCode.State.FAILED == mmiCode.getState()) {
                    sendTerminalResponse(cmdMsg.mCmdDet, err, false, 0, null);
                    return;
                } else {
                    sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, null);
                    return;
                }
            }
            sendTerminalResponse(cmdMsg.mCmdDet, err, false, 0, null);
            CatLog.d(this, "MSG_ID_EVENT_MMI_COMPLETE error!!");
        }
    }

    private void handleDTMFString(CommandParams cmdPara, int offset) {
        SendDTMFParams dtmf = (SendDTMFParams) cmdPara;
        CatLog.d(this, "handleDTMFString dtmfString: " + dtmf.mDTMFString + " offset: " + offset);
        if (dtmf.mDTMFString == null) {
            CatLog.d(this, "dtmfString is null");
            return;
        }
        if (offset >= dtmf.mDTMFString.length()) {
            sendTerminalResponse(cmdPara.mCmdDet, ResultCode.OK, false, 0, null);
            CatLog.d(this, "DTMF process completed");
            return;
        }
        int offset2 = offset + 1;
        char c = dtmf.mDTMFString.charAt(offset);
        if (PhoneNumberUtils.is12Key(c)) {
            this.mCmdIf.sendDtmf(c, obtainMessage(42, offset2, -1, cmdPara));
        } else if (c == ',') {
            sendMessageDelayed(obtainMessage(MSG_ID_EVENT_PAUSE_DONE, offset2, -1, cmdPara), 3000L);
        }
    }
}
