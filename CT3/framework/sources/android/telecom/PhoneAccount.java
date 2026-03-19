package android.telecom;

import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PhoneAccount implements Parcelable {
    public static final int CAPABILITY_CALL_PROVIDER = 2;
    public static final int CAPABILITY_CALL_SUBJECT = 64;
    public static final int CAPABILITY_CDMA_CALL_PROVIDER = 262144;
    public static final int CAPABILITY_CONNECTION_MANAGER = 1;
    public static final int CAPABILITY_EMERGENCY_CALLS_ONLY = 128;
    public static final int CAPABILITY_EMERGENCY_VIDEO_CALLING = 512;
    public static final int CAPABILITY_MULTI_USER = 32;
    public static final int CAPABILITY_PLACE_EMERGENCY_CALLS = 16;
    public static final int CAPABILITY_SIM_SUBSCRIPTION = 4;
    public static final int CAPABILITY_VIDEO_CALLING = 8;
    public static final int CAPABILITY_VIDEO_CALLING_RELIES_ON_PRESENCE = 256;
    public static final int CAPABILITY_VOLTE_CALLING = 65536;
    public static final int CAPABILITY_VOLTE_CONFERENCE_ENHANCED = 131072;
    public static final int CAPABILITY_WIFI_CALLING = 524288;
    public static final Parcelable.Creator<PhoneAccount> CREATOR = new Parcelable.Creator<PhoneAccount>() {
        @Override
        public PhoneAccount createFromParcel(Parcel in) {
            return new PhoneAccount(in, null);
        }

        @Override
        public PhoneAccount[] newArray(int size) {
            return new PhoneAccount[size];
        }
    };
    private static final int CUSTOM_CAPABILITY_BASE = 32768;
    public static final String EXTRA_CALL_SUBJECT_CHARACTER_ENCODING = "android.telecom.extra.CALL_SUBJECT_CHARACTER_ENCODING";
    public static final String EXTRA_CALL_SUBJECT_MAX_LENGTH = "android.telecom.extra.CALL_SUBJECT_MAX_LENGTH";
    public static final String EXTRA_EXT_SUBSCRIPTION_ID = "android.telecom.extra.EXTRA_EXT_SUBSCRIPTION_ID";
    public static final String EXTRA_PHONE_ACCOUNT_SORT_KEY = "android.telecom.extra.EXTRA_PHONE_ACCOUNT_SORT_KEY";
    public static final int NO_HIGHLIGHT_COLOR = 0;
    public static final int NO_ICON_TINT = 0;
    public static final int NO_RESOURCE_ID = -1;
    public static final String SCHEME_SIP = "sip";
    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_VOICEMAIL = "voicemail";
    private final PhoneAccountHandle mAccountHandle;
    private final Uri mAddress;
    private final int mCapabilities;
    private final Bundle mExtras;
    private final int mHighlightColor;
    private final Icon mIcon;
    private boolean mIsEnabled;
    private final CharSequence mLabel;
    private final CharSequence mShortDescription;
    private final Uri mSubscriptionAddress;
    private final List<String> mSupportedUriSchemes;

    PhoneAccount(Parcel in, PhoneAccount phoneAccount) {
        this(in);
    }

    PhoneAccount(PhoneAccountHandle account, Uri address, Uri subscriptionAddress, int capabilities, Icon icon, int highlightColor, CharSequence label, CharSequence shortDescription, List supportedUriSchemes, Bundle extras, boolean isEnabled, PhoneAccount phoneAccount) {
        this(account, address, subscriptionAddress, capabilities, icon, highlightColor, label, shortDescription, supportedUriSchemes, extras, isEnabled);
    }

    public static class Builder {
        private PhoneAccountHandle mAccountHandle;
        private Uri mAddress;
        private int mCapabilities;
        private Bundle mExtras;
        private int mHighlightColor;
        private Icon mIcon;
        private boolean mIsEnabled;
        private CharSequence mLabel;
        private CharSequence mShortDescription;
        private Uri mSubscriptionAddress;
        private List<String> mSupportedUriSchemes;

        public Builder(PhoneAccountHandle accountHandle, CharSequence label) {
            this.mHighlightColor = 0;
            this.mSupportedUriSchemes = new ArrayList();
            this.mIsEnabled = false;
            this.mAccountHandle = accountHandle;
            this.mLabel = label;
        }

        public Builder(PhoneAccount phoneAccount) {
            this.mHighlightColor = 0;
            this.mSupportedUriSchemes = new ArrayList();
            this.mIsEnabled = false;
            this.mAccountHandle = phoneAccount.getAccountHandle();
            this.mAddress = phoneAccount.getAddress();
            this.mSubscriptionAddress = phoneAccount.getSubscriptionAddress();
            this.mCapabilities = phoneAccount.getCapabilities();
            this.mHighlightColor = phoneAccount.getHighlightColor();
            this.mLabel = phoneAccount.getLabel();
            this.mShortDescription = phoneAccount.getShortDescription();
            this.mSupportedUriSchemes.addAll(phoneAccount.getSupportedUriSchemes());
            this.mIcon = phoneAccount.getIcon();
            this.mIsEnabled = phoneAccount.isEnabled();
            this.mExtras = phoneAccount.getExtras();
        }

        public Builder setAddress(Uri value) {
            this.mAddress = value;
            return this;
        }

        public Builder setSubscriptionAddress(Uri value) {
            this.mSubscriptionAddress = value;
            return this;
        }

        public Builder setCapabilities(int value) {
            this.mCapabilities = value;
            return this;
        }

        public Builder setIcon(Icon icon) {
            this.mIcon = icon;
            return this;
        }

        public Builder setHighlightColor(int value) {
            this.mHighlightColor = value;
            return this;
        }

        public Builder setShortDescription(CharSequence value) {
            this.mShortDescription = value;
            return this;
        }

        public Builder addSupportedUriScheme(String uriScheme) {
            if (!TextUtils.isEmpty(uriScheme) && !this.mSupportedUriSchemes.contains(uriScheme)) {
                this.mSupportedUriSchemes.add(uriScheme);
            }
            return this;
        }

        public Builder setSupportedUriSchemes(List<String> uriSchemes) {
            this.mSupportedUriSchemes.clear();
            if (uriSchemes != null && !uriSchemes.isEmpty()) {
                for (String uriScheme : uriSchemes) {
                    addSupportedUriScheme(uriScheme);
                }
            }
            return this;
        }

        public Builder setExtras(Bundle extras) {
            this.mExtras = extras;
            return this;
        }

        public Builder setIsEnabled(boolean isEnabled) {
            this.mIsEnabled = isEnabled;
            return this;
        }

        public PhoneAccount build() {
            if (this.mSupportedUriSchemes.isEmpty()) {
                addSupportedUriScheme(PhoneAccount.SCHEME_TEL);
            }
            return new PhoneAccount(this.mAccountHandle, this.mAddress, this.mSubscriptionAddress, this.mCapabilities, this.mIcon, this.mHighlightColor, this.mLabel, this.mShortDescription, this.mSupportedUriSchemes, this.mExtras, this.mIsEnabled, null);
        }
    }

    private PhoneAccount(PhoneAccountHandle account, Uri address, Uri subscriptionAddress, int capabilities, Icon icon, int highlightColor, CharSequence label, CharSequence shortDescription, List<String> supportedUriSchemes, Bundle extras, boolean isEnabled) {
        this.mAccountHandle = account;
        this.mAddress = address;
        this.mSubscriptionAddress = subscriptionAddress;
        this.mCapabilities = capabilities;
        this.mIcon = icon;
        this.mHighlightColor = highlightColor;
        this.mLabel = label;
        this.mShortDescription = shortDescription;
        this.mSupportedUriSchemes = Collections.unmodifiableList(supportedUriSchemes);
        this.mExtras = extras;
        this.mIsEnabled = isEnabled;
    }

    public static Builder builder(PhoneAccountHandle accountHandle, CharSequence label) {
        return new Builder(accountHandle, label);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public PhoneAccountHandle getAccountHandle() {
        return this.mAccountHandle;
    }

    public Uri getAddress() {
        return this.mAddress;
    }

    public Uri getSubscriptionAddress() {
        return this.mSubscriptionAddress;
    }

    public int getCapabilities() {
        return this.mCapabilities;
    }

    public boolean hasCapabilities(int capability) {
        return (this.mCapabilities & capability) == capability;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public CharSequence getShortDescription() {
        return this.mShortDescription;
    }

    public List<String> getSupportedUriSchemes() {
        return this.mSupportedUriSchemes;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public Icon getIcon() {
        return this.mIcon;
    }

    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    public boolean supportsUriScheme(String uriScheme) {
        if (this.mSupportedUriSchemes == null || uriScheme == null) {
            return false;
        }
        for (String scheme : this.mSupportedUriSchemes) {
            if (scheme != null && scheme.equals(uriScheme)) {
                return true;
            }
        }
        return false;
    }

    public int getHighlightColor() {
        return this.mHighlightColor;
    }

    public void setIsEnabled(boolean isEnabled) {
        if (this.mIsEnabled != isEnabled) {
            Log.d(this, "[setIsEnabled]" + Log.pii(this.mAccountHandle.getId()) + this.mIsEnabled + " -> " + isEnabled, new Object[0]);
        }
        this.mIsEnabled = isEnabled;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (this.mAccountHandle == null) {
            out.writeInt(0);
        } else {
            out.writeInt(1);
            this.mAccountHandle.writeToParcel(out, flags);
        }
        if (this.mAddress == null) {
            out.writeInt(0);
        } else {
            out.writeInt(1);
            this.mAddress.writeToParcel(out, flags);
        }
        if (this.mSubscriptionAddress == null) {
            out.writeInt(0);
        } else {
            out.writeInt(1);
            this.mSubscriptionAddress.writeToParcel(out, flags);
        }
        out.writeInt(this.mCapabilities);
        out.writeInt(this.mHighlightColor);
        out.writeCharSequence(this.mLabel);
        out.writeCharSequence(this.mShortDescription);
        out.writeStringList(this.mSupportedUriSchemes);
        if (this.mIcon == null) {
            out.writeInt(0);
        } else {
            out.writeInt(1);
            this.mIcon.writeToParcel(out, flags);
        }
        out.writeByte((byte) (this.mIsEnabled ? 1 : 0));
        out.writeBundle(this.mExtras);
    }

    private PhoneAccount(Parcel in) {
        if (in.readInt() > 0) {
            this.mAccountHandle = PhoneAccountHandle.CREATOR.createFromParcel(in);
        } else {
            this.mAccountHandle = null;
        }
        if (in.readInt() > 0) {
            this.mAddress = Uri.CREATOR.createFromParcel(in);
        } else {
            this.mAddress = null;
        }
        if (in.readInt() > 0) {
            this.mSubscriptionAddress = Uri.CREATOR.createFromParcel(in);
        } else {
            this.mSubscriptionAddress = null;
        }
        this.mCapabilities = in.readInt();
        this.mHighlightColor = in.readInt();
        this.mLabel = in.readCharSequence();
        this.mShortDescription = in.readCharSequence();
        this.mSupportedUriSchemes = Collections.unmodifiableList(in.createStringArrayList());
        if (in.readInt() > 0) {
            this.mIcon = Icon.CREATOR.createFromParcel(in);
        } else {
            this.mIcon = null;
        }
        this.mIsEnabled = in.readByte() == 1;
        this.mExtras = in.readBundle();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder().append("PhoneAccount{[").append(this.mIsEnabled ? 'V' : 'X').append("]").append(this.mAccountHandle).append(", subId: ").append(getSubscriptionId()).append(", Capabilities: [").append(capabilitiesToString(this.mCapabilities)).append("], Schemes: ");
        for (String scheme : this.mSupportedUriSchemes) {
            sb.append(scheme).append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        }
        sb.append(", Extras: ");
        sb.append(this.mExtras);
        sb.append("}");
        return sb.toString();
    }

    private String capabilitiesToString(int capabilities) {
        StringBuilder sb = new StringBuilder();
        if (hasCapabilities(8)) {
            sb.append("Video ");
        }
        if (hasCapabilities(256)) {
            sb.append("Presence ");
        }
        if (hasCapabilities(2)) {
            sb.append("CallProvider ");
        }
        if (hasCapabilities(64)) {
            sb.append("CallSubject ");
        }
        if (hasCapabilities(1)) {
            sb.append("ConnectionMgr ");
        }
        if (hasCapabilities(128)) {
            sb.append("EmergOnly ");
        }
        if (hasCapabilities(32)) {
            sb.append("MultiUser ");
        }
        if (hasCapabilities(16)) {
            sb.append("PlaceEmerg ");
        }
        if (hasCapabilities(512)) {
            sb.append("EmergVideo ");
        }
        if (hasCapabilities(4)) {
            sb.append("SimSub ");
        }
        if (hasCapabilities(65536)) {
            sb.append("Volte ");
        }
        if (hasCapabilities(131072)) {
            sb.append("VolteConf ");
        }
        if (hasCapabilities(524288)) {
            sb.append("WFC ");
        }
        if (hasCapabilities(262144)) {
            sb.append("Cdma ");
        }
        return sb.toString();
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof PhoneAccount)) {
            return false;
        }
        PhoneAccount targetAccount = (PhoneAccount) other;
        if (Objects.equals(Integer.valueOf(targetAccount.getCapabilities()), Integer.valueOf(getCapabilities())) && Objects.equals(Integer.valueOf(targetAccount.getHighlightColor()), Integer.valueOf(getHighlightColor())) && Objects.equals(targetAccount.getLabel(), getLabel()) && Objects.equals(targetAccount.getShortDescription(), getShortDescription()) && Objects.equals(targetAccount.getAddress(), getAddress()) && Objects.equals(targetAccount.getSubscriptionAddress(), getSubscriptionAddress()) && Objects.equals(targetAccount.getSupportedUriSchemes(), getSupportedUriSchemes())) {
            return Objects.equals(targetAccount.getAccountHandle(), getAccountHandle());
        }
        return false;
    }

    public int getSubscriptionId() {
        if (this.mExtras == null) {
            return -1;
        }
        return this.mExtras.getInt(EXTRA_EXT_SUBSCRIPTION_ID, -1);
    }
}
