package com.android.contacts.editor;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.dataitem.DataKind;

public final class ViewIdGenerator implements Parcelable {
    private Bundle mIdMap = new Bundle();
    private int mNextId = 1;
    private static final StringBuilder sWorkStringBuilder = new StringBuilder();
    public static final Parcelable.Creator<ViewIdGenerator> CREATOR = new Parcelable.Creator<ViewIdGenerator>() {
        @Override
        public ViewIdGenerator createFromParcel(Parcel parcel) {
            ViewIdGenerator viewIdGenerator = new ViewIdGenerator();
            viewIdGenerator.readFromParcel(parcel);
            return viewIdGenerator;
        }

        @Override
        public ViewIdGenerator[] newArray(int i) {
            return new ViewIdGenerator[i];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public int getId(RawContactDelta rawContactDelta, DataKind dataKind, ValuesDelta valuesDelta, int i) {
        String mapKey = getMapKey(rawContactDelta, dataKind, valuesDelta, i);
        int i2 = this.mIdMap.getInt(mapKey, 0);
        if (i2 == 0) {
            int i3 = this.mNextId;
            this.mNextId = i3 + 1;
            int i4 = i3 & 65535;
            this.mIdMap.putInt(mapKey, i4);
            return i4;
        }
        return i2;
    }

    private static String getMapKey(RawContactDelta rawContactDelta, DataKind dataKind, ValuesDelta valuesDelta, int i) {
        sWorkStringBuilder.setLength(0);
        if (rawContactDelta != null) {
            sWorkStringBuilder.append(rawContactDelta.getValues().getId());
            if (dataKind != null) {
                sWorkStringBuilder.append('*');
                sWorkStringBuilder.append(dataKind.mimeType);
                if (valuesDelta != null) {
                    sWorkStringBuilder.append('*');
                    sWorkStringBuilder.append(valuesDelta.getId());
                    if (i != -1) {
                        sWorkStringBuilder.append('*');
                        sWorkStringBuilder.append(i);
                    }
                }
            }
        }
        return sWorkStringBuilder.toString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mNextId);
        parcel.writeBundle(this.mIdMap);
    }

    private void readFromParcel(Parcel parcel) {
        this.mNextId = parcel.readInt();
        this.mIdMap = parcel.readBundle();
    }
}
