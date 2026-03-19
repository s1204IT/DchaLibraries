package android.media;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AudioPresentation {
    public static final int MASTERED_FOR_3D = 3;
    public static final int MASTERED_FOR_HEADPHONE = 4;
    public static final int MASTERED_FOR_STEREO = 1;
    public static final int MASTERED_FOR_SURROUND = 2;
    public static final int MASTERING_NOT_INDICATED = 0;
    private final boolean mAudioDescriptionAvailable;
    private final boolean mDialogueEnhancementAvailable;
    private final Map<String, String> mLabels;
    private final String mLanguage;
    private final int mMasteringIndication;
    private final int mPresentationId;
    private final int mProgramId;
    private final boolean mSpokenSubtitlesAvailable;

    @Retention(RetentionPolicy.SOURCE)
    public @interface MasteringIndicationType {
    }

    public AudioPresentation(int i, int i2, Map<String, String> map, String str, int i3, boolean z, boolean z2, boolean z3) {
        this.mPresentationId = i;
        this.mProgramId = i2;
        this.mLanguage = str;
        this.mMasteringIndication = i3;
        this.mAudioDescriptionAvailable = z;
        this.mSpokenSubtitlesAvailable = z2;
        this.mDialogueEnhancementAvailable = z3;
        this.mLabels = new HashMap(map);
    }

    public int getPresentationId() {
        return this.mPresentationId;
    }

    public int getProgramId() {
        return this.mProgramId;
    }

    public Map<Locale, String> getLabels() {
        HashMap map = new HashMap();
        for (Map.Entry<String, String> entry : this.mLabels.entrySet()) {
            map.put(new Locale(entry.getKey()), entry.getValue());
        }
        return map;
    }

    public Locale getLocale() {
        return new Locale(this.mLanguage);
    }

    public int getMasteringIndication() {
        return this.mMasteringIndication;
    }

    public boolean hasAudioDescription() {
        return this.mAudioDescriptionAvailable;
    }

    public boolean hasSpokenSubtitles() {
        return this.mSpokenSubtitlesAvailable;
    }

    public boolean hasDialogueEnhancement() {
        return this.mDialogueEnhancementAvailable;
    }
}
