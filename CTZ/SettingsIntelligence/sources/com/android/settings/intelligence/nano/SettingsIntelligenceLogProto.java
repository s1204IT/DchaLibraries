package com.android.settings.intelligence.nano;

import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface SettingsIntelligenceLogProto {

    public static final class SettingsIntelligenceEvent extends MessageNano {
        public int eventType;
        public long latencyMillis;
        public SearchResultMetadata searchResultMetadata;
        public String[] suggestionIds;

        public static final class SearchResultMetadata extends MessageNano {
            public int resultCount;
            public int searchQueryLength;
            public String searchResultKey;
            public int searchResultRank;

            public SearchResultMetadata() {
                clear();
            }

            public SearchResultMetadata clear() {
                this.searchResultKey = "";
                this.searchResultRank = 0;
                this.resultCount = 0;
                this.searchQueryLength = 0;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (!this.searchResultKey.equals("")) {
                    codedOutputByteBufferNano.writeString(1, this.searchResultKey);
                }
                if (this.searchResultRank != 0) {
                    codedOutputByteBufferNano.writeInt32(2, this.searchResultRank);
                }
                if (this.resultCount != 0) {
                    codedOutputByteBufferNano.writeInt32(3, this.resultCount);
                }
                if (this.searchQueryLength != 0) {
                    codedOutputByteBufferNano.writeInt32(4, this.searchQueryLength);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (!this.searchResultKey.equals("")) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(1, this.searchResultKey);
                }
                if (this.searchResultRank != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.searchResultRank);
                }
                if (this.resultCount != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.resultCount);
                }
                if (this.searchQueryLength != 0) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(4, this.searchQueryLength);
                }
                return iComputeSerializedSize;
            }
        }

        public SettingsIntelligenceEvent() {
            clear();
        }

        public SettingsIntelligenceEvent clear() {
            this.eventType = 0;
            this.suggestionIds = WireFormatNano.EMPTY_STRING_ARRAY;
            this.searchResultMetadata = null;
            this.latencyMillis = 0L;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.eventType != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.eventType);
            }
            if (this.suggestionIds != null && this.suggestionIds.length > 0) {
                for (int i = 0; i < this.suggestionIds.length; i++) {
                    String str = this.suggestionIds[i];
                    if (str != null) {
                        codedOutputByteBufferNano.writeString(2, str);
                    }
                }
            }
            if (this.searchResultMetadata != null) {
                codedOutputByteBufferNano.writeMessage(3, this.searchResultMetadata);
            }
            if (this.latencyMillis != 0) {
                codedOutputByteBufferNano.writeInt64(4, this.latencyMillis);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.eventType != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.eventType);
            }
            if (this.suggestionIds != null && this.suggestionIds.length > 0) {
                int iComputeStringSizeNoTag = 0;
                int i = 0;
                for (int i2 = 0; i2 < this.suggestionIds.length; i2++) {
                    String str = this.suggestionIds[i2];
                    if (str != null) {
                        i++;
                        iComputeStringSizeNoTag += CodedOutputByteBufferNano.computeStringSizeNoTag(str);
                    }
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeStringSizeNoTag + (1 * i);
            }
            if (this.searchResultMetadata != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(3, this.searchResultMetadata);
            }
            if (this.latencyMillis != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(4, this.latencyMillis);
            }
            return iComputeSerializedSize;
        }
    }
}
