package android.nfc;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

public final class BeamShareData implements Parcelable {
    public static final Parcelable.Creator<BeamShareData> CREATOR = new Parcelable.Creator<BeamShareData>() {
        @Override
        public BeamShareData createFromParcel(Parcel parcel) {
            Uri[] uriArr;
            NdefMessage ndefMessage = (NdefMessage) parcel.readParcelable(NdefMessage.class.getClassLoader());
            int i = parcel.readInt();
            if (i > 0) {
                uriArr = new Uri[i];
                parcel.readTypedArray(uriArr, Uri.CREATOR);
            } else {
                uriArr = null;
            }
            return new BeamShareData(ndefMessage, uriArr, (UserHandle) parcel.readParcelable(UserHandle.class.getClassLoader()), parcel.readInt());
        }

        @Override
        public BeamShareData[] newArray(int i) {
            return new BeamShareData[i];
        }
    };
    public final int flags;
    public final NdefMessage ndefMessage;
    public final Uri[] uris;
    public final UserHandle userHandle;

    public BeamShareData(NdefMessage ndefMessage, Uri[] uriArr, UserHandle userHandle, int i) {
        this.ndefMessage = ndefMessage;
        this.uris = uriArr;
        this.userHandle = userHandle;
        this.flags = i;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        int length = this.uris != null ? this.uris.length : 0;
        parcel.writeParcelable(this.ndefMessage, 0);
        parcel.writeInt(length);
        if (length > 0) {
            parcel.writeTypedArray(this.uris, 0);
        }
        parcel.writeParcelable(this.userHandle, 0);
        parcel.writeInt(this.flags);
    }
}
