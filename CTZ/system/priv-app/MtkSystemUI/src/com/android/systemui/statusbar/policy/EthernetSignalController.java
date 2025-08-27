package com.android.systemui.statusbar.policy;

import android.content.Context;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalController;
import java.util.BitSet;

/* loaded from: classes.dex */
public class EthernetSignalController extends SignalController<SignalController.State, SignalController.IconGroup> {
    public EthernetSignalController(Context context, CallbackHandler callbackHandler, NetworkControllerImpl networkControllerImpl) {
        super("EthernetSignalController", context, 3, callbackHandler, networkControllerImpl);
        T t = this.mCurrentState;
        T t2 = this.mLastState;
        SignalController.IconGroup iconGroup = new SignalController.IconGroup("Ethernet Icons", EthernetIcons.ETHERNET_ICONS, null, AccessibilityContentDescriptions.ETHERNET_CONNECTION_VALUES, 0, 0, 0, 0, AccessibilityContentDescriptions.ETHERNET_CONNECTION_VALUES[0]);
        t2.iconGroup = iconGroup;
        t.iconGroup = iconGroup;
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public void updateConnectivity(BitSet bitSet, BitSet bitSet2) {
        this.mCurrentState.connected = bitSet.get(this.mTransportType);
        super.updateConnectivity(bitSet, bitSet2);
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public void notifyListeners(NetworkController.SignalCallback signalCallback) {
        signalCallback.setEthernetIndicators(new NetworkController.IconState(this.mCurrentState.connected, getCurrentIconId(), getStringIfExists(getContentDescription())));
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public SignalController.State cleanState() {
        return new SignalController.State();
    }
}
