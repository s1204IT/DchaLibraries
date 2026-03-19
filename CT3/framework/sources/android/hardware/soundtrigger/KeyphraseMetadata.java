package android.hardware.soundtrigger;

import android.util.ArraySet;
import java.util.Locale;

public class KeyphraseMetadata {
    public final int id;
    public final String keyphrase;
    public final int recognitionModeFlags;
    public final ArraySet<Locale> supportedLocales;

    public KeyphraseMetadata(int id, String keyphrase, ArraySet<Locale> supportedLocales, int recognitionModeFlags) {
        this.id = id;
        this.keyphrase = keyphrase;
        this.supportedLocales = supportedLocales;
        this.recognitionModeFlags = recognitionModeFlags;
    }

    public String toString() {
        return "id=" + this.id + ", keyphrase=" + this.keyphrase + ", supported-locales=" + this.supportedLocales + ", recognition-modes=" + this.recognitionModeFlags;
    }

    public boolean supportsPhrase(String phrase) {
        if (this.keyphrase.isEmpty()) {
            return true;
        }
        return this.keyphrase.equalsIgnoreCase(phrase);
    }

    public boolean supportsLocale(Locale locale) {
        if (this.supportedLocales.isEmpty()) {
            return true;
        }
        return this.supportedLocales.contains(locale);
    }
}
