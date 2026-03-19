package android.accounts;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Account implements Parcelable {
    public static final Parcelable.Creator<Account> CREATOR = new Parcelable.Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel source) {
            return new Account(source);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };
    public final String name;
    public final String type;

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Account)) {
            return false;
        }
        Account other = (Account) o;
        if (this.name.equals(other.name)) {
            return this.type.equals(other.type);
        }
        return false;
    }

    public int hashCode() {
        int result = this.name.hashCode() + 527;
        return (result * 31) + this.type.hashCode();
    }

    public Account(String name, String type) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("the name must not be empty: " + name);
        }
        if (TextUtils.isEmpty(type)) {
            throw new IllegalArgumentException("the type must not be empty: " + type);
        }
        this.name = name;
        this.type = type;
    }

    public Account(Parcel in) {
        this.name = in.readString();
        this.type = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.type);
    }

    public String toString() {
        return "Account {name=" + this.name + ", type=" + this.type + "}";
    }
}
