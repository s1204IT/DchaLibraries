package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.ResultException;

public class MtkResultException extends ResultException {
    MtkResultException(ResultCode resultCode, int i) {
        super(resultCode);
        this.mResult = resultCode;
        this.mExplanation = "";
        if (i < 0) {
            throw new AssertionError("Additional info must be greater than zero!");
        }
        this.mAdditionalInfo = i;
    }

    MtkResultException(ResultCode resultCode, int i, String str) {
        this(resultCode, i);
        this.mExplanation = str;
    }
}
