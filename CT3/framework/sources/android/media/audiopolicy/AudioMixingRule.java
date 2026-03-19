package android.media.audiopolicy;

import android.media.AudioAttributes;
import android.os.Parcel;
import android.util.Log;
import java.util.ArrayList;
import java.util.Objects;

public class AudioMixingRule {
    public static final int RULE_EXCLUDE_ATTRIBUTE_CAPTURE_PRESET = 32770;
    public static final int RULE_EXCLUDE_ATTRIBUTE_USAGE = 32769;
    public static final int RULE_EXCLUDE_UID = 32772;
    private static final int RULE_EXCLUSION_MASK = 32768;
    public static final int RULE_MATCH_ATTRIBUTE_CAPTURE_PRESET = 2;
    public static final int RULE_MATCH_ATTRIBUTE_USAGE = 1;
    public static final int RULE_MATCH_UID = 4;
    private final ArrayList<AudioMixMatchCriterion> mCriteria;
    private final int mTargetMixType;

    AudioMixingRule(int mixType, ArrayList criteria, AudioMixingRule audioMixingRule) {
        this(mixType, criteria);
    }

    private AudioMixingRule(int mixType, ArrayList<AudioMixMatchCriterion> criteria) {
        this.mCriteria = criteria;
        this.mTargetMixType = mixType;
    }

    static final class AudioMixMatchCriterion {
        final AudioAttributes mAttr;
        final int mIntProp;
        final int mRule;

        AudioMixMatchCriterion(AudioAttributes attributes, int rule) {
            this.mAttr = attributes;
            this.mIntProp = Integer.MIN_VALUE;
            this.mRule = rule;
        }

        AudioMixMatchCriterion(Integer intProp, int rule) {
            this.mAttr = null;
            this.mIntProp = intProp.intValue();
            this.mRule = rule;
        }

        public int hashCode() {
            return Objects.hash(this.mAttr, Integer.valueOf(this.mIntProp), Integer.valueOf(this.mRule));
        }

        void writeToParcel(Parcel dest) {
            dest.writeInt(this.mRule);
            int match_rule = this.mRule & (-32769);
            switch (match_rule) {
                case 1:
                    dest.writeInt(this.mAttr.getUsage());
                    break;
                case 2:
                    dest.writeInt(this.mAttr.getCapturePreset());
                    break;
                case 3:
                default:
                    Log.e("AudioMixMatchCriterion", "Unknown match rule" + match_rule + " when writing to Parcel");
                    dest.writeInt(-1);
                    break;
                case 4:
                    dest.writeInt(this.mIntProp);
                    break;
            }
        }
    }

    int getTargetMixType() {
        return this.mTargetMixType;
    }

    ArrayList<AudioMixMatchCriterion> getCriteria() {
        return this.mCriteria;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mTargetMixType), this.mCriteria);
    }

    private static boolean isValidSystemApiRule(int rule) {
        switch (rule) {
            case 1:
            case 2:
            case 4:
                return true;
            case 3:
            default:
                return false;
        }
    }

    private static boolean isValidAttributesSystemApiRule(int rule) {
        switch (rule) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    private static boolean isValidRule(int rule) {
        int match_rule = rule & (-32769);
        switch (match_rule) {
            case 1:
            case 2:
            case 4:
                return true;
            case 3:
            default:
                return false;
        }
    }

    private static boolean isPlayerRule(int rule) {
        int match_rule = rule & (-32769);
        switch (match_rule) {
            case 1:
            case 4:
                return true;
            case 2:
            case 3:
            default:
                return false;
        }
    }

    private static boolean isAudioAttributeRule(int match_rule) {
        switch (match_rule) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    public static class Builder {
        private int mTargetMixType = -1;
        private ArrayList<AudioMixMatchCriterion> mCriteria = new ArrayList<>();

        public Builder addRule(AudioAttributes attrToMatch, int rule) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidAttributesSystemApiRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            return checkAddRuleObjInternal(rule, attrToMatch);
        }

        public Builder excludeRule(AudioAttributes attrToMatch, int rule) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidAttributesSystemApiRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            return checkAddRuleObjInternal(32768 | rule, attrToMatch);
        }

        public Builder addMixRule(int rule, Object property) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidSystemApiRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            return checkAddRuleObjInternal(rule, property);
        }

        public Builder excludeMixRule(int rule, Object property) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidSystemApiRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            return checkAddRuleObjInternal(32768 | rule, property);
        }

        private Builder checkAddRuleObjInternal(int rule, Object obj) throws IllegalArgumentException {
            if (obj == 0) {
                throw new IllegalArgumentException("Illegal null argument for mixing rule");
            }
            if (!AudioMixingRule.isValidRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            int match_rule = rule & (-32769);
            if (AudioMixingRule.isAudioAttributeRule(match_rule)) {
                if (obj instanceof AudioAttributes) {
                    return addRuleInternal(obj, null, rule);
                }
                throw new IllegalArgumentException("Invalid AudioAttributes argument");
            }
            if (obj instanceof Integer) {
                return addRuleInternal(null, obj, rule);
            }
            throw new IllegalArgumentException("Invalid Integer argument");
        }

        private Builder addRuleInternal(AudioAttributes attrToMatch, Integer intProp, int rule) throws IllegalArgumentException {
            if (this.mTargetMixType == -1) {
                if (AudioMixingRule.isPlayerRule(rule)) {
                    this.mTargetMixType = 0;
                } else {
                    this.mTargetMixType = 1;
                }
            } else if ((this.mTargetMixType == 0 && !AudioMixingRule.isPlayerRule(rule)) || (this.mTargetMixType == 1 && AudioMixingRule.isPlayerRule(rule))) {
                throw new IllegalArgumentException("Incompatible rule for mix");
            }
            synchronized (this.mCriteria) {
                int match_rule = rule & (-32769);
                for (AudioMixMatchCriterion criterion : this.mCriteria) {
                    switch (match_rule) {
                        case 1:
                            if (criterion.mAttr.getUsage() == attrToMatch.getUsage()) {
                                if (criterion.mRule == rule) {
                                    return this;
                                }
                                throw new IllegalArgumentException("Contradictory rule exists for " + attrToMatch);
                            }
                            break;
                            break;
                        case 2:
                            if (criterion.mAttr.getCapturePreset() == attrToMatch.getCapturePreset()) {
                                if (criterion.mRule == rule) {
                                    return this;
                                }
                                throw new IllegalArgumentException("Contradictory rule exists for " + attrToMatch);
                            }
                            break;
                            break;
                        case 4:
                            if (criterion.mIntProp == intProp.intValue()) {
                                if (criterion.mRule == rule) {
                                    return this;
                                }
                                throw new IllegalArgumentException("Contradictory rule exists for UID " + intProp);
                            }
                            break;
                            break;
                    }
                }
                switch (match_rule) {
                    case 1:
                    case 2:
                        this.mCriteria.add(new AudioMixMatchCriterion(attrToMatch, rule));
                        break;
                    case 3:
                    default:
                        throw new IllegalStateException("Unreachable code in addRuleInternal()");
                    case 4:
                        this.mCriteria.add(new AudioMixMatchCriterion(intProp, rule));
                        break;
                }
                return this;
            }
        }

        Builder addRuleFromParcel(Parcel in) throws IllegalArgumentException {
            int rule = in.readInt();
            int match_rule = rule & (-32769);
            AudioAttributes attr = null;
            Integer intProp = null;
            switch (match_rule) {
                case 1:
                    int usage = in.readInt();
                    attr = new AudioAttributes.Builder().setUsage(usage).build();
                    break;
                case 2:
                    int preset = in.readInt();
                    attr = new AudioAttributes.Builder().setInternalCapturePreset(preset).build();
                    break;
                case 3:
                default:
                    in.readInt();
                    throw new IllegalArgumentException("Illegal rule value " + rule + " in parcel");
                case 4:
                    intProp = new Integer(in.readInt());
                    break;
            }
            return addRuleInternal(attr, intProp, rule);
        }

        public AudioMixingRule build() {
            return new AudioMixingRule(this.mTargetMixType, this.mCriteria, null);
        }
    }
}
