package org.gsma.joyn.contacts;

import android.os.Parcel;
import android.os.Parcelable;
import org.gsma.joyn.Logger;
import org.gsma.joyn.capability.Capabilities;

public class JoynContact implements Parcelable {
    public static final Parcelable.Creator<JoynContact> CREATOR = new Parcelable.Creator<JoynContact>() {
        @Override
        public JoynContact createFromParcel(Parcel source) {
            return new JoynContact(source);
        }

        @Override
        public JoynContact[] newArray(int size) {
            return new JoynContact[size];
        }
    };
    public static final String TAG = "TAPI-JoynContact";
    private Capabilities capabilities;
    private String contactId;
    private boolean registered;

    public JoynContact(String contactId, boolean registered, Capabilities capabilities) {
        this.capabilities = null;
        this.contactId = null;
        this.registered = false;
        Logger.i(TAG, "JoynContact entrycontactId =" + contactId + " registered=" + registered + " capabilities=" + capabilities);
        this.contactId = contactId;
        this.registered = registered;
        this.capabilities = capabilities;
    }

    public JoynContact(Parcel source) {
        this.capabilities = null;
        this.contactId = null;
        this.registered = false;
        this.contactId = source.readString();
        this.registered = source.readInt() != 0;
        boolean flag = source.readInt() != 0;
        if (flag) {
            this.capabilities = (Capabilities) source.readParcelable(getClass().getClassLoader());
        } else {
            this.capabilities = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.contactId);
        dest.writeInt(this.registered ? 1 : 0);
        if (this.capabilities != null) {
            dest.writeInt(1);
            dest.writeParcelable(this.capabilities, flags);
        } else {
            dest.writeInt(0);
        }
    }

    public String getContactId() {
        return this.contactId;
    }

    public boolean isRegistered() {
        return this.registered;
    }

    public Capabilities getCapabilities() {
        return this.capabilities;
    }
}
