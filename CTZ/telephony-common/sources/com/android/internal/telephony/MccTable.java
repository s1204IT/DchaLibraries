package com.android.internal.telephony;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.radio.V1_0.RadioError;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.app.LocaleStore;
import com.android.internal.telephony.cat.BerTlv;
import com.google.android.mms.pdu.PduHeaders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import libcore.icu.ICU;
import libcore.util.TimeZoneFinder;

public final class MccTable {
    private static final Map<Locale, Locale> FALLBACKS = new HashMap();
    static final String LOG_TAG = "MccTable";
    static ArrayList<MccEntry> sTable;

    static class MccEntry implements Comparable<MccEntry> {
        final String mIso;
        final int mMcc;
        final int mSmallestDigitsMnc;

        MccEntry(int i, String str, int i2) {
            if (str == null) {
                throw new NullPointerException();
            }
            this.mMcc = i;
            this.mIso = str;
            this.mSmallestDigitsMnc = i2;
        }

        @Override
        public int compareTo(MccEntry mccEntry) {
            return this.mMcc - mccEntry.mMcc;
        }
    }

    private static MccEntry entryForMcc(int i) {
        int iBinarySearch = Collections.binarySearch(sTable, new MccEntry(i, "", 0));
        if (iBinarySearch < 0) {
            return null;
        }
        return sTable.get(iBinarySearch);
    }

    public static String defaultTimeZoneForMcc(int i) {
        MccEntry mccEntryEntryForMcc = entryForMcc(i);
        if (mccEntryEntryForMcc == null) {
            return null;
        }
        return TimeZoneFinder.getInstance().lookupDefaultTimeZoneIdByCountry(mccEntryEntryForMcc.mIso);
    }

    public static String countryCodeForMcc(int i) {
        MccEntry mccEntryEntryForMcc = entryForMcc(i);
        if (mccEntryEntryForMcc == null) {
            return "";
        }
        return mccEntryEntryForMcc.mIso;
    }

    public static String defaultLanguageForMcc(int i) {
        MccEntry mccEntryEntryForMcc = entryForMcc(i);
        if (mccEntryEntryForMcc == null) {
            Slog.d(LOG_TAG, "defaultLanguageForMcc(" + i + "): no country for mcc");
            return null;
        }
        String str = mccEntryEntryForMcc.mIso;
        if ("in".equals(str)) {
            return "en";
        }
        String language = ICU.addLikelySubtags(new Locale("und", str)).getLanguage();
        Slog.d(LOG_TAG, "defaultLanguageForMcc(" + i + "): country " + str + " uses " + language);
        return language;
    }

    public static int smallestDigitsMccForMnc(int i) {
        MccEntry mccEntryEntryForMcc = entryForMcc(i);
        if (mccEntryEntryForMcc == null) {
            return 2;
        }
        return mccEntryEntryForMcc.mSmallestDigitsMnc;
    }

    public static void updateMccMncConfiguration(Context context, String str, boolean z) {
        Slog.d(LOG_TAG, "updateMccMncConfiguration mccmnc='" + str + "' fromServiceState=" + z);
        if (Build.IS_DEBUGGABLE) {
            String str2 = SystemProperties.get("persist.sys.override_mcc");
            if (!TextUtils.isEmpty(str2)) {
                Slog.d(LOG_TAG, "updateMccMncConfiguration overriding mccmnc='" + str2 + "'");
                str = str2;
            }
        }
        boolean z2 = false;
        if (!TextUtils.isEmpty(str)) {
            Slog.d(LOG_TAG, "updateMccMncConfiguration defaultMccMnc=" + TelephonyManager.getDefault().getSimOperatorNumeric());
            try {
                int i = Integer.parseInt(str.substring(0, 3));
                int i2 = Integer.parseInt(str.substring(3));
                Slog.d(LOG_TAG, "updateMccMncConfiguration: mcc=" + i + ", mnc=" + i2);
                if (i != 0) {
                    setTimezoneFromMccIfNeeded(context, i);
                }
                if (z) {
                    setWifiCountryCodeFromMcc(context, i);
                    return;
                }
                try {
                    Configuration configuration = new Configuration();
                    if (i != 0) {
                        configuration.mcc = i;
                        if (i2 == 0) {
                            i2 = 65535;
                        }
                        configuration.mnc = i2;
                        z2 = true;
                    }
                    if (z2) {
                        Slog.d(LOG_TAG, "updateMccMncConfiguration updateConfig config=" + configuration);
                        ActivityManager.getService().updateConfiguration(configuration);
                        return;
                    }
                    Slog.d(LOG_TAG, "updateMccMncConfiguration nothing to update");
                    return;
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Can't update configuration", e);
                    return;
                }
            } catch (NumberFormatException e2) {
                Slog.e(LOG_TAG, "Error parsing IMSI: " + str);
                return;
            }
        }
        if (z) {
            setWifiCountryCodeFromMcc(context, 0);
        }
    }

    static {
        FALLBACKS.put(Locale.ENGLISH, Locale.US);
        sTable = new ArrayList<>(240);
        sTable.add(new MccEntry(202, "gr", 2));
        sTable.add(new MccEntry(204, "nl", 2));
        sTable.add(new MccEntry(206, "be", 2));
        sTable.add(new MccEntry(BerTlv.BER_PROACTIVE_COMMAND_TAG, "fr", 2));
        sTable.add(new MccEntry(CommandsInterface.GSM_SMS_FAIL_CAUSE_USIM_APP_TOOLKIT_BUSY, "mc", 2));
        sTable.add(new MccEntry(CommandsInterface.GSM_SMS_FAIL_CAUSE_USIM_DATA_DOWNLOAD_ERROR, "ad", 2));
        sTable.add(new MccEntry(BerTlv.BER_EVENT_DOWNLOAD_TAG, "es", 2));
        sTable.add(new MccEntry(216, "hu", 2));
        sTable.add(new MccEntry(218, "ba", 2));
        sTable.add(new MccEntry(219, "hr", 2));
        sTable.add(new MccEntry(220, "rs", 2));
        sTable.add(new MccEntry(222, "it", 2));
        sTable.add(new MccEntry(225, "va", 2));
        sTable.add(new MccEntry(226, "ro", 2));
        sTable.add(new MccEntry(228, "ch", 2));
        sTable.add(new MccEntry(230, "cz", 2));
        sTable.add(new MccEntry(231, "sk", 2));
        sTable.add(new MccEntry(PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_REPLY_CHARGING_FORWARDING_DENIED, "at", 2));
        sTable.add(new MccEntry(PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_ADDRESS_HIDING_NOT_SUPPORTED, "gb", 2));
        sTable.add(new MccEntry(PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_LACK_OF_PREPAID, "gb", 2));
        sTable.add(new MccEntry(238, "dk", 2));
        sTable.add(new MccEntry(240, "se", 2));
        sTable.add(new MccEntry(android.hardware.radio.V1_0.LastCallFailCause.IMSI_UNKNOWN_IN_VLR, "no", 2));
        sTable.add(new MccEntry(244, "fi", 2));
        sTable.add(new MccEntry(246, "lt", 2));
        sTable.add(new MccEntry(android.hardware.radio.V1_0.LastCallFailCause.RADIO_OFF, "lv", 2));
        sTable.add(new MccEntry(android.hardware.radio.V1_0.LastCallFailCause.OUT_OF_SERVICE, "ee", 2));
        sTable.add(new MccEntry(android.hardware.radio.V1_0.LastCallFailCause.RADIO_INTERNAL_ERROR, "ru", 2));
        sTable.add(new MccEntry(255, "ua", 2));
        sTable.add(new MccEntry(android.hardware.radio.V1_0.LastCallFailCause.RADIO_SETUP_FAILURE, "by", 2));
        sTable.add(new MccEntry(android.hardware.radio.V1_0.LastCallFailCause.RADIO_RELEASE_ABNORMAL, "md", 2));
        sTable.add(new MccEntry(android.hardware.radio.V1_0.LastCallFailCause.ACCESS_CLASS_BLOCKED, "pl", 2));
        sTable.add(new MccEntry(262, "de", 2));
        sTable.add(new MccEntry(266, "gi", 2));
        sTable.add(new MccEntry(268, "pt", 2));
        sTable.add(new MccEntry(270, "lu", 2));
        sTable.add(new MccEntry(272, "ie", 2));
        sTable.add(new MccEntry(274, "is", 2));
        sTable.add(new MccEntry(276, "al", 2));
        sTable.add(new MccEntry(278, "mt", 2));
        sTable.add(new MccEntry(280, "cy", 2));
        sTable.add(new MccEntry(282, "ge", 2));
        sTable.add(new MccEntry(283, "am", 2));
        sTable.add(new MccEntry(284, "bg", 2));
        sTable.add(new MccEntry(286, "tr", 2));
        sTable.add(new MccEntry(288, "fo", 2));
        sTable.add(new MccEntry(289, "ge", 2));
        sTable.add(new MccEntry(290, "gl", 2));
        sTable.add(new MccEntry(292, "sm", 2));
        sTable.add(new MccEntry(293, "si", 2));
        sTable.add(new MccEntry(294, "mk", 2));
        sTable.add(new MccEntry(295, "li", 2));
        sTable.add(new MccEntry(297, "me", 2));
        sTable.add(new MccEntry(302, "ca", 3));
        sTable.add(new MccEntry(308, "pm", 2));
        sTable.add(new MccEntry(310, "us", 3));
        sTable.add(new MccEntry(311, "us", 3));
        sTable.add(new MccEntry(312, "us", 3));
        sTable.add(new MccEntry(313, "us", 3));
        sTable.add(new MccEntry(314, "us", 3));
        sTable.add(new MccEntry(315, "us", 3));
        sTable.add(new MccEntry(316, "us", 3));
        sTable.add(new MccEntry(330, "pr", 2));
        sTable.add(new MccEntry(332, "vi", 2));
        sTable.add(new MccEntry(334, "mx", 3));
        sTable.add(new MccEntry(338, "jm", 3));
        sTable.add(new MccEntry(340, "gp", 2));
        sTable.add(new MccEntry(342, "bb", 3));
        sTable.add(new MccEntry(344, "ag", 3));
        sTable.add(new MccEntry(346, "ky", 3));
        sTable.add(new MccEntry(348, "vg", 3));
        sTable.add(new MccEntry(350, "bm", 2));
        sTable.add(new MccEntry(352, "gd", 2));
        sTable.add(new MccEntry(354, "ms", 2));
        sTable.add(new MccEntry(356, "kn", 2));
        sTable.add(new MccEntry(358, "lc", 2));
        sTable.add(new MccEntry(360, "vc", 2));
        sTable.add(new MccEntry(362, "ai", 2));
        sTable.add(new MccEntry(363, "aw", 2));
        sTable.add(new MccEntry(364, "bs", 2));
        sTable.add(new MccEntry(365, "ai", 3));
        sTable.add(new MccEntry(366, "dm", 2));
        sTable.add(new MccEntry(368, "cu", 2));
        sTable.add(new MccEntry(370, "do", 2));
        sTable.add(new MccEntry(372, "ht", 2));
        sTable.add(new MccEntry(374, "tt", 2));
        sTable.add(new MccEntry(376, "tc", 2));
        sTable.add(new MccEntry(400, "az", 2));
        sTable.add(new MccEntry(401, "kz", 2));
        sTable.add(new MccEntry(402, "bt", 2));
        sTable.add(new MccEntry(404, "in", 2));
        sTable.add(new MccEntry(405, "in", 2));
        sTable.add(new MccEntry(406, "in", 2));
        sTable.add(new MccEntry(410, "pk", 2));
        sTable.add(new MccEntry(412, "af", 2));
        sTable.add(new MccEntry(413, "lk", 2));
        sTable.add(new MccEntry(414, "mm", 2));
        sTable.add(new MccEntry(415, "lb", 2));
        sTable.add(new MccEntry(416, "jo", 2));
        sTable.add(new MccEntry(417, "sy", 2));
        sTable.add(new MccEntry(418, "iq", 2));
        sTable.add(new MccEntry(419, "kw", 2));
        sTable.add(new MccEntry(420, "sa", 2));
        sTable.add(new MccEntry(421, "ye", 2));
        sTable.add(new MccEntry(422, "om", 2));
        sTable.add(new MccEntry(423, "ps", 2));
        sTable.add(new MccEntry(424, "ae", 2));
        sTable.add(new MccEntry(425, "il", 2));
        sTable.add(new MccEntry(426, "bh", 2));
        sTable.add(new MccEntry(427, "qa", 2));
        sTable.add(new MccEntry(428, "mn", 2));
        sTable.add(new MccEntry(429, "np", 2));
        sTable.add(new MccEntry(430, "ae", 2));
        sTable.add(new MccEntry(431, "ae", 2));
        sTable.add(new MccEntry(432, "ir", 2));
        sTable.add(new MccEntry(434, "uz", 2));
        sTable.add(new MccEntry(436, "tj", 2));
        sTable.add(new MccEntry(437, "kg", 2));
        sTable.add(new MccEntry(438, "tm", 2));
        sTable.add(new MccEntry(440, "jp", 2));
        sTable.add(new MccEntry(441, "jp", 2));
        sTable.add(new MccEntry(450, "kr", 2));
        sTable.add(new MccEntry(452, "vn", 2));
        sTable.add(new MccEntry(454, "hk", 2));
        sTable.add(new MccEntry(455, "mo", 2));
        sTable.add(new MccEntry(456, "kh", 2));
        sTable.add(new MccEntry(457, "la", 2));
        sTable.add(new MccEntry(460, "cn", 2));
        sTable.add(new MccEntry(461, "cn", 2));
        sTable.add(new MccEntry(466, "tw", 2));
        sTable.add(new MccEntry(467, "kp", 2));
        sTable.add(new MccEntry(470, "bd", 2));
        sTable.add(new MccEntry(472, "mv", 2));
        sTable.add(new MccEntry(RadioError.OEM_ERROR_2, "my", 2));
        sTable.add(new MccEntry(RadioError.OEM_ERROR_5, "au", 2));
        sTable.add(new MccEntry(RadioError.OEM_ERROR_10, "id", 2));
        sTable.add(new MccEntry(RadioError.OEM_ERROR_14, "tl", 2));
        sTable.add(new MccEntry(RadioError.OEM_ERROR_15, "ph", 2));
        sTable.add(new MccEntry(RadioError.OEM_ERROR_20, "th", 2));
        sTable.add(new MccEntry(RadioError.OEM_ERROR_25, "sg", 2));
        sTable.add(new MccEntry(528, "bn", 2));
        sTable.add(new MccEntry(530, "nz", 2));
        sTable.add(new MccEntry(534, "mp", 2));
        sTable.add(new MccEntry(535, "gu", 2));
        sTable.add(new MccEntry(536, "nr", 2));
        sTable.add(new MccEntry(537, "pg", 2));
        sTable.add(new MccEntry(539, "to", 2));
        sTable.add(new MccEntry(540, "sb", 2));
        sTable.add(new MccEntry(541, "vu", 2));
        sTable.add(new MccEntry(542, "fj", 2));
        sTable.add(new MccEntry(543, "wf", 2));
        sTable.add(new MccEntry(544, "as", 2));
        sTable.add(new MccEntry(545, "ki", 2));
        sTable.add(new MccEntry(546, "nc", 2));
        sTable.add(new MccEntry(547, "pf", 2));
        sTable.add(new MccEntry(548, "ck", 2));
        sTable.add(new MccEntry(549, "ws", 2));
        sTable.add(new MccEntry(550, "fm", 2));
        sTable.add(new MccEntry(551, "mh", 2));
        sTable.add(new MccEntry(552, "pw", 2));
        sTable.add(new MccEntry(553, "tv", 2));
        sTable.add(new MccEntry(555, "nu", 2));
        sTable.add(new MccEntry(602, "eg", 2));
        sTable.add(new MccEntry(603, "dz", 2));
        sTable.add(new MccEntry(604, "ma", 2));
        sTable.add(new MccEntry(605, "tn", 2));
        sTable.add(new MccEntry(606, "ly", 2));
        sTable.add(new MccEntry(607, "gm", 2));
        sTable.add(new MccEntry(608, "sn", 2));
        sTable.add(new MccEntry(609, "mr", 2));
        sTable.add(new MccEntry(610, "ml", 2));
        sTable.add(new MccEntry(611, "gn", 2));
        sTable.add(new MccEntry(612, "ci", 2));
        sTable.add(new MccEntry(613, "bf", 2));
        sTable.add(new MccEntry(614, "ne", 2));
        sTable.add(new MccEntry(615, "tg", 2));
        sTable.add(new MccEntry(616, "bj", 2));
        sTable.add(new MccEntry(617, "mu", 2));
        sTable.add(new MccEntry(618, "lr", 2));
        sTable.add(new MccEntry(619, "sl", 2));
        sTable.add(new MccEntry(620, "gh", 2));
        sTable.add(new MccEntry(621, "ng", 2));
        sTable.add(new MccEntry(622, "td", 2));
        sTable.add(new MccEntry(623, "cf", 2));
        sTable.add(new MccEntry(624, "cm", 2));
        sTable.add(new MccEntry(625, "cv", 2));
        sTable.add(new MccEntry(626, "st", 2));
        sTable.add(new MccEntry(627, "gq", 2));
        sTable.add(new MccEntry(628, "ga", 2));
        sTable.add(new MccEntry(629, "cg", 2));
        sTable.add(new MccEntry(630, "cd", 2));
        sTable.add(new MccEntry(631, "ao", 2));
        sTable.add(new MccEntry(632, "gw", 2));
        sTable.add(new MccEntry(633, "sc", 2));
        sTable.add(new MccEntry(634, "sd", 2));
        sTable.add(new MccEntry(635, "rw", 2));
        sTable.add(new MccEntry(636, "et", 2));
        sTable.add(new MccEntry(637, "so", 2));
        sTable.add(new MccEntry(638, "dj", 2));
        sTable.add(new MccEntry(639, "ke", 2));
        sTable.add(new MccEntry(640, "tz", 2));
        sTable.add(new MccEntry(641, "ug", 2));
        sTable.add(new MccEntry(642, "bi", 2));
        sTable.add(new MccEntry(643, "mz", 2));
        sTable.add(new MccEntry(645, "zm", 2));
        sTable.add(new MccEntry(646, "mg", 2));
        sTable.add(new MccEntry(647, "re", 2));
        sTable.add(new MccEntry(648, "zw", 2));
        sTable.add(new MccEntry(649, "na", 2));
        sTable.add(new MccEntry(650, "mw", 2));
        sTable.add(new MccEntry(651, "ls", 2));
        sTable.add(new MccEntry(652, "bw", 2));
        sTable.add(new MccEntry(653, "sz", 2));
        sTable.add(new MccEntry(654, "km", 2));
        sTable.add(new MccEntry(655, "za", 2));
        sTable.add(new MccEntry(657, "er", 2));
        sTable.add(new MccEntry(658, "sh", 2));
        sTable.add(new MccEntry(659, "ss", 2));
        sTable.add(new MccEntry(702, "bz", 2));
        sTable.add(new MccEntry(704, "gt", 2));
        sTable.add(new MccEntry(706, "sv", 2));
        sTable.add(new MccEntry(708, "hn", 3));
        sTable.add(new MccEntry(710, "ni", 2));
        sTable.add(new MccEntry(712, "cr", 2));
        sTable.add(new MccEntry(714, "pa", 2));
        sTable.add(new MccEntry(716, "pe", 2));
        sTable.add(new MccEntry(722, "ar", 3));
        sTable.add(new MccEntry(724, "br", 2));
        sTable.add(new MccEntry(730, "cl", 2));
        sTable.add(new MccEntry(732, "co", 3));
        sTable.add(new MccEntry(734, "ve", 2));
        sTable.add(new MccEntry(736, "bo", 2));
        sTable.add(new MccEntry(738, "gy", 2));
        sTable.add(new MccEntry(740, "ec", 2));
        sTable.add(new MccEntry(742, "gf", 2));
        sTable.add(new MccEntry(744, "py", 2));
        sTable.add(new MccEntry(746, "sr", 2));
        sTable.add(new MccEntry(748, "uy", 2));
        sTable.add(new MccEntry(750, "fk", 2));
        Collections.sort(sTable);
    }

    private static Locale lookupFallback(Locale locale, List<Locale> list) {
        do {
            locale = FALLBACKS.get(locale);
            if (locale == null) {
                return null;
            }
        } while (!list.contains(locale));
        return locale;
    }

    private static Locale getLocaleForLanguageCountry(Context context, String str, String str2) {
        if (str == null) {
            Slog.d(LOG_TAG, "getLocaleForLanguageCountry: skipping no language");
            return null;
        }
        if (str2 == null) {
            str2 = "";
        }
        Locale locale = new Locale(str, str2);
        try {
            ArrayList arrayList = new ArrayList(Arrays.asList(context.getAssets().getLocales()));
            arrayList.remove("ar-XB");
            arrayList.remove("en-XA");
            ArrayList arrayList2 = new ArrayList();
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                Locale localeForLanguageTag = Locale.forLanguageTag(((String) it.next()).replace('_', '-'));
                if (localeForLanguageTag != null && !"und".equals(localeForLanguageTag.getLanguage()) && !localeForLanguageTag.getLanguage().isEmpty() && !localeForLanguageTag.getCountry().isEmpty()) {
                    if (localeForLanguageTag.getLanguage().equals(locale.getLanguage())) {
                        if (localeForLanguageTag.getCountry().equals(locale.getCountry())) {
                            Slog.d(LOG_TAG, "getLocaleForLanguageCountry: got perfect match: " + localeForLanguageTag.toLanguageTag());
                            return localeForLanguageTag;
                        }
                        arrayList2.add(localeForLanguageTag);
                    }
                }
            }
            if (arrayList2.isEmpty()) {
                Slog.d(LOG_TAG, "getLocaleForLanguageCountry: no locales for language " + str);
                return null;
            }
            Locale localeLookupFallback = lookupFallback(locale, arrayList2);
            if (localeLookupFallback != null) {
                Slog.d(LOG_TAG, "getLocaleForLanguageCountry: got a fallback match: " + localeLookupFallback.toLanguageTag());
                return localeLookupFallback;
            }
            if (!TextUtils.isEmpty(locale.getCountry())) {
                LocaleStore.fillCache(context);
                if (LocaleStore.getLocaleInfo(locale).isTranslated()) {
                    Slog.d(LOG_TAG, "getLocaleForLanguageCountry: target locale is translated: " + locale);
                    return locale;
                }
            }
            Slog.d(LOG_TAG, "getLocaleForLanguageCountry: got language-only match: " + str);
            return (Locale) arrayList2.get(0);
        } catch (Exception e) {
            Slog.d(LOG_TAG, "getLocaleForLanguageCountry: exception", e);
            return null;
        }
    }

    private static void setTimezoneFromMccIfNeeded(Context context, int i) {
        String strDefaultTimeZoneForMcc;
        if (!TimeServiceHelper.isTimeZoneSettingInitializedStatic() && (strDefaultTimeZoneForMcc = defaultTimeZoneForMcc(i)) != null && strDefaultTimeZoneForMcc.length() > 0) {
            TimeServiceHelper.setDeviceTimeZoneStatic(context, strDefaultTimeZoneForMcc);
            Slog.d(LOG_TAG, "timezone set to " + strDefaultTimeZoneForMcc);
        }
    }

    public static Locale getLocaleFromMcc(Context context, int i, String str) {
        boolean z = !TextUtils.isEmpty(str);
        if (!z) {
            str = defaultLanguageForMcc(i);
        }
        String strCountryCodeForMcc = countryCodeForMcc(i);
        Slog.d(LOG_TAG, "getLocaleFromMcc(" + str + ", " + strCountryCodeForMcc + ", " + i);
        Locale localeForLanguageCountry = getLocaleForLanguageCountry(context, str, strCountryCodeForMcc);
        if (localeForLanguageCountry == null && z) {
            String strDefaultLanguageForMcc = defaultLanguageForMcc(i);
            Slog.d(LOG_TAG, "[retry ] getLocaleFromMcc(" + strDefaultLanguageForMcc + ", " + strCountryCodeForMcc + ", " + i);
            return getLocaleForLanguageCountry(context, strDefaultLanguageForMcc, strCountryCodeForMcc);
        }
        return localeForLanguageCountry;
    }

    private static void setWifiCountryCodeFromMcc(Context context, int i) {
        String strCountryCodeForMcc = countryCodeForMcc(i);
        Slog.d(LOG_TAG, "WIFI_COUNTRY_CODE set to " + strCountryCodeForMcc);
        ((WifiManager) context.getSystemService("wifi")).setCountryCode(strCountryCodeForMcc);
    }
}
