package com.mediatek.camera.feature.setting.selftimer;

public class ISelfTimerViewListener {

    public interface OnItemClickListener {
        void onItemClick(String str);
    }

    public interface OnSelfTimerListener {
        void onTimerDone();

        void onTimerInterrupt();

        void onTimerStart();
    }

    public interface OnValueChangeListener {
        void onValueChanged(String str);
    }
}
