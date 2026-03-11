package com.android.settings.connecteddevice.dock;

public interface DockUpdater {
    default void registerCallback() {
    }

    default void unregisterCallback() {
    }

    default void forceUpdate() {
    }
}
