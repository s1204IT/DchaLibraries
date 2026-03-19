package com.mediatek.common.dm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface DmAgent extends IInterface {
    boolean clearLockFlag() throws RemoteException;

    int clearOtaResult() throws RemoteException;

    boolean clearRebootFlag() throws RemoteException;

    boolean clearWipeFlag() throws RemoteException;

    byte[] getDmSwitchValue() throws RemoteException;

    int getLockType() throws RemoteException;

    byte[] getMacAddr() throws RemoteException;

    int getOperatorId() throws RemoteException;

    byte[] getOperatorName() throws RemoteException;

    byte[] getReceivedMeid() throws RemoteException;

    byte[] getReceivedPreconditionFlag() throws RemoteException;

    byte[] getRegisterSwitch() throws RemoteException;

    byte[] getSmsRegSwitchValue() throws RemoteException;

    byte[] getSwitchValue() throws RemoteException;

    int getUpgradeStatus() throws RemoteException;

    boolean isBootRecoveryFlag() throws RemoteException;

    boolean isHangMoCallLocking() throws RemoteException;

    boolean isHangMtCallLocking() throws RemoteException;

    boolean isLockFlagSet() throws RemoteException;

    boolean isWipeSet() throws RemoteException;

    byte[] readDmTree() throws RemoteException;

    byte[] readIccID1() throws RemoteException;

    byte[] readIccID2() throws RemoteException;

    byte[] readImsi() throws RemoteException;

    byte[] readImsi1() throws RemoteException;

    byte[] readImsi2() throws RemoteException;

    byte[] readOperatorName() throws RemoteException;

    int readOtaResult() throws RemoteException;

    byte[] readRegisterFlag() throws RemoteException;

    byte[] readSelfRegisterFlag() throws RemoteException;

    int restartAndroid() throws RemoteException;

    boolean setDmSwitchValue(byte[] bArr) throws RemoteException;

    boolean setLockFlag(byte[] bArr) throws RemoteException;

    boolean setRebootFlag() throws RemoteException;

    boolean setReceivedMeid(byte[] bArr, int i) throws RemoteException;

    boolean setReceivedPreconditionFlag(byte[] bArr, int i) throws RemoteException;

    boolean setRegisterFlag(byte[] bArr, int i) throws RemoteException;

    boolean setRegisterSwitch(byte[] bArr) throws RemoteException;

    boolean setSelfRegisterFlag(byte[] bArr, int i) throws RemoteException;

    boolean setSmsRegSwitchValue(byte[] bArr) throws RemoteException;

    boolean setSwitchValue(byte[] bArr) throws RemoteException;

    boolean setWipeFlag() throws RemoteException;

    boolean writeDmTree(byte[] bArr) throws RemoteException;

    boolean writeIccID1(byte[] bArr, int i) throws RemoteException;

    boolean writeIccID2(byte[] bArr, int i) throws RemoteException;

    boolean writeImsi(byte[] bArr) throws RemoteException;

    boolean writeImsi1(byte[] bArr, int i) throws RemoteException;

    boolean writeImsi2(byte[] bArr, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements DmAgent {
        private static final String DESCRIPTOR = "com.mediatek.common.dm.DmAgent";
        static final int TRANSACTION_clearLockFlag = 5;
        static final int TRANSACTION_clearOtaResult = 25;
        static final int TRANSACTION_clearRebootFlag = 17;
        static final int TRANSACTION_clearWipeFlag = 23;
        static final int TRANSACTION_getDmSwitchValue = 28;
        static final int TRANSACTION_getLockType = 12;
        static final int TRANSACTION_getMacAddr = 38;
        static final int TRANSACTION_getOperatorId = 13;
        static final int TRANSACTION_getOperatorName = 14;
        static final int TRANSACTION_getReceivedMeid = 47;
        static final int TRANSACTION_getReceivedPreconditionFlag = 45;
        static final int TRANSACTION_getRegisterSwitch = 9;
        static final int TRANSACTION_getSmsRegSwitchValue = 30;
        static final int TRANSACTION_getSwitchValue = 26;
        static final int TRANSACTION_getUpgradeStatus = 19;
        static final int TRANSACTION_isBootRecoveryFlag = 18;
        static final int TRANSACTION_isHangMoCallLocking = 15;
        static final int TRANSACTION_isHangMtCallLocking = 16;
        static final int TRANSACTION_isLockFlagSet = 3;
        static final int TRANSACTION_isWipeSet = 21;
        static final int TRANSACTION_readDmTree = 1;
        static final int TRANSACTION_readIccID1 = 41;
        static final int TRANSACTION_readIccID2 = 42;
        static final int TRANSACTION_readImsi = 6;
        static final int TRANSACTION_readImsi1 = 36;
        static final int TRANSACTION_readImsi2 = 37;
        static final int TRANSACTION_readOperatorName = 8;
        static final int TRANSACTION_readOtaResult = 24;
        static final int TRANSACTION_readRegisterFlag = 33;
        static final int TRANSACTION_readSelfRegisterFlag = 44;
        static final int TRANSACTION_restartAndroid = 20;
        static final int TRANSACTION_setDmSwitchValue = 29;
        static final int TRANSACTION_setLockFlag = 4;
        static final int TRANSACTION_setRebootFlag = 11;
        static final int TRANSACTION_setReceivedMeid = 48;
        static final int TRANSACTION_setReceivedPreconditionFlag = 46;
        static final int TRANSACTION_setRegisterFlag = 32;
        static final int TRANSACTION_setRegisterSwitch = 10;
        static final int TRANSACTION_setSelfRegisterFlag = 43;
        static final int TRANSACTION_setSmsRegSwitchValue = 31;
        static final int TRANSACTION_setSwitchValue = 27;
        static final int TRANSACTION_setWipeFlag = 22;
        static final int TRANSACTION_writeDmTree = 2;
        static final int TRANSACTION_writeIccID1 = 39;
        static final int TRANSACTION_writeIccID2 = 40;
        static final int TRANSACTION_writeImsi = 7;
        static final int TRANSACTION_writeImsi1 = 34;
        static final int TRANSACTION_writeImsi2 = 35;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static DmAgent asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof DmAgent)) {
                return (DmAgent) iin;
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
                    byte[] _result = readDmTree();
                    reply.writeNoException();
                    reply.writeByteArray(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg0 = data.createByteArray();
                    boolean _result2 = writeDmTree(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result3 = isLockFlagSet();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg02 = data.createByteArray();
                    boolean _result4 = setLockFlag(_arg02);
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result5 = clearLockFlag();
                    reply.writeNoException();
                    reply.writeInt(_result5 ? 1 : 0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result6 = readImsi();
                    reply.writeNoException();
                    reply.writeByteArray(_result6);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg03 = data.createByteArray();
                    boolean _result7 = writeImsi(_arg03);
                    reply.writeNoException();
                    reply.writeInt(_result7 ? 1 : 0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result8 = readOperatorName();
                    reply.writeNoException();
                    reply.writeByteArray(_result8);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result9 = getRegisterSwitch();
                    reply.writeNoException();
                    reply.writeByteArray(_result9);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg04 = data.createByteArray();
                    boolean _result10 = setRegisterSwitch(_arg04);
                    reply.writeNoException();
                    reply.writeInt(_result10 ? 1 : 0);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result11 = setRebootFlag();
                    reply.writeNoException();
                    reply.writeInt(_result11 ? 1 : 0);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _result12 = getLockType();
                    reply.writeNoException();
                    reply.writeInt(_result12);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _result13 = getOperatorId();
                    reply.writeNoException();
                    reply.writeInt(_result13);
                    return true;
                case TRANSACTION_getOperatorName:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result14 = getOperatorName();
                    reply.writeNoException();
                    reply.writeByteArray(_result14);
                    return true;
                case TRANSACTION_isHangMoCallLocking:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result15 = isHangMoCallLocking();
                    reply.writeNoException();
                    reply.writeInt(_result15 ? 1 : 0);
                    return true;
                case TRANSACTION_isHangMtCallLocking:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result16 = isHangMtCallLocking();
                    reply.writeNoException();
                    reply.writeInt(_result16 ? 1 : 0);
                    return true;
                case TRANSACTION_clearRebootFlag:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result17 = clearRebootFlag();
                    reply.writeNoException();
                    reply.writeInt(_result17 ? 1 : 0);
                    return true;
                case TRANSACTION_isBootRecoveryFlag:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result18 = isBootRecoveryFlag();
                    reply.writeNoException();
                    reply.writeInt(_result18 ? 1 : 0);
                    return true;
                case TRANSACTION_getUpgradeStatus:
                    data.enforceInterface(DESCRIPTOR);
                    int _result19 = getUpgradeStatus();
                    reply.writeNoException();
                    reply.writeInt(_result19);
                    return true;
                case TRANSACTION_restartAndroid:
                    data.enforceInterface(DESCRIPTOR);
                    int _result20 = restartAndroid();
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case TRANSACTION_isWipeSet:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result21 = isWipeSet();
                    reply.writeNoException();
                    reply.writeInt(_result21 ? 1 : 0);
                    return true;
                case TRANSACTION_setWipeFlag:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result22 = setWipeFlag();
                    reply.writeNoException();
                    reply.writeInt(_result22 ? 1 : 0);
                    return true;
                case TRANSACTION_clearWipeFlag:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result23 = clearWipeFlag();
                    reply.writeNoException();
                    reply.writeInt(_result23 ? 1 : 0);
                    return true;
                case TRANSACTION_readOtaResult:
                    data.enforceInterface(DESCRIPTOR);
                    int _result24 = readOtaResult();
                    reply.writeNoException();
                    reply.writeInt(_result24);
                    return true;
                case TRANSACTION_clearOtaResult:
                    data.enforceInterface(DESCRIPTOR);
                    int _result25 = clearOtaResult();
                    reply.writeNoException();
                    reply.writeInt(_result25);
                    return true;
                case TRANSACTION_getSwitchValue:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result26 = getSwitchValue();
                    reply.writeNoException();
                    reply.writeByteArray(_result26);
                    return true;
                case TRANSACTION_setSwitchValue:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg05 = data.createByteArray();
                    boolean _result27 = setSwitchValue(_arg05);
                    reply.writeNoException();
                    reply.writeInt(_result27 ? 1 : 0);
                    return true;
                case TRANSACTION_getDmSwitchValue:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result28 = getDmSwitchValue();
                    reply.writeNoException();
                    reply.writeByteArray(_result28);
                    return true;
                case TRANSACTION_setDmSwitchValue:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg06 = data.createByteArray();
                    boolean _result29 = setDmSwitchValue(_arg06);
                    reply.writeNoException();
                    reply.writeInt(_result29 ? 1 : 0);
                    return true;
                case TRANSACTION_getSmsRegSwitchValue:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result30 = getSmsRegSwitchValue();
                    reply.writeNoException();
                    reply.writeByteArray(_result30);
                    return true;
                case TRANSACTION_setSmsRegSwitchValue:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg07 = data.createByteArray();
                    boolean _result31 = setSmsRegSwitchValue(_arg07);
                    reply.writeNoException();
                    reply.writeInt(_result31 ? 1 : 0);
                    return true;
                case TRANSACTION_setRegisterFlag:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg08 = data.createByteArray();
                    int _arg1 = data.readInt();
                    boolean _result32 = setRegisterFlag(_arg08, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result32 ? 1 : 0);
                    return true;
                case TRANSACTION_readRegisterFlag:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result33 = readRegisterFlag();
                    reply.writeNoException();
                    reply.writeByteArray(_result33);
                    return true;
                case TRANSACTION_writeImsi1:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg09 = data.createByteArray();
                    int _arg12 = data.readInt();
                    boolean _result34 = writeImsi1(_arg09, _arg12);
                    reply.writeNoException();
                    reply.writeInt(_result34 ? 1 : 0);
                    return true;
                case TRANSACTION_writeImsi2:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg010 = data.createByteArray();
                    int _arg13 = data.readInt();
                    boolean _result35 = writeImsi2(_arg010, _arg13);
                    reply.writeNoException();
                    reply.writeInt(_result35 ? 1 : 0);
                    return true;
                case TRANSACTION_readImsi1:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result36 = readImsi1();
                    reply.writeNoException();
                    reply.writeByteArray(_result36);
                    return true;
                case TRANSACTION_readImsi2:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result37 = readImsi2();
                    reply.writeNoException();
                    reply.writeByteArray(_result37);
                    return true;
                case TRANSACTION_getMacAddr:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result38 = getMacAddr();
                    reply.writeNoException();
                    reply.writeByteArray(_result38);
                    return true;
                case TRANSACTION_writeIccID1:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg011 = data.createByteArray();
                    int _arg14 = data.readInt();
                    boolean _result39 = writeIccID1(_arg011, _arg14);
                    reply.writeNoException();
                    reply.writeInt(_result39 ? 1 : 0);
                    return true;
                case TRANSACTION_writeIccID2:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg012 = data.createByteArray();
                    int _arg15 = data.readInt();
                    boolean _result40 = writeIccID2(_arg012, _arg15);
                    reply.writeNoException();
                    reply.writeInt(_result40 ? 1 : 0);
                    return true;
                case TRANSACTION_readIccID1:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result41 = readIccID1();
                    reply.writeNoException();
                    reply.writeByteArray(_result41);
                    return true;
                case TRANSACTION_readIccID2:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result42 = readIccID2();
                    reply.writeNoException();
                    reply.writeByteArray(_result42);
                    return true;
                case TRANSACTION_setSelfRegisterFlag:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg013 = data.createByteArray();
                    int _arg16 = data.readInt();
                    boolean _result43 = setSelfRegisterFlag(_arg013, _arg16);
                    reply.writeNoException();
                    reply.writeInt(_result43 ? 1 : 0);
                    return true;
                case TRANSACTION_readSelfRegisterFlag:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result44 = readSelfRegisterFlag();
                    reply.writeNoException();
                    reply.writeByteArray(_result44);
                    return true;
                case TRANSACTION_getReceivedPreconditionFlag:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result45 = getReceivedPreconditionFlag();
                    reply.writeNoException();
                    reply.writeByteArray(_result45);
                    return true;
                case TRANSACTION_setReceivedPreconditionFlag:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg014 = data.createByteArray();
                    int _arg17 = data.readInt();
                    boolean _result46 = setReceivedPreconditionFlag(_arg014, _arg17);
                    reply.writeNoException();
                    reply.writeInt(_result46 ? 1 : 0);
                    return true;
                case TRANSACTION_getReceivedMeid:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result47 = getReceivedMeid();
                    reply.writeNoException();
                    reply.writeByteArray(_result47);
                    return true;
                case TRANSACTION_setReceivedMeid:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg015 = data.createByteArray();
                    int _arg18 = data.readInt();
                    boolean _result48 = setReceivedMeid(_arg015, _arg18);
                    reply.writeNoException();
                    reply.writeInt(_result48 ? 1 : 0);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements DmAgent {
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
            public byte[] readDmTree() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean writeDmTree(byte[] tree) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(tree);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isLockFlagSet() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setLockFlag(byte[] lockType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(lockType);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean clearLockFlag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] readImsi() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean writeImsi(byte[] imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(imsi);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] readOperatorName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getRegisterSwitch() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setRegisterSwitch(byte[] registerSwitch) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(registerSwitch);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setRebootFlag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getLockType() throws RemoteException {
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
            public int getOperatorId() throws RemoteException {
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
            public byte[] getOperatorName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getOperatorName, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isHangMoCallLocking() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isHangMoCallLocking, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isHangMtCallLocking() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isHangMtCallLocking, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean clearRebootFlag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_clearRebootFlag, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isBootRecoveryFlag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isBootRecoveryFlag, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getUpgradeStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getUpgradeStatus, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int restartAndroid() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_restartAndroid, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isWipeSet() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isWipeSet, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setWipeFlag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_setWipeFlag, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean clearWipeFlag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_clearWipeFlag, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int readOtaResult() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_readOtaResult, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int clearOtaResult() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_clearOtaResult, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getSwitchValue() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getSwitchValue, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setSwitchValue(byte[] registerSwitch) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(registerSwitch);
                    this.mRemote.transact(Stub.TRANSACTION_setSwitchValue, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getDmSwitchValue() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getDmSwitchValue, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setDmSwitchValue(byte[] registerSwitch) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(registerSwitch);
                    this.mRemote.transact(Stub.TRANSACTION_setDmSwitchValue, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getSmsRegSwitchValue() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getSmsRegSwitchValue, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setSmsRegSwitchValue(byte[] registerSwitch) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(registerSwitch);
                    this.mRemote.transact(Stub.TRANSACTION_setSmsRegSwitchValue, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setRegisterFlag(byte[] flag, int size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(flag);
                    _data.writeInt(size);
                    this.mRemote.transact(Stub.TRANSACTION_setRegisterFlag, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] readRegisterFlag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_readRegisterFlag, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean writeImsi1(byte[] imsi, int size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(imsi);
                    _data.writeInt(size);
                    this.mRemote.transact(Stub.TRANSACTION_writeImsi1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean writeImsi2(byte[] imsi, int size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(imsi);
                    _data.writeInt(size);
                    this.mRemote.transact(Stub.TRANSACTION_writeImsi2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] readImsi1() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_readImsi1, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] readImsi2() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_readImsi2, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getMacAddr() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getMacAddr, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean writeIccID1(byte[] iccID, int size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(iccID);
                    _data.writeInt(size);
                    this.mRemote.transact(Stub.TRANSACTION_writeIccID1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean writeIccID2(byte[] iccID, int size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(iccID);
                    _data.writeInt(size);
                    this.mRemote.transact(Stub.TRANSACTION_writeIccID2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] readIccID1() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_readIccID1, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] readIccID2() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_readIccID2, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setSelfRegisterFlag(byte[] flag, int size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(flag);
                    _data.writeInt(size);
                    this.mRemote.transact(Stub.TRANSACTION_setSelfRegisterFlag, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] readSelfRegisterFlag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_readSelfRegisterFlag, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getReceivedPreconditionFlag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getReceivedPreconditionFlag, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setReceivedPreconditionFlag(byte[] flag, int size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(flag);
                    _data.writeInt(size);
                    this.mRemote.transact(Stub.TRANSACTION_setReceivedPreconditionFlag, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getReceivedMeid() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getReceivedMeid, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setReceivedMeid(byte[] meid, int size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(meid);
                    _data.writeInt(size);
                    this.mRemote.transact(Stub.TRANSACTION_setReceivedMeid, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
