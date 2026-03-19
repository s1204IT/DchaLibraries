package com.mediatek.camera.common.debug.profiler;

import com.mediatek.camera.common.debug.LogUtil;

public interface ILogWriter {
    void write(LogUtil.Tag tag, String str);
}
