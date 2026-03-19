package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class DataSpecificRegistrationStates implements Parcelable {
    public static final Parcelable.Creator<DataSpecificRegistrationStates> CREATOR = new Parcelable.Creator<DataSpecificRegistrationStates>() {
        @Override
        public DataSpecificRegistrationStates createFromParcel(Parcel parcel) {
            return DataSpecificRegistrationStates.makeDataSpecificRegistrationStates(parcel);
        }

        @Override
        public DataSpecificRegistrationStates[] newArray(int i) {
            return new DataSpecificRegistrationStates[i];
        }
    };
    protected static final String LOG_TAG = "DataSpecificRegistrationStates";
    public final int maxDataCalls;

    protected DataSpecificRegistrationStates(int i) {
        this.maxDataCalls = i;
    }

    protected DataSpecificRegistrationStates(Parcel parcel) {
        this.maxDataCalls = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.maxDataCalls);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "DataSpecificRegistrationStates { mMaxDataCalls=" + this.maxDataCalls + "}";
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.maxDataCalls));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && (obj instanceof DataSpecificRegistrationStates) && this.maxDataCalls == ((DataSpecificRegistrationStates) obj).maxDataCalls) {
            return true;
        }
        return false;
    }

    private static final DataSpecificRegistrationStates makeDataSpecificRegistrationStates(Parcel parcel) {
        try {
            Constructor<?> constructor = Class.forName("mediatek.telephony.MtkDataSpecificRegistrationStates").getConstructor(Parcel.class);
            constructor.setAccessible(true);
            return (DataSpecificRegistrationStates) constructor.newInstance(parcel);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Rlog.e(LOG_TAG, "IllegalAccessException! Used AOSP!");
            return new DataSpecificRegistrationStates(parcel);
        } catch (InstantiationException e2) {
            e2.printStackTrace();
            Rlog.e(LOG_TAG, "InstantiationException! Used AOSP!");
            return new DataSpecificRegistrationStates(parcel);
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
            Rlog.e(LOG_TAG, "NoSuchMethodException! Used AOSP!");
            return new DataSpecificRegistrationStates(parcel);
        } catch (InvocationTargetException e4) {
            e4.printStackTrace();
            Rlog.e(LOG_TAG, "InvocationTargetException! Used AOSP!");
            return new DataSpecificRegistrationStates(parcel);
        } catch (Exception e5) {
            return new DataSpecificRegistrationStates(parcel);
        }
    }
}
