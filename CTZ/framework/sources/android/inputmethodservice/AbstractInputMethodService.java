package android.inputmethodservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodSession;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class AbstractInputMethodService extends Service implements KeyEvent.Callback {
    final KeyEvent.DispatcherState mDispatcherState = new KeyEvent.DispatcherState();
    private InputMethod mInputMethod;

    public abstract AbstractInputMethodImpl onCreateInputMethodInterface();

    public abstract AbstractInputMethodSessionImpl onCreateInputMethodSessionInterface();

    public abstract class AbstractInputMethodImpl implements InputMethod {
        public AbstractInputMethodImpl() {
        }

        @Override
        public void createSession(InputMethod.SessionCallback sessionCallback) {
            sessionCallback.sessionCreated(AbstractInputMethodService.this.onCreateInputMethodSessionInterface());
        }

        @Override
        public void setSessionEnabled(InputMethodSession inputMethodSession, boolean z) {
            ((AbstractInputMethodSessionImpl) inputMethodSession).setEnabled(z);
        }

        @Override
        public void revokeSession(InputMethodSession inputMethodSession) {
            ((AbstractInputMethodSessionImpl) inputMethodSession).revokeSelf();
        }
    }

    public abstract class AbstractInputMethodSessionImpl implements InputMethodSession {
        boolean mEnabled = true;
        boolean mRevoked;

        public AbstractInputMethodSessionImpl() {
        }

        public boolean isEnabled() {
            return this.mEnabled;
        }

        public boolean isRevoked() {
            return this.mRevoked;
        }

        public void setEnabled(boolean z) {
            if (!this.mRevoked) {
                this.mEnabled = z;
            }
        }

        public void revokeSelf() {
            this.mRevoked = true;
            this.mEnabled = false;
        }

        @Override
        public void dispatchKeyEvent(int i, KeyEvent keyEvent, InputMethodSession.EventCallback eventCallback) {
            boolean zDispatch = keyEvent.dispatch(AbstractInputMethodService.this, AbstractInputMethodService.this.mDispatcherState, this);
            if (eventCallback != null) {
                eventCallback.finishedEvent(i, zDispatch);
            }
        }

        @Override
        public void dispatchTrackballEvent(int i, MotionEvent motionEvent, InputMethodSession.EventCallback eventCallback) {
            boolean zOnTrackballEvent = AbstractInputMethodService.this.onTrackballEvent(motionEvent);
            if (eventCallback != null) {
                eventCallback.finishedEvent(i, zOnTrackballEvent);
            }
        }

        @Override
        public void dispatchGenericMotionEvent(int i, MotionEvent motionEvent, InputMethodSession.EventCallback eventCallback) {
            boolean zOnGenericMotionEvent = AbstractInputMethodService.this.onGenericMotionEvent(motionEvent);
            if (eventCallback != null) {
                eventCallback.finishedEvent(i, zOnGenericMotionEvent);
            }
        }
    }

    public KeyEvent.DispatcherState getKeyDispatcherState() {
        return this.mDispatcherState;
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    @Override
    public final IBinder onBind(Intent intent) {
        if (this.mInputMethod == null) {
            this.mInputMethod = onCreateInputMethodInterface();
        }
        return new IInputMethodWrapper(this, this.mInputMethod);
    }

    public boolean onTrackballEvent(MotionEvent motionEvent) {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return false;
    }

    public void exposeContent(InputContentInfo inputContentInfo, InputConnection inputConnection) {
    }
}
