package com.android.server.wm;

import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy;
import com.android.server.UiThread;
import java.util.ArrayList;

public class PointerEventDispatcher extends InputEventReceiver {
    ArrayList<WindowManagerPolicy.PointerEventListener> mListeners;
    WindowManagerPolicy.PointerEventListener[] mListenersArray;

    public PointerEventDispatcher(InputChannel inputChannel) {
        super(inputChannel, UiThread.getHandler().getLooper());
        this.mListeners = new ArrayList<>();
        this.mListenersArray = new WindowManagerPolicy.PointerEventListener[0];
    }

    public void onInputEvent(InputEvent event) {
        WindowManagerPolicy.PointerEventListener[] listeners;
        try {
            if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0) {
                MotionEvent motionEvent = (MotionEvent) event;
                synchronized (this.mListeners) {
                    if (this.mListenersArray == null) {
                        this.mListenersArray = new WindowManagerPolicy.PointerEventListener[this.mListeners.size()];
                        this.mListeners.toArray(this.mListenersArray);
                    }
                    listeners = this.mListenersArray;
                }
                for (WindowManagerPolicy.PointerEventListener pointerEventListener : listeners) {
                    pointerEventListener.onPointerEvent(motionEvent);
                }
            }
        } finally {
            finishInputEvent(event, false);
        }
    }

    public void registerInputEventListener(WindowManagerPolicy.PointerEventListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                throw new IllegalStateException("registerInputEventListener: trying to register" + listener + " twice.");
            }
            this.mListeners.add(listener);
            this.mListenersArray = null;
        }
    }

    public void unregisterInputEventListener(WindowManagerPolicy.PointerEventListener listener) {
        synchronized (this.mListeners) {
            if (!this.mListeners.contains(listener)) {
                throw new IllegalStateException("registerInputEventListener: " + listener + " not registered.");
            }
            this.mListeners.remove(listener);
            this.mListenersArray = null;
        }
    }
}
