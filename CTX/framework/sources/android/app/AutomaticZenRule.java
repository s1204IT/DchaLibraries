package android.app;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class AutomaticZenRule implements Parcelable {
    public static final Parcelable.Creator<AutomaticZenRule> CREATOR = new Parcelable.Creator<AutomaticZenRule>() {
        @Override
        public AutomaticZenRule createFromParcel(Parcel parcel) {
            return new AutomaticZenRule(parcel);
        }

        @Override
        public AutomaticZenRule[] newArray(int i) {
            return new AutomaticZenRule[i];
        }
    };
    private Uri conditionId;
    private long creationTime;
    private boolean enabled;
    private int interruptionFilter;
    private String name;
    private ComponentName owner;

    public AutomaticZenRule(String str, ComponentName componentName, Uri uri, int i, boolean z) {
        this.enabled = false;
        this.name = str;
        this.owner = componentName;
        this.conditionId = uri;
        this.interruptionFilter = i;
        this.enabled = z;
    }

    public AutomaticZenRule(String str, ComponentName componentName, Uri uri, int i, boolean z, long j) {
        this(str, componentName, uri, i, z);
        this.creationTime = j;
    }

    public AutomaticZenRule(Parcel parcel) {
        this.enabled = false;
        this.enabled = parcel.readInt() == 1;
        if (parcel.readInt() == 1) {
            this.name = parcel.readString();
        }
        this.interruptionFilter = parcel.readInt();
        this.conditionId = (Uri) parcel.readParcelable(null);
        this.owner = (ComponentName) parcel.readParcelable(null);
        this.creationTime = parcel.readLong();
    }

    public ComponentName getOwner() {
        return this.owner;
    }

    public Uri getConditionId() {
        return this.conditionId;
    }

    public int getInterruptionFilter() {
        return this.interruptionFilter;
    }

    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public void setConditionId(Uri uri) {
        this.conditionId = uri;
    }

    public void setInterruptionFilter(int i) {
        this.interruptionFilter = i;
    }

    public void setName(String str) {
        this.name = str;
    }

    public void setEnabled(boolean z) {
        this.enabled = z;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.enabled ? 1 : 0);
        if (this.name != null) {
            parcel.writeInt(1);
            parcel.writeString(this.name);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.interruptionFilter);
        parcel.writeParcelable(this.conditionId, 0);
        parcel.writeParcelable(this.owner, 0);
        parcel.writeLong(this.creationTime);
    }

    public String toString() {
        return AutomaticZenRule.class.getSimpleName() + "[enabled=" + this.enabled + ",name=" + this.name + ",interruptionFilter=" + this.interruptionFilter + ",conditionId=" + this.conditionId + ",owner=" + this.owner + ",creationTime=" + this.creationTime + ']';
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AutomaticZenRule)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        AutomaticZenRule automaticZenRule = (AutomaticZenRule) obj;
        return automaticZenRule.enabled == this.enabled && Objects.equals(automaticZenRule.name, this.name) && automaticZenRule.interruptionFilter == this.interruptionFilter && Objects.equals(automaticZenRule.conditionId, this.conditionId) && Objects.equals(automaticZenRule.owner, this.owner) && automaticZenRule.creationTime == this.creationTime;
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.enabled), this.name, Integer.valueOf(this.interruptionFilter), this.conditionId, this.owner, Long.valueOf(this.creationTime));
    }
}
