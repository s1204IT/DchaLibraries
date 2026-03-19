package com.android.internal.telephony.uicc;

import android.R;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccSlotStatus;
import com.android.internal.telephony.uicc.euicc.EuiccCard;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class UiccSlot extends Handler {
    private static final boolean DBG = true;
    private static final int EVENT_CARD_ADDED = 14;
    private static final int EVENT_CARD_REMOVED = 13;
    public static final String EXTRA_ICC_CARD_ADDED = "com.android.internal.telephony.uicc.ICC_CARD_ADDED";
    public static final int INVALID_PHONE_ID = -1;
    private static final String TAG = "UiccSlot";
    private boolean mActive;
    private AnswerToReset mAtr;
    private IccCardStatus.CardState mCardState;
    private CommandsInterface mCi;
    private Context mContext;
    private String mIccId;
    private boolean mIsEuicc;
    private UiccCard mUiccCard;
    private final Object mLock = new Object();
    private boolean mStateIsUnknown = true;
    private CommandsInterface.RadioState mLastRadioState = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
    private int mPhoneId = -1;

    public UiccSlot(Context context, boolean z) {
        log("Creating");
        this.mContext = context;
        this.mActive = z;
        this.mCardState = null;
    }

    public void update(CommandsInterface commandsInterface, IccCardStatus iccCardStatus, int i) {
        log("cardStatus update: " + iccCardStatus.toString());
        synchronized (this.mLock) {
            IccCardStatus.CardState cardState = this.mCardState;
            this.mCardState = iccCardStatus.mCardState;
            this.mIccId = iccCardStatus.iccid;
            this.mPhoneId = i;
            parseAtr(iccCardStatus.atr);
            this.mCi = commandsInterface;
            CommandsInterface.RadioState radioState = this.mCi.getRadioState();
            log("update: radioState=" + radioState + " mLastRadioState=" + this.mLastRadioState);
            if (absentStateUpdateNeeded(cardState)) {
                updateCardStateAbsent();
            } else if ((cardState == null || cardState == IccCardStatus.CardState.CARDSTATE_ABSENT || this.mUiccCard == null) && this.mCardState != IccCardStatus.CardState.CARDSTATE_ABSENT) {
                if (radioState == CommandsInterface.RadioState.RADIO_ON && this.mLastRadioState == CommandsInterface.RadioState.RADIO_ON) {
                    log("update: notify card added");
                    sendMessage(obtainMessage(14, null));
                }
                if (this.mUiccCard != null) {
                    loge("update: mUiccCard != null when card was present; disposing it now");
                    this.mUiccCard.dispose();
                }
                if (!this.mIsEuicc) {
                    this.mUiccCard = new UiccCard(this.mContext, this.mCi, iccCardStatus, this.mPhoneId, this.mLock);
                } else {
                    this.mUiccCard = new EuiccCard(this.mContext, this.mCi, iccCardStatus, i, this.mLock);
                }
            } else if (this.mUiccCard != null) {
                this.mUiccCard.update(this.mContext, this.mCi, iccCardStatus);
            }
            this.mLastRadioState = radioState;
        }
    }

    public void update(CommandsInterface commandsInterface, IccSlotStatus iccSlotStatus) {
        log("slotStatus update: " + iccSlotStatus.toString());
        synchronized (this.mLock) {
            IccCardStatus.CardState cardState = this.mCardState;
            this.mCi = commandsInterface;
            parseAtr(iccSlotStatus.atr);
            this.mCardState = iccSlotStatus.cardState;
            this.mIccId = iccSlotStatus.iccid;
            if (iccSlotStatus.slotState == IccSlotStatus.SlotState.SLOTSTATE_INACTIVE) {
                if (this.mActive) {
                    this.mActive = false;
                    this.mLastRadioState = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
                    this.mPhoneId = -1;
                    if (this.mUiccCard != null) {
                        this.mUiccCard.dispose();
                    }
                    nullifyUiccCard(true);
                }
            } else {
                this.mActive = true;
                this.mPhoneId = iccSlotStatus.logicalSlotIndex;
                if (absentStateUpdateNeeded(cardState)) {
                    updateCardStateAbsent();
                }
            }
        }
    }

    private boolean absentStateUpdateNeeded(IccCardStatus.CardState cardState) {
        return !(cardState == IccCardStatus.CardState.CARDSTATE_ABSENT && this.mUiccCard == null) && this.mCardState == IccCardStatus.CardState.CARDSTATE_ABSENT;
    }

    private void updateCardStateAbsent() {
        CommandsInterface.RadioState radioState = this.mCi == null ? CommandsInterface.RadioState.RADIO_UNAVAILABLE : this.mCi.getRadioState();
        if (radioState == CommandsInterface.RadioState.RADIO_ON && this.mLastRadioState == CommandsInterface.RadioState.RADIO_ON) {
            log("update: notify card removed");
            sendMessage(obtainMessage(13, null));
        }
        UiccController.updateInternalIccState("ABSENT", null, this.mPhoneId);
        if (this.mUiccCard != null) {
            this.mUiccCard.dispose();
        }
        nullifyUiccCard(false);
        this.mLastRadioState = radioState;
    }

    private void nullifyUiccCard(boolean z) {
        this.mStateIsUnknown = z;
        this.mUiccCard = null;
    }

    public boolean isStateUnknown() {
        return (this.mCardState == null || this.mCardState == IccCardStatus.CardState.CARDSTATE_ABSENT) && this.mStateIsUnknown;
    }

    private void checkIsEuiccSupported() {
        if (this.mAtr != null && this.mAtr.isEuiccSupported()) {
            this.mIsEuicc = true;
        } else {
            this.mIsEuicc = false;
        }
    }

    private void parseAtr(String str) {
        this.mAtr = AnswerToReset.parseAtr(str);
        if (this.mAtr == null) {
            return;
        }
        checkIsEuiccSupported();
    }

    public boolean isEuicc() {
        return this.mIsEuicc;
    }

    public boolean isActive() {
        return this.mActive;
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public String getIccId() {
        if (this.mIccId != null) {
            return this.mIccId;
        }
        if (this.mUiccCard != null) {
            return this.mUiccCard.getIccId();
        }
        return null;
    }

    public boolean isExtendedApduSupported() {
        return this.mAtr != null && this.mAtr.isExtendedApduSupported();
    }

    protected void finalize() {
        log("UiccSlot finalized");
    }

    private void onIccSwap(boolean z) {
        if (this.mContext.getResources().getBoolean(R.^attr-private.keepDotActivated)) {
            log("onIccSwap: isHotSwapSupported is true, don't prompt for rebooting");
        } else {
            log("onIccSwap: isHotSwapSupported is false, prompt for rebooting");
            promptForRestart(z);
        }
    }

    private void promptForRestart(boolean z) {
        synchronized (this.mLock) {
            String string = this.mContext.getResources().getString(R.string.alert_windows_notification_turn_off_action);
            if (string != null) {
                try {
                    this.mContext.startActivity(new Intent().setComponent(ComponentName.unflattenFromString(string)).addFlags(268435456).putExtra("com.android.internal.telephony.uicc.ICC_CARD_ADDED", z));
                    return;
                } catch (ActivityNotFoundException e) {
                    loge("Unable to find ICC hotswap prompt for restart activity: " + e);
                }
            }
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    synchronized (UiccSlot.this.mLock) {
                        if (i == -1) {
                            try {
                                UiccSlot.this.log("Reboot due to SIM swap");
                                ((PowerManager) UiccSlot.this.mContext.getSystemService("power")).reboot("SIM is added.");
                            } catch (Throwable th) {
                                throw th;
                            }
                        }
                    }
                }
            };
            Resources system = Resources.getSystem();
            AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setTitle(z ? system.getString(R.string.media_route_status_not_available) : system.getString(R.string.mediasize_chinese_om_jurro_ku_kai)).setMessage(z ? system.getString(R.string.media_route_status_in_use) : system.getString(R.string.mediasize_chinese_om_dai_pa_kai)).setPositiveButton(system.getString(R.string.mediasize_chinese_om_pa_kai), onClickListener).create();
            alertDialogCreate.getWindow().setType(2003);
            alertDialogCreate.show();
        }
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 13:
                onIccSwap(false);
                break;
            case 14:
                onIccSwap(true);
                break;
            default:
                loge("Unknown Event " + message.what);
                break;
        }
    }

    public IccCardStatus.CardState getCardState() {
        synchronized (this.mLock) {
            if (this.mCardState == null) {
                return IccCardStatus.CardState.CARDSTATE_ABSENT;
            }
            return this.mCardState;
        }
    }

    public UiccCard getUiccCard() {
        UiccCard uiccCard;
        synchronized (this.mLock) {
            uiccCard = this.mUiccCard;
        }
        return uiccCard;
    }

    public void onRadioStateUnavailable() {
        if (this.mUiccCard != null) {
            this.mUiccCard.dispose();
        }
        nullifyUiccCard(true);
        if (this.mPhoneId != -1) {
            UiccController.updateInternalIccState("UNKNOWN", null, this.mPhoneId);
        }
        this.mCardState = null;
        this.mLastRadioState = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("UiccSlot:");
        printWriter.println(" mCi=" + this.mCi);
        printWriter.println(" mActive=" + this.mActive);
        printWriter.println(" mLastRadioState=" + this.mLastRadioState);
        printWriter.println(" mCardState=" + this.mCardState);
        if (this.mUiccCard != null) {
            printWriter.println(" mUiccCard=" + this.mUiccCard);
            this.mUiccCard.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println(" mUiccCard=null");
        }
        printWriter.println();
        printWriter.flush();
        printWriter.flush();
    }
}
