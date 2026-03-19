package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.CatCmdMessage;
import com.android.internal.telephony.cat.CatResponseMessage;

public class MtkCatResponseMessage extends CatResponseMessage {
    byte[] mAdditionalInfo;
    int mDestinationId;
    int mEvent;
    boolean mOneShot;
    int mSourceId;

    public MtkCatResponseMessage(CatCmdMessage catCmdMessage) {
        super(catCmdMessage);
        this.mEvent = 0;
        this.mSourceId = 0;
        this.mDestinationId = 0;
        this.mAdditionalInfo = null;
        this.mOneShot = false;
    }

    public MtkCatResponseMessage(CatCmdMessage catCmdMessage, int i) {
        super(catCmdMessage);
        this.mEvent = 0;
        this.mSourceId = 0;
        this.mDestinationId = 0;
        this.mAdditionalInfo = null;
        this.mOneShot = false;
        this.mEvent = i;
    }

    public MtkCatResponseMessage(CatCmdMessage catCmdMessage, CatResponseMessage catResponseMessage) {
        super(catCmdMessage);
        this.mEvent = 0;
        this.mSourceId = 0;
        this.mDestinationId = 0;
        this.mAdditionalInfo = null;
        this.mOneShot = false;
        this.mCmdDet = catResponseMessage.mCmdDet;
        this.mResCode = catResponseMessage.mResCode;
        this.mUsersMenuSelection = catResponseMessage.mUsersMenuSelection;
        this.mUsersInput = catResponseMessage.mUsersInput;
        this.mUsersYesNoSelection = catResponseMessage.mUsersYesNoSelection;
        this.mUsersConfirm = catResponseMessage.mUsersConfirm;
        this.mIncludeAdditionalInfo = catResponseMessage.mIncludeAdditionalInfo;
        this.mEventValue = catResponseMessage.mEventValue;
        this.mAddedInfo = catResponseMessage.mAddedInfo;
    }

    public void setSourceId(int i) {
        this.mSourceId = i;
    }

    public void setEventId(int i) {
        this.mEvent = i;
    }

    public void setDestinationId(int i) {
        this.mDestinationId = i;
    }

    public void setAdditionalInfo(byte[] bArr) {
        if (bArr != null) {
            this.mIncludeAdditionalInfo = true;
        }
        this.mAdditionalInfo = bArr;
    }

    public void setOneShot(boolean z) {
        this.mOneShot = z;
    }
}
