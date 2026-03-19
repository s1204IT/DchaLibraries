package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import com.android.internal.telephony.cat.AppInterface;

public class CommandParams {
    public CommandDetails mCmdDet;
    public boolean mLoadIconFailed = false;

    public CommandParams(CommandDetails commandDetails) {
        this.mCmdDet = commandDetails;
    }

    public AppInterface.CommandType getCommandType() {
        return AppInterface.CommandType.fromInt(this.mCmdDet.typeOfCommand);
    }

    boolean setIcon(Bitmap bitmap) {
        return true;
    }

    public String toString() {
        return this.mCmdDet.toString();
    }
}
