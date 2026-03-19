package com.android.internal.inputmethod;

import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.res.Resources;
import android.os.LocaleList;
import android.os.RemoteException;
import android.provider.Settings;
import android.telecom.Logging.Session;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Printer;
import android.util.Slog;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.TextServicesManager;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.inputmethod.LocaleUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InputMethodUtils {
    public static final boolean DEBUG = false;
    private static final char INPUT_METHOD_SEPARATOR = ':';
    private static final char INPUT_METHOD_SUBTYPE_SEPARATOR = ';';
    public static final int NOT_A_SUBTYPE_ID = -1;
    public static final String SUBTYPE_MODE_KEYBOARD = "keyboard";
    public static final String SUBTYPE_MODE_VOICE = "voice";
    private static final String TAG = "InputMethodUtils";
    private static final String TAG_ASCII_CAPABLE = "AsciiCapable";
    private static final String TAG_ENABLED_WHEN_DEFAULT_IS_NOT_ASCII_CAPABLE = "EnabledWhenDefaultIsNotAsciiCapable";

    @GuardedBy("sCacheLock")
    private static InputMethodInfo sCachedInputMethodInfo;

    @GuardedBy("sCacheLock")
    private static ArrayList<InputMethodSubtype> sCachedResult;

    @GuardedBy("sCacheLock")
    private static LocaleList sCachedSystemLocales;
    public static final String SUBTYPE_MODE_ANY = null;
    private static final Locale ENGLISH_LOCALE = new Locale("en");
    private static final String NOT_A_SUBTYPE_ID_STR = String.valueOf(-1);
    private static final Locale[] SEARCH_ORDER_OF_FALLBACK_LOCALES = {Locale.ENGLISH, Locale.US, Locale.UK};
    private static final Object sCacheLock = new Object();
    private static final LocaleUtils.LocaleExtractor<InputMethodSubtype> sSubtypeToLocale = new LocaleUtils.LocaleExtractor<InputMethodSubtype>() {
        @Override
        public Locale get(InputMethodSubtype inputMethodSubtype) {
            if (inputMethodSubtype != null) {
                return inputMethodSubtype.getLocaleObject();
            }
            return null;
        }
    };
    private static final Locale LOCALE_EN_US = new Locale("en", "US");
    private static final Locale LOCALE_EN_GB = new Locale("en", "GB");

    private InputMethodUtils() {
    }

    public static String getApiCallStack() {
        String str = "";
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            int i = 1;
            while (i < stackTrace.length) {
                String string = stackTrace[i].toString();
                if (!TextUtils.isEmpty(str) && string.indexOf("Transact(") >= 0) {
                    break;
                }
                i++;
                str = string;
            }
            return str;
        }
    }

    public static boolean isSystemIme(InputMethodInfo inputMethodInfo) {
        return (inputMethodInfo.getServiceInfo().applicationInfo.flags & 1) != 0;
    }

    public static boolean isSystemImeThatHasSubtypeOf(InputMethodInfo inputMethodInfo, Context context, boolean z, Locale locale, boolean z2, String str) {
        if (isSystemIme(inputMethodInfo)) {
            return (!z || inputMethodInfo.isDefault(context)) && containsSubtypeOf(inputMethodInfo, locale, z2, str);
        }
        return false;
    }

    public static Locale getFallbackLocaleForDefaultIme(ArrayList<InputMethodInfo> arrayList, Context context) {
        for (Locale locale : SEARCH_ORDER_OF_FALLBACK_LOCALES) {
            for (int i = 0; i < arrayList.size(); i++) {
                if (isSystemImeThatHasSubtypeOf(arrayList.get(i), context, true, locale, true, SUBTYPE_MODE_KEYBOARD)) {
                    return locale;
                }
            }
        }
        for (Locale locale2 : SEARCH_ORDER_OF_FALLBACK_LOCALES) {
            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                if (isSystemImeThatHasSubtypeOf(arrayList.get(i2), context, false, locale2, true, SUBTYPE_MODE_KEYBOARD)) {
                    return locale2;
                }
            }
        }
        Slog.w(TAG, "Found no fallback locale. imis=" + Arrays.toString(arrayList.toArray()));
        return null;
    }

    private static boolean isSystemAuxilialyImeThatHasAutomaticSubtype(InputMethodInfo inputMethodInfo, Context context, boolean z) {
        if (!isSystemIme(inputMethodInfo)) {
            return false;
        }
        if ((z && !inputMethodInfo.isDefault(context)) || !inputMethodInfo.isAuxiliaryIme()) {
            return false;
        }
        int subtypeCount = inputMethodInfo.getSubtypeCount();
        for (int i = 0; i < subtypeCount; i++) {
            if (inputMethodInfo.getSubtypeAt(i).overridesImplicitlyEnabledSubtype()) {
                return true;
            }
        }
        return false;
    }

    public static Locale getSystemLocaleFromContext(Context context) {
        try {
            return context.getResources().getConfiguration().locale;
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    private static final class InputMethodListBuilder {
        private final LinkedHashSet<InputMethodInfo> mInputMethodSet;

        private InputMethodListBuilder() {
            this.mInputMethodSet = new LinkedHashSet<>();
        }

        public InputMethodListBuilder fillImes(ArrayList<InputMethodInfo> arrayList, Context context, boolean z, Locale locale, boolean z2, String str) {
            for (int i = 0; i < arrayList.size(); i++) {
                InputMethodInfo inputMethodInfo = arrayList.get(i);
                if (InputMethodUtils.isSystemImeThatHasSubtypeOf(inputMethodInfo, context, z, locale, z2, str)) {
                    this.mInputMethodSet.add(inputMethodInfo);
                }
            }
            return this;
        }

        public InputMethodListBuilder fillAuxiliaryImes(ArrayList<InputMethodInfo> arrayList, Context context) {
            Iterator<InputMethodInfo> it = this.mInputMethodSet.iterator();
            while (it.hasNext()) {
                if (it.next().isAuxiliaryIme()) {
                    return this;
                }
            }
            boolean z = false;
            for (int i = 0; i < arrayList.size(); i++) {
                InputMethodInfo inputMethodInfo = arrayList.get(i);
                if (InputMethodUtils.isSystemAuxilialyImeThatHasAutomaticSubtype(inputMethodInfo, context, true)) {
                    this.mInputMethodSet.add(inputMethodInfo);
                    z = true;
                }
            }
            if (z) {
                return this;
            }
            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                InputMethodInfo inputMethodInfo2 = arrayList.get(i2);
                if (InputMethodUtils.isSystemAuxilialyImeThatHasAutomaticSubtype(inputMethodInfo2, context, false)) {
                    this.mInputMethodSet.add(inputMethodInfo2);
                }
            }
            return this;
        }

        public boolean isEmpty() {
            return this.mInputMethodSet.isEmpty();
        }

        public ArrayList<InputMethodInfo> build() {
            return new ArrayList<>(this.mInputMethodSet);
        }
    }

    private static InputMethodListBuilder getMinimumKeyboardSetWithSystemLocale(ArrayList<InputMethodInfo> arrayList, Context context, Locale locale, Locale locale2) {
        InputMethodListBuilder inputMethodListBuilder = new InputMethodListBuilder();
        inputMethodListBuilder.fillImes(arrayList, context, true, locale, true, SUBTYPE_MODE_KEYBOARD);
        if (!inputMethodListBuilder.isEmpty()) {
            return inputMethodListBuilder;
        }
        inputMethodListBuilder.fillImes(arrayList, context, true, locale, false, SUBTYPE_MODE_KEYBOARD);
        if (!inputMethodListBuilder.isEmpty()) {
            return inputMethodListBuilder;
        }
        inputMethodListBuilder.fillImes(arrayList, context, true, locale2, true, SUBTYPE_MODE_KEYBOARD);
        if (!inputMethodListBuilder.isEmpty()) {
            return inputMethodListBuilder;
        }
        inputMethodListBuilder.fillImes(arrayList, context, true, locale2, false, SUBTYPE_MODE_KEYBOARD);
        if (!inputMethodListBuilder.isEmpty()) {
            return inputMethodListBuilder;
        }
        inputMethodListBuilder.fillImes(arrayList, context, false, locale2, true, SUBTYPE_MODE_KEYBOARD);
        if (!inputMethodListBuilder.isEmpty()) {
            return inputMethodListBuilder;
        }
        inputMethodListBuilder.fillImes(arrayList, context, false, locale2, false, SUBTYPE_MODE_KEYBOARD);
        if (!inputMethodListBuilder.isEmpty()) {
            return inputMethodListBuilder;
        }
        Slog.w(TAG, "No software keyboard is found. imis=" + Arrays.toString(arrayList.toArray()) + " systemLocale=" + locale + " fallbackLocale=" + locale2);
        return inputMethodListBuilder;
    }

    public static ArrayList<InputMethodInfo> getDefaultEnabledImes(Context context, ArrayList<InputMethodInfo> arrayList, boolean z) {
        Locale fallbackLocaleForDefaultIme = getFallbackLocaleForDefaultIme(arrayList, context);
        Locale systemLocaleFromContext = getSystemLocaleFromContext(context);
        InputMethodListBuilder minimumKeyboardSetWithSystemLocale = getMinimumKeyboardSetWithSystemLocale(arrayList, context, systemLocaleFromContext, fallbackLocaleForDefaultIme);
        if (!z) {
            minimumKeyboardSetWithSystemLocale.fillImes(arrayList, context, true, systemLocaleFromContext, true, SUBTYPE_MODE_ANY).fillAuxiliaryImes(arrayList, context);
        }
        return minimumKeyboardSetWithSystemLocale.build();
    }

    public static ArrayList<InputMethodInfo> getDefaultEnabledImes(Context context, ArrayList<InputMethodInfo> arrayList) {
        return getDefaultEnabledImes(context, arrayList, false);
    }

    public static Locale constructLocaleFromString(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String[] strArrSplit = str.split(Session.SESSION_SEPARATION_CHAR_CHILD, 3);
        if (strArrSplit.length >= 1 && "tl".equals(strArrSplit[0])) {
            strArrSplit[0] = "fil";
        }
        if (strArrSplit.length == 1) {
            return new Locale(strArrSplit[0]);
        }
        if (strArrSplit.length == 2) {
            return new Locale(strArrSplit[0], strArrSplit[1]);
        }
        if (strArrSplit.length == 3) {
            return new Locale(strArrSplit[0], strArrSplit[1], strArrSplit[2]);
        }
        return null;
    }

    public static boolean containsSubtypeOf(InputMethodInfo inputMethodInfo, Locale locale, boolean z, String str) {
        if (locale == null) {
            return false;
        }
        int subtypeCount = inputMethodInfo.getSubtypeCount();
        for (int i = 0; i < subtypeCount; i++) {
            InputMethodSubtype subtypeAt = inputMethodInfo.getSubtypeAt(i);
            if (z) {
                Locale localeObject = subtypeAt.getLocaleObject();
                if (localeObject != null && TextUtils.equals(localeObject.getLanguage(), locale.getLanguage()) && TextUtils.equals(localeObject.getCountry(), locale.getCountry())) {
                    if (str == SUBTYPE_MODE_ANY || TextUtils.isEmpty(str) || str.equalsIgnoreCase(subtypeAt.getMode())) {
                        return true;
                    }
                }
            } else if (!TextUtils.equals(new Locale(getLanguageFromLocaleString(subtypeAt.getLocale())).getLanguage(), locale.getLanguage())) {
                continue;
            }
        }
        return false;
    }

    public static ArrayList<InputMethodSubtype> getSubtypes(InputMethodInfo inputMethodInfo) {
        ArrayList<InputMethodSubtype> arrayList = new ArrayList<>();
        int subtypeCount = inputMethodInfo.getSubtypeCount();
        for (int i = 0; i < subtypeCount; i++) {
            arrayList.add(inputMethodInfo.getSubtypeAt(i));
        }
        return arrayList;
    }

    public static ArrayList<InputMethodSubtype> getOverridingImplicitlyEnabledSubtypes(InputMethodInfo inputMethodInfo, String str) {
        ArrayList<InputMethodSubtype> arrayList = new ArrayList<>();
        int subtypeCount = inputMethodInfo.getSubtypeCount();
        for (int i = 0; i < subtypeCount; i++) {
            InputMethodSubtype subtypeAt = inputMethodInfo.getSubtypeAt(i);
            if (subtypeAt.overridesImplicitlyEnabledSubtype() && subtypeAt.getMode().equals(str)) {
                arrayList.add(subtypeAt);
            }
        }
        return arrayList;
    }

    public static InputMethodInfo getMostApplicableDefaultIME(List<InputMethodInfo> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        int size = list.size();
        int i = -1;
        while (size > 0) {
            size--;
            InputMethodInfo inputMethodInfo = list.get(size);
            if (!inputMethodInfo.isAuxiliaryIme()) {
                if (isSystemIme(inputMethodInfo) && containsSubtypeOf(inputMethodInfo, ENGLISH_LOCALE, false, SUBTYPE_MODE_KEYBOARD)) {
                    return inputMethodInfo;
                }
                if (i < 0 && isSystemIme(inputMethodInfo)) {
                    i = size;
                }
            }
        }
        return list.get(Math.max(i, 0));
    }

    public static boolean isValidSubtypeId(InputMethodInfo inputMethodInfo, int i) {
        return getSubtypeIdFromHashCode(inputMethodInfo, i) != -1;
    }

    public static int getSubtypeIdFromHashCode(InputMethodInfo inputMethodInfo, int i) {
        if (inputMethodInfo != null) {
            int subtypeCount = inputMethodInfo.getSubtypeCount();
            for (int i2 = 0; i2 < subtypeCount; i2++) {
                if (i == inputMethodInfo.getSubtypeAt(i2).hashCode()) {
                    return i2;
                }
            }
            return -1;
        }
        return -1;
    }

    @VisibleForTesting
    public static ArrayList<InputMethodSubtype> getImplicitlyApplicableSubtypesLocked(Resources resources, InputMethodInfo inputMethodInfo) {
        LocaleList locales = resources.getConfiguration().getLocales();
        synchronized (sCacheLock) {
            if (locales.equals(sCachedSystemLocales) && sCachedInputMethodInfo == inputMethodInfo) {
                return new ArrayList<>(sCachedResult);
            }
            ArrayList<InputMethodSubtype> implicitlyApplicableSubtypesLockedImpl = getImplicitlyApplicableSubtypesLockedImpl(resources, inputMethodInfo);
            synchronized (sCacheLock) {
                sCachedSystemLocales = locales;
                sCachedInputMethodInfo = inputMethodInfo;
                sCachedResult = new ArrayList<>(implicitlyApplicableSubtypesLockedImpl);
            }
            return implicitlyApplicableSubtypesLockedImpl;
        }
    }

    private static ArrayList<InputMethodSubtype> getImplicitlyApplicableSubtypesLockedImpl(Resources resources, InputMethodInfo inputMethodInfo) {
        InputMethodSubtype inputMethodSubtypeFindLastResortApplicableSubtypeLocked;
        boolean z;
        ArrayList<InputMethodSubtype> subtypes = getSubtypes(inputMethodInfo);
        LocaleList locales = resources.getConfiguration().getLocales();
        String string = locales.get(0).toString();
        if (TextUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        int size = subtypes.size();
        HashMap map = new HashMap();
        for (int i = 0; i < size; i++) {
            InputMethodSubtype inputMethodSubtype = subtypes.get(i);
            if (inputMethodSubtype.overridesImplicitlyEnabledSubtype()) {
                String mode = inputMethodSubtype.getMode();
                if (!map.containsKey(mode)) {
                    map.put(mode, inputMethodSubtype);
                }
            }
        }
        if (map.size() > 0) {
            return new ArrayList<>(map.values());
        }
        HashMap map2 = new HashMap();
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < size; i2++) {
            InputMethodSubtype inputMethodSubtype2 = subtypes.get(i2);
            String mode2 = inputMethodSubtype2.getMode();
            if (SUBTYPE_MODE_KEYBOARD.equals(mode2)) {
                arrayList.add(inputMethodSubtype2);
            } else {
                if (!map2.containsKey(mode2)) {
                    map2.put(mode2, new ArrayList());
                }
                ((ArrayList) map2.get(mode2)).add(inputMethodSubtype2);
            }
        }
        ArrayList<InputMethodSubtype> arrayList2 = new ArrayList<>();
        LocaleUtils.filterByLanguage(arrayList, sSubtypeToLocale, locales, arrayList2);
        if (!arrayList2.isEmpty()) {
            int size2 = arrayList2.size();
            int i3 = 0;
            while (true) {
                if (i3 < size2) {
                    if (!arrayList2.get(i3).containsExtraValueKey(TAG_ASCII_CAPABLE)) {
                        i3++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                int size3 = arrayList.size();
                for (int i4 = 0; i4 < size3; i4++) {
                    InputMethodSubtype inputMethodSubtype3 = (InputMethodSubtype) arrayList.get(i4);
                    if (SUBTYPE_MODE_KEYBOARD.equals(inputMethodSubtype3.getMode()) && inputMethodSubtype3.containsExtraValueKey(TAG_ENABLED_WHEN_DEFAULT_IS_NOT_ASCII_CAPABLE)) {
                        arrayList2.add(inputMethodSubtype3);
                    }
                }
            }
        }
        if (arrayList2.isEmpty() && (inputMethodSubtypeFindLastResortApplicableSubtypeLocked = findLastResortApplicableSubtypeLocked(resources, subtypes, SUBTYPE_MODE_KEYBOARD, string, true)) != null) {
            arrayList2.add(inputMethodSubtypeFindLastResortApplicableSubtypeLocked);
        }
        Iterator it = map2.values().iterator();
        while (it.hasNext()) {
            LocaleUtils.filterByLanguage((ArrayList) it.next(), sSubtypeToLocale, locales, arrayList2);
        }
        return arrayList2;
    }

    public static String getLanguageFromLocaleString(String str) {
        int iIndexOf = str.indexOf(95);
        if (iIndexOf < 0) {
            return str;
        }
        return str.substring(0, iIndexOf);
    }

    public static InputMethodSubtype findLastResortApplicableSubtypeLocked(Resources resources, List<InputMethodSubtype> list, String str, String str2, boolean z) {
        InputMethodSubtype inputMethodSubtype = null;
        if (list == null || list.size() == 0) {
            return null;
        }
        if (TextUtils.isEmpty(str2)) {
            str2 = resources.getConfiguration().locale.toString();
        }
        String languageFromLocaleString = getLanguageFromLocaleString(str2);
        int size = list.size();
        int i = 0;
        InputMethodSubtype inputMethodSubtype2 = null;
        boolean z2 = false;
        while (true) {
            if (i >= size) {
                break;
            }
            InputMethodSubtype inputMethodSubtype3 = list.get(i);
            String locale = inputMethodSubtype3.getLocale();
            String languageFromLocaleString2 = getLanguageFromLocaleString(locale);
            if (str == null || list.get(i).getMode().equalsIgnoreCase(str)) {
                if (inputMethodSubtype == null) {
                    inputMethodSubtype = inputMethodSubtype3;
                }
                if (!str2.equals(locale)) {
                    if (!z2 && languageFromLocaleString.equals(languageFromLocaleString2)) {
                        z2 = true;
                        inputMethodSubtype2 = inputMethodSubtype3;
                    }
                } else {
                    inputMethodSubtype2 = inputMethodSubtype3;
                    break;
                }
            }
            i++;
        }
        if (inputMethodSubtype2 == null && z) {
            return inputMethodSubtype;
        }
        return inputMethodSubtype2;
    }

    public static boolean canAddToLastInputMethod(InputMethodSubtype inputMethodSubtype) {
        if (inputMethodSubtype == null) {
            return true;
        }
        return !inputMethodSubtype.isAuxiliary();
    }

    public static void setNonSelectedSystemImesDisabledUntilUsed(IPackageManager iPackageManager, List<InputMethodInfo> list, int i, String str) {
        boolean z;
        String[] stringArray = Resources.getSystem().getStringArray(R.array.config_disabledUntilUsedPreinstalledImes);
        if (stringArray == null || stringArray.length == 0) {
            return;
        }
        SpellCheckerInfo currentSpellChecker = TextServicesManager.getInstance().getCurrentSpellChecker();
        for (String str2 : stringArray) {
            int i2 = 0;
            while (true) {
                if (i2 < list.size()) {
                    if (!str2.equals(list.get(i2).getPackageName())) {
                        i2++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z && (currentSpellChecker == null || !str2.equals(currentSpellChecker.getPackageName()))) {
                try {
                    ApplicationInfo applicationInfo = iPackageManager.getApplicationInfo(str2, 32768, i);
                    if (applicationInfo != null) {
                        if ((applicationInfo.flags & 1) != 0) {
                            setDisabledUntilUsed(iPackageManager, str2, i, str);
                        }
                    }
                } catch (RemoteException e) {
                    Slog.w(TAG, "getApplicationInfo failed. packageName=" + str2 + " userId=" + i, e);
                }
            }
        }
    }

    private static void setDisabledUntilUsed(IPackageManager iPackageManager, String str, int i, String str2) {
        try {
            int applicationEnabledSetting = iPackageManager.getApplicationEnabledSetting(str, i);
            if (applicationEnabledSetting == 0 || applicationEnabledSetting == 1) {
                try {
                    iPackageManager.setApplicationEnabledSetting(str, 4, 0, i, str2);
                } catch (RemoteException e) {
                    Slog.w(TAG, "setApplicationEnabledSetting failed. packageName=" + str + " userId=" + i + " callingPackage=" + str2, e);
                }
            }
        } catch (RemoteException e2) {
            Slog.w(TAG, "getApplicationEnabledSetting failed. packageName=" + str + " userId=" + i, e2);
        }
    }

    public static CharSequence getImeAndSubtypeDisplayName(Context context, InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype) {
        String str;
        CharSequence charSequenceLoadLabel = inputMethodInfo.loadLabel(context.getPackageManager());
        if (inputMethodSubtype == null) {
            return charSequenceLoadLabel;
        }
        CharSequence[] charSequenceArr = new CharSequence[2];
        charSequenceArr[0] = inputMethodSubtype.getDisplayName(context, inputMethodInfo.getPackageName(), inputMethodInfo.getServiceInfo().applicationInfo);
        if (TextUtils.isEmpty(charSequenceLoadLabel)) {
            str = "";
        } else {
            str = " - " + ((Object) charSequenceLoadLabel);
        }
        charSequenceArr[1] = str;
        return TextUtils.concat(charSequenceArr);
    }

    public static boolean checkIfPackageBelongsToUid(AppOpsManager appOpsManager, int i, String str) {
        try {
            appOpsManager.checkPackage(i, str);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    @VisibleForTesting
    public static ArrayMap<String, ArraySet<String>> parseInputMethodsAndSubtypesString(String str) {
        ArrayMap<String, ArraySet<String>> arrayMap = new ArrayMap<>();
        if (TextUtils.isEmpty(str)) {
            return arrayMap;
        }
        for (Pair<String, ArrayList<String>> pair : InputMethodSettings.buildInputMethodsAndSubtypeList(str, new TextUtils.SimpleStringSplitter(INPUT_METHOD_SEPARATOR), new TextUtils.SimpleStringSplitter(';'))) {
            ArraySet<String> arraySet = new ArraySet<>();
            if (pair.second != null) {
                arraySet.addAll(pair.second);
            }
            arrayMap.put(pair.first, arraySet);
        }
        return arrayMap;
    }

    public static String buildInputMethodsAndSubtypesString(ArrayMap<String, ArraySet<String>> arrayMap) {
        ArrayList arrayList = new ArrayList(4);
        for (Map.Entry<String, ArraySet<String>> entry : arrayMap.entrySet()) {
            String key = entry.getKey();
            ArraySet<String> value = entry.getValue();
            ArrayList arrayList2 = new ArrayList(2);
            if (value != null) {
                arrayList2.addAll(value);
            }
            arrayList.add(new Pair(key, arrayList2));
        }
        return InputMethodSettings.buildInputMethodsSettingString(arrayList);
    }

    public static class InputMethodSettings {
        private int mCurrentUserId;
        private final HashMap<String, InputMethodInfo> mMethodMap;
        private final Resources mRes;
        private final ContentResolver mResolver;
        private final TextUtils.SimpleStringSplitter mInputMethodSplitter = new TextUtils.SimpleStringSplitter(InputMethodUtils.INPUT_METHOD_SEPARATOR);
        private final TextUtils.SimpleStringSplitter mSubtypeSplitter = new TextUtils.SimpleStringSplitter(';');
        private final HashMap<String, String> mCopyOnWriteDataStore = new HashMap<>();
        private boolean mCopyOnWrite = false;
        private String mEnabledInputMethodsStrCache = "";
        private int[] mCurrentProfileIds = new int[0];

        private static void buildEnabledInputMethodsSettingString(StringBuilder sb, Pair<String, ArrayList<String>> pair) {
            sb.append(pair.first);
            for (String str : pair.second) {
                sb.append(';');
                sb.append(str);
            }
        }

        public static String buildInputMethodsSettingString(List<Pair<String, ArrayList<String>>> list) {
            StringBuilder sb = new StringBuilder();
            boolean z = false;
            for (Pair<String, ArrayList<String>> pair : list) {
                if (z) {
                    sb.append(InputMethodUtils.INPUT_METHOD_SEPARATOR);
                }
                buildEnabledInputMethodsSettingString(sb, pair);
                z = true;
            }
            return sb.toString();
        }

        public static List<Pair<String, ArrayList<String>>> buildInputMethodsAndSubtypeList(String str, TextUtils.SimpleStringSplitter simpleStringSplitter, TextUtils.SimpleStringSplitter simpleStringSplitter2) {
            ArrayList arrayList = new ArrayList();
            if (TextUtils.isEmpty(str)) {
                return arrayList;
            }
            simpleStringSplitter.setString(str);
            while (simpleStringSplitter.hasNext()) {
                simpleStringSplitter2.setString(simpleStringSplitter.next());
                if (simpleStringSplitter2.hasNext()) {
                    ArrayList arrayList2 = new ArrayList();
                    String next = simpleStringSplitter2.next();
                    while (simpleStringSplitter2.hasNext()) {
                        arrayList2.add(simpleStringSplitter2.next());
                    }
                    arrayList.add(new Pair(next, arrayList2));
                }
            }
            return arrayList;
        }

        public InputMethodSettings(Resources resources, ContentResolver contentResolver, HashMap<String, InputMethodInfo> map, ArrayList<InputMethodInfo> arrayList, int i, boolean z) {
            this.mRes = resources;
            this.mResolver = contentResolver;
            this.mMethodMap = map;
            switchCurrentUser(i, z);
        }

        public void switchCurrentUser(int i, boolean z) {
            if (this.mCurrentUserId != i || this.mCopyOnWrite != z) {
                this.mCopyOnWriteDataStore.clear();
                this.mEnabledInputMethodsStrCache = "";
            }
            this.mCurrentUserId = i;
            this.mCopyOnWrite = z;
        }

        private void putString(String str, String str2) {
            if (this.mCopyOnWrite) {
                this.mCopyOnWriteDataStore.put(str, str2);
            } else {
                Settings.Secure.putStringForUser(this.mResolver, str, str2, this.mCurrentUserId);
            }
        }

        private String getString(String str, String str2) {
            String stringForUser;
            if (this.mCopyOnWrite && this.mCopyOnWriteDataStore.containsKey(str)) {
                stringForUser = this.mCopyOnWriteDataStore.get(str);
            } else {
                stringForUser = Settings.Secure.getStringForUser(this.mResolver, str, this.mCurrentUserId);
            }
            return stringForUser != null ? stringForUser : str2;
        }

        private void putInt(String str, int i) {
            if (this.mCopyOnWrite) {
                this.mCopyOnWriteDataStore.put(str, String.valueOf(i));
            } else {
                Settings.Secure.putIntForUser(this.mResolver, str, i, this.mCurrentUserId);
            }
        }

        private int getInt(String str, int i) {
            if (this.mCopyOnWrite && this.mCopyOnWriteDataStore.containsKey(str)) {
                String str2 = this.mCopyOnWriteDataStore.get(str);
                if (str2 != null) {
                    return Integer.parseInt(str2);
                }
                return 0;
            }
            return Settings.Secure.getIntForUser(this.mResolver, str, i, this.mCurrentUserId);
        }

        private void putBoolean(String str, boolean z) {
            putInt(str, z ? 1 : 0);
        }

        private boolean getBoolean(String str, boolean z) {
            return getInt(str, z ? 1 : 0) == 1;
        }

        public void setCurrentProfileIds(int[] iArr) {
            synchronized (this) {
                this.mCurrentProfileIds = iArr;
            }
        }

        public boolean isCurrentProfile(int i) {
            synchronized (this) {
                if (i == this.mCurrentUserId) {
                    return true;
                }
                for (int i2 = 0; i2 < this.mCurrentProfileIds.length; i2++) {
                    if (i == this.mCurrentProfileIds[i2]) {
                        return true;
                    }
                }
                return false;
            }
        }

        public ArrayList<InputMethodInfo> getEnabledInputMethodListLocked() {
            return createEnabledInputMethodListLocked(getEnabledInputMethodsAndSubtypeListLocked());
        }

        public List<InputMethodSubtype> getEnabledInputMethodSubtypeListLocked(Context context, InputMethodInfo inputMethodInfo, boolean z) {
            List<InputMethodSubtype> enabledInputMethodSubtypeListLocked = getEnabledInputMethodSubtypeListLocked(inputMethodInfo);
            if (z && enabledInputMethodSubtypeListLocked.isEmpty()) {
                enabledInputMethodSubtypeListLocked = InputMethodUtils.getImplicitlyApplicableSubtypesLocked(context.getResources(), inputMethodInfo);
            }
            return InputMethodSubtype.sort(context, 0, inputMethodInfo, enabledInputMethodSubtypeListLocked);
        }

        public List<InputMethodSubtype> getEnabledInputMethodSubtypeListLocked(InputMethodInfo inputMethodInfo) {
            List<Pair<String, ArrayList<String>>> enabledInputMethodsAndSubtypeListLocked = getEnabledInputMethodsAndSubtypeListLocked();
            ArrayList arrayList = new ArrayList();
            if (inputMethodInfo != null) {
                Iterator<Pair<String, ArrayList<String>>> it = enabledInputMethodsAndSubtypeListLocked.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Pair<String, ArrayList<String>> next = it.next();
                    InputMethodInfo inputMethodInfo2 = this.mMethodMap.get(next.first);
                    if (inputMethodInfo2 != null && inputMethodInfo2.getId().equals(inputMethodInfo.getId())) {
                        int subtypeCount = inputMethodInfo2.getSubtypeCount();
                        for (int i = 0; i < subtypeCount; i++) {
                            InputMethodSubtype subtypeAt = inputMethodInfo2.getSubtypeAt(i);
                            Iterator<String> it2 = next.second.iterator();
                            while (it2.hasNext()) {
                                if (String.valueOf(subtypeAt.hashCode()).equals(it2.next())) {
                                    arrayList.add(subtypeAt);
                                }
                            }
                        }
                    }
                }
            }
            return arrayList;
        }

        public List<Pair<String, ArrayList<String>>> getEnabledInputMethodsAndSubtypeListLocked() {
            return buildInputMethodsAndSubtypeList(getEnabledInputMethodsStr(), this.mInputMethodSplitter, this.mSubtypeSplitter);
        }

        public void appendAndPutEnabledInputMethodLocked(String str, boolean z) {
            if (z) {
                getEnabledInputMethodsStr();
            }
            if (TextUtils.isEmpty(this.mEnabledInputMethodsStrCache)) {
                putEnabledInputMethodsStr(str);
                return;
            }
            putEnabledInputMethodsStr(this.mEnabledInputMethodsStrCache + InputMethodUtils.INPUT_METHOD_SEPARATOR + str);
        }

        public boolean buildAndPutEnabledInputMethodsStrRemovingIdLocked(StringBuilder sb, List<Pair<String, ArrayList<String>>> list, String str) {
            boolean z = false;
            boolean z2 = false;
            for (Pair<String, ArrayList<String>> pair : list) {
                if (!pair.first.equals(str)) {
                    if (z2) {
                        sb.append(InputMethodUtils.INPUT_METHOD_SEPARATOR);
                    } else {
                        z2 = true;
                    }
                    buildEnabledInputMethodsSettingString(sb, pair);
                } else {
                    z = true;
                }
            }
            if (z) {
                putEnabledInputMethodsStr(sb.toString());
            }
            return z;
        }

        private ArrayList<InputMethodInfo> createEnabledInputMethodListLocked(List<Pair<String, ArrayList<String>>> list) {
            ArrayList<InputMethodInfo> arrayList = new ArrayList<>();
            Iterator<Pair<String, ArrayList<String>>> it = list.iterator();
            while (it.hasNext()) {
                InputMethodInfo inputMethodInfo = this.mMethodMap.get(it.next().first);
                if (inputMethodInfo != null && !inputMethodInfo.isVrOnly()) {
                    arrayList.add(inputMethodInfo);
                }
            }
            return arrayList;
        }

        private void putEnabledInputMethodsStr(String str) {
            if (TextUtils.isEmpty(str)) {
                putString(Settings.Secure.ENABLED_INPUT_METHODS, null);
            } else {
                putString(Settings.Secure.ENABLED_INPUT_METHODS, str);
            }
            if (str == null) {
                str = "";
            }
            this.mEnabledInputMethodsStrCache = str;
        }

        public String getEnabledInputMethodsStr() {
            this.mEnabledInputMethodsStrCache = getString(Settings.Secure.ENABLED_INPUT_METHODS, "");
            return this.mEnabledInputMethodsStrCache;
        }

        private void saveSubtypeHistory(List<Pair<String, String>> list, String str, String str2) {
            boolean z;
            StringBuilder sb = new StringBuilder();
            if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
                z = false;
            } else {
                sb.append(str);
                sb.append(';');
                sb.append(str2);
                z = true;
            }
            for (Pair<String, String> pair : list) {
                String str3 = pair.first;
                String str4 = pair.second;
                if (TextUtils.isEmpty(str4)) {
                    str4 = InputMethodUtils.NOT_A_SUBTYPE_ID_STR;
                }
                if (z) {
                    sb.append(InputMethodUtils.INPUT_METHOD_SEPARATOR);
                } else {
                    z = true;
                }
                sb.append(str3);
                sb.append(';');
                sb.append(str4);
            }
            putSubtypeHistoryStr(sb.toString());
        }

        private void addSubtypeToHistory(String str, String str2) {
            List<Pair<String, String>> listLoadInputMethodAndSubtypeHistoryLocked = loadInputMethodAndSubtypeHistoryLocked();
            Iterator<Pair<String, String>> it = listLoadInputMethodAndSubtypeHistoryLocked.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Pair<String, String> next = it.next();
                if (next.first.equals(str)) {
                    listLoadInputMethodAndSubtypeHistoryLocked.remove(next);
                    break;
                }
            }
            saveSubtypeHistory(listLoadInputMethodAndSubtypeHistoryLocked, str, str2);
        }

        private void putSubtypeHistoryStr(String str) {
            if (TextUtils.isEmpty(str)) {
                putString(Settings.Secure.INPUT_METHODS_SUBTYPE_HISTORY, null);
            } else {
                putString(Settings.Secure.INPUT_METHODS_SUBTYPE_HISTORY, str);
            }
        }

        public Pair<String, String> getLastInputMethodAndSubtypeLocked() {
            return getLastSubtypeForInputMethodLockedInternal(null);
        }

        public String getLastSubtypeForInputMethodLocked(String str) {
            Pair<String, String> lastSubtypeForInputMethodLockedInternal = getLastSubtypeForInputMethodLockedInternal(str);
            if (lastSubtypeForInputMethodLockedInternal != null) {
                return lastSubtypeForInputMethodLockedInternal.second;
            }
            return null;
        }

        private Pair<String, String> getLastSubtypeForInputMethodLockedInternal(String str) {
            List<Pair<String, ArrayList<String>>> enabledInputMethodsAndSubtypeListLocked = getEnabledInputMethodsAndSubtypeListLocked();
            for (Pair<String, String> pair : loadInputMethodAndSubtypeHistoryLocked()) {
                String str2 = pair.first;
                if (TextUtils.isEmpty(str) || str2.equals(str)) {
                    String enabledSubtypeHashCodeForInputMethodAndSubtypeLocked = getEnabledSubtypeHashCodeForInputMethodAndSubtypeLocked(enabledInputMethodsAndSubtypeListLocked, str2, pair.second);
                    if (!TextUtils.isEmpty(enabledSubtypeHashCodeForInputMethodAndSubtypeLocked)) {
                        return new Pair<>(str2, enabledSubtypeHashCodeForInputMethodAndSubtypeLocked);
                    }
                }
            }
            return null;
        }

        private String getEnabledSubtypeHashCodeForInputMethodAndSubtypeLocked(List<Pair<String, ArrayList<String>>> list, String str, String str2) {
            ArrayList<InputMethodSubtype> implicitlyApplicableSubtypesLocked;
            for (Pair<String, ArrayList<String>> pair : list) {
                if (pair.first.equals(str)) {
                    ArrayList<String> arrayList = pair.second;
                    InputMethodInfo inputMethodInfo = this.mMethodMap.get(str);
                    if (arrayList.size() == 0) {
                        if (inputMethodInfo != null && inputMethodInfo.getSubtypeCount() > 0 && (implicitlyApplicableSubtypesLocked = InputMethodUtils.getImplicitlyApplicableSubtypesLocked(this.mRes, inputMethodInfo)) != null) {
                            int size = implicitlyApplicableSubtypesLocked.size();
                            for (int i = 0; i < size; i++) {
                                if (String.valueOf(implicitlyApplicableSubtypesLocked.get(i).hashCode()).equals(str2)) {
                                    return str2;
                                }
                            }
                        }
                    } else {
                        for (String str3 : arrayList) {
                            if (str3.equals(str2)) {
                                try {
                                    if (!InputMethodUtils.isValidSubtypeId(inputMethodInfo, Integer.parseInt(str2))) {
                                        return InputMethodUtils.NOT_A_SUBTYPE_ID_STR;
                                    }
                                    return str3;
                                } catch (NumberFormatException e) {
                                    return InputMethodUtils.NOT_A_SUBTYPE_ID_STR;
                                }
                            }
                        }
                    }
                    return InputMethodUtils.NOT_A_SUBTYPE_ID_STR;
                }
            }
            return null;
        }

        private List<Pair<String, String>> loadInputMethodAndSubtypeHistoryLocked() {
            ArrayList arrayList = new ArrayList();
            String subtypeHistoryStr = getSubtypeHistoryStr();
            if (TextUtils.isEmpty(subtypeHistoryStr)) {
                return arrayList;
            }
            this.mInputMethodSplitter.setString(subtypeHistoryStr);
            while (this.mInputMethodSplitter.hasNext()) {
                this.mSubtypeSplitter.setString(this.mInputMethodSplitter.next());
                if (this.mSubtypeSplitter.hasNext()) {
                    String next = InputMethodUtils.NOT_A_SUBTYPE_ID_STR;
                    String next2 = this.mSubtypeSplitter.next();
                    if (this.mSubtypeSplitter.hasNext()) {
                        next = this.mSubtypeSplitter.next();
                    }
                    arrayList.add(new Pair(next2, next));
                }
            }
            return arrayList;
        }

        private String getSubtypeHistoryStr() {
            return getString(Settings.Secure.INPUT_METHODS_SUBTYPE_HISTORY, "");
        }

        public void putSelectedInputMethod(String str) {
            putString(Settings.Secure.DEFAULT_INPUT_METHOD, str);
        }

        public void putSelectedSubtype(int i) {
            putInt(Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE, i);
        }

        public String getSelectedInputMethod() {
            return getString(Settings.Secure.DEFAULT_INPUT_METHOD, null);
        }

        public boolean isSubtypeSelected() {
            return getSelectedInputMethodSubtypeHashCode() != -1;
        }

        private int getSelectedInputMethodSubtypeHashCode() {
            return getInt(Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE, -1);
        }

        public boolean isShowImeWithHardKeyboardEnabled() {
            return getBoolean(Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD, false);
        }

        public void setShowImeWithHardKeyboard(boolean z) {
            putBoolean(Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD, z);
        }

        public int getCurrentUserId() {
            return this.mCurrentUserId;
        }

        public int getSelectedInputMethodSubtypeId(String str) {
            InputMethodInfo inputMethodInfo = this.mMethodMap.get(str);
            if (inputMethodInfo == null) {
                return -1;
            }
            return InputMethodUtils.getSubtypeIdFromHashCode(inputMethodInfo, getSelectedInputMethodSubtypeHashCode());
        }

        public void saveCurrentInputMethodAndSubtypeToHistory(String str, InputMethodSubtype inputMethodSubtype) {
            String strValueOf = InputMethodUtils.NOT_A_SUBTYPE_ID_STR;
            if (inputMethodSubtype != null) {
                strValueOf = String.valueOf(inputMethodSubtype.hashCode());
            }
            if (InputMethodUtils.canAddToLastInputMethod(inputMethodSubtype)) {
                addSubtypeToHistory(str, strValueOf);
            }
        }

        public HashMap<InputMethodInfo, List<InputMethodSubtype>> getExplicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked(Context context) {
            HashMap<InputMethodInfo, List<InputMethodSubtype>> map = new HashMap<>();
            for (InputMethodInfo inputMethodInfo : getEnabledInputMethodListLocked()) {
                map.put(inputMethodInfo, getEnabledInputMethodSubtypeListLocked(context, inputMethodInfo, true));
            }
            return map;
        }

        public void dumpLocked(Printer printer, String str) {
            printer.println(str + "mCurrentUserId=" + this.mCurrentUserId);
            printer.println(str + "mCurrentProfileIds=" + Arrays.toString(this.mCurrentProfileIds));
            printer.println(str + "mCopyOnWrite=" + this.mCopyOnWrite);
            printer.println(str + "mEnabledInputMethodsStrCache=" + this.mEnabledInputMethodsStrCache);
        }
    }

    @VisibleForTesting
    public static ArrayList<Locale> getSuitableLocalesForSpellChecker(Locale locale) {
        Locale locale2;
        Locale locale3;
        Locale locale4;
        Locale locale5;
        Locale locale6 = null;
        if (locale != null) {
            String language = locale.getLanguage();
            boolean z = !TextUtils.isEmpty(language);
            String country = locale.getCountry();
            boolean z2 = !TextUtils.isEmpty(country);
            String variant = locale.getVariant();
            boolean z3 = !TextUtils.isEmpty(variant);
            if (z && z2 && z3) {
                locale4 = new Locale(language, country, variant);
            } else {
                locale4 = null;
            }
            if (z && z2) {
                locale5 = new Locale(language, country);
            } else {
                locale5 = null;
            }
            if (z) {
                locale6 = new Locale(language);
            }
            locale3 = locale5;
            locale2 = locale6;
            locale6 = locale4;
        } else {
            locale2 = null;
            locale3 = null;
        }
        ArrayList<Locale> arrayList = new ArrayList<>();
        if (locale6 != null) {
            arrayList.add(locale6);
        }
        if (Locale.ENGLISH.equals(locale2)) {
            if (locale3 != null) {
                if (locale3 != null) {
                    arrayList.add(locale3);
                }
                if (!LOCALE_EN_US.equals(locale3)) {
                    arrayList.add(LOCALE_EN_US);
                }
                if (!LOCALE_EN_GB.equals(locale3)) {
                    arrayList.add(LOCALE_EN_GB);
                }
                arrayList.add(Locale.ENGLISH);
            } else {
                arrayList.add(Locale.ENGLISH);
                arrayList.add(LOCALE_EN_US);
                arrayList.add(LOCALE_EN_GB);
            }
        } else {
            if (locale3 != null) {
                arrayList.add(locale3);
            }
            if (locale2 != null) {
                arrayList.add(locale2);
            }
            arrayList.add(LOCALE_EN_US);
            arrayList.add(LOCALE_EN_GB);
            arrayList.add(Locale.ENGLISH);
        }
        return arrayList;
    }

    public static boolean isSoftInputModeStateVisibleAllowed(int i, int i2) {
        if (i < 28) {
            return true;
        }
        return ((i2 & 1) == 0 || (i2 & 2) == 0) ? false : true;
    }
}
