package android.media.session;

import android.content.ComponentName;
import android.content.Context;
import android.media.IRemoteVolumeController;
import android.media.session.IActiveSessionsListener;
import android.media.session.ISessionController;
import android.media.session.ISessionManager;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public final class MediaSessionManager {
    public static final int DIRECTION_MUTE = -99;
    private static final String TAG = "SessionManager";
    private Context mContext;
    private final ArrayMap<OnActiveSessionsChangedListener, SessionsChangedWrapper> mListeners = new ArrayMap<>();
    private final Object mLock = new Object();
    private final ISessionManager mService;

    public interface OnActiveSessionsChangedListener {
        void onActiveSessionsChanged(List<MediaController> list);
    }

    public MediaSessionManager(Context context) {
        this.mContext = context;
        IBinder b = ServiceManager.getService(Context.MEDIA_SESSION_SERVICE);
        this.mService = ISessionManager.Stub.asInterface(b);
    }

    public ISession createSession(MediaSession.CallbackStub cbStub, String tag, int userId) throws RemoteException {
        return this.mService.createSession(this.mContext.getPackageName(), cbStub, tag, userId);
    }

    public List<MediaController> getActiveSessions(ComponentName notificationListener) {
        return getActiveSessionsForUser(notificationListener, UserHandle.myUserId());
    }

    public List<MediaController> getActiveSessionsForUser(ComponentName notificationListener, int userId) {
        ArrayList<MediaController> controllers = new ArrayList<>();
        try {
            List<IBinder> binders = this.mService.getSessions(notificationListener, userId);
            int size = binders.size();
            for (int i = 0; i < size; i++) {
                MediaController controller = new MediaController(this.mContext, ISessionController.Stub.asInterface(binders.get(i)));
                controllers.add(controller);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get active sessions: ", e);
        }
        return controllers;
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener sessionListener, ComponentName notificationListener) {
        addOnActiveSessionsChangedListener(sessionListener, notificationListener, null);
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener sessionListener, ComponentName notificationListener, Handler handler) {
        addOnActiveSessionsChangedListener(sessionListener, notificationListener, UserHandle.myUserId(), handler);
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener sessionListener, ComponentName notificationListener, int userId, Handler handler) {
        if (sessionListener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        if (handler == null) {
            handler = new Handler();
        }
        synchronized (this.mLock) {
            if (this.mListeners.get(sessionListener) != null) {
                Log.w(TAG, "Attempted to add session listener twice, ignoring.");
                return;
            }
            SessionsChangedWrapper wrapper = new SessionsChangedWrapper(sessionListener, handler);
            try {
                this.mService.addSessionsListener(wrapper.mStub, notificationListener, userId);
                this.mListeners.put(sessionListener, wrapper);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in addOnActiveSessionsChangedListener.", e);
            }
        }
    }

    public void removeOnActiveSessionsChangedListener(OnActiveSessionsChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        synchronized (this.mLock) {
            SessionsChangedWrapper wrapper = this.mListeners.remove(listener);
            if (wrapper != null) {
                try {
                    this.mService.removeSessionsListener(wrapper.mStub);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error in removeOnActiveSessionsChangedListener.", e);
                }
            }
        }
    }

    public void setRemoteVolumeController(IRemoteVolumeController rvc) {
        try {
            this.mService.setRemoteVolumeController(rvc);
        } catch (RemoteException e) {
            Log.e(TAG, "Error in setRemoteVolumeController.", e);
        }
    }

    public void dispatchMediaKeyEvent(KeyEvent keyEvent) {
        dispatchMediaKeyEvent(keyEvent, false);
    }

    public void dispatchMediaKeyEvent(KeyEvent keyEvent, boolean needWakeLock) {
        try {
            this.mService.dispatchMediaKeyEvent(keyEvent, needWakeLock);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send key event.", e);
        }
    }

    public void dispatchAdjustVolume(int suggestedStream, int direction, int flags) {
        try {
            this.mService.dispatchAdjustVolume(suggestedStream, direction, flags);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send adjust volume.", e);
        }
    }

    public boolean isGlobalPriorityActive() {
        try {
            return this.mService.isGlobalPriorityActive();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to check if the global priority is active.", e);
            return false;
        }
    }

    private final class SessionsChangedWrapper {
        private final Handler mHandler;
        private final OnActiveSessionsChangedListener mListener;
        private final IActiveSessionsListener.Stub mStub = new IActiveSessionsListener.Stub() {
            @Override
            public void onActiveSessionsChanged(final List<MediaSession.Token> tokens) {
                if (SessionsChangedWrapper.this.mHandler != null) {
                    SessionsChangedWrapper.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<MediaController> controllers = new ArrayList<>();
                            int size = tokens.size();
                            for (int i = 0; i < size; i++) {
                                controllers.add(new MediaController(MediaSessionManager.this.mContext, (MediaSession.Token) tokens.get(i)));
                            }
                            SessionsChangedWrapper.this.mListener.onActiveSessionsChanged(controllers);
                        }
                    });
                }
            }
        };

        public SessionsChangedWrapper(OnActiveSessionsChangedListener listener, Handler handler) {
            this.mListener = listener;
            this.mHandler = handler;
        }
    }
}
