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

        public static IEffectHalClient asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IEffectHalClient)) {
                return (IEffectHalClient) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            BaseParameters baseParametersCreateFromParcel;
            BaseParameters baseParametersCreateFromParcel2;
            BaseParameters baseParametersCreateFromParcel3;
            BaseParameters baseParametersCreateFromParcel4;
            BaseParameters baseParametersCreateFromParcel5;
            BaseParameters baseParametersCreateFromParcel6;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _result = init();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _result2 = uninit();
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _result3 = configure();
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _result4 = unconfigure();
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    long _result5 = start();
                    reply.writeNoException();
                    reply.writeLong(_result5);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel6 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel6 = null;
                    }
                    int _result6 = abort(baseParametersCreateFromParcel6);
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    EffectHalVersion _arg0 = new EffectHalVersion();
                    int _result7 = getNameVersion(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    if (_arg0 != null) {
                        reply.writeInt(1);
                        _arg0.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    IEffectListener _arg02 = IEffectListener.Stub.asInterface(data.readStrongBinder());
                    int _result8 = setEffectListener(_arg02);
                    reply.writeNoException();
                    reply.writeInt(_result8);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    String _arg1 = data.readString();
                    int _result9 = setParameter(_arg03, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result9);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel5 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel5 = null;
                    }
                    int _result10 = setParameters(baseParametersCreateFromParcel5);
                    reply.writeNoException();
                    reply.writeInt(_result10);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel4 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel4 = null;
                    }
                    ArrayList arrayList = new ArrayList();
                    int _result11 = getCaptureRequirement(baseParametersCreateFromParcel4, arrayList);
                    reply.writeNoException();
                    reply.writeInt(_result11);
                    reply.writeTypedList(arrayList);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _result12 = prepare();
                    reply.writeNoException();
                    reply.writeInt(_result12);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _result13 = release();
                    reply.writeNoException();
                    reply.writeInt(_result13);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    ArrayList arrayList2 = new ArrayList();
                    int _result14 = getInputSurfaces(arrayList2);
                    reply.writeNoException();
                    reply.writeInt(_result14);
                    reply.writeTypedList(arrayList2);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    List<Surface> _arg04 = data.createTypedArrayList(Surface.CREATOR);
                    List<BaseParameters> _arg12 = data.createTypedArrayList(BaseParameters.CREATOR);
                    int _result15 = setOutputSurfaces(_arg04, _arg12);
                    reply.writeNoException();
                    reply.writeInt(_result15);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel3 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel3 = null;
                    }
                    long _arg2 = data.readLong();
                    boolean _arg3 = data.readInt() != 0;
                    int _result16 = addInputParameter(_arg05, baseParametersCreateFromParcel3, _arg2, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result16);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel2 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel2 = null;
                    }
                    long _arg22 = data.readLong();
                    boolean _arg32 = data.readInt() != 0;
                    int _result17 = addOutputParameter(_arg06, baseParametersCreateFromParcel2, _arg22, _arg32);
                    reply.writeNoException();
                    reply.writeInt(_result17);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    boolean _arg13 = data.readInt() != 0;
                    int _result18 = setInputsyncMode(_arg07, _arg13);
                    reply.writeNoException();
                    reply.writeInt(_result18);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    boolean _result19 = getInputsyncMode(_arg08);
                    reply.writeNoException();
                    reply.writeInt(_result19 ? 1 : 0);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    boolean _arg14 = data.readInt() != 0;
                    int _result20 = setOutputsyncMode(_arg09, _arg14);
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    boolean _result21 = getOutputsyncMode(_arg010);
                    reply.writeNoException();
                    reply.writeInt(_result21 ? 1 : 0);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel = null;
                    }
                    int _result22 = setBaseParameter(baseParametersCreateFromParcel);
                    reply.writeNoException();
                    reply.writeInt(_result22);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg011 = data.readLong();
                    int _result23 = dequeueAndQueueBuf(_arg011);
                    reply.writeNoException();
                    reply.writeInt(_result23);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IEffectHalClient {
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
            public int init() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int uninit() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int configure() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int unconfigure() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long start() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int abort(BaseParameters effectParameter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (effectParameter != null) {
                        _data.writeInt(1);
                        effectParameter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getNameVersion(EffectHalVersion version) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        version.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setEffectListener(IEffectListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setParameter(String key, String paramValue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeString(paramValue);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setParameters(BaseParameters parameter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (parameter != null) {
                        _data.writeInt(1);
                        parameter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getCaptureRequirement(BaseParameters effectParameter, List<BaseParameters> requirement) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (effectParameter != null) {
                        _data.writeInt(1);
                        effectParameter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readTypedList(requirement, BaseParameters.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int prepare() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int release() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getInputSurfaces(List<Surface> input) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readTypedList(input, Surface.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setOutputSurfaces(List<Surface> output, List<BaseParameters> parameters) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(output);
                    _data.writeTypedList(parameters);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int addInputParameter(int index, BaseParameters parameter, long timestamp, boolean repeat) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    if (parameter != null) {
                        _data.writeInt(1);
                        parameter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(timestamp);
                    _data.writeInt(repeat ? 1 : 0);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int addOutputParameter(int index, BaseParameters parameter, long timestamp, boolean repeat) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    if (parameter != null) {
                        _data.writeInt(1);
                        parameter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(timestamp);
                    _data.writeInt(repeat ? 1 : 0);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setInputsyncMode(int index, boolean sync) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    _data.writeInt(sync ? 1 : 0);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getInputsyncMode(int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setOutputsyncMode(int index, boolean sync) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    _data.writeInt(sync ? 1 : 0);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getOutputsyncMode(int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setBaseParameter(BaseParameters parameters) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (parameters != null) {
                        _data.writeInt(1);
                        parameters.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int dequeueAndQueueBuf(long timestamp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(timestamp);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
