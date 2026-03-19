package com.mediatek.mmsdk;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Surface;
import com.mediatek.mmsdk.IEffectListener;
import java.util.ArrayList;
import java.util.List;

public interface IEffectHalClient extends IInterface {
    int abort(BaseParameters baseParameters) throws RemoteException;

    int addInputParameter(int i, BaseParameters baseParameters, long j, boolean z) throws RemoteException;

    int addOutputParameter(int i, BaseParameters baseParameters, long j, boolean z) throws RemoteException;

    int configure() throws RemoteException;

    int dequeueAndQueueBuf(long j) throws RemoteException;

    int getCaptureRequirement(BaseParameters baseParameters, List<BaseParameters> list) throws RemoteException;

    int getInputSurfaces(List<Surface> list) throws RemoteException;

    boolean getInputsyncMode(int i) throws RemoteException;

    int getNameVersion(EffectHalVersion effectHalVersion) throws RemoteException;

    boolean getOutputsyncMode(int i) throws RemoteException;

    int init() throws RemoteException;

    int prepare() throws RemoteException;

    int release() throws RemoteException;

    int setBaseParameter(BaseParameters baseParameters) throws RemoteException;

    int setEffectListener(IEffectListener iEffectListener) throws RemoteException;

    int setInputsyncMode(int i, boolean z) throws RemoteException;

    int setOutputSurfaces(List<Surface> list, List<BaseParameters> list2) throws RemoteException;

    int setOutputsyncMode(int i, boolean z) throws RemoteException;

    int setParameter(String str, String str2) throws RemoteException;

    int setParameters(BaseParameters baseParameters) throws RemoteException;

    long start() throws RemoteException;

    int unconfigure() throws RemoteException;

    int uninit() throws RemoteException;

    public static abstract class Stub extends Binder implements IEffectHalClient {
        private static final String DESCRIPTOR = "com.mediatek.mmsdk.IEffectHalClient";
        static final int TRANSACTION_abort = 6;
        static final int TRANSACTION_addInputParameter = 16;
        static final int TRANSACTION_addOutputParameter = 17;
        static final int TRANSACTION_configure = 3;
        static final int TRANSACTION_dequeueAndQueueBuf = 23;
        static final int TRANSACTION_getCaptureRequirement = 11;
        static final int TRANSACTION_getInputSurfaces = 14;
        static final int TRANSACTION_getInputsyncMode = 19;
        static final int TRANSACTION_getNameVersion = 7;
        static final int TRANSACTION_getOutputsyncMode = 21;
        static final int TRANSACTION_init = 1;
        static final int TRANSACTION_prepare = 12;
        static final int TRANSACTION_release = 13;
        static final int TRANSACTION_setBaseParameter = 22;
        static final int TRANSACTION_setEffectListener = 8;
        static final int TRANSACTION_setInputsyncMode = 18;
        static final int TRANSACTION_setOutputSurfaces = 15;
        static final int TRANSACTION_setOutputsyncMode = 20;
        static final int TRANSACTION_setParameter = 9;
        static final int TRANSACTION_setParameters = 10;
        static final int TRANSACTION_start = 5;
        static final int TRANSACTION_unconfigure = 4;
        static final int TRANSACTION_uninit = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IEffectHalClient asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IEffectHalClient)) {
                return (IEffectHalClient) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iInit = init();
                    parcel2.writeNoException();
                    parcel2.writeInt(iInit);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iUninit = uninit();
                    parcel2.writeNoException();
                    parcel2.writeInt(iUninit);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iConfigure = configure();
                    parcel2.writeNoException();
                    parcel2.writeInt(iConfigure);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iUnconfigure = unconfigure();
                    parcel2.writeNoException();
                    parcel2.writeInt(iUnconfigure);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    long jStart = start();
                    parcel2.writeNoException();
                    parcel2.writeLong(jStart);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAbort = abort(parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iAbort);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    EffectHalVersion effectHalVersion = new EffectHalVersion();
                    int nameVersion = getNameVersion(effectHalVersion);
                    parcel2.writeNoException();
                    parcel2.writeInt(nameVersion);
                    parcel2.writeInt(1);
                    effectHalVersion.writeToParcel(parcel2, 1);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    int effectListener = setEffectListener(IEffectListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(effectListener);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    int parameter = setParameter(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(parameter);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    int parameters = setParameters(parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(parameters);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    BaseParameters baseParametersCreateFromParcel = parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null;
                    ArrayList arrayList = new ArrayList();
                    int captureRequirement = getCaptureRequirement(baseParametersCreateFromParcel, arrayList);
                    parcel2.writeNoException();
                    parcel2.writeInt(captureRequirement);
                    parcel2.writeTypedList(arrayList);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iPrepare = prepare();
                    parcel2.writeNoException();
                    parcel2.writeInt(iPrepare);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iRelease = release();
                    parcel2.writeNoException();
                    parcel2.writeInt(iRelease);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    ArrayList arrayList2 = new ArrayList();
                    int inputSurfaces = getInputSurfaces(arrayList2);
                    parcel2.writeNoException();
                    parcel2.writeInt(inputSurfaces);
                    parcel2.writeTypedList(arrayList2);
                    return true;
                case TRANSACTION_setOutputSurfaces:
                    parcel.enforceInterface(DESCRIPTOR);
                    int outputSurfaces = setOutputSurfaces(parcel.createTypedArrayList(Surface.CREATOR), parcel.createTypedArrayList(BaseParameters.CREATOR));
                    parcel2.writeNoException();
                    parcel2.writeInt(outputSurfaces);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAddInputParameter = addInputParameter(parcel.readInt(), parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null, parcel.readLong(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddInputParameter);
                    return true;
                case TRANSACTION_addOutputParameter:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAddOutputParameter = addOutputParameter(parcel.readInt(), parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null, parcel.readLong(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddOutputParameter);
                    return true;
                case TRANSACTION_setInputsyncMode:
                    parcel.enforceInterface(DESCRIPTOR);
                    int inputsyncMode = setInputsyncMode(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(inputsyncMode);
                    return true;
                case TRANSACTION_getInputsyncMode:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean inputsyncMode2 = getInputsyncMode(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(inputsyncMode2 ? 1 : 0);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    int outputsyncMode = setOutputsyncMode(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(outputsyncMode);
                    return true;
                case TRANSACTION_getOutputsyncMode:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean outputsyncMode2 = getOutputsyncMode(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(outputsyncMode2 ? 1 : 0);
                    return true;
                case TRANSACTION_setBaseParameter:
                    parcel.enforceInterface(DESCRIPTOR);
                    int baseParameter = setBaseParameter(parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(baseParameter);
                    return true;
                case TRANSACTION_dequeueAndQueueBuf:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iDequeueAndQueueBuf = dequeueAndQueueBuf(parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(iDequeueAndQueueBuf);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IEffectHalClient {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public int init() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int uninit() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int configure() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int unconfigure() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long start() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int abort(BaseParameters baseParameters) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getNameVersion(EffectHalVersion effectHalVersion) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    if (parcelObtain2.readInt() != 0) {
                        effectHalVersion.readFromParcel(parcelObtain2);
                    }
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setEffectListener(IEffectListener iEffectListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iEffectListener != null ? iEffectListener.asBinder() : null);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setParameter(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setParameters(BaseParameters baseParameters) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getCaptureRequirement(BaseParameters baseParameters, List<BaseParameters> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    parcelObtain2.readTypedList(list, BaseParameters.CREATOR);
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int prepare() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int release() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getInputSurfaces(List<Surface> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    parcelObtain2.readTypedList(list, Surface.CREATOR);
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setOutputSurfaces(List<Surface> list, List<BaseParameters> list2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeTypedList(list);
                    parcelObtain.writeTypedList(list2);
                    this.mRemote.transact(Stub.TRANSACTION_setOutputSurfaces, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addInputParameter(int i, BaseParameters baseParameters, long j, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addOutputParameter(int i, BaseParameters baseParameters, long j, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(Stub.TRANSACTION_addOutputParameter, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setInputsyncMode(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(Stub.TRANSACTION_setInputsyncMode, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean getInputsyncMode(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_getInputsyncMode, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setOutputsyncMode(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean getOutputsyncMode(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_getOutputsyncMode, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setBaseParameter(BaseParameters baseParameters) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_setBaseParameter, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int dequeueAndQueueBuf(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(Stub.TRANSACTION_dequeueAndQueueBuf, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
