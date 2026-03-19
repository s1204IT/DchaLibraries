package com.mediatek.camera.feature.setting.hdr;

interface IHdr {

    public enum HdrModeType {
        SCENE_MODE_DEFAULT,
        ZVHDR_PHOTO,
        MVHDR_PHOTP,
        NONVHDR_PHOTO,
        ZVHDR_VIDEO,
        MVHDR_VIDEO,
        NONVHDR_VIDEO
    }

    public interface Listener {
        boolean isZsdHdrSupported();

        void onHdrValueChanged();

        void onPreviewStateChanged(boolean z);

        void setCameraId(int i);

        void updateModeDeviceState(String str);
    }
}
