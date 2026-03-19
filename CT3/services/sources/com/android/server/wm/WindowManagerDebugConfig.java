package com.android.server.wm;

import android.os.SystemProperties;

public class WindowManagerDebugConfig {
    static boolean DEBUG_ANIM = false;
    static boolean DEBUG_APP_ORIENTATION = false;
    static boolean DEBUG_APP_TRANSITIONS = false;
    static boolean DEBUG_BOOT = false;
    static boolean DEBUG_CONFIGURATION = false;
    static boolean DEBUG_DIM_LAYER = false;
    static boolean DEBUG_DISPLAY = false;
    static boolean DEBUG_DRAG = false;
    static boolean DEBUG_FOCUS_LIGHT = false;
    static boolean DEBUG_INPUT = false;
    static boolean DEBUG_INPUT_METHOD = false;
    static boolean DEBUG_KEEP_SCREEN_ON = false;
    static boolean DEBUG_KEYGUARD = false;
    static boolean DEBUG_LAYERS = false;
    static boolean DEBUG_LAYOUT = false;
    static boolean DEBUG_LAYOUT_REPEATS = false;
    static boolean DEBUG_ORIENTATION = false;
    static boolean DEBUG_POWER = false;
    static boolean DEBUG_SCREENSHOT = false;
    static boolean DEBUG_SCREEN_ON = false;
    static boolean DEBUG_STACK = false;
    static boolean DEBUG_STARTING_WINDOW = false;
    static boolean DEBUG_SURFACE_TRACE = false;
    static boolean DEBUG_TASK_MOVEMENT = false;
    static boolean DEBUG_TASK_POSITIONING = false;
    static boolean DEBUG_TOKEN_MOVEMENT = false;
    static boolean DEBUG_VISIBILITY = false;
    static boolean DEBUG_WAKEUP = false;
    static boolean DEBUG_WALLPAPER = false;
    static boolean DEBUG_WALLPAPER_LIGHT = false;
    static boolean DEBUG_WINDOW_CROP = false;
    static boolean DEBUG_WINDOW_MOVEMENT = false;
    static boolean DEBUG_WINDOW_TRACE = false;
    static boolean SHOW_LIGHT_TRANSACTIONS = false;
    static boolean SHOW_STACK_CRAWLS = false;
    static boolean SHOW_SURFACE_ALLOC = false;
    static boolean SHOW_TRANSACTIONS = false;
    static boolean SHOW_VERBOSE_TRANSACTIONS = false;
    static final String TAG_KEEP_SCREEN_ON = "DebugKeepScreenOn";
    static final boolean TAG_WITH_CLASS_NAME = false;
    static final String TAG_WM = "WindowManager";
    static boolean localLOGV;
    static boolean DEBUG_RESIZE = false;
    static boolean DEBUG = false;
    static boolean DEBUG_ADD_REMOVE = false;
    static boolean DEBUG_FOCUS = false;

    static {
        if (!DEBUG_FOCUS) {
        }
        DEBUG_FOCUS_LIGHT = true;
        DEBUG_ANIM = false;
        DEBUG_KEYGUARD = true;
        DEBUG_LAYOUT = false;
        DEBUG_LAYERS = false;
        DEBUG_INPUT = false;
        DEBUG_INPUT_METHOD = false;
        DEBUG_VISIBILITY = false;
        DEBUG_WINDOW_MOVEMENT = false;
        DEBUG_TOKEN_MOVEMENT = false;
        DEBUG_ORIENTATION = false;
        DEBUG_APP_ORIENTATION = false;
        DEBUG_CONFIGURATION = false;
        DEBUG_APP_TRANSITIONS = false;
        DEBUG_STARTING_WINDOW = false;
        DEBUG_WALLPAPER = false;
        DEBUG_WALLPAPER_LIGHT = DEBUG_WALLPAPER;
        DEBUG_DRAG = false;
        DEBUG_SCREEN_ON = true;
        DEBUG_SCREENSHOT = false;
        DEBUG_BOOT = true;
        DEBUG_LAYOUT_REPEATS = false;
        DEBUG_SURFACE_TRACE = SystemProperties.getBoolean("debug.wms.surface", false);
        DEBUG_WINDOW_TRACE = false;
        DEBUG_TASK_MOVEMENT = false;
        DEBUG_TASK_POSITIONING = false;
        DEBUG_STACK = false;
        DEBUG_DISPLAY = true;
        DEBUG_POWER = false;
        DEBUG_DIM_LAYER = false;
        SHOW_SURFACE_ALLOC = false;
        SHOW_TRANSACTIONS = false;
        SHOW_VERBOSE_TRANSACTIONS = false;
        SHOW_LIGHT_TRANSACTIONS = SHOW_TRANSACTIONS;
        SHOW_STACK_CRAWLS = false;
        DEBUG_WINDOW_CROP = false;
        DEBUG_KEEP_SCREEN_ON = false;
        DEBUG_WAKEUP = false;
        localLOGV = false;
    }
}
