package com.android.internal.telephony.cat;

public class CatResponseMessage {
    public CommandDetails mCmdDet;
    public ResultCode mResCode = ResultCode.OK;
    public int mUsersMenuSelection = 0;
    public String mUsersInput = null;
    public boolean mUsersYesNoSelection = false;
    public boolean mUsersConfirm = false;
    public boolean mIncludeAdditionalInfo = false;
    public int mAdditionalInfo = 0;
    public int mEventValue = -1;
    public byte[] mAddedInfo = null;

    public CatResponseMessage(CatCmdMessage catCmdMessage) {
        this.mCmdDet = null;
        this.mCmdDet = catCmdMessage.mCmdDet;
    }

    public void setResultCode(ResultCode resultCode) {
        this.mResCode = resultCode;
    }

    public void setMenuSelection(int i) {
        this.mUsersMenuSelection = i;
    }

    public void setInput(String str) {
        this.mUsersInput = str;
    }

    public void setEventDownload(int i, byte[] bArr) {
        this.mEventValue = i;
        this.mAddedInfo = bArr;
    }

    public void setYesNo(boolean z) {
        this.mUsersYesNoSelection = z;
    }

    public void setConfirmation(boolean z) {
        this.mUsersConfirm = z;
    }

    public void setAdditionalInfo(int i) {
        this.mIncludeAdditionalInfo = true;
        this.mAdditionalInfo = i;
    }

    public CommandDetails getCmdDetails() {
        return this.mCmdDet;
    }
}
