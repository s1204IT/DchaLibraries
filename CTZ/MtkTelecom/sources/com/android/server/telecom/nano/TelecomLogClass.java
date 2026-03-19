package com.android.server.telecom.nano;

import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.InternalNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface TelecomLogClass {

    public static final class TelecomLog extends MessageNano {
        public CallLog[] callLogs;
        public LogSessionTiming[] sessionTimings;

        public TelecomLog() {
            clear();
        }

        public TelecomLog clear() {
            this.callLogs = CallLog.emptyArray();
            this.sessionTimings = LogSessionTiming.emptyArray();
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.callLogs != null && this.callLogs.length > 0) {
                for (int i = 0; i < this.callLogs.length; i++) {
                    CallLog callLog = this.callLogs[i];
                    if (callLog != null) {
                        codedOutputByteBufferNano.writeMessage(1, callLog);
                    }
                }
            }
            if (this.sessionTimings != null && this.sessionTimings.length > 0) {
                for (int i2 = 0; i2 < this.sessionTimings.length; i2++) {
                    LogSessionTiming logSessionTiming = this.sessionTimings[i2];
                    if (logSessionTiming != null) {
                        codedOutputByteBufferNano.writeMessage(2, logSessionTiming);
                    }
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.callLogs != null && this.callLogs.length > 0) {
                int iComputeMessageSize = iComputeSerializedSize;
                for (int i = 0; i < this.callLogs.length; i++) {
                    CallLog callLog = this.callLogs[i];
                    if (callLog != null) {
                        iComputeMessageSize += CodedOutputByteBufferNano.computeMessageSize(1, callLog);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize;
            }
            if (this.sessionTimings != null && this.sessionTimings.length > 0) {
                for (int i2 = 0; i2 < this.sessionTimings.length; i2++) {
                    LogSessionTiming logSessionTiming = this.sessionTimings[i2];
                    if (logSessionTiming != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(2, logSessionTiming);
                    }
                }
            }
            return iComputeSerializedSize;
        }
    }

    public static final class LogSessionTiming extends MessageNano {
        private static volatile LogSessionTiming[] _emptyArray;
        private int bitField0_;
        private int sessionEntryPoint_;
        private long timeMillis_;

        public static LogSessionTiming[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new LogSessionTiming[0];
                    }
                }
            }
            return _emptyArray;
        }

        public LogSessionTiming setSessionEntryPoint(int i) {
            this.sessionEntryPoint_ = i;
            this.bitField0_ |= 1;
            return this;
        }

        public LogSessionTiming setTimeMillis(long j) {
            this.timeMillis_ = j;
            this.bitField0_ |= 2;
            return this;
        }

        public LogSessionTiming() {
            clear();
        }

        public LogSessionTiming clear() {
            this.bitField0_ = 0;
            this.sessionEntryPoint_ = 0;
            this.timeMillis_ = 0L;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if ((this.bitField0_ & 1) != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.sessionEntryPoint_);
            }
            if ((this.bitField0_ & 2) != 0) {
                codedOutputByteBufferNano.writeInt64(2, this.timeMillis_);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if ((this.bitField0_ & 1) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.sessionEntryPoint_);
            }
            if ((this.bitField0_ & 2) != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(2, this.timeMillis_);
            }
            return iComputeSerializedSize;
        }
    }

    public static final class Event extends MessageNano {
        private static volatile Event[] _emptyArray;
        private int bitField0_;
        private int eventName_;
        private long timeSinceLastEventMillis_;

        public static Event[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Event[0];
                    }
                }
            }
            return _emptyArray;
        }

        public int getEventName() {
            return this.eventName_;
        }

        public Event setEventName(int i) {
            this.eventName_ = i;
            this.bitField0_ |= 1;
            return this;
        }

        public long getTimeSinceLastEventMillis() {
            return this.timeSinceLastEventMillis_;
        }

        public Event setTimeSinceLastEventMillis(long j) {
            this.timeSinceLastEventMillis_ = j;
            this.bitField0_ |= 2;
            return this;
        }

        public Event() {
            clear();
        }

        public Event clear() {
            this.bitField0_ = 0;
            this.eventName_ = 9999;
            this.timeSinceLastEventMillis_ = 0L;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if ((this.bitField0_ & 1) != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.eventName_);
            }
            if ((this.bitField0_ & 2) != 0) {
                codedOutputByteBufferNano.writeInt64(2, this.timeSinceLastEventMillis_);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if ((this.bitField0_ & 1) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.eventName_);
            }
            if ((this.bitField0_ & 2) != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(2, this.timeSinceLastEventMillis_);
            }
            return iComputeSerializedSize;
        }
    }

    public static final class VideoEvent extends MessageNano {
        private static volatile VideoEvent[] _emptyArray;
        private int bitField0_;
        private int eventName_;
        private long timeSinceLastEventMillis_;
        private int videoState_;

        public static VideoEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new VideoEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public int getEventName() {
            return this.eventName_;
        }

        public VideoEvent setEventName(int i) {
            this.eventName_ = i;
            this.bitField0_ |= 1;
            return this;
        }

        public long getTimeSinceLastEventMillis() {
            return this.timeSinceLastEventMillis_;
        }

        public VideoEvent setTimeSinceLastEventMillis(long j) {
            this.timeSinceLastEventMillis_ = j;
            this.bitField0_ |= 2;
            return this;
        }

        public int getVideoState() {
            return this.videoState_;
        }

        public VideoEvent setVideoState(int i) {
            this.videoState_ = i;
            this.bitField0_ |= 4;
            return this;
        }

        public VideoEvent() {
            clear();
        }

        public VideoEvent clear() {
            this.bitField0_ = 0;
            this.eventName_ = 9999;
            this.timeSinceLastEventMillis_ = 0L;
            this.videoState_ = 0;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if ((this.bitField0_ & 1) != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.eventName_);
            }
            if ((this.bitField0_ & 2) != 0) {
                codedOutputByteBufferNano.writeInt64(2, this.timeSinceLastEventMillis_);
            }
            if ((this.bitField0_ & 4) != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.videoState_);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if ((this.bitField0_ & 1) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.eventName_);
            }
            if ((this.bitField0_ & 2) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(2, this.timeSinceLastEventMillis_);
            }
            if ((this.bitField0_ & 4) != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(3, this.videoState_);
            }
            return iComputeSerializedSize;
        }
    }

    public static final class EventTimingEntry extends MessageNano {
        private static volatile EventTimingEntry[] _emptyArray;
        private int bitField0_;
        private long timeMillis_;
        private int timingName_;

        public static EventTimingEntry[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new EventTimingEntry[0];
                    }
                }
            }
            return _emptyArray;
        }

        public int getTimingName() {
            return this.timingName_;
        }

        public EventTimingEntry setTimingName(int i) {
            this.timingName_ = i;
            this.bitField0_ |= 1;
            return this;
        }

        public long getTimeMillis() {
            return this.timeMillis_;
        }

        public EventTimingEntry setTimeMillis(long j) {
            this.timeMillis_ = j;
            this.bitField0_ |= 2;
            return this;
        }

        public EventTimingEntry() {
            clear();
        }

        public EventTimingEntry clear() {
            this.bitField0_ = 0;
            this.timingName_ = 9999;
            this.timeMillis_ = 0L;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if ((this.bitField0_ & 1) != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.timingName_);
            }
            if ((this.bitField0_ & 2) != 0) {
                codedOutputByteBufferNano.writeInt64(2, this.timeMillis_);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if ((this.bitField0_ & 1) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.timingName_);
            }
            if ((this.bitField0_ & 2) != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(2, this.timeMillis_);
            }
            return iComputeSerializedSize;
        }
    }

    public static final class InCallServiceInfo extends MessageNano {
        private static volatile InCallServiceInfo[] _emptyArray;
        private int bitField0_;
        private String inCallServiceName_;
        private int inCallServiceType_;

        public static InCallServiceInfo[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new InCallServiceInfo[0];
                    }
                }
            }
            return _emptyArray;
        }

        public String getInCallServiceName() {
            return this.inCallServiceName_;
        }

        public InCallServiceInfo setInCallServiceName(String str) {
            if (str == null) {
                throw new NullPointerException();
            }
            this.inCallServiceName_ = str;
            this.bitField0_ |= 1;
            return this;
        }

        public int getInCallServiceType() {
            return this.inCallServiceType_;
        }

        public InCallServiceInfo setInCallServiceType(int i) {
            this.inCallServiceType_ = i;
            this.bitField0_ |= 2;
            return this;
        }

        public InCallServiceInfo() {
            clear();
        }

        public InCallServiceInfo clear() {
            this.bitField0_ = 0;
            this.inCallServiceName_ = "";
            this.inCallServiceType_ = 9999;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if ((this.bitField0_ & 1) != 0) {
                codedOutputByteBufferNano.writeString(1, this.inCallServiceName_);
            }
            if ((this.bitField0_ & 2) != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.inCallServiceType_);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if ((this.bitField0_ & 1) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(1, this.inCallServiceName_);
            }
            if ((this.bitField0_ & 2) != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(2, this.inCallServiceType_);
            }
            return iComputeSerializedSize;
        }
    }

    public static final class CallLog extends MessageNano {
        private static volatile CallLog[] _emptyArray;
        private int bitField0_;
        private long callDurationMillis_;
        public Event[] callEvents;
        private int callTechnologies_;
        private int callTerminationCode_;
        public EventTimingEntry[] callTimings;
        private int connectionProperties_;
        public String[] connectionService;
        public InCallServiceInfo[] inCallServices;
        private boolean isAdditionalCall_;
        private boolean isCreatedFromExistingConnection_;
        private boolean isEmergencyCall_;
        private boolean isInterrupted_;
        private boolean isVideoCall_;
        private long startTime5Min_;
        private int type_;
        public VideoEvent[] videoEvents;

        public static CallLog[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new CallLog[0];
                    }
                }
            }
            return _emptyArray;
        }

        public long getStartTime5Min() {
            return this.startTime5Min_;
        }

        public CallLog setStartTime5Min(long j) {
            this.startTime5Min_ = j;
            this.bitField0_ |= 1;
            return this;
        }

        public long getCallDurationMillis() {
            return this.callDurationMillis_;
        }

        public CallLog setCallDurationMillis(long j) {
            this.callDurationMillis_ = j;
            this.bitField0_ |= 2;
            return this;
        }

        public int getType() {
            return this.type_;
        }

        public CallLog setType(int i) {
            this.type_ = i;
            this.bitField0_ |= 4;
            return this;
        }

        public boolean getIsAdditionalCall() {
            return this.isAdditionalCall_;
        }

        public CallLog setIsAdditionalCall(boolean z) {
            this.isAdditionalCall_ = z;
            this.bitField0_ |= 8;
            return this;
        }

        public boolean getIsInterrupted() {
            return this.isInterrupted_;
        }

        public CallLog setIsInterrupted(boolean z) {
            this.isInterrupted_ = z;
            this.bitField0_ |= 16;
            return this;
        }

        public int getCallTechnologies() {
            return this.callTechnologies_;
        }

        public CallLog setCallTechnologies(int i) {
            this.callTechnologies_ = i;
            this.bitField0_ |= 32;
            return this;
        }

        public int getCallTerminationCode() {
            return this.callTerminationCode_;
        }

        public CallLog setCallTerminationCode(int i) {
            this.callTerminationCode_ = i;
            this.bitField0_ |= 64;
            return this;
        }

        public boolean getIsCreatedFromExistingConnection() {
            return this.isCreatedFromExistingConnection_;
        }

        public CallLog setIsCreatedFromExistingConnection(boolean z) {
            this.isCreatedFromExistingConnection_ = z;
            this.bitField0_ |= 128;
            return this;
        }

        public boolean getIsEmergencyCall() {
            return this.isEmergencyCall_;
        }

        public CallLog setIsEmergencyCall(boolean z) {
            this.isEmergencyCall_ = z;
            this.bitField0_ |= 256;
            return this;
        }

        public boolean getIsVideoCall() {
            return this.isVideoCall_;
        }

        public CallLog setIsVideoCall(boolean z) {
            this.isVideoCall_ = z;
            this.bitField0_ |= 512;
            return this;
        }

        public CallLog setConnectionProperties(int i) {
            this.connectionProperties_ = i;
            this.bitField0_ |= 1024;
            return this;
        }

        public CallLog() {
            clear();
        }

        public CallLog clear() {
            this.bitField0_ = 0;
            this.startTime5Min_ = 0L;
            this.callDurationMillis_ = 0L;
            this.type_ = 0;
            this.isAdditionalCall_ = false;
            this.isInterrupted_ = false;
            this.callTechnologies_ = 0;
            this.callTerminationCode_ = 0;
            this.connectionService = WireFormatNano.EMPTY_STRING_ARRAY;
            this.isCreatedFromExistingConnection_ = false;
            this.isEmergencyCall_ = false;
            this.callEvents = Event.emptyArray();
            this.callTimings = EventTimingEntry.emptyArray();
            this.isVideoCall_ = false;
            this.videoEvents = VideoEvent.emptyArray();
            this.inCallServices = InCallServiceInfo.emptyArray();
            this.connectionProperties_ = 0;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if ((this.bitField0_ & 1) != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.startTime5Min_);
            }
            if ((this.bitField0_ & 2) != 0) {
                codedOutputByteBufferNano.writeInt64(2, this.callDurationMillis_);
            }
            if ((this.bitField0_ & 4) != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.type_);
            }
            if ((this.bitField0_ & 8) != 0) {
                codedOutputByteBufferNano.writeBool(4, this.isAdditionalCall_);
            }
            if ((this.bitField0_ & 16) != 0) {
                codedOutputByteBufferNano.writeBool(5, this.isInterrupted_);
            }
            if ((this.bitField0_ & 32) != 0) {
                codedOutputByteBufferNano.writeInt32(6, this.callTechnologies_);
            }
            if ((this.bitField0_ & 64) != 0) {
                codedOutputByteBufferNano.writeInt32(7, this.callTerminationCode_);
            }
            if (this.connectionService != null && this.connectionService.length > 0) {
                for (int i = 0; i < this.connectionService.length; i++) {
                    String str = this.connectionService[i];
                    if (str != null) {
                        codedOutputByteBufferNano.writeString(9, str);
                    }
                }
            }
            if ((this.bitField0_ & 128) != 0) {
                codedOutputByteBufferNano.writeBool(10, this.isCreatedFromExistingConnection_);
            }
            if ((this.bitField0_ & 256) != 0) {
                codedOutputByteBufferNano.writeBool(11, this.isEmergencyCall_);
            }
            if (this.callEvents != null && this.callEvents.length > 0) {
                for (int i2 = 0; i2 < this.callEvents.length; i2++) {
                    Event event = this.callEvents[i2];
                    if (event != null) {
                        codedOutputByteBufferNano.writeMessage(12, event);
                    }
                }
            }
            if (this.callTimings != null && this.callTimings.length > 0) {
                for (int i3 = 0; i3 < this.callTimings.length; i3++) {
                    EventTimingEntry eventTimingEntry = this.callTimings[i3];
                    if (eventTimingEntry != null) {
                        codedOutputByteBufferNano.writeMessage(13, eventTimingEntry);
                    }
                }
            }
            if ((this.bitField0_ & 512) != 0) {
                codedOutputByteBufferNano.writeBool(14, this.isVideoCall_);
            }
            if (this.videoEvents != null && this.videoEvents.length > 0) {
                for (int i4 = 0; i4 < this.videoEvents.length; i4++) {
                    VideoEvent videoEvent = this.videoEvents[i4];
                    if (videoEvent != null) {
                        codedOutputByteBufferNano.writeMessage(15, videoEvent);
                    }
                }
            }
            if (this.inCallServices != null && this.inCallServices.length > 0) {
                for (int i5 = 0; i5 < this.inCallServices.length; i5++) {
                    InCallServiceInfo inCallServiceInfo = this.inCallServices[i5];
                    if (inCallServiceInfo != null) {
                        codedOutputByteBufferNano.writeMessage(16, inCallServiceInfo);
                    }
                }
            }
            if ((this.bitField0_ & 1024) != 0) {
                codedOutputByteBufferNano.writeInt32(17, this.connectionProperties_);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if ((this.bitField0_ & 1) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.startTime5Min_);
            }
            if ((this.bitField0_ & 2) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(2, this.callDurationMillis_);
            }
            if ((this.bitField0_ & 4) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.type_);
            }
            if ((this.bitField0_ & 8) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(4, this.isAdditionalCall_);
            }
            if ((this.bitField0_ & 16) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(5, this.isInterrupted_);
            }
            if ((this.bitField0_ & 32) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(6, this.callTechnologies_);
            }
            if ((this.bitField0_ & 64) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(7, this.callTerminationCode_);
            }
            if (this.connectionService != null && this.connectionService.length > 0) {
                int iComputeStringSizeNoTag = 0;
                int i = 0;
                for (int i2 = 0; i2 < this.connectionService.length; i2++) {
                    String str = this.connectionService[i2];
                    if (str != null) {
                        i++;
                        iComputeStringSizeNoTag += CodedOutputByteBufferNano.computeStringSizeNoTag(str);
                    }
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeStringSizeNoTag + (1 * i);
            }
            if ((this.bitField0_ & 128) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(10, this.isCreatedFromExistingConnection_);
            }
            if ((this.bitField0_ & 256) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(11, this.isEmergencyCall_);
            }
            if (this.callEvents != null && this.callEvents.length > 0) {
                int iComputeMessageSize = iComputeSerializedSize;
                for (int i3 = 0; i3 < this.callEvents.length; i3++) {
                    Event event = this.callEvents[i3];
                    if (event != null) {
                        iComputeMessageSize += CodedOutputByteBufferNano.computeMessageSize(12, event);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize;
            }
            if (this.callTimings != null && this.callTimings.length > 0) {
                int iComputeMessageSize2 = iComputeSerializedSize;
                for (int i4 = 0; i4 < this.callTimings.length; i4++) {
                    EventTimingEntry eventTimingEntry = this.callTimings[i4];
                    if (eventTimingEntry != null) {
                        iComputeMessageSize2 += CodedOutputByteBufferNano.computeMessageSize(13, eventTimingEntry);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize2;
            }
            if ((this.bitField0_ & 512) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(14, this.isVideoCall_);
            }
            if (this.videoEvents != null && this.videoEvents.length > 0) {
                int iComputeMessageSize3 = iComputeSerializedSize;
                for (int i5 = 0; i5 < this.videoEvents.length; i5++) {
                    VideoEvent videoEvent = this.videoEvents[i5];
                    if (videoEvent != null) {
                        iComputeMessageSize3 += CodedOutputByteBufferNano.computeMessageSize(15, videoEvent);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize3;
            }
            if (this.inCallServices != null && this.inCallServices.length > 0) {
                for (int i6 = 0; i6 < this.inCallServices.length; i6++) {
                    InCallServiceInfo inCallServiceInfo = this.inCallServices[i6];
                    if (inCallServiceInfo != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(16, inCallServiceInfo);
                    }
                }
            }
            if ((this.bitField0_ & 1024) != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(17, this.connectionProperties_);
            }
            return iComputeSerializedSize;
        }
    }
}
