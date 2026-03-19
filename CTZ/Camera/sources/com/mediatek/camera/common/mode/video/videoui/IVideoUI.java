package com.mediatek.camera.common.mode.video.videoui;

import android.view.View;

public interface IVideoUI {

    public static class UISpec {
        public boolean isSupportedVss = false;
        public boolean isSupportedPause = false;
        public long recordingTotalSize = 0;
        public View.OnClickListener vssListener = null;
        public View.OnClickListener pauseResumeListener = null;
        public View.OnClickListener stopListener = null;
    }

    public enum VideoUIState {
        STATE_PREVIEW,
        STATE_PRE_RECORDING,
        STATE_RECORDING,
        STATE_PAUSE_RECORDING,
        STATE_RESUME_RECORDING
    }

    void initVideoUI(UISpec uISpec);

    void showInfo(int i);

    void unInitVideoUI();

    void updateOrientation(int i);

    void updateRecordingSize(long j);

    void updateUIState(VideoUIState videoUIState);
}
