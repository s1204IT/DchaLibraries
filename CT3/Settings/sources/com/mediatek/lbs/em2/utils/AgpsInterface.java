package com.mediatek.lbs.em2.utils;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class AgpsInterface {
    protected LocalSocket client;
    protected DataInputStream in;
    protected BufferedOutputStream out;

    public AgpsInterface() throws IOException {
        checkVersion();
    }

    public void checkVersion() {
        try {
            try {
                connect();
                DataCoder.putInt(this.out, 1);
                DataCoder.putShort(this.out, (short) 1);
                DataCoder.putShort(this.out, (short) 1);
                this.out.flush();
                short majorVersion = DataCoder.getShort(this.in);
                short minorVersion = DataCoder.getShort(this.in);
                if (majorVersion != 1) {
                    throw new IOException("app maj ver=1 is not equal to AGPSD's maj ver=" + ((int) majorVersion));
                }
                if (minorVersion < 1) {
                    throw new IOException("app min ver=1 is greater than AGPSD's min ver=" + ((int) minorVersion));
                }
                DataCoder.getByte(this.in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            close();
        }
    }

    public AgpsConfig getAgpsConfig() {
        AgpsConfig config = new AgpsConfig();
        try {
            try {
                connect();
                DataCoder.putInt(this.out, 100);
                this.out.flush();
                getAgpsConfigInt(100, config);
                DataCoder.getByte(this.in);
                return config;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            close();
        }
    }

    private void getAgpsConfigInt(int cmd, AgpsConfig config) throws IOException {
        AgpsSetting agpsSetting = config.getAgpsSetting();
        agpsSetting.agpsEnable = DataCoder.getBoolean(this.in);
        agpsSetting.agpsProtocol = DataCoder.getInt(this.in);
        agpsSetting.gpevt = DataCoder.getBoolean(this.in);
        CpSetting cpSetting = config.getCpSetting();
        cpSetting.molrPosMethod = DataCoder.getInt(this.in);
        cpSetting.externalAddrEnable = DataCoder.getBoolean(this.in);
        cpSetting.externalAddr = DataCoder.getString(this.in);
        cpSetting.mlcNumberEnable = DataCoder.getBoolean(this.in);
        cpSetting.mlcNumber = DataCoder.getString(this.in);
        cpSetting.cpAutoReset = DataCoder.getBoolean(this.in);
        cpSetting.epcMolrLppPayloadEnable = DataCoder.getBoolean(this.in);
        cpSetting.epcMolrLppPayload = DataCoder.getBinary(this.in);
        UpSetting upSetting = config.getUpSetting();
        GnssSetting gnssSetting = config.getGnssSetting();
        upSetting.caEnable = DataCoder.getBoolean(this.in);
        upSetting.niRequest = DataCoder.getBoolean(this.in);
        upSetting.roaming = DataCoder.getBoolean(this.in);
        upSetting.cdmaPreferred = DataCoder.getInt(this.in);
        upSetting.prefMethod = DataCoder.getInt(this.in);
        upSetting.suplVersion = DataCoder.getInt(this.in);
        upSetting.tlsVersion = DataCoder.getInt(this.in);
        upSetting.suplLog = DataCoder.getBoolean(this.in);
        upSetting.msaEnable = DataCoder.getBoolean(this.in);
        upSetting.msbEnable = DataCoder.getBoolean(this.in);
        upSetting.ecidEnable = DataCoder.getBoolean(this.in);
        upSetting.otdoaEnable = DataCoder.getBoolean(this.in);
        upSetting.qopHacc = DataCoder.getInt(this.in);
        upSetting.qopVacc = DataCoder.getInt(this.in);
        upSetting.qopLocAge = DataCoder.getInt(this.in);
        upSetting.qopDelay = DataCoder.getInt(this.in);
        if (cmd >= 105) {
            upSetting.lppEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 106) {
            upSetting.certFromSdcard = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 107) {
            upSetting.autoProfileEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 108) {
            upSetting.ut2 = DataCoder.getByte(this.in);
            upSetting.ut3 = DataCoder.getByte(this.in);
        }
        if (cmd >= 109) {
            upSetting.apnEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 110) {
            upSetting.syncToslp = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 111) {
            upSetting.udpEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 112) {
            upSetting.autonomousEnable = DataCoder.getBoolean(this.in);
            upSetting.afltEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 113) {
            upSetting.imsiEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 114) {
            gnssSetting.sib8sib16Enable = DataCoder.getBoolean(this.in);
            gnssSetting.gpsSatelliteEnable = DataCoder.getBoolean(this.in);
            gnssSetting.glonassSatelliteEnable = DataCoder.getBoolean(this.in);
            gnssSetting.beidouSatelliteEnable = DataCoder.getBoolean(this.in);
            gnssSetting.galileoSatelliteEnable = DataCoder.getBoolean(this.in);
            gnssSetting.gpsSatelliteSupport = DataCoder.getBoolean(this.in);
            gnssSetting.glonassSatelliteSupport = DataCoder.getBoolean(this.in);
            gnssSetting.beidousSatelliteSupport = DataCoder.getBoolean(this.in);
            gnssSetting.galileoSatelliteSupport = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 115) {
            upSetting.suplVerMinor = DataCoder.getByte(this.in);
            upSetting.suplVerSerInd = DataCoder.getByte(this.in);
        }
        if (cmd >= 116) {
            gnssSetting.aGlonassSatelliteEnable = DataCoder.getBoolean(this.in);
        }
        SuplProfile suplProfile = config.getCurSuplProfile();
        suplProfile.name = DataCoder.getString(this.in);
        suplProfile.addr = DataCoder.getString(this.in);
        suplProfile.port = DataCoder.getInt(this.in);
        suplProfile.tls = DataCoder.getBoolean(this.in);
        suplProfile.mccMnc = DataCoder.getString(this.in);
        suplProfile.appId = DataCoder.getString(this.in);
        suplProfile.providerId = DataCoder.getString(this.in);
        suplProfile.defaultApn = DataCoder.getString(this.in);
        suplProfile.optionalApn = DataCoder.getString(this.in);
        suplProfile.optionalApn2 = DataCoder.getString(this.in);
        suplProfile.addressType = DataCoder.getString(this.in);
        if (cmd >= 117) {
            CdmaProfile cdmaProfile = config.getCdmaProfile();
            cdmaProfile.name = DataCoder.getString(this.in);
            cdmaProfile.mcpEnable = DataCoder.getBoolean(this.in);
            cdmaProfile.mcpAddr = DataCoder.getString(this.in);
            cdmaProfile.mcpPort = DataCoder.getInt(this.in);
            cdmaProfile.pdeAddrValid = DataCoder.getBoolean(this.in);
            cdmaProfile.pdeIpType = DataCoder.getInt(this.in);
            cdmaProfile.pdeAddr = DataCoder.getString(this.in);
            cdmaProfile.pdePort = DataCoder.getInt(this.in);
            cdmaProfile.pdeUrlValid = DataCoder.getBoolean(this.in);
            cdmaProfile.pdeUrlAddr = DataCoder.getString(this.in);
        }
        if (cmd >= 118) {
            agpsSetting.e911GpsIconEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 119) {
            agpsSetting.e911OpenGpsEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 120) {
            gnssSetting.aGpsSatelliteEnable = DataCoder.getBoolean(this.in);
            gnssSetting.aBeidouSatelliteEnable = DataCoder.getBoolean(this.in);
            gnssSetting.aGalileoSatelliteEnable = DataCoder.getBoolean(this.in);
        }
    }

    public void setSuplProfile(SuplProfile profile) {
        try {
            try {
                connect();
                DataCoder.putInt(this.out, 219);
                DataCoder.putString(this.out, profile.name);
                DataCoder.putString(this.out, profile.addr);
                DataCoder.putInt(this.out, profile.port);
                DataCoder.putBoolean(this.out, profile.tls);
                DataCoder.putString(this.out, profile.mccMnc);
                DataCoder.putString(this.out, profile.appId);
                DataCoder.putString(this.out, profile.providerId);
                DataCoder.putString(this.out, profile.defaultApn);
                DataCoder.putString(this.out, profile.optionalApn);
                DataCoder.putString(this.out, profile.optionalApn2);
                DataCoder.putString(this.out, profile.addressType);
                this.out.flush();
                DataCoder.getByte(this.in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            close();
        }
    }

    protected void connect() throws IOException {
        if (this.client != null) {
            this.client.close();
        }
        this.client = new LocalSocket();
        this.client.connect(new LocalSocketAddress("agpsd2", LocalSocketAddress.Namespace.RESERVED));
        this.client.setSoTimeout(3000);
        this.out = new BufferedOutputStream(this.client.getOutputStream());
        this.in = new DataInputStream(this.client.getInputStream());
    }

    protected void close() {
        try {
            if (this.client != null) {
                this.client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
