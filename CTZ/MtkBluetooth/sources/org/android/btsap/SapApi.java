package org.android.btsap;

import com.google.protobuf.micro.ByteStringMicro;
import com.google.protobuf.micro.CodedInputStreamMicro;
import com.google.protobuf.micro.CodedOutputStreamMicro;
import com.google.protobuf.micro.InvalidProtocolBufferMicroException;
import com.google.protobuf.micro.MessageMicro;
import java.io.IOException;

public final class SapApi {
    public static final int REQUEST = 1;
    public static final int RESPONSE = 2;
    public static final int RIL_E_CANCELLED = 4;
    public static final int RIL_E_GENERIC_FAILURE = 2;
    public static final int RIL_E_INVALID_PARAMETER = 5;
    public static final int RIL_E_RADIO_NOT_AVAILABLE = 1;
    public static final int RIL_E_REQUEST_NOT_SUPPORTED = 3;
    public static final int RIL_E_SUCCESS = 0;
    public static final int RIL_E_UNUSED = 6;
    public static final int RIL_SIM_SAP_APDU = 3;
    public static final int RIL_SIM_SAP_CONNECT = 1;
    public static final int RIL_SIM_SAP_DISCONNECT = 2;
    public static final int RIL_SIM_SAP_ERROR_RESP = 9;
    public static final int RIL_SIM_SAP_POWER = 5;
    public static final int RIL_SIM_SAP_RESET_SIM = 6;
    public static final int RIL_SIM_SAP_SET_TRANSFER_PROTOCOL = 10;
    public static final int RIL_SIM_SAP_STATUS = 7;
    public static final int RIL_SIM_SAP_TRANSFER_ATR = 4;
    public static final int RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS = 8;
    public static final int UNKNOWN = 0;
    public static final int UNKNOWN_REQ = 0;
    public static final int UNSOL_RESPONSE = 3;

    private SapApi() {
    }

    public static final class MsgHeader extends MessageMicro {
        public static final int ERROR_FIELD_NUMBER = 4;
        public static final int ID_FIELD_NUMBER = 3;
        public static final int PAYLOAD_FIELD_NUMBER = 5;
        public static final int TOKEN_FIELD_NUMBER = 1;
        public static final int TYPE_FIELD_NUMBER = 2;
        private boolean hasError;
        private boolean hasId;
        private boolean hasPayload;
        private boolean hasToken;
        private boolean hasType;
        private int token_ = 0;
        private int type_ = 0;
        private int id_ = 0;
        private int error_ = 0;
        private ByteStringMicro payload_ = ByteStringMicro.EMPTY;
        private int cachedSize = -1;

        public int getToken() {
            return this.token_;
        }

        public boolean hasToken() {
            return this.hasToken;
        }

        public MsgHeader setToken(int i) {
            this.hasToken = true;
            this.token_ = i;
            return this;
        }

        public MsgHeader clearToken() {
            this.hasToken = false;
            this.token_ = 0;
            return this;
        }

        public boolean hasType() {
            return this.hasType;
        }

        public int getType() {
            return this.type_;
        }

        public MsgHeader setType(int i) {
            this.hasType = true;
            this.type_ = i;
            return this;
        }

        public MsgHeader clearType() {
            this.hasType = false;
            this.type_ = 0;
            return this;
        }

        public boolean hasId() {
            return this.hasId;
        }

        public int getId() {
            return this.id_;
        }

        public MsgHeader setId(int i) {
            this.hasId = true;
            this.id_ = i;
            return this;
        }

        public MsgHeader clearId() {
            this.hasId = false;
            this.id_ = 0;
            return this;
        }

        public boolean hasError() {
            return this.hasError;
        }

        public int getError() {
            return this.error_;
        }

        public MsgHeader setError(int i) {
            this.hasError = true;
            this.error_ = i;
            return this;
        }

        public MsgHeader clearError() {
            this.hasError = false;
            this.error_ = 0;
            return this;
        }

        public ByteStringMicro getPayload() {
            return this.payload_;
        }

        public boolean hasPayload() {
            return this.hasPayload;
        }

        public MsgHeader setPayload(ByteStringMicro byteStringMicro) {
            this.hasPayload = true;
            this.payload_ = byteStringMicro;
            return this;
        }

        public MsgHeader clearPayload() {
            this.hasPayload = false;
            this.payload_ = ByteStringMicro.EMPTY;
            return this;
        }

        public final MsgHeader clear() {
            clearToken();
            clearType();
            clearId();
            clearError();
            clearPayload();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasToken && this.hasType && this.hasId && this.hasError && this.hasPayload;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasToken()) {
                codedOutputStreamMicro.writeFixed32(1, getToken());
            }
            if (hasType()) {
                codedOutputStreamMicro.writeInt32(2, getType());
            }
            if (hasId()) {
                codedOutputStreamMicro.writeInt32(3, getId());
            }
            if (hasError()) {
                codedOutputStreamMicro.writeInt32(4, getError());
            }
            if (hasPayload()) {
                codedOutputStreamMicro.writeBytes(5, getPayload());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeFixed32Size = hasToken() ? 0 + CodedOutputStreamMicro.computeFixed32Size(1, getToken()) : 0;
            if (hasType()) {
                iComputeFixed32Size += CodedOutputStreamMicro.computeInt32Size(2, getType());
            }
            if (hasId()) {
                iComputeFixed32Size += CodedOutputStreamMicro.computeInt32Size(3, getId());
            }
            if (hasError()) {
                iComputeFixed32Size += CodedOutputStreamMicro.computeInt32Size(4, getError());
            }
            if (hasPayload()) {
                iComputeFixed32Size += CodedOutputStreamMicro.computeBytesSize(5, getPayload());
            }
            this.cachedSize = iComputeFixed32Size;
            return iComputeFixed32Size;
        }

        @Override
        public MsgHeader mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 13) {
                    setToken(codedInputStreamMicro.readFixed32());
                } else if (tag == 16) {
                    setType(codedInputStreamMicro.readInt32());
                } else if (tag == 24) {
                    setId(codedInputStreamMicro.readInt32());
                } else if (tag == 32) {
                    setError(codedInputStreamMicro.readInt32());
                } else if (tag != 42) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setPayload(codedInputStreamMicro.readBytes());
                }
            }
        }

        public static MsgHeader parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (MsgHeader) new MsgHeader().mergeFrom(bArr);
        }

        public static MsgHeader parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new MsgHeader().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_CONNECT_REQ extends MessageMicro {
        public static final int MAX_MESSAGE_SIZE_FIELD_NUMBER = 1;
        private boolean hasMaxMessageSize;
        private int maxMessageSize_ = 0;
        private int cachedSize = -1;

        public int getMaxMessageSize() {
            return this.maxMessageSize_;
        }

        public boolean hasMaxMessageSize() {
            return this.hasMaxMessageSize;
        }

        public RIL_SIM_SAP_CONNECT_REQ setMaxMessageSize(int i) {
            this.hasMaxMessageSize = true;
            this.maxMessageSize_ = i;
            return this;
        }

        public RIL_SIM_SAP_CONNECT_REQ clearMaxMessageSize() {
            this.hasMaxMessageSize = false;
            this.maxMessageSize_ = 0;
            return this;
        }

        public final RIL_SIM_SAP_CONNECT_REQ clear() {
            clearMaxMessageSize();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasMaxMessageSize;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasMaxMessageSize()) {
                codedOutputStreamMicro.writeInt32(1, getMaxMessageSize());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasMaxMessageSize() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getMaxMessageSize()) : 0;
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_CONNECT_REQ mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag != 8) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setMaxMessageSize(codedInputStreamMicro.readInt32());
                }
            }
        }

        public static RIL_SIM_SAP_CONNECT_REQ parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_CONNECT_REQ) new RIL_SIM_SAP_CONNECT_REQ().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_CONNECT_REQ parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_CONNECT_REQ().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_CONNECT_RSP extends MessageMicro {
        public static final int MAX_MESSAGE_SIZE_FIELD_NUMBER = 2;
        public static final int RESPONSE_FIELD_NUMBER = 1;
        public static final int RIL_E_SAP_CONNECT_FAILURE = 1;
        public static final int RIL_E_SAP_CONNECT_OK_CALL_ONGOING = 4;
        public static final int RIL_E_SAP_MSG_SIZE_TOO_LARGE = 2;
        public static final int RIL_E_SAP_MSG_SIZE_TOO_SMALL = 3;
        public static final int RIL_E_SUCCESS = 0;
        private boolean hasMaxMessageSize;
        private boolean hasResponse;
        private int response_ = 0;
        private int maxMessageSize_ = 0;
        private int cachedSize = -1;

        public boolean hasResponse() {
            return this.hasResponse;
        }

        public int getResponse() {
            return this.response_;
        }

        public RIL_SIM_SAP_CONNECT_RSP setResponse(int i) {
            this.hasResponse = true;
            this.response_ = i;
            return this;
        }

        public RIL_SIM_SAP_CONNECT_RSP clearResponse() {
            this.hasResponse = false;
            this.response_ = 0;
            return this;
        }

        public int getMaxMessageSize() {
            return this.maxMessageSize_;
        }

        public boolean hasMaxMessageSize() {
            return this.hasMaxMessageSize;
        }

        public RIL_SIM_SAP_CONNECT_RSP setMaxMessageSize(int i) {
            this.hasMaxMessageSize = true;
            this.maxMessageSize_ = i;
            return this;
        }

        public RIL_SIM_SAP_CONNECT_RSP clearMaxMessageSize() {
            this.hasMaxMessageSize = false;
            this.maxMessageSize_ = 0;
            return this;
        }

        public final RIL_SIM_SAP_CONNECT_RSP clear() {
            clearResponse();
            clearMaxMessageSize();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasResponse;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasResponse()) {
                codedOutputStreamMicro.writeInt32(1, getResponse());
            }
            if (hasMaxMessageSize()) {
                codedOutputStreamMicro.writeInt32(2, getMaxMessageSize());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasResponse() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getResponse()) : 0;
            if (hasMaxMessageSize()) {
                iComputeInt32Size += CodedOutputStreamMicro.computeInt32Size(2, getMaxMessageSize());
            }
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_CONNECT_RSP mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    setResponse(codedInputStreamMicro.readInt32());
                } else if (tag != 16) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setMaxMessageSize(codedInputStreamMicro.readInt32());
                }
            }
        }

        public static RIL_SIM_SAP_CONNECT_RSP parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_CONNECT_RSP) new RIL_SIM_SAP_CONNECT_RSP().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_CONNECT_RSP parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_CONNECT_RSP().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_DISCONNECT_REQ extends MessageMicro {
        private int cachedSize = -1;

        public final RIL_SIM_SAP_DISCONNECT_REQ clear() {
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return true;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) {
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            this.cachedSize = 0;
            return 0;
        }

        @Override
        public RIL_SIM_SAP_DISCONNECT_REQ mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            int tag;
            do {
                tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
            } while (parseUnknownField(codedInputStreamMicro, tag));
            return this;
        }

        public static RIL_SIM_SAP_DISCONNECT_REQ parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_DISCONNECT_REQ) new RIL_SIM_SAP_DISCONNECT_REQ().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_DISCONNECT_REQ parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_DISCONNECT_REQ().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_DISCONNECT_RSP extends MessageMicro {
        private int cachedSize = -1;

        public final RIL_SIM_SAP_DISCONNECT_RSP clear() {
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return true;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) {
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            this.cachedSize = 0;
            return 0;
        }

        @Override
        public RIL_SIM_SAP_DISCONNECT_RSP mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            int tag;
            do {
                tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
            } while (parseUnknownField(codedInputStreamMicro, tag));
            return this;
        }

        public static RIL_SIM_SAP_DISCONNECT_RSP parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_DISCONNECT_RSP) new RIL_SIM_SAP_DISCONNECT_RSP().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_DISCONNECT_RSP parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_DISCONNECT_RSP().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_DISCONNECT_IND extends MessageMicro {
        public static final int DISCONNECTTYPE_FIELD_NUMBER = 1;
        public static final int RIL_S_DISCONNECT_TYPE_GRACEFUL = 0;
        public static final int RIL_S_DISCONNECT_TYPE_IMMEDIATE = 1;
        private boolean hasDisconnectType;
        private int disconnectType_ = 0;
        private int cachedSize = -1;

        public boolean hasDisconnectType() {
            return this.hasDisconnectType;
        }

        public int getDisconnectType() {
            return this.disconnectType_;
        }

        public RIL_SIM_SAP_DISCONNECT_IND setDisconnectType(int i) {
            this.hasDisconnectType = true;
            this.disconnectType_ = i;
            return this;
        }

        public RIL_SIM_SAP_DISCONNECT_IND clearDisconnectType() {
            this.hasDisconnectType = false;
            this.disconnectType_ = 0;
            return this;
        }

        public final RIL_SIM_SAP_DISCONNECT_IND clear() {
            clearDisconnectType();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasDisconnectType;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasDisconnectType()) {
                codedOutputStreamMicro.writeInt32(1, getDisconnectType());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasDisconnectType() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getDisconnectType()) : 0;
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_DISCONNECT_IND mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag != 8) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setDisconnectType(codedInputStreamMicro.readInt32());
                }
            }
        }

        public static RIL_SIM_SAP_DISCONNECT_IND parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_DISCONNECT_IND) new RIL_SIM_SAP_DISCONNECT_IND().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_DISCONNECT_IND parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_DISCONNECT_IND().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_APDU_REQ extends MessageMicro {
        public static final int COMMAND_FIELD_NUMBER = 2;
        public static final int RIL_TYPE_APDU = 0;
        public static final int RIL_TYPE_APDU7816 = 1;
        public static final int TYPE_FIELD_NUMBER = 1;
        private boolean hasCommand;
        private boolean hasType;
        private int type_ = 0;
        private ByteStringMicro command_ = ByteStringMicro.EMPTY;
        private int cachedSize = -1;

        public boolean hasType() {
            return this.hasType;
        }

        public int getType() {
            return this.type_;
        }

        public RIL_SIM_SAP_APDU_REQ setType(int i) {
            this.hasType = true;
            this.type_ = i;
            return this;
        }

        public RIL_SIM_SAP_APDU_REQ clearType() {
            this.hasType = false;
            this.type_ = 0;
            return this;
        }

        public ByteStringMicro getCommand() {
            return this.command_;
        }

        public boolean hasCommand() {
            return this.hasCommand;
        }

        public RIL_SIM_SAP_APDU_REQ setCommand(ByteStringMicro byteStringMicro) {
            this.hasCommand = true;
            this.command_ = byteStringMicro;
            return this;
        }

        public RIL_SIM_SAP_APDU_REQ clearCommand() {
            this.hasCommand = false;
            this.command_ = ByteStringMicro.EMPTY;
            return this;
        }

        public final RIL_SIM_SAP_APDU_REQ clear() {
            clearType();
            clearCommand();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasType && this.hasCommand;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasType()) {
                codedOutputStreamMicro.writeInt32(1, getType());
            }
            if (hasCommand()) {
                codedOutputStreamMicro.writeBytes(2, getCommand());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasType() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getType()) : 0;
            if (hasCommand()) {
                iComputeInt32Size += CodedOutputStreamMicro.computeBytesSize(2, getCommand());
            }
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_APDU_REQ mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    setType(codedInputStreamMicro.readInt32());
                } else if (tag != 18) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setCommand(codedInputStreamMicro.readBytes());
                }
            }
        }

        public static RIL_SIM_SAP_APDU_REQ parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_APDU_REQ) new RIL_SIM_SAP_APDU_REQ().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_APDU_REQ parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_APDU_REQ().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_APDU_RSP extends MessageMicro {
        public static final int APDURESPONSE_FIELD_NUMBER = 3;
        public static final int RESPONSE_FIELD_NUMBER = 2;
        public static final int RIL_E_GENERIC_FAILURE = 1;
        public static final int RIL_E_SIM_ABSENT = 4;
        public static final int RIL_E_SIM_ALREADY_POWERED_OFF = 3;
        public static final int RIL_E_SIM_NOT_READY = 2;
        public static final int RIL_E_SUCCESS = 0;
        public static final int RIL_TYPE_APDU = 0;
        public static final int RIL_TYPE_APDU7816 = 1;
        public static final int TYPE_FIELD_NUMBER = 1;
        private boolean hasApduResponse;
        private boolean hasResponse;
        private boolean hasType;
        private int type_ = 0;
        private int response_ = 0;
        private ByteStringMicro apduResponse_ = ByteStringMicro.EMPTY;
        private int cachedSize = -1;

        public boolean hasType() {
            return this.hasType;
        }

        public int getType() {
            return this.type_;
        }

        public RIL_SIM_SAP_APDU_RSP setType(int i) {
            this.hasType = true;
            this.type_ = i;
            return this;
        }

        public RIL_SIM_SAP_APDU_RSP clearType() {
            this.hasType = false;
            this.type_ = 0;
            return this;
        }

        public boolean hasResponse() {
            return this.hasResponse;
        }

        public int getResponse() {
            return this.response_;
        }

        public RIL_SIM_SAP_APDU_RSP setResponse(int i) {
            this.hasResponse = true;
            this.response_ = i;
            return this;
        }

        public RIL_SIM_SAP_APDU_RSP clearResponse() {
            this.hasResponse = false;
            this.response_ = 0;
            return this;
        }

        public ByteStringMicro getApduResponse() {
            return this.apduResponse_;
        }

        public boolean hasApduResponse() {
            return this.hasApduResponse;
        }

        public RIL_SIM_SAP_APDU_RSP setApduResponse(ByteStringMicro byteStringMicro) {
            this.hasApduResponse = true;
            this.apduResponse_ = byteStringMicro;
            return this;
        }

        public RIL_SIM_SAP_APDU_RSP clearApduResponse() {
            this.hasApduResponse = false;
            this.apduResponse_ = ByteStringMicro.EMPTY;
            return this;
        }

        public final RIL_SIM_SAP_APDU_RSP clear() {
            clearType();
            clearResponse();
            clearApduResponse();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasType && this.hasResponse;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasType()) {
                codedOutputStreamMicro.writeInt32(1, getType());
            }
            if (hasResponse()) {
                codedOutputStreamMicro.writeInt32(2, getResponse());
            }
            if (hasApduResponse()) {
                codedOutputStreamMicro.writeBytes(3, getApduResponse());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasType() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getType()) : 0;
            if (hasResponse()) {
                iComputeInt32Size += CodedOutputStreamMicro.computeInt32Size(2, getResponse());
            }
            if (hasApduResponse()) {
                iComputeInt32Size += CodedOutputStreamMicro.computeBytesSize(3, getApduResponse());
            }
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_APDU_RSP mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    setType(codedInputStreamMicro.readInt32());
                } else if (tag == 16) {
                    setResponse(codedInputStreamMicro.readInt32());
                } else if (tag != 26) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setApduResponse(codedInputStreamMicro.readBytes());
                }
            }
        }

        public static RIL_SIM_SAP_APDU_RSP parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_APDU_RSP) new RIL_SIM_SAP_APDU_RSP().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_APDU_RSP parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_APDU_RSP().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_TRANSFER_ATR_REQ extends MessageMicro {
        private int cachedSize = -1;

        public final RIL_SIM_SAP_TRANSFER_ATR_REQ clear() {
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return true;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) {
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            this.cachedSize = 0;
            return 0;
        }

        @Override
        public RIL_SIM_SAP_TRANSFER_ATR_REQ mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            int tag;
            do {
                tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
            } while (parseUnknownField(codedInputStreamMicro, tag));
            return this;
        }

        public static RIL_SIM_SAP_TRANSFER_ATR_REQ parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_TRANSFER_ATR_REQ) new RIL_SIM_SAP_TRANSFER_ATR_REQ().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_TRANSFER_ATR_REQ parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_TRANSFER_ATR_REQ().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_TRANSFER_ATR_RSP extends MessageMicro {
        public static final int ATR_FIELD_NUMBER = 2;
        public static final int RESPONSE_FIELD_NUMBER = 1;
        public static final int RIL_E_GENERIC_FAILURE = 1;
        public static final int RIL_E_SIM_ABSENT = 4;
        public static final int RIL_E_SIM_ALREADY_POWERED_OFF = 3;
        public static final int RIL_E_SIM_ALREADY_POWERED_ON = 18;
        public static final int RIL_E_SIM_DATA_NOT_AVAILABLE = 6;
        public static final int RIL_E_SUCCESS = 0;
        private boolean hasAtr;
        private boolean hasResponse;
        private int response_ = 0;
        private ByteStringMicro atr_ = ByteStringMicro.EMPTY;
        private int cachedSize = -1;

        public boolean hasResponse() {
            return this.hasResponse;
        }

        public int getResponse() {
            return this.response_;
        }

        public RIL_SIM_SAP_TRANSFER_ATR_RSP setResponse(int i) {
            this.hasResponse = true;
            this.response_ = i;
            return this;
        }

        public RIL_SIM_SAP_TRANSFER_ATR_RSP clearResponse() {
            this.hasResponse = false;
            this.response_ = 0;
            return this;
        }

        public ByteStringMicro getAtr() {
            return this.atr_;
        }

        public boolean hasAtr() {
            return this.hasAtr;
        }

        public RIL_SIM_SAP_TRANSFER_ATR_RSP setAtr(ByteStringMicro byteStringMicro) {
            this.hasAtr = true;
            this.atr_ = byteStringMicro;
            return this;
        }

        public RIL_SIM_SAP_TRANSFER_ATR_RSP clearAtr() {
            this.hasAtr = false;
            this.atr_ = ByteStringMicro.EMPTY;
            return this;
        }

        public final RIL_SIM_SAP_TRANSFER_ATR_RSP clear() {
            clearResponse();
            clearAtr();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasResponse;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasResponse()) {
                codedOutputStreamMicro.writeInt32(1, getResponse());
            }
            if (hasAtr()) {
                codedOutputStreamMicro.writeBytes(2, getAtr());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasResponse() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getResponse()) : 0;
            if (hasAtr()) {
                iComputeInt32Size += CodedOutputStreamMicro.computeBytesSize(2, getAtr());
            }
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_TRANSFER_ATR_RSP mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    setResponse(codedInputStreamMicro.readInt32());
                } else if (tag != 18) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setAtr(codedInputStreamMicro.readBytes());
                }
            }
        }

        public static RIL_SIM_SAP_TRANSFER_ATR_RSP parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_TRANSFER_ATR_RSP) new RIL_SIM_SAP_TRANSFER_ATR_RSP().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_TRANSFER_ATR_RSP parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_TRANSFER_ATR_RSP().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_POWER_REQ extends MessageMicro {
        public static final int STATE_FIELD_NUMBER = 1;
        private boolean hasState;
        private boolean state_ = false;
        private int cachedSize = -1;

        public boolean getState() {
            return this.state_;
        }

        public boolean hasState() {
            return this.hasState;
        }

        public RIL_SIM_SAP_POWER_REQ setState(boolean z) {
            this.hasState = true;
            this.state_ = z;
            return this;
        }

        public RIL_SIM_SAP_POWER_REQ clearState() {
            this.hasState = false;
            this.state_ = false;
            return this;
        }

        public final RIL_SIM_SAP_POWER_REQ clear() {
            clearState();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasState;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasState()) {
                codedOutputStreamMicro.writeBool(1, getState());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeBoolSize = hasState() ? 0 + CodedOutputStreamMicro.computeBoolSize(1, getState()) : 0;
            this.cachedSize = iComputeBoolSize;
            return iComputeBoolSize;
        }

        @Override
        public RIL_SIM_SAP_POWER_REQ mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag != 8) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setState(codedInputStreamMicro.readBool());
                }
            }
        }

        public static RIL_SIM_SAP_POWER_REQ parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_POWER_REQ) new RIL_SIM_SAP_POWER_REQ().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_POWER_REQ parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_POWER_REQ().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_POWER_RSP extends MessageMicro {
        public static final int RESPONSE_FIELD_NUMBER = 1;
        public static final int RIL_E_GENERIC_FAILURE = 2;
        public static final int RIL_E_SIM_ABSENT = 11;
        public static final int RIL_E_SIM_ALREADY_POWERED_OFF = 17;
        public static final int RIL_E_SIM_ALREADY_POWERED_ON = 18;
        public static final int RIL_E_SUCCESS = 0;
        private boolean hasResponse;
        private int response_ = 0;
        private int cachedSize = -1;

        public boolean hasResponse() {
            return this.hasResponse;
        }

        public int getResponse() {
            return this.response_;
        }

        public RIL_SIM_SAP_POWER_RSP setResponse(int i) {
            this.hasResponse = true;
            this.response_ = i;
            return this;
        }

        public RIL_SIM_SAP_POWER_RSP clearResponse() {
            this.hasResponse = false;
            this.response_ = 0;
            return this;
        }

        public final RIL_SIM_SAP_POWER_RSP clear() {
            clearResponse();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasResponse;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasResponse()) {
                codedOutputStreamMicro.writeInt32(1, getResponse());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasResponse() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getResponse()) : 0;
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_POWER_RSP mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag != 8) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setResponse(codedInputStreamMicro.readInt32());
                }
            }
        }

        public static RIL_SIM_SAP_POWER_RSP parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_POWER_RSP) new RIL_SIM_SAP_POWER_RSP().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_POWER_RSP parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_POWER_RSP().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_RESET_SIM_REQ extends MessageMicro {
        private int cachedSize = -1;

        public final RIL_SIM_SAP_RESET_SIM_REQ clear() {
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return true;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) {
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            this.cachedSize = 0;
            return 0;
        }

        @Override
        public RIL_SIM_SAP_RESET_SIM_REQ mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            int tag;
            do {
                tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
            } while (parseUnknownField(codedInputStreamMicro, tag));
            return this;
        }

        public static RIL_SIM_SAP_RESET_SIM_REQ parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_RESET_SIM_REQ) new RIL_SIM_SAP_RESET_SIM_REQ().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_RESET_SIM_REQ parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_RESET_SIM_REQ().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_RESET_SIM_RSP extends MessageMicro {
        public static final int RESPONSE_FIELD_NUMBER = 1;
        public static final int RIL_E_GENERIC_FAILURE = 2;
        public static final int RIL_E_SIM_ABSENT = 11;
        public static final int RIL_E_SIM_ALREADY_POWERED_OFF = 17;
        public static final int RIL_E_SIM_NOT_READY = 16;
        public static final int RIL_E_SUCCESS = 0;
        private boolean hasResponse;
        private int response_ = 0;
        private int cachedSize = -1;

        public boolean hasResponse() {
            return this.hasResponse;
        }

        public int getResponse() {
            return this.response_;
        }

        public RIL_SIM_SAP_RESET_SIM_RSP setResponse(int i) {
            this.hasResponse = true;
            this.response_ = i;
            return this;
        }

        public RIL_SIM_SAP_RESET_SIM_RSP clearResponse() {
            this.hasResponse = false;
            this.response_ = 0;
            return this;
        }

        public final RIL_SIM_SAP_RESET_SIM_RSP clear() {
            clearResponse();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasResponse;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasResponse()) {
                codedOutputStreamMicro.writeInt32(1, getResponse());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasResponse() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getResponse()) : 0;
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_RESET_SIM_RSP mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag != 8) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setResponse(codedInputStreamMicro.readInt32());
                }
            }
        }

        public static RIL_SIM_SAP_RESET_SIM_RSP parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_RESET_SIM_RSP) new RIL_SIM_SAP_RESET_SIM_RSP().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_RESET_SIM_RSP parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_RESET_SIM_RSP().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_STATUS_IND extends MessageMicro {
        public static final int RIL_SIM_STATUS_CARD_INSERTED = 4;
        public static final int RIL_SIM_STATUS_CARD_NOT_ACCESSIBLE = 2;
        public static final int RIL_SIM_STATUS_CARD_REMOVED = 3;
        public static final int RIL_SIM_STATUS_CARD_RESET = 1;
        public static final int RIL_SIM_STATUS_RECOVERED = 5;
        public static final int RIL_SIM_STATUS_UNKNOWN_ERROR = 0;
        public static final int STATUSCHANGE_FIELD_NUMBER = 1;
        private boolean hasStatusChange;
        private int statusChange_ = 0;
        private int cachedSize = -1;

        public boolean hasStatusChange() {
            return this.hasStatusChange;
        }

        public int getStatusChange() {
            return this.statusChange_;
        }

        public RIL_SIM_SAP_STATUS_IND setStatusChange(int i) {
            this.hasStatusChange = true;
            this.statusChange_ = i;
            return this;
        }

        public RIL_SIM_SAP_STATUS_IND clearStatusChange() {
            this.hasStatusChange = false;
            this.statusChange_ = 0;
            return this;
        }

        public final RIL_SIM_SAP_STATUS_IND clear() {
            clearStatusChange();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasStatusChange;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasStatusChange()) {
                codedOutputStreamMicro.writeInt32(1, getStatusChange());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasStatusChange() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getStatusChange()) : 0;
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_STATUS_IND mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag != 8) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setStatusChange(codedInputStreamMicro.readInt32());
                }
            }
        }

        public static RIL_SIM_SAP_STATUS_IND parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_STATUS_IND) new RIL_SIM_SAP_STATUS_IND().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_STATUS_IND parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_STATUS_IND().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_REQ extends MessageMicro {
        private int cachedSize = -1;

        public final RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_REQ clear() {
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return true;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) {
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            this.cachedSize = 0;
            return 0;
        }

        @Override
        public RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_REQ mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            int tag;
            do {
                tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
            } while (parseUnknownField(codedInputStreamMicro, tag));
            return this;
        }

        public static RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_REQ parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_REQ) new RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_REQ().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_REQ parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_REQ().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP extends MessageMicro {
        public static final int CARDREADERSTATUS_FIELD_NUMBER = 2;
        public static final int RESPONSE_FIELD_NUMBER = 1;
        public static final int RIL_E_GENERIC_FAILURE = 2;
        public static final int RIL_E_SIM_DATA_NOT_AVAILABLE = 6;
        public static final int RIL_E_SUCCESS = 0;
        private boolean hasCardReaderStatus;
        private boolean hasResponse;
        private int response_ = 0;
        private int cardReaderStatus_ = 0;
        private int cachedSize = -1;

        public boolean hasResponse() {
            return this.hasResponse;
        }

        public int getResponse() {
            return this.response_;
        }

        public RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP setResponse(int i) {
            this.hasResponse = true;
            this.response_ = i;
            return this;
        }

        public RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP clearResponse() {
            this.hasResponse = false;
            this.response_ = 0;
            return this;
        }

        public int getCardReaderStatus() {
            return this.cardReaderStatus_;
        }

        public boolean hasCardReaderStatus() {
            return this.hasCardReaderStatus;
        }

        public RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP setCardReaderStatus(int i) {
            this.hasCardReaderStatus = true;
            this.cardReaderStatus_ = i;
            return this;
        }

        public RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP clearCardReaderStatus() {
            this.hasCardReaderStatus = false;
            this.cardReaderStatus_ = 0;
            return this;
        }

        public final RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP clear() {
            clearResponse();
            clearCardReaderStatus();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasResponse;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasResponse()) {
                codedOutputStreamMicro.writeInt32(1, getResponse());
            }
            if (hasCardReaderStatus()) {
                codedOutputStreamMicro.writeInt32(2, getCardReaderStatus());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasResponse() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getResponse()) : 0;
            if (hasCardReaderStatus()) {
                iComputeInt32Size += CodedOutputStreamMicro.computeInt32Size(2, getCardReaderStatus());
            }
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    setResponse(codedInputStreamMicro.readInt32());
                } else if (tag != 16) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setCardReaderStatus(codedInputStreamMicro.readInt32());
                }
            }
        }

        public static RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP) new RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_ERROR_RSP extends MessageMicro {
        private int cachedSize = -1;

        public final RIL_SIM_SAP_ERROR_RSP clear() {
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return true;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) {
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            this.cachedSize = 0;
            return 0;
        }

        @Override
        public RIL_SIM_SAP_ERROR_RSP mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            int tag;
            do {
                tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
            } while (parseUnknownField(codedInputStreamMicro, tag));
            return this;
        }

        public static RIL_SIM_SAP_ERROR_RSP parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_ERROR_RSP) new RIL_SIM_SAP_ERROR_RSP().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_ERROR_RSP parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_ERROR_RSP().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ extends MessageMicro {
        public static final int PROTOCOL_FIELD_NUMBER = 1;
        public static final int t0 = 0;
        public static final int t1 = 1;
        private boolean hasProtocol;
        private int protocol_ = 0;
        private int cachedSize = -1;

        public boolean hasProtocol() {
            return this.hasProtocol;
        }

        public int getProtocol() {
            return this.protocol_;
        }

        public RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ setProtocol(int i) {
            this.hasProtocol = true;
            this.protocol_ = i;
            return this;
        }

        public RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ clearProtocol() {
            this.hasProtocol = false;
            this.protocol_ = 0;
            return this;
        }

        public final RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ clear() {
            clearProtocol();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasProtocol;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasProtocol()) {
                codedOutputStreamMicro.writeInt32(1, getProtocol());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasProtocol() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getProtocol()) : 0;
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag != 8) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setProtocol(codedInputStreamMicro.readInt32());
                }
            }
        }

        public static RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ) new RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_REQ().mergeFrom(codedInputStreamMicro);
        }
    }

    public static final class RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP extends MessageMicro {
        public static final int RESPONSE_FIELD_NUMBER = 1;
        public static final int RIL_E_GENERIC_FAILURE = 2;
        public static final int RIL_E_SIM_ABSENT = 11;
        public static final int RIL_E_SIM_ALREADY_POWERED_OFF = 17;
        public static final int RIL_E_SIM_NOT_READY = 16;
        public static final int RIL_E_SUCCESS = 0;
        private boolean hasResponse;
        private int response_ = 0;
        private int cachedSize = -1;

        public boolean hasResponse() {
            return this.hasResponse;
        }

        public int getResponse() {
            return this.response_;
        }

        public RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP setResponse(int i) {
            this.hasResponse = true;
            this.response_ = i;
            return this;
        }

        public RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP clearResponse() {
            this.hasResponse = false;
            this.response_ = 0;
            return this;
        }

        public final RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP clear() {
            clearResponse();
            this.cachedSize = -1;
            return this;
        }

        public final boolean isInitialized() {
            return this.hasResponse;
        }

        @Override
        public void writeTo(CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
            if (hasResponse()) {
                codedOutputStreamMicro.writeInt32(1, getResponse());
            }
        }

        @Override
        public int getCachedSize() {
            if (this.cachedSize < 0) {
                getSerializedSize();
            }
            return this.cachedSize;
        }

        @Override
        public int getSerializedSize() {
            int iComputeInt32Size = hasResponse() ? 0 + CodedOutputStreamMicro.computeInt32Size(1, getResponse()) : 0;
            this.cachedSize = iComputeInt32Size;
            return iComputeInt32Size;
        }

        @Override
        public RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP mergeFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            while (true) {
                int tag = codedInputStreamMicro.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag != 8) {
                    if (!parseUnknownField(codedInputStreamMicro, tag)) {
                        return this;
                    }
                } else {
                    setResponse(codedInputStreamMicro.readInt32());
                }
            }
        }

        public static RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP parseFrom(byte[] bArr) throws InvalidProtocolBufferMicroException {
            return (RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP) new RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP().mergeFrom(bArr);
        }

        public static RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP parseFrom(CodedInputStreamMicro codedInputStreamMicro) throws IOException {
            return new RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP().mergeFrom(codedInputStreamMicro);
        }
    }
}
