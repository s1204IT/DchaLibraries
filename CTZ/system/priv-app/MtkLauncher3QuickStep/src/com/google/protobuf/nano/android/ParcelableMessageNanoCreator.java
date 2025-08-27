package com.google.protobuf.nano.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

/* loaded from: classes.dex */
public final class ParcelableMessageNanoCreator<T extends MessageNano> implements Parcelable.Creator<T> {
    private static final String TAG = "PMNCreator";
    private final Class<T> mClazz;

    public ParcelableMessageNanoCreator(Class<T> cls) {
        this.mClazz = cls;
    }

    /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
    @Override // android.os.Parcelable.Creator
    public T createFromParcel(Parcel parcel) {
        T t;
        String string = parcel.readString();
        byte[] bArrCreateByteArray = parcel.createByteArray();
        try {
            t = (T) Class.forName(string, false, getClass().getClassLoader()).asSubclass(MessageNano.class).getConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (InvalidProtocolBufferNanoException e) {
            e = e;
            t = null;
        } catch (ClassNotFoundException e2) {
            e = e2;
            t = null;
        } catch (IllegalAccessException e3) {
            e = e3;
            t = null;
        } catch (InstantiationException e4) {
            e = e4;
            t = null;
        } catch (NoSuchMethodException e5) {
            e = e5;
            t = null;
        } catch (InvocationTargetException e6) {
            e = e6;
            t = null;
        }
        try {
            MessageNano.mergeFrom(t, bArrCreateByteArray);
        } catch (InvalidProtocolBufferNanoException e7) {
            e = e7;
            Log.e(TAG, "Exception trying to create proto from parcel", e);
            return t;
        } catch (ClassNotFoundException e8) {
            e = e8;
            Log.e(TAG, "Exception trying to create proto from parcel", e);
            return t;
        } catch (IllegalAccessException e9) {
            e = e9;
            Log.e(TAG, "Exception trying to create proto from parcel", e);
            return t;
        } catch (InstantiationException e10) {
            e = e10;
            Log.e(TAG, "Exception trying to create proto from parcel", e);
            return t;
        } catch (NoSuchMethodException e11) {
            e = e11;
            Log.e(TAG, "Exception trying to create proto from parcel", e);
            return t;
        } catch (InvocationTargetException e12) {
            e = e12;
            Log.e(TAG, "Exception trying to create proto from parcel", e);
            return t;
        }
        return t;
    }

    /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
    @Override // android.os.Parcelable.Creator
    public T[] newArray(int i) {
        return (T[]) ((MessageNano[]) Array.newInstance((Class<?>) this.mClazz, i));
    }

    static <T extends MessageNano> void writeToParcel(Class<T> cls, MessageNano messageNano, Parcel parcel) {
        parcel.writeString(cls.getName());
        parcel.writeByteArray(MessageNano.toByteArray(messageNano));
    }
}
