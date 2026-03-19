package android.telecom.Logging;

import android.os.Parcel;
import android.os.Parcelable;
import android.telecom.Log;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;

public class Session {
    public static final String CONTINUE_SUBSESSION = "CONTINUE_SUBSESSION";
    public static final String CREATE_SUBSESSION = "CREATE_SUBSESSION";
    public static final String END_SESSION = "END_SESSION";
    public static final String END_SUBSESSION = "END_SUBSESSION";
    public static final String EXTERNAL_INDICATOR = "E-";
    public static final String SESSION_SEPARATION_CHAR_CHILD = "_";
    public static final String START_EXTERNAL_SESSION = "START_EXTERNAL_SESSION";
    public static final String START_SESSION = "START_SESSION";
    public static final String SUBSESSION_SEPARATION_CHAR = "->";
    public static final String TRUNCATE_STRING = "...";
    public static final int UNDEFINED = -1;
    private ArrayList<Session> mChildSessions;
    private long mExecutionStartTimeMs;
    private String mFullMethodPathCache;
    private boolean mIsStartedFromActiveSession;
    private String mOwnerInfo;
    private Session mParentSession;
    private String mSessionId;
    private String mShortMethodName;
    private long mExecutionEndTimeMs = -1;
    private boolean mIsCompleted = false;
    private boolean mIsExternal = false;
    private int mChildCounter = 0;

    public static class Info implements Parcelable {
        public static final Parcelable.Creator<Info> CREATOR = new Parcelable.Creator<Info>() {
            @Override
            public Info createFromParcel(Parcel parcel) {
                return new Info(parcel.readString(), parcel.readString());
            }

            @Override
            public Info[] newArray(int i) {
                return new Info[i];
            }
        };
        public final String methodPath;
        public final String sessionId;

        private Info(String str, String str2) {
            this.sessionId = str;
            this.methodPath = str2;
        }

        public static Info getInfo(Session session) {
            return new Info(session.getFullSessionId(), session.getFullMethodPath(!Log.DEBUG && session.isSessionExternal()));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.sessionId);
            parcel.writeString(this.methodPath);
        }
    }

    public Session(String str, String str2, long j, boolean z, String str3) {
        this.mIsStartedFromActiveSession = false;
        setSessionId(str);
        setShortMethodName(str2);
        this.mExecutionStartTimeMs = j;
        this.mParentSession = null;
        this.mChildSessions = new ArrayList<>(5);
        this.mIsStartedFromActiveSession = z;
        this.mOwnerInfo = str3;
    }

    public void setSessionId(String str) {
        if (str == null) {
            this.mSessionId = "?";
        }
        this.mSessionId = str;
    }

    public String getShortMethodName() {
        return this.mShortMethodName;
    }

    public void setShortMethodName(String str) {
        if (str == null) {
            str = "";
        }
        this.mShortMethodName = str;
    }

    public void setIsExternal(boolean z) {
        this.mIsExternal = z;
    }

    public boolean isExternal() {
        return this.mIsExternal;
    }

    public void setParentSession(Session session) {
        this.mParentSession = session;
    }

    public void addChild(Session session) {
        if (session != null) {
            this.mChildSessions.add(session);
        }
    }

    public void removeChild(Session session) {
        if (session != null) {
            this.mChildSessions.remove(session);
        }
    }

    public long getExecutionStartTimeMilliseconds() {
        return this.mExecutionStartTimeMs;
    }

    public void setExecutionStartTimeMs(long j) {
        this.mExecutionStartTimeMs = j;
    }

    public Session getParentSession() {
        return this.mParentSession;
    }

    public ArrayList<Session> getChildSessions() {
        return this.mChildSessions;
    }

    public boolean isSessionCompleted() {
        return this.mIsCompleted;
    }

    public boolean isStartedFromActiveSession() {
        return this.mIsStartedFromActiveSession;
    }

    public Info getInfo() {
        return Info.getInfo(this);
    }

    @VisibleForTesting
    public String getSessionId() {
        return this.mSessionId;
    }

    public void markSessionCompleted(long j) {
        this.mExecutionEndTimeMs = j;
        this.mIsCompleted = true;
    }

    public long getLocalExecutionTime() {
        if (this.mExecutionEndTimeMs == -1) {
            return -1L;
        }
        return this.mExecutionEndTimeMs - this.mExecutionStartTimeMs;
    }

    public synchronized String getNextChildId() {
        int i;
        i = this.mChildCounter;
        this.mChildCounter = i + 1;
        return String.valueOf(i);
    }

    private String getFullSessionId() {
        Session session = this.mParentSession;
        if (session == null) {
            return this.mSessionId;
        }
        if (Log.VERBOSE) {
            return session.getFullSessionId() + SESSION_SEPARATION_CHAR_CHILD + this.mSessionId;
        }
        return session.getFullSessionId();
    }

    public String printFullSessionTree() {
        Session parentSession = this;
        while (parentSession.getParentSession() != null) {
            parentSession = parentSession.getParentSession();
        }
        return parentSession.printSessionTree();
    }

    public String printSessionTree() {
        StringBuilder sb = new StringBuilder();
        printSessionTree(0, sb);
        return sb.toString();
    }

    private void printSessionTree(int i, StringBuilder sb) {
        sb.append(toString());
        for (Session session : this.mChildSessions) {
            sb.append("\n");
            for (int i2 = 0; i2 <= i; i2++) {
                sb.append("\t");
            }
            session.printSessionTree(i + 1, sb);
        }
    }

    public String getFullMethodPath(boolean z) {
        StringBuilder sb = new StringBuilder();
        getFullMethodPath(sb, z);
        return sb.toString();
    }

    private synchronized void getFullMethodPath(StringBuilder sb, boolean z) {
        if (!TextUtils.isEmpty(this.mFullMethodPathCache) && !z) {
            sb.append(this.mFullMethodPathCache);
            return;
        }
        Session parentSession = getParentSession();
        boolean z2 = false;
        if (parentSession != null) {
            z2 = !this.mShortMethodName.equals(parentSession.mShortMethodName);
            parentSession.getFullMethodPath(sb, z);
            sb.append(SUBSESSION_SEPARATION_CHAR);
        }
        if (isExternal()) {
            if (z) {
                sb.append(TRUNCATE_STRING);
            } else {
                sb.append("(");
                sb.append(this.mShortMethodName);
                sb.append(")");
            }
        } else {
            sb.append(this.mShortMethodName);
        }
        if (z2 && !z) {
            this.mFullMethodPathCache = sb.toString();
        }
    }

    private boolean isSessionExternal() {
        if (getParentSession() == null) {
            return isExternal();
        }
        return getParentSession().isSessionExternal();
    }

    public int hashCode() {
        return (31 * (((((((((((((((((this.mSessionId != null ? this.mSessionId.hashCode() : 0) * 31) + (this.mShortMethodName != null ? this.mShortMethodName.hashCode() : 0)) * 31) + ((int) (this.mExecutionStartTimeMs ^ (this.mExecutionStartTimeMs >>> 32)))) * 31) + ((int) (this.mExecutionEndTimeMs ^ (this.mExecutionEndTimeMs >>> 32)))) * 31) + (this.mParentSession != null ? this.mParentSession.hashCode() : 0)) * 31) + (this.mChildSessions != null ? this.mChildSessions.hashCode() : 0)) * 31) + (this.mIsCompleted ? 1 : 0)) * 31) + this.mChildCounter) * 31) + (this.mIsStartedFromActiveSession ? 1 : 0))) + (this.mOwnerInfo != null ? this.mOwnerInfo.hashCode() : 0);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Session session = (Session) obj;
        if (this.mExecutionStartTimeMs != session.mExecutionStartTimeMs || this.mExecutionEndTimeMs != session.mExecutionEndTimeMs || this.mIsCompleted != session.mIsCompleted || this.mChildCounter != session.mChildCounter || this.mIsStartedFromActiveSession != session.mIsStartedFromActiveSession) {
            return false;
        }
        if (this.mSessionId == null ? session.mSessionId != null : !this.mSessionId.equals(session.mSessionId)) {
            return false;
        }
        if (this.mShortMethodName == null ? session.mShortMethodName != null : !this.mShortMethodName.equals(session.mShortMethodName)) {
            return false;
        }
        if (this.mParentSession == null ? session.mParentSession != null : !this.mParentSession.equals(session.mParentSession)) {
            return false;
        }
        if (this.mChildSessions == null ? session.mChildSessions != null : !this.mChildSessions.equals(session.mChildSessions)) {
            return false;
        }
        if (this.mOwnerInfo != null) {
            return this.mOwnerInfo.equals(session.mOwnerInfo);
        }
        if (session.mOwnerInfo == null) {
            return true;
        }
        return false;
    }

    public String toString() {
        if (this.mParentSession != null && this.mIsStartedFromActiveSession) {
            return this.mParentSession.toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getFullMethodPath(false));
        if (this.mOwnerInfo != null && !this.mOwnerInfo.isEmpty()) {
            sb.append("(InCall package: ");
            sb.append(this.mOwnerInfo);
            sb.append(")");
        }
        return sb.toString() + "@" + getFullSessionId();
    }
}
