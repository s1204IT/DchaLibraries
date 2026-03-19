package com.mediatek.internal.telephony;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.RadioAccessFamily;
import java.util.List;

public interface IMtkTelephonyEx extends IInterface {
    boolean exitEmergencyCallbackMode(int i) throws RemoteException;

    int[] getAdnStorageInfo(int i) throws RemoteException;

    List<CellInfo> getAllCellInfo(int i, String str) throws RemoteException;

    PseudoCellInfo getApcInfoUsingSlotId(int i) throws RemoteException;

    int getCdmaSubscriptionActStatus(int i) throws RemoteException;

    Bundle getCellLocationUsingSlotId(int i) throws RemoteException;

    int getIccAppFamily(int i) throws RemoteException;

    String getIccAtr(int i) throws RemoteException;

    String getIccCardType(int i) throws RemoteException;

    boolean getIsLastEccIms() throws RemoteException;

    String getLocatedPlmn(int i) throws RemoteException;

    String getLteAccessStratumState() throws RemoteException;

    int getMainCapabilityPhoneId() throws RemoteException;

    String getMvnoMatchType(int i) throws RemoteException;

    String getMvnoPattern(int i, String str) throws RemoteException;

    int getPCO520State(int i) throws RemoteException;

    int[] getRxTestResult(int i) throws RemoteException;

    int getSelfActivateState(int i) throws RemoteException;

    String getSimSerialNumber(String str, int i) throws RemoteException;

    String getUimSubscriberId(String str, int i) throws RemoteException;

    byte[] iccExchangeSimIOEx(int i, int i2, int i3, int i4, int i5, int i6, String str, String str2, String str3) throws RemoteException;

    int invokeOemRilRequestRaw(byte[] bArr, byte[] bArr2) throws RemoteException;

    int invokeOemRilRequestRawBySlot(int i, byte[] bArr, byte[] bArr2) throws RemoteException;

    boolean isAppTypeSupported(int i, int i2) throws RemoteException;

    boolean isCapabilitySwitching() throws RemoteException;

    boolean isEccInProgress() throws RemoteException;

    boolean isFdnEnabled(int i) throws RemoteException;

    boolean isImsRegistered(int i) throws RemoteException;

    boolean isInCsCall(int i) throws RemoteException;

    boolean isInHomeNetwork(int i) throws RemoteException;

    boolean isPhbReady(int i) throws RemoteException;

    boolean isRadioOffBySimManagement(int i) throws RemoteException;

    boolean isSharedDefaultApn() throws RemoteException;

    boolean isTestIccCard(int i) throws RemoteException;

    boolean isVolteEnabled(int i) throws RemoteException;

    boolean isWifiCallingEnabled(int i) throws RemoteException;

    List<String> loadEFLinearFixedAll(int i, int i2, int i3, String str) throws RemoteException;

    byte[] loadEFTransparent(int i, int i2, int i3, String str) throws RemoteException;

    Bundle queryNetworkLock(int i, int i2) throws RemoteException;

    void repollIccStateForNetworkLock(int i, boolean z) throws RemoteException;

    int selfActivationAction(int i, Bundle bundle, int i2) throws RemoteException;

    void setApcModeUsingSlotId(int i, int i2, boolean z, int i3) throws RemoteException;

    void setEccInProgress(boolean z) throws RemoteException;

    void setIsLastEccIms(boolean z) throws RemoteException;

    boolean setLteAccessStratumReport(boolean z) throws RemoteException;

    boolean setLteUplinkDataTransfer(boolean z, int i) throws RemoteException;

    boolean setRadioCapability(RadioAccessFamily[] radioAccessFamilyArr) throws RemoteException;

    int[] setRxTestConfig(int i, int i2) throws RemoteException;

    void setTelLog(boolean z) throws RemoteException;

    byte[] simAkaAuthentication(int i, int i2, byte[] bArr, byte[] bArr2) throws RemoteException;

    byte[] simGbaAuthBootStrapMode(int i, int i2, byte[] bArr, byte[] bArr2) throws RemoteException;

    byte[] simGbaAuthNafMode(int i, int i2, byte[] bArr, byte[] bArr2) throws RemoteException;

    int[] supplyDeviceNetworkDepersonalization(String str) throws RemoteException;

    int supplyNetworkDepersonalization(int i, String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkTelephonyEx {
        private static final String DESCRIPTOR = "com.mediatek.internal.telephony.IMtkTelephonyEx";
        static final int TRANSACTION_exitEmergencyCallbackMode = 43;
        static final int TRANSACTION_getAdnStorageInfo = 34;
        static final int TRANSACTION_getAllCellInfo = 52;
        static final int TRANSACTION_getApcInfoUsingSlotId = 45;
        static final int TRANSACTION_getCdmaSubscriptionActStatus = 46;
        static final int TRANSACTION_getCellLocationUsingSlotId = 21;
        static final int TRANSACTION_getIccAppFamily = 3;
        static final int TRANSACTION_getIccAtr = 7;
        static final int TRANSACTION_getIccCardType = 4;
        static final int TRANSACTION_getIsLastEccIms = 48;
        static final int TRANSACTION_getLocatedPlmn = 53;
        static final int TRANSACTION_getLteAccessStratumState = 32;
        static final int TRANSACTION_getMainCapabilityPhoneId = 26;
        static final int TRANSACTION_getMvnoMatchType = 14;
        static final int TRANSACTION_getMvnoPattern = 15;
        static final int TRANSACTION_getPCO520State = 40;
        static final int TRANSACTION_getRxTestResult = 37;
        static final int TRANSACTION_getSelfActivateState = 39;
        static final int TRANSACTION_getSimSerialNumber = 23;
        static final int TRANSACTION_getUimSubscriberId = 22;
        static final int TRANSACTION_iccExchangeSimIOEx = 8;
        static final int TRANSACTION_invokeOemRilRequestRaw = 49;
        static final int TRANSACTION_invokeOemRilRequestRawBySlot = 50;
        static final int TRANSACTION_isAppTypeSupported = 5;
        static final int TRANSACTION_isCapabilitySwitching = 25;
        static final int TRANSACTION_isEccInProgress = 42;
        static final int TRANSACTION_isFdnEnabled = 20;
        static final int TRANSACTION_isImsRegistered = 27;
        static final int TRANSACTION_isInCsCall = 51;
        static final int TRANSACTION_isInHomeNetwork = 2;
        static final int TRANSACTION_isPhbReady = 35;
        static final int TRANSACTION_isRadioOffBySimManagement = 19;
        static final int TRANSACTION_isSharedDefaultApn = 33;
        static final int TRANSACTION_isTestIccCard = 6;
        static final int TRANSACTION_isVolteEnabled = 28;
        static final int TRANSACTION_isWifiCallingEnabled = 29;
        static final int TRANSACTION_loadEFLinearFixedAll = 10;
        static final int TRANSACTION_loadEFTransparent = 9;
        static final int TRANSACTION_queryNetworkLock = 11;
        static final int TRANSACTION_repollIccStateForNetworkLock = 13;
        static final int TRANSACTION_selfActivationAction = 38;
        static final int TRANSACTION_setApcModeUsingSlotId = 44;
        static final int TRANSACTION_setEccInProgress = 41;
        static final int TRANSACTION_setIsLastEccIms = 47;
        static final int TRANSACTION_setLteAccessStratumReport = 30;
        static final int TRANSACTION_setLteUplinkDataTransfer = 31;
        static final int TRANSACTION_setRadioCapability = 24;
        static final int TRANSACTION_setRxTestConfig = 36;
        static final int TRANSACTION_setTelLog = 1;
        static final int TRANSACTION_simAkaAuthentication = 16;
        static final int TRANSACTION_simGbaAuthBootStrapMode = 17;
        static final int TRANSACTION_simGbaAuthNafMode = 18;
        static final int TRANSACTION_supplyDeviceNetworkDepersonalization = 54;
        static final int TRANSACTION_supplyNetworkDepersonalization = 12;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkTelephonyEx asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkTelephonyEx)) {
                return (IMtkTelephonyEx) iInterfaceQueryLocalInterface;
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
                    setTelLog(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsInHomeNetwork = isInHomeNetwork(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInHomeNetwork ? 1 : 0);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iccAppFamily = getIccAppFamily(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iccAppFamily);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    String iccCardType = getIccCardType(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(iccCardType);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsAppTypeSupported = isAppTypeSupported(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsAppTypeSupported ? 1 : 0);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsTestIccCard = isTestIccCard(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsTestIccCard ? 1 : 0);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    String iccAtr = getIccAtr(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(iccAtr);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArrIccExchangeSimIOEx = iccExchangeSimIOEx(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(bArrIccExchangeSimIOEx);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArrLoadEFTransparent = loadEFTransparent(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(bArrLoadEFTransparent);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<String> listLoadEFLinearFixedAll = loadEFLinearFixedAll(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeStringList(listLoadEFLinearFixedAll);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    Bundle bundleQueryNetworkLock = queryNetworkLock(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (bundleQueryNetworkLock != null) {
                        parcel2.writeInt(1);
                        bundleQueryNetworkLock.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSupplyNetworkDepersonalization = supplyNetworkDepersonalization(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iSupplyNetworkDepersonalization);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    repollIccStateForNetworkLock(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    String mvnoMatchType = getMvnoMatchType(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(mvnoMatchType);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    String mvnoPattern = getMvnoPattern(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(mvnoPattern);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArrSimAkaAuthentication = simAkaAuthentication(parcel.readInt(), parcel.readInt(), parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(bArrSimAkaAuthentication);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArrSimGbaAuthBootStrapMode = simGbaAuthBootStrapMode(parcel.readInt(), parcel.readInt(), parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(bArrSimGbaAuthBootStrapMode);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArrSimGbaAuthNafMode = simGbaAuthNafMode(parcel.readInt(), parcel.readInt(), parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(bArrSimGbaAuthNafMode);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsRadioOffBySimManagement = isRadioOffBySimManagement(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsRadioOffBySimManagement ? 1 : 0);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsFdnEnabled = isFdnEnabled(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsFdnEnabled ? 1 : 0);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    Bundle cellLocationUsingSlotId = getCellLocationUsingSlotId(parcel.readInt());
                    parcel2.writeNoException();
                    if (cellLocationUsingSlotId != null) {
                        parcel2.writeInt(1);
                        cellLocationUsingSlotId.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    String uimSubscriberId = getUimSubscriberId(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(uimSubscriberId);
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    String simSerialNumber = getSimSerialNumber(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(simSerialNumber);
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean radioCapability = setRadioCapability((RadioAccessFamily[]) parcel.createTypedArray(RadioAccessFamily.CREATOR));
                    parcel2.writeNoException();
                    parcel2.writeInt(radioCapability ? 1 : 0);
                    return true;
                case TRANSACTION_isCapabilitySwitching:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsCapabilitySwitching = isCapabilitySwitching();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsCapabilitySwitching ? 1 : 0);
                    return true;
                case TRANSACTION_getMainCapabilityPhoneId:
                    parcel.enforceInterface(DESCRIPTOR);
                    int mainCapabilityPhoneId = getMainCapabilityPhoneId();
                    parcel2.writeNoException();
                    parcel2.writeInt(mainCapabilityPhoneId);
                    return true;
                case TRANSACTION_isImsRegistered:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsImsRegistered = isImsRegistered(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsImsRegistered ? 1 : 0);
                    return true;
                case TRANSACTION_isVolteEnabled:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsVolteEnabled = isVolteEnabled(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsVolteEnabled ? 1 : 0);
                    return true;
                case TRANSACTION_isWifiCallingEnabled:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsWifiCallingEnabled = isWifiCallingEnabled(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsWifiCallingEnabled ? 1 : 0);
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean lteAccessStratumReport = setLteAccessStratumReport(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(lteAccessStratumReport ? 1 : 0);
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean lteUplinkDataTransfer = setLteUplinkDataTransfer(parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(lteUplinkDataTransfer ? 1 : 0);
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    String lteAccessStratumState = getLteAccessStratumState();
                    parcel2.writeNoException();
                    parcel2.writeString(lteAccessStratumState);
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsSharedDefaultApn = isSharedDefaultApn();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSharedDefaultApn ? 1 : 0);
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] adnStorageInfo = getAdnStorageInfo(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeIntArray(adnStorageInfo);
                    return true;
                case TRANSACTION_isPhbReady:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsPhbReady = isPhbReady(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPhbReady ? 1 : 0);
                    return true;
                case TRANSACTION_setRxTestConfig:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] rxTestConfig = setRxTestConfig(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeIntArray(rxTestConfig);
                    return true;
                case TRANSACTION_getRxTestResult:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] rxTestResult = getRxTestResult(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeIntArray(rxTestResult);
                    return true;
                case TRANSACTION_selfActivationAction:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSelfActivationAction = selfActivationAction(parcel.readInt(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iSelfActivationAction);
                    return true;
                case TRANSACTION_getSelfActivateState:
                    parcel.enforceInterface(DESCRIPTOR);
                    int selfActivateState = getSelfActivateState(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(selfActivateState);
                    return true;
                case TRANSACTION_getPCO520State:
                    parcel.enforceInterface(DESCRIPTOR);
                    int pCO520State = getPCO520State(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(pCO520State);
                    return true;
                case TRANSACTION_setEccInProgress:
                    parcel.enforceInterface(DESCRIPTOR);
                    setEccInProgress(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_isEccInProgress:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsEccInProgress = isEccInProgress();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsEccInProgress ? 1 : 0);
                    return true;
                case TRANSACTION_exitEmergencyCallbackMode:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zExitEmergencyCallbackMode = exitEmergencyCallbackMode(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zExitEmergencyCallbackMode ? 1 : 0);
                    return true;
                case TRANSACTION_setApcModeUsingSlotId:
                    parcel.enforceInterface(DESCRIPTOR);
                    setApcModeUsingSlotId(parcel.readInt(), parcel.readInt(), parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_getApcInfoUsingSlotId:
                    parcel.enforceInterface(DESCRIPTOR);
                    PseudoCellInfo apcInfoUsingSlotId = getApcInfoUsingSlotId(parcel.readInt());
                    parcel2.writeNoException();
                    if (apcInfoUsingSlotId != null) {
                        parcel2.writeInt(1);
                        apcInfoUsingSlotId.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case TRANSACTION_getCdmaSubscriptionActStatus:
                    parcel.enforceInterface(DESCRIPTOR);
                    int cdmaSubscriptionActStatus = getCdmaSubscriptionActStatus(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(cdmaSubscriptionActStatus);
                    return true;
                case TRANSACTION_setIsLastEccIms:
                    parcel.enforceInterface(DESCRIPTOR);
                    setIsLastEccIms(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_getIsLastEccIms:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean isLastEccIms = getIsLastEccIms();
                    parcel2.writeNoException();
                    parcel2.writeInt(isLastEccIms ? 1 : 0);
                    return true;
                case TRANSACTION_invokeOemRilRequestRaw:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArrCreateByteArray = parcel.createByteArray();
                    int i3 = parcel.readInt();
                    byte[] bArr = i3 >= 0 ? new byte[i3] : null;
                    int iInvokeOemRilRequestRaw = invokeOemRilRequestRaw(bArrCreateByteArray, bArr);
                    parcel2.writeNoException();
                    parcel2.writeInt(iInvokeOemRilRequestRaw);
                    parcel2.writeByteArray(bArr);
                    return true;
                case TRANSACTION_invokeOemRilRequestRawBySlot:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i4 = parcel.readInt();
                    byte[] bArrCreateByteArray2 = parcel.createByteArray();
                    int i5 = parcel.readInt();
                    byte[] bArr2 = i5 >= 0 ? new byte[i5] : null;
                    int iInvokeOemRilRequestRawBySlot = invokeOemRilRequestRawBySlot(i4, bArrCreateByteArray2, bArr2);
                    parcel2.writeNoException();
                    parcel2.writeInt(iInvokeOemRilRequestRawBySlot);
                    parcel2.writeByteArray(bArr2);
                    return true;
                case TRANSACTION_isInCsCall:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsInCsCall = isInCsCall(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInCsCall ? 1 : 0);
                    return true;
                case TRANSACTION_getAllCellInfo:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<CellInfo> allCellInfo = getAllCellInfo(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(allCellInfo);
                    return true;
                case TRANSACTION_getLocatedPlmn:
                    parcel.enforceInterface(DESCRIPTOR);
                    String locatedPlmn = getLocatedPlmn(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(locatedPlmn);
                    return true;
                case TRANSACTION_supplyDeviceNetworkDepersonalization:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] iArrSupplyDeviceNetworkDepersonalization = supplyDeviceNetworkDepersonalization(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeIntArray(iArrSupplyDeviceNetworkDepersonalization);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkTelephonyEx {
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
            public void setTelLog(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isInHomeNetwork(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getIccAppFamily(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getIccCardType(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isAppTypeSupported(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isTestIccCard(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getIccAtr(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] iccExchangeSimIOEx(int i, int i2, int i3, int i4, int i5, int i6, String str, String str2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeInt(i5);
                    parcelObtain.writeInt(i6);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] loadEFTransparent(int i, int i2, int i3, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<String> loadEFLinearFixedAll(int i, int i2, int i3, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArrayList();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bundle queryNetworkLock(int i, int i2) throws RemoteException {
                Bundle bundle;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bundle = null;
                    }
                    return bundle;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int supplyNetworkDepersonalization(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void repollIccStateForNetworkLock(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getMvnoMatchType(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getMvnoPattern(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] simAkaAuthentication(int i, int i2, byte[] bArr, byte[] bArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] simGbaAuthBootStrapMode(int i, int i2, byte[] bArr, byte[] bArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] simGbaAuthNafMode(int i, int i2, byte[] bArr, byte[] bArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isRadioOffBySimManagement(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isFdnEnabled(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bundle getCellLocationUsingSlotId(int i) throws RemoteException {
                Bundle bundle;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bundle = null;
                    }
                    return bundle;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getUimSubscriberId(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getSimSerialNumber(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setRadioCapability(RadioAccessFamily[] radioAccessFamilyArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeTypedArray(radioAccessFamilyArr, 0);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isCapabilitySwitching() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isCapabilitySwitching, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getMainCapabilityPhoneId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getMainCapabilityPhoneId, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isImsRegistered(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_isImsRegistered, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isVolteEnabled(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_isVolteEnabled, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isWifiCallingEnabled(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_isWifiCallingEnabled, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setLteAccessStratumReport(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setLteUplinkDataTransfer(boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getLteAccessStratumState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isSharedDefaultApn() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] getAdnStorageInfo(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPhbReady(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_isPhbReady, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] setRxTestConfig(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(Stub.TRANSACTION_setRxTestConfig, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] getRxTestResult(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_getRxTestResult, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int selfActivationAction(int i, Bundle bundle, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(Stub.TRANSACTION_selfActivationAction, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getSelfActivateState(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_getSelfActivateState, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPCO520State(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_getPCO520State, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setEccInProgress(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(Stub.TRANSACTION_setEccInProgress, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isEccInProgress() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_isEccInProgress, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean exitEmergencyCallbackMode(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_exitEmergencyCallbackMode, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setApcModeUsingSlotId(int i, int i2, boolean z, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(Stub.TRANSACTION_setApcModeUsingSlotId, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PseudoCellInfo getApcInfoUsingSlotId(int i) throws RemoteException {
                PseudoCellInfo pseudoCellInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_getApcInfoUsingSlotId, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        pseudoCellInfoCreateFromParcel = PseudoCellInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        pseudoCellInfoCreateFromParcel = null;
                    }
                    return pseudoCellInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getCdmaSubscriptionActStatus(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_getCdmaSubscriptionActStatus, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setIsLastEccIms(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(Stub.TRANSACTION_setIsLastEccIms, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean getIsLastEccIms() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getIsLastEccIms, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int invokeOemRilRequestRaw(byte[] bArr, byte[] bArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    if (bArr2 == null) {
                        parcelObtain.writeInt(-1);
                    } else {
                        parcelObtain.writeInt(bArr2.length);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_invokeOemRilRequestRaw, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    parcelObtain2.readByteArray(bArr2);
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int invokeOemRilRequestRawBySlot(int i, byte[] bArr, byte[] bArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    if (bArr2 == null) {
                        parcelObtain.writeInt(-1);
                    } else {
                        parcelObtain.writeInt(bArr2.length);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_invokeOemRilRequestRawBySlot, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i2 = parcelObtain2.readInt();
                    parcelObtain2.readByteArray(bArr2);
                    return i2;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isInCsCall(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_isInCsCall, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<CellInfo> getAllCellInfo(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_getAllCellInfo, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(CellInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getLocatedPlmn(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_getLocatedPlmn, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] supplyDeviceNetworkDepersonalization(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_supplyDeviceNetworkDepersonalization, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
