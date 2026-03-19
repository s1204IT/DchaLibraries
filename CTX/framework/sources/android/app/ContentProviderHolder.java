package android.app;

import android.content.ContentProviderNative;
import android.content.IContentProvider;
import android.content.pm.ProviderInfo;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class ContentProviderHolder implements Parcelable {
    public static final Parcelable.Creator<ContentProviderHolder> CREATOR = new Parcelable.Creator<ContentProviderHolder>() {
        @Override
        public ContentProviderHolder createFromParcel(Parcel parcel) {
            return new ContentProviderHolder(parcel);
        }

        @Override
        public ContentProviderHolder[] newArray(int i) {
            return new ContentProviderHolder[i];
        }
    };
    public IBinder connection;
    public final ProviderInfo info;
    public boolean noReleaseNeeded;
    public IContentProvider provider;

    public ContentProviderHolder(ProviderInfo providerInfo) {
        this.info = providerInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.info.writeToParcel(parcel, 0);
        if (this.provider != null) {
            parcel.writeStrongBinder(this.provider.asBinder());
        } else {
            parcel.writeStrongBinder(null);
        }
        parcel.writeStrongBinder(this.connection);
        parcel.writeInt(this.noReleaseNeeded ? 1 : 0);
    }

    private ContentProviderHolder(Parcel parcel) {
        this.info = ProviderInfo.CREATOR.createFromParcel(parcel);
        this.provider = ContentProviderNative.asInterface(parcel.readStrongBinder());
        this.connection = parcel.readStrongBinder();
        this.noReleaseNeeded = parcel.readInt() != 0;
    }
}
