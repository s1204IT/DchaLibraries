package com.android.systemui.doze;

import android.os.Trace;
import android.util.Log;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.util.Preconditions;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.Assert;
import com.android.systemui.util.wakelock.WakeLock;
import java.io.PrintWriter;
import java.util.ArrayList;

public class DozeMachine {
    static final boolean DEBUG = DozeService.DEBUG;
    private final AmbientDisplayConfiguration mConfig;
    private final Service mDozeService;
    private Part[] mParts;
    private int mPulseReason;
    private final WakeLock mWakeLock;
    private final ArrayList<State> mQueuedRequests = new ArrayList<>();
    private State mState = State.UNINITIALIZED;
    private boolean mWakeLockHeldForCurrentState = false;

    public enum State {
        UNINITIALIZED,
        INITIALIZED,
        DOZE,
        DOZE_AOD,
        DOZE_REQUEST_PULSE,
        DOZE_PULSING,
        DOZE_PULSE_DONE,
        FINISH,
        DOZE_AOD_PAUSED,
        DOZE_AOD_PAUSING;

        boolean canPulse() {
            switch (this) {
                case DOZE:
                case DOZE_AOD:
                case DOZE_AOD_PAUSED:
                case DOZE_AOD_PAUSING:
                    return true;
                default:
                    return false;
            }
        }

        boolean staysAwake() {
            switch (this) {
                case DOZE_REQUEST_PULSE:
                case DOZE_PULSING:
                    return true;
                default:
                    return false;
            }
        }

        int screenState(DozeParameters dozeParameters) {
            switch (this) {
                case DOZE:
                case DOZE_AOD_PAUSED:
                    return 1;
                case DOZE_AOD:
                case DOZE_AOD_PAUSING:
                    return 4;
                case DOZE_REQUEST_PULSE:
                case UNINITIALIZED:
                case INITIALIZED:
                    return dozeParameters.shouldControlScreenOff() ? 2 : 1;
                case DOZE_PULSING:
                    return 2;
                default:
                    return 0;
            }
        }
    }

    public DozeMachine(Service service, AmbientDisplayConfiguration ambientDisplayConfiguration, WakeLock wakeLock) {
        this.mDozeService = service;
        this.mConfig = ambientDisplayConfiguration;
        this.mWakeLock = wakeLock;
    }

    public void setParts(Part[] partArr) {
        Preconditions.checkState(this.mParts == null);
        this.mParts = partArr;
    }

    public void requestState(State state) {
        Preconditions.checkArgument(state != State.DOZE_REQUEST_PULSE);
        requestState(state, -1);
    }

    public void requestPulse(int i) {
        Preconditions.checkState(!isExecutingTransition());
        requestState(State.DOZE_REQUEST_PULSE, i);
    }

    private void requestState(State state, int i) {
        Assert.isMainThread();
        if (DEBUG) {
            Log.i("DozeMachine", "request: current=" + this.mState + " req=" + state, new Throwable("here"));
        }
        boolean z = !isExecutingTransition();
        this.mQueuedRequests.add(state);
        if (z) {
            this.mWakeLock.acquire();
            for (int i2 = 0; i2 < this.mQueuedRequests.size(); i2++) {
                transitionTo(this.mQueuedRequests.get(i2), i);
            }
            this.mQueuedRequests.clear();
            this.mWakeLock.release();
        }
    }

    public State getState() {
        Assert.isMainThread();
        Preconditions.checkState(!isExecutingTransition());
        return this.mState;
    }

    public int getPulseReason() {
        Assert.isMainThread();
        Preconditions.checkState(this.mState == State.DOZE_REQUEST_PULSE || this.mState == State.DOZE_PULSING || this.mState == State.DOZE_PULSE_DONE, "must be in pulsing state, but is " + this.mState);
        return this.mPulseReason;
    }

    public void wakeUp() {
        this.mDozeService.requestWakeUp();
    }

    private boolean isExecutingTransition() {
        return !this.mQueuedRequests.isEmpty();
    }

    private void transitionTo(State state, int i) {
        State stateTransitionPolicy = transitionPolicy(state);
        if (DEBUG) {
            Log.i("DozeMachine", "transition: old=" + this.mState + " req=" + state + " new=" + stateTransitionPolicy);
        }
        if (stateTransitionPolicy == this.mState) {
            return;
        }
        validateTransition(stateTransitionPolicy);
        State state2 = this.mState;
        this.mState = stateTransitionPolicy;
        DozeLog.traceState(stateTransitionPolicy);
        Trace.traceCounter(4096L, "doze_machine_state", stateTransitionPolicy.ordinal());
        updatePulseReason(stateTransitionPolicy, state2, i);
        performTransitionOnComponents(state2, stateTransitionPolicy);
        updateWakeLockState(stateTransitionPolicy);
        resolveIntermediateState(stateTransitionPolicy);
    }

    private void updatePulseReason(State state, State state2, int i) {
        if (state == State.DOZE_REQUEST_PULSE) {
            this.mPulseReason = i;
        } else if (state2 == State.DOZE_PULSE_DONE) {
            this.mPulseReason = -1;
        }
    }

    private void performTransitionOnComponents(State state, State state2) {
        for (Part part : this.mParts) {
            part.transitionTo(state, state2);
        }
        if (AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State[state2.ordinal()] == 9) {
            this.mDozeService.finish();
        }
    }

    private void validateTransition(State state) {
        try {
            int i = AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State[this.mState.ordinal()];
            if (i == 7) {
                Preconditions.checkState(state == State.INITIALIZED);
            } else if (i == 9) {
                Preconditions.checkState(state == State.FINISH);
            }
            switch (state) {
                case DOZE_PULSING:
                    Preconditions.checkState(this.mState == State.DOZE_REQUEST_PULSE);
                    return;
                case UNINITIALIZED:
                    throw new IllegalArgumentException("can't transition to UNINITIALIZED");
                case INITIALIZED:
                    Preconditions.checkState(this.mState == State.UNINITIALIZED);
                    return;
                case FINISH:
                default:
                    return;
                case DOZE_PULSE_DONE:
                    if (this.mState == State.DOZE_REQUEST_PULSE || this.mState == State.DOZE_PULSING) {
                        z = true;
                    }
                    Preconditions.checkState(z);
                    return;
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("Illegal Transition: " + this.mState + " -> " + state, e);
        }
    }

    private State transitionPolicy(State state) {
        if (this.mState == State.FINISH) {
            return State.FINISH;
        }
        if ((this.mState == State.DOZE_AOD_PAUSED || this.mState == State.DOZE_AOD_PAUSING || this.mState == State.DOZE_AOD || this.mState == State.DOZE) && state == State.DOZE_PULSE_DONE) {
            Log.i("DozeMachine", "Dropping pulse done because current state is already done: " + this.mState);
            return this.mState;
        }
        if (state == State.DOZE_REQUEST_PULSE && !this.mState.canPulse()) {
            Log.i("DozeMachine", "Dropping pulse request because current state can't pulse: " + this.mState);
            return this.mState;
        }
        return state;
    }

    private void updateWakeLockState(State state) {
        boolean zStaysAwake = state.staysAwake();
        if (this.mWakeLockHeldForCurrentState && !zStaysAwake) {
            this.mWakeLock.release();
            this.mWakeLockHeldForCurrentState = false;
        } else if (!this.mWakeLockHeldForCurrentState && zStaysAwake) {
            this.mWakeLock.acquire();
            this.mWakeLockHeldForCurrentState = true;
        }
    }

    private void resolveIntermediateState(State state) {
        int i = AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State[state.ordinal()];
        if (i == 8 || i == 10) {
            transitionTo(this.mConfig.alwaysOnEnabled(-2) ? State.DOZE_AOD : State.DOZE, -1);
        }
    }

    public void dump(PrintWriter printWriter) {
        printWriter.print(" state=");
        printWriter.println(this.mState);
        printWriter.print(" wakeLockHeldForCurrentState=");
        printWriter.println(this.mWakeLockHeldForCurrentState);
        printWriter.println("Parts:");
        for (Part part : this.mParts) {
            part.dump(printWriter);
        }
    }

    public interface Part {
        void transitionTo(State state, State state2);

        default void dump(PrintWriter printWriter) {
        }
    }

    public interface Service {
        void finish();

        void requestWakeUp();

        void setDozeScreenBrightness(int i);

        void setDozeScreenState(int i);

        public static class Delegate implements Service {
            private final Service mDelegate;

            public Delegate(Service service) {
                this.mDelegate = service;
            }

            @Override
            public void finish() {
                this.mDelegate.finish();
            }

            @Override
            public void setDozeScreenState(int i) {
                this.mDelegate.setDozeScreenState(i);
            }

            @Override
            public void requestWakeUp() {
                this.mDelegate.requestWakeUp();
            }

            @Override
            public void setDozeScreenBrightness(int i) {
                this.mDelegate.setDozeScreenBrightness(i);
            }
        }
    }
}
