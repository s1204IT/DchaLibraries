package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.os.Binder;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.concurrent.Executor;

public class CallbackProxies {

    public static class SessionStateCallbackProxy extends CameraCaptureSession.StateCallback {
        private final CameraCaptureSession.StateCallback mCallback;
        private final Executor mExecutor;

        public SessionStateCallbackProxy(Executor executor, CameraCaptureSession.StateCallback stateCallback) {
            this.mExecutor = (Executor) Preconditions.checkNotNull(executor, "executor must not be null");
            this.mCallback = (CameraCaptureSession.StateCallback) Preconditions.checkNotNull(stateCallback, "callback must not be null");
        }

        @Override
        public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mCallback.onConfigured(cameraCaptureSession);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mCallback.onConfigureFailed(cameraCaptureSession);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onReady(final CameraCaptureSession cameraCaptureSession) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mCallback.onReady(cameraCaptureSession);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onActive(final CameraCaptureSession cameraCaptureSession) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mCallback.onActive(cameraCaptureSession);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onCaptureQueueEmpty(final CameraCaptureSession cameraCaptureSession) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mCallback.onCaptureQueueEmpty(cameraCaptureSession);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onClosed(final CameraCaptureSession cameraCaptureSession) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mCallback.onClosed(cameraCaptureSession);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onSurfacePrepared(final CameraCaptureSession cameraCaptureSession, final Surface surface) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mCallback.onSurfacePrepared(cameraCaptureSession, surface);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private CallbackProxies() {
        throw new AssertionError();
    }
}
