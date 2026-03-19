package com.android.commands.monkey;

public class MonkeyTouchEvent extends MonkeyMotionEvent {
    public MonkeyTouchEvent(int i) {
        super(1, 4098, i);
    }

    @Override
    protected String getTypeLabel() {
        return "Touch";
    }
}
