package com.mediatek.camera.common.mode.photo.intent;

public interface IIntentPhotoUi {

    public interface OkButtonClickListener {
        void onOkClickClicked();
    }

    public interface RetakeButtonClickListener {
        void onRetakeClicked();
    }

    void hide();

    boolean isShown();

    void onOrientationChanged(int i);

    void onPictureReceived(byte[] bArr);

    void setOkButtonClickListener(OkButtonClickListener okButtonClickListener);

    void setRetakeButtonClickListener(RetakeButtonClickListener retakeButtonClickListener);
}
