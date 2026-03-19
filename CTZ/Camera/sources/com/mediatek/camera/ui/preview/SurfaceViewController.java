package com.mediatek.camera.ui.preview;

import android.app.Activity;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class SurfaceViewController implements IController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SurfaceViewController.class.getSimpleName());
    private IApp mApp;
    private ViewGroup mLastPreviewContainer;
    private View.OnLayoutChangeListener mOnLayoutChangeListener;
    private View.OnTouchListener mOnTouchListener;
    private ViewGroup mPreviewContainer;
    private ViewGroup mPreviewRoot;
    private Thread mProducerThread;
    private GLRendererImpl mRenderer;
    private SurfaceChangeCallback mSurfaceChangeCallback;
    private PreviewSurfaceView mSurfaceView;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private double mPreviewAspectRatio = 0.0d;
    private final Object mRenderSyncLock = new Object();
    private BlockingQueue<View> mFrameLayoutQueue = new LinkedBlockingQueue();
    private boolean mIsSurfaceCreated = false;

    public SurfaceViewController(IApp iApp) {
        this.mApp = iApp;
        this.mPreviewRoot = (ViewGroup) this.mApp.getActivity().findViewById(R.id.preview_frame_root);
        if (isThirdPartyIntent(this.mApp.getActivity())) {
            this.mApp.getActivity().findViewById(R.id.preview_cover).setVisibility(0);
        }
    }

    @Override
    public void updatePreviewSize(int i, int i2, IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
        LogHelper.i(TAG, "updatePreviewSize: new size (" + i + " , " + i2 + " ) current size (" + this.mPreviewWidth + " , " + this.mPreviewHeight + " ),mIsSurfaceCreated = " + this.mIsSurfaceCreated + " listener = " + iSurfaceStatusListener);
        if (this.mPreviewWidth == i && this.mPreviewHeight == i2) {
            IAppUiListener.ISurfaceStatusListener bindStatusListener = this.mSurfaceChangeCallback.getBindStatusListener();
            if (iSurfaceStatusListener != null && iSurfaceStatusListener != bindStatusListener) {
                this.mSurfaceView.getHolder().removeCallback(this.mSurfaceChangeCallback);
                this.mSurfaceChangeCallback = new SurfaceChangeCallback(iSurfaceStatusListener);
                this.mSurfaceView.getHolder().addCallback(this.mSurfaceChangeCallback);
            }
            if (this.mIsSurfaceCreated && iSurfaceStatusListener != null) {
                iSurfaceStatusListener.surfaceAvailable(this.mSurfaceView.getHolder(), this.mPreviewWidth, this.mPreviewHeight);
                return;
            }
            return;
        }
        if (this.mPreviewAspectRatio != 0.0d) {
            this.mLastPreviewContainer = this.mPreviewContainer;
            this.mSurfaceView = null;
        } else {
            double dMax = ((double) Math.max(i, i2)) / ((double) Math.min(i, i2));
            if (this.mSurfaceView != null && !this.mSurfaceView.isFullScreenPreview(dMax)) {
                this.mLastPreviewContainer = this.mPreviewContainer;
                this.mSurfaceView = null;
            }
        }
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
        this.mPreviewAspectRatio = ((double) Math.max(i, i2)) / ((double) Math.min(i, i2));
        if (this.mSurfaceView == null) {
            attachSurfaceView(iSurfaceStatusListener);
        } else {
            this.mSurfaceView.getHolder().removeCallback(this.mSurfaceChangeCallback);
            this.mSurfaceChangeCallback = new SurfaceChangeCallback(iSurfaceStatusListener);
            this.mSurfaceView.getHolder().addCallback(this.mSurfaceChangeCallback);
        }
        this.mSurfaceView.getHolder().setFixedSize(this.mPreviewWidth, this.mPreviewHeight);
        this.mSurfaceView.setAspectRatio(this.mPreviewAspectRatio);
    }

    @Override
    public void clearPreviewStatusListener(IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
        IAppUiListener.ISurfaceStatusListener bindStatusListener;
        if (this.mSurfaceChangeCallback != null && (bindStatusListener = this.mSurfaceChangeCallback.getBindStatusListener()) != null && bindStatusListener == iSurfaceStatusListener) {
            this.mSurfaceView.getHolder().removeCallback(this.mSurfaceChangeCallback);
            this.mSurfaceChangeCallback = new SurfaceChangeCallback(null);
            this.mSurfaceView.getHolder().addCallback(this.mSurfaceChangeCallback);
        }
    }

    @Override
    public void setEnabled(boolean z) {
        if (this.mSurfaceView != null) {
            this.mSurfaceView.setEnabled(z);
        }
    }

    @Override
    public void onPause() {
        if (!this.mApp.getActivity().isFinishing()) {
            if (this.mLastPreviewContainer != null) {
                this.mLastPreviewContainer.setVisibility(8);
                this.mPreviewRoot.removeView(this.mLastPreviewContainer);
            }
            if (this.mSurfaceView != null) {
                this.mLastPreviewContainer = this.mPreviewContainer;
                this.mSurfaceView = null;
                attachSurfaceView(this.mSurfaceChangeCallback.getBindStatusListener());
                this.mSurfaceView.getHolder().setFixedSize(this.mPreviewWidth, this.mPreviewHeight);
                this.mSurfaceView.setAspectRatio(this.mPreviewAspectRatio);
            }
        }
    }

    @Override
    public void removeTopSurface() {
        int size = this.mFrameLayoutQueue.size();
        LogHelper.d(TAG, "removeTopSurface size = " + size);
        if (isThirdPartyIntent(this.mApp.getActivity())) {
            this.mApp.getActivity().findViewById(R.id.preview_cover).setVisibility(8);
        }
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

    private void attachSurfaceView(IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
        ViewGroup viewGroup = (ViewGroup) this.mApp.getActivity().getLayoutInflater().inflate(R.layout.surfacepreview_layout, (ViewGroup) null);
        PreviewSurfaceView previewSurfaceView = (PreviewSurfaceView) viewGroup.findViewById(R.id.preview_surface);
        previewSurfaceView.setVisibility(8);
        if (this.mLastPreviewContainer != null) {
            SurfaceView surfaceView = (SurfaceView) this.mLastPreviewContainer.findViewById(R.id.preview_surface);
            surfaceView.removeOnLayoutChangeListener(this.mOnLayoutChangeListener);
            surfaceView.getHolder().removeCallback(this.mSurfaceChangeCallback);
            if (this.mSurfaceChangeCallback != null) {
                this.mSurfaceChangeCallback.surfaceDestroyed(surfaceView.getHolder());
            }
            this.mLastPreviewContainer.bringToFront();
            if (!this.mFrameLayoutQueue.contains(this.mLastPreviewContainer)) {
                this.mFrameLayoutQueue.add(this.mLastPreviewContainer);
            }
        }
        SurfaceHolder holder = previewSurfaceView.getHolder();
        this.mSurfaceChangeCallback = new SurfaceChangeCallback(iSurfaceStatusListener);
        holder.addCallback(this.mSurfaceChangeCallback);
        holder.setType(3);
        previewSurfaceView.addOnLayoutChangeListener(this.mOnLayoutChangeListener);
        previewSurfaceView.setOnTouchListener(this.mOnTouchListener);
        this.mPreviewRoot.addView(viewGroup, 0);
        this.mPreviewContainer = viewGroup;
        this.mSurfaceView = previewSurfaceView;
        this.mSurfaceView.setVisibility(0);
    }

    private class SurfaceChangeCallback implements SurfaceHolder.Callback {
        private IAppUiListener.ISurfaceStatusListener mListener;

        SurfaceChangeCallback(IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
            this.mListener = iSurfaceStatusListener;
        }

        IAppUiListener.ISurfaceStatusListener getBindStatusListener() {
            return this.mListener;
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            if (Build.VERSION.SDK_INT <= 26) {
                SurfaceViewController.this.mRenderer = new GLRendererImpl(SurfaceViewController.this.mApp.getActivity());
                SurfaceViewController.this.mProducerThread = new GLProducerThread(surfaceHolder.getSurface(), SurfaceViewController.this.mRenderer, SurfaceViewController.this.mRenderSyncLock);
                SurfaceViewController.this.mProducerThread.start();
                synchronized (SurfaceViewController.this.mRenderSyncLock) {
                    try {
                        SurfaceViewController.this.mRenderSyncLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            SurfaceViewController.this.mIsSurfaceCreated = true;
            if (this.mListener != null && i2 == SurfaceViewController.this.mPreviewWidth && i3 == SurfaceViewController.this.mPreviewHeight) {
                this.mListener.surfaceChanged(surfaceHolder, i2, i3);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            SurfaceViewController.this.mIsSurfaceCreated = false;
            if (this.mListener != null) {
                this.mListener.surfaceDestroyed(surfaceHolder, SurfaceViewController.this.mPreviewWidth, SurfaceViewController.this.mPreviewHeight);
            }
        }
    }

    private boolean isThirdPartyIntent(Activity activity) {
        String action = activity.getIntent().getAction();
        return "android.media.action.IMAGE_CAPTURE".equals(action) || "android.media.action.VIDEO_CAPTURE".equals(action);
    }
}
