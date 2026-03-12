package com.android.settings.intelligence.search;

import android.os.Parcel;
import android.os.Parcelable;

public class ResultPayloadUtils {
    public static byte[] marshall(ResultPayload resultPayload) {
        Parcel parcelObtain = Parcel.obtain();
        resultPayload.writeToParcel(parcelObtain, 0);
        byte[] bArrMarshall = parcelObtain.marshall();
        parcelObtain.recycle();
        return bArrMarshall;
    }

    public static <T> T unmarshall(byte[] bArr, Parcelable.Creator<T> creator) {
        Parcel parcelUnmarshall = unmarshall(bArr);
        T tCreateFromParcel = creator.createFromParcel(parcelUnmarshall);
        parcelUnmarshall.recycle();
        return tCreateFromParcel;
    }

    private static Parcel unmarshall(byte[] bArr) {
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.unmarshall(bArr, 0, bArr.length);
        parcelObtain.setDataPosition(0);
        return parcelObtain;
    }
}
