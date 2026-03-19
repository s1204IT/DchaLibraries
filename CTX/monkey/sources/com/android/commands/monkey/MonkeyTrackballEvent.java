package com.android.commands.monkey;

public class MonkeyTrackballEvent extends MonkeyMotionEvent {
    public MonkeyTrackballEvent(int i) {
        super(2, 65540, i);
    }

    @Override
    protected String getTypeLabel() {
        return "Trackball";
    }
}
