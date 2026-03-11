package com.android.systemui.recents.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.util.MutableBoolean;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class EventBus extends BroadcastReceiver {
    private static EventBus sDefaultBus;
    private Handler mHandler;
    private static final Comparator<EventHandler> EVENT_HANDLER_COMPARATOR = new Comparator<EventHandler>() {
        @Override
        public int compare(EventHandler h1, EventHandler h2) {
            if (h1.priority != h2.priority) {
                return h2.priority - h1.priority;
            }
            return Long.compare(h2.subscriber.registrationTime, h1.subscriber.registrationTime);
        }
    };
    private static final Object sLock = new Object();
    private HashMap<Class<? extends Event>, ArrayList<EventHandler>> mEventTypeMap = new HashMap<>();
    private HashMap<Class<? extends Object>, ArrayList<EventHandlerMethod>> mSubscriberTypeMap = new HashMap<>();
    private HashMap<String, Class<? extends InterprocessEvent>> mInterprocessEventNameMap = new HashMap<>();
    private ArrayList<Subscriber> mSubscribers = new ArrayList<>();

    public static class InterprocessEvent extends Event {
    }

    public static class Event implements Cloneable {
        boolean cancelled;
        boolean requiresPost;
        boolean trace;

        protected Event() {
        }

        void onPreDispatch() {
        }

        void onPostDispatch() {
        }

        protected Object clone() throws CloneNotSupportedException {
            Event evt = (Event) super.clone();
            evt.cancelled = false;
            return evt;
        }
    }

    public static class AnimatedEvent extends Event {
        private final ReferenceCountedTrigger mTrigger = new ReferenceCountedTrigger();

        protected AnimatedEvent() {
        }

        public ReferenceCountedTrigger getAnimationTrigger() {
            return this.mTrigger;
        }

        public void addPostAnimationCallback(Runnable r) {
            this.mTrigger.addLastDecrementRunnable(r);
        }

        @Override
        void onPreDispatch() {
            this.mTrigger.increment();
        }

        @Override
        void onPostDispatch() {
            this.mTrigger.decrement();
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }

    public static class ReusableEvent extends Event {
        private int mDispatchCount;

        protected ReusableEvent() {
        }

        @Override
        void onPostDispatch() {
            super.onPostDispatch();
            this.mDispatchCount++;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }

    private EventBus(Looper looper) {
        this.mHandler = new Handler(looper);
    }

    public static EventBus getDefault() {
        if (sDefaultBus == null) {
            synchronized (sLock) {
                if (sDefaultBus == null) {
                    sDefaultBus = new EventBus(Looper.getMainLooper());
                }
            }
        }
        return sDefaultBus;
    }

    public void register(Object subscriber) {
        registerSubscriber(subscriber, 1, null);
    }

    public void register(Object subscriber, int priority) {
        registerSubscriber(subscriber, priority, null);
    }

    public void unregister(Object subscriber) {
        long callingThreadId = Thread.currentThread().getId();
        if (callingThreadId != this.mHandler.getLooper().getThread().getId()) {
            throw new RuntimeException("Can not unregister() a subscriber from a non-main thread.");
        }
        if (!findRegisteredSubscriber(subscriber, true)) {
            return;
        }
        Class<?> subscriberType = subscriber.getClass();
        ArrayList<EventHandlerMethod> subscriberMethods = this.mSubscriberTypeMap.get(subscriberType);
        if (subscriberMethods == null) {
            return;
        }
        for (EventHandlerMethod method : subscriberMethods) {
            ArrayList<EventHandler> eventHandlers = this.mEventTypeMap.get(method.eventType);
            for (int i = eventHandlers.size() - 1; i >= 0; i--) {
                if (eventHandlers.get(i).subscriber.getReference() == subscriber) {
                    eventHandlers.remove(i);
                }
            }
        }
    }

    public void send(Event event) {
        long callingThreadId = Thread.currentThread().getId();
        if (callingThreadId != this.mHandler.getLooper().getThread().getId()) {
            throw new RuntimeException("Can not send() a message from a non-main thread.");
        }
        event.requiresPost = false;
        event.cancelled = false;
        queueEvent(event);
    }

    public void post(Event event) {
        event.requiresPost = true;
        event.cancelled = false;
        queueEvent(event);
    }

    public void sendOntoMainThread(Event event) {
        long callingThreadId = Thread.currentThread().getId();
        if (callingThreadId != this.mHandler.getLooper().getThread().getId()) {
            post(event);
        } else {
            send(event);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle eventBundle = intent.getBundleExtra("interprocess_event_bundle");
        Class<? extends InterprocessEvent> eventType = this.mInterprocessEventNameMap.get(intent.getAction());
        try {
            Constructor<? extends InterprocessEvent> ctor = eventType.getConstructor(Bundle.class);
            send(ctor.newInstance(eventBundle));
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            Log.e("EventBus", "Failed to create InterprocessEvent", e.getCause());
        }
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.println(dumpInternal(prefix));
    }

    public String dumpInternal(String prefix) {
        String innerPrefix = prefix + "  ";
        String innerInnerPrefix = innerPrefix + "  ";
        StringBuilder output = new StringBuilder();
        output.append(prefix);
        output.append("Registered class types:");
        output.append("\n");
        ArrayList<Class<?>> subsciberTypes = new ArrayList<>(this.mSubscriberTypeMap.keySet());
        Collections.sort(subsciberTypes, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                return o1.getSimpleName().compareTo(o2.getSimpleName());
            }
        });
        for (int i = 0; i < subsciberTypes.size(); i++) {
            Class<?> clz = subsciberTypes.get(i);
            output.append(innerPrefix);
            output.append(clz.getSimpleName());
            output.append("\n");
        }
        output.append(prefix);
        output.append("Event map:");
        output.append("\n");
        ArrayList<Class<?>> classes = new ArrayList<>(this.mEventTypeMap.keySet());
        Collections.sort(classes, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                return o1.getSimpleName().compareTo(o2.getSimpleName());
            }
        });
        for (int i2 = 0; i2 < classes.size(); i2++) {
            Class<?> clz2 = classes.get(i2);
            output.append(innerPrefix);
            output.append(clz2.getSimpleName());
            output.append(" -> ");
            output.append("\n");
            ArrayList<EventHandler> handlers = this.mEventTypeMap.get(clz2);
            for (EventHandler handler : handlers) {
                Object subscriber = handler.subscriber.getReference();
                if (subscriber != null) {
                    String id = Integer.toHexString(System.identityHashCode(subscriber));
                    output.append(innerInnerPrefix);
                    output.append(subscriber.getClass().getSimpleName());
                    output.append(" [0x").append(id).append(", #").append(handler.priority).append("]");
                    output.append("\n");
                }
            }
        }
        return output.toString();
    }

    private void registerSubscriber(Object obj, int i, MutableBoolean mutableBoolean) {
        if (Thread.currentThread().getId() != this.mHandler.getLooper().getThread().getId()) {
            throw new RuntimeException("Can not register() a subscriber from a non-main thread.");
        }
        if (findRegisteredSubscriber(obj, false)) {
            return;
        }
        Subscriber subscriber = new Subscriber(obj, SystemClock.uptimeMillis());
        Class<?> cls = obj.getClass();
        ArrayList<EventHandlerMethod> arrayList = this.mSubscriberTypeMap.get(cls);
        if (arrayList != null) {
            for (EventHandlerMethod eventHandlerMethod : arrayList) {
                ArrayList<EventHandler> arrayList2 = this.mEventTypeMap.get(eventHandlerMethod.eventType);
                arrayList2.add(new EventHandler(subscriber, eventHandlerMethod, i));
                sortEventHandlersByPriority(arrayList2);
            }
            this.mSubscribers.add(subscriber);
            return;
        }
        ArrayList<EventHandlerMethod> arrayList3 = new ArrayList<>();
        this.mSubscriberTypeMap.put((Class<? extends Object>) cls, arrayList3);
        this.mSubscribers.add(subscriber);
        MutableBoolean mutableBoolean2 = new MutableBoolean(false);
        for (Method method : cls.getDeclaredMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            mutableBoolean2.value = false;
            if (isValidEventBusHandlerMethod(method, parameterTypes, mutableBoolean2)) {
                Class<?> cls2 = parameterTypes[0];
                ArrayList<EventHandler> arrayList4 = this.mEventTypeMap.get(cls2);
                if (arrayList4 == null) {
                    arrayList4 = new ArrayList<>();
                    this.mEventTypeMap.put((Class<? extends Event>) cls2, arrayList4);
                }
                if (mutableBoolean2.value) {
                    try {
                        cls2.getConstructor(Bundle.class);
                        this.mInterprocessEventNameMap.put(cls2.getName(), (Class<? extends InterprocessEvent>) cls2);
                        if (mutableBoolean != null) {
                            mutableBoolean.value = true;
                        }
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException("Expected InterprocessEvent to have a Bundle constructor");
                    }
                }
                EventHandlerMethod eventHandlerMethod2 = new EventHandlerMethod(method, cls2);
                arrayList4.add(new EventHandler(subscriber, eventHandlerMethod2, i));
                arrayList3.add(eventHandlerMethod2);
                sortEventHandlersByPriority(arrayList4);
            }
        }
    }

    private void queueEvent(final Event event) {
        ArrayList<EventHandler> eventHandlers = this.mEventTypeMap.get(event.getClass());
        if (eventHandlers == null) {
            return;
        }
        boolean hasPostedEvent = false;
        event.onPreDispatch();
        ArrayList<EventHandler> eventHandlers2 = (ArrayList) eventHandlers.clone();
        int eventHandlerCount = eventHandlers2.size();
        for (int i = 0; i < eventHandlerCount; i++) {
            final EventHandler eventHandler = eventHandlers2.get(i);
            if (eventHandler.subscriber.getReference() != null) {
                if (event.requiresPost) {
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            EventBus.this.processEvent(eventHandler, event);
                        }
                    });
                    hasPostedEvent = true;
                } else {
                    processEvent(eventHandler, event);
                }
            }
        }
        if (hasPostedEvent) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    event.onPostDispatch();
                }
            });
        } else {
            event.onPostDispatch();
        }
    }

    public void processEvent(EventHandler eventHandler, Event event) {
        if (event.cancelled) {
            if (event.trace) {
                logWithPid("Event dispatch cancelled");
                return;
            }
            return;
        }
        try {
            if (event.trace) {
                logWithPid(" -> " + eventHandler.toString());
            }
            Object sub = eventHandler.subscriber.getReference();
            if (sub != null) {
                eventHandler.method.invoke(sub, event);
            } else {
                Log.e("EventBus", "Failed to deliver event to null subscriber");
            }
        } catch (IllegalAccessException e) {
            Log.e("EventBus", "Failed to invoke method", e.getCause());
        } catch (InvocationTargetException e2) {
            throw new RuntimeException(e2.getCause());
        }
    }

    private boolean findRegisteredSubscriber(Object subscriber, boolean removeFoundSubscriber) {
        for (int i = this.mSubscribers.size() - 1; i >= 0; i--) {
            Subscriber sub = this.mSubscribers.get(i);
            if (sub.getReference() == subscriber) {
                if (removeFoundSubscriber) {
                    this.mSubscribers.remove(i);
                    return true;
                }
                return true;
            }
        }
        return false;
    }

    private boolean isValidEventBusHandlerMethod(Method method, Class<?>[] parameterTypes, MutableBoolean isInterprocessEventOut) {
        int modifiers = method.getModifiers();
        if (Modifier.isPublic(modifiers) && Modifier.isFinal(modifiers) && method.getReturnType().equals(Void.TYPE) && parameterTypes.length == 1) {
            if (InterprocessEvent.class.isAssignableFrom(parameterTypes[0]) && method.getName().startsWith("onInterprocessBusEvent")) {
                isInterprocessEventOut.value = true;
                return true;
            }
            if (Event.class.isAssignableFrom(parameterTypes[0]) && method.getName().startsWith("onBusEvent")) {
                isInterprocessEventOut.value = false;
                return true;
            }
        }
        return false;
    }

    private void sortEventHandlersByPriority(List<EventHandler> eventHandlers) {
        Collections.sort(eventHandlers, EVENT_HANDLER_COMPARATOR);
    }

    private static void logWithPid(String text) {
        Log.d("EventBus", "[" + Process.myPid() + ", u" + UserHandle.myUserId() + "] " + text);
    }
}
