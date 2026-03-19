package com.android.internal.telephony;

public interface MmiCode {
    void cancel();

    CharSequence getMessage();

    Phone getPhone();

    State getState();

    boolean getUserInitiatedMMI();

    boolean isCancelable();

    boolean isPinPukCommand();

    boolean isUssdRequest();

    void processCode() throws CallStateException;

    public enum State {
        PENDING,
        CANCELLED,
        COMPLETE,
        FAILED;

        public static State[] valuesCustom() {
            return values();
        }
    }
}
