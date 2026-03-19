package org.gsma.joyn.gsh;

import org.gsma.joyn.gsh.INewGeolocSharingListener;

public abstract class NewGeolocSharingListener extends INewGeolocSharingListener.Stub {
    @Override
    public abstract void onNewGeolocSharing(String str);
}
