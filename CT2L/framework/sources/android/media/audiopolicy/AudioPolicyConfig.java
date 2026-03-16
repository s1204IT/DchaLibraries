package android.media.audiopolicy;

import android.app.backup.FullBackup;
import android.media.AudioFormat;
import android.media.TtmlUtils;
import android.media.audiopolicy.AudioMix;
import android.media.audiopolicy.AudioMixingRule;
import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.ArrayList;
import java.util.Objects;

public class AudioPolicyConfig implements Parcelable {
    public static final Parcelable.Creator<AudioPolicyConfig> CREATOR = new Parcelable.Creator<AudioPolicyConfig>() {
        @Override
        public AudioPolicyConfig createFromParcel(Parcel p) {
            return new AudioPolicyConfig(p);
        }

        @Override
        public AudioPolicyConfig[] newArray(int size) {
            return new AudioPolicyConfig[size];
        }
    };
    private static final String TAG = "AudioPolicyConfig";
    protected int mDuckingPolicy;
    protected ArrayList<AudioMix> mMixes;
    private String mRegistrationId;

    protected AudioPolicyConfig(AudioPolicyConfig conf) {
        this.mDuckingPolicy = 0;
        this.mRegistrationId = null;
        this.mMixes = conf.mMixes;
    }

    AudioPolicyConfig(ArrayList<AudioMix> mixes) {
        this.mDuckingPolicy = 0;
        this.mRegistrationId = null;
        this.mMixes = mixes;
    }

    public void addMix(AudioMix mix) throws IllegalArgumentException {
        if (mix == null) {
            throw new IllegalArgumentException("Illegal null AudioMix argument");
        }
        this.mMixes.add(mix);
    }

    public int hashCode() {
        return Objects.hash(this.mMixes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mMixes.size());
        for (AudioMix mix : this.mMixes) {
            dest.writeInt(mix.getRouteFlags());
            dest.writeInt(mix.getFormat().getSampleRate());
            dest.writeInt(mix.getFormat().getEncoding());
            dest.writeInt(mix.getFormat().getChannelMask());
            ArrayList<AudioMixingRule.AttributeMatchCriterion> criteria = mix.getRule().getCriteria();
            dest.writeInt(criteria.size());
            for (AudioMixingRule.AttributeMatchCriterion criterion : criteria) {
                criterion.writeToParcel(dest);
            }
        }
    }

    private AudioPolicyConfig(Parcel in) {
        this.mDuckingPolicy = 0;
        this.mRegistrationId = null;
        this.mMixes = new ArrayList<>();
        int nbMixes = in.readInt();
        for (int i = 0; i < nbMixes; i++) {
            AudioMix.Builder mixBuilder = new AudioMix.Builder();
            int routeFlags = in.readInt();
            mixBuilder.setRouteFlags(routeFlags);
            int sampleRate = in.readInt();
            int encoding = in.readInt();
            int channelMask = in.readInt();
            AudioFormat format = new AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(channelMask).setEncoding(encoding).build();
            mixBuilder.setFormat(format);
            int nbRules = in.readInt();
            AudioMixingRule.Builder ruleBuilder = new AudioMixingRule.Builder();
            for (int j = 0; j < nbRules; j++) {
                ruleBuilder.addRuleFromParcel(in);
            }
            mixBuilder.setMixingRule(ruleBuilder.build());
            this.mMixes.add(mixBuilder.build());
        }
    }

    public String toLogFriendlyString() {
        String textDump;
        String textDump2 = new String("android.media.audiopolicy.AudioPolicyConfig:\n");
        String textDump3 = textDump2 + this.mMixes.size() + " AudioMix: " + this.mRegistrationId + "\n";
        for (AudioMix mix : this.mMixes) {
            textDump3 = ((((textDump3 + "* route flags=0x" + Integer.toHexString(mix.getRouteFlags()) + "\n") + "  rate=" + mix.getFormat().getSampleRate() + "Hz\n") + "  encoding=" + mix.getFormat().getEncoding() + "\n") + "  channels=0x") + Integer.toHexString(mix.getFormat().getChannelMask()).toUpperCase() + "\n";
            ArrayList<AudioMixingRule.AttributeMatchCriterion> criteria = mix.getRule().getCriteria();
            for (AudioMixingRule.AttributeMatchCriterion criterion : criteria) {
                switch (criterion.mRule) {
                    case 1:
                        textDump = (textDump3 + "  match usage ") + criterion.mAttr.usageToString();
                        break;
                    case 2:
                        textDump = (textDump3 + "  match capture preset ") + criterion.mAttr.getCapturePreset();
                        break;
                    case 32769:
                        textDump = (textDump3 + "  exclude usage ") + criterion.mAttr.usageToString();
                        break;
                    case 32770:
                        textDump = (textDump3 + "  exclude capture preset ") + criterion.mAttr.getCapturePreset();
                        break;
                    default:
                        textDump = textDump3 + "invalid rule!";
                        break;
                }
                textDump3 = textDump + "\n";
            }
        }
        return textDump3;
    }

    protected void setRegistration(String regId) {
        boolean currentRegNull = this.mRegistrationId == null || this.mRegistrationId.isEmpty();
        boolean newRegNull = regId == null || regId.isEmpty();
        if (!currentRegNull && !newRegNull && !this.mRegistrationId.equals(regId)) {
            Log.e(TAG, "Invalid registration transition from " + this.mRegistrationId + " to " + regId);
            return;
        }
        if (regId == null) {
            regId = ProxyInfo.LOCAL_EXCL_LIST;
        }
        this.mRegistrationId = regId;
        int mixIndex = 0;
        for (AudioMix mix : this.mMixes) {
            if (!this.mRegistrationId.isEmpty()) {
                mix.setRegistration(this.mRegistrationId + "mix" + mixTypeId(mix.getMixType()) + ":" + mixIndex);
                mixIndex++;
            } else {
                mix.setRegistration(ProxyInfo.LOCAL_EXCL_LIST);
            }
        }
    }

    private static String mixTypeId(int type) {
        return type == 0 ? TtmlUtils.TAG_P : type == 1 ? FullBackup.ROOT_TREE_TOKEN : "i";
    }

    protected String getRegistration() {
        return this.mRegistrationId;
    }
}
