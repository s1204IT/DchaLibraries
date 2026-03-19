package android.content;

import android.annotation.SystemApi;
import android.app.backup.FullBackup;
import android.bluetooth.BluetoothHidDevice;
import android.content.ClipData;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.ShellCommand;
import android.os.StrictMode;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.ims.ImsReasonInfo;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import com.android.internal.R;
import com.android.internal.midi.MidiConstants;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class Intent implements Parcelable, Cloneable {
    public static final String ACTION_ADVANCED_SETTINGS_CHANGED = "android.intent.action.ADVANCED_SETTINGS";
    public static final String ACTION_AIRPLANE_MODE_CHANGED = "android.intent.action.AIRPLANE_MODE";
    public static final String ACTION_ALARM_CHANGED = "android.intent.action.ALARM_CHANGED";
    public static final String ACTION_ALL_APPS = "android.intent.action.ALL_APPS";
    public static final String ACTION_ANSWER = "android.intent.action.ANSWER";
    public static final String ACTION_APPLICATION_PREFERENCES = "android.intent.action.APPLICATION_PREFERENCES";
    public static final String ACTION_APPLICATION_RESTRICTIONS_CHANGED = "android.intent.action.APPLICATION_RESTRICTIONS_CHANGED";
    public static final String ACTION_APP_ERROR = "android.intent.action.APP_ERROR";
    public static final String ACTION_ASSIST = "android.intent.action.ASSIST";
    public static final String ACTION_ATTACH_DATA = "android.intent.action.ATTACH_DATA";
    public static final String ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";

    @SystemApi
    public static final String ACTION_BATTERY_LEVEL_CHANGED = "android.intent.action.BATTERY_LEVEL_CHANGED";
    public static final String ACTION_BATTERY_LOW = "android.intent.action.BATTERY_LOW";
    public static final String ACTION_BATTERY_OKAY = "android.intent.action.BATTERY_OKAY";
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_BUG_REPORT = "android.intent.action.BUG_REPORT";
    public static final String ACTION_CALL = "android.intent.action.CALL";
    public static final String ACTION_CALL_BUTTON = "android.intent.action.CALL_BUTTON";

    @SystemApi
    public static final String ACTION_CALL_EMERGENCY = "android.intent.action.CALL_EMERGENCY";

    @SystemApi
    public static final String ACTION_CALL_PRIVILEGED = "android.intent.action.CALL_PRIVILEGED";
    public static final String ACTION_CAMERA_BUTTON = "android.intent.action.CAMERA_BUTTON";
    public static final String ACTION_CARRIER_SETUP = "android.intent.action.CARRIER_SETUP";
    public static final String ACTION_CHOOSER = "android.intent.action.CHOOSER";
    public static final String ACTION_CLEAR_DNS_CACHE = "android.intent.action.CLEAR_DNS_CACHE";
    public static final String ACTION_CLOSE_SYSTEM_DIALOGS = "android.intent.action.CLOSE_SYSTEM_DIALOGS";
    public static final String ACTION_CONFIGURATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    public static final String ACTION_CREATE_DOCUMENT = "android.intent.action.CREATE_DOCUMENT";
    public static final String ACTION_CREATE_SHORTCUT = "android.intent.action.CREATE_SHORTCUT";
    public static final String ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED";
    public static final String ACTION_DEFAULT = "android.intent.action.VIEW";
    public static final String ACTION_DELETE = "android.intent.action.DELETE";

    @SystemApi
    @Deprecated
    public static final String ACTION_DEVICE_INITIALIZATION_WIZARD = "android.intent.action.DEVICE_INITIALIZATION_WIZARD";
    public static final String ACTION_DEVICE_LOCKED_CHANGED = "android.intent.action.DEVICE_LOCKED_CHANGED";

    @Deprecated
    public static final String ACTION_DEVICE_STORAGE_FULL = "android.intent.action.DEVICE_STORAGE_FULL";

    @Deprecated
    public static final String ACTION_DEVICE_STORAGE_LOW = "android.intent.action.DEVICE_STORAGE_LOW";

    @Deprecated
    public static final String ACTION_DEVICE_STORAGE_NOT_FULL = "android.intent.action.DEVICE_STORAGE_NOT_FULL";

    @Deprecated
    public static final String ACTION_DEVICE_STORAGE_OK = "android.intent.action.DEVICE_STORAGE_OK";
    public static final String ACTION_DIAL = "android.intent.action.DIAL";
    public static final String ACTION_DISMISS_KEYBOARD_SHORTCUTS = "com.android.intent.action.DISMISS_KEYBOARD_SHORTCUTS";
    public static final String ACTION_DOCK_ACTIVE = "android.intent.action.DOCK_ACTIVE";
    public static final String ACTION_DOCK_EVENT = "android.intent.action.DOCK_EVENT";
    public static final String ACTION_DOCK_IDLE = "android.intent.action.DOCK_IDLE";
    public static final String ACTION_DREAMING_STARTED = "android.intent.action.DREAMING_STARTED";
    public static final String ACTION_DREAMING_STOPPED = "android.intent.action.DREAMING_STOPPED";
    public static final String ACTION_DYNAMIC_SENSOR_CHANGED = "android.intent.action.DYNAMIC_SENSOR_CHANGED";
    public static final String ACTION_EDIT = "android.intent.action.EDIT";
    public static final String ACTION_EXTERNAL_APPLICATIONS_AVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";
    public static final String ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";

    @SystemApi
    public static final String ACTION_FACTORY_RESET = "android.intent.action.FACTORY_RESET";
    public static final String ACTION_FACTORY_TEST = "android.intent.action.FACTORY_TEST";
    public static final String ACTION_GET_CONTENT = "android.intent.action.GET_CONTENT";
    public static final String ACTION_GET_RESTRICTION_ENTRIES = "android.intent.action.GET_RESTRICTION_ENTRIES";

    @SystemApi
    public static final String ACTION_GLOBAL_BUTTON = "android.intent.action.GLOBAL_BUTTON";
    public static final String ACTION_GTALK_SERVICE_CONNECTED = "android.intent.action.GTALK_CONNECTED";
    public static final String ACTION_GTALK_SERVICE_DISCONNECTED = "android.intent.action.GTALK_DISCONNECTED";
    public static final String ACTION_HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    public static final String ACTION_IDLE_MAINTENANCE_END = "android.intent.action.ACTION_IDLE_MAINTENANCE_END";
    public static final String ACTION_IDLE_MAINTENANCE_START = "android.intent.action.ACTION_IDLE_MAINTENANCE_START";
    public static final String ACTION_INPUT_METHOD_CHANGED = "android.intent.action.INPUT_METHOD_CHANGED";
    public static final String ACTION_INSERT = "android.intent.action.INSERT";
    public static final String ACTION_INSERT_OR_EDIT = "android.intent.action.INSERT_OR_EDIT";
    public static final String ACTION_INSTALL_FAILURE = "android.intent.action.INSTALL_FAILURE";

    @SystemApi
    public static final String ACTION_INSTALL_INSTANT_APP_PACKAGE = "android.intent.action.INSTALL_INSTANT_APP_PACKAGE";
    public static final String ACTION_INSTALL_PACKAGE = "android.intent.action.INSTALL_PACKAGE";

    @SystemApi
    public static final String ACTION_INSTANT_APP_RESOLVER_SETTINGS = "android.intent.action.INSTANT_APP_RESOLVER_SETTINGS";

    @SystemApi
    public static final String ACTION_INTENT_FILTER_NEEDS_VERIFICATION = "android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION";
    public static final String ACTION_LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED";
    public static final String ACTION_LOCKED_BOOT_COMPLETED = "android.intent.action.LOCKED_BOOT_COMPLETED";
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_MANAGED_PROFILE_ADDED = "android.intent.action.MANAGED_PROFILE_ADDED";
    public static final String ACTION_MANAGED_PROFILE_AVAILABLE = "android.intent.action.MANAGED_PROFILE_AVAILABLE";
    public static final String ACTION_MANAGED_PROFILE_REMOVED = "android.intent.action.MANAGED_PROFILE_REMOVED";
    public static final String ACTION_MANAGED_PROFILE_UNAVAILABLE = "android.intent.action.MANAGED_PROFILE_UNAVAILABLE";
    public static final String ACTION_MANAGED_PROFILE_UNLOCKED = "android.intent.action.MANAGED_PROFILE_UNLOCKED";

    @SystemApi
    public static final String ACTION_MANAGE_APP_PERMISSIONS = "android.intent.action.MANAGE_APP_PERMISSIONS";
    public static final String ACTION_MANAGE_NETWORK_USAGE = "android.intent.action.MANAGE_NETWORK_USAGE";
    public static final String ACTION_MANAGE_PACKAGE_STORAGE = "android.intent.action.MANAGE_PACKAGE_STORAGE";

    @SystemApi
    public static final String ACTION_MANAGE_PERMISSIONS = "android.intent.action.MANAGE_PERMISSIONS";

    @SystemApi
    public static final String ACTION_MANAGE_PERMISSION_APPS = "android.intent.action.MANAGE_PERMISSION_APPS";

    @SystemApi
    @Deprecated
    public static final String ACTION_MASTER_CLEAR = "android.intent.action.MASTER_CLEAR";

    @SystemApi
    public static final String ACTION_MASTER_CLEAR_NOTIFICATION = "android.intent.action.MASTER_CLEAR_NOTIFICATION";
    public static final String ACTION_MEDIA_BAD_REMOVAL = "android.intent.action.MEDIA_BAD_REMOVAL";
    public static final String ACTION_MEDIA_BUTTON = "android.intent.action.MEDIA_BUTTON";
    public static final String ACTION_MEDIA_CHECKING = "android.intent.action.MEDIA_CHECKING";
    public static final String ACTION_MEDIA_EJECT = "android.intent.action.MEDIA_EJECT";
    public static final String ACTION_MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
    public static final String ACTION_MEDIA_NOFS = "android.intent.action.MEDIA_NOFS";
    public static final String ACTION_MEDIA_REMOVED = "android.intent.action.MEDIA_REMOVED";
    public static final String ACTION_MEDIA_RESOURCE_GRANTED = "android.intent.action.MEDIA_RESOURCE_GRANTED";
    public static final String ACTION_MEDIA_SCANNER_FINISHED = "android.intent.action.MEDIA_SCANNER_FINISHED";
    public static final String ACTION_MEDIA_SCANNER_SCAN_FILE = "android.intent.action.MEDIA_SCANNER_SCAN_FILE";
    public static final String ACTION_MEDIA_SCANNER_STARTED = "android.intent.action.MEDIA_SCANNER_STARTED";
    public static final String ACTION_MEDIA_SHARED = "android.intent.action.MEDIA_SHARED";
    public static final String ACTION_MEDIA_UNMOUNTABLE = "android.intent.action.MEDIA_UNMOUNTABLE";
    public static final String ACTION_MEDIA_UNMOUNTED = "android.intent.action.MEDIA_UNMOUNTED";
    public static final String ACTION_MEDIA_UNSHARED = "android.intent.action.MEDIA_UNSHARED";
    public static final String ACTION_MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED";
    public static final String ACTION_MY_PACKAGE_SUSPENDED = "android.intent.action.MY_PACKAGE_SUSPENDED";
    public static final String ACTION_MY_PACKAGE_UNSUSPENDED = "android.intent.action.MY_PACKAGE_UNSUSPENDED";
    public static final String ACTION_NEW_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL";
    public static final String ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";
    public static final String ACTION_OPEN_DOCUMENT_TREE = "android.intent.action.OPEN_DOCUMENT_TREE";
    public static final String ACTION_OVERLAY_CHANGED = "android.intent.action.OVERLAY_CHANGED";
    public static final String ACTION_PACKAGES_SUSPENDED = "android.intent.action.PACKAGES_SUSPENDED";
    public static final String ACTION_PACKAGES_UNSUSPENDED = "android.intent.action.PACKAGES_UNSUSPENDED";
    public static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    public static final String ACTION_PACKAGE_CHANGED = "android.intent.action.PACKAGE_CHANGED";
    public static final String ACTION_PACKAGE_DATA_CLEARED = "android.intent.action.PACKAGE_DATA_CLEARED";
    public static final String ACTION_PACKAGE_FIRST_LAUNCH = "android.intent.action.PACKAGE_FIRST_LAUNCH";
    public static final String ACTION_PACKAGE_FULLY_REMOVED = "android.intent.action.PACKAGE_FULLY_REMOVED";

    @Deprecated
    public static final String ACTION_PACKAGE_INSTALL = "android.intent.action.PACKAGE_INSTALL";
    public static final String ACTION_PACKAGE_NEEDS_VERIFICATION = "android.intent.action.PACKAGE_NEEDS_VERIFICATION";
    public static final String ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED";
    public static final String ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";
    public static final String ACTION_PACKAGE_RESTARTED = "android.intent.action.PACKAGE_RESTARTED";
    public static final String ACTION_PACKAGE_VERIFIED = "android.intent.action.PACKAGE_VERIFIED";
    public static final String ACTION_PASTE = "android.intent.action.PASTE";
    public static final String ACTION_PICK = "android.intent.action.PICK";
    public static final String ACTION_PICK_ACTIVITY = "android.intent.action.PICK_ACTIVITY";
    public static final String ACTION_POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED";
    public static final String ACTION_POWER_DISCONNECTED = "android.intent.action.ACTION_POWER_DISCONNECTED";
    public static final String ACTION_POWER_USAGE_SUMMARY = "android.intent.action.POWER_USAGE_SUMMARY";
    public static final String ACTION_PREFERRED_ACTIVITY_CHANGED = "android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED";

    @SystemApi
    public static final String ACTION_PRE_BOOT_COMPLETED = "android.intent.action.PRE_BOOT_COMPLETED";
    public static final String ACTION_PROCESS_TEXT = "android.intent.action.PROCESS_TEXT";
    public static final String ACTION_PROVIDER_CHANGED = "android.intent.action.PROVIDER_CHANGED";

    @SystemApi
    public static final String ACTION_QUERY_PACKAGE_RESTART = "android.intent.action.QUERY_PACKAGE_RESTART";
    public static final String ACTION_QUICK_CLOCK = "android.intent.action.QUICK_CLOCK";
    public static final String ACTION_QUICK_VIEW = "android.intent.action.QUICK_VIEW";
    public static final String ACTION_REBOOT = "android.intent.action.REBOOT";
    public static final String ACTION_REMOTE_INTENT = "com.google.android.c2dm.intent.RECEIVE";
    public static final String ACTION_REQUEST_SHUTDOWN = "com.android.internal.intent.action.REQUEST_SHUTDOWN";

    @SystemApi
    public static final String ACTION_RESOLVE_INSTANT_APP_PACKAGE = "android.intent.action.RESOLVE_INSTANT_APP_PACKAGE";

    @SystemApi
    public static final String ACTION_REVIEW_PERMISSIONS = "android.intent.action.REVIEW_PERMISSIONS";
    public static final String ACTION_RUN = "android.intent.action.RUN";
    public static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    public static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
    public static final String ACTION_SEARCH = "android.intent.action.SEARCH";
    public static final String ACTION_SEARCH_LONG_PRESS = "android.intent.action.SEARCH_LONG_PRESS";
    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String ACTION_SENDTO = "android.intent.action.SENDTO";
    public static final String ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE";

    @SystemApi
    @Deprecated
    public static final String ACTION_SERVICE_STATE = "android.intent.action.SERVICE_STATE";
    public static final String ACTION_SETTING_RESTORED = "android.os.action.SETTING_RESTORED";
    public static final String ACTION_SET_WALLPAPER = "android.intent.action.SET_WALLPAPER";
    public static final String ACTION_SHOW_APP_INFO = "android.intent.action.SHOW_APP_INFO";
    public static final String ACTION_SHOW_BRIGHTNESS_DIALOG = "com.android.intent.action.SHOW_BRIGHTNESS_DIALOG";
    public static final String ACTION_SHOW_KEYBOARD_SHORTCUTS = "com.android.intent.action.SHOW_KEYBOARD_SHORTCUTS";

    @SystemApi
    public static final String ACTION_SHOW_SUSPENDED_APP_DETAILS = "android.intent.action.SHOW_SUSPENDED_APP_DETAILS";
    public static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";

    @SystemApi
    @Deprecated
    public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";

    @SystemApi
    public static final String ACTION_SPLIT_CONFIGURATION_CHANGED = "android.intent.action.SPLIT_CONFIGURATION_CHANGED";
    public static final String ACTION_SYNC = "android.intent.action.SYNC";
    public static final String ACTION_SYSTEM_TUTORIAL = "android.intent.action.SYSTEM_TUTORIAL";
    public static final String ACTION_THERMAL_EVENT = "android.intent.action.THERMAL_EVENT";
    public static final String ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
    public static final String ACTION_TIME_CHANGED = "android.intent.action.TIME_SET";
    public static final String ACTION_TIME_TICK = "android.intent.action.TIME_TICK";
    public static final String ACTION_UID_REMOVED = "android.intent.action.UID_REMOVED";

    @Deprecated
    public static final String ACTION_UMS_CONNECTED = "android.intent.action.UMS_CONNECTED";

    @Deprecated
    public static final String ACTION_UMS_DISCONNECTED = "android.intent.action.UMS_DISCONNECTED";
    public static final String ACTION_UNINSTALL_PACKAGE = "android.intent.action.UNINSTALL_PACKAGE";

    @SystemApi
    public static final String ACTION_UPGRADE_SETUP = "android.intent.action.UPGRADE_SETUP";
    public static final String ACTION_USER_ADDED = "android.intent.action.USER_ADDED";
    public static final String ACTION_USER_BACKGROUND = "android.intent.action.USER_BACKGROUND";
    public static final String ACTION_USER_FOREGROUND = "android.intent.action.USER_FOREGROUND";
    public static final String ACTION_USER_INFO_CHANGED = "android.intent.action.USER_INFO_CHANGED";
    public static final String ACTION_USER_INITIALIZE = "android.intent.action.USER_INITIALIZE";
    public static final String ACTION_USER_PRESENT = "android.intent.action.USER_PRESENT";

    @SystemApi
    public static final String ACTION_USER_REMOVED = "android.intent.action.USER_REMOVED";
    public static final String ACTION_USER_STARTED = "android.intent.action.USER_STARTED";
    public static final String ACTION_USER_STARTING = "android.intent.action.USER_STARTING";
    public static final String ACTION_USER_STOPPED = "android.intent.action.USER_STOPPED";
    public static final String ACTION_USER_STOPPING = "android.intent.action.USER_STOPPING";
    public static final String ACTION_USER_SWITCHED = "android.intent.action.USER_SWITCHED";
    public static final String ACTION_USER_UNLOCKED = "android.intent.action.USER_UNLOCKED";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";

    @SystemApi
    public static final String ACTION_VOICE_ASSIST = "android.intent.action.VOICE_ASSIST";
    public static final String ACTION_VOICE_COMMAND = "android.intent.action.VOICE_COMMAND";

    @Deprecated
    public static final String ACTION_WALLPAPER_CHANGED = "android.intent.action.WALLPAPER_CHANGED";
    public static final String ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH";
    private static final String ATTR_ACTION = "action";
    private static final String ATTR_CATEGORY = "category";
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_DATA = "data";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_TYPE = "type";
    public static final String CATEGORY_ALTERNATIVE = "android.intent.category.ALTERNATIVE";
    public static final String CATEGORY_APP_BROWSER = "android.intent.category.APP_BROWSER";
    public static final String CATEGORY_APP_CALCULATOR = "android.intent.category.APP_CALCULATOR";
    public static final String CATEGORY_APP_CALENDAR = "android.intent.category.APP_CALENDAR";
    public static final String CATEGORY_APP_CONTACTS = "android.intent.category.APP_CONTACTS";
    public static final String CATEGORY_APP_EMAIL = "android.intent.category.APP_EMAIL";
    public static final String CATEGORY_APP_GALLERY = "android.intent.category.APP_GALLERY";
    public static final String CATEGORY_APP_MAPS = "android.intent.category.APP_MAPS";
    public static final String CATEGORY_APP_MARKET = "android.intent.category.APP_MARKET";
    public static final String CATEGORY_APP_MESSAGING = "android.intent.category.APP_MESSAGING";
    public static final String CATEGORY_APP_MUSIC = "android.intent.category.APP_MUSIC";
    public static final String CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE";
    public static final String CATEGORY_CAR_DOCK = "android.intent.category.CAR_DOCK";
    public static final String CATEGORY_CAR_LAUNCHER = "android.intent.category.CAR_LAUNCHER";
    public static final String CATEGORY_CAR_MODE = "android.intent.category.CAR_MODE";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final String CATEGORY_DESK_DOCK = "android.intent.category.DESK_DOCK";
    public static final String CATEGORY_DEVELOPMENT_PREFERENCE = "android.intent.category.DEVELOPMENT_PREFERENCE";
    public static final String CATEGORY_EMBED = "android.intent.category.EMBED";
    public static final String CATEGORY_FRAMEWORK_INSTRUMENTATION_TEST = "android.intent.category.FRAMEWORK_INSTRUMENTATION_TEST";
    public static final String CATEGORY_HE_DESK_DOCK = "android.intent.category.HE_DESK_DOCK";
    public static final String CATEGORY_HOME = "android.intent.category.HOME";
    public static final String CATEGORY_HOME_MAIN = "android.intent.category.HOME_MAIN";
    public static final String CATEGORY_INFO = "android.intent.category.INFO";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_LAUNCHER_APP = "android.intent.category.LAUNCHER_APP";
    public static final String CATEGORY_LEANBACK_LAUNCHER = "android.intent.category.LEANBACK_LAUNCHER";

    @SystemApi
    public static final String CATEGORY_LEANBACK_SETTINGS = "android.intent.category.LEANBACK_SETTINGS";
    public static final String CATEGORY_LE_DESK_DOCK = "android.intent.category.LE_DESK_DOCK";
    public static final String CATEGORY_MONKEY = "android.intent.category.MONKEY";
    public static final String CATEGORY_OPENABLE = "android.intent.category.OPENABLE";
    public static final String CATEGORY_PREFERENCE = "android.intent.category.PREFERENCE";
    public static final String CATEGORY_SAMPLE_CODE = "android.intent.category.SAMPLE_CODE";
    public static final String CATEGORY_SELECTED_ALTERNATIVE = "android.intent.category.SELECTED_ALTERNATIVE";
    public static final String CATEGORY_SETUP_WIZARD = "android.intent.category.SETUP_WIZARD";
    public static final String CATEGORY_TAB = "android.intent.category.TAB";
    public static final String CATEGORY_TEST = "android.intent.category.TEST";
    public static final String CATEGORY_TYPED_OPENABLE = "android.intent.category.TYPED_OPENABLE";
    public static final String CATEGORY_UNIT_TEST = "android.intent.category.UNIT_TEST";
    public static final String CATEGORY_VOICE = "android.intent.category.VOICE";
    public static final String CATEGORY_VR_HOME = "android.intent.category.VR_HOME";
    private static final int COPY_MODE_ALL = 0;
    private static final int COPY_MODE_FILTER = 1;
    private static final int COPY_MODE_HISTORY = 2;
    public static final Parcelable.Creator<Intent> CREATOR = new Parcelable.Creator<Intent>() {
        @Override
        public Intent createFromParcel(Parcel parcel) {
            return new Intent(parcel);
        }

        @Override
        public Intent[] newArray(int i) {
            return new Intent[i];
        }
    };
    public static final String EXTRA_ALARM_COUNT = "android.intent.extra.ALARM_COUNT";
    public static final String EXTRA_ALLOW_MULTIPLE = "android.intent.extra.ALLOW_MULTIPLE";

    @Deprecated
    public static final String EXTRA_ALLOW_REPLACE = "android.intent.extra.ALLOW_REPLACE";
    public static final String EXTRA_ALTERNATE_INTENTS = "android.intent.extra.ALTERNATE_INTENTS";
    public static final String EXTRA_ASSIST_CONTEXT = "android.intent.extra.ASSIST_CONTEXT";
    public static final String EXTRA_ASSIST_INPUT_DEVICE_ID = "android.intent.extra.ASSIST_INPUT_DEVICE_ID";
    public static final String EXTRA_ASSIST_INPUT_HINT_KEYBOARD = "android.intent.extra.ASSIST_INPUT_HINT_KEYBOARD";
    public static final String EXTRA_ASSIST_PACKAGE = "android.intent.extra.ASSIST_PACKAGE";
    public static final String EXTRA_ASSIST_UID = "android.intent.extra.ASSIST_UID";
    public static final String EXTRA_AUTO_LAUNCH_SINGLE_CHOICE = "android.intent.extra.AUTO_LAUNCH_SINGLE_CHOICE";
    public static final String EXTRA_BCC = "android.intent.extra.BCC";
    public static final String EXTRA_BUG_REPORT = "android.intent.extra.BUG_REPORT";

    @SystemApi
    public static final String EXTRA_CALLING_PACKAGE = "android.intent.extra.CALLING_PACKAGE";
    public static final String EXTRA_CC = "android.intent.extra.CC";

    @SystemApi
    @Deprecated
    public static final String EXTRA_CDMA_DEFAULT_ROAMING_INDICATOR = "cdmaDefaultRoamingIndicator";

    @SystemApi
    @Deprecated
    public static final String EXTRA_CDMA_ROAMING_INDICATOR = "cdmaRoamingIndicator";

    @Deprecated
    public static final String EXTRA_CHANGED_COMPONENT_NAME = "android.intent.extra.changed_component_name";
    public static final String EXTRA_CHANGED_COMPONENT_NAME_LIST = "android.intent.extra.changed_component_name_list";
    public static final String EXTRA_CHANGED_PACKAGE_LIST = "android.intent.extra.changed_package_list";
    public static final String EXTRA_CHANGED_UID_LIST = "android.intent.extra.changed_uid_list";
    public static final String EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER = "android.intent.extra.CHOOSER_REFINEMENT_INTENT_SENDER";
    public static final String EXTRA_CHOOSER_TARGETS = "android.intent.extra.CHOOSER_TARGETS";
    public static final String EXTRA_CHOSEN_COMPONENT = "android.intent.extra.CHOSEN_COMPONENT";
    public static final String EXTRA_CHOSEN_COMPONENT_INTENT_SENDER = "android.intent.extra.CHOSEN_COMPONENT_INTENT_SENDER";
    public static final String EXTRA_CLIENT_INTENT = "android.intent.extra.client_intent";
    public static final String EXTRA_CLIENT_LABEL = "android.intent.extra.client_label";
    public static final String EXTRA_COMPONENT_NAME = "android.intent.extra.COMPONENT_NAME";
    public static final String EXTRA_CONTENT_ANNOTATIONS = "android.intent.extra.CONTENT_ANNOTATIONS";

    @SystemApi
    @Deprecated
    public static final String EXTRA_CSS_INDICATOR = "cssIndicator";

    @SystemApi
    @Deprecated
    public static final String EXTRA_DATA_OPERATOR_ALPHA_LONG = "data-operator-alpha-long";

    @SystemApi
    @Deprecated
    public static final String EXTRA_DATA_OPERATOR_ALPHA_SHORT = "data-operator-alpha-short";

    @SystemApi
    @Deprecated
    public static final String EXTRA_DATA_OPERATOR_NUMERIC = "data-operator-numeric";

    @SystemApi
    @Deprecated
    public static final String EXTRA_DATA_RADIO_TECH = "dataRadioTechnology";

    @SystemApi
    @Deprecated
    public static final String EXTRA_DATA_REG_STATE = "dataRegState";
    public static final String EXTRA_DATA_REMOVED = "android.intent.extra.DATA_REMOVED";

    @SystemApi
    @Deprecated
    public static final String EXTRA_DATA_ROAMING_TYPE = "dataRoamingType";
    public static final String EXTRA_DOCK_STATE = "android.intent.extra.DOCK_STATE";
    public static final int EXTRA_DOCK_STATE_CAR = 2;
    public static final int EXTRA_DOCK_STATE_DESK = 1;
    public static final int EXTRA_DOCK_STATE_HE_DESK = 4;
    public static final int EXTRA_DOCK_STATE_LE_DESK = 3;
    public static final int EXTRA_DOCK_STATE_UNDOCKED = 0;
    public static final String EXTRA_DONT_KILL_APP = "android.intent.extra.DONT_KILL_APP";
    public static final String EXTRA_EMAIL = "android.intent.extra.EMAIL";

    @SystemApi
    @Deprecated
    public static final String EXTRA_EMERGENCY_ONLY = "emergencyOnly";

    @Deprecated
    public static final String EXTRA_EPHEMERAL_FAILURE = "android.intent.extra.EPHEMERAL_FAILURE";

    @Deprecated
    public static final String EXTRA_EPHEMERAL_HOSTNAME = "android.intent.extra.EPHEMERAL_HOSTNAME";

    @Deprecated
    public static final String EXTRA_EPHEMERAL_SUCCESS = "android.intent.extra.EPHEMERAL_SUCCESS";

    @Deprecated
    public static final String EXTRA_EPHEMERAL_TOKEN = "android.intent.extra.EPHEMERAL_TOKEN";
    public static final String EXTRA_EXCLUDE_COMPONENTS = "android.intent.extra.EXCLUDE_COMPONENTS";

    @SystemApi
    public static final String EXTRA_FORCE_FACTORY_RESET = "android.intent.extra.FORCE_FACTORY_RESET";

    @Deprecated
    public static final String EXTRA_FORCE_MASTER_CLEAR = "android.intent.extra.FORCE_MASTER_CLEAR";
    public static final String EXTRA_FROM_STORAGE = "android.intent.extra.FROM_STORAGE";
    public static final String EXTRA_HTML_TEXT = "android.intent.extra.HTML_TEXT";
    public static final String EXTRA_INDEX = "android.intent.extra.INDEX";
    public static final String EXTRA_INITIAL_INTENTS = "android.intent.extra.INITIAL_INTENTS";
    public static final String EXTRA_INSTALLER_PACKAGE_NAME = "android.intent.extra.INSTALLER_PACKAGE_NAME";
    public static final String EXTRA_INSTALL_RESULT = "android.intent.extra.INSTALL_RESULT";

    @SystemApi
    public static final String EXTRA_INSTANT_APP_ACTION = "android.intent.extra.INSTANT_APP_ACTION";

    @SystemApi
    public static final String EXTRA_INSTANT_APP_BUNDLES = "android.intent.extra.INSTANT_APP_BUNDLES";

    @SystemApi
    public static final String EXTRA_INSTANT_APP_EXTRAS = "android.intent.extra.INSTANT_APP_EXTRAS";

    @SystemApi
    public static final String EXTRA_INSTANT_APP_FAILURE = "android.intent.extra.INSTANT_APP_FAILURE";

    @SystemApi
    public static final String EXTRA_INSTANT_APP_HOSTNAME = "android.intent.extra.INSTANT_APP_HOSTNAME";

    @SystemApi
    public static final String EXTRA_INSTANT_APP_SUCCESS = "android.intent.extra.INSTANT_APP_SUCCESS";

    @SystemApi
    public static final String EXTRA_INSTANT_APP_TOKEN = "android.intent.extra.INSTANT_APP_TOKEN";
    public static final String EXTRA_INTENT = "android.intent.extra.INTENT";

    @SystemApi
    @Deprecated
    public static final String EXTRA_IS_DATA_ROAMING_FROM_REGISTRATION = "isDataRoamingFromRegistration";

    @SystemApi
    @Deprecated
    public static final String EXTRA_IS_USING_CARRIER_AGGREGATION = "isUsingCarrierAggregation";
    public static final String EXTRA_KEY_CONFIRM = "android.intent.extra.KEY_CONFIRM";
    public static final String EXTRA_KEY_EVENT = "android.intent.extra.KEY_EVENT";
    public static final String EXTRA_LAUNCHER_EXTRAS = "android.intent.extra.LAUNCHER_EXTRAS";
    public static final String EXTRA_LOCAL_ONLY = "android.intent.extra.LOCAL_ONLY";

    @SystemApi
    public static final String EXTRA_LONG_VERSION_CODE = "android.intent.extra.LONG_VERSION_CODE";

    @SystemApi
    @Deprecated
    public static final String EXTRA_LTE_EARFCN_RSRP_BOOST = "LteEarfcnRsrpBoost";

    @SystemApi
    @Deprecated
    public static final String EXTRA_MANUAL = "manual";
    public static final String EXTRA_MEDIA_RESOURCE_TYPE = "android.intent.extra.MEDIA_RESOURCE_TYPE";
    public static final int EXTRA_MEDIA_RESOURCE_TYPE_AUDIO_CODEC = 1;
    public static final int EXTRA_MEDIA_RESOURCE_TYPE_VIDEO_CODEC = 0;
    public static final String EXTRA_MIME_TYPES = "android.intent.extra.MIME_TYPES";

    @SystemApi
    @Deprecated
    public static final String EXTRA_NETWORK_ID = "networkId";
    public static final String EXTRA_NOT_UNKNOWN_SOURCE = "android.intent.extra.NOT_UNKNOWN_SOURCE";

    @SystemApi
    @Deprecated
    public static final String EXTRA_OPERATOR_ALPHA_LONG = "operator-alpha-long";

    @SystemApi
    @Deprecated
    public static final String EXTRA_OPERATOR_ALPHA_SHORT = "operator-alpha-short";

    @SystemApi
    @Deprecated
    public static final String EXTRA_OPERATOR_NUMERIC = "operator-numeric";

    @SystemApi
    public static final String EXTRA_ORIGINATING_UID = "android.intent.extra.ORIGINATING_UID";
    public static final String EXTRA_ORIGINATING_URI = "android.intent.extra.ORIGINATING_URI";

    @SystemApi
    public static final String EXTRA_PACKAGES = "android.intent.extra.PACKAGES";
    public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME";

    @SystemApi
    public static final String EXTRA_PERMISSION_NAME = "android.intent.extra.PERMISSION_NAME";
    public static final String EXTRA_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER";
    public static final String EXTRA_PROCESS_TEXT = "android.intent.extra.PROCESS_TEXT";
    public static final String EXTRA_PROCESS_TEXT_READONLY = "android.intent.extra.PROCESS_TEXT_READONLY";

    @Deprecated
    public static final String EXTRA_QUICK_VIEW_ADVANCED = "android.intent.extra.QUICK_VIEW_ADVANCED";
    public static final String EXTRA_QUICK_VIEW_FEATURES = "android.intent.extra.QUICK_VIEW_FEATURES";
    public static final String EXTRA_QUIET_MODE = "android.intent.extra.QUIET_MODE";

    @SystemApi
    public static final String EXTRA_REASON = "android.intent.extra.REASON";
    public static final String EXTRA_REFERRER = "android.intent.extra.REFERRER";
    public static final String EXTRA_REFERRER_NAME = "android.intent.extra.REFERRER_NAME";

    @SystemApi
    public static final String EXTRA_REMOTE_CALLBACK = "android.intent.extra.REMOTE_CALLBACK";
    public static final String EXTRA_REMOTE_INTENT_TOKEN = "android.intent.extra.remote_intent_token";
    public static final String EXTRA_REMOVED_FOR_ALL_USERS = "android.intent.extra.REMOVED_FOR_ALL_USERS";
    public static final String EXTRA_REPLACEMENT_EXTRAS = "android.intent.extra.REPLACEMENT_EXTRAS";
    public static final String EXTRA_REPLACING = "android.intent.extra.REPLACING";
    public static final String EXTRA_RESTRICTIONS_BUNDLE = "android.intent.extra.restrictions_bundle";
    public static final String EXTRA_RESTRICTIONS_INTENT = "android.intent.extra.restrictions_intent";
    public static final String EXTRA_RESTRICTIONS_LIST = "android.intent.extra.restrictions_list";

    @SystemApi
    public static final String EXTRA_RESULT_NEEDED = "android.intent.extra.RESULT_NEEDED";
    public static final String EXTRA_RESULT_RECEIVER = "android.intent.extra.RESULT_RECEIVER";
    public static final String EXTRA_RETURN_RESULT = "android.intent.extra.RETURN_RESULT";
    public static final String EXTRA_SETTING_NAME = "setting_name";
    public static final String EXTRA_SETTING_NEW_VALUE = "new_value";
    public static final String EXTRA_SETTING_PREVIOUS_VALUE = "previous_value";
    public static final String EXTRA_SETTING_RESTORED_FROM_SDK_INT = "restored_from_sdk_int";

    @Deprecated
    public static final String EXTRA_SHORTCUT_ICON = "android.intent.extra.shortcut.ICON";

    @Deprecated
    public static final String EXTRA_SHORTCUT_ICON_RESOURCE = "android.intent.extra.shortcut.ICON_RESOURCE";

    @Deprecated
    public static final String EXTRA_SHORTCUT_INTENT = "android.intent.extra.shortcut.INTENT";

    @Deprecated
    public static final String EXTRA_SHORTCUT_NAME = "android.intent.extra.shortcut.NAME";
    public static final String EXTRA_SHUTDOWN_USERSPACE_ONLY = "android.intent.extra.SHUTDOWN_USERSPACE_ONLY";
    public static final String EXTRA_SIM_ACTIVATION_RESPONSE = "android.intent.extra.SIM_ACTIVATION_RESPONSE";
    public static final String EXTRA_SPLIT_NAME = "android.intent.extra.SPLIT_NAME";
    public static final String EXTRA_STREAM = "android.intent.extra.STREAM";
    public static final String EXTRA_SUBJECT = "android.intent.extra.SUBJECT";
    public static final String EXTRA_SUSPENDED_PACKAGE_EXTRAS = "android.intent.extra.SUSPENDED_PACKAGE_EXTRAS";

    @SystemApi
    @Deprecated
    public static final String EXTRA_SYSTEM_ID = "systemId";
    public static final String EXTRA_TASK_ID = "android.intent.extra.TASK_ID";
    public static final String EXTRA_TEMPLATE = "android.intent.extra.TEMPLATE";
    public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
    public static final String EXTRA_THERMAL_STATE = "android.intent.extra.THERMAL_STATE";
    public static final int EXTRA_THERMAL_STATE_EXCEEDED = 2;
    public static final int EXTRA_THERMAL_STATE_NORMAL = 0;
    public static final int EXTRA_THERMAL_STATE_WARNING = 1;
    public static final String EXTRA_TIME_PREF_24_HOUR_FORMAT = "android.intent.extra.TIME_PREF_24_HOUR_FORMAT";
    public static final int EXTRA_TIME_PREF_VALUE_USE_12_HOUR = 0;
    public static final int EXTRA_TIME_PREF_VALUE_USE_24_HOUR = 1;
    public static final int EXTRA_TIME_PREF_VALUE_USE_LOCALE_DEFAULT = 2;
    public static final String EXTRA_TITLE = "android.intent.extra.TITLE";
    public static final String EXTRA_UID = "android.intent.extra.UID";
    public static final String EXTRA_UNINSTALL_ALL_USERS = "android.intent.extra.UNINSTALL_ALL_USERS";

    @SystemApi
    public static final String EXTRA_UNKNOWN_INSTANT_APP = "android.intent.extra.UNKNOWN_INSTANT_APP";
    public static final String EXTRA_USER = "android.intent.extra.USER";
    public static final String EXTRA_USER_HANDLE = "android.intent.extra.user_handle";
    public static final String EXTRA_USER_ID = "android.intent.extra.USER_ID";
    public static final String EXTRA_USER_REQUESTED_SHUTDOWN = "android.intent.extra.USER_REQUESTED_SHUTDOWN";

    @SystemApi
    public static final String EXTRA_VERIFICATION_BUNDLE = "android.intent.extra.VERIFICATION_BUNDLE";

    @Deprecated
    public static final String EXTRA_VERSION_CODE = "android.intent.extra.VERSION_CODE";

    @SystemApi
    @Deprecated
    public static final String EXTRA_VOICE_RADIO_TECH = "radioTechnology";

    @SystemApi
    @Deprecated
    public static final String EXTRA_VOICE_REG_STATE = "voiceRegState";

    @SystemApi
    @Deprecated
    public static final String EXTRA_VOICE_ROAMING_TYPE = "voiceRoamingType";
    public static final String EXTRA_WIPE_ESIMS = "com.android.internal.intent.extra.WIPE_ESIMS";
    public static final String EXTRA_WIPE_EXTERNAL_STORAGE = "android.intent.extra.WIPE_EXTERNAL_STORAGE";
    public static final int FILL_IN_ACTION = 1;
    public static final int FILL_IN_CATEGORIES = 4;
    public static final int FILL_IN_CLIP_DATA = 128;
    public static final int FILL_IN_COMPONENT = 8;
    public static final int FILL_IN_DATA = 2;
    public static final int FILL_IN_PACKAGE = 16;
    public static final int FILL_IN_SELECTOR = 64;
    public static final int FILL_IN_SOURCE_BOUNDS = 32;
    public static final int FLAG_ACTIVITY_BROUGHT_TO_FRONT = 4194304;
    public static final int FLAG_ACTIVITY_CLEAR_TASK = 32768;
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 67108864;

    @Deprecated
    public static final int FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET = 524288;
    public static final int FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS = 8388608;
    public static final int FLAG_ACTIVITY_FORWARD_RESULT = 33554432;
    public static final int FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 1048576;
    public static final int FLAG_ACTIVITY_LAUNCH_ADJACENT = 4096;
    public static final int FLAG_ACTIVITY_MATCH_EXTERNAL = 2048;
    public static final int FLAG_ACTIVITY_MULTIPLE_TASK = 134217728;
    public static final int FLAG_ACTIVITY_NEW_DOCUMENT = 524288;
    public static final int FLAG_ACTIVITY_NEW_TASK = 268435456;
    public static final int FLAG_ACTIVITY_NO_ANIMATION = 65536;
    public static final int FLAG_ACTIVITY_NO_HISTORY = 1073741824;
    public static final int FLAG_ACTIVITY_NO_USER_ACTION = 262144;
    public static final int FLAG_ACTIVITY_PREVIOUS_IS_TOP = 16777216;
    public static final int FLAG_ACTIVITY_REORDER_TO_FRONT = 131072;
    public static final int FLAG_ACTIVITY_RESET_TASK_IF_NEEDED = 2097152;
    public static final int FLAG_ACTIVITY_RETAIN_IN_RECENTS = 8192;
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 536870912;
    public static final int FLAG_ACTIVITY_TASK_ON_HOME = 16384;
    public static final int FLAG_DEBUG_LOG_RESOLUTION = 8;
    public static final int FLAG_DEBUG_TRIAGED_MISSING = 256;
    public static final int FLAG_EXCLUDE_STOPPED_PACKAGES = 16;
    public static final int FLAG_FROM_BACKGROUND = 4;
    public static final int FLAG_GRANT_PERSISTABLE_URI_PERMISSION = 64;
    public static final int FLAG_GRANT_PREFIX_URI_PERMISSION = 128;
    public static final int FLAG_GRANT_READ_URI_PERMISSION = 1;
    public static final int FLAG_GRANT_WRITE_URI_PERMISSION = 2;
    public static final int FLAG_IGNORE_EPHEMERAL = 512;
    public static final int FLAG_INCLUDE_STOPPED_PACKAGES = 32;
    public static final int FLAG_RECEIVER_BOOT_UPGRADE = 33554432;
    public static final int FLAG_RECEIVER_EXCLUDE_BACKGROUND = 8388608;
    public static final int FLAG_RECEIVER_FOREGROUND = 268435456;
    public static final int FLAG_RECEIVER_FROM_SHELL = 4194304;
    public static final int FLAG_RECEIVER_INCLUDE_BACKGROUND = 16777216;
    public static final int FLAG_RECEIVER_NO_ABORT = 134217728;
    public static final int FLAG_RECEIVER_REGISTERED_ONLY = 1073741824;
    public static final int FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT = 67108864;
    public static final int FLAG_RECEIVER_REPLACE_PENDING = 536870912;
    public static final int FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS = 2097152;
    public static final int IMMUTABLE_FLAGS = 195;
    public static final String METADATA_DOCK_HOME = "android.dock_home";
    public static final String METADATA_SETUP_VERSION = "android.SETUP_VERSION";
    private static final String TAG_CATEGORIES = "categories";
    private static final String TAG_EXTRA = "extra";
    public static final int URI_ALLOW_UNSAFE = 4;
    public static final int URI_ANDROID_APP_SCHEME = 2;
    public static final int URI_INTENT_SCHEME = 1;
    private String mAction;
    private ArraySet<String> mCategories;
    private ClipData mClipData;
    private ComponentName mComponent;
    private int mContentUserHint;
    private Uri mData;
    private Bundle mExtras;
    private int mFlags;
    private String mLaunchToken;
    private String mPackage;
    private Intent mSelector;
    private Rect mSourceBounds;
    private String mType;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AccessUriMode {
    }

    public interface CommandOptionHandler {
        boolean handleOption(String str, ShellCommand shellCommand);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface CopyMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FillInFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface GrantUriMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface MutableFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface UriFlags {
    }

    public static class ShortcutIconResource implements Parcelable {
        public static final Parcelable.Creator<ShortcutIconResource> CREATOR = new Parcelable.Creator<ShortcutIconResource>() {
            @Override
            public ShortcutIconResource createFromParcel(Parcel parcel) {
                ShortcutIconResource shortcutIconResource = new ShortcutIconResource();
                shortcutIconResource.packageName = parcel.readString();
                shortcutIconResource.resourceName = parcel.readString();
                return shortcutIconResource;
            }

            @Override
            public ShortcutIconResource[] newArray(int i) {
                return new ShortcutIconResource[i];
            }
        };
        public String packageName;
        public String resourceName;

        public static ShortcutIconResource fromContext(Context context, int i) {
            ShortcutIconResource shortcutIconResource = new ShortcutIconResource();
            shortcutIconResource.packageName = context.getPackageName();
            shortcutIconResource.resourceName = context.getResources().getResourceName(i);
            return shortcutIconResource;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.packageName);
            parcel.writeString(this.resourceName);
        }

        public String toString() {
            return this.resourceName;
        }
    }

    public static Intent createChooser(Intent intent, CharSequence charSequence) {
        return createChooser(intent, charSequence, null);
    }

    public static Intent createChooser(Intent intent, CharSequence charSequence, IntentSender intentSender) {
        ClipData clipData;
        String[] strArr;
        Intent intent2 = new Intent(ACTION_CHOOSER);
        intent2.putExtra(EXTRA_INTENT, intent);
        if (charSequence != null) {
            intent2.putExtra(EXTRA_TITLE, charSequence);
        }
        if (intentSender != null) {
            intent2.putExtra(EXTRA_CHOSEN_COMPONENT_INTENT_SENDER, intentSender);
        }
        int flags = intent.getFlags() & 195;
        if (flags != 0) {
            ClipData clipData2 = intent.getClipData();
            if (clipData2 == null && intent.getData() != null) {
                ClipData.Item item = new ClipData.Item(intent.getData());
                if (intent.getType() != null) {
                    strArr = new String[]{intent.getType()};
                } else {
                    strArr = new String[0];
                }
                clipData = new ClipData(null, strArr, item);
            } else {
                clipData = clipData2;
            }
            if (clipData != null) {
                intent2.setClipData(clipData);
                intent2.addFlags(flags);
            }
        }
        return intent2;
    }

    public static boolean isAccessUriMode(int i) {
        return (i & 3) != 0;
    }

    public Intent() {
        this.mContentUserHint = -2;
    }

    public Intent(Intent intent) {
        this(intent, 0);
    }

    private Intent(Intent intent, int i) {
        this.mContentUserHint = -2;
        this.mAction = intent.mAction;
        this.mData = intent.mData;
        this.mType = intent.mType;
        this.mPackage = intent.mPackage;
        this.mComponent = intent.mComponent;
        if (intent.mCategories != null) {
            this.mCategories = new ArraySet<>((ArraySet) intent.mCategories);
        }
        if (i != 1) {
            this.mFlags = intent.mFlags;
            this.mContentUserHint = intent.mContentUserHint;
            this.mLaunchToken = intent.mLaunchToken;
            if (intent.mSourceBounds != null) {
                this.mSourceBounds = new Rect(intent.mSourceBounds);
            }
            if (intent.mSelector != null) {
                this.mSelector = new Intent(intent.mSelector);
            }
            if (i != 2) {
                if (intent.mExtras != null) {
                    this.mExtras = new Bundle(intent.mExtras);
                }
                if (intent.mClipData != null) {
                    this.mClipData = new ClipData(intent.mClipData);
                    return;
                }
                return;
            }
            if (intent.mExtras != null && !intent.mExtras.maybeIsEmpty()) {
                this.mExtras = Bundle.STRIPPED;
            }
        }
    }

    public Object clone() {
        return new Intent(this);
    }

    public Intent cloneFilter() {
        return new Intent(this, 1);
    }

    public Intent(String str) {
        this.mContentUserHint = -2;
        setAction(str);
    }

    public Intent(String str, Uri uri) {
        this.mContentUserHint = -2;
        setAction(str);
        this.mData = uri;
    }

    public Intent(Context context, Class<?> cls) {
        this.mContentUserHint = -2;
        this.mComponent = new ComponentName(context, cls);
    }

    public Intent(String str, Uri uri, Context context, Class<?> cls) {
        this.mContentUserHint = -2;
        setAction(str);
        this.mData = uri;
        this.mComponent = new ComponentName(context, cls);
    }

    public static Intent makeMainActivity(ComponentName componentName) {
        Intent intent = new Intent(ACTION_MAIN);
        intent.setComponent(componentName);
        intent.addCategory(CATEGORY_LAUNCHER);
        return intent;
    }

    public static Intent makeMainSelectorActivity(String str, String str2) {
        Intent intent = new Intent(ACTION_MAIN);
        intent.addCategory(CATEGORY_LAUNCHER);
        Intent intent2 = new Intent();
        intent2.setAction(str);
        intent2.addCategory(str2);
        intent.setSelector(intent2);
        return intent;
    }

    public static Intent makeRestartActivityTask(ComponentName componentName) {
        Intent intentMakeMainActivity = makeMainActivity(componentName);
        intentMakeMainActivity.addFlags(268468224);
        return intentMakeMainActivity;
    }

    @Deprecated
    public static Intent getIntent(String str) throws URISyntaxException {
        return parseUri(str, 0);
    }

    public static android.content.Intent parseUri(java.lang.String r17, int r18) throws java.net.URISyntaxException {
        throw new UnsupportedOperationException("Method not decompiled: android.content.Intent.parseUri(java.lang.String, int):android.content.Intent");
    }

    public static Intent getIntentOld(String str) throws URISyntaxException {
        return getIntentOld(str, 0);
    }

    private static Intent getIntentOld(String str, int i) throws URISyntaxException {
        boolean z;
        int iLastIndexOf = str.lastIndexOf(35);
        if (iLastIndexOf >= 0) {
            String str2 = null;
            int i2 = iLastIndexOf + 1;
            if (str.regionMatches(i2, "action(", 0, 7)) {
                int i3 = i2 + 7;
                int iIndexOf = str.indexOf(41, i3);
                String strSubstring = str.substring(i3, iIndexOf);
                z = true;
                i2 = iIndexOf + 1;
                str2 = strSubstring;
            } else {
                z = false;
            }
            Intent intent = new Intent(str2);
            if (str.regionMatches(i2, "categories(", 0, 11)) {
                int i4 = i2 + 11;
                int iIndexOf2 = str.indexOf(41, i4);
                while (i4 < iIndexOf2) {
                    int iIndexOf3 = str.indexOf(33, i4);
                    if (iIndexOf3 < 0 || iIndexOf3 > iIndexOf2) {
                        iIndexOf3 = iIndexOf2;
                    }
                    if (i4 < iIndexOf3) {
                        intent.addCategory(str.substring(i4, iIndexOf3));
                    }
                    i4 = iIndexOf3 + 1;
                }
                i2 = iIndexOf2 + 1;
                z = true;
            }
            if (str.regionMatches(i2, "type(", 0, 5)) {
                int i5 = i2 + 5;
                int iIndexOf4 = str.indexOf(41, i5);
                intent.mType = str.substring(i5, iIndexOf4);
                i2 = iIndexOf4 + 1;
                z = true;
            }
            if (str.regionMatches(i2, "launchFlags(", 0, 12)) {
                int i6 = i2 + 12;
                int iIndexOf5 = str.indexOf(41, i6);
                intent.mFlags = Integer.decode(str.substring(i6, iIndexOf5)).intValue();
                if ((i & 4) == 0) {
                    intent.mFlags &= -196;
                }
                i2 = iIndexOf5 + 1;
                z = true;
            }
            if (str.regionMatches(i2, "component(", 0, 10)) {
                int i7 = i2 + 10;
                int iIndexOf6 = str.indexOf(41, i7);
                int iIndexOf7 = str.indexOf(33, i7);
                if (iIndexOf7 >= 0 && iIndexOf7 < iIndexOf6) {
                    intent.mComponent = new ComponentName(str.substring(i7, iIndexOf7), str.substring(iIndexOf7 + 1, iIndexOf6));
                }
                i2 = iIndexOf6 + 1;
                z = true;
            }
            if (str.regionMatches(i2, "extras(", 0, 7)) {
                int i8 = i2 + 7;
                int iIndexOf8 = str.indexOf(41, i8);
                if (iIndexOf8 == -1) {
                    throw new URISyntaxException(str, "EXTRA missing trailing ')'", i8);
                }
                while (i8 < iIndexOf8) {
                    int iIndexOf9 = str.indexOf(61, i8);
                    int i9 = i8 + 1;
                    if (iIndexOf9 <= i9 || i8 >= iIndexOf8) {
                        throw new URISyntaxException(str, "EXTRA missing '='", i8);
                    }
                    char cCharAt = str.charAt(i8);
                    String strSubstring2 = str.substring(i9, iIndexOf9);
                    int i10 = iIndexOf9 + 1;
                    int iIndexOf10 = str.indexOf(33, i10);
                    if (iIndexOf10 == -1 || iIndexOf10 >= iIndexOf8) {
                        iIndexOf10 = iIndexOf8;
                    }
                    if (i10 >= iIndexOf10) {
                        throw new URISyntaxException(str, "EXTRA missing '!'", i10);
                    }
                    String strSubstring3 = str.substring(i10, iIndexOf10);
                    if (intent.mExtras == null) {
                        intent.mExtras = new Bundle();
                    }
                    if (cCharAt == 'B') {
                        intent.mExtras.putBoolean(strSubstring2, Boolean.parseBoolean(strSubstring3));
                    } else if (cCharAt == 'S') {
                        intent.mExtras.putString(strSubstring2, Uri.decode(strSubstring3));
                    } else if (cCharAt == 'f') {
                        intent.mExtras.putFloat(strSubstring2, Float.parseFloat(strSubstring3));
                    } else if (cCharAt == 'i') {
                        intent.mExtras.putInt(strSubstring2, Integer.parseInt(strSubstring3));
                    } else if (cCharAt == 'l') {
                        intent.mExtras.putLong(strSubstring2, Long.parseLong(strSubstring3));
                    } else {
                        if (cCharAt != 's') {
                            switch (cCharAt) {
                                case 'b':
                                    intent.mExtras.putByte(strSubstring2, Byte.parseByte(strSubstring3));
                                    break;
                                case 'c':
                                    intent.mExtras.putChar(strSubstring2, Uri.decode(strSubstring3).charAt(0));
                                    break;
                                case 'd':
                                    try {
                                        intent.mExtras.putDouble(strSubstring2, Double.parseDouble(strSubstring3));
                                    } catch (NumberFormatException e) {
                                        throw new URISyntaxException(str, "EXTRA value can't be parsed", iIndexOf10);
                                    }
                                    break;
                                default:
                                    throw new URISyntaxException(str, "EXTRA has unknown type", iIndexOf10);
                            }
                            throw new URISyntaxException(str, "EXTRA value can't be parsed", iIndexOf10);
                        }
                        intent.mExtras.putShort(strSubstring2, Short.parseShort(strSubstring3));
                    }
                    char cCharAt2 = str.charAt(iIndexOf10);
                    if (cCharAt2 != ')') {
                        if (cCharAt2 != '!') {
                            throw new URISyntaxException(str, "EXTRA missing '!'", iIndexOf10);
                        }
                        i8 = iIndexOf10 + 1;
                    } else {
                        z = true;
                    }
                }
                z = true;
            }
            if (z) {
                intent.mData = Uri.parse(str.substring(0, iLastIndexOf));
            } else {
                intent.mData = Uri.parse(str);
            }
            if (intent.mAction == null) {
                intent.mAction = "android.intent.action.VIEW";
                return intent;
            }
            return intent;
        }
        return new Intent("android.intent.action.VIEW", Uri.parse(str));
    }

    public static Intent parseCommandArgs(ShellCommand shellCommand, CommandOptionHandler commandOptionHandler) throws URISyntaxException {
        Intent intent;
        Intent uri;
        boolean z;
        Intent intent2 = new Intent();
        Intent intent3 = intent2;
        Uri uri2 = null;
        String str = null;
        boolean z2 = false;
        while (true) {
            String nextOption = shellCommand.getNextOption();
            byte b = 7;
            if (nextOption == null) {
                boolean z3 = true;
                intent3.setDataAndType(uri2, str);
                boolean z4 = intent3 != intent2;
                if (z4) {
                    intent2.setSelector(intent3);
                } else {
                    intent2 = intent3;
                }
                String nextArg = shellCommand.getNextArg();
                if (nextArg == null) {
                    if (z4) {
                        uri = new Intent(ACTION_MAIN);
                        uri.addCategory(CATEGORY_LAUNCHER);
                    } else {
                        uri = null;
                    }
                } else if (nextArg.indexOf(58) >= 0) {
                    uri = parseUri(nextArg, 7);
                } else {
                    if (nextArg.indexOf(47) >= 0) {
                        intent = new Intent(ACTION_MAIN);
                        intent.addCategory(CATEGORY_LAUNCHER);
                        intent.setComponent(ComponentName.unflattenFromString(nextArg));
                    } else {
                        intent = new Intent(ACTION_MAIN);
                        intent.addCategory(CATEGORY_LAUNCHER);
                        intent.setPackage(nextArg);
                    }
                    uri = intent;
                }
                if (uri != null) {
                    Bundle extras = intent2.getExtras();
                    Bundle bundle = (Bundle) null;
                    intent2.replaceExtras(bundle);
                    Bundle extras2 = uri.getExtras();
                    uri.replaceExtras(bundle);
                    if (intent2.getAction() != null && uri.getCategories() != null) {
                        Iterator it = new HashSet(uri.getCategories()).iterator();
                        while (it.hasNext()) {
                            uri.removeCategory((String) it.next());
                        }
                    }
                    intent2.fillIn(uri, 72);
                    if (extras != null) {
                        if (extras2 != null) {
                            extras2.putAll(extras);
                            extras = extras2;
                        }
                        intent2.replaceExtras(extras);
                    } else {
                        extras = extras2;
                        intent2.replaceExtras(extras);
                    }
                } else {
                    z3 = z2;
                }
                if (z3) {
                    return intent2;
                }
                throw new IllegalArgumentException("No intent supplied");
            }
            int iHashCode = nextOption.hashCode();
            switch (iHashCode) {
                case 1494:
                    b = nextOption.equals("-c") ? (byte) 3 : (byte) -1;
                    break;
                case 1495:
                    if (nextOption.equals("-d")) {
                        b = 1;
                        break;
                    }
                    break;
                case 1496:
                    if (nextOption.equals("-e")) {
                        b = 4;
                        break;
                    }
                    break;
                case 1497:
                    if (nextOption.equals("-f")) {
                        b = 23;
                        break;
                    }
                    break;
                default:
                    switch (iHashCode) {
                        case -2147394086:
                            if (nextOption.equals("--grant-prefix-uri-permission")) {
                                b = GsmAlphabet.GSM_EXTENDED_ESCAPE;
                                break;
                            }
                            break;
                        case -2118172637:
                            if (nextOption.equals("--activity-task-on-home")) {
                                b = 45;
                                break;
                            }
                            break;
                        case -1630559130:
                            if (nextOption.equals("--activity-no-history")) {
                                b = 38;
                                break;
                            }
                            break;
                        case -1252939549:
                            if (nextOption.equals("--activity-clear-task")) {
                                b = 44;
                                break;
                            }
                            break;
                        case -1069446353:
                            if (nextOption.equals("--debug-log-resolution")) {
                                b = 30;
                                break;
                            }
                            break;
                        case -848214457:
                            if (nextOption.equals("--activity-reorder-to-front")) {
                                b = 41;
                                break;
                            }
                            break;
                        case -833172539:
                            if (nextOption.equals("--activity-brought-to-front")) {
                                b = 31;
                                break;
                            }
                            break;
                        case -792169302:
                            if (nextOption.equals("--activity-previous-is-top")) {
                                b = 40;
                                break;
                            }
                            break;
                        case -780160399:
                            if (nextOption.equals("--receiver-include-background")) {
                                b = 51;
                                break;
                            }
                            break;
                        case 1492:
                            if (nextOption.equals("-a")) {
                                b = 0;
                                break;
                            }
                            break;
                        case 1505:
                            if (nextOption.equals("-n")) {
                                b = 21;
                                break;
                            }
                            break;
                        case ImsReasonInfo.CODE_RADIO_LINK_LOST:
                            if (nextOption.equals("-p")) {
                                b = 22;
                                break;
                            }
                            break;
                        case ImsReasonInfo.CODE_RADIO_RELEASE_ABNORMAL:
                            if (nextOption.equals("-t")) {
                                b = 2;
                                break;
                            }
                            break;
                        case 1387073:
                            if (nextOption.equals("--ef")) {
                                b = MidiConstants.STATUS_CHANNEL_MASK;
                                break;
                            }
                            break;
                        case 1387076:
                            if (!nextOption.equals("--ei")) {
                            }
                            break;
                        case 1387079:
                            if (nextOption.equals("--el")) {
                                b = 12;
                                break;
                            }
                            break;
                        case 1387086:
                            if (nextOption.equals("--es")) {
                                b = 5;
                                break;
                            }
                            break;
                        case 1387088:
                            if (nextOption.equals("--eu")) {
                                b = 8;
                                break;
                            }
                            break;
                        case 1387093:
                            if (nextOption.equals("--ez")) {
                                b = 20;
                                break;
                            }
                            break;
                        case 42999280:
                            if (nextOption.equals("--ecn")) {
                                b = 9;
                                break;
                            }
                            break;
                        case 42999360:
                            if (nextOption.equals("--efa")) {
                                b = 16;
                                break;
                            }
                            break;
                        case 42999453:
                            if (nextOption.equals("--eia")) {
                                b = 10;
                                break;
                            }
                            break;
                        case 42999546:
                            if (nextOption.equals("--ela")) {
                                b = 13;
                                break;
                            }
                            break;
                        case 42999763:
                            if (nextOption.equals("--esa")) {
                                b = 18;
                                break;
                            }
                            break;
                        case 42999776:
                            if (nextOption.equals("--esn")) {
                                b = 6;
                                break;
                            }
                            break;
                        case 69120454:
                            if (nextOption.equals("--activity-exclude-from-recents")) {
                                b = 34;
                                break;
                            }
                            break;
                        case 88747734:
                            if (nextOption.equals("--activity-no-animation")) {
                                b = 37;
                                break;
                            }
                            break;
                        case 190913209:
                            if (nextOption.equals("--activity-reset-task-if-needed")) {
                                b = 42;
                                break;
                            }
                            break;
                        case 236677687:
                            if (nextOption.equals("--activity-clear-top")) {
                                b = 32;
                                break;
                            }
                            break;
                        case 429439306:
                            if (nextOption.equals("--activity-no-user-action")) {
                                b = 39;
                                break;
                            }
                            break;
                        case 436286937:
                            if (nextOption.equals("--receiver-registered-only")) {
                                b = 47;
                                break;
                            }
                            break;
                        case 438531630:
                            if (nextOption.equals("--activity-single-top")) {
                                b = 43;
                                break;
                            }
                            break;
                        case 527014976:
                            if (nextOption.equals("--grant-persistable-uri-permission")) {
                                b = 26;
                                break;
                            }
                            break;
                        case 580418080:
                            if (nextOption.equals("--exclude-stopped-packages")) {
                                b = 28;
                                break;
                            }
                            break;
                        case 749648146:
                            if (nextOption.equals("--include-stopped-packages")) {
                                b = 29;
                                break;
                            }
                            break;
                        case 775126336:
                            if (nextOption.equals("--receiver-replace-pending")) {
                                b = 48;
                                break;
                            }
                            break;
                        case 1110195121:
                            if (nextOption.equals("--activity-match-external")) {
                                b = 46;
                                break;
                            }
                            break;
                        case 1207327103:
                            if (nextOption.equals("--selector")) {
                                b = 52;
                                break;
                            }
                            break;
                        case 1332980268:
                            if (nextOption.equals("--efal")) {
                                b = 17;
                                break;
                            }
                            break;
                        case 1332983151:
                            if (nextOption.equals("--eial")) {
                                b = 11;
                                break;
                            }
                            break;
                        case 1332986034:
                            if (nextOption.equals("--elal")) {
                                b = BluetoothHidDevice.ERROR_RSP_UNKNOWN;
                                break;
                            }
                            break;
                        case 1332992761:
                            if (nextOption.equals("--esal")) {
                                b = 19;
                                break;
                            }
                            break;
                        case 1353919836:
                            if (nextOption.equals("--activity-clear-when-task-reset")) {
                                b = 33;
                                break;
                            }
                            break;
                        case 1398403374:
                            if (nextOption.equals("--activity-launched-from-history")) {
                                b = 35;
                                break;
                            }
                            break;
                        case 1453225122:
                            if (nextOption.equals("--receiver-no-abort")) {
                                b = 50;
                                break;
                            }
                            break;
                        case 1652786753:
                            if (nextOption.equals("--receiver-foreground")) {
                                b = 49;
                                break;
                            }
                            break;
                        case 1742380566:
                            if (nextOption.equals("--grant-read-uri-permission")) {
                                b = 24;
                                break;
                            }
                            break;
                        case 1765369476:
                            if (nextOption.equals("--activity-multiple-task")) {
                                b = 36;
                                break;
                            }
                            break;
                        case 1816558127:
                            if (nextOption.equals("--grant-write-uri-permission")) {
                                b = 25;
                                break;
                            }
                            break;
                    }
                    break;
            }
            switch (b) {
                case 0:
                    z = true;
                    intent3.setAction(shellCommand.getNextArgRequired());
                    if (intent3 == intent2) {
                        z2 = z;
                    }
                    break;
                case 1:
                    Uri uri3 = Uri.parse(shellCommand.getNextArgRequired());
                    if (intent3 == intent2) {
                        z2 = true;
                    }
                    uri2 = uri3;
                    break;
                case 2:
                    String nextArgRequired = shellCommand.getNextArgRequired();
                    if (intent3 == intent2) {
                        z2 = true;
                    }
                    str = nextArgRequired;
                    break;
                case 3:
                    z = true;
                    intent3.addCategory(shellCommand.getNextArgRequired());
                    if (intent3 == intent2) {
                        z2 = z;
                    }
                    break;
                case 4:
                case 5:
                    intent3.putExtra(shellCommand.getNextArgRequired(), shellCommand.getNextArgRequired());
                    break;
                case 6:
                    intent3.putExtra(shellCommand.getNextArgRequired(), (String) null);
                    break;
                case 7:
                    intent3.putExtra(shellCommand.getNextArgRequired(), Integer.decode(shellCommand.getNextArgRequired()));
                    break;
                case 8:
                    intent3.putExtra(shellCommand.getNextArgRequired(), Uri.parse(shellCommand.getNextArgRequired()));
                    break;
                case 9:
                    String nextArgRequired2 = shellCommand.getNextArgRequired();
                    String nextArgRequired3 = shellCommand.getNextArgRequired();
                    ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(nextArgRequired3);
                    if (componentNameUnflattenFromString == null) {
                        throw new IllegalArgumentException("Bad component name: " + nextArgRequired3);
                    }
                    intent3.putExtra(nextArgRequired2, componentNameUnflattenFromString);
                    break;
                    break;
                case 10:
                    String nextArgRequired4 = shellCommand.getNextArgRequired();
                    String[] strArrSplit = shellCommand.getNextArgRequired().split(",");
                    int[] iArr = new int[strArrSplit.length];
                    for (int i = 0; i < strArrSplit.length; i++) {
                        iArr[i] = Integer.decode(strArrSplit[i]).intValue();
                    }
                    intent3.putExtra(nextArgRequired4, iArr);
                    break;
                case 11:
                    String nextArgRequired5 = shellCommand.getNextArgRequired();
                    String[] strArrSplit2 = shellCommand.getNextArgRequired().split(",");
                    ArrayList arrayList = new ArrayList(strArrSplit2.length);
                    for (String str2 : strArrSplit2) {
                        arrayList.add(Integer.decode(str2));
                    }
                    intent3.putExtra(nextArgRequired5, arrayList);
                    break;
                case 12:
                    intent3.putExtra(shellCommand.getNextArgRequired(), Long.valueOf(shellCommand.getNextArgRequired()));
                    break;
                case 13:
                    z = true;
                    String nextArgRequired6 = shellCommand.getNextArgRequired();
                    String[] strArrSplit3 = shellCommand.getNextArgRequired().split(",");
                    long[] jArr = new long[strArrSplit3.length];
                    for (int i2 = 0; i2 < strArrSplit3.length; i2++) {
                        jArr[i2] = Long.valueOf(strArrSplit3[i2]).longValue();
                    }
                    intent3.putExtra(nextArgRequired6, jArr);
                    z2 = z;
                    break;
                case 14:
                    z = true;
                    String nextArgRequired7 = shellCommand.getNextArgRequired();
                    String[] strArrSplit4 = shellCommand.getNextArgRequired().split(",");
                    ArrayList arrayList2 = new ArrayList(strArrSplit4.length);
                    for (String str3 : strArrSplit4) {
                        arrayList2.add(Long.valueOf(str3));
                    }
                    intent3.putExtra(nextArgRequired7, arrayList2);
                    z2 = z;
                    break;
                case 15:
                    z = true;
                    intent3.putExtra(shellCommand.getNextArgRequired(), Float.valueOf(shellCommand.getNextArgRequired()));
                    z2 = z;
                    break;
                case 16:
                    z = true;
                    String nextArgRequired8 = shellCommand.getNextArgRequired();
                    String[] strArrSplit5 = shellCommand.getNextArgRequired().split(",");
                    float[] fArr = new float[strArrSplit5.length];
                    for (int i3 = 0; i3 < strArrSplit5.length; i3++) {
                        fArr[i3] = Float.valueOf(strArrSplit5[i3]).floatValue();
                    }
                    intent3.putExtra(nextArgRequired8, fArr);
                    z2 = z;
                    break;
                case 17:
                    z = true;
                    String nextArgRequired9 = shellCommand.getNextArgRequired();
                    String[] strArrSplit6 = shellCommand.getNextArgRequired().split(",");
                    ArrayList arrayList3 = new ArrayList(strArrSplit6.length);
                    for (String str4 : strArrSplit6) {
                        arrayList3.add(Float.valueOf(str4));
                    }
                    intent3.putExtra(nextArgRequired9, arrayList3);
                    z2 = z;
                    break;
                case 18:
                    z = true;
                    intent3.putExtra(shellCommand.getNextArgRequired(), shellCommand.getNextArgRequired().split("(?<!\\\\),"));
                    z2 = z;
                    break;
                case 19:
                    z = true;
                    String nextArgRequired10 = shellCommand.getNextArgRequired();
                    String[] strArrSplit7 = shellCommand.getNextArgRequired().split("(?<!\\\\),");
                    ArrayList arrayList4 = new ArrayList(strArrSplit7.length);
                    for (String str5 : strArrSplit7) {
                        arrayList4.add(str5);
                    }
                    intent3.putExtra(nextArgRequired10, arrayList4);
                    z2 = z;
                    break;
                case 20:
                    boolean z5 = true;
                    String nextArgRequired11 = shellCommand.getNextArgRequired();
                    String lowerCase = shellCommand.getNextArgRequired().toLowerCase();
                    if (!"true".equals(lowerCase) && !"t".equals(lowerCase)) {
                        if ("false".equals(lowerCase) || FullBackup.FILES_TREE_TOKEN.equals(lowerCase)) {
                            z5 = false;
                        } else {
                            try {
                                if (Integer.decode(lowerCase).intValue() == 0) {
                                    z5 = false;
                                }
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid boolean value: " + lowerCase);
                            }
                        }
                    }
                    intent3.putExtra(nextArgRequired11, z5);
                    break;
                case 21:
                    z = true;
                    String nextArgRequired12 = shellCommand.getNextArgRequired();
                    ComponentName componentNameUnflattenFromString2 = ComponentName.unflattenFromString(nextArgRequired12);
                    if (componentNameUnflattenFromString2 == null) {
                        throw new IllegalArgumentException("Bad component name: " + nextArgRequired12);
                    }
                    intent3.setComponent(componentNameUnflattenFromString2);
                    if (intent3 != intent2) {
                        z = z2;
                    }
                    z2 = z;
                    break;
                    break;
                case 22:
                    z = true;
                    intent3.setPackage(shellCommand.getNextArgRequired());
                    if (intent3 != intent2) {
                        z = z2;
                    }
                    z2 = z;
                    break;
                case 23:
                    intent3.setFlags(Integer.decode(shellCommand.getNextArgRequired()).intValue());
                    break;
                case 24:
                    intent3.addFlags(1);
                    break;
                case 25:
                    intent3.addFlags(2);
                    break;
                case 26:
                    intent3.addFlags(64);
                    break;
                case 27:
                    intent3.addFlags(128);
                    break;
                case 28:
                    intent3.addFlags(16);
                    break;
                case 29:
                    intent3.addFlags(32);
                    break;
                case 30:
                    intent3.addFlags(8);
                    break;
                case 31:
                    intent3.addFlags(4194304);
                    break;
                case 32:
                    intent3.addFlags(67108864);
                    break;
                case 33:
                    intent3.addFlags(524288);
                    break;
                case 34:
                    intent3.addFlags(8388608);
                    break;
                case 35:
                    intent3.addFlags(1048576);
                    break;
                case 36:
                    intent3.addFlags(134217728);
                    break;
                case 37:
                    intent3.addFlags(65536);
                    break;
                case 38:
                    intent3.addFlags(1073741824);
                    break;
                case 39:
                    intent3.addFlags(262144);
                    break;
                case 40:
                    intent3.addFlags(16777216);
                    break;
                case 41:
                    intent3.addFlags(131072);
                    break;
                case 42:
                    intent3.addFlags(2097152);
                    break;
                case 43:
                    intent3.addFlags(536870912);
                    break;
                case 44:
                    intent3.addFlags(32768);
                    break;
                case 45:
                    intent3.addFlags(16384);
                    break;
                case 46:
                    intent3.addFlags(2048);
                    break;
                case 47:
                    intent3.addFlags(1073741824);
                    break;
                case 48:
                    intent3.addFlags(536870912);
                    break;
                case 49:
                    intent3.addFlags(268435456);
                    break;
                case 50:
                    intent3.addFlags(134217728);
                    break;
                case 51:
                    intent3.addFlags(16777216);
                    break;
                case 52:
                    intent3.setDataAndType(uri2, str);
                    intent3 = new Intent();
                    break;
                default:
                    if (commandOptionHandler == null || !commandOptionHandler.handleOption(nextOption, shellCommand)) {
                    }
                    break;
            }
        }
    }

    public static void printIntentArgsHelp(PrintWriter printWriter, String str) {
        for (String str2 : new String[]{"<INTENT> specifications include these flags and arguments:", "    [-a <ACTION>] [-d <DATA_URI>] [-t <MIME_TYPE>]", "    [-c <CATEGORY> [-c <CATEGORY>] ...]", "    [-n <COMPONENT_NAME>]", "    [-e|--es <EXTRA_KEY> <EXTRA_STRING_VALUE> ...]", "    [--esn <EXTRA_KEY> ...]", "    [--ez <EXTRA_KEY> <EXTRA_BOOLEAN_VALUE> ...]", "    [--ei <EXTRA_KEY> <EXTRA_INT_VALUE> ...]", "    [--el <EXTRA_KEY> <EXTRA_LONG_VALUE> ...]", "    [--ef <EXTRA_KEY> <EXTRA_FLOAT_VALUE> ...]", "    [--eu <EXTRA_KEY> <EXTRA_URI_VALUE> ...]", "    [--ecn <EXTRA_KEY> <EXTRA_COMPONENT_NAME_VALUE>]", "    [--eia <EXTRA_KEY> <EXTRA_INT_VALUE>[,<EXTRA_INT_VALUE...]]", "        (mutiple extras passed as Integer[])", "    [--eial <EXTRA_KEY> <EXTRA_INT_VALUE>[,<EXTRA_INT_VALUE...]]", "        (mutiple extras passed as List<Integer>)", "    [--ela <EXTRA_KEY> <EXTRA_LONG_VALUE>[,<EXTRA_LONG_VALUE...]]", "        (mutiple extras passed as Long[])", "    [--elal <EXTRA_KEY> <EXTRA_LONG_VALUE>[,<EXTRA_LONG_VALUE...]]", "        (mutiple extras passed as List<Long>)", "    [--efa <EXTRA_KEY> <EXTRA_FLOAT_VALUE>[,<EXTRA_FLOAT_VALUE...]]", "        (mutiple extras passed as Float[])", "    [--efal <EXTRA_KEY> <EXTRA_FLOAT_VALUE>[,<EXTRA_FLOAT_VALUE...]]", "        (mutiple extras passed as List<Float>)", "    [--esa <EXTRA_KEY> <EXTRA_STRING_VALUE>[,<EXTRA_STRING_VALUE...]]", "        (mutiple extras passed as String[]; to embed a comma into a string,", "         escape it using \"\\,\")", "    [--esal <EXTRA_KEY> <EXTRA_STRING_VALUE>[,<EXTRA_STRING_VALUE...]]", "        (mutiple extras passed as List<String>; to embed a comma into a string,", "         escape it using \"\\,\")", "    [-f <FLAG>]", "    [--grant-read-uri-permission] [--grant-write-uri-permission]", "    [--grant-persistable-uri-permission] [--grant-prefix-uri-permission]", "    [--debug-log-resolution] [--exclude-stopped-packages]", "    [--include-stopped-packages]", "    [--activity-brought-to-front] [--activity-clear-top]", "    [--activity-clear-when-task-reset] [--activity-exclude-from-recents]", "    [--activity-launched-from-history] [--activity-multiple-task]", "    [--activity-no-animation] [--activity-no-history]", "    [--activity-no-user-action] [--activity-previous-is-top]", "    [--activity-reorder-to-front] [--activity-reset-task-if-needed]", "    [--activity-single-top] [--activity-clear-task]", "    [--activity-task-on-home] [--activity-match-external]", "    [--receiver-registered-only] [--receiver-replace-pending]", "    [--receiver-foreground] [--receiver-no-abort]", "    [--receiver-include-background]", "    [--selector]", "    [<URI> | <PACKAGE> | <COMPONENT>]"}) {
            printWriter.print(str);
            printWriter.println(str2);
        }
    }

    public String getAction() {
        return this.mAction;
    }

    public Uri getData() {
        return this.mData;
    }

    public String getDataString() {
        if (this.mData != null) {
            return this.mData.toString();
        }
        return null;
    }

    public String getScheme() {
        if (this.mData != null) {
            return this.mData.getScheme();
        }
        return null;
    }

    public String getType() {
        return this.mType;
    }

    public String resolveType(Context context) {
        return resolveType(context.getContentResolver());
    }

    public String resolveType(ContentResolver contentResolver) {
        if (this.mType != null) {
            return this.mType;
        }
        if (this.mData != null && "content".equals(this.mData.getScheme())) {
            return contentResolver.getType(this.mData);
        }
        return null;
    }

    public String resolveTypeIfNeeded(ContentResolver contentResolver) {
        if (this.mComponent != null) {
            return this.mType;
        }
        return resolveType(contentResolver);
    }

    public boolean hasCategory(String str) {
        return this.mCategories != null && this.mCategories.contains(str);
    }

    public Set<String> getCategories() {
        return this.mCategories;
    }

    public Intent getSelector() {
        return this.mSelector;
    }

    public ClipData getClipData() {
        return this.mClipData;
    }

    public int getContentUserHint() {
        return this.mContentUserHint;
    }

    public String getLaunchToken() {
        return this.mLaunchToken;
    }

    public void setLaunchToken(String str) {
        this.mLaunchToken = str;
    }

    public void setExtrasClassLoader(ClassLoader classLoader) {
        if (this.mExtras != null) {
            this.mExtras.setClassLoader(classLoader);
        }
    }

    public boolean hasExtra(String str) {
        return this.mExtras != null && this.mExtras.containsKey(str);
    }

    public boolean hasFileDescriptors() {
        return this.mExtras != null && this.mExtras.hasFileDescriptors();
    }

    public void setAllowFds(boolean z) {
        if (this.mExtras != null) {
            this.mExtras.setAllowFds(z);
        }
    }

    public void setDefusable(boolean z) {
        if (this.mExtras != null) {
            this.mExtras.setDefusable(z);
        }
    }

    @Deprecated
    public Object getExtra(String str) {
        return getExtra(str, null);
    }

    public boolean getBooleanExtra(String str, boolean z) {
        if (this.mExtras == null) {
            return z;
        }
        return this.mExtras.getBoolean(str, z);
    }

    public byte getByteExtra(String str, byte b) {
        if (this.mExtras == null) {
            return b;
        }
        return this.mExtras.getByte(str, b).byteValue();
    }

    public short getShortExtra(String str, short s) {
        if (this.mExtras == null) {
            return s;
        }
        return this.mExtras.getShort(str, s);
    }

    public char getCharExtra(String str, char c) {
        if (this.mExtras == null) {
            return c;
        }
        return this.mExtras.getChar(str, c);
    }

    public int getIntExtra(String str, int i) {
        if (this.mExtras == null) {
            return i;
        }
        return this.mExtras.getInt(str, i);
    }

    public long getLongExtra(String str, long j) {
        if (this.mExtras == null) {
            return j;
        }
        return this.mExtras.getLong(str, j);
    }

    public float getFloatExtra(String str, float f) {
        if (this.mExtras == null) {
            return f;
        }
        return this.mExtras.getFloat(str, f);
    }

    public double getDoubleExtra(String str, double d) {
        if (this.mExtras == null) {
            return d;
        }
        return this.mExtras.getDouble(str, d);
    }

    public String getStringExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getString(str);
    }

    public CharSequence getCharSequenceExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getCharSequence(str);
    }

    public <T extends Parcelable> T getParcelableExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return (T) this.mExtras.getParcelable(str);
    }

    public Parcelable[] getParcelableArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getParcelableArray(str);
    }

    public <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getParcelableArrayList(str);
    }

    public Serializable getSerializableExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getSerializable(str);
    }

    public ArrayList<Integer> getIntegerArrayListExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getIntegerArrayList(str);
    }

    public ArrayList<String> getStringArrayListExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getStringArrayList(str);
    }

    public ArrayList<CharSequence> getCharSequenceArrayListExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getCharSequenceArrayList(str);
    }

    public boolean[] getBooleanArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getBooleanArray(str);
    }

    public byte[] getByteArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getByteArray(str);
    }

    public short[] getShortArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getShortArray(str);
    }

    public char[] getCharArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getCharArray(str);
    }

    public int[] getIntArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getIntArray(str);
    }

    public long[] getLongArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getLongArray(str);
    }

    public float[] getFloatArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getFloatArray(str);
    }

    public double[] getDoubleArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getDoubleArray(str);
    }

    public String[] getStringArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getStringArray(str);
    }

    public CharSequence[] getCharSequenceArrayExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getCharSequenceArray(str);
    }

    public Bundle getBundleExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getBundle(str);
    }

    @Deprecated
    public IBinder getIBinderExtra(String str) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.getIBinder(str);
    }

    @Deprecated
    public Object getExtra(String str, Object obj) {
        Object obj2;
        return (this.mExtras == null || (obj2 = this.mExtras.get(str)) == null) ? obj : obj2;
    }

    public Bundle getExtras() {
        if (this.mExtras != null) {
            return new Bundle(this.mExtras);
        }
        return null;
    }

    public void removeUnsafeExtras() {
        if (this.mExtras != null) {
            this.mExtras = this.mExtras.filterValues();
        }
    }

    public boolean canStripForHistory() {
        return (this.mExtras != null && this.mExtras.isParcelled()) || this.mClipData != null;
    }

    public Intent maybeStripForHistory() {
        if (!canStripForHistory()) {
            return this;
        }
        return new Intent(this, 2);
    }

    public int getFlags() {
        return this.mFlags;
    }

    public boolean isExcludingStopped() {
        return (this.mFlags & 48) == 16;
    }

    public String getPackage() {
        return this.mPackage;
    }

    public ComponentName getComponent() {
        return this.mComponent;
    }

    public Rect getSourceBounds() {
        return this.mSourceBounds;
    }

    public ComponentName resolveActivity(PackageManager packageManager) {
        if (this.mComponent != null) {
            return this.mComponent;
        }
        ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(this, 65536);
        if (resolveInfoResolveActivity != null) {
            return new ComponentName(resolveInfoResolveActivity.activityInfo.applicationInfo.packageName, resolveInfoResolveActivity.activityInfo.name);
        }
        return null;
    }

    public ActivityInfo resolveActivityInfo(PackageManager packageManager, int i) {
        if (this.mComponent != null) {
            try {
                return packageManager.getActivityInfo(this.mComponent, i);
            } catch (PackageManager.NameNotFoundException e) {
            }
        } else {
            ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(this, i | 65536);
            if (resolveInfoResolveActivity != null) {
                return resolveInfoResolveActivity.activityInfo;
            }
        }
        return null;
    }

    public ComponentName resolveSystemService(PackageManager packageManager, int i) {
        if (this.mComponent != null) {
            return this.mComponent;
        }
        List<ResolveInfo> listQueryIntentServices = packageManager.queryIntentServices(this, i);
        ComponentName componentName = null;
        if (listQueryIntentServices == null) {
            return null;
        }
        for (int i2 = 0; i2 < listQueryIntentServices.size(); i2++) {
            ResolveInfo resolveInfo = listQueryIntentServices.get(i2);
            if ((resolveInfo.serviceInfo.applicationInfo.flags & 1) != 0) {
                ComponentName componentName2 = new ComponentName(resolveInfo.serviceInfo.applicationInfo.packageName, resolveInfo.serviceInfo.name);
                if (componentName == null) {
                    componentName = componentName2;
                } else {
                    throw new IllegalStateException("Multiple system services handle " + this + ": " + componentName + ", " + componentName2);
                }
            }
        }
        return componentName;
    }

    public Intent setAction(String str) {
        this.mAction = str != null ? str.intern() : null;
        return this;
    }

    public Intent setData(Uri uri) {
        this.mData = uri;
        this.mType = null;
        return this;
    }

    public Intent setDataAndNormalize(Uri uri) {
        return setData(uri.normalizeScheme());
    }

    public Intent setType(String str) {
        this.mData = null;
        this.mType = str;
        return this;
    }

    public Intent setTypeAndNormalize(String str) {
        return setType(normalizeMimeType(str));
    }

    public Intent setDataAndType(Uri uri, String str) {
        this.mData = uri;
        this.mType = str;
        return this;
    }

    public Intent setDataAndTypeAndNormalize(Uri uri, String str) {
        return setDataAndType(uri.normalizeScheme(), normalizeMimeType(str));
    }

    public Intent addCategory(String str) {
        if (this.mCategories == null) {
            this.mCategories = new ArraySet<>();
        }
        this.mCategories.add(str.intern());
        return this;
    }

    public void removeCategory(String str) {
        if (this.mCategories != null) {
            this.mCategories.remove(str);
            if (this.mCategories.size() == 0) {
                this.mCategories = null;
            }
        }
    }

    public void setSelector(Intent intent) {
        if (intent == this) {
            throw new IllegalArgumentException("Intent being set as a selector of itself");
        }
        if (intent != null && this.mPackage != null) {
            throw new IllegalArgumentException("Can't set selector when package name is already set");
        }
        this.mSelector = intent;
    }

    public void setClipData(ClipData clipData) {
        this.mClipData = clipData;
    }

    public void prepareToLeaveUser(int i) {
        if (this.mContentUserHint == -2) {
            this.mContentUserHint = i;
        }
    }

    public Intent putExtra(String str, boolean z) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putBoolean(str, z);
        return this;
    }

    public Intent putExtra(String str, byte b) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putByte(str, b);
        return this;
    }

    public Intent putExtra(String str, char c) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putChar(str, c);
        return this;
    }

    public Intent putExtra(String str, short s) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putShort(str, s);
        return this;
    }

    public Intent putExtra(String str, int i) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putInt(str, i);
        return this;
    }

    public Intent putExtra(String str, long j) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putLong(str, j);
        return this;
    }

    public Intent putExtra(String str, float f) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putFloat(str, f);
        return this;
    }

    public Intent putExtra(String str, double d) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putDouble(str, d);
        return this;
    }

    public Intent putExtra(String str, String str2) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putString(str, str2);
        return this;
    }

    public Intent putExtra(String str, CharSequence charSequence) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putCharSequence(str, charSequence);
        return this;
    }

    public Intent putExtra(String str, Parcelable parcelable) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putParcelable(str, parcelable);
        return this;
    }

    public Intent putExtra(String str, Parcelable[] parcelableArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putParcelableArray(str, parcelableArr);
        return this;
    }

    public Intent putParcelableArrayListExtra(String str, ArrayList<? extends Parcelable> arrayList) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putParcelableArrayList(str, arrayList);
        return this;
    }

    public Intent putIntegerArrayListExtra(String str, ArrayList<Integer> arrayList) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putIntegerArrayList(str, arrayList);
        return this;
    }

    public Intent putStringArrayListExtra(String str, ArrayList<String> arrayList) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putStringArrayList(str, arrayList);
        return this;
    }

    public Intent putCharSequenceArrayListExtra(String str, ArrayList<CharSequence> arrayList) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putCharSequenceArrayList(str, arrayList);
        return this;
    }

    public Intent putExtra(String str, Serializable serializable) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putSerializable(str, serializable);
        return this;
    }

    public Intent putExtra(String str, boolean[] zArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putBooleanArray(str, zArr);
        return this;
    }

    public Intent putExtra(String str, byte[] bArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putByteArray(str, bArr);
        return this;
    }

    public Intent putExtra(String str, short[] sArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putShortArray(str, sArr);
        return this;
    }

    public Intent putExtra(String str, char[] cArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putCharArray(str, cArr);
        return this;
    }

    public Intent putExtra(String str, int[] iArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putIntArray(str, iArr);
        return this;
    }

    public Intent putExtra(String str, long[] jArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putLongArray(str, jArr);
        return this;
    }

    public Intent putExtra(String str, float[] fArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putFloatArray(str, fArr);
        return this;
    }

    public Intent putExtra(String str, double[] dArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putDoubleArray(str, dArr);
        return this;
    }

    public Intent putExtra(String str, String[] strArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putStringArray(str, strArr);
        return this;
    }

    public Intent putExtra(String str, CharSequence[] charSequenceArr) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putCharSequenceArray(str, charSequenceArr);
        return this;
    }

    public Intent putExtra(String str, Bundle bundle) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putBundle(str, bundle);
        return this;
    }

    @Deprecated
    public Intent putExtra(String str, IBinder iBinder) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putIBinder(str, iBinder);
        return this;
    }

    public Intent putExtras(Intent intent) {
        if (intent.mExtras != null) {
            if (this.mExtras == null) {
                this.mExtras = new Bundle(intent.mExtras);
            } else {
                this.mExtras.putAll(intent.mExtras);
            }
        }
        return this;
    }

    public Intent putExtras(Bundle bundle) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putAll(bundle);
        return this;
    }

    public Intent replaceExtras(Intent intent) {
        this.mExtras = intent.mExtras != null ? new Bundle(intent.mExtras) : null;
        return this;
    }

    public Intent replaceExtras(Bundle bundle) {
        this.mExtras = bundle != null ? new Bundle(bundle) : null;
        return this;
    }

    public void removeExtra(String str) {
        if (this.mExtras != null) {
            this.mExtras.remove(str);
            if (this.mExtras.size() == 0) {
                this.mExtras = null;
            }
        }
    }

    public Intent setFlags(int i) {
        this.mFlags = i;
        return this;
    }

    public Intent addFlags(int i) {
        this.mFlags = i | this.mFlags;
        return this;
    }

    public void removeFlags(int i) {
        this.mFlags = (~i) & this.mFlags;
    }

    public Intent setPackage(String str) {
        if (str != null && this.mSelector != null) {
            throw new IllegalArgumentException("Can't set package name when selector is already set");
        }
        this.mPackage = str;
        return this;
    }

    public Intent setComponent(ComponentName componentName) {
        this.mComponent = componentName;
        return this;
    }

    public Intent setClassName(Context context, String str) {
        this.mComponent = new ComponentName(context, str);
        return this;
    }

    public Intent setClassName(String str, String str2) {
        this.mComponent = new ComponentName(str, str2);
        return this;
    }

    public Intent setClass(Context context, Class<?> cls) {
        this.mComponent = new ComponentName(context, cls);
        return this;
    }

    public void setSourceBounds(Rect rect) {
        if (rect != null) {
            this.mSourceBounds = new Rect(rect);
        } else {
            this.mSourceBounds = null;
        }
    }

    public int fillIn(Intent intent, int i) {
        int i2;
        boolean z = false;
        boolean z2 = true;
        if (intent.mAction == null || (this.mAction != null && (i & 1) == 0)) {
            i2 = 0;
        } else {
            this.mAction = intent.mAction;
            i2 = 1;
        }
        if ((intent.mData != null || intent.mType != null) && ((this.mData == null && this.mType == null) || (i & 2) != 0)) {
            this.mData = intent.mData;
            this.mType = intent.mType;
            i2 |= 2;
            z = true;
        }
        if (intent.mCategories != null && (this.mCategories == null || (i & 4) != 0)) {
            if (intent.mCategories != null) {
                this.mCategories = new ArraySet<>((ArraySet) intent.mCategories);
            }
            i2 |= 4;
        }
        if (intent.mPackage != null && ((this.mPackage == null || (i & 16) != 0) && this.mSelector == null)) {
            this.mPackage = intent.mPackage;
            i2 |= 16;
        }
        if (intent.mSelector != null && (i & 64) != 0 && this.mPackage == null) {
            this.mSelector = new Intent(intent.mSelector);
            this.mPackage = null;
            i2 |= 64;
        }
        if (intent.mClipData != null && (this.mClipData == null || (i & 128) != 0)) {
            this.mClipData = intent.mClipData;
            i2 |= 128;
            z = true;
        }
        if (intent.mComponent != null && (i & 8) != 0) {
            this.mComponent = intent.mComponent;
            i2 |= 8;
        }
        this.mFlags |= intent.mFlags;
        if (intent.mSourceBounds != null && (this.mSourceBounds == null || (i & 32) != 0)) {
            this.mSourceBounds = new Rect(intent.mSourceBounds);
            i2 |= 32;
        }
        if (this.mExtras == null) {
            if (intent.mExtras != null) {
                this.mExtras = new Bundle(intent.mExtras);
            } else {
                z2 = z;
            }
        } else if (intent.mExtras != null) {
            try {
                Bundle bundle = new Bundle(intent.mExtras);
                bundle.putAll(this.mExtras);
                this.mExtras = bundle;
            } catch (RuntimeException e) {
                Log.w("Intent", "Failure filling in extras", e);
                z2 = z;
            }
        }
        if (z2 && this.mContentUserHint == -2 && intent.mContentUserHint != -2) {
            this.mContentUserHint = intent.mContentUserHint;
        }
        return i2;
    }

    public static final class FilterComparison {
        private final int mHashCode;
        private final Intent mIntent;

        public FilterComparison(Intent intent) {
            this.mIntent = intent;
            this.mHashCode = intent.filterHashCode();
        }

        public Intent getIntent() {
            return this.mIntent;
        }

        public boolean equals(Object obj) {
            if (obj instanceof FilterComparison) {
                return this.mIntent.filterEquals(((FilterComparison) obj).mIntent);
            }
            return false;
        }

        public int hashCode() {
            return this.mHashCode;
        }
    }

    public boolean filterEquals(Intent intent) {
        if (intent == null || !Objects.equals(this.mAction, intent.mAction) || !Objects.equals(this.mData, intent.mData) || !Objects.equals(this.mType, intent.mType) || !Objects.equals(this.mPackage, intent.mPackage) || !Objects.equals(this.mComponent, intent.mComponent) || !Objects.equals(this.mCategories, intent.mCategories)) {
            return false;
        }
        return true;
    }

    public int filterHashCode() {
        int iHashCode = this.mAction != null ? 0 + this.mAction.hashCode() : 0;
        if (this.mData != null) {
            iHashCode += this.mData.hashCode();
        }
        if (this.mType != null) {
            iHashCode += this.mType.hashCode();
        }
        if (this.mPackage != null) {
            iHashCode += this.mPackage.hashCode();
        }
        if (this.mComponent != null) {
            iHashCode += this.mComponent.hashCode();
        }
        if (this.mCategories != null) {
            return iHashCode + this.mCategories.hashCode();
        }
        return iHashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Intent { ");
        toShortString(sb, true, true, true, false);
        sb.append(" }");
        return sb.toString();
    }

    public String toInsecureString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Intent { ");
        toShortString(sb, false, true, true, false);
        sb.append(" }");
        return sb.toString();
    }

    public String toInsecureStringWithClip() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Intent { ");
        toShortString(sb, false, true, true, true);
        sb.append(" }");
        return sb.toString();
    }

    public String toShortString(boolean z, boolean z2, boolean z3, boolean z4) {
        StringBuilder sb = new StringBuilder(128);
        toShortString(sb, z, z2, z3, z4);
        return sb.toString();
    }

    public void toShortString(StringBuilder sb, boolean z, boolean z2, boolean z3, boolean z4) {
        boolean z5;
        if (this.mAction != null) {
            sb.append("act=");
            sb.append(this.mAction);
            z5 = false;
        } else {
            z5 = true;
        }
        if (this.mCategories != null) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("cat=[");
            for (int i = 0; i < this.mCategories.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(this.mCategories.valueAt(i));
            }
            sb.append("]");
            z5 = false;
        }
        if (this.mData != null) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("dat=");
            if (z) {
                sb.append(this.mData.toSafeString());
            } else {
                sb.append(this.mData);
            }
            z5 = false;
        }
        if (this.mType != null) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("typ=");
            sb.append(this.mType);
            z5 = false;
        }
        if (this.mFlags != 0) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("flg=0x");
            sb.append(Integer.toHexString(this.mFlags));
            z5 = false;
        }
        if (this.mPackage != null) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("pkg=");
            sb.append(this.mPackage);
            z5 = false;
        }
        if (z2 && this.mComponent != null) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("cmp=");
            sb.append(this.mComponent.flattenToShortString());
            z5 = false;
        }
        if (this.mSourceBounds != null) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("bnds=");
            sb.append(this.mSourceBounds.toShortString());
            z5 = false;
        }
        if (this.mClipData != null) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("clip={");
            if (z4) {
                this.mClipData.toShortString(sb);
            } else {
                this.mClipData.toShortStringShortItems(sb, this.mClipData.getDescription() != null ? true ^ this.mClipData.getDescription().toShortStringTypesOnly(sb) : true);
            }
            sb.append('}');
            z5 = false;
        }
        if (z3 && this.mExtras != null) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("(has extras)");
            z5 = false;
        }
        if (this.mContentUserHint != -2) {
            if (!z5) {
                sb.append(' ');
            }
            sb.append("u=");
            sb.append(this.mContentUserHint);
        }
        if (this.mSelector != null) {
            sb.append(" sel=");
            this.mSelector.toShortString(sb, z, z2, z3, z4);
            sb.append("}");
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        writeToProto(protoOutputStream, j, true, true, true, false);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z, boolean z2, boolean z3, boolean z4) {
        long jStart = protoOutputStream.start(j);
        if (this.mAction != null) {
            protoOutputStream.write(1138166333441L, this.mAction);
        }
        if (this.mCategories != null) {
            Iterator<String> it = this.mCategories.iterator();
            while (it.hasNext()) {
                protoOutputStream.write(2237677961218L, it.next());
            }
        }
        if (this.mData != null) {
            protoOutputStream.write(1138166333443L, z ? this.mData.toSafeString() : this.mData.toString());
        }
        if (this.mType != null) {
            protoOutputStream.write(1138166333444L, this.mType);
        }
        if (this.mFlags != 0) {
            protoOutputStream.write(1138166333445L, "0x" + Integer.toHexString(this.mFlags));
        }
        if (this.mPackage != null) {
            protoOutputStream.write(1138166333446L, this.mPackage);
        }
        if (z2 && this.mComponent != null) {
            this.mComponent.writeToProto(protoOutputStream, 1146756268039L);
        }
        if (this.mSourceBounds != null) {
            protoOutputStream.write(1138166333448L, this.mSourceBounds.toShortString());
        }
        if (this.mClipData != null) {
            StringBuilder sb = new StringBuilder();
            if (z4) {
                this.mClipData.toShortString(sb);
            } else {
                this.mClipData.toShortStringShortItems(sb, false);
            }
            protoOutputStream.write(1138166333449L, sb.toString());
        }
        if (z3 && this.mExtras != null) {
            protoOutputStream.write(1138166333450L, this.mExtras.toShortString());
        }
        if (this.mContentUserHint != 0) {
            protoOutputStream.write(1120986464267L, this.mContentUserHint);
        }
        if (this.mSelector != null) {
            protoOutputStream.write(1138166333452L, this.mSelector.toShortString(z, z2, z3, z4));
        }
        protoOutputStream.end(jStart);
    }

    @Deprecated
    public String toURI() {
        return toUri(0);
    }

    public String toUri(int i) {
        StringBuilder sb = new StringBuilder(128);
        String strSubstring = null;
        if ((i & 2) != 0) {
            if (this.mPackage == null) {
                throw new IllegalArgumentException("Intent must include an explicit package name to build an android-app: " + this);
            }
            sb.append("android-app://");
            sb.append(this.mPackage);
            if (this.mData != null && (strSubstring = this.mData.getScheme()) != null) {
                sb.append('/');
                sb.append(strSubstring);
                String encodedAuthority = this.mData.getEncodedAuthority();
                if (encodedAuthority != null) {
                    sb.append('/');
                    sb.append(encodedAuthority);
                    String encodedPath = this.mData.getEncodedPath();
                    if (encodedPath != null) {
                        sb.append(encodedPath);
                    }
                    String encodedQuery = this.mData.getEncodedQuery();
                    if (encodedQuery != null) {
                        sb.append('?');
                        sb.append(encodedQuery);
                    }
                    String encodedFragment = this.mData.getEncodedFragment();
                    if (encodedFragment != null) {
                        sb.append('#');
                        sb.append(encodedFragment);
                    }
                }
            }
            toUriFragment(sb, null, strSubstring == null ? ACTION_MAIN : "android.intent.action.VIEW", this.mPackage, i);
            return sb.toString();
        }
        if (this.mData != null) {
            String string = this.mData.toString();
            if ((i & 1) != 0) {
                int length = string.length();
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        break;
                    }
                    char cCharAt = string.charAt(i2);
                    if ((cCharAt >= 'a' && cCharAt <= 'z') || ((cCharAt >= 'A' && cCharAt <= 'Z') || ((cCharAt >= '0' && cCharAt <= '9') || cCharAt == '.' || cCharAt == '-' || cCharAt == '+'))) {
                        i2++;
                    } else if (cCharAt == ':' && i2 > 0) {
                        strSubstring = string.substring(0, i2);
                        sb.append("intent:");
                        string = string.substring(i2 + 1);
                    }
                }
            }
            sb.append(string);
        } else if ((i & 1) != 0) {
            sb.append("intent:");
        }
        toUriFragment(sb, strSubstring, "android.intent.action.VIEW", null, i);
        return sb.toString();
    }

    private void toUriFragment(StringBuilder sb, String str, String str2, String str3, int i) {
        StringBuilder sb2 = new StringBuilder(128);
        toUriInner(sb2, str, str2, str3, i);
        if (this.mSelector != null) {
            sb2.append("SEL;");
            this.mSelector.toUriInner(sb2, this.mSelector.mData != null ? this.mSelector.mData.getScheme() : null, null, null, i);
        }
        if (sb2.length() > 0) {
            sb.append("#Intent;");
            sb.append((CharSequence) sb2);
            sb.append("end");
        }
    }

    private void toUriInner(StringBuilder sb, String str, String str2, String str3, int i) {
        char c;
        if (str != null) {
            sb.append("scheme=");
            sb.append(str);
            sb.append(';');
        }
        if (this.mAction != null && !this.mAction.equals(str2)) {
            sb.append("action=");
            sb.append(Uri.encode(this.mAction));
            sb.append(';');
        }
        if (this.mCategories != null) {
            for (int i2 = 0; i2 < this.mCategories.size(); i2++) {
                sb.append("category=");
                sb.append(Uri.encode(this.mCategories.valueAt(i2)));
                sb.append(';');
            }
        }
        if (this.mType != null) {
            sb.append("type=");
            sb.append(Uri.encode(this.mType, "/"));
            sb.append(';');
        }
        if (this.mFlags != 0) {
            sb.append("launchFlags=0x");
            sb.append(Integer.toHexString(this.mFlags));
            sb.append(';');
        }
        if (this.mPackage != null && !this.mPackage.equals(str3)) {
            sb.append("package=");
            sb.append(Uri.encode(this.mPackage));
            sb.append(';');
        }
        if (this.mComponent != null) {
            sb.append("component=");
            sb.append(Uri.encode(this.mComponent.flattenToShortString(), "/"));
            sb.append(';');
        }
        if (this.mSourceBounds != null) {
            sb.append("sourceBounds=");
            sb.append(Uri.encode(this.mSourceBounds.flattenToString()));
            sb.append(';');
        }
        if (this.mExtras != null) {
            for (String str4 : this.mExtras.keySet()) {
                Object obj = this.mExtras.get(str4);
                if (obj instanceof String) {
                    c = 'S';
                } else if (obj instanceof Boolean) {
                    c = 'B';
                } else if (obj instanceof Byte) {
                    c = 'b';
                } else if (obj instanceof Character) {
                    c = 'c';
                } else if (obj instanceof Double) {
                    c = DateFormat.DATE;
                } else if (obj instanceof Float) {
                    c = 'f';
                } else if (obj instanceof Integer) {
                    c = 'i';
                } else if (obj instanceof Long) {
                    c = 'l';
                } else {
                    c = obj instanceof Short ? 's' : (char) 0;
                }
                if (c != 0) {
                    sb.append(c);
                    sb.append('.');
                    sb.append(Uri.encode(str4));
                    sb.append('=');
                    sb.append(Uri.encode(obj.toString()));
                    sb.append(';');
                }
            }
        }
    }

    @Override
    public int describeContents() {
        if (this.mExtras != null) {
            return this.mExtras.describeContents();
        }
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mAction);
        Uri.writeToParcel(parcel, this.mData);
        parcel.writeString(this.mType);
        parcel.writeInt(this.mFlags);
        parcel.writeString(this.mPackage);
        ComponentName.writeToParcel(this.mComponent, parcel);
        if (this.mSourceBounds != null) {
            parcel.writeInt(1);
            this.mSourceBounds.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        if (this.mCategories != null) {
            int size = this.mCategories.size();
            parcel.writeInt(size);
            for (int i2 = 0; i2 < size; i2++) {
                parcel.writeString(this.mCategories.valueAt(i2));
            }
        } else {
            parcel.writeInt(0);
        }
        if (this.mSelector != null) {
            parcel.writeInt(1);
            this.mSelector.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        if (this.mClipData != null) {
            parcel.writeInt(1);
            this.mClipData.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.mContentUserHint);
        parcel.writeBundle(this.mExtras);
    }

    protected Intent(Parcel parcel) {
        this.mContentUserHint = -2;
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        setAction(parcel.readString());
        this.mData = Uri.CREATOR.createFromParcel(parcel);
        this.mType = parcel.readString();
        this.mFlags = parcel.readInt();
        this.mPackage = parcel.readString();
        this.mComponent = ComponentName.readFromParcel(parcel);
        if (parcel.readInt() != 0) {
            this.mSourceBounds = Rect.CREATOR.createFromParcel(parcel);
        }
        int i = parcel.readInt();
        if (i > 0) {
            this.mCategories = new ArraySet<>();
            for (int i2 = 0; i2 < i; i2++) {
                this.mCategories.add(parcel.readString().intern());
            }
        } else {
            this.mCategories = null;
        }
        if (parcel.readInt() != 0) {
            this.mSelector = new Intent(parcel);
        }
        if (parcel.readInt() != 0) {
            this.mClipData = new ClipData(parcel);
        }
        this.mContentUserHint = parcel.readInt();
        this.mExtras = parcel.readBundle();
    }

    public static Intent parseIntent(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, IOException {
        Intent intent = new Intent();
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.Intent);
        intent.setAction(typedArrayObtainAttributes.getString(2));
        String string = typedArrayObtainAttributes.getString(3);
        intent.setDataAndType(string != null ? Uri.parse(string) : null, typedArrayObtainAttributes.getString(1));
        String string2 = typedArrayObtainAttributes.getString(0);
        String string3 = typedArrayObtainAttributes.getString(4);
        if (string2 != null && string3 != null) {
            intent.setComponent(new ComponentName(string2, string3));
        }
        typedArrayObtainAttributes.recycle();
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                String name = xmlPullParser.getName();
                if (name.equals(TAG_CATEGORIES)) {
                    TypedArray typedArrayObtainAttributes2 = resources.obtainAttributes(attributeSet, R.styleable.IntentCategory);
                    String string4 = typedArrayObtainAttributes2.getString(0);
                    typedArrayObtainAttributes2.recycle();
                    if (string4 != null) {
                        intent.addCategory(string4);
                    }
                    XmlUtils.skipCurrentTag(xmlPullParser);
                } else if (name.equals(TAG_EXTRA)) {
                    if (intent.mExtras == null) {
                        intent.mExtras = new Bundle();
                    }
                    resources.parseBundleExtra(TAG_EXTRA, attributeSet, intent.mExtras);
                    XmlUtils.skipCurrentTag(xmlPullParser);
                } else {
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        return intent;
    }

    public void saveToXml(XmlSerializer xmlSerializer) throws IOException {
        if (this.mAction != null) {
            xmlSerializer.attribute(null, "action", this.mAction);
        }
        if (this.mData != null) {
            xmlSerializer.attribute(null, "data", this.mData.toString());
        }
        if (this.mType != null) {
            xmlSerializer.attribute(null, "type", this.mType);
        }
        if (this.mComponent != null) {
            xmlSerializer.attribute(null, "component", this.mComponent.flattenToShortString());
        }
        xmlSerializer.attribute(null, "flags", Integer.toHexString(getFlags()));
        if (this.mCategories != null) {
            xmlSerializer.startTag(null, TAG_CATEGORIES);
            for (int size = this.mCategories.size() - 1; size >= 0; size--) {
                xmlSerializer.attribute(null, "category", this.mCategories.valueAt(size));
            }
            xmlSerializer.endTag(null, TAG_CATEGORIES);
        }
    }

    public static Intent restoreFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        Intent intent = new Intent();
        int depth = xmlPullParser.getDepth();
        for (int attributeCount = xmlPullParser.getAttributeCount() - 1; attributeCount >= 0; attributeCount--) {
            String attributeName = xmlPullParser.getAttributeName(attributeCount);
            String attributeValue = xmlPullParser.getAttributeValue(attributeCount);
            if ("action".equals(attributeName)) {
                intent.setAction(attributeValue);
            } else if ("data".equals(attributeName)) {
                intent.setData(Uri.parse(attributeValue));
            } else if ("type".equals(attributeName)) {
                intent.setType(attributeValue);
            } else if ("component".equals(attributeName)) {
                intent.setComponent(ComponentName.unflattenFromString(attributeValue));
            } else if ("flags".equals(attributeName)) {
                intent.setFlags(Integer.parseInt(attributeValue, 16));
            } else {
                Log.e("Intent", "restoreFromXml: unknown attribute=" + attributeName);
            }
        }
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() >= depth)) {
                break;
            }
            if (next == 2) {
                String name = xmlPullParser.getName();
                if (TAG_CATEGORIES.equals(name)) {
                    for (int attributeCount2 = xmlPullParser.getAttributeCount() - 1; attributeCount2 >= 0; attributeCount2--) {
                        intent.addCategory(xmlPullParser.getAttributeValue(attributeCount2));
                    }
                } else {
                    Log.w("Intent", "restoreFromXml: unknown name=" + name);
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        return intent;
    }

    public static String normalizeMimeType(String str) {
        if (str == null) {
            return null;
        }
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        int iIndexOf = lowerCase.indexOf(59);
        if (iIndexOf != -1) {
            return lowerCase.substring(0, iIndexOf);
        }
        return lowerCase;
    }

    public void prepareToLeaveProcess(Context context) {
        prepareToLeaveProcess(this.mComponent == null || !Objects.equals(this.mComponent.getPackageName(), context.getPackageName()));
    }

    public void prepareToLeaveProcess(boolean z) {
        byte b;
        byte b2 = 0;
        setAllowFds(false);
        if (this.mSelector != null) {
            this.mSelector.prepareToLeaveProcess(z);
        }
        if (this.mClipData != null) {
            this.mClipData.prepareToLeaveProcess(z, getFlags());
        }
        if (this.mExtras != null && !this.mExtras.isParcelled()) {
            Object obj = this.mExtras.get(EXTRA_INTENT);
            if (obj instanceof Intent) {
                ((Intent) obj).prepareToLeaveProcess(z);
            }
        }
        if (this.mAction != null && this.mData != null && StrictMode.vmFileUriExposureEnabled() && z) {
            String str = this.mAction;
            switch (str.hashCode()) {
                case -1823790459:
                    b = !str.equals(ACTION_MEDIA_SHARED) ? (byte) -1 : (byte) 5;
                    break;
                case -1665311200:
                    b = !str.equals(ACTION_MEDIA_REMOVED) ? (byte) -1 : (byte) 0;
                    break;
                case -1514214344:
                    b = !str.equals(ACTION_MEDIA_MOUNTED) ? (byte) -1 : (byte) 4;
                    break;
                case -1142424621:
                    b = !str.equals(ACTION_MEDIA_SCANNER_FINISHED) ? (byte) -1 : (byte) 11;
                    break;
                case -963871873:
                    b = !str.equals(ACTION_MEDIA_UNMOUNTED) ? (byte) -1 : (byte) 1;
                    break;
                case -625887599:
                    b = !str.equals(ACTION_MEDIA_EJECT) ? (byte) -1 : (byte) 9;
                    break;
                case 257177710:
                    b = !str.equals(ACTION_MEDIA_NOFS) ? (byte) -1 : (byte) 3;
                    break;
                case 410719838:
                    b = !str.equals(ACTION_MEDIA_UNSHARED) ? (byte) -1 : (byte) 6;
                    break;
                case 582421979:
                    b = !str.equals(ACTION_PACKAGE_NEEDS_VERIFICATION) ? (byte) -1 : (byte) 13;
                    break;
                case 852070077:
                    b = !str.equals(ACTION_MEDIA_SCANNER_SCAN_FILE) ? (byte) -1 : (byte) 12;
                    break;
                case 1412829408:
                    b = !str.equals(ACTION_MEDIA_SCANNER_STARTED) ? (byte) -1 : (byte) 10;
                    break;
                case 1431947322:
                    b = !str.equals(ACTION_MEDIA_UNMOUNTABLE) ? (byte) -1 : (byte) 8;
                    break;
                case 1920444806:
                    b = !str.equals(ACTION_PACKAGE_VERIFIED) ? (byte) -1 : BluetoothHidDevice.ERROR_RSP_UNKNOWN;
                    break;
                case 1964681210:
                    b = !str.equals(ACTION_MEDIA_CHECKING) ? (byte) -1 : (byte) 2;
                    break;
                case 2045140818:
                    b = !str.equals(ACTION_MEDIA_BAD_REMOVAL) ? (byte) -1 : (byte) 7;
                    break;
                default:
                    b = -1;
                    break;
            }
            switch (b) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                    break;
                default:
                    this.mData.checkFileUriExposed("Intent.getData()");
                    break;
            }
        }
        if (this.mAction != null && this.mData != null && StrictMode.vmContentUriWithoutPermissionEnabled() && z) {
            String str2 = this.mAction;
            int iHashCode = str2.hashCode();
            if (iHashCode != -577088908) {
                if (iHashCode != 1662413067 || !str2.equals(ACTION_PROVIDER_CHANGED)) {
                    b2 = -1;
                }
            } else if (str2.equals(ContactsContract.QuickContact.ACTION_QUICK_CONTACT)) {
                b2 = 1;
            }
            switch (b2) {
                case 0:
                case 1:
                    break;
                default:
                    this.mData.checkContentUriWithoutPermission("Intent.getData()", getFlags());
                    break;
            }
        }
    }

    public void prepareToEnterProcess() {
        setDefusable(true);
        if (this.mSelector != null) {
            this.mSelector.prepareToEnterProcess();
        }
        if (this.mClipData != null) {
            this.mClipData.prepareToEnterProcess();
        }
        if (this.mContentUserHint != -2 && UserHandle.getAppId(Process.myUid()) != 1000) {
            fixUris(this.mContentUserHint);
            this.mContentUserHint = -2;
        }
    }

    public boolean hasWebURI() {
        if (getData() == null) {
            return false;
        }
        String scheme = getScheme();
        if (TextUtils.isEmpty(scheme)) {
            return false;
        }
        return scheme.equals(IntentFilter.SCHEME_HTTP) || scheme.equals(IntentFilter.SCHEME_HTTPS);
    }

    public boolean isWebIntent() {
        return "android.intent.action.VIEW".equals(this.mAction) && hasWebURI();
    }

    public void fixUris(int i) {
        Uri uri;
        Uri data = getData();
        if (data != null) {
            this.mData = ContentProvider.maybeAddUserId(data, i);
        }
        if (this.mClipData != null) {
            this.mClipData.fixUris(i);
        }
        String action = getAction();
        if (ACTION_SEND.equals(action)) {
            Uri uri2 = (Uri) getParcelableExtra(EXTRA_STREAM);
            if (uri2 != null) {
                putExtra(EXTRA_STREAM, ContentProvider.maybeAddUserId(uri2, i));
                return;
            }
            return;
        }
        if (ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList parcelableArrayListExtra = getParcelableArrayListExtra(EXTRA_STREAM);
            if (parcelableArrayListExtra != null) {
                ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
                for (int i2 = 0; i2 < parcelableArrayListExtra.size(); i2++) {
                    arrayList.add(ContentProvider.maybeAddUserId((Uri) parcelableArrayListExtra.get(i2), i));
                }
                putParcelableArrayListExtra(EXTRA_STREAM, arrayList);
                return;
            }
            return;
        }
        if ((MediaStore.ACTION_IMAGE_CAPTURE.equals(action) || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action) || MediaStore.ACTION_VIDEO_CAPTURE.equals(action)) && (uri = (Uri) getParcelableExtra(MediaStore.EXTRA_OUTPUT)) != null) {
            putExtra(MediaStore.EXTRA_OUTPUT, ContentProvider.maybeAddUserId(uri, i));
        }
    }

    public boolean migrateExtraStreamToClipData() {
        boolean zMigrateExtraStreamToClipData;
        if ((this.mExtras != null && this.mExtras.isParcelled()) || getClipData() != null) {
            return false;
        }
        String action = getAction();
        if (ACTION_CHOOSER.equals(action)) {
            try {
                Intent intent = (Intent) getParcelableExtra(EXTRA_INTENT);
                if (intent != null) {
                    zMigrateExtraStreamToClipData = intent.migrateExtraStreamToClipData() | false;
                } else {
                    zMigrateExtraStreamToClipData = false;
                }
            } catch (ClassCastException e) {
                zMigrateExtraStreamToClipData = false;
            }
            try {
                Parcelable[] parcelableArrayExtra = getParcelableArrayExtra(EXTRA_INITIAL_INTENTS);
                if (parcelableArrayExtra != null) {
                    for (Parcelable parcelable : parcelableArrayExtra) {
                        Intent intent2 = (Intent) parcelable;
                        if (intent2 != null) {
                            zMigrateExtraStreamToClipData |= intent2.migrateExtraStreamToClipData();
                        }
                    }
                }
            } catch (ClassCastException e2) {
            }
            return zMigrateExtraStreamToClipData;
        }
        if (ACTION_SEND.equals(action)) {
            try {
                Uri uri = (Uri) getParcelableExtra(EXTRA_STREAM);
                CharSequence charSequenceExtra = getCharSequenceExtra(EXTRA_TEXT);
                String stringExtra = getStringExtra(EXTRA_HTML_TEXT);
                if (uri != null || charSequenceExtra != null || stringExtra != null) {
                    setClipData(new ClipData(null, new String[]{getType()}, new ClipData.Item(charSequenceExtra, stringExtra, null, uri)));
                    addFlags(1);
                    return true;
                }
            } catch (ClassCastException e3) {
            }
        } else if (ACTION_SEND_MULTIPLE.equals(action)) {
            try {
                ArrayList parcelableArrayListExtra = getParcelableArrayListExtra(EXTRA_STREAM);
                ArrayList<CharSequence> charSequenceArrayListExtra = getCharSequenceArrayListExtra(EXTRA_TEXT);
                ArrayList<String> stringArrayListExtra = getStringArrayListExtra(EXTRA_HTML_TEXT);
                int size = -1;
                if (parcelableArrayListExtra != null) {
                    size = parcelableArrayListExtra.size();
                }
                if (charSequenceArrayListExtra != null) {
                    if (size >= 0 && size != charSequenceArrayListExtra.size()) {
                        return false;
                    }
                    size = charSequenceArrayListExtra.size();
                }
                if (stringArrayListExtra != null) {
                    if (size >= 0 && size != stringArrayListExtra.size()) {
                        return false;
                    }
                    size = stringArrayListExtra.size();
                }
                if (size > 0) {
                    ClipData clipData = new ClipData(null, new String[]{getType()}, makeClipItem(parcelableArrayListExtra, charSequenceArrayListExtra, stringArrayListExtra, 0));
                    for (int i = 1; i < size; i++) {
                        clipData.addItem(makeClipItem(parcelableArrayListExtra, charSequenceArrayListExtra, stringArrayListExtra, i));
                    }
                    setClipData(clipData);
                    addFlags(1);
                    return true;
                }
            } catch (ClassCastException e4) {
            }
        } else if (MediaStore.ACTION_IMAGE_CAPTURE.equals(action) || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action) || MediaStore.ACTION_VIDEO_CAPTURE.equals(action)) {
            try {
                Uri uri2 = (Uri) getParcelableExtra(MediaStore.EXTRA_OUTPUT);
                if (uri2 != null) {
                    setClipData(ClipData.newRawUri("", uri2));
                    addFlags(3);
                    return true;
                }
            } catch (ClassCastException e5) {
                return false;
            }
        }
        return false;
    }

    public static String dockStateToString(int i) {
        switch (i) {
            case 0:
                return "EXTRA_DOCK_STATE_UNDOCKED";
            case 1:
                return "EXTRA_DOCK_STATE_DESK";
            case 2:
                return "EXTRA_DOCK_STATE_CAR";
            case 3:
                return "EXTRA_DOCK_STATE_LE_DESK";
            case 4:
                return "EXTRA_DOCK_STATE_HE_DESK";
            default:
                return Integer.toString(i);
        }
    }

    private static ClipData.Item makeClipItem(ArrayList<Uri> arrayList, ArrayList<CharSequence> arrayList2, ArrayList<String> arrayList3, int i) {
        return new ClipData.Item(arrayList2 != null ? arrayList2.get(i) : null, arrayList3 != null ? arrayList3.get(i) : null, null, arrayList != null ? arrayList.get(i) : null);
    }

    public boolean isDocument() {
        return (this.mFlags & 524288) == 524288;
    }
}
