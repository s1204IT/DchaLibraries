package com.android.systemui.recents.events.ui.focus;

import com.android.systemui.recents.events.EventBus;

/* loaded from: classes.dex */
public class NavigateTaskViewEvent extends EventBus.Event {
    public Direction direction;

    public enum Direction {
        UNDEFINED,
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    public NavigateTaskViewEvent(Direction direction) {
        this.direction = direction;
    }

    public static Direction getDirectionFromKeyCode(int i) {
        switch (i) {
            case 19:
                return Direction.UP;
            case 20:
                return Direction.DOWN;
            case 21:
                return Direction.LEFT;
            case 22:
                return Direction.RIGHT;
            default:
                return Direction.UNDEFINED;
        }
    }
}
