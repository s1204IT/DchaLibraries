package com.android.internal.util;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.SettingsStringUtil;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.function.Predicate;

public class StateMachine {
    public static final boolean HANDLED = true;
    public static final boolean NOT_HANDLED = false;
    private static final int SM_INIT_CMD = -2;
    private static final int SM_QUIT_CMD = -1;
    private String mName;
    private SmHandler mSmHandler;
    private HandlerThread mSmThread;

    public static class LogRec {
        private IState mDstState;
        private String mInfo;
        private IState mOrgState;
        private StateMachine mSm;
        private IState mState;
        private long mTime;
        private int mWhat;

        LogRec(StateMachine stateMachine, Message message, String str, IState iState, IState iState2, IState iState3) {
            update(stateMachine, message, str, iState, iState2, iState3);
        }

        public void update(StateMachine stateMachine, Message message, String str, IState iState, IState iState2, IState iState3) {
            this.mSm = stateMachine;
            this.mTime = System.currentTimeMillis();
            this.mWhat = message != null ? message.what : 0;
            this.mInfo = str;
            this.mState = iState;
            this.mOrgState = iState2;
            this.mDstState = iState3;
        }

        public long getTime() {
            return this.mTime;
        }

        public long getWhat() {
            return this.mWhat;
        }

        public String getInfo() {
            return this.mInfo;
        }

        public IState getState() {
            return this.mState;
        }

        public IState getDestState() {
            return this.mDstState;
        }

        public IState getOriginalState() {
            return this.mOrgState;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("time=");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(this.mTime);
            sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", calendar, calendar, calendar, calendar, calendar, calendar));
            sb.append(" processed=");
            sb.append(this.mState == null ? "<null>" : this.mState.getName());
            sb.append(" org=");
            sb.append(this.mOrgState == null ? "<null>" : this.mOrgState.getName());
            sb.append(" dest=");
            sb.append(this.mDstState == null ? "<null>" : this.mDstState.getName());
            sb.append(" what=");
            String whatToString = this.mSm != null ? this.mSm.getWhatToString(this.mWhat) : "";
            if (TextUtils.isEmpty(whatToString)) {
                sb.append(this.mWhat);
                sb.append("(0x");
                sb.append(Integer.toHexString(this.mWhat));
                sb.append(")");
            } else {
                sb.append(whatToString);
            }
            if (!TextUtils.isEmpty(this.mInfo)) {
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                sb.append(this.mInfo);
            }
            return sb.toString();
        }
    }

    private static class LogRecords {
        private static final int DEFAULT_SIZE = 20;
        private int mCount;
        private boolean mLogOnlyTransitions;
        private Vector<LogRec> mLogRecVector;
        private int mMaxSize;
        private int mOldestIndex;

        private LogRecords() {
            this.mLogRecVector = new Vector<>();
            this.mMaxSize = 20;
            this.mOldestIndex = 0;
            this.mCount = 0;
            this.mLogOnlyTransitions = false;
        }

        synchronized void setSize(int i) {
            this.mMaxSize = i;
            this.mOldestIndex = 0;
            this.mCount = 0;
            this.mLogRecVector.clear();
        }

        synchronized void setLogOnlyTransitions(boolean z) {
            this.mLogOnlyTransitions = z;
        }

        synchronized boolean logOnlyTransitions() {
            return this.mLogOnlyTransitions;
        }

        synchronized int size() {
            return this.mLogRecVector.size();
        }

        synchronized int count() {
            return this.mCount;
        }

        synchronized void cleanup() {
            this.mLogRecVector.clear();
        }

        synchronized LogRec get(int i) {
            int i2 = this.mOldestIndex + i;
            if (i2 >= this.mMaxSize) {
                i2 -= this.mMaxSize;
            }
            if (i2 >= size()) {
                return null;
            }
            return this.mLogRecVector.get(i2);
        }

        synchronized void add(StateMachine stateMachine, Message message, String str, IState iState, IState iState2, IState iState3) {
            this.mCount++;
            if (this.mLogRecVector.size() < this.mMaxSize) {
                this.mLogRecVector.add(new LogRec(stateMachine, message, str, iState, iState2, iState3));
            } else {
                LogRec logRec = this.mLogRecVector.get(this.mOldestIndex);
                this.mOldestIndex++;
                if (this.mOldestIndex >= this.mMaxSize) {
                    this.mOldestIndex = 0;
                }
                logRec.update(stateMachine, message, str, iState, iState2, iState3);
            }
        }
    }

    private static class SmHandler extends Handler {
        private static final Object mSmHandlerObj = new Object();
        private boolean mDbg;
        private ArrayList<Message> mDeferredMessages;
        private State mDestState;
        private HaltingState mHaltingState;
        private boolean mHasQuit;
        private State mInitialState;
        private boolean mIsConstructionCompleted;
        private LogRecords mLogRecords;
        private Message mMsg;
        private QuittingState mQuittingState;
        private StateMachine mSm;
        private HashMap<State, StateInfo> mStateInfo;
        private StateInfo[] mStateStack;
        private int mStateStackTopIndex;
        private StateInfo[] mTempStateStack;
        private int mTempStateStackCount;
        private boolean mTransitionInProgress;

        private class StateInfo {
            boolean active;
            StateInfo parentStateInfo;
            State state;

            private StateInfo() {
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("state=");
                sb.append(this.state.getName());
                sb.append(",active=");
                sb.append(this.active);
                sb.append(",parent=");
                sb.append(this.parentStateInfo == null ? "null" : this.parentStateInfo.state.getName());
                return sb.toString();
            }
        }

        private class HaltingState extends State {
            private HaltingState() {
            }

            @Override
            public boolean processMessage(Message message) {
                SmHandler.this.mSm.haltedProcessMessage(message);
                return true;
            }
        }

        private class QuittingState extends State {
            private QuittingState() {
            }

            @Override
            public boolean processMessage(Message message) {
                return false;
            }
        }

        @Override
        public final void handleMessage(Message message) {
            if (!this.mHasQuit) {
                if (this.mSm != null && message.what != -2 && message.what != -1) {
                    this.mSm.onPreHandleMessage(message);
                }
                if (this.mDbg) {
                    this.mSm.log("handleMessage: E msg.what=" + message.what);
                }
                this.mMsg = message;
                State stateProcessMsg = null;
                if (this.mIsConstructionCompleted || this.mMsg.what == -1) {
                    stateProcessMsg = processMsg(message);
                } else if (!this.mIsConstructionCompleted && this.mMsg.what == -2 && this.mMsg.obj == mSmHandlerObj) {
                    this.mIsConstructionCompleted = true;
                    invokeEnterMethods(0);
                } else {
                    throw new RuntimeException("StateMachine.handleMessage: The start method not called, received msg: " + message);
                }
                performTransitions(stateProcessMsg, message);
                if (this.mDbg && this.mSm != null) {
                    this.mSm.log("handleMessage: X");
                }
                if (this.mSm != null && message.what != -2 && message.what != -1) {
                    this.mSm.onPostHandleMessage(message);
                }
            }
        }

        private void performTransitions(State state, Message message) {
            boolean z;
            State state2 = this.mStateStack[this.mStateStackTopIndex].state;
            if (!this.mSm.recordLogRec(this.mMsg) || message.obj == mSmHandlerObj) {
                z = false;
            } else {
                z = true;
            }
            if (this.mLogRecords.logOnlyTransitions()) {
                if (this.mDestState != null) {
                    this.mLogRecords.add(this.mSm, this.mMsg, this.mSm.getLogRecString(this.mMsg), state, state2, this.mDestState);
                }
            } else if (z) {
                this.mLogRecords.add(this.mSm, this.mMsg, this.mSm.getLogRecString(this.mMsg), state, state2, this.mDestState);
            }
            State state3 = this.mDestState;
            if (state3 != null) {
                while (true) {
                    if (this.mDbg) {
                        this.mSm.log("handleMessage: new destination call exit/enter");
                    }
                    StateInfo stateInfo = setupTempStateStackWithStatesToEnter(state3);
                    this.mTransitionInProgress = true;
                    invokeExitMethods(stateInfo);
                    invokeEnterMethods(moveTempStateStackToStateStack());
                    moveDeferredMessageAtFrontOfQueue();
                    if (state3 == this.mDestState) {
                        break;
                    } else {
                        state3 = this.mDestState;
                    }
                }
                this.mDestState = null;
            }
            if (state3 != null) {
                if (state3 == this.mQuittingState) {
                    this.mSm.onQuitting();
                    cleanupAfterQuitting();
                } else if (state3 == this.mHaltingState) {
                    this.mSm.onHalting();
                }
            }
        }

        private final void cleanupAfterQuitting() {
            if (this.mSm.mSmThread != null) {
                getLooper().quit();
                this.mSm.mSmThread = null;
            }
            this.mSm.mSmHandler = null;
            this.mSm = null;
            this.mMsg = null;
            this.mLogRecords.cleanup();
            this.mStateStack = null;
            this.mTempStateStack = null;
            this.mStateInfo.clear();
            this.mInitialState = null;
            this.mDestState = null;
            this.mDeferredMessages.clear();
            this.mHasQuit = true;
        }

        private final void completeConstruction() {
            if (this.mDbg) {
                this.mSm.log("completeConstruction: E");
            }
            int i = 0;
            for (StateInfo stateInfo : this.mStateInfo.values()) {
                int i2 = 0;
                while (stateInfo != null) {
                    stateInfo = stateInfo.parentStateInfo;
                    i2++;
                }
                if (i < i2) {
                    i = i2;
                }
            }
            if (this.mDbg) {
                this.mSm.log("completeConstruction: maxDepth=" + i);
            }
            this.mStateStack = new StateInfo[i];
            this.mTempStateStack = new StateInfo[i];
            setupInitialStateStack();
            sendMessageAtFrontOfQueue(obtainMessage(-2, mSmHandlerObj));
            if (this.mDbg) {
                this.mSm.log("completeConstruction: X");
            }
        }

        private final State processMsg(Message message) {
            StateInfo stateInfo = this.mStateStack[this.mStateStackTopIndex];
            if (this.mDbg) {
                this.mSm.log("processMsg: " + stateInfo.state.getName());
            }
            if (isQuit(message)) {
                transitionTo(this.mQuittingState);
            } else {
                while (true) {
                    if (stateInfo.state.processMessage(message)) {
                        break;
                    }
                    stateInfo = stateInfo.parentStateInfo;
                    if (stateInfo == null) {
                        this.mSm.unhandledMessage(message);
                        break;
                    }
                    if (this.mDbg) {
                        this.mSm.log("processMsg: " + stateInfo.state.getName());
                    }
                }
            }
            if (stateInfo != null) {
                return stateInfo.state;
            }
            return null;
        }

        private final void invokeExitMethods(StateInfo stateInfo) {
            while (this.mStateStackTopIndex >= 0 && this.mStateStack[this.mStateStackTopIndex] != stateInfo) {
                State state = this.mStateStack[this.mStateStackTopIndex].state;
                if (this.mDbg) {
                    this.mSm.log("invokeExitMethods: " + state.getName());
                }
                state.exit();
                this.mStateStack[this.mStateStackTopIndex].active = false;
                this.mStateStackTopIndex--;
            }
        }

        private final void invokeEnterMethods(int i) {
            for (int i2 = i; i2 <= this.mStateStackTopIndex; i2++) {
                if (i == this.mStateStackTopIndex) {
                    this.mTransitionInProgress = false;
                }
                if (this.mDbg) {
                    this.mSm.log("invokeEnterMethods: " + this.mStateStack[i2].state.getName());
                }
                this.mStateStack[i2].state.enter();
                this.mStateStack[i2].active = true;
            }
            this.mTransitionInProgress = false;
        }

        private final void moveDeferredMessageAtFrontOfQueue() {
            for (int size = this.mDeferredMessages.size() - 1; size >= 0; size--) {
                Message message = this.mDeferredMessages.get(size);
                if (this.mDbg) {
                    this.mSm.log("moveDeferredMessageAtFrontOfQueue; what=" + message.what);
                }
                sendMessageAtFrontOfQueue(message);
            }
            this.mDeferredMessages.clear();
        }

        private final int moveTempStateStackToStateStack() {
            int i = this.mStateStackTopIndex + 1;
            int i2 = i;
            for (int i3 = this.mTempStateStackCount - 1; i3 >= 0; i3--) {
                if (this.mDbg) {
                    this.mSm.log("moveTempStackToStateStack: i=" + i3 + ",j=" + i2);
                }
                this.mStateStack[i2] = this.mTempStateStack[i3];
                i2++;
            }
            this.mStateStackTopIndex = i2 - 1;
            if (this.mDbg) {
                this.mSm.log("moveTempStackToStateStack: X mStateStackTop=" + this.mStateStackTopIndex + ",startingIndex=" + i + ",Top=" + this.mStateStack[this.mStateStackTopIndex].state.getName());
            }
            return i;
        }

        private final StateInfo setupTempStateStackWithStatesToEnter(State state) {
            this.mTempStateStackCount = 0;
            StateInfo stateInfo = this.mStateInfo.get(state);
            do {
                StateInfo[] stateInfoArr = this.mTempStateStack;
                int i = this.mTempStateStackCount;
                this.mTempStateStackCount = i + 1;
                stateInfoArr[i] = stateInfo;
                stateInfo = stateInfo.parentStateInfo;
                if (stateInfo == null) {
                    break;
                }
            } while (!stateInfo.active);
            if (this.mDbg) {
                this.mSm.log("setupTempStateStackWithStatesToEnter: X mTempStateStackCount=" + this.mTempStateStackCount + ",curStateInfo: " + stateInfo);
            }
            return stateInfo;
        }

        private final void setupInitialStateStack() {
            if (this.mDbg) {
                this.mSm.log("setupInitialStateStack: E mInitialState=" + this.mInitialState.getName());
            }
            StateInfo stateInfo = this.mStateInfo.get(this.mInitialState);
            int i = 0;
            while (true) {
                this.mTempStateStackCount = i;
                if (stateInfo != null) {
                    this.mTempStateStack[this.mTempStateStackCount] = stateInfo;
                    stateInfo = stateInfo.parentStateInfo;
                    i = this.mTempStateStackCount + 1;
                } else {
                    this.mStateStackTopIndex = -1;
                    moveTempStateStackToStateStack();
                    return;
                }
            }
        }

        private final Message getCurrentMessage() {
            return this.mMsg;
        }

        private final IState getCurrentState() {
            return this.mStateStack[this.mStateStackTopIndex].state;
        }

        private final StateInfo addState(State state, State state2) {
            StateInfo stateInfoAddState;
            if (this.mDbg) {
                StateMachine stateMachine = this.mSm;
                StringBuilder sb = new StringBuilder();
                sb.append("addStateInternal: E state=");
                sb.append(state.getName());
                sb.append(",parent=");
                sb.append(state2 == null ? "" : state2.getName());
                stateMachine.log(sb.toString());
            }
            if (state2 == null) {
                stateInfoAddState = null;
            } else {
                StateInfo stateInfo = this.mStateInfo.get(state2);
                if (stateInfo == null) {
                    stateInfoAddState = addState(state2, null);
                } else {
                    stateInfoAddState = stateInfo;
                }
            }
            StateInfo stateInfo2 = this.mStateInfo.get(state);
            if (stateInfo2 == null) {
                stateInfo2 = new StateInfo();
                this.mStateInfo.put(state, stateInfo2);
            }
            if (stateInfo2.parentStateInfo != null && stateInfo2.parentStateInfo != stateInfoAddState) {
                throw new RuntimeException("state already added");
            }
            stateInfo2.state = state;
            stateInfo2.parentStateInfo = stateInfoAddState;
            stateInfo2.active = false;
            if (this.mDbg) {
                this.mSm.log("addStateInternal: X stateInfo: " + stateInfo2);
            }
            return stateInfo2;
        }

        private void removeState(State state) {
            final StateInfo stateInfo = this.mStateInfo.get(state);
            if (stateInfo == null || stateInfo.active || this.mStateInfo.values().stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return StateMachine.SmHandler.lambda$removeState$0(stateInfo, (StateMachine.SmHandler.StateInfo) obj);
                }
            }).findAny().isPresent()) {
                return;
            }
            this.mStateInfo.remove(state);
        }

        static boolean lambda$removeState$0(StateInfo stateInfo, StateInfo stateInfo2) {
            return stateInfo2.parentStateInfo == stateInfo;
        }

        private SmHandler(Looper looper, StateMachine stateMachine) {
            super(looper);
            this.mHasQuit = false;
            this.mDbg = false;
            this.mLogRecords = new LogRecords();
            this.mStateStackTopIndex = -1;
            this.mHaltingState = new HaltingState();
            this.mQuittingState = new QuittingState();
            this.mStateInfo = new HashMap<>();
            this.mTransitionInProgress = false;
            this.mDeferredMessages = new ArrayList<>();
            this.mSm = stateMachine;
            addState(this.mHaltingState, null);
            addState(this.mQuittingState, null);
        }

        private final void setInitialState(State state) {
            if (this.mDbg) {
                this.mSm.log("setInitialState: initialState=" + state.getName());
            }
            this.mInitialState = state;
        }

        private final void transitionTo(IState iState) {
            if (this.mTransitionInProgress) {
                Log.wtf(this.mSm.mName, "transitionTo called while transition already in progress to " + this.mDestState + ", new target state=" + iState);
            }
            this.mDestState = (State) iState;
            if (this.mDbg) {
                this.mSm.log("transitionTo: destState=" + this.mDestState.getName());
            }
        }

        private final void deferMessage(Message message) {
            if (this.mDbg) {
                this.mSm.log("deferMessage: msg=" + message.what);
            }
            Message messageObtainMessage = obtainMessage();
            messageObtainMessage.copyFrom(message);
            this.mDeferredMessages.add(messageObtainMessage);
        }

        private final void quit() {
            if (this.mDbg) {
                this.mSm.log("quit:");
            }
            sendMessage(obtainMessage(-1, mSmHandlerObj));
        }

        private final void quitNow() {
            if (this.mDbg) {
                this.mSm.log("quitNow:");
            }
            sendMessageAtFrontOfQueue(obtainMessage(-1, mSmHandlerObj));
        }

        private final boolean isQuit(Message message) {
            return message.what == -1 && message.obj == mSmHandlerObj;
        }

        private final boolean isDbg() {
            return this.mDbg;
        }

        private final void setDbg(boolean z) {
            this.mDbg = z;
        }
    }

    private void initStateMachine(String str, Looper looper) {
        this.mName = str;
        this.mSmHandler = new SmHandler(looper, this);
    }

    protected StateMachine(String str) {
        this.mSmThread = new HandlerThread(str);
        this.mSmThread.start();
        initStateMachine(str, this.mSmThread.getLooper());
    }

    protected StateMachine(String str, Looper looper) {
        initStateMachine(str, looper);
    }

    protected StateMachine(String str, Handler handler) {
        initStateMachine(str, handler.getLooper());
    }

    protected void onPreHandleMessage(Message message) {
    }

    protected void onPostHandleMessage(Message message) {
    }

    public final void addState(State state, State state2) {
        this.mSmHandler.addState(state, state2);
    }

    public final void addState(State state) {
        this.mSmHandler.addState(state, null);
    }

    public final void removeState(State state) {
        this.mSmHandler.removeState(state);
    }

    public final void setInitialState(State state) {
        this.mSmHandler.setInitialState(state);
    }

    public final Message getCurrentMessage() {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return null;
        }
        return smHandler.getCurrentMessage();
    }

    public final IState getCurrentState() {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return null;
        }
        return smHandler.getCurrentState();
    }

    public final void transitionTo(IState iState) {
        this.mSmHandler.transitionTo(iState);
    }

    public final void transitionToHaltingState() {
        this.mSmHandler.transitionTo(this.mSmHandler.mHaltingState);
    }

    public final void deferMessage(Message message) {
        this.mSmHandler.deferMessage(message);
    }

    protected void unhandledMessage(Message message) {
        if (this.mSmHandler.mDbg) {
            loge(" - unhandledMessage: msg.what=" + message.what);
        }
    }

    protected void haltedProcessMessage(Message message) {
    }

    protected void onHalting() {
    }

    protected void onQuitting() {
    }

    public final String getName() {
        return this.mName;
    }

    public final void setLogRecSize(int i) {
        this.mSmHandler.mLogRecords.setSize(i);
    }

    public final void setLogOnlyTransitions(boolean z) {
        this.mSmHandler.mLogRecords.setLogOnlyTransitions(z);
    }

    public final int getLogRecSize() {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return 0;
        }
        return smHandler.mLogRecords.size();
    }

    @VisibleForTesting
    public final int getLogRecMaxSize() {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return 0;
        }
        return smHandler.mLogRecords.mMaxSize;
    }

    public final int getLogRecCount() {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return 0;
        }
        return smHandler.mLogRecords.count();
    }

    public final LogRec getLogRec(int i) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return null;
        }
        return smHandler.mLogRecords.get(i);
    }

    public final Collection<LogRec> copyLogRecs() {
        Vector vector = new Vector();
        SmHandler smHandler = this.mSmHandler;
        if (smHandler != null) {
            Iterator it = smHandler.mLogRecords.mLogRecVector.iterator();
            while (it.hasNext()) {
                vector.add((LogRec) it.next());
            }
        }
        return vector;
    }

    public void addLogRec(String str) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.mLogRecords.add(this, smHandler.getCurrentMessage(), str, smHandler.getCurrentState(), smHandler.mStateStack[smHandler.mStateStackTopIndex].state, smHandler.mDestState);
    }

    protected boolean recordLogRec(Message message) {
        return true;
    }

    protected String getLogRecString(Message message) {
        return "";
    }

    protected String getWhatToString(int i) {
        return null;
    }

    public final Handler getHandler() {
        return this.mSmHandler;
    }

    public final Message obtainMessage() {
        return Message.obtain(this.mSmHandler);
    }

    public final Message obtainMessage(int i) {
        return Message.obtain(this.mSmHandler, i);
    }

    public final Message obtainMessage(int i, Object obj) {
        return Message.obtain(this.mSmHandler, i, obj);
    }

    public final Message obtainMessage(int i, int i2) {
        return Message.obtain(this.mSmHandler, i, i2, 0);
    }

    public final Message obtainMessage(int i, int i2, int i3) {
        return Message.obtain(this.mSmHandler, i, i2, i3);
    }

    public final Message obtainMessage(int i, int i2, int i3, Object obj) {
        return Message.obtain(this.mSmHandler, i, i2, i3, obj);
    }

    public void sendMessage(int i) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessage(obtainMessage(i));
    }

    public void sendMessage(int i, Object obj) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessage(obtainMessage(i, obj));
    }

    public void sendMessage(int i, int i2) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessage(obtainMessage(i, i2));
    }

    public void sendMessage(int i, int i2, int i3) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessage(obtainMessage(i, i2, i3));
    }

    public void sendMessage(int i, int i2, int i3, Object obj) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessage(obtainMessage(i, i2, i3, obj));
    }

    public void sendMessage(Message message) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessage(message);
    }

    public void sendMessageDelayed(int i, long j) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageDelayed(obtainMessage(i), j);
    }

    public void sendMessageDelayed(int i, Object obj, long j) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageDelayed(obtainMessage(i, obj), j);
    }

    public void sendMessageDelayed(int i, int i2, long j) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageDelayed(obtainMessage(i, i2), j);
    }

    public void sendMessageDelayed(int i, int i2, int i3, long j) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageDelayed(obtainMessage(i, i2, i3), j);
    }

    public void sendMessageDelayed(int i, int i2, int i3, Object obj, long j) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageDelayed(obtainMessage(i, i2, i3, obj), j);
    }

    public void sendMessageDelayed(Message message, long j) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageDelayed(message, j);
    }

    protected final void sendMessageAtFrontOfQueue(int i) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageAtFrontOfQueue(obtainMessage(i));
    }

    protected final void sendMessageAtFrontOfQueue(int i, Object obj) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageAtFrontOfQueue(obtainMessage(i, obj));
    }

    protected final void sendMessageAtFrontOfQueue(int i, int i2) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageAtFrontOfQueue(obtainMessage(i, i2));
    }

    protected final void sendMessageAtFrontOfQueue(int i, int i2, int i3) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageAtFrontOfQueue(obtainMessage(i, i2, i3));
    }

    protected final void sendMessageAtFrontOfQueue(int i, int i2, int i3, Object obj) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageAtFrontOfQueue(obtainMessage(i, i2, i3, obj));
    }

    protected final void sendMessageAtFrontOfQueue(Message message) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.sendMessageAtFrontOfQueue(message);
    }

    protected final void removeMessages(int i) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.removeMessages(i);
    }

    protected final void removeDeferredMessages(int i) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        Iterator it = smHandler.mDeferredMessages.iterator();
        while (it.hasNext()) {
            if (((Message) it.next()).what == i) {
                it.remove();
            }
        }
    }

    protected final boolean hasDeferredMessages(int i) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler != null) {
            Iterator it = smHandler.mDeferredMessages.iterator();
            while (it.hasNext()) {
                if (((Message) it.next()).what == i) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    protected final boolean hasMessages(int i) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return false;
        }
        return smHandler.hasMessages(i);
    }

    protected final boolean isQuit(Message message) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return message.what == -1;
        }
        return smHandler.isQuit(message);
    }

    public final void quit() {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.quit();
    }

    public final void quitNow() {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.quitNow();
    }

    public boolean isDbg() {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return false;
        }
        return smHandler.isDbg();
    }

    public void setDbg(boolean z) {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.setDbg(z);
    }

    public void start() {
        SmHandler smHandler = this.mSmHandler;
        if (smHandler == null) {
            return;
        }
        smHandler.completeConstruction();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(getName() + SettingsStringUtil.DELIMITER);
        printWriter.println(" total records=" + getLogRecCount());
        for (int i = 0; i < getLogRecSize(); i++) {
            printWriter.println(" rec[" + i + "]: " + getLogRec(i).toString());
            printWriter.flush();
        }
        printWriter.println("curState=" + getCurrentState().getName());
    }

    public String toString() {
        String string;
        String string2;
        try {
            string = this.mName.toString();
        } catch (NullPointerException e) {
            string = "(null)";
        }
        try {
            string2 = this.mSmHandler.getCurrentState().getName().toString();
        } catch (NullPointerException e2) {
            string2 = "(null)";
        }
        return "name=" + string + " state=" + string2;
    }

    protected void logAndAddLogRec(String str) {
        addLogRec(str);
        log(str);
    }

    protected void log(String str) {
        Log.d(this.mName, str);
    }

    protected void logd(String str) {
        Log.d(this.mName, str);
    }

    protected void logv(String str) {
        Log.v(this.mName, str);
    }

    protected void logi(String str) {
        Log.i(this.mName, str);
    }

    protected void logw(String str) {
        Log.w(this.mName, str);
    }

    protected void loge(String str) {
        Log.e(this.mName, str);
    }

    protected void loge(String str, Throwable th) {
        Log.e(this.mName, str, th);
    }
}
