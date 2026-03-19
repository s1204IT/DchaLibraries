package com.mediatek.camera.ui;

import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.IReviewUI;
import com.mediatek.camera.common.mode.photo.intent.IIntentPhotoUi;
import com.mediatek.camera.common.mode.video.videoui.IVideoUI;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.widget.PreviewFrameLayout;
import com.mediatek.camera.common.widget.RotateLayout;
import com.mediatek.camera.gesture.GestureManager;
import com.mediatek.camera.ui.SettingFragment;
import com.mediatek.camera.ui.modepicker.ModePickerManager;
import com.mediatek.camera.ui.modepicker.ModeProvider;
import com.mediatek.camera.ui.photo.IntentPhotoUi;
import com.mediatek.camera.ui.preview.PreviewManager;
import com.mediatek.camera.ui.shutter.ShutterButtonManager;
import com.mediatek.camera.ui.video.VideoUI;
import java.util.ArrayList;
import java.util.List;

public class CameraAppUI implements IAppUi {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CameraAppUI.class.getSimpleName());
    private AnimationManager mAnimationManager;
    private final IApp mApp;
    private Handler mConfigUIHandler;
    private String mCurrentModeName;
    private String mCurrentModeType;
    private EffectViewManager mEffectViewManager;
    private GestureManager mGestureManager;
    private IndicatorViewManager mIndicatorViewManager;
    private IAppUiListener.OnModeChangeListener mModeChangeListener;
    private ModePickerManager mModePickerManager;
    private ModeProvider mModeProvider;
    private OnScreenHintManager mOnScreenHintManager;
    private final OnOrientationChangeListenerImpl mOrientationChangeListener;
    private PreviewManager mPreviewManager;
    private QuickSwitcherManager mQuickSwitcherManager;
    private ViewGroup mSavingDialog;
    private SettingFragment mSettingFragment;
    private ShutterButtonManager mShutterManager;
    private ThumbnailViewManager mThumbnailViewManager;
    private String mCurrentCameraId = "0";
    private final List<IViewManager> mViewManagers = new ArrayList();

    public CameraAppUI(IApp iApp) {
        this.mConfigUIHandler = new ConfigUIHandler();
        this.mApp = iApp;
        this.mOrientationChangeListener = new OnOrientationChangeListenerImpl();
    }

    public void onCreate() {
        ViewGroup viewGroup = (ViewGroup) this.mApp.getActivity().getLayoutInflater().inflate(R.layout.camera_ui_root, (ViewGroup) this.mApp.getActivity().findViewById(R.id.app_ui_root), true);
        View viewFindViewById = viewGroup.findViewById(R.id.camera_ui_root);
        if (CameraUtil.isHasNavigationBar(this.mApp.getActivity())) {
            int navigationBarHeight = CameraUtil.getNavigationBarHeight(this.mApp.getActivity());
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) viewFindViewById.getLayoutParams();
            if (CameraUtil.isTablet()) {
                int displayRotation = CameraUtil.getDisplayRotation(this.mApp.getActivity());
                LogHelper.d(TAG, " onCreate displayRotation  " + displayRotation);
                if (displayRotation == 90 || displayRotation == 270) {
                    ((ViewGroup.MarginLayoutParams) layoutParams).leftMargin += navigationBarHeight;
                    viewFindViewById.setLayoutParams(layoutParams);
                } else {
                    ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin += navigationBarHeight;
                    viewFindViewById.setLayoutParams(layoutParams);
                }
            } else {
                ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin += navigationBarHeight;
                viewFindViewById.setLayoutParams(layoutParams);
            }
        }
        this.mModeProvider = new ModeProvider();
        String action = this.mApp.getActivity().getIntent().getAction();
        this.mGestureManager = new GestureManager(this.mApp.getActivity());
        this.mAnimationManager = new AnimationManager(this.mApp, this);
        this.mShutterManager = new ShutterButtonManager(this.mApp, viewGroup);
        this.mShutterManager.setVisibility(0);
        this.mShutterManager.setOnShutterChangedListener(new OnShutterChangeListenerImpl());
        this.mViewManagers.add(this.mShutterManager);
        if (!"android.media.action.IMAGE_CAPTURE".equals(action) && !"android.media.action.VIDEO_CAPTURE".equals(action)) {
            this.mThumbnailViewManager = new ThumbnailViewManager(this.mApp, viewGroup);
            this.mViewManagers.add(this.mThumbnailViewManager);
            this.mThumbnailViewManager.setVisibility(0);
        }
        this.mPreviewManager = new PreviewManager(this.mApp);
        this.mPreviewManager.setOnTouchListener(new OnTouchListenerImpl());
        this.mModePickerManager = new ModePickerManager(this.mApp, viewGroup);
        this.mModePickerManager.setSettingClickedListener(new OnSettingClickedListenerImpl());
        this.mModePickerManager.setModeChangeListener(new OnModeChangedListenerImpl());
        this.mModePickerManager.setVisibility(0);
        this.mViewManagers.add(this.mModePickerManager);
        this.mQuickSwitcherManager = new QuickSwitcherManager(this.mApp, viewGroup);
        this.mQuickSwitcherManager.setVisibility(0);
        this.mViewManagers.add(this.mQuickSwitcherManager);
        this.mIndicatorViewManager = new IndicatorViewManager(this.mApp, viewGroup);
        this.mIndicatorViewManager.setVisibility(0);
        this.mViewManagers.add(this.mIndicatorViewManager);
        this.mSettingFragment = new SettingFragment();
        this.mSettingFragment.setStateListener(new SettingStateListener());
        layoutSettingUI();
        this.mEffectViewManager = new EffectViewManager(this.mApp, viewGroup);
        this.mEffectViewManager.setVisibility(0);
        this.mViewManagers.add(this.mEffectViewManager);
        this.mOnScreenHintManager = new OnScreenHintManager(this.mApp, viewGroup);
        for (int i = 0; i < this.mViewManagers.size(); i++) {
            this.mViewManagers.get(i).onCreate();
        }
        this.mApp.registerOnOrientationChangeListener(this.mOrientationChangeListener);
        this.mApp.registerKeyEventListener(getKeyEventListener(), 2147483646);
    }

    public void onResume() {
        RotateLayout rotateLayout = (RotateLayout) this.mApp.getActivity().findViewById(R.id.app_ui);
        Configuration configuration = this.mApp.getActivity().getResources().getConfiguration();
        hideAlertDialog();
        LogHelper.d(TAG, "onResume orientation = " + configuration.orientation);
        if (rotateLayout != null) {
            if (configuration.orientation == 1) {
                rotateLayout.setOrientation(90, false);
            } else if (configuration.orientation == 2) {
                rotateLayout.setOrientation(0, false);
            }
        }
        for (int i = 0; i < this.mViewManagers.size(); i++) {
            this.mViewManagers.get(i).onResume();
        }
    }

    public void onPause() {
        for (int i = 0; i < this.mViewManagers.size(); i++) {
            this.mViewManagers.get(i).onPause();
        }
        hideAlertDialog();
        hideSetting();
        this.mPreviewManager.onPause();
    }

    public void onDestroy() {
        for (int i = 0; i < this.mViewManagers.size(); i++) {
            this.mViewManagers.get(i).onDestroy();
        }
        this.mApp.unregisterOnOrientationChangeListener(this.mOrientationChangeListener);
        this.mApp.unRegisterKeyEventListener(getKeyEventListener());
    }

    public void onConfigurationChanged(Configuration configuration) {
    }

    @Override
    public void updateThumbnail(final Bitmap bitmap) {
        if (this.mThumbnailViewManager != null) {
            this.mApp.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CameraAppUI.this.mThumbnailViewManager.updateThumbnail(bitmap);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
            });
        }
    }

    @Override
    public int getThumbnailViewWidth() {
        if (this.mThumbnailViewManager != null) {
            return this.mThumbnailViewManager.getThumbnailViewWidth();
        }
        return 0;
    }

    @Override
    public void registerQuickIconDone() {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mQuickSwitcherManager.registerQuickIconDone();
            }
        });
    }

    @Override
    public void registerMode(List<IAppUi.ModeItem> list) {
        this.mModeProvider.clearAllModes();
        for (int i = 0; i < list.size(); i++) {
            IAppUi.ModeItem modeItem = list.get(i);
            this.mModeProvider.registerMode(modeItem);
            if (modeItem.mType.equals("Picture")) {
                this.mShutterManager.registerShutterButton(this.mApp.getActivity().getResources().getDrawable(R.drawable.ic_shutter_photo), "Picture", 0);
            } else if (modeItem.mType.equals("Video")) {
                this.mShutterManager.registerShutterButton(this.mApp.getActivity().getResources().getDrawable(R.drawable.ic_shutter_video), "Video", 1);
            }
        }
        this.mModePickerManager.registerModeProvider(this.mModeProvider);
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mShutterManager.registerDone();
            }
        });
    }

    @Override
    public void updateCurrentMode(String str) {
        IAppUi.ModeItem mode;
        LogHelper.d(TAG, "updateCurrentMode mode = " + str);
        if (this.mModeProvider == null || (mode = this.mModeProvider.getMode(str)) == null || mode.mModeName.equals(this.mCurrentModeName)) {
            return;
        }
        this.mCurrentModeName = mode.mModeName;
        this.mCurrentModeType = mode.mType;
        final String[] modeSupportTypes = this.mModeProvider.getModeSupportTypes(this.mCurrentModeName, this.mCurrentCameraId);
        this.mModePickerManager.updateCurrentModeItem(mode);
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mShutterManager.updateModeSupportType(CameraAppUI.this.mCurrentModeType, modeSupportTypes);
            }
        });
    }

    @Override
    public void setPreviewSize(final int i, final int i2, final IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mPreviewManager.updatePreviewSize(i, i2, iSurfaceStatusListener);
            }
        });
    }

    @Override
    public void showScreenHint(final IAppUi.HintInfo hintInfo) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mOnScreenHintManager.showScreenHint(hintInfo);
            }
        });
    }

    @Override
    public void hideScreenHint(final IAppUi.HintInfo hintInfo) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mOnScreenHintManager.hideScreenHint(hintInfo);
            }
        });
    }

    @Override
    public ViewGroup getModeRootView() {
        return (ViewGroup) this.mApp.getActivity().findViewById(R.id.feature_root);
    }

    @Override
    public View getShutterRootView() {
        if (this.mShutterManager != null) {
            return this.mShutterManager.getShutterRootView();
        }
        return null;
    }

    @Override
    public PreviewFrameLayout getPreviewFrameLayout() {
        return this.mPreviewManager.getPreviewFrameLayout();
    }

    @Override
    public void onPreviewStarted(String str) {
        LogHelper.d(TAG, "onPreviewStarted previewCameraId = " + str);
        if (str == null) {
            return;
        }
        synchronized (this.mCurrentCameraId) {
            this.mCurrentCameraId = str;
        }
        this.mModePickerManager.onPreviewStarted(str);
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mAnimationManager.onPreviewStarted();
            }
        });
    }

    @Override
    public void onCameraSelected(String str) {
        synchronized (this.mCurrentCameraId) {
            this.mCurrentCameraId = str;
        }
        this.mModePickerManager.onPreviewStarted(str);
    }

    @Override
    public IVideoUI getVideoUi() {
        return new VideoUI(this.mApp, getModeRootView());
    }

    @Override
    public IReviewUI getReviewUI() {
        return new ReviewUI(this.mApp, (ViewGroup) ((ViewGroup) this.mApp.getActivity().findViewById(R.id.app_ui)).getChildAt(0));
    }

    @Override
    public IIntentPhotoUi getPhotoUi() {
        return new IntentPhotoUi(this.mApp.getActivity(), getModeRootView(), this);
    }

    @Override
    public void animationStart(final IAppUi.AnimationType animationType, final IAppUi.AnimationData animationData) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mAnimationManager.animationStart(animationType, animationData);
            }
        });
    }

    @Override
    public void animationEnd(final IAppUi.AnimationType animationType) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mAnimationManager.animationEnd(animationType);
            }
        });
    }

    @Override
    public void setUIVisibility(int i, int i2) {
        if (!isMainThread()) {
            LogHelper.d(TAG, "setUIVisibility + module " + i + " visibility " + i2);
            Message messageObtain = Message.obtain();
            messageObtain.arg1 = i;
            messageObtain.arg2 = i2;
            messageObtain.what = 2;
            this.mConfigUIHandler.sendMessage(messageObtain);
            LogHelper.d(TAG, "setUIVisibility - ");
            return;
        }
        setUIVisibilityImmediately(i, i2);
    }

    @Override
    public void setUIEnabled(int i, boolean z) {
        if (!isMainThread()) {
            LogHelper.d(TAG, "setUIEnabled + module " + i + " enabled " + z);
            Message messageObtain = Message.obtain();
            messageObtain.arg1 = i;
            messageObtain.arg2 = z ? 1 : 0;
            messageObtain.what = 3;
            this.mConfigUIHandler.sendMessage(messageObtain);
            LogHelper.d(TAG, "setUIEnabled - ");
            return;
        }
        setUIEnabledImmediately(i, z);
    }

    @Override
    public void applyAllUIVisibility(int i) {
        if (!isMainThread()) {
            LogHelper.d(TAG, "applyAllUIVisibility + visibility " + i);
            Message messageObtain = Message.obtain();
            messageObtain.arg1 = i;
            messageObtain.what = 0;
            this.mConfigUIHandler.sendMessage(messageObtain);
            LogHelper.d(TAG, "applyAllUIVisibility -");
            return;
        }
        applyAllUIVisibilityImmediately(i);
    }

    @Override
    public void applyAllUIEnabled(boolean z) {
        if (!isMainThread()) {
            LogHelper.d(TAG, "applyAllUIEnabled + enabled " + z);
            Message messageObtain = Message.obtain();
            messageObtain.arg1 = z ? 1 : 0;
            messageObtain.what = 1;
            this.mConfigUIHandler.sendMessage(messageObtain);
            LogHelper.d(TAG, "applyAllUIEnabled -");
            return;
        }
        applyAllUIEnabledImmediately(z);
    }

    private void setUIVisibilityImmediately(int i, int i2) {
        LogHelper.d(TAG, "setUIVisibilityImmediately + module " + i + " visibility " + i2);
        configUIVisibility(i, i2);
    }

    private void setUIEnabledImmediately(int i, boolean z) {
        LogHelper.d(TAG, "setUIEnabledImmediately + module " + i + " enabled " + z);
        configUIEnabled(i, z);
    }

    @Override
    public void updateBrightnessBackGround(final boolean z) {
        LogHelper.d(TAG, "setBackgroundColor visible = " + z);
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View viewFindViewById = CameraAppUI.this.mApp.getActivity().findViewById(R.id.brightness_view);
                if (z) {
                    viewFindViewById.setVisibility(0);
                } else {
                    viewFindViewById.setVisibility(8);
                }
            }
        });
    }

    private void applyAllUIVisibilityImmediately(int i) {
        LogHelper.d(TAG, "applyAllUIVisibilityImmediately + visibility " + i);
        this.mConfigUIHandler.removeMessages(0);
        for (int i2 = 0; i2 < this.mViewManagers.size(); i2++) {
            this.mViewManagers.get(i2).setVisibility(i);
        }
        getPreviewFrameLayout().setVisibility(i);
        this.mOnScreenHintManager.setVisibility(i);
        if (i == 8) {
            this.mQuickSwitcherManager.hideQuickSwitcherImmediately();
        }
    }

    private void applyAllUIEnabledImmediately(boolean z) {
        LogHelper.d(TAG, "applyAllUIEnabledImmediately + enabled " + z);
        this.mConfigUIHandler.removeMessages(1);
        for (int i = 0; i < this.mViewManagers.size(); i++) {
            this.mViewManagers.get(i).setEnabled(z);
        }
    }

    private boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    @Override
    public void clearPreviewStatusListener(final IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mPreviewManager.clearPreviewStatusListener(iSurfaceStatusListener);
            }
        });
    }

    @Override
    public void registerOnPreviewAreaChangedListener(IAppUiListener.OnPreviewAreaChangedListener onPreviewAreaChangedListener) {
        this.mPreviewManager.registerPreviewAreaChangedListener(onPreviewAreaChangedListener);
    }

    @Override
    public void unregisterOnPreviewAreaChangedListener(IAppUiListener.OnPreviewAreaChangedListener onPreviewAreaChangedListener) {
        this.mPreviewManager.unregisterPreviewAreaChangedListener(onPreviewAreaChangedListener);
    }

    @Override
    public void registerGestureListener(IAppUiListener.OnGestureListener onGestureListener, int i) {
        this.mGestureManager.registerGestureListener(onGestureListener, i);
    }

    @Override
    public void unregisterGestureListener(IAppUiListener.OnGestureListener onGestureListener) {
        this.mGestureManager.unregisterGestureListener(onGestureListener);
    }

    @Override
    public void registerOnShutterButtonListener(IAppUiListener.OnShutterButtonListener onShutterButtonListener, int i) {
        this.mShutterManager.registerOnShutterButtonListener(onShutterButtonListener, i);
    }

    @Override
    public void unregisterOnShutterButtonListener(IAppUiListener.OnShutterButtonListener onShutterButtonListener) {
        this.mShutterManager.unregisterOnShutterButtonListener(onShutterButtonListener);
    }

    public void setThumbnailClickedListener(IAppUiListener.OnThumbnailClickedListener onThumbnailClickedListener) {
        if (this.mThumbnailViewManager != null) {
            this.mThumbnailViewManager.setThumbnailClickedListener(onThumbnailClickedListener);
        }
    }

    @Override
    public void setModeChangeListener(IAppUiListener.OnModeChangeListener onModeChangeListener) {
        this.mModeChangeListener = onModeChangeListener;
    }

    @Override
    public void triggerShutterButtonClick(int i) {
        this.mShutterManager.triggerShutterButtonClicked(i);
    }

    @Override
    public void addToQuickSwitcher(View view, int i) {
        this.mQuickSwitcherManager.addToQuickSwitcher(view, i);
    }

    @Override
    public void removeFromQuickSwitcher(View view) {
        this.mQuickSwitcherManager.removeFromQuickSwitcher(view);
    }

    @Override
    public void addToIndicatorView(final View view, final int i) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mIndicatorViewManager.addToIndicatorView(view, i);
            }
        });
    }

    @Override
    public void removeFromIndicatorView(final View view) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mIndicatorViewManager.removeFromIndicatorView(view);
            }
        });
    }

    @Override
    public void addSettingView(ICameraSettingView iCameraSettingView) {
        this.mSettingFragment.addSettingView(iCameraSettingView);
    }

    @Override
    public void removeSettingView(ICameraSettingView iCameraSettingView) {
        this.mSettingFragment.removeSettingView(iCameraSettingView);
    }

    @Override
    public void refreshSettingView() {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mSettingFragment.refreshSettingView();
            }
        });
    }

    @Override
    public void updateSettingIconVisibility() {
        this.mModePickerManager.setSettingIconVisible(this.mSettingFragment.hasVisibleChild());
    }

    @Override
    public void showSavingDialog(final String str, final boolean z) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup viewGroup = (ViewGroup) CameraAppUI.this.mApp.getActivity().getWindow().getDecorView();
                if (CameraAppUI.this.mSavingDialog == null) {
                    CameraAppUI.this.mSavingDialog = (ViewGroup) CameraAppUI.this.mApp.getActivity().getLayoutInflater().inflate(R.layout.rotate_dialog, viewGroup, false);
                    View viewFindViewById = CameraAppUI.this.mSavingDialog.findViewById(R.id.dialog_progress);
                    TextView textView = (TextView) CameraAppUI.this.mSavingDialog.findViewById(R.id.dialog_text);
                    if (z) {
                        viewFindViewById.setVisibility(0);
                    } else {
                        viewFindViewById.setVisibility(8);
                    }
                    if (str != null) {
                        textView.setText(str);
                    } else {
                        textView.setText(R.string.saving_dialog_default_string);
                    }
                    viewGroup.addView(CameraAppUI.this.mSavingDialog);
                    int gSensorOrientation = CameraAppUI.this.mApp.getGSensorOrientation();
                    if (gSensorOrientation != -1) {
                        CameraUtil.rotateViewOrientation(CameraAppUI.this.mSavingDialog, gSensorOrientation + CameraUtil.getDisplayRotation(CameraAppUI.this.mApp.getActivity()), false);
                    }
                    CameraAppUI.this.mSavingDialog.setVisibility(0);
                    return;
                }
                ((TextView) CameraAppUI.this.mSavingDialog.findViewById(R.id.dialog_text)).setText(str);
            }
        });
    }

    @Override
    public void hideSavingDialog() {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (CameraAppUI.this.mSavingDialog != null) {
                    ViewGroup viewGroup = (ViewGroup) CameraAppUI.this.mApp.getActivity().getWindow().getDecorView();
                    CameraAppUI.this.mSavingDialog.setVisibility(8);
                    viewGroup.removeView(CameraAppUI.this.mSavingDialog);
                    CameraAppUI.this.mSavingDialog = null;
                }
            }
        });
    }

    @Override
    public void setEffectViewEntry(View view) {
        this.mEffectViewManager.setViewEntry(view);
    }

    @Override
    public void attachEffectViewEntry() {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mEffectViewManager.attachViewEntry();
            }
        });
    }

    @Override
    public void showQuickSwitcherOption(final View view) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mQuickSwitcherManager.showQuickSwitcherOption(view);
            }
        });
    }

    @Override
    public void hideQuickSwitcherOption() {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mQuickSwitcherManager.hideQuickSwitcherOption();
            }
        });
    }

    protected void removeTopSurface() {
        this.mPreviewManager.removeTopSurface();
    }

    private void layoutSettingUI() {
        LinearLayout linearLayout = (LinearLayout) this.mApp.getActivity().findViewById(R.id.setting_ui_root);
        if (CameraUtil.isHasNavigationBar(this.mApp.getActivity())) {
            Point point = new Point();
            this.mApp.getActivity().getWindowManager().getDefaultDisplay().getSize(point);
            LogHelper.d(TAG, "[layoutSettingUI], preview size don't contain navigation:" + point);
            LinearLayout linearLayout2 = (LinearLayout) linearLayout.findViewById(R.id.setting_container);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) linearLayout2.getLayoutParams();
            ((ViewGroup.LayoutParams) layoutParams).height = point.y;
            linearLayout2.setLayoutParams(layoutParams);
            LinearLayout linearLayout3 = (LinearLayout) linearLayout.findViewById(R.id.setting_tail);
            int navigationBarHeight = CameraUtil.getNavigationBarHeight(this.mApp.getActivity());
            LogHelper.d(TAG, "[layoutSettingUI], navigationBarHeight:" + navigationBarHeight);
            LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) linearLayout3.getLayoutParams();
            ((ViewGroup.LayoutParams) layoutParams2).height = navigationBarHeight;
            linearLayout3.setLayoutParams(layoutParams2);
        }
    }

    private void showSetting() {
        FragmentTransaction fragmentTransactionBeginTransaction = this.mApp.getActivity().getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.addToBackStack("setting_fragment");
        fragmentTransactionBeginTransaction.replace(R.id.setting_container, this.mSettingFragment, "Setting").commitAllowingStateLoss();
    }

    private void hideSetting() {
        this.mApp.getActivity().getFragmentManager().popBackStackImmediate("setting_fragment", 1);
    }

    private void hideAlertDialog() {
        CameraUtil.hideAlertDialog(this.mApp.getActivity());
    }

    private class OnShutterChangeListenerImpl implements ShutterButtonManager.OnShutterChangeListener {
        private OnShutterChangeListenerImpl() {
        }

        @Override
        public void onShutterTypeChanged(String str) {
            CameraAppUI.this.mCurrentModeType = str;
            LogHelper.i(CameraAppUI.TAG, "onShutterTypeChanged mCurrentModeType " + CameraAppUI.this.mCurrentModeType);
            CameraAppUI.this.mModeChangeListener.onModeSelected(CameraAppUI.this.mModeProvider.getModeEntryName(CameraAppUI.this.mCurrentModeName, CameraAppUI.this.mCurrentModeType).mClassName);
        }
    }

    private class SettingStateListener implements SettingFragment.StateListener {
        private SettingStateListener() {
        }

        @Override
        public void onCreate() {
            CameraAppUI.this.mApp.getActivity().findViewById(R.id.setting_ui_root).setVisibility(0);
            CameraAppUI.this.applyAllUIVisibility(8);
            CameraAppUI.this.setUIEnabled(3, false);
        }

        @Override
        public void onResume() {
        }

        @Override
        public void onPause() {
        }

        @Override
        public void onDestroy() {
            CameraAppUI.this.mApp.getActivity().findViewById(R.id.setting_ui_root).setVisibility(8);
            CameraAppUI.this.applyAllUIVisibility(0);
            CameraAppUI.this.setUIEnabled(3, true);
        }
    }

    private class OnTouchListenerImpl implements View.OnTouchListener {
        private OnTouchListenerImpl() {
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (CameraAppUI.this.mGestureManager != null) {
                CameraAppUI.this.getShutterRootView().getHitRect(new Rect());
                Configuration configuration = CameraAppUI.this.mApp.getActivity().getResources().getConfiguration();
                if (configuration.orientation == 1) {
                    if (motionEvent.getRawX() > r0.top) {
                        return true;
                    }
                } else if (configuration.orientation == 2 && motionEvent.getRawY() > r0.top) {
                    return true;
                }
                CameraAppUI.this.mGestureManager.getOnTouchListener().onTouch(view, motionEvent);
            }
            return true;
        }
    }

    private class OnSettingClickedListenerImpl implements View.OnClickListener {
        private OnSettingClickedListenerImpl() {
        }

        @Override
        public void onClick(View view) {
            if (CameraAppUI.this.mSettingFragment.hasVisibleChild()) {
                CameraAppUI.this.showSetting();
            }
        }
    }

    private class OnModeChangedListenerImpl implements ModePickerManager.OnModeChangedListener {
        private OnModeChangedListenerImpl() {
        }

        @Override
        public void onModeChanged(String str) {
            CameraAppUI.this.mCurrentModeName = str;
            IAppUi.ModeItem modeEntryName = CameraAppUI.this.mModeProvider.getModeEntryName(CameraAppUI.this.mCurrentModeName, CameraAppUI.this.mCurrentModeType);
            CameraAppUI.this.mModeChangeListener.onModeSelected(modeEntryName.mClassName);
            CameraAppUI.this.mModePickerManager.updateCurrentModeItem(modeEntryName);
            CameraAppUI.this.mShutterManager.updateModeSupportType(CameraAppUI.this.mCurrentModeType, CameraAppUI.this.mModeProvider.getModeSupportTypes(modeEntryName.mModeName, CameraAppUI.this.mCurrentCameraId));
            CameraAppUI.this.mShutterManager.updateCurrentModeShutter(modeEntryName.mType, modeEntryName.mShutterIcon);
        }
    }

    private class OnOrientationChangeListenerImpl implements IApp.OnOrientationChangeListener {
        private OnOrientationChangeListenerImpl() {
        }

        @Override
        public void onOrientationChanged(int i) {
            if (CameraAppUI.this.mSavingDialog != null) {
                CameraUtil.rotateViewOrientation(CameraAppUI.this.mSavingDialog, i + CameraUtil.getDisplayRotation(CameraAppUI.this.mApp.getActivity()), true);
            }
        }
    }

    private class ConfigUIHandler extends Handler {
        private ConfigUIHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            LogHelper.d(CameraAppUI.TAG, "handleMessage what =  " + message.what);
            switch (message.what) {
                case 0:
                    int i = message.arg1;
                    for (int i2 = 0; i2 < CameraAppUI.this.mViewManagers.size(); i2++) {
                        ((IViewManager) CameraAppUI.this.mViewManagers.get(i2)).setVisibility(i);
                    }
                    CameraAppUI.this.getPreviewFrameLayout().setVisibility(i);
                    CameraAppUI.this.mOnScreenHintManager.setVisibility(i);
                    if (i == 8) {
                        CameraAppUI.this.mQuickSwitcherManager.hideQuickSwitcherImmediately();
                    }
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    boolean z = message.arg1 == 1;
                    for (int i3 = 0; i3 < CameraAppUI.this.mViewManagers.size(); i3++) {
                        ((IViewManager) CameraAppUI.this.mViewManagers.get(i3)).setEnabled(z);
                    }
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    CameraAppUI.this.configUIVisibility(message.arg1, message.arg2);
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    CameraAppUI.this.configUIEnabled(message.arg1, message.arg2 == 1);
                    break;
            }
        }
    }

    private void configUIVisibility(int i, int i2) {
        LogHelper.d(TAG, "configUIVisibility + module " + i + " visibility " + i2);
        if (i != 8) {
            switch (i) {
                case 0:
                    this.mQuickSwitcherManager.setVisibility(i2);
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    this.mModePickerManager.setVisibility(i2);
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    if (this.mThumbnailViewManager != null) {
                        this.mThumbnailViewManager.setVisibility(i2);
                    }
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    this.mShutterManager.setVisibility(i2);
                    break;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    this.mIndicatorViewManager.setVisibility(i2);
                    break;
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                    getPreviewFrameLayout().setVisibility(i2);
                    break;
            }
        }
        this.mOnScreenHintManager.setVisibility(i2);
    }

    private void configUIEnabled(int i, boolean z) {
        LogHelper.d(TAG, "configUIEnabled + module " + i + " enabled " + z);
        switch (i) {
            case 0:
                this.mQuickSwitcherManager.setEnabled(z);
                break;
            case Camera2Proxy.TEMPLATE_PREVIEW:
                this.mModePickerManager.setEnabled(z);
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                if (this.mThumbnailViewManager != null) {
                    this.mThumbnailViewManager.setEnabled(z);
                }
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                this.mShutterManager.setEnabled(z);
                break;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                this.mIndicatorViewManager.setEnabled(z);
                break;
            case Camera2Proxy.TEMPLATE_MANUAL:
                this.mPreviewManager.setEnabled(z);
                break;
            case 7:
                this.mShutterManager.setTextEnabled(z);
                break;
        }
    }

    public IApp.KeyEventListener getKeyEventListener() {
        return new IApp.KeyEventListener() {
            @Override
            public boolean onKeyDown(int i, KeyEvent keyEvent) {
                return false;
            }

            @Override
            public boolean onKeyUp(int i, KeyEvent keyEvent) {
                if (!CameraUtil.isSpecialKeyCodeEnabled() || !CameraUtil.isNeedInitSetting(i)) {
                    return false;
                }
                CameraAppUI.this.showSetting();
                CameraAppUI.this.hideSetting();
                return false;
            }
        };
    }
}
