package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class ParceledListSlice<T extends Parcelable> implements Parcelable {
    private static final int MAX_FIRST_IPC_SIZE = 131072;
    private static final int MAX_IPC_SIZE = 262144;
    private final List<T> mList;
    private static String TAG = "ParceledListSlice";
    private static boolean DEBUG = false;
    public static final Parcelable.ClassLoaderCreator<ParceledListSlice> CREATOR = new Parcelable.ClassLoaderCreator<ParceledListSlice>() {
        @Override
        public ParceledListSlice createFromParcel(Parcel parcel) {
            return new ParceledListSlice(parcel, null);
        }

        @Override
        public ParceledListSlice createFromParcel(Parcel in, ClassLoader loader) {
            return new ParceledListSlice(in, loader);
        }

        @Override
        public ParceledListSlice[] newArray(int size) {
            return new ParceledListSlice[size];
        }
    };

    public ParceledListSlice(List<T> list) {
        this.mList = list;
    }

    private ParceledListSlice(Parcel parcel, ClassLoader classLoader) {
        int i = parcel.readInt();
        this.mList = new ArrayList(i);
        if (DEBUG) {
            Log.d(TAG, "Retrieving " + i + " items");
        }
        if (i > 0) {
            Parcelable.Creator<T> parcelableCreator = parcel.readParcelableCreator(classLoader);
            Class<?> cls = null;
            int i2 = 0;
            while (i2 < i && parcel.readInt() != 0) {
                Parcelable creator = parcel.readCreator(parcelableCreator, classLoader);
                if (cls == null) {
                    cls = creator.getClass();
                } else {
                    verifySameType(cls, creator.getClass());
                }
                this.mList.add((T) creator);
                if (DEBUG) {
                    Log.d(TAG, "Read inline #" + i2 + ": " + this.mList.get(this.mList.size() - 1));
                }
                i2++;
            }
            if (i2 < i) {
                IBinder strongBinder = parcel.readStrongBinder();
                while (i2 < i) {
                    if (DEBUG) {
                        Log.d(TAG, "Reading more @" + i2 + " of " + i + ": retriever=" + strongBinder);
                    }
                    Parcel parcelObtain = Parcel.obtain();
                    Parcel parcelObtain2 = Parcel.obtain();
                    parcelObtain.writeInt(i2);
                    try {
                        strongBinder.transact(1, parcelObtain, parcelObtain2, 0);
                        while (i2 < i && parcelObtain2.readInt() != 0) {
                            Parcelable creator2 = parcelObtain2.readCreator(parcelableCreator, classLoader);
                            verifySameType(cls, creator2.getClass());
                            this.mList.add((T) creator2);
                            if (DEBUG) {
                                Log.d(TAG, "Read extra #" + i2 + ": " + this.mList.get(this.mList.size() - 1));
                            }
                            i2++;
                        }
                        parcelObtain2.recycle();
                        parcelObtain.recycle();
                    } catch (RemoteException e) {
                        Log.w(TAG, "Failure retrieving array; only received " + i2 + " of " + i, e);
                        return;
                    }
                }
            }
        }
    }

    private static void verifySameType(Class<?> expected, Class<?> actual) {
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("Can't unparcel type " + actual.getName() + " in list of type " + expected.getName());
        }
    }

    public List<T> getList() {
        return this.mList;
    }

    @Override
    public int describeContents() {
        int contents = 0;
        for (int i = 0; i < this.mList.size(); i++) {
            contents |= this.mList.get(i).describeContents();
        }
        return contents;
    }

    @Override
    public void writeToParcel(Parcel dest, final int flags) {
        final int N = this.mList.size();
        dest.writeInt(N);
        if (DEBUG) {
            Log.d(TAG, "Writing " + N + " items");
        }
        if (N > 0) {
            final Class<?> listElementClass = this.mList.get(0).getClass();
            dest.writeParcelableCreator(this.mList.get(0));
            int i = 0;
            while (i < N && dest.dataSize() < 131072) {
                dest.writeInt(1);
                T parcelable = this.mList.get(i);
                verifySameType(listElementClass, parcelable.getClass());
                parcelable.writeToParcel(dest, flags);
                if (DEBUG) {
                    Log.d(TAG, "Wrote inline #" + i + ": " + this.mList.get(i));
                }
                i++;
            }
            if (i < N) {
                dest.writeInt(0);
                Binder retriever = new Binder() {
                    @Override
                    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags2) throws RemoteException {
                        if (code != 1) {
                            return super.onTransact(code, data, reply, flags2);
                        }
                        int i2 = data.readInt();
                        if (ParceledListSlice.DEBUG) {
                            Log.d(ParceledListSlice.TAG, "Writing more @" + i2 + " of " + N);
                        }
                        while (i2 < N && reply.dataSize() < 262144) {
                            reply.writeInt(1);
                            Parcelable parcelable2 = (Parcelable) ParceledListSlice.this.mList.get(i2);
                            ParceledListSlice.verifySameType(listElementClass, parcelable2.getClass());
                            parcelable2.writeToParcel(reply, flags);
                            if (ParceledListSlice.DEBUG) {
                                Log.d(ParceledListSlice.TAG, "Wrote extra #" + i2 + ": " + ParceledListSlice.this.mList.get(i2));
                            }
                            i2++;
                        }
                        if (i2 >= N) {
                            return true;
                        }
                        if (ParceledListSlice.DEBUG) {
                            Log.d(ParceledListSlice.TAG, "Breaking @" + i2 + " of " + N);
                        }
                        reply.writeInt(0);
                        return true;
                    }
                };
                if (DEBUG) {
                    Log.d(TAG, "Breaking @" + i + " of " + N + ": retriever=" + retriever);
                }
                dest.writeStrongBinder(retriever);
            }
        }
    }
}
