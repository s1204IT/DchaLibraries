package com.android.internal.telephony.cat;

class ActivateParams extends CommandParams {
    int mTarget;

    ActivateParams(CommandDetails cmdDet, int target) {
        super(cmdDet);
        this.mTarget = 0;
        this.mTarget = target;
    }
}
