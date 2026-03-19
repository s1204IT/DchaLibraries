package android.telephony.euicc;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.carrier.CarrierIdentifier;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

@SystemApi
public final class EuiccRulesAuthTable implements Parcelable {
    public static final Parcelable.Creator<EuiccRulesAuthTable> CREATOR = new Parcelable.Creator<EuiccRulesAuthTable>() {
        @Override
        public EuiccRulesAuthTable createFromParcel(Parcel parcel) {
            return new EuiccRulesAuthTable(parcel);
        }

        @Override
        public EuiccRulesAuthTable[] newArray(int i) {
            return new EuiccRulesAuthTable[i];
        }
    };
    public static final int POLICY_RULE_FLAG_CONSENT_REQUIRED = 1;
    private final CarrierIdentifier[][] mCarrierIds;
    private final int[] mPolicyRuleFlags;
    private final int[] mPolicyRules;

    @Retention(RetentionPolicy.SOURCE)
    public @interface PolicyRuleFlag {
    }

    public static final class Builder {
        private CarrierIdentifier[][] mCarrierIds;
        private int[] mPolicyRuleFlags;
        private int[] mPolicyRules;
        private int mPosition;

        public Builder(int i) {
            this.mPolicyRules = new int[i];
            this.mCarrierIds = new CarrierIdentifier[i][];
            this.mPolicyRuleFlags = new int[i];
        }

        public EuiccRulesAuthTable build() {
            if (this.mPosition != this.mPolicyRules.length) {
                throw new IllegalStateException("Not enough rules are added, expected: " + this.mPolicyRules.length + ", added: " + this.mPosition);
            }
            return new EuiccRulesAuthTable(this.mPolicyRules, this.mCarrierIds, this.mPolicyRuleFlags);
        }

        public Builder add(int i, List<CarrierIdentifier> list, int i2) {
            if (this.mPosition >= this.mPolicyRules.length) {
                throw new ArrayIndexOutOfBoundsException(this.mPosition);
            }
            this.mPolicyRules[this.mPosition] = i;
            if (list != null && list.size() > 0) {
                this.mCarrierIds[this.mPosition] = (CarrierIdentifier[]) list.toArray(new CarrierIdentifier[list.size()]);
            }
            this.mPolicyRuleFlags[this.mPosition] = i2;
            this.mPosition++;
            return this;
        }
    }

    @VisibleForTesting
    public static boolean match(String str, String str2) {
        if (str.length() < str2.length()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != 'E' && (i >= str2.length() || str.charAt(i) != str2.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private EuiccRulesAuthTable(int[] iArr, CarrierIdentifier[][] carrierIdentifierArr, int[] iArr2) {
        this.mPolicyRules = iArr;
        this.mCarrierIds = carrierIdentifierArr;
        this.mPolicyRuleFlags = iArr2;
    }

    public int findIndex(int i, CarrierIdentifier carrierIdentifier) {
        CarrierIdentifier[] carrierIdentifierArr;
        for (int i2 = 0; i2 < this.mPolicyRules.length; i2++) {
            if ((this.mPolicyRules[i2] & i) != 0 && (carrierIdentifierArr = this.mCarrierIds[i2]) != null && carrierIdentifierArr.length != 0) {
                for (CarrierIdentifier carrierIdentifier2 : carrierIdentifierArr) {
                    if (match(carrierIdentifier2.getMcc(), carrierIdentifier.getMcc()) && match(carrierIdentifier2.getMnc(), carrierIdentifier.getMnc())) {
                        String gid1 = carrierIdentifier2.getGid1();
                        if (TextUtils.isEmpty(gid1) || gid1.equals(carrierIdentifier.getGid1())) {
                            String gid2 = carrierIdentifier2.getGid2();
                            if (TextUtils.isEmpty(gid2) || gid2.equals(carrierIdentifier.getGid2())) {
                                return i2;
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    public boolean hasPolicyRuleFlag(int i, int i2) {
        if (i < 0 || i >= this.mPolicyRules.length) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        return (this.mPolicyRuleFlags[i] & i2) != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeIntArray(this.mPolicyRules);
        for (CarrierIdentifier[] carrierIdentifierArr : this.mCarrierIds) {
            parcel.writeTypedArray(carrierIdentifierArr, i);
        }
        parcel.writeIntArray(this.mPolicyRuleFlags);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EuiccRulesAuthTable euiccRulesAuthTable = (EuiccRulesAuthTable) obj;
        if (this.mCarrierIds.length != euiccRulesAuthTable.mCarrierIds.length) {
            return false;
        }
        for (int i = 0; i < this.mCarrierIds.length; i++) {
            CarrierIdentifier[] carrierIdentifierArr = this.mCarrierIds[i];
            CarrierIdentifier[] carrierIdentifierArr2 = euiccRulesAuthTable.mCarrierIds[i];
            if (carrierIdentifierArr != null && carrierIdentifierArr2 != null) {
                if (carrierIdentifierArr.length != carrierIdentifierArr2.length) {
                    return false;
                }
                for (int i2 = 0; i2 < carrierIdentifierArr.length; i2++) {
                    if (!carrierIdentifierArr[i2].equals(carrierIdentifierArr2[i2])) {
                        return false;
                    }
                }
            } else if (carrierIdentifierArr != null || carrierIdentifierArr2 != null) {
                return false;
            }
        }
        if (Arrays.equals(this.mPolicyRules, euiccRulesAuthTable.mPolicyRules) && Arrays.equals(this.mPolicyRuleFlags, euiccRulesAuthTable.mPolicyRuleFlags)) {
            return true;
        }
        return false;
    }

    private EuiccRulesAuthTable(Parcel parcel) {
        this.mPolicyRules = parcel.createIntArray();
        int length = this.mPolicyRules.length;
        this.mCarrierIds = new CarrierIdentifier[length][];
        for (int i = 0; i < length; i++) {
            this.mCarrierIds[i] = (CarrierIdentifier[]) parcel.createTypedArray(CarrierIdentifier.CREATOR);
        }
        this.mPolicyRuleFlags = parcel.createIntArray();
    }
}
