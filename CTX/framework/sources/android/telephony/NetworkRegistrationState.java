package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.telephony.IccCardConstants;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

public class NetworkRegistrationState implements Parcelable {
    public static final Parcelable.Creator<NetworkRegistrationState> CREATOR = new Parcelable.Creator<NetworkRegistrationState>() {
        @Override
        public NetworkRegistrationState createFromParcel(Parcel parcel) {
            return NetworkRegistrationState.makeNetworkRegistrationState(parcel);
        }

        @Override
        public NetworkRegistrationState[] newArray(int i) {
            return new NetworkRegistrationState[i];
        }
    };
    public static final int DOMAIN_CS = 1;
    public static final int DOMAIN_PS = 2;
    protected static final String LOG_TAG = "NetworkRegistrationState";
    public static final int REG_STATE_DENIED = 3;
    public static final int REG_STATE_HOME = 1;
    public static final int REG_STATE_NOT_REG_NOT_SEARCHING = 0;
    public static final int REG_STATE_NOT_REG_SEARCHING = 2;
    public static final int REG_STATE_ROAMING = 5;
    public static final int REG_STATE_UNKNOWN = 4;
    public static final int SERVICE_TYPE_DATA = 2;
    public static final int SERVICE_TYPE_EMERGENCY = 5;
    public static final int SERVICE_TYPE_SMS = 3;
    public static final int SERVICE_TYPE_VIDEO = 4;
    public static final int SERVICE_TYPE_VOICE = 1;
    private final int mAccessNetworkTechnology;
    private final int[] mAvailableServices;
    private final CellIdentity mCellIdentity;
    protected DataSpecificRegistrationStates mDataSpecificStates;
    private final int mDomain;
    private final boolean mEmergencyOnly;
    private final int mReasonForDenial;
    private final int mRegState;
    private final int mTransportType;
    protected VoiceSpecificRegistrationStates mVoiceSpecificStates;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Domain {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RegState {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ServiceType {
    }

    public NetworkRegistrationState(int i, int i2, int i3, int i4, int i5, boolean z, int[] iArr, CellIdentity cellIdentity) {
        this.mTransportType = i;
        this.mDomain = i2;
        this.mRegState = i3;
        this.mAccessNetworkTechnology = i4;
        this.mReasonForDenial = i5;
        this.mAvailableServices = iArr;
        this.mCellIdentity = cellIdentity;
        this.mEmergencyOnly = z;
    }

    public NetworkRegistrationState(int i, int i2, int i3, int i4, int i5, boolean z, int[] iArr, CellIdentity cellIdentity, boolean z2, int i6, int i7, int i8) {
        this(i, i2, i3, i4, i5, z, iArr, cellIdentity);
        this.mVoiceSpecificStates = new VoiceSpecificRegistrationStates(z2, i6, i7, i8);
    }

    public NetworkRegistrationState(int i, int i2, int i3, int i4, int i5, boolean z, int[] iArr, CellIdentity cellIdentity, int i6) {
        this(i, i2, i3, i4, i5, z, iArr, cellIdentity);
        this.mDataSpecificStates = new DataSpecificRegistrationStates(i6);
    }

    protected NetworkRegistrationState(Parcel parcel) {
        this.mTransportType = parcel.readInt();
        this.mDomain = parcel.readInt();
        this.mRegState = parcel.readInt();
        this.mAccessNetworkTechnology = parcel.readInt();
        this.mReasonForDenial = parcel.readInt();
        this.mEmergencyOnly = parcel.readBoolean();
        this.mAvailableServices = parcel.createIntArray();
        this.mCellIdentity = (CellIdentity) parcel.readParcelable(CellIdentity.class.getClassLoader());
        this.mVoiceSpecificStates = (VoiceSpecificRegistrationStates) parcel.readParcelable(VoiceSpecificRegistrationStates.class.getClassLoader());
        this.mDataSpecificStates = (DataSpecificRegistrationStates) parcel.readParcelable(DataSpecificRegistrationStates.class.getClassLoader());
    }

    public int getTransportType() {
        return this.mTransportType;
    }

    public int getDomain() {
        return this.mDomain;
    }

    public int getRegState() {
        return this.mRegState;
    }

    public boolean isEmergencyEnabled() {
        return this.mEmergencyOnly;
    }

    public int[] getAvailableServices() {
        return this.mAvailableServices;
    }

    public int getAccessNetworkTechnology() {
        return this.mAccessNetworkTechnology;
    }

    public int getReasonForDenial() {
        return this.mReasonForDenial;
    }

    public CellIdentity getCellIdentity() {
        return this.mCellIdentity;
    }

    public VoiceSpecificRegistrationStates getVoiceSpecificStates() {
        return this.mVoiceSpecificStates;
    }

    public DataSpecificRegistrationStates getDataSpecificStates() {
        return this.mDataSpecificStates;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static String regStateToString(int i) {
        switch (i) {
            case 0:
                return "NOT_REG_NOT_SEARCHING";
            case 1:
                return "HOME";
            case 2:
                return "NOT_REG_SEARCHING";
            case 3:
                return "DENIED";
            case 4:
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
            case 5:
                return "ROAMING";
            default:
                return "Unknown reg state " + i;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("NetworkRegistrationState{");
        sb.append("transportType=");
        sb.append(this.mTransportType);
        sb.append(" domain=");
        sb.append(this.mDomain == 1 ? "CS" : "PS");
        sb.append(" regState=");
        sb.append(regStateToString(this.mRegState));
        sb.append(" accessNetworkTechnology=");
        sb.append(TelephonyManager.getNetworkTypeName(this.mAccessNetworkTechnology));
        sb.append(" reasonForDenial=");
        sb.append(this.mReasonForDenial);
        sb.append(" emergencyEnabled=");
        sb.append(this.mEmergencyOnly);
        sb.append(" supportedServices=");
        sb.append(this.mAvailableServices);
        sb.append(" cellIdentity=");
        sb.append(this.mCellIdentity);
        sb.append(" voiceSpecificStates=");
        sb.append(this.mVoiceSpecificStates);
        sb.append(" dataSpecificStates=");
        sb.append(this.mDataSpecificStates);
        sb.append("}");
        return sb.toString();
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mTransportType), Integer.valueOf(this.mDomain), Integer.valueOf(this.mRegState), Integer.valueOf(this.mAccessNetworkTechnology), Integer.valueOf(this.mReasonForDenial), Boolean.valueOf(this.mEmergencyOnly), this.mAvailableServices, this.mCellIdentity, this.mVoiceSpecificStates, this.mDataSpecificStates);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof NetworkRegistrationState)) {
            return false;
        }
        NetworkRegistrationState networkRegistrationState = (NetworkRegistrationState) obj;
        if (this.mTransportType == networkRegistrationState.mTransportType && this.mDomain == networkRegistrationState.mDomain && this.mRegState == networkRegistrationState.mRegState && this.mAccessNetworkTechnology == networkRegistrationState.mAccessNetworkTechnology && this.mReasonForDenial == networkRegistrationState.mReasonForDenial && this.mEmergencyOnly == networkRegistrationState.mEmergencyOnly && ((this.mAvailableServices == networkRegistrationState.mAvailableServices || Arrays.equals(this.mAvailableServices, networkRegistrationState.mAvailableServices)) && equals(this.mCellIdentity, networkRegistrationState.mCellIdentity) && equals(this.mVoiceSpecificStates, networkRegistrationState.mVoiceSpecificStates) && equals(this.mDataSpecificStates, networkRegistrationState.mDataSpecificStates))) {
            return true;
        }
        return false;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mTransportType);
        parcel.writeInt(this.mDomain);
        parcel.writeInt(this.mRegState);
        parcel.writeInt(this.mAccessNetworkTechnology);
        parcel.writeInt(this.mReasonForDenial);
        parcel.writeBoolean(this.mEmergencyOnly);
        parcel.writeIntArray(this.mAvailableServices);
        parcel.writeParcelable(this.mCellIdentity, 0);
        parcel.writeParcelable(this.mVoiceSpecificStates, 0);
        parcel.writeParcelable(this.mDataSpecificStates, 0);
    }

    private static boolean equals(Object obj, Object obj2) {
        if (obj == obj2) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return obj.equals(obj2);
    }

    private static final NetworkRegistrationState makeNetworkRegistrationState(Parcel parcel) {
        try {
            Constructor<?> constructor = Class.forName("mediatek.telephony.MtkNetworkRegistrationState").getConstructor(Parcel.class);
            constructor.setAccessible(true);
            return (NetworkRegistrationState) constructor.newInstance(parcel);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Rlog.e(LOG_TAG, "IllegalAccessException! Used AOSP!");
            return new NetworkRegistrationState(parcel);
        } catch (InstantiationException e2) {
            e2.printStackTrace();
            Rlog.e(LOG_TAG, "InstantiationException! Used AOSP!");
            return new NetworkRegistrationState(parcel);
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
            Rlog.e(LOG_TAG, "NoSuchMethodException! Used AOSP!");
            return new NetworkRegistrationState(parcel);
        } catch (InvocationTargetException e4) {
            e4.printStackTrace();
            Rlog.e(LOG_TAG, "InvocationTargetException! Used AOSP!");
            return new NetworkRegistrationState(parcel);
        } catch (Exception e5) {
            return new NetworkRegistrationState(parcel);
        }
    }
}
