package android.telephony.euicc;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.UiccAccessRule;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DownloadableSubscription implements Parcelable {
    public static final Parcelable.Creator<DownloadableSubscription> CREATOR = new Parcelable.Creator<DownloadableSubscription>() {
        @Override
        public DownloadableSubscription createFromParcel(Parcel parcel) {
            return new DownloadableSubscription(parcel);
        }

        @Override
        public DownloadableSubscription[] newArray(int i) {
            return new DownloadableSubscription[i];
        }
    };
    private List<UiccAccessRule> accessRules;
    private String carrierName;
    private String confirmationCode;

    @Deprecated
    public final String encodedActivationCode;

    public String getEncodedActivationCode() {
        return this.encodedActivationCode;
    }

    private DownloadableSubscription(String str) {
        this.encodedActivationCode = str;
    }

    private DownloadableSubscription(Parcel parcel) {
        this.encodedActivationCode = parcel.readString();
        this.confirmationCode = parcel.readString();
        this.carrierName = parcel.readString();
        this.accessRules = new ArrayList();
        parcel.readTypedList(this.accessRules, UiccAccessRule.CREATOR);
    }

    private DownloadableSubscription(String str, String str2, String str3, List<UiccAccessRule> list) {
        this.encodedActivationCode = str;
        this.confirmationCode = str2;
        this.carrierName = str3;
        this.accessRules = list;
    }

    @SystemApi
    public static final class Builder {
        List<UiccAccessRule> accessRules;
        private String carrierName;
        private String confirmationCode;
        private String encodedActivationCode;

        public Builder() {
        }

        public Builder(DownloadableSubscription downloadableSubscription) {
            this.encodedActivationCode = downloadableSubscription.getEncodedActivationCode();
            this.confirmationCode = downloadableSubscription.getConfirmationCode();
            this.carrierName = downloadableSubscription.getCarrierName();
            this.accessRules = downloadableSubscription.getAccessRules();
        }

        public DownloadableSubscription build() {
            return new DownloadableSubscription(this.encodedActivationCode, this.confirmationCode, this.carrierName, this.accessRules);
        }

        public Builder setEncodedActivationCode(String str) {
            this.encodedActivationCode = str;
            return this;
        }

        public Builder setConfirmationCode(String str) {
            this.confirmationCode = str;
            return this;
        }

        public Builder setCarrierName(String str) {
            this.carrierName = str;
            return this;
        }

        public Builder setAccessRules(List<UiccAccessRule> list) {
            this.accessRules = list;
            return this;
        }
    }

    public static DownloadableSubscription forActivationCode(String str) {
        Preconditions.checkNotNull(str, "Activation code may not be null");
        return new DownloadableSubscription(str);
    }

    @Deprecated
    public void setConfirmationCode(String str) {
        this.confirmationCode = str;
    }

    public String getConfirmationCode() {
        return this.confirmationCode;
    }

    @Deprecated
    public void setCarrierName(String str) {
        this.carrierName = str;
    }

    @SystemApi
    public String getCarrierName() {
        return this.carrierName;
    }

    @SystemApi
    public List<UiccAccessRule> getAccessRules() {
        return this.accessRules;
    }

    @Deprecated
    public void setAccessRules(List<UiccAccessRule> list) {
        this.accessRules = list;
    }

    @Deprecated
    public void setAccessRules(UiccAccessRule[] uiccAccessRuleArr) {
        this.accessRules = Arrays.asList(uiccAccessRuleArr);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.encodedActivationCode);
        parcel.writeString(this.confirmationCode);
        parcel.writeString(this.carrierName);
        parcel.writeTypedList(this.accessRules);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
