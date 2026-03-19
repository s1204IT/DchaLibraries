package com.android.internal.telephony.cat;

public class CatResponseMessage {
    byte[] mAddedInfo;
    byte[] mAdditionalInfo;
    CommandDetails mCmdDet;
    int mDestinationId;
    int mEvent;
    int mEventValue;
    boolean mIncludeAdditionalInfo;
    boolean mOneShot;
    ResultCode mResCode;
    int mSourceId;
    boolean mUsersConfirm;
    String mUsersInput;
    int mUsersMenuSelection;
    boolean mUsersYesNoSelection;

    public CatResponseMessage(CatCmdMessage cmdMsg) {
        this.mCmdDet = null;
        this.mResCode = ResultCode.OK;
        this.mUsersMenuSelection = 0;
        this.mUsersInput = null;
        this.mUsersYesNoSelection = false;
        this.mUsersConfirm = false;
        this.mIncludeAdditionalInfo = false;
        this.mEvent = 0;
        this.mSourceId = 0;
        this.mDestinationId = 0;
        this.mAdditionalInfo = null;
        this.mOneShot = false;
        this.mEventValue = -1;
        this.mAddedInfo = null;
        this.mCmdDet = cmdMsg.mCmdDet;
    }

    public void setResultCode(ResultCode resCode) {
        this.mResCode = resCode;
    }

    public void setMenuSelection(int selection) {
        this.mUsersMenuSelection = selection;
    }

    public void setInput(String input) {
        this.mUsersInput = input;
    }

    public void setEventDownload(int event, byte[] addedInfo) {
        this.mEventValue = event;
        this.mAddedInfo = addedInfo;
    }

    public void setYesNo(boolean yesNo) {
        this.mUsersYesNoSelection = yesNo;
    }

    public void setConfirmation(boolean confirm) {
        this.mUsersConfirm = confirm;
    }

    CommandDetails getCmdDetails() {
        return this.mCmdDet;
    }

    public CatResponseMessage() {
        this.mCmdDet = null;
        this.mResCode = ResultCode.OK;
        this.mUsersMenuSelection = 0;
        this.mUsersInput = null;
        this.mUsersYesNoSelection = false;
        this.mUsersConfirm = false;
        this.mIncludeAdditionalInfo = false;
        this.mEvent = 0;
        this.mSourceId = 0;
        this.mDestinationId = 0;
        this.mAdditionalInfo = null;
        this.mOneShot = false;
        this.mEventValue = -1;
        this.mAddedInfo = null;
    }

    public CatResponseMessage(int event) {
        this.mCmdDet = null;
        this.mResCode = ResultCode.OK;
        this.mUsersMenuSelection = 0;
        this.mUsersInput = null;
        this.mUsersYesNoSelection = false;
        this.mUsersConfirm = false;
        this.mIncludeAdditionalInfo = false;
        this.mEvent = 0;
        this.mSourceId = 0;
        this.mDestinationId = 0;
        this.mAdditionalInfo = null;
        this.mOneShot = false;
        this.mEventValue = -1;
        this.mAddedInfo = null;
        this.mEvent = event;
    }

    public void setSourceId(int sId) {
        this.mSourceId = sId;
    }

    public void setEventId(int event) {
        this.mEvent = event;
    }

    public void setDestinationId(int dId) {
        this.mDestinationId = dId;
    }

    public void setAdditionalInfo(byte[] additionalInfo) {
        if (additionalInfo != null) {
            this.mIncludeAdditionalInfo = true;
        }
        this.mAdditionalInfo = additionalInfo;
    }

    public void setOneShot(boolean shot) {
        this.mOneShot = shot;
    }
}
