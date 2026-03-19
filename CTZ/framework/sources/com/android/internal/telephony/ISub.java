package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.SubscriptionInfo;
import java.util.List;

public interface ISub extends IInterface {
    int addSubInfoRecord(String str, int i) throws RemoteException;

    void clearDefaultsForInactiveSubIds() throws RemoteException;

    int clearSubInfo() throws RemoteException;

    List<SubscriptionInfo> getAccessibleSubscriptionInfoList(String str) throws RemoteException;

    int[] getActiveSubIdList() throws RemoteException;

    int getActiveSubInfoCount(String str) throws RemoteException;

    int getActiveSubInfoCountMax() throws RemoteException;

    SubscriptionInfo getActiveSubscriptionInfo(int i, String str) throws RemoteException;

    SubscriptionInfo getActiveSubscriptionInfoForIccId(String str, String str2) throws RemoteException;

    SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int i, String str) throws RemoteException;

    List<SubscriptionInfo> getActiveSubscriptionInfoList(String str) throws RemoteException;

    int getAllSubInfoCount(String str) throws RemoteException;

    List<SubscriptionInfo> getAllSubInfoList(String str) throws RemoteException;

    List<SubscriptionInfo> getAvailableSubscriptionInfoList(String str) throws RemoteException;

    int getDefaultDataSubId() throws RemoteException;

    int getDefaultSmsSubId() throws RemoteException;

    int getDefaultSubId() throws RemoteException;

    int getDefaultVoiceSubId() throws RemoteException;

    int getPhoneId(int i) throws RemoteException;

    int getSimStateForSlotIndex(int i) throws RemoteException;

    int getSlotIndex(int i) throws RemoteException;

    int[] getSubId(int i) throws RemoteException;

    String getSubscriptionProperty(int i, String str, String str2) throws RemoteException;

    boolean isActiveSubId(int i) throws RemoteException;

    void requestEmbeddedSubscriptionInfoListRefresh() throws RemoteException;

    int setDataRoaming(int i, int i2) throws RemoteException;

    void setDefaultDataSubId(int i) throws RemoteException;

    void setDefaultSmsSubId(int i) throws RemoteException;

    void setDefaultVoiceSubId(int i) throws RemoteException;

    int setDisplayName(String str, int i) throws RemoteException;

    int setDisplayNameUsingSrc(String str, int i, long j) throws RemoteException;

    int setDisplayNumber(String str, int i) throws RemoteException;

    int setIconTint(int i, int i2) throws RemoteException;

    void setSubscriptionProperty(int i, String str, String str2) throws RemoteException;

    public static abstract class Stub extends Binder implements ISub {
        private static final String DESCRIPTOR = "com.android.internal.telephony.ISub";
        static final int TRANSACTION_addSubInfoRecord = 12;
        static final int TRANSACTION_clearDefaultsForInactiveSubIds = 29;
        static final int TRANSACTION_clearSubInfo = 21;
        static final int TRANSACTION_getAccessibleSubscriptionInfoList = 10;
        static final int TRANSACTION_getActiveSubIdList = 30;
        static final int TRANSACTION_getActiveSubInfoCount = 7;
        static final int TRANSACTION_getActiveSubInfoCountMax = 8;
        static final int TRANSACTION_getActiveSubscriptionInfo = 3;
        static final int TRANSACTION_getActiveSubscriptionInfoForIccId = 4;
        static final int TRANSACTION_getActiveSubscriptionInfoForSimSlotIndex = 5;
        static final int TRANSACTION_getActiveSubscriptionInfoList = 6;
        static final int TRANSACTION_getAllSubInfoCount = 2;
        static final int TRANSACTION_getAllSubInfoList = 1;
        static final int TRANSACTION_getAvailableSubscriptionInfoList = 9;
        static final int TRANSACTION_getDefaultDataSubId = 23;
        static final int TRANSACTION_getDefaultSmsSubId = 27;
        static final int TRANSACTION_getDefaultSubId = 20;
        static final int TRANSACTION_getDefaultVoiceSubId = 25;
        static final int TRANSACTION_getPhoneId = 22;
        static final int TRANSACTION_getSimStateForSlotIndex = 33;
        static final int TRANSACTION_getSlotIndex = 18;
        static final int TRANSACTION_getSubId = 19;
        static final int TRANSACTION_getSubscriptionProperty = 32;
        static final int TRANSACTION_isActiveSubId = 34;
        static final int TRANSACTION_requestEmbeddedSubscriptionInfoListRefresh = 11;
        static final int TRANSACTION_setDataRoaming = 17;
        static final int TRANSACTION_setDefaultDataSubId = 24;
        static final int TRANSACTION_setDefaultSmsSubId = 28;
        static final int TRANSACTION_setDefaultVoiceSubId = 26;
        static final int TRANSACTION_setDisplayName = 14;
        static final int TRANSACTION_setDisplayNameUsingSrc = 15;
        static final int TRANSACTION_setDisplayNumber = 16;
        static final int TRANSACTION_setIconTint = 13;
        static final int TRANSACTION_setSubscriptionProperty = 31;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISub asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ISub)) {
                return (ISub) iInterfaceQueryLocalInterface;
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
                    List<SubscriptionInfo> allSubInfoList = getAllSubInfoList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(allSubInfoList);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int allSubInfoCount = getAllSubInfoCount(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(allSubInfoCount);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    SubscriptionInfo activeSubscriptionInfo = getActiveSubscriptionInfo(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    if (activeSubscriptionInfo != null) {
                        parcel2.writeInt(1);
                        activeSubscriptionInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    SubscriptionInfo activeSubscriptionInfoForIccId = getActiveSubscriptionInfoForIccId(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    if (activeSubscriptionInfoForIccId != null) {
                        parcel2.writeInt(1);
                        activeSubscriptionInfoForIccId.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = getActiveSubscriptionInfoForSimSlotIndex(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    if (activeSubscriptionInfoForSimSlotIndex != null) {
                        parcel2.writeInt(1);
                        activeSubscriptionInfoForSimSlotIndex.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<SubscriptionInfo> activeSubscriptionInfoList = getActiveSubscriptionInfoList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(activeSubscriptionInfoList);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    int activeSubInfoCount = getActiveSubInfoCount(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(activeSubInfoCount);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    int activeSubInfoCountMax = getActiveSubInfoCountMax();
                    parcel2.writeNoException();
                    parcel2.writeInt(activeSubInfoCountMax);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<SubscriptionInfo> availableSubscriptionInfoList = getAvailableSubscriptionInfoList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(availableSubscriptionInfoList);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<SubscriptionInfo> accessibleSubscriptionInfoList = getAccessibleSubscriptionInfoList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(accessibleSubscriptionInfoList);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestEmbeddedSubscriptionInfoListRefresh();
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAddSubInfoRecord = addSubInfoRecord(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddSubInfoRecord);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iconTint = setIconTint(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iconTint);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    int displayName = setDisplayName(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(displayName);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    int displayNameUsingSrc = setDisplayNameUsingSrc(parcel.readString(), parcel.readInt(), parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(displayNameUsingSrc);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    int displayNumber = setDisplayNumber(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(displayNumber);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    int dataRoaming = setDataRoaming(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(dataRoaming);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    int slotIndex = getSlotIndex(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(slotIndex);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] subId = getSubId(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeIntArray(subId);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    int defaultSubId = getDefaultSubId();
                    parcel2.writeNoException();
                    parcel2.writeInt(defaultSubId);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iClearSubInfo = clearSubInfo();
                    parcel2.writeNoException();
                    parcel2.writeInt(iClearSubInfo);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    int phoneId = getPhoneId(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(phoneId);
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    int defaultDataSubId = getDefaultDataSubId();
                    parcel2.writeNoException();
                    parcel2.writeInt(defaultDataSubId);
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDefaultDataSubId(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    int defaultVoiceSubId = getDefaultVoiceSubId();
                    parcel2.writeNoException();
                    parcel2.writeInt(defaultVoiceSubId);
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDefaultVoiceSubId(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    int defaultSmsSubId = getDefaultSmsSubId();
                    parcel2.writeNoException();
                    parcel2.writeInt(defaultSmsSubId);
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDefaultSmsSubId(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearDefaultsForInactiveSubIds();
                    parcel2.writeNoException();
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] activeSubIdList = getActiveSubIdList();
                    parcel2.writeNoException();
                    parcel2.writeIntArray(activeSubIdList);
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSubscriptionProperty(parcel.readInt(), parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    String subscriptionProperty = getSubscriptionProperty(parcel.readInt(), parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(subscriptionProperty);
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    int simStateForSlotIndex = getSimStateForSlotIndex(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(simStateForSlotIndex);
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsActiveSubId = isActiveSubId(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsActiveSubId ? 1 : 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ISub {
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
            public List<SubscriptionInfo> getAllSubInfoList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(SubscriptionInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getAllSubInfoCount(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public SubscriptionInfo getActiveSubscriptionInfo(int i, String str) throws RemoteException {
                SubscriptionInfo subscriptionInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        subscriptionInfoCreateFromParcel = SubscriptionInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        subscriptionInfoCreateFromParcel = null;
                    }
                    return subscriptionInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public SubscriptionInfo getActiveSubscriptionInfoForIccId(String str, String str2) throws RemoteException {
                SubscriptionInfo subscriptionInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        subscriptionInfoCreateFromParcel = SubscriptionInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        subscriptionInfoCreateFromParcel = null;
                    }
                    return subscriptionInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int i, String str) throws RemoteException {
                SubscriptionInfo subscriptionInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        subscriptionInfoCreateFromParcel = SubscriptionInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        subscriptionInfoCreateFromParcel = null;
                    }
                    return subscriptionInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<SubscriptionInfo> getActiveSubscriptionInfoList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(SubscriptionInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getActiveSubInfoCount(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getActiveSubInfoCountMax() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<SubscriptionInfo> getAvailableSubscriptionInfoList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(SubscriptionInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<SubscriptionInfo> getAccessibleSubscriptionInfoList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(SubscriptionInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void requestEmbeddedSubscriptionInfoListRefresh() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addSubInfoRecord(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setIconTint(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setDisplayName(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setDisplayNameUsingSrc(String str, int i, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setDisplayNumber(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setDataRoaming(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getSlotIndex(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] getSubId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getDefaultSubId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int clearSubInfo() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPhoneId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getDefaultDataSubId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDefaultDataSubId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getDefaultVoiceSubId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDefaultVoiceSubId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getDefaultSmsSubId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDefaultSmsSubId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearDefaultsForInactiveSubIds() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] getActiveSubIdList() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSubscriptionProperty(int i, String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getSubscriptionProperty(int i, String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getSimStateForSlotIndex(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isActiveSubId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
