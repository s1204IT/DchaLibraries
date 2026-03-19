package android.media.audiopolicy;

import android.annotation.SystemApi;
import android.media.AudioAttributes;
import android.os.Parcel;
import android.util.Log;
import java.util.ArrayList;
import java.util.Objects;

@SystemApi
public class AudioMixingRule {
    public static final int RULE_EXCLUDE_ATTRIBUTE_CAPTURE_PRESET = 32770;
    public static final int RULE_EXCLUDE_ATTRIBUTE_USAGE = 32769;
    public static final int RULE_EXCLUDE_UID = 32772;
    private static final int RULE_EXCLUSION_MASK = 32768;

    @SystemApi
    public static final int RULE_MATCH_ATTRIBUTE_CAPTURE_PRESET = 2;

    @SystemApi
    public static final int RULE_MATCH_ATTRIBUTE_USAGE = 1;

    @SystemApi
    public static final int RULE_MATCH_UID = 4;
    private final ArrayList<AudioMixMatchCriterion> mCriteria;
    private final int mTargetMixType;

    private AudioMixingRule(int i, ArrayList<AudioMixMatchCriterion> arrayList) {
        this.mCriteria = arrayList;
        this.mTargetMixType = i;
    }

    static final class AudioMixMatchCriterion {
        final AudioAttributes mAttr;
        final int mIntProp;
        final int mRule;

        AudioMixMatchCriterion(AudioAttributes audioAttributes, int i) {
            this.mAttr = audioAttributes;
            this.mIntProp = Integer.MIN_VALUE;
            this.mRule = i;
        }

        AudioMixMatchCriterion(Integer num, int i) {
            this.mAttr = null;
            this.mIntProp = num.intValue();
            this.mRule = i;
        }

        public int hashCode() {
            return Objects.hash(this.mAttr, Integer.valueOf(this.mIntProp), Integer.valueOf(this.mRule));
        }

        void writeToParcel(Parcel parcel) {
            parcel.writeInt(this.mRule);
            int i = this.mRule & (-32769);
            if (i != 4) {
                switch (i) {
                    case 1:
                        parcel.writeInt(this.mAttr.getUsage());
                        break;
                    case 2:
                        parcel.writeInt(this.mAttr.getCapturePreset());
                        break;
                    default:
                        Log.e("AudioMixMatchCriterion", "Unknown match rule" + i + " when writing to Parcel");
                        parcel.writeInt(-1);
                        break;
                }
            }
            parcel.writeInt(this.mIntProp);
        }
    }

    boolean isAffectingUsage(int i) {
        for (AudioMixMatchCriterion audioMixMatchCriterion : this.mCriteria) {
            if ((audioMixMatchCriterion.mRule & 1) != 0 && audioMixMatchCriterion.mAttr != null && audioMixMatchCriterion.mAttr.getUsage() == i) {
                return true;
            }
        }
        return false;
    }

    private static boolean areCriteriaEquivalent(ArrayList<AudioMixMatchCriterion> arrayList, ArrayList<AudioMixMatchCriterion> arrayList2) {
        if (arrayList == null || arrayList2 == null) {
            return false;
        }
        if (arrayList == arrayList2) {
            return true;
        }
        if (arrayList.size() != arrayList2.size() || arrayList.hashCode() != arrayList2.hashCode()) {
            return false;
        }
        return true;
    }

    int getTargetMixType() {
        return this.mTargetMixType;
    }

    ArrayList<AudioMixMatchCriterion> getCriteria() {
        return this.mCriteria;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AudioMixingRule audioMixingRule = (AudioMixingRule) obj;
        if (this.mTargetMixType == audioMixingRule.mTargetMixType && areCriteriaEquivalent(this.mCriteria, audioMixingRule.mCriteria)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mTargetMixType), this.mCriteria);
    }

    private static boolean isValidSystemApiRule(int i) {
        if (i != 4) {
            switch (i) {
                case 1:
                case 2:
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }

    private static boolean isValidAttributesSystemApiRule(int i) {
        switch (i) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    private static boolean isValidRule(int i) {
        int i2 = i & (-32769);
        if (i2 != 4) {
            switch (i2) {
                case 1:
                case 2:
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }

    private static boolean isPlayerRule(int i) {
        int i2 = i & (-32769);
        if (i2 == 1 || i2 == 4) {
            return true;
        }
        return false;
    }

    private static boolean isAudioAttributeRule(int i) {
        switch (i) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    @SystemApi
    public static class Builder {
        private int mTargetMixType = -1;
        private ArrayList<AudioMixMatchCriterion> mCriteria = new ArrayList<>();

        @SystemApi
        public Builder() {
        }

        @SystemApi
        public Builder addRule(AudioAttributes audioAttributes, int i) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidAttributesSystemApiRule(i)) {
                throw new IllegalArgumentException("Illegal rule value " + i);
            }
            return checkAddRuleObjInternal(i, audioAttributes);
        }

        @SystemApi
        public Builder excludeRule(AudioAttributes audioAttributes, int i) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidAttributesSystemApiRule(i)) {
                throw new IllegalArgumentException("Illegal rule value " + i);
            }
            return checkAddRuleObjInternal(i | 32768, audioAttributes);
        }

        @SystemApi
        public Builder addMixRule(int i, Object obj) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidSystemApiRule(i)) {
                throw new IllegalArgumentException("Illegal rule value " + i);
            }
            return checkAddRuleObjInternal(i, obj);
        }

        @SystemApi
        public Builder excludeMixRule(int i, Object obj) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidSystemApiRule(i)) {
                throw new IllegalArgumentException("Illegal rule value " + i);
            }
            return checkAddRuleObjInternal(i | 32768, obj);
        }

        private Builder checkAddRuleObjInternal(int i, Object obj) throws IllegalArgumentException {
            if (obj != null) {
                if (AudioMixingRule.isValidRule(i)) {
                    if (AudioMixingRule.isAudioAttributeRule((-32769) & i)) {
                        if (!(obj instanceof AudioAttributes)) {
                            throw new IllegalArgumentException("Invalid AudioAttributes argument");
                        }
                        return addRuleInternal((AudioAttributes) obj, null, i);
                    }
                    if (!(obj instanceof Integer)) {
                        throw new IllegalArgumentException("Invalid Integer argument");
                    }
                    return addRuleInternal(null, (Integer) obj, i);
                }
                throw new IllegalArgumentException("Illegal rule value " + i);
            }
            throw new IllegalArgumentException("Illegal null argument for mixing rule");
        }

        private Builder addRuleInternal(AudioAttributes audioAttributes, Integer num, int i) throws IllegalArgumentException {
            if (this.mTargetMixType == -1) {
                if (AudioMixingRule.isPlayerRule(i)) {
                    this.mTargetMixType = 0;
                } else {
                    this.mTargetMixType = 1;
                }
            } else if ((this.mTargetMixType == 0 && !AudioMixingRule.isPlayerRule(i)) || (this.mTargetMixType == 1 && AudioMixingRule.isPlayerRule(i))) {
                throw new IllegalArgumentException("Incompatible rule for mix");
            }
            synchronized (this.mCriteria) {
                int i2 = (-32769) & i;
                for (AudioMixMatchCriterion audioMixMatchCriterion : this.mCriteria) {
                    if (i2 != 4) {
                        switch (i2) {
                            case 1:
                                if (audioMixMatchCriterion.mAttr.getUsage() == audioAttributes.getUsage()) {
                                    if (audioMixMatchCriterion.mRule == i) {
                                        return this;
                                    }
                                    throw new IllegalArgumentException("Contradictory rule exists for " + audioAttributes);
                                }
                                break;
                                break;
                            case 2:
                                if (audioMixMatchCriterion.mAttr.getCapturePreset() == audioAttributes.getCapturePreset()) {
                                    if (audioMixMatchCriterion.mRule == i) {
                                        return this;
                                    }
                                    throw new IllegalArgumentException("Contradictory rule exists for " + audioAttributes);
                                }
                                break;
                                break;
                        }
                    } else if (audioMixMatchCriterion.mIntProp == num.intValue()) {
                        if (audioMixMatchCriterion.mRule == i) {
                            return this;
                        }
                        throw new IllegalArgumentException("Contradictory rule exists for UID " + num);
                    }
                }
                if (i2 != 4) {
                    switch (i2) {
                        case 1:
                        case 2:
                            this.mCriteria.add(new AudioMixMatchCriterion(audioAttributes, i));
                            break;
                        default:
                            throw new IllegalStateException("Unreachable code in addRuleInternal()");
                    }
                } else {
                    this.mCriteria.add(new AudioMixMatchCriterion(num, i));
                }
                return this;
            }
        }

        Builder addRuleFromParcel(Parcel parcel) throws IllegalArgumentException {
            AudioAttributes audioAttributesBuild;
            int i = parcel.readInt();
            int i2 = (-32769) & i;
            Integer num = null;
            if (i2 != 4) {
                switch (i2) {
                    case 1:
                        audioAttributesBuild = new AudioAttributes.Builder().setUsage(parcel.readInt()).build();
                        break;
                    case 2:
                        audioAttributesBuild = new AudioAttributes.Builder().setInternalCapturePreset(parcel.readInt()).build();
                        break;
                    default:
                        parcel.readInt();
                        throw new IllegalArgumentException("Illegal rule value " + i + " in parcel");
                }
            } else {
                Integer num2 = new Integer(parcel.readInt());
                audioAttributesBuild = null;
                num = num2;
            }
            return addRuleInternal(audioAttributesBuild, num, i);
        }

        public AudioMixingRule build() {
            return new AudioMixingRule(this.mTargetMixType, this.mCriteria);
        }
    }
}
