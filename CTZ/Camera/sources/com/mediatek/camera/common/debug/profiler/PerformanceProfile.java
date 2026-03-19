package com.mediatek.camera.common.debug.profiler;

import com.mediatek.camera.common.debug.LogUtil;

public class PerformanceProfile extends ProfileBase {
    private final LogUtil.Tag mTag;
    private ILogWriter mWriter;

    public PerformanceProfile(ILogWriter iLogWriter, LogUtil.Tag tag, String str) {
        super(str);
        this.mTag = tag;
        this.mWriter = iLogWriter;
    }

    @Override
    protected void onStart() {
        this.mWriter.write(this.mTag, this.mFormatter.format(0.0d, "[BEGIN]"));
    }

    @Override
    protected void onMark(double d, double d2, String str) {
        this.mWriter.write(this.mTag, this.mFormatter.format(d, "[MARK]", d2, str));
    }

    @Override
    protected void onStop(double d, double d2) {
        this.mWriter.write(this.mTag, this.mFormatter.format(d, "[END]"));
    }
}
