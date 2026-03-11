package android.support.v4.app;

import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.Nullable;

abstract class BaseFragmentActivityEclair extends BaseFragmentActivityDonut {
    boolean mStartedIntentSenderFromFragment;

    BaseFragmentActivityEclair() {
    }

    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        if (!this.mStartedIntentSenderFromFragment && requestCode != -1) {
            checkForValidRequestCode(requestCode);
        }
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    void onBackPressedNotHandled() {
        super.onBackPressed();
    }

    static void checkForValidRequestCode(int requestCode) {
        if (((-65536) & requestCode) == 0) {
        } else {
            throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
        }
    }
}
