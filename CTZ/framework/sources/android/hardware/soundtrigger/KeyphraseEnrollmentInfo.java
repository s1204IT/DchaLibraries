package android.hardware.soundtrigger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;

public class KeyphraseEnrollmentInfo {
    public static final String ACTION_MANAGE_VOICE_KEYPHRASES = "com.android.intent.action.MANAGE_VOICE_KEYPHRASES";
    public static final String EXTRA_VOICE_KEYPHRASE_ACTION = "com.android.intent.extra.VOICE_KEYPHRASE_ACTION";
    public static final String EXTRA_VOICE_KEYPHRASE_HINT_TEXT = "com.android.intent.extra.VOICE_KEYPHRASE_HINT_TEXT";
    public static final String EXTRA_VOICE_KEYPHRASE_LOCALE = "com.android.intent.extra.VOICE_KEYPHRASE_LOCALE";
    private static final String TAG = "KeyphraseEnrollmentInfo";
    private static final String VOICE_KEYPHRASE_META_DATA = "android.voice_enrollment";
    private final Map<KeyphraseMetadata, String> mKeyphrasePackageMap;
    private final KeyphraseMetadata[] mKeyphrases;
    private String mParseError;

    public KeyphraseEnrollmentInfo(PackageManager packageManager) throws Throwable {
        List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(new Intent(ACTION_MANAGE_VOICE_KEYPHRASES), 65536);
        if (listQueryIntentActivities == null || listQueryIntentActivities.isEmpty()) {
            this.mParseError = "No enrollment applications found";
            this.mKeyphrasePackageMap = Collections.emptyMap();
            this.mKeyphrases = null;
            return;
        }
        LinkedList linkedList = new LinkedList();
        this.mKeyphrasePackageMap = new HashMap();
        for (ResolveInfo resolveInfo : listQueryIntentActivities) {
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(resolveInfo.activityInfo.packageName, 128);
                if ((applicationInfo.privateFlags & 8) == 0) {
                    Slog.w(TAG, applicationInfo.packageName + "is not a privileged system app");
                } else if (!Manifest.permission.MANAGE_VOICE_KEYPHRASES.equals(applicationInfo.permission)) {
                    Slog.w(TAG, applicationInfo.packageName + " does not require MANAGE_VOICE_KEYPHRASES");
                } else {
                    KeyphraseMetadata keyphraseMetadataFromApplicationInfo = getKeyphraseMetadataFromApplicationInfo(packageManager, applicationInfo, linkedList);
                    if (keyphraseMetadataFromApplicationInfo != null) {
                        this.mKeyphrasePackageMap.put(keyphraseMetadataFromApplicationInfo, applicationInfo.packageName);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                String str = "error parsing voice enrollment meta-data for " + resolveInfo.activityInfo.packageName;
                linkedList.add(str + ": " + e);
                Slog.w(TAG, str, e);
            }
        }
        if (this.mKeyphrasePackageMap.isEmpty()) {
            linkedList.add("No suitable enrollment application found");
            Slog.w(TAG, "No suitable enrollment application found");
            this.mKeyphrases = null;
        } else {
            this.mKeyphrases = (KeyphraseMetadata[]) this.mKeyphrasePackageMap.keySet().toArray(new KeyphraseMetadata[this.mKeyphrasePackageMap.size()]);
        }
        if (!linkedList.isEmpty()) {
            this.mParseError = TextUtils.join("\n", linkedList);
        }
    }

    private KeyphraseMetadata getKeyphraseMetadataFromApplicationInfo(PackageManager packageManager, ApplicationInfo applicationInfo, List<String> list) throws Throwable {
        KeyphraseMetadata keyphraseFromTypedArray;
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        int next;
        String str = applicationInfo.packageName;
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                xmlResourceParserLoadXmlMetaData = applicationInfo.loadXmlMetaData(packageManager, VOICE_KEYPHRASE_META_DATA);
                try {
                    try {
                    } catch (Throwable th) {
                        th = th;
                        xmlResourceParser = xmlResourceParserLoadXmlMetaData;
                        if (xmlResourceParser != null) {
                            xmlResourceParser.close();
                        }
                        throw th;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e = e;
                    keyphraseFromTypedArray = null;
                } catch (IOException e2) {
                    e = e2;
                    keyphraseFromTypedArray = null;
                } catch (XmlPullParserException e3) {
                    e = e3;
                    keyphraseFromTypedArray = null;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (PackageManager.NameNotFoundException e4) {
            e = e4;
            keyphraseFromTypedArray = null;
        } catch (IOException e5) {
            e = e5;
            keyphraseFromTypedArray = null;
        } catch (XmlPullParserException e6) {
            e = e6;
            keyphraseFromTypedArray = null;
        }
        if (xmlResourceParserLoadXmlMetaData == null) {
            String str2 = "No android.voice_enrollment meta-data for " + str;
            list.add(str2);
            Slog.w(TAG, str2);
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            return null;
        }
        Resources resourcesForApplication = packageManager.getResourcesForApplication(applicationInfo);
        AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
        do {
            next = xmlResourceParserLoadXmlMetaData.next();
            if (next == 1) {
                break;
            }
        } while (next != 2);
        if (!"voice-enrollment-application".equals(xmlResourceParserLoadXmlMetaData.getName())) {
            String str3 = "Meta-data does not start with voice-enrollment-application tag for " + str;
            list.add(str3);
            Slog.w(TAG, str3);
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            return null;
        }
        TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.VoiceEnrollmentApplication);
        keyphraseFromTypedArray = getKeyphraseFromTypedArray(typedArrayObtainAttributes, str, list);
        try {
            typedArrayObtainAttributes.recycle();
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
        } catch (PackageManager.NameNotFoundException e7) {
            e = e7;
            xmlResourceParser = xmlResourceParserLoadXmlMetaData;
            String str4 = "Error parsing keyphrase enrollment meta-data for " + str;
            list.add(str4 + ": " + e);
            Slog.w(TAG, str4, e);
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
        } catch (IOException e8) {
            e = e8;
            xmlResourceParser = xmlResourceParserLoadXmlMetaData;
            String str5 = "Error parsing keyphrase enrollment meta-data for " + str;
            list.add(str5 + ": " + e);
            Slog.w(TAG, str5, e);
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
        } catch (XmlPullParserException e9) {
            e = e9;
            xmlResourceParser = xmlResourceParserLoadXmlMetaData;
            String str6 = "Error parsing keyphrase enrollment meta-data for " + str;
            list.add(str6 + ": " + e);
            Slog.w(TAG, str6, e);
            if (xmlResourceParser != null) {
            }
        }
        return keyphraseFromTypedArray;
    }

    private KeyphraseMetadata getKeyphraseFromTypedArray(TypedArray typedArray, String str, List<String> list) {
        int i = typedArray.getInt(0, -1);
        if (i <= 0) {
            String str2 = "No valid searchKeyphraseId specified in meta-data for " + str;
            list.add(str2);
            Slog.w(TAG, str2);
            return null;
        }
        String string = typedArray.getString(1);
        if (string == null) {
            String str3 = "No valid searchKeyphrase specified in meta-data for " + str;
            list.add(str3);
            Slog.w(TAG, str3);
            return null;
        }
        String string2 = typedArray.getString(2);
        if (string2 == null) {
            String str4 = "No valid searchKeyphraseSupportedLocales specified in meta-data for " + str;
            list.add(str4);
            Slog.w(TAG, str4);
            return null;
        }
        ArraySet arraySet = new ArraySet();
        if (!TextUtils.isEmpty(string2)) {
            try {
                for (String str5 : string2.split(",")) {
                    arraySet.add(Locale.forLanguageTag(str5));
                }
            } catch (Exception e) {
                String str6 = "Error reading searchKeyphraseSupportedLocales from meta-data for " + str;
                list.add(str6);
                Slog.w(TAG, str6);
                return null;
            }
        }
        int i2 = typedArray.getInt(3, -1);
        if (i2 < 0) {
            String str7 = "No valid searchKeyphraseRecognitionFlags specified in meta-data for " + str;
            list.add(str7);
            Slog.w(TAG, str7);
            return null;
        }
        return new KeyphraseMetadata(i, string, arraySet, i2);
    }

    public String getParseError() {
        return this.mParseError;
    }

    public KeyphraseMetadata[] listKeyphraseMetadata() {
        return this.mKeyphrases;
    }

    public Intent getManageKeyphraseIntent(int i, String str, Locale locale) {
        if (this.mKeyphrasePackageMap == null || this.mKeyphrasePackageMap.isEmpty()) {
            Slog.w(TAG, "No enrollment application exists");
            return null;
        }
        KeyphraseMetadata keyphraseMetadata = getKeyphraseMetadata(str, locale);
        if (keyphraseMetadata != null) {
            return new Intent(ACTION_MANAGE_VOICE_KEYPHRASES).setPackage(this.mKeyphrasePackageMap.get(keyphraseMetadata)).putExtra(EXTRA_VOICE_KEYPHRASE_HINT_TEXT, str).putExtra(EXTRA_VOICE_KEYPHRASE_LOCALE, locale.toLanguageTag()).putExtra(EXTRA_VOICE_KEYPHRASE_ACTION, i);
        }
        return null;
    }

    public KeyphraseMetadata getKeyphraseMetadata(String str, Locale locale) {
        if (this.mKeyphrases != null && this.mKeyphrases.length > 0) {
            for (KeyphraseMetadata keyphraseMetadata : this.mKeyphrases) {
                if (keyphraseMetadata.supportsPhrase(str) && keyphraseMetadata.supportsLocale(locale)) {
                    return keyphraseMetadata;
                }
            }
        }
        Slog.w(TAG, "No enrollment application supports the given keyphrase/locale: '" + str + "'/" + locale);
        return null;
    }

    public String toString() {
        return "KeyphraseEnrollmentInfo [Keyphrases=" + this.mKeyphrasePackageMap.toString() + ", ParseError=" + this.mParseError + "]";
    }
}
