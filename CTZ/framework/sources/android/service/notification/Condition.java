package android.service.notification;

import android.annotation.SystemApi;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyProperties;
import android.util.proto.ProtoOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

public final class Condition implements Parcelable {
    public static final Parcelable.Creator<Condition> CREATOR = new Parcelable.Creator<Condition>() {
        @Override
        public Condition createFromParcel(Parcel parcel) {
            return new Condition(parcel);
        }

        @Override
        public Condition[] newArray(int i) {
            return new Condition[i];
        }
    };

    @SystemApi
    public static final int FLAG_RELEVANT_ALWAYS = 2;

    @SystemApi
    public static final int FLAG_RELEVANT_NOW = 1;

    @SystemApi
    public static final String SCHEME = "condition";

    @SystemApi
    public static final int STATE_ERROR = 3;
    public static final int STATE_FALSE = 0;
    public static final int STATE_TRUE = 1;

    @SystemApi
    public static final int STATE_UNKNOWN = 2;

    @SystemApi
    public final int flags;

    @SystemApi
    public final int icon;
    public final Uri id;

    @SystemApi
    public final String line1;

    @SystemApi
    public final String line2;
    public final int state;
    public final String summary;

    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    public Condition(Uri uri, String str, int i) {
        this(uri, str, "", "", -1, i, 2);
    }

    @SystemApi
    public Condition(Uri uri, String str, String str2, String str3, int i, int i2, int i3) {
        if (uri == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (str == null) {
            throw new IllegalArgumentException("summary is required");
        }
        if (!isValidState(i2)) {
            throw new IllegalArgumentException("state is invalid: " + i2);
        }
        this.id = uri;
        this.summary = str;
        this.line1 = str2;
        this.line2 = str3;
        this.icon = i;
        this.state = i2;
        this.flags = i3;
    }

    public Condition(Parcel parcel) {
        this((Uri) parcel.readParcelable(Condition.class.getClassLoader()), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
    }

    private static boolean isValidState(int i) {
        return i >= 0 && i <= 3;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.id, 0);
        parcel.writeString(this.summary);
        parcel.writeString(this.line1);
        parcel.writeString(this.line2);
        parcel.writeInt(this.icon);
        parcel.writeInt(this.state);
        parcel.writeInt(this.flags);
    }

    public String toString() {
        return Condition.class.getSimpleName() + "[id=" + this.id + ",summary=" + this.summary + ",line1=" + this.line1 + ",line2=" + this.line2 + ",icon=" + this.icon + ",state=" + stateToString(this.state) + ",flags=" + this.flags + ']';
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.id.toString());
        protoOutputStream.write(1138166333442L, this.summary);
        protoOutputStream.write(1138166333443L, this.line1);
        protoOutputStream.write(1138166333444L, this.line2);
        protoOutputStream.write(1120986464261L, this.icon);
        protoOutputStream.write(1159641169926L, this.state);
        protoOutputStream.write(1120986464263L, this.flags);
        protoOutputStream.end(jStart);
    }

    @SystemApi
    public static String stateToString(int i) {
        if (i == 0) {
            return "STATE_FALSE";
        }
        if (i == 1) {
            return "STATE_TRUE";
        }
        if (i == 2) {
            return "STATE_UNKNOWN";
        }
        if (i == 3) {
            return "STATE_ERROR";
        }
        throw new IllegalArgumentException("state is invalid: " + i);
    }

    @SystemApi
    public static String relevanceToString(int i) {
        boolean z = (i & 1) != 0;
        boolean z2 = (i & 2) != 0;
        return (z || z2) ? (z && z2) ? "NOW, ALWAYS" : z ? "NOW" : "ALWAYS" : KeyProperties.DIGEST_NONE;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Condition)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Condition condition = (Condition) obj;
        return Objects.equals(condition.id, this.id) && Objects.equals(condition.summary, this.summary) && Objects.equals(condition.line1, this.line1) && Objects.equals(condition.line2, this.line2) && condition.icon == this.icon && condition.state == this.state && condition.flags == this.flags;
    }

    public int hashCode() {
        return Objects.hash(this.id, this.summary, this.line1, this.line2, Integer.valueOf(this.icon), Integer.valueOf(this.state), Integer.valueOf(this.flags));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SystemApi
    public Condition copy() {
        Parcel parcelObtain = Parcel.obtain();
        try {
            writeToParcel(parcelObtain, 0);
            parcelObtain.setDataPosition(0);
            return new Condition(parcelObtain);
        } finally {
            parcelObtain.recycle();
        }
    }

    @SystemApi
    public static Uri.Builder newId(Context context) {
        return new Uri.Builder().scheme(SCHEME).authority(context.getPackageName());
    }

    @SystemApi
    public static boolean isValidId(Uri uri, String str) {
        return uri != null && SCHEME.equals(uri.getScheme()) && str.equals(uri.getAuthority());
    }
}
