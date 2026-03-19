package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class UiccCard {
    protected static final boolean DBG = true;
    public static final String EXTRA_ICC_CARD_ADDED = "com.android.internal.telephony.uicc.ICC_CARD_ADDED";
    protected static final String LOG_TAG = "UiccCard";
    protected String mCardId;
    private IccCardStatus.CardState mCardState;
    private CommandsInterface mCi;
    private Context mContext;
    private String mIccid;
    private final Object mLock;
    private final int mPhoneId;
    private UiccProfile mUiccProfile;

    public UiccCard(Context context, CommandsInterface commandsInterface, IccCardStatus iccCardStatus, int i, Object obj) {
        log("Creating");
        this.mCardState = iccCardStatus.mCardState;
        this.mPhoneId = i;
        this.mLock = obj;
        update(context, commandsInterface, iccCardStatus);
    }

    public void dispose() {
        synchronized (this.mLock) {
            log("Disposing card");
            if (this.mUiccProfile != null) {
                this.mUiccProfile.dispose();
            }
            this.mUiccProfile = null;
        }
    }

    public void update(Context context, CommandsInterface commandsInterface, IccCardStatus iccCardStatus) {
        synchronized (this.mLock) {
            this.mCardState = iccCardStatus.mCardState;
            this.mContext = context;
            this.mCi = commandsInterface;
            this.mIccid = iccCardStatus.iccid;
            updateCardId();
            if (this.mCardState != IccCardStatus.CardState.CARDSTATE_ABSENT) {
                if (this.mUiccProfile == null) {
                    this.mUiccProfile = TelephonyComponentFactory.getInstance().makeUiccProfile(this.mContext, this.mCi, iccCardStatus, this.mPhoneId, this, this.mLock);
                } else {
                    this.mUiccProfile.update(this.mContext, this.mCi, iccCardStatus);
                }
            } else {
                throw new RuntimeException("Card state is absent when updating!");
            }
        }
    }

    protected void finalize() {
        log("UiccCard finalized");
    }

    protected void updateCardId() {
        this.mCardId = this.mIccid;
    }

    @Deprecated
    public void registerForCarrierPrivilegeRulesLoaded(Handler handler, int i, Object obj) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                this.mUiccProfile.registerForCarrierPrivilegeRulesLoaded(handler, i, obj);
            } else {
                loge("registerForCarrierPrivilegeRulesLoaded Failed!");
            }
        }
    }

    @Deprecated
    public void unregisterForCarrierPrivilegeRulesLoaded(Handler handler) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                this.mUiccProfile.unregisterForCarrierPrivilegeRulesLoaded(handler);
            } else {
                loge("unregisterForCarrierPrivilegeRulesLoaded Failed!");
            }
        }
    }

    @Deprecated
    public boolean isApplicationOnIcc(IccCardApplicationStatus.AppType appType) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                return this.mUiccProfile.isApplicationOnIcc(appType);
            }
            return false;
        }
    }

    public IccCardStatus.CardState getCardState() {
        IccCardStatus.CardState cardState;
        synchronized (this.mLock) {
            cardState = this.mCardState;
        }
        return cardState;
    }

    @Deprecated
    public IccCardStatus.PinState getUniversalPinState() {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                return this.mUiccProfile.getUniversalPinState();
            }
            return IccCardStatus.PinState.PINSTATE_UNKNOWN;
        }
    }

    @Deprecated
    public UiccCardApplication getApplication(int i) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                return this.mUiccProfile.getApplication(i);
            }
            return null;
        }
    }

    @Deprecated
    public UiccCardApplication getApplicationIndex(int i) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                return this.mUiccProfile.getApplicationIndex(i);
            }
            return null;
        }
    }

    @Deprecated
    public UiccCardApplication getApplicationByType(int i) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                return this.mUiccProfile.getApplicationByType(i);
            }
            return null;
        }
    }

    @Deprecated
    public boolean resetAppWithAid(String str) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                return this.mUiccProfile.resetAppWithAid(str);
            }
            return false;
        }
    }

    @Deprecated
    public void iccOpenLogicalChannel(String str, int i, Message message) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccOpenLogicalChannel(str, i, message);
        } else {
            loge("iccOpenLogicalChannel Failed!");
        }
    }

    @Deprecated
    public void iccCloseLogicalChannel(int i, Message message) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccCloseLogicalChannel(i, message);
        } else {
            loge("iccCloseLogicalChannel Failed!");
        }
    }

    @Deprecated
    public void iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, String str, Message message) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccTransmitApduLogicalChannel(i, i2, i3, i4, i5, i6, str, message);
        } else {
            loge("iccTransmitApduLogicalChannel Failed!");
        }
    }

    @Deprecated
    public void iccTransmitApduBasicChannel(int i, int i2, int i3, int i4, int i5, String str, Message message) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccTransmitApduBasicChannel(i, i2, i3, i4, i5, str, message);
        } else {
            loge("iccTransmitApduBasicChannel Failed!");
        }
    }

    @Deprecated
    public void iccExchangeSimIO(int i, int i2, int i3, int i4, int i5, String str, Message message) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccExchangeSimIO(i, i2, i3, i4, i5, str, message);
        } else {
            loge("iccExchangeSimIO Failed!");
        }
    }

    @Deprecated
    public void sendEnvelopeWithStatus(String str, Message message) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.sendEnvelopeWithStatus(str, message);
        } else {
            loge("sendEnvelopeWithStatus Failed!");
        }
    }

    @Deprecated
    public int getNumApplications() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getNumApplications();
        }
        return 0;
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public UiccProfile getUiccProfile() {
        return this.mUiccProfile;
    }

    @Deprecated
    public boolean areCarrierPriviligeRulesLoaded() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.areCarrierPriviligeRulesLoaded();
        }
        return false;
    }

    @Deprecated
    public boolean hasCarrierPrivilegeRules() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.hasCarrierPrivilegeRules();
        }
        return false;
    }

    @Deprecated
    public int getCarrierPrivilegeStatus(Signature signature, String str) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPrivilegeStatus(signature, str);
        }
        return -1;
    }

    @Deprecated
    public int getCarrierPrivilegeStatus(PackageManager packageManager, String str) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPrivilegeStatus(packageManager, str);
        }
        return -1;
    }

    @Deprecated
    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPrivilegeStatus(packageInfo);
        }
        return -1;
    }

    @Deprecated
    public int getCarrierPrivilegeStatusForCurrentTransaction(PackageManager packageManager) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPrivilegeStatusForCurrentTransaction(packageManager);
        }
        return -1;
    }

    @Deprecated
    public List<String> getCarrierPackageNamesForIntent(PackageManager packageManager, Intent intent) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPackageNamesForIntent(packageManager, intent);
        }
        return null;
    }

    @Deprecated
    public boolean setOperatorBrandOverride(String str) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.setOperatorBrandOverride(str);
        }
        return false;
    }

    @Deprecated
    public String getOperatorBrandOverride() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getOperatorBrandOverride();
        }
        return null;
    }

    public String getIccId() {
        if (this.mIccid != null) {
            return this.mIccid;
        }
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getIccId();
        }
        return null;
    }

    public String getCardId() {
        if (this.mCardId != null) {
            return this.mCardId;
        }
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getIccId();
        }
        return null;
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("UiccCard:");
        printWriter.println(" mCi=" + this.mCi);
        printWriter.println(" mCardState=" + this.mCardState);
        printWriter.println();
        if (this.mUiccProfile != null) {
            this.mUiccProfile.dump(fileDescriptor, printWriter, strArr);
        }
    }
}
