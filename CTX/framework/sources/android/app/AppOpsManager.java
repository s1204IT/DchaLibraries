package android.app;

import android.Manifest;
import android.annotation.SystemApi;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserManager;
import android.security.keystore.KeyProperties;
import android.util.ArrayMap;
import com.android.internal.app.IAppOpsActiveCallback;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.Preconditions;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AppOpsManager {
    public static final int MODE_ALLOWED = 0;
    public static final int MODE_DEFAULT = 3;
    public static final int MODE_ERRORED = 2;
    public static final int MODE_FOREGROUND = 4;
    public static final int MODE_IGNORED = 1;
    public static final int OP_ACCEPT_HANDOVER = 74;
    public static final int OP_ACCESS_NOTIFICATIONS = 25;
    public static final int OP_ACTIVATE_VPN = 47;
    public static final int OP_ADD_VOICEMAIL = 52;
    public static final int OP_ANSWER_PHONE_CALLS = 69;
    public static final int OP_ASSIST_SCREENSHOT = 50;
    public static final int OP_ASSIST_STRUCTURE = 49;
    public static final int OP_AUDIO_ACCESSIBILITY_VOLUME = 64;
    public static final int OP_AUDIO_ALARM_VOLUME = 37;
    public static final int OP_AUDIO_BLUETOOTH_VOLUME = 39;
    public static final int OP_AUDIO_MASTER_VOLUME = 33;
    public static final int OP_AUDIO_MEDIA_VOLUME = 36;
    public static final int OP_AUDIO_NOTIFICATION_VOLUME = 38;
    public static final int OP_AUDIO_RING_VOLUME = 35;
    public static final int OP_AUDIO_VOICE_VOLUME = 34;
    public static final int OP_BIND_ACCESSIBILITY_SERVICE = 73;
    public static final int OP_BLUETOOTH_SCAN = 77;
    public static final int OP_BODY_SENSORS = 56;
    public static final int OP_CALL_PHONE = 13;
    public static final int OP_CAMERA = 26;
    public static final int OP_CHANGE_WIFI_STATE = 71;
    public static final int OP_COARSE_LOCATION = 0;
    public static final int OP_FINE_LOCATION = 1;
    public static final int OP_GET_ACCOUNTS = 62;
    public static final int OP_GET_USAGE_STATS = 43;
    public static final int OP_GPS = 2;
    public static final int OP_INSTANT_APP_START_FOREGROUND = 68;
    public static final int OP_MANAGE_IPSEC_TUNNELS = 75;
    public static final int OP_MOCK_LOCATION = 58;
    public static final int OP_MONITOR_HIGH_POWER_LOCATION = 42;
    public static final int OP_MONITOR_LOCATION = 41;
    public static final int OP_MUTE_MICROPHONE = 44;
    public static final int OP_NEIGHBORING_CELLS = 12;
    public static final int OP_NONE = -1;
    public static final int OP_PICTURE_IN_PICTURE = 67;
    public static final int OP_PLAY_AUDIO = 28;
    public static final int OP_POST_NOTIFICATION = 11;
    public static final int OP_PROCESS_OUTGOING_CALLS = 54;
    public static final int OP_PROJECT_MEDIA = 46;
    public static final int OP_READ_CALENDAR = 8;
    public static final int OP_READ_CALL_LOG = 6;
    public static final int OP_READ_CELL_BROADCASTS = 57;
    public static final int OP_READ_CLIPBOARD = 29;
    public static final int OP_READ_CONTACTS = 4;
    public static final int OP_READ_EXTERNAL_STORAGE = 59;
    public static final int OP_READ_ICC_SMS = 21;
    public static final int OP_READ_PHONE_NUMBERS = 65;
    public static final int OP_READ_PHONE_STATE = 51;
    public static final int OP_READ_SMS = 14;
    public static final int OP_RECEIVE_EMERGECY_SMS = 17;
    public static final int OP_RECEIVE_MMS = 18;
    public static final int OP_RECEIVE_SMS = 16;
    public static final int OP_RECEIVE_WAP_PUSH = 19;
    public static final int OP_RECORD_AUDIO = 27;
    public static final int OP_REQUEST_DELETE_PACKAGES = 72;
    public static final int OP_REQUEST_INSTALL_PACKAGES = 66;
    public static final int OP_RUN_ANY_IN_BACKGROUND = 70;
    public static final int OP_RUN_IN_BACKGROUND = 63;
    public static final int OP_SEND_SMS = 20;
    public static final int OP_START_FOREGROUND = 76;
    public static final int OP_SYSTEM_ALERT_WINDOW = 24;
    public static final int OP_TAKE_AUDIO_FOCUS = 32;
    public static final int OP_TAKE_MEDIA_BUTTONS = 31;
    public static final int OP_TOAST_WINDOW = 45;
    public static final int OP_TURN_SCREEN_ON = 61;
    public static final int OP_USE_FINGERPRINT = 55;
    public static final int OP_USE_SIP = 53;
    public static final int OP_VIBRATE = 3;
    public static final int OP_WAKE_LOCK = 40;
    public static final int OP_WIFI_SCAN = 10;
    public static final int OP_WRITE_CALENDAR = 9;
    public static final int OP_WRITE_CALL_LOG = 7;
    public static final int OP_WRITE_CLIPBOARD = 30;
    public static final int OP_WRITE_CONTACTS = 5;
    public static final int OP_WRITE_EXTERNAL_STORAGE = 60;
    public static final int OP_WRITE_ICC_SMS = 22;
    public static final int OP_WRITE_SETTINGS = 23;
    public static final int OP_WRITE_SMS = 15;
    public static final int OP_WRITE_WALLPAPER = 48;
    public static final int UID_STATE_BACKGROUND = 4;
    public static final int UID_STATE_CACHED = 5;
    public static final int UID_STATE_FOREGROUND = 3;
    public static final int UID_STATE_FOREGROUND_SERVICE = 2;
    public static final int UID_STATE_LAST_NON_RESTRICTED = 2;
    public static final int UID_STATE_PERSISTENT = 0;
    public static final int UID_STATE_TOP = 1;
    public static final int WATCH_FOREGROUND_CHANGES = 1;
    public static final int _NUM_OP = 78;
    public static final int _NUM_UID_STATE = 6;
    static IBinder sToken;
    final Context mContext;
    final IAppOpsService mService;
    public static final String[] MODE_NAMES = {"allow", "ignore", "deny", PhoneConstants.APN_TYPE_DEFAULT, "foreground"};
    private static final CtaManager CTA_MANAGER = CtaManagerFactory.getInstance().makeCtaManager();
    private static final int[] RUNTIME_AND_APPOP_PERMISSIONS_OPS = {4, 5, 62, 8, 9, 20, 16, 14, 19, 18, 57, 59, 60, 0, 1, 51, 65, 13, 6, 7, 52, 53, 54, 69, 74, 27, 26, 56, 25, 24, 23, 66, 76};
    private static int[] sOpToSwitch = {0, 0, 0, 3, 4, 5, 6, 7, 8, 9, 0, 11, 0, 13, 14, 15, 16, 16, 18, 19, 20, 14, 15, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 0, 0, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 0};
    public static final String OPSTR_COARSE_LOCATION = "android:coarse_location";
    public static final String OPSTR_FINE_LOCATION = "android:fine_location";

    @SystemApi
    public static final String OPSTR_GPS = "android:gps";

    @SystemApi
    public static final String OPSTR_VIBRATE = "android:vibrate";
    public static final String OPSTR_READ_CONTACTS = "android:read_contacts";
    public static final String OPSTR_WRITE_CONTACTS = "android:write_contacts";
    public static final String OPSTR_READ_CALL_LOG = "android:read_call_log";
    public static final String OPSTR_WRITE_CALL_LOG = "android:write_call_log";
    public static final String OPSTR_READ_CALENDAR = "android:read_calendar";
    public static final String OPSTR_WRITE_CALENDAR = "android:write_calendar";

    @SystemApi
    public static final String OPSTR_WIFI_SCAN = "android:wifi_scan";

    @SystemApi
    public static final String OPSTR_POST_NOTIFICATION = "android:post_notification";

    @SystemApi
    public static final String OPSTR_NEIGHBORING_CELLS = "android:neighboring_cells";
    public static final String OPSTR_CALL_PHONE = "android:call_phone";
    public static final String OPSTR_READ_SMS = "android:read_sms";

    @SystemApi
    public static final String OPSTR_WRITE_SMS = "android:write_sms";
    public static final String OPSTR_RECEIVE_SMS = "android:receive_sms";

    @SystemApi
    public static final String OPSTR_RECEIVE_EMERGENCY_BROADCAST = "android:receive_emergency_broadcast";
    public static final String OPSTR_RECEIVE_MMS = "android:receive_mms";
    public static final String OPSTR_RECEIVE_WAP_PUSH = "android:receive_wap_push";
    public static final String OPSTR_SEND_SMS = "android:send_sms";

    @SystemApi
    public static final String OPSTR_READ_ICC_SMS = "android:read_icc_sms";

    @SystemApi
    public static final String OPSTR_WRITE_ICC_SMS = "android:write_icc_sms";
    public static final String OPSTR_WRITE_SETTINGS = "android:write_settings";
    public static final String OPSTR_SYSTEM_ALERT_WINDOW = "android:system_alert_window";

    @SystemApi
    public static final String OPSTR_ACCESS_NOTIFICATIONS = "android:access_notifications";
    public static final String OPSTR_CAMERA = "android:camera";
    public static final String OPSTR_RECORD_AUDIO = "android:record_audio";

    @SystemApi
    public static final String OPSTR_PLAY_AUDIO = "android:play_audio";

    @SystemApi
    public static final String OPSTR_READ_CLIPBOARD = "android:read_clipboard";

    @SystemApi
    public static final String OPSTR_WRITE_CLIPBOARD = "android:write_clipboard";

    @SystemApi
    public static final String OPSTR_TAKE_MEDIA_BUTTONS = "android:take_media_buttons";

    @SystemApi
    public static final String OPSTR_TAKE_AUDIO_FOCUS = "android:take_audio_focus";

    @SystemApi
    public static final String OPSTR_AUDIO_MASTER_VOLUME = "android:audio_master_volume";

    @SystemApi
    public static final String OPSTR_AUDIO_VOICE_VOLUME = "android:audio_voice_volume";

    @SystemApi
    public static final String OPSTR_AUDIO_RING_VOLUME = "android:audio_ring_volume";

    @SystemApi
    public static final String OPSTR_AUDIO_MEDIA_VOLUME = "android:audio_media_volume";

    @SystemApi
    public static final String OPSTR_AUDIO_ALARM_VOLUME = "android:audio_alarm_volume";

    @SystemApi
    public static final String OPSTR_AUDIO_NOTIFICATION_VOLUME = "android:audio_notification_volume";

    @SystemApi
    public static final String OPSTR_AUDIO_BLUETOOTH_VOLUME = "android:audio_bluetooth_volume";

    @SystemApi
    public static final String OPSTR_WAKE_LOCK = "android:wake_lock";
    public static final String OPSTR_MONITOR_LOCATION = "android:monitor_location";
    public static final String OPSTR_MONITOR_HIGH_POWER_LOCATION = "android:monitor_location_high_power";
    public static final String OPSTR_GET_USAGE_STATS = "android:get_usage_stats";

    @SystemApi
    public static final String OPSTR_MUTE_MICROPHONE = "android:mute_microphone";

    @SystemApi
    public static final String OPSTR_TOAST_WINDOW = "android:toast_window";

    @SystemApi
    public static final String OPSTR_PROJECT_MEDIA = "android:project_media";

    @SystemApi
    public static final String OPSTR_ACTIVATE_VPN = "android:activate_vpn";

    @SystemApi
    public static final String OPSTR_WRITE_WALLPAPER = "android:write_wallpaper";

    @SystemApi
    public static final String OPSTR_ASSIST_STRUCTURE = "android:assist_structure";

    @SystemApi
    public static final String OPSTR_ASSIST_SCREENSHOT = "android:assist_screenshot";
    public static final String OPSTR_READ_PHONE_STATE = "android:read_phone_state";
    public static final String OPSTR_ADD_VOICEMAIL = "android:add_voicemail";
    public static final String OPSTR_USE_SIP = "android:use_sip";
    public static final String OPSTR_PROCESS_OUTGOING_CALLS = "android:process_outgoing_calls";
    public static final String OPSTR_USE_FINGERPRINT = "android:use_fingerprint";
    public static final String OPSTR_BODY_SENSORS = "android:body_sensors";
    public static final String OPSTR_READ_CELL_BROADCASTS = "android:read_cell_broadcasts";
    public static final String OPSTR_MOCK_LOCATION = "android:mock_location";
    public static final String OPSTR_READ_EXTERNAL_STORAGE = "android:read_external_storage";
    public static final String OPSTR_WRITE_EXTERNAL_STORAGE = "android:write_external_storage";

    @SystemApi
    public static final String OPSTR_TURN_SCREEN_ON = "android:turn_screen_on";

    @SystemApi
    public static final String OPSTR_GET_ACCOUNTS = "android:get_accounts";

    @SystemApi
    public static final String OPSTR_RUN_IN_BACKGROUND = "android:run_in_background";

    @SystemApi
    public static final String OPSTR_AUDIO_ACCESSIBILITY_VOLUME = "android:audio_accessibility_volume";
    public static final String OPSTR_READ_PHONE_NUMBERS = "android:read_phone_numbers";

    @SystemApi
    public static final String OPSTR_REQUEST_INSTALL_PACKAGES = "android:request_install_packages";
    public static final String OPSTR_PICTURE_IN_PICTURE = "android:picture_in_picture";

    @SystemApi
    public static final String OPSTR_INSTANT_APP_START_FOREGROUND = "android:instant_app_start_foreground";
    public static final String OPSTR_ANSWER_PHONE_CALLS = "android:answer_phone_calls";

    @SystemApi
    public static final String OPSTR_RUN_ANY_IN_BACKGROUND = "android:run_any_in_background";

    @SystemApi
    public static final String OPSTR_CHANGE_WIFI_STATE = "android:change_wifi_state";

    @SystemApi
    public static final String OPSTR_REQUEST_DELETE_PACKAGES = "android:request_delete_packages";

    @SystemApi
    public static final String OPSTR_BIND_ACCESSIBILITY_SERVICE = "android:bind_accessibility_service";

    @SystemApi
    public static final String OPSTR_ACCEPT_HANDOVER = "android:accept_handover";

    @SystemApi
    public static final String OPSTR_MANAGE_IPSEC_TUNNELS = "android:manage_ipsec_tunnels";

    @SystemApi
    public static final String OPSTR_START_FOREGROUND = "android:start_foreground";
    public static final String OPSTR_BLUETOOTH_SCAN = "android:bluetooth_scan";
    private static String[] sOpToString = {OPSTR_COARSE_LOCATION, OPSTR_FINE_LOCATION, OPSTR_GPS, OPSTR_VIBRATE, OPSTR_READ_CONTACTS, OPSTR_WRITE_CONTACTS, OPSTR_READ_CALL_LOG, OPSTR_WRITE_CALL_LOG, OPSTR_READ_CALENDAR, OPSTR_WRITE_CALENDAR, OPSTR_WIFI_SCAN, OPSTR_POST_NOTIFICATION, OPSTR_NEIGHBORING_CELLS, OPSTR_CALL_PHONE, OPSTR_READ_SMS, OPSTR_WRITE_SMS, OPSTR_RECEIVE_SMS, OPSTR_RECEIVE_EMERGENCY_BROADCAST, OPSTR_RECEIVE_MMS, OPSTR_RECEIVE_WAP_PUSH, OPSTR_SEND_SMS, OPSTR_READ_ICC_SMS, OPSTR_WRITE_ICC_SMS, OPSTR_WRITE_SETTINGS, OPSTR_SYSTEM_ALERT_WINDOW, OPSTR_ACCESS_NOTIFICATIONS, OPSTR_CAMERA, OPSTR_RECORD_AUDIO, OPSTR_PLAY_AUDIO, OPSTR_READ_CLIPBOARD, OPSTR_WRITE_CLIPBOARD, OPSTR_TAKE_MEDIA_BUTTONS, OPSTR_TAKE_AUDIO_FOCUS, OPSTR_AUDIO_MASTER_VOLUME, OPSTR_AUDIO_VOICE_VOLUME, OPSTR_AUDIO_RING_VOLUME, OPSTR_AUDIO_MEDIA_VOLUME, OPSTR_AUDIO_ALARM_VOLUME, OPSTR_AUDIO_NOTIFICATION_VOLUME, OPSTR_AUDIO_BLUETOOTH_VOLUME, OPSTR_WAKE_LOCK, OPSTR_MONITOR_LOCATION, OPSTR_MONITOR_HIGH_POWER_LOCATION, OPSTR_GET_USAGE_STATS, OPSTR_MUTE_MICROPHONE, OPSTR_TOAST_WINDOW, OPSTR_PROJECT_MEDIA, OPSTR_ACTIVATE_VPN, OPSTR_WRITE_WALLPAPER, OPSTR_ASSIST_STRUCTURE, OPSTR_ASSIST_SCREENSHOT, OPSTR_READ_PHONE_STATE, OPSTR_ADD_VOICEMAIL, OPSTR_USE_SIP, OPSTR_PROCESS_OUTGOING_CALLS, OPSTR_USE_FINGERPRINT, OPSTR_BODY_SENSORS, OPSTR_READ_CELL_BROADCASTS, OPSTR_MOCK_LOCATION, OPSTR_READ_EXTERNAL_STORAGE, OPSTR_WRITE_EXTERNAL_STORAGE, OPSTR_TURN_SCREEN_ON, OPSTR_GET_ACCOUNTS, OPSTR_RUN_IN_BACKGROUND, OPSTR_AUDIO_ACCESSIBILITY_VOLUME, OPSTR_READ_PHONE_NUMBERS, OPSTR_REQUEST_INSTALL_PACKAGES, OPSTR_PICTURE_IN_PICTURE, OPSTR_INSTANT_APP_START_FOREGROUND, OPSTR_ANSWER_PHONE_CALLS, OPSTR_RUN_ANY_IN_BACKGROUND, OPSTR_CHANGE_WIFI_STATE, OPSTR_REQUEST_DELETE_PACKAGES, OPSTR_BIND_ACCESSIBILITY_SERVICE, OPSTR_ACCEPT_HANDOVER, OPSTR_MANAGE_IPSEC_TUNNELS, OPSTR_START_FOREGROUND, OPSTR_BLUETOOTH_SCAN};
    private static String[] sOpNames = {"COARSE_LOCATION", "FINE_LOCATION", "GPS", "VIBRATE", "READ_CONTACTS", "WRITE_CONTACTS", "READ_CALL_LOG", "WRITE_CALL_LOG", "READ_CALENDAR", "WRITE_CALENDAR", "WIFI_SCAN", "POST_NOTIFICATION", "NEIGHBORING_CELLS", "CALL_PHONE", "READ_SMS", "WRITE_SMS", "RECEIVE_SMS", "RECEIVE_EMERGECY_SMS", "RECEIVE_MMS", "RECEIVE_WAP_PUSH", "SEND_SMS", "READ_ICC_SMS", "WRITE_ICC_SMS", "WRITE_SETTINGS", "SYSTEM_ALERT_WINDOW", "ACCESS_NOTIFICATIONS", "CAMERA", "RECORD_AUDIO", "PLAY_AUDIO", "READ_CLIPBOARD", "WRITE_CLIPBOARD", "TAKE_MEDIA_BUTTONS", "TAKE_AUDIO_FOCUS", "AUDIO_MASTER_VOLUME", "AUDIO_VOICE_VOLUME", "AUDIO_RING_VOLUME", "AUDIO_MEDIA_VOLUME", "AUDIO_ALARM_VOLUME", "AUDIO_NOTIFICATION_VOLUME", "AUDIO_BLUETOOTH_VOLUME", "WAKE_LOCK", "MONITOR_LOCATION", "MONITOR_HIGH_POWER_LOCATION", "GET_USAGE_STATS", "MUTE_MICROPHONE", "TOAST_WINDOW", "PROJECT_MEDIA", "ACTIVATE_VPN", "WRITE_WALLPAPER", "ASSIST_STRUCTURE", "ASSIST_SCREENSHOT", "OP_READ_PHONE_STATE", "ADD_VOICEMAIL", "USE_SIP", "PROCESS_OUTGOING_CALLS", "USE_FINGERPRINT", "BODY_SENSORS", "READ_CELL_BROADCASTS", "MOCK_LOCATION", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "TURN_ON_SCREEN", "GET_ACCOUNTS", "RUN_IN_BACKGROUND", "AUDIO_ACCESSIBILITY_VOLUME", "READ_PHONE_NUMBERS", "REQUEST_INSTALL_PACKAGES", "PICTURE_IN_PICTURE", "INSTANT_APP_START_FOREGROUND", "ANSWER_PHONE_CALLS", "RUN_ANY_IN_BACKGROUND", "CHANGE_WIFI_STATE", "REQUEST_DELETE_PACKAGES", "BIND_ACCESSIBILITY_SERVICE", "ACCEPT_HANDOVER", "MANAGE_IPSEC_TUNNELS", "START_FOREGROUND", "BLUETOOTH_SCAN"};
    private static String[] sOpPerms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, null, Manifest.permission.VIBRATE, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR, Manifest.permission.ACCESS_WIFI_STATE, null, null, Manifest.permission.CALL_PHONE, Manifest.permission.READ_SMS, null, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECEIVE_EMERGENCY_BROADCAST, Manifest.permission.RECEIVE_MMS, Manifest.permission.RECEIVE_WAP_PUSH, Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, null, Manifest.permission.WRITE_SETTINGS, Manifest.permission.SYSTEM_ALERT_WINDOW, Manifest.permission.ACCESS_NOTIFICATIONS, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, null, null, null, null, null, null, null, null, null, null, null, null, Manifest.permission.WAKE_LOCK, null, null, Manifest.permission.PACKAGE_USAGE_STATS, null, null, null, null, null, null, null, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ADD_VOICEMAIL, Manifest.permission.USE_SIP, Manifest.permission.PROCESS_OUTGOING_CALLS, Manifest.permission.USE_FINGERPRINT, Manifest.permission.BODY_SENSORS, Manifest.permission.READ_CELL_BROADCASTS, null, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, null, Manifest.permission.GET_ACCOUNTS, null, null, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.REQUEST_INSTALL_PACKAGES, null, Manifest.permission.INSTANT_APP_FOREGROUND_SERVICE, Manifest.permission.ANSWER_PHONE_CALLS, null, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.REQUEST_DELETE_PACKAGES, Manifest.permission.BIND_ACCESSIBILITY_SERVICE, Manifest.permission.ACCEPT_HANDOVER, null, Manifest.permission.FOREGROUND_SERVICE, null};
    private static String[] sOpRestrictions = {UserManager.DISALLOW_SHARE_LOCATION, UserManager.DISALLOW_SHARE_LOCATION, UserManager.DISALLOW_SHARE_LOCATION, null, null, null, UserManager.DISALLOW_OUTGOING_CALLS, UserManager.DISALLOW_OUTGOING_CALLS, null, null, UserManager.DISALLOW_SHARE_LOCATION, null, null, null, UserManager.DISALLOW_SMS, UserManager.DISALLOW_SMS, UserManager.DISALLOW_SMS, null, UserManager.DISALLOW_SMS, null, UserManager.DISALLOW_SMS, UserManager.DISALLOW_SMS, UserManager.DISALLOW_SMS, null, UserManager.DISALLOW_CREATE_WINDOWS, null, UserManager.DISALLOW_CAMERA, UserManager.DISALLOW_RECORD_AUDIO, null, null, null, null, null, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, null, UserManager.DISALLOW_SHARE_LOCATION, UserManager.DISALLOW_SHARE_LOCATION, null, UserManager.DISALLOW_UNMUTE_MICROPHONE, UserManager.DISALLOW_CREATE_WINDOWS, null, null, UserManager.DISALLOW_WALLPAPER, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, UserManager.DISALLOW_ADJUST_VOLUME, null, null, null, null, null, null, null, null, null, null, null, null, null};
    private static boolean[] sOpAllowSystemRestrictionBypass = {true, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true};
    private static int[] sOpDefaultMode = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, 3, 0, 0, 0, 0, 0, 0, 2, 0, 0};
    private static boolean[] sOpDisableReset = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    private static HashMap<String, Integer> sOpStrToOp = new HashMap<>();
    private static HashMap<String, Integer> sPermToOp = new HashMap<>();
    final ArrayMap<OnOpChangedListener, IAppOpsCallback> mModeWatchers = new ArrayMap<>();
    final ArrayMap<OnOpActiveChangedListener, IAppOpsActiveCallback> mActiveWatchers = new ArrayMap<>();

    public interface OnOpActiveChangedListener {
        void onOpActiveChanged(int i, int i2, String str, boolean z);
    }

    public interface OnOpChangedListener {
        void onOpChanged(String str, String str2);
    }

    static {
        if (sOpToSwitch.length == 78) {
            if (sOpToString.length == 78) {
                if (sOpNames.length == 78) {
                    if (sOpPerms.length == 78) {
                        if (sOpDefaultMode.length == 78) {
                            if (sOpDisableReset.length == 78) {
                                if (sOpRestrictions.length == 78) {
                                    if (sOpAllowSystemRestrictionBypass.length != 78) {
                                        throw new IllegalStateException("sOpAllowSYstemRestrictionsBypass length " + sOpRestrictions.length + " should be 78");
                                    }
                                    for (int i = 0; i < 78; i++) {
                                        if (sOpToString[i] != null) {
                                            sOpStrToOp.put(sOpToString[i], Integer.valueOf(i));
                                        }
                                    }
                                    for (int i2 : RUNTIME_AND_APPOP_PERMISSIONS_OPS) {
                                        if (sOpPerms[i2] != null) {
                                            sPermToOp.put(sOpPerms[i2], Integer.valueOf(i2));
                                        }
                                    }
                                    return;
                                }
                                throw new IllegalStateException("sOpRestrictions length " + sOpRestrictions.length + " should be 78");
                            }
                            throw new IllegalStateException("sOpDisableReset length " + sOpDisableReset.length + " should be 78");
                        }
                        throw new IllegalStateException("sOpDefaultMode length " + sOpDefaultMode.length + " should be 78");
                    }
                    throw new IllegalStateException("sOpPerms length " + sOpPerms.length + " should be 78");
                }
                throw new IllegalStateException("sOpNames length " + sOpNames.length + " should be 78");
            }
            throw new IllegalStateException("sOpToString length " + sOpToString.length + " should be 78");
        }
        throw new IllegalStateException("sOpToSwitch length " + sOpToSwitch.length + " should be 78");
    }

    public static int opToSwitch(int i) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.opToSwitch(i);
        }
        return sOpToSwitch[i];
    }

    public static String opToName(int i) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.opToName(i);
        }
        if (i == -1) {
            return KeyProperties.DIGEST_NONE;
        }
        if (i < sOpNames.length) {
            return sOpNames[i];
        }
        return "Unknown(" + i + ")";
    }

    public static int strDebugOpToOp(String str) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.strDebugOpToOp(str);
        }
        for (int i = 0; i < sOpNames.length; i++) {
            if (sOpNames[i].equals(str)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown operation string: " + str);
    }

    public static String opToPermission(int i) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.opToPermission(i);
        }
        return sOpPerms[i];
    }

    public static String opToRestriction(int i) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.opToRestriction(i);
        }
        return sOpRestrictions[i];
    }

    public static int permissionToOpCode(String str) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.permissionToOpCode(str);
        }
        Integer num = sPermToOp.get(str);
        if (num != null) {
            return num.intValue();
        }
        return -1;
    }

    public static boolean opAllowSystemBypassRestriction(int i) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.opAllowSystemBypassRestriction(i);
        }
        return sOpAllowSystemRestrictionBypass[i];
    }

    public static int opToDefaultMode(int i) {
        if (CTA_MANAGER.isCtaSupported() || i >= sOpDefaultMode.length) {
            return CTA_MANAGER.opToDefaultMode(i);
        }
        return sOpDefaultMode[i];
    }

    public static String modeToName(int i) {
        if (i >= 0 && i < MODE_NAMES.length) {
            return MODE_NAMES[i];
        }
        return "mode=" + i;
    }

    public static boolean opAllowsReset(int i) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.opAllowsReset(i);
        }
        return !sOpDisableReset[i];
    }

    public static class PackageOps implements Parcelable {
        public static final Parcelable.Creator<PackageOps> CREATOR = new Parcelable.Creator<PackageOps>() {
            @Override
            public PackageOps createFromParcel(Parcel parcel) {
                return new PackageOps(parcel);
            }

            @Override
            public PackageOps[] newArray(int i) {
                return new PackageOps[i];
            }
        };
        private final List<OpEntry> mEntries;
        private final String mPackageName;
        private final int mUid;

        public PackageOps(String str, int i, List<OpEntry> list) {
            this.mPackageName = str;
            this.mUid = i;
            this.mEntries = list;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public int getUid() {
            return this.mUid;
        }

        public List<OpEntry> getOps() {
            return this.mEntries;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mPackageName);
            parcel.writeInt(this.mUid);
            parcel.writeInt(this.mEntries.size());
            for (int i2 = 0; i2 < this.mEntries.size(); i2++) {
                this.mEntries.get(i2).writeToParcel(parcel, i);
            }
        }

        PackageOps(Parcel parcel) {
            this.mPackageName = parcel.readString();
            this.mUid = parcel.readInt();
            this.mEntries = new ArrayList();
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                this.mEntries.add(OpEntry.CREATOR.createFromParcel(parcel));
            }
        }
    }

    public static class OpEntry implements Parcelable {
        public static final Parcelable.Creator<OpEntry> CREATOR = new Parcelable.Creator<OpEntry>() {
            @Override
            public OpEntry createFromParcel(Parcel parcel) {
                return new OpEntry(parcel);
            }

            @Override
            public OpEntry[] newArray(int i) {
                return new OpEntry[i];
            }
        };
        private final int mDuration;
        private final int mMode;
        private final int mOp;
        private final String mProxyPackageName;
        private final int mProxyUid;
        private final long[] mRejectTimes;
        private final boolean mRunning;
        private final long[] mTimes;

        public OpEntry(int i, int i2, long j, long j2, int i3, int i4, String str) {
            this.mOp = i;
            this.mMode = i2;
            this.mTimes = new long[6];
            this.mRejectTimes = new long[6];
            this.mTimes[0] = j;
            this.mRejectTimes[0] = j2;
            this.mDuration = i3;
            this.mRunning = i3 == -1;
            this.mProxyUid = i4;
            this.mProxyPackageName = str;
        }

        public OpEntry(int i, int i2, long[] jArr, long[] jArr2, int i3, boolean z, int i4, String str) {
            this.mOp = i;
            this.mMode = i2;
            this.mTimes = new long[6];
            this.mRejectTimes = new long[6];
            System.arraycopy(jArr, 0, this.mTimes, 0, 6);
            System.arraycopy(jArr2, 0, this.mRejectTimes, 0, 6);
            this.mDuration = i3;
            this.mRunning = z;
            this.mProxyUid = i4;
            this.mProxyPackageName = str;
        }

        public OpEntry(int i, int i2, long[] jArr, long[] jArr2, int i3, int i4, String str) {
            this(i, i2, jArr, jArr2, i3, i3 == -1, i4, str);
        }

        public int getOp() {
            return this.mOp;
        }

        public int getMode() {
            return this.mMode;
        }

        public long getTime() {
            return AppOpsManager.maxTime(this.mTimes, 0, 6);
        }

        public long getLastAccessTime() {
            return AppOpsManager.maxTime(this.mTimes, 0, 6);
        }

        public long getLastAccessForegroundTime() {
            return AppOpsManager.maxTime(this.mTimes, 0, 3);
        }

        public long getLastAccessBackgroundTime() {
            return AppOpsManager.maxTime(this.mTimes, 3, 6);
        }

        public long getLastTimeFor(int i) {
            return this.mTimes[i];
        }

        public long getRejectTime() {
            return AppOpsManager.maxTime(this.mRejectTimes, 0, 6);
        }

        public long getLastRejectTime() {
            return AppOpsManager.maxTime(this.mRejectTimes, 0, 6);
        }

        public long getLastRejectForegroundTime() {
            return AppOpsManager.maxTime(this.mRejectTimes, 0, 3);
        }

        public long getLastRejectBackgroundTime() {
            return AppOpsManager.maxTime(this.mRejectTimes, 3, 6);
        }

        public long getLastRejectTimeFor(int i) {
            return this.mRejectTimes[i];
        }

        public boolean isRunning() {
            return this.mRunning;
        }

        public int getDuration() {
            return this.mDuration;
        }

        public int getProxyUid() {
            return this.mProxyUid;
        }

        public String getProxyPackageName() {
            return this.mProxyPackageName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mOp);
            parcel.writeInt(this.mMode);
            parcel.writeLongArray(this.mTimes);
            parcel.writeLongArray(this.mRejectTimes);
            parcel.writeInt(this.mDuration);
            parcel.writeBoolean(this.mRunning);
            parcel.writeInt(this.mProxyUid);
            parcel.writeString(this.mProxyPackageName);
        }

        OpEntry(Parcel parcel) {
            this.mOp = parcel.readInt();
            this.mMode = parcel.readInt();
            this.mTimes = parcel.createLongArray();
            this.mRejectTimes = parcel.createLongArray();
            this.mDuration = parcel.readInt();
            this.mRunning = parcel.readBoolean();
            this.mProxyUid = parcel.readInt();
            this.mProxyPackageName = parcel.readString();
        }
    }

    public static class OnOpChangedInternalListener implements OnOpChangedListener {
        @Override
        public void onOpChanged(String str, String str2) {
        }

        public void onOpChanged(int i, String str) {
        }
    }

    AppOpsManager(Context context, IAppOpsService iAppOpsService) {
        this.mContext = context;
        this.mService = iAppOpsService;
    }

    public List<PackageOps> getPackagesForOps(int[] iArr) {
        try {
            return this.mService.getPackagesForOps(iArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<PackageOps> getOpsForPackage(int i, String str, int[] iArr) {
        try {
            return this.mService.getOpsForPackage(i, str, iArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUidMode(int i, int i2, int i3) {
        try {
            this.mService.setUidMode(i, i2, i3);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setUidMode(String str, int i, int i2) {
        try {
            this.mService.setUidMode(strOpToOp(str), i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUserRestriction(int i, boolean z, IBinder iBinder) {
        setUserRestriction(i, z, iBinder, null);
    }

    public void setUserRestriction(int i, boolean z, IBinder iBinder, String[] strArr) {
        setUserRestrictionForUser(i, z, iBinder, strArr, this.mContext.getUserId());
    }

    public void setUserRestrictionForUser(int i, boolean z, IBinder iBinder, String[] strArr, int i2) {
        try {
            this.mService.setUserRestriction(i, z, iBinder, i2, strArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMode(int i, int i2, String str, int i3) {
        try {
            this.mService.setMode(i, i2, str, i3);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setMode(String str, int i, String str2, int i2) {
        try {
            this.mService.setMode(strOpToOp(str), i, str2, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setRestriction(int i, int i2, int i3, String[] strArr) {
        try {
            this.mService.setAudioRestriction(i, i2, Binder.getCallingUid(), i3, strArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resetAllModes() {
        try {
            this.mService.resetAllModes(this.mContext.getUserId(), null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static String permissionToOp(String str) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.permissionToOp(str);
        }
        Integer num = sPermToOp.get(str);
        if (num == null) {
            return null;
        }
        return sOpToString[num.intValue()];
    }

    public void startWatchingMode(String str, String str2, OnOpChangedListener onOpChangedListener) {
        startWatchingMode(strOpToOp(str), str2, onOpChangedListener);
    }

    public void startWatchingMode(String str, String str2, int i, OnOpChangedListener onOpChangedListener) {
        startWatchingMode(strOpToOp(str), str2, i, onOpChangedListener);
    }

    public void startWatchingMode(int i, String str, OnOpChangedListener onOpChangedListener) {
        startWatchingMode(i, str, 0, onOpChangedListener);
    }

    public void startWatchingMode(int i, String str, int i2, final OnOpChangedListener onOpChangedListener) {
        synchronized (this.mModeWatchers) {
            IAppOpsCallback iAppOpsCallback = this.mModeWatchers.get(onOpChangedListener);
            if (iAppOpsCallback == null) {
                iAppOpsCallback = new IAppOpsCallback.Stub() {
                    @Override
                    public void opChanged(int i3, int i4, String str2) {
                        if (onOpChangedListener instanceof OnOpChangedInternalListener) {
                            ((OnOpChangedInternalListener) onOpChangedListener).onOpChanged(i3, str2);
                        }
                        if (AppOpsManager.sOpToString[i3] != null) {
                            onOpChangedListener.onOpChanged(AppOpsManager.sOpToString[i3], str2);
                        }
                    }
                };
                this.mModeWatchers.put(onOpChangedListener, iAppOpsCallback);
            }
            try {
                this.mService.startWatchingModeWithFlags(i, str, i2, iAppOpsCallback);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void stopWatchingMode(OnOpChangedListener onOpChangedListener) {
        synchronized (this.mModeWatchers) {
            IAppOpsCallback iAppOpsCallback = this.mModeWatchers.get(onOpChangedListener);
            if (iAppOpsCallback != null) {
                try {
                    this.mService.stopWatchingMode(iAppOpsCallback);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    public void startWatchingActive(int[] iArr, final OnOpActiveChangedListener onOpActiveChangedListener) {
        Preconditions.checkNotNull(iArr, "ops cannot be null");
        Preconditions.checkNotNull(onOpActiveChangedListener, "callback cannot be null");
        synchronized (this.mActiveWatchers) {
            if (this.mActiveWatchers.get(onOpActiveChangedListener) != null) {
                return;
            }
            IAppOpsActiveCallback.Stub stub = new IAppOpsActiveCallback.Stub() {
                @Override
                public void opActiveChanged(int i, int i2, String str, boolean z) {
                    onOpActiveChangedListener.onOpActiveChanged(i, i2, str, z);
                }
            };
            this.mActiveWatchers.put(onOpActiveChangedListener, stub);
            try {
                this.mService.startWatchingActive(iArr, stub);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void stopWatchingActive(OnOpActiveChangedListener onOpActiveChangedListener) {
        synchronized (this.mActiveWatchers) {
            IAppOpsActiveCallback iAppOpsActiveCallback = this.mActiveWatchers.get(onOpActiveChangedListener);
            if (iAppOpsActiveCallback != null) {
                try {
                    this.mService.stopWatchingActive(iAppOpsActiveCallback);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    private String buildSecurityExceptionMsg(int i, int i2, String str) {
        return str + " from uid " + i2 + " not allowed to perform " + sOpNames[i];
    }

    public static int strOpToOp(String str) {
        if (CTA_MANAGER.isCtaSupported()) {
            return CTA_MANAGER.strOpToOp(str);
        }
        Integer num = sOpStrToOp.get(str);
        if (num == null) {
            throw new IllegalArgumentException("Unknown operation string: " + str);
        }
        return num.intValue();
    }

    public int checkOp(String str, int i, String str2) {
        return checkOp(strOpToOp(str), i, str2);
    }

    public int checkOpNoThrow(String str, int i, String str2) {
        return checkOpNoThrow(strOpToOp(str), i, str2);
    }

    public int unsafeCheckOpRaw(String str, int i, String str2) {
        try {
            return this.mService.checkOperation(strOpToOp(str), i, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int noteOp(String str, int i, String str2) {
        return noteOp(strOpToOp(str), i, str2);
    }

    public int noteOpNoThrow(String str, int i, String str2) {
        return noteOpNoThrow(strOpToOp(str), i, str2);
    }

    public int noteProxyOp(String str, String str2) {
        return noteProxyOp(strOpToOp(str), str2);
    }

    public int noteProxyOpNoThrow(String str, String str2) {
        return noteProxyOpNoThrow(strOpToOp(str), str2);
    }

    public int startOp(String str, int i, String str2) {
        return startOp(strOpToOp(str), i, str2);
    }

    public int startOpNoThrow(String str, int i, String str2) {
        return startOpNoThrow(strOpToOp(str), i, str2);
    }

    public void finishOp(String str, int i, String str2) {
        finishOp(strOpToOp(str), i, str2);
    }

    public int checkOp(int i, int i2, String str) {
        try {
            int iCheckOperation = this.mService.checkOperation(i, i2, str);
            if (iCheckOperation == 2) {
                throw new SecurityException(buildSecurityExceptionMsg(i, i2, str));
            }
            return iCheckOperation;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int checkOpNoThrow(int i, int i2, String str) {
        try {
            int iCheckOperation = this.mService.checkOperation(i, i2, str);
            if (iCheckOperation == 4) {
                return 0;
            }
            return iCheckOperation;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void checkPackage(int i, String str) {
        try {
            if (this.mService.checkPackage(i, str) != 0) {
                throw new SecurityException("Package " + str + " does not belong to " + i);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int checkAudioOp(int i, int i2, int i3, String str) {
        try {
            int iCheckAudioOperation = this.mService.checkAudioOperation(i, i2, i3, str);
            if (iCheckAudioOperation == 2) {
                throw new SecurityException(buildSecurityExceptionMsg(i, i3, str));
            }
            return iCheckAudioOperation;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int checkAudioOpNoThrow(int i, int i2, int i3, String str) {
        try {
            return this.mService.checkAudioOperation(i, i2, i3, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int noteOp(int i, int i2, String str) {
        int iNoteOpNoThrow = noteOpNoThrow(i, i2, str);
        if (iNoteOpNoThrow == 2) {
            throw new SecurityException(buildSecurityExceptionMsg(i, i2, str));
        }
        return iNoteOpNoThrow;
    }

    public int noteProxyOp(int i, String str) {
        int iNoteProxyOpNoThrow = noteProxyOpNoThrow(i, str);
        if (iNoteProxyOpNoThrow == 2) {
            throw new SecurityException("Proxy package " + this.mContext.getOpPackageName() + " from uid " + Process.myUid() + " or calling package " + str + " from uid " + Binder.getCallingUid() + " not allowed to perform " + sOpNames[i]);
        }
        return iNoteProxyOpNoThrow;
    }

    public int noteProxyOpNoThrow(int i, String str) {
        try {
            return this.mService.noteProxyOperation(i, this.mContext.getOpPackageName(), Binder.getCallingUid(), str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int noteOpNoThrow(int i, int i2, String str) {
        try {
            return this.mService.noteOperation(i, i2, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int noteOp(int i) {
        return noteOp(i, Process.myUid(), this.mContext.getOpPackageName());
    }

    public static IBinder getToken(IAppOpsService iAppOpsService) {
        synchronized (AppOpsManager.class) {
            if (sToken != null) {
                return sToken;
            }
            try {
                sToken = iAppOpsService.getToken(new Binder());
                return sToken;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int startOp(int i) {
        return startOp(i, Process.myUid(), this.mContext.getOpPackageName());
    }

    public int startOp(int i, int i2, String str) {
        return startOp(i, i2, str, false);
    }

    public int startOp(int i, int i2, String str, boolean z) {
        int iStartOpNoThrow = startOpNoThrow(i, i2, str, z);
        if (iStartOpNoThrow == 2) {
            throw new SecurityException(buildSecurityExceptionMsg(i, i2, str));
        }
        return iStartOpNoThrow;
    }

    public int startOpNoThrow(int i, int i2, String str) {
        return startOpNoThrow(i, i2, str, false);
    }

    public int startOpNoThrow(int i, int i2, String str, boolean z) {
        try {
            return this.mService.startOperation(getToken(this.mService), i, i2, str, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void finishOp(int i, int i2, String str) {
        try {
            this.mService.finishOperation(getToken(this.mService), i, i2, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void finishOp(int i) {
        finishOp(i, Process.myUid(), this.mContext.getOpPackageName());
    }

    public boolean isOperationActive(int i, int i2, String str) {
        try {
            return this.mService.isOperationActive(i, i2, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public static String[] getOpStrs() {
        return (String[]) Arrays.copyOf(sOpToString, sOpToString.length);
    }

    public static long maxTime(long[] jArr, int i, int i2) {
        long j = 0;
        while (i < i2) {
            if (jArr[i] > j) {
                j = jArr[i];
            }
            i++;
        }
        return j;
    }
}
