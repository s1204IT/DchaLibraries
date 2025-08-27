package com.mediatek.nfcsettingsadapter;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/* loaded from: classes.dex */
public final class ServiceEntry implements Parcelable {
    Integer banner;
    ComponentName component;
    String tag;
    String title;
    Boolean wantEnabled;
    Boolean wasEnabled;
    private static String TAG = "ServiceEntry";
    public static final Parcelable.Creator<ServiceEntry> CREATOR = new Parcelable.Creator<ServiceEntry>() { // from class: com.mediatek.nfcsettingsadapter.ServiceEntry.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ServiceEntry createFromParcel(Parcel parcel) {
            ComponentName componentName;
            String string = parcel.readString();
            String string2 = parcel.readString();
            Integer numValueOf = Integer.valueOf(parcel.readInt());
            Boolean bool = new Boolean(parcel.readInt() != 0);
            Boolean bool2 = new Boolean(parcel.readInt() != 0);
            if (getClass().getClassLoader() == null) {
                componentName = null;
            } else {
                componentName = (ComponentName) parcel.readParcelable(getClass().getClassLoader());
            }
            return new ServiceEntry(componentName, string, string2, numValueOf, bool, bool2);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ServiceEntry[] newArray(int i) {
            return new ServiceEntry[i];
        }
    };

    public ServiceEntry(ComponentName componentName, String str, String str2, Integer num, Boolean bool, Boolean bool2) {
        this.component = componentName;
        this.tag = str;
        this.title = str2;
        this.banner = num;
        this.wasEnabled = bool;
        this.wantEnabled = bool2;
    }

    public Drawable getIcon(PackageManager packageManager) {
        try {
            return packageManager.getApplicationIcon(this.component.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not load icon.");
            return null;
        }
    }

    public String getTitle() {
        return this.title;
    }

    public Boolean getWasEnabled() {
        return this.wasEnabled;
    }

    public Boolean getWantEnabled() {
        return this.wantEnabled;
    }

    public void setWantEnabled(Boolean bool) {
        this.wantEnabled = bool;
    }

    public ComponentName getComponent() {
        return this.component;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.tag);
        parcel.writeString(this.title);
        parcel.writeInt(this.banner.intValue());
        if (this.wasEnabled.booleanValue()) {
            parcel.writeInt(1);
        } else {
            parcel.writeInt(0);
        }
        if (this.wantEnabled.booleanValue()) {
            parcel.writeInt(1);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeParcelable(this.component, i);
    }
}
