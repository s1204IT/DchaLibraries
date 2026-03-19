package android.printservice.recommendation;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IRecommendationServiceCallbacks extends IInterface {
    void onRecommendationsUpdated(List<RecommendationInfo> list) throws RemoteException;

    public static abstract class Stub extends Binder implements IRecommendationServiceCallbacks {
        private static final String DESCRIPTOR = "android.printservice.recommendation.IRecommendationServiceCallbacks";
        static final int TRANSACTION_onRecommendationsUpdated = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRecommendationServiceCallbacks asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IRecommendationServiceCallbacks)) {
                return (IRecommendationServiceCallbacks) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    List<RecommendationInfo> _arg0 = data.createTypedArrayList(RecommendationInfo.CREATOR);
                    onRecommendationsUpdated(_arg0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IRecommendationServiceCallbacks {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void onRecommendationsUpdated(List<RecommendationInfo> recommendations) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(recommendations);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
