package com.android.quickstep;

import android.annotation.TargetApi;
import android.view.Choreographer;
import android.view.MotionEvent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Consumer;

@FunctionalInterface
@TargetApi(26)
public interface TouchConsumer extends Consumer<MotionEvent> {
    public static final int INTERACTION_NORMAL = 0;
    public static final int INTERACTION_QUICK_SCRUB = 1;

    @Retention(RetentionPolicy.SOURCE)
    public @interface InteractionType {
    }

    default void reset() {
    }

    default void updateTouchTracking(int i) {
    }

    default void onQuickScrubEnd() {
    }

    default void onQuickScrubProgress(float f) {
    }

    default void onQuickStep(MotionEvent motionEvent) {
    }

    default void onCommand(int i) {
    }

    default void preProcessMotionEvent(MotionEvent motionEvent) {
    }

    default Choreographer getIntrimChoreographer(MotionEventQueue motionEventQueue) {
        return null;
    }

    default void deferInit() {
    }

    default boolean deferNextEventToMainThread() {
        return false;
    }

    default boolean forceToLauncherConsumer() {
        return false;
    }

    default void onShowOverviewFromAltTab() {
    }
}
