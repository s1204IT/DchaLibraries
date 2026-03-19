package com.android.services.telephony;

import android.telecom.PhoneAccountHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HoldTracker {
    private final Map<PhoneAccountHandle, List<Holdable>> mHoldables = new HashMap();

    public void addHoldable(PhoneAccountHandle phoneAccountHandle, Holdable holdable) {
        if (!this.mHoldables.containsKey(phoneAccountHandle)) {
            this.mHoldables.put(phoneAccountHandle, new ArrayList(1));
        }
        List<Holdable> list = this.mHoldables.get(phoneAccountHandle);
        if (!list.contains(holdable)) {
            list.add(holdable);
            updateHoldCapability(phoneAccountHandle);
        }
    }

    public void removeHoldable(PhoneAccountHandle phoneAccountHandle, Holdable holdable) {
        if (this.mHoldables.containsKey(phoneAccountHandle) && this.mHoldables.get(phoneAccountHandle).remove(holdable)) {
            updateHoldCapability(phoneAccountHandle);
        }
    }

    public void updateHoldCapability(PhoneAccountHandle phoneAccountHandle) {
        if (!this.mHoldables.containsKey(phoneAccountHandle)) {
            return;
        }
        List<Holdable> list = this.mHoldables.get(phoneAccountHandle);
        Iterator<Holdable> it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (!it.next().isChildHoldable()) {
                i++;
            }
        }
        android.telecom.Log.d(this, "topHoldableCount = " + i, new Object[0]);
        boolean z = i < 2;
        for (Holdable holdable : list) {
            holdable.setHoldable(holdable.isChildHoldable() ? false : z);
        }
    }
}
