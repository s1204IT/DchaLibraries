package com.android.server.accessibility;

import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Pools;
import android.view.InputEventConsistencyVerifier;
import android.view.KeyEvent;
import com.android.server.accessibility.AccessibilityManagerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeyEventDispatcher {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "KeyEventDispatcher";
    private static final int MAX_POOL_SIZE = 10;
    private static final int MSG_ON_KEY_EVENT_TIMEOUT = 1;
    private static final long ON_KEY_EVENT_TIMEOUT_MILLIS = 500;
    private final Handler mHandlerToSendKeyEventsToInputFilter;
    private final Handler mKeyEventTimeoutHandler;
    private final Object mLock;
    private final int mMessageTypeForSendKeyEvent;
    private final Pools.Pool<PendingKeyEvent> mPendingEventPool = new Pools.SimplePool(10);
    private final Map<AccessibilityManagerService.Service, ArrayList<PendingKeyEvent>> mPendingEventsMap = new ArrayMap();
    private final PowerManager mPowerManager;
    private final InputEventConsistencyVerifier mSentEventsVerifier;

    public KeyEventDispatcher(Handler handlerToSendKeyEventsToInputFilter, int messageTypeForSendKeyEvent, Object lock, PowerManager powerManager) {
        Callback callback = null;
        if (InputEventConsistencyVerifier.isInstrumentationEnabled()) {
            this.mSentEventsVerifier = new InputEventConsistencyVerifier(this, 0, KeyEventDispatcher.class.getSimpleName());
        } else {
            this.mSentEventsVerifier = null;
        }
        this.mHandlerToSendKeyEventsToInputFilter = handlerToSendKeyEventsToInputFilter;
        this.mMessageTypeForSendKeyEvent = messageTypeForSendKeyEvent;
        this.mKeyEventTimeoutHandler = new Handler(this.mHandlerToSendKeyEventsToInputFilter.getLooper(), new Callback(this, callback));
        this.mLock = lock;
        this.mPowerManager = powerManager;
    }

    public boolean notifyKeyEventLocked(KeyEvent event, int policyFlags, List<AccessibilityManagerService.Service> boundServices) {
        PendingKeyEvent pendingKeyEvent = null;
        KeyEvent localClone = KeyEvent.obtain(event);
        for (int i = 0; i < boundServices.size(); i++) {
            AccessibilityManagerService.Service service = boundServices.get(i);
            if (service.mRequestFilterKeyEvents) {
                int filterKeyEventBit = service.mAccessibilityServiceInfo.getCapabilities() & 8;
                if (filterKeyEventBit != 0) {
                    try {
                        service.mServiceInterface.onKeyEvent(localClone, localClone.getSequenceNumber());
                        if (pendingKeyEvent == null) {
                            pendingKeyEvent = obtainPendingEventLocked(localClone, policyFlags);
                        }
                        ArrayList<PendingKeyEvent> pendingEventList = this.mPendingEventsMap.get(service);
                        if (pendingEventList == null) {
                            pendingEventList = new ArrayList<>();
                            this.mPendingEventsMap.put(service, pendingEventList);
                        }
                        pendingEventList.add(pendingKeyEvent);
                        pendingKeyEvent.referenceCount++;
                    } catch (RemoteException e) {
                    }
                }
            }
        }
        if (pendingKeyEvent == null) {
            localClone.recycle();
            return false;
        }
        Message message = this.mKeyEventTimeoutHandler.obtainMessage(1, pendingKeyEvent);
        this.mKeyEventTimeoutHandler.sendMessageDelayed(message, 500L);
        return true;
    }

    public void setOnKeyEventResult(AccessibilityManagerService.Service service, boolean handled, int sequence) {
        synchronized (this.mLock) {
            PendingKeyEvent pendingEvent = removeEventFromListLocked(this.mPendingEventsMap.get(service), sequence);
            if (pendingEvent != null) {
                if (handled && !pendingEvent.handled) {
                    pendingEvent.handled = handled;
                    long identity = Binder.clearCallingIdentity();
                    try {
                        this.mPowerManager.userActivity(pendingEvent.event.getEventTime(), 3, 0);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
                removeReferenceToPendingEventLocked(pendingEvent);
            }
        }
    }

    public void flush(AccessibilityManagerService.Service service) {
        synchronized (this.mLock) {
            List<PendingKeyEvent> pendingEvents = this.mPendingEventsMap.get(service);
            if (pendingEvents != null) {
                for (int i = 0; i < pendingEvents.size(); i++) {
                    PendingKeyEvent pendingEvent = pendingEvents.get(i);
                    removeReferenceToPendingEventLocked(pendingEvent);
                }
                this.mPendingEventsMap.remove(service);
            }
        }
    }

    private PendingKeyEvent obtainPendingEventLocked(KeyEvent event, int policyFlags) {
        PendingKeyEvent pendingKeyEvent = null;
        PendingKeyEvent pendingEvent = (PendingKeyEvent) this.mPendingEventPool.acquire();
        if (pendingEvent == null) {
            pendingEvent = new PendingKeyEvent(pendingKeyEvent);
        }
        pendingEvent.event = event;
        pendingEvent.policyFlags = policyFlags;
        pendingEvent.referenceCount = 0;
        pendingEvent.handled = false;
        return pendingEvent;
    }

    private static PendingKeyEvent removeEventFromListLocked(List<PendingKeyEvent> listOfEvents, int sequence) {
        for (int i = 0; i < listOfEvents.size(); i++) {
            PendingKeyEvent pendingKeyEvent = listOfEvents.get(i);
            if (pendingKeyEvent.event.getSequenceNumber() == sequence) {
                listOfEvents.remove(pendingKeyEvent);
                return pendingKeyEvent;
            }
        }
        return null;
    }

    private boolean removeReferenceToPendingEventLocked(PendingKeyEvent pendingEvent) {
        int i = pendingEvent.referenceCount - 1;
        pendingEvent.referenceCount = i;
        if (i > 0) {
            return false;
        }
        this.mKeyEventTimeoutHandler.removeMessages(1, pendingEvent);
        if (!pendingEvent.handled) {
            if (this.mSentEventsVerifier != null) {
                this.mSentEventsVerifier.onKeyEvent(pendingEvent.event, 0);
            }
            int policyFlags = pendingEvent.policyFlags | 1073741824;
            this.mHandlerToSendKeyEventsToInputFilter.obtainMessage(this.mMessageTypeForSendKeyEvent, policyFlags, 0, pendingEvent.event).sendToTarget();
        } else {
            pendingEvent.event.recycle();
        }
        this.mPendingEventPool.release(pendingEvent);
        return true;
    }

    private static final class PendingKeyEvent {
        KeyEvent event;
        boolean handled;
        int policyFlags;
        int referenceCount;

        PendingKeyEvent(PendingKeyEvent pendingKeyEvent) {
            this();
        }

        private PendingKeyEvent() {
        }
    }

    private class Callback implements Handler.Callback {
        Callback(KeyEventDispatcher this$0, Callback callback) {
            this();
        }

        private Callback() {
        }

        @Override
        public boolean handleMessage(Message message) {
            if (message.what != 1) {
                throw new IllegalArgumentException("Unknown message: " + message.what);
            }
            PendingKeyEvent pendingKeyEvent = (PendingKeyEvent) message.obj;
            synchronized (KeyEventDispatcher.this.mLock) {
                for (ArrayList<PendingKeyEvent> listForService : KeyEventDispatcher.this.mPendingEventsMap.values()) {
                    if (listForService.remove(pendingKeyEvent) && KeyEventDispatcher.this.removeReferenceToPendingEventLocked(pendingKeyEvent)) {
                        break;
                    }
                }
            }
            return true;
        }
    }
}
