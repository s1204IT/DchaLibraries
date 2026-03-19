package org.gsma.joyn.capability;

import org.gsma.joyn.capability.ICapabilitiesListener;

public abstract class CapabilitiesListener extends ICapabilitiesListener.Stub {
    @Override
    public abstract void onCapabilitiesReceived(String str, Capabilities capabilities);
}
