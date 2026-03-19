package android.media.session;

import android.annotation.SystemApi;
import android.content.ComponentName;
import android.content.Context;
import android.media.IRemoteVolumeController;
import android.media.ISessionTokensListener;
import android.media.SessionToken2;
import android.media.session.IActiveSessionsListener;
import android.media.session.ICallback;
import android.media.session.IOnMediaKeyListener;
import android.media.session.IOnVolumeKeyLongPressListener;
import android.media.session.ISessionController;
import android.media.session.ISessionManager;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

public final class MediaSessionManager {
    public static final int RESULT_MEDIA_KEY_HANDLED = 1;
    public static final int RESULT_MEDIA_KEY_NOT_HANDLED = 0;
    private static final String TAG = "SessionManager";
    private CallbackImpl mCallback;
    private Context mContext;
    private OnMediaKeyListenerImpl mOnMediaKeyListener;
    private OnVolumeKeyLongPressListenerImpl mOnVolumeKeyLongPressListener;
    private final ArrayMap<OnActiveSessionsChangedListener, SessionsChangedWrapper> mListeners = new ArrayMap<>();
    private final ArrayMap<OnSessionTokensChangedListener, SessionTokensChangedWrapper> mSessionTokensListener = new ArrayMap<>();
    private final Object mLock = new Object();
    private final ISessionManager mService = ISessionManager.Stub.asInterface(ServiceManager.getService(Context.MEDIA_SESSION_SERVICE));

    public static abstract class Callback {
        public abstract void onAddressedPlayerChanged(ComponentName componentName);

        public abstract void onAddressedPlayerChanged(MediaSession.Token token);

        public abstract void onMediaKeyEventDispatched(KeyEvent keyEvent, ComponentName componentName);

        public abstract void onMediaKeyEventDispatched(KeyEvent keyEvent, MediaSession.Token token);
    }

    public interface OnActiveSessionsChangedListener {
        void onActiveSessionsChanged(List<MediaController> list);
    }

    @SystemApi
    public interface OnMediaKeyListener {
        boolean onMediaKey(KeyEvent keyEvent);
    }

    public interface OnSessionTokensChangedListener {
        void onSessionTokensChanged(List<SessionToken2> list);
    }

    @SystemApi
    public interface OnVolumeKeyLongPressListener {
        void onVolumeKeyLongPress(KeyEvent keyEvent);
    }

    public MediaSessionManager(Context context) {
        this.mContext = context;
    }

    public ISession createSession(MediaSession.CallbackStub callbackStub, String str, int i) throws RemoteException {
        return this.mService.createSession(this.mContext.getPackageName(), callbackStub, str, i);
    }

    public List<MediaController> getActiveSessions(ComponentName componentName) {
        return getActiveSessionsForUser(componentName, UserHandle.myUserId());
    }

    public List<MediaController> getActiveSessionsForUser(ComponentName componentName, int i) {
        ArrayList arrayList = new ArrayList();
        try {
            List<IBinder> sessions = this.mService.getSessions(componentName, i);
            int size = sessions.size();
            for (int i2 = 0; i2 < size; i2++) {
                arrayList.add(new MediaController(this.mContext, ISessionController.Stub.asInterface(sessions.get(i2))));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get active sessions: ", e);
        }
        return arrayList;
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener onActiveSessionsChangedListener, ComponentName componentName) {
        addOnActiveSessionsChangedListener(onActiveSessionsChangedListener, componentName, null);
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener onActiveSessionsChangedListener, ComponentName componentName, Handler handler) {
        addOnActiveSessionsChangedListener(onActiveSessionsChangedListener, componentName, UserHandle.myUserId(), handler);
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener onActiveSessionsChangedListener, ComponentName componentName, int i, Handler handler) {
        if (onActiveSessionsChangedListener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        if (handler == null) {
            handler = new Handler();
        }
        synchronized (this.mLock) {
            if (this.mListeners.get(onActiveSessionsChangedListener) != null) {
                Log.w(TAG, "Attempted to add session listener twice, ignoring.");
                return;
            }
            SessionsChangedWrapper sessionsChangedWrapper = new SessionsChangedWrapper(this.mContext, onActiveSessionsChangedListener, handler);
            try {
                this.mService.addSessionsListener(sessionsChangedWrapper.mStub, componentName, i);
                this.mListeners.put(onActiveSessionsChangedListener, sessionsChangedWrapper);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in addOnActiveSessionsChangedListener.", e);
            }
        }
    }

    public void removeOnActiveSessionsChangedListener(OnActiveSessionsChangedListener onActiveSessionsChangedListener) {
        if (onActiveSessionsChangedListener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        synchronized (this.mLock) {
            SessionsChangedWrapper sessionsChangedWrapperRemove = this.mListeners.remove(onActiveSessionsChangedListener);
            if (sessionsChangedWrapperRemove != null) {
                try {
                    try {
                        this.mService.removeSessionsListener(sessionsChangedWrapperRemove.mStub);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error in removeOnActiveSessionsChangedListener.", e);
                    }
                } finally {
                    sessionsChangedWrapperRemove.release();
                }
            }
        }
    }

    public void setRemoteVolumeController(IRemoteVolumeController iRemoteVolumeController) {
        try {
            this.mService.setRemoteVolumeController(iRemoteVolumeController);
        } catch (RemoteException e) {
            Log.e(TAG, "Error in setRemoteVolumeController.", e);
        }
    }

    public void dispatchMediaKeyEvent(KeyEvent keyEvent) {
        dispatchMediaKeyEvent(keyEvent, false);
    }

    public void dispatchMediaKeyEvent(KeyEvent keyEvent, boolean z) {
        dispatchMediaKeyEventInternal(false, keyEvent, z);
    }

    public void dispatchMediaKeyEventAsSystemService(KeyEvent keyEvent) {
        dispatchMediaKeyEventInternal(true, keyEvent, false);
    }

    private void dispatchMediaKeyEventInternal(boolean z, KeyEvent keyEvent, boolean z2) {
        try {
            this.mService.dispatchMediaKeyEvent(this.mContext.getPackageName(), z, keyEvent, z2);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send key event.", e);
        }
    }

    public void dispatchVolumeKeyEvent(KeyEvent keyEvent, int i, boolean z) {
        dispatchVolumeKeyEventInternal(false, keyEvent, i, z);
    }

    public void dispatchVolumeKeyEventAsSystemService(KeyEvent keyEvent, int i) {
        dispatchVolumeKeyEventInternal(true, keyEvent, i, false);
    }

    private void dispatchVolumeKeyEventInternal(boolean z, KeyEvent keyEvent, int i, boolean z2) {
        try {
            this.mService.dispatchVolumeKeyEvent(this.mContext.getPackageName(), z, keyEvent, i, z2);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send volume key event.", e);
        }
    }

    public void dispatchAdjustVolume(int i, int i2, int i3) {
        try {
            this.mService.dispatchAdjustVolume(this.mContext.getPackageName(), i, i2, i3);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send adjust volume.", e);
        }
    }

    public boolean isTrustedForMediaControl(RemoteUserInfo remoteUserInfo) {
        if (remoteUserInfo == null) {
            throw new IllegalArgumentException("userInfo may not be null");
        }
        if (remoteUserInfo.getPackageName() == null) {
            return false;
        }
        try {
            return this.mService.isTrusted(remoteUserInfo.getPackageName(), remoteUserInfo.getPid(), remoteUserInfo.getUid());
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
            return false;
        }
    }

    public boolean createSession2(SessionToken2 sessionToken2) {
        if (sessionToken2 == null) {
            return false;
        }
        try {
            return this.mService.createSession2(sessionToken2.toBundle());
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
            return false;
        }
    }

    public void destroySession2(SessionToken2 sessionToken2) {
        if (sessionToken2 == null) {
            return;
        }
        try {
            this.mService.destroySession2(sessionToken2.toBundle());
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
        }
    }

    public List<SessionToken2> getActiveSessionTokens() {
        try {
            return toTokenList(this.mService.getSessionTokens(true, false, this.mContext.getPackageName()));
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
            return Collections.emptyList();
        }
    }

    public List<SessionToken2> getSessionServiceTokens() {
        try {
            return toTokenList(this.mService.getSessionTokens(false, true, this.mContext.getPackageName()));
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
            return Collections.emptyList();
        }
    }

    public List<SessionToken2> getAllSessionTokens() {
        try {
            return toTokenList(this.mService.getSessionTokens(false, false, this.mContext.getPackageName()));
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
            return Collections.emptyList();
        }
    }

    public void addOnSessionTokensChangedListener(Executor executor, OnSessionTokensChangedListener onSessionTokensChangedListener) {
        addOnSessionTokensChangedListener(UserHandle.myUserId(), executor, onSessionTokensChangedListener);
    }

    public void addOnSessionTokensChangedListener(int i, Executor executor, OnSessionTokensChangedListener onSessionTokensChangedListener) {
        if (executor == null) {
            throw new IllegalArgumentException("executor may not be null");
        }
        if (onSessionTokensChangedListener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        synchronized (this.mLock) {
            if (this.mSessionTokensListener.get(onSessionTokensChangedListener) != null) {
                Log.w(TAG, "Attempted to add session listener twice, ignoring.");
                return;
            }
            SessionTokensChangedWrapper sessionTokensChangedWrapper = new SessionTokensChangedWrapper(this.mContext, executor, onSessionTokensChangedListener);
            try {
                this.mService.addSessionTokensListener(sessionTokensChangedWrapper.mStub, i, this.mContext.getPackageName());
                this.mSessionTokensListener.put(onSessionTokensChangedListener, sessionTokensChangedWrapper);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in addSessionTokensListener.", e);
            }
        }
    }

    public void removeOnSessionTokensChangedListener(OnSessionTokensChangedListener onSessionTokensChangedListener) {
        if (onSessionTokensChangedListener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        synchronized (this.mLock) {
            SessionTokensChangedWrapper sessionTokensChangedWrapperRemove = this.mSessionTokensListener.remove(onSessionTokensChangedListener);
            if (sessionTokensChangedWrapperRemove != null) {
                try {
                    try {
                        this.mService.removeSessionTokensListener(sessionTokensChangedWrapperRemove.mStub, this.mContext.getPackageName());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error in removeSessionTokensListener.", e);
                    }
                    sessionTokensChangedWrapperRemove.release();
                } catch (Throwable th) {
                    sessionTokensChangedWrapperRemove.release();
                    throw th;
                }
            }
        }
    }

    private static List<SessionToken2> toTokenList(List<Bundle> list) {
        ArrayList arrayList = new ArrayList();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                SessionToken2 sessionToken2FromBundle = SessionToken2.fromBundle(list.get(i));
                if (sessionToken2FromBundle != null) {
                    arrayList.add(sessionToken2FromBundle);
                }
            }
        }
        return arrayList;
    }

    public boolean isGlobalPriorityActive() {
        try {
            return this.mService.isGlobalPriorityActive();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to check if the global priority is active.", e);
            return false;
        }
    }

    @SystemApi
    public void setOnVolumeKeyLongPressListener(OnVolumeKeyLongPressListener onVolumeKeyLongPressListener, Handler handler) {
        synchronized (this.mLock) {
            try {
                try {
                    if (onVolumeKeyLongPressListener == null) {
                        this.mOnVolumeKeyLongPressListener = null;
                        this.mService.setOnVolumeKeyLongPressListener(null);
                    } else {
                        if (handler == null) {
                            handler = new Handler();
                        }
                        this.mOnVolumeKeyLongPressListener = new OnVolumeKeyLongPressListenerImpl(onVolumeKeyLongPressListener, handler);
                        this.mService.setOnVolumeKeyLongPressListener(this.mOnVolumeKeyLongPressListener);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to set volume key long press listener", e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @SystemApi
    public void setOnMediaKeyListener(OnMediaKeyListener onMediaKeyListener, Handler handler) {
        synchronized (this.mLock) {
            try {
                try {
                    if (onMediaKeyListener == null) {
                        this.mOnMediaKeyListener = null;
                        this.mService.setOnMediaKeyListener(null);
                    } else {
                        if (handler == null) {
                            handler = new Handler();
                        }
                        this.mOnMediaKeyListener = new OnMediaKeyListenerImpl(onMediaKeyListener, handler);
                        this.mService.setOnMediaKeyListener(this.mOnMediaKeyListener);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to set media key listener", e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void setCallback(Callback callback, Handler handler) {
        synchronized (this.mLock) {
            try {
                try {
                    if (callback == null) {
                        this.mCallback = null;
                        this.mService.setCallback(null);
                    } else {
                        if (handler == null) {
                            handler = new Handler();
                        }
                        this.mCallback = new CallbackImpl(callback, handler);
                        this.mService.setCallback(this.mCallback);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to set media key callback", e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public static final class RemoteUserInfo {
        private final IBinder mCallerBinder;
        private final String mPackageName;
        private final int mPid;
        private final int mUid;

        public RemoteUserInfo(String str, int i, int i2) {
            this(str, i, i2, null);
        }

        public RemoteUserInfo(String str, int i, int i2, IBinder iBinder) {
            this.mPackageName = str;
            this.mPid = i;
            this.mUid = i2;
            this.mCallerBinder = iBinder;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public int getPid() {
            return this.mPid;
        }

        public int getUid() {
            return this.mUid;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof RemoteUserInfo)) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            RemoteUserInfo remoteUserInfo = (RemoteUserInfo) obj;
            if (this.mCallerBinder == null || remoteUserInfo.mCallerBinder == null) {
                return false;
            }
            return this.mCallerBinder.equals(remoteUserInfo.mCallerBinder);
        }

        public int hashCode() {
            return Objects.hash(this.mPackageName, Integer.valueOf(this.mPid), Integer.valueOf(this.mUid));
        }
    }

    private static final class SessionsChangedWrapper {
        private Context mContext;
        private Handler mHandler;
        private OnActiveSessionsChangedListener mListener;
        private final IActiveSessionsListener.Stub mStub = new IActiveSessionsListener.Stub() {
            @Override
            public void onActiveSessionsChanged(final List<MediaSession.Token> list) {
                Handler handler = SessionsChangedWrapper.this.mHandler;
                if (handler != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Context context = SessionsChangedWrapper.this.mContext;
                            if (context != null) {
                                ArrayList arrayList = new ArrayList();
                                int size = list.size();
                                for (int i = 0; i < size; i++) {
                                    arrayList.add(new MediaController(context, (MediaSession.Token) list.get(i)));
                                }
                                OnActiveSessionsChangedListener onActiveSessionsChangedListener = SessionsChangedWrapper.this.mListener;
                                if (onActiveSessionsChangedListener != null) {
                                    onActiveSessionsChangedListener.onActiveSessionsChanged(arrayList);
                                }
                            }
                        }
                    });
                }
            }
        };

        public SessionsChangedWrapper(Context context, OnActiveSessionsChangedListener onActiveSessionsChangedListener, Handler handler) {
            this.mContext = context;
            this.mListener = onActiveSessionsChangedListener;
            this.mHandler = handler;
        }

        private void release() {
            this.mListener = null;
            this.mContext = null;
            this.mHandler = null;
        }
    }

    private static final class SessionTokensChangedWrapper {
        private Context mContext;
        private Executor mExecutor;
        private OnSessionTokensChangedListener mListener;
        private final ISessionTokensListener.Stub mStub = new AnonymousClass1();

        public SessionTokensChangedWrapper(Context context, Executor executor, OnSessionTokensChangedListener onSessionTokensChangedListener) {
            this.mContext = context;
            this.mExecutor = executor;
            this.mListener = onSessionTokensChangedListener;
        }

        class AnonymousClass1 extends ISessionTokensListener.Stub {
            AnonymousClass1() {
            }

            @Override
            public void onSessionTokensChanged(final List<Bundle> list) {
                Executor executor = SessionTokensChangedWrapper.this.mExecutor;
                if (executor != null) {
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            MediaSessionManager.SessionTokensChangedWrapper.AnonymousClass1.lambda$onSessionTokensChanged$0(this.f$0, list);
                        }
                    });
                }
            }

            public static void lambda$onSessionTokensChanged$0(AnonymousClass1 anonymousClass1, List list) {
                Context context = SessionTokensChangedWrapper.this.mContext;
                OnSessionTokensChangedListener onSessionTokensChangedListener = SessionTokensChangedWrapper.this.mListener;
                if (context != null && onSessionTokensChangedListener != null) {
                    onSessionTokensChangedListener.onSessionTokensChanged(MediaSessionManager.toTokenList(list));
                }
            }
        }

        private void release() {
            this.mListener = null;
            this.mContext = null;
            this.mExecutor = null;
        }
    }

    private static final class OnVolumeKeyLongPressListenerImpl extends IOnVolumeKeyLongPressListener.Stub {
        private Handler mHandler;
        private OnVolumeKeyLongPressListener mListener;

        public OnVolumeKeyLongPressListenerImpl(OnVolumeKeyLongPressListener onVolumeKeyLongPressListener, Handler handler) {
            this.mListener = onVolumeKeyLongPressListener;
            this.mHandler = handler;
        }

        @Override
        public void onVolumeKeyLongPress(final KeyEvent keyEvent) {
            if (this.mListener == null || this.mHandler == null) {
                Log.w(MediaSessionManager.TAG, "Failed to call volume key long-press listener. Either mListener or mHandler is null");
            } else {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        OnVolumeKeyLongPressListenerImpl.this.mListener.onVolumeKeyLongPress(keyEvent);
                    }
                });
            }
        }
    }

    private static final class OnMediaKeyListenerImpl extends IOnMediaKeyListener.Stub {
        private Handler mHandler;
        private OnMediaKeyListener mListener;

        public OnMediaKeyListenerImpl(OnMediaKeyListener onMediaKeyListener, Handler handler) {
            this.mListener = onMediaKeyListener;
            this.mHandler = handler;
        }

        @Override
        public void onMediaKey(final KeyEvent keyEvent, final ResultReceiver resultReceiver) {
            if (this.mListener == null || this.mHandler == null) {
                Log.w(MediaSessionManager.TAG, "Failed to call media key listener. Either mListener or mHandler is null");
            } else {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        boolean zOnMediaKey = OnMediaKeyListenerImpl.this.mListener.onMediaKey(keyEvent);
                        Log.d(MediaSessionManager.TAG, "The media key listener is returned " + zOnMediaKey);
                        if (resultReceiver != null) {
                            resultReceiver.send(zOnMediaKey ? 1 : 0, null);
                        }
                    }
                });
            }
        }
    }

    private static final class CallbackImpl extends ICallback.Stub {
        private final Callback mCallback;
        private final Handler mHandler;

        public CallbackImpl(Callback callback, Handler handler) {
            this.mCallback = callback;
            this.mHandler = handler;
        }

        @Override
        public void onMediaKeyEventDispatchedToMediaSession(final KeyEvent keyEvent, final MediaSession.Token token) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CallbackImpl.this.mCallback.onMediaKeyEventDispatched(keyEvent, token);
                }
            });
        }

        @Override
        public void onMediaKeyEventDispatchedToMediaButtonReceiver(final KeyEvent keyEvent, final ComponentName componentName) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CallbackImpl.this.mCallback.onMediaKeyEventDispatched(keyEvent, componentName);
                }
            });
        }

        @Override
        public void onAddressedPlayerChangedToMediaSession(final MediaSession.Token token) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CallbackImpl.this.mCallback.onAddressedPlayerChanged(token);
                }
            });
        }

        @Override
        public void onAddressedPlayerChangedToMediaButtonReceiver(final ComponentName componentName) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CallbackImpl.this.mCallback.onAddressedPlayerChanged(componentName);
                }
            });
        }
    }
}
