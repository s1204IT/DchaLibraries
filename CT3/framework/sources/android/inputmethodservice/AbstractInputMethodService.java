package android.inputmethodservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
        public void createSession(InputMethod.SessionCallback callback) {
            callback.sessionCreated(AbstractInputMethodService.this.onCreateInputMethodSessionInterface());
        }

        @Override
        public void setSessionEnabled(InputMethodSession session, boolean enabled) {
            ((AbstractInputMethodSessionImpl) session).setEnabled(enabled);
        }

        @Override
        public void revokeSession(InputMethodSession session) {
            ((AbstractInputMethodSessionImpl) session).revokeSelf();
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

        public void setEnabled(boolean enabled) {
            if (this.mRevoked) {
                return;
            }
            this.mEnabled = enabled;
        }

        public void revokeSelf() {
            this.mRevoked = true;
            this.mEnabled = false;
        }

        @Override
        public void dispatchKeyEvent(int seq, KeyEvent event, InputMethodSession.EventCallback callback) {
            boolean handled = event.dispatch(AbstractInputMethodService.this, AbstractInputMethodService.this.mDispatcherState, this);
            if (callback == null) {
                return;
            }
            callback.finishedEvent(seq, handled);
        }

        @Override
        public void dispatchTrackballEvent(int seq, MotionEvent event, InputMethodSession.EventCallback callback) {
            boolean handled = AbstractInputMethodService.this.onTrackballEvent(event);
            if (callback == null) {
                return;
            }
            callback.finishedEvent(seq, handled);
        }

        @Override
        public void dispatchGenericMotionEvent(int seq, MotionEvent event, InputMethodSession.EventCallback callback) {
            boolean handled = AbstractInputMethodService.this.onGenericMotionEvent(event);
            if (callback == null) {
                return;
            }
            callback.finishedEvent(seq, handled);
        }
    }

    public KeyEvent.DispatcherState getKeyDispatcherState() {
        return this.mDispatcherState;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
    }

    @Override
    public final IBinder onBind(Intent intent) {
        if (this.mInputMethod == null) {
            this.mInputMethod = onCreateInputMethodInterface();
        }
        return new IInputMethodWrapper(this, this.mInputMethod);
    }

    public boolean onTrackballEvent(MotionEvent event) {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return false;
    }
}
