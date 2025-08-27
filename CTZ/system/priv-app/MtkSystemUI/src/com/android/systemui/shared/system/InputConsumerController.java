package com.android.systemui.shared.system;

import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.BatchedInputEventReceiver;
import android.view.Choreographer;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.WindowManagerGlobal;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class InputConsumerController {
    private static final String TAG = InputConsumerController.class.getSimpleName();
    private InputEventReceiver mInputEventReceiver;
    private TouchListener mListener;
    private final String mName;
    private RegistrationListener mRegistrationListener;
    private final IBinder mToken = new Binder();
    private final IWindowManager mWindowManager;

    public interface RegistrationListener {
        void onRegistrationChanged(boolean z);
    }

    public interface TouchListener {
        boolean onTouchEvent(MotionEvent motionEvent);
    }

    private final class InputEventReceiver extends BatchedInputEventReceiver {
        public InputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper, Choreographer.getSfInstance());
        }

        public void onInputEvent(InputEvent inputEvent, int i) {
            boolean zOnTouchEvent = true;
            try {
                if (InputConsumerController.this.mListener != null && (inputEvent instanceof MotionEvent)) {
                    zOnTouchEvent = InputConsumerController.this.mListener.onTouchEvent((MotionEvent) inputEvent);
                }
            } finally {
                finishInputEvent(inputEvent, true);
            }
        }
    }

    public InputConsumerController(IWindowManager iWindowManager, String str) {
        this.mWindowManager = iWindowManager;
        this.mName = str;
    }

    public static InputConsumerController getPipInputConsumer() {
        return new InputConsumerController(WindowManagerGlobal.getWindowManagerService(), "pip_input_consumer");
    }

    public void setTouchListener(TouchListener touchListener) {
        this.mListener = touchListener;
    }

    public void setRegistrationListener(RegistrationListener registrationListener) {
        this.mRegistrationListener = registrationListener;
        if (this.mRegistrationListener != null) {
            this.mRegistrationListener.onRegistrationChanged(this.mInputEventReceiver != null);
        }
    }

    public boolean isRegistered() {
        return this.mInputEventReceiver != null;
    }

    public void registerInputConsumer() {
        if (this.mInputEventReceiver == null) {
            InputChannel inputChannel = new InputChannel();
            try {
                this.mWindowManager.destroyInputConsumer(this.mName);
                this.mWindowManager.createInputConsumer(this.mToken, this.mName, inputChannel);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to create input consumer", e);
            }
            this.mInputEventReceiver = new InputEventReceiver(inputChannel, Looper.myLooper());
            if (this.mRegistrationListener != null) {
                this.mRegistrationListener.onRegistrationChanged(true);
            }
        }
    }

    public void unregisterInputConsumer() {
        if (this.mInputEventReceiver != null) {
            try {
                this.mWindowManager.destroyInputConsumer(this.mName);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to destroy input consumer", e);
            }
            this.mInputEventReceiver.dispose();
            this.mInputEventReceiver = null;
            if (this.mRegistrationListener != null) {
                this.mRegistrationListener.onRegistrationChanged(false);
            }
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        String str2 = str + "  ";
        printWriter.println(str + TAG);
        StringBuilder sb = new StringBuilder();
        sb.append(str2);
        sb.append("registered=");
        sb.append(this.mInputEventReceiver != null);
        printWriter.println(sb.toString());
    }
}
