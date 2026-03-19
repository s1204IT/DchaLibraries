package android.service.notification;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

public class StatusBarNotification implements Parcelable {
    public static final Parcelable.Creator<StatusBarNotification> CREATOR = new Parcelable.Creator<StatusBarNotification>() {
        @Override
        public StatusBarNotification createFromParcel(Parcel parcel) {
            return new StatusBarNotification(parcel);
        }

        @Override
        public StatusBarNotification[] newArray(int i) {
            return new StatusBarNotification[i];
        }
    };
    private String groupKey;
    private final int id;
    private final int initialPid;
    private final String key;
    private Context mContext;
    private final Notification notification;
    private final String opPkg;
    private String overrideGroupKey;
    private final String pkg;
    private final long postTime;
    private final String tag;
    private final int uid;
    private final UserHandle user;

    public StatusBarNotification(String str, String str2, int i, String str3, int i2, int i3, Notification notification, UserHandle userHandle, String str4, long j) {
        if (str == null) {
            throw new NullPointerException();
        }
        if (notification == null) {
            throw new NullPointerException();
        }
        this.pkg = str;
        this.opPkg = str2;
        this.id = i;
        this.tag = str3;
        this.uid = i2;
        this.initialPid = i3;
        this.notification = notification;
        this.user = userHandle;
        this.postTime = j;
        this.overrideGroupKey = str4;
        this.key = key();
        this.groupKey = groupKey();
    }

    @Deprecated
    public StatusBarNotification(String str, String str2, int i, String str3, int i2, int i3, int i4, Notification notification, UserHandle userHandle, long j) {
        if (str == null) {
            throw new NullPointerException();
        }
        if (notification == null) {
            throw new NullPointerException();
        }
        this.pkg = str;
        this.opPkg = str2;
        this.id = i;
        this.tag = str3;
        this.uid = i2;
        this.initialPid = i3;
        this.notification = notification;
        this.user = userHandle;
        this.postTime = j;
        this.key = key();
        this.groupKey = groupKey();
    }

    public StatusBarNotification(Parcel parcel) {
        this.pkg = parcel.readString();
        this.opPkg = parcel.readString();
        this.id = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.tag = parcel.readString();
        } else {
            this.tag = null;
        }
        this.uid = parcel.readInt();
        this.initialPid = parcel.readInt();
        this.notification = new Notification(parcel);
        this.user = UserHandle.readFromParcel(parcel);
        this.postTime = parcel.readLong();
        if (parcel.readInt() != 0) {
            this.overrideGroupKey = parcel.readString();
        } else {
            this.overrideGroupKey = null;
        }
        this.key = key();
        this.groupKey = groupKey();
    }

    private String key() {
        String str = this.user.getIdentifier() + "|" + this.pkg + "|" + this.id + "|" + this.tag + "|" + this.uid;
        if (this.overrideGroupKey != null && getNotification().isGroupSummary()) {
            return str + "|" + this.overrideGroupKey;
        }
        return str;
    }

    private String groupKey() {
        String str;
        if (this.overrideGroupKey != null) {
            return this.user.getIdentifier() + "|" + this.pkg + "|g:" + this.overrideGroupKey;
        }
        String group = getNotification().getGroup();
        String sortKey = getNotification().getSortKey();
        if (group == null && sortKey == null) {
            return this.key;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.user.getIdentifier());
        sb.append("|");
        sb.append(this.pkg);
        sb.append("|");
        if (group == null) {
            str = "c:" + this.notification.getChannelId();
        } else {
            str = "g:" + group;
        }
        sb.append(str);
        return sb.toString();
    }

    public boolean isGroup() {
        if (this.overrideGroupKey != null || isAppGroup()) {
            return true;
        }
        return false;
    }

    public boolean isAppGroup() {
        if (getNotification().getGroup() != null || getNotification().getSortKey() != null) {
            return true;
        }
        return false;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.pkg);
        parcel.writeString(this.opPkg);
        parcel.writeInt(this.id);
        if (this.tag != null) {
            parcel.writeInt(1);
            parcel.writeString(this.tag);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.uid);
        parcel.writeInt(this.initialPid);
        this.notification.writeToParcel(parcel, i);
        this.user.writeToParcel(parcel, i);
        parcel.writeLong(this.postTime);
        if (this.overrideGroupKey != null) {
            parcel.writeInt(1);
            parcel.writeString(this.overrideGroupKey);
        } else {
            parcel.writeInt(0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public StatusBarNotification cloneLight() {
        Notification notification = new Notification();
        this.notification.cloneInto(notification, false);
        return new StatusBarNotification(this.pkg, this.opPkg, this.id, this.tag, this.uid, this.initialPid, notification, this.user, this.overrideGroupKey, this.postTime);
    }

    public StatusBarNotification m29clone() {
        return new StatusBarNotification(this.pkg, this.opPkg, this.id, this.tag, this.uid, this.initialPid, this.notification.m8clone(), this.user, this.overrideGroupKey, this.postTime);
    }

    public String toString() {
        return String.format("StatusBarNotification(pkg=%s user=%s id=%d tag=%s key=%s: %s)", this.pkg, this.user, Integer.valueOf(this.id), this.tag, this.key, this.notification);
    }

    public boolean isOngoing() {
        return (this.notification.flags & 2) != 0;
    }

    public boolean isClearable() {
        return (this.notification.flags & 2) == 0 && (this.notification.flags & 32) == 0;
    }

    @Deprecated
    public int getUserId() {
        return this.user.getIdentifier();
    }

    public String getPackageName() {
        return this.pkg;
    }

    public int getId() {
        return this.id;
    }

    public String getTag() {
        return this.tag;
    }

    public int getUid() {
        return this.uid;
    }

    public String getOpPkg() {
        return this.opPkg;
    }

    public int getInitialPid() {
        return this.initialPid;
    }

    public Notification getNotification() {
        return this.notification;
    }

    public UserHandle getUser() {
        return this.user;
    }

    public long getPostTime() {
        return this.postTime;
    }

    public String getKey() {
        return this.key;
    }

    public String getGroupKey() {
        return this.groupKey;
    }

    public String getGroup() {
        if (this.overrideGroupKey != null) {
            return this.overrideGroupKey;
        }
        return getNotification().getGroup();
    }

    public void setOverrideGroupKey(String str) {
        this.overrideGroupKey = str;
        this.groupKey = groupKey();
    }

    public String getOverrideGroupKey() {
        return this.overrideGroupKey;
    }

    public Context getPackageContext(Context context) {
        if (this.mContext == null) {
            try {
                this.mContext = context.createApplicationContext(context.getPackageManager().getApplicationInfoAsUser(this.pkg, 8192, getUserId()), 4);
            } catch (PackageManager.NameNotFoundException e) {
                this.mContext = null;
            }
        }
        if (this.mContext == null) {
            this.mContext = context;
        }
        return this.mContext;
    }
}
