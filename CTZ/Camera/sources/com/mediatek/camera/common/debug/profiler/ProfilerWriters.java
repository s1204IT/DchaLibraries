package com.mediatek.camera.common.debug.profiler;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class ProfilerWriters {
    private static ILogWriter sDebugWriter = new DebugWriter();

    public static ILogWriter getLogWriter() {
        return sDebugWriter;
    }

    private static class DebugWriter implements ILogWriter {
        private DebugWriter() {
        }

        @Override
        public void write(LogUtil.Tag tag, String str) {
            LogHelper.d(tag, str);
        }
    }
}
