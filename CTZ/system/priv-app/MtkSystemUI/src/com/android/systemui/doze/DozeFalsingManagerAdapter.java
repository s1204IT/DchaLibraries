package com.android.systemui.doze;

import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.doze.DozeMachine;

/* loaded from: classes.dex */
public class DozeFalsingManagerAdapter implements DozeMachine.Part {
    private final FalsingManager mFalsingManager;

    public DozeFalsingManagerAdapter(FalsingManager falsingManager) {
        this.mFalsingManager = falsingManager;
    }

    @Override // com.android.systemui.doze.DozeMachine.Part
    public void transitionTo(DozeMachine.State state, DozeMachine.State state2) {
        this.mFalsingManager.setShowingAod(isAodMode(state2));
    }

    private boolean isAodMode(DozeMachine.State state) {
        switch (state) {
            case DOZE_AOD:
            case DOZE_AOD_PAUSING:
            case DOZE_AOD_PAUSED:
                return true;
            default:
                return false;
        }
    }
}
