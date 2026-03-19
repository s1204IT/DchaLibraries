package android.view;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.view.IInputFilter;

public abstract class InputFilter extends IInputFilter.Stub {
    private static final int MSG_INPUT_EVENT = 3;
    private static final int MSG_INSTALL = 1;
    private static final int MSG_UNINSTALL = 2;
    private final H mH;
    private IInputFilterHost mHost;
    private final InputEventConsistencyVerifier mInboundInputEventConsistencyVerifier;
    private final InputEventConsistencyVerifier mOutboundInputEventConsistencyVerifier;

    public InputFilter(Looper looper) {
        InputEventConsistencyVerifier inputEventConsistencyVerifier;
        if (InputEventConsistencyVerifier.isInstrumentationEnabled()) {
            inputEventConsistencyVerifier = new InputEventConsistencyVerifier(this, 1, "InputFilter#InboundInputEventConsistencyVerifier");
        } else {
            inputEventConsistencyVerifier = null;
        }
        this.mInboundInputEventConsistencyVerifier = inputEventConsistencyVerifier;
        this.mOutboundInputEventConsistencyVerifier = InputEventConsistencyVerifier.isInstrumentationEnabled() ? new InputEventConsistencyVerifier(this, 1, "InputFilter#OutboundInputEventConsistencyVerifier") : null;
        this.mH = new H(looper);
    }

    @Override
    public final void install(IInputFilterHost iInputFilterHost) {
        this.mH.obtainMessage(1, iInputFilterHost).sendToTarget();
    }

    @Override
    public final void uninstall() {
        this.mH.obtainMessage(2).sendToTarget();
    }

    @Override
    public final void filterInputEvent(InputEvent inputEvent, int i) {
        this.mH.obtainMessage(3, i, 0, inputEvent).sendToTarget();
    }

    public void sendInputEvent(InputEvent inputEvent, int i) {
        if (inputEvent == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (this.mHost == null) {
            throw new IllegalStateException("Cannot send input event because the input filter is not installed.");
        }
        if (this.mOutboundInputEventConsistencyVerifier != null) {
            this.mOutboundInputEventConsistencyVerifier.onInputEvent(inputEvent, 0);
        }
        try {
            this.mHost.sendInputEvent(inputEvent, i);
        } catch (RemoteException e) {
        }
    }

    public void onInputEvent(InputEvent inputEvent, int i) {
        sendInputEvent(inputEvent, i);
    }

    public void onInstalled() {
    }

    public void onUninstalled() {
    }

    private final class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    InputFilter.this.mHost = (IInputFilterHost) message.obj;
                    if (InputFilter.this.mInboundInputEventConsistencyVerifier != null) {
                        InputFilter.this.mInboundInputEventConsistencyVerifier.reset();
                    }
                    if (InputFilter.this.mOutboundInputEventConsistencyVerifier != null) {
                        InputFilter.this.mOutboundInputEventConsistencyVerifier.reset();
                    }
                    InputFilter.this.onInstalled();
                    return;
                case 2:
                    try {
                        InputFilter.this.onUninstalled();
                        return;
                    } finally {
                        InputFilter.this.mHost = null;
                    }
                case 3:
                    InputEvent inputEvent = (InputEvent) message.obj;
                    try {
                        if (InputFilter.this.mInboundInputEventConsistencyVerifier != null) {
                            InputFilter.this.mInboundInputEventConsistencyVerifier.onInputEvent(inputEvent, 0);
                        }
                        InputFilter.this.onInputEvent(inputEvent, message.arg1);
                        return;
                    } finally {
                        inputEvent.recycle();
                    }
                default:
                    return;
            }
        }
    }
}
