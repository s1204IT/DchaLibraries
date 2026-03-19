package android.telecom.Logging;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.telecom.Log;
import android.telecom.Logging.Session;
import android.util.Base64;
import com.android.internal.annotations.VisibleForTesting;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final long DEFAULT_SESSION_TIMEOUT_MS = 30000;
    private static final String LOGGING_TAG = "Logging";
    private static final long SESSION_ID_ROLLOVER_THRESHOLD = 262144;
    private static final String TIMEOUTS_PREFIX = "telecom.";
    private Context mContext;
    private int sCodeEntryCounter = 0;

    @VisibleForTesting
    public ConcurrentHashMap<Integer, Session> mSessionMapper = new ConcurrentHashMap<>(100);

    @VisibleForTesting
    public java.lang.Runnable mCleanStaleSessions = new java.lang.Runnable() {
        @Override
        public final void run() {
            SessionManager sessionManager = this.f$0;
            sessionManager.cleanupStaleSessions(sessionManager.getSessionCleanupTimeoutMs());
        }
    };
    private Handler mSessionCleanupHandler = new Handler(Looper.getMainLooper());

    @VisibleForTesting
    public ICurrentThreadId mCurrentThreadId = new ICurrentThreadId() {
        @Override
        public final int get() {
            return Process.myTid();
        }
    };
    private ISessionCleanupTimeoutMs mSessionCleanupTimeoutMs = new ISessionCleanupTimeoutMs() {
        @Override
        public final long get() {
            return SessionManager.lambda$new$1(this.f$0);
        }
    };
    private List<ISessionListener> mSessionListeners = new ArrayList();

    public interface ICurrentThreadId {
        int get();
    }

    private interface ISessionCleanupTimeoutMs {
        long get();
    }

    public interface ISessionIdQueryHandler {
        String getSessionId();
    }

    public interface ISessionListener {
        void sessionComplete(String str, long j);
    }

    public static long lambda$new$1(SessionManager sessionManager) {
        if (sessionManager.mContext == null) {
            return 30000L;
        }
        return sessionManager.getCleanupTimeout(sessionManager.mContext);
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    private long getSessionCleanupTimeoutMs() {
        return this.mSessionCleanupTimeoutMs.get();
    }

    private synchronized void resetStaleSessionTimer() {
        this.mSessionCleanupHandler.removeCallbacksAndMessages(null);
        if (this.mCleanStaleSessions != null) {
            this.mSessionCleanupHandler.postDelayed(this.mCleanStaleSessions, getSessionCleanupTimeoutMs());
        }
    }

    public synchronized void startSession(Session.Info info, String str, String str2) {
        try {
            if (info == null) {
                startSession(str, str2);
            } else {
                startExternalSession(info, str);
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    public synchronized void startSession(String str, String str2) {
        resetStaleSessionTimer();
        int callingThreadId = getCallingThreadId();
        if (this.mSessionMapper.get(Integer.valueOf(callingThreadId)) != null) {
            continueSession(createSubsession(true), str);
            return;
        }
        Log.d(LOGGING_TAG, Session.START_SESSION, new Object[0]);
        this.mSessionMapper.put(Integer.valueOf(callingThreadId), new Session(getNextSessionID(), str, System.currentTimeMillis(), false, str2));
    }

    public synchronized void startExternalSession(Session.Info info, String str) {
        if (info == null) {
            return;
        }
        int callingThreadId = getCallingThreadId();
        if (this.mSessionMapper.get(Integer.valueOf(callingThreadId)) != null) {
            Log.w(LOGGING_TAG, "trying to start an external session with a session already active.", new Object[0]);
            return;
        }
        Log.d(LOGGING_TAG, Session.START_EXTERNAL_SESSION, new Object[0]);
        Session session = new Session(Session.EXTERNAL_INDICATOR + info.sessionId, info.methodPath, System.currentTimeMillis(), false, null);
        session.setIsExternal(true);
        session.markSessionCompleted(-1L);
        this.mSessionMapper.put(Integer.valueOf(callingThreadId), session);
        continueSession(createSubsession(), str);
    }

    public Session createSubsession() {
        return createSubsession(false);
    }

    private synchronized Session createSubsession(boolean z) {
        Session session = this.mSessionMapper.get(Integer.valueOf(getCallingThreadId()));
        if (session == null) {
            Log.d(LOGGING_TAG, "Log.createSubsession was called with no session active.", new Object[0]);
            return null;
        }
        Session session2 = new Session(session.getNextChildId(), session.getShortMethodName(), System.currentTimeMillis(), z, null);
        session.addChild(session2);
        session2.setParentSession(session);
        if (!z) {
            Log.v(LOGGING_TAG, "CREATE_SUBSESSION " + session2.toString(), new Object[0]);
        } else {
            Log.v(LOGGING_TAG, "CREATE_SUBSESSION (Invisible subsession)", new Object[0]);
        }
        return session2;
    }

    public synchronized Session.Info getExternalSession() {
        Session session = this.mSessionMapper.get(Integer.valueOf(getCallingThreadId()));
        if (session == null) {
            Log.d(LOGGING_TAG, "Log.getExternalSession was called with no session active.", new Object[0]);
            return null;
        }
        return session.getInfo();
    }

    public synchronized void cancelSubsession(Session session) {
        if (session == null) {
            return;
        }
        session.markSessionCompleted(-1L);
        endParentSessions(session);
    }

    public synchronized void continueSession(Session session, String str) {
        if (session == null) {
            return;
        }
        resetStaleSessionTimer();
        session.setShortMethodName(str);
        session.setExecutionStartTimeMs(System.currentTimeMillis());
        if (session.getParentSession() == null) {
            Log.i(LOGGING_TAG, "Log.continueSession was called with no session active for method " + str, new Object[0]);
            return;
        }
        this.mSessionMapper.put(Integer.valueOf(getCallingThreadId()), session);
        if (!session.isStartedFromActiveSession()) {
            Log.v(LOGGING_TAG, Session.CONTINUE_SUBSESSION, new Object[0]);
        } else {
            Log.v(LOGGING_TAG, "CONTINUE_SUBSESSION (Invisible Subsession) with Method " + str, new Object[0]);
        }
    }

    public synchronized void endSession() {
        int callingThreadId = getCallingThreadId();
        Session session = this.mSessionMapper.get(Integer.valueOf(callingThreadId));
        if (session == null) {
            Log.w(LOGGING_TAG, "Log.endSession was called with no session active.", new Object[0]);
            return;
        }
        session.markSessionCompleted(System.currentTimeMillis());
        if (!session.isStartedFromActiveSession()) {
            Log.v(LOGGING_TAG, "END_SUBSESSION (dur: " + session.getLocalExecutionTime() + " mS)", new Object[0]);
        } else {
            Log.v(LOGGING_TAG, "END_SUBSESSION (Invisible Subsession) (dur: " + session.getLocalExecutionTime() + " ms)", new Object[0]);
        }
        Session parentSession = session.getParentSession();
        this.mSessionMapper.remove(Integer.valueOf(callingThreadId));
        endParentSessions(session);
        if (parentSession != null && !parentSession.isSessionCompleted() && session.isStartedFromActiveSession()) {
            this.mSessionMapper.put(Integer.valueOf(callingThreadId), parentSession);
        }
    }

    private void endParentSessions(Session session) {
        if (!session.isSessionCompleted() || session.getChildSessions().size() != 0) {
            return;
        }
        Session parentSession = session.getParentSession();
        if (parentSession != null) {
            session.setParentSession(null);
            parentSession.removeChild(session);
            if (parentSession.isExternal()) {
                notifySessionCompleteListeners(session.getShortMethodName(), System.currentTimeMillis() - session.getExecutionStartTimeMilliseconds());
            }
            endParentSessions(parentSession);
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis() - session.getExecutionStartTimeMilliseconds();
        Log.d(LOGGING_TAG, "END_SESSION (dur: " + jCurrentTimeMillis + " ms): " + session.toString(), new Object[0]);
        if (!session.isExternal()) {
            notifySessionCompleteListeners(session.getShortMethodName(), jCurrentTimeMillis);
        }
    }

    private void notifySessionCompleteListeners(String str, long j) {
        Iterator<ISessionListener> it = this.mSessionListeners.iterator();
        while (it.hasNext()) {
            it.next().sessionComplete(str, j);
        }
    }

    public String getSessionId() {
        Session session = this.mSessionMapper.get(Integer.valueOf(getCallingThreadId()));
        return session != null ? session.toString() : "";
    }

    public synchronized void registerSessionListener(ISessionListener iSessionListener) {
        if (iSessionListener != null) {
            this.mSessionListeners.add(iSessionListener);
        }
    }

    private synchronized String getNextSessionID() {
        Integer numValueOf;
        int i = this.sCodeEntryCounter;
        this.sCodeEntryCounter = i + 1;
        numValueOf = Integer.valueOf(i);
        if (numValueOf.intValue() >= 262144) {
            restartSessionCounter();
            int i2 = this.sCodeEntryCounter;
            this.sCodeEntryCounter = i2 + 1;
            numValueOf = Integer.valueOf(i2);
        }
        return getBase64Encoding(numValueOf.intValue());
    }

    private synchronized void restartSessionCounter() {
        this.sCodeEntryCounter = 0;
    }

    private String getBase64Encoding(int i) {
        return Base64.encodeToString(Arrays.copyOfRange(ByteBuffer.allocate(4).putInt(i).array(), 2, 4), 3);
    }

    private int getCallingThreadId() {
        return this.mCurrentThreadId.get();
    }

    @VisibleForTesting
    public synchronized void cleanupStaleSessions(long j) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Session>> it = this.mSessionMapper.entrySet().iterator();
        String str = "Stale Sessions Cleaned:\n";
        boolean z = false;
        while (it.hasNext()) {
            Session value = it.next().getValue();
            if (jCurrentTimeMillis - value.getExecutionStartTimeMilliseconds() > j) {
                it.remove();
                str = str + value.printFullSessionTree() + "\n";
                z = true;
            }
        }
        if (z) {
            Log.w(LOGGING_TAG, str, new Object[0]);
        } else {
            Log.v(LOGGING_TAG, "No stale logging sessions needed to be cleaned...", new Object[0]);
        }
    }

    private long getCleanupTimeout(Context context) {
        return Settings.Secure.getLong(context.getContentResolver(), "telecom.stale_session_cleanup_timeout_millis", 30000L);
    }
}
