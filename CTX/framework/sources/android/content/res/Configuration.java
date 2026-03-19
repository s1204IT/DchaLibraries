package android.content.res;

import android.app.WindowConfiguration;
import android.app.slice.Slice;
import android.content.ConfigurationProto;
import android.content.ResourcesConfigurationProto;
import android.hardware.Camera;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.proto.ProtoOutputStream;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class Configuration implements Parcelable, Comparable<Configuration> {
    public static final int ASSETS_SEQ_UNDEFINED = 0;
    public static final int COLOR_MODE_HDR_MASK = 12;
    public static final int COLOR_MODE_HDR_NO = 4;
    public static final int COLOR_MODE_HDR_SHIFT = 2;
    public static final int COLOR_MODE_HDR_UNDEFINED = 0;
    public static final int COLOR_MODE_HDR_YES = 8;
    public static final int COLOR_MODE_UNDEFINED = 0;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_MASK = 3;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_NO = 1;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_UNDEFINED = 0;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_YES = 2;
    public static final int DENSITY_DPI_ANY = 65534;
    public static final int DENSITY_DPI_NONE = 65535;
    public static final int DENSITY_DPI_UNDEFINED = 0;
    public static final int HARDKEYBOARDHIDDEN_NO = 1;
    public static final int HARDKEYBOARDHIDDEN_UNDEFINED = 0;
    public static final int HARDKEYBOARDHIDDEN_YES = 2;
    public static final int KEYBOARDHIDDEN_NO = 1;
    public static final int KEYBOARDHIDDEN_SOFT = 3;
    public static final int KEYBOARDHIDDEN_UNDEFINED = 0;
    public static final int KEYBOARDHIDDEN_YES = 2;
    public static final int KEYBOARD_12KEY = 3;
    public static final int KEYBOARD_NOKEYS = 1;
    public static final int KEYBOARD_QWERTY = 2;
    public static final int KEYBOARD_UNDEFINED = 0;
    public static final int MNC_ZERO = 65535;
    public static final int NATIVE_CONFIG_COLOR_MODE = 65536;
    public static final int NATIVE_CONFIG_DENSITY = 256;
    public static final int NATIVE_CONFIG_KEYBOARD = 16;
    public static final int NATIVE_CONFIG_KEYBOARD_HIDDEN = 32;
    public static final int NATIVE_CONFIG_LAYOUTDIR = 16384;
    public static final int NATIVE_CONFIG_LOCALE = 4;
    public static final int NATIVE_CONFIG_MCC = 1;
    public static final int NATIVE_CONFIG_MNC = 2;
    public static final int NATIVE_CONFIG_NAVIGATION = 64;
    public static final int NATIVE_CONFIG_ORIENTATION = 128;
    public static final int NATIVE_CONFIG_SCREEN_LAYOUT = 2048;
    public static final int NATIVE_CONFIG_SCREEN_SIZE = 512;
    public static final int NATIVE_CONFIG_SMALLEST_SCREEN_SIZE = 8192;
    public static final int NATIVE_CONFIG_TOUCHSCREEN = 8;
    public static final int NATIVE_CONFIG_UI_MODE = 4096;
    public static final int NATIVE_CONFIG_VERSION = 1024;
    public static final int NAVIGATIONHIDDEN_NO = 1;
    public static final int NAVIGATIONHIDDEN_UNDEFINED = 0;
    public static final int NAVIGATIONHIDDEN_YES = 2;
    public static final int NAVIGATION_DPAD = 2;
    public static final int NAVIGATION_NONAV = 1;
    public static final int NAVIGATION_TRACKBALL = 3;
    public static final int NAVIGATION_UNDEFINED = 0;
    public static final int NAVIGATION_WHEEL = 4;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int ORIENTATION_PORTRAIT = 1;

    @Deprecated
    public static final int ORIENTATION_SQUARE = 3;
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int SCREENLAYOUT_COMPAT_NEEDED = 268435456;
    public static final int SCREENLAYOUT_LAYOUTDIR_LTR = 64;
    public static final int SCREENLAYOUT_LAYOUTDIR_MASK = 192;
    public static final int SCREENLAYOUT_LAYOUTDIR_RTL = 128;
    public static final int SCREENLAYOUT_LAYOUTDIR_SHIFT = 6;
    public static final int SCREENLAYOUT_LAYOUTDIR_UNDEFINED = 0;
    public static final int SCREENLAYOUT_LONG_MASK = 48;
    public static final int SCREENLAYOUT_LONG_NO = 16;
    public static final int SCREENLAYOUT_LONG_UNDEFINED = 0;
    public static final int SCREENLAYOUT_LONG_YES = 32;
    public static final int SCREENLAYOUT_ROUND_MASK = 768;
    public static final int SCREENLAYOUT_ROUND_NO = 256;
    public static final int SCREENLAYOUT_ROUND_SHIFT = 8;
    public static final int SCREENLAYOUT_ROUND_UNDEFINED = 0;
    public static final int SCREENLAYOUT_ROUND_YES = 512;
    public static final int SCREENLAYOUT_SIZE_LARGE = 3;
    public static final int SCREENLAYOUT_SIZE_MASK = 15;
    public static final int SCREENLAYOUT_SIZE_NORMAL = 2;
    public static final int SCREENLAYOUT_SIZE_SMALL = 1;
    public static final int SCREENLAYOUT_SIZE_UNDEFINED = 0;
    public static final int SCREENLAYOUT_SIZE_XLARGE = 4;
    public static final int SCREENLAYOUT_UNDEFINED = 0;
    public static final int SCREEN_HEIGHT_DP_UNDEFINED = 0;
    public static final int SCREEN_WIDTH_DP_UNDEFINED = 0;
    public static final int SMALLEST_SCREEN_WIDTH_DP_UNDEFINED = 0;
    public static final int TOUCHSCREEN_FINGER = 3;
    public static final int TOUCHSCREEN_NOTOUCH = 1;

    @Deprecated
    public static final int TOUCHSCREEN_STYLUS = 2;
    public static final int TOUCHSCREEN_UNDEFINED = 0;
    public static final int UI_MODE_NIGHT_MASK = 48;
    public static final int UI_MODE_NIGHT_NO = 16;
    public static final int UI_MODE_NIGHT_UNDEFINED = 0;
    public static final int UI_MODE_NIGHT_YES = 32;
    public static final int UI_MODE_TYPE_APPLIANCE = 5;
    public static final int UI_MODE_TYPE_CAR = 3;
    public static final int UI_MODE_TYPE_DESK = 2;
    public static final int UI_MODE_TYPE_MASK = 15;
    public static final int UI_MODE_TYPE_NORMAL = 1;
    public static final int UI_MODE_TYPE_TELEVISION = 4;
    public static final int UI_MODE_TYPE_UNDEFINED = 0;
    public static final int UI_MODE_TYPE_VR_HEADSET = 7;
    public static final int UI_MODE_TYPE_WATCH = 6;
    private static final String XML_ATTR_APP_BOUNDS = "app_bounds";
    private static final String XML_ATTR_COLOR_MODE = "clrMod";
    private static final String XML_ATTR_DENSITY = "density";
    private static final String XML_ATTR_FONT_SCALE = "fs";
    private static final String XML_ATTR_HARD_KEYBOARD_HIDDEN = "hardKeyHid";
    private static final String XML_ATTR_KEYBOARD = "key";
    private static final String XML_ATTR_KEYBOARD_HIDDEN = "keyHid";
    private static final String XML_ATTR_LOCALES = "locales";
    private static final String XML_ATTR_MCC = "mcc";
    private static final String XML_ATTR_MNC = "mnc";
    private static final String XML_ATTR_NAVIGATION = "nav";
    private static final String XML_ATTR_NAVIGATION_HIDDEN = "navHid";
    private static final String XML_ATTR_ORIENTATION = "ori";
    private static final String XML_ATTR_ROTATION = "rot";
    private static final String XML_ATTR_SCREEN_HEIGHT = "height";
    private static final String XML_ATTR_SCREEN_LAYOUT = "scrLay";
    private static final String XML_ATTR_SCREEN_WIDTH = "width";
    private static final String XML_ATTR_SMALLEST_WIDTH = "sw";
    private static final String XML_ATTR_TOUCHSCREEN = "touch";
    private static final String XML_ATTR_UI_MODE = "ui";
    public int assetsSeq;
    public int colorMode;
    public int compatScreenHeightDp;
    public int compatScreenWidthDp;
    public int compatSmallestScreenWidthDp;
    public int densityDpi;
    public float fontScale;
    public int hardKeyboardHidden;
    public int keyboard;
    public int keyboardHidden;

    @Deprecated
    public Locale locale;
    private LocaleList mLocaleList;
    public int mcc;
    public int mnc;
    public int navigation;
    public int navigationHidden;
    public int orientation;
    public int screenHeightDp;
    public int screenLayout;
    public int screenWidthDp;
    public int seq;
    public int smallestScreenWidthDp;
    public int touchscreen;
    public int uiMode;
    public boolean userSetLocale;
    public final WindowConfiguration windowConfiguration;
    public static final Configuration EMPTY = new Configuration();
    public static final Parcelable.Creator<Configuration> CREATOR = new Parcelable.Creator<Configuration>() {
        @Override
        public Configuration createFromParcel(Parcel parcel) {
            return new Configuration(parcel);
        }

        @Override
        public Configuration[] newArray(int i) {
            return new Configuration[i];
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface NativeConfig {
    }

    public static int resetScreenLayout(int i) {
        return (i & (-268435520)) | 36;
    }

    public static int reduceScreenLayout(int i, int i2, int i3) {
        int i4;
        boolean z;
        boolean z2 = false;
        if (i2 >= 470) {
            if (i2 >= 960 && i3 >= 720) {
                i4 = 4;
            } else if (i2 < 640 || i3 < 480) {
                i4 = 2;
            } else {
                i4 = 3;
            }
            z = i3 > 321 || i2 > 570;
            if ((i2 * 3) / 5 >= i3 - 1) {
                z2 = true;
            }
        } else {
            z = false;
            i4 = 1;
        }
        if (!z2) {
            i = (i & (-49)) | 16;
        }
        if (z) {
            i |= 268435456;
        }
        if (i4 < (i & 15)) {
            return (i & (-16)) | i4;
        }
        return i;
    }

    public static String configurationDiffToString(int i) {
        ArrayList arrayList = new ArrayList();
        if ((i & 1) != 0) {
            arrayList.add("CONFIG_MCC");
        }
        if ((i & 2) != 0) {
            arrayList.add("CONFIG_MNC");
        }
        if ((i & 4) != 0) {
            arrayList.add("CONFIG_LOCALE");
        }
        if ((i & 8) != 0) {
            arrayList.add("CONFIG_TOUCHSCREEN");
        }
        if ((i & 16) != 0) {
            arrayList.add("CONFIG_KEYBOARD");
        }
        if ((i & 32) != 0) {
            arrayList.add("CONFIG_KEYBOARD_HIDDEN");
        }
        if ((i & 64) != 0) {
            arrayList.add("CONFIG_NAVIGATION");
        }
        if ((i & 128) != 0) {
            arrayList.add("CONFIG_ORIENTATION");
        }
        if ((i & 256) != 0) {
            arrayList.add("CONFIG_SCREEN_LAYOUT");
        }
        if ((i & 16384) != 0) {
            arrayList.add("CONFIG_COLOR_MODE");
        }
        if ((i & 512) != 0) {
            arrayList.add("CONFIG_UI_MODE");
        }
        if ((i & 1024) != 0) {
            arrayList.add("CONFIG_SCREEN_SIZE");
        }
        if ((i & 2048) != 0) {
            arrayList.add("CONFIG_SMALLEST_SCREEN_SIZE");
        }
        if ((i & 8192) != 0) {
            arrayList.add("CONFIG_LAYOUT_DIRECTION");
        }
        if ((1073741824 & i) != 0) {
            arrayList.add("CONFIG_FONT_SCALE");
        }
        if ((i & Integer.MIN_VALUE) != 0) {
            arrayList.add("CONFIG_ASSETS_PATHS");
        }
        StringBuilder sb = new StringBuilder("{");
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            sb.append((String) arrayList.get(i2));
            if (i2 != size - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public boolean isLayoutSizeAtLeast(int i) {
        int i2 = this.screenLayout & 15;
        return i2 != 0 && i2 >= i;
    }

    public Configuration() {
        this.windowConfiguration = new WindowConfiguration();
        unset();
    }

    public Configuration(Configuration configuration) {
        this.windowConfiguration = new WindowConfiguration();
        setTo(configuration);
    }

    private void fixUpLocaleList() {
        if ((this.locale == null && !this.mLocaleList.isEmpty()) || (this.locale != null && !this.locale.equals(this.mLocaleList.get(0)))) {
            this.mLocaleList = this.locale == null ? LocaleList.getEmptyLocaleList() : new LocaleList(this.locale);
        }
    }

    public void setTo(Configuration configuration) {
        this.fontScale = configuration.fontScale;
        this.mcc = configuration.mcc;
        this.mnc = configuration.mnc;
        this.locale = configuration.locale == null ? null : (Locale) configuration.locale.clone();
        configuration.fixUpLocaleList();
        this.mLocaleList = configuration.mLocaleList;
        this.userSetLocale = configuration.userSetLocale;
        this.touchscreen = configuration.touchscreen;
        this.keyboard = configuration.keyboard;
        this.keyboardHidden = configuration.keyboardHidden;
        this.hardKeyboardHidden = configuration.hardKeyboardHidden;
        this.navigation = configuration.navigation;
        this.navigationHidden = configuration.navigationHidden;
        this.orientation = configuration.orientation;
        this.screenLayout = configuration.screenLayout;
        this.colorMode = configuration.colorMode;
        this.uiMode = configuration.uiMode;
        this.screenWidthDp = configuration.screenWidthDp;
        this.screenHeightDp = configuration.screenHeightDp;
        this.smallestScreenWidthDp = configuration.smallestScreenWidthDp;
        this.densityDpi = configuration.densityDpi;
        this.compatScreenWidthDp = configuration.compatScreenWidthDp;
        this.compatScreenHeightDp = configuration.compatScreenHeightDp;
        this.compatSmallestScreenWidthDp = configuration.compatSmallestScreenWidthDp;
        this.assetsSeq = configuration.assetsSeq;
        this.seq = configuration.seq;
        this.windowConfiguration.setTo(configuration.windowConfiguration);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("{");
        sb.append(this.fontScale);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        if (this.mcc != 0) {
            sb.append(this.mcc);
            sb.append("mcc");
        } else {
            sb.append("?mcc");
        }
        if (this.mnc != 0) {
            sb.append(this.mnc);
            sb.append("mnc");
        } else {
            sb.append("?mnc");
        }
        fixUpLocaleList();
        if (!this.mLocaleList.isEmpty()) {
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(this.mLocaleList);
        } else {
            sb.append(" ?localeList");
        }
        int i = this.screenLayout & 192;
        if (i == 0) {
            sb.append(" ?layoutDir");
        } else if (i == 64) {
            sb.append(" ldltr");
        } else if (i == 128) {
            sb.append(" ldrtl");
        } else {
            sb.append(" layoutDir=");
            sb.append(i >> 6);
        }
        if (this.smallestScreenWidthDp != 0) {
            sb.append(" sw");
            sb.append(this.smallestScreenWidthDp);
            sb.append("dp");
        } else {
            sb.append(" ?swdp");
        }
        if (this.screenWidthDp != 0) {
            sb.append(" w");
            sb.append(this.screenWidthDp);
            sb.append("dp");
        } else {
            sb.append(" ?wdp");
        }
        if (this.screenHeightDp != 0) {
            sb.append(" h");
            sb.append(this.screenHeightDp);
            sb.append("dp");
        } else {
            sb.append(" ?hdp");
        }
        if (this.densityDpi != 0) {
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(this.densityDpi);
            sb.append("dpi");
        } else {
            sb.append(" ?density");
        }
        switch (this.screenLayout & 15) {
            case 0:
                sb.append(" ?lsize");
                break;
            case 1:
                sb.append(" smll");
                break;
            case 2:
                sb.append(" nrml");
                break;
            case 3:
                sb.append(" lrg");
                break;
            case 4:
                sb.append(" xlrg");
                break;
            default:
                sb.append(" layoutSize=");
                sb.append(this.screenLayout & 15);
                break;
        }
        int i2 = this.screenLayout & 48;
        if (i2 == 0) {
            sb.append(" ?long");
        } else if (i2 != 16) {
            if (i2 == 32) {
                sb.append(" long");
            } else {
                sb.append(" layoutLong=");
                sb.append(this.screenLayout & 48);
            }
        }
        int i3 = this.colorMode & 12;
        if (i3 == 0) {
            sb.append(" ?ldr");
        } else if (i3 != 4) {
            if (i3 == 8) {
                sb.append(" hdr");
            } else {
                sb.append(" dynamicRange=");
                sb.append(this.colorMode & 12);
            }
        }
        switch (this.colorMode & 3) {
            case 0:
                sb.append(" ?wideColorGamut");
                break;
            case 1:
                break;
            case 2:
                sb.append(" widecg");
                break;
            default:
                sb.append(" wideColorGamut=");
                sb.append(this.colorMode & 3);
                break;
        }
        switch (this.orientation) {
            case 0:
                sb.append(" ?orien");
                break;
            case 1:
                sb.append(" port");
                break;
            case 2:
                sb.append(" land");
                break;
            default:
                sb.append(" orien=");
                sb.append(this.orientation);
                break;
        }
        switch (this.uiMode & 15) {
            case 0:
                sb.append(" ?uimode");
                break;
            case 1:
                break;
            case 2:
                sb.append(" desk");
                break;
            case 3:
                sb.append(" car");
                break;
            case 4:
                sb.append(" television");
                break;
            case 5:
                sb.append(" appliance");
                break;
            case 6:
                sb.append(" watch");
                break;
            case 7:
                sb.append(" vrheadset");
                break;
            default:
                sb.append(" uimode=");
                sb.append(this.uiMode & 15);
                break;
        }
        int i4 = this.uiMode & 48;
        if (i4 == 0) {
            sb.append(" ?night");
        } else if (i4 != 16) {
            if (i4 == 32) {
                sb.append(" night");
            } else {
                sb.append(" night=");
                sb.append(this.uiMode & 48);
            }
        }
        switch (this.touchscreen) {
            case 0:
                sb.append(" ?touch");
                break;
            case 1:
                sb.append(" -touch");
                break;
            case 2:
                sb.append(" stylus");
                break;
            case 3:
                sb.append(" finger");
                break;
            default:
                sb.append(" touch=");
                sb.append(this.touchscreen);
                break;
        }
        switch (this.keyboard) {
            case 0:
                sb.append(" ?keyb");
                break;
            case 1:
                sb.append(" -keyb");
                break;
            case 2:
                sb.append(" qwerty");
                break;
            case 3:
                sb.append(" 12key");
                break;
            default:
                sb.append(" keys=");
                sb.append(this.keyboard);
                break;
        }
        switch (this.keyboardHidden) {
            case 0:
                sb.append("/?");
                break;
            case 1:
                sb.append("/v");
                break;
            case 2:
                sb.append("/h");
                break;
            case 3:
                sb.append("/s");
                break;
            default:
                sb.append("/");
                sb.append(this.keyboardHidden);
                break;
        }
        switch (this.hardKeyboardHidden) {
            case 0:
                sb.append("/?");
                break;
            case 1:
                sb.append("/v");
                break;
            case 2:
                sb.append("/h");
                break;
            default:
                sb.append("/");
                sb.append(this.hardKeyboardHidden);
                break;
        }
        switch (this.navigation) {
            case 0:
                sb.append(" ?nav");
                break;
            case 1:
                sb.append(" -nav");
                break;
            case 2:
                sb.append(" dpad");
                break;
            case 3:
                sb.append(" tball");
                break;
            case 4:
                sb.append(" wheel");
                break;
            default:
                sb.append(" nav=");
                sb.append(this.navigation);
                break;
        }
        switch (this.navigationHidden) {
            case 0:
                sb.append("/?");
                break;
            case 1:
                sb.append("/v");
                break;
            case 2:
                sb.append("/h");
                break;
            default:
                sb.append("/");
                sb.append(this.navigationHidden);
                break;
        }
        sb.append(" winConfig=");
        sb.append(this.windowConfiguration);
        if (this.assetsSeq != 0) {
            sb.append(" as.");
            sb.append(this.assetsSeq);
        }
        if (this.seq != 0) {
            sb.append(" s.");
            sb.append(this.seq);
        }
        sb.append('}');
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1108101562369L, this.fontScale);
        protoOutputStream.write(1155346202626L, this.mcc);
        protoOutputStream.write(1155346202627L, this.mnc);
        this.mLocaleList.writeToProto(protoOutputStream, 2246267895812L);
        protoOutputStream.write(1155346202629L, this.screenLayout);
        protoOutputStream.write(1155346202630L, this.colorMode);
        protoOutputStream.write(ConfigurationProto.TOUCHSCREEN, this.touchscreen);
        protoOutputStream.write(1155346202632L, this.keyboard);
        protoOutputStream.write(ConfigurationProto.KEYBOARD_HIDDEN, this.keyboardHidden);
        protoOutputStream.write(ConfigurationProto.HARD_KEYBOARD_HIDDEN, this.hardKeyboardHidden);
        protoOutputStream.write(ConfigurationProto.NAVIGATION, this.navigation);
        protoOutputStream.write(ConfigurationProto.NAVIGATION_HIDDEN, this.navigationHidden);
        protoOutputStream.write(ConfigurationProto.ORIENTATION, this.orientation);
        protoOutputStream.write(ConfigurationProto.UI_MODE, this.uiMode);
        protoOutputStream.write(ConfigurationProto.SCREEN_WIDTH_DP, this.screenWidthDp);
        protoOutputStream.write(ConfigurationProto.SCREEN_HEIGHT_DP, this.screenHeightDp);
        protoOutputStream.write(ConfigurationProto.SMALLEST_SCREEN_WIDTH_DP, this.smallestScreenWidthDp);
        protoOutputStream.write(ConfigurationProto.DENSITY_DPI, this.densityDpi);
        this.windowConfiguration.writeToProto(protoOutputStream, 1146756268051L);
        protoOutputStream.end(jStart);
    }

    public void writeResConfigToProto(ProtoOutputStream protoOutputStream, long j, DisplayMetrics displayMetrics) {
        int i;
        int i2;
        if (displayMetrics.widthPixels >= displayMetrics.heightPixels) {
            i = displayMetrics.widthPixels;
            i2 = displayMetrics.heightPixels;
        } else {
            i = displayMetrics.heightPixels;
            i2 = displayMetrics.widthPixels;
        }
        long jStart = protoOutputStream.start(j);
        writeToProto(protoOutputStream, 1146756268033L);
        protoOutputStream.write(1155346202626L, Build.VERSION.RESOURCES_SDK_INT);
        protoOutputStream.write(1155346202627L, i);
        protoOutputStream.write(ResourcesConfigurationProto.SCREEN_HEIGHT_PX, i2);
        protoOutputStream.end(jStart);
    }

    public static String uiModeToString(int i) {
        switch (i) {
            case 0:
                return "UI_MODE_TYPE_UNDEFINED";
            case 1:
                return "UI_MODE_TYPE_NORMAL";
            case 2:
                return "UI_MODE_TYPE_DESK";
            case 3:
                return "UI_MODE_TYPE_CAR";
            case 4:
                return "UI_MODE_TYPE_TELEVISION";
            case 5:
                return "UI_MODE_TYPE_APPLIANCE";
            case 6:
                return "UI_MODE_TYPE_WATCH";
            case 7:
                return "UI_MODE_TYPE_VR_HEADSET";
            default:
                return Integer.toString(i);
        }
    }

    public void setToDefaults() {
        this.fontScale = 1.0f;
        this.mnc = 0;
        this.mcc = 0;
        this.mLocaleList = LocaleList.getEmptyLocaleList();
        this.locale = null;
        this.userSetLocale = false;
        this.touchscreen = 0;
        this.keyboard = 0;
        this.keyboardHidden = 0;
        this.hardKeyboardHidden = 0;
        this.navigation = 0;
        this.navigationHidden = 0;
        this.orientation = 0;
        this.screenLayout = 0;
        this.colorMode = 0;
        this.uiMode = 0;
        this.compatScreenWidthDp = 0;
        this.screenWidthDp = 0;
        this.compatScreenHeightDp = 0;
        this.screenHeightDp = 0;
        this.compatSmallestScreenWidthDp = 0;
        this.smallestScreenWidthDp = 0;
        this.densityDpi = 0;
        this.assetsSeq = 0;
        this.seq = 0;
        this.windowConfiguration.setToDefaults();
    }

    public void unset() {
        setToDefaults();
        this.fontScale = 0.0f;
    }

    @Deprecated
    public void makeDefault() {
        setToDefaults();
    }

    public int updateFrom(Configuration configuration) {
        int i;
        if (configuration.fontScale > 0.0f && this.fontScale != configuration.fontScale) {
            i = 1073741824;
            this.fontScale = configuration.fontScale;
        } else {
            i = 0;
        }
        if (configuration.mcc != 0 && this.mcc != configuration.mcc) {
            i |= 1;
            this.mcc = configuration.mcc;
        }
        if (configuration.mnc != 0 && this.mnc != configuration.mnc) {
            i |= 2;
            this.mnc = configuration.mnc;
        }
        fixUpLocaleList();
        configuration.fixUpLocaleList();
        if (!configuration.mLocaleList.isEmpty() && !this.mLocaleList.equals(configuration.mLocaleList)) {
            i |= 4;
            this.mLocaleList = configuration.mLocaleList;
            if (!configuration.locale.equals(this.locale)) {
                this.locale = (Locale) configuration.locale.clone();
                i |= 8192;
                setLayoutDirection(this.locale);
            }
        }
        int i2 = configuration.screenLayout & 192;
        if (i2 != 0 && i2 != (this.screenLayout & 192)) {
            this.screenLayout = i2 | (this.screenLayout & (-193));
            i |= 8192;
        }
        if (configuration.userSetLocale && (!this.userSetLocale || (i & 4) != 0)) {
            i |= 4;
            this.userSetLocale = true;
        }
        if (configuration.touchscreen != 0 && this.touchscreen != configuration.touchscreen) {
            i |= 8;
            this.touchscreen = configuration.touchscreen;
        }
        if (configuration.keyboard != 0 && this.keyboard != configuration.keyboard) {
            i |= 16;
            this.keyboard = configuration.keyboard;
        }
        if (configuration.keyboardHidden != 0 && this.keyboardHidden != configuration.keyboardHidden) {
            i |= 32;
            this.keyboardHidden = configuration.keyboardHidden;
        }
        if (configuration.hardKeyboardHidden != 0 && this.hardKeyboardHidden != configuration.hardKeyboardHidden) {
            i |= 32;
            this.hardKeyboardHidden = configuration.hardKeyboardHidden;
        }
        if (configuration.navigation != 0 && this.navigation != configuration.navigation) {
            i |= 64;
            this.navigation = configuration.navigation;
        }
        if (configuration.navigationHidden != 0 && this.navigationHidden != configuration.navigationHidden) {
            i |= 32;
            this.navigationHidden = configuration.navigationHidden;
        }
        if (configuration.orientation != 0 && this.orientation != configuration.orientation) {
            i |= 128;
            this.orientation = configuration.orientation;
        }
        if ((configuration.screenLayout & 15) != 0 && (configuration.screenLayout & 15) != (this.screenLayout & 15)) {
            i |= 256;
            this.screenLayout = (this.screenLayout & (-16)) | (configuration.screenLayout & 15);
        }
        if ((configuration.screenLayout & 48) != 0 && (configuration.screenLayout & 48) != (this.screenLayout & 48)) {
            i |= 256;
            this.screenLayout = (this.screenLayout & (-49)) | (configuration.screenLayout & 48);
        }
        if ((configuration.screenLayout & 768) != 0 && (configuration.screenLayout & 768) != (this.screenLayout & 768)) {
            i |= 256;
            this.screenLayout = (this.screenLayout & (-769)) | (configuration.screenLayout & 768);
        }
        if ((configuration.screenLayout & 268435456) != (this.screenLayout & 268435456) && configuration.screenLayout != 0) {
            i |= 256;
            this.screenLayout = (this.screenLayout & (-268435457)) | (268435456 & configuration.screenLayout);
        }
        if ((configuration.colorMode & 3) != 0 && (configuration.colorMode & 3) != (this.colorMode & 3)) {
            i |= 16384;
            this.colorMode = (this.colorMode & (-4)) | (configuration.colorMode & 3);
        }
        if ((configuration.colorMode & 12) != 0 && (configuration.colorMode & 12) != (this.colorMode & 12)) {
            i |= 16384;
            this.colorMode = (this.colorMode & (-13)) | (configuration.colorMode & 12);
        }
        if (configuration.uiMode != 0 && this.uiMode != configuration.uiMode) {
            i |= 512;
            if ((configuration.uiMode & 15) != 0) {
                this.uiMode = (this.uiMode & (-16)) | (configuration.uiMode & 15);
            }
            if ((configuration.uiMode & 48) != 0) {
                this.uiMode = (this.uiMode & (-49)) | (configuration.uiMode & 48);
            }
        }
        if (configuration.screenWidthDp != 0 && this.screenWidthDp != configuration.screenWidthDp) {
            i |= 1024;
            this.screenWidthDp = configuration.screenWidthDp;
        }
        if (configuration.screenHeightDp != 0 && this.screenHeightDp != configuration.screenHeightDp) {
            i |= 1024;
            this.screenHeightDp = configuration.screenHeightDp;
        }
        if (configuration.smallestScreenWidthDp != 0 && this.smallestScreenWidthDp != configuration.smallestScreenWidthDp) {
            i |= 2048;
            this.smallestScreenWidthDp = configuration.smallestScreenWidthDp;
        }
        if (configuration.densityDpi != 0 && this.densityDpi != configuration.densityDpi) {
            i |= 4096;
            this.densityDpi = configuration.densityDpi;
        }
        if (configuration.compatScreenWidthDp != 0) {
            this.compatScreenWidthDp = configuration.compatScreenWidthDp;
        }
        if (configuration.compatScreenHeightDp != 0) {
            this.compatScreenHeightDp = configuration.compatScreenHeightDp;
        }
        if (configuration.compatSmallestScreenWidthDp != 0) {
            this.compatSmallestScreenWidthDp = configuration.compatSmallestScreenWidthDp;
        }
        if (configuration.assetsSeq != 0 && configuration.assetsSeq != this.assetsSeq) {
            i |= Integer.MIN_VALUE;
            this.assetsSeq = configuration.assetsSeq;
        }
        if (configuration.seq != 0) {
            this.seq = configuration.seq;
        }
        if (this.windowConfiguration.updateFrom(configuration.windowConfiguration) != 0) {
            return i | 536870912;
        }
        return i;
    }

    public int diff(Configuration configuration) {
        return diff(configuration, false, false);
    }

    public int diffPublicOnly(Configuration configuration) {
        return diff(configuration, false, true);
    }

    public int diff(Configuration configuration, boolean z, boolean z2) {
        int i;
        if ((z || configuration.fontScale > 0.0f) && this.fontScale != configuration.fontScale) {
            i = 1073741824;
        } else {
            i = 0;
        }
        if ((z || configuration.mcc != 0) && this.mcc != configuration.mcc) {
            i |= 1;
        }
        if ((z || configuration.mnc != 0) && this.mnc != configuration.mnc) {
            i |= 2;
        }
        fixUpLocaleList();
        configuration.fixUpLocaleList();
        if ((z || !configuration.mLocaleList.isEmpty()) && !this.mLocaleList.equals(configuration.mLocaleList)) {
            i = i | 4 | 8192;
        }
        int i2 = configuration.screenLayout & 192;
        if ((z || i2 != 0) && i2 != (this.screenLayout & 192)) {
            i |= 8192;
        }
        if ((z || configuration.touchscreen != 0) && this.touchscreen != configuration.touchscreen) {
            i |= 8;
        }
        if ((z || configuration.keyboard != 0) && this.keyboard != configuration.keyboard) {
            i |= 16;
        }
        if ((z || configuration.keyboardHidden != 0) && this.keyboardHidden != configuration.keyboardHidden) {
            i |= 32;
        }
        if ((z || configuration.hardKeyboardHidden != 0) && this.hardKeyboardHidden != configuration.hardKeyboardHidden) {
            i |= 32;
        }
        if ((z || configuration.navigation != 0) && this.navigation != configuration.navigation) {
            i |= 64;
        }
        if ((z || configuration.navigationHidden != 0) && this.navigationHidden != configuration.navigationHidden) {
            i |= 32;
        }
        if ((z || configuration.orientation != 0) && this.orientation != configuration.orientation) {
            i |= 128;
        }
        if ((z || getScreenLayoutNoDirection(configuration.screenLayout) != 0) && getScreenLayoutNoDirection(this.screenLayout) != getScreenLayoutNoDirection(configuration.screenLayout)) {
            i |= 256;
        }
        if ((z || (configuration.colorMode & 12) != 0) && (this.colorMode & 12) != (configuration.colorMode & 12)) {
            i |= 16384;
        }
        if ((z || (configuration.colorMode & 3) != 0) && (this.colorMode & 3) != (configuration.colorMode & 3)) {
            i |= 16384;
        }
        if ((z || configuration.uiMode != 0) && this.uiMode != configuration.uiMode) {
            i |= 512;
        }
        if ((z || configuration.screenWidthDp != 0) && this.screenWidthDp != configuration.screenWidthDp) {
            i |= 1024;
        }
        if ((z || configuration.screenHeightDp != 0) && this.screenHeightDp != configuration.screenHeightDp) {
            i |= 1024;
        }
        if ((z || configuration.smallestScreenWidthDp != 0) && this.smallestScreenWidthDp != configuration.smallestScreenWidthDp) {
            i |= 2048;
        }
        if ((z || configuration.densityDpi != 0) && this.densityDpi != configuration.densityDpi) {
            i |= 4096;
        }
        if ((z || configuration.assetsSeq != 0) && this.assetsSeq != configuration.assetsSeq) {
            i |= Integer.MIN_VALUE;
        }
        if (!z2 && this.windowConfiguration.diff(configuration.windowConfiguration, z) != 0) {
            return i | 536870912;
        }
        return i;
    }

    public static boolean needNewResources(int i, int i2) {
        return (i & ((i2 | Integer.MIN_VALUE) | 1073741824)) != 0;
    }

    public boolean isOtherSeqNewer(Configuration configuration) {
        if (configuration == null) {
            return false;
        }
        if (configuration.seq == 0 || this.seq == 0) {
            return true;
        }
        int i = configuration.seq - this.seq;
        return i <= 65536 && i > 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(this.fontScale);
        parcel.writeInt(this.mcc);
        parcel.writeInt(this.mnc);
        fixUpLocaleList();
        parcel.writeParcelable(this.mLocaleList, i);
        if (this.userSetLocale) {
            parcel.writeInt(1);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.touchscreen);
        parcel.writeInt(this.keyboard);
        parcel.writeInt(this.keyboardHidden);
        parcel.writeInt(this.hardKeyboardHidden);
        parcel.writeInt(this.navigation);
        parcel.writeInt(this.navigationHidden);
        parcel.writeInt(this.orientation);
        parcel.writeInt(this.screenLayout);
        parcel.writeInt(this.colorMode);
        parcel.writeInt(this.uiMode);
        parcel.writeInt(this.screenWidthDp);
        parcel.writeInt(this.screenHeightDp);
        parcel.writeInt(this.smallestScreenWidthDp);
        parcel.writeInt(this.densityDpi);
        parcel.writeInt(this.compatScreenWidthDp);
        parcel.writeInt(this.compatScreenHeightDp);
        parcel.writeInt(this.compatSmallestScreenWidthDp);
        parcel.writeValue(this.windowConfiguration);
        parcel.writeInt(this.assetsSeq);
        parcel.writeInt(this.seq);
    }

    public void readFromParcel(Parcel parcel) {
        this.fontScale = parcel.readFloat();
        this.mcc = parcel.readInt();
        this.mnc = parcel.readInt();
        this.mLocaleList = (LocaleList) parcel.readParcelable(LocaleList.class.getClassLoader());
        this.locale = this.mLocaleList.get(0);
        this.userSetLocale = parcel.readInt() == 1;
        this.touchscreen = parcel.readInt();
        this.keyboard = parcel.readInt();
        this.keyboardHidden = parcel.readInt();
        this.hardKeyboardHidden = parcel.readInt();
        this.navigation = parcel.readInt();
        this.navigationHidden = parcel.readInt();
        this.orientation = parcel.readInt();
        this.screenLayout = parcel.readInt();
        this.colorMode = parcel.readInt();
        this.uiMode = parcel.readInt();
        this.screenWidthDp = parcel.readInt();
        this.screenHeightDp = parcel.readInt();
        this.smallestScreenWidthDp = parcel.readInt();
        this.densityDpi = parcel.readInt();
        this.compatScreenWidthDp = parcel.readInt();
        this.compatScreenHeightDp = parcel.readInt();
        this.compatSmallestScreenWidthDp = parcel.readInt();
        this.windowConfiguration.setTo((WindowConfiguration) parcel.readValue(null));
        this.assetsSeq = parcel.readInt();
        this.seq = parcel.readInt();
    }

    private Configuration(Parcel parcel) {
        this.windowConfiguration = new WindowConfiguration();
        readFromParcel(parcel);
    }

    @Override
    public int compareTo(Configuration configuration) {
        float f = this.fontScale;
        float f2 = configuration.fontScale;
        if (f < f2) {
            return -1;
        }
        if (f > f2) {
            return 1;
        }
        int i = this.mcc - configuration.mcc;
        if (i != 0) {
            return i;
        }
        int i2 = this.mnc - configuration.mnc;
        if (i2 != 0) {
            return i2;
        }
        fixUpLocaleList();
        configuration.fixUpLocaleList();
        if (this.mLocaleList.isEmpty()) {
            if (!configuration.mLocaleList.isEmpty()) {
                return 1;
            }
        } else {
            if (configuration.mLocaleList.isEmpty()) {
                return -1;
            }
            int iMin = Math.min(this.mLocaleList.size(), configuration.mLocaleList.size());
            for (int i3 = 0; i3 < iMin; i3++) {
                Locale locale = this.mLocaleList.get(i3);
                Locale locale2 = configuration.mLocaleList.get(i3);
                int iCompareTo = locale.getLanguage().compareTo(locale2.getLanguage());
                if (iCompareTo != 0) {
                    return iCompareTo;
                }
                int iCompareTo2 = locale.getCountry().compareTo(locale2.getCountry());
                if (iCompareTo2 != 0) {
                    return iCompareTo2;
                }
                int iCompareTo3 = locale.getVariant().compareTo(locale2.getVariant());
                if (iCompareTo3 != 0) {
                    return iCompareTo3;
                }
                int iCompareTo4 = locale.toLanguageTag().compareTo(locale2.toLanguageTag());
                if (iCompareTo4 != 0) {
                    return iCompareTo4;
                }
            }
            int size = this.mLocaleList.size() - configuration.mLocaleList.size();
            if (size != 0) {
                return size;
            }
        }
        int i4 = this.touchscreen - configuration.touchscreen;
        if (i4 != 0) {
            return i4;
        }
        int i5 = this.keyboard - configuration.keyboard;
        if (i5 != 0) {
            return i5;
        }
        int i6 = this.keyboardHidden - configuration.keyboardHidden;
        if (i6 != 0) {
            return i6;
        }
        int i7 = this.hardKeyboardHidden - configuration.hardKeyboardHidden;
        if (i7 != 0) {
            return i7;
        }
        int i8 = this.navigation - configuration.navigation;
        if (i8 != 0) {
            return i8;
        }
        int i9 = this.navigationHidden - configuration.navigationHidden;
        if (i9 != 0) {
            return i9;
        }
        int i10 = this.orientation - configuration.orientation;
        if (i10 != 0) {
            return i10;
        }
        int i11 = this.colorMode - configuration.colorMode;
        if (i11 != 0) {
            return i11;
        }
        int i12 = this.screenLayout - configuration.screenLayout;
        if (i12 != 0) {
            return i12;
        }
        int i13 = this.uiMode - configuration.uiMode;
        if (i13 != 0) {
            return i13;
        }
        int i14 = this.screenWidthDp - configuration.screenWidthDp;
        if (i14 != 0) {
            return i14;
        }
        int i15 = this.screenHeightDp - configuration.screenHeightDp;
        if (i15 != 0) {
            return i15;
        }
        int i16 = this.smallestScreenWidthDp - configuration.smallestScreenWidthDp;
        if (i16 != 0) {
            return i16;
        }
        int i17 = this.densityDpi - configuration.densityDpi;
        if (i17 != 0) {
            return i17;
        }
        int i18 = this.assetsSeq - configuration.assetsSeq;
        if (i18 != 0) {
            return i18;
        }
        int iCompareTo22 = this.windowConfiguration.compareTo(configuration.windowConfiguration);
        return iCompareTo22 != 0 ? iCompareTo22 : iCompareTo22;
    }

    public boolean equals(Configuration configuration) {
        if (configuration == null) {
            return false;
        }
        if (configuration != this && compareTo(configuration) != 0) {
            return false;
        }
        return true;
    }

    public boolean equals(Object obj) {
        try {
            return equals((Configuration) obj);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {
        return (31 * (((((((((((((((((((((((((((((((((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + Float.floatToIntBits(this.fontScale)) * 31) + this.mcc) * 31) + this.mnc) * 31) + this.mLocaleList.hashCode()) * 31) + this.touchscreen) * 31) + this.keyboard) * 31) + this.keyboardHidden) * 31) + this.hardKeyboardHidden) * 31) + this.navigation) * 31) + this.navigationHidden) * 31) + this.orientation) * 31) + this.screenLayout) * 31) + this.colorMode) * 31) + this.uiMode) * 31) + this.screenWidthDp) * 31) + this.screenHeightDp) * 31) + this.smallestScreenWidthDp) * 31) + this.densityDpi)) + this.assetsSeq;
    }

    public LocaleList getLocales() {
        fixUpLocaleList();
        return this.mLocaleList;
    }

    public void setLocales(LocaleList localeList) {
        if (localeList == null) {
            localeList = LocaleList.getEmptyLocaleList();
        }
        this.mLocaleList = localeList;
        this.locale = this.mLocaleList.get(0);
        setLayoutDirection(this.locale);
    }

    public void setLocale(Locale locale) {
        setLocales(locale == null ? LocaleList.getEmptyLocaleList() : new LocaleList(locale));
    }

    public void clearLocales() {
        this.mLocaleList = LocaleList.getEmptyLocaleList();
        this.locale = null;
    }

    public int getLayoutDirection() {
        return (this.screenLayout & 192) == 128 ? 1 : 0;
    }

    public void setLayoutDirection(Locale locale) {
        this.screenLayout = (this.screenLayout & (-193)) | ((1 + TextUtils.getLayoutDirectionFromLocale(locale)) << 6);
    }

    private static int getScreenLayoutNoDirection(int i) {
        return i & (-193);
    }

    public boolean isScreenRound() {
        return (this.screenLayout & 768) == 512;
    }

    public boolean isScreenWideColorGamut() {
        return (this.colorMode & 3) == 2;
    }

    public boolean isScreenHdr() {
        return (this.colorMode & 12) == 8;
    }

    public static String localesToResourceQualifier(LocaleList localeList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < localeList.size(); i++) {
            Locale locale = localeList.get(i);
            int length = locale.getLanguage().length();
            if (length != 0) {
                int length2 = locale.getScript().length();
                int length3 = locale.getCountry().length();
                int length4 = locale.getVariant().length();
                if (sb.length() != 0) {
                    sb.append(",");
                }
                if (length == 2 && length2 == 0 && ((length3 == 0 || length3 == 2) && length4 == 0)) {
                    sb.append(locale.getLanguage());
                    if (length3 == 2) {
                        sb.append("-r");
                        sb.append(locale.getCountry());
                    }
                } else {
                    sb.append("b+");
                    sb.append(locale.getLanguage());
                    if (length2 != 0) {
                        sb.append("+");
                        sb.append(locale.getScript());
                    }
                    if (length3 != 0) {
                        sb.append("+");
                        sb.append(locale.getCountry());
                    }
                    if (length4 != 0) {
                        sb.append("+");
                        sb.append(locale.getVariant());
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String resourceQualifierString(Configuration configuration) {
        return resourceQualifierString(configuration, null);
    }

    public static String resourceQualifierString(Configuration configuration, DisplayMetrics displayMetrics) {
        int i;
        int i2;
        ArrayList arrayList = new ArrayList();
        if (configuration.mcc != 0) {
            arrayList.add("mcc" + configuration.mcc);
            if (configuration.mnc != 0) {
                arrayList.add("mnc" + configuration.mnc);
            }
        }
        if (!configuration.mLocaleList.isEmpty()) {
            String strLocalesToResourceQualifier = localesToResourceQualifier(configuration.mLocaleList);
            if (!strLocalesToResourceQualifier.isEmpty()) {
                arrayList.add(strLocalesToResourceQualifier);
            }
        }
        int i3 = configuration.screenLayout & 192;
        if (i3 == 64) {
            arrayList.add("ldltr");
        } else if (i3 == 128) {
            arrayList.add("ldrtl");
        }
        if (configuration.smallestScreenWidthDp != 0) {
            arrayList.add(XML_ATTR_SMALLEST_WIDTH + configuration.smallestScreenWidthDp + "dp");
        }
        if (configuration.screenWidthDp != 0) {
            arrayList.add("w" + configuration.screenWidthDp + "dp");
        }
        if (configuration.screenHeightDp != 0) {
            arrayList.add("h" + configuration.screenHeightDp + "dp");
        }
        switch (configuration.screenLayout & 15) {
            case 1:
                arrayList.add("small");
                break;
            case 2:
                arrayList.add("normal");
                break;
            case 3:
                arrayList.add(Slice.HINT_LARGE);
                break;
            case 4:
                arrayList.add("xlarge");
                break;
        }
        int i4 = configuration.screenLayout & 48;
        if (i4 == 16) {
            arrayList.add("notlong");
        } else if (i4 == 32) {
            arrayList.add("long");
        }
        int i5 = configuration.screenLayout & 768;
        if (i5 == 256) {
            arrayList.add("notround");
        } else if (i5 == 512) {
            arrayList.add("round");
        }
        int i6 = configuration.colorMode & 12;
        if (i6 == 4) {
            arrayList.add("lowdr");
        } else if (i6 == 8) {
            arrayList.add("highdr");
        }
        switch (configuration.colorMode & 3) {
            case 1:
                arrayList.add("nowidecg");
                break;
            case 2:
                arrayList.add("widecg");
                break;
        }
        switch (configuration.orientation) {
            case 1:
                arrayList.add("port");
                break;
            case 2:
                arrayList.add("land");
                break;
        }
        switch (configuration.uiMode & 15) {
            case 2:
                arrayList.add("desk");
                break;
            case 3:
                arrayList.add("car");
                break;
            case 4:
                arrayList.add("television");
                break;
            case 5:
                arrayList.add("appliance");
                break;
            case 6:
                arrayList.add("watch");
                break;
            case 7:
                arrayList.add("vrheadset");
                break;
        }
        int i7 = configuration.uiMode & 48;
        if (i7 == 16) {
            arrayList.add("notnight");
        } else if (i7 == 32) {
            arrayList.add(Camera.Parameters.SCENE_MODE_NIGHT);
        }
        int i8 = configuration.densityDpi;
        if (i8 != 0) {
            if (i8 == 120) {
                arrayList.add("ldpi");
            } else if (i8 == 160) {
                arrayList.add("mdpi");
            } else if (i8 == 213) {
                arrayList.add("tvdpi");
            } else if (i8 == 240) {
                arrayList.add("hdpi");
            } else if (i8 == 320) {
                arrayList.add("xhdpi");
            } else if (i8 == 480) {
                arrayList.add("xxhdpi");
            } else if (i8 == 640) {
                arrayList.add("xxxhdpi");
            } else {
                switch (i8) {
                    case 65534:
                        arrayList.add("anydpi");
                        break;
                    case 65535:
                        arrayList.add("nodpi");
                        break;
                    default:
                        arrayList.add(configuration.densityDpi + "dpi");
                        break;
                }
            }
        }
        int i9 = configuration.touchscreen;
        if (i9 == 1) {
            arrayList.add("notouch");
        } else if (i9 == 3) {
            arrayList.add("finger");
        }
        switch (configuration.keyboardHidden) {
            case 1:
                arrayList.add("keysexposed");
                break;
            case 2:
                arrayList.add("keyshidden");
                break;
            case 3:
                arrayList.add("keyssoft");
                break;
        }
        switch (configuration.keyboard) {
            case 1:
                arrayList.add("nokeys");
                break;
            case 2:
                arrayList.add("qwerty");
                break;
            case 3:
                arrayList.add("12key");
                break;
        }
        switch (configuration.navigationHidden) {
            case 1:
                arrayList.add("navexposed");
                break;
            case 2:
                arrayList.add("navhidden");
                break;
        }
        switch (configuration.navigation) {
            case 1:
                arrayList.add("nonav");
                break;
            case 2:
                arrayList.add("dpad");
                break;
            case 3:
                arrayList.add("trackball");
                break;
            case 4:
                arrayList.add("wheel");
                break;
        }
        if (displayMetrics != null) {
            if (displayMetrics.widthPixels >= displayMetrics.heightPixels) {
                i = displayMetrics.widthPixels;
                i2 = displayMetrics.heightPixels;
            } else {
                i = displayMetrics.heightPixels;
                i2 = displayMetrics.widthPixels;
            }
            arrayList.add(i + "x" + i2);
        }
        arrayList.add(Telephony.BaseMmsColumns.MMS_VERSION + Build.VERSION.RESOURCES_SDK_INT);
        return TextUtils.join(NativeLibraryHelper.CLEAR_ABI_OVERRIDE, arrayList);
    }

    public static Configuration generateDelta(Configuration configuration, Configuration configuration2) {
        Configuration configuration3 = new Configuration();
        if (configuration.fontScale != configuration2.fontScale) {
            configuration3.fontScale = configuration2.fontScale;
        }
        if (configuration.mcc != configuration2.mcc) {
            configuration3.mcc = configuration2.mcc;
        }
        if (configuration.mnc != configuration2.mnc) {
            configuration3.mnc = configuration2.mnc;
        }
        configuration.fixUpLocaleList();
        configuration2.fixUpLocaleList();
        if (!configuration.mLocaleList.equals(configuration2.mLocaleList)) {
            configuration3.mLocaleList = configuration2.mLocaleList;
            configuration3.locale = configuration2.locale;
        }
        if (configuration.touchscreen != configuration2.touchscreen) {
            configuration3.touchscreen = configuration2.touchscreen;
        }
        if (configuration.keyboard != configuration2.keyboard) {
            configuration3.keyboard = configuration2.keyboard;
        }
        if (configuration.keyboardHidden != configuration2.keyboardHidden) {
            configuration3.keyboardHidden = configuration2.keyboardHidden;
        }
        if (configuration.navigation != configuration2.navigation) {
            configuration3.navigation = configuration2.navigation;
        }
        if (configuration.navigationHidden != configuration2.navigationHidden) {
            configuration3.navigationHidden = configuration2.navigationHidden;
        }
        if (configuration.orientation != configuration2.orientation) {
            configuration3.orientation = configuration2.orientation;
        }
        if ((configuration.screenLayout & 15) != (configuration2.screenLayout & 15)) {
            configuration3.screenLayout |= configuration2.screenLayout & 15;
        }
        if ((configuration.screenLayout & 192) != (configuration2.screenLayout & 192)) {
            configuration3.screenLayout |= configuration2.screenLayout & 192;
        }
        if ((configuration.screenLayout & 48) != (configuration2.screenLayout & 48)) {
            configuration3.screenLayout |= configuration2.screenLayout & 48;
        }
        if ((configuration.screenLayout & 768) != (configuration2.screenLayout & 768)) {
            configuration3.screenLayout |= configuration2.screenLayout & 768;
        }
        if ((configuration.colorMode & 3) != (configuration2.colorMode & 3)) {
            configuration3.colorMode |= configuration2.colorMode & 3;
        }
        if ((configuration.colorMode & 12) != (configuration2.colorMode & 12)) {
            configuration3.colorMode |= configuration2.colorMode & 12;
        }
        if ((configuration.uiMode & 15) != (configuration2.uiMode & 15)) {
            configuration3.uiMode |= configuration2.uiMode & 15;
        }
        if ((configuration.uiMode & 48) != (configuration2.uiMode & 48)) {
            configuration3.uiMode |= configuration2.uiMode & 48;
        }
        if (configuration.screenWidthDp != configuration2.screenWidthDp) {
            configuration3.screenWidthDp = configuration2.screenWidthDp;
        }
        if (configuration.screenHeightDp != configuration2.screenHeightDp) {
            configuration3.screenHeightDp = configuration2.screenHeightDp;
        }
        if (configuration.smallestScreenWidthDp != configuration2.smallestScreenWidthDp) {
            configuration3.smallestScreenWidthDp = configuration2.smallestScreenWidthDp;
        }
        if (configuration.densityDpi != configuration2.densityDpi) {
            configuration3.densityDpi = configuration2.densityDpi;
        }
        if (configuration.assetsSeq != configuration2.assetsSeq) {
            configuration3.assetsSeq = configuration2.assetsSeq;
        }
        if (!configuration.windowConfiguration.equals(configuration2.windowConfiguration)) {
            configuration3.windowConfiguration.setTo(configuration2.windowConfiguration);
        }
        return configuration3;
    }

    public static void readXmlAttrs(XmlPullParser xmlPullParser, Configuration configuration) throws XmlPullParserException, IOException {
        configuration.fontScale = Float.intBitsToFloat(XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_FONT_SCALE, 0));
        configuration.mcc = XmlUtils.readIntAttribute(xmlPullParser, "mcc", 0);
        configuration.mnc = XmlUtils.readIntAttribute(xmlPullParser, "mnc", 0);
        configuration.mLocaleList = LocaleList.forLanguageTags(XmlUtils.readStringAttribute(xmlPullParser, XML_ATTR_LOCALES));
        configuration.locale = configuration.mLocaleList.get(0);
        configuration.touchscreen = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_TOUCHSCREEN, 0);
        configuration.keyboard = XmlUtils.readIntAttribute(xmlPullParser, "key", 0);
        configuration.keyboardHidden = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_KEYBOARD_HIDDEN, 0);
        configuration.hardKeyboardHidden = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_HARD_KEYBOARD_HIDDEN, 0);
        configuration.navigation = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_NAVIGATION, 0);
        configuration.navigationHidden = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_NAVIGATION_HIDDEN, 0);
        configuration.orientation = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_ORIENTATION, 0);
        configuration.screenLayout = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_SCREEN_LAYOUT, 0);
        configuration.colorMode = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_COLOR_MODE, 0);
        configuration.uiMode = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_UI_MODE, 0);
        configuration.screenWidthDp = XmlUtils.readIntAttribute(xmlPullParser, "width", 0);
        configuration.screenHeightDp = XmlUtils.readIntAttribute(xmlPullParser, "height", 0);
        configuration.smallestScreenWidthDp = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_SMALLEST_WIDTH, 0);
        configuration.densityDpi = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_DENSITY, 0);
    }

    public static void writeXmlAttrs(XmlSerializer xmlSerializer, Configuration configuration) throws IOException {
        XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_FONT_SCALE, Float.floatToIntBits(configuration.fontScale));
        if (configuration.mcc != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, "mcc", configuration.mcc);
        }
        if (configuration.mnc != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, "mnc", configuration.mnc);
        }
        configuration.fixUpLocaleList();
        if (!configuration.mLocaleList.isEmpty()) {
            XmlUtils.writeStringAttribute(xmlSerializer, XML_ATTR_LOCALES, configuration.mLocaleList.toLanguageTags());
        }
        if (configuration.touchscreen != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_TOUCHSCREEN, configuration.touchscreen);
        }
        if (configuration.keyboard != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, "key", configuration.keyboard);
        }
        if (configuration.keyboardHidden != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_KEYBOARD_HIDDEN, configuration.keyboardHidden);
        }
        if (configuration.hardKeyboardHidden != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_HARD_KEYBOARD_HIDDEN, configuration.hardKeyboardHidden);
        }
        if (configuration.navigation != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_NAVIGATION, configuration.navigation);
        }
        if (configuration.navigationHidden != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_NAVIGATION_HIDDEN, configuration.navigationHidden);
        }
        if (configuration.orientation != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_ORIENTATION, configuration.orientation);
        }
        if (configuration.screenLayout != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_SCREEN_LAYOUT, configuration.screenLayout);
        }
        if (configuration.colorMode != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_COLOR_MODE, configuration.colorMode);
        }
        if (configuration.uiMode != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_UI_MODE, configuration.uiMode);
        }
        if (configuration.screenWidthDp != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, "width", configuration.screenWidthDp);
        }
        if (configuration.screenHeightDp != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, "height", configuration.screenHeightDp);
        }
        if (configuration.smallestScreenWidthDp != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_SMALLEST_WIDTH, configuration.smallestScreenWidthDp);
        }
        if (configuration.densityDpi != 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, XML_ATTR_DENSITY, configuration.densityDpi);
        }
    }
}
