package jp.co.omronsoft.iwnnime.ml;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class KeyboardLanguagePackData extends KeyboardSkinData {
    public static final String DEFAULT_LANG = "ja";
    private static final String LANGUAGE_PACK_ACTION = "jp.co.omronsoft.iwnnime.LANGUAGE_PACK";
    public static final int READ_RESOURCE_TAG_4KEY = 12;
    public static final int READ_RESOURCE_TAG_ALPHA = 0;
    public static final int READ_RESOURCE_TAG_ALPHA_SHIFT = 1;
    public static final int READ_RESOURCE_TAG_ALPHA_SHIFT_VOICE = 3;
    public static final int READ_RESOURCE_TAG_ALPHA_VOICE = 2;
    public static final int READ_RESOURCE_TAG_ORIGINAL = 8;
    public static final int READ_RESOURCE_TAG_ORIGINAL_SHIFT = 9;
    public static final int READ_RESOURCE_TAG_ORIGINAL_SHIFT_VOICE = 11;
    public static final int READ_RESOURCE_TAG_ORIGINAL_VOICE = 10;
    public static final int READ_RESOURCE_TAG_POPUP = 13;
    public static final int READ_RESOURCE_TAG_SYMBOL = 4;
    public static final int READ_RESOURCE_TAG_SYMBOL_SHIFT = 5;
    public static final int READ_RESOURCE_TAG_SYMBOL_SHIFT_VOICE = 7;
    public static final int READ_RESOURCE_TAG_SYMBOL_VOICE = 6;
    private static final String TAG = "iWnn";
    private static KeyboardLanguagePackData mLangPack;
    private String mClassName = "ja";
    private static final String[] READ_XML_TAG_TABLE = {"keyboardAlpha", "keyboardAlphaShift", "keyboardAlphaVoice", "keyboardAlphaShiftVoice", "keyboardSymbol", "keyboardSymbolShift", "keyboardSymbolVoice", "keyboardSymbolShiftVoice", "keyboardOriginal", "keyboardOriginalShift", "keyboardOriginalVoice", "keyboardOriginalShiftVoice", "keyboard4Key", "keyboardPopup"};
    private static final String SUBTYPE_MODE_KEYBOARD = "keyboard";
    private static final boolean DEBUG = false;
    public static final InputMethodSubtype SUBTYPE_JAPANESE_INSTANCE = new InputMethodSubtype(R.string.ti_choose_language_entry_ja_txt, R.drawable.iwnnime_icon, "ja", SUBTYPE_MODE_KEYBOARD, LoggingEvents.EXTRA_CALLING_APP_NAME, DEBUG, DEBUG);

    private KeyboardLanguagePackData() {
        mLangPack = null;
        reset();
    }

    public static synchronized KeyboardLanguagePackData getInstance() {
        if (mLangPack == null) {
            mLangPack = new KeyboardLanguagePackData();
        }
        return mLangPack;
    }

    public List<ResolveInfo> getLangagePackInfo(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(LANGUAGE_PACK_ACTION);
        List<ResolveInfo> resolveInfo = pm.queryIntentActivities(intent, 0);
        Collections.sort(resolveInfo, new ResolveInfo.DisplayNameComparator(pm));
        return resolveInfo;
    }

    public String getLangPackClassName(Context context, int langtype) {
        Locale langLocale;
        String lang;
        List<ResolveInfo> resolveInfo;
        InputMethodInfo imi;
        String label;
        if (context == null || (langLocale = LanguageManager.getChosenLocale(langtype)) == null || (lang = langLocale.toString()) == null || "ja".equals(lang) || IWnnIME.SUBTYPE_LOCALE_EMOJI_INPUT.equals(lang) || (resolveInfo = getLangagePackInfo(context)) == null || (imi = getMyselfInputMethodInfo(context)) == null) {
            return null;
        }
        String targetName = null;
        int cntHasSubType = imi.getSubtypeCount();
        int index = 0;
        while (true) {
            if (index >= cntHasSubType) {
                break;
            }
            InputMethodSubtype ims = imi.getSubtypeAt(index);
            if (ims == null || !lang.equals(ims.getLocale())) {
                index++;
            } else {
                targetName = ims.getExtraValue();
                break;
            }
        }
        if (targetName == null) {
            return null;
        }
        for (ResolveInfo info : resolveInfo) {
            ActivityInfo actInfo = info.activityInfo;
            if (actInfo != null && actInfo.name != null && (label = actInfo.name.toString()) != null) {
                String packageName = label.substring(0, label.lastIndexOf(46));
                if (targetName.equals(packageName)) {
                    return label;
                }
            }
        }
        return null;
    }

    public InputMethodInfo getMyselfInputMethodInfo(Context context) {
        InputMethodManager manager = (InputMethodManager) context.getSystemService("input_method");
        if (manager == null) {
            return null;
        }
        List<InputMethodInfo> imiList = manager.getInputMethodList();
        String packageName = context.getPackageName();
        for (int cnt = 0; cnt < imiList.size(); cnt++) {
            InputMethodInfo imi = imiList.get(cnt);
            if (packageName.equals(imi.getPackageName())) {
                return imi;
            }
        }
        return null;
    }

    public void setInputMethodSubtypeInstallLangPack(Context context) {
        InputMethodInfo imi = getMyselfInputMethodInfo(context);
        if (imi != null) {
            int cntHasSubType = imi.getSubtypeCount();
            HashMap<String, InputMethodSubtype> currentSubTypeMap = new HashMap<>();
            for (int index = 0; index < cntHasSubType; index++) {
                InputMethodSubtype ims = imi.getSubtypeAt(index);
                if (ims != null) {
                    String templocale = ims.getLocale();
                    if (!"ja".equals(templocale) && !IWnnIME.SUBTYPE_LOCALE_EMOJI_INPUT.equals(templocale)) {
                        currentSubTypeMap.put(ims.getExtraValue(), ims);
                    }
                }
            }
            List<ResolveInfo> resolveInfo = getLangagePackInfo(context);
            int infoSize = resolveInfo.size();
            ArrayList<InputMethodSubtype> workSubType = new ArrayList<>();
            boolean hasDifferentSubtype = DEBUG;
            for (int cnt = 0; cnt < infoSize; cnt++) {
                ResolveInfo info = resolveInfo.get(cnt);
                ActivityInfo actInfo = info.activityInfo;
                String label = actInfo.name.toString();
                String packageName = label.substring(0, label.lastIndexOf(46));
                InputMethodSubtype targetIms = currentSubTypeMap.get(packageName);
                if (targetIms != null) {
                    workSubType.add(targetIms);
                    String imsLocale = targetIms.getLocale();
                    String localeCode = LanguageManager.getChosenLocaleCode(imsLocale);
                    if (localeCode == null) {
                        LanguageManager.addLanguageList(imsLocale);
                    }
                } else {
                    hasDifferentSubtype = true;
                    String locale = getLocale(context, packageName);
                    if (locale != null) {
                        int nameId = LanguageManager.getNameId(locale);
                        if (nameId < 0) {
                            nameId = 0;
                            LanguageManager.addLanguageList(locale);
                        }
                        workSubType.add(new InputMethodSubtype(nameId, R.drawable.iwnnime_icon, locale, SUBTYPE_MODE_KEYBOARD, packageName, DEBUG, DEBUG));
                    }
                }
            }
            int cntNewSubtype = workSubType.size();
            if (cntNewSubtype != currentSubTypeMap.size() || (cntNewSubtype != 0 && hasDifferentSubtype)) {
                if (cntNewSubtype == 0) {
                    workSubType.add(SUBTYPE_JAPANESE_INSTANCE);
                }
                InputMethodSubtype[] ims2 = (InputMethodSubtype[]) workSubType.toArray(new InputMethodSubtype[workSubType.size()]);
                InputMethodManager manager = (InputMethodManager) context.getSystemService("input_method");
                if (manager != null) {
                    manager.setAdditionalInputMethodSubtypes(imi.getId(), ims2);
                }
            }
        }
    }

    public String getConfFile(Context context, String packageName) {
        return getStringQuery(context, packageName, "conf_so_file", null);
    }

    public String getLocale(Context context, String packageName) {
        return getStringQuery(context, packageName, LoggingEvents.VoiceIme.EXTRA_START_LOCALE, null);
    }

    public String conversionSkinKey(Context context, String packageName, int resourceId) {
        return getStringQuery(context, packageName, "skin_key", Integer.toString(resourceId));
    }

    public int getLangCategory(Context context) {
        return getIntQuery(context, this.mPackageName, "language_category", null);
    }

    private String getStringQuery(Context context, String packageName, String key, String selection) {
        Uri uri = Uri.parse("content://" + packageName + "/" + key);
        Cursor cursor = context.getContentResolver().query(uri, null, selection, null, null);
        if (cursor == null) {
            return null;
        }
        String str = null;
        if (cursor.moveToFirst()) {
            str = cursor.getString(0);
        }
        cursor.close();
        return str;
    }

    private int getIntQuery(Context context, String packageName, String key, String selection) {
        Uri uri = Uri.parse("content://" + packageName + "/" + key);
        Cursor cursor = context.getContentResolver().query(uri, null, selection, null, null);
        int ret = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                ret = cursor.getInt(0);
            }
            cursor.close();
        }
        return ret;
    }

    public void init(Context context, String classname) {
        reset();
        this.mPm = context.getPackageManager();
        if (classname != null && !classname.equals("ja") && this.mPm != null) {
            String packagename = classname.substring(0, classname.lastIndexOf(46));
            try {
                ComponentName name = new ComponentName(packagename, classname);
                ActivityInfo activityInfo = this.mPm.getActivityInfo(name, 128);
                if (activityInfo.metaData != null) {
                    this.mClassName = classname;
                    this.mPackageName = packagename;
                    this.mResourceId = activityInfo.metaData.getInt("resourcemaster");
                    makeResourceHashMap();
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "KeyboardLanguagePackData::init()" + e.toString());
            }
        }
    }

    public void reset() {
        this.mPm = null;
        this.mResourceId = 0;
        this.mClassName = "ja";
        this.mPackageName = "ja";
    }

    public String getEnableKeyboardLanguagePackDataName() {
        return this.mPackageName;
    }

    public String getEnableKeyboardLanguagePackDataClassName() {
        return this.mClassName;
    }

    @Override
    public boolean isValid() {
        if (this.mPackageName.equals("ja")) {
            return DEBUG;
        }
        return true;
    }

    public XmlResourceParser getXmlParser(int resId) {
        if (resId < 0 || READ_XML_TAG_TABLE.length <= resId) {
            return null;
        }
        return getXmlParser(READ_XML_TAG_TABLE[resId]);
    }

    public XmlResourceParser getXmlParser(String key) {
        XmlResourceParser parser = getSettingXmlParser();
        XmlResourceParser result = null;
        if (parser == null || this.mPm == null) {
            return null;
        }
        XmlResourceParser parser2 = getStartTag(key, parser);
        if (parser2 != null) {
            Integer resValueId = Integer.valueOf(parser2.getAttributeResourceValue(0, 0));
            result = this.mPm.getXml(this.mPackageName, resValueId.intValue(), null);
        }
        return result;
    }

    public XmlResourceParser getSimpleXmlParser(Integer id) {
        if (this.mPm == null) {
            return null;
        }
        return this.mPm.getXml(this.mPackageName, id.intValue(), null);
    }

    public Drawable getDrawableDirect(int resourceid) {
        if (!isValid() || this.mPm == null || resourceid == 0) {
            return null;
        }
        return this.mPm.getDrawable(this.mPackageName, resourceid, null);
    }

    public CharSequence getTextDirect(int resourceid) {
        if (!isValid() || this.mPm == null) {
            return null;
        }
        return this.mPm.getText(this.mPackageName, resourceid, null);
    }
}
