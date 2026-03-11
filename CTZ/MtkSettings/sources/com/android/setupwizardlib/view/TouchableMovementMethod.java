package com.android.setupwizardlib.view;

import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

public interface TouchableMovementMethod {
    MotionEvent getLastTouchEvent();

    boolean isLastTouchEventHandled();

    public static class TouchableLinkMovementMethod extends LinkMovementMethod implements TouchableMovementMethod {
        MotionEvent mLastEvent;
        boolean mLastEventResult = false;

        public static TouchableLinkMovementMethod getInstance() {
            return new TouchableLinkMovementMethod();
        }

        @Override
        public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent motionEvent) {
            this.mLastEvent = motionEvent;
            boolean zOnTouchEvent = super.onTouchEvent(textView, spannable, motionEvent);
            if (motionEvent.getAction() == 0) {
                this.mLastEventResult = Selection.getSelectionStart(spannable) != -1;
            } else {
                this.mLastEventResult = zOnTouchEvent;
            }
            return zOnTouchEvent;
        }

        @Override
        public MotionEvent getLastTouchEvent() {
            return this.mLastEvent;
        }

        @Override
        public boolean isLastTouchEventHandled() {
            return this.mLastEventResult;
        }
    }
}
