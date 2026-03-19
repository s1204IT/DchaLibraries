package android.view;

public class SoundEffectConstants {
    public static final int CLICK = 0;
    public static final int NAVIGATION_DOWN = 4;
    public static final int NAVIGATION_LEFT = 1;
    public static final int NAVIGATION_RIGHT = 3;
    public static final int NAVIGATION_UP = 2;

    private SoundEffectConstants() {
    }

    public static int getContantForFocusDirection(int i) {
        if (i == 17) {
            return 1;
        }
        if (i == 33) {
            return 2;
        }
        if (i == 66) {
            return 3;
        }
        if (i == 130) {
            return 4;
        }
        switch (i) {
            case 1:
                return 2;
            case 2:
                return 4;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, FOCUS_BACKWARD}.");
        }
    }
}
