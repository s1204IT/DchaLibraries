package com.android.internal.telephony;

import android.hardware.radio.V1_0.CellIdentityCdma;
import android.hardware.radio.V1_0.CellIdentityGsm;
import android.hardware.radio.V1_0.CellIdentityLte;
import android.hardware.radio.V1_0.CellIdentityTdscdma;
import android.hardware.radio.V1_0.CellIdentityWcdma;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.CellIdentity;
import android.telephony.NetworkRegistrationState;
import android.telephony.NetworkService;
import android.telephony.NetworkServiceCallback;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import java.util.concurrent.ConcurrentHashMap;

public class CellularNetworkService extends NetworkService {
    private static final boolean DBG = false;
    protected static final int GET_CS_REGISTRATION_STATE_DONE = 1;
    protected static final int GET_PS_REGISTRATION_STATE_DONE = 2;
    protected static final int NETWORK_REGISTRATION_STATE_CHANGED = 3;
    private static final String TAG = CellularNetworkService.class.getSimpleName();

    public class CellularNetworkServiceProvider extends NetworkService.NetworkServiceProvider {
        protected final ConcurrentHashMap<Message, NetworkServiceCallback> mCallbackMap;
        protected final Handler mHandler;
        private final HandlerThread mHandlerThread;
        private final Looper mLooper;
        protected final Phone mPhone;

        protected CellularNetworkServiceProvider(int i) {
            super(CellularNetworkService.this, i);
            this.mCallbackMap = new ConcurrentHashMap<>();
            this.mPhone = PhoneFactory.getPhone(getSlotId());
            this.mHandlerThread = new HandlerThread(CellularNetworkService.class.getSimpleName());
            this.mHandlerThread.start();
            this.mLooper = this.mHandlerThread.getLooper();
            this.mHandler = new Handler(this.mLooper) {
                @Override
                public void handleMessage(Message message) {
                    int i2;
                    NetworkServiceCallback networkServiceCallbackRemove = CellularNetworkServiceProvider.this.mCallbackMap.remove(message);
                    switch (message.what) {
                        case 1:
                        case 2:
                            if (networkServiceCallbackRemove != null) {
                                AsyncResult asyncResult = (AsyncResult) message.obj;
                                NetworkRegistrationState registrationStateFromResult = CellularNetworkServiceProvider.this.getRegistrationStateFromResult(asyncResult.result, message.what != 1 ? 2 : 1);
                                if (asyncResult.exception != null || registrationStateFromResult == null) {
                                    i2 = 5;
                                } else {
                                    i2 = 0;
                                }
                                try {
                                    networkServiceCallbackRemove.onGetNetworkRegistrationStateComplete(i2, registrationStateFromResult);
                                } catch (Exception e) {
                                    CellularNetworkService.this.loge("Exception: " + e);
                                    return;
                                }
                                break;
                            }
                            break;
                        case 3:
                            CellularNetworkServiceProvider.this.notifyNetworkRegistrationStateChanged();
                            break;
                    }
                }
            };
            this.mPhone.mCi.registerForNetworkStateChanged(this.mHandler, 3, null);
        }

        protected int getRegStateFromHalRegState(int i) {
            if (i != 10) {
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        return 1;
                    case 2:
                        return 2;
                    case 3:
                        return 3;
                    case 4:
                        return 4;
                    case 5:
                        return 5;
                    default:
                        switch (i) {
                            case 12:
                                return 2;
                            case 13:
                                return 3;
                            case 14:
                                return 4;
                            default:
                                return 0;
                        }
                }
            }
            return 0;
        }

        protected boolean isEmergencyOnly(int i) {
            switch (i) {
                case 10:
                case 12:
                case 13:
                case 14:
                    return true;
                case 11:
                default:
                    return false;
            }
        }

        protected int[] getAvailableServices(int i, int i2, boolean z) {
            if (z) {
                return new int[]{5};
            }
            if (i == 5 || i == 1) {
                if (i2 == 2) {
                    return new int[]{2};
                }
                if (i2 == 1) {
                    return new int[]{1, 3, 4};
                }
            }
            return null;
        }

        protected int getAccessNetworkTechnologyFromRat(int i) {
            return ServiceState.rilRadioTechnologyToNetworkType(i);
        }

        protected NetworkRegistrationState getRegistrationStateFromResult(Object obj, int i) {
            if (obj == null) {
                return null;
            }
            if (i == 1) {
                return createRegistrationStateFromVoiceRegState(obj);
            }
            if (i != 2) {
                return null;
            }
            return createRegistrationStateFromDataRegState(obj);
        }

        protected NetworkRegistrationState createRegistrationStateFromVoiceRegState(Object obj) {
            if (obj instanceof VoiceRegStateResult) {
                VoiceRegStateResult voiceRegStateResult = (VoiceRegStateResult) obj;
                int regStateFromHalRegState = getRegStateFromHalRegState(voiceRegStateResult.regState);
                int accessNetworkTechnologyFromRat = getAccessNetworkTechnologyFromRat(voiceRegStateResult.rat);
                int i = voiceRegStateResult.reasonForDenial;
                boolean zIsEmergencyOnly = isEmergencyOnly(voiceRegStateResult.regState);
                return new NetworkRegistrationState(1, 1, regStateFromHalRegState, accessNetworkTechnologyFromRat, i, zIsEmergencyOnly, getAvailableServices(regStateFromHalRegState, 1, zIsEmergencyOnly), convertHalCellIdentityToCellIdentity(voiceRegStateResult.cellIdentity), voiceRegStateResult.cssSupported, voiceRegStateResult.roamingIndicator, voiceRegStateResult.systemIsInPrl, voiceRegStateResult.defaultRoamingIndicator);
            }
            if (obj instanceof android.hardware.radio.V1_2.VoiceRegStateResult) {
                android.hardware.radio.V1_2.VoiceRegStateResult voiceRegStateResult2 = (android.hardware.radio.V1_2.VoiceRegStateResult) obj;
                int regStateFromHalRegState2 = getRegStateFromHalRegState(voiceRegStateResult2.regState);
                int accessNetworkTechnologyFromRat2 = getAccessNetworkTechnologyFromRat(voiceRegStateResult2.rat);
                int i2 = voiceRegStateResult2.reasonForDenial;
                boolean zIsEmergencyOnly2 = isEmergencyOnly(voiceRegStateResult2.regState);
                return new NetworkRegistrationState(1, 1, regStateFromHalRegState2, accessNetworkTechnologyFromRat2, i2, zIsEmergencyOnly2, getAvailableServices(regStateFromHalRegState2, 1, zIsEmergencyOnly2), convertHalCellIdentityToCellIdentity(voiceRegStateResult2.cellIdentity), voiceRegStateResult2.cssSupported, voiceRegStateResult2.roamingIndicator, voiceRegStateResult2.systemIsInPrl, voiceRegStateResult2.defaultRoamingIndicator);
            }
            return null;
        }

        protected NetworkRegistrationState createRegistrationStateFromDataRegState(Object obj) {
            if (obj instanceof DataRegStateResult) {
                DataRegStateResult dataRegStateResult = (DataRegStateResult) obj;
                int regStateFromHalRegState = getRegStateFromHalRegState(dataRegStateResult.regState);
                int accessNetworkTechnologyFromRat = getAccessNetworkTechnologyFromRat(dataRegStateResult.rat);
                int i = dataRegStateResult.reasonDataDenied;
                boolean zIsEmergencyOnly = isEmergencyOnly(dataRegStateResult.regState);
                return new NetworkRegistrationState(1, 2, regStateFromHalRegState, accessNetworkTechnologyFromRat, i, zIsEmergencyOnly, getAvailableServices(regStateFromHalRegState, 2, zIsEmergencyOnly), convertHalCellIdentityToCellIdentity(dataRegStateResult.cellIdentity), dataRegStateResult.maxDataCalls);
            }
            if (obj instanceof android.hardware.radio.V1_2.DataRegStateResult) {
                android.hardware.radio.V1_2.DataRegStateResult dataRegStateResult2 = (android.hardware.radio.V1_2.DataRegStateResult) obj;
                int regStateFromHalRegState2 = getRegStateFromHalRegState(dataRegStateResult2.regState);
                int accessNetworkTechnologyFromRat2 = getAccessNetworkTechnologyFromRat(dataRegStateResult2.rat);
                int i2 = dataRegStateResult2.reasonDataDenied;
                boolean zIsEmergencyOnly2 = isEmergencyOnly(dataRegStateResult2.regState);
                return new NetworkRegistrationState(1, 2, regStateFromHalRegState2, accessNetworkTechnologyFromRat2, i2, zIsEmergencyOnly2, getAvailableServices(regStateFromHalRegState2, 2, zIsEmergencyOnly2), convertHalCellIdentityToCellIdentity(dataRegStateResult2.cellIdentity), dataRegStateResult2.maxDataCalls);
            }
            return null;
        }

        protected CellIdentity convertHalCellIdentityToCellIdentity(android.hardware.radio.V1_0.CellIdentity cellIdentity) {
            CellIdentity cellIdentityGsm;
            if (cellIdentity == null) {
                return null;
            }
            switch (cellIdentity.cellInfoType) {
                case 1:
                    if (cellIdentity.cellIdentityGsm.size() != 1) {
                        return null;
                    }
                    CellIdentityGsm cellIdentityGsm2 = cellIdentity.cellIdentityGsm.get(0);
                    cellIdentityGsm = new android.telephony.CellIdentityGsm(cellIdentityGsm2.lac, cellIdentityGsm2.cid, cellIdentityGsm2.arfcn, cellIdentityGsm2.bsic, cellIdentityGsm2.mcc, cellIdentityGsm2.mnc, null, null);
                    break;
                    break;
                case 2:
                    if (cellIdentity.cellIdentityCdma.size() != 1) {
                        return null;
                    }
                    CellIdentityCdma cellIdentityCdma = cellIdentity.cellIdentityCdma.get(0);
                    return new android.telephony.CellIdentityCdma(cellIdentityCdma.networkId, cellIdentityCdma.systemId, cellIdentityCdma.baseStationId, cellIdentityCdma.longitude, cellIdentityCdma.latitude);
                case 3:
                    if (cellIdentity.cellIdentityLte.size() != 1) {
                        return null;
                    }
                    CellIdentityLte cellIdentityLte = cellIdentity.cellIdentityLte.get(0);
                    return new android.telephony.CellIdentityLte(cellIdentityLte.ci, cellIdentityLte.pci, cellIdentityLte.tac, cellIdentityLte.earfcn, KeepaliveStatus.INVALID_HANDLE, cellIdentityLte.mcc, cellIdentityLte.mnc, null, null);
                case 4:
                    if (cellIdentity.cellIdentityWcdma.size() != 1) {
                        return null;
                    }
                    CellIdentityWcdma cellIdentityWcdma = cellIdentity.cellIdentityWcdma.get(0);
                    cellIdentityGsm = new android.telephony.CellIdentityWcdma(cellIdentityWcdma.lac, cellIdentityWcdma.cid, cellIdentityWcdma.psc, cellIdentityWcdma.uarfcn, cellIdentityWcdma.mcc, cellIdentityWcdma.mnc, null, null);
                    break;
                    break;
                case 5:
                    if (cellIdentity.cellIdentityTdscdma.size() != 1) {
                        return null;
                    }
                    CellIdentityTdscdma cellIdentityTdscdma = cellIdentity.cellIdentityTdscdma.get(0);
                    return new android.telephony.CellIdentityTdscdma(cellIdentityTdscdma.mcc, cellIdentityTdscdma.mnc, cellIdentityTdscdma.lac, cellIdentityTdscdma.cid, cellIdentityTdscdma.cpid);
                default:
                    return null;
            }
            return cellIdentityGsm;
        }

        protected CellIdentity convertHalCellIdentityToCellIdentity(android.hardware.radio.V1_2.CellIdentity cellIdentity) {
            CellIdentity cellIdentityGsm;
            if (cellIdentity == null) {
                return null;
            }
            switch (cellIdentity.cellInfoType) {
                case 1:
                    if (cellIdentity.cellIdentityGsm.size() != 1) {
                        return null;
                    }
                    android.hardware.radio.V1_2.CellIdentityGsm cellIdentityGsm2 = cellIdentity.cellIdentityGsm.get(0);
                    cellIdentityGsm = new android.telephony.CellIdentityGsm(cellIdentityGsm2.base.lac, cellIdentityGsm2.base.cid, cellIdentityGsm2.base.arfcn, cellIdentityGsm2.base.bsic, cellIdentityGsm2.base.mcc, cellIdentityGsm2.base.mnc, cellIdentityGsm2.operatorNames.alphaLong, cellIdentityGsm2.operatorNames.alphaShort);
                    break;
                    break;
                case 2:
                    if (cellIdentity.cellIdentityCdma.size() != 1) {
                        return null;
                    }
                    android.hardware.radio.V1_2.CellIdentityCdma cellIdentityCdma = cellIdentity.cellIdentityCdma.get(0);
                    return new android.telephony.CellIdentityCdma(cellIdentityCdma.base.networkId, cellIdentityCdma.base.systemId, cellIdentityCdma.base.baseStationId, cellIdentityCdma.base.longitude, cellIdentityCdma.base.latitude, cellIdentityCdma.operatorNames.alphaLong, cellIdentityCdma.operatorNames.alphaShort);
                case 3:
                    if (cellIdentity.cellIdentityLte.size() != 1) {
                        return null;
                    }
                    android.hardware.radio.V1_2.CellIdentityLte cellIdentityLte = cellIdentity.cellIdentityLte.get(0);
                    return new android.telephony.CellIdentityLte(cellIdentityLte.base.ci, cellIdentityLte.base.pci, cellIdentityLte.base.tac, cellIdentityLte.base.earfcn, cellIdentityLte.bandwidth, cellIdentityLte.base.mcc, cellIdentityLte.base.mnc, cellIdentityLte.operatorNames.alphaLong, cellIdentityLte.operatorNames.alphaShort);
                case 4:
                    if (cellIdentity.cellIdentityWcdma.size() != 1) {
                        return null;
                    }
                    android.hardware.radio.V1_2.CellIdentityWcdma cellIdentityWcdma = cellIdentity.cellIdentityWcdma.get(0);
                    cellIdentityGsm = new android.telephony.CellIdentityWcdma(cellIdentityWcdma.base.lac, cellIdentityWcdma.base.cid, cellIdentityWcdma.base.psc, cellIdentityWcdma.base.uarfcn, cellIdentityWcdma.base.mcc, cellIdentityWcdma.base.mnc, cellIdentityWcdma.operatorNames.alphaLong, cellIdentityWcdma.operatorNames.alphaShort);
                    break;
                    break;
                case 5:
                    if (cellIdentity.cellIdentityTdscdma.size() != 1) {
                        return null;
                    }
                    android.hardware.radio.V1_2.CellIdentityTdscdma cellIdentityTdscdma = cellIdentity.cellIdentityTdscdma.get(0);
                    return new android.telephony.CellIdentityTdscdma(cellIdentityTdscdma.base.mcc, cellIdentityTdscdma.base.mnc, cellIdentityTdscdma.base.lac, cellIdentityTdscdma.base.cid, cellIdentityTdscdma.base.cpid, cellIdentityTdscdma.operatorNames.alphaLong, cellIdentityTdscdma.operatorNames.alphaShort);
                default:
                    return null;
            }
            return cellIdentityGsm;
        }

        public void getNetworkRegistrationState(int i, NetworkServiceCallback networkServiceCallback) {
            if (i == 1) {
                Message messageObtain = Message.obtain(this.mHandler, 1);
                this.mCallbackMap.put(messageObtain, networkServiceCallback);
                this.mPhone.mCi.getVoiceRegistrationState(messageObtain);
            } else if (i == 2) {
                Message messageObtain2 = Message.obtain(this.mHandler, 2);
                this.mCallbackMap.put(messageObtain2, networkServiceCallback);
                this.mPhone.mCi.getDataRegistrationState(messageObtain2);
            } else {
                CellularNetworkService.this.loge("getNetworkRegistrationState invalid domain " + i);
                networkServiceCallback.onGetNetworkRegistrationStateComplete(2, (NetworkRegistrationState) null);
            }
        }

        protected void onDestroy() {
            super.onDestroy();
            this.mCallbackMap.clear();
            this.mHandlerThread.quit();
            this.mPhone.mCi.unregisterForNetworkStateChanged(this.mHandler);
        }
    }

    protected NetworkService.NetworkServiceProvider createNetworkServiceProvider(int i) {
        if (!SubscriptionManager.isValidSlotIndex(i)) {
            loge("Tried to Cellular network service with invalid slotId " + i);
            return null;
        }
        return new CellularNetworkServiceProvider(i);
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }
}
