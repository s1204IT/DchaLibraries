package com.android.internal.telephony.cat;

import android.R;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.LocaleList;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.Duration;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

public class CatService extends Handler implements AppInterface {
    private static final boolean DBG = false;
    protected static final int DEV_ID_DISPLAY = 2;
    protected static final int DEV_ID_KEYPAD = 1;
    protected static final int DEV_ID_NETWORK = 131;
    protected static final int DEV_ID_TERMINAL = 130;
    protected static final int DEV_ID_UICC = 129;
    protected static final int MSG_ID_ALPHA_NOTIFY = 9;
    public static final int MSG_ID_CALL_SETUP = 4;
    public static final int MSG_ID_EVENT_NOTIFY = 3;
    protected static final int MSG_ID_ICC_CHANGED = 8;
    public static final int MSG_ID_ICC_RECORDS_LOADED = 20;
    public static final int MSG_ID_ICC_REFRESH = 30;
    public static final int MSG_ID_PROACTIVE_COMMAND = 2;
    public static final int MSG_ID_REFRESH = 5;
    public static final int MSG_ID_RESPONSE = 6;
    public static final int MSG_ID_RIL_MSG_DECODED = 10;
    public static final int MSG_ID_SESSION_END = 1;
    public static final int MSG_ID_SIM_READY = 7;
    public static final String STK_DEFAULT = "Default Message";
    protected static IccRecords mIccRecords;
    protected static UiccCardApplication mUiccApplication;
    protected CommandsInterface mCmdIf;
    protected Context mContext;
    protected HandlerThread mHandlerThread;
    protected RilMessageDecoder mMsgDecoder;
    protected int mSlotId;
    protected boolean mStkAppInstalled;
    protected UiccController mUiccController;
    protected static final Object sInstanceLock = new Object();
    protected static CatService[] sInstance = null;
    protected CatCmdMessage mCurrntCmd = null;
    protected CatCmdMessage mMenuCmd = null;
    protected IccCardStatus.CardState mCardState = IccCardStatus.CardState.CARDSTATE_ABSENT;

    public CatService(CommandsInterface commandsInterface, UiccCardApplication uiccCardApplication, IccRecords iccRecords, Context context, IccFileHandler iccFileHandler, UiccProfile uiccProfile, int i) {
        this.mMsgDecoder = null;
        this.mStkAppInstalled = false;
        if (commandsInterface == null || uiccCardApplication == null || iccRecords == null || context == null || iccFileHandler == null || uiccProfile == null) {
            throw new NullPointerException("Service: Input parameters must not be null");
        }
        this.mCmdIf = commandsInterface;
        this.mContext = context;
        this.mSlotId = i;
        this.mHandlerThread = new HandlerThread("Cat Telephony service" + i);
        this.mHandlerThread.start();
        this.mMsgDecoder = RilMessageDecoder.getInstance(this, iccFileHandler, i);
        if (this.mMsgDecoder == null) {
            CatLog.d(this, "Null RilMessageDecoder instance");
            return;
        }
        this.mMsgDecoder.start();
        this.mCmdIf.setOnCatSessionEnd(this, 1, null);
        this.mCmdIf.setOnCatProactiveCmd(this, 2, null);
        this.mCmdIf.setOnCatEvent(this, 3, null);
        this.mCmdIf.setOnCatCallSetUp(this, 4, null);
        this.mCmdIf.registerForIccRefresh(this, 30, null);
        this.mCmdIf.setOnCatCcAlphaNotify(this, 9, null);
        mIccRecords = iccRecords;
        mUiccApplication = uiccCardApplication;
        mIccRecords.registerForRecordsLoaded(this, 20, null);
        CatLog.d(this, "registerForRecordsLoaded slotid=" + this.mSlotId + " instance:" + this);
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 8, null);
        this.mStkAppInstalled = isStkAppInstalled();
        CatLog.d(this, "Running CAT service on Slotid: " + this.mSlotId + ". STK app installed:" + this.mStkAppInstalled);
    }

    public static CatService getInstance(CommandsInterface commandsInterface, Context context, UiccProfile uiccProfile, int i) {
        UiccCardApplication applicationIndex;
        IccRecords iccRecords;
        ?? iccFileHandler;
        if (uiccProfile != null) {
            applicationIndex = uiccProfile.getApplicationIndex(0);
            if (applicationIndex != null) {
                iccFileHandler = applicationIndex.getIccFileHandler();
                iccRecords = applicationIndex.getIccRecords();
                synchronized (sInstanceLock) {
                    if (sInstance == null) {
                        int simCount = TelephonyManager.getDefault().getSimCount();
                        sInstance = new CatService[simCount];
                        for (int i2 = 0; i2 < simCount; i2++) {
                            sInstance[i2] = null;
                        }
                    }
                    if (sInstance[i] == null) {
                        if (commandsInterface != null && applicationIndex != null && iccRecords != null && context != null && iccFileHandler != 0 && uiccProfile != null) {
                            sInstance[i] = TelephonyComponentFactory.getInstance().makeCatService(commandsInterface, applicationIndex, iccRecords, context, iccFileHandler, uiccProfile, i);
                        }
                        return null;
                    }
                    if (iccRecords != null && mIccRecords != iccRecords) {
                        if (mIccRecords != null) {
                            mIccRecords.unregisterForRecordsLoaded(sInstance[i]);
                        }
                        mIccRecords = iccRecords;
                        mUiccApplication = applicationIndex;
                        mIccRecords.registerForRecordsLoaded(sInstance[i], 20, null);
                        CatLog.d(sInstance[i], "registerForRecordsLoaded slotid=" + i + " instance:" + sInstance[i]);
                    }
                    return sInstance[i];
                }
            }
            iccRecords = null;
        } else {
            applicationIndex = null;
            iccRecords = null;
        }
        iccFileHandler = iccRecords;
        synchronized (sInstanceLock) {
        }
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
            if (this.mUiccController != null) {
                this.mUiccController.unregisterForIccChanged(this);
                this.mUiccController = null;
            }
            this.mMsgDecoder.dispose();
            this.mMsgDecoder = null;
            this.mHandlerThread.quit();
            this.mHandlerThread = null;
            removeCallbacksAndMessages(null);
            if (sInstance != null) {
                if (SubscriptionManager.isValidSlotIndex(this.mSlotId)) {
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

    protected void handleRilMsg(RilMessage rilMessage) {
        CommandParams commandParams;
        if (rilMessage == null) {
        }
        int i = rilMessage.mId;
        if (i == 5) {
            CommandParams commandParams2 = (CommandParams) rilMessage.mData;
            if (commandParams2 != null) {
                handleCommand(commandParams2, false);
                return;
            }
            return;
        }
        switch (i) {
            case 1:
                handleSessionEnd();
                break;
            case 2:
                try {
                    CommandParams commandParams3 = (CommandParams) rilMessage.mData;
                    if (commandParams3 != null) {
                        if (rilMessage.mResCode == ResultCode.OK) {
                            handleCommand(commandParams3, true);
                        } else {
                            sendTerminalResponse(commandParams3.mCmdDet, rilMessage.mResCode, false, 0, null);
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
                if (rilMessage.mResCode == ResultCode.OK && (commandParams = (CommandParams) rilMessage.mData) != null) {
                    handleCommand(commandParams, false);
                    break;
                }
                break;
        }
    }

    protected boolean isSupportedSetupEventCommand(CatCmdMessage catCmdMessage) {
        boolean z = true;
        for (int i : catCmdMessage.getSetEventList().eventList) {
            CatLog.d(this, "Event: " + i);
            if (i != 7) {
                switch (i) {
                    case 4:
                    case 5:
                        break;
                    default:
                        z = false;
                        break;
                }
            }
        }
        return z;
    }

    protected void handleCommand(CommandParams commandParams, boolean z) {
        ResultCode resultCode;
        boolean z2;
        CatLog.d(this, commandParams.getCommandType().name());
        if (z && this.mUiccController != null) {
            this.mUiccController.addCardLog("ProactiveCommand mSlotId=" + this.mSlotId + " cmdParams=" + commandParams);
        }
        CatCmdMessage catCmdMessage = new CatCmdMessage(commandParams);
        switch (commandParams.getCommandType()) {
            case SET_UP_MENU:
                if (removeMenu(catCmdMessage.getMenu())) {
                    this.mMenuCmd = null;
                } else {
                    this.mMenuCmd = catCmdMessage;
                }
                sendTerminalResponse(commandParams.mCmdDet, commandParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED : ResultCode.OK, false, 0, null);
                break;
            case DISPLAY_TEXT:
            case SELECT_ITEM:
            case GET_INPUT:
            case GET_INKEY:
            case PLAY_TONE:
                break;
            case REFRESH:
                commandParams.mCmdDet.typeOfCommand = AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.value();
                break;
            case SET_UP_IDLE_MODE_TEXT:
                sendTerminalResponse(commandParams.mCmdDet, commandParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED : ResultCode.OK, false, 0, null);
                break;
            case SET_UP_EVENT_LIST:
                if (isSupportedSetupEventCommand(catCmdMessage)) {
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, null);
                } else {
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
                }
                break;
            case PROVIDE_LOCAL_INFORMATION:
                switch (commandParams.mCmdDet.commandQualifier) {
                    case 3:
                        sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, new DTTZResponseData(null));
                        break;
                    case 4:
                        sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, new LanguageResponseData(Locale.getDefault().getLanguage()));
                        break;
                    default:
                        sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, null);
                        break;
                }
                return;
            case LAUNCH_BROWSER:
                LaunchBrowserParams launchBrowserParams = (LaunchBrowserParams) commandParams;
                if (launchBrowserParams.mConfirmMsg.text != null && launchBrowserParams.mConfirmMsg.text.equals(STK_DEFAULT)) {
                    launchBrowserParams.mConfirmMsg.text = this.mContext.getText(R.string.config_wimaxStateTrackerClassname).toString();
                }
                break;
            case SEND_DTMF:
            case SEND_SMS:
            case SEND_SS:
            case SEND_USSD:
                DisplayTextParams displayTextParams = (DisplayTextParams) commandParams;
                if (displayTextParams.mTextMsg.text != null && displayTextParams.mTextMsg.text.equals(STK_DEFAULT)) {
                    displayTextParams.mTextMsg.text = this.mContext.getText(R.string.lockscreen_screen_locked).toString();
                }
                break;
            case SET_UP_CALL:
                CallSetupParams callSetupParams = (CallSetupParams) commandParams;
                if (callSetupParams.mConfirmMsg.text != null && callSetupParams.mConfirmMsg.text.equals(STK_DEFAULT)) {
                    callSetupParams.mConfirmMsg.text = this.mContext.getText(R.string.config_systemUi).toString();
                }
                break;
            case LANGUAGE_NOTIFICATION:
                String str = ((LanguageParams) commandParams).mLanguage;
                ResultCode resultCode2 = ResultCode.OK;
                if (str == null || str.length() <= 0) {
                    resultCode = resultCode2;
                } else {
                    try {
                        changeLanguage(str);
                        resultCode = resultCode2;
                    } catch (RemoteException e) {
                        resultCode = ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS;
                    }
                }
                sendTerminalResponse(commandParams.mCmdDet, resultCode, false, 0, null);
                return;
            case OPEN_CHANNEL:
            case CLOSE_CHANNEL:
            case RECEIVE_DATA:
            case SEND_DATA:
                BIPClientParams bIPClientParams = (BIPClientParams) commandParams;
                try {
                    z2 = this.mContext.getResources().getBoolean(R.^attr-private.outKeycode);
                } catch (Resources.NotFoundException e2) {
                    z2 = false;
                }
                if (bIPClientParams.mTextMsg.text == null && (bIPClientParams.mHasAlphaId || z2)) {
                    CatLog.d(this, "cmd " + commandParams.getCommandType() + " with null alpha id");
                    if (z) {
                        sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, null);
                        return;
                    } else {
                        if (commandParams.getCommandType() == AppInterface.CommandType.OPEN_CHANNEL) {
                            this.mCmdIf.handleCallSetupRequestFromSim(true, null);
                            return;
                        }
                        return;
                    }
                }
                if (!this.mStkAppInstalled) {
                    CatLog.d(this, "No STK application found.");
                    if (z) {
                        sendTerminalResponse(commandParams.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
                        return;
                    }
                }
                if (z && (commandParams.getCommandType() == AppInterface.CommandType.CLOSE_CHANNEL || commandParams.getCommandType() == AppInterface.CommandType.RECEIVE_DATA || commandParams.getCommandType() == AppInterface.CommandType.SEND_DATA)) {
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, null);
                }
                break;
                break;
            default:
                CatLog.d(this, "Unsupported command");
                return;
        }
        this.mCurrntCmd = catCmdMessage;
        broadcastCatCmdIntent(catCmdMessage);
    }

    protected void broadcastCatCmdIntent(CatCmdMessage catCmdMessage) {
        Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
        intent.putExtra("STK CMD", catCmdMessage);
        intent.putExtra("SLOT_ID", this.mSlotId);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        CatLog.d(this, "Sending CmdMsg: " + catCmdMessage + " on slotid:" + this.mSlotId);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    protected void handleSessionEnd() {
        CatLog.d(this, "SESSION END on " + this.mSlotId);
        this.mCurrntCmd = this.mMenuCmd;
        Intent intent = new Intent(AppInterface.CAT_SESSION_END_ACTION);
        intent.putExtra("SLOT_ID", this.mSlotId);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    protected void sendTerminalResponse(CommandDetails commandDetails, ResultCode resultCode, boolean z, int i, ResponseData responseData) {
        Input inputGeInput;
        if (commandDetails == null) {
            return;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (this.mCurrntCmd != null) {
            inputGeInput = this.mCurrntCmd.geInput();
        } else {
            inputGeInput = null;
        }
        int iValue = ComprehensionTlvTag.COMMAND_DETAILS.value();
        if (commandDetails.compRequired) {
            iValue |= 128;
        }
        byteArrayOutputStream.write(iValue);
        byteArrayOutputStream.write(3);
        byteArrayOutputStream.write(commandDetails.commandNumber);
        byteArrayOutputStream.write(commandDetails.typeOfCommand);
        byteArrayOutputStream.write(commandDetails.commandQualifier);
        byteArrayOutputStream.write(ComprehensionTlvTag.DEVICE_IDENTITIES.value());
        byteArrayOutputStream.write(2);
        byteArrayOutputStream.write(130);
        byteArrayOutputStream.write(129);
        int iValue2 = ComprehensionTlvTag.RESULT.value();
        if (commandDetails.compRequired) {
            iValue2 |= 128;
        }
        byteArrayOutputStream.write(iValue2);
        byteArrayOutputStream.write(z ? 2 : 1);
        byteArrayOutputStream.write(resultCode.value());
        if (z) {
            byteArrayOutputStream.write(i);
        }
        if (responseData != null) {
            responseData.format(byteArrayOutputStream);
        } else {
            encodeOptionalTags(commandDetails, resultCode, inputGeInput, byteArrayOutputStream);
        }
        this.mCmdIf.sendTerminalResponse(IccUtils.bytesToHexString(byteArrayOutputStream.toByteArray()), null);
        onSetResponsedFlag();
    }

    protected void encodeOptionalTags(CommandDetails commandDetails, ResultCode resultCode, Input input, ByteArrayOutputStream byteArrayOutputStream) {
        AppInterface.CommandType commandTypeFromInt = AppInterface.CommandType.fromInt(commandDetails.typeOfCommand);
        if (commandTypeFromInt != null) {
            int i = AnonymousClass1.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[commandTypeFromInt.ordinal()];
            if (i != 6) {
                switch (i) {
                    case 9:
                    case 10:
                        if (resultCode.value() == ResultCode.NO_RESPONSE_FROM_USER.value() && input != null && input.duration != null) {
                            getInKeyResponse(byteArrayOutputStream, input);
                            break;
                        }
                        break;
                    default:
                        CatLog.d(this, "encodeOptionalTags() Unsupported Cmd details=" + commandDetails);
                        break;
                }
            }
            if (commandDetails.commandQualifier == 4 && resultCode.value() == ResultCode.OK.value()) {
                getPliResponse(byteArrayOutputStream);
                return;
            }
            return;
        }
        CatLog.d(this, "encodeOptionalTags() bad Cmd details=" + commandDetails);
    }

    protected void getInKeyResponse(ByteArrayOutputStream byteArrayOutputStream, Input input) {
        byteArrayOutputStream.write(ComprehensionTlvTag.DURATION.value());
        byteArrayOutputStream.write(2);
        Duration.TimeUnit timeUnit = input.duration.timeUnit;
        byteArrayOutputStream.write(Duration.TimeUnit.SECOND.value());
        byteArrayOutputStream.write(input.duration.timeInterval);
    }

    protected void getPliResponse(ByteArrayOutputStream byteArrayOutputStream) {
        String language = Locale.getDefault().getLanguage();
        if (language != null) {
            byteArrayOutputStream.write(ComprehensionTlvTag.LANGUAGE.value());
            ResponseData.writeLength(byteArrayOutputStream, language.length());
            byteArrayOutputStream.write(language.getBytes(), 0, language.length());
        }
    }

    protected void sendMenuSelection(int i, boolean z) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(211);
        byteArrayOutputStream.write(0);
        byteArrayOutputStream.write(ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128);
        byteArrayOutputStream.write(2);
        byteArrayOutputStream.write(1);
        byteArrayOutputStream.write(129);
        byteArrayOutputStream.write(128 | ComprehensionTlvTag.ITEM_ID.value());
        byteArrayOutputStream.write(1);
        byteArrayOutputStream.write(i);
        if (z) {
            byteArrayOutputStream.write(ComprehensionTlvTag.HELP_REQUEST.value());
            byteArrayOutputStream.write(0);
        }
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        byteArray[1] = (byte) (byteArray.length - 2);
        this.mCmdIf.sendEnvelope(IccUtils.bytesToHexString(byteArray), null);
    }

    protected void eventDownload(int i, int i2, int i3, byte[] bArr, boolean z) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(BerTlv.BER_EVENT_DOWNLOAD_TAG);
        byteArrayOutputStream.write(0);
        byteArrayOutputStream.write(ComprehensionTlvTag.EVENT_LIST.value() | 128);
        byteArrayOutputStream.write(1);
        byteArrayOutputStream.write(i);
        byteArrayOutputStream.write(ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128);
        byteArrayOutputStream.write(2);
        byteArrayOutputStream.write(i2);
        byteArrayOutputStream.write(i3);
        if (i != 7) {
            switch (i) {
                case 5:
                    CatLog.d(sInstance, " Sending Idle Screen Available event download to ICC");
                    break;
            }
        } else {
            CatLog.d(sInstance, " Sending Language Selection event download to ICC");
            byteArrayOutputStream.write(ComprehensionTlvTag.LANGUAGE.value() | 128);
            byteArrayOutputStream.write(2);
        }
        if (bArr != null) {
            for (byte b : bArr) {
                byteArrayOutputStream.write(b);
            }
        }
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        byteArray[1] = (byte) (byteArray.length - 2);
        String strBytesToHexString = IccUtils.bytesToHexString(byteArray);
        CatLog.d(this, "ENVELOPE COMMAND: " + strBytesToHexString);
        this.mCmdIf.sendEnvelope(strBytesToHexString, null);
    }

    public static AppInterface getInstance() {
        int slotIndex;
        SubscriptionController subscriptionController = SubscriptionController.getInstance();
        if (subscriptionController != null) {
            slotIndex = subscriptionController.getSlotIndex(subscriptionController.getDefaultSubId());
        } else {
            slotIndex = 0;
        }
        return getInstance(null, null, null, slotIndex);
    }

    public static AppInterface getInstance(int i) {
        return getInstance(null, null, null, i);
    }

    @Override
    public void handleMessage(Message message) {
        AsyncResult asyncResult;
        CatLog.d(this, "handleMessage[" + message.what + "]");
        int i = message.what;
        if (i != 20) {
            if (i != 30) {
                String str = null;
                switch (i) {
                    case 1:
                    case 2:
                    case 3:
                    case 5:
                        CatLog.d(this, "ril message arrived,slotid:" + this.mSlotId);
                        if (message.obj != null && (asyncResult = (AsyncResult) message.obj) != null && asyncResult.result != null) {
                            try {
                                str = (String) asyncResult.result;
                            } catch (ClassCastException e) {
                                return;
                            }
                            break;
                        }
                        this.mMsgDecoder.sendStartDecodingMessageParams(new RilMessage(message.what, str));
                        return;
                    case 4:
                        this.mMsgDecoder.sendStartDecodingMessageParams(new RilMessage(message.what, null));
                        return;
                    case 6:
                        handleCmdResponse((CatResponseMessage) message.obj);
                        return;
                    default:
                        switch (i) {
                            case 8:
                                CatLog.d(this, "MSG_ID_ICC_CHANGED");
                                updateIccAvailability();
                                return;
                            case 9:
                                CatLog.d(this, "Received CAT CC Alpha message from card");
                                if (message.obj != null) {
                                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                                    if (asyncResult2 != null && asyncResult2.result != null) {
                                        broadcastAlphaMessage((String) asyncResult2.result);
                                        return;
                                    } else {
                                        CatLog.d(this, "CAT Alpha message: ar.result is null");
                                        return;
                                    }
                                }
                                CatLog.d(this, "CAT Alpha message: msg.obj is null");
                                return;
                            case 10:
                                handleRilMsg((RilMessage) message.obj);
                                return;
                            default:
                                throw new AssertionError("Unrecognized CAT command: " + message.what);
                        }
                }
            }
            if (message.obj != null) {
                AsyncResult asyncResult3 = (AsyncResult) message.obj;
                if (asyncResult3 != null && asyncResult3.result != null) {
                    broadcastCardStateAndIccRefreshResp(IccCardStatus.CardState.CARDSTATE_PRESENT, (IccRefreshResponse) asyncResult3.result);
                    return;
                }
                CatLog.d(this, "Icc REFRESH with exception: " + asyncResult3.exception);
                return;
            }
            CatLog.d(this, "IccRefresh Message is null");
        }
    }

    protected void broadcastCardStateAndIccRefreshResp(IccCardStatus.CardState cardState, IccRefreshResponse iccRefreshResponse) {
        Intent intent = new Intent(AppInterface.CAT_ICC_STATUS_CHANGE);
        boolean z = cardState == IccCardStatus.CardState.CARDSTATE_PRESENT;
        if (iccRefreshResponse != null) {
            intent.putExtra(AppInterface.REFRESH_RESULT, iccRefreshResponse.refreshResult);
            CatLog.d(this, "Sending IccResult with Result: " + iccRefreshResponse.refreshResult);
        }
        intent.putExtra(AppInterface.CARD_STATUS, z);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        intent.putExtra("SLOT_ID", this.mSlotId);
        CatLog.d(this, "Sending Card Status: " + cardState + " cardPresent: " + z + "SLOT_ID: " + this.mSlotId);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    protected void broadcastAlphaMessage(String str) {
        CatLog.d(this, "Broadcasting CAT Alpha message from card: " + str);
        Intent intent = new Intent(AppInterface.CAT_ALPHA_NOTIFY_ACTION);
        intent.addFlags(268435456);
        intent.putExtra(AppInterface.ALPHA_STRING, str);
        intent.putExtra("SLOT_ID", this.mSlotId);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    @Override
    public synchronized void onCmdResponse(CatResponseMessage catResponseMessage) {
        if (catResponseMessage == null) {
            return;
        }
        obtainMessage(6, catResponseMessage).sendToTarget();
    }

    protected boolean validateResponse(CatResponseMessage catResponseMessage) {
        if (catResponseMessage.mCmdDet.typeOfCommand == AppInterface.CommandType.SET_UP_EVENT_LIST.value() || catResponseMessage.mCmdDet.typeOfCommand == AppInterface.CommandType.SET_UP_MENU.value()) {
            CatLog.d(this, "CmdType: " + catResponseMessage.mCmdDet.typeOfCommand);
            return true;
        }
        if (this.mCurrntCmd != null) {
            boolean zCompareTo = catResponseMessage.mCmdDet.compareTo(this.mCurrntCmd.mCmdDet);
            CatLog.d(this, "isResponse for last valid cmd: " + zCompareTo);
            return zCompareTo;
        }
        return false;
    }

    protected boolean removeMenu(Menu menu) {
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

    protected void handleCmdResponse(CatResponseMessage catResponseMessage) {
        boolean z;
        ResponseData getInkeyInputResponseData;
        int i;
        ResponseData selectItemResponseData;
        if (validateResponse(catResponseMessage)) {
            CommandDetails cmdDetails = catResponseMessage.getCmdDetails();
            AppInterface.CommandType commandTypeFromInt = AppInterface.CommandType.fromInt(cmdDetails.typeOfCommand);
            switch (catResponseMessage.mResCode) {
                case HELP_INFO_REQUIRED:
                    z = true;
                    i = AnonymousClass1.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[commandTypeFromInt.ordinal()];
                    if (i == 5) {
                        if (i != 16 && i != 18) {
                            switch (i) {
                                case 1:
                                    sendMenuSelection(catResponseMessage.mUsersMenuSelection, catResponseMessage.mResCode == ResultCode.HELP_INFO_REQUIRED);
                                    break;
                                case 2:
                                    if (catResponseMessage.mResCode == ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS) {
                                        catResponseMessage.setAdditionalInfo(1);
                                    } else {
                                        catResponseMessage.mIncludeAdditionalInfo = false;
                                        catResponseMessage.mAdditionalInfo = 0;
                                    }
                                    sendTerminalResponse(cmdDetails, catResponseMessage.mResCode, catResponseMessage.mIncludeAdditionalInfo, catResponseMessage.mAdditionalInfo, getInkeyInputResponseData);
                                    this.mCurrntCmd = null;
                                    break;
                                default:
                                    switch (i) {
                                        case 7:
                                            if (catResponseMessage.mResCode == ResultCode.LAUNCH_BROWSER_ERROR) {
                                                catResponseMessage.setAdditionalInfo(4);
                                            } else {
                                                catResponseMessage.mIncludeAdditionalInfo = false;
                                                catResponseMessage.mAdditionalInfo = 0;
                                            }
                                            break;
                                        case 8:
                                            selectItemResponseData = new SelectItemResponseData(catResponseMessage.mUsersMenuSelection);
                                            getInkeyInputResponseData = selectItemResponseData;
                                            break;
                                        case 9:
                                        case 10:
                                            Input inputGeInput = this.mCurrntCmd.geInput();
                                            if (!inputGeInput.yesNo) {
                                                getInkeyInputResponseData = !z ? new GetInkeyInputResponseData(catResponseMessage.mUsersInput, inputGeInput.ucs2, inputGeInput.packed) : null;
                                            } else {
                                                selectItemResponseData = new GetInkeyInputResponseData(catResponseMessage.mUsersYesNoSelection);
                                                getInkeyInputResponseData = selectItemResponseData;
                                            }
                                            break;
                                    }
                                    sendTerminalResponse(cmdDetails, catResponseMessage.mResCode, catResponseMessage.mIncludeAdditionalInfo, catResponseMessage.mAdditionalInfo, getInkeyInputResponseData);
                                    this.mCurrntCmd = null;
                                    break;
                            }
                        } else {
                            this.mCmdIf.handleCallSetupRequestFromSim(catResponseMessage.mUsersConfirm, null);
                            this.mCurrntCmd = null;
                            break;
                        }
                    } else {
                        if (5 != catResponseMessage.mEventValue) {
                            eventDownload(catResponseMessage.mEventValue, 130, 129, catResponseMessage.mAddedInfo, false);
                        } else {
                            eventDownload(catResponseMessage.mEventValue, 2, 129, catResponseMessage.mAddedInfo, false);
                        }
                        break;
                    }
                    break;
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
                case LAUNCH_BROWSER_ERROR:
                case TERMINAL_CRNTLY_UNABLE_TO_PROCESS:
                    z = false;
                    i = AnonymousClass1.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[commandTypeFromInt.ordinal()];
                    if (i == 5) {
                    }
                    break;
                case BACKWARD_MOVE_BY_USER:
                case USER_NOT_ACCEPT:
                    if (commandTypeFromInt == AppInterface.CommandType.SET_UP_CALL || commandTypeFromInt == AppInterface.CommandType.OPEN_CHANNEL) {
                        this.mCmdIf.handleCallSetupRequestFromSim(false, null);
                        this.mCurrntCmd = null;
                    }
                    sendTerminalResponse(cmdDetails, catResponseMessage.mResCode, catResponseMessage.mIncludeAdditionalInfo, catResponseMessage.mAdditionalInfo, getInkeyInputResponseData);
                    this.mCurrntCmd = null;
                    break;
                case NO_RESPONSE_FROM_USER:
                    if (commandTypeFromInt == AppInterface.CommandType.SET_UP_CALL) {
                        this.mCmdIf.handleCallSetupRequestFromSim(false, null);
                        this.mCurrntCmd = null;
                    }
                    sendTerminalResponse(cmdDetails, catResponseMessage.mResCode, catResponseMessage.mIncludeAdditionalInfo, catResponseMessage.mAdditionalInfo, getInkeyInputResponseData);
                    this.mCurrntCmd = null;
                    break;
            }
        }
    }

    private boolean isStkAppInstalled() {
        int size;
        List<ResolveInfo> listQueryBroadcastReceivers = this.mContext.getPackageManager().queryBroadcastReceivers(new Intent(AppInterface.CAT_CMD_ACTION), 128);
        if (listQueryBroadcastReceivers != null) {
            size = listQueryBroadcastReceivers.size();
        } else {
            size = 0;
        }
        return size > 0;
    }

    public void update(CommandsInterface commandsInterface, Context context, UiccProfile uiccProfile) {
        UiccCardApplication applicationIndex;
        IccRecords iccRecords;
        if (uiccProfile == null) {
            applicationIndex = null;
            iccRecords = null;
        } else {
            applicationIndex = uiccProfile.getApplicationIndex(0);
            if (applicationIndex != null) {
                iccRecords = applicationIndex.getIccRecords();
            } else {
                iccRecords = null;
            }
        }
        synchronized (sInstanceLock) {
            if (iccRecords != null) {
                try {
                    if (mIccRecords != iccRecords) {
                        if (mIccRecords != null) {
                            mIccRecords.unregisterForRecordsLoaded(this);
                        }
                        CatLog.d(this, "Reinitialize the Service with SIMRecords and UiccCardApplication");
                        mIccRecords = iccRecords;
                        mUiccApplication = applicationIndex;
                        mIccRecords.registerForRecordsLoaded(this, 20, null);
                        CatLog.d(this, "registerForRecordsLoaded slotid=" + this.mSlotId + " instance:" + this);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    protected void updateIccAvailability() {
        if (this.mUiccController == null) {
            return;
        }
        IccCardStatus.CardState cardState = IccCardStatus.CardState.CARDSTATE_ABSENT;
        UiccCard uiccCard = this.mUiccController.getUiccCard(this.mSlotId);
        if (uiccCard != null) {
            cardState = uiccCard.getCardState();
        }
        IccCardStatus.CardState cardState2 = this.mCardState;
        this.mCardState = cardState;
        CatLog.d(this, "New Card State = " + cardState + " Old Card State = " + cardState2);
        if (cardState2 == IccCardStatus.CardState.CARDSTATE_PRESENT && cardState != IccCardStatus.CardState.CARDSTATE_PRESENT) {
            broadcastCardStateAndIccRefreshResp(cardState, null);
        } else if (cardState2 != IccCardStatus.CardState.CARDSTATE_PRESENT && cardState == IccCardStatus.CardState.CARDSTATE_PRESENT) {
            this.mCmdIf.reportStkServiceIsRunning(null);
        }
    }

    private void changeLanguage(String str) throws RemoteException {
        IActivityManager iActivityManager = ActivityManagerNative.getDefault();
        Configuration configuration = iActivityManager.getConfiguration();
        configuration.setLocales(new LocaleList(new Locale(str), LocaleList.getDefault()));
        configuration.userSetLocale = true;
        iActivityManager.updatePersistentConfiguration(configuration);
        BackupManager.dataChanged("com.android.providers.settings");
    }

    protected void onSetResponsedFlag() {
    }
}
