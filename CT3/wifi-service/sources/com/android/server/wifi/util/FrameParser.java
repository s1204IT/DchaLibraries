package com.android.server.wifi.util;

import android.util.Log;
import com.android.server.wifi.WifiQualifiedNetworkSelector;
import com.android.server.wifi.anqp.CivicLocationElement;
import com.android.server.wifi.anqp.eap.EAP;
import com.google.protobuf.nano.Extension;
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

    public FrameParser(byte frameType, byte[] frameBytes) {
        try {
            ByteBuffer frameBuffer = ByteBuffer.wrap(frameBytes);
            frameBuffer.order(ByteOrder.BIG_ENDIAN);
            if (frameType == 1) {
                parseEthernetFrame(frameBuffer);
            } else if (frameType == 2) {
                parseManagementFrame(frameBuffer);
            }
        } catch (IllegalArgumentException | BufferUnderflowException e) {
            Log.e(TAG, "Dissection aborted mid-frame: " + e);
        }
    }

    private static short getUnsignedByte(ByteBuffer data) {
        return (short) (data.get() & 255);
    }

    private static int getUnsignedShort(ByteBuffer data) {
        return data.getShort() & 65535;
    }

    private void parseEthernetFrame(ByteBuffer data) {
        this.mMostSpecificProtocolString = "Ethernet";
        data.position(data.position() + 6 + 6);
        short etherType = data.getShort();
        switch (etherType) {
            case -31011:
                parseIpv6Packet(data);
                break;
            case -30578:
                parseEapolPacket(data);
                break;
            case 2048:
                parseIpv4Packet(data);
                break;
            case 2054:
                parseArpPacket(data);
                break;
        }
    }

    private void parseIpv4Packet(ByteBuffer data) {
        this.mMostSpecificProtocolString = "IPv4";
        data.mark();
        byte versionAndHeaderLen = data.get();
        int version = (versionAndHeaderLen & IP_V4_VERSION_BYTE_MASK) >> 4;
        if (version != 4) {
            Log.e(TAG, "IPv4 header: Unrecognized protocol version " + version);
        }
        data.position(data.position() + 1 + 2 + 2 + 2 + 1);
        short protocolNumber = getUnsignedByte(data);
        data.position(data.position() + 2 + 4 + 4);
        int headerLen = (versionAndHeaderLen & IP_V4_IHL_BYTE_MASK) * 4;
        data.reset();
        data.position(data.position() + headerLen);
        switch (protocolNumber) {
            case 1:
                parseIcmpPacket(data);
                break;
            case 6:
                parseTcpPacket(data);
                break;
            case 17:
                parseUdpPacket(data);
                break;
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

    private void parseTcpPacket(ByteBuffer data) {
        this.mMostSpecificProtocolString = "TCP";
        data.position(data.position() + 2);
        int dstPort = getUnsignedShort(data);
        if (dstPort == HTTPS_PORT) {
            this.mTypeString = "HTTPS";
        } else {
            if (!HTTP_PORTS.contains(Integer.valueOf(dstPort))) {
                return;
            }
            this.mTypeString = "HTTP";
        }
    }

    private void parseUdpPacket(ByteBuffer data) {
        this.mMostSpecificProtocolString = "UDP";
        int srcPort = getUnsignedShort(data);
        int dstPort = getUnsignedShort(data);
        getUnsignedShort(data);
        data.position(data.position() + 2);
        if ((srcPort == 68 && dstPort == 67) || (srcPort == 67 && dstPort == 68)) {
            parseDhcpPacket(data);
        } else {
            if (srcPort != 123 && dstPort != 123) {
                return;
            }
            this.mMostSpecificProtocolString = "NTP";
        }
    }

    private void parseDhcpPacket(ByteBuffer data) {
        this.mMostSpecificProtocolString = "DHCP";
        data.position(data.position() + 1 + 1 + 1 + 1 + 4 + 2 + 2 + 16 + 16 + 64 + 128 + 4);
        while (data.remaining() > 0) {
            short dhcpOptionTag = getUnsignedByte(data);
            if (dhcpOptionTag != 0) {
                if (dhcpOptionTag == 255) {
                    return;
                }
                short dhcpOptionLen = getUnsignedByte(data);
                switch (dhcpOptionTag) {
                    case EAP.EAP_EKE:
                        if (dhcpOptionLen != 1) {
                            Log.e(TAG, "DHCP option len: " + ((int) dhcpOptionLen) + " (expected |1|)");
                            return;
                        } else {
                            this.mTypeString = decodeDhcpMessageType(getUnsignedByte(data));
                            return;
                        }
                    default:
                        data.position(data.position() + dhcpOptionLen);
                        break;
                }
            }
        }
    }

    private static String decodeDhcpMessageType(short messageType) {
        switch (messageType) {
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
                return "Unknown type " + ((int) messageType);
        }
    }

    private void parseIcmpPacket(ByteBuffer data) {
        this.mMostSpecificProtocolString = "ICMP";
        short messageType = getUnsignedByte(data);
        switch (messageType) {
            case 0:
                this.mTypeString = "Echo Reply";
                break;
            case 1:
            case 2:
            case 4:
            case 6:
            case 7:
            default:
                this.mTypeString = "Type " + ((int) messageType);
                break;
            case 3:
                this.mTypeString = "Destination Unreachable";
                break;
            case 5:
                this.mTypeString = "Redirect";
                break;
            case 8:
                this.mTypeString = "Echo Request";
                break;
        }
    }

    private void parseArpPacket(ByteBuffer data) {
        this.mMostSpecificProtocolString = "ARP";
        data.position(data.position() + 2 + 2 + 1 + 1);
        int opCode = getUnsignedShort(data);
        switch (opCode) {
            case 1:
                this.mTypeString = "Request";
                break;
            case 2:
                this.mTypeString = "Reply";
                break;
            default:
                this.mTypeString = "Operation " + opCode;
                break;
        }
    }

    private void parseIpv6Packet(ByteBuffer data) {
        this.mMostSpecificProtocolString = "IPv6";
        int versionClassAndLabel = data.getInt();
        int version = ((-268435456) & versionClassAndLabel) >> 28;
        if (version != 6) {
            Log.e(TAG, "IPv6 header: invalid IP version " + version);
        }
        data.position(data.position() + 2);
        short nextHeaderType = getUnsignedByte(data);
        data.position(data.position() + 1 + 32);
        while (nextHeaderType == 0) {
            data.mark();
            nextHeaderType = getUnsignedByte(data);
            int thisHeaderLen = (getUnsignedByte(data) + IEEE_80211_AUTH_ALG_SHARED_KEY) * 8;
            data.reset();
            data.position(data.position() + thisHeaderLen);
        }
        switch (nextHeaderType) {
            case 58:
                parseIcmpV6Packet(data);
                break;
            default:
                this.mTypeString = "Option/Protocol " + ((int) nextHeaderType);
                break;
        }
    }

    private void parseIcmpV6Packet(ByteBuffer data) {
        this.mMostSpecificProtocolString = "ICMPv6";
        short icmpV6Type = getUnsignedByte(data);
        switch (icmpV6Type) {
            case 128:
                this.mTypeString = "Echo Request";
                break;
            case 129:
                this.mTypeString = "Echo Reply";
                break;
            case 130:
            case 131:
            case 132:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            default:
                this.mTypeString = "Type " + ((int) icmpV6Type);
                break;
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
            case 143:
                this.mTypeString = "MLDv2 report";
                break;
        }
    }

    private void parseEapolPacket(ByteBuffer data) {
        this.mMostSpecificProtocolString = "EAPOL";
        short eapolVersion = getUnsignedByte(data);
        if (eapolVersion < 1 || eapolVersion > 2) {
            Log.e(TAG, "Unrecognized EAPOL version " + ((int) eapolVersion));
            return;
        }
        short eapolType = getUnsignedByte(data);
        if (eapolType != 3) {
            Log.e(TAG, "Unrecognized EAPOL type " + ((int) eapolType));
            return;
        }
        data.position(data.position() + 2);
        short eapolKeyDescriptorType = getUnsignedByte(data);
        if (eapolKeyDescriptorType != 2) {
            Log.e(TAG, "Unrecognized key descriptor " + ((int) eapolKeyDescriptorType));
            return;
        }
        short wpaKeyInfo = data.getShort();
        if ((wpaKeyInfo & WPA_KEY_INFO_FLAG_PAIRWISE) == 0) {
            this.mTypeString = "Group Key";
        } else {
            this.mTypeString = "Pairwise Key";
        }
        if ((wpaKeyInfo & WPA_KEY_INFO_FLAG_MIC) == 0) {
            this.mTypeString += " message 1/4";
            return;
        }
        if ((wpaKeyInfo & WPA_KEY_INFO_FLAG_INSTALL) != 0) {
            this.mTypeString += " message 3/4";
            return;
        }
        data.position(data.position() + 2 + 8 + 32 + 16 + 8 + 8 + 16);
        int wpaKeyDataLen = getUnsignedShort(data);
        if (wpaKeyDataLen > 0) {
            this.mTypeString += " message 2/4";
        } else {
            this.mTypeString += " message 4/4";
        }
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

    private void parseManagementFrame(ByteBuffer data) {
        data.order(ByteOrder.LITTLE_ENDIAN);
        this.mMostSpecificProtocolString = "802.11 Mgmt";
        byte frameControlVersionTypeSubtype = data.get();
        byte ieee80211Version = parseIeee80211FrameCtrlVersion(frameControlVersionTypeSubtype);
        if (ieee80211Version != 0) {
            Log.e(TAG, "Unrecognized 802.11 version " + ((int) ieee80211Version));
        }
        byte ieee80211FrameType = parseIeee80211FrameCtrlType(frameControlVersionTypeSubtype);
        if (ieee80211FrameType != 0) {
            Log.e(TAG, "Unexpected frame type " + ((int) ieee80211FrameType));
            return;
        }
        byte frameControlFlags = data.get();
        data.position(data.position() + 2 + 6 + 6 + 6 + 2);
        if ((frameControlFlags & IEEE_80211_FRAME_CTRL_FLAG_ORDER) != 0) {
            data.position(data.position() + 4);
        }
        byte ieee80211FrameSubtype = parseIeee80211FrameCtrlSubtype(frameControlVersionTypeSubtype);
        switch (ieee80211FrameSubtype) {
            case 0:
                this.mTypeString = "Association Request";
                break;
            case 1:
                this.mTypeString = "Association Response";
                parseAssociationResponse(data);
                break;
            case 4:
                this.mTypeString = "Probe Request";
                break;
            case 5:
                this.mTypeString = "Probe Response";
                break;
            case 11:
                this.mTypeString = "Authentication";
                parseAuthenticationFrame(data);
                break;
            default:
                this.mTypeString = "Unexpected subtype " + ((int) ieee80211FrameSubtype);
                break;
        }
    }

    private void parseAssociationResponse(ByteBuffer data) {
        data.position(data.position() + 2);
        short resultCode = data.getShort();
        this.mResultString = String.format("%d: %s", Short.valueOf(resultCode), decodeIeee80211StatusCode(resultCode));
    }

    private void parseAuthenticationFrame(ByteBuffer data) {
        short algorithm = data.getShort();
        short sequenceNum = data.getShort();
        boolean hasResultCode = false;
        switch (algorithm) {
            case 0:
            case 1:
                if (sequenceNum == 2) {
                    hasResultCode = true;
                }
                break;
            case 2:
                if (sequenceNum == 2 || sequenceNum == 4) {
                    hasResultCode = true;
                }
                break;
            case 3:
                hasResultCode = true;
                break;
        }
        if (!hasResultCode) {
            return;
        }
        short resultCode = data.getShort();
        this.mResultString = String.format("%d: %s", Short.valueOf(resultCode), decodeIeee80211StatusCode(resultCode));
    }

    private String decodeIeee80211StatusCode(short statusCode) {
        switch (statusCode) {
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
            case Extension.TYPE_ENUM:
                return "Authentication sequence number of of sequence";
            case 15:
                return "Authentication challenge failure";
            case 16:
                return "Authentication timeout";
            case 17:
                return "Association denied; too many STAs";
            case 18:
                return "Association denied; must support BSSBasicRateSet";
            case CivicLocationElement.HOUSE_NUMBER:
                return "Association denied; must support short preamble";
            case CivicLocationElement.HOUSE_NUMBER_SUFFIX:
                return "Association denied; must support PBCC";
            case 21:
                return "Association denied; must support channel agility";
            case CivicLocationElement.ADDITIONAL_LOCATION:
                return "Association rejected; must support spectrum management";
            case 23:
                return "Association rejected; unacceptable power capability";
            case 24:
                return "Association rejected; unacceptable supported channels";
            case CivicLocationElement.BUILDING:
                return "Association denied; must support short slot time";
            case 26:
                return "Association denied; must support DSSS-OFDM";
            case CivicLocationElement.FLOOR:
                return "Association denied; must support HT";
            case CivicLocationElement.ROOM:
                return "R0 keyholder unreachable (802.11r)";
            case 29:
                return "Association denied; must support PCO transition time";
            case CivicLocationElement.POSTAL_COMMUNITY:
                return "Refused temporarily";
            case CivicLocationElement.PO_BOX:
                return "Robust management frame policy violation";
            case 32:
                return "Unspecified QoS failure";
            case CivicLocationElement.SEAT_DESK:
                return "Association denied; insufficient bandwidth for QoS";
            case CivicLocationElement.PRIMARY_ROAD:
                return "Association denied; poor channel";
            case 35:
                return "Association denied; must support QoS";
            case CivicLocationElement.BRANCH_ROAD:
                return "Reserved";
            case CivicLocationElement.SUB_BRANCH_ROAD:
                return "Declined";
            case 38:
                return "Invalid parameters";
            case CivicLocationElement.STREET_NAME_POST_MOD:
                return "TS cannot be honored; changes suggested";
            case 40:
                return "Invalid element";
            case EAP.EAP_SPEKE:
                return "Invalid group cipher";
            case EAP.EAP_MOBAC:
                return "Invalid pairwise cipher";
            case EAP.EAP_FAST:
                return "Invalid auth/key mgmt proto (AKMP)";
            case EAP.EAP_ZLXEAP:
                return "Unsupported RSNE version";
            case EAP.EAP_Link:
                return "Invalid RSNE capabilities";
            case EAP.EAP_PAX:
                return "Cipher suite rejected by policy";
            case EAP.EAP_PSK:
                return "TS cannot be honored now; try again later";
            case EAP.EAP_SAKE:
                return "Direct link rejected by policy";
            case EAP.EAP_IKEv2:
                return "Destination STA not in BSS";
            case EAP.EAP_AKAPrim:
                return "Destination STA not configured for QoS";
            case EAP.EAP_GPSK:
                return "Association denied; listen interval too large";
            case EAP.EAP_PWD:
                return "Invalid fast transition action frame count";
            case EAP.EAP_EKE:
                return "Invalid PMKID";
            case 54:
                return "Invalid MDE";
            case EAP.EAP_TEAP:
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
            case 66:
                return "Reserved";
            case 67:
                return "Rejected for SSP permissions";
            case 68:
                return "Authentication required";
            case 69:
            case 70:
            case 71:
                return "Reserved";
            case 72:
                return "Invalid RSNE contents";
            case 73:
                return "U-APSD coexistence unsupported";
            case 74:
                return "Requested U-APSD coex mode unsupported";
            case 75:
                return "Requested parameter unsupported with U-APSD coex";
            case 76:
                return "Auth rejected; anti-clogging token required";
            case 77:
                return "Auth rejected; offered group is not supported";
            case 78:
                return "Cannot find alternative TBTT";
            case 79:
                return "Transmission failure";
            case 80:
                return "Requested TCLAS not supported";
            case 81:
                return "TCLAS resources exhausted";
            case 82:
                return "Rejected with suggested BSS transition";
            case 83:
                return "Reserved";
            case 84:
            case WifiQualifiedNetworkSelector.RSSI_SCORE_OFFSET:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
                return "<unspecified>";
            case 92:
                return "Refused due to external reason";
            case 93:
                return "Refused; AP out of memory";
            case 94:
                return "Refused; emergency services not supported";
            case 95:
                return "GAS query response outstanding";
            case 96:
            case 97:
            case 98:
            case 99:
                return "Reserved";
            case 100:
                return "Failed; reservation conflict";
            case 101:
                return "Failed; exceeded MAF limit";
            case 102:
                return "Failed; exceeded MCCA track limit";
            default:
                return "Reserved";
        }
    }
}
