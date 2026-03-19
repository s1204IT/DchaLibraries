package com.android.server.accessibility;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardInterceptor implements EventStreamTransformation {
    private AccessibilityManagerService mAms;
    private EventStreamTransformation mNext;

    public KeyboardInterceptor(AccessibilityManagerService service) {
        this.mAms = service;
    }

    @Override
    public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (this.mNext == null) {
            return;
        }
        this.mNext.onMotionEvent(event, rawEvent, policyFlags);
    }

    @Override
    public void onKeyEvent(KeyEvent event, int policyFlags) {
        this.mAms.notifyKeyEvent(event, policyFlags);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (this.mNext == null) {
            return;
        }
        this.mNext.onAccessibilityEvent(event);
    }

    @Override
    public void setNext(EventStreamTransformation next) {
        this.mNext = next;
    }

    @Override
    public void clearEvents(int inputSource) {
        if (this.mNext == null) {
            return;
        }
        this.mNext.clearEvents(inputSource);
    }

    @Override
    public void onDestroy() {
    }
}
