package android.view;

import android.annotation.SystemApi;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.CalendarContract;
import android.provider.SettingsStringUtil;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.transition.EpicenterTranslateClipReveal;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;

public interface WindowManager extends ViewManager {
    public static final int DOCKED_BOTTOM = 4;
    public static final int DOCKED_INVALID = -1;
    public static final int DOCKED_LEFT = 1;
    public static final int DOCKED_RIGHT = 3;
    public static final int DOCKED_TOP = 2;
    public static final String INPUT_CONSUMER_NAVIGATION = "nav_input_consumer";
    public static final String INPUT_CONSUMER_PIP = "pip_input_consumer";
    public static final String INPUT_CONSUMER_RECENTS_ANIMATION = "recents_animation_input_consumer";
    public static final String INPUT_CONSUMER_WALLPAPER = "wallpaper_input_consumer";
    public static final String PARCEL_KEY_SHORTCUTS_ARRAY = "shortcuts_array";
    public static final int TAKE_SCREENSHOT_FULLSCREEN = 1;
    public static final int TAKE_SCREENSHOT_SELECTED_REGION = 2;
    public static final int TRANSIT_ACTIVITY_CLOSE = 7;
    public static final int TRANSIT_ACTIVITY_OPEN = 6;
    public static final int TRANSIT_ACTIVITY_RELAUNCH = 18;
    public static final int TRANSIT_CRASHING_ACTIVITY_CLOSE = 26;
    public static final int TRANSIT_DOCK_TASK_FROM_RECENTS = 19;
    public static final int TRANSIT_FLAG_KEYGUARD_GOING_AWAY_NO_ANIMATION = 2;
    public static final int TRANSIT_FLAG_KEYGUARD_GOING_AWAY_TO_SHADE = 1;
    public static final int TRANSIT_FLAG_KEYGUARD_GOING_AWAY_WITH_WALLPAPER = 4;
    public static final int TRANSIT_KEYGUARD_GOING_AWAY = 20;
    public static final int TRANSIT_KEYGUARD_GOING_AWAY_ON_WALLPAPER = 21;
    public static final int TRANSIT_KEYGUARD_OCCLUDE = 22;
    public static final int TRANSIT_KEYGUARD_UNOCCLUDE = 23;
    public static final int TRANSIT_NONE = 0;
    public static final int TRANSIT_TASK_CLOSE = 9;
    public static final int TRANSIT_TASK_IN_PLACE = 17;
    public static final int TRANSIT_TASK_OPEN = 8;
    public static final int TRANSIT_TASK_OPEN_BEHIND = 16;
    public static final int TRANSIT_TASK_TO_BACK = 11;
    public static final int TRANSIT_TASK_TO_FRONT = 10;
    public static final int TRANSIT_TRANSLUCENT_ACTIVITY_CLOSE = 25;
    public static final int TRANSIT_TRANSLUCENT_ACTIVITY_OPEN = 24;
    public static final int TRANSIT_UNSET = -1;
    public static final int TRANSIT_WALLPAPER_CLOSE = 12;
    public static final int TRANSIT_WALLPAPER_INTRA_CLOSE = 15;
    public static final int TRANSIT_WALLPAPER_INTRA_OPEN = 14;
    public static final int TRANSIT_WALLPAPER_OPEN = 13;

    public interface KeyboardShortcutsReceiver {
        void onKeyboardShortcutsReceived(List<KeyboardShortcutGroup> list);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TransitionFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TransitionType {
    }

    @SystemApi
    Region getCurrentImeTouchRegion();

    Display getDefaultDisplay();

    void removeViewImmediate(View view);

    void requestAppKeyboardShortcuts(KeyboardShortcutsReceiver keyboardShortcutsReceiver, int i);

    public static class BadTokenException extends RuntimeException {
        public BadTokenException() {
        }

        public BadTokenException(String str) {
            super(str);
        }
    }

    public static class InvalidDisplayException extends RuntimeException {
        public InvalidDisplayException() {
        }

        public InvalidDisplayException(String str) {
            super(str);
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams implements Parcelable {
        public static final int ACCESSIBILITY_ANCHOR_CHANGED = 16777216;
        public static final int ACCESSIBILITY_TITLE_CHANGED = 33554432;
        public static final int ALPHA_CHANGED = 128;
        public static final int ANIMATION_CHANGED = 16;
        public static final float BRIGHTNESS_OVERRIDE_FULL = 1.0f;
        public static final float BRIGHTNESS_OVERRIDE_NONE = -1.0f;
        public static final float BRIGHTNESS_OVERRIDE_OFF = 0.0f;
        public static final int BUTTON_BRIGHTNESS_CHANGED = 8192;
        public static final int COLOR_MODE_CHANGED = 67108864;
        public static final Parcelable.Creator<LayoutParams> CREATOR = new Parcelable.Creator<LayoutParams>() {
            @Override
            public LayoutParams createFromParcel(Parcel parcel) {
                return new LayoutParams(parcel);
            }

            @Override
            public LayoutParams[] newArray(int i) {
                return new LayoutParams[i];
            }
        };
        public static final int DIM_AMOUNT_CHANGED = 32;
        public static final int EVERYTHING_CHANGED = -1;
        public static final int FIRST_APPLICATION_WINDOW = 1;
        public static final int FIRST_SUB_WINDOW = 1000;
        public static final int FIRST_SYSTEM_WINDOW = 2000;
        public static final int FLAGS_CHANGED = 4;
        public static final int FLAG_ALLOW_LOCK_WHILE_SCREEN_ON = 1;
        public static final int FLAG_ALT_FOCUSABLE_IM = 131072;

        @Deprecated
        public static final int FLAG_BLUR_BEHIND = 4;
        public static final int FLAG_DIM_BEHIND = 2;

        @Deprecated
        public static final int FLAG_DISMISS_KEYGUARD = 4194304;

        @Deprecated
        public static final int FLAG_DITHER = 4096;
        public static final int FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = Integer.MIN_VALUE;
        public static final int FLAG_FORCE_NOT_FULLSCREEN = 2048;
        public static final int FLAG_FULLSCREEN = 1024;
        public static final int FLAG_HARDWARE_ACCELERATED = 16777216;
        public static final int FLAG_IGNORE_CHEEK_PRESSES = 32768;
        public static final int FLAG_IGNORE_NAVIGATION_BAR = 0;
        public static final int FLAG_KEEP_SCREEN_ON = 128;
        public static final int FLAG_LAYOUT_ATTACHED_IN_DECOR = 1073741824;
        public static final int FLAG_LAYOUT_INSET_DECOR = 65536;
        public static final int FLAG_LAYOUT_IN_OVERSCAN = 33554432;
        public static final int FLAG_LAYOUT_IN_SCREEN = 256;
        public static final int FLAG_LAYOUT_NO_LIMITS = 512;
        public static final int FLAG_LOCAL_FOCUS_MODE = 268435456;
        public static final int FLAG_NOT_FOCUSABLE = 8;
        public static final int FLAG_NOT_TOUCHABLE = 16;
        public static final int FLAG_NOT_TOUCH_MODAL = 32;
        public static final int FLAG_SCALED = 16384;
        public static final int FLAG_SECURE = 8192;
        public static final int FLAG_SHOW_WALLPAPER = 1048576;

        @Deprecated
        public static final int FLAG_SHOW_WHEN_LOCKED = 524288;
        public static final int FLAG_SLIPPERY = 536870912;
        public static final int FLAG_SPLIT_TOUCH = 8388608;

        @Deprecated
        public static final int FLAG_TOUCHABLE_WHEN_WAKING = 64;
        public static final int FLAG_TRANSLUCENT_NAVIGATION = 134217728;
        public static final int FLAG_TRANSLUCENT_STATUS = 67108864;

        @Deprecated
        public static final int FLAG_TURN_SCREEN_ON = 2097152;
        public static final int FLAG_WATCH_OUTSIDE_TOUCH = 262144;
        public static final int FORMAT_CHANGED = 8;
        public static final int INPUT_FEATURES_CHANGED = 65536;
        public static final int INPUT_FEATURE_DISABLE_POINTER_GESTURES = 1;
        public static final int INPUT_FEATURE_DISABLE_USER_ACTIVITY = 4;
        public static final int INPUT_FEATURE_NO_INPUT_CHANNEL = 2;
        public static final int INVALID_WINDOW_TYPE = -1;
        public static final int LAST_APPLICATION_WINDOW = 99;
        public static final int LAST_SUB_WINDOW = 1999;
        public static final int LAST_SYSTEM_WINDOW = 2999;
        public static final int LAYOUT_CHANGED = 1;

        @Deprecated
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS = 1;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT = 0;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER = 2;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES = 1;
        public static final int MEMORY_TYPE_CHANGED = 256;

        @Deprecated
        public static final int MEMORY_TYPE_GPU = 2;

        @Deprecated
        public static final int MEMORY_TYPE_HARDWARE = 1;

        @Deprecated
        public static final int MEMORY_TYPE_NORMAL = 0;

        @Deprecated
        public static final int MEMORY_TYPE_PUSH_BUFFERS = 3;
        public static final int NEEDS_MENU_KEY_CHANGED = 4194304;
        public static final int NEEDS_MENU_SET_FALSE = 2;
        public static final int NEEDS_MENU_SET_TRUE = 1;
        public static final int NEEDS_MENU_UNSET = 0;
        public static final int PREFERRED_DISPLAY_MODE_ID = 8388608;
        public static final int PREFERRED_REFRESH_RATE_CHANGED = 2097152;
        public static final int PRIVATE_FLAGS_CHANGED = 131072;
        public static final int PRIVATE_FLAG_ACQUIRES_SLEEP_TOKEN = 2097152;
        public static final int PRIVATE_FLAG_COMPATIBLE_WINDOW = 128;
        public static final int PRIVATE_FLAG_DISABLE_WALLPAPER_TOUCH_EVENTS = 2048;
        public static final int PRIVATE_FLAG_FAKE_HARDWARE_ACCELERATED = 1;
        public static final int PRIVATE_FLAG_FORCE_DECOR_VIEW_VISIBILITY = 16384;
        public static final int PRIVATE_FLAG_FORCE_DRAW_STATUS_BAR_BACKGROUND = 131072;
        public static final int PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED = 2;
        public static final int PRIVATE_FLAG_FORCE_STATUS_BAR_VISIBLE_TRANSPARENT = 4096;
        public static final int PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS = 524288;
        public static final int PRIVATE_FLAG_INHERIT_TRANSLUCENT_DECOR = 512;
        public static final int PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY = 1048576;
        public static final int PRIVATE_FLAG_IS_SCREEN_DECOR = 4194304;
        public static final int PRIVATE_FLAG_KEYGUARD = 1024;
        public static final int PRIVATE_FLAG_LAYOUT_CHILD_WINDOW_IN_PARENT_FRAME = 65536;
        public static final int PRIVATE_FLAG_NO_MOVE_ANIMATION = 64;
        public static final int PRIVATE_FLAG_PRESERVE_GEOMETRY = 8192;
        public static final int PRIVATE_FLAG_SHOW_FOR_ALL_USERS = 16;
        public static final int PRIVATE_FLAG_SUSTAINED_PERFORMANCE_MODE = 262144;
        public static final int PRIVATE_FLAG_SYSTEM_ERROR = 256;
        public static final int PRIVATE_FLAG_WANTS_OFFSET_NOTIFICATIONS = 4;
        public static final int PRIVATE_FLAG_WILL_NOT_REPLACE_ON_RELAUNCH = 32768;
        public static final int ROTATION_ANIMATION_CHANGED = 4096;
        public static final int ROTATION_ANIMATION_CROSSFADE = 1;
        public static final int ROTATION_ANIMATION_JUMPCUT = 2;
        public static final int ROTATION_ANIMATION_ROTATE = 0;
        public static final int ROTATION_ANIMATION_SEAMLESS = 3;
        public static final int ROTATION_ANIMATION_UNSPECIFIED = -1;
        public static final int SCREEN_BRIGHTNESS_CHANGED = 2048;
        public static final int SCREEN_ORIENTATION_CHANGED = 1024;
        public static final int SOFT_INPUT_ADJUST_NOTHING = 48;
        public static final int SOFT_INPUT_ADJUST_PAN = 32;
        public static final int SOFT_INPUT_ADJUST_RESIZE = 16;
        public static final int SOFT_INPUT_ADJUST_UNSPECIFIED = 0;
        public static final int SOFT_INPUT_IS_FORWARD_NAVIGATION = 256;
        public static final int SOFT_INPUT_MASK_ADJUST = 240;
        public static final int SOFT_INPUT_MASK_STATE = 15;
        public static final int SOFT_INPUT_MODE_CHANGED = 512;
        public static final int SOFT_INPUT_STATE_ALWAYS_HIDDEN = 3;
        public static final int SOFT_INPUT_STATE_ALWAYS_VISIBLE = 5;
        public static final int SOFT_INPUT_STATE_HIDDEN = 2;
        public static final int SOFT_INPUT_STATE_UNCHANGED = 1;
        public static final int SOFT_INPUT_STATE_UNSPECIFIED = 0;
        public static final int SOFT_INPUT_STATE_VISIBLE = 4;
        public static final int SURFACE_INSETS_CHANGED = 1048576;
        public static final int SYSTEM_UI_LISTENER_CHANGED = 32768;
        public static final int SYSTEM_UI_VISIBILITY_CHANGED = 16384;
        public static final int TITLE_CHANGED = 64;
        public static final int TRANSLUCENT_FLAGS_CHANGED = 524288;
        public static final int TYPE_ACCESSIBILITY_OVERLAY = 2032;
        public static final int TYPE_APPLICATION = 2;
        public static final int TYPE_APPLICATION_ABOVE_SUB_PANEL = 1005;
        public static final int TYPE_APPLICATION_ATTACHED_DIALOG = 1003;
        public static final int TYPE_APPLICATION_MEDIA = 1001;
        public static final int TYPE_APPLICATION_MEDIA_OVERLAY = 1004;
        public static final int TYPE_APPLICATION_OVERLAY = 2038;
        public static final int TYPE_APPLICATION_PANEL = 1000;
        public static final int TYPE_APPLICATION_STARTING = 3;
        public static final int TYPE_APPLICATION_SUB_PANEL = 1002;
        public static final int TYPE_BASE_APPLICATION = 1;
        public static final int TYPE_BOOT_PROGRESS = 2021;
        public static final int TYPE_CHANGED = 2;
        public static final int TYPE_DISPLAY_OVERLAY = 2026;
        public static final int TYPE_DOCK_DIVIDER = 2034;
        public static final int TYPE_DRAG = 2016;
        public static final int TYPE_DRAWN_APPLICATION = 4;
        public static final int TYPE_DREAM = 2023;
        public static final int TYPE_INPUT_CONSUMER = 2022;
        public static final int TYPE_INPUT_METHOD = 2011;
        public static final int TYPE_INPUT_METHOD_DIALOG = 2012;
        public static final int TYPE_KEYGUARD = 2004;
        public static final int TYPE_KEYGUARD_DIALOG = 2009;
        public static final int TYPE_MAGNIFICATION_OVERLAY = 2027;
        public static final int TYPE_NAVIGATION_BAR = 2019;
        public static final int TYPE_NAVIGATION_BAR_PANEL = 2024;

        @Deprecated
        public static final int TYPE_PHONE = 2002;
        public static final int TYPE_POINTER = 2018;
        public static final int TYPE_PRESENTATION = 2037;

        @Deprecated
        public static final int TYPE_PRIORITY_PHONE = 2007;
        public static final int TYPE_PRIVATE_PRESENTATION = 2030;
        public static final int TYPE_QS_DIALOG = 2035;
        public static final int TYPE_SCREENSHOT = 2036;
        public static final int TYPE_SEARCH_BAR = 2001;
        public static final int TYPE_SECURE_SYSTEM_OVERLAY = 2015;
        public static final int TYPE_STATUS_BAR = 2000;
        public static final int TYPE_STATUS_BAR_PANEL = 2014;
        public static final int TYPE_STATUS_BAR_SUB_PANEL = 2017;

        @Deprecated
        public static final int TYPE_SYSTEM_ALERT = 2003;
        public static final int TYPE_SYSTEM_DIALOG = 2008;

        @Deprecated
        public static final int TYPE_SYSTEM_ERROR = 2010;

        @Deprecated
        public static final int TYPE_SYSTEM_OVERLAY = 2006;

        @Deprecated
        public static final int TYPE_TOAST = 2005;
        public static final int TYPE_TOP_MOST = 2039;
        public static final int TYPE_VOICE_INTERACTION = 2031;
        public static final int TYPE_VOICE_INTERACTION_STARTING = 2033;
        public static final int TYPE_VOLUME_OVERLAY = 2020;
        public static final int TYPE_WALLPAPER = 2013;
        public static final int USER_ACTIVITY_TIMEOUT_CHANGED = 262144;
        public long accessibilityIdOfAnchor;
        public CharSequence accessibilityTitle;
        public float alpha;
        public float buttonBrightness;
        public float dimAmount;

        @ViewDebug.ExportedProperty(flagMapping = {@ViewDebug.FlagToString(equals = 1, mask = 1, name = "ALLOW_LOCK_WHILE_SCREEN_ON"), @ViewDebug.FlagToString(equals = 2, mask = 2, name = "DIM_BEHIND"), @ViewDebug.FlagToString(equals = 4, mask = 4, name = "BLUR_BEHIND"), @ViewDebug.FlagToString(equals = 8, mask = 8, name = "NOT_FOCUSABLE"), @ViewDebug.FlagToString(equals = 16, mask = 16, name = "NOT_TOUCHABLE"), @ViewDebug.FlagToString(equals = 32, mask = 32, name = "NOT_TOUCH_MODAL"), @ViewDebug.FlagToString(equals = 64, mask = 64, name = "TOUCHABLE_WHEN_WAKING"), @ViewDebug.FlagToString(equals = 128, mask = 128, name = "KEEP_SCREEN_ON"), @ViewDebug.FlagToString(equals = 256, mask = 256, name = "LAYOUT_IN_SCREEN"), @ViewDebug.FlagToString(equals = 512, mask = 512, name = "LAYOUT_NO_LIMITS"), @ViewDebug.FlagToString(equals = 1024, mask = 1024, name = "FULLSCREEN"), @ViewDebug.FlagToString(equals = 2048, mask = 2048, name = "FORCE_NOT_FULLSCREEN"), @ViewDebug.FlagToString(equals = 4096, mask = 4096, name = "DITHER"), @ViewDebug.FlagToString(equals = 8192, mask = 8192, name = "SECURE"), @ViewDebug.FlagToString(equals = 16384, mask = 16384, name = "SCALED"), @ViewDebug.FlagToString(equals = 32768, mask = 32768, name = "IGNORE_CHEEK_PRESSES"), @ViewDebug.FlagToString(equals = 65536, mask = 65536, name = "LAYOUT_INSET_DECOR"), @ViewDebug.FlagToString(equals = 131072, mask = 131072, name = "ALT_FOCUSABLE_IM"), @ViewDebug.FlagToString(equals = 262144, mask = 262144, name = "WATCH_OUTSIDE_TOUCH"), @ViewDebug.FlagToString(equals = 524288, mask = 524288, name = "SHOW_WHEN_LOCKED"), @ViewDebug.FlagToString(equals = 1048576, mask = 1048576, name = "SHOW_WALLPAPER"), @ViewDebug.FlagToString(equals = 2097152, mask = 2097152, name = "TURN_SCREEN_ON"), @ViewDebug.FlagToString(equals = 4194304, mask = 4194304, name = "DISMISS_KEYGUARD"), @ViewDebug.FlagToString(equals = 8388608, mask = 8388608, name = "SPLIT_TOUCH"), @ViewDebug.FlagToString(equals = 16777216, mask = 16777216, name = "HARDWARE_ACCELERATED"), @ViewDebug.FlagToString(equals = 33554432, mask = 33554432, name = "LOCAL_FOCUS_MODE"), @ViewDebug.FlagToString(equals = 67108864, mask = 67108864, name = "TRANSLUCENT_STATUS"), @ViewDebug.FlagToString(equals = 134217728, mask = 134217728, name = "TRANSLUCENT_NAVIGATION"), @ViewDebug.FlagToString(equals = 268435456, mask = 268435456, name = "LOCAL_FOCUS_MODE"), @ViewDebug.FlagToString(equals = 536870912, mask = 536870912, name = "FLAG_SLIPPERY"), @ViewDebug.FlagToString(equals = 1073741824, mask = 1073741824, name = "FLAG_LAYOUT_ATTACHED_IN_DECOR"), @ViewDebug.FlagToString(equals = Integer.MIN_VALUE, mask = Integer.MIN_VALUE, name = "DRAWS_SYSTEM_BAR_BACKGROUNDS")}, formatToHexString = true)
        public int flags;
        public int format;
        public int gravity;
        public boolean hasManualSurfaceInsets;
        public boolean hasSystemUiListeners;
        public long hideTimeoutMilliseconds;
        public float horizontalMargin;

        @ViewDebug.ExportedProperty
        public float horizontalWeight;
        public int inputFeatures;
        public int layoutInDisplayCutoutMode;
        private int mColorMode;
        private int[] mCompatibilityParamsBackup;
        private CharSequence mTitle;

        @Deprecated
        public int memoryType;
        public int needsMenuKey;
        public String packageName;
        public int preferredDisplayModeId;

        @Deprecated
        public float preferredRefreshRate;
        public boolean preservePreviousSurfaceInsets;

        @ViewDebug.ExportedProperty(flagMapping = {@ViewDebug.FlagToString(equals = 1, mask = 1, name = "FAKE_HARDWARE_ACCELERATED"), @ViewDebug.FlagToString(equals = 2, mask = 2, name = "FORCE_HARDWARE_ACCELERATED"), @ViewDebug.FlagToString(equals = 4, mask = 4, name = "WANTS_OFFSET_NOTIFICATIONS"), @ViewDebug.FlagToString(equals = 16, mask = 16, name = "SHOW_FOR_ALL_USERS"), @ViewDebug.FlagToString(equals = 64, mask = 64, name = "NO_MOVE_ANIMATION"), @ViewDebug.FlagToString(equals = 128, mask = 128, name = "COMPATIBLE_WINDOW"), @ViewDebug.FlagToString(equals = 256, mask = 256, name = "SYSTEM_ERROR"), @ViewDebug.FlagToString(equals = 512, mask = 512, name = "INHERIT_TRANSLUCENT_DECOR"), @ViewDebug.FlagToString(equals = 1024, mask = 1024, name = "KEYGUARD"), @ViewDebug.FlagToString(equals = 2048, mask = 2048, name = "DISABLE_WALLPAPER_TOUCH_EVENTS"), @ViewDebug.FlagToString(equals = 4096, mask = 4096, name = "FORCE_STATUS_BAR_VISIBLE_TRANSPARENT"), @ViewDebug.FlagToString(equals = 8192, mask = 8192, name = "PRESERVE_GEOMETRY"), @ViewDebug.FlagToString(equals = 16384, mask = 16384, name = "FORCE_DECOR_VIEW_VISIBILITY"), @ViewDebug.FlagToString(equals = 32768, mask = 32768, name = "WILL_NOT_REPLACE_ON_RELAUNCH"), @ViewDebug.FlagToString(equals = 65536, mask = 65536, name = "LAYOUT_CHILD_WINDOW_IN_PARENT_FRAME"), @ViewDebug.FlagToString(equals = 131072, mask = 131072, name = "FORCE_DRAW_STATUS_BAR_BACKGROUND"), @ViewDebug.FlagToString(equals = 262144, mask = 262144, name = "SUSTAINED_PERFORMANCE_MODE"), @ViewDebug.FlagToString(equals = 524288, mask = 524288, name = "HIDE_NON_SYSTEM_OVERLAY_WINDOWS"), @ViewDebug.FlagToString(equals = 1048576, mask = 1048576, name = "IS_ROUNDED_CORNERS_OVERLAY"), @ViewDebug.FlagToString(equals = 2097152, mask = 2097152, name = "ACQUIRES_SLEEP_TOKEN"), @ViewDebug.FlagToString(equals = 4194304, mask = 4194304, name = "IS_SCREEN_DECOR")})
        public int privateFlags;
        public int rotationAnimation;
        public float screenBrightness;
        public int screenOrientation;
        public int softInputMode;
        public int subtreeSystemUiVisibility;
        public final Rect surfaceInsets;
        public int systemUiVisibility;
        public IBinder token;

        @ViewDebug.ExportedProperty(mapping = {@ViewDebug.IntToString(from = 1, to = "BASE_APPLICATION"), @ViewDebug.IntToString(from = 2, to = "APPLICATION"), @ViewDebug.IntToString(from = 3, to = "APPLICATION_STARTING"), @ViewDebug.IntToString(from = 4, to = "DRAWN_APPLICATION"), @ViewDebug.IntToString(from = 1000, to = "APPLICATION_PANEL"), @ViewDebug.IntToString(from = 1001, to = "APPLICATION_MEDIA"), @ViewDebug.IntToString(from = 1002, to = "APPLICATION_SUB_PANEL"), @ViewDebug.IntToString(from = 1005, to = "APPLICATION_ABOVE_SUB_PANEL"), @ViewDebug.IntToString(from = 1003, to = "APPLICATION_ATTACHED_DIALOG"), @ViewDebug.IntToString(from = 1004, to = "APPLICATION_MEDIA_OVERLAY"), @ViewDebug.IntToString(from = 2000, to = "STATUS_BAR"), @ViewDebug.IntToString(from = 2001, to = "SEARCH_BAR"), @ViewDebug.IntToString(from = 2002, to = "PHONE"), @ViewDebug.IntToString(from = 2003, to = "SYSTEM_ALERT"), @ViewDebug.IntToString(from = 2005, to = "TOAST"), @ViewDebug.IntToString(from = 2006, to = "SYSTEM_OVERLAY"), @ViewDebug.IntToString(from = 2007, to = "PRIORITY_PHONE"), @ViewDebug.IntToString(from = 2008, to = "SYSTEM_DIALOG"), @ViewDebug.IntToString(from = TYPE_KEYGUARD_DIALOG, to = "KEYGUARD_DIALOG"), @ViewDebug.IntToString(from = TYPE_SYSTEM_ERROR, to = "SYSTEM_ERROR"), @ViewDebug.IntToString(from = TYPE_INPUT_METHOD, to = "INPUT_METHOD"), @ViewDebug.IntToString(from = TYPE_INPUT_METHOD_DIALOG, to = "INPUT_METHOD_DIALOG"), @ViewDebug.IntToString(from = TYPE_WALLPAPER, to = "WALLPAPER"), @ViewDebug.IntToString(from = TYPE_STATUS_BAR_PANEL, to = "STATUS_BAR_PANEL"), @ViewDebug.IntToString(from = TYPE_SECURE_SYSTEM_OVERLAY, to = "SECURE_SYSTEM_OVERLAY"), @ViewDebug.IntToString(from = TYPE_DRAG, to = "DRAG"), @ViewDebug.IntToString(from = TYPE_STATUS_BAR_SUB_PANEL, to = "STATUS_BAR_SUB_PANEL"), @ViewDebug.IntToString(from = TYPE_POINTER, to = "POINTER"), @ViewDebug.IntToString(from = TYPE_NAVIGATION_BAR, to = "NAVIGATION_BAR"), @ViewDebug.IntToString(from = TYPE_VOLUME_OVERLAY, to = "VOLUME_OVERLAY"), @ViewDebug.IntToString(from = 2021, to = "BOOT_PROGRESS"), @ViewDebug.IntToString(from = 2022, to = "INPUT_CONSUMER"), @ViewDebug.IntToString(from = TYPE_DREAM, to = "DREAM"), @ViewDebug.IntToString(from = TYPE_NAVIGATION_BAR_PANEL, to = "NAVIGATION_BAR_PANEL"), @ViewDebug.IntToString(from = TYPE_DISPLAY_OVERLAY, to = "DISPLAY_OVERLAY"), @ViewDebug.IntToString(from = TYPE_MAGNIFICATION_OVERLAY, to = "MAGNIFICATION_OVERLAY"), @ViewDebug.IntToString(from = TYPE_PRESENTATION, to = "PRESENTATION"), @ViewDebug.IntToString(from = TYPE_PRIVATE_PRESENTATION, to = "PRIVATE_PRESENTATION"), @ViewDebug.IntToString(from = TYPE_VOICE_INTERACTION, to = "VOICE_INTERACTION"), @ViewDebug.IntToString(from = TYPE_VOICE_INTERACTION_STARTING, to = "VOICE_INTERACTION_STARTING"), @ViewDebug.IntToString(from = TYPE_DOCK_DIVIDER, to = "DOCK_DIVIDER"), @ViewDebug.IntToString(from = TYPE_QS_DIALOG, to = "QS_DIALOG"), @ViewDebug.IntToString(from = TYPE_SCREENSHOT, to = "SCREENSHOT"), @ViewDebug.IntToString(from = TYPE_APPLICATION_OVERLAY, to = "APPLICATION_OVERLAY")})
        public int type;
        public long userActivityTimeout;
        public float verticalMargin;

        @ViewDebug.ExportedProperty
        public float verticalWeight;
        public int windowAnimations;

        @ViewDebug.ExportedProperty
        public int x;

        @ViewDebug.ExportedProperty
        public int y;

        @Retention(RetentionPolicy.SOURCE)
        @interface LayoutInDisplayCutoutMode {
        }

        @Retention(RetentionPolicy.SOURCE)
        public @interface SoftInputModeFlags {
        }

        public static boolean isSystemAlertWindowType(int i) {
            switch (i) {
                case 2002:
                case 2003:
                case 2006:
                case 2007:
                case TYPE_SYSTEM_ERROR:
                case TYPE_APPLICATION_OVERLAY:
                    return true;
                default:
                    return false;
            }
        }

        public static boolean mayUseInputMethod(int i) {
            int i2 = i & 131080;
            if (i2 == 0 || i2 == 131080) {
                return true;
            }
            return false;
        }

        public LayoutParams() {
            super(-1, -1);
            this.needsMenuKey = 0;
            this.surfaceInsets = new Rect();
            this.preservePreviousSurfaceInsets = true;
            this.alpha = 1.0f;
            this.dimAmount = 1.0f;
            this.screenBrightness = -1.0f;
            this.buttonBrightness = -1.0f;
            this.rotationAnimation = 0;
            this.token = null;
            this.packageName = null;
            this.screenOrientation = -1;
            this.layoutInDisplayCutoutMode = 0;
            this.userActivityTimeout = -1L;
            this.accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
            this.hideTimeoutMilliseconds = -1L;
            this.mColorMode = 0;
            this.mCompatibilityParamsBackup = null;
            this.mTitle = null;
            this.type = 2;
            this.format = -1;
        }

        public LayoutParams(int i) {
            super(-1, -1);
            this.needsMenuKey = 0;
            this.surfaceInsets = new Rect();
            this.preservePreviousSurfaceInsets = true;
            this.alpha = 1.0f;
            this.dimAmount = 1.0f;
            this.screenBrightness = -1.0f;
            this.buttonBrightness = -1.0f;
            this.rotationAnimation = 0;
            this.token = null;
            this.packageName = null;
            this.screenOrientation = -1;
            this.layoutInDisplayCutoutMode = 0;
            this.userActivityTimeout = -1L;
            this.accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
            this.hideTimeoutMilliseconds = -1L;
            this.mColorMode = 0;
            this.mCompatibilityParamsBackup = null;
            this.mTitle = null;
            this.type = i;
            this.format = -1;
        }

        public LayoutParams(int i, int i2) {
            super(-1, -1);
            this.needsMenuKey = 0;
            this.surfaceInsets = new Rect();
            this.preservePreviousSurfaceInsets = true;
            this.alpha = 1.0f;
            this.dimAmount = 1.0f;
            this.screenBrightness = -1.0f;
            this.buttonBrightness = -1.0f;
            this.rotationAnimation = 0;
            this.token = null;
            this.packageName = null;
            this.screenOrientation = -1;
            this.layoutInDisplayCutoutMode = 0;
            this.userActivityTimeout = -1L;
            this.accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
            this.hideTimeoutMilliseconds = -1L;
            this.mColorMode = 0;
            this.mCompatibilityParamsBackup = null;
            this.mTitle = null;
            this.type = i;
            this.flags = i2;
            this.format = -1;
        }

        public LayoutParams(int i, int i2, int i3) {
            super(-1, -1);
            this.needsMenuKey = 0;
            this.surfaceInsets = new Rect();
            this.preservePreviousSurfaceInsets = true;
            this.alpha = 1.0f;
            this.dimAmount = 1.0f;
            this.screenBrightness = -1.0f;
            this.buttonBrightness = -1.0f;
            this.rotationAnimation = 0;
            this.token = null;
            this.packageName = null;
            this.screenOrientation = -1;
            this.layoutInDisplayCutoutMode = 0;
            this.userActivityTimeout = -1L;
            this.accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
            this.hideTimeoutMilliseconds = -1L;
            this.mColorMode = 0;
            this.mCompatibilityParamsBackup = null;
            this.mTitle = null;
            this.type = i;
            this.flags = i2;
            this.format = i3;
        }

        public LayoutParams(int i, int i2, int i3, int i4, int i5) {
            super(i, i2);
            this.needsMenuKey = 0;
            this.surfaceInsets = new Rect();
            this.preservePreviousSurfaceInsets = true;
            this.alpha = 1.0f;
            this.dimAmount = 1.0f;
            this.screenBrightness = -1.0f;
            this.buttonBrightness = -1.0f;
            this.rotationAnimation = 0;
            this.token = null;
            this.packageName = null;
            this.screenOrientation = -1;
            this.layoutInDisplayCutoutMode = 0;
            this.userActivityTimeout = -1L;
            this.accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
            this.hideTimeoutMilliseconds = -1L;
            this.mColorMode = 0;
            this.mCompatibilityParamsBackup = null;
            this.mTitle = null;
            this.type = i3;
            this.flags = i4;
            this.format = i5;
        }

        public LayoutParams(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
            super(i, i2);
            this.needsMenuKey = 0;
            this.surfaceInsets = new Rect();
            this.preservePreviousSurfaceInsets = true;
            this.alpha = 1.0f;
            this.dimAmount = 1.0f;
            this.screenBrightness = -1.0f;
            this.buttonBrightness = -1.0f;
            this.rotationAnimation = 0;
            this.token = null;
            this.packageName = null;
            this.screenOrientation = -1;
            this.layoutInDisplayCutoutMode = 0;
            this.userActivityTimeout = -1L;
            this.accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
            this.hideTimeoutMilliseconds = -1L;
            this.mColorMode = 0;
            this.mCompatibilityParamsBackup = null;
            this.mTitle = null;
            this.x = i3;
            this.y = i4;
            this.type = i5;
            this.flags = i6;
            this.format = i7;
        }

        public final void setTitle(CharSequence charSequence) {
            if (charSequence == null) {
                charSequence = "";
            }
            this.mTitle = TextUtils.stringOrSpannedString(charSequence);
        }

        public final CharSequence getTitle() {
            return this.mTitle != null ? this.mTitle : "";
        }

        public final void setSurfaceInsets(View view, boolean z, boolean z2) {
            int iCeil = (int) Math.ceil(view.getZ() * 2.0f);
            if (iCeil == 0) {
                this.surfaceInsets.set(0, 0, 0, 0);
            } else {
                this.surfaceInsets.set(Math.max(iCeil, this.surfaceInsets.left), Math.max(iCeil, this.surfaceInsets.top), Math.max(iCeil, this.surfaceInsets.right), Math.max(iCeil, this.surfaceInsets.bottom));
            }
            this.hasManualSurfaceInsets = z;
            this.preservePreviousSurfaceInsets = z2;
        }

        public void setColorMode(int i) {
            this.mColorMode = i;
        }

        public int getColorMode() {
            return this.mColorMode;
        }

        @SystemApi
        public final void setUserActivityTimeout(long j) {
            this.userActivityTimeout = j;
        }

        @SystemApi
        public final long getUserActivityTimeout() {
            return this.userActivityTimeout;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.width);
            parcel.writeInt(this.height);
            parcel.writeInt(this.x);
            parcel.writeInt(this.y);
            parcel.writeInt(this.type);
            parcel.writeInt(this.flags);
            parcel.writeInt(this.privateFlags);
            parcel.writeInt(this.softInputMode);
            parcel.writeInt(this.layoutInDisplayCutoutMode);
            parcel.writeInt(this.gravity);
            parcel.writeFloat(this.horizontalMargin);
            parcel.writeFloat(this.verticalMargin);
            parcel.writeInt(this.format);
            parcel.writeInt(this.windowAnimations);
            parcel.writeFloat(this.alpha);
            parcel.writeFloat(this.dimAmount);
            parcel.writeFloat(this.screenBrightness);
            parcel.writeFloat(this.buttonBrightness);
            parcel.writeInt(this.rotationAnimation);
            parcel.writeStrongBinder(this.token);
            parcel.writeString(this.packageName);
            TextUtils.writeToParcel(this.mTitle, parcel, i);
            parcel.writeInt(this.screenOrientation);
            parcel.writeFloat(this.preferredRefreshRate);
            parcel.writeInt(this.preferredDisplayModeId);
            parcel.writeInt(this.systemUiVisibility);
            parcel.writeInt(this.subtreeSystemUiVisibility);
            parcel.writeInt(this.hasSystemUiListeners ? 1 : 0);
            parcel.writeInt(this.inputFeatures);
            parcel.writeLong(this.userActivityTimeout);
            parcel.writeInt(this.surfaceInsets.left);
            parcel.writeInt(this.surfaceInsets.top);
            parcel.writeInt(this.surfaceInsets.right);
            parcel.writeInt(this.surfaceInsets.bottom);
            parcel.writeInt(this.hasManualSurfaceInsets ? 1 : 0);
            parcel.writeInt(this.preservePreviousSurfaceInsets ? 1 : 0);
            parcel.writeInt(this.needsMenuKey);
            parcel.writeLong(this.accessibilityIdOfAnchor);
            TextUtils.writeToParcel(this.accessibilityTitle, parcel, i);
            parcel.writeInt(this.mColorMode);
            parcel.writeLong(this.hideTimeoutMilliseconds);
        }

        public LayoutParams(Parcel parcel) {
            boolean z;
            boolean z2;
            this.needsMenuKey = 0;
            this.surfaceInsets = new Rect();
            this.preservePreviousSurfaceInsets = true;
            this.alpha = 1.0f;
            this.dimAmount = 1.0f;
            this.screenBrightness = -1.0f;
            this.buttonBrightness = -1.0f;
            this.rotationAnimation = 0;
            this.token = null;
            this.packageName = null;
            this.screenOrientation = -1;
            this.layoutInDisplayCutoutMode = 0;
            this.userActivityTimeout = -1L;
            this.accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
            this.hideTimeoutMilliseconds = -1L;
            this.mColorMode = 0;
            this.mCompatibilityParamsBackup = null;
            this.mTitle = null;
            this.width = parcel.readInt();
            this.height = parcel.readInt();
            this.x = parcel.readInt();
            this.y = parcel.readInt();
            this.type = parcel.readInt();
            this.flags = parcel.readInt();
            this.privateFlags = parcel.readInt();
            this.softInputMode = parcel.readInt();
            this.layoutInDisplayCutoutMode = parcel.readInt();
            this.gravity = parcel.readInt();
            this.horizontalMargin = parcel.readFloat();
            this.verticalMargin = parcel.readFloat();
            this.format = parcel.readInt();
            this.windowAnimations = parcel.readInt();
            this.alpha = parcel.readFloat();
            this.dimAmount = parcel.readFloat();
            this.screenBrightness = parcel.readFloat();
            this.buttonBrightness = parcel.readFloat();
            this.rotationAnimation = parcel.readInt();
            this.token = parcel.readStrongBinder();
            this.packageName = parcel.readString();
            this.mTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.screenOrientation = parcel.readInt();
            this.preferredRefreshRate = parcel.readFloat();
            this.preferredDisplayModeId = parcel.readInt();
            this.systemUiVisibility = parcel.readInt();
            this.subtreeSystemUiVisibility = parcel.readInt();
            if (parcel.readInt() == 0) {
                z = false;
            } else {
                z = true;
            }
            this.hasSystemUiListeners = z;
            this.inputFeatures = parcel.readInt();
            this.userActivityTimeout = parcel.readLong();
            this.surfaceInsets.left = parcel.readInt();
            this.surfaceInsets.top = parcel.readInt();
            this.surfaceInsets.right = parcel.readInt();
            this.surfaceInsets.bottom = parcel.readInt();
            if (parcel.readInt() == 0) {
                z2 = false;
            } else {
                z2 = true;
            }
            this.hasManualSurfaceInsets = z2;
            this.preservePreviousSurfaceInsets = parcel.readInt() != 0;
            this.needsMenuKey = parcel.readInt();
            this.accessibilityIdOfAnchor = parcel.readLong();
            this.accessibilityTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.mColorMode = parcel.readInt();
            this.hideTimeoutMilliseconds = parcel.readLong();
        }

        public final int copyFrom(LayoutParams layoutParams) {
            int i;
            if (this.width != layoutParams.width) {
                this.width = layoutParams.width;
                i = 1;
            } else {
                i = 0;
            }
            if (this.height != layoutParams.height) {
                this.height = layoutParams.height;
                i |= 1;
            }
            if (this.x != layoutParams.x) {
                this.x = layoutParams.x;
                i |= 1;
            }
            if (this.y != layoutParams.y) {
                this.y = layoutParams.y;
                i |= 1;
            }
            if (this.horizontalWeight != layoutParams.horizontalWeight) {
                this.horizontalWeight = layoutParams.horizontalWeight;
                i |= 1;
            }
            if (this.verticalWeight != layoutParams.verticalWeight) {
                this.verticalWeight = layoutParams.verticalWeight;
                i |= 1;
            }
            if (this.horizontalMargin != layoutParams.horizontalMargin) {
                this.horizontalMargin = layoutParams.horizontalMargin;
                i |= 1;
            }
            if (this.verticalMargin != layoutParams.verticalMargin) {
                this.verticalMargin = layoutParams.verticalMargin;
                i |= 1;
            }
            if (this.type != layoutParams.type) {
                this.type = layoutParams.type;
                i |= 2;
            }
            if (this.flags != layoutParams.flags) {
                if (((this.flags ^ layoutParams.flags) & 201326592) != 0) {
                    i |= 524288;
                }
                this.flags = layoutParams.flags;
                i |= 4;
            }
            if (this.privateFlags != layoutParams.privateFlags) {
                this.privateFlags = layoutParams.privateFlags;
                i |= 131072;
            }
            if (this.softInputMode != layoutParams.softInputMode) {
                this.softInputMode = layoutParams.softInputMode;
                i |= 512;
            }
            if (this.layoutInDisplayCutoutMode != layoutParams.layoutInDisplayCutoutMode) {
                this.layoutInDisplayCutoutMode = layoutParams.layoutInDisplayCutoutMode;
                i |= 1;
            }
            if (this.gravity != layoutParams.gravity) {
                this.gravity = layoutParams.gravity;
                i |= 1;
            }
            if (this.format != layoutParams.format) {
                this.format = layoutParams.format;
                i |= 8;
            }
            if (this.windowAnimations != layoutParams.windowAnimations) {
                this.windowAnimations = layoutParams.windowAnimations;
                i |= 16;
            }
            if (this.token == null) {
                this.token = layoutParams.token;
            }
            if (this.packageName == null) {
                this.packageName = layoutParams.packageName;
            }
            if (!Objects.equals(this.mTitle, layoutParams.mTitle) && layoutParams.mTitle != null) {
                this.mTitle = layoutParams.mTitle;
                i |= 64;
            }
            if (this.alpha != layoutParams.alpha) {
                this.alpha = layoutParams.alpha;
                i |= 128;
            }
            if (this.dimAmount != layoutParams.dimAmount) {
                this.dimAmount = layoutParams.dimAmount;
                i |= 32;
            }
            if (this.screenBrightness != layoutParams.screenBrightness) {
                this.screenBrightness = layoutParams.screenBrightness;
                i |= 2048;
            }
            if (this.buttonBrightness != layoutParams.buttonBrightness) {
                this.buttonBrightness = layoutParams.buttonBrightness;
                i |= 8192;
            }
            if (this.rotationAnimation != layoutParams.rotationAnimation) {
                this.rotationAnimation = layoutParams.rotationAnimation;
                i |= 4096;
            }
            if (this.screenOrientation != layoutParams.screenOrientation) {
                this.screenOrientation = layoutParams.screenOrientation;
                i |= 1024;
            }
            if (this.preferredRefreshRate != layoutParams.preferredRefreshRate) {
                this.preferredRefreshRate = layoutParams.preferredRefreshRate;
                i |= 2097152;
            }
            if (this.preferredDisplayModeId != layoutParams.preferredDisplayModeId) {
                this.preferredDisplayModeId = layoutParams.preferredDisplayModeId;
                i |= 8388608;
            }
            if (this.systemUiVisibility != layoutParams.systemUiVisibility || this.subtreeSystemUiVisibility != layoutParams.subtreeSystemUiVisibility) {
                this.systemUiVisibility = layoutParams.systemUiVisibility;
                this.subtreeSystemUiVisibility = layoutParams.subtreeSystemUiVisibility;
                i |= 16384;
            }
            if (this.hasSystemUiListeners != layoutParams.hasSystemUiListeners) {
                this.hasSystemUiListeners = layoutParams.hasSystemUiListeners;
                i |= 32768;
            }
            if (this.inputFeatures != layoutParams.inputFeatures) {
                this.inputFeatures = layoutParams.inputFeatures;
                i |= 65536;
            }
            if (this.userActivityTimeout != layoutParams.userActivityTimeout) {
                this.userActivityTimeout = layoutParams.userActivityTimeout;
                i |= 262144;
            }
            if (!this.surfaceInsets.equals(layoutParams.surfaceInsets)) {
                this.surfaceInsets.set(layoutParams.surfaceInsets);
                i |= 1048576;
            }
            if (this.hasManualSurfaceInsets != layoutParams.hasManualSurfaceInsets) {
                this.hasManualSurfaceInsets = layoutParams.hasManualSurfaceInsets;
                i |= 1048576;
            }
            if (this.preservePreviousSurfaceInsets != layoutParams.preservePreviousSurfaceInsets) {
                this.preservePreviousSurfaceInsets = layoutParams.preservePreviousSurfaceInsets;
                i |= 1048576;
            }
            if (this.needsMenuKey != layoutParams.needsMenuKey) {
                this.needsMenuKey = layoutParams.needsMenuKey;
                i |= 4194304;
            }
            if (this.accessibilityIdOfAnchor != layoutParams.accessibilityIdOfAnchor) {
                this.accessibilityIdOfAnchor = layoutParams.accessibilityIdOfAnchor;
                i |= 16777216;
            }
            if (!Objects.equals(this.accessibilityTitle, layoutParams.accessibilityTitle) && layoutParams.accessibilityTitle != null) {
                this.accessibilityTitle = layoutParams.accessibilityTitle;
                i |= 33554432;
            }
            if (this.mColorMode != layoutParams.mColorMode) {
                this.mColorMode = layoutParams.mColorMode;
                i |= 67108864;
            }
            this.hideTimeoutMilliseconds = layoutParams.hideTimeoutMilliseconds;
            return i;
        }

        @Override
        public String debug(String str) {
            Log.d("Debug", str + "Contents of " + this + SettingsStringUtil.DELIMITER);
            Log.d("Debug", super.debug(""));
            Log.d("Debug", "");
            Log.d("Debug", "WindowManager.LayoutParams={title=" + ((Object) this.mTitle) + "}");
            return "";
        }

        public String toString() {
            return toString("");
        }

        public void dumpDimensions(StringBuilder sb) {
            String strValueOf;
            String strValueOf2;
            sb.append('(');
            sb.append(this.x);
            sb.append(',');
            sb.append(this.y);
            sb.append(")(");
            if (this.width == -1) {
                strValueOf = "fill";
            } else {
                strValueOf = this.width == -2 ? "wrap" : String.valueOf(this.width);
            }
            sb.append(strValueOf);
            sb.append(EpicenterTranslateClipReveal.StateProperty.TARGET_X);
            if (this.height == -1) {
                strValueOf2 = "fill";
            } else {
                strValueOf2 = this.height == -2 ? "wrap" : String.valueOf(this.height);
            }
            sb.append(strValueOf2);
            sb.append(")");
        }

        public String toString(String str) {
            StringBuilder sb = new StringBuilder(256);
            sb.append('{');
            dumpDimensions(sb);
            if (this.horizontalMargin != 0.0f) {
                sb.append(" hm=");
                sb.append(this.horizontalMargin);
            }
            if (this.verticalMargin != 0.0f) {
                sb.append(" vm=");
                sb.append(this.verticalMargin);
            }
            if (this.gravity != 0) {
                sb.append(" gr=");
                sb.append(Gravity.toString(this.gravity));
            }
            if (this.softInputMode != 0) {
                sb.append(" sim={");
                sb.append(softInputModeToString(this.softInputMode));
                sb.append('}');
            }
            if (this.layoutInDisplayCutoutMode != 0) {
                sb.append(" layoutInDisplayCutoutMode=");
                sb.append(layoutInDisplayCutoutModeToString(this.layoutInDisplayCutoutMode));
            }
            sb.append(" ty=");
            sb.append(ViewDebug.intToString(LayoutParams.class, "type", this.type));
            if (this.format != -1) {
                sb.append(" fmt=");
                sb.append(PixelFormat.formatToString(this.format));
            }
            if (this.windowAnimations != 0) {
                sb.append(" wanim=0x");
                sb.append(Integer.toHexString(this.windowAnimations));
            }
            if (this.screenOrientation != -1) {
                sb.append(" or=");
                sb.append(ActivityInfo.screenOrientationToString(this.screenOrientation));
            }
            if (this.alpha != 1.0f) {
                sb.append(" alpha=");
                sb.append(this.alpha);
            }
            if (this.screenBrightness != -1.0f) {
                sb.append(" sbrt=");
                sb.append(this.screenBrightness);
            }
            if (this.buttonBrightness != -1.0f) {
                sb.append(" bbrt=");
                sb.append(this.buttonBrightness);
            }
            if (this.rotationAnimation != 0) {
                sb.append(" rotAnim=");
                sb.append(rotationAnimationToString(this.rotationAnimation));
            }
            if (this.preferredRefreshRate != 0.0f) {
                sb.append(" preferredRefreshRate=");
                sb.append(this.preferredRefreshRate);
            }
            if (this.preferredDisplayModeId != 0) {
                sb.append(" preferredDisplayMode=");
                sb.append(this.preferredDisplayModeId);
            }
            if (this.hasSystemUiListeners) {
                sb.append(" sysuil=");
                sb.append(this.hasSystemUiListeners);
            }
            if (this.inputFeatures != 0) {
                sb.append(" if=");
                sb.append(inputFeatureToString(this.inputFeatures));
            }
            if (this.userActivityTimeout >= 0) {
                sb.append(" userActivityTimeout=");
                sb.append(this.userActivityTimeout);
            }
            if (this.surfaceInsets.left != 0 || this.surfaceInsets.top != 0 || this.surfaceInsets.right != 0 || this.surfaceInsets.bottom != 0 || this.hasManualSurfaceInsets || !this.preservePreviousSurfaceInsets) {
                sb.append(" surfaceInsets=");
                sb.append(this.surfaceInsets);
                if (this.hasManualSurfaceInsets) {
                    sb.append(" (manual)");
                }
                if (!this.preservePreviousSurfaceInsets) {
                    sb.append(" (!preservePreviousSurfaceInsets)");
                }
            }
            if (this.needsMenuKey == 1) {
                sb.append(" needsMenuKey");
            }
            if (this.mColorMode != 0) {
                sb.append(" colorMode=");
                sb.append(ActivityInfo.colorModeToString(this.mColorMode));
            }
            sb.append(System.lineSeparator());
            sb.append(str);
            sb.append("  fl=");
            sb.append(ViewDebug.flagsToString(LayoutParams.class, "flags", this.flags));
            if (this.privateFlags != 0) {
                sb.append(System.lineSeparator());
                sb.append(str);
                sb.append("  pfl=");
                sb.append(ViewDebug.flagsToString(LayoutParams.class, "privateFlags", this.privateFlags));
            }
            if (this.systemUiVisibility != 0) {
                sb.append(System.lineSeparator());
                sb.append(str);
                sb.append("  sysui=");
                sb.append(ViewDebug.flagsToString(View.class, "mSystemUiVisibility", this.systemUiVisibility));
            }
            if (this.subtreeSystemUiVisibility != 0) {
                sb.append(System.lineSeparator());
                sb.append(str);
                sb.append("  vsysui=");
                sb.append(ViewDebug.flagsToString(View.class, "mSystemUiVisibility", this.subtreeSystemUiVisibility));
            }
            sb.append('}');
            return sb.toString();
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1120986464257L, this.type);
            protoOutputStream.write(1120986464258L, this.x);
            protoOutputStream.write(1120986464259L, this.y);
            protoOutputStream.write(1120986464260L, this.width);
            protoOutputStream.write(1120986464261L, this.height);
            protoOutputStream.write(1108101562374L, this.horizontalMargin);
            protoOutputStream.write(WindowLayoutParamsProto.VERTICAL_MARGIN, this.verticalMargin);
            protoOutputStream.write(1120986464264L, this.gravity);
            protoOutputStream.write(1120986464265L, this.softInputMode);
            protoOutputStream.write(1159641169930L, this.format);
            protoOutputStream.write(1120986464267L, this.windowAnimations);
            protoOutputStream.write(1108101562380L, this.alpha);
            protoOutputStream.write(WindowLayoutParamsProto.SCREEN_BRIGHTNESS, this.screenBrightness);
            protoOutputStream.write(WindowLayoutParamsProto.BUTTON_BRIGHTNESS, this.buttonBrightness);
            protoOutputStream.write(1159641169935L, this.rotationAnimation);
            protoOutputStream.write(WindowLayoutParamsProto.PREFERRED_REFRESH_RATE, this.preferredRefreshRate);
            protoOutputStream.write(1120986464273L, this.preferredDisplayModeId);
            protoOutputStream.write(1133871366162L, this.hasSystemUiListeners);
            protoOutputStream.write(WindowLayoutParamsProto.INPUT_FEATURE_FLAGS, this.inputFeatures);
            protoOutputStream.write(1112396529684L, this.userActivityTimeout);
            protoOutputStream.write(1159641169942L, this.needsMenuKey);
            protoOutputStream.write(1159641169943L, this.mColorMode);
            protoOutputStream.write(WindowLayoutParamsProto.FLAGS, this.flags);
            protoOutputStream.write(WindowLayoutParamsProto.PRIVATE_FLAGS, this.privateFlags);
            protoOutputStream.write(WindowLayoutParamsProto.SYSTEM_UI_VISIBILITY_FLAGS, this.systemUiVisibility);
            protoOutputStream.write(WindowLayoutParamsProto.SUBTREE_SYSTEM_UI_VISIBILITY_FLAGS, this.subtreeSystemUiVisibility);
            protoOutputStream.end(jStart);
        }

        public void scale(float f) {
            this.x = (int) ((this.x * f) + 0.5f);
            this.y = (int) ((this.y * f) + 0.5f);
            if (this.width > 0) {
                this.width = (int) ((this.width * f) + 0.5f);
            }
            if (this.height > 0) {
                this.height = (int) ((this.height * f) + 0.5f);
            }
        }

        void backup() {
            int[] iArr = this.mCompatibilityParamsBackup;
            if (iArr == null) {
                iArr = new int[4];
                this.mCompatibilityParamsBackup = iArr;
            }
            iArr[0] = this.x;
            iArr[1] = this.y;
            iArr[2] = this.width;
            iArr[3] = this.height;
        }

        void restore() {
            int[] iArr = this.mCompatibilityParamsBackup;
            if (iArr != null) {
                this.x = iArr[0];
                this.y = iArr[1];
                this.width = iArr[2];
                this.height = iArr[3];
            }
        }

        @Override
        protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
            super.encodeProperties(viewHierarchyEncoder);
            viewHierarchyEncoder.addProperty("x", this.x);
            viewHierarchyEncoder.addProperty("y", this.y);
            viewHierarchyEncoder.addProperty("horizontalWeight", this.horizontalWeight);
            viewHierarchyEncoder.addProperty("verticalWeight", this.verticalWeight);
            viewHierarchyEncoder.addProperty("type", this.type);
            viewHierarchyEncoder.addProperty("flags", this.flags);
        }

        public boolean isFullscreen() {
            return this.x == 0 && this.y == 0 && this.width == -1 && this.height == -1;
        }

        private static String layoutInDisplayCutoutModeToString(int i) {
            switch (i) {
                case 0:
                    return PhoneConstants.APN_TYPE_DEFAULT;
                case 1:
                    return "always";
                case 2:
                    return "never";
                default:
                    return "unknown(" + i + ")";
            }
        }

        private static String softInputModeToString(int i) {
            StringBuilder sb = new StringBuilder();
            int i2 = i & 15;
            if (i2 != 0) {
                sb.append("state=");
                switch (i2) {
                    case 1:
                        sb.append("unchanged");
                        break;
                    case 2:
                        sb.append("hidden");
                        break;
                    case 3:
                        sb.append("always_hidden");
                        break;
                    case 4:
                        sb.append(CalendarContract.CalendarColumns.VISIBLE);
                        break;
                    case 5:
                        sb.append("always_visible");
                        break;
                    default:
                        sb.append(i2);
                        break;
                }
                sb.append(' ');
            }
            int i3 = i & 240;
            if (i3 != 0) {
                sb.append("adjust=");
                if (i3 != 16) {
                    if (i3 == 32) {
                        sb.append(TextToSpeech.Engine.KEY_PARAM_PAN);
                    } else if (i3 == 48) {
                        sb.append("nothing");
                    } else {
                        sb.append(i3);
                    }
                } else {
                    sb.append("resize");
                }
                sb.append(' ');
            }
            if ((i & 256) != 0) {
                sb.append("forwardNavigation");
                sb.append(' ');
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        private static String rotationAnimationToString(int i) {
            switch (i) {
                case -1:
                    return "UNSPECIFIED";
                case 0:
                    return "ROTATE";
                case 1:
                    return "CROSSFADE";
                case 2:
                    return "JUMPCUT";
                case 3:
                    return "SEAMLESS";
                default:
                    return Integer.toString(i);
            }
        }

        private static String inputFeatureToString(int i) {
            if (i != 4) {
                switch (i) {
                    case 1:
                        return "DISABLE_POINTER_GESTURES";
                    case 2:
                        return "NO_INPUT_CHANNEL";
                    default:
                        return Integer.toString(i);
                }
            }
            return "DISABLE_USER_ACTIVITY";
        }
    }
}
