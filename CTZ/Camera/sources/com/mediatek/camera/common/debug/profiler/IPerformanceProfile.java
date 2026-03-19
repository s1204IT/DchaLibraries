package com.mediatek.camera.common.debug.profiler;

public interface IPerformanceProfile {
    void mark(String str);

    IPerformanceProfile start();

    void stop();
}
