package com.android.launcher3.widget.custom;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.Utilities;

/* loaded from: classes.dex */
public class CustomAppWidgetProviderInfo extends LauncherAppWidgetProviderInfo implements Parcelable {
    public static final Parcelable.Creator<CustomAppWidgetProviderInfo> CREATOR = new Parcelable.Creator<CustomAppWidgetProviderInfo>() { // from class: com.android.launcher3.widget.custom.CustomAppWidgetProviderInfo.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CustomAppWidgetProviderInfo createFromParcel(Parcel parcel) {
            return new CustomAppWidgetProviderInfo(parcel, true, 0);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CustomAppWidgetProviderInfo[] newArray(int i) {
            return new CustomAppWidgetProviderInfo[i];
        }
    };
    public final int providerId;

    protected CustomAppWidgetProviderInfo(Parcel parcel, boolean z, int i) {
        super(parcel);
        if (z) {
            this.providerId = parcel.readInt();
            this.provider = new ComponentName(parcel.readString(), LauncherAppWidgetProviderInfo.CLS_CUSTOM_WIDGET_PREFIX + i);
            this.label = parcel.readString();
            this.initialLayout = parcel.readInt();
            this.icon = parcel.readInt();
            this.previewImage = parcel.readInt();
            this.resizeMode = parcel.readInt();
            this.spanX = parcel.readInt();
            this.spanY = parcel.readInt();
            this.minSpanX = parcel.readInt();
            this.minSpanY = parcel.readInt();
            return;
        }
        this.providerId = i;
    }

    @Override // com.android.launcher3.LauncherAppWidgetProviderInfo
    public void initSpans(Context context) {
    }

    @Override // com.android.launcher3.LauncherAppWidgetProviderInfo
    public String getLabel(PackageManager packageManager) {
        return Utilities.trim(this.label);
    }

    @Override // android.appwidget.AppWidgetProviderInfo
    public String toString() {
        return "WidgetProviderInfo(" + this.provider + ")";
    }

    @Override // android.appwidget.AppWidgetProviderInfo, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.providerId);
        parcel.writeString(this.provider.getPackageName());
        parcel.writeString(this.label);
        parcel.writeInt(this.initialLayout);
        parcel.writeInt(this.icon);
        parcel.writeInt(this.previewImage);
        parcel.writeInt(this.resizeMode);
        parcel.writeInt(this.spanX);
        parcel.writeInt(this.spanY);
        parcel.writeInt(this.minSpanX);
        parcel.writeInt(this.minSpanY);
    }
}
