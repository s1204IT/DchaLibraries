package com.android.ex.camera2.portability;

import android.os.Handler;

public class CameraExceptionHandler {
    private CameraExceptionCallback mCallback;
    private Handler mHandler;

    public interface CameraExceptionCallback {
        void onCameraError(int i);

        void onCameraException(RuntimeException runtimeException, String str, int i, int i2);

        void onDispatchThreadException(RuntimeException runtimeException);
    }

    public CameraExceptionHandler(CameraExceptionCallback callback, Handler handler) {
        this.mCallback = new CameraExceptionCallback() {
            @Override
            public void onCameraError(int errorCode) {
            }

            @Override
            public void onCameraException(RuntimeException e, String commandHistory, int action, int state) {
                throw e;
            }

            @Override
            public void onDispatchThreadException(RuntimeException e) {
                throw e;
            }
        };
        this.mHandler = handler;
        this.mCallback = callback;
    }

    public CameraExceptionHandler(Handler handler) {
        this.mCallback = new CameraExceptionCallback() {
            @Override
            public void onCameraError(int errorCode) {
            }

            @Override
            public void onCameraException(RuntimeException e, String commandHistory, int action, int state) {
                throw e;
            }

            @Override
            public void onDispatchThreadException(RuntimeException e) {
                throw e;
            }
        };
        this.mHandler = handler;
    }

    public void onCameraError(final int errorCode) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                CameraExceptionHandler.this.mCallback.onCameraError(errorCode);
            }
        });
    }

    public void onCameraException(final RuntimeException ex, final String commandHistory, final int action, final int state) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                CameraExceptionHandler.this.mCallback.onCameraException(ex, commandHistory, action, state);
            }
        });
    }

    public void onDispatchThreadException(final RuntimeException ex) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                CameraExceptionHandler.this.mCallback.onDispatchThreadException(ex);
            }
        });
    }
}
