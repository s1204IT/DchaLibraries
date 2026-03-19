package com.mediatek.camera.ui.preview;

import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class TextureViewController implements IController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(TextureViewController.class.getSimpleName());
    private IApp mApp;
    private ViewGroup mLastPreviewContainer;
    private View.OnLayoutChangeListener mOnLayoutChangeListener;
    private View.OnTouchListener mOnTouchListener;
    private ViewGroup mPreviewContainer;
    private ViewGroup mPreviewRoot;
    private SurfaceChangeCallback mSurfaceChangeCallback;
    private PreviewTextureView mTextureView;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private double mPreviewAspectRatio = 0.0d;
    private BlockingQueue<View> mFrameLayoutQueue = new LinkedBlockingQueue();
    private boolean mIsSurfaceCreated = false;

    public TextureViewController(IApp iApp) {
        this.mApp = iApp;
        this.mPreviewRoot = (ViewGroup) this.mApp.getActivity().findViewById(R.id.preview_frame_root);
    }

    @Override
    public void updatePreviewSize(int i, int i2, IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
        LogHelper.i(TAG, "updatePreviewSize: new size (" + i + " , " + i2 + " ) current size (" + this.mPreviewWidth + " , " + this.mPreviewHeight + " ),mIsSurfaceCreated = " + this.mIsSurfaceCreated + " listener = " + iSurfaceStatusListener);
        if (this.mPreviewWidth == i && this.mPreviewHeight == i2) {
            IAppUiListener.ISurfaceStatusListener bindStatusListener = this.mSurfaceChangeCallback.getBindStatusListener();
            if (iSurfaceStatusListener != null && iSurfaceStatusListener != bindStatusListener) {
                this.mTextureView.setSurfaceTextureListener(null);
                this.mSurfaceChangeCallback = new SurfaceChangeCallback(iSurfaceStatusListener);
                this.mTextureView.setSurfaceTextureListener(this.mSurfaceChangeCallback);
            }
            if (this.mIsSurfaceCreated && iSurfaceStatusListener != null && this.mTextureView.isAvailable()) {
                this.mTextureView.getSurfaceTexture().setDefaultBufferSize(this.mPreviewWidth, this.mPreviewHeight);
                iSurfaceStatusListener.surfaceAvailable(this.mTextureView.getSurfaceTexture(), this.mPreviewWidth, this.mPreviewHeight);
                return;
            }
            return;
        }
        if (((double) Math.max(i, i2)) / ((double) Math.min(i, i2)) == this.mPreviewAspectRatio) {
            this.mPreviewWidth = i;
            this.mPreviewHeight = i2;
            if (this.mTextureView.isAvailable()) {
                this.mTextureView.getSurfaceTexture().setDefaultBufferSize(this.mPreviewWidth, this.mPreviewHeight);
            }
            if (iSurfaceStatusListener != null) {
                iSurfaceStatusListener.surfaceAvailable(this.mTextureView.getSurfaceTexture(), this.mPreviewWidth, this.mPreviewHeight);
                if (iSurfaceStatusListener != this.mSurfaceChangeCallback.getBindStatusListener()) {
                    this.mTextureView.setSurfaceTextureListener(null);
                    this.mSurfaceChangeCallback = new SurfaceChangeCallback(iSurfaceStatusListener);
                    this.mTextureView.setSurfaceTextureListener(this.mSurfaceChangeCallback);
                    return;
                }
                return;
            }
            return;
        }
        if (this.mPreviewAspectRatio != 0.0d) {
            this.mLastPreviewContainer = this.mPreviewContainer;
            this.mTextureView = null;
        }
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
        this.mPreviewAspectRatio = ((double) Math.max(i, i2)) / ((double) Math.min(i, i2));
        if (this.mTextureView == null) {
            attachTextureView(iSurfaceStatusListener);
        } else {
            IAppUiListener.ISurfaceStatusListener bindStatusListener2 = this.mSurfaceChangeCallback.getBindStatusListener();
            if (iSurfaceStatusListener != null && iSurfaceStatusListener != bindStatusListener2) {
                this.mTextureView.setSurfaceTextureListener(null);
                this.mSurfaceChangeCallback = new SurfaceChangeCallback(iSurfaceStatusListener);
                this.mTextureView.setSurfaceTextureListener(this.mSurfaceChangeCallback);
                iSurfaceStatusListener.surfaceAvailable(this.mTextureView.getSurfaceTexture(), this.mPreviewWidth, this.mPreviewHeight);
            }
        }
        this.mTextureView.setAspectRatio(this.mPreviewAspectRatio);
    }

    @Override
    public void removeTopSurface() {
        int size = this.mFrameLayoutQueue.size();
        LogHelper.d(TAG, "removeTopSurface size = " + size);
        for (int i = 0; i < size; i++) {
            View viewPoll = this.mFrameLayoutQueue.poll();
            if (viewPoll != null) {
                viewPoll.setVisibility(8);
                this.mPreviewRoot.removeView(viewPoll);
            }
        }
        this.mLastPreviewContainer = null;
    }

    @Override
    public void setOnLayoutChangeListener(View.OnLayoutChangeListener onLayoutChangeListener) {
        this.mOnLayoutChangeListener = onLayoutChangeListener;
    }

    @Override
    public void setOnTouchListener(View.OnTouchListener onTouchListener) {
        this.mOnTouchListener = onTouchListener;
    }

    @Override
    public void clearPreviewStatusListener(IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
    }

    @Override
    public void setEnabled(boolean z) {
        if (this.mTextureView != null) {
            this.mTextureView.setEnabled(z);
        }
    }

    @Override
    public void onPause() {
    }

    private void attachTextureView(IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
        ViewGroup viewGroup = (ViewGroup) this.mApp.getActivity().getLayoutInflater().inflate(R.layout.textureview_layout, (ViewGroup) null);
        PreviewTextureView previewTextureView = (PreviewTextureView) viewGroup.findViewById(R.id.preview_surface);
        if (this.mLastPreviewContainer != null) {
            TextureView textureView = (TextureView) this.mLastPreviewContainer.findViewById(R.id.preview_surface);
            textureView.removeOnLayoutChangeListener(this.mOnLayoutChangeListener);
            textureView.setSurfaceTextureListener(null);
            previewTextureView.setOnTouchListener(null);
            if (this.mSurfaceChangeCallback != null) {
                this.mSurfaceChangeCallback.onSurfaceTextureDestroyed(textureView.getSurfaceTexture());
            }
            this.mLastPreviewContainer.bringToFront();
            if (!this.mFrameLayoutQueue.contains(this.mLastPreviewContainer)) {
                this.mFrameLayoutQueue.add(this.mLastPreviewContainer);
            }
        }
        this.mSurfaceChangeCallback = new SurfaceChangeCallback(iSurfaceStatusListener);
        previewTextureView.setSurfaceTextureListener(this.mSurfaceChangeCallback);
        previewTextureView.addOnLayoutChangeListener(this.mOnLayoutChangeListener);
        previewTextureView.setOnTouchListener(this.mOnTouchListener);
        this.mPreviewRoot.addView(viewGroup, 0);
        this.mTextureView = previewTextureView;
        this.mPreviewContainer = viewGroup;
    }

    private class SurfaceChangeCallback implements TextureView.SurfaceTextureListener {
        private IAppUiListener.ISurfaceStatusListener mListener;

        SurfaceChangeCallback(IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
            this.mListener = iSurfaceStatusListener;
        }

        IAppUiListener.ISurfaceStatusListener getBindStatusListener() {
            return this.mListener;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
            TextureViewController.this.mIsSurfaceCreated = true;
            surfaceTexture.setDefaultBufferSize(TextureViewController.this.mPreviewWidth, TextureViewController.this.mPreviewHeight);
            if (this.mListener != null) {
                this.mListener.surfaceChanged(surfaceTexture, TextureViewController.this.mPreviewWidth, TextureViewController.this.mPreviewHeight);
            }
            LogHelper.d(TextureViewController.TAG, "onSurfaceTextureAvailable surface  = " + surfaceTexture + " width " + i + " height " + i2);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
            if (this.mListener != null) {
                this.mListener.surfaceChanged(surfaceTexture, i, i2);
            }
            LogHelper.d(TextureViewController.TAG, "onSurfaceTextureSizeChanged surface  = " + surfaceTexture + " width " + i + " height " + i2);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            TextureViewController.this.mIsSurfaceCreated = false;
            if (this.mListener != null) {
                this.mListener.surfaceDestroyed(surfaceTexture, TextureViewController.this.mPreviewWidth, TextureViewController.this.mPreviewHeight);
            }
            LogHelper.d(TextureViewController.TAG, "onSurfaceTextureDestroyed surface  = " + surfaceTexture);
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    }
}
