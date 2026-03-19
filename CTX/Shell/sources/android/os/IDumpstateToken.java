package android.os;

public interface IDumpstateToken extends IInterface {

    public static abstract class Stub extends Binder implements IDumpstateToken {
        public static IDumpstateToken asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("android.os.IDumpstateToken");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IDumpstateToken)) {
                return (IDumpstateToken) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString("android.os.IDumpstateToken");
                return true;
            }
            return super.onTransact(i, parcel, parcel2, i2);
        }

        private static class Proxy implements IDumpstateToken {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }
        }
    }
}
