package android.service.quicksettings;

import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

public final class Tile implements Parcelable {
    public static final Parcelable.Creator<Tile> CREATOR = new Parcelable.Creator<Tile>() {
        @Override
        public Tile createFromParcel(Parcel parcel) {
            return new Tile(parcel);
        }

        @Override
        public Tile[] newArray(int i) {
            return new Tile[i];
        }
    };
    public static final int STATE_ACTIVE = 2;
    public static final int STATE_INACTIVE = 1;
    public static final int STATE_UNAVAILABLE = 0;
    private static final String TAG = "Tile";
    private CharSequence mContentDescription;
    private Icon mIcon;
    private CharSequence mLabel;
    private IQSService mService;
    private int mState = 2;
    private IBinder mToken;

    public Tile(Parcel parcel) {
        readFromParcel(parcel);
    }

    public Tile() {
    }

    public void setService(IQSService iQSService, IBinder iBinder) {
        this.mService = iQSService;
        this.mToken = iBinder;
    }

    public int getState() {
        return this.mState;
    }

    public void setState(int i) {
        this.mState = i;
    }

    public Icon getIcon() {
        return this.mIcon;
    }

    public void setIcon(Icon icon) {
        this.mIcon = icon;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public void setLabel(CharSequence charSequence) {
        this.mLabel = charSequence;
    }

    public CharSequence getContentDescription() {
        return this.mContentDescription;
    }

    public void setContentDescription(CharSequence charSequence) {
        this.mContentDescription = charSequence;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void updateTile() {
        try {
            this.mService.updateQsTile(this, this.mToken);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't update tile");
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.mIcon != null) {
            parcel.writeByte((byte) 1);
            this.mIcon.writeToParcel(parcel, i);
        } else {
            parcel.writeByte((byte) 0);
        }
        parcel.writeInt(this.mState);
        TextUtils.writeToParcel(this.mLabel, parcel, i);
        TextUtils.writeToParcel(this.mContentDescription, parcel, i);
    }

    private void readFromParcel(Parcel parcel) {
        if (parcel.readByte() != 0) {
            this.mIcon = Icon.CREATOR.createFromParcel(parcel);
        } else {
            this.mIcon = null;
        }
        this.mState = parcel.readInt();
        this.mLabel = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.mContentDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
    }
}
