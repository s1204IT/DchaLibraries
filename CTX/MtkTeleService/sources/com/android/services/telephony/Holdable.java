package com.android.services.telephony;

public interface Holdable {
    boolean isChildHoldable();

    void setHoldable(boolean z);
}
