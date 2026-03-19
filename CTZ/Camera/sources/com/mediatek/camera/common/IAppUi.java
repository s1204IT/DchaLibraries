package com.mediatek.camera.common;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.mode.IReviewUI;
import com.mediatek.camera.common.mode.photo.intent.IIntentPhotoUi;
import com.mediatek.camera.common.mode.video.videoui.IVideoUI;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.common.widget.PreviewFrameLayout;
import java.util.List;

public interface IAppUi {

    public static class AnimationData {
        public byte[] mData;
        public int mFormat;
        public int mHeight;
        public boolean mIsMirror;
        public int mOrientation;
        public int mWidth;
    }

    public enum AnimationType {
        TYPE_SWITCH_CAMERA,
        TYPE_CAPTURE,
        TYPE_SWITCH_MODE
    }

    public static class HintInfo {
        public Drawable mBackground;
        public int mDelayTime;
        public String mHintText;
        public HintType mType;
    }

    public enum HintType {
        TYPE_ALWAYS_TOP,
        TYPE_AUTO_HIDE,
        TYPE_ALWAYS_BOTTOM
    }

    public static class ModeItem {
        public String mClassName;
        public String mModeName;
        public Drawable mModeSelectedIcon;
        public Drawable mModeUnselectedIcon;
        public int mPriority;
        public Drawable mShutterIcon;
        public String[] mSupportedCameraIds;
        public String mType;
    }

    void addSettingView(ICameraSettingView iCameraSettingView);

    void addToIndicatorView(View view, int i);

    void addToQuickSwitcher(View view, int i);

    void animationEnd(AnimationType animationType);

    void animationStart(AnimationType animationType, AnimationData animationData);

    void applyAllUIEnabled(boolean z);

    void applyAllUIVisibility(int i);

    void attachEffectViewEntry();

    void clearPreviewStatusListener(IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener);

    ViewGroup getModeRootView();

    IIntentPhotoUi getPhotoUi();

    PreviewFrameLayout getPreviewFrameLayout();

    IReviewUI getReviewUI();

    View getShutterRootView();

    int getThumbnailViewWidth();

    IVideoUI getVideoUi();

    void hideQuickSwitcherOption();

    void hideSavingDialog();

    void hideScreenHint(HintInfo hintInfo);

    void onCameraSelected(String str);

    void onPreviewStarted(String str);

    void refreshSettingView();

    void registerGestureListener(IAppUiListener.OnGestureListener onGestureListener, int i);

    void registerMode(List<ModeItem> list);

    void registerOnPreviewAreaChangedListener(IAppUiListener.OnPreviewAreaChangedListener onPreviewAreaChangedListener);

    void registerOnShutterButtonListener(IAppUiListener.OnShutterButtonListener onShutterButtonListener, int i);

    void registerQuickIconDone();

    void removeFromIndicatorView(View view);

    void removeFromQuickSwitcher(View view);

    void removeSettingView(ICameraSettingView iCameraSettingView);

    void setEffectViewEntry(View view);

    void setModeChangeListener(IAppUiListener.OnModeChangeListener onModeChangeListener);

    void setPreviewSize(int i, int i2, IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener);

    void setUIEnabled(int i, boolean z);

    void setUIVisibility(int i, int i2);

    void showQuickSwitcherOption(View view);

    void showSavingDialog(String str, boolean z);

    void showScreenHint(HintInfo hintInfo);

    void triggerShutterButtonClick(int i);

    void unregisterGestureListener(IAppUiListener.OnGestureListener onGestureListener);

    void unregisterOnPreviewAreaChangedListener(IAppUiListener.OnPreviewAreaChangedListener onPreviewAreaChangedListener);

    void unregisterOnShutterButtonListener(IAppUiListener.OnShutterButtonListener onShutterButtonListener);

    void updateBrightnessBackGround(boolean z);

    void updateCurrentMode(String str);

    void updateSettingIconVisibility();

    void updateThumbnail(Bitmap bitmap);
}
