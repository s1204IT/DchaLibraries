package com.android.internal.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.WorkSource;
import android.telephony.SignalStrength;

public interface IBatteryStats extends IInterface {
    long computeBatteryTimeRemaining() throws RemoteException;

    long computeChargeTimeRemaining() throws RemoteException;

    long getAwakeTimeBattery() throws RemoteException;

    long getAwakeTimePlugged() throws RemoteException;

    byte[] getStatistics() throws RemoteException;

    ParcelFileDescriptor getStatisticsStream() throws RemoteException;

    void noteBluetoothOff() throws RemoteException;

    void noteBluetoothOn() throws RemoteException;

    void noteBluetoothState(int i) throws RemoteException;

    void noteChangeWakelockFromSource(WorkSource workSource, int i, String str, String str2, int i2, WorkSource workSource2, int i3, String str3, String str4, int i4, boolean z) throws RemoteException;

    void noteConnectivityChanged(int i, String str) throws RemoteException;

    void noteEvent(int i, String str, int i2) throws RemoteException;

    void noteFlashlightOff() throws RemoteException;

    void noteFlashlightOn() throws RemoteException;

    void noteFullWifiLockAcquired(int i) throws RemoteException;

    void noteFullWifiLockAcquiredFromSource(WorkSource workSource) throws RemoteException;

    void noteFullWifiLockReleased(int i) throws RemoteException;

    void noteFullWifiLockReleasedFromSource(WorkSource workSource) throws RemoteException;

    void noteInteractive(boolean z) throws RemoteException;

    void noteJobFinish(String str, int i) throws RemoteException;

    void noteJobStart(String str, int i) throws RemoteException;

    void noteMobileRadioPowerState(int i, long j) throws RemoteException;

    void noteNetworkInterfaceType(String str, int i) throws RemoteException;

    void noteNetworkStatsEnabled() throws RemoteException;

    void notePhoneDataConnectionState(int i, boolean z) throws RemoteException;

    void notePhoneOff() throws RemoteException;

    void notePhoneOn() throws RemoteException;

    void notePhoneSignalStrength(SignalStrength signalStrength) throws RemoteException;

    void notePhoneState(int i) throws RemoteException;

    void noteResetAudio() throws RemoteException;

    void noteResetVideo() throws RemoteException;

    void noteScreenBrightness(int i) throws RemoteException;

    void noteScreenState(int i) throws RemoteException;

    void noteStartAudio(int i) throws RemoteException;

    void noteStartGps(int i) throws RemoteException;

    void noteStartSensor(int i, int i2) throws RemoteException;

    void noteStartVideo(int i) throws RemoteException;

    void noteStartWakelock(int i, int i2, String str, String str2, int i3, boolean z) throws RemoteException;

    void noteStartWakelockFromSource(WorkSource workSource, int i, String str, String str2, int i2, boolean z) throws RemoteException;

    void noteStopAudio(int i) throws RemoteException;

    void noteStopGps(int i) throws RemoteException;

    void noteStopSensor(int i, int i2) throws RemoteException;

    void noteStopVideo(int i) throws RemoteException;

    void noteStopWakelock(int i, int i2, String str, String str2, int i3) throws RemoteException;

    void noteStopWakelockFromSource(WorkSource workSource, int i, String str, String str2, int i2) throws RemoteException;

    void noteSyncFinish(String str, int i) throws RemoteException;

    void noteSyncStart(String str, int i) throws RemoteException;

    void noteUserActivity(int i, int i2) throws RemoteException;

    void noteVibratorOff(int i) throws RemoteException;

    void noteVibratorOn(int i, long j) throws RemoteException;

    void noteWifiBatchedScanStartedFromSource(WorkSource workSource, int i) throws RemoteException;

    void noteWifiBatchedScanStoppedFromSource(WorkSource workSource) throws RemoteException;

    void noteWifiMulticastDisabled(int i) throws RemoteException;

    void noteWifiMulticastDisabledFromSource(WorkSource workSource) throws RemoteException;

    void noteWifiMulticastEnabled(int i) throws RemoteException;

    void noteWifiMulticastEnabledFromSource(WorkSource workSource) throws RemoteException;

    void noteWifiOff() throws RemoteException;

    void noteWifiOn() throws RemoteException;

    void noteWifiRssiChanged(int i) throws RemoteException;

    void noteWifiRunning(WorkSource workSource) throws RemoteException;

    void noteWifiRunningChanged(WorkSource workSource, WorkSource workSource2) throws RemoteException;

    void noteWifiScanStarted(int i) throws RemoteException;

    void noteWifiScanStartedFromSource(WorkSource workSource) throws RemoteException;

    void noteWifiScanStopped(int i) throws RemoteException;

    void noteWifiScanStoppedFromSource(WorkSource workSource) throws RemoteException;

    void noteWifiState(int i, String str) throws RemoteException;

    void noteWifiStopped(WorkSource workSource) throws RemoteException;

    void noteWifiSupplicantStateChanged(int i, boolean z) throws RemoteException;

    void setBatteryState(int i, int i2, int i3, int i4, int i5, int i6) throws RemoteException;

    public static abstract class Stub extends Binder implements IBatteryStats {
        private static final String DESCRIPTOR = "com.android.internal.app.IBatteryStats";
        static final int TRANSACTION_computeBatteryTimeRemaining = 11;
        static final int TRANSACTION_computeChargeTimeRemaining = 12;
        static final int TRANSACTION_getAwakeTimeBattery = 68;
        static final int TRANSACTION_getAwakeTimePlugged = 69;
        static final int TRANSACTION_getStatistics = 9;
        static final int TRANSACTION_getStatisticsStream = 10;
        static final int TRANSACTION_noteBluetoothOff = 49;
        static final int TRANSACTION_noteBluetoothOn = 48;
        static final int TRANSACTION_noteBluetoothState = 50;
        static final int TRANSACTION_noteChangeWakelockFromSource = 21;
        static final int TRANSACTION_noteConnectivityChanged = 33;
        static final int TRANSACTION_noteEvent = 13;
        static final int TRANSACTION_noteFlashlightOff = 26;
        static final int TRANSACTION_noteFlashlightOn = 25;
        static final int TRANSACTION_noteFullWifiLockAcquired = 51;
        static final int TRANSACTION_noteFullWifiLockAcquiredFromSource = 57;
        static final int TRANSACTION_noteFullWifiLockReleased = 52;
        static final int TRANSACTION_noteFullWifiLockReleasedFromSource = 58;
        static final int TRANSACTION_noteInteractive = 32;
        static final int TRANSACTION_noteJobFinish = 17;
        static final int TRANSACTION_noteJobStart = 16;
        static final int TRANSACTION_noteMobileRadioPowerState = 34;
        static final int TRANSACTION_noteNetworkInterfaceType = 65;
        static final int TRANSACTION_noteNetworkStatsEnabled = 66;
        static final int TRANSACTION_notePhoneDataConnectionState = 38;
        static final int TRANSACTION_notePhoneOff = 36;
        static final int TRANSACTION_notePhoneOn = 35;
        static final int TRANSACTION_notePhoneSignalStrength = 37;
        static final int TRANSACTION_notePhoneState = 39;
        static final int TRANSACTION_noteResetAudio = 8;
        static final int TRANSACTION_noteResetVideo = 7;
        static final int TRANSACTION_noteScreenBrightness = 30;
        static final int TRANSACTION_noteScreenState = 29;
        static final int TRANSACTION_noteStartAudio = 5;
        static final int TRANSACTION_noteStartGps = 27;
        static final int TRANSACTION_noteStartSensor = 1;
        static final int TRANSACTION_noteStartVideo = 3;
        static final int TRANSACTION_noteStartWakelock = 18;
        static final int TRANSACTION_noteStartWakelockFromSource = 20;
        static final int TRANSACTION_noteStopAudio = 6;
        static final int TRANSACTION_noteStopGps = 28;
        static final int TRANSACTION_noteStopSensor = 2;
        static final int TRANSACTION_noteStopVideo = 4;
        static final int TRANSACTION_noteStopWakelock = 19;
        static final int TRANSACTION_noteStopWakelockFromSource = 22;
        static final int TRANSACTION_noteSyncFinish = 15;
        static final int TRANSACTION_noteSyncStart = 14;
        static final int TRANSACTION_noteUserActivity = 31;
        static final int TRANSACTION_noteVibratorOff = 24;
        static final int TRANSACTION_noteVibratorOn = 23;
        static final int TRANSACTION_noteWifiBatchedScanStartedFromSource = 61;
        static final int TRANSACTION_noteWifiBatchedScanStoppedFromSource = 62;
        static final int TRANSACTION_noteWifiMulticastDisabled = 56;
        static final int TRANSACTION_noteWifiMulticastDisabledFromSource = 64;
        static final int TRANSACTION_noteWifiMulticastEnabled = 55;
        static final int TRANSACTION_noteWifiMulticastEnabledFromSource = 63;
        static final int TRANSACTION_noteWifiOff = 41;
        static final int TRANSACTION_noteWifiOn = 40;
        static final int TRANSACTION_noteWifiRssiChanged = 47;
        static final int TRANSACTION_noteWifiRunning = 42;
        static final int TRANSACTION_noteWifiRunningChanged = 43;
        static final int TRANSACTION_noteWifiScanStarted = 53;
        static final int TRANSACTION_noteWifiScanStartedFromSource = 59;
        static final int TRANSACTION_noteWifiScanStopped = 54;
        static final int TRANSACTION_noteWifiScanStoppedFromSource = 60;
        static final int TRANSACTION_noteWifiState = 45;
        static final int TRANSACTION_noteWifiStopped = 44;
        static final int TRANSACTION_noteWifiSupplicantStateChanged = 46;
        static final int TRANSACTION_setBatteryState = 67;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBatteryStats asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IBatteryStats)) {
                return (IBatteryStats) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            WorkSource _arg0;
            WorkSource _arg02;
            WorkSource _arg03;
            WorkSource _arg04;
            WorkSource _arg05;
            WorkSource _arg06;
            WorkSource _arg07;
            WorkSource _arg08;
            WorkSource _arg09;
            WorkSource _arg010;
            WorkSource _arg1;
            WorkSource _arg011;
            SignalStrength _arg012;
            WorkSource _arg013;
            WorkSource _arg014;
            WorkSource _arg5;
            WorkSource _arg015;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg016 = data.readInt();
                    int _arg12 = data.readInt();
                    noteStartSensor(_arg016, _arg12);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    int _arg13 = data.readInt();
                    noteStopSensor(_arg017, _arg13);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg018 = data.readInt();
                    noteStartVideo(_arg018);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    noteStopVideo(_arg019);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg020 = data.readInt();
                    noteStartAudio(_arg020);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg021 = data.readInt();
                    noteStopAudio(_arg021);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    noteResetVideo();
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    noteResetAudio();
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _result = getStatistics();
                    reply.writeNoException();
                    reply.writeByteArray(_result);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    ParcelFileDescriptor _result2 = getStatisticsStream();
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    long _result3 = computeBatteryTimeRemaining();
                    reply.writeNoException();
                    reply.writeLong(_result3);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    long _result4 = computeChargeTimeRemaining();
                    reply.writeNoException();
                    reply.writeLong(_result4);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg022 = data.readInt();
                    String _arg14 = data.readString();
                    int _arg2 = data.readInt();
                    noteEvent(_arg022, _arg14, _arg2);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg023 = data.readString();
                    int _arg15 = data.readInt();
                    noteSyncStart(_arg023, _arg15);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg024 = data.readString();
                    int _arg16 = data.readInt();
                    noteSyncFinish(_arg024, _arg16);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg025 = data.readString();
                    int _arg17 = data.readInt();
                    noteJobStart(_arg025, _arg17);
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg026 = data.readString();
                    int _arg18 = data.readInt();
                    noteJobFinish(_arg026, _arg18);
                    reply.writeNoException();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg027 = data.readInt();
                    int _arg19 = data.readInt();
                    String _arg22 = data.readString();
                    String _arg3 = data.readString();
                    int _arg4 = data.readInt();
                    boolean _arg52 = data.readInt() != 0;
                    noteStartWakelock(_arg027, _arg19, _arg22, _arg3, _arg4, _arg52);
                    reply.writeNoException();
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg028 = data.readInt();
                    int _arg110 = data.readInt();
                    String _arg23 = data.readString();
                    String _arg32 = data.readString();
                    int _arg42 = data.readInt();
                    noteStopWakelock(_arg028, _arg110, _arg23, _arg32, _arg42);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg015 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg015 = null;
                    }
                    int _arg111 = data.readInt();
                    String _arg24 = data.readString();
                    String _arg33 = data.readString();
                    int _arg43 = data.readInt();
                    boolean _arg53 = data.readInt() != 0;
                    noteStartWakelockFromSource(_arg015, _arg111, _arg24, _arg33, _arg43, _arg53);
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg014 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg014 = null;
                    }
                    int _arg112 = data.readInt();
                    String _arg25 = data.readString();
                    String _arg34 = data.readString();
                    int _arg44 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg5 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg5 = null;
                    }
                    int _arg6 = data.readInt();
                    String _arg7 = data.readString();
                    String _arg8 = data.readString();
                    int _arg9 = data.readInt();
                    boolean _arg10 = data.readInt() != 0;
                    noteChangeWakelockFromSource(_arg014, _arg112, _arg25, _arg34, _arg44, _arg5, _arg6, _arg7, _arg8, _arg9, _arg10);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg013 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg013 = null;
                    }
                    int _arg113 = data.readInt();
                    String _arg26 = data.readString();
                    String _arg35 = data.readString();
                    int _arg45 = data.readInt();
                    noteStopWakelockFromSource(_arg013, _arg113, _arg26, _arg35, _arg45);
                    reply.writeNoException();
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg029 = data.readInt();
                    long _arg114 = data.readLong();
                    noteVibratorOn(_arg029, _arg114);
                    reply.writeNoException();
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg030 = data.readInt();
                    noteVibratorOff(_arg030);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    noteFlashlightOn();
                    reply.writeNoException();
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    noteFlashlightOff();
                    reply.writeNoException();
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg031 = data.readInt();
                    noteStartGps(_arg031);
                    reply.writeNoException();
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg032 = data.readInt();
                    noteStopGps(_arg032);
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg033 = data.readInt();
                    noteScreenState(_arg033);
                    reply.writeNoException();
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg034 = data.readInt();
                    noteScreenBrightness(_arg034);
                    reply.writeNoException();
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg035 = data.readInt();
                    int _arg115 = data.readInt();
                    noteUserActivity(_arg035, _arg115);
                    reply.writeNoException();
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg036 = data.readInt() != 0;
                    noteInteractive(_arg036);
                    reply.writeNoException();
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg037 = data.readInt();
                    String _arg116 = data.readString();
                    noteConnectivityChanged(_arg037, _arg116);
                    reply.writeNoException();
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg038 = data.readInt();
                    long _arg117 = data.readLong();
                    noteMobileRadioPowerState(_arg038, _arg117);
                    reply.writeNoException();
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    notePhoneOn();
                    reply.writeNoException();
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    notePhoneOff();
                    reply.writeNoException();
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg012 = SignalStrength.CREATOR.createFromParcel(data);
                    } else {
                        _arg012 = null;
                    }
                    notePhoneSignalStrength(_arg012);
                    reply.writeNoException();
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg039 = data.readInt();
                    boolean _arg118 = data.readInt() != 0;
                    notePhoneDataConnectionState(_arg039, _arg118);
                    reply.writeNoException();
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg040 = data.readInt();
                    notePhoneState(_arg040);
                    reply.writeNoException();
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    noteWifiOn();
                    reply.writeNoException();
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    noteWifiOff();
                    reply.writeNoException();
                    return true;
                case 42:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg011 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg011 = null;
                    }
                    noteWifiRunning(_arg011);
                    reply.writeNoException();
                    return true;
                case 43:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg010 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg010 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg1 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    noteWifiRunningChanged(_arg010, _arg1);
                    reply.writeNoException();
                    return true;
                case 44:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg09 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg09 = null;
                    }
                    noteWifiStopped(_arg09);
                    reply.writeNoException();
                    return true;
                case 45:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg041 = data.readInt();
                    String _arg119 = data.readString();
                    noteWifiState(_arg041, _arg119);
                    reply.writeNoException();
                    return true;
                case 46:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg042 = data.readInt();
                    boolean _arg120 = data.readInt() != 0;
                    noteWifiSupplicantStateChanged(_arg042, _arg120);
                    reply.writeNoException();
                    return true;
                case 47:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg043 = data.readInt();
                    noteWifiRssiChanged(_arg043);
                    reply.writeNoException();
                    return true;
                case 48:
                    data.enforceInterface(DESCRIPTOR);
                    noteBluetoothOn();
                    reply.writeNoException();
                    return true;
                case 49:
                    data.enforceInterface(DESCRIPTOR);
                    noteBluetoothOff();
                    reply.writeNoException();
                    return true;
                case 50:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg044 = data.readInt();
                    noteBluetoothState(_arg044);
                    reply.writeNoException();
                    return true;
                case 51:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg045 = data.readInt();
                    noteFullWifiLockAcquired(_arg045);
                    reply.writeNoException();
                    return true;
                case 52:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg046 = data.readInt();
                    noteFullWifiLockReleased(_arg046);
                    reply.writeNoException();
                    return true;
                case 53:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg047 = data.readInt();
                    noteWifiScanStarted(_arg047);
                    reply.writeNoException();
                    return true;
                case 54:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg048 = data.readInt();
                    noteWifiScanStopped(_arg048);
                    reply.writeNoException();
                    return true;
                case 55:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg049 = data.readInt();
                    noteWifiMulticastEnabled(_arg049);
                    reply.writeNoException();
                    return true;
                case 56:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg050 = data.readInt();
                    noteWifiMulticastDisabled(_arg050);
                    reply.writeNoException();
                    return true;
                case 57:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg08 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg08 = null;
                    }
                    noteFullWifiLockAcquiredFromSource(_arg08);
                    reply.writeNoException();
                    return true;
                case 58:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg07 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg07 = null;
                    }
                    noteFullWifiLockReleasedFromSource(_arg07);
                    reply.writeNoException();
                    return true;
                case 59:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg06 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg06 = null;
                    }
                    noteWifiScanStartedFromSource(_arg06);
                    reply.writeNoException();
                    return true;
                case 60:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg05 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg05 = null;
                    }
                    noteWifiScanStoppedFromSource(_arg05);
                    reply.writeNoException();
                    return true;
                case 61:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg04 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    int _arg121 = data.readInt();
                    noteWifiBatchedScanStartedFromSource(_arg04, _arg121);
                    reply.writeNoException();
                    return true;
                case 62:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    noteWifiBatchedScanStoppedFromSource(_arg03);
                    reply.writeNoException();
                    return true;
                case 63:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    noteWifiMulticastEnabledFromSource(_arg02);
                    reply.writeNoException();
                    return true;
                case 64:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    noteWifiMulticastDisabledFromSource(_arg0);
                    reply.writeNoException();
                    return true;
                case 65:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg051 = data.readString();
                    int _arg122 = data.readInt();
                    noteNetworkInterfaceType(_arg051, _arg122);
                    reply.writeNoException();
                    return true;
                case 66:
                    data.enforceInterface(DESCRIPTOR);
                    noteNetworkStatsEnabled();
                    reply.writeNoException();
                    return true;
                case 67:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg052 = data.readInt();
                    int _arg123 = data.readInt();
                    int _arg27 = data.readInt();
                    int _arg36 = data.readInt();
                    int _arg46 = data.readInt();
                    int _arg54 = data.readInt();
                    setBatteryState(_arg052, _arg123, _arg27, _arg36, _arg46, _arg54);
                    reply.writeNoException();
                    return true;
                case 68:
                    data.enforceInterface(DESCRIPTOR);
                    long _result5 = getAwakeTimeBattery();
                    reply.writeNoException();
                    reply.writeLong(_result5);
                    return true;
                case 69:
                    data.enforceInterface(DESCRIPTOR);
                    long _result6 = getAwakeTimePlugged();
                    reply.writeNoException();
                    reply.writeLong(_result6);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IBatteryStats {
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
            public void noteStartSensor(int uid, int sensor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(sensor);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStopSensor(int uid, int sensor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(sensor);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStartVideo(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStopVideo(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStartAudio(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStopAudio(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteResetVideo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteResetAudio() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getStatistics() throws RemoteException {
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
            public ParcelFileDescriptor getStatisticsStream() throws RemoteException {
                ParcelFileDescriptor _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long computeBatteryTimeRemaining() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long computeChargeTimeRemaining() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteEvent(int code, String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteSyncStart(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteSyncFinish(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteJobStart(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteJobFinish(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStartWakelock(int uid, int pid, String name, String historyName, int type, boolean unimportantForLogging) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(pid);
                    _data.writeString(name);
                    _data.writeString(historyName);
                    _data.writeInt(type);
                    _data.writeInt(unimportantForLogging ? 1 : 0);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStopWakelock(int uid, int pid, String name, String historyName, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(pid);
                    _data.writeString(name);
                    _data.writeString(historyName);
                    _data.writeInt(type);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStartWakelockFromSource(WorkSource ws, int pid, String name, String historyName, int type, boolean unimportantForLogging) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(pid);
                    _data.writeString(name);
                    _data.writeString(historyName);
                    _data.writeInt(type);
                    _data.writeInt(unimportantForLogging ? 1 : 0);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteChangeWakelockFromSource(WorkSource ws, int pid, String name, String histyoryName, int type, WorkSource newWs, int newPid, String newName, String newHistoryName, int newType, boolean newUnimportantForLogging) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(pid);
                    _data.writeString(name);
                    _data.writeString(histyoryName);
                    _data.writeInt(type);
                    if (newWs != null) {
                        _data.writeInt(1);
                        newWs.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(newPid);
                    _data.writeString(newName);
                    _data.writeString(newHistoryName);
                    _data.writeInt(newType);
                    _data.writeInt(newUnimportantForLogging ? 1 : 0);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStopWakelockFromSource(WorkSource ws, int pid, String name, String historyName, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(pid);
                    _data.writeString(name);
                    _data.writeString(historyName);
                    _data.writeInt(type);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteVibratorOn(int uid, long durationMillis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeLong(durationMillis);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteVibratorOff(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteFlashlightOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteFlashlightOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStartGps(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteStopGps(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteScreenState(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteScreenBrightness(int brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(brightness);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteUserActivity(int uid, int event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(event);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteInteractive(boolean interactive) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(interactive ? 1 : 0);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteConnectivityChanged(int type, String extra) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(extra);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteMobileRadioPowerState(int powerState, long timestampNs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(powerState);
                    _data.writeLong(timestampNs);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notePhoneOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notePhoneOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notePhoneSignalStrength(SignalStrength signalStrength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (signalStrength != null) {
                        _data.writeInt(1);
                        signalStrength.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notePhoneDataConnectionState(int dataType, boolean hasData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(dataType);
                    _data.writeInt(hasData ? 1 : 0);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notePhoneState(int phoneState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneState);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiRunning(WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiRunningChanged(WorkSource oldWs, WorkSource newWs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oldWs != null) {
                        _data.writeInt(1);
                        oldWs.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (newWs != null) {
                        _data.writeInt(1);
                        newWs.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiStopped(WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiState(int wifiState, String accessPoint) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(wifiState);
                    _data.writeString(accessPoint);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiSupplicantStateChanged(int supplState, boolean failedAuth) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(supplState);
                    _data.writeInt(failedAuth ? 1 : 0);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiRssiChanged(int newRssi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(newRssi);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteBluetoothOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteBluetoothOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteBluetoothState(int bluetoothState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(bluetoothState);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteFullWifiLockAcquired(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteFullWifiLockReleased(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiScanStarted(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiScanStopped(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiMulticastEnabled(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiMulticastDisabled(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteFullWifiLockAcquiredFromSource(WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteFullWifiLockReleasedFromSource(WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiScanStartedFromSource(WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiScanStoppedFromSource(WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiBatchedScanStartedFromSource(WorkSource ws, int csph) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(csph);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiBatchedScanStoppedFromSource(WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiMulticastEnabledFromSource(WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteWifiMulticastDisabledFromSource(WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteNetworkInterfaceType(String iface, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(type);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void noteNetworkStatsEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setBatteryState(int status, int health, int plugType, int level, int temp, int volt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeInt(health);
                    _data.writeInt(plugType);
                    _data.writeInt(level);
                    _data.writeInt(temp);
                    _data.writeInt(volt);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getAwakeTimeBattery() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getAwakeTimePlugged() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
