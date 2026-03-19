package com.android.server.wifi.util;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.util.Log;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

public class FrameParser {
    private static final byte ARP_HWADDR_LEN_LEN = 1;
    private static final byte ARP_HWTYPE_LEN = 2;
    private static final byte ARP_OPCODE_REPLY = 2;
    private static final byte ARP_OPCODE_REQUEST = 1;
    private static final byte ARP_PROTOADDR_LEN_LEN = 1;
    private static final byte ARP_PROTOTYPE_LEN = 2;
    private static final short BOOTP_BOOT_FILENAME_LEN = 128;
    private static final byte BOOTP_CLIENT_HWADDR_LEN = 16;
    private static final byte BOOTP_ELAPSED_SECONDS_LEN = 2;
    private static final byte BOOTP_FLAGS_LEN = 2;
    private static final byte BOOTP_HOPCOUNT_LEN = 1;
    private static final byte BOOTP_HWADDR_LEN_LEN = 1;
    private static final byte BOOTP_HWTYPE_LEN = 1;
    private static final byte BOOTP_MAGIC_COOKIE_LEN = 4;
    private static final byte BOOTP_OPCODE_LEN = 1;
    private static final byte BOOTP_SERVER_HOSTNAME_LEN = 64;
    private static final byte BOOTP_TRANSACTION_ID_LEN = 4;
    private static final byte BYTES_PER_OCT = 8;
    private static final byte BYTES_PER_QUAD = 4;
    private static final byte DHCP_MESSAGE_TYPE_ACK = 5;
    private static final byte DHCP_MESSAGE_TYPE_DECLINE = 4;
    private static final byte DHCP_MESSAGE_TYPE_DISCOVER = 1;
    private static final byte DHCP_MESSAGE_TYPE_INFORM = 8;
    private static final byte DHCP_MESSAGE_TYPE_NAK = 6;
    private static final byte DHCP_MESSAGE_TYPE_OFFER = 2;
    private static final byte DHCP_MESSAGE_TYPE_RELEASE = 7;
    private static final byte DHCP_MESSAGE_TYPE_REQUEST = 3;
    private static final short DHCP_OPTION_TAG_END = 255;
    private static final short DHCP_OPTION_TAG_MESSAGE_TYPE = 53;
    private static final short DHCP_OPTION_TAG_PAD = 0;
    private static final byte EAPOL_KEY_DESCRIPTOR_RSN_KEY = 2;
    private static final byte EAPOL_LENGTH_LEN = 2;
    private static final byte EAPOL_TYPE_KEY = 3;
    private static final int ETHERNET_DST_MAC_ADDR_LEN = 6;
    private static final int ETHERNET_SRC_MAC_ADDR_LEN = 6;
    private static final short ETHERTYPE_ARP = 2054;
    private static final short ETHERTYPE_EAPOL = -30578;
    private static final short ETHERTYPE_IP_V4 = 2048;
    private static final short ETHERTYPE_IP_V6 = -31011;
    private static final int HTTPS_PORT = 443;
    private static final Set<Integer> HTTP_PORTS = new HashSet();
    private static final byte ICMP_TYPE_DEST_UNREACHABLE = 3;
    private static final byte ICMP_TYPE_ECHO_REPLY = 0;
    private static final byte ICMP_TYPE_ECHO_REQUEST = 8;
    private static final byte ICMP_TYPE_REDIRECT = 5;
    private static final short ICMP_V6_TYPE_ECHO_REPLY = 129;
    private static final short ICMP_V6_TYPE_ECHO_REQUEST = 128;
    private static final short ICMP_V6_TYPE_MULTICAST_LISTENER_DISCOVERY = 143;
    private static final short ICMP_V6_TYPE_NEIGHBOR_ADVERTISEMENT = 136;
    private static final short ICMP_V6_TYPE_NEIGHBOR_SOLICITATION = 135;
    private static final short ICMP_V6_TYPE_ROUTER_ADVERTISEMENT = 134;
    private static final short ICMP_V6_TYPE_ROUTER_SOLICITATION = 133;
    private static final byte IEEE_80211_ADDR1_LEN = 6;
    private static final byte IEEE_80211_ADDR2_LEN = 6;
    private static final byte IEEE_80211_ADDR3_LEN = 6;
    private static final short IEEE_80211_AUTH_ALG_FAST_BSS_TRANSITION = 2;
    private static final short IEEE_80211_AUTH_ALG_OPEN = 0;
    private static final short IEEE_80211_AUTH_ALG_SHARED_KEY = 1;
    private static final short IEEE_80211_AUTH_ALG_SIMUL_AUTH_OF_EQUALS = 3;
    private static final byte IEEE_80211_CAPABILITY_INFO_LEN = 2;
    private static final byte IEEE_80211_DURATION_LEN = 2;
    private static final byte IEEE_80211_FRAME_CTRL_FLAG_ORDER = -128;
    private static final byte IEEE_80211_FRAME_CTRL_SUBTYPE_ASSOC_REQ = 0;
    private static final byte IEEE_80211_FRAME_CTRL_SUBTYPE_ASSOC_RESP = 1;
    private static final byte IEEE_80211_FRAME_CTRL_SUBTYPE_AUTH = 11;
    private static final byte IEEE_80211_FRAME_CTRL_SUBTYPE_PROBE_REQ = 4;
    private static final byte IEEE_80211_FRAME_CTRL_SUBTYPE_PROBE_RESP = 5;
    private static final byte IEEE_80211_FRAME_CTRL_TYPE_MGMT = 0;
    private static final byte IEEE_80211_HT_CONTROL_LEN = 4;
    private static final byte IEEE_80211_SEQUENCE_CONTROL_LEN = 2;
    private static final byte IP_PROTO_ICMP = 1;
    private static final byte IP_PROTO_TCP = 6;
    private static final byte IP_PROTO_UDP = 17;
    private static final byte IP_V4_ADDR_LEN = 4;
    private static final byte IP_V4_DSCP_AND_ECN_LEN = 1;
    private static final byte IP_V4_DST_ADDR_LEN = 4;
    private static final byte IP_V4_FLAGS_AND_FRAG_OFFSET_LEN = 2;
    private static final byte IP_V4_HEADER_CHECKSUM_LEN = 2;
    private static final byte IP_V4_ID_LEN = 2;
    private static final byte IP_V4_IHL_BYTE_MASK = 15;
    private static final byte IP_V4_SRC_ADDR_LEN = 4;
    private static final byte IP_V4_TOTAL_LEN_LEN = 2;
    private static final byte IP_V4_TTL_LEN = 1;
    private static final byte IP_V4_VERSION_BYTE_MASK = -16;
    private static final byte IP_V6_ADDR_LEN = 16;
    private static final byte IP_V6_HEADER_TYPE_HOP_BY_HOP_OPTION = 0;
    private static final byte IP_V6_HEADER_TYPE_ICMP_V6 = 58;
    private static final byte IP_V6_HOP_LIMIT_LEN = 1;
    private static final byte IP_V6_PAYLOAD_LENGTH_LEN = 2;
    private static final String TAG = "FrameParser";
    private static final byte TCP_SRC_PORT_LEN = 2;
    private static final byte UDP_CHECKSUM_LEN = 2;
    private static final byte UDP_PORT_BOOTPC = 68;
    private static final byte UDP_PORT_BOOTPS = 67;
    private static final byte UDP_PORT_NTP = 123;
    private static final byte WPA_KEYLEN_LEN = 2;
    private static final byte WPA_KEY_IDENTIFIER_LEN = 8;
    private static final short WPA_KEY_INFO_FLAG_INSTALL = 64;
    private static final short WPA_KEY_INFO_FLAG_MIC = 256;
    private static final short WPA_KEY_INFO_FLAG_PAIRWISE = 8;
    private static final byte WPA_KEY_IV_LEN = 16;
    private static final byte WPA_KEY_MIC_LEN = 16;
    private static final byte WPA_KEY_NONCE_LEN = 32;
    private static final byte WPA_KEY_RECEIVE_SEQUENCE_COUNTER_LEN = 8;
    private static final byte WPA_REPLAY_COUNTER_LEN = 8;
    public String mMostSpecificProtocolString = "N/A";
    public String mTypeString = "N/A";
    public String mResultString = "N/A";

    public FrameParser(byte b, byte[] bArr) {
        try {
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
            byteBufferWrap.order(ByteOrder.BIG_ENDIAN);
            if (b == 1) {
                parseEthernetFrame(byteBufferWrap);
            } else if (b == 2) {
                parseManagementFrame(byteBufferWrap);
            }
        } catch (IllegalArgumentException | BufferUnderflowException e) {
            Log.e(TAG, "Dissection aborted mid-frame: " + e);
        }
    }

    private static short getUnsignedByte(ByteBuffer byteBuffer) {
        return (short) (byteBuffer.get() & 255);
    }

    private static int getUnsignedShort(ByteBuffer byteBuffer) {
        return byteBuffer.getShort() & 65535;
    }

    private void parseEthernetFrame(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "Ethernet";
        byteBuffer.position(byteBuffer.position() + 6 + 6);
        short s = byteBuffer.getShort();
        if (s == -31011) {
            parseIpv6Packet(byteBuffer);
            return;
        }
        if (s == -30578) {
            parseEapolPacket(byteBuffer);
        } else if (s == 2048) {
            parseIpv4Packet(byteBuffer);
        } else if (s == 2054) {
            parseArpPacket(byteBuffer);
        }
    }

    private void parseIpv4Packet(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "IPv4";
        byteBuffer.mark();
        byte b = byteBuffer.get();
        int i = (b & IP_V4_VERSION_BYTE_MASK) >> 4;
        if (i != 4) {
            Log.e(TAG, "IPv4 header: Unrecognized protocol version " + i);
            return;
        }
        byteBuffer.position(byteBuffer.position() + 1 + 2 + 2 + 2 + 1);
        short unsignedByte = getUnsignedByte(byteBuffer);
        byteBuffer.position(byteBuffer.position() + 2 + 4 + 4);
        int i2 = (b & IP_V4_IHL_BYTE_MASK) * 4;
        byteBuffer.reset();
        byteBuffer.position(byteBuffer.position() + i2);
        if (unsignedByte == 1) {
            parseIcmpPacket(byteBuffer);
        } else if (unsignedByte == 6) {
            parseTcpPacket(byteBuffer);
        } else if (unsignedByte == 17) {
            parseUdpPacket(byteBuffer);
        }
    }

    static {
        HTTP_PORTS.add(80);
        HTTP_PORTS.add(3128);
        HTTP_PORTS.add(3132);
        HTTP_PORTS.add(5985);
        HTTP_PORTS.add(8080);
        HTTP_PORTS.add(8088);
        HTTP_PORTS.add(11371);
        HTTP_PORTS.add(1900);
        HTTP_PORTS.add(2869);
        HTTP_PORTS.add(2710);
    }

    private void parseTcpPacket(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "TCP";
        byteBuffer.position(byteBuffer.position() + 2);
        int unsignedShort = getUnsignedShort(byteBuffer);
        if (unsignedShort == HTTPS_PORT) {
            this.mTypeString = "HTTPS";
        } else if (HTTP_PORTS.contains(Integer.valueOf(unsignedShort))) {
            this.mTypeString = "HTTP";
        }
    }

    private void parseUdpPacket(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "UDP";
        int unsignedShort = getUnsignedShort(byteBuffer);
        int unsignedShort2 = getUnsignedShort(byteBuffer);
        getUnsignedShort(byteBuffer);
        byteBuffer.position(byteBuffer.position() + 2);
        if ((unsignedShort == 68 && unsignedShort2 == 67) || (unsignedShort == 67 && unsignedShort2 == 68)) {
            parseDhcpPacket(byteBuffer);
        } else if (unsignedShort == 123 || unsignedShort2 == 123) {
            this.mMostSpecificProtocolString = "NTP";
        }
    }

    private void parseDhcpPacket(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "DHCP";
        byteBuffer.position(byteBuffer.position() + 1 + 1 + 1 + 1 + 4 + 2 + 2 + 16 + 16 + 64 + 128 + 4);
        while (byteBuffer.remaining() > 0) {
            short unsignedByte = getUnsignedByte(byteBuffer);
            if (unsignedByte != 0) {
                if (unsignedByte != 255) {
                    short unsignedByte2 = getUnsignedByte(byteBuffer);
                    if (unsignedByte == 53) {
                        if (unsignedByte2 != 1) {
                            Log.e(TAG, "DHCP option len: " + ((int) unsignedByte2) + " (expected |1|)");
                            return;
                        }
                        this.mTypeString = decodeDhcpMessageType(getUnsignedByte(byteBuffer));
                        return;
                    }
                    byteBuffer.position(byteBuffer.position() + unsignedByte2);
                } else {
                    return;
                }
            }
        }
    }

    private static String decodeDhcpMessageType(short s) {
        switch (s) {
            case 1:
                return "Discover";
            case 2:
                return "Offer";
            case 3:
                return "Request";
            case 4:
                return "Decline";
            case 5:
                return "Ack";
            case 6:
                return "Nak";
            case 7:
                return "Release";
            case 8:
                return "Inform";
            default:
                return "Unknown type " + ((int) s);
        }
    }

    private void parseIcmpPacket(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "ICMP";
        short unsignedByte = getUnsignedByte(byteBuffer);
        if (unsignedByte == 0) {
            this.mTypeString = "Echo Reply";
            return;
        }
        if (unsignedByte == 3) {
            this.mTypeString = "Destination Unreachable";
            return;
        }
        if (unsignedByte == 5) {
            this.mTypeString = "Redirect";
            return;
        }
        if (unsignedByte == 8) {
            this.mTypeString = "Echo Request";
            return;
        }
        this.mTypeString = "Type " + ((int) unsignedByte);
    }

    private void parseArpPacket(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "ARP";
        byteBuffer.position(byteBuffer.position() + 2 + 2 + 1 + 1);
        int unsignedShort = getUnsignedShort(byteBuffer);
        switch (unsignedShort) {
            case 1:
                this.mTypeString = "Request";
                break;
            case 2:
                this.mTypeString = "Reply";
                break;
            default:
                this.mTypeString = "Operation " + unsignedShort;
                break;
        }
    }

    private void parseIpv6Packet(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "IPv6";
        int i = (byteBuffer.getInt() & (-268435456)) >> 28;
        if (i != 6) {
            Log.e(TAG, "IPv6 header: invalid IP version " + i);
            return;
        }
        byteBuffer.position(byteBuffer.position() + 2);
        short unsignedByte = getUnsignedByte(byteBuffer);
        byteBuffer.position(byteBuffer.position() + 1 + 32);
        while (unsignedByte == 0) {
            byteBuffer.mark();
            unsignedByte = getUnsignedByte(byteBuffer);
            int unsignedByte2 = (getUnsignedByte(byteBuffer) + 1) * 8;
            byteBuffer.reset();
            byteBuffer.position(byteBuffer.position() + unsignedByte2);
        }
        if (unsignedByte == 58) {
            parseIcmpV6Packet(byteBuffer);
            return;
        }
        this.mTypeString = "Option/Protocol " + ((int) unsignedByte);
    }

    private void parseIcmpV6Packet(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "ICMPv6";
        short unsignedByte = getUnsignedByte(byteBuffer);
        if (unsignedByte != 143) {
            switch (unsignedByte) {
                case 128:
                    this.mTypeString = "Echo Request";
                    break;
                case 129:
                    this.mTypeString = "Echo Reply";
                    break;
                default:
                    switch (unsignedByte) {
                        case 133:
                            this.mTypeString = "Router Solicitation";
                            break;
                        case 134:
                            this.mTypeString = "Router Advertisement";
                            break;
                        case 135:
                            this.mTypeString = "Neighbor Solicitation";
                            break;
                        case 136:
                            this.mTypeString = "Neighbor Advertisement";
                            break;
                        default:
                            this.mTypeString = "Type " + ((int) unsignedByte);
                            break;
                    }
                    break;
            }
            return;
        }
        this.mTypeString = "MLDv2 report";
    }

    private void parseEapolPacket(ByteBuffer byteBuffer) {
        this.mMostSpecificProtocolString = "EAPOL";
        short unsignedByte = getUnsignedByte(byteBuffer);
        if (unsignedByte < 1 || unsignedByte > 2) {
            Log.e(TAG, "Unrecognized EAPOL version " + ((int) unsignedByte));
            return;
        }
        short unsignedByte2 = getUnsignedByte(byteBuffer);
        if (unsignedByte2 != 3) {
            Log.e(TAG, "Unrecognized EAPOL type " + ((int) unsignedByte2));
            return;
        }
        byteBuffer.position(byteBuffer.position() + 2);
        short unsignedByte3 = getUnsignedByte(byteBuffer);
        if (unsignedByte3 != 2) {
            Log.e(TAG, "Unrecognized key descriptor " + ((int) unsignedByte3));
            return;
        }
        short s = byteBuffer.getShort();
        if ((s & 8) == 0) {
            this.mTypeString = "Group Key";
        } else {
            this.mTypeString = "Pairwise Key";
        }
        if ((s & 256) == 0) {
            this.mTypeString += " message 1/4";
            return;
        }
        if ((s & 64) != 0) {
            this.mTypeString += " message 3/4";
            return;
        }
        byteBuffer.position(byteBuffer.position() + 2 + 8 + 32 + 16 + 8 + 8 + 16);
        if (getUnsignedShort(byteBuffer) > 0) {
            this.mTypeString += " message 2/4";
            return;
        }
        this.mTypeString += " message 4/4";
    }

    private static byte parseIeee80211FrameCtrlVersion(byte b) {
        return (byte) (b & 3);
    }

    private static byte parseIeee80211FrameCtrlType(byte b) {
        return (byte) ((b & 12) >> 2);
    }

    private static byte parseIeee80211FrameCtrlSubtype(byte b) {
        return (byte) ((b & IP_V4_VERSION_BYTE_MASK) >> 4);
    }

    private void parseManagementFrame(ByteBuffer byteBuffer) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.mMostSpecificProtocolString = "802.11 Mgmt";
        byte b = byteBuffer.get();
        byte ieee80211FrameCtrlVersion = parseIeee80211FrameCtrlVersion(b);
        if (ieee80211FrameCtrlVersion != 0) {
            Log.e(TAG, "Unrecognized 802.11 version " + ((int) ieee80211FrameCtrlVersion));
        }
        byte ieee80211FrameCtrlType = parseIeee80211FrameCtrlType(b);
        if (ieee80211FrameCtrlType != 0) {
            Log.e(TAG, "Unexpected frame type " + ((int) ieee80211FrameCtrlType));
            return;
        }
        byte b2 = byteBuffer.get();
        byteBuffer.position(byteBuffer.position() + 2 + 6 + 6 + 6 + 2);
        if ((b2 & IEEE_80211_FRAME_CTRL_FLAG_ORDER) != 0) {
            byteBuffer.position(byteBuffer.position() + 4);
        }
        byte ieee80211FrameCtrlSubtype = parseIeee80211FrameCtrlSubtype(b);
        switch (ieee80211FrameCtrlSubtype) {
            case 0:
                this.mTypeString = "Association Request";
                break;
            case 1:
                this.mTypeString = "Association Response";
                parseAssociationResponse(byteBuffer);
                break;
            case 4:
                this.mTypeString = "Probe Request";
                break;
            case 5:
                this.mTypeString = "Probe Response";
                break;
            case 11:
                this.mTypeString = "Authentication";
                parseAuthenticationFrame(byteBuffer);
                break;
            default:
                this.mTypeString = "Unexpected subtype " + ((int) ieee80211FrameCtrlSubtype);
                break;
        }
    }

    private void parseAssociationResponse(ByteBuffer byteBuffer) {
        byteBuffer.position(byteBuffer.position() + 2);
        short s = byteBuffer.getShort();
        this.mResultString = String.format("%d: %s", Short.valueOf(s), decodeIeee80211StatusCode(s));
    }

    private void parseAuthenticationFrame(ByteBuffer byteBuffer) {
        boolean z;
        short s = byteBuffer.getShort();
        short s2 = byteBuffer.getShort();
        switch (s) {
            case 0:
            case 1:
                z = s2 == 2;
                break;
            case 2:
                z = s2 == 2 || s2 == 4;
                break;
            case 3:
                z = true;
                break;
            default:
                z = false;
                break;
        }
        if (z) {
            short s3 = byteBuffer.getShort();
            this.mResultString = String.format("%d: %s", Short.valueOf(s3), decodeIeee80211StatusCode(s3));
        }
    }

    private String decodeIeee80211StatusCode(short s) {
        switch (s) {
            case 0:
                return "Success";
            case 1:
                return "Unspecified failure";
            case 2:
                return "TDLS wakeup schedule rejected; alternative provided";
            case 3:
                return "TDLS wakeup schedule rejected";
            case 4:
                return "Reserved";
            case 5:
                return "Security disabled";
            case 6:
                return "Unacceptable lifetime";
            case 7:
                return "Not in same BSS";
            case 8:
            case 9:
                return "Reserved";
            case 10:
                return "Capabilities mismatch";
            case 11:
                return "Reassociation denied; could not confirm association exists";
            case 12:
                return "Association denied for reasons outside standard";
            case 13:
                return "Unsupported authentication algorithm";
            case 14:
                return "Authentication sequence number of of sequence";
            case 15:
                return "Authentication challenge failure";
            case 16:
                return "Authentication timeout";
            case 17:
                return "Association denied; too many STAs";
            case 18:
                return "Association denied; must support BSSBasicRateSet";
            case 19:
                return "Association denied; must support short preamble";
            case 20:
                return "Association denied; must support PBCC";
            case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                return "Association denied; must support channel agility";
            case 22:
                return "Association rejected; must support spectrum management";
            case 23:
                return "Association rejected; unacceptable power capability";
            case 24:
                return "Association rejected; unacceptable supported channels";
            case 25:
                return "Association denied; must support short slot time";
            case ISupplicantStaIfaceCallback.ReasonCode.TDLS_TEARDOWN_UNSPECIFIED:
                return "Association denied; must support DSSS-OFDM";
            case 27:
                return "Association denied; must support HT";
            case 28:
                return "R0 keyholder unreachable (802.11r)";
            case 29:
                return "Association denied; must support PCO transition time";
            case 30:
                return "Refused temporarily";
            case 31:
                return "Robust management frame policy violation";
            case 32:
                return "Unspecified QoS failure";
            case 33:
                return "Association denied; insufficient bandwidth for QoS";
            case 34:
                return "Association denied; poor channel";
            case 35:
                return "Association denied; must support QoS";
            case ISupplicantStaIfaceCallback.ReasonCode.STA_LEAVING:
                return "Reserved";
            case 37:
                return "Declined";
            case 38:
                return "Invalid parameters";
            case 39:
                return "TS cannot be honored; changes suggested";
            case ISupplicantStaIfaceCallback.StatusCode.INVALID_IE:
                return "Invalid element";
            case ISupplicantStaIfaceCallback.StatusCode.GROUP_CIPHER_NOT_VALID:
                return "Invalid group cipher";
            case 42:
                return "Invalid pairwise cipher";
            case 43:
                return "Invalid auth/key mgmt proto (AKMP)";
            case ISupplicantStaIfaceCallback.StatusCode.UNSUPPORTED_RSN_IE_VERSION:
                return "Unsupported RSNE version";
            case 45:
                return "Invalid RSNE capabilities";
            case 46:
                return "Cipher suite rejected by policy";
            case 47:
                return "TS cannot be honored now; try again later";
            case 48:
                return "Direct link rejected by policy";
            case 49:
                return "Destination STA not in BSS";
            case 50:
                return "Destination STA not configured for QoS";
            case 51:
                return "Association denied; listen interval too large";
            case 52:
                return "Invalid fast transition action frame count";
            case 53:
                return "Invalid PMKID";
            case 54:
                return "Invalid MDE";
            case 55:
                return "Invalid FTE";
            case 56:
                return "Unsupported TCLAS";
            case 57:
                return "Requested TCLAS exceeds resources";
            case 58:
                return "TS cannot be honored; try another BSS";
            case 59:
                return "GAS Advertisement not supported";
            case 60:
                return "No outstanding GAS request";
            case 61:
                return "No query response from GAS server";
            case 62:
                return "GAS query timeout";
            case 63:
                return "GAS response too large";
            case 64:
                return "Home network does not support request";
            case 65:
                return "Advertisement server unreachable";
            case ISupplicantStaIfaceCallback.ReasonCode.MESH_CHANNEL_SWITCH_UNSPECIFIED:
                return "Reserved";
            case ISupplicantStaIfaceCallback.StatusCode.REQ_REFUSED_SSPN:
                return "Rejected for SSP permissions";
            case 68:
                return "Authentication required";
            case 69:
            case 70:
            case 71:
                return "Reserved";
            case ISupplicantStaIfaceCallback.StatusCode.INVALID_RSNIE:
                return "Invalid RSNE contents";
            case ISupplicantStaIfaceCallback.StatusCode.U_APSD_COEX_NOT_SUPPORTED:
                return "U-APSD coexistence unsupported";
            case ISupplicantStaIfaceCallback.StatusCode.U_APSD_COEX_MODE_NOT_SUPPORTED:
                return "Requested U-APSD coex mode unsupported";
            case ISupplicantStaIfaceCallback.StatusCode.BAD_INTERVAL_WITH_U_APSD_COEX:
                return "Requested parameter unsupported with U-APSD coex";
            case ISupplicantStaIfaceCallback.StatusCode.ANTI_CLOGGING_TOKEN_REQ:
                return "Auth rejected; anti-clogging token required";
            case ISupplicantStaIfaceCallback.StatusCode.FINITE_CYCLIC_GROUP_NOT_SUPPORTED:
                return "Auth rejected; offered group is not supported";
            case ISupplicantStaIfaceCallback.StatusCode.CANNOT_FIND_ALT_TBTT:
                return "Cannot find alternative TBTT";
            case ISupplicantStaIfaceCallback.StatusCode.TRANSMISSION_FAILURE:
                return "Transmission failure";
            case ISupplicantStaIfaceCallback.StatusCode.REQ_TCLAS_NOT_SUPPORTED:
                return "Requested TCLAS not supported";
            case ISupplicantStaIfaceCallback.StatusCode.TCLAS_RESOURCES_EXCHAUSTED:
                return "TCLAS resources exhausted";
            case ISupplicantStaIfaceCallback.StatusCode.REJECTED_WITH_SUGGESTED_BSS_TRANSITION:
                return "Rejected with suggested BSS transition";
            case ISupplicantStaIfaceCallback.StatusCode.REJECT_WITH_SCHEDULE:
                return "Reserved";
            case ISupplicantStaIfaceCallback.StatusCode.REJECT_NO_WAKEUP_SPECIFIED:
            case ISupplicantStaIfaceCallback.StatusCode.SUCCESS_POWER_SAVE_MODE:
            case ISupplicantStaIfaceCallback.StatusCode.PENDING_ADMITTING_FST_SESSION:
            case ISupplicantStaIfaceCallback.StatusCode.PERFORMING_FST_NOW:
            case ISupplicantStaIfaceCallback.StatusCode.PENDING_GAP_IN_BA_WINDOW:
            case ISupplicantStaIfaceCallback.StatusCode.REJECT_U_PID_SETTING:
            case 90:
            case 91:
                return "<unspecified>";
            case ISupplicantStaIfaceCallback.StatusCode.REFUSED_EXTERNAL_REASON:
                return "Refused due to external reason";
            case ISupplicantStaIfaceCallback.StatusCode.REFUSED_AP_OUT_OF_MEMORY:
                return "Refused; AP out of memory";
            case ISupplicantStaIfaceCallback.StatusCode.REJECTED_EMERGENCY_SERVICE_NOT_SUPPORTED:
                return "Refused; emergency services not supported";
            case ISupplicantStaIfaceCallback.StatusCode.QUERY_RESP_OUTSTANDING:
                return "GAS query response outstanding";
            case ISupplicantStaIfaceCallback.StatusCode.REJECT_DSE_BAND:
            case ISupplicantStaIfaceCallback.StatusCode.TCLAS_PROCESSING_TERMINATED:
            case ISupplicantStaIfaceCallback.StatusCode.TS_SCHEDULE_CONFLICT:
            case ISupplicantStaIfaceCallback.StatusCode.DENIED_WITH_SUGGESTED_BAND_AND_CHANNEL:
                return "Reserved";
            case 100:
                return "Failed; reservation conflict";
            case ISupplicantStaIfaceCallback.StatusCode.MAF_LIMIT_EXCEEDED:
                return "Failed; exceeded MAF limit";
            case ISupplicantStaIfaceCallback.StatusCode.MCCA_TRACK_LIMIT_EXCEEDED:
                return "Failed; exceeded MCCA track limit";
            default:
                return "Reserved";
        }
    }
}
