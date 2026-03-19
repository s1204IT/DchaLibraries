package com.mediatek.camera.feature.mode.panorama;

import android.graphics.Matrix;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.widget.Rotatable;
import com.mediatek.camera.common.widget.RotateImageView;
import com.mediatek.camera.common.widget.RotateLayout;

public class PanoramaView {
    private AnimationController mAnimationController;
    private IApp mApp;
    private int mCameraId;
    private RotateImageView mCancelButton;
    private ViewGroup mCenterIndicator;
    private ViewGroup mCollimatedArrowsDrawable;
    private int mDisplayOrientation;
    private IAppUi.HintInfo mGuideHint;
    private NaviLineImageView mNaviLine;
    private OnCancelButtonClickedListener mOnCancleButtonClickedListener;
    private OnSaveButtonClickedListener mOnSaveButtonClickedListener;
    private View mPanoView;
    private ViewGroup mParentViewGroup;
    private ProgressIndicator mProgressIndicator;
    private View mRootView;
    private RotateImageView mSaveButton;
    private RotateLayout mScreenProgressLayout;
    private Matrix[] mSensorMatrix;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PanoramaView.class.getSimpleName());
    private static final int[] DIRECTIONS = {0, 3, 1, 2};
    private static final int DIRECTIONS_COUNT = DIRECTIONS.length;
    private ViewGroup[] mDirectionSigns = new ViewGroup[4];
    private boolean mIsCapturing = false;
    private Matrix mDisplayMatrix = new Matrix();
    private int mSensorDirection = 4;
    private int mHalfArrowHeight = 0;
    private int mHalfArrowLength = 0;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private int[] mBlockSizes = {17, 15, 13, 12, 11, 12, 13, 15, 17};
    private int mDistanceHorizontal = 0;
    private int mDistanceVertical = 0;
    private RotateLayout.OnSizeChangedListener mOnSizeChangedListener = new RotateLayout.OnSizeChangedListener() {
        @Override
        public void onSizeChanged(int i, int i2) {
            LogHelper.d(PanoramaView.TAG, "[onSizeChanged]width=" + i + " height=" + i2);
            PanoramaView.this.mPreviewWidth = Math.max(i, i2);
            PanoramaView.this.mPreviewHeight = Math.min(i, i2);
        }
    };

    interface OnCancelButtonClickedListener {
        void onCancelButtonClicked();
    }

    interface OnSaveButtonClickedListener {
        void onSaveButtonClicked();
    }

    public PanoramaView(IApp iApp, int i) {
        this.mCameraId = 0;
        LogHelper.d(TAG, "[PanoramaView]constructor...");
        this.mApp = iApp;
        this.mCameraId = i;
        this.mParentViewGroup = this.mApp.getAppUi().getModeRootView();
    }

    public void init() {
        LogHelper.i(TAG, "[init]");
        getView();
        this.mGuideHint = new IAppUi.HintInfo();
        int identifier = this.mApp.getActivity().getResources().getIdentifier("hint_text_background", "drawable", this.mApp.getActivity().getPackageName());
        this.mGuideHint.mBackground = this.mApp.getActivity().getDrawable(identifier);
        this.mGuideHint.mType = IAppUi.HintType.TYPE_AUTO_HIDE;
        this.mGuideHint.mDelayTime = 5000;
    }

    public void show() {
        LogHelper.i(TAG, "[show]");
        if (this.mRootView == null) {
            this.mRootView = getView();
        }
        int displayOrientation = CameraUtil.getDisplayOrientation(CameraUtil.getDisplayRotation(this.mApp.getActivity()), this.mCameraId, this.mApp.getActivity());
        int requestedOrientation = this.mApp.getActivity().getRequestedOrientation();
        if (requestedOrientation == 0 || 8 == requestedOrientation) {
            this.mDisplayOrientation = displayOrientation;
        } else {
            this.mDisplayOrientation = displayOrientation + 90;
        }
        setSaveCancelButtonOrientation(this.mApp.getGSensorOrientation());
        this.mRootView.setVisibility(0);
        showCaptureView();
        this.mApp.getAppUi().applyAllUIVisibility(4);
        this.mApp.getAppUi().setUIVisibility(8, 0);
    }

    public void hide() {
        LogHelper.i(TAG, "[hide]");
        if (this.mRootView == null) {
            return;
        }
        this.mRootView.setVisibility(8);
        this.mApp.getAppUi().applyAllUIVisibility(0);
    }

    public void unInit() {
        LogHelper.i(TAG, "[unInit]");
        if (this.mParentViewGroup != null) {
            this.mParentViewGroup.removeView(this.mRootView);
            this.mRootView = null;
            this.mParentViewGroup = null;
        }
        this.mApp.getAppUi().hideScreenHint(this.mGuideHint);
    }

    public void reset() {
        LogHelper.i(TAG, "[reset] mRootView = " + this.mRootView + ",mPanoView = " + this.mPanoView);
        if (this.mRootView == null) {
            return;
        }
        this.mPanoView.setVisibility(8);
        this.mAnimationController.stopCenterAnimation();
        this.mCenterIndicator.setVisibility(8);
        this.mSensorDirection = 4;
        this.mNaviLine.setVisibility(8);
        this.mCollimatedArrowsDrawable.setVisibility(8);
        for (int i = 0; i < 4; i++) {
            this.mDirectionSigns[i].setSelected(false);
            this.mDirectionSigns[i].setVisibility(0);
        }
    }

    public void setSaveButtonClickedListener(OnSaveButtonClickedListener onSaveButtonClickedListener) {
        this.mOnSaveButtonClickedListener = onSaveButtonClickedListener;
        this.mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogHelper.i(PanoramaView.TAG, "save button clicked, mIsCapturing = " + PanoramaView.this.mIsCapturing);
                if (PanoramaView.this.mOnSaveButtonClickedListener != null && PanoramaView.this.mIsCapturing) {
                    PanoramaView.this.mOnSaveButtonClickedListener.onSaveButtonClicked();
                }
            }
        });
    }

    public void setCancelButtonClickedListener(OnCancelButtonClickedListener onCancelButtonClickedListener) {
        this.mOnCancleButtonClickedListener = onCancelButtonClickedListener;
        this.mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogHelper.i(PanoramaView.TAG, "cancel button clicked, mIsCapturing = " + PanoramaView.this.mIsCapturing);
                if (PanoramaView.this.mOnCancleButtonClickedListener != null && PanoramaView.this.mIsCapturing) {
                    PanoramaView.this.mOnCancleButtonClickedListener.onCancelButtonClicked();
                }
            }
        });
    }

    public void update(int i, Object... objArr) {
        LogHelper.d(TAG, "[update] type =" + i);
        switch (i) {
            case 0:
                setViewsForNext(Integer.parseInt(objArr[0].toString()));
                break;
            case Camera2Proxy.TEMPLATE_PREVIEW:
                if (objArr[0] != null && objArr[1] != null && objArr[2] != null && this.mRootView != null && this.mRootView.isShown()) {
                    updateMovingUI(Integer.parseInt(objArr[0].toString()), Integer.parseInt(objArr[1].toString()), Boolean.parseBoolean(objArr[2].toString()));
                    break;
                }
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                startCenterAnimation();
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                this.mIsCapturing = true;
                break;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                this.mIsCapturing = false;
                break;
        }
    }

    public void showGuideView(int i) {
        int i2;
        switch (i) {
            case 0:
                i2 = R.string.panorama_guide_shutter;
                break;
            case Camera2Proxy.TEMPLATE_PREVIEW:
                i2 = R.string.panorama_guide_choose_direction;
                break;
            default:
                i2 = 0;
                break;
        }
        if (i2 != 0) {
            this.mGuideHint.mHintText = this.mApp.getActivity().getString(i2);
            this.mApp.getAppUi().showScreenHint(this.mGuideHint);
        }
    }

    private View getView() {
        this.mRootView = this.mApp.getActivity().getLayoutInflater().inflate(R.layout.pano_preview, this.mParentViewGroup, true).findViewById(R.id.pano_frame_layout);
        initializeViewManager();
        return this.mRootView;
    }

    private void initializeViewManager() {
        this.mPanoView = this.mRootView.findViewById(R.id.pano_view);
        this.mScreenProgressLayout = (RotateLayout) this.mRootView.findViewById(R.id.on_screen_progress);
        this.mCenterIndicator = (ViewGroup) this.mRootView.findViewById(R.id.center_indicator);
        this.mDirectionSigns[0] = (ViewGroup) this.mRootView.findViewById(R.id.pano_right);
        this.mDirectionSigns[1] = (ViewGroup) this.mRootView.findViewById(R.id.pano_left);
        this.mDirectionSigns[2] = (ViewGroup) this.mRootView.findViewById(R.id.pano_up);
        this.mDirectionSigns[3] = (ViewGroup) this.mRootView.findViewById(R.id.pano_down);
        this.mAnimationController = new AnimationController(this.mDirectionSigns, (ViewGroup) this.mCenterIndicator.getChildAt(0));
        this.mCancelButton = (RotateImageView) this.mRootView.findViewById(R.id.btn_pano_cancel);
        this.mSaveButton = (RotateImageView) this.mRootView.findViewById(R.id.btn_pano_save);
        this.mDistanceHorizontal = 160;
        this.mDistanceVertical = 120;
        this.mNaviLine = (NaviLineImageView) this.mRootView.findViewById(R.id.navi_line);
        this.mCollimatedArrowsDrawable = (ViewGroup) this.mRootView.findViewById(R.id.static_center_indicator);
        this.mProgressIndicator = new ProgressIndicator(this.mApp.getActivity(), 9, this.mBlockSizes);
        this.mProgressIndicator.setVisibility(8);
        this.mScreenProgressLayout.setOrientation(this.mDisplayOrientation, true);
        prepareSensorMatrix();
        this.mScreenProgressLayout.setOnSizeChangedListener(this.mOnSizeChangedListener);
    }

    private void prepareSensorMatrix() {
        this.mSensorMatrix = new Matrix[4];
        this.mSensorMatrix[1] = new Matrix();
        this.mSensorMatrix[1].setScale(-1.0f, -1.0f);
        this.mSensorMatrix[1].postTranslate(0.0f, this.mDistanceVertical);
        this.mSensorMatrix[0] = new Matrix();
        this.mSensorMatrix[0].setScale(-1.0f, -1.0f);
        this.mSensorMatrix[0].postTranslate(this.mDistanceHorizontal * 2, this.mDistanceVertical);
        this.mSensorMatrix[2] = new Matrix();
        this.mSensorMatrix[2].setScale(-1.0f, -1.0f);
        this.mSensorMatrix[2].postTranslate(this.mDistanceHorizontal, 0.0f);
        this.mSensorMatrix[3] = new Matrix();
        this.mSensorMatrix[3].setScale(-1.0f, -1.0f);
        this.mSensorMatrix[3].postTranslate(this.mDistanceHorizontal, this.mDistanceVertical * 2);
    }

    private void showCaptureView() {
        this.mCenterIndicator.setVisibility(8);
        this.mPanoView.setVisibility(0);
        this.mProgressIndicator.setProgress(0);
        this.mProgressIndicator.setVisibility(0);
    }

    private void setViewsForNext(int i) {
        this.mProgressIndicator.setProgress(i + 1);
        if (i == 0) {
            this.mAnimationController.startDirectionAnimation();
            return;
        }
        this.mNaviLine.setVisibility(4);
        this.mAnimationController.stopCenterAnimation();
        this.mCenterIndicator.setVisibility(8);
        this.mCollimatedArrowsDrawable.setVisibility(0);
    }

    private void updateMovingUI(int i, int i2, boolean z) {
        LogHelper.d(TAG, "[updateMovingUI] xy:" + i + ", direction:" + i2 + ", shown:" + z);
        if (i2 == 4 || z || this.mNaviLine.getWidth() == 0 || this.mNaviLine.getHeight() == 0) {
            this.mNaviLine.setVisibility(4);
        } else {
            updateUIShowingMatrix((short) (((-65536) & i) >> 16), (short) (i & 65535), i2);
        }
    }

    private void updateUIShowingMatrix(int i, int i2, int i3) {
        float[] fArr = {i, i2};
        this.mSensorMatrix[i3].mapPoints(fArr);
        LogHelper.v(TAG, "[updateUIShowingMatrix]Matrix x = " + fArr[0] + " y = " + fArr[1]);
        prepareTransformMatrix(i3);
        this.mDisplayMatrix.mapPoints(fArr);
        LogHelper.v(TAG, "[updateUIShowingMatrix]DisplayMatrix x = " + fArr[0] + " y = " + fArr[1]);
        int i4 = (int) fArr[0];
        int i5 = (int) fArr[1];
        this.mNaviLine.setLayoutPosition(i4 - this.mHalfArrowHeight, i5 - this.mHalfArrowLength, i4 + this.mHalfArrowHeight, i5 + this.mHalfArrowLength);
        updateDirection(i3);
        this.mNaviLine.setVisibility(0);
    }

    private void prepareTransformMatrix(int i) {
        this.mDisplayMatrix.reset();
        int i2 = this.mPreviewWidth >> 1;
        int i3 = this.mPreviewHeight >> 1;
        getArrowHL();
        float f = i2 - this.mHalfArrowLength;
        float f2 = i3 - this.mHalfArrowLength;
        this.mDisplayMatrix.postScale(f / this.mDistanceHorizontal, f2 / this.mDistanceVertical);
        int i4 = this.mDisplayOrientation;
        if (i4 != 0) {
            if (i4 == 90) {
                this.mDisplayMatrix.postTranslate(0.0f, (-f2) * 2.0f);
                this.mDisplayMatrix.postRotate(90.0f);
            } else if (i4 == 180) {
                this.mDisplayMatrix.postTranslate((-f) * 2.0f, (-f2) * 2.0f);
                this.mDisplayMatrix.postRotate(180.0f);
            } else if (i4 == 270) {
                this.mDisplayMatrix.postTranslate((-f) * 2.0f, 0.0f);
                this.mDisplayMatrix.postRotate(-90.0f);
            }
        }
        this.mDisplayMatrix.postTranslate(this.mHalfArrowLength, this.mHalfArrowLength);
    }

    private void getArrowHL() {
        if (this.mHalfArrowHeight == 0) {
            int width = this.mNaviLine.getWidth();
            int height = this.mNaviLine.getHeight();
            if (width > height) {
                this.mHalfArrowLength = width >> 1;
                this.mHalfArrowHeight = height >> 1;
            } else {
                this.mHalfArrowHeight = width >> 1;
                this.mHalfArrowLength = height >> 1;
            }
        }
    }

    private void updateDirection(int i) {
        LogHelper.d(TAG, "[updateDirection]mDisplayOrientation:" + this.mDisplayOrientation + ",mSensorDirection =" + this.mSensorDirection + ", direction = " + i);
        int i2 = 0;
        while (true) {
            if (i2 < DIRECTIONS_COUNT) {
                if (DIRECTIONS[i2] == i) {
                    break;
                } else {
                    i2++;
                }
            } else {
                i2 = 0;
                break;
            }
        }
        int i3 = this.mDisplayOrientation;
        if (i3 != 0) {
            if (i3 == 90) {
                i = DIRECTIONS[(i2 + 1) % DIRECTIONS_COUNT];
            } else if (i3 == 180) {
                i = DIRECTIONS[(i2 + 2) % DIRECTIONS_COUNT];
            } else if (i3 == 270) {
                i = DIRECTIONS[((i2 - 1) + DIRECTIONS_COUNT) % DIRECTIONS_COUNT];
            }
        }
        if (this.mSensorDirection != i) {
            this.mSensorDirection = i;
            if (this.mSensorDirection != 4) {
                setOrientationIndicator(i);
                this.mCenterIndicator.setVisibility(0);
                this.mAnimationController.startCenterAnimation();
                for (int i4 = 0; i4 < 4; i4++) {
                    this.mDirectionSigns[i4].setVisibility(4);
                }
                return;
            }
            this.mCenterIndicator.setVisibility(4);
        }
    }

    private void setOrientationIndicator(int i) {
        LogHelper.d(TAG, "[setOrientationIndicator]direction = " + i);
        if (i == 0) {
            ((Rotatable) this.mCollimatedArrowsDrawable).setOrientation(0, true);
            ((Rotatable) this.mCenterIndicator).setOrientation(0, true);
            this.mNaviLine.setRotation(-90.0f);
            return;
        }
        if (i == 1) {
            ((Rotatable) this.mCollimatedArrowsDrawable).setOrientation(180, true);
            ((Rotatable) this.mCenterIndicator).setOrientation(180, true);
            this.mNaviLine.setRotation(90.0f);
        } else if (i == 2) {
            ((Rotatable) this.mCollimatedArrowsDrawable).setOrientation(90, true);
            ((Rotatable) this.mCenterIndicator).setOrientation(90, true);
            this.mNaviLine.setRotation(180.0f);
        } else if (i == 3) {
            ((Rotatable) this.mCollimatedArrowsDrawable).setOrientation(270, true);
            ((Rotatable) this.mCenterIndicator).setOrientation(270, true);
            this.mNaviLine.setRotation(0.0f);
        }
    }

    private void startCenterAnimation() {
        this.mCollimatedArrowsDrawable.setVisibility(8);
        this.mAnimationController.startCenterAnimation();
        this.mCenterIndicator.setVisibility(0);
    }

    private void setSaveCancelButtonOrientation(final int i) {
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CameraUtil.rotateRotateLayoutChildView(PanoramaView.this.mApp.getActivity(), PanoramaView.this.mSaveButton, i, false);
                CameraUtil.rotateRotateLayoutChildView(PanoramaView.this.mApp.getActivity(), PanoramaView.this.mCancelButton, i, false);
            }
        });
    }
}
