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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParserException;

public class KeyphraseEnrollmentInfo {
    public static final String ACTION_MANAGE_VOICE_KEYPHRASES = "com.android.intent.action.MANAGE_VOICE_KEYPHRASES";
    public static final String EXTRA_VOICE_KEYPHRASE_ACTION = "com.android.intent.extra.VOICE_KEYPHRASE_ACTION";
    public static final String EXTRA_VOICE_KEYPHRASE_HINT_TEXT = "com.android.intent.extra.VOICE_KEYPHRASE_HINT_TEXT";
    public static final String EXTRA_VOICE_KEYPHRASE_LOCALE = "com.android.intent.extra.VOICE_KEYPHRASE_LOCALE";
    private static final String TAG = "KeyphraseEnrollmentInfo";
    private static final String VOICE_KEYPHRASE_META_DATA = "android.voice_enrollment";
    private String mEnrollmentPackage;
    private KeyphraseMetadata[] mKeyphrases;
    private String mParseError;

    public KeyphraseEnrollmentInfo(PackageManager pm) {
        int type;
        List<ResolveInfo> ris = pm.queryIntentActivities(new Intent(ACTION_MANAGE_VOICE_KEYPHRASES), 65536);
        if (ris == null || ris.isEmpty()) {
            this.mParseError = "No enrollment application found";
            return;
        }
        boolean found = false;
        ApplicationInfo ai = null;
        Iterator<ResolveInfo> it = ris.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ResolveInfo ri = it.next();
            try {
                ai = pm.getApplicationInfo(ri.activityInfo.packageName, 128);
                if ((ai.flags & 1073741824) != 0) {
                    if (Manifest.permission.MANAGE_VOICE_KEYPHRASES.equals(ai.permission)) {
                        this.mEnrollmentPackage = ai.packageName;
                        found = true;
                        break;
                    }
                    Slog.w(TAG, ai.packageName + " does not require MANAGE_VOICE_KEYPHRASES");
                } else {
                    Slog.w(TAG, ai.packageName + "is not a privileged system app");
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.w(TAG, "error parsing voice enrollment meta-data", e);
            }
        }
        if (!found) {
            this.mKeyphrases = null;
            this.mParseError = "No suitable enrollment application found";
            return;
        }
        XmlResourceParser parser = null;
        try {
            try {
                try {
                    parser = ai.loadXmlMetaData(pm, VOICE_KEYPHRASE_META_DATA);
                    if (parser == null) {
                        this.mParseError = "No android.voice_enrollment meta-data for " + ai.packageName;
                        if (parser != null) {
                            parser.close();
                        }
                    } else {
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
                            initializeKeyphrasesFromTypedArray(array);
                            array.recycle();
                            if (parser != null) {
                                parser.close();
                            }
                        } else {
                            this.mParseError = "Meta-data does not start with voice-enrollment-application tag";
                            if (parser != null) {
                                parser.close();
                            }
                        }
                    }
                } catch (Throwable th) {
                    if (parser != null) {
                        parser.close();
                    }
                    throw th;
                }
            } catch (XmlPullParserException e2) {
                this.mParseError = "Error parsing keyphrase enrollment meta-data: " + e2;
                Slog.w(TAG, "error parsing keyphrase enrollment meta-data", e2);
                if (parser != null) {
                    parser.close();
                }
            }
        } catch (PackageManager.NameNotFoundException e3) {
            this.mParseError = "Error parsing keyphrase enrollment meta-data: " + e3;
            Slog.w(TAG, "error parsing keyphrase enrollment meta-data", e3);
            if (parser != null) {
                parser.close();
            }
        } catch (IOException e4) {
            this.mParseError = "Error parsing keyphrase enrollment meta-data: " + e4;
            Slog.w(TAG, "error parsing keyphrase enrollment meta-data", e4);
            if (parser != null) {
                parser.close();
            }
        }
    }

    private void initializeKeyphrasesFromTypedArray(TypedArray array) {
        int searchKeyphraseId = array.getInt(0, -1);
        if (searchKeyphraseId <= 0) {
            this.mParseError = "No valid searchKeyphraseId specified in meta-data";
            Slog.w(TAG, this.mParseError);
            return;
        }
        String searchKeyphrase = array.getString(1);
        if (searchKeyphrase == null) {
            this.mParseError = "No valid searchKeyphrase specified in meta-data";
            Slog.w(TAG, this.mParseError);
            return;
        }
        String searchKeyphraseSupportedLocales = array.getString(2);
        if (searchKeyphraseSupportedLocales == null) {
            this.mParseError = "No valid searchKeyphraseSupportedLocales specified in meta-data";
            Slog.w(TAG, this.mParseError);
            return;
        }
        ArraySet<Locale> locales = new ArraySet<>();
        if (!TextUtils.isEmpty(searchKeyphraseSupportedLocales)) {
            try {
                String[] supportedLocalesDelimited = searchKeyphraseSupportedLocales.split(",");
                for (String str : supportedLocalesDelimited) {
                    locales.add(Locale.forLanguageTag(str));
                }
            } catch (Exception ex) {
                this.mParseError = "Error reading searchKeyphraseSupportedLocales from meta-data";
                Slog.w(TAG, this.mParseError, ex);
                return;
            }
        }
        int recognitionModes = array.getInt(3, -1);
        if (recognitionModes < 0) {
            this.mParseError = "No valid searchKeyphraseRecognitionFlags specified in meta-data";
            Slog.w(TAG, this.mParseError);
        } else {
            this.mKeyphrases = new KeyphraseMetadata[1];
            this.mKeyphrases[0] = new KeyphraseMetadata(searchKeyphraseId, searchKeyphrase, locales, recognitionModes);
        }
    }

    public String getParseError() {
        return this.mParseError;
    }

    public KeyphraseMetadata[] listKeyphraseMetadata() {
        return this.mKeyphrases;
    }

    public Intent getManageKeyphraseIntent(int action, String keyphrase, Locale locale) {
        if (this.mEnrollmentPackage == null || this.mEnrollmentPackage.isEmpty()) {
            Slog.w(TAG, "No enrollment application exists");
            return null;
        }
        if (getKeyphraseMetadata(keyphrase, locale) != null) {
            return new Intent(ACTION_MANAGE_VOICE_KEYPHRASES).setPackage(this.mEnrollmentPackage).putExtra(EXTRA_VOICE_KEYPHRASE_HINT_TEXT, keyphrase).putExtra(EXTRA_VOICE_KEYPHRASE_LOCALE, locale.toLanguageTag()).putExtra(EXTRA_VOICE_KEYPHRASE_ACTION, action);
        }
        return null;
    }

    public KeyphraseMetadata getKeyphraseMetadata(String keyphrase, Locale locale) {
        if (this.mKeyphrases == null || this.mKeyphrases.length == 0) {
            Slog.w(TAG, "Enrollment application doesn't support keyphrases");
            return null;
        }
        KeyphraseMetadata[] arr$ = this.mKeyphrases;
        for (KeyphraseMetadata keyphraseMetadata : arr$) {
            if (keyphraseMetadata.supportsPhrase(keyphrase) && keyphraseMetadata.supportsLocale(locale)) {
                return keyphraseMetadata;
            }
        }
        Slog.w(TAG, "Enrollment application doesn't support the given keyphrase/locale");
        return null;
    }

    public String toString() {
        return "KeyphraseEnrollmentInfo [Keyphrases=" + Arrays.toString(this.mKeyphrases) + ", EnrollmentPackage=" + this.mEnrollmentPackage + ", ParseError=" + this.mParseError + "]";
    }
}
