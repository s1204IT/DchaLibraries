package android.appwidget;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.RemoteViews;

public class PendingHostUpdate implements Parcelable {
    public static final Parcelable.Creator<PendingHostUpdate> CREATOR = new Parcelable.Creator<PendingHostUpdate>() {
        @Override
        public PendingHostUpdate createFromParcel(Parcel parcel) {
            return new PendingHostUpdate(parcel);
        }

        @Override
        public PendingHostUpdate[] newArray(int i) {
            return new PendingHostUpdate[i];
        }
    };
    static final int TYPE_PROVIDER_CHANGED = 1;
    static final int TYPE_VIEWS_UPDATE = 0;
    static final int TYPE_VIEW_DATA_CHANGED = 2;
    final int appWidgetId;
    final int type;
    int viewId;
    RemoteViews views;
    AppWidgetProviderInfo widgetInfo;

    public static PendingHostUpdate updateAppWidget(int i, RemoteViews remoteViews) {
        PendingHostUpdate pendingHostUpdate = new PendingHostUpdate(i, 0);
        pendingHostUpdate.views = remoteViews;
        return pendingHostUpdate;
    }

    public static PendingHostUpdate providerChanged(int i, AppWidgetProviderInfo appWidgetProviderInfo) {
        PendingHostUpdate pendingHostUpdate = new PendingHostUpdate(i, 1);
        pendingHostUpdate.widgetInfo = appWidgetProviderInfo;
        return pendingHostUpdate;
    }

    public static PendingHostUpdate viewDataChanged(int i, int i2) {
        PendingHostUpdate pendingHostUpdate = new PendingHostUpdate(i, 2);
        pendingHostUpdate.viewId = i2;
        return pendingHostUpdate;
    }

    private PendingHostUpdate(int i, int i2) {
        this.appWidgetId = i;
        this.type = i2;
    }

    private PendingHostUpdate(Parcel parcel) {
        this.appWidgetId = parcel.readInt();
        this.type = parcel.readInt();
        switch (this.type) {
            case 0:
                if (parcel.readInt() != 0) {
                    this.views = new RemoteViews(parcel);
                }
                break;
            case 1:
                if (parcel.readInt() != 0) {
                    this.widgetInfo = new AppWidgetProviderInfo(parcel);
                }
                break;
            case 2:
                this.viewId = parcel.readInt();
                break;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.appWidgetId);
        parcel.writeInt(this.type);
        switch (this.type) {
            case 0:
                writeNullParcelable(this.views, parcel, i);
                break;
            case 1:
                writeNullParcelable(this.widgetInfo, parcel, i);
                break;
            case 2:
                parcel.writeInt(this.viewId);
                break;
        }
    }

    private void writeNullParcelable(Parcelable parcelable, Parcel parcel, int i) {
        if (parcelable != null) {
            parcel.writeInt(1);
            parcelable.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
    }
}
