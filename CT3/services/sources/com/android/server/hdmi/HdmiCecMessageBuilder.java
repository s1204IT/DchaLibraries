package com.android.server.hdmi;

import android.net.dhcp.DhcpPacket;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class HdmiCecMessageBuilder {
    private static final int OSD_NAME_MAX_LENGTH = 13;

    private HdmiCecMessageBuilder() {
    }

    static HdmiCecMessage of(int src, int dest, byte[] body) {
        byte opcode = body[0];
        byte[] params = Arrays.copyOfRange(body, 1, body.length);
        return new HdmiCecMessage(src, dest, opcode, params);
    }

    static HdmiCecMessage buildFeatureAbortCommand(int src, int dest, int originalOpcode, int reason) {
        byte[] params = {(byte) (originalOpcode & DhcpPacket.MAX_OPTION_LEN), (byte) (reason & DhcpPacket.MAX_OPTION_LEN)};
        return buildCommand(src, dest, 0, params);
    }

    static HdmiCecMessage buildGivePhysicalAddress(int src, int dest) {
        return buildCommand(src, dest, 131);
    }

    static HdmiCecMessage buildGiveOsdNameCommand(int src, int dest) {
        return buildCommand(src, dest, 70);
    }

    static HdmiCecMessage buildGiveDeviceVendorIdCommand(int src, int dest) {
        return buildCommand(src, dest, 140);
    }

    static HdmiCecMessage buildSetMenuLanguageCommand(int src, String language) {
        if (language.length() != 3) {
            return null;
        }
        String normalized = language.toLowerCase();
        byte[] params = {(byte) (normalized.charAt(0) & 255), (byte) (normalized.charAt(1) & 255), (byte) (normalized.charAt(2) & 255)};
        return buildCommand(src, 15, 50, params);
    }

    static HdmiCecMessage buildSetOsdNameCommand(int src, int dest, String name) {
        int length = Math.min(name.length(), 13);
        try {
            byte[] params = name.substring(0, length).getBytes("US-ASCII");
            return buildCommand(src, dest, 71, params);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    static HdmiCecMessage buildReportPhysicalAddressCommand(int src, int address, int deviceType) {
        byte[] params = {(byte) ((address >> 8) & DhcpPacket.MAX_OPTION_LEN), (byte) (address & DhcpPacket.MAX_OPTION_LEN), (byte) (deviceType & DhcpPacket.MAX_OPTION_LEN)};
        return buildCommand(src, 15, 132, params);
    }

    static HdmiCecMessage buildDeviceVendorIdCommand(int src, int vendorId) {
        byte[] params = {(byte) ((vendorId >> 16) & DhcpPacket.MAX_OPTION_LEN), (byte) ((vendorId >> 8) & DhcpPacket.MAX_OPTION_LEN), (byte) (vendorId & DhcpPacket.MAX_OPTION_LEN)};
        return buildCommand(src, 15, 135, params);
    }

    static HdmiCecMessage buildCecVersion(int src, int dest, int version) {
        byte[] params = {(byte) (version & DhcpPacket.MAX_OPTION_LEN)};
        return buildCommand(src, dest, 158, params);
    }

    static HdmiCecMessage buildRequestArcInitiation(int src, int dest) {
        return buildCommand(src, dest, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_MINUS);
    }

    static HdmiCecMessage buildRequestArcTermination(int src, int dest) {
        return buildCommand(src, dest, 196);
    }

    static HdmiCecMessage buildReportArcInitiated(int src, int dest) {
        return buildCommand(src, dest, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS);
    }

    static HdmiCecMessage buildReportArcTerminated(int src, int dest) {
        return buildCommand(src, dest, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_NEUTRAL);
    }

    static HdmiCecMessage buildTextViewOn(int src, int dest) {
        return buildCommand(src, dest, 13);
    }

    static HdmiCecMessage buildActiveSource(int src, int physicalAddress) {
        return buildCommand(src, 15, 130, physicalAddressToParam(physicalAddress));
    }

    static HdmiCecMessage buildInactiveSource(int src, int physicalAddress) {
        return buildCommand(src, 0, 157, physicalAddressToParam(physicalAddress));
    }

    static HdmiCecMessage buildSetStreamPath(int src, int streamPath) {
        return buildCommand(src, 15, 134, physicalAddressToParam(streamPath));
    }

    static HdmiCecMessage buildRoutingChange(int src, int oldPath, int newPath) {
        byte[] param = {(byte) ((oldPath >> 8) & DhcpPacket.MAX_OPTION_LEN), (byte) (oldPath & DhcpPacket.MAX_OPTION_LEN), (byte) ((newPath >> 8) & DhcpPacket.MAX_OPTION_LEN), (byte) (newPath & DhcpPacket.MAX_OPTION_LEN)};
        return buildCommand(src, 15, 128, param);
    }

    static HdmiCecMessage buildGiveDevicePowerStatus(int src, int dest) {
        return buildCommand(src, dest, 143);
    }

    static HdmiCecMessage buildReportPowerStatus(int src, int dest, int powerStatus) {
        byte[] param = {(byte) (powerStatus & DhcpPacket.MAX_OPTION_LEN)};
        return buildCommand(src, dest, 144, param);
    }

    static HdmiCecMessage buildReportMenuStatus(int src, int dest, int menuStatus) {
        byte[] param = {(byte) (menuStatus & DhcpPacket.MAX_OPTION_LEN)};
        return buildCommand(src, dest, 142, param);
    }

    static HdmiCecMessage buildSystemAudioModeRequest(int src, int avr, int avrPhysicalAddress, boolean enableSystemAudio) {
        if (enableSystemAudio) {
            return buildCommand(src, avr, 112, physicalAddressToParam(avrPhysicalAddress));
        }
        return buildCommand(src, avr, 112);
    }

    static HdmiCecMessage buildGiveAudioStatus(int src, int dest) {
        return buildCommand(src, dest, 113);
    }

    static HdmiCecMessage buildUserControlPressed(int src, int dest, int uiCommand) {
        return buildUserControlPressed(src, dest, new byte[]{(byte) (uiCommand & DhcpPacket.MAX_OPTION_LEN)});
    }

    static HdmiCecMessage buildUserControlPressed(int src, int dest, byte[] commandParam) {
        return buildCommand(src, dest, 68, commandParam);
    }

    static HdmiCecMessage buildUserControlReleased(int src, int dest) {
        return buildCommand(src, dest, 69);
    }

    static HdmiCecMessage buildGiveSystemAudioModeStatus(int src, int dest) {
        return buildCommand(src, dest, 125);
    }

    public static HdmiCecMessage buildStandby(int src, int dest) {
        return buildCommand(src, dest, 54);
    }

    static HdmiCecMessage buildVendorCommand(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 137, params);
    }

    static HdmiCecMessage buildVendorCommandWithId(int src, int dest, int vendorId, byte[] operands) {
        byte[] params = new byte[operands.length + 3];
        params[0] = (byte) ((vendorId >> 16) & DhcpPacket.MAX_OPTION_LEN);
        params[1] = (byte) ((vendorId >> 8) & DhcpPacket.MAX_OPTION_LEN);
        params[2] = (byte) (vendorId & DhcpPacket.MAX_OPTION_LEN);
        System.arraycopy(operands, 0, params, 3, operands.length);
        return buildCommand(src, dest, 160, params);
    }

    static HdmiCecMessage buildRecordOn(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 9, params);
    }

    static HdmiCecMessage buildRecordOff(int src, int dest) {
        return buildCommand(src, dest, 11);
    }

    static HdmiCecMessage buildSetDigitalTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 151, params);
    }

    static HdmiCecMessage buildSetAnalogueTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 52, params);
    }

    static HdmiCecMessage buildSetExternalTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 162, params);
    }

    static HdmiCecMessage buildClearDigitalTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 153, params);
    }

    static HdmiCecMessage buildClearAnalogueTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 51, params);
    }

    static HdmiCecMessage buildClearExternalTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 161, params);
    }

    private static HdmiCecMessage buildCommand(int src, int dest, int opcode) {
        return new HdmiCecMessage(src, dest, opcode, HdmiCecMessage.EMPTY_PARAM);
    }

    private static HdmiCecMessage buildCommand(int src, int dest, int opcode, byte[] params) {
        return new HdmiCecMessage(src, dest, opcode, params);
    }

    private static byte[] physicalAddressToParam(int physicalAddress) {
        return new byte[]{(byte) ((physicalAddress >> 8) & DhcpPacket.MAX_OPTION_LEN), (byte) (physicalAddress & DhcpPacket.MAX_OPTION_LEN)};
    }
}
