package android.widget;

import android.os.Handler;

class DoubleDigitManager {
    private Integer intermediateDigit;
    private final CallBack mCallBack;
    private final long timeoutInMillis;

    interface CallBack {
        void singleDigitFinal(int i);

        boolean singleDigitIntermediate(int i);

        boolean twoDigitsFinal(int i, int i2);
    }

    public DoubleDigitManager(long j, CallBack callBack) {
        this.timeoutInMillis = j;
        this.mCallBack = callBack;
    }

    public void reportDigit(int i) {
        if (this.intermediateDigit == null) {
            this.intermediateDigit = Integer.valueOf(i);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (DoubleDigitManager.this.intermediateDigit != null) {
                        DoubleDigitManager.this.mCallBack.singleDigitFinal(DoubleDigitManager.this.intermediateDigit.intValue());
                        DoubleDigitManager.this.intermediateDigit = null;
                    }
                }
            }, this.timeoutInMillis);
            if (!this.mCallBack.singleDigitIntermediate(i)) {
                this.intermediateDigit = null;
                this.mCallBack.singleDigitFinal(i);
                return;
            }
            return;
        }
        if (this.mCallBack.twoDigitsFinal(this.intermediateDigit.intValue(), i)) {
            this.intermediateDigit = null;
        }
    }
}
