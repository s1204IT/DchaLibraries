package com.bumptech.glide.manager;

public interface ConnectivityMonitor {

    public interface ConnectivityListener {
        void onConnectivityChanged(boolean z);
    }

    void register();

    void unregister();
}
