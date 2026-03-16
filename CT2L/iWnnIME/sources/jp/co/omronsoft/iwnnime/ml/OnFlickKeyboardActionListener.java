package jp.co.omronsoft.iwnnime.ml;

import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.KeyboardView;

public interface OnFlickKeyboardActionListener extends KeyboardView.OnKeyboardActionListener {
    public static final int FLICK_DIRECTION_DOWN = -2;
    public static final int FLICK_DIRECTION_DOWN_INDEX = 4;
    public static final int FLICK_DIRECTION_LEFT = -1;
    public static final int FLICK_DIRECTION_LEFT_INDEX = 1;
    public static final int FLICK_DIRECTION_NEUTRAL = 0;
    public static final int FLICK_DIRECTION_NEUTRAL_INDEX = 0;
    public static final int FLICK_DIRECTION_RIGHT = 1;
    public static final int FLICK_DIRECTION_RIGHT_INDEX = 3;
    public static final int FLICK_DIRECTION_SLIDE1 = 5;
    public static final int FLICK_DIRECTION_SLIDE1_INDEX = 0;
    public static final int FLICK_DIRECTION_SLIDE2 = 6;
    public static final int FLICK_DIRECTION_SLIDE2_INDEX = 1;
    public static final int FLICK_DIRECTION_SLIDE3 = 7;
    public static final int FLICK_DIRECTION_SLIDE3_INDEX = 2;
    public static final int FLICK_DIRECTION_SLIDE4 = 8;
    public static final int FLICK_DIRECTION_SLIDE4_INDEX = 3;
    public static final int FLICK_DIRECTION_SLIDE_NEUTRAL = 9;
    public static final int FLICK_DIRECTION_SLIDE_NEUTRAL_INDEX = 4;
    public static final int FLICK_DIRECTION_SLIDE_NORMAL = 10;
    public static final int FLICK_DIRECTION_SLIDE_NORMAL_INDEX = 5;
    public static final int FLICK_DIRECTION_UP = 2;
    public static final int FLICK_DIRECTION_UP_INDEX = 2;

    void onFlick(int i, int i2);

    boolean onLongPress(Keyboard.Key key);
}
