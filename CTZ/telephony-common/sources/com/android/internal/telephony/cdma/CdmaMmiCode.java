package com.android.internal.telephony.cdma;

import android.R;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.UiccCardApplication;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CdmaMmiCode extends Handler implements MmiCode {
    static final String ACTION_REGISTER = "**";
    static final int EVENT_SET_COMPLETE = 1;
    static final String LOG_TAG = "CdmaMmiCode";
    static final int MATCH_GROUP_ACTION = 2;
    static final int MATCH_GROUP_DIALING_NUMBER = 12;
    static final int MATCH_GROUP_POUND_STRING = 1;
    static final int MATCH_GROUP_PWD_CONFIRM = 11;
    static final int MATCH_GROUP_SERVICE_CODE = 3;
    static final int MATCH_GROUP_SIA = 5;
    static final int MATCH_GROUP_SIB = 7;
    static final int MATCH_GROUP_SIC = 9;
    static final String SC_PIN = "04";
    static final String SC_PIN2 = "042";
    static final String SC_PUK = "05";
    static final String SC_PUK2 = "052";
    static Pattern sPatternSuppService = Pattern.compile("((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
    String mAction;
    Context mContext;
    String mDialingNumber;
    CharSequence mMessage;
    GsmCdmaPhone mPhone;
    String mPoundString;
    String mPwd;
    String mSc;
    String mSia;
    String mSib;
    String mSic;
    MmiCode.State mState;
    UiccCardApplication mUiccApplication;

    public static CdmaMmiCode newFromDialString(String str, GsmCdmaPhone gsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        Matcher matcher = sPatternSuppService.matcher(str);
        if (matcher.matches()) {
            CdmaMmiCode cdmaMmiCode = new CdmaMmiCode(gsmCdmaPhone, uiccCardApplication);
            cdmaMmiCode.mPoundString = makeEmptyNull(matcher.group(1));
            cdmaMmiCode.mAction = makeEmptyNull(matcher.group(2));
            cdmaMmiCode.mSc = makeEmptyNull(matcher.group(3));
            cdmaMmiCode.mSia = makeEmptyNull(matcher.group(5));
            cdmaMmiCode.mSib = makeEmptyNull(matcher.group(7));
            cdmaMmiCode.mSic = makeEmptyNull(matcher.group(9));
            cdmaMmiCode.mPwd = makeEmptyNull(matcher.group(11));
            cdmaMmiCode.mDialingNumber = makeEmptyNull(matcher.group(12));
            return cdmaMmiCode;
        }
        return null;
    }

    private static String makeEmptyNull(String str) {
        if (str == null || str.length() != 0) {
            return str;
        }
        return null;
    }

    CdmaMmiCode(GsmCdmaPhone gsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        super(gsmCdmaPhone.getHandler().getLooper());
        this.mState = MmiCode.State.PENDING;
        this.mPhone = gsmCdmaPhone;
        this.mContext = gsmCdmaPhone.getContext();
        this.mUiccApplication = uiccCardApplication;
    }

    @Override
    public MmiCode.State getState() {
        return this.mState;
    }

    @Override
    public CharSequence getMessage() {
        return this.mMessage;
    }

    @Override
    public Phone getPhone() {
        return this.mPhone;
    }

    @Override
    public void cancel() {
        if (this.mState == MmiCode.State.COMPLETE || this.mState == MmiCode.State.FAILED) {
            return;
        }
        this.mState = MmiCode.State.CANCELLED;
        this.mPhone.onMMIDone(this);
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    public boolean isPinPukCommand() {
        return this.mSc != null && (this.mSc.equals(SC_PIN) || this.mSc.equals(SC_PIN2) || this.mSc.equals(SC_PUK) || this.mSc.equals(SC_PUK2));
    }

    boolean isRegister() {
        return this.mAction != null && this.mAction.equals(ACTION_REGISTER);
    }

    @Override
    public boolean isUssdRequest() {
        Rlog.w(LOG_TAG, "isUssdRequest is not implemented in CdmaMmiCode");
        return false;
    }

    @Override
    public String getDialString() {
        return null;
    }

    @Override
    public void processCode() {
        try {
            if (isPinPukCommand()) {
                String str = this.mSia;
                String str2 = this.mSib;
                int length = str2.length();
                if (isRegister()) {
                    if (!str2.equals(this.mSic)) {
                        handlePasswordError(R.string.expand_action_accessibility);
                        return;
                    }
                    if (length >= 4 && length <= 8) {
                        if (this.mSc.equals(SC_PIN) && this.mUiccApplication != null && this.mUiccApplication.getState() == IccCardApplicationStatus.AppState.APPSTATE_PUK) {
                            handlePasswordError(R.string.ext_media_new_notification_message);
                            return;
                        }
                        if (this.mUiccApplication != null) {
                            Rlog.d(LOG_TAG, "process mmi service code using UiccApp sc=" + this.mSc);
                            if (this.mSc.equals(SC_PIN)) {
                                this.mUiccApplication.changeIccLockPassword(str, str2, obtainMessage(1, this));
                                return;
                            }
                            if (this.mSc.equals(SC_PIN2)) {
                                this.mUiccApplication.changeIccFdnPassword(str, str2, obtainMessage(1, this));
                                return;
                            }
                            if (this.mSc.equals(SC_PUK)) {
                                this.mUiccApplication.supplyPuk(str, str2, obtainMessage(1, this));
                                return;
                            } else {
                                if (this.mSc.equals(SC_PUK2)) {
                                    this.mUiccApplication.supplyPuk2(str, str2, obtainMessage(1, this));
                                    return;
                                }
                                throw new RuntimeException("Unsupported service code=" + this.mSc);
                            }
                        }
                        throw new RuntimeException("No application mUiccApplicaiton is null");
                    }
                    handlePasswordError(R.string.config_oemCredentialManagerDialogComponent);
                    return;
                }
                throw new RuntimeException("Ivalid register/action=" + this.mAction);
            }
        } catch (RuntimeException e) {
            this.mState = MmiCode.State.FAILED;
            this.mMessage = this.mContext.getText(R.string.ext_media_missing_message);
            this.mPhone.onMMIDone(this);
        }
    }

    private void handlePasswordError(int i) {
        this.mState = MmiCode.State.FAILED;
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        sb.append(this.mContext.getText(i));
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == 1) {
            onSetComplete(message, (AsyncResult) message.obj);
        } else {
            Rlog.e(LOG_TAG, "Unexpected reply");
        }
    }

    private CharSequence getScString() {
        if (this.mSc != null && isPinPukCommand()) {
            return this.mContext.getText(R.string.config_systemTelevisionNotificationHandler);
        }
        return "";
    }

    private void onSetComplete(Message message, AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (asyncResult.exception instanceof CommandException) {
                CommandException.Error commandError = ((CommandException) asyncResult.exception).getCommandError();
                if (commandError == CommandException.Error.PASSWORD_INCORRECT) {
                    if (isPinPukCommand()) {
                        if (this.mSc.equals(SC_PUK) || this.mSc.equals(SC_PUK2)) {
                            sb.append(this.mContext.getText(R.string.accept));
                        } else {
                            sb.append(this.mContext.getText(R.string.ThreeWCMmi));
                        }
                        int i = message.arg1;
                        if (i <= 0) {
                            Rlog.d(LOG_TAG, "onSetComplete: PUK locked, cancel as lock screen will handle this");
                            this.mState = MmiCode.State.CANCELLED;
                        } else if (i > 0) {
                            Rlog.d(LOG_TAG, "onSetComplete: attemptsRemaining=" + i);
                            sb.append(this.mContext.getResources().getQuantityString(R.plurals.matches_found, i, Integer.valueOf(i)));
                        }
                    } else {
                        sb.append(this.mContext.getText(R.string.factorytest_not_system));
                    }
                } else if (commandError == CommandException.Error.SIM_PUK2) {
                    sb.append(this.mContext.getText(R.string.ThreeWCMmi));
                    sb.append("\n");
                    sb.append(this.mContext.getText(R.string.ext_media_new_notification_title));
                } else if (commandError == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                    if (this.mSc.equals(SC_PIN)) {
                        sb.append(this.mContext.getText(R.string.capability_desc_canRetrieveWindowContent));
                    }
                } else {
                    sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                }
            } else {
                sb.append(this.mContext.getText(R.string.ext_media_missing_message));
            }
        } else if (isRegister()) {
            this.mState = MmiCode.State.COMPLETE;
            sb.append(this.mContext.getText(R.string.lockscreen_transport_play_description));
        } else {
            this.mState = MmiCode.State.FAILED;
            sb.append(this.mContext.getText(R.string.ext_media_missing_message));
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    @Override
    public ResultReceiver getUssdCallbackReceiver() {
        return null;
    }
}
