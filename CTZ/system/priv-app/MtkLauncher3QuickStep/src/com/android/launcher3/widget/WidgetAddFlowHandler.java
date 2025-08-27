package com.android.launcher3.widget;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.util.PendingRequestArgs;

/* loaded from: classes.dex */
public class WidgetAddFlowHandler implements Parcelable {
    public static final Parcelable.Creator<WidgetAddFlowHandler> CREATOR = new Parcelable.Creator<WidgetAddFlowHandler>() { // from class: com.android.launcher3.widget.WidgetAddFlowHandler.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public WidgetAddFlowHandler createFromParcel(Parcel parcel) {
            return new WidgetAddFlowHandler(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public WidgetAddFlowHandler[] newArray(int i) {
            return new WidgetAddFlowHandler[i];
        }
    };
    private final AppWidgetProviderInfo mProviderInfo;

    public WidgetAddFlowHandler(AppWidgetProviderInfo appWidgetProviderInfo) {
        this.mProviderInfo = appWidgetProviderInfo;
    }

    protected WidgetAddFlowHandler(Parcel parcel) {
        this.mProviderInfo = (AppWidgetProviderInfo) AppWidgetProviderInfo.CREATOR.createFromParcel(parcel);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        this.mProviderInfo.writeToParcel(parcel, i);
    }

    public void startBindFlow(Launcher launcher, int i, ItemInfo itemInfo, int i2) {
        launcher.setWaitingForResult(PendingRequestArgs.forWidgetInfo(i, this, itemInfo));
        launcher.getAppWidgetHost().startBindFlow(launcher, i, this.mProviderInfo, i2);
    }

    public boolean startConfigActivity(Launcher launcher, LauncherAppWidgetInfo launcherAppWidgetInfo, int i) {
        return startConfigActivity(launcher, launcherAppWidgetInfo.appWidgetId, launcherAppWidgetInfo, i);
    }

    public boolean startConfigActivity(Launcher launcher, int i, ItemInfo itemInfo, int i2) {
        if (!needsConfigure()) {
            return false;
        }
        launcher.setWaitingForResult(PendingRequestArgs.forWidgetInfo(i, this, itemInfo));
        launcher.getAppWidgetHost().startConfigActivity(launcher, i, i2);
        return true;
    }

    public boolean needsConfigure() {
        return this.mProviderInfo.configure != null;
    }

    public LauncherAppWidgetProviderInfo getProviderInfo(Context context) {
        return LauncherAppWidgetProviderInfo.fromProviderInfo(context, this.mProviderInfo);
    }
}
