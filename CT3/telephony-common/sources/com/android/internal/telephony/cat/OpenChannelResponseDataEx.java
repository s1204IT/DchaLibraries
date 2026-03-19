package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

class OpenChannelResponseDataEx extends OpenChannelResponseData {
    DnsServerAddress mDnsServerAddress;
    int mProtocolType;

    OpenChannelResponseDataEx(ChannelStatus channelStatus, BearerDesc bearerDesc, int bufferSize, int protocolType) {
        super(channelStatus, bearerDesc, bufferSize);
        this.mProtocolType = -1;
        this.mDnsServerAddress = null;
        CatLog.d("[BIP]", "OpenChannelResponseDataEx-constructor: protocolType " + protocolType);
        this.mProtocolType = protocolType;
    }

    OpenChannelResponseDataEx(ChannelStatus channelStatus, BearerDesc bearerDesc, int bufferSize, DnsServerAddress dnsServerAddress) {
        super(channelStatus, bearerDesc, bufferSize);
        this.mProtocolType = -1;
        this.mDnsServerAddress = null;
        this.mDnsServerAddress = dnsServerAddress;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        if (buf == null) {
            CatLog.e("[BIP]", "OpenChannelResponseDataEx-format: buf is null");
            return;
        }
        if (2 == this.mProtocolType || 1 == this.mProtocolType) {
            if (this.mBearerDesc == null) {
                CatLog.e("[BIP]", "OpenChannelResponseDataEx-format: bearer null");
                return;
            } else if (this.mBearerDesc.bearerType != 2 && this.mBearerDesc.bearerType != 3 && this.mBearerDesc.bearerType != 9 && this.mBearerDesc.bearerType != 11) {
                CatLog.e("[BIP]", "OpenChannelResponseDataEx-format: bearer type is not gprs");
            }
        }
        if (this.mChannelStatus != null) {
            CatLog.d("[BIP]", "OpenChannelResponseDataEx-format: Write channel status into TR");
            int tag = ComprehensionTlvTag.CHANNEL_STATUS.value();
            buf.write(tag);
            buf.write(2);
            buf.write(this.mChannelStatus.mChannelId | this.mChannelStatus.mChannelStatus);
            buf.write(this.mChannelStatus.mChannelStatusInfo);
            CatLog.d("[BIP]", "OpenChannel Channel status Rsp:tag[" + tag + "],len[2],cId[" + this.mChannelStatus.mChannelId + "],status[" + this.mChannelStatus.mChannelStatus + "]");
        } else {
            CatLog.d("[BIP]", "No Channel status in TR.");
        }
        if (this.mBearerDesc != null) {
            CatLog.d("[BIP]", "Write bearer description into TR. bearerType: " + this.mBearerDesc.bearerType);
            int tag2 = ComprehensionTlvTag.BEARER_DESCRIPTION.value();
            buf.write(tag2);
            if (2 == this.mBearerDesc.bearerType) {
                if (this.mBearerDesc instanceof GPRSBearerDesc) {
                    GPRSBearerDesc gprsBD = (GPRSBearerDesc) this.mBearerDesc;
                    buf.write(7);
                    buf.write(gprsBD.bearerType);
                    buf.write(gprsBD.precedence);
                    buf.write(gprsBD.delay);
                    buf.write(gprsBD.reliability);
                    buf.write(gprsBD.peak);
                    buf.write(gprsBD.mean);
                    buf.write(gprsBD.pdpType);
                    CatLog.d("[BIP]", "OpenChannelResponseDataEx-format: tag: " + tag2 + ",length: 7,bearerType: " + gprsBD.bearerType + ",precedence: " + gprsBD.precedence + ",delay: " + gprsBD.delay + ",reliability: " + gprsBD.reliability + ",peak: " + gprsBD.peak + ",mean: " + gprsBD.mean + ",pdp type: " + gprsBD.pdpType);
                } else {
                    CatLog.d("[BIP]", "Not expected GPRSBearerDesc instance");
                }
            } else if (11 == this.mBearerDesc.bearerType) {
                int[] bufferArr = new int[10];
                int index = 0;
                if (this.mBearerDesc instanceof EUTranBearerDesc) {
                    EUTranBearerDesc euTranBD = (EUTranBearerDesc) this.mBearerDesc;
                    if (euTranBD.QCI != 0) {
                        bufferArr[0] = euTranBD.QCI;
                        index = 1;
                    }
                    if (euTranBD.maxBitRateU != 0) {
                        bufferArr[index] = euTranBD.maxBitRateU;
                        index++;
                    }
                    if (euTranBD.maxBitRateD != 0) {
                        bufferArr[index] = euTranBD.maxBitRateD;
                        index++;
                    }
                    if (euTranBD.guarBitRateU != 0) {
                        bufferArr[index] = euTranBD.guarBitRateU;
                        index++;
                    }
                    if (euTranBD.guarBitRateD != 0) {
                        bufferArr[index] = euTranBD.guarBitRateD;
                        index++;
                    }
                    if (euTranBD.maxBitRateUEx != 0) {
                        bufferArr[index] = euTranBD.maxBitRateUEx;
                        index++;
                    }
                    if (euTranBD.maxBitRateDEx != 0) {
                        bufferArr[index] = euTranBD.maxBitRateDEx;
                        index++;
                    }
                    if (euTranBD.guarBitRateUEx != 0) {
                        bufferArr[index] = euTranBD.guarBitRateUEx;
                        index++;
                    }
                    if (euTranBD.guarBitRateDEx != 0) {
                        bufferArr[index] = euTranBD.guarBitRateDEx;
                        index++;
                    }
                    if (euTranBD.pdnType != 0) {
                        bufferArr[index] = euTranBD.pdnType;
                        index++;
                    }
                    CatLog.d("[BIP]", "EUTranBearerDesc length: " + index);
                    if (index > 0) {
                        buf.write(index + 1);
                    } else {
                        buf.write(1);
                    }
                    buf.write(euTranBD.bearerType);
                    for (int i = 0; i < index; i++) {
                        buf.write(bufferArr[i]);
                        CatLog.d("[BIP]", "EUTranBearerDesc buf: " + bufferArr[i]);
                    }
                } else {
                    CatLog.d("[BIP]", "Not expected EUTranBearerDesc instance");
                }
            } else if (9 == this.mBearerDesc.bearerType) {
                if (this.mBearerDesc instanceof UTranBearerDesc) {
                    UTranBearerDesc uTranBD = (UTranBearerDesc) this.mBearerDesc;
                    buf.write(18);
                    buf.write(uTranBD.bearerType);
                    buf.write(uTranBD.trafficClass);
                    buf.write(uTranBD.maxBitRateUL_High);
                    buf.write(uTranBD.maxBitRateUL_Low);
                    buf.write(uTranBD.maxBitRateDL_High);
                    buf.write(uTranBD.maxBitRateDL_Low);
                    buf.write(uTranBD.guarBitRateUL_High);
                    buf.write(uTranBD.guarBitRateUL_Low);
                    buf.write(uTranBD.guarBitRateDL_High);
                    buf.write(uTranBD.guarBitRateDL_Low);
                    buf.write(uTranBD.deliveryOrder);
                    buf.write(uTranBD.maxSduSize);
                    buf.write(uTranBD.sduErrorRatio);
                    buf.write(uTranBD.residualBitErrorRadio);
                    buf.write(uTranBD.deliveryOfErroneousSdus);
                    buf.write(uTranBD.transferDelay);
                    buf.write(uTranBD.trafficHandlingPriority);
                    buf.write(uTranBD.pdpType);
                    CatLog.d("[BIP]", "OpenChannelResponseDataEx-format: tag: " + tag2 + ",length: 18,bearerType: " + uTranBD.bearerType + ",trafficClass: " + uTranBD.trafficClass + ",maxBitRateUL_High: " + uTranBD.maxBitRateUL_High + ",maxBitRateUL_Low: " + uTranBD.maxBitRateUL_Low + ",maxBitRateDL_High: " + uTranBD.maxBitRateDL_High + ",maxBitRateDL_Low: " + uTranBD.maxBitRateDL_Low + ",guarBitRateUL_High: " + uTranBD.guarBitRateUL_High + ",guarBitRateUL_Low: " + uTranBD.guarBitRateUL_Low + ",guarBitRateDL_High: " + uTranBD.guarBitRateDL_High + ",guarBitRateDL_Low: " + uTranBD.guarBitRateDL_Low + ",deliveryOrder: " + uTranBD.deliveryOrder + ",maxSduSize: " + uTranBD.maxSduSize + ",sduErrorRatio: " + uTranBD.sduErrorRatio + ",residualBitErrorRadio: " + uTranBD.residualBitErrorRadio + ",deliveryOfErroneousSdus: " + uTranBD.deliveryOfErroneousSdus + ",transferDelay: " + uTranBD.transferDelay + ",trafficHandlingPriority: " + uTranBD.trafficHandlingPriority + ",pdp type: " + uTranBD.pdpType);
                } else {
                    CatLog.d("[BIP]", "Not expected UTranBearerDesc instance");
                }
            } else if (3 == this.mBearerDesc.bearerType) {
                buf.write(1);
                buf.write(((DefaultBearerDesc) this.mBearerDesc).bearerType);
            }
        } else {
            CatLog.d("[BIP]", "No bearer description in TR.");
        }
        if (this.mBufferSize >= 0) {
            CatLog.d("[BIP]", "Write buffer size into TR.[" + this.mBufferSize + "]");
            int tag3 = ComprehensionTlvTag.BUFFER_SIZE.value();
            buf.write(tag3);
            buf.write(2);
            buf.write(this.mBufferSize >> 8);
            buf.write(this.mBufferSize & 255);
            CatLog.d("[BIP]", "OpenChannelResponseDataEx-format: tag: " + tag3 + ",length: 2,buffer size(hi-byte): " + (this.mBufferSize >> 8) + ",buffer size(low-byte): " + (this.mBufferSize & 255));
        } else {
            CatLog.d("[BIP]", "No buffer size in TR.[" + this.mBufferSize + "]");
        }
        if (this.mDnsServerAddress != null) {
            for (InetAddress addr : this.mDnsServerAddress.dnsAddresses) {
                byte[] rawAddress = addr.getAddress();
                if (rawAddress != null) {
                    buf.write(ComprehensionTlvTag.DNS_SERVER_ADDRESS.value());
                    buf.write(rawAddress.length + 1);
                    if (rawAddress.length == 4) {
                        buf.write(33);
                    } else if (rawAddress.length == 16) {
                        buf.write(87);
                    } else {
                        CatLog.e("[BIP]", "length error: " + rawAddress.length);
                        buf.write(33);
                    }
                    buf.write(rawAddress, 0, rawAddress.length);
                }
            }
        }
    }
}
