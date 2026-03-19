package com.mediatek.camera.feature.setting.matrixdisplay;

import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.setting.matrixdisplay.MatrixDisplayHandler;
import com.mediatek.camera.feature.setting.matrixdisplay.MatrixDisplayParametersConfig;
import com.mediatek.camera.feature.setting.matrixdisplay.MatrixDisplayRequestConfig;
import com.mediatek.camera.feature.setting.matrixdisplay.MatrixDisplayViewManager;
import java.util.ArrayList;
import java.util.List;

public class MatrixDisplay extends SettingBase implements MatrixDisplayHandler.EffectAvailableListener, MatrixDisplayViewManager.ItemClickListener, MatrixDisplayViewManager.ViewStateCallback {
    private IMatrixDisplayConfig mDisplayConfig;
    private List<CharSequence> mEffectEntries;
    private List<CharSequence> mEffectEntryValues;
    private ImageView mImageView;
    private int mLayoutHeight;
    private int mLayoutWidth;
    private MainHandler mMainHandler;
    private MatrixDisplayHandler mMatrixDisplayHandler;
    private StatusMonitor.StatusResponder mMatrixDisplayResponder;
    private String mModeDeviceState;
    private String mModeKey;
    private int mOrientation;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private String mSelectedEffect;
    private List<String> mSupportedEffects;
    private List<String> mSupportedPreviewSize;
    private MatrixDisplayViewManager mViewManager;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MatrixDisplay.class.getSimpleName());
    private static final int[] MAX_SUPPORTED_PREVIEW_SIZE = {518400, 921600};
    private Object mDisplayConfigLock = new Object();
    private boolean mViewIsShowing = false;
    private View.OnClickListener mEntryViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.ui(MatrixDisplay.TAG, "[onClick], mViewIsShowing:" + MatrixDisplay.this.mViewIsShowing);
            if (MatrixDisplay.this.mViewIsShowing) {
                return;
            }
            if (!view.isEnabled()) {
                LogHelper.d(MatrixDisplay.TAG, "[onClick], view is disabled, return");
                return;
            }
            if (MatrixDisplay.this.mSupportedEffects == null) {
                LogHelper.d(MatrixDisplay.TAG, "[onClick], supported effect is null, return");
                return;
            }
            if (!"previewing".equals(MatrixDisplay.this.mModeDeviceState)) {
                LogHelper.d(MatrixDisplay.TAG, "[onClick] mModeDeviceState = " + MatrixDisplay.this.mModeDeviceState + ", not in previewing state, return");
                return;
            }
            if (MatrixDisplay.this.mViewManager == null) {
                MatrixDisplay.this.initializeViewManager();
            }
            MatrixDisplay.this.enterMatrixDisplay();
        }
    };
    private IApp.OnOrientationChangeListener mOrientationListener = new IApp.OnOrientationChangeListener() {
        @Override
        public void onOrientationChanged(int i) {
            MatrixDisplay.this.mOrientation = i;
            if (MatrixDisplay.this.mViewManager != null) {
                MatrixDisplay.this.mViewManager.setOrientation(MatrixDisplay.this.mOrientation);
            }
        }
    };
    private IAppUiListener.OnPreviewAreaChangedListener mPreviewAreaChangedListener = new IAppUiListener.OnPreviewAreaChangedListener() {
        @Override
        public void onPreviewAreaChanged(RectF rectF, Size size) {
            int i = (int) (rectF.right - rectF.left);
            int i2 = (int) (rectF.bottom - rectF.top);
            LogHelper.d(MatrixDisplay.TAG, "[onPreviewAreaChanged], layoutWidth = " + i + ", layoutHeight = " + i2);
            if (i != MatrixDisplay.this.mLayoutWidth || i2 != MatrixDisplay.this.mLayoutHeight) {
                MatrixDisplay.this.mLayoutWidth = i;
                MatrixDisplay.this.mLayoutHeight = i2;
                if (MatrixDisplay.this.mViewManager != null) {
                    MatrixDisplay.this.mViewManager.setLayoutSize(MatrixDisplay.this.mLayoutWidth, MatrixDisplay.this.mLayoutHeight);
                    MatrixDisplay.this.mViewManager.hideView(false, 0);
                }
            }
        }
    };
    private IApp.BackPressedListener mBackPressedListener = new IApp.BackPressedListener() {
        @Override
        public boolean onBackPressed() {
            LogHelper.d(MatrixDisplay.TAG, "[onBackPressed] mViewIsShowing:" + MatrixDisplay.this.mViewIsShowing);
            if (MatrixDisplay.this.mViewIsShowing) {
                MatrixDisplay.this.exitMatrixDisplay(true, 3000);
                return true;
            }
            return false;
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        LogHelper.d(TAG, "[init]");
        iApp.registerOnOrientationChangeListener(this.mOrientationListener);
        iApp.registerBackPressedListener(this.mBackPressedListener, Integer.MAX_VALUE);
        iApp.getAppUi().registerOnPreviewAreaChangedListener(this.mPreviewAreaChangedListener);
        if (this.mMatrixDisplayHandler == null) {
            this.mMatrixDisplayHandler = new MatrixDisplayHandler();
            this.mMatrixDisplayHandler.setEffectAvailableListener(this);
        }
        this.mMainHandler = new MainHandler(this.mActivity.getMainLooper());
        this.mMatrixDisplayResponder = this.mStatusMonitor.getStatusResponder("key_matrix_display_show");
    }

    @Override
    public void updateModeDeviceState(String str) {
        this.mModeDeviceState = str;
    }

    @Override
    public void unInit() {
        LogHelper.d(TAG, "[unInit]");
        this.mApp.unregisterOnOrientationChangeListener(this.mOrientationListener);
        this.mApp.unRegisterBackPressedListener(this.mBackPressedListener);
        this.mAppUi.unregisterOnPreviewAreaChangedListener(this.mPreviewAreaChangedListener);
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MatrixDisplay.this.mViewManager != null) {
                    MatrixDisplay.this.mViewManager.hideView(false, 0);
                    MatrixDisplay.this.mViewIsShowing = false;
                    ((SettingBase) MatrixDisplay.this).mAppUi.applyAllUIVisibility(0);
                    MatrixDisplay.this.mAppUi.applyAllUIEnabled(true);
                    MatrixDisplay.this.mViewManager = null;
                }
            }
        });
        if (this.mMatrixDisplayHandler != null) {
            this.mMatrixDisplayHandler.release();
        }
    }

    @Override
    public void addViewEntry() {
        LogHelper.d(TAG, "[addViewEntry], mImageView:" + this.mImageView);
        if (this.mImageView == null) {
            ImageView imageView = (ImageView) this.mActivity.getLayoutInflater().inflate(R.layout.matrix_display_entry_view, (ViewGroup) null, false);
            imageView.setImageDrawable(this.mActivity.getResources().getDrawable(R.drawable.ic_matrix_display_entry));
            imageView.setOnClickListener(this.mEntryViewClickListener);
            imageView.setVisibility(8);
            this.mAppUi.setEffectViewEntry(imageView);
            this.mImageView = imageView;
        }
    }

    @Override
    public void removeViewEntry() {
        this.mImageView = null;
        this.mAppUi.setEffectViewEntry(null);
    }

    @Override
    public void refreshViewEntry() {
        LogHelper.d(TAG, "[refreshViewEntry], entry values:" + getEntryValues());
        if ("com.mediatek.camera.feature.mode.pip.photo.PipPhotoMode".equals(this.mModeKey) || "com.mediatek.camera.feature.mode.pip.video.PipVideoMode".equals(this.mModeKey)) {
            LogHelper.d(TAG, "[refreshViewEntry], in pip mode, don't refresh, return");
        } else {
            this.mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (MatrixDisplay.this.mImageView != null) {
                        if (MatrixDisplay.this.getEntryValues().size() <= 1) {
                            MatrixDisplay.this.mImageView.setVisibility(8);
                        } else {
                            MatrixDisplay.this.mImageView.setVisibility(0);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        super.onModeOpened(str, modeType);
        this.mModeKey = str;
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_color_effect";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        synchronized (this.mDisplayConfigLock) {
            if (this.mDisplayConfig == null) {
                MatrixDisplayParametersConfig matrixDisplayParametersConfig = new MatrixDisplayParametersConfig(getKey(), this.mSettingDeviceRequester, new MatrixDisplayParametersConfig.ValueInitializedListener() {
                    @Override
                    public void onValueInitialized(List<String> list, String str, List<String> list2) {
                        MatrixDisplay.this.initializeValue(list, str, list2);
                    }
                });
                matrixDisplayParametersConfig.setPreviewFrameCallback(this.mMatrixDisplayHandler);
                this.mDisplayConfig = matrixDisplayParametersConfig;
            }
        }
        return (MatrixDisplayParametersConfig) this.mDisplayConfig;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        synchronized (this.mDisplayConfigLock) {
            if (this.mDisplayConfig == null) {
                MatrixDisplayRequestConfig matrixDisplayRequestConfig = new MatrixDisplayRequestConfig(getKey(), this.mSettingDevice2Requester, new MatrixDisplayRequestConfig.ValueInitializedListener() {
                    @Override
                    public void onValueInitialized(List<String> list, String str, List<String> list2) {
                        MatrixDisplay.this.initializeValue(list, str, list2);
                    }
                });
                matrixDisplayRequestConfig.setPreviewFrameCallback(this.mMatrixDisplayHandler);
                this.mDisplayConfig = matrixDisplayRequestConfig;
            }
        }
        return (MatrixDisplayRequestConfig) this.mDisplayConfig;
    }

    @Override
    public boolean onItemClicked(String str) {
        synchronized (this.mDisplayConfig) {
            if (this.mDisplayConfig == null) {
                return false;
            }
            if (!this.mViewIsShowing) {
                return false;
            }
            this.mSelectedEffect = str;
            setValue(this.mSelectedEffect);
            this.mDataStore.setValue("key_color_effect", str, getStoreScope(), true);
            exitMatrixDisplay(true, 3000);
            return true;
        }
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        LogHelper.d(TAG, "[overrideValues], headerKey:" + str + ", currentValue:" + str2 + ", supportValues:" + list);
        super.overrideValues(str, str2, list);
        synchronized (this.mDisplayConfigLock) {
            if (this.mDisplayConfig != null) {
                this.mDisplayConfig.setSelectedEffect(getValue());
            }
        }
    }

    @Override
    public void onViewCreated() {
        LogHelper.d(TAG, "[onViewCreated]");
        this.mMatrixDisplayHandler.initialize(this.mPreviewWidth, this.mPreviewHeight, 842094169, this.mLayoutWidth, this.mLayoutHeight);
    }

    @Override
    public void onViewScrollOut() {
        if (this.mViewIsShowing) {
            exitMatrixDisplay(false, 0);
        }
    }

    @Override
    public void onViewHidden() {
        LogHelper.d(TAG, "[onViewHidden]");
        this.mMatrixDisplayResponder.statusChanged("key_matrix_display_show", String.valueOf(false));
    }

    @Override
    public void onViewDestroyed() {
        LogHelper.d(TAG, "[onViewDestroyed]");
        this.mMatrixDisplayHandler.release();
    }

    public void initializeValue(final List<String> list, final String str, final List<String> list2) {
        LogHelper.d(TAG, "[initializeValue], supportedEffects:" + list);
        setSupportedPlatformValues(list);
        setSupportedEntryValues(list);
        setEntryValues(list);
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (list != null && list.size() != 0) {
                    MatrixDisplay.this.mSelectedEffect = ((SettingBase) MatrixDisplay.this).mDataStore.getValue("key_color_effect", str, MatrixDisplay.this.getStoreScope());
                    if ("com.mediatek.camera.feature.mode.pip.photo.PipPhotoMode".equals(MatrixDisplay.this.mModeKey) || "com.mediatek.camera.feature.mode.pip.video.PipVideoMode".equals(MatrixDisplay.this.mModeKey)) {
                        MatrixDisplay.this.mSelectedEffect = "none";
                        LogHelper.d(MatrixDisplay.TAG, "[initializeValue], in pip mode, set effect as none");
                    }
                    synchronized (MatrixDisplay.this.mDisplayConfigLock) {
                        if (MatrixDisplay.this.mDisplayConfig != null) {
                            MatrixDisplay.this.mDisplayConfig.setSelectedEffect(MatrixDisplay.this.mSelectedEffect);
                        }
                    }
                    MatrixDisplay.this.setValue(MatrixDisplay.this.mSelectedEffect);
                    MatrixDisplay.this.mSupportedPreviewSize = list2;
                    MatrixDisplay.this.mSupportedEffects = list;
                }
            }
        });
    }

    private void initializeViewManager() {
        this.mViewManager = new MatrixDisplayViewManager(this.mActivity);
        this.mViewManager.setViewStateCallback(this);
        this.mViewManager.setItemClickListener(this);
        this.mViewManager.setSurfaceAvailableListener(this.mMatrixDisplayHandler);
        this.mViewManager.setEffectUpdateListener(this.mMatrixDisplayHandler);
        initEffectEntriesAndEntryValues(this.mSupportedEffects);
        this.mViewManager.setEffectEntriesAndEntryValues(this.mEffectEntries, this.mEffectEntryValues);
        this.mViewManager.setLayoutSize(this.mLayoutWidth, this.mLayoutHeight);
        this.mViewManager.setOrientation(this.mOrientation);
        this.mViewManager.setDisplayOrientation(getDisplayOrientation());
    }

    private void enterMatrixDisplay() {
        synchronized (this.mDisplayConfigLock) {
            if (this.mDisplayConfig != null) {
                this.mMatrixDisplayResponder.statusChanged("key_matrix_display_show", String.valueOf(true));
                LogHelper.d(TAG, "[enterMatrixDisplay]");
                this.mAppUi.applyAllUIEnabled(false);
                this.mAppUi.applyAllUIVisibility(4);
                this.mDisplayConfig.setSelectedEffect(this.mEffectEntryValues.get(0).toString());
                computeSuitablePreviewSize();
                this.mDisplayConfig.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
                this.mDisplayConfig.setDisplayStatus(true);
                this.mSettingController.postRestriction(MatrixDisplayRestriction.getRestrictionGroup().getRelation("on", true));
                this.mDisplayConfig.sendSettingChangeRequest();
                this.mViewManager.setSelectedEffect(this.mSelectedEffect);
                this.mViewManager.setMirror(CameraUtil.isCameraFacingFront(this.mApp.getActivity(), Integer.parseInt(this.mSettingController.getCameraId())));
                this.mViewManager.showView();
                this.mViewIsShowing = true;
            }
        }
    }

    private void exitMatrixDisplay(boolean z, int i) {
        synchronized (this.mDisplayConfigLock) {
            if (this.mDisplayConfig != null) {
                LogHelper.d(TAG, "[exitMatrixDisplay]");
                this.mMatrixDisplayResponder.statusChanged("key_matrix_display_show", String.valueOf(false));
                this.mViewIsShowing = false;
                this.mDisplayConfig.setSelectedEffect(this.mSelectedEffect);
                this.mDisplayConfig.setDisplayStatus(false);
                this.mSettingController.postRestriction(MatrixDisplayRestriction.getRestrictionGroup().getRelation("off", true));
                this.mDisplayConfig.sendSettingChangeRequest();
                this.mMainHandler.sendMessageDelayed(this.mMainHandler.obtainMessage(0, i, 0, Boolean.valueOf(z)), 500L);
            }
        }
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0 && MatrixDisplay.this.mViewManager != null) {
                MatrixDisplay.this.mViewManager.hideView(((Boolean) message.obj).booleanValue(), message.arg1);
                MatrixDisplay.this.mViewIsShowing = false;
                MatrixDisplay.this.mAppUi.applyAllUIVisibility(0);
                MatrixDisplay.this.mAppUi.applyAllUIEnabled(true);
            }
        }
    }

    private int getDisplayOrientation() {
        return CameraUtil.getDisplayOrientation(CameraUtil.getDisplayRotation(this.mActivity), Integer.valueOf(this.mSettingController.getCameraId()).intValue(), this.mApp.getActivity());
    }

    private void initEffectEntriesAndEntryValues(List<String> list) {
        this.mEffectEntryValues = new ArrayList();
        this.mEffectEntries = new ArrayList();
        String[] stringArray = this.mActivity.getResources().getStringArray(R.array.pref_camera_coloreffect_entries);
        String[] stringArray2 = this.mActivity.getResources().getStringArray(R.array.pref_camera_coloreffect_entryvalues);
        for (int i = 0; i < stringArray2.length; i++) {
            String str = stringArray2[i];
            int i2 = 0;
            while (true) {
                if (i2 >= list.size()) {
                    break;
                }
                if (!str.equals(list.get(i2))) {
                    i2++;
                } else {
                    this.mEffectEntryValues.add(str);
                    this.mEffectEntries.add(stringArray[i]);
                    break;
                }
            }
        }
    }

    private void computeSuitablePreviewSize() {
        int i;
        double dMax = ((double) Math.max(this.mLayoutWidth, this.mLayoutHeight)) / ((double) Math.min(this.mLayoutWidth, this.mLayoutHeight));
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (true) {
            if (i2 >= MAX_SUPPORTED_PREVIEW_SIZE.length) {
                break;
            }
            int i5 = i4;
            int i6 = i3;
            for (int i7 = 0; i7 < this.mSupportedPreviewSize.size(); i7++) {
                String str = this.mSupportedPreviewSize.get(i7);
                int iIndexOf = str.indexOf(120);
                int i8 = Integer.parseInt(str.substring(0, iIndexOf));
                int i9 = Integer.parseInt(str.substring(iIndexOf + 1));
                double d = ((double) i8) / ((double) i9);
                if (i8 % 32 == 0 && (i = i8 * i9) <= MAX_SUPPORTED_PREVIEW_SIZE[i2] && Math.abs(dMax - d) <= 0.02d && i > i6 * i5) {
                    i5 = i9;
                    i6 = i8;
                }
            }
            if (i6 == 0 || i5 == 0) {
                i2++;
                i3 = i6;
                i4 = i5;
            } else {
                i3 = i6;
                i4 = i5;
                break;
            }
        }
        this.mPreviewWidth = i3;
        this.mPreviewHeight = i4;
        LogHelper.d(TAG, "[computeSuitablePreviewSize], preview size for matrix display, Width:" + this.mPreviewWidth + ", mPreviewHeight:" + this.mPreviewHeight);
    }
}
