package org.gsma.joyn.gsh;

import org.gsma.joyn.chat.Geoloc;
import org.gsma.joyn.gsh.IGeolocSharingListener;

public abstract class GeolocSharingListener extends IGeolocSharingListener.Stub {
    @Override
    public abstract void onGeolocShared(Geoloc geoloc);

    @Override
    public abstract void onSharingAborted();

    @Override
    public abstract void onSharingError(int i);

    @Override
    public abstract void onSharingProgress(long j, long j2);

    @Override
    public abstract void onSharingStarted();
}
