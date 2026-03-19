package com.mediatek.camera.ui.photo;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.IReviewUI;
import com.mediatek.camera.common.mode.photo.intent.IIntentPhotoUi;

public class IntentPhotoUi implements IIntentPhotoUi {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(IntentPhotoUi.class.getSimpleName());
    private Activity mActivity;
    private IAppUi mIAppUi;
    private IReviewUI mIReviewUI;
    private boolean mIsShown;
    private IIntentPhotoUi.OkButtonClickListener mOkButtonClickListener;
    private IIntentPhotoUi.RetakeButtonClickListener mRetakeButtonClickListener;
    private IReviewUI.ReviewSpec mReviewSpec;
    private ViewGroup mViewGroup;
    private View.OnClickListener mOkButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.d(IntentPhotoUi.TAG, "[mOkButtonListener]");
            if (IntentPhotoUi.this.mOkButtonClickListener != null) {
                IntentPhotoUi.this.mOkButtonClickListener.onOkClickClicked();
            }
        }
    };
    private View.OnClickListener mRetakeListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.d(IntentPhotoUi.TAG, "[mRetakeListener]");
            IntentPhotoUi.this.mIsShown = false;
            IntentPhotoUi.this.mIReviewUI.hideReviewUI();
            IntentPhotoUi.this.mIAppUi.applyAllUIVisibility(0);
            if (IntentPhotoUi.this.mRetakeButtonClickListener != null) {
                IntentPhotoUi.this.mRetakeButtonClickListener.onRetakeClicked();
            }
        }
    };

    public IntentPhotoUi(Activity activity, ViewGroup viewGroup, IAppUi iAppUi) {
        LogHelper.i(TAG, "[IntentPhotoUi] Construct");
        this.mActivity = activity;
        this.mViewGroup = viewGroup;
        this.mIAppUi = iAppUi;
        this.mIReviewUI = iAppUi.getReviewUI();
        this.mReviewSpec = new IReviewUI.ReviewSpec();
    }

    @Override
    public void onPictureReceived(byte[] bArr) {
        LogHelper.d(TAG, "[onPictureReceived]");
        this.mIAppUi.applyAllUIVisibility(4);
        this.mIReviewUI.initReviewUI(this.mReviewSpec);
        this.mIsShown = true;
        this.mIReviewUI.showReviewUI(null);
    }

    @Override
    public void setOkButtonClickListener(IIntentPhotoUi.OkButtonClickListener okButtonClickListener) {
        this.mOkButtonClickListener = okButtonClickListener;
        this.mReviewSpec.saveListener = this.mOkButtonListener;
    }

    @Override
    public void setRetakeButtonClickListener(IIntentPhotoUi.RetakeButtonClickListener retakeButtonClickListener) {
        this.mRetakeButtonClickListener = retakeButtonClickListener;
        this.mReviewSpec.retakeListener = this.mRetakeListener;
    }

    @Override
    public void hide() {
        this.mIsShown = false;
        this.mIReviewUI.hideReviewUI();
    }

    @Override
    public boolean isShown() {
        return this.mIsShown;
    }

    @Override
    public void onOrientationChanged(int i) {
        this.mIReviewUI.updateOrientation(i);
    }
}
