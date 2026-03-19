package com.android.bips.discovery;

public interface ConnectionListener {
    void onConnectionComplete(DiscoveredPrinter discoveredPrinter);

    void onConnectionDelayed(boolean z);
}
