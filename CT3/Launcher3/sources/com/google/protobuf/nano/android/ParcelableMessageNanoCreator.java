package com.google.protobuf.nano.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;
import java.lang.reflect.Array;

public final class ParcelableMessageNanoCreator<T extends MessageNano> implements Parcelable.Creator<T> {
    private final Class<T> mClazz;

    @Override
    public T createFromParcel(Parcel parcel) {
        String string = parcel.readString();
        byte[] bArrCreateByteArray = parcel.createByteArray();
        T t = null;
        try {
            t = (T) ((MessageNano) Class.forName(string).newInstance());
            MessageNano.mergeFrom(t, bArrCreateByteArray);
            return t;
        } catch (InvalidProtocolBufferNanoException e) {
            Log.e("PMNCreator", "Exception trying to create proto from parcel", e);
            return t;
        } catch (ClassNotFoundException e2) {
            Log.e("PMNCreator", "Exception trying to create proto from parcel", e2);
            return t;
        } catch (IllegalAccessException e3) {
            Log.e("PMNCreator", "Exception trying to create proto from parcel", e3);
            return t;
        } catch (InstantiationException e4) {
            Log.e("PMNCreator", "Exception trying to create proto from parcel", e4);
            return t;
        }
    }

    @Override
    public T[] newArray(int i) {
        return (T[]) ((MessageNano[]) Array.newInstance((Class<?>) this.mClazz, i));
    }

    static <T extends MessageNano> void writeToParcel(Class<T> clazz, MessageNano message, Parcel out) {
        out.writeString(clazz.getName());
        out.writeByteArray(MessageNano.toByteArray(message));
    }
}
