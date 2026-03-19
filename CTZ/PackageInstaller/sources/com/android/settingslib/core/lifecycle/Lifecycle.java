package com.android.settingslib.core.lifecycle;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;
import java.util.List;

public class Lifecycle extends LifecycleRegistry {
    private final List<LifecycleObserver> mObservers;

    @Override
    public void addObserver(android.arch.lifecycle.LifecycleObserver lifecycleObserver) {
        ThreadUtils.ensureMainThread();
        super.addObserver(lifecycleObserver);
        if (lifecycleObserver instanceof LifecycleObserver) {
            this.mObservers.add((LifecycleObserver) lifecycleObserver);
        }
    }

    @Override
    public void removeObserver(android.arch.lifecycle.LifecycleObserver lifecycleObserver) {
        ThreadUtils.ensureMainThread();
        super.removeObserver(lifecycleObserver);
        if (lifecycleObserver instanceof LifecycleObserver) {
            this.mObservers.remove(lifecycleObserver);
        }
    }

    private void onStart() {
        int size = this.mObservers.size();
        for (int i = 0; i < size; i++) {
            LifecycleObserver lifecycleObserver = this.mObservers.get(i);
            if (lifecycleObserver instanceof OnStart) {
                ((OnStart) lifecycleObserver).onStart();
            }
        }
    }

    private void onResume() {
        int size = this.mObservers.size();
        for (int i = 0; i < size; i++) {
            LifecycleObserver lifecycleObserver = this.mObservers.get(i);
            if (lifecycleObserver instanceof OnResume) {
                ((OnResume) lifecycleObserver).onResume();
            }
        }
    }

    private void onPause() {
        int size = this.mObservers.size();
        for (int i = 0; i < size; i++) {
            LifecycleObserver lifecycleObserver = this.mObservers.get(i);
            if (lifecycleObserver instanceof OnPause) {
                ((OnPause) lifecycleObserver).onPause();
            }
        }
    }

    private void onStop() {
        int size = this.mObservers.size();
        for (int i = 0; i < size; i++) {
            LifecycleObserver lifecycleObserver = this.mObservers.get(i);
            if (lifecycleObserver instanceof OnStop) {
                ((OnStop) lifecycleObserver).onStop();
            }
        }
    }

    private void onDestroy() {
        int size = this.mObservers.size();
        for (int i = 0; i < size; i++) {
            LifecycleObserver lifecycleObserver = this.mObservers.get(i);
            if (lifecycleObserver instanceof OnDestroy) {
                ((OnDestroy) lifecycleObserver).onDestroy();
            }
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$arch$lifecycle$Lifecycle$Event = new int[Lifecycle.Event.values().length];

        static {
            try {
                $SwitchMap$android$arch$lifecycle$Lifecycle$Event[Lifecycle.Event.ON_CREATE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$arch$lifecycle$Lifecycle$Event[Lifecycle.Event.ON_START.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$arch$lifecycle$Lifecycle$Event[Lifecycle.Event.ON_RESUME.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$arch$lifecycle$Lifecycle$Event[Lifecycle.Event.ON_PAUSE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$arch$lifecycle$Lifecycle$Event[Lifecycle.Event.ON_STOP.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$arch$lifecycle$Lifecycle$Event[Lifecycle.Event.ON_DESTROY.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$arch$lifecycle$Lifecycle$Event[Lifecycle.Event.ON_ANY.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    private class LifecycleProxy implements android.arch.lifecycle.LifecycleObserver {
        final Lifecycle this$0;

        @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
        public void onLifecycleEvent(LifecycleOwner lifecycleOwner, Lifecycle.Event event) {
            switch (AnonymousClass1.$SwitchMap$android$arch$lifecycle$Lifecycle$Event[event.ordinal()]) {
                case DialogFragment.STYLE_NO_FRAME:
                    this.this$0.onStart();
                    break;
                case DialogFragment.STYLE_NO_INPUT:
                    this.this$0.onResume();
                    break;
                case 4:
                    this.this$0.onPause();
                    break;
                case 5:
                    this.this$0.onStop();
                    break;
                case 6:
                    this.this$0.onDestroy();
                    break;
                case 7:
                    Log.wtf("LifecycleObserver", "Should not receive an 'ANY' event!");
                    break;
            }
        }
    }
}
