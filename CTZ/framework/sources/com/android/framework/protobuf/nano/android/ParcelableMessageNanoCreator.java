package com.android.framework.protobuf.nano.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.android.framework.protobuf.nano.InvalidProtocolBufferNanoException;
import com.android.framework.protobuf.nano.MessageNano;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

public final class ParcelableMessageNanoCreator<T extends MessageNano> implements Parcelable.Creator<T> {
    private static final String TAG = "PMNCreator";
    private final Class<T> mClazz;

    public ParcelableMessageNanoCreator(Class<T> cls) {
        this.mClazz = cls;
    }

    @Override
    public T createFromParcel(Parcel parcel) {
        T t;
        String string = parcel.readString();
        byte[] bArrCreateByteArray = parcel.createByteArray();
        try {
            t = (T) Class.forName(string, false, getClass().getClassLoader()).asSubclass(MessageNano.class).getConstructor(new Class[0]).newInstance(new Object[0]);
            try {
                MessageNano.mergeFrom(t, bArrCreateByteArray);
            } catch (InvalidProtocolBufferNanoException e) {
                e = e;
                Log.e(TAG, "Exception trying to create proto from parcel", e);
            } catch (ClassNotFoundException e2) {
                e = e2;
                Log.e(TAG, "Exception trying to create proto from parcel", e);
            } catch (IllegalAccessException e3) {
                e = e3;
                Log.e(TAG, "Exception trying to create proto from parcel", e);
            } catch (InstantiationException e4) {
                e = e4;
                Log.e(TAG, "Exception trying to create proto from parcel", e);
            } catch (NoSuchMethodException e5) {
                e = e5;
                Log.e(TAG, "Exception trying to create proto from parcel", e);
            } catch (InvocationTargetException e6) {
                e = e6;
                Log.e(TAG, "Exception trying to create proto from parcel", e);
            }
        } catch (InvalidProtocolBufferNanoException e7) {
            e = e7;
            t = null;
        } catch (ClassNotFoundException e8) {
            e = e8;
            t = null;
        } catch (IllegalAccessException e9) {
            e = e9;
            t = null;
        } catch (InstantiationException e10) {
            e = e10;
            t = null;
        } catch (NoSuchMethodException e11) {
            e = e11;
            t = null;
        } catch (InvocationTargetException e12) {
            e = e12;
            t = null;
        }
        return t;
    }

    @Override
    public T[] newArray(int i) {
        return (T[]) ((MessageNano[]) Array.newInstance((Class<?>) this.mClazz, i));
    }

    static <T extends MessageNano> void writeToParcel(Class<T> cls, MessageNano messageNano, Parcel parcel) {
        parcel.writeString(cls.getName());
        parcel.writeByteArray(MessageNano.toByteArray(messageNano));
    }
}
