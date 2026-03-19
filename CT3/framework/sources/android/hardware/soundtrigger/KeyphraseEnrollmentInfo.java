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
import dalvik.system.BlockGuard;
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

    public KeyphraseEnrollmentInfo(PackageManager pm) {
        List<ResolveInfo> ris = pm.queryIntentActivities(new Intent(ACTION_MANAGE_VOICE_KEYPHRASES), 65536);
        if (ris == null || ris.isEmpty()) {
            this.mParseError = "No enrollment applications found";
            this.mKeyphrasePackageMap = Collections.emptyMap();
            this.mKeyphrases = null;
            return;
        }
        List<String> parseErrors = new LinkedList<>();
        this.mKeyphrasePackageMap = new HashMap();
        for (ResolveInfo ri : ris) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(ri.activityInfo.packageName, 128);
                if ((ai.privateFlags & 8) == 0) {
                    Slog.w(TAG, ai.packageName + "is not a privileged system app");
                } else if (!Manifest.permission.MANAGE_VOICE_KEYPHRASES.equals(ai.permission)) {
                    Slog.w(TAG, ai.packageName + " does not require MANAGE_VOICE_KEYPHRASES");
                } else {
                    this.mKeyphrasePackageMap.put(getKeyphraseMetadataFromApplicationInfo(pm, ai, parseErrors), ai.packageName);
                }
            } catch (PackageManager.NameNotFoundException e) {
                String error = "error parsing voice enrollment meta-data for " + ri.activityInfo.packageName;
                parseErrors.add(error + ": " + e);
                Slog.w(TAG, error, e);
            }
        }
        if (this.mKeyphrasePackageMap.isEmpty()) {
            parseErrors.add("No suitable enrollment application found");
            Slog.w(TAG, "No suitable enrollment application found");
            this.mKeyphrases = null;
        } else {
            this.mKeyphrases = (KeyphraseMetadata[]) this.mKeyphrasePackageMap.keySet().toArray(new KeyphraseMetadata[this.mKeyphrasePackageMap.size()]);
        }
        if (parseErrors.isEmpty()) {
            return;
        }
        this.mParseError = TextUtils.join("\n", parseErrors);
    }

    private KeyphraseMetadata getKeyphraseMetadataFromApplicationInfo(PackageManager pm, ApplicationInfo ai, List<String> parseErrors) {
        XmlResourceParser parser;
        int type;
        XmlResourceParser xmlResourceParser = null;
        String packageName = ai.packageName;
        KeyphraseMetadata keyphraseMetadata = null;
        try {
            try {
                try {
                    try {
                        parser = ai.loadXmlMetaData(pm, VOICE_KEYPHRASE_META_DATA);
                    } catch (PackageManager.NameNotFoundException e) {
                        String error = "Error parsing keyphrase enrollment meta-data for " + packageName;
                        parseErrors.add(error + ": " + e);
                        Slog.w(TAG, error, e);
                        if (0 != 0) {
                            xmlResourceParser.close();
                        }
                    }
                } catch (IOException e2) {
                    String error2 = "Error parsing keyphrase enrollment meta-data for " + packageName;
                    parseErrors.add(error2 + ": " + e2);
                    Slog.w(TAG, error2, e2);
                    if (0 != 0) {
                        xmlResourceParser.close();
                    }
                }
            } catch (XmlPullParserException e3) {
                String error3 = "Error parsing keyphrase enrollment meta-data for " + packageName;
                parseErrors.add(error3 + ": " + e3);
                Slog.w(TAG, error3, e3);
                if (0 != 0) {
                    xmlResourceParser.close();
                }
            }
            if (parser == null) {
                String error4 = "No android.voice_enrollment meta-data for " + packageName;
                parseErrors.add(error4);
                Slog.w(TAG, error4);
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            Resources res = pm.getResourcesForApplication(ai);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            do {
                type = parser.next();
                if (type == 1) {
                    break;
                }
            } while (type != 2);
            String nodeName = parser.getName();
            if ("voice-enrollment-application".equals(nodeName)) {
                TypedArray array = res.obtainAttributes(attrs, R.styleable.VoiceEnrollmentApplication);
                keyphraseMetadata = getKeyphraseFromTypedArray(array, packageName, parseErrors);
                array.recycle();
                if (parser != null) {
                    parser.close();
                }
                return keyphraseMetadata;
            }
            String error5 = "Meta-data does not start with voice-enrollment-application tag for " + packageName;
            parseErrors.add(error5);
            Slog.w(TAG, error5);
            if (parser != null) {
                parser.close();
            }
            return null;
        } catch (Throwable th) {
            if (0 != 0) {
                xmlResourceParser.close();
            }
            throw th;
        }
    }

    private KeyphraseMetadata getKeyphraseFromTypedArray(TypedArray array, String packageName, List<String> parseErrors) throws BlockGuard.BlockGuardPolicyException {
        int searchKeyphraseId = array.getInt(0, -1);
        if (searchKeyphraseId <= 0) {
            String error = "No valid searchKeyphraseId specified in meta-data for " + packageName;
            parseErrors.add(error);
            Slog.w(TAG, error);
            return null;
        }
        String searchKeyphrase = array.getString(1);
        if (searchKeyphrase == null) {
            String error2 = "No valid searchKeyphrase specified in meta-data for " + packageName;
            parseErrors.add(error2);
            Slog.w(TAG, error2);
            return null;
        }
        String searchKeyphraseSupportedLocales = array.getString(2);
        if (searchKeyphraseSupportedLocales == null) {
            String error3 = "No valid searchKeyphraseSupportedLocales specified in meta-data for " + packageName;
            parseErrors.add(error3);
            Slog.w(TAG, error3);
            return null;
        }
        ArraySet<Locale> locales = new ArraySet<>();
        if (!TextUtils.isEmpty(searchKeyphraseSupportedLocales)) {
            try {
                String[] supportedLocalesDelimited = searchKeyphraseSupportedLocales.split(",");
                for (String str : supportedLocalesDelimited) {
                    locales.add(Locale.forLanguageTag(str));
                }
            } catch (Exception e) {
                String error4 = "Error reading searchKeyphraseSupportedLocales from meta-data for " + packageName;
                parseErrors.add(error4);
                Slog.w(TAG, error4);
                return null;
            }
        }
        int recognitionModes = array.getInt(3, -1);
        if (recognitionModes < 0) {
            String error5 = "No valid searchKeyphraseRecognitionFlags specified in meta-data for " + packageName;
            parseErrors.add(error5);
            Slog.w(TAG, error5);
            return null;
        }
        return new KeyphraseMetadata(searchKeyphraseId, searchKeyphrase, locales, recognitionModes);
    }

    public String getParseError() {
        return this.mParseError;
    }

    public KeyphraseMetadata[] listKeyphraseMetadata() {
        return this.mKeyphrases;
    }

    public Intent getManageKeyphraseIntent(int action, String keyphrase, Locale locale) {
        if (this.mKeyphrasePackageMap == null || this.mKeyphrasePackageMap.isEmpty()) {
            Slog.w(TAG, "No enrollment application exists");
            return null;
        }
        KeyphraseMetadata keyphraseMetadata = getKeyphraseMetadata(keyphrase, locale);
        if (keyphraseMetadata == null) {
            return null;
        }
        Intent intent = new Intent(ACTION_MANAGE_VOICE_KEYPHRASES).setPackage(this.mKeyphrasePackageMap.get(keyphraseMetadata)).putExtra(EXTRA_VOICE_KEYPHRASE_HINT_TEXT, keyphrase).putExtra(EXTRA_VOICE_KEYPHRASE_LOCALE, locale.toLanguageTag()).putExtra(EXTRA_VOICE_KEYPHRASE_ACTION, action);
        return intent;
    }

    public KeyphraseMetadata getKeyphraseMetadata(String keyphrase, Locale locale) {
        if (this.mKeyphrases != null && this.mKeyphrases.length > 0) {
            for (KeyphraseMetadata keyphraseMetadata : this.mKeyphrases) {
                if (keyphraseMetadata.supportsPhrase(keyphrase) && keyphraseMetadata.supportsLocale(locale)) {
                    return keyphraseMetadata;
                }
            }
        }
        Slog.w(TAG, "No Enrollment application supports the given keyphrase/locale");
        return null;
    }

    public String toString() {
        return "KeyphraseEnrollmentInfo [Keyphrases=" + this.mKeyphrasePackageMap.toString() + ", ParseError=" + this.mParseError + "]";
    }
}
