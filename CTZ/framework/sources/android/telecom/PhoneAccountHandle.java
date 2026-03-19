package android.telecom;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import java.util.Objects;

public final class PhoneAccountHandle implements Parcelable {
    public static final Parcelable.Creator<PhoneAccountHandle> CREATOR = new Parcelable.Creator<PhoneAccountHandle>() {
        @Override
        public PhoneAccountHandle createFromParcel(Parcel parcel) {
            return new PhoneAccountHandle(parcel);
        }

        @Override
        public PhoneAccountHandle[] newArray(int i) {
            return new PhoneAccountHandle[i];
        }
    };
    private final ComponentName mComponentName;
    private final String mId;
    private final UserHandle mUserHandle;

    public PhoneAccountHandle(ComponentName componentName, String str) {
        this(componentName, str, Process.myUserHandle());
    }

    public PhoneAccountHandle(ComponentName componentName, String str, UserHandle userHandle) {
        checkParameters(componentName, userHandle);
        this.mComponentName = componentName;
        this.mId = str;
        this.mUserHandle = userHandle;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public String getId() {
        return this.mId;
    }

    public UserHandle getUserHandle() {
        return this.mUserHandle;
    }

    public int hashCode() {
        return Objects.hash(this.mComponentName, this.mId, this.mUserHandle);
    }

    public String toString() {
        return this.mComponentName + ", " + Log.pii(this.mId) + ", " + this.mUserHandle;
    }

    public boolean equals(Object obj) {
        if (obj != null && (obj instanceof PhoneAccountHandle)) {
            PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) obj;
            if (Objects.equals(phoneAccountHandle.getComponentName(), getComponentName()) && Objects.equals(phoneAccountHandle.getId(), getId()) && Objects.equals(phoneAccountHandle.getUserHandle(), getUserHandle())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mComponentName.writeToParcel(parcel, i);
        parcel.writeString(this.mId);
        this.mUserHandle.writeToParcel(parcel, i);
    }

    private void checkParameters(ComponentName componentName, UserHandle userHandle) {
        if (componentName == null) {
            android.util.Log.w("PhoneAccountHandle", new Exception("PhoneAccountHandle has been created with null ComponentName!"));
        }
        if (userHandle == null) {
            android.util.Log.w("PhoneAccountHandle", new Exception("PhoneAccountHandle has been created with null UserHandle!"));
        }
    }

    private PhoneAccountHandle(Parcel parcel) {
        this(ComponentName.CREATOR.createFromParcel(parcel), parcel.readString(), UserHandle.CREATOR.createFromParcel(parcel));
    }
}
