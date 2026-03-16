package com.android.ex.camera2.utils;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;

public class Camera2CaptureCallbackForwarder extends CameraCaptureSession.CaptureCallback {
    private Handler mHandler;
    private CameraCaptureSession.CaptureCallback mListener;

    public Camera2CaptureCallbackForwarder(CameraCaptureSession.CaptureCallback listener, Handler handler) {
        this.mListener = listener;
        this.mHandler = handler;
    }

    @Override
    public void onCaptureCompleted(final CameraCaptureSession session, final CaptureRequest request, final TotalCaptureResult result) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureCompleted(session, request, result);
            }
        });
    }

    @Override
    public void onCaptureFailed(final CameraCaptureSession session, final CaptureRequest request, final CaptureFailure failure) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureFailed(session, request, failure);
            }
        });
    }

    @Override
    public void onCaptureProgressed(final CameraCaptureSession session, final CaptureRequest request, final CaptureResult partialResult) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureProgressed(session, request, partialResult);
            }
        });
    }

    @Override
    public void onCaptureSequenceAborted(final CameraCaptureSession session, final int sequenceId) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureSequenceAborted(session, sequenceId);
            }
        });
    }

    @Override
    public void onCaptureSequenceCompleted(final CameraCaptureSession session, final int sequenceId, final long frameNumber) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            }
        });
    }

    @Override
    public void onCaptureStarted(final CameraCaptureSession session, final CaptureRequest request, final long timestamp, final long frameNumber) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureStarted(session, request, timestamp, frameNumber);
            }
        });
    }
}
