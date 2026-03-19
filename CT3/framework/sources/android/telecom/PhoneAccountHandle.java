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
        public PhoneAccountHandle createFromParcel(Parcel in) {
            return new PhoneAccountHandle(in, (PhoneAccountHandle) null);
        }

        @Override
        public PhoneAccountHandle[] newArray(int size) {
            return new PhoneAccountHandle[size];
        }
    };
    private final ComponentName mComponentName;
    private final String mId;
    private final UserHandle mUserHandle;

    PhoneAccountHandle(Parcel in, PhoneAccountHandle phoneAccountHandle) {
        this(in);
    }

    public PhoneAccountHandle(ComponentName componentName, String id) {
        this(componentName, id, Process.myUserHandle());
    }

    public PhoneAccountHandle(ComponentName componentName, String id, UserHandle userHandle) {
        checkParameters(componentName, userHandle);
        this.mComponentName = componentName;
        this.mId = id;
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
        String className = this.mComponentName.getClassName();
        return "PhoneAccountHandle{" + className.substring(className.lastIndexOf(46) + 1) + ", " + Log.pii(this.mId) + ", " + this.mUserHandle + "}";
    }

    public boolean equals(Object other) {
        if (other != null && (other instanceof PhoneAccountHandle) && Objects.equals(((PhoneAccountHandle) other).getComponentName(), getComponentName()) && Objects.equals(((PhoneAccountHandle) other).getId(), getId())) {
            return Objects.equals(((PhoneAccountHandle) other).getUserHandle(), getUserHandle());
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        this.mComponentName.writeToParcel(out, flags);
        out.writeString(this.mId);
        this.mUserHandle.writeToParcel(out, flags);
    }

    private void checkParameters(ComponentName componentName, UserHandle userHandle) {
        if (componentName == null) {
            android.util.Log.w("PhoneAccountHandle", new Exception("PhoneAccountHandle has been created with null ComponentName!"));
        }
        if (userHandle != null) {
            return;
        }
        android.util.Log.w("PhoneAccountHandle", new Exception("PhoneAccountHandle has been created with null UserHandle!"));
    }

    private PhoneAccountHandle(Parcel in) {
        this(ComponentName.CREATOR.createFromParcel(in), in.readString(), UserHandle.CREATOR.createFromParcel(in));
    }
}
