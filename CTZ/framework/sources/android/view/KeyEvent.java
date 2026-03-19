package android.view;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.view.KeyCharacterMap;

public class KeyEvent extends InputEvent implements Parcelable {
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_MULTIPLE = 2;
    public static final int ACTION_UP = 1;
    static final boolean DEBUG = false;
    public static final int FLAG_CANCELED = 32;
    public static final int FLAG_CANCELED_LONG_PRESS = 256;
    public static final int FLAG_EDITOR_ACTION = 16;
    public static final int FLAG_FALLBACK = 1024;
    public static final int FLAG_FROM_SYSTEM = 8;
    public static final int FLAG_KEEP_TOUCH_MODE = 4;
    public static final int FLAG_LONG_PRESS = 128;
    public static final int FLAG_PREDISPATCH = 536870912;
    public static final int FLAG_SOFT_KEYBOARD = 2;
    public static final int FLAG_START_TRACKING = 1073741824;
    public static final int FLAG_TAINTED = Integer.MIN_VALUE;
    public static final int FLAG_TRACKING = 512;
    public static final int FLAG_VIRTUAL_HARD_KEY = 64;

    @Deprecated
    public static final int FLAG_WOKE_HERE = 1;
    public static final int KEYCODE_0 = 7;
    public static final int KEYCODE_1 = 8;
    public static final int KEYCODE_11 = 227;
    public static final int KEYCODE_12 = 228;
    public static final int KEYCODE_2 = 9;
    public static final int KEYCODE_3 = 10;
    public static final int KEYCODE_3D_MODE = 206;
    public static final int KEYCODE_4 = 11;
    public static final int KEYCODE_5 = 12;
    public static final int KEYCODE_6 = 13;
    public static final int KEYCODE_7 = 14;
    public static final int KEYCODE_8 = 15;
    public static final int KEYCODE_9 = 16;
    public static final int KEYCODE_A = 29;
    public static final int KEYCODE_ALL_APPS = 284;
    public static final int KEYCODE_ALT_LEFT = 57;
    public static final int KEYCODE_ALT_RIGHT = 58;
    public static final int KEYCODE_APOSTROPHE = 75;
    public static final int KEYCODE_APP_SWITCH = 187;
    public static final int KEYCODE_ASSIST = 219;
    public static final int KEYCODE_AT = 77;
    public static final int KEYCODE_AVR_INPUT = 182;
    public static final int KEYCODE_AVR_POWER = 181;
    public static final int KEYCODE_B = 30;
    public static final int KEYCODE_BACK = 4;
    public static final int KEYCODE_BACKSLASH = 73;
    public static final int KEYCODE_BOOKMARK = 174;
    public static final int KEYCODE_BREAK = 121;
    public static final int KEYCODE_BRIGHTNESS_DOWN = 220;
    public static final int KEYCODE_BRIGHTNESS_UP = 221;
    public static final int KEYCODE_BUTTON_1 = 188;
    public static final int KEYCODE_BUTTON_10 = 197;
    public static final int KEYCODE_BUTTON_11 = 198;
    public static final int KEYCODE_BUTTON_12 = 199;
    public static final int KEYCODE_BUTTON_13 = 200;
    public static final int KEYCODE_BUTTON_14 = 201;
    public static final int KEYCODE_BUTTON_15 = 202;
    public static final int KEYCODE_BUTTON_16 = 203;
    public static final int KEYCODE_BUTTON_2 = 189;
    public static final int KEYCODE_BUTTON_3 = 190;
    public static final int KEYCODE_BUTTON_4 = 191;
    public static final int KEYCODE_BUTTON_5 = 192;
    public static final int KEYCODE_BUTTON_6 = 193;
    public static final int KEYCODE_BUTTON_7 = 194;
    public static final int KEYCODE_BUTTON_8 = 195;
    public static final int KEYCODE_BUTTON_9 = 196;
    public static final int KEYCODE_BUTTON_A = 96;
    public static final int KEYCODE_BUTTON_B = 97;
    public static final int KEYCODE_BUTTON_C = 98;
    public static final int KEYCODE_BUTTON_L1 = 102;
    public static final int KEYCODE_BUTTON_L2 = 104;
    public static final int KEYCODE_BUTTON_MODE = 110;
    public static final int KEYCODE_BUTTON_R1 = 103;
    public static final int KEYCODE_BUTTON_R2 = 105;
    public static final int KEYCODE_BUTTON_SELECT = 109;
    public static final int KEYCODE_BUTTON_START = 108;
    public static final int KEYCODE_BUTTON_THUMBL = 106;
    public static final int KEYCODE_BUTTON_THUMBR = 107;
    public static final int KEYCODE_BUTTON_X = 99;
    public static final int KEYCODE_BUTTON_Y = 100;
    public static final int KEYCODE_BUTTON_Z = 101;
    public static final int KEYCODE_C = 31;
    public static final int KEYCODE_CALCULATOR = 210;
    public static final int KEYCODE_CALENDAR = 208;
    public static final int KEYCODE_CALL = 5;
    public static final int KEYCODE_CAMERA = 27;
    public static final int KEYCODE_CAPS_LOCK = 115;
    public static final int KEYCODE_CAPTIONS = 175;
    public static final int KEYCODE_CHANNEL_DOWN = 167;
    public static final int KEYCODE_CHANNEL_UP = 166;
    public static final int KEYCODE_CLEAR = 28;
    public static final int KEYCODE_COMMA = 55;
    public static final int KEYCODE_CONTACTS = 207;
    public static final int KEYCODE_COPY = 278;
    public static final int KEYCODE_CTRL_LEFT = 113;
    public static final int KEYCODE_CTRL_RIGHT = 114;
    public static final int KEYCODE_CUT = 277;
    public static final int KEYCODE_D = 32;
    public static final int KEYCODE_DEL = 67;
    public static final int KEYCODE_DPAD_CENTER = 23;
    public static final int KEYCODE_DPAD_DOWN = 20;
    public static final int KEYCODE_DPAD_DOWN_LEFT = 269;
    public static final int KEYCODE_DPAD_DOWN_RIGHT = 271;
    public static final int KEYCODE_DPAD_LEFT = 21;
    public static final int KEYCODE_DPAD_RIGHT = 22;
    public static final int KEYCODE_DPAD_UP = 19;
    public static final int KEYCODE_DPAD_UP_LEFT = 268;
    public static final int KEYCODE_DPAD_UP_RIGHT = 270;
    public static final int KEYCODE_DVR = 173;
    public static final int KEYCODE_E = 33;
    public static final int KEYCODE_EISU = 212;
    public static final int KEYCODE_ENDCALL = 6;
    public static final int KEYCODE_ENTER = 66;
    public static final int KEYCODE_ENVELOPE = 65;
    public static final int KEYCODE_EQUALS = 70;
    public static final int KEYCODE_ESCAPE = 111;
    public static final int KEYCODE_EXPLORER = 64;
    public static final int KEYCODE_F = 34;
    public static final int KEYCODE_F1 = 131;
    public static final int KEYCODE_F10 = 140;
    public static final int KEYCODE_F11 = 141;
    public static final int KEYCODE_F12 = 142;
    public static final int KEYCODE_F2 = 132;
    public static final int KEYCODE_F3 = 133;
    public static final int KEYCODE_F4 = 134;
    public static final int KEYCODE_F5 = 135;
    public static final int KEYCODE_F6 = 136;
    public static final int KEYCODE_F7 = 137;
    public static final int KEYCODE_F8 = 138;
    public static final int KEYCODE_F9 = 139;
    public static final int KEYCODE_FOCUS = 80;
    public static final int KEYCODE_FORWARD = 125;
    public static final int KEYCODE_FORWARD_DEL = 112;
    public static final int KEYCODE_FUNCTION = 119;
    public static final int KEYCODE_G = 35;
    public static final int KEYCODE_GRAVE = 68;
    public static final int KEYCODE_GUIDE = 172;
    public static final int KEYCODE_H = 36;
    public static final int KEYCODE_HEADSETHOOK = 79;
    public static final int KEYCODE_HELP = 259;
    public static final int KEYCODE_HENKAN = 214;
    public static final int KEYCODE_HOME = 3;
    public static final int KEYCODE_I = 37;
    public static final int KEYCODE_INFO = 165;
    public static final int KEYCODE_INSERT = 124;
    public static final int KEYCODE_J = 38;
    public static final int KEYCODE_K = 39;
    public static final int KEYCODE_KANA = 218;
    public static final int KEYCODE_KATAKANA_HIRAGANA = 215;
    public static final int KEYCODE_L = 40;
    public static final int KEYCODE_LANGUAGE_SWITCH = 204;
    public static final int KEYCODE_LAST_CHANNEL = 229;
    public static final int KEYCODE_LEFT_BRACKET = 71;
    public static final int KEYCODE_M = 41;
    public static final int KEYCODE_MANNER_MODE = 205;
    public static final int KEYCODE_MEDIA_AUDIO_TRACK = 222;
    public static final int KEYCODE_MEDIA_CLOSE = 128;
    public static final int KEYCODE_MEDIA_EJECT = 129;
    public static final int KEYCODE_MEDIA_FAST_FORWARD = 90;
    public static final int KEYCODE_MEDIA_NEXT = 87;
    public static final int KEYCODE_MEDIA_PAUSE = 127;
    public static final int KEYCODE_MEDIA_PLAY = 126;
    public static final int KEYCODE_MEDIA_PLAY_PAUSE = 85;
    public static final int KEYCODE_MEDIA_PREVIOUS = 88;
    public static final int KEYCODE_MEDIA_RECORD = 130;
    public static final int KEYCODE_MEDIA_REWIND = 89;
    public static final int KEYCODE_MEDIA_SKIP_BACKWARD = 273;
    public static final int KEYCODE_MEDIA_SKIP_FORWARD = 272;
    public static final int KEYCODE_MEDIA_STEP_BACKWARD = 275;
    public static final int KEYCODE_MEDIA_STEP_FORWARD = 274;
    public static final int KEYCODE_MEDIA_STOP = 86;
    public static final int KEYCODE_MEDIA_TOP_MENU = 226;
    public static final int KEYCODE_MENU = 82;
    public static final int KEYCODE_META_LEFT = 117;
    public static final int KEYCODE_META_RIGHT = 118;
    public static final int KEYCODE_MINUS = 69;
    public static final int KEYCODE_MOVE_END = 123;
    public static final int KEYCODE_MOVE_HOME = 122;
    public static final int KEYCODE_MUHENKAN = 213;
    public static final int KEYCODE_MUSIC = 209;
    public static final int KEYCODE_MUTE = 91;
    public static final int KEYCODE_N = 42;
    public static final int KEYCODE_NAVIGATE_IN = 262;
    public static final int KEYCODE_NAVIGATE_NEXT = 261;
    public static final int KEYCODE_NAVIGATE_OUT = 263;
    public static final int KEYCODE_NAVIGATE_PREVIOUS = 260;
    public static final int KEYCODE_NOTIFICATION = 83;
    public static final int KEYCODE_NUM = 78;
    public static final int KEYCODE_NUMPAD_0 = 144;
    public static final int KEYCODE_NUMPAD_1 = 145;
    public static final int KEYCODE_NUMPAD_2 = 146;
    public static final int KEYCODE_NUMPAD_3 = 147;
    public static final int KEYCODE_NUMPAD_4 = 148;
    public static final int KEYCODE_NUMPAD_5 = 149;
    public static final int KEYCODE_NUMPAD_6 = 150;
    public static final int KEYCODE_NUMPAD_7 = 151;
    public static final int KEYCODE_NUMPAD_8 = 152;
    public static final int KEYCODE_NUMPAD_9 = 153;
    public static final int KEYCODE_NUMPAD_ADD = 157;
    public static final int KEYCODE_NUMPAD_COMMA = 159;
    public static final int KEYCODE_NUMPAD_DIVIDE = 154;
    public static final int KEYCODE_NUMPAD_DOT = 158;
    public static final int KEYCODE_NUMPAD_ENTER = 160;
    public static final int KEYCODE_NUMPAD_EQUALS = 161;
    public static final int KEYCODE_NUMPAD_LEFT_PAREN = 162;
    public static final int KEYCODE_NUMPAD_MULTIPLY = 155;
    public static final int KEYCODE_NUMPAD_RIGHT_PAREN = 163;
    public static final int KEYCODE_NUMPAD_SUBTRACT = 156;
    public static final int KEYCODE_NUM_LOCK = 143;
    public static final int KEYCODE_O = 43;
    public static final int KEYCODE_P = 44;
    public static final int KEYCODE_PAGE_DOWN = 93;
    public static final int KEYCODE_PAGE_UP = 92;
    public static final int KEYCODE_PAIRING = 225;
    public static final int KEYCODE_PASTE = 279;
    public static final int KEYCODE_PERIOD = 56;
    public static final int KEYCODE_PICTSYMBOLS = 94;
    public static final int KEYCODE_PLUS = 81;
    public static final int KEYCODE_POUND = 18;
    public static final int KEYCODE_POWER = 26;
    public static final int KEYCODE_PROG_BLUE = 186;
    public static final int KEYCODE_PROG_GREEN = 184;
    public static final int KEYCODE_PROG_RED = 183;
    public static final int KEYCODE_PROG_YELLOW = 185;
    public static final int KEYCODE_Q = 45;
    public static final int KEYCODE_R = 46;
    public static final int KEYCODE_REFRESH = 285;
    public static final int KEYCODE_RIGHT_BRACKET = 72;
    public static final int KEYCODE_RO = 217;
    public static final int KEYCODE_S = 47;
    public static final int KEYCODE_SCROLL_LOCK = 116;
    public static final int KEYCODE_SEARCH = 84;
    public static final int KEYCODE_SEMICOLON = 74;
    public static final int KEYCODE_SETTINGS = 176;
    public static final int KEYCODE_SHIFT_LEFT = 59;
    public static final int KEYCODE_SHIFT_RIGHT = 60;
    public static final int KEYCODE_SLASH = 76;
    public static final int KEYCODE_SLEEP = 223;
    public static final int KEYCODE_SOFT_LEFT = 1;
    public static final int KEYCODE_SOFT_RIGHT = 2;
    public static final int KEYCODE_SOFT_SLEEP = 276;
    public static final int KEYCODE_SPACE = 62;
    public static final int KEYCODE_STAR = 17;
    public static final int KEYCODE_STB_INPUT = 180;
    public static final int KEYCODE_STB_POWER = 179;
    public static final int KEYCODE_STEM_1 = 265;
    public static final int KEYCODE_STEM_2 = 266;
    public static final int KEYCODE_STEM_3 = 267;
    public static final int KEYCODE_STEM_PRIMARY = 264;
    public static final int KEYCODE_SWITCH_CHARSET = 95;
    public static final int KEYCODE_SYM = 63;
    public static final int KEYCODE_SYSRQ = 120;
    public static final int KEYCODE_SYSTEM_NAVIGATION_DOWN = 281;
    public static final int KEYCODE_SYSTEM_NAVIGATION_LEFT = 282;
    public static final int KEYCODE_SYSTEM_NAVIGATION_RIGHT = 283;
    public static final int KEYCODE_SYSTEM_NAVIGATION_UP = 280;
    public static final int KEYCODE_T = 48;
    public static final int KEYCODE_TAB = 61;
    public static final int KEYCODE_TV = 170;
    public static final int KEYCODE_TV_ANTENNA_CABLE = 242;
    public static final int KEYCODE_TV_AUDIO_DESCRIPTION = 252;
    public static final int KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN = 254;
    public static final int KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP = 253;
    public static final int KEYCODE_TV_CONTENTS_MENU = 256;
    public static final int KEYCODE_TV_DATA_SERVICE = 230;
    public static final int KEYCODE_TV_INPUT = 178;
    public static final int KEYCODE_TV_INPUT_COMPONENT_1 = 249;
    public static final int KEYCODE_TV_INPUT_COMPONENT_2 = 250;
    public static final int KEYCODE_TV_INPUT_COMPOSITE_1 = 247;
    public static final int KEYCODE_TV_INPUT_COMPOSITE_2 = 248;
    public static final int KEYCODE_TV_INPUT_HDMI_1 = 243;
    public static final int KEYCODE_TV_INPUT_HDMI_2 = 244;
    public static final int KEYCODE_TV_INPUT_HDMI_3 = 245;
    public static final int KEYCODE_TV_INPUT_HDMI_4 = 246;
    public static final int KEYCODE_TV_INPUT_VGA_1 = 251;
    public static final int KEYCODE_TV_MEDIA_CONTEXT_MENU = 257;
    public static final int KEYCODE_TV_NETWORK = 241;
    public static final int KEYCODE_TV_NUMBER_ENTRY = 234;
    public static final int KEYCODE_TV_POWER = 177;
    public static final int KEYCODE_TV_RADIO_SERVICE = 232;
    public static final int KEYCODE_TV_SATELLITE = 237;
    public static final int KEYCODE_TV_SATELLITE_BS = 238;
    public static final int KEYCODE_TV_SATELLITE_CS = 239;
    public static final int KEYCODE_TV_SATELLITE_SERVICE = 240;
    public static final int KEYCODE_TV_TELETEXT = 233;
    public static final int KEYCODE_TV_TERRESTRIAL_ANALOG = 235;
    public static final int KEYCODE_TV_TERRESTRIAL_DIGITAL = 236;
    public static final int KEYCODE_TV_TIMER_PROGRAMMING = 258;
    public static final int KEYCODE_TV_ZOOM_MODE = 255;
    public static final int KEYCODE_U = 49;
    public static final int KEYCODE_UNKNOWN = 0;
    public static final int KEYCODE_V = 50;
    public static final int KEYCODE_VOICE_ASSIST = 231;
    public static final int KEYCODE_VOLUME_DOWN = 25;
    public static final int KEYCODE_VOLUME_MUTE = 164;
    public static final int KEYCODE_VOLUME_UP = 24;
    public static final int KEYCODE_W = 51;
    public static final int KEYCODE_WAKEUP = 224;
    public static final int KEYCODE_WINDOW = 171;
    public static final int KEYCODE_X = 52;
    public static final int KEYCODE_Y = 53;
    public static final int KEYCODE_YEN = 216;
    public static final int KEYCODE_Z = 54;
    public static final int KEYCODE_ZENKAKU_HANKAKU = 211;
    public static final int KEYCODE_ZOOM_IN = 168;
    public static final int KEYCODE_ZOOM_OUT = 169;
    private static final String LABEL_PREFIX = "KEYCODE_";
    private static final int LAST_KEYCODE = 285;

    @Deprecated
    public static final int MAX_KEYCODE = 84;
    private static final int MAX_RECYCLED = 10;
    private static final int META_ALL_MASK = 7827711;
    public static final int META_ALT_LEFT_ON = 16;
    public static final int META_ALT_LOCKED = 512;
    public static final int META_ALT_MASK = 50;
    public static final int META_ALT_ON = 2;
    public static final int META_ALT_RIGHT_ON = 32;
    public static final int META_CAPS_LOCK_ON = 1048576;
    public static final int META_CAP_LOCKED = 256;
    public static final int META_CTRL_LEFT_ON = 8192;
    public static final int META_CTRL_MASK = 28672;
    public static final int META_CTRL_ON = 4096;
    public static final int META_CTRL_RIGHT_ON = 16384;
    public static final int META_FUNCTION_ON = 8;
    private static final int META_INVALID_MODIFIER_MASK = 7343872;
    private static final int META_LOCK_MASK = 7340032;
    public static final int META_META_LEFT_ON = 131072;
    public static final int META_META_MASK = 458752;
    public static final int META_META_ON = 65536;
    public static final int META_META_RIGHT_ON = 262144;
    private static final int META_MODIFIER_MASK = 487679;
    public static final int META_NUM_LOCK_ON = 2097152;
    public static final int META_SCROLL_LOCK_ON = 4194304;
    public static final int META_SELECTING = 2048;
    public static final int META_SHIFT_LEFT_ON = 64;
    public static final int META_SHIFT_MASK = 193;
    public static final int META_SHIFT_ON = 1;
    public static final int META_SHIFT_RIGHT_ON = 128;
    public static final int META_SYM_LOCKED = 1024;
    public static final int META_SYM_ON = 4;
    private static final int META_SYNTHETIC_MASK = 3840;
    static final String TAG = "KeyEvent";
    private static KeyEvent gRecyclerTop;
    private static int gRecyclerUsed;
    private int mAction;
    private String mCharacters;
    private int mDeviceId;
    private long mDownTime;
    private long mEventTime;
    private int mFlags;
    private int mKeyCode;
    private int mMetaState;
    private KeyEvent mNext;
    private int mRepeatCount;
    private int mScanCode;
    private int mSource;
    private static final String[] META_SYMBOLIC_NAMES = {"META_SHIFT_ON", "META_ALT_ON", "META_SYM_ON", "META_FUNCTION_ON", "META_ALT_LEFT_ON", "META_ALT_RIGHT_ON", "META_SHIFT_LEFT_ON", "META_SHIFT_RIGHT_ON", "META_CAP_LOCKED", "META_ALT_LOCKED", "META_SYM_LOCKED", "0x00000800", "META_CTRL_ON", "META_CTRL_LEFT_ON", "META_CTRL_RIGHT_ON", "0x00008000", "META_META_ON", "META_META_LEFT_ON", "META_META_RIGHT_ON", "0x00080000", "META_CAPS_LOCK_ON", "META_NUM_LOCK_ON", "META_SCROLL_LOCK_ON", "0x00800000", "0x01000000", "0x02000000", "0x04000000", "0x08000000", "0x10000000", "0x20000000", "0x40000000", "0x80000000"};
    private static final Object gRecyclerLock = new Object();
    public static final Parcelable.Creator<KeyEvent> CREATOR = new Parcelable.Creator<KeyEvent>() {
        @Override
        public KeyEvent createFromParcel(Parcel parcel) {
            parcel.readInt();
            return KeyEvent.createFromParcelBody(parcel);
        }

        @Override
        public KeyEvent[] newArray(int i) {
            return new KeyEvent[i];
        }
    };

    public interface Callback {
        boolean onKeyDown(int i, KeyEvent keyEvent);

        boolean onKeyLongPress(int i, KeyEvent keyEvent);

        boolean onKeyMultiple(int i, int i2, KeyEvent keyEvent);

        boolean onKeyUp(int i, KeyEvent keyEvent);
    }

    private static native int nativeKeyCodeFromString(String str);

    private static native String nativeKeyCodeToString(int i);

    static int access$076(KeyEvent keyEvent, int i) {
        int i2 = i | keyEvent.mFlags;
        keyEvent.mFlags = i2;
        return i2;
    }

    public static int getMaxKeyCode() {
        return 285;
    }

    public static int getDeadChar(int i, int i2) {
        return KeyCharacterMap.getDeadChar(i, i2);
    }

    private KeyEvent() {
    }

    public KeyEvent(int i, int i2) {
        this.mAction = i;
        this.mKeyCode = i2;
        this.mRepeatCount = 0;
        this.mDeviceId = -1;
    }

    public KeyEvent(long j, long j2, int i, int i2, int i3) {
        this.mDownTime = j;
        this.mEventTime = j2;
        this.mAction = i;
        this.mKeyCode = i2;
        this.mRepeatCount = i3;
        this.mDeviceId = -1;
    }

    public KeyEvent(long j, long j2, int i, int i2, int i3, int i4) {
        this.mDownTime = j;
        this.mEventTime = j2;
        this.mAction = i;
        this.mKeyCode = i2;
        this.mRepeatCount = i3;
        this.mMetaState = i4;
        this.mDeviceId = -1;
    }

    public KeyEvent(long j, long j2, int i, int i2, int i3, int i4, int i5, int i6) {
        this.mDownTime = j;
        this.mEventTime = j2;
        this.mAction = i;
        this.mKeyCode = i2;
        this.mRepeatCount = i3;
        this.mMetaState = i4;
        this.mDeviceId = i5;
        this.mScanCode = i6;
    }

    public KeyEvent(long j, long j2, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        this.mDownTime = j;
        this.mEventTime = j2;
        this.mAction = i;
        this.mKeyCode = i2;
        this.mRepeatCount = i3;
        this.mMetaState = i4;
        this.mDeviceId = i5;
        this.mScanCode = i6;
        this.mFlags = i7;
    }

    public KeyEvent(long j, long j2, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        this.mDownTime = j;
        this.mEventTime = j2;
        this.mAction = i;
        this.mKeyCode = i2;
        this.mRepeatCount = i3;
        this.mMetaState = i4;
        this.mDeviceId = i5;
        this.mScanCode = i6;
        this.mFlags = i7;
        this.mSource = i8;
    }

    public KeyEvent(long j, String str, int i, int i2) {
        this.mDownTime = j;
        this.mEventTime = j;
        this.mCharacters = str;
        this.mAction = 2;
        this.mKeyCode = 0;
        this.mRepeatCount = 0;
        this.mDeviceId = i;
        this.mFlags = i2;
        this.mSource = 257;
    }

    public KeyEvent(KeyEvent keyEvent) {
        this.mDownTime = keyEvent.mDownTime;
        this.mEventTime = keyEvent.mEventTime;
        this.mAction = keyEvent.mAction;
        this.mKeyCode = keyEvent.mKeyCode;
        this.mRepeatCount = keyEvent.mRepeatCount;
        this.mMetaState = keyEvent.mMetaState;
        this.mDeviceId = keyEvent.mDeviceId;
        this.mSource = keyEvent.mSource;
        this.mScanCode = keyEvent.mScanCode;
        this.mFlags = keyEvent.mFlags;
        this.mCharacters = keyEvent.mCharacters;
    }

    @Deprecated
    public KeyEvent(KeyEvent keyEvent, long j, int i) {
        this.mDownTime = keyEvent.mDownTime;
        this.mEventTime = j;
        this.mAction = keyEvent.mAction;
        this.mKeyCode = keyEvent.mKeyCode;
        this.mRepeatCount = i;
        this.mMetaState = keyEvent.mMetaState;
        this.mDeviceId = keyEvent.mDeviceId;
        this.mSource = keyEvent.mSource;
        this.mScanCode = keyEvent.mScanCode;
        this.mFlags = keyEvent.mFlags;
        this.mCharacters = keyEvent.mCharacters;
    }

    private static KeyEvent obtain() {
        synchronized (gRecyclerLock) {
            KeyEvent keyEvent = gRecyclerTop;
            if (keyEvent == null) {
                return new KeyEvent();
            }
            gRecyclerTop = keyEvent.mNext;
            gRecyclerUsed--;
            keyEvent.mNext = null;
            keyEvent.prepareForReuse();
            return keyEvent;
        }
    }

    public static KeyEvent obtain(long j, long j2, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, String str) {
        KeyEvent keyEventObtain = obtain();
        keyEventObtain.mDownTime = j;
        keyEventObtain.mEventTime = j2;
        keyEventObtain.mAction = i;
        keyEventObtain.mKeyCode = i2;
        keyEventObtain.mRepeatCount = i3;
        keyEventObtain.mMetaState = i4;
        keyEventObtain.mDeviceId = i5;
        keyEventObtain.mScanCode = i6;
        keyEventObtain.mFlags = i7;
        keyEventObtain.mSource = i8;
        keyEventObtain.mCharacters = str;
        return keyEventObtain;
    }

    public static KeyEvent obtain(KeyEvent keyEvent) {
        KeyEvent keyEventObtain = obtain();
        keyEventObtain.mDownTime = keyEvent.mDownTime;
        keyEventObtain.mEventTime = keyEvent.mEventTime;
        keyEventObtain.mAction = keyEvent.mAction;
        keyEventObtain.mKeyCode = keyEvent.mKeyCode;
        keyEventObtain.mRepeatCount = keyEvent.mRepeatCount;
        keyEventObtain.mMetaState = keyEvent.mMetaState;
        keyEventObtain.mDeviceId = keyEvent.mDeviceId;
        keyEventObtain.mScanCode = keyEvent.mScanCode;
        keyEventObtain.mFlags = keyEvent.mFlags;
        keyEventObtain.mSource = keyEvent.mSource;
        keyEventObtain.mCharacters = keyEvent.mCharacters;
        return keyEventObtain;
    }

    @Override
    public KeyEvent copy() {
        return obtain(this);
    }

    @Override
    public final void recycle() {
        super.recycle();
        this.mCharacters = null;
        synchronized (gRecyclerLock) {
            if (gRecyclerUsed < 10) {
                gRecyclerUsed++;
                this.mNext = gRecyclerTop;
                gRecyclerTop = this;
            }
        }
    }

    @Override
    public final void recycleIfNeededAfterDispatch() {
    }

    public static KeyEvent changeTimeRepeat(KeyEvent keyEvent, long j, int i) {
        return new KeyEvent(keyEvent, j, i);
    }

    public static KeyEvent changeTimeRepeat(KeyEvent keyEvent, long j, int i, int i2) {
        KeyEvent keyEvent2 = new KeyEvent(keyEvent);
        keyEvent2.mEventTime = j;
        keyEvent2.mRepeatCount = i;
        keyEvent2.mFlags = i2;
        return keyEvent2;
    }

    private KeyEvent(KeyEvent keyEvent, int i) {
        this.mDownTime = keyEvent.mDownTime;
        this.mEventTime = keyEvent.mEventTime;
        this.mAction = i;
        this.mKeyCode = keyEvent.mKeyCode;
        this.mRepeatCount = keyEvent.mRepeatCount;
        this.mMetaState = keyEvent.mMetaState;
        this.mDeviceId = keyEvent.mDeviceId;
        this.mSource = keyEvent.mSource;
        this.mScanCode = keyEvent.mScanCode;
        this.mFlags = keyEvent.mFlags;
    }

    public static KeyEvent changeAction(KeyEvent keyEvent, int i) {
        return new KeyEvent(keyEvent, i);
    }

    public static KeyEvent changeFlags(KeyEvent keyEvent, int i) {
        KeyEvent keyEvent2 = new KeyEvent(keyEvent);
        keyEvent2.mFlags = i;
        return keyEvent2;
    }

    @Override
    public final boolean isTainted() {
        return (this.mFlags & Integer.MIN_VALUE) != 0;
    }

    @Override
    public final void setTainted(boolean z) {
        this.mFlags = z ? this.mFlags | Integer.MIN_VALUE : this.mFlags & Integer.MAX_VALUE;
    }

    @Deprecated
    public final boolean isDown() {
        return this.mAction == 0;
    }

    public final boolean isSystem() {
        return isSystemKey(this.mKeyCode);
    }

    public final boolean isWakeKey() {
        return isWakeKey(this.mKeyCode);
    }

    public static final boolean isGamepadButton(int i) {
        switch (i) {
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
                return true;
            default:
                switch (i) {
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                        return true;
                    default:
                        return false;
                }
        }
    }

    public static final boolean isConfirmKey(int i) {
        if (i == 23 || i == 62 || i == 66 || i == 160) {
            return true;
        }
        return false;
    }

    public static final boolean isMediaKey(int i) {
        if (i == 79 || i == 130) {
            return true;
        }
        switch (i) {
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
                return true;
            default:
                switch (i) {
                    case 126:
                    case 127:
                        return true;
                    default:
                        return false;
                }
        }
    }

    public static final boolean isSystemKey(int i) {
        if (i == 82 || i == 130 || i == 164) {
            return true;
        }
        switch (i) {
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                return true;
            default:
                switch (i) {
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                        return true;
                    default:
                        switch (i) {
                            case 79:
                            case 80:
                                return true;
                            default:
                                switch (i) {
                                    case 84:
                                    case 85:
                                    case 86:
                                    case 87:
                                    case 88:
                                    case 89:
                                    case 90:
                                    case 91:
                                        return true;
                                    default:
                                        switch (i) {
                                            case 126:
                                            case 127:
                                                return true;
                                            default:
                                                switch (i) {
                                                    case 220:
                                                    case 221:
                                                    case 222:
                                                        return true;
                                                    default:
                                                        switch (i) {
                                                            case 280:
                                                            case 281:
                                                            case 282:
                                                            case 283:
                                                                return true;
                                                            default:
                                                                return false;
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
    }

    public static final boolean isWakeKey(int i) {
        if (i == 4 || i == 82) {
            return true;
        }
        switch (i) {
            case 224:
            case 225:
                return true;
            default:
                switch (i) {
                    case 265:
                    case 266:
                    case 267:
                        return true;
                    default:
                        return false;
                }
        }
    }

    public static final boolean isMetaKey(int i) {
        return i == 117 || i == 118;
    }

    public static final boolean isAltKey(int i) {
        return i == 57 || i == 58;
    }

    @Override
    public final int getDeviceId() {
        return this.mDeviceId;
    }

    @Override
    public final int getSource() {
        return this.mSource;
    }

    @Override
    public final void setSource(int i) {
        this.mSource = i;
    }

    public final int getMetaState() {
        return this.mMetaState;
    }

    public final int getModifiers() {
        return normalizeMetaState(this.mMetaState) & META_MODIFIER_MASK;
    }

    public final int getFlags() {
        return this.mFlags;
    }

    public static int getModifierMetaStateMask() {
        return META_MODIFIER_MASK;
    }

    public static boolean isModifierKey(int i) {
        if (i == 63 || i == 78) {
            return true;
        }
        switch (i) {
            case 57:
            case 58:
            case 59:
            case 60:
                return true;
            default:
                switch (i) {
                    case 113:
                    case 114:
                        return true;
                    default:
                        switch (i) {
                            case 117:
                            case 118:
                            case 119:
                                return true;
                            default:
                                return false;
                        }
                }
        }
    }

    public static int normalizeMetaState(int i) {
        if ((i & 192) != 0) {
            i |= 1;
        }
        if ((i & 48) != 0) {
            i |= 2;
        }
        if ((i & 24576) != 0) {
            i |= 4096;
        }
        if ((393216 & i) != 0) {
            i |= 65536;
        }
        if ((i & 256) != 0) {
            i |= 1048576;
        }
        if ((i & 512) != 0) {
            i |= 2;
        }
        if ((i & 1024) != 0) {
            i |= 4;
        }
        return i & META_ALL_MASK;
    }

    public static boolean metaStateHasNoModifiers(int i) {
        return (normalizeMetaState(i) & META_MODIFIER_MASK) == 0;
    }

    public static boolean metaStateHasModifiers(int i, int i2) {
        if ((META_INVALID_MODIFIER_MASK & i2) == 0) {
            return metaStateFilterDirectionalModifiers(metaStateFilterDirectionalModifiers(metaStateFilterDirectionalModifiers(metaStateFilterDirectionalModifiers(normalizeMetaState(i) & META_MODIFIER_MASK, i2, 1, 64, 128), i2, 2, 16, 32), i2, 4096, 8192, 16384), i2, 65536, 131072, 262144) == i2;
        }
        throw new IllegalArgumentException("modifiers must not contain META_CAPS_LOCK_ON, META_NUM_LOCK_ON, META_SCROLL_LOCK_ON, META_CAP_LOCKED, META_ALT_LOCKED, META_SYM_LOCKED, or META_SELECTING");
    }

    private static int metaStateFilterDirectionalModifiers(int i, int i2, int i3, int i4, int i5) {
        boolean z = (i2 & i3) != 0;
        int i6 = i4 | i5;
        boolean z2 = (i2 & i6) != 0;
        if (z) {
            if (z2) {
                throw new IllegalArgumentException("modifiers must not contain " + metaStateToString(i3) + " combined with " + metaStateToString(i4) + " or " + metaStateToString(i5));
            }
            return i & (~i6);
        }
        if (z2) {
            return i & (~i3);
        }
        return i;
    }

    public final boolean hasNoModifiers() {
        return metaStateHasNoModifiers(this.mMetaState);
    }

    public final boolean hasModifiers(int i) {
        return metaStateHasModifiers(this.mMetaState, i);
    }

    public final boolean isAltPressed() {
        return (this.mMetaState & 2) != 0;
    }

    public final boolean isShiftPressed() {
        return (this.mMetaState & 1) != 0;
    }

    public final boolean isSymPressed() {
        return (this.mMetaState & 4) != 0;
    }

    public final boolean isCtrlPressed() {
        return (this.mMetaState & 4096) != 0;
    }

    public final boolean isMetaPressed() {
        return (this.mMetaState & 65536) != 0;
    }

    public final boolean isFunctionPressed() {
        return (this.mMetaState & 8) != 0;
    }

    public final boolean isCapsLockOn() {
        return (this.mMetaState & 1048576) != 0;
    }

    public final boolean isNumLockOn() {
        return (this.mMetaState & 2097152) != 0;
    }

    public final boolean isScrollLockOn() {
        return (this.mMetaState & 4194304) != 0;
    }

    public final int getAction() {
        return this.mAction;
    }

    public final boolean isCanceled() {
        return (this.mFlags & 32) != 0;
    }

    @Override
    public final void cancel() {
        this.mFlags |= 32;
    }

    public final void startTracking() {
        this.mFlags |= 1073741824;
    }

    public final boolean isTracking() {
        return (this.mFlags & 512) != 0;
    }

    public final boolean isLongPress() {
        return (this.mFlags & 128) != 0;
    }

    public final int getKeyCode() {
        return this.mKeyCode;
    }

    public final String getCharacters() {
        return this.mCharacters;
    }

    public final int getScanCode() {
        return this.mScanCode;
    }

    public final int getRepeatCount() {
        return this.mRepeatCount;
    }

    public final long getDownTime() {
        return this.mDownTime;
    }

    @Override
    public final long getEventTime() {
        return this.mEventTime;
    }

    @Override
    public final long getEventTimeNano() {
        return this.mEventTime * TimeUtils.NANOS_PER_MS;
    }

    @Deprecated
    public final int getKeyboardDevice() {
        return this.mDeviceId;
    }

    public final KeyCharacterMap getKeyCharacterMap() {
        return KeyCharacterMap.load(this.mDeviceId);
    }

    public char getDisplayLabel() {
        return getKeyCharacterMap().getDisplayLabel(this.mKeyCode);
    }

    public int getUnicodeChar() {
        return getUnicodeChar(this.mMetaState);
    }

    public int getUnicodeChar(int i) {
        return getKeyCharacterMap().get(this.mKeyCode, i);
    }

    @Deprecated
    public boolean getKeyData(KeyCharacterMap.KeyData keyData) {
        return getKeyCharacterMap().getKeyData(this.mKeyCode, keyData);
    }

    public char getMatch(char[] cArr) {
        return getMatch(cArr, 0);
    }

    public char getMatch(char[] cArr, int i) {
        return getKeyCharacterMap().getMatch(this.mKeyCode, cArr, i);
    }

    public char getNumber() {
        return getKeyCharacterMap().getNumber(this.mKeyCode);
    }

    public boolean isPrintingKey() {
        return getKeyCharacterMap().isPrintingKey(this.mKeyCode);
    }

    @Deprecated
    public final boolean dispatch(Callback callback) {
        return dispatch(callback, null, null);
    }

    public final boolean dispatch(Callback callback, DispatcherState dispatcherState, Object obj) {
        switch (this.mAction) {
            case 0:
                this.mFlags &= -1073741825;
                boolean zOnKeyDown = callback.onKeyDown(this.mKeyCode, this);
                if (dispatcherState != null) {
                    if (zOnKeyDown && this.mRepeatCount == 0 && (this.mFlags & 1073741824) != 0) {
                        dispatcherState.startTracking(this, obj);
                        return zOnKeyDown;
                    }
                    if (isLongPress() && dispatcherState.isTracking(this)) {
                        try {
                            if (callback.onKeyLongPress(this.mKeyCode, this)) {
                                dispatcherState.performedLongPress(this);
                                return true;
                            }
                            return zOnKeyDown;
                        } catch (AbstractMethodError e) {
                            return zOnKeyDown;
                        }
                    }
                    return zOnKeyDown;
                }
                return zOnKeyDown;
            case 1:
                if (dispatcherState != null) {
                    dispatcherState.handleUpEvent(this);
                }
                return callback.onKeyUp(this.mKeyCode, this);
            case 2:
                int i = this.mRepeatCount;
                int i2 = this.mKeyCode;
                if (callback.onKeyMultiple(i2, i, this)) {
                    return true;
                }
                if (i2 == 0) {
                    return false;
                }
                this.mAction = 0;
                this.mRepeatCount = 0;
                boolean zOnKeyDown2 = callback.onKeyDown(i2, this);
                if (zOnKeyDown2) {
                    this.mAction = 1;
                    callback.onKeyUp(i2, this);
                }
                this.mAction = 2;
                this.mRepeatCount = i;
                return zOnKeyDown2;
            default:
                return false;
        }
    }

    public static class DispatcherState {
        SparseIntArray mActiveLongPresses = new SparseIntArray();
        int mDownKeyCode;
        Object mDownTarget;

        public void reset() {
            this.mDownKeyCode = 0;
            this.mDownTarget = null;
            this.mActiveLongPresses.clear();
        }

        public void reset(Object obj) {
            if (this.mDownTarget == obj) {
                this.mDownKeyCode = 0;
                this.mDownTarget = null;
            }
        }

        public void startTracking(KeyEvent keyEvent, Object obj) {
            if (keyEvent.getAction() != 0) {
                throw new IllegalArgumentException("Can only start tracking on a down event");
            }
            this.mDownKeyCode = keyEvent.getKeyCode();
            this.mDownTarget = obj;
        }

        public boolean isTracking(KeyEvent keyEvent) {
            return this.mDownKeyCode == keyEvent.getKeyCode();
        }

        public void performedLongPress(KeyEvent keyEvent) {
            this.mActiveLongPresses.put(keyEvent.getKeyCode(), 1);
        }

        public void handleUpEvent(KeyEvent keyEvent) {
            int keyCode = keyEvent.getKeyCode();
            int iIndexOfKey = this.mActiveLongPresses.indexOfKey(keyCode);
            if (iIndexOfKey >= 0) {
                KeyEvent.access$076(keyEvent, 288);
                this.mActiveLongPresses.removeAt(iIndexOfKey);
            }
            if (this.mDownKeyCode == keyCode) {
                KeyEvent.access$076(keyEvent, 512);
                this.mDownKeyCode = 0;
                this.mDownTarget = null;
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("KeyEvent { action=");
        sb.append(actionToString(this.mAction));
        sb.append(", keyCode=");
        sb.append(keyCodeToString(this.mKeyCode));
        sb.append(", scanCode=");
        sb.append(this.mScanCode);
        if (this.mCharacters != null) {
            sb.append(", characters=\"");
            sb.append(this.mCharacters);
            sb.append("\"");
        }
        sb.append(", metaState=");
        sb.append(metaStateToString(this.mMetaState));
        sb.append(", flags=0x");
        sb.append(Integer.toHexString(this.mFlags));
        sb.append(", repeatCount=");
        sb.append(this.mRepeatCount);
        sb.append(", eventTime=");
        sb.append(this.mEventTime);
        sb.append(", downTime=");
        sb.append(this.mDownTime);
        sb.append(", deviceId=");
        sb.append(this.mDeviceId);
        sb.append(", source=0x");
        sb.append(Integer.toHexString(this.mSource));
        sb.append(" }");
        return sb.toString();
    }

    public static String actionToString(int i) {
        switch (i) {
            case 0:
                return "ACTION_DOWN";
            case 1:
                return "ACTION_UP";
            case 2:
                return "ACTION_MULTIPLE";
            default:
                return Integer.toString(i);
        }
    }

    public static String keyCodeToString(int i) {
        String strNativeKeyCodeToString = nativeKeyCodeToString(i);
        if (strNativeKeyCodeToString == null) {
            return Integer.toString(i);
        }
        return LABEL_PREFIX + strNativeKeyCodeToString;
    }

    public static int keyCodeFromString(String str) {
        int iNativeKeyCodeFromString;
        if (str.startsWith(LABEL_PREFIX) && (iNativeKeyCodeFromString = nativeKeyCodeFromString((str = str.substring(LABEL_PREFIX.length())))) > 0) {
            return iNativeKeyCodeFromString;
        }
        try {
            return Integer.parseInt(str, 10);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String metaStateToString(int i) {
        if (i == 0) {
            return WifiEnterpriseConfig.ENGINE_DISABLE;
        }
        StringBuilder sb = null;
        int i2 = 0;
        while (i != 0) {
            boolean z = (i & 1) != 0;
            i >>>= 1;
            if (z) {
                String str = META_SYMBOLIC_NAMES[i2];
                if (sb == null) {
                    if (i == 0) {
                        return str;
                    }
                    sb = new StringBuilder(str);
                } else {
                    sb.append('|');
                    sb.append(str);
                }
            }
            i2++;
        }
        return sb.toString();
    }

    public static KeyEvent createFromParcelBody(Parcel parcel) {
        return new KeyEvent(parcel);
    }

    private KeyEvent(Parcel parcel) {
        this.mDeviceId = parcel.readInt();
        this.mSource = parcel.readInt();
        this.mAction = parcel.readInt();
        this.mKeyCode = parcel.readInt();
        this.mRepeatCount = parcel.readInt();
        this.mMetaState = parcel.readInt();
        this.mScanCode = parcel.readInt();
        this.mFlags = parcel.readInt();
        this.mDownTime = parcel.readLong();
        this.mEventTime = parcel.readLong();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(2);
        parcel.writeInt(this.mDeviceId);
        parcel.writeInt(this.mSource);
        parcel.writeInt(this.mAction);
        parcel.writeInt(this.mKeyCode);
        parcel.writeInt(this.mRepeatCount);
        parcel.writeInt(this.mMetaState);
        parcel.writeInt(this.mScanCode);
        parcel.writeInt(this.mFlags);
        parcel.writeLong(this.mDownTime);
        parcel.writeLong(this.mEventTime);
    }
}
