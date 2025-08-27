package com.mediatek.lbs.em2.utils;

import android.os.IHwBinder;
import android.os.RemoteException;
import android.util.Log;
import com.mediatek.lbs.em2.utils.DataCoder2;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import vendor.mediatek.hardware.lbs.V1_0.ILbs;
import vendor.mediatek.hardware.lbs.V1_0.ILbsCallback;

/* loaded from: classes.dex */
public class AgpsInterface {
    ILbs mLbsHidlClient;
    protected DataCoder2.DataCoderBuffer out = new DataCoder2.DataCoderBuffer(16384);
    protected DataCoder2.DataCoderBuffer in = new DataCoder2.DataCoderBuffer(16384);
    ArrayList<Byte> mData = new ArrayList<>();
    LbsHidlCallback mLbsHidlCallback = new LbsHidlCallback();
    LbsHidlDeathRecipient mLLbsHidlDeathRecipient = new LbsHidlDeathRecipient();

    public static ArrayList<Byte> convertByteArrayToArrayList(byte[] bArr, int i) {
        if (bArr == null) {
            return null;
        }
        if (i >= bArr.length) {
            i = bArr.length;
        }
        ArrayList<Byte> arrayList = new ArrayList<>();
        for (int i2 = 0; i2 < i; i2++) {
            arrayList.add(Byte.valueOf(bArr[i2]));
        }
        return arrayList;
    }

    public static void covertArrayListToByteArray(ArrayList<Byte> arrayList, byte[] bArr) {
        for (int i = 0; i < arrayList.size() && i < bArr.length; i++) {
            bArr[i] = arrayList.get(i).byteValue();
        }
    }

    class LbsHidlCallback extends ILbsCallback.Stub {
        LbsHidlCallback() {
        }

        @Override // vendor.mediatek.hardware.lbs.V1_0.ILbsCallback
        public boolean callbackToClient(ArrayList<Byte> arrayList) {
            AgpsInterface.this.mData = arrayList;
            return true;
        }
    }

    class LbsHidlDeathRecipient implements IHwBinder.DeathRecipient {
        LbsHidlDeathRecipient() {
        }

        public void serviceDied(long j) {
            AgpsInterface.log("serviceDied");
            AgpsInterface.this.mLbsHidlClient = null;
        }
    }

    private void doHidl(String str, byte[] bArr, int i) {
        try {
            if (this.mLbsHidlClient == null) {
                this.mLbsHidlClient = ILbs.getService(str);
                this.mLbsHidlClient.linkToDeath(this.mLLbsHidlDeathRecipient, 0L);
            }
            if (this.mLbsHidlClient.sendToServerWithCallback(this.mLbsHidlCallback, convertByteArrayToArrayList(bArr, i))) {
                covertArrayListToByteArray(this.mData, this.in.mBuff);
            } else {
                Arrays.fill(this.in.mBuff, (byte) 0);
            }
        } catch (RemoteException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void writeToHidl() {
        doHidl("AgpsInterface", this.out.mBuff, this.out.mOffset);
        this.out.flush();
        this.in.clear();
    }

    public AgpsInterface() throws IOException {
        checkVersion();
    }

    public void checkVersion() {
        try {
            try {
                connect();
                DataCoder2.putInt(this.out, 1);
                DataCoder2.putShort(this.out, (short) 1);
                DataCoder2.putShort(this.out, (short) 1);
                writeToHidl();
                short s = DataCoder2.getShort(this.in);
                short s2 = DataCoder2.getShort(this.in);
                if (s != 1) {
                    throw new IOException("app maj ver=1 is not equal to AGPSD's maj ver=" + ((int) s));
                }
                if (s2 < 1) {
                    throw new IOException("app min ver=1 is greater than AGPSD's min ver=" + ((int) s2));
                }
                DataCoder2.getByte(this.in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            close();
        }
    }

    public AgpsConfig getAgpsConfig() {
        AgpsConfig agpsConfig = new AgpsConfig();
        try {
            try {
                connect();
                DataCoder2.putInt(this.out, 100);
                writeToHidl();
                getAgpsConfigInt(100, agpsConfig);
                DataCoder2.getByte(this.in);
                return agpsConfig;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            close();
        }
    }

    private void getAgpsConfigInt(int i, AgpsConfig agpsConfig) throws IOException {
        AgpsSetting agpsSetting = agpsConfig.getAgpsSetting();
        agpsSetting.agpsEnable = DataCoder2.getBoolean(this.in);
        agpsSetting.agpsProtocol = DataCoder2.getInt(this.in);
        agpsSetting.gpevt = DataCoder2.getBoolean(this.in);
        CpSetting cpSetting = agpsConfig.getCpSetting();
        cpSetting.molrPosMethod = DataCoder2.getInt(this.in);
        cpSetting.externalAddrEnable = DataCoder2.getBoolean(this.in);
        cpSetting.externalAddr = DataCoder2.getString(this.in);
        cpSetting.mlcNumberEnable = DataCoder2.getBoolean(this.in);
        cpSetting.mlcNumber = DataCoder2.getString(this.in);
        cpSetting.cpAutoReset = DataCoder2.getBoolean(this.in);
        cpSetting.epcMolrLppPayloadEnable = DataCoder2.getBoolean(this.in);
        cpSetting.epcMolrLppPayload = DataCoder2.getBinary(this.in);
        UpSetting upSetting = agpsConfig.getUpSetting();
        GnssSetting gnssSetting = agpsConfig.getGnssSetting();
        upSetting.caEnable = DataCoder2.getBoolean(this.in);
        upSetting.niRequest = DataCoder2.getBoolean(this.in);
        upSetting.roaming = DataCoder2.getBoolean(this.in);
        upSetting.cdmaPreferred = DataCoder2.getInt(this.in);
        upSetting.prefMethod = DataCoder2.getInt(this.in);
        upSetting.suplVersion = DataCoder2.getInt(this.in);
        upSetting.tlsVersion = DataCoder2.getInt(this.in);
        upSetting.suplLog = DataCoder2.getBoolean(this.in);
        upSetting.msaEnable = DataCoder2.getBoolean(this.in);
        upSetting.msbEnable = DataCoder2.getBoolean(this.in);
        upSetting.ecidEnable = DataCoder2.getBoolean(this.in);
        upSetting.otdoaEnable = DataCoder2.getBoolean(this.in);
        upSetting.qopHacc = DataCoder2.getInt(this.in);
        upSetting.qopVacc = DataCoder2.getInt(this.in);
        upSetting.qopLocAge = DataCoder2.getInt(this.in);
        upSetting.qopDelay = DataCoder2.getInt(this.in);
        if (i >= 105) {
            upSetting.lppEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 106) {
            upSetting.certFromSdcard = DataCoder2.getBoolean(this.in);
        }
        if (i >= 107) {
            upSetting.autoProfileEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 108) {
            upSetting.ut2 = DataCoder2.getByte(this.in);
            upSetting.ut3 = DataCoder2.getByte(this.in);
        }
        if (i >= 109) {
            upSetting.apnEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 110) {
            upSetting.syncToslp = DataCoder2.getBoolean(this.in);
        }
        if (i >= 111) {
            upSetting.udpEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 112) {
            upSetting.autonomousEnable = DataCoder2.getBoolean(this.in);
            upSetting.afltEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 113) {
            upSetting.imsiEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 114) {
            gnssSetting.sib8sib16Enable = DataCoder2.getBoolean(this.in);
            gnssSetting.gpsSatelliteEnable = DataCoder2.getBoolean(this.in);
            gnssSetting.glonassSatelliteEnable = DataCoder2.getBoolean(this.in);
            gnssSetting.beidouSatelliteEnable = DataCoder2.getBoolean(this.in);
            gnssSetting.galileoSatelliteEnable = DataCoder2.getBoolean(this.in);
            gnssSetting.gpsSatelliteSupport = DataCoder2.getBoolean(this.in);
            gnssSetting.glonassSatelliteSupport = DataCoder2.getBoolean(this.in);
            gnssSetting.beidousSatelliteSupport = DataCoder2.getBoolean(this.in);
            gnssSetting.galileoSatelliteSupport = DataCoder2.getBoolean(this.in);
        }
        if (i >= 115) {
            upSetting.suplVerMinor = DataCoder2.getByte(this.in);
            upSetting.suplVerSerInd = DataCoder2.getByte(this.in);
        }
        if (i >= 116) {
            gnssSetting.aGlonassSatelliteEnable = DataCoder2.getBoolean(this.in);
        }
        SuplProfile curSuplProfile = agpsConfig.getCurSuplProfile();
        curSuplProfile.name = DataCoder2.getString(this.in);
        curSuplProfile.addr = DataCoder2.getString(this.in);
        curSuplProfile.port = DataCoder2.getInt(this.in);
        curSuplProfile.tls = DataCoder2.getBoolean(this.in);
        curSuplProfile.mccMnc = DataCoder2.getString(this.in);
        curSuplProfile.appId = DataCoder2.getString(this.in);
        curSuplProfile.providerId = DataCoder2.getString(this.in);
        curSuplProfile.defaultApn = DataCoder2.getString(this.in);
        curSuplProfile.optionalApn = DataCoder2.getString(this.in);
        curSuplProfile.optionalApn2 = DataCoder2.getString(this.in);
        curSuplProfile.addressType = DataCoder2.getString(this.in);
        if (i >= 117) {
            CdmaProfile cdmaProfile = agpsConfig.getCdmaProfile();
            cdmaProfile.name = DataCoder2.getString(this.in);
            cdmaProfile.mcpEnable = DataCoder2.getBoolean(this.in);
            cdmaProfile.mcpAddr = DataCoder2.getString(this.in);
            cdmaProfile.mcpPort = DataCoder2.getInt(this.in);
            cdmaProfile.pdeAddrValid = DataCoder2.getBoolean(this.in);
            cdmaProfile.pdeIpType = DataCoder2.getInt(this.in);
            cdmaProfile.pdeAddr = DataCoder2.getString(this.in);
            cdmaProfile.pdePort = DataCoder2.getInt(this.in);
            cdmaProfile.pdeUrlValid = DataCoder2.getBoolean(this.in);
            cdmaProfile.pdeUrlAddr = DataCoder2.getString(this.in);
        }
        if (i >= 118) {
            agpsSetting.e911GpsIconEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 119) {
            agpsSetting.e911OpenGpsEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 120) {
            gnssSetting.aGpsSatelliteEnable = DataCoder2.getBoolean(this.in);
            gnssSetting.aBeidouSatelliteEnable = DataCoder2.getBoolean(this.in);
            gnssSetting.aGalileoSatelliteEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 121) {
            upSetting.shaVersion = DataCoder2.getInt(this.in);
            upSetting.preferred2g3gCellAge = DataCoder2.getInt(this.in);
            upSetting.ut1 = DataCoder2.getByte(this.in);
            upSetting.noSensitiveLog = DataCoder2.getBoolean(this.in);
            upSetting.tlsReuseEnable = DataCoder2.getBoolean(this.in);
            upSetting.imsiCacheEnable = DataCoder2.getBoolean(this.in);
            upSetting.suplRawDataEnable = DataCoder2.getBoolean(this.in);
            upSetting.tc10Enable = DataCoder2.getBoolean(this.in);
            upSetting.tc10UseApn = DataCoder2.getBoolean(this.in);
            upSetting.tc10UseFwDns = DataCoder2.getBoolean(this.in);
            upSetting.allowNiForGpsOff = DataCoder2.getBoolean(this.in);
            upSetting.forceOtdoaAssistReq = DataCoder2.getBoolean(this.in);
            cpSetting.rejectNon911NilrEnable = DataCoder2.getBoolean(this.in);
            cpSetting.cp2gDisable = DataCoder2.getBoolean(this.in);
            cpSetting.cp3gDisable = DataCoder2.getBoolean(this.in);
            cpSetting.cp4gDisable = DataCoder2.getBoolean(this.in);
            agpsSetting.tc10IgnoreFwConfig = DataCoder2.getBoolean(this.in);
            agpsSetting.lppeHideWifiBtStatus = DataCoder2.getBoolean(this.in);
        }
        if (i >= 122) {
            agpsSetting.lppeNetworkLocationDisable = DataCoder2.getBoolean(this.in);
            cpSetting.cpLppeEnable = DataCoder2.getBoolean(this.in);
            upSetting.upLppeEnable = DataCoder2.getBoolean(this.in);
        }
        if (i >= 123) {
            cpSetting.cpLppeSupport = DataCoder2.getBoolean(this.in);
            gnssSetting.lppeSupport = DataCoder2.getBoolean(this.in);
        }
        if (i >= 124) {
            agpsSetting.agpsNvramEnable = DataCoder2.getBoolean(this.in);
            agpsSetting.lbsLogEnable = DataCoder2.getBoolean(this.in);
            agpsSetting.lppeCrowdSourceConfident = DataCoder2.getInt(this.in);
            upSetting.esuplApnMode = DataCoder2.getInt(this.in);
            upSetting.tcpKeepAlive = DataCoder2.getInt(this.in);
            upSetting.aospProfileEnable = DataCoder2.getBoolean(this.in);
            upSetting.bindNlpSettingToSupl = DataCoder2.getBoolean(this.in);
        }
    }

    public void setSuplProfile(SuplProfile suplProfile) {
        try {
            try {
                connect();
                DataCoder2.putInt(this.out, 219);
                DataCoder2.putString(this.out, suplProfile.name);
                DataCoder2.putString(this.out, suplProfile.addr);
                DataCoder2.putInt(this.out, suplProfile.port);
                DataCoder2.putBoolean(this.out, suplProfile.tls);
                DataCoder2.putString(this.out, suplProfile.mccMnc);
                DataCoder2.putString(this.out, suplProfile.appId);
                DataCoder2.putString(this.out, suplProfile.providerId);
                DataCoder2.putString(this.out, suplProfile.defaultApn);
                DataCoder2.putString(this.out, suplProfile.optionalApn);
                DataCoder2.putString(this.out, suplProfile.optionalApn2);
                DataCoder2.putString(this.out, suplProfile.addressType);
                writeToHidl();
                DataCoder2.getByte(this.in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            close();
        }
    }

    protected void connect() throws IOException {
    }

    protected void close() {
        this.in.clear();
    }

    protected static void log(String str) {
        Log.d("AgpsInterface [agps]", str);
    }
}
