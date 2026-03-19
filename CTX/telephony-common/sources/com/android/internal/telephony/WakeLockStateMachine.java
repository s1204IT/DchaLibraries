package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.Rlog;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

public abstract class WakeLockStateMachine extends StateMachine {
    protected static final boolean DBG = true;
    protected static final int EVENT_BROADCAST_COMPLETE = 2;
    public static final int EVENT_NEW_SMS_MESSAGE = 1;
    static final int EVENT_RELEASE_WAKE_LOCK = 3;
    static final int EVENT_UPDATE_PHONE_OBJECT = 4;
    private static final int WAKE_LOCK_TIMEOUT = 3000;
    protected Context mContext;
    protected DefaultState mDefaultState;
    protected IdleState mIdleState;
    protected Phone mPhone;
    protected final BroadcastReceiver mReceiver;
    protected WaitingState mWaitingState;
    private final PowerManager.WakeLock mWakeLock;

    protected abstract boolean handleSmsMessage(Message message);

    protected WakeLockStateMachine(String str, Context context, Phone phone) {
        super(str);
        this.mDefaultState = new DefaultState();
        this.mIdleState = new IdleState();
        this.mWaitingState = new WaitingState();
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                WakeLockStateMachine.this.sendMessage(2);
            }
        };
        this.mContext = context;
        this.mPhone = phone;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, str);
        this.mWakeLock.acquire();
        addState(this.mDefaultState);
        addState(this.mIdleState, this.mDefaultState);
        addState(this.mWaitingState, this.mDefaultState);
        setInitialState(this.mIdleState);
    }

    protected WakeLockStateMachine(String str, Context context, Phone phone, Object obj) {
        super(str);
        this.mDefaultState = new DefaultState();
        this.mIdleState = new IdleState();
        this.mWaitingState = new WaitingState();
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                WakeLockStateMachine.this.sendMessage(2);
            }
        };
        this.mContext = context;
        this.mPhone = phone;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, str);
        this.mWakeLock.acquire();
    }

    public void updatePhoneObject(Phone phone) {
        sendMessage(4, phone);
    }

    public final void dispose() {
        quit();
    }

    protected void onQuitting() {
        while (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
    }

    public final void dispatchSmsMessage(Object obj) {
        sendMessage(1, obj);
    }

    public class DefaultState extends State {
        public DefaultState() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 4) {
                WakeLockStateMachine.this.mPhone = (Phone) message.obj;
                WakeLockStateMachine.this.log("updatePhoneObject: phone=" + WakeLockStateMachine.this.mPhone.getClass().getSimpleName());
                return true;
            }
            String str = "processMessage: unhandled message type " + message.what;
            if (Build.IS_DEBUGGABLE) {
                throw new RuntimeException(str);
            }
            WakeLockStateMachine.this.loge(str);
            return true;
        }
    }

    public class IdleState extends State {
        public IdleState() {
        }

        public void enter() {
            WakeLockStateMachine.this.sendMessageDelayed(3, 3000L);
        }

        public void exit() {
            WakeLockStateMachine.this.mWakeLock.acquire();
            WakeLockStateMachine.this.log("acquired wakelock, leaving Idle state");
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                if (WakeLockStateMachine.this.handleSmsMessage(message)) {
                    WakeLockStateMachine.this.transitionTo(WakeLockStateMachine.this.mWaitingState);
                }
                return true;
            }
            if (i == 3) {
                WakeLockStateMachine.this.mWakeLock.release();
                if (WakeLockStateMachine.this.mWakeLock.isHeld()) {
                    WakeLockStateMachine.this.log("mWakeLock is still held after release");
                } else {
                    WakeLockStateMachine.this.log("mWakeLock released");
                }
                return true;
            }
            return false;
        }
    }

    public class WaitingState extends State {
        public WaitingState() {
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case 1:
                    WakeLockStateMachine.this.log("deferring message until return to idle");
                    WakeLockStateMachine.this.deferMessage(message);
                    break;
                case 2:
                    WakeLockStateMachine.this.log("broadcast complete, returning to idle");
                    WakeLockStateMachine.this.transitionTo(WakeLockStateMachine.this.mIdleState);
                    break;
                case 3:
                    WakeLockStateMachine.this.mWakeLock.release();
                    if (!WakeLockStateMachine.this.mWakeLock.isHeld()) {
                        WakeLockStateMachine.this.loge("mWakeLock released while still in WaitingState!");
                    }
                    break;
            }
            return true;
        }
    }

    protected void log(String str) {
        Rlog.d(getName(), str);
    }

    protected void loge(String str) {
        Rlog.e(getName(), str);
    }

    protected void loge(String str, Throwable th) {
        Rlog.e(getName(), str, th);
    }
}
