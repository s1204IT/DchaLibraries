package com.mediatek.camera.common.memory;

public interface IMemoryManager {

    public interface IMemoryListener {
        void onMemoryStateChanged(MemoryAction memoryAction);
    }

    public enum MemoryAction {
        NORMAL,
        ADJUST_SPEED,
        STOP
    }
}
