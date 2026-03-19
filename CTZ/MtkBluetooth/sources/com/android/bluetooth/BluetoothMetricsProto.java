package com.android.bluetooth;

import com.android.bluetooth.avrcpcontroller.AvrcpControllerService;
import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLiteOrBuilder;
import com.google.protobuf.Parser;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public final class BluetoothMetricsProto {

    public interface A2DPSessionOrBuilder extends MessageLiteOrBuilder {
        long getAudioDurationMillis();

        int getBufferOverrunsMaxCount();

        int getBufferOverrunsTotal();

        float getBufferUnderrunsAverage();

        int getBufferUnderrunsCount();

        int getMediaTimerAvgMillis();

        int getMediaTimerMaxMillis();

        int getMediaTimerMinMillis();

        boolean hasAudioDurationMillis();

        boolean hasBufferOverrunsMaxCount();

        boolean hasBufferOverrunsTotal();

        boolean hasBufferUnderrunsAverage();

        boolean hasBufferUnderrunsCount();

        boolean hasMediaTimerAvgMillis();

        boolean hasMediaTimerMaxMillis();

        boolean hasMediaTimerMinMillis();
    }

    public interface BluetoothLogOrBuilder extends MessageLiteOrBuilder {
        HeadsetProfileConnectionStats getHeadsetProfileConnectionStats(int i);

        int getHeadsetProfileConnectionStatsCount();

        List<HeadsetProfileConnectionStats> getHeadsetProfileConnectionStatsList();

        long getNumBluetoothSession();

        int getNumBondedDevices();

        long getNumPairEvent();

        long getNumScanEvent();

        long getNumWakeEvent();

        PairEvent getPairEvent(int i);

        int getPairEventCount();

        List<PairEvent> getPairEventList();

        ProfileConnectionStats getProfileConnectionStats(int i);

        int getProfileConnectionStatsCount();

        List<ProfileConnectionStats> getProfileConnectionStatsList();

        ScanEvent getScanEvent(int i);

        int getScanEventCount();

        List<ScanEvent> getScanEventList();

        BluetoothSession getSession(int i);

        int getSessionCount();

        List<BluetoothSession> getSessionList();

        WakeEvent getWakeEvent(int i);

        int getWakeEventCount();

        List<WakeEvent> getWakeEventList();

        boolean hasNumBluetoothSession();

        boolean hasNumBondedDevices();

        boolean hasNumPairEvent();

        boolean hasNumScanEvent();

        boolean hasNumWakeEvent();
    }

    public interface BluetoothSessionOrBuilder extends MessageLiteOrBuilder {
        A2DPSession getA2DpSession();

        BluetoothSession.ConnectionTechnologyType getConnectionTechnologyType();

        DeviceInfo getDeviceConnectedTo();

        @Deprecated
        String getDisconnectReason();

        @Deprecated
        ByteString getDisconnectReasonBytes();

        BluetoothSession.DisconnectReasonType getDisconnectReasonType();

        RFCommSession getRfcommSession();

        long getSessionDurationSec();

        boolean hasA2DpSession();

        boolean hasConnectionTechnologyType();

        boolean hasDeviceConnectedTo();

        @Deprecated
        boolean hasDisconnectReason();

        boolean hasDisconnectReasonType();

        boolean hasRfcommSession();

        boolean hasSessionDurationSec();
    }

    public interface DeviceInfoOrBuilder extends MessageLiteOrBuilder {
        int getDeviceClass();

        DeviceInfo.DeviceType getDeviceType();

        boolean hasDeviceClass();

        boolean hasDeviceType();
    }

    public interface HeadsetProfileConnectionStatsOrBuilder extends MessageLiteOrBuilder {
        HeadsetProfileType getHeadsetProfileType();

        int getNumTimesConnected();

        boolean hasHeadsetProfileType();

        boolean hasNumTimesConnected();
    }

    public interface PairEventOrBuilder extends MessageLiteOrBuilder {
        DeviceInfo getDevicePairedWith();

        int getDisconnectReason();

        long getEventTimeMillis();

        boolean hasDevicePairedWith();

        boolean hasDisconnectReason();

        boolean hasEventTimeMillis();
    }

    public interface ProfileConnectionStatsOrBuilder extends MessageLiteOrBuilder {
        int getNumTimesConnected();

        ProfileId getProfileId();

        boolean hasNumTimesConnected();

        boolean hasProfileId();
    }

    public interface RFCommSessionOrBuilder extends MessageLiteOrBuilder {
        int getRxBytes();

        int getTxBytes();

        boolean hasRxBytes();

        boolean hasTxBytes();
    }

    public interface ScanEventOrBuilder extends MessageLiteOrBuilder {
        long getEventTimeMillis();

        String getInitiator();

        ByteString getInitiatorBytes();

        int getNumberResults();

        ScanEvent.ScanEventType getScanEventType();

        ScanEvent.ScanTechnologyType getScanTechnologyType();

        boolean hasEventTimeMillis();

        boolean hasInitiator();

        boolean hasNumberResults();

        boolean hasScanEventType();

        boolean hasScanTechnologyType();
    }

    public interface WakeEventOrBuilder extends MessageLiteOrBuilder {
        long getEventTimeMillis();

        String getName();

        ByteString getNameBytes();

        String getRequestor();

        ByteString getRequestorBytes();

        WakeEvent.WakeEventType getWakeEventType();

        boolean hasEventTimeMillis();

        boolean hasName();

        boolean hasRequestor();

        boolean hasWakeEventType();
    }

    private BluetoothMetricsProto() {
    }

    public static void registerAllExtensions(ExtensionRegistryLite extensionRegistryLite) {
    }

    public enum ProfileId implements Internal.EnumLite {
        PROFILE_UNKNOWN(0),
        HEADSET(1),
        A2DP(2),
        HEALTH(3),
        HID_HOST(4),
        PAN(5),
        PBAP(6),
        GATT(7),
        GATT_SERVER(8),
        MAP(9),
        SAP(10),
        A2DP_SINK(11),
        AVRCP_CONTROLLER(12),
        AVRCP(13),
        HEADSET_CLIENT(16),
        PBAP_CLIENT(17),
        MAP_CLIENT(18),
        HID_DEVICE(19),
        OPP(20),
        HEARING_AID(21);

        public static final int A2DP_SINK_VALUE = 11;
        public static final int A2DP_VALUE = 2;
        public static final int AVRCP_CONTROLLER_VALUE = 12;
        public static final int AVRCP_VALUE = 13;
        public static final int GATT_SERVER_VALUE = 8;
        public static final int GATT_VALUE = 7;
        public static final int HEADSET_CLIENT_VALUE = 16;
        public static final int HEADSET_VALUE = 1;
        public static final int HEALTH_VALUE = 3;
        public static final int HEARING_AID_VALUE = 21;
        public static final int HID_DEVICE_VALUE = 19;
        public static final int HID_HOST_VALUE = 4;
        public static final int MAP_CLIENT_VALUE = 18;
        public static final int MAP_VALUE = 9;
        public static final int OPP_VALUE = 20;
        public static final int PAN_VALUE = 5;
        public static final int PBAP_CLIENT_VALUE = 17;
        public static final int PBAP_VALUE = 6;
        public static final int PROFILE_UNKNOWN_VALUE = 0;
        public static final int SAP_VALUE = 10;
        private static final Internal.EnumLiteMap<ProfileId> internalValueMap = new Internal.EnumLiteMap<ProfileId>() {
            @Override
            public ProfileId findValueByNumber(int i) {
                return ProfileId.forNumber(i);
            }
        };
        private final int value;

        @Override
        public final int getNumber() {
            return this.value;
        }

        @Deprecated
        public static ProfileId valueOf(int i) {
            return forNumber(i);
        }

        public static ProfileId forNumber(int i) {
            switch (i) {
                case 0:
                    return PROFILE_UNKNOWN;
                case 1:
                    return HEADSET;
                case 2:
                    return A2DP;
                case 3:
                    return HEALTH;
                case 4:
                    return HID_HOST;
                case 5:
                    return PAN;
                case 6:
                    return PBAP;
                case 7:
                    return GATT;
                case 8:
                    return GATT_SERVER;
                case 9:
                    return MAP;
                case 10:
                    return SAP;
                case 11:
                    return A2DP_SINK;
                case 12:
                    return AVRCP_CONTROLLER;
                case 13:
                    return AVRCP;
                case 14:
                case 15:
                default:
                    return null;
                case 16:
                    return HEADSET_CLIENT;
                case 17:
                    return PBAP_CLIENT;
                case 18:
                    return MAP_CLIENT;
                case 19:
                    return HID_DEVICE;
                case 20:
                    return OPP;
                case 21:
                    return HEARING_AID;
            }
        }

        public static Internal.EnumLiteMap<ProfileId> internalGetValueMap() {
            return internalValueMap;
        }

        ProfileId(int i) {
            this.value = i;
        }
    }

    public enum HeadsetProfileType implements Internal.EnumLite {
        HEADSET_PROFILE_UNKNOWN(0),
        HSP(1),
        HFP(2);

        public static final int HEADSET_PROFILE_UNKNOWN_VALUE = 0;
        public static final int HFP_VALUE = 2;
        public static final int HSP_VALUE = 1;
        private static final Internal.EnumLiteMap<HeadsetProfileType> internalValueMap = new Internal.EnumLiteMap<HeadsetProfileType>() {
            @Override
            public HeadsetProfileType findValueByNumber(int i) {
                return HeadsetProfileType.forNumber(i);
            }
        };
        private final int value;

        @Override
        public final int getNumber() {
            return this.value;
        }

        @Deprecated
        public static HeadsetProfileType valueOf(int i) {
            return forNumber(i);
        }

        public static HeadsetProfileType forNumber(int i) {
            switch (i) {
                case 0:
                    return HEADSET_PROFILE_UNKNOWN;
                case 1:
                    return HSP;
                case 2:
                    return HFP;
                default:
                    return null;
            }
        }

        public static Internal.EnumLiteMap<HeadsetProfileType> internalGetValueMap() {
            return internalValueMap;
        }

        HeadsetProfileType(int i) {
            this.value = i;
        }
    }

    public static final class BluetoothLog extends GeneratedMessageLite<BluetoothLog, Builder> implements BluetoothLogOrBuilder {
        private static final BluetoothLog DEFAULT_INSTANCE = new BluetoothLog();
        public static final int HEADSET_PROFILE_CONNECTION_STATS_FIELD_NUMBER = 11;
        public static final int NUM_BLUETOOTH_SESSION_FIELD_NUMBER = 6;
        public static final int NUM_BONDED_DEVICES_FIELD_NUMBER = 5;
        public static final int NUM_PAIR_EVENT_FIELD_NUMBER = 7;
        public static final int NUM_SCAN_EVENT_FIELD_NUMBER = 9;
        public static final int NUM_WAKE_EVENT_FIELD_NUMBER = 8;
        public static final int PAIR_EVENT_FIELD_NUMBER = 2;
        private static volatile Parser<BluetoothLog> PARSER = null;
        public static final int PROFILE_CONNECTION_STATS_FIELD_NUMBER = 10;
        public static final int SCAN_EVENT_FIELD_NUMBER = 4;
        public static final int SESSION_FIELD_NUMBER = 1;
        public static final int WAKE_EVENT_FIELD_NUMBER = 3;
        private int bitField0_;
        private Internal.ProtobufList<BluetoothSession> session_ = emptyProtobufList();
        private Internal.ProtobufList<PairEvent> pairEvent_ = emptyProtobufList();
        private Internal.ProtobufList<WakeEvent> wakeEvent_ = emptyProtobufList();
        private Internal.ProtobufList<ScanEvent> scanEvent_ = emptyProtobufList();
        private int numBondedDevices_ = 0;
        private long numBluetoothSession_ = 0;
        private long numPairEvent_ = 0;
        private long numWakeEvent_ = 0;
        private long numScanEvent_ = 0;
        private Internal.ProtobufList<ProfileConnectionStats> profileConnectionStats_ = emptyProtobufList();
        private Internal.ProtobufList<HeadsetProfileConnectionStats> headsetProfileConnectionStats_ = emptyProtobufList();

        private BluetoothLog() {
        }

        @Override
        public List<BluetoothSession> getSessionList() {
            return this.session_;
        }

        public List<? extends BluetoothSessionOrBuilder> getSessionOrBuilderList() {
            return this.session_;
        }

        @Override
        public int getSessionCount() {
            return this.session_.size();
        }

        @Override
        public BluetoothSession getSession(int i) {
            return this.session_.get(i);
        }

        public BluetoothSessionOrBuilder getSessionOrBuilder(int i) {
            return this.session_.get(i);
        }

        private void ensureSessionIsMutable() {
            if (!this.session_.isModifiable()) {
                this.session_ = GeneratedMessageLite.mutableCopy(this.session_);
            }
        }

        private void setSession(int i, BluetoothSession bluetoothSession) {
            if (bluetoothSession == null) {
                throw new NullPointerException();
            }
            ensureSessionIsMutable();
            this.session_.set(i, bluetoothSession);
        }

        private void setSession(int i, BluetoothSession.Builder builder) {
            ensureSessionIsMutable();
            this.session_.set(i, builder.build());
        }

        private void addSession(BluetoothSession bluetoothSession) {
            if (bluetoothSession == null) {
                throw new NullPointerException();
            }
            ensureSessionIsMutable();
            this.session_.add(bluetoothSession);
        }

        private void addSession(int i, BluetoothSession bluetoothSession) {
            if (bluetoothSession == null) {
                throw new NullPointerException();
            }
            ensureSessionIsMutable();
            this.session_.add(i, bluetoothSession);
        }

        private void addSession(BluetoothSession.Builder builder) {
            ensureSessionIsMutable();
            this.session_.add(builder.build());
        }

        private void addSession(int i, BluetoothSession.Builder builder) {
            ensureSessionIsMutable();
            this.session_.add(i, builder.build());
        }

        private void addAllSession(Iterable<? extends BluetoothSession> iterable) {
            ensureSessionIsMutable();
            AbstractMessageLite.addAll(iterable, this.session_);
        }

        private void clearSession() {
            this.session_ = emptyProtobufList();
        }

        private void removeSession(int i) {
            ensureSessionIsMutable();
            this.session_.remove(i);
        }

        @Override
        public List<PairEvent> getPairEventList() {
            return this.pairEvent_;
        }

        public List<? extends PairEventOrBuilder> getPairEventOrBuilderList() {
            return this.pairEvent_;
        }

        @Override
        public int getPairEventCount() {
            return this.pairEvent_.size();
        }

        @Override
        public PairEvent getPairEvent(int i) {
            return this.pairEvent_.get(i);
        }

        public PairEventOrBuilder getPairEventOrBuilder(int i) {
            return this.pairEvent_.get(i);
        }

        private void ensurePairEventIsMutable() {
            if (!this.pairEvent_.isModifiable()) {
                this.pairEvent_ = GeneratedMessageLite.mutableCopy(this.pairEvent_);
            }
        }

        private void setPairEvent(int i, PairEvent pairEvent) {
            if (pairEvent == null) {
                throw new NullPointerException();
            }
            ensurePairEventIsMutable();
            this.pairEvent_.set(i, pairEvent);
        }

        private void setPairEvent(int i, PairEvent.Builder builder) {
            ensurePairEventIsMutable();
            this.pairEvent_.set(i, builder.build());
        }

        private void addPairEvent(PairEvent pairEvent) {
            if (pairEvent == null) {
                throw new NullPointerException();
            }
            ensurePairEventIsMutable();
            this.pairEvent_.add(pairEvent);
        }

        private void addPairEvent(int i, PairEvent pairEvent) {
            if (pairEvent == null) {
                throw new NullPointerException();
            }
            ensurePairEventIsMutable();
            this.pairEvent_.add(i, pairEvent);
        }

        private void addPairEvent(PairEvent.Builder builder) {
            ensurePairEventIsMutable();
            this.pairEvent_.add(builder.build());
        }

        private void addPairEvent(int i, PairEvent.Builder builder) {
            ensurePairEventIsMutable();
            this.pairEvent_.add(i, builder.build());
        }

        private void addAllPairEvent(Iterable<? extends PairEvent> iterable) {
            ensurePairEventIsMutable();
            AbstractMessageLite.addAll(iterable, this.pairEvent_);
        }

        private void clearPairEvent() {
            this.pairEvent_ = emptyProtobufList();
        }

        private void removePairEvent(int i) {
            ensurePairEventIsMutable();
            this.pairEvent_.remove(i);
        }

        @Override
        public List<WakeEvent> getWakeEventList() {
            return this.wakeEvent_;
        }

        public List<? extends WakeEventOrBuilder> getWakeEventOrBuilderList() {
            return this.wakeEvent_;
        }

        @Override
        public int getWakeEventCount() {
            return this.wakeEvent_.size();
        }

        @Override
        public WakeEvent getWakeEvent(int i) {
            return this.wakeEvent_.get(i);
        }

        public WakeEventOrBuilder getWakeEventOrBuilder(int i) {
            return this.wakeEvent_.get(i);
        }

        private void ensureWakeEventIsMutable() {
            if (!this.wakeEvent_.isModifiable()) {
                this.wakeEvent_ = GeneratedMessageLite.mutableCopy(this.wakeEvent_);
            }
        }

        private void setWakeEvent(int i, WakeEvent wakeEvent) {
            if (wakeEvent == null) {
                throw new NullPointerException();
            }
            ensureWakeEventIsMutable();
            this.wakeEvent_.set(i, wakeEvent);
        }

        private void setWakeEvent(int i, WakeEvent.Builder builder) {
            ensureWakeEventIsMutable();
            this.wakeEvent_.set(i, builder.build());
        }

        private void addWakeEvent(WakeEvent wakeEvent) {
            if (wakeEvent == null) {
                throw new NullPointerException();
            }
            ensureWakeEventIsMutable();
            this.wakeEvent_.add(wakeEvent);
        }

        private void addWakeEvent(int i, WakeEvent wakeEvent) {
            if (wakeEvent == null) {
                throw new NullPointerException();
            }
            ensureWakeEventIsMutable();
            this.wakeEvent_.add(i, wakeEvent);
        }

        private void addWakeEvent(WakeEvent.Builder builder) {
            ensureWakeEventIsMutable();
            this.wakeEvent_.add(builder.build());
        }

        private void addWakeEvent(int i, WakeEvent.Builder builder) {
            ensureWakeEventIsMutable();
            this.wakeEvent_.add(i, builder.build());
        }

        private void addAllWakeEvent(Iterable<? extends WakeEvent> iterable) {
            ensureWakeEventIsMutable();
            AbstractMessageLite.addAll(iterable, this.wakeEvent_);
        }

        private void clearWakeEvent() {
            this.wakeEvent_ = emptyProtobufList();
        }

        private void removeWakeEvent(int i) {
            ensureWakeEventIsMutable();
            this.wakeEvent_.remove(i);
        }

        @Override
        public List<ScanEvent> getScanEventList() {
            return this.scanEvent_;
        }

        public List<? extends ScanEventOrBuilder> getScanEventOrBuilderList() {
            return this.scanEvent_;
        }

        @Override
        public int getScanEventCount() {
            return this.scanEvent_.size();
        }

        @Override
        public ScanEvent getScanEvent(int i) {
            return this.scanEvent_.get(i);
        }

        public ScanEventOrBuilder getScanEventOrBuilder(int i) {
            return this.scanEvent_.get(i);
        }

        private void ensureScanEventIsMutable() {
            if (!this.scanEvent_.isModifiable()) {
                this.scanEvent_ = GeneratedMessageLite.mutableCopy(this.scanEvent_);
            }
        }

        private void setScanEvent(int i, ScanEvent scanEvent) {
            if (scanEvent == null) {
                throw new NullPointerException();
            }
            ensureScanEventIsMutable();
            this.scanEvent_.set(i, scanEvent);
        }

        private void setScanEvent(int i, ScanEvent.Builder builder) {
            ensureScanEventIsMutable();
            this.scanEvent_.set(i, builder.build());
        }

        private void addScanEvent(ScanEvent scanEvent) {
            if (scanEvent == null) {
                throw new NullPointerException();
            }
            ensureScanEventIsMutable();
            this.scanEvent_.add(scanEvent);
        }

        private void addScanEvent(int i, ScanEvent scanEvent) {
            if (scanEvent == null) {
                throw new NullPointerException();
            }
            ensureScanEventIsMutable();
            this.scanEvent_.add(i, scanEvent);
        }

        private void addScanEvent(ScanEvent.Builder builder) {
            ensureScanEventIsMutable();
            this.scanEvent_.add(builder.build());
        }

        private void addScanEvent(int i, ScanEvent.Builder builder) {
            ensureScanEventIsMutable();
            this.scanEvent_.add(i, builder.build());
        }

        private void addAllScanEvent(Iterable<? extends ScanEvent> iterable) {
            ensureScanEventIsMutable();
            AbstractMessageLite.addAll(iterable, this.scanEvent_);
        }

        private void clearScanEvent() {
            this.scanEvent_ = emptyProtobufList();
        }

        private void removeScanEvent(int i) {
            ensureScanEventIsMutable();
            this.scanEvent_.remove(i);
        }

        @Override
        public boolean hasNumBondedDevices() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public int getNumBondedDevices() {
            return this.numBondedDevices_;
        }

        private void setNumBondedDevices(int i) {
            this.bitField0_ |= 1;
            this.numBondedDevices_ = i;
        }

        private void clearNumBondedDevices() {
            this.bitField0_ &= -2;
            this.numBondedDevices_ = 0;
        }

        @Override
        public boolean hasNumBluetoothSession() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public long getNumBluetoothSession() {
            return this.numBluetoothSession_;
        }

        private void setNumBluetoothSession(long j) {
            this.bitField0_ |= 2;
            this.numBluetoothSession_ = j;
        }

        private void clearNumBluetoothSession() {
            this.bitField0_ &= -3;
            this.numBluetoothSession_ = 0L;
        }

        @Override
        public boolean hasNumPairEvent() {
            return (this.bitField0_ & 4) == 4;
        }

        @Override
        public long getNumPairEvent() {
            return this.numPairEvent_;
        }

        private void setNumPairEvent(long j) {
            this.bitField0_ |= 4;
            this.numPairEvent_ = j;
        }

        private void clearNumPairEvent() {
            this.bitField0_ &= -5;
            this.numPairEvent_ = 0L;
        }

        @Override
        public boolean hasNumWakeEvent() {
            return (this.bitField0_ & 8) == 8;
        }

        @Override
        public long getNumWakeEvent() {
            return this.numWakeEvent_;
        }

        private void setNumWakeEvent(long j) {
            this.bitField0_ |= 8;
            this.numWakeEvent_ = j;
        }

        private void clearNumWakeEvent() {
            this.bitField0_ &= -9;
            this.numWakeEvent_ = 0L;
        }

        @Override
        public boolean hasNumScanEvent() {
            return (this.bitField0_ & 16) == 16;
        }

        @Override
        public long getNumScanEvent() {
            return this.numScanEvent_;
        }

        private void setNumScanEvent(long j) {
            this.bitField0_ |= 16;
            this.numScanEvent_ = j;
        }

        private void clearNumScanEvent() {
            this.bitField0_ &= -17;
            this.numScanEvent_ = 0L;
        }

        @Override
        public List<ProfileConnectionStats> getProfileConnectionStatsList() {
            return this.profileConnectionStats_;
        }

        public List<? extends ProfileConnectionStatsOrBuilder> getProfileConnectionStatsOrBuilderList() {
            return this.profileConnectionStats_;
        }

        @Override
        public int getProfileConnectionStatsCount() {
            return this.profileConnectionStats_.size();
        }

        @Override
        public ProfileConnectionStats getProfileConnectionStats(int i) {
            return this.profileConnectionStats_.get(i);
        }

        public ProfileConnectionStatsOrBuilder getProfileConnectionStatsOrBuilder(int i) {
            return this.profileConnectionStats_.get(i);
        }

        private void ensureProfileConnectionStatsIsMutable() {
            if (!this.profileConnectionStats_.isModifiable()) {
                this.profileConnectionStats_ = GeneratedMessageLite.mutableCopy(this.profileConnectionStats_);
            }
        }

        private void setProfileConnectionStats(int i, ProfileConnectionStats profileConnectionStats) {
            if (profileConnectionStats == null) {
                throw new NullPointerException();
            }
            ensureProfileConnectionStatsIsMutable();
            this.profileConnectionStats_.set(i, profileConnectionStats);
        }

        private void setProfileConnectionStats(int i, ProfileConnectionStats.Builder builder) {
            ensureProfileConnectionStatsIsMutable();
            this.profileConnectionStats_.set(i, builder.build());
        }

        private void addProfileConnectionStats(ProfileConnectionStats profileConnectionStats) {
            if (profileConnectionStats == null) {
                throw new NullPointerException();
            }
            ensureProfileConnectionStatsIsMutable();
            this.profileConnectionStats_.add(profileConnectionStats);
        }

        private void addProfileConnectionStats(int i, ProfileConnectionStats profileConnectionStats) {
            if (profileConnectionStats == null) {
                throw new NullPointerException();
            }
            ensureProfileConnectionStatsIsMutable();
            this.profileConnectionStats_.add(i, profileConnectionStats);
        }

        private void addProfileConnectionStats(ProfileConnectionStats.Builder builder) {
            ensureProfileConnectionStatsIsMutable();
            this.profileConnectionStats_.add(builder.build());
        }

        private void addProfileConnectionStats(int i, ProfileConnectionStats.Builder builder) {
            ensureProfileConnectionStatsIsMutable();
            this.profileConnectionStats_.add(i, builder.build());
        }

        private void addAllProfileConnectionStats(Iterable<? extends ProfileConnectionStats> iterable) {
            ensureProfileConnectionStatsIsMutable();
            AbstractMessageLite.addAll(iterable, this.profileConnectionStats_);
        }

        private void clearProfileConnectionStats() {
            this.profileConnectionStats_ = emptyProtobufList();
        }

        private void removeProfileConnectionStats(int i) {
            ensureProfileConnectionStatsIsMutable();
            this.profileConnectionStats_.remove(i);
        }

        @Override
        public List<HeadsetProfileConnectionStats> getHeadsetProfileConnectionStatsList() {
            return this.headsetProfileConnectionStats_;
        }

        public List<? extends HeadsetProfileConnectionStatsOrBuilder> getHeadsetProfileConnectionStatsOrBuilderList() {
            return this.headsetProfileConnectionStats_;
        }

        @Override
        public int getHeadsetProfileConnectionStatsCount() {
            return this.headsetProfileConnectionStats_.size();
        }

        @Override
        public HeadsetProfileConnectionStats getHeadsetProfileConnectionStats(int i) {
            return this.headsetProfileConnectionStats_.get(i);
        }

        public HeadsetProfileConnectionStatsOrBuilder getHeadsetProfileConnectionStatsOrBuilder(int i) {
            return this.headsetProfileConnectionStats_.get(i);
        }

        private void ensureHeadsetProfileConnectionStatsIsMutable() {
            if (!this.headsetProfileConnectionStats_.isModifiable()) {
                this.headsetProfileConnectionStats_ = GeneratedMessageLite.mutableCopy(this.headsetProfileConnectionStats_);
            }
        }

        private void setHeadsetProfileConnectionStats(int i, HeadsetProfileConnectionStats headsetProfileConnectionStats) {
            if (headsetProfileConnectionStats == null) {
                throw new NullPointerException();
            }
            ensureHeadsetProfileConnectionStatsIsMutable();
            this.headsetProfileConnectionStats_.set(i, headsetProfileConnectionStats);
        }

        private void setHeadsetProfileConnectionStats(int i, HeadsetProfileConnectionStats.Builder builder) {
            ensureHeadsetProfileConnectionStatsIsMutable();
            this.headsetProfileConnectionStats_.set(i, builder.build());
        }

        private void addHeadsetProfileConnectionStats(HeadsetProfileConnectionStats headsetProfileConnectionStats) {
            if (headsetProfileConnectionStats == null) {
                throw new NullPointerException();
            }
            ensureHeadsetProfileConnectionStatsIsMutable();
            this.headsetProfileConnectionStats_.add(headsetProfileConnectionStats);
        }

        private void addHeadsetProfileConnectionStats(int i, HeadsetProfileConnectionStats headsetProfileConnectionStats) {
            if (headsetProfileConnectionStats == null) {
                throw new NullPointerException();
            }
            ensureHeadsetProfileConnectionStatsIsMutable();
            this.headsetProfileConnectionStats_.add(i, headsetProfileConnectionStats);
        }

        private void addHeadsetProfileConnectionStats(HeadsetProfileConnectionStats.Builder builder) {
            ensureHeadsetProfileConnectionStatsIsMutable();
            this.headsetProfileConnectionStats_.add(builder.build());
        }

        private void addHeadsetProfileConnectionStats(int i, HeadsetProfileConnectionStats.Builder builder) {
            ensureHeadsetProfileConnectionStatsIsMutable();
            this.headsetProfileConnectionStats_.add(i, builder.build());
        }

        private void addAllHeadsetProfileConnectionStats(Iterable<? extends HeadsetProfileConnectionStats> iterable) {
            ensureHeadsetProfileConnectionStatsIsMutable();
            AbstractMessageLite.addAll(iterable, this.headsetProfileConnectionStats_);
        }

        private void clearHeadsetProfileConnectionStats() {
            this.headsetProfileConnectionStats_ = emptyProtobufList();
        }

        private void removeHeadsetProfileConnectionStats(int i) {
            ensureHeadsetProfileConnectionStatsIsMutable();
            this.headsetProfileConnectionStats_.remove(i);
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            for (int i = 0; i < this.session_.size(); i++) {
                codedOutputStream.writeMessage(1, this.session_.get(i));
            }
            for (int i2 = 0; i2 < this.pairEvent_.size(); i2++) {
                codedOutputStream.writeMessage(2, this.pairEvent_.get(i2));
            }
            for (int i3 = 0; i3 < this.wakeEvent_.size(); i3++) {
                codedOutputStream.writeMessage(3, this.wakeEvent_.get(i3));
            }
            for (int i4 = 0; i4 < this.scanEvent_.size(); i4++) {
                codedOutputStream.writeMessage(4, this.scanEvent_.get(i4));
            }
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeInt32(5, this.numBondedDevices_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeInt64(6, this.numBluetoothSession_);
            }
            if ((this.bitField0_ & 4) == 4) {
                codedOutputStream.writeInt64(7, this.numPairEvent_);
            }
            if ((this.bitField0_ & 8) == 8) {
                codedOutputStream.writeInt64(8, this.numWakeEvent_);
            }
            if ((this.bitField0_ & 16) == 16) {
                codedOutputStream.writeInt64(9, this.numScanEvent_);
            }
            for (int i5 = 0; i5 < this.profileConnectionStats_.size(); i5++) {
                codedOutputStream.writeMessage(10, this.profileConnectionStats_.get(i5));
            }
            for (int i6 = 0; i6 < this.headsetProfileConnectionStats_.size(); i6++) {
                codedOutputStream.writeMessage(11, this.headsetProfileConnectionStats_.get(i6));
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeMessageSize = 0;
            for (int i2 = 0; i2 < this.session_.size(); i2++) {
                iComputeMessageSize += CodedOutputStream.computeMessageSize(1, this.session_.get(i2));
            }
            for (int i3 = 0; i3 < this.pairEvent_.size(); i3++) {
                iComputeMessageSize += CodedOutputStream.computeMessageSize(2, this.pairEvent_.get(i3));
            }
            for (int i4 = 0; i4 < this.wakeEvent_.size(); i4++) {
                iComputeMessageSize += CodedOutputStream.computeMessageSize(3, this.wakeEvent_.get(i4));
            }
            for (int i5 = 0; i5 < this.scanEvent_.size(); i5++) {
                iComputeMessageSize += CodedOutputStream.computeMessageSize(4, this.scanEvent_.get(i5));
            }
            if ((this.bitField0_ & 1) == 1) {
                iComputeMessageSize += CodedOutputStream.computeInt32Size(5, this.numBondedDevices_);
            }
            if ((this.bitField0_ & 2) == 2) {
                iComputeMessageSize += CodedOutputStream.computeInt64Size(6, this.numBluetoothSession_);
            }
            if ((this.bitField0_ & 4) == 4) {
                iComputeMessageSize += CodedOutputStream.computeInt64Size(7, this.numPairEvent_);
            }
            if ((this.bitField0_ & 8) == 8) {
                iComputeMessageSize += CodedOutputStream.computeInt64Size(8, this.numWakeEvent_);
            }
            if ((this.bitField0_ & 16) == 16) {
                iComputeMessageSize += CodedOutputStream.computeInt64Size(9, this.numScanEvent_);
            }
            for (int i6 = 0; i6 < this.profileConnectionStats_.size(); i6++) {
                iComputeMessageSize += CodedOutputStream.computeMessageSize(10, this.profileConnectionStats_.get(i6));
            }
            for (int i7 = 0; i7 < this.headsetProfileConnectionStats_.size(); i7++) {
                iComputeMessageSize += CodedOutputStream.computeMessageSize(11, this.headsetProfileConnectionStats_.get(i7));
            }
            int serializedSize = iComputeMessageSize + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static BluetoothLog parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (BluetoothLog) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static BluetoothLog parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (BluetoothLog) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static BluetoothLog parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (BluetoothLog) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static BluetoothLog parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (BluetoothLog) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static BluetoothLog parseFrom(InputStream inputStream) throws IOException {
            return (BluetoothLog) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static BluetoothLog parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (BluetoothLog) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static BluetoothLog parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (BluetoothLog) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static BluetoothLog parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (BluetoothLog) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static BluetoothLog parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (BluetoothLog) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static BluetoothLog parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (BluetoothLog) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(BluetoothLog bluetoothLog) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(bluetoothLog);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<BluetoothLog, Builder> implements BluetoothLogOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(BluetoothLog.DEFAULT_INSTANCE);
            }

            @Override
            public List<BluetoothSession> getSessionList() {
                return Collections.unmodifiableList(((BluetoothLog) this.instance).getSessionList());
            }

            @Override
            public int getSessionCount() {
                return ((BluetoothLog) this.instance).getSessionCount();
            }

            @Override
            public BluetoothSession getSession(int i) {
                return ((BluetoothLog) this.instance).getSession(i);
            }

            public Builder setSession(int i, BluetoothSession bluetoothSession) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setSession(i, bluetoothSession);
                return this;
            }

            public Builder setSession(int i, BluetoothSession.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setSession(i, builder);
                return this;
            }

            public Builder addSession(BluetoothSession bluetoothSession) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addSession(bluetoothSession);
                return this;
            }

            public Builder addSession(int i, BluetoothSession bluetoothSession) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addSession(i, bluetoothSession);
                return this;
            }

            public Builder addSession(BluetoothSession.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addSession(builder);
                return this;
            }

            public Builder addSession(int i, BluetoothSession.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addSession(i, builder);
                return this;
            }

            public Builder addAllSession(Iterable<? extends BluetoothSession> iterable) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addAllSession(iterable);
                return this;
            }

            public Builder clearSession() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearSession();
                return this;
            }

            public Builder removeSession(int i) {
                copyOnWrite();
                ((BluetoothLog) this.instance).removeSession(i);
                return this;
            }

            @Override
            public List<PairEvent> getPairEventList() {
                return Collections.unmodifiableList(((BluetoothLog) this.instance).getPairEventList());
            }

            @Override
            public int getPairEventCount() {
                return ((BluetoothLog) this.instance).getPairEventCount();
            }

            @Override
            public PairEvent getPairEvent(int i) {
                return ((BluetoothLog) this.instance).getPairEvent(i);
            }

            public Builder setPairEvent(int i, PairEvent pairEvent) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setPairEvent(i, pairEvent);
                return this;
            }

            public Builder setPairEvent(int i, PairEvent.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setPairEvent(i, builder);
                return this;
            }

            public Builder addPairEvent(PairEvent pairEvent) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addPairEvent(pairEvent);
                return this;
            }

            public Builder addPairEvent(int i, PairEvent pairEvent) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addPairEvent(i, pairEvent);
                return this;
            }

            public Builder addPairEvent(PairEvent.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addPairEvent(builder);
                return this;
            }

            public Builder addPairEvent(int i, PairEvent.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addPairEvent(i, builder);
                return this;
            }

            public Builder addAllPairEvent(Iterable<? extends PairEvent> iterable) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addAllPairEvent(iterable);
                return this;
            }

            public Builder clearPairEvent() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearPairEvent();
                return this;
            }

            public Builder removePairEvent(int i) {
                copyOnWrite();
                ((BluetoothLog) this.instance).removePairEvent(i);
                return this;
            }

            @Override
            public List<WakeEvent> getWakeEventList() {
                return Collections.unmodifiableList(((BluetoothLog) this.instance).getWakeEventList());
            }

            @Override
            public int getWakeEventCount() {
                return ((BluetoothLog) this.instance).getWakeEventCount();
            }

            @Override
            public WakeEvent getWakeEvent(int i) {
                return ((BluetoothLog) this.instance).getWakeEvent(i);
            }

            public Builder setWakeEvent(int i, WakeEvent wakeEvent) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setWakeEvent(i, wakeEvent);
                return this;
            }

            public Builder setWakeEvent(int i, WakeEvent.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setWakeEvent(i, builder);
                return this;
            }

            public Builder addWakeEvent(WakeEvent wakeEvent) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addWakeEvent(wakeEvent);
                return this;
            }

            public Builder addWakeEvent(int i, WakeEvent wakeEvent) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addWakeEvent(i, wakeEvent);
                return this;
            }

            public Builder addWakeEvent(WakeEvent.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addWakeEvent(builder);
                return this;
            }

            public Builder addWakeEvent(int i, WakeEvent.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addWakeEvent(i, builder);
                return this;
            }

            public Builder addAllWakeEvent(Iterable<? extends WakeEvent> iterable) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addAllWakeEvent(iterable);
                return this;
            }

            public Builder clearWakeEvent() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearWakeEvent();
                return this;
            }

            public Builder removeWakeEvent(int i) {
                copyOnWrite();
                ((BluetoothLog) this.instance).removeWakeEvent(i);
                return this;
            }

            @Override
            public List<ScanEvent> getScanEventList() {
                return Collections.unmodifiableList(((BluetoothLog) this.instance).getScanEventList());
            }

            @Override
            public int getScanEventCount() {
                return ((BluetoothLog) this.instance).getScanEventCount();
            }

            @Override
            public ScanEvent getScanEvent(int i) {
                return ((BluetoothLog) this.instance).getScanEvent(i);
            }

            public Builder setScanEvent(int i, ScanEvent scanEvent) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setScanEvent(i, scanEvent);
                return this;
            }

            public Builder setScanEvent(int i, ScanEvent.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setScanEvent(i, builder);
                return this;
            }

            public Builder addScanEvent(ScanEvent scanEvent) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addScanEvent(scanEvent);
                return this;
            }

            public Builder addScanEvent(int i, ScanEvent scanEvent) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addScanEvent(i, scanEvent);
                return this;
            }

            public Builder addScanEvent(ScanEvent.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addScanEvent(builder);
                return this;
            }

            public Builder addScanEvent(int i, ScanEvent.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addScanEvent(i, builder);
                return this;
            }

            public Builder addAllScanEvent(Iterable<? extends ScanEvent> iterable) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addAllScanEvent(iterable);
                return this;
            }

            public Builder clearScanEvent() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearScanEvent();
                return this;
            }

            public Builder removeScanEvent(int i) {
                copyOnWrite();
                ((BluetoothLog) this.instance).removeScanEvent(i);
                return this;
            }

            @Override
            public boolean hasNumBondedDevices() {
                return ((BluetoothLog) this.instance).hasNumBondedDevices();
            }

            @Override
            public int getNumBondedDevices() {
                return ((BluetoothLog) this.instance).getNumBondedDevices();
            }

            public Builder setNumBondedDevices(int i) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setNumBondedDevices(i);
                return this;
            }

            public Builder clearNumBondedDevices() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearNumBondedDevices();
                return this;
            }

            @Override
            public boolean hasNumBluetoothSession() {
                return ((BluetoothLog) this.instance).hasNumBluetoothSession();
            }

            @Override
            public long getNumBluetoothSession() {
                return ((BluetoothLog) this.instance).getNumBluetoothSession();
            }

            public Builder setNumBluetoothSession(long j) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setNumBluetoothSession(j);
                return this;
            }

            public Builder clearNumBluetoothSession() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearNumBluetoothSession();
                return this;
            }

            @Override
            public boolean hasNumPairEvent() {
                return ((BluetoothLog) this.instance).hasNumPairEvent();
            }

            @Override
            public long getNumPairEvent() {
                return ((BluetoothLog) this.instance).getNumPairEvent();
            }

            public Builder setNumPairEvent(long j) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setNumPairEvent(j);
                return this;
            }

            public Builder clearNumPairEvent() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearNumPairEvent();
                return this;
            }

            @Override
            public boolean hasNumWakeEvent() {
                return ((BluetoothLog) this.instance).hasNumWakeEvent();
            }

            @Override
            public long getNumWakeEvent() {
                return ((BluetoothLog) this.instance).getNumWakeEvent();
            }

            public Builder setNumWakeEvent(long j) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setNumWakeEvent(j);
                return this;
            }

            public Builder clearNumWakeEvent() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearNumWakeEvent();
                return this;
            }

            @Override
            public boolean hasNumScanEvent() {
                return ((BluetoothLog) this.instance).hasNumScanEvent();
            }

            @Override
            public long getNumScanEvent() {
                return ((BluetoothLog) this.instance).getNumScanEvent();
            }

            public Builder setNumScanEvent(long j) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setNumScanEvent(j);
                return this;
            }

            public Builder clearNumScanEvent() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearNumScanEvent();
                return this;
            }

            @Override
            public List<ProfileConnectionStats> getProfileConnectionStatsList() {
                return Collections.unmodifiableList(((BluetoothLog) this.instance).getProfileConnectionStatsList());
            }

            @Override
            public int getProfileConnectionStatsCount() {
                return ((BluetoothLog) this.instance).getProfileConnectionStatsCount();
            }

            @Override
            public ProfileConnectionStats getProfileConnectionStats(int i) {
                return ((BluetoothLog) this.instance).getProfileConnectionStats(i);
            }

            public Builder setProfileConnectionStats(int i, ProfileConnectionStats profileConnectionStats) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setProfileConnectionStats(i, profileConnectionStats);
                return this;
            }

            public Builder setProfileConnectionStats(int i, ProfileConnectionStats.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setProfileConnectionStats(i, builder);
                return this;
            }

            public Builder addProfileConnectionStats(ProfileConnectionStats profileConnectionStats) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addProfileConnectionStats(profileConnectionStats);
                return this;
            }

            public Builder addProfileConnectionStats(int i, ProfileConnectionStats profileConnectionStats) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addProfileConnectionStats(i, profileConnectionStats);
                return this;
            }

            public Builder addProfileConnectionStats(ProfileConnectionStats.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addProfileConnectionStats(builder);
                return this;
            }

            public Builder addProfileConnectionStats(int i, ProfileConnectionStats.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addProfileConnectionStats(i, builder);
                return this;
            }

            public Builder addAllProfileConnectionStats(Iterable<? extends ProfileConnectionStats> iterable) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addAllProfileConnectionStats(iterable);
                return this;
            }

            public Builder clearProfileConnectionStats() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearProfileConnectionStats();
                return this;
            }

            public Builder removeProfileConnectionStats(int i) {
                copyOnWrite();
                ((BluetoothLog) this.instance).removeProfileConnectionStats(i);
                return this;
            }

            @Override
            public List<HeadsetProfileConnectionStats> getHeadsetProfileConnectionStatsList() {
                return Collections.unmodifiableList(((BluetoothLog) this.instance).getHeadsetProfileConnectionStatsList());
            }

            @Override
            public int getHeadsetProfileConnectionStatsCount() {
                return ((BluetoothLog) this.instance).getHeadsetProfileConnectionStatsCount();
            }

            @Override
            public HeadsetProfileConnectionStats getHeadsetProfileConnectionStats(int i) {
                return ((BluetoothLog) this.instance).getHeadsetProfileConnectionStats(i);
            }

            public Builder setHeadsetProfileConnectionStats(int i, HeadsetProfileConnectionStats headsetProfileConnectionStats) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setHeadsetProfileConnectionStats(i, headsetProfileConnectionStats);
                return this;
            }

            public Builder setHeadsetProfileConnectionStats(int i, HeadsetProfileConnectionStats.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).setHeadsetProfileConnectionStats(i, builder);
                return this;
            }

            public Builder addHeadsetProfileConnectionStats(HeadsetProfileConnectionStats headsetProfileConnectionStats) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addHeadsetProfileConnectionStats(headsetProfileConnectionStats);
                return this;
            }

            public Builder addHeadsetProfileConnectionStats(int i, HeadsetProfileConnectionStats headsetProfileConnectionStats) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addHeadsetProfileConnectionStats(i, headsetProfileConnectionStats);
                return this;
            }

            public Builder addHeadsetProfileConnectionStats(HeadsetProfileConnectionStats.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addHeadsetProfileConnectionStats(builder);
                return this;
            }

            public Builder addHeadsetProfileConnectionStats(int i, HeadsetProfileConnectionStats.Builder builder) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addHeadsetProfileConnectionStats(i, builder);
                return this;
            }

            public Builder addAllHeadsetProfileConnectionStats(Iterable<? extends HeadsetProfileConnectionStats> iterable) {
                copyOnWrite();
                ((BluetoothLog) this.instance).addAllHeadsetProfileConnectionStats(iterable);
                return this;
            }

            public Builder clearHeadsetProfileConnectionStats() {
                copyOnWrite();
                ((BluetoothLog) this.instance).clearHeadsetProfileConnectionStats();
                return this;
            }

            public Builder removeHeadsetProfileConnectionStats(int i) {
                copyOnWrite();
                ((BluetoothLog) this.instance).removeHeadsetProfileConnectionStats(i);
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new BluetoothLog();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    this.session_.makeImmutable();
                    this.pairEvent_.makeImmutable();
                    this.wakeEvent_.makeImmutable();
                    this.scanEvent_.makeImmutable();
                    this.profileConnectionStats_.makeImmutable();
                    this.headsetProfileConnectionStats_.makeImmutable();
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    BluetoothLog bluetoothLog = (BluetoothLog) obj2;
                    this.session_ = visitor.visitList(this.session_, bluetoothLog.session_);
                    this.pairEvent_ = visitor.visitList(this.pairEvent_, bluetoothLog.pairEvent_);
                    this.wakeEvent_ = visitor.visitList(this.wakeEvent_, bluetoothLog.wakeEvent_);
                    this.scanEvent_ = visitor.visitList(this.scanEvent_, bluetoothLog.scanEvent_);
                    this.numBondedDevices_ = visitor.visitInt(hasNumBondedDevices(), this.numBondedDevices_, bluetoothLog.hasNumBondedDevices(), bluetoothLog.numBondedDevices_);
                    this.numBluetoothSession_ = visitor.visitLong(hasNumBluetoothSession(), this.numBluetoothSession_, bluetoothLog.hasNumBluetoothSession(), bluetoothLog.numBluetoothSession_);
                    this.numPairEvent_ = visitor.visitLong(hasNumPairEvent(), this.numPairEvent_, bluetoothLog.hasNumPairEvent(), bluetoothLog.numPairEvent_);
                    this.numWakeEvent_ = visitor.visitLong(hasNumWakeEvent(), this.numWakeEvent_, bluetoothLog.hasNumWakeEvent(), bluetoothLog.numWakeEvent_);
                    this.numScanEvent_ = visitor.visitLong(hasNumScanEvent(), this.numScanEvent_, bluetoothLog.hasNumScanEvent(), bluetoothLog.numScanEvent_);
                    this.profileConnectionStats_ = visitor.visitList(this.profileConnectionStats_, bluetoothLog.profileConnectionStats_);
                    this.headsetProfileConnectionStats_ = visitor.visitList(this.headsetProfileConnectionStats_, bluetoothLog.headsetProfileConnectionStats_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= bluetoothLog.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    ExtensionRegistryLite extensionRegistryLite = (ExtensionRegistryLite) obj2;
                    boolean z = false;
                    while (!z) {
                        try {
                            try {
                                int tag = codedInputStream.readTag();
                                switch (tag) {
                                    case 0:
                                        z = true;
                                        break;
                                    case 10:
                                        if (!this.session_.isModifiable()) {
                                            this.session_ = GeneratedMessageLite.mutableCopy(this.session_);
                                        }
                                        this.session_.add((BluetoothSession) codedInputStream.readMessage(BluetoothSession.parser(), extensionRegistryLite));
                                        break;
                                    case 18:
                                        if (!this.pairEvent_.isModifiable()) {
                                            this.pairEvent_ = GeneratedMessageLite.mutableCopy(this.pairEvent_);
                                        }
                                        this.pairEvent_.add((PairEvent) codedInputStream.readMessage(PairEvent.parser(), extensionRegistryLite));
                                        break;
                                    case 26:
                                        if (!this.wakeEvent_.isModifiable()) {
                                            this.wakeEvent_ = GeneratedMessageLite.mutableCopy(this.wakeEvent_);
                                        }
                                        this.wakeEvent_.add((WakeEvent) codedInputStream.readMessage(WakeEvent.parser(), extensionRegistryLite));
                                        break;
                                    case 34:
                                        if (!this.scanEvent_.isModifiable()) {
                                            this.scanEvent_ = GeneratedMessageLite.mutableCopy(this.scanEvent_);
                                        }
                                        this.scanEvent_.add((ScanEvent) codedInputStream.readMessage(ScanEvent.parser(), extensionRegistryLite));
                                        break;
                                    case 40:
                                        this.bitField0_ |= 1;
                                        this.numBondedDevices_ = codedInputStream.readInt32();
                                        break;
                                    case 48:
                                        this.bitField0_ |= 2;
                                        this.numBluetoothSession_ = codedInputStream.readInt64();
                                        break;
                                    case 56:
                                        this.bitField0_ |= 4;
                                        this.numPairEvent_ = codedInputStream.readInt64();
                                        break;
                                    case 64:
                                        this.bitField0_ |= 8;
                                        this.numWakeEvent_ = codedInputStream.readInt64();
                                        break;
                                    case AvrcpControllerService.PASS_THRU_CMD_ID_REWIND:
                                        this.bitField0_ |= 16;
                                        this.numScanEvent_ = codedInputStream.readInt64();
                                        break;
                                    case 82:
                                        if (!this.profileConnectionStats_.isModifiable()) {
                                            this.profileConnectionStats_ = GeneratedMessageLite.mutableCopy(this.profileConnectionStats_);
                                        }
                                        this.profileConnectionStats_.add((ProfileConnectionStats) codedInputStream.readMessage(ProfileConnectionStats.parser(), extensionRegistryLite));
                                        break;
                                    case 90:
                                        if (!this.headsetProfileConnectionStats_.isModifiable()) {
                                            this.headsetProfileConnectionStats_ = GeneratedMessageLite.mutableCopy(this.headsetProfileConnectionStats_);
                                        }
                                        this.headsetProfileConnectionStats_.add((HeadsetProfileConnectionStats) codedInputStream.readMessage(HeadsetProfileConnectionStats.parser(), extensionRegistryLite));
                                        break;
                                    default:
                                        if (!parseUnknownField(tag, codedInputStream)) {
                                            z = true;
                                        }
                                        break;
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(new InvalidProtocolBufferException(e.getMessage()).setUnfinishedMessage(this));
                            }
                        } catch (InvalidProtocolBufferException e2) {
                            throw new RuntimeException(e2.setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (BluetoothLog.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static BluetoothLog getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<BluetoothLog> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke = new int[GeneratedMessageLite.MethodToInvoke.values().length];

        static {
            try {
                $SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[GeneratedMessageLite.MethodToInvoke.NEW_MUTABLE_INSTANCE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[GeneratedMessageLite.MethodToInvoke.IS_INITIALIZED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[GeneratedMessageLite.MethodToInvoke.MAKE_IMMUTABLE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[GeneratedMessageLite.MethodToInvoke.NEW_BUILDER.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[GeneratedMessageLite.MethodToInvoke.VISIT.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[GeneratedMessageLite.MethodToInvoke.MERGE_FROM_STREAM.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[GeneratedMessageLite.MethodToInvoke.GET_DEFAULT_INSTANCE.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[GeneratedMessageLite.MethodToInvoke.GET_PARSER.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
        }
    }

    public static final class DeviceInfo extends GeneratedMessageLite<DeviceInfo, Builder> implements DeviceInfoOrBuilder {
        private static final DeviceInfo DEFAULT_INSTANCE = new DeviceInfo();
        public static final int DEVICE_CLASS_FIELD_NUMBER = 1;
        public static final int DEVICE_TYPE_FIELD_NUMBER = 2;
        private static volatile Parser<DeviceInfo> PARSER;
        private int bitField0_;
        private int deviceClass_ = 0;
        private int deviceType_ = 0;

        private DeviceInfo() {
        }

        public enum DeviceType implements Internal.EnumLite {
            DEVICE_TYPE_UNKNOWN(0),
            DEVICE_TYPE_BREDR(1),
            DEVICE_TYPE_LE(2),
            DEVICE_TYPE_DUMO(3);

            public static final int DEVICE_TYPE_BREDR_VALUE = 1;
            public static final int DEVICE_TYPE_DUMO_VALUE = 3;
            public static final int DEVICE_TYPE_LE_VALUE = 2;
            public static final int DEVICE_TYPE_UNKNOWN_VALUE = 0;
            private static final Internal.EnumLiteMap<DeviceType> internalValueMap = new Internal.EnumLiteMap<DeviceType>() {
                @Override
                public DeviceType findValueByNumber(int i) {
                    return DeviceType.forNumber(i);
                }
            };
            private final int value;

            @Override
            public final int getNumber() {
                return this.value;
            }

            @Deprecated
            public static DeviceType valueOf(int i) {
                return forNumber(i);
            }

            public static DeviceType forNumber(int i) {
                switch (i) {
                    case 0:
                        return DEVICE_TYPE_UNKNOWN;
                    case 1:
                        return DEVICE_TYPE_BREDR;
                    case 2:
                        return DEVICE_TYPE_LE;
                    case 3:
                        return DEVICE_TYPE_DUMO;
                    default:
                        return null;
                }
            }

            public static Internal.EnumLiteMap<DeviceType> internalGetValueMap() {
                return internalValueMap;
            }

            DeviceType(int i) {
                this.value = i;
            }
        }

        @Override
        public boolean hasDeviceClass() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public int getDeviceClass() {
            return this.deviceClass_;
        }

        private void setDeviceClass(int i) {
            this.bitField0_ |= 1;
            this.deviceClass_ = i;
        }

        private void clearDeviceClass() {
            this.bitField0_ &= -2;
            this.deviceClass_ = 0;
        }

        @Override
        public boolean hasDeviceType() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public DeviceType getDeviceType() {
            DeviceType deviceTypeForNumber = DeviceType.forNumber(this.deviceType_);
            return deviceTypeForNumber == null ? DeviceType.DEVICE_TYPE_UNKNOWN : deviceTypeForNumber;
        }

        private void setDeviceType(DeviceType deviceType) {
            if (deviceType == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 2;
            this.deviceType_ = deviceType.getNumber();
        }

        private void clearDeviceType() {
            this.bitField0_ &= -3;
            this.deviceType_ = 0;
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeInt32(1, this.deviceClass_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeEnum(2, this.deviceType_);
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeInt32Size = (this.bitField0_ & 1) == 1 ? 0 + CodedOutputStream.computeInt32Size(1, this.deviceClass_) : 0;
            if ((this.bitField0_ & 2) == 2) {
                iComputeInt32Size += CodedOutputStream.computeEnumSize(2, this.deviceType_);
            }
            int serializedSize = iComputeInt32Size + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static DeviceInfo parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (DeviceInfo) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static DeviceInfo parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (DeviceInfo) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static DeviceInfo parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (DeviceInfo) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static DeviceInfo parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (DeviceInfo) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static DeviceInfo parseFrom(InputStream inputStream) throws IOException {
            return (DeviceInfo) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static DeviceInfo parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (DeviceInfo) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static DeviceInfo parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (DeviceInfo) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static DeviceInfo parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (DeviceInfo) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static DeviceInfo parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (DeviceInfo) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static DeviceInfo parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (DeviceInfo) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(DeviceInfo deviceInfo) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(deviceInfo);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<DeviceInfo, Builder> implements DeviceInfoOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(DeviceInfo.DEFAULT_INSTANCE);
            }

            @Override
            public boolean hasDeviceClass() {
                return ((DeviceInfo) this.instance).hasDeviceClass();
            }

            @Override
            public int getDeviceClass() {
                return ((DeviceInfo) this.instance).getDeviceClass();
            }

            public Builder setDeviceClass(int i) {
                copyOnWrite();
                ((DeviceInfo) this.instance).setDeviceClass(i);
                return this;
            }

            public Builder clearDeviceClass() {
                copyOnWrite();
                ((DeviceInfo) this.instance).clearDeviceClass();
                return this;
            }

            @Override
            public boolean hasDeviceType() {
                return ((DeviceInfo) this.instance).hasDeviceType();
            }

            @Override
            public DeviceType getDeviceType() {
                return ((DeviceInfo) this.instance).getDeviceType();
            }

            public Builder setDeviceType(DeviceType deviceType) {
                copyOnWrite();
                ((DeviceInfo) this.instance).setDeviceType(deviceType);
                return this;
            }

            public Builder clearDeviceType() {
                copyOnWrite();
                ((DeviceInfo) this.instance).clearDeviceType();
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new DeviceInfo();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    DeviceInfo deviceInfo = (DeviceInfo) obj2;
                    this.deviceClass_ = visitor.visitInt(hasDeviceClass(), this.deviceClass_, deviceInfo.hasDeviceClass(), deviceInfo.deviceClass_);
                    this.deviceType_ = visitor.visitInt(hasDeviceType(), this.deviceType_, deviceInfo.hasDeviceType(), deviceInfo.deviceType_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= deviceInfo.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    boolean z = false;
                    while (!z) {
                        try {
                            int tag = codedInputStream.readTag();
                            if (tag != 0) {
                                if (tag == 8) {
                                    this.bitField0_ |= 1;
                                    this.deviceClass_ = codedInputStream.readInt32();
                                } else if (tag != 16) {
                                    if (!parseUnknownField(tag, codedInputStream)) {
                                    }
                                } else {
                                    int i = codedInputStream.readEnum();
                                    if (DeviceType.forNumber(i) != null) {
                                        this.bitField0_ |= 2;
                                        this.deviceType_ = i;
                                    } else {
                                        super.mergeVarintField(2, i);
                                    }
                                }
                            }
                            z = true;
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e.setUnfinishedMessage(this));
                        } catch (IOException e2) {
                            throw new RuntimeException(new InvalidProtocolBufferException(e2.getMessage()).setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (DeviceInfo.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static DeviceInfo getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<DeviceInfo> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class BluetoothSession extends GeneratedMessageLite<BluetoothSession, Builder> implements BluetoothSessionOrBuilder {
        public static final int A2DP_SESSION_FIELD_NUMBER = 7;
        public static final int CONNECTION_TECHNOLOGY_TYPE_FIELD_NUMBER = 3;
        private static final BluetoothSession DEFAULT_INSTANCE = new BluetoothSession();
        public static final int DEVICE_CONNECTED_TO_FIELD_NUMBER = 5;
        public static final int DISCONNECT_REASON_FIELD_NUMBER = 4;
        public static final int DISCONNECT_REASON_TYPE_FIELD_NUMBER = 8;
        private static volatile Parser<BluetoothSession> PARSER = null;
        public static final int RFCOMM_SESSION_FIELD_NUMBER = 6;
        public static final int SESSION_DURATION_SEC_FIELD_NUMBER = 2;
        private A2DPSession a2DpSession_;
        private int bitField0_;
        private DeviceInfo deviceConnectedTo_;
        private RFCommSession rfcommSession_;
        private long sessionDurationSec_ = 0;
        private int connectionTechnologyType_ = 0;
        private String disconnectReason_ = "";
        private int disconnectReasonType_ = 0;

        private BluetoothSession() {
        }

        public enum ConnectionTechnologyType implements Internal.EnumLite {
            CONNECTION_TECHNOLOGY_TYPE_UNKNOWN(0),
            CONNECTION_TECHNOLOGY_TYPE_LE(1),
            CONNECTION_TECHNOLOGY_TYPE_BREDR(2);

            public static final int CONNECTION_TECHNOLOGY_TYPE_BREDR_VALUE = 2;
            public static final int CONNECTION_TECHNOLOGY_TYPE_LE_VALUE = 1;
            public static final int CONNECTION_TECHNOLOGY_TYPE_UNKNOWN_VALUE = 0;
            private static final Internal.EnumLiteMap<ConnectionTechnologyType> internalValueMap = new Internal.EnumLiteMap<ConnectionTechnologyType>() {
                @Override
                public ConnectionTechnologyType findValueByNumber(int i) {
                    return ConnectionTechnologyType.forNumber(i);
                }
            };
            private final int value;

            @Override
            public final int getNumber() {
                return this.value;
            }

            @Deprecated
            public static ConnectionTechnologyType valueOf(int i) {
                return forNumber(i);
            }

            public static ConnectionTechnologyType forNumber(int i) {
                switch (i) {
                    case 0:
                        return CONNECTION_TECHNOLOGY_TYPE_UNKNOWN;
                    case 1:
                        return CONNECTION_TECHNOLOGY_TYPE_LE;
                    case 2:
                        return CONNECTION_TECHNOLOGY_TYPE_BREDR;
                    default:
                        return null;
                }
            }

            public static Internal.EnumLiteMap<ConnectionTechnologyType> internalGetValueMap() {
                return internalValueMap;
            }

            ConnectionTechnologyType(int i) {
                this.value = i;
            }
        }

        public enum DisconnectReasonType implements Internal.EnumLite {
            UNKNOWN(0),
            METRICS_DUMP(1),
            NEXT_START_WITHOUT_END_PREVIOUS(2);

            public static final int METRICS_DUMP_VALUE = 1;
            public static final int NEXT_START_WITHOUT_END_PREVIOUS_VALUE = 2;
            public static final int UNKNOWN_VALUE = 0;
            private static final Internal.EnumLiteMap<DisconnectReasonType> internalValueMap = new Internal.EnumLiteMap<DisconnectReasonType>() {
                @Override
                public DisconnectReasonType findValueByNumber(int i) {
                    return DisconnectReasonType.forNumber(i);
                }
            };
            private final int value;

            @Override
            public final int getNumber() {
                return this.value;
            }

            @Deprecated
            public static DisconnectReasonType valueOf(int i) {
                return forNumber(i);
            }

            public static DisconnectReasonType forNumber(int i) {
                switch (i) {
                    case 0:
                        return UNKNOWN;
                    case 1:
                        return METRICS_DUMP;
                    case 2:
                        return NEXT_START_WITHOUT_END_PREVIOUS;
                    default:
                        return null;
                }
            }

            public static Internal.EnumLiteMap<DisconnectReasonType> internalGetValueMap() {
                return internalValueMap;
            }

            DisconnectReasonType(int i) {
                this.value = i;
            }
        }

        @Override
        public boolean hasSessionDurationSec() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public long getSessionDurationSec() {
            return this.sessionDurationSec_;
        }

        private void setSessionDurationSec(long j) {
            this.bitField0_ |= 1;
            this.sessionDurationSec_ = j;
        }

        private void clearSessionDurationSec() {
            this.bitField0_ &= -2;
            this.sessionDurationSec_ = 0L;
        }

        @Override
        public boolean hasConnectionTechnologyType() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public ConnectionTechnologyType getConnectionTechnologyType() {
            ConnectionTechnologyType connectionTechnologyTypeForNumber = ConnectionTechnologyType.forNumber(this.connectionTechnologyType_);
            return connectionTechnologyTypeForNumber == null ? ConnectionTechnologyType.CONNECTION_TECHNOLOGY_TYPE_UNKNOWN : connectionTechnologyTypeForNumber;
        }

        private void setConnectionTechnologyType(ConnectionTechnologyType connectionTechnologyType) {
            if (connectionTechnologyType == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 2;
            this.connectionTechnologyType_ = connectionTechnologyType.getNumber();
        }

        private void clearConnectionTechnologyType() {
            this.bitField0_ &= -3;
            this.connectionTechnologyType_ = 0;
        }

        @Override
        @Deprecated
        public boolean hasDisconnectReason() {
            return (this.bitField0_ & 4) == 4;
        }

        @Override
        @Deprecated
        public String getDisconnectReason() {
            return this.disconnectReason_;
        }

        @Override
        @Deprecated
        public ByteString getDisconnectReasonBytes() {
            return ByteString.copyFromUtf8(this.disconnectReason_);
        }

        private void setDisconnectReason(String str) {
            if (str == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 4;
            this.disconnectReason_ = str;
        }

        private void clearDisconnectReason() {
            this.bitField0_ &= -5;
            this.disconnectReason_ = getDefaultInstance().getDisconnectReason();
        }

        private void setDisconnectReasonBytes(ByteString byteString) {
            if (byteString == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 4;
            this.disconnectReason_ = byteString.toStringUtf8();
        }

        @Override
        public boolean hasDeviceConnectedTo() {
            return (this.bitField0_ & 8) == 8;
        }

        @Override
        public DeviceInfo getDeviceConnectedTo() {
            return this.deviceConnectedTo_ == null ? DeviceInfo.getDefaultInstance() : this.deviceConnectedTo_;
        }

        private void setDeviceConnectedTo(DeviceInfo deviceInfo) {
            if (deviceInfo == null) {
                throw new NullPointerException();
            }
            this.deviceConnectedTo_ = deviceInfo;
            this.bitField0_ |= 8;
        }

        private void setDeviceConnectedTo(DeviceInfo.Builder builder) {
            this.deviceConnectedTo_ = builder.build();
            this.bitField0_ |= 8;
        }

        private void mergeDeviceConnectedTo(DeviceInfo deviceInfo) {
            if (this.deviceConnectedTo_ != null && this.deviceConnectedTo_ != DeviceInfo.getDefaultInstance()) {
                this.deviceConnectedTo_ = DeviceInfo.newBuilder(this.deviceConnectedTo_).mergeFrom(deviceInfo).buildPartial();
            } else {
                this.deviceConnectedTo_ = deviceInfo;
            }
            this.bitField0_ |= 8;
        }

        private void clearDeviceConnectedTo() {
            this.deviceConnectedTo_ = null;
            this.bitField0_ &= -9;
        }

        @Override
        public boolean hasRfcommSession() {
            return (this.bitField0_ & 16) == 16;
        }

        @Override
        public RFCommSession getRfcommSession() {
            return this.rfcommSession_ == null ? RFCommSession.getDefaultInstance() : this.rfcommSession_;
        }

        private void setRfcommSession(RFCommSession rFCommSession) {
            if (rFCommSession == null) {
                throw new NullPointerException();
            }
            this.rfcommSession_ = rFCommSession;
            this.bitField0_ |= 16;
        }

        private void setRfcommSession(RFCommSession.Builder builder) {
            this.rfcommSession_ = builder.build();
            this.bitField0_ |= 16;
        }

        private void mergeRfcommSession(RFCommSession rFCommSession) {
            if (this.rfcommSession_ != null && this.rfcommSession_ != RFCommSession.getDefaultInstance()) {
                this.rfcommSession_ = RFCommSession.newBuilder(this.rfcommSession_).mergeFrom(rFCommSession).buildPartial();
            } else {
                this.rfcommSession_ = rFCommSession;
            }
            this.bitField0_ |= 16;
        }

        private void clearRfcommSession() {
            this.rfcommSession_ = null;
            this.bitField0_ &= -17;
        }

        @Override
        public boolean hasA2DpSession() {
            return (this.bitField0_ & 32) == 32;
        }

        @Override
        public A2DPSession getA2DpSession() {
            return this.a2DpSession_ == null ? A2DPSession.getDefaultInstance() : this.a2DpSession_;
        }

        private void setA2DpSession(A2DPSession a2DPSession) {
            if (a2DPSession == null) {
                throw new NullPointerException();
            }
            this.a2DpSession_ = a2DPSession;
            this.bitField0_ |= 32;
        }

        private void setA2DpSession(A2DPSession.Builder builder) {
            this.a2DpSession_ = builder.build();
            this.bitField0_ |= 32;
        }

        private void mergeA2DpSession(A2DPSession a2DPSession) {
            if (this.a2DpSession_ != null && this.a2DpSession_ != A2DPSession.getDefaultInstance()) {
                this.a2DpSession_ = A2DPSession.newBuilder(this.a2DpSession_).mergeFrom(a2DPSession).buildPartial();
            } else {
                this.a2DpSession_ = a2DPSession;
            }
            this.bitField0_ |= 32;
        }

        private void clearA2DpSession() {
            this.a2DpSession_ = null;
            this.bitField0_ &= -33;
        }

        @Override
        public boolean hasDisconnectReasonType() {
            return (this.bitField0_ & 64) == 64;
        }

        @Override
        public DisconnectReasonType getDisconnectReasonType() {
            DisconnectReasonType disconnectReasonTypeForNumber = DisconnectReasonType.forNumber(this.disconnectReasonType_);
            return disconnectReasonTypeForNumber == null ? DisconnectReasonType.UNKNOWN : disconnectReasonTypeForNumber;
        }

        private void setDisconnectReasonType(DisconnectReasonType disconnectReasonType) {
            if (disconnectReasonType == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 64;
            this.disconnectReasonType_ = disconnectReasonType.getNumber();
        }

        private void clearDisconnectReasonType() {
            this.bitField0_ &= -65;
            this.disconnectReasonType_ = 0;
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeInt64(2, this.sessionDurationSec_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeEnum(3, this.connectionTechnologyType_);
            }
            if ((this.bitField0_ & 4) == 4) {
                codedOutputStream.writeString(4, getDisconnectReason());
            }
            if ((this.bitField0_ & 8) == 8) {
                codedOutputStream.writeMessage(5, getDeviceConnectedTo());
            }
            if ((this.bitField0_ & 16) == 16) {
                codedOutputStream.writeMessage(6, getRfcommSession());
            }
            if ((this.bitField0_ & 32) == 32) {
                codedOutputStream.writeMessage(7, getA2DpSession());
            }
            if ((this.bitField0_ & 64) == 64) {
                codedOutputStream.writeEnum(8, this.disconnectReasonType_);
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeInt64Size = (this.bitField0_ & 1) == 1 ? 0 + CodedOutputStream.computeInt64Size(2, this.sessionDurationSec_) : 0;
            if ((this.bitField0_ & 2) == 2) {
                iComputeInt64Size += CodedOutputStream.computeEnumSize(3, this.connectionTechnologyType_);
            }
            if ((this.bitField0_ & 4) == 4) {
                iComputeInt64Size += CodedOutputStream.computeStringSize(4, getDisconnectReason());
            }
            if ((this.bitField0_ & 8) == 8) {
                iComputeInt64Size += CodedOutputStream.computeMessageSize(5, getDeviceConnectedTo());
            }
            if ((this.bitField0_ & 16) == 16) {
                iComputeInt64Size += CodedOutputStream.computeMessageSize(6, getRfcommSession());
            }
            if ((this.bitField0_ & 32) == 32) {
                iComputeInt64Size += CodedOutputStream.computeMessageSize(7, getA2DpSession());
            }
            if ((this.bitField0_ & 64) == 64) {
                iComputeInt64Size += CodedOutputStream.computeEnumSize(8, this.disconnectReasonType_);
            }
            int serializedSize = iComputeInt64Size + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static BluetoothSession parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (BluetoothSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static BluetoothSession parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (BluetoothSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static BluetoothSession parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (BluetoothSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static BluetoothSession parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (BluetoothSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static BluetoothSession parseFrom(InputStream inputStream) throws IOException {
            return (BluetoothSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static BluetoothSession parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (BluetoothSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static BluetoothSession parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (BluetoothSession) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static BluetoothSession parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (BluetoothSession) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static BluetoothSession parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (BluetoothSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static BluetoothSession parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (BluetoothSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(BluetoothSession bluetoothSession) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(bluetoothSession);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<BluetoothSession, Builder> implements BluetoothSessionOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(BluetoothSession.DEFAULT_INSTANCE);
            }

            @Override
            public boolean hasSessionDurationSec() {
                return ((BluetoothSession) this.instance).hasSessionDurationSec();
            }

            @Override
            public long getSessionDurationSec() {
                return ((BluetoothSession) this.instance).getSessionDurationSec();
            }

            public Builder setSessionDurationSec(long j) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setSessionDurationSec(j);
                return this;
            }

            public Builder clearSessionDurationSec() {
                copyOnWrite();
                ((BluetoothSession) this.instance).clearSessionDurationSec();
                return this;
            }

            @Override
            public boolean hasConnectionTechnologyType() {
                return ((BluetoothSession) this.instance).hasConnectionTechnologyType();
            }

            @Override
            public ConnectionTechnologyType getConnectionTechnologyType() {
                return ((BluetoothSession) this.instance).getConnectionTechnologyType();
            }

            public Builder setConnectionTechnologyType(ConnectionTechnologyType connectionTechnologyType) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setConnectionTechnologyType(connectionTechnologyType);
                return this;
            }

            public Builder clearConnectionTechnologyType() {
                copyOnWrite();
                ((BluetoothSession) this.instance).clearConnectionTechnologyType();
                return this;
            }

            @Override
            @Deprecated
            public boolean hasDisconnectReason() {
                return ((BluetoothSession) this.instance).hasDisconnectReason();
            }

            @Override
            @Deprecated
            public String getDisconnectReason() {
                return ((BluetoothSession) this.instance).getDisconnectReason();
            }

            @Override
            @Deprecated
            public ByteString getDisconnectReasonBytes() {
                return ((BluetoothSession) this.instance).getDisconnectReasonBytes();
            }

            @Deprecated
            public Builder setDisconnectReason(String str) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setDisconnectReason(str);
                return this;
            }

            @Deprecated
            public Builder clearDisconnectReason() {
                copyOnWrite();
                ((BluetoothSession) this.instance).clearDisconnectReason();
                return this;
            }

            @Deprecated
            public Builder setDisconnectReasonBytes(ByteString byteString) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setDisconnectReasonBytes(byteString);
                return this;
            }

            @Override
            public boolean hasDeviceConnectedTo() {
                return ((BluetoothSession) this.instance).hasDeviceConnectedTo();
            }

            @Override
            public DeviceInfo getDeviceConnectedTo() {
                return ((BluetoothSession) this.instance).getDeviceConnectedTo();
            }

            public Builder setDeviceConnectedTo(DeviceInfo deviceInfo) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setDeviceConnectedTo(deviceInfo);
                return this;
            }

            public Builder setDeviceConnectedTo(DeviceInfo.Builder builder) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setDeviceConnectedTo(builder);
                return this;
            }

            public Builder mergeDeviceConnectedTo(DeviceInfo deviceInfo) {
                copyOnWrite();
                ((BluetoothSession) this.instance).mergeDeviceConnectedTo(deviceInfo);
                return this;
            }

            public Builder clearDeviceConnectedTo() {
                copyOnWrite();
                ((BluetoothSession) this.instance).clearDeviceConnectedTo();
                return this;
            }

            @Override
            public boolean hasRfcommSession() {
                return ((BluetoothSession) this.instance).hasRfcommSession();
            }

            @Override
            public RFCommSession getRfcommSession() {
                return ((BluetoothSession) this.instance).getRfcommSession();
            }

            public Builder setRfcommSession(RFCommSession rFCommSession) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setRfcommSession(rFCommSession);
                return this;
            }

            public Builder setRfcommSession(RFCommSession.Builder builder) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setRfcommSession(builder);
                return this;
            }

            public Builder mergeRfcommSession(RFCommSession rFCommSession) {
                copyOnWrite();
                ((BluetoothSession) this.instance).mergeRfcommSession(rFCommSession);
                return this;
            }

            public Builder clearRfcommSession() {
                copyOnWrite();
                ((BluetoothSession) this.instance).clearRfcommSession();
                return this;
            }

            @Override
            public boolean hasA2DpSession() {
                return ((BluetoothSession) this.instance).hasA2DpSession();
            }

            @Override
            public A2DPSession getA2DpSession() {
                return ((BluetoothSession) this.instance).getA2DpSession();
            }

            public Builder setA2DpSession(A2DPSession a2DPSession) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setA2DpSession(a2DPSession);
                return this;
            }

            public Builder setA2DpSession(A2DPSession.Builder builder) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setA2DpSession(builder);
                return this;
            }

            public Builder mergeA2DpSession(A2DPSession a2DPSession) {
                copyOnWrite();
                ((BluetoothSession) this.instance).mergeA2DpSession(a2DPSession);
                return this;
            }

            public Builder clearA2DpSession() {
                copyOnWrite();
                ((BluetoothSession) this.instance).clearA2DpSession();
                return this;
            }

            @Override
            public boolean hasDisconnectReasonType() {
                return ((BluetoothSession) this.instance).hasDisconnectReasonType();
            }

            @Override
            public DisconnectReasonType getDisconnectReasonType() {
                return ((BluetoothSession) this.instance).getDisconnectReasonType();
            }

            public Builder setDisconnectReasonType(DisconnectReasonType disconnectReasonType) {
                copyOnWrite();
                ((BluetoothSession) this.instance).setDisconnectReasonType(disconnectReasonType);
                return this;
            }

            public Builder clearDisconnectReasonType() {
                copyOnWrite();
                ((BluetoothSession) this.instance).clearDisconnectReasonType();
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            A2DPSession.Builder builder;
            RFCommSession.Builder builder2;
            DeviceInfo.Builder builder3;
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new BluetoothSession();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    BluetoothSession bluetoothSession = (BluetoothSession) obj2;
                    this.sessionDurationSec_ = visitor.visitLong(hasSessionDurationSec(), this.sessionDurationSec_, bluetoothSession.hasSessionDurationSec(), bluetoothSession.sessionDurationSec_);
                    this.connectionTechnologyType_ = visitor.visitInt(hasConnectionTechnologyType(), this.connectionTechnologyType_, bluetoothSession.hasConnectionTechnologyType(), bluetoothSession.connectionTechnologyType_);
                    this.disconnectReason_ = visitor.visitString(hasDisconnectReason(), this.disconnectReason_, bluetoothSession.hasDisconnectReason(), bluetoothSession.disconnectReason_);
                    this.deviceConnectedTo_ = (DeviceInfo) visitor.visitMessage(this.deviceConnectedTo_, bluetoothSession.deviceConnectedTo_);
                    this.rfcommSession_ = (RFCommSession) visitor.visitMessage(this.rfcommSession_, bluetoothSession.rfcommSession_);
                    this.a2DpSession_ = (A2DPSession) visitor.visitMessage(this.a2DpSession_, bluetoothSession.a2DpSession_);
                    this.disconnectReasonType_ = visitor.visitInt(hasDisconnectReasonType(), this.disconnectReasonType_, bluetoothSession.hasDisconnectReasonType(), bluetoothSession.disconnectReasonType_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= bluetoothSession.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    ExtensionRegistryLite extensionRegistryLite = (ExtensionRegistryLite) obj2;
                    boolean z = false;
                    while (!z) {
                        try {
                            int tag = codedInputStream.readTag();
                            if (tag != 0) {
                                if (tag == 16) {
                                    this.bitField0_ |= 1;
                                    this.sessionDurationSec_ = codedInputStream.readInt64();
                                } else if (tag == 24) {
                                    int i = codedInputStream.readEnum();
                                    if (ConnectionTechnologyType.forNumber(i) == null) {
                                        super.mergeVarintField(3, i);
                                    } else {
                                        this.bitField0_ |= 2;
                                        this.connectionTechnologyType_ = i;
                                    }
                                } else if (tag == 34) {
                                    String string = codedInputStream.readString();
                                    this.bitField0_ |= 4;
                                    this.disconnectReason_ = string;
                                } else if (tag == 42) {
                                    if ((this.bitField0_ & 8) == 8) {
                                        builder3 = this.deviceConnectedTo_.toBuilder();
                                    } else {
                                        builder3 = null;
                                    }
                                    this.deviceConnectedTo_ = (DeviceInfo) codedInputStream.readMessage(DeviceInfo.parser(), extensionRegistryLite);
                                    if (builder3 != null) {
                                        builder3.mergeFrom(this.deviceConnectedTo_);
                                        this.deviceConnectedTo_ = builder3.buildPartial();
                                    }
                                    this.bitField0_ |= 8;
                                } else if (tag == 50) {
                                    if ((this.bitField0_ & 16) == 16) {
                                        builder2 = this.rfcommSession_.toBuilder();
                                    } else {
                                        builder2 = null;
                                    }
                                    this.rfcommSession_ = (RFCommSession) codedInputStream.readMessage(RFCommSession.parser(), extensionRegistryLite);
                                    if (builder2 != null) {
                                        builder2.mergeFrom(this.rfcommSession_);
                                        this.rfcommSession_ = builder2.buildPartial();
                                    }
                                    this.bitField0_ |= 16;
                                } else if (tag == 58) {
                                    if ((this.bitField0_ & 32) == 32) {
                                        builder = this.a2DpSession_.toBuilder();
                                    } else {
                                        builder = null;
                                    }
                                    this.a2DpSession_ = (A2DPSession) codedInputStream.readMessage(A2DPSession.parser(), extensionRegistryLite);
                                    if (builder != null) {
                                        builder.mergeFrom(this.a2DpSession_);
                                        this.a2DpSession_ = builder.buildPartial();
                                    }
                                    this.bitField0_ |= 32;
                                } else if (tag != 64) {
                                    if (!parseUnknownField(tag, codedInputStream)) {
                                    }
                                } else {
                                    int i2 = codedInputStream.readEnum();
                                    if (DisconnectReasonType.forNumber(i2) == null) {
                                        super.mergeVarintField(8, i2);
                                    } else {
                                        this.bitField0_ |= 64;
                                        this.disconnectReasonType_ = i2;
                                    }
                                }
                            }
                            z = true;
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e.setUnfinishedMessage(this));
                        } catch (IOException e2) {
                            throw new RuntimeException(new InvalidProtocolBufferException(e2.getMessage()).setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (BluetoothSession.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static BluetoothSession getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<BluetoothSession> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class RFCommSession extends GeneratedMessageLite<RFCommSession, Builder> implements RFCommSessionOrBuilder {
        private static final RFCommSession DEFAULT_INSTANCE = new RFCommSession();
        private static volatile Parser<RFCommSession> PARSER = null;
        public static final int RX_BYTES_FIELD_NUMBER = 1;
        public static final int TX_BYTES_FIELD_NUMBER = 2;
        private int bitField0_;
        private int rxBytes_ = 0;
        private int txBytes_ = 0;

        private RFCommSession() {
        }

        @Override
        public boolean hasRxBytes() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public int getRxBytes() {
            return this.rxBytes_;
        }

        private void setRxBytes(int i) {
            this.bitField0_ |= 1;
            this.rxBytes_ = i;
        }

        private void clearRxBytes() {
            this.bitField0_ &= -2;
            this.rxBytes_ = 0;
        }

        @Override
        public boolean hasTxBytes() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public int getTxBytes() {
            return this.txBytes_;
        }

        private void setTxBytes(int i) {
            this.bitField0_ |= 2;
            this.txBytes_ = i;
        }

        private void clearTxBytes() {
            this.bitField0_ &= -3;
            this.txBytes_ = 0;
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeInt32(1, this.rxBytes_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeInt32(2, this.txBytes_);
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeInt32Size = (this.bitField0_ & 1) == 1 ? 0 + CodedOutputStream.computeInt32Size(1, this.rxBytes_) : 0;
            if ((this.bitField0_ & 2) == 2) {
                iComputeInt32Size += CodedOutputStream.computeInt32Size(2, this.txBytes_);
            }
            int serializedSize = iComputeInt32Size + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static RFCommSession parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (RFCommSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static RFCommSession parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (RFCommSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static RFCommSession parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (RFCommSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static RFCommSession parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (RFCommSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static RFCommSession parseFrom(InputStream inputStream) throws IOException {
            return (RFCommSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static RFCommSession parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (RFCommSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static RFCommSession parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (RFCommSession) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static RFCommSession parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (RFCommSession) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static RFCommSession parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (RFCommSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static RFCommSession parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (RFCommSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(RFCommSession rFCommSession) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(rFCommSession);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<RFCommSession, Builder> implements RFCommSessionOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(RFCommSession.DEFAULT_INSTANCE);
            }

            @Override
            public boolean hasRxBytes() {
                return ((RFCommSession) this.instance).hasRxBytes();
            }

            @Override
            public int getRxBytes() {
                return ((RFCommSession) this.instance).getRxBytes();
            }

            public Builder setRxBytes(int i) {
                copyOnWrite();
                ((RFCommSession) this.instance).setRxBytes(i);
                return this;
            }

            public Builder clearRxBytes() {
                copyOnWrite();
                ((RFCommSession) this.instance).clearRxBytes();
                return this;
            }

            @Override
            public boolean hasTxBytes() {
                return ((RFCommSession) this.instance).hasTxBytes();
            }

            @Override
            public int getTxBytes() {
                return ((RFCommSession) this.instance).getTxBytes();
            }

            public Builder setTxBytes(int i) {
                copyOnWrite();
                ((RFCommSession) this.instance).setTxBytes(i);
                return this;
            }

            public Builder clearTxBytes() {
                copyOnWrite();
                ((RFCommSession) this.instance).clearTxBytes();
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new RFCommSession();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    RFCommSession rFCommSession = (RFCommSession) obj2;
                    this.rxBytes_ = visitor.visitInt(hasRxBytes(), this.rxBytes_, rFCommSession.hasRxBytes(), rFCommSession.rxBytes_);
                    this.txBytes_ = visitor.visitInt(hasTxBytes(), this.txBytes_, rFCommSession.hasTxBytes(), rFCommSession.txBytes_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= rFCommSession.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    boolean z = false;
                    while (!z) {
                        try {
                            int tag = codedInputStream.readTag();
                            if (tag != 0) {
                                if (tag == 8) {
                                    this.bitField0_ |= 1;
                                    this.rxBytes_ = codedInputStream.readInt32();
                                } else if (tag != 16) {
                                    if (!parseUnknownField(tag, codedInputStream)) {
                                    }
                                } else {
                                    this.bitField0_ |= 2;
                                    this.txBytes_ = codedInputStream.readInt32();
                                }
                            }
                            z = true;
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e.setUnfinishedMessage(this));
                        } catch (IOException e2) {
                            throw new RuntimeException(new InvalidProtocolBufferException(e2.getMessage()).setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (RFCommSession.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static RFCommSession getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<RFCommSession> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class A2DPSession extends GeneratedMessageLite<A2DPSession, Builder> implements A2DPSessionOrBuilder {
        public static final int AUDIO_DURATION_MILLIS_FIELD_NUMBER = 8;
        public static final int BUFFER_OVERRUNS_MAX_COUNT_FIELD_NUMBER = 4;
        public static final int BUFFER_OVERRUNS_TOTAL_FIELD_NUMBER = 5;
        public static final int BUFFER_UNDERRUNS_AVERAGE_FIELD_NUMBER = 6;
        public static final int BUFFER_UNDERRUNS_COUNT_FIELD_NUMBER = 7;
        private static final A2DPSession DEFAULT_INSTANCE = new A2DPSession();
        public static final int MEDIA_TIMER_AVG_MILLIS_FIELD_NUMBER = 3;
        public static final int MEDIA_TIMER_MAX_MILLIS_FIELD_NUMBER = 2;
        public static final int MEDIA_TIMER_MIN_MILLIS_FIELD_NUMBER = 1;
        private static volatile Parser<A2DPSession> PARSER;
        private int bitField0_;
        private int mediaTimerMinMillis_ = 0;
        private int mediaTimerMaxMillis_ = 0;
        private int mediaTimerAvgMillis_ = 0;
        private int bufferOverrunsMaxCount_ = 0;
        private int bufferOverrunsTotal_ = 0;
        private float bufferUnderrunsAverage_ = 0.0f;
        private int bufferUnderrunsCount_ = 0;
        private long audioDurationMillis_ = 0;

        private A2DPSession() {
        }

        @Override
        public boolean hasMediaTimerMinMillis() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public int getMediaTimerMinMillis() {
            return this.mediaTimerMinMillis_;
        }

        private void setMediaTimerMinMillis(int i) {
            this.bitField0_ |= 1;
            this.mediaTimerMinMillis_ = i;
        }

        private void clearMediaTimerMinMillis() {
            this.bitField0_ &= -2;
            this.mediaTimerMinMillis_ = 0;
        }

        @Override
        public boolean hasMediaTimerMaxMillis() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public int getMediaTimerMaxMillis() {
            return this.mediaTimerMaxMillis_;
        }

        private void setMediaTimerMaxMillis(int i) {
            this.bitField0_ |= 2;
            this.mediaTimerMaxMillis_ = i;
        }

        private void clearMediaTimerMaxMillis() {
            this.bitField0_ &= -3;
            this.mediaTimerMaxMillis_ = 0;
        }

        @Override
        public boolean hasMediaTimerAvgMillis() {
            return (this.bitField0_ & 4) == 4;
        }

        @Override
        public int getMediaTimerAvgMillis() {
            return this.mediaTimerAvgMillis_;
        }

        private void setMediaTimerAvgMillis(int i) {
            this.bitField0_ |= 4;
            this.mediaTimerAvgMillis_ = i;
        }

        private void clearMediaTimerAvgMillis() {
            this.bitField0_ &= -5;
            this.mediaTimerAvgMillis_ = 0;
        }

        @Override
        public boolean hasBufferOverrunsMaxCount() {
            return (this.bitField0_ & 8) == 8;
        }

        @Override
        public int getBufferOverrunsMaxCount() {
            return this.bufferOverrunsMaxCount_;
        }

        private void setBufferOverrunsMaxCount(int i) {
            this.bitField0_ |= 8;
            this.bufferOverrunsMaxCount_ = i;
        }

        private void clearBufferOverrunsMaxCount() {
            this.bitField0_ &= -9;
            this.bufferOverrunsMaxCount_ = 0;
        }

        @Override
        public boolean hasBufferOverrunsTotal() {
            return (this.bitField0_ & 16) == 16;
        }

        @Override
        public int getBufferOverrunsTotal() {
            return this.bufferOverrunsTotal_;
        }

        private void setBufferOverrunsTotal(int i) {
            this.bitField0_ |= 16;
            this.bufferOverrunsTotal_ = i;
        }

        private void clearBufferOverrunsTotal() {
            this.bitField0_ &= -17;
            this.bufferOverrunsTotal_ = 0;
        }

        @Override
        public boolean hasBufferUnderrunsAverage() {
            return (this.bitField0_ & 32) == 32;
        }

        @Override
        public float getBufferUnderrunsAverage() {
            return this.bufferUnderrunsAverage_;
        }

        private void setBufferUnderrunsAverage(float f) {
            this.bitField0_ |= 32;
            this.bufferUnderrunsAverage_ = f;
        }

        private void clearBufferUnderrunsAverage() {
            this.bitField0_ &= -33;
            this.bufferUnderrunsAverage_ = 0.0f;
        }

        @Override
        public boolean hasBufferUnderrunsCount() {
            return (this.bitField0_ & 64) == 64;
        }

        @Override
        public int getBufferUnderrunsCount() {
            return this.bufferUnderrunsCount_;
        }

        private void setBufferUnderrunsCount(int i) {
            this.bitField0_ |= 64;
            this.bufferUnderrunsCount_ = i;
        }

        private void clearBufferUnderrunsCount() {
            this.bitField0_ &= -65;
            this.bufferUnderrunsCount_ = 0;
        }

        @Override
        public boolean hasAudioDurationMillis() {
            return (this.bitField0_ & 128) == 128;
        }

        @Override
        public long getAudioDurationMillis() {
            return this.audioDurationMillis_;
        }

        private void setAudioDurationMillis(long j) {
            this.bitField0_ |= 128;
            this.audioDurationMillis_ = j;
        }

        private void clearAudioDurationMillis() {
            this.bitField0_ &= -129;
            this.audioDurationMillis_ = 0L;
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeInt32(1, this.mediaTimerMinMillis_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeInt32(2, this.mediaTimerMaxMillis_);
            }
            if ((this.bitField0_ & 4) == 4) {
                codedOutputStream.writeInt32(3, this.mediaTimerAvgMillis_);
            }
            if ((this.bitField0_ & 8) == 8) {
                codedOutputStream.writeInt32(4, this.bufferOverrunsMaxCount_);
            }
            if ((this.bitField0_ & 16) == 16) {
                codedOutputStream.writeInt32(5, this.bufferOverrunsTotal_);
            }
            if ((this.bitField0_ & 32) == 32) {
                codedOutputStream.writeFloat(6, this.bufferUnderrunsAverage_);
            }
            if ((this.bitField0_ & 64) == 64) {
                codedOutputStream.writeInt32(7, this.bufferUnderrunsCount_);
            }
            if ((this.bitField0_ & 128) == 128) {
                codedOutputStream.writeInt64(8, this.audioDurationMillis_);
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeInt32Size = (this.bitField0_ & 1) == 1 ? 0 + CodedOutputStream.computeInt32Size(1, this.mediaTimerMinMillis_) : 0;
            if ((this.bitField0_ & 2) == 2) {
                iComputeInt32Size += CodedOutputStream.computeInt32Size(2, this.mediaTimerMaxMillis_);
            }
            if ((this.bitField0_ & 4) == 4) {
                iComputeInt32Size += CodedOutputStream.computeInt32Size(3, this.mediaTimerAvgMillis_);
            }
            if ((this.bitField0_ & 8) == 8) {
                iComputeInt32Size += CodedOutputStream.computeInt32Size(4, this.bufferOverrunsMaxCount_);
            }
            if ((this.bitField0_ & 16) == 16) {
                iComputeInt32Size += CodedOutputStream.computeInt32Size(5, this.bufferOverrunsTotal_);
            }
            if ((this.bitField0_ & 32) == 32) {
                iComputeInt32Size += CodedOutputStream.computeFloatSize(6, this.bufferUnderrunsAverage_);
            }
            if ((this.bitField0_ & 64) == 64) {
                iComputeInt32Size += CodedOutputStream.computeInt32Size(7, this.bufferUnderrunsCount_);
            }
            if ((this.bitField0_ & 128) == 128) {
                iComputeInt32Size += CodedOutputStream.computeInt64Size(8, this.audioDurationMillis_);
            }
            int serializedSize = iComputeInt32Size + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static A2DPSession parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (A2DPSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static A2DPSession parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (A2DPSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static A2DPSession parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (A2DPSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static A2DPSession parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (A2DPSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static A2DPSession parseFrom(InputStream inputStream) throws IOException {
            return (A2DPSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static A2DPSession parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (A2DPSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static A2DPSession parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (A2DPSession) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static A2DPSession parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (A2DPSession) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static A2DPSession parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (A2DPSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static A2DPSession parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (A2DPSession) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(A2DPSession a2DPSession) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(a2DPSession);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<A2DPSession, Builder> implements A2DPSessionOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(A2DPSession.DEFAULT_INSTANCE);
            }

            @Override
            public boolean hasMediaTimerMinMillis() {
                return ((A2DPSession) this.instance).hasMediaTimerMinMillis();
            }

            @Override
            public int getMediaTimerMinMillis() {
                return ((A2DPSession) this.instance).getMediaTimerMinMillis();
            }

            public Builder setMediaTimerMinMillis(int i) {
                copyOnWrite();
                ((A2DPSession) this.instance).setMediaTimerMinMillis(i);
                return this;
            }

            public Builder clearMediaTimerMinMillis() {
                copyOnWrite();
                ((A2DPSession) this.instance).clearMediaTimerMinMillis();
                return this;
            }

            @Override
            public boolean hasMediaTimerMaxMillis() {
                return ((A2DPSession) this.instance).hasMediaTimerMaxMillis();
            }

            @Override
            public int getMediaTimerMaxMillis() {
                return ((A2DPSession) this.instance).getMediaTimerMaxMillis();
            }

            public Builder setMediaTimerMaxMillis(int i) {
                copyOnWrite();
                ((A2DPSession) this.instance).setMediaTimerMaxMillis(i);
                return this;
            }

            public Builder clearMediaTimerMaxMillis() {
                copyOnWrite();
                ((A2DPSession) this.instance).clearMediaTimerMaxMillis();
                return this;
            }

            @Override
            public boolean hasMediaTimerAvgMillis() {
                return ((A2DPSession) this.instance).hasMediaTimerAvgMillis();
            }

            @Override
            public int getMediaTimerAvgMillis() {
                return ((A2DPSession) this.instance).getMediaTimerAvgMillis();
            }

            public Builder setMediaTimerAvgMillis(int i) {
                copyOnWrite();
                ((A2DPSession) this.instance).setMediaTimerAvgMillis(i);
                return this;
            }

            public Builder clearMediaTimerAvgMillis() {
                copyOnWrite();
                ((A2DPSession) this.instance).clearMediaTimerAvgMillis();
                return this;
            }

            @Override
            public boolean hasBufferOverrunsMaxCount() {
                return ((A2DPSession) this.instance).hasBufferOverrunsMaxCount();
            }

            @Override
            public int getBufferOverrunsMaxCount() {
                return ((A2DPSession) this.instance).getBufferOverrunsMaxCount();
            }

            public Builder setBufferOverrunsMaxCount(int i) {
                copyOnWrite();
                ((A2DPSession) this.instance).setBufferOverrunsMaxCount(i);
                return this;
            }

            public Builder clearBufferOverrunsMaxCount() {
                copyOnWrite();
                ((A2DPSession) this.instance).clearBufferOverrunsMaxCount();
                return this;
            }

            @Override
            public boolean hasBufferOverrunsTotal() {
                return ((A2DPSession) this.instance).hasBufferOverrunsTotal();
            }

            @Override
            public int getBufferOverrunsTotal() {
                return ((A2DPSession) this.instance).getBufferOverrunsTotal();
            }

            public Builder setBufferOverrunsTotal(int i) {
                copyOnWrite();
                ((A2DPSession) this.instance).setBufferOverrunsTotal(i);
                return this;
            }

            public Builder clearBufferOverrunsTotal() {
                copyOnWrite();
                ((A2DPSession) this.instance).clearBufferOverrunsTotal();
                return this;
            }

            @Override
            public boolean hasBufferUnderrunsAverage() {
                return ((A2DPSession) this.instance).hasBufferUnderrunsAverage();
            }

            @Override
            public float getBufferUnderrunsAverage() {
                return ((A2DPSession) this.instance).getBufferUnderrunsAverage();
            }

            public Builder setBufferUnderrunsAverage(float f) {
                copyOnWrite();
                ((A2DPSession) this.instance).setBufferUnderrunsAverage(f);
                return this;
            }

            public Builder clearBufferUnderrunsAverage() {
                copyOnWrite();
                ((A2DPSession) this.instance).clearBufferUnderrunsAverage();
                return this;
            }

            @Override
            public boolean hasBufferUnderrunsCount() {
                return ((A2DPSession) this.instance).hasBufferUnderrunsCount();
            }

            @Override
            public int getBufferUnderrunsCount() {
                return ((A2DPSession) this.instance).getBufferUnderrunsCount();
            }

            public Builder setBufferUnderrunsCount(int i) {
                copyOnWrite();
                ((A2DPSession) this.instance).setBufferUnderrunsCount(i);
                return this;
            }

            public Builder clearBufferUnderrunsCount() {
                copyOnWrite();
                ((A2DPSession) this.instance).clearBufferUnderrunsCount();
                return this;
            }

            @Override
            public boolean hasAudioDurationMillis() {
                return ((A2DPSession) this.instance).hasAudioDurationMillis();
            }

            @Override
            public long getAudioDurationMillis() {
                return ((A2DPSession) this.instance).getAudioDurationMillis();
            }

            public Builder setAudioDurationMillis(long j) {
                copyOnWrite();
                ((A2DPSession) this.instance).setAudioDurationMillis(j);
                return this;
            }

            public Builder clearAudioDurationMillis() {
                copyOnWrite();
                ((A2DPSession) this.instance).clearAudioDurationMillis();
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new A2DPSession();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    A2DPSession a2DPSession = (A2DPSession) obj2;
                    this.mediaTimerMinMillis_ = visitor.visitInt(hasMediaTimerMinMillis(), this.mediaTimerMinMillis_, a2DPSession.hasMediaTimerMinMillis(), a2DPSession.mediaTimerMinMillis_);
                    this.mediaTimerMaxMillis_ = visitor.visitInt(hasMediaTimerMaxMillis(), this.mediaTimerMaxMillis_, a2DPSession.hasMediaTimerMaxMillis(), a2DPSession.mediaTimerMaxMillis_);
                    this.mediaTimerAvgMillis_ = visitor.visitInt(hasMediaTimerAvgMillis(), this.mediaTimerAvgMillis_, a2DPSession.hasMediaTimerAvgMillis(), a2DPSession.mediaTimerAvgMillis_);
                    this.bufferOverrunsMaxCount_ = visitor.visitInt(hasBufferOverrunsMaxCount(), this.bufferOverrunsMaxCount_, a2DPSession.hasBufferOverrunsMaxCount(), a2DPSession.bufferOverrunsMaxCount_);
                    this.bufferOverrunsTotal_ = visitor.visitInt(hasBufferOverrunsTotal(), this.bufferOverrunsTotal_, a2DPSession.hasBufferOverrunsTotal(), a2DPSession.bufferOverrunsTotal_);
                    this.bufferUnderrunsAverage_ = visitor.visitFloat(hasBufferUnderrunsAverage(), this.bufferUnderrunsAverage_, a2DPSession.hasBufferUnderrunsAverage(), a2DPSession.bufferUnderrunsAverage_);
                    this.bufferUnderrunsCount_ = visitor.visitInt(hasBufferUnderrunsCount(), this.bufferUnderrunsCount_, a2DPSession.hasBufferUnderrunsCount(), a2DPSession.bufferUnderrunsCount_);
                    this.audioDurationMillis_ = visitor.visitLong(hasAudioDurationMillis(), this.audioDurationMillis_, a2DPSession.hasAudioDurationMillis(), a2DPSession.audioDurationMillis_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= a2DPSession.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    boolean z = false;
                    while (!z) {
                        try {
                            int tag = codedInputStream.readTag();
                            if (tag != 0) {
                                if (tag == 8) {
                                    this.bitField0_ |= 1;
                                    this.mediaTimerMinMillis_ = codedInputStream.readInt32();
                                } else if (tag == 16) {
                                    this.bitField0_ |= 2;
                                    this.mediaTimerMaxMillis_ = codedInputStream.readInt32();
                                } else if (tag == 24) {
                                    this.bitField0_ |= 4;
                                    this.mediaTimerAvgMillis_ = codedInputStream.readInt32();
                                } else if (tag == 32) {
                                    this.bitField0_ |= 8;
                                    this.bufferOverrunsMaxCount_ = codedInputStream.readInt32();
                                } else if (tag == 40) {
                                    this.bitField0_ |= 16;
                                    this.bufferOverrunsTotal_ = codedInputStream.readInt32();
                                } else if (tag == 53) {
                                    this.bitField0_ |= 32;
                                    this.bufferUnderrunsAverage_ = codedInputStream.readFloat();
                                } else if (tag == 56) {
                                    this.bitField0_ |= 64;
                                    this.bufferUnderrunsCount_ = codedInputStream.readInt32();
                                } else if (tag != 64) {
                                    if (!parseUnknownField(tag, codedInputStream)) {
                                    }
                                } else {
                                    this.bitField0_ |= 128;
                                    this.audioDurationMillis_ = codedInputStream.readInt64();
                                }
                            }
                            z = true;
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e.setUnfinishedMessage(this));
                        } catch (IOException e2) {
                            throw new RuntimeException(new InvalidProtocolBufferException(e2.getMessage()).setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (A2DPSession.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static A2DPSession getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<A2DPSession> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class PairEvent extends GeneratedMessageLite<PairEvent, Builder> implements PairEventOrBuilder {
        private static final PairEvent DEFAULT_INSTANCE = new PairEvent();
        public static final int DEVICE_PAIRED_WITH_FIELD_NUMBER = 3;
        public static final int DISCONNECT_REASON_FIELD_NUMBER = 1;
        public static final int EVENT_TIME_MILLIS_FIELD_NUMBER = 2;
        private static volatile Parser<PairEvent> PARSER;
        private int bitField0_;
        private DeviceInfo devicePairedWith_;
        private int disconnectReason_ = 0;
        private long eventTimeMillis_ = 0;

        private PairEvent() {
        }

        @Override
        public boolean hasDisconnectReason() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public int getDisconnectReason() {
            return this.disconnectReason_;
        }

        private void setDisconnectReason(int i) {
            this.bitField0_ |= 1;
            this.disconnectReason_ = i;
        }

        private void clearDisconnectReason() {
            this.bitField0_ &= -2;
            this.disconnectReason_ = 0;
        }

        @Override
        public boolean hasEventTimeMillis() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public long getEventTimeMillis() {
            return this.eventTimeMillis_;
        }

        private void setEventTimeMillis(long j) {
            this.bitField0_ |= 2;
            this.eventTimeMillis_ = j;
        }

        private void clearEventTimeMillis() {
            this.bitField0_ &= -3;
            this.eventTimeMillis_ = 0L;
        }

        @Override
        public boolean hasDevicePairedWith() {
            return (this.bitField0_ & 4) == 4;
        }

        @Override
        public DeviceInfo getDevicePairedWith() {
            return this.devicePairedWith_ == null ? DeviceInfo.getDefaultInstance() : this.devicePairedWith_;
        }

        private void setDevicePairedWith(DeviceInfo deviceInfo) {
            if (deviceInfo == null) {
                throw new NullPointerException();
            }
            this.devicePairedWith_ = deviceInfo;
            this.bitField0_ |= 4;
        }

        private void setDevicePairedWith(DeviceInfo.Builder builder) {
            this.devicePairedWith_ = builder.build();
            this.bitField0_ |= 4;
        }

        private void mergeDevicePairedWith(DeviceInfo deviceInfo) {
            if (this.devicePairedWith_ != null && this.devicePairedWith_ != DeviceInfo.getDefaultInstance()) {
                this.devicePairedWith_ = DeviceInfo.newBuilder(this.devicePairedWith_).mergeFrom(deviceInfo).buildPartial();
            } else {
                this.devicePairedWith_ = deviceInfo;
            }
            this.bitField0_ |= 4;
        }

        private void clearDevicePairedWith() {
            this.devicePairedWith_ = null;
            this.bitField0_ &= -5;
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeInt32(1, this.disconnectReason_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeInt64(2, this.eventTimeMillis_);
            }
            if ((this.bitField0_ & 4) == 4) {
                codedOutputStream.writeMessage(3, getDevicePairedWith());
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeInt32Size = (this.bitField0_ & 1) == 1 ? 0 + CodedOutputStream.computeInt32Size(1, this.disconnectReason_) : 0;
            if ((this.bitField0_ & 2) == 2) {
                iComputeInt32Size += CodedOutputStream.computeInt64Size(2, this.eventTimeMillis_);
            }
            if ((this.bitField0_ & 4) == 4) {
                iComputeInt32Size += CodedOutputStream.computeMessageSize(3, getDevicePairedWith());
            }
            int serializedSize = iComputeInt32Size + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static PairEvent parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (PairEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static PairEvent parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (PairEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static PairEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (PairEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static PairEvent parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (PairEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static PairEvent parseFrom(InputStream inputStream) throws IOException {
            return (PairEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static PairEvent parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (PairEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static PairEvent parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (PairEvent) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static PairEvent parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (PairEvent) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static PairEvent parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (PairEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static PairEvent parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (PairEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(PairEvent pairEvent) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(pairEvent);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<PairEvent, Builder> implements PairEventOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(PairEvent.DEFAULT_INSTANCE);
            }

            @Override
            public boolean hasDisconnectReason() {
                return ((PairEvent) this.instance).hasDisconnectReason();
            }

            @Override
            public int getDisconnectReason() {
                return ((PairEvent) this.instance).getDisconnectReason();
            }

            public Builder setDisconnectReason(int i) {
                copyOnWrite();
                ((PairEvent) this.instance).setDisconnectReason(i);
                return this;
            }

            public Builder clearDisconnectReason() {
                copyOnWrite();
                ((PairEvent) this.instance).clearDisconnectReason();
                return this;
            }

            @Override
            public boolean hasEventTimeMillis() {
                return ((PairEvent) this.instance).hasEventTimeMillis();
            }

            @Override
            public long getEventTimeMillis() {
                return ((PairEvent) this.instance).getEventTimeMillis();
            }

            public Builder setEventTimeMillis(long j) {
                copyOnWrite();
                ((PairEvent) this.instance).setEventTimeMillis(j);
                return this;
            }

            public Builder clearEventTimeMillis() {
                copyOnWrite();
                ((PairEvent) this.instance).clearEventTimeMillis();
                return this;
            }

            @Override
            public boolean hasDevicePairedWith() {
                return ((PairEvent) this.instance).hasDevicePairedWith();
            }

            @Override
            public DeviceInfo getDevicePairedWith() {
                return ((PairEvent) this.instance).getDevicePairedWith();
            }

            public Builder setDevicePairedWith(DeviceInfo deviceInfo) {
                copyOnWrite();
                ((PairEvent) this.instance).setDevicePairedWith(deviceInfo);
                return this;
            }

            public Builder setDevicePairedWith(DeviceInfo.Builder builder) {
                copyOnWrite();
                ((PairEvent) this.instance).setDevicePairedWith(builder);
                return this;
            }

            public Builder mergeDevicePairedWith(DeviceInfo deviceInfo) {
                copyOnWrite();
                ((PairEvent) this.instance).mergeDevicePairedWith(deviceInfo);
                return this;
            }

            public Builder clearDevicePairedWith() {
                copyOnWrite();
                ((PairEvent) this.instance).clearDevicePairedWith();
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            DeviceInfo.Builder builder;
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new PairEvent();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    PairEvent pairEvent = (PairEvent) obj2;
                    this.disconnectReason_ = visitor.visitInt(hasDisconnectReason(), this.disconnectReason_, pairEvent.hasDisconnectReason(), pairEvent.disconnectReason_);
                    this.eventTimeMillis_ = visitor.visitLong(hasEventTimeMillis(), this.eventTimeMillis_, pairEvent.hasEventTimeMillis(), pairEvent.eventTimeMillis_);
                    this.devicePairedWith_ = (DeviceInfo) visitor.visitMessage(this.devicePairedWith_, pairEvent.devicePairedWith_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= pairEvent.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    ExtensionRegistryLite extensionRegistryLite = (ExtensionRegistryLite) obj2;
                    boolean z = false;
                    while (!z) {
                        try {
                            int tag = codedInputStream.readTag();
                            if (tag != 0) {
                                if (tag == 8) {
                                    this.bitField0_ |= 1;
                                    this.disconnectReason_ = codedInputStream.readInt32();
                                } else if (tag == 16) {
                                    this.bitField0_ |= 2;
                                    this.eventTimeMillis_ = codedInputStream.readInt64();
                                } else if (tag != 26) {
                                    if (!parseUnknownField(tag, codedInputStream)) {
                                    }
                                } else {
                                    if ((this.bitField0_ & 4) == 4) {
                                        builder = this.devicePairedWith_.toBuilder();
                                    } else {
                                        builder = null;
                                    }
                                    this.devicePairedWith_ = (DeviceInfo) codedInputStream.readMessage(DeviceInfo.parser(), extensionRegistryLite);
                                    if (builder != null) {
                                        builder.mergeFrom(this.devicePairedWith_);
                                        this.devicePairedWith_ = builder.buildPartial();
                                    }
                                    this.bitField0_ |= 4;
                                }
                            }
                            z = true;
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e.setUnfinishedMessage(this));
                        } catch (IOException e2) {
                            throw new RuntimeException(new InvalidProtocolBufferException(e2.getMessage()).setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (PairEvent.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static PairEvent getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<PairEvent> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class WakeEvent extends GeneratedMessageLite<WakeEvent, Builder> implements WakeEventOrBuilder {
        private static final WakeEvent DEFAULT_INSTANCE = new WakeEvent();
        public static final int EVENT_TIME_MILLIS_FIELD_NUMBER = 4;
        public static final int NAME_FIELD_NUMBER = 3;
        private static volatile Parser<WakeEvent> PARSER = null;
        public static final int REQUESTOR_FIELD_NUMBER = 2;
        public static final int WAKE_EVENT_TYPE_FIELD_NUMBER = 1;
        private int bitField0_;
        private int wakeEventType_ = 0;
        private String requestor_ = "";
        private String name_ = "";
        private long eventTimeMillis_ = 0;

        private WakeEvent() {
        }

        public enum WakeEventType implements Internal.EnumLite {
            UNKNOWN(0),
            ACQUIRED(1),
            RELEASED(2);

            public static final int ACQUIRED_VALUE = 1;
            public static final int RELEASED_VALUE = 2;
            public static final int UNKNOWN_VALUE = 0;
            private static final Internal.EnumLiteMap<WakeEventType> internalValueMap = new Internal.EnumLiteMap<WakeEventType>() {
                @Override
                public WakeEventType findValueByNumber(int i) {
                    return WakeEventType.forNumber(i);
                }
            };
            private final int value;

            @Override
            public final int getNumber() {
                return this.value;
            }

            @Deprecated
            public static WakeEventType valueOf(int i) {
                return forNumber(i);
            }

            public static WakeEventType forNumber(int i) {
                switch (i) {
                    case 0:
                        return UNKNOWN;
                    case 1:
                        return ACQUIRED;
                    case 2:
                        return RELEASED;
                    default:
                        return null;
                }
            }

            public static Internal.EnumLiteMap<WakeEventType> internalGetValueMap() {
                return internalValueMap;
            }

            WakeEventType(int i) {
                this.value = i;
            }
        }

        @Override
        public boolean hasWakeEventType() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public WakeEventType getWakeEventType() {
            WakeEventType wakeEventTypeForNumber = WakeEventType.forNumber(this.wakeEventType_);
            return wakeEventTypeForNumber == null ? WakeEventType.UNKNOWN : wakeEventTypeForNumber;
        }

        private void setWakeEventType(WakeEventType wakeEventType) {
            if (wakeEventType == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 1;
            this.wakeEventType_ = wakeEventType.getNumber();
        }

        private void clearWakeEventType() {
            this.bitField0_ &= -2;
            this.wakeEventType_ = 0;
        }

        @Override
        public boolean hasRequestor() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public String getRequestor() {
            return this.requestor_;
        }

        @Override
        public ByteString getRequestorBytes() {
            return ByteString.copyFromUtf8(this.requestor_);
        }

        private void setRequestor(String str) {
            if (str == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 2;
            this.requestor_ = str;
        }

        private void clearRequestor() {
            this.bitField0_ &= -3;
            this.requestor_ = getDefaultInstance().getRequestor();
        }

        private void setRequestorBytes(ByteString byteString) {
            if (byteString == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 2;
            this.requestor_ = byteString.toStringUtf8();
        }

        @Override
        public boolean hasName() {
            return (this.bitField0_ & 4) == 4;
        }

        @Override
        public String getName() {
            return this.name_;
        }

        @Override
        public ByteString getNameBytes() {
            return ByteString.copyFromUtf8(this.name_);
        }

        private void setName(String str) {
            if (str == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 4;
            this.name_ = str;
        }

        private void clearName() {
            this.bitField0_ &= -5;
            this.name_ = getDefaultInstance().getName();
        }

        private void setNameBytes(ByteString byteString) {
            if (byteString == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 4;
            this.name_ = byteString.toStringUtf8();
        }

        @Override
        public boolean hasEventTimeMillis() {
            return (this.bitField0_ & 8) == 8;
        }

        @Override
        public long getEventTimeMillis() {
            return this.eventTimeMillis_;
        }

        private void setEventTimeMillis(long j) {
            this.bitField0_ |= 8;
            this.eventTimeMillis_ = j;
        }

        private void clearEventTimeMillis() {
            this.bitField0_ &= -9;
            this.eventTimeMillis_ = 0L;
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeEnum(1, this.wakeEventType_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeString(2, getRequestor());
            }
            if ((this.bitField0_ & 4) == 4) {
                codedOutputStream.writeString(3, getName());
            }
            if ((this.bitField0_ & 8) == 8) {
                codedOutputStream.writeInt64(4, this.eventTimeMillis_);
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeEnumSize = (this.bitField0_ & 1) == 1 ? 0 + CodedOutputStream.computeEnumSize(1, this.wakeEventType_) : 0;
            if ((this.bitField0_ & 2) == 2) {
                iComputeEnumSize += CodedOutputStream.computeStringSize(2, getRequestor());
            }
            if ((this.bitField0_ & 4) == 4) {
                iComputeEnumSize += CodedOutputStream.computeStringSize(3, getName());
            }
            if ((this.bitField0_ & 8) == 8) {
                iComputeEnumSize += CodedOutputStream.computeInt64Size(4, this.eventTimeMillis_);
            }
            int serializedSize = iComputeEnumSize + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static WakeEvent parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (WakeEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static WakeEvent parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (WakeEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static WakeEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (WakeEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static WakeEvent parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (WakeEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static WakeEvent parseFrom(InputStream inputStream) throws IOException {
            return (WakeEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static WakeEvent parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (WakeEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static WakeEvent parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (WakeEvent) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static WakeEvent parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (WakeEvent) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static WakeEvent parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (WakeEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static WakeEvent parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (WakeEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(WakeEvent wakeEvent) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(wakeEvent);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<WakeEvent, Builder> implements WakeEventOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(WakeEvent.DEFAULT_INSTANCE);
            }

            @Override
            public boolean hasWakeEventType() {
                return ((WakeEvent) this.instance).hasWakeEventType();
            }

            @Override
            public WakeEventType getWakeEventType() {
                return ((WakeEvent) this.instance).getWakeEventType();
            }

            public Builder setWakeEventType(WakeEventType wakeEventType) {
                copyOnWrite();
                ((WakeEvent) this.instance).setWakeEventType(wakeEventType);
                return this;
            }

            public Builder clearWakeEventType() {
                copyOnWrite();
                ((WakeEvent) this.instance).clearWakeEventType();
                return this;
            }

            @Override
            public boolean hasRequestor() {
                return ((WakeEvent) this.instance).hasRequestor();
            }

            @Override
            public String getRequestor() {
                return ((WakeEvent) this.instance).getRequestor();
            }

            @Override
            public ByteString getRequestorBytes() {
                return ((WakeEvent) this.instance).getRequestorBytes();
            }

            public Builder setRequestor(String str) {
                copyOnWrite();
                ((WakeEvent) this.instance).setRequestor(str);
                return this;
            }

            public Builder clearRequestor() {
                copyOnWrite();
                ((WakeEvent) this.instance).clearRequestor();
                return this;
            }

            public Builder setRequestorBytes(ByteString byteString) {
                copyOnWrite();
                ((WakeEvent) this.instance).setRequestorBytes(byteString);
                return this;
            }

            @Override
            public boolean hasName() {
                return ((WakeEvent) this.instance).hasName();
            }

            @Override
            public String getName() {
                return ((WakeEvent) this.instance).getName();
            }

            @Override
            public ByteString getNameBytes() {
                return ((WakeEvent) this.instance).getNameBytes();
            }

            public Builder setName(String str) {
                copyOnWrite();
                ((WakeEvent) this.instance).setName(str);
                return this;
            }

            public Builder clearName() {
                copyOnWrite();
                ((WakeEvent) this.instance).clearName();
                return this;
            }

            public Builder setNameBytes(ByteString byteString) {
                copyOnWrite();
                ((WakeEvent) this.instance).setNameBytes(byteString);
                return this;
            }

            @Override
            public boolean hasEventTimeMillis() {
                return ((WakeEvent) this.instance).hasEventTimeMillis();
            }

            @Override
            public long getEventTimeMillis() {
                return ((WakeEvent) this.instance).getEventTimeMillis();
            }

            public Builder setEventTimeMillis(long j) {
                copyOnWrite();
                ((WakeEvent) this.instance).setEventTimeMillis(j);
                return this;
            }

            public Builder clearEventTimeMillis() {
                copyOnWrite();
                ((WakeEvent) this.instance).clearEventTimeMillis();
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new WakeEvent();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    WakeEvent wakeEvent = (WakeEvent) obj2;
                    this.wakeEventType_ = visitor.visitInt(hasWakeEventType(), this.wakeEventType_, wakeEvent.hasWakeEventType(), wakeEvent.wakeEventType_);
                    this.requestor_ = visitor.visitString(hasRequestor(), this.requestor_, wakeEvent.hasRequestor(), wakeEvent.requestor_);
                    this.name_ = visitor.visitString(hasName(), this.name_, wakeEvent.hasName(), wakeEvent.name_);
                    this.eventTimeMillis_ = visitor.visitLong(hasEventTimeMillis(), this.eventTimeMillis_, wakeEvent.hasEventTimeMillis(), wakeEvent.eventTimeMillis_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= wakeEvent.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    boolean z = false;
                    while (!z) {
                        try {
                            try {
                                int tag = codedInputStream.readTag();
                                if (tag != 0) {
                                    if (tag == 8) {
                                        int i = codedInputStream.readEnum();
                                        if (WakeEventType.forNumber(i) != null) {
                                            this.bitField0_ = 1 | this.bitField0_;
                                            this.wakeEventType_ = i;
                                        } else {
                                            super.mergeVarintField(1, i);
                                        }
                                    } else if (tag == 18) {
                                        String string = codedInputStream.readString();
                                        this.bitField0_ |= 2;
                                        this.requestor_ = string;
                                    } else if (tag == 26) {
                                        String string2 = codedInputStream.readString();
                                        this.bitField0_ |= 4;
                                        this.name_ = string2;
                                    } else if (tag != 32) {
                                        if (!parseUnknownField(tag, codedInputStream)) {
                                        }
                                    } else {
                                        this.bitField0_ |= 8;
                                        this.eventTimeMillis_ = codedInputStream.readInt64();
                                    }
                                }
                                z = true;
                            } catch (IOException e) {
                                throw new RuntimeException(new InvalidProtocolBufferException(e.getMessage()).setUnfinishedMessage(this));
                            }
                        } catch (InvalidProtocolBufferException e2) {
                            throw new RuntimeException(e2.setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (WakeEvent.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static WakeEvent getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<WakeEvent> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class ScanEvent extends GeneratedMessageLite<ScanEvent, Builder> implements ScanEventOrBuilder {
        private static final ScanEvent DEFAULT_INSTANCE = new ScanEvent();
        public static final int EVENT_TIME_MILLIS_FIELD_NUMBER = 5;
        public static final int INITIATOR_FIELD_NUMBER = 2;
        public static final int NUMBER_RESULTS_FIELD_NUMBER = 4;
        private static volatile Parser<ScanEvent> PARSER = null;
        public static final int SCAN_EVENT_TYPE_FIELD_NUMBER = 1;
        public static final int SCAN_TECHNOLOGY_TYPE_FIELD_NUMBER = 3;
        private int bitField0_;
        private int scanEventType_ = 0;
        private String initiator_ = "";
        private int scanTechnologyType_ = 0;
        private int numberResults_ = 0;
        private long eventTimeMillis_ = 0;

        private ScanEvent() {
        }

        public enum ScanTechnologyType implements Internal.EnumLite {
            SCAN_TYPE_UNKNOWN(0),
            SCAN_TECH_TYPE_LE(1),
            SCAN_TECH_TYPE_BREDR(2),
            SCAN_TECH_TYPE_BOTH(3);

            public static final int SCAN_TECH_TYPE_BOTH_VALUE = 3;
            public static final int SCAN_TECH_TYPE_BREDR_VALUE = 2;
            public static final int SCAN_TECH_TYPE_LE_VALUE = 1;
            public static final int SCAN_TYPE_UNKNOWN_VALUE = 0;
            private static final Internal.EnumLiteMap<ScanTechnologyType> internalValueMap = new Internal.EnumLiteMap<ScanTechnologyType>() {
                @Override
                public ScanTechnologyType findValueByNumber(int i) {
                    return ScanTechnologyType.forNumber(i);
                }
            };
            private final int value;

            @Override
            public final int getNumber() {
                return this.value;
            }

            @Deprecated
            public static ScanTechnologyType valueOf(int i) {
                return forNumber(i);
            }

            public static ScanTechnologyType forNumber(int i) {
                switch (i) {
                    case 0:
                        return SCAN_TYPE_UNKNOWN;
                    case 1:
                        return SCAN_TECH_TYPE_LE;
                    case 2:
                        return SCAN_TECH_TYPE_BREDR;
                    case 3:
                        return SCAN_TECH_TYPE_BOTH;
                    default:
                        return null;
                }
            }

            public static Internal.EnumLiteMap<ScanTechnologyType> internalGetValueMap() {
                return internalValueMap;
            }

            ScanTechnologyType(int i) {
                this.value = i;
            }
        }

        public enum ScanEventType implements Internal.EnumLite {
            SCAN_EVENT_START(0),
            SCAN_EVENT_STOP(1);

            public static final int SCAN_EVENT_START_VALUE = 0;
            public static final int SCAN_EVENT_STOP_VALUE = 1;
            private static final Internal.EnumLiteMap<ScanEventType> internalValueMap = new Internal.EnumLiteMap<ScanEventType>() {
                @Override
                public ScanEventType findValueByNumber(int i) {
                    return ScanEventType.forNumber(i);
                }
            };
            private final int value;

            @Override
            public final int getNumber() {
                return this.value;
            }

            @Deprecated
            public static ScanEventType valueOf(int i) {
                return forNumber(i);
            }

            public static ScanEventType forNumber(int i) {
                switch (i) {
                    case 0:
                        return SCAN_EVENT_START;
                    case 1:
                        return SCAN_EVENT_STOP;
                    default:
                        return null;
                }
            }

            public static Internal.EnumLiteMap<ScanEventType> internalGetValueMap() {
                return internalValueMap;
            }

            ScanEventType(int i) {
                this.value = i;
            }
        }

        @Override
        public boolean hasScanEventType() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public ScanEventType getScanEventType() {
            ScanEventType scanEventTypeForNumber = ScanEventType.forNumber(this.scanEventType_);
            return scanEventTypeForNumber == null ? ScanEventType.SCAN_EVENT_START : scanEventTypeForNumber;
        }

        private void setScanEventType(ScanEventType scanEventType) {
            if (scanEventType == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 1;
            this.scanEventType_ = scanEventType.getNumber();
        }

        private void clearScanEventType() {
            this.bitField0_ &= -2;
            this.scanEventType_ = 0;
        }

        @Override
        public boolean hasInitiator() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public String getInitiator() {
            return this.initiator_;
        }

        @Override
        public ByteString getInitiatorBytes() {
            return ByteString.copyFromUtf8(this.initiator_);
        }

        private void setInitiator(String str) {
            if (str == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 2;
            this.initiator_ = str;
        }

        private void clearInitiator() {
            this.bitField0_ &= -3;
            this.initiator_ = getDefaultInstance().getInitiator();
        }

        private void setInitiatorBytes(ByteString byteString) {
            if (byteString == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 2;
            this.initiator_ = byteString.toStringUtf8();
        }

        @Override
        public boolean hasScanTechnologyType() {
            return (this.bitField0_ & 4) == 4;
        }

        @Override
        public ScanTechnologyType getScanTechnologyType() {
            ScanTechnologyType scanTechnologyTypeForNumber = ScanTechnologyType.forNumber(this.scanTechnologyType_);
            return scanTechnologyTypeForNumber == null ? ScanTechnologyType.SCAN_TYPE_UNKNOWN : scanTechnologyTypeForNumber;
        }

        private void setScanTechnologyType(ScanTechnologyType scanTechnologyType) {
            if (scanTechnologyType == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 4;
            this.scanTechnologyType_ = scanTechnologyType.getNumber();
        }

        private void clearScanTechnologyType() {
            this.bitField0_ &= -5;
            this.scanTechnologyType_ = 0;
        }

        @Override
        public boolean hasNumberResults() {
            return (this.bitField0_ & 8) == 8;
        }

        @Override
        public int getNumberResults() {
            return this.numberResults_;
        }

        private void setNumberResults(int i) {
            this.bitField0_ |= 8;
            this.numberResults_ = i;
        }

        private void clearNumberResults() {
            this.bitField0_ &= -9;
            this.numberResults_ = 0;
        }

        @Override
        public boolean hasEventTimeMillis() {
            return (this.bitField0_ & 16) == 16;
        }

        @Override
        public long getEventTimeMillis() {
            return this.eventTimeMillis_;
        }

        private void setEventTimeMillis(long j) {
            this.bitField0_ |= 16;
            this.eventTimeMillis_ = j;
        }

        private void clearEventTimeMillis() {
            this.bitField0_ &= -17;
            this.eventTimeMillis_ = 0L;
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeEnum(1, this.scanEventType_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeString(2, getInitiator());
            }
            if ((this.bitField0_ & 4) == 4) {
                codedOutputStream.writeEnum(3, this.scanTechnologyType_);
            }
            if ((this.bitField0_ & 8) == 8) {
                codedOutputStream.writeInt32(4, this.numberResults_);
            }
            if ((this.bitField0_ & 16) == 16) {
                codedOutputStream.writeInt64(5, this.eventTimeMillis_);
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeEnumSize = (this.bitField0_ & 1) == 1 ? 0 + CodedOutputStream.computeEnumSize(1, this.scanEventType_) : 0;
            if ((this.bitField0_ & 2) == 2) {
                iComputeEnumSize += CodedOutputStream.computeStringSize(2, getInitiator());
            }
            if ((this.bitField0_ & 4) == 4) {
                iComputeEnumSize += CodedOutputStream.computeEnumSize(3, this.scanTechnologyType_);
            }
            if ((this.bitField0_ & 8) == 8) {
                iComputeEnumSize += CodedOutputStream.computeInt32Size(4, this.numberResults_);
            }
            if ((this.bitField0_ & 16) == 16) {
                iComputeEnumSize += CodedOutputStream.computeInt64Size(5, this.eventTimeMillis_);
            }
            int serializedSize = iComputeEnumSize + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static ScanEvent parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (ScanEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static ScanEvent parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (ScanEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static ScanEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (ScanEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static ScanEvent parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (ScanEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static ScanEvent parseFrom(InputStream inputStream) throws IOException {
            return (ScanEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static ScanEvent parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (ScanEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static ScanEvent parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (ScanEvent) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static ScanEvent parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (ScanEvent) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static ScanEvent parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (ScanEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static ScanEvent parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (ScanEvent) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(ScanEvent scanEvent) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(scanEvent);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<ScanEvent, Builder> implements ScanEventOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(ScanEvent.DEFAULT_INSTANCE);
            }

            @Override
            public boolean hasScanEventType() {
                return ((ScanEvent) this.instance).hasScanEventType();
            }

            @Override
            public ScanEventType getScanEventType() {
                return ((ScanEvent) this.instance).getScanEventType();
            }

            public Builder setScanEventType(ScanEventType scanEventType) {
                copyOnWrite();
                ((ScanEvent) this.instance).setScanEventType(scanEventType);
                return this;
            }

            public Builder clearScanEventType() {
                copyOnWrite();
                ((ScanEvent) this.instance).clearScanEventType();
                return this;
            }

            @Override
            public boolean hasInitiator() {
                return ((ScanEvent) this.instance).hasInitiator();
            }

            @Override
            public String getInitiator() {
                return ((ScanEvent) this.instance).getInitiator();
            }

            @Override
            public ByteString getInitiatorBytes() {
                return ((ScanEvent) this.instance).getInitiatorBytes();
            }

            public Builder setInitiator(String str) {
                copyOnWrite();
                ((ScanEvent) this.instance).setInitiator(str);
                return this;
            }

            public Builder clearInitiator() {
                copyOnWrite();
                ((ScanEvent) this.instance).clearInitiator();
                return this;
            }

            public Builder setInitiatorBytes(ByteString byteString) {
                copyOnWrite();
                ((ScanEvent) this.instance).setInitiatorBytes(byteString);
                return this;
            }

            @Override
            public boolean hasScanTechnologyType() {
                return ((ScanEvent) this.instance).hasScanTechnologyType();
            }

            @Override
            public ScanTechnologyType getScanTechnologyType() {
                return ((ScanEvent) this.instance).getScanTechnologyType();
            }

            public Builder setScanTechnologyType(ScanTechnologyType scanTechnologyType) {
                copyOnWrite();
                ((ScanEvent) this.instance).setScanTechnologyType(scanTechnologyType);
                return this;
            }

            public Builder clearScanTechnologyType() {
                copyOnWrite();
                ((ScanEvent) this.instance).clearScanTechnologyType();
                return this;
            }

            @Override
            public boolean hasNumberResults() {
                return ((ScanEvent) this.instance).hasNumberResults();
            }

            @Override
            public int getNumberResults() {
                return ((ScanEvent) this.instance).getNumberResults();
            }

            public Builder setNumberResults(int i) {
                copyOnWrite();
                ((ScanEvent) this.instance).setNumberResults(i);
                return this;
            }

            public Builder clearNumberResults() {
                copyOnWrite();
                ((ScanEvent) this.instance).clearNumberResults();
                return this;
            }

            @Override
            public boolean hasEventTimeMillis() {
                return ((ScanEvent) this.instance).hasEventTimeMillis();
            }

            @Override
            public long getEventTimeMillis() {
                return ((ScanEvent) this.instance).getEventTimeMillis();
            }

            public Builder setEventTimeMillis(long j) {
                copyOnWrite();
                ((ScanEvent) this.instance).setEventTimeMillis(j);
                return this;
            }

            public Builder clearEventTimeMillis() {
                copyOnWrite();
                ((ScanEvent) this.instance).clearEventTimeMillis();
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new ScanEvent();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    ScanEvent scanEvent = (ScanEvent) obj2;
                    this.scanEventType_ = visitor.visitInt(hasScanEventType(), this.scanEventType_, scanEvent.hasScanEventType(), scanEvent.scanEventType_);
                    this.initiator_ = visitor.visitString(hasInitiator(), this.initiator_, scanEvent.hasInitiator(), scanEvent.initiator_);
                    this.scanTechnologyType_ = visitor.visitInt(hasScanTechnologyType(), this.scanTechnologyType_, scanEvent.hasScanTechnologyType(), scanEvent.scanTechnologyType_);
                    this.numberResults_ = visitor.visitInt(hasNumberResults(), this.numberResults_, scanEvent.hasNumberResults(), scanEvent.numberResults_);
                    this.eventTimeMillis_ = visitor.visitLong(hasEventTimeMillis(), this.eventTimeMillis_, scanEvent.hasEventTimeMillis(), scanEvent.eventTimeMillis_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= scanEvent.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    boolean z = false;
                    while (!z) {
                        try {
                            int tag = codedInputStream.readTag();
                            if (tag != 0) {
                                if (tag == 8) {
                                    int i = codedInputStream.readEnum();
                                    if (ScanEventType.forNumber(i) != null) {
                                        this.bitField0_ = 1 | this.bitField0_;
                                        this.scanEventType_ = i;
                                    } else {
                                        super.mergeVarintField(1, i);
                                    }
                                } else if (tag == 18) {
                                    String string = codedInputStream.readString();
                                    this.bitField0_ |= 2;
                                    this.initiator_ = string;
                                } else if (tag == 24) {
                                    int i2 = codedInputStream.readEnum();
                                    if (ScanTechnologyType.forNumber(i2) == null) {
                                        super.mergeVarintField(3, i2);
                                    } else {
                                        this.bitField0_ |= 4;
                                        this.scanTechnologyType_ = i2;
                                    }
                                } else if (tag == 32) {
                                    this.bitField0_ |= 8;
                                    this.numberResults_ = codedInputStream.readInt32();
                                } else if (tag != 40) {
                                    if (!parseUnknownField(tag, codedInputStream)) {
                                    }
                                } else {
                                    this.bitField0_ |= 16;
                                    this.eventTimeMillis_ = codedInputStream.readInt64();
                                }
                            }
                            z = true;
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e.setUnfinishedMessage(this));
                        } catch (IOException e2) {
                            throw new RuntimeException(new InvalidProtocolBufferException(e2.getMessage()).setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (ScanEvent.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static ScanEvent getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<ScanEvent> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class ProfileConnectionStats extends GeneratedMessageLite<ProfileConnectionStats, Builder> implements ProfileConnectionStatsOrBuilder {
        private static final ProfileConnectionStats DEFAULT_INSTANCE = new ProfileConnectionStats();
        public static final int NUM_TIMES_CONNECTED_FIELD_NUMBER = 2;
        private static volatile Parser<ProfileConnectionStats> PARSER = null;
        public static final int PROFILE_ID_FIELD_NUMBER = 1;
        private int bitField0_;
        private int profileId_ = 0;
        private int numTimesConnected_ = 0;

        private ProfileConnectionStats() {
        }

        @Override
        public boolean hasProfileId() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public ProfileId getProfileId() {
            ProfileId profileIdForNumber = ProfileId.forNumber(this.profileId_);
            return profileIdForNumber == null ? ProfileId.PROFILE_UNKNOWN : profileIdForNumber;
        }

        private void setProfileId(ProfileId profileId) {
            if (profileId == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 1;
            this.profileId_ = profileId.getNumber();
        }

        private void clearProfileId() {
            this.bitField0_ &= -2;
            this.profileId_ = 0;
        }

        @Override
        public boolean hasNumTimesConnected() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public int getNumTimesConnected() {
            return this.numTimesConnected_;
        }

        private void setNumTimesConnected(int i) {
            this.bitField0_ |= 2;
            this.numTimesConnected_ = i;
        }

        private void clearNumTimesConnected() {
            this.bitField0_ &= -3;
            this.numTimesConnected_ = 0;
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeEnum(1, this.profileId_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeInt32(2, this.numTimesConnected_);
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeEnumSize = (this.bitField0_ & 1) == 1 ? 0 + CodedOutputStream.computeEnumSize(1, this.profileId_) : 0;
            if ((this.bitField0_ & 2) == 2) {
                iComputeEnumSize += CodedOutputStream.computeInt32Size(2, this.numTimesConnected_);
            }
            int serializedSize = iComputeEnumSize + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static ProfileConnectionStats parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (ProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static ProfileConnectionStats parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (ProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static ProfileConnectionStats parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (ProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static ProfileConnectionStats parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (ProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static ProfileConnectionStats parseFrom(InputStream inputStream) throws IOException {
            return (ProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static ProfileConnectionStats parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (ProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static ProfileConnectionStats parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (ProfileConnectionStats) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static ProfileConnectionStats parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (ProfileConnectionStats) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static ProfileConnectionStats parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (ProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static ProfileConnectionStats parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (ProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(ProfileConnectionStats profileConnectionStats) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(profileConnectionStats);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<ProfileConnectionStats, Builder> implements ProfileConnectionStatsOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(ProfileConnectionStats.DEFAULT_INSTANCE);
            }

            @Override
            public boolean hasProfileId() {
                return ((ProfileConnectionStats) this.instance).hasProfileId();
            }

            @Override
            public ProfileId getProfileId() {
                return ((ProfileConnectionStats) this.instance).getProfileId();
            }

            public Builder setProfileId(ProfileId profileId) {
                copyOnWrite();
                ((ProfileConnectionStats) this.instance).setProfileId(profileId);
                return this;
            }

            public Builder clearProfileId() {
                copyOnWrite();
                ((ProfileConnectionStats) this.instance).clearProfileId();
                return this;
            }

            @Override
            public boolean hasNumTimesConnected() {
                return ((ProfileConnectionStats) this.instance).hasNumTimesConnected();
            }

            @Override
            public int getNumTimesConnected() {
                return ((ProfileConnectionStats) this.instance).getNumTimesConnected();
            }

            public Builder setNumTimesConnected(int i) {
                copyOnWrite();
                ((ProfileConnectionStats) this.instance).setNumTimesConnected(i);
                return this;
            }

            public Builder clearNumTimesConnected() {
                copyOnWrite();
                ((ProfileConnectionStats) this.instance).clearNumTimesConnected();
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new ProfileConnectionStats();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    ProfileConnectionStats profileConnectionStats = (ProfileConnectionStats) obj2;
                    this.profileId_ = visitor.visitInt(hasProfileId(), this.profileId_, profileConnectionStats.hasProfileId(), profileConnectionStats.profileId_);
                    this.numTimesConnected_ = visitor.visitInt(hasNumTimesConnected(), this.numTimesConnected_, profileConnectionStats.hasNumTimesConnected(), profileConnectionStats.numTimesConnected_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= profileConnectionStats.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    boolean z = false;
                    while (!z) {
                        try {
                            int tag = codedInputStream.readTag();
                            if (tag != 0) {
                                if (tag == 8) {
                                    int i = codedInputStream.readEnum();
                                    if (ProfileId.forNumber(i) != null) {
                                        this.bitField0_ = 1 | this.bitField0_;
                                        this.profileId_ = i;
                                    } else {
                                        super.mergeVarintField(1, i);
                                    }
                                } else if (tag != 16) {
                                    if (!parseUnknownField(tag, codedInputStream)) {
                                    }
                                } else {
                                    this.bitField0_ |= 2;
                                    this.numTimesConnected_ = codedInputStream.readInt32();
                                }
                            }
                            z = true;
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e.setUnfinishedMessage(this));
                        } catch (IOException e2) {
                            throw new RuntimeException(new InvalidProtocolBufferException(e2.getMessage()).setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (ProfileConnectionStats.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static ProfileConnectionStats getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<ProfileConnectionStats> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class HeadsetProfileConnectionStats extends GeneratedMessageLite<HeadsetProfileConnectionStats, Builder> implements HeadsetProfileConnectionStatsOrBuilder {
        private static final HeadsetProfileConnectionStats DEFAULT_INSTANCE = new HeadsetProfileConnectionStats();
        public static final int HEADSET_PROFILE_TYPE_FIELD_NUMBER = 1;
        public static final int NUM_TIMES_CONNECTED_FIELD_NUMBER = 2;
        private static volatile Parser<HeadsetProfileConnectionStats> PARSER;
        private int bitField0_;
        private int headsetProfileType_ = 0;
        private int numTimesConnected_ = 0;

        private HeadsetProfileConnectionStats() {
        }

        @Override
        public boolean hasHeadsetProfileType() {
            return (this.bitField0_ & 1) == 1;
        }

        @Override
        public HeadsetProfileType getHeadsetProfileType() {
            HeadsetProfileType headsetProfileTypeForNumber = HeadsetProfileType.forNumber(this.headsetProfileType_);
            return headsetProfileTypeForNumber == null ? HeadsetProfileType.HEADSET_PROFILE_UNKNOWN : headsetProfileTypeForNumber;
        }

        private void setHeadsetProfileType(HeadsetProfileType headsetProfileType) {
            if (headsetProfileType == null) {
                throw new NullPointerException();
            }
            this.bitField0_ |= 1;
            this.headsetProfileType_ = headsetProfileType.getNumber();
        }

        private void clearHeadsetProfileType() {
            this.bitField0_ &= -2;
            this.headsetProfileType_ = 0;
        }

        @Override
        public boolean hasNumTimesConnected() {
            return (this.bitField0_ & 2) == 2;
        }

        @Override
        public int getNumTimesConnected() {
            return this.numTimesConnected_;
        }

        private void setNumTimesConnected(int i) {
            this.bitField0_ |= 2;
            this.numTimesConnected_ = i;
        }

        private void clearNumTimesConnected() {
            this.bitField0_ &= -3;
            this.numTimesConnected_ = 0;
        }

        @Override
        public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
            if ((this.bitField0_ & 1) == 1) {
                codedOutputStream.writeEnum(1, this.headsetProfileType_);
            }
            if ((this.bitField0_ & 2) == 2) {
                codedOutputStream.writeInt32(2, this.numTimesConnected_);
            }
            this.unknownFields.writeTo(codedOutputStream);
        }

        @Override
        public int getSerializedSize() {
            int i = this.memoizedSerializedSize;
            if (i != -1) {
                return i;
            }
            int iComputeEnumSize = (this.bitField0_ & 1) == 1 ? 0 + CodedOutputStream.computeEnumSize(1, this.headsetProfileType_) : 0;
            if ((this.bitField0_ & 2) == 2) {
                iComputeEnumSize += CodedOutputStream.computeInt32Size(2, this.numTimesConnected_);
            }
            int serializedSize = iComputeEnumSize + this.unknownFields.getSerializedSize();
            this.memoizedSerializedSize = serializedSize;
            return serializedSize;
        }

        public static HeadsetProfileConnectionStats parseFrom(ByteString byteString) throws InvalidProtocolBufferException {
            return (HeadsetProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static HeadsetProfileConnectionStats parseFrom(ByteString byteString, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (HeadsetProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static HeadsetProfileConnectionStats parseFrom(byte[] bArr) throws InvalidProtocolBufferException {
            return (HeadsetProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static HeadsetProfileConnectionStats parseFrom(byte[] bArr, ExtensionRegistryLite extensionRegistryLite) throws InvalidProtocolBufferException {
            return (HeadsetProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static HeadsetProfileConnectionStats parseFrom(InputStream inputStream) throws IOException {
            return (HeadsetProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static HeadsetProfileConnectionStats parseFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (HeadsetProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static HeadsetProfileConnectionStats parseDelimitedFrom(InputStream inputStream) throws IOException {
            return (HeadsetProfileConnectionStats) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static HeadsetProfileConnectionStats parseDelimitedFrom(InputStream inputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (HeadsetProfileConnectionStats) parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static HeadsetProfileConnectionStats parseFrom(CodedInputStream codedInputStream) throws IOException {
            return (HeadsetProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static HeadsetProfileConnectionStats parseFrom(CodedInputStream codedInputStream, ExtensionRegistryLite extensionRegistryLite) throws IOException {
            return (HeadsetProfileConnectionStats) GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(HeadsetProfileConnectionStats headsetProfileConnectionStats) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(headsetProfileConnectionStats);
        }

        public static final class Builder extends GeneratedMessageLite.Builder<HeadsetProfileConnectionStats, Builder> implements HeadsetProfileConnectionStatsOrBuilder {
            Builder(AnonymousClass1 anonymousClass1) {
                this();
            }

            private Builder() {
                super(HeadsetProfileConnectionStats.DEFAULT_INSTANCE);
            }

            @Override
            public boolean hasHeadsetProfileType() {
                return ((HeadsetProfileConnectionStats) this.instance).hasHeadsetProfileType();
            }

            @Override
            public HeadsetProfileType getHeadsetProfileType() {
                return ((HeadsetProfileConnectionStats) this.instance).getHeadsetProfileType();
            }

            public Builder setHeadsetProfileType(HeadsetProfileType headsetProfileType) {
                copyOnWrite();
                ((HeadsetProfileConnectionStats) this.instance).setHeadsetProfileType(headsetProfileType);
                return this;
            }

            public Builder clearHeadsetProfileType() {
                copyOnWrite();
                ((HeadsetProfileConnectionStats) this.instance).clearHeadsetProfileType();
                return this;
            }

            @Override
            public boolean hasNumTimesConnected() {
                return ((HeadsetProfileConnectionStats) this.instance).hasNumTimesConnected();
            }

            @Override
            public int getNumTimesConnected() {
                return ((HeadsetProfileConnectionStats) this.instance).getNumTimesConnected();
            }

            public Builder setNumTimesConnected(int i) {
                copyOnWrite();
                ((HeadsetProfileConnectionStats) this.instance).setNumTimesConnected(i);
                return this;
            }

            public Builder clearNumTimesConnected() {
                copyOnWrite();
                ((HeadsetProfileConnectionStats) this.instance).clearNumTimesConnected();
                return this;
            }
        }

        @Override
        protected final Object dynamicMethod(GeneratedMessageLite.MethodToInvoke methodToInvoke, Object obj, Object obj2) {
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()]) {
                case 1:
                    return new HeadsetProfileConnectionStats();
                case 2:
                    return DEFAULT_INSTANCE;
                case 3:
                    return null;
                case 4:
                    return new Builder(anonymousClass1);
                case 5:
                    GeneratedMessageLite.Visitor visitor = (GeneratedMessageLite.Visitor) obj;
                    HeadsetProfileConnectionStats headsetProfileConnectionStats = (HeadsetProfileConnectionStats) obj2;
                    this.headsetProfileType_ = visitor.visitInt(hasHeadsetProfileType(), this.headsetProfileType_, headsetProfileConnectionStats.hasHeadsetProfileType(), headsetProfileConnectionStats.headsetProfileType_);
                    this.numTimesConnected_ = visitor.visitInt(hasNumTimesConnected(), this.numTimesConnected_, headsetProfileConnectionStats.hasNumTimesConnected(), headsetProfileConnectionStats.numTimesConnected_);
                    if (visitor == GeneratedMessageLite.MergeFromVisitor.INSTANCE) {
                        this.bitField0_ |= headsetProfileConnectionStats.bitField0_;
                    }
                    return this;
                case 6:
                    CodedInputStream codedInputStream = (CodedInputStream) obj;
                    boolean z = false;
                    while (!z) {
                        try {
                            int tag = codedInputStream.readTag();
                            if (tag != 0) {
                                if (tag == 8) {
                                    int i = codedInputStream.readEnum();
                                    if (HeadsetProfileType.forNumber(i) != null) {
                                        this.bitField0_ = 1 | this.bitField0_;
                                        this.headsetProfileType_ = i;
                                    } else {
                                        super.mergeVarintField(1, i);
                                    }
                                } else if (tag != 16) {
                                    if (!parseUnknownField(tag, codedInputStream)) {
                                    }
                                } else {
                                    this.bitField0_ |= 2;
                                    this.numTimesConnected_ = codedInputStream.readInt32();
                                }
                            }
                            z = true;
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e.setUnfinishedMessage(this));
                        } catch (IOException e2) {
                            throw new RuntimeException(new InvalidProtocolBufferException(e2.getMessage()).setUnfinishedMessage(this));
                        }
                    }
                    break;
                case 7:
                    break;
                case 8:
                    if (PARSER == null) {
                        synchronized (HeadsetProfileConnectionStats.class) {
                            if (PARSER == null) {
                                PARSER = new GeneratedMessageLite.DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                            }
                            break;
                        }
                    }
                    return PARSER;
                default:
                    throw new UnsupportedOperationException();
            }
            return DEFAULT_INSTANCE;
        }

        static {
            DEFAULT_INSTANCE.makeImmutable();
        }

        public static HeadsetProfileConnectionStats getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<HeadsetProfileConnectionStats> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }
}
