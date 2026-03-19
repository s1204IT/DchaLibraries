package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;

public class IccCard {
    private IccCardConstants.State mIccCardState;

    public IccCard() {
        this.mIccCardState = IccCardConstants.State.UNKNOWN;
    }

    public IccCard(IccCardConstants.State state) {
        this.mIccCardState = IccCardConstants.State.UNKNOWN;
        this.mIccCardState = state;
    }

    public IccCardConstants.State getState() {
        return this.mIccCardState;
    }

    public IccRecords getIccRecords() {
        return null;
    }

    public void registerForNetworkLocked(Handler handler, int i, Object obj) {
    }

    public void unregisterForNetworkLocked(Handler handler) {
    }

    public void supplyPin(String str, Message message) {
        sendMessageWithCardAbsentException(message);
    }

    public void supplyPuk(String str, String str2, Message message) {
        sendMessageWithCardAbsentException(message);
    }

    public void supplyPin2(String str, Message message) {
        sendMessageWithCardAbsentException(message);
    }

    public void supplyPuk2(String str, String str2, Message message) {
        sendMessageWithCardAbsentException(message);
    }

    public void supplyNetworkDepersonalization(String str, Message message) {
        sendMessageWithCardAbsentException(message);
    }

    public boolean getIccLockEnabled() {
        return false;
    }

    public boolean getIccFdnEnabled() {
        return false;
    }

    public void setIccLockEnabled(boolean z, String str, Message message) {
        sendMessageWithCardAbsentException(message);
    }

    public void setIccFdnEnabled(boolean z, String str, Message message) {
        sendMessageWithCardAbsentException(message);
    }

    public void changeIccLockPassword(String str, String str2, Message message) {
        sendMessageWithCardAbsentException(message);
    }

    public void changeIccFdnPassword(String str, String str2, Message message) {
        sendMessageWithCardAbsentException(message);
    }

    public String getServiceProviderName() {
        return null;
    }

    public boolean isApplicationOnIcc(IccCardApplicationStatus.AppType appType) {
        return false;
    }

    public boolean hasIccCard() {
        return false;
    }

    public boolean getIccPin2Blocked() {
        return false;
    }

    public boolean getIccPuk2Blocked() {
        return false;
    }

    private void sendMessageWithCardAbsentException(Message message) {
        AsyncResult.forMessage(message).exception = new RuntimeException("No valid IccCard");
        message.sendToTarget();
    }

    public boolean getIccFdnAvailable() {
        return false;
    }

    public void repollIccStateForModemSmlChangeFeatrue(boolean z) {
    }

    public String getIccCardType() {
        return null;
    }

    public void registerForFdnChanged(Handler handler, int i, Object obj) {
    }

    public void unregisterForFdnChanged(Handler handler) {
    }

    public void iccExchangeSimIOEx(int i, int i2, int i3, int i4, int i5, String str, String str2, String str3, Message message) {
    }

    public void iccGetAtr(Message message) {
    }
}
