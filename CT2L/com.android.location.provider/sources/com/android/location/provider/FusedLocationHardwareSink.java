package com.android.location.provider;

import android.location.Location;

public abstract class FusedLocationHardwareSink {
    public abstract void onDiagnosticDataAvailable(String str);

    public abstract void onLocationAvailable(Location[] locationArr);
}
