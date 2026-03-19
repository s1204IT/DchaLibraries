package com.android.server.hdmi;

import android.net.util.NetworkConstants;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class HdmiCecMessageBuilder {
    private static final int OSD_NAME_MAX_LENGTH = 13;

    private HdmiCecMessageBuilder() {
    }

    static HdmiCecMessage of(int i, int i2, byte[] bArr) {
        return new HdmiCecMessage(i, i2, bArr[0], Arrays.copyOfRange(bArr, 1, bArr.length));
    }

    static HdmiCecMessage buildFeatureAbortCommand(int i, int i2, int i3, int i4) {
        return buildCommand(i, i2, 0, new byte[]{(byte) (i3 & 255), (byte) (i4 & 255)});
    }

    static HdmiCecMessage buildGivePhysicalAddress(int i, int i2) {
        return buildCommand(i, i2, 131);
    }

    static HdmiCecMessage buildGiveOsdNameCommand(int i, int i2) {
        return buildCommand(i, i2, 70);
    }

    static HdmiCecMessage buildGiveDeviceVendorIdCommand(int i, int i2) {
        return buildCommand(i, i2, 140);
    }

    static HdmiCecMessage buildSetMenuLanguageCommand(int i, String str) {
        if (str.length() != 3) {
            return null;
        }
        String lowerCase = str.toLowerCase();
        return buildCommand(i, 15, 50, new byte[]{(byte) (lowerCase.charAt(0) & 255), (byte) (lowerCase.charAt(1) & 255), (byte) (lowerCase.charAt(2) & 255)});
    }

    static HdmiCecMessage buildSetOsdNameCommand(int i, int i2, String str) {
        try {
            return buildCommand(i, i2, 71, str.substring(0, Math.min(str.length(), 13)).getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    static HdmiCecMessage buildReportPhysicalAddressCommand(int i, int i2, int i3) {
        return buildCommand(i, 15, 132, new byte[]{(byte) ((i2 >> 8) & 255), (byte) (i2 & 255), (byte) (i3 & 255)});
    }

    static HdmiCecMessage buildDeviceVendorIdCommand(int i, int i2) {
        return buildCommand(i, 15, NetworkConstants.ICMPV6_NEIGHBOR_SOLICITATION, new byte[]{(byte) ((i2 >> 16) & 255), (byte) ((i2 >> 8) & 255), (byte) (i2 & 255)});
    }

    static HdmiCecMessage buildCecVersion(int i, int i2, int i3) {
        return buildCommand(i, i2, 158, new byte[]{(byte) (i3 & 255)});
    }

    static HdmiCecMessage buildRequestArcInitiation(int i, int i2) {
        return buildCommand(i, i2, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_MINUS);
    }

    static HdmiCecMessage buildRequestArcTermination(int i, int i2) {
        return buildCommand(i, i2, 196);
    }

    static HdmiCecMessage buildReportArcInitiated(int i, int i2) {
        return buildCommand(i, i2, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS);
    }

    static HdmiCecMessage buildReportArcTerminated(int i, int i2) {
        return buildCommand(i, i2, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_NEUTRAL);
    }

    static HdmiCecMessage buildTextViewOn(int i, int i2) {
        return buildCommand(i, i2, 13);
    }

    static HdmiCecMessage buildActiveSource(int i, int i2) {
        return buildCommand(i, 15, 130, physicalAddressToParam(i2));
    }

    static HdmiCecMessage buildInactiveSource(int i, int i2) {
        return buildCommand(i, 0, 157, physicalAddressToParam(i2));
    }

    static HdmiCecMessage buildSetStreamPath(int i, int i2) {
        return buildCommand(i, 15, NetworkConstants.ICMPV6_ROUTER_ADVERTISEMENT, physicalAddressToParam(i2));
    }

    static HdmiCecMessage buildRoutingChange(int i, int i2, int i3) {
        return buildCommand(i, 15, 128, new byte[]{(byte) ((i2 >> 8) & 255), (byte) (i2 & 255), (byte) ((i3 >> 8) & 255), (byte) (i3 & 255)});
    }

    static HdmiCecMessage buildGiveDevicePowerStatus(int i, int i2) {
        return buildCommand(i, i2, 143);
    }

    static HdmiCecMessage buildReportPowerStatus(int i, int i2, int i3) {
        return buildCommand(i, i2, 144, new byte[]{(byte) (i3 & 255)});
    }

    static HdmiCecMessage buildReportMenuStatus(int i, int i2, int i3) {
        return buildCommand(i, i2, 142, new byte[]{(byte) (i3 & 255)});
    }

    static HdmiCecMessage buildSystemAudioModeRequest(int i, int i2, int i3, boolean z) {
        if (z) {
            return buildCommand(i, i2, 112, physicalAddressToParam(i3));
        }
        return buildCommand(i, i2, 112);
    }

    static HdmiCecMessage buildGiveAudioStatus(int i, int i2) {
        return buildCommand(i, i2, 113);
    }

    static HdmiCecMessage buildUserControlPressed(int i, int i2, int i3) {
        return buildUserControlPressed(i, i2, new byte[]{(byte) (i3 & 255)});
    }

    static HdmiCecMessage buildUserControlPressed(int i, int i2, byte[] bArr) {
        return buildCommand(i, i2, 68, bArr);
    }

    static HdmiCecMessage buildUserControlReleased(int i, int i2) {
        return buildCommand(i, i2, 69);
    }

    static HdmiCecMessage buildGiveSystemAudioModeStatus(int i, int i2) {
        return buildCommand(i, i2, 125);
    }

    public static HdmiCecMessage buildStandby(int i, int i2) {
        return buildCommand(i, i2, 54);
    }

    static HdmiCecMessage buildVendorCommand(int i, int i2, byte[] bArr) {
        return buildCommand(i, i2, 137, bArr);
    }

    static HdmiCecMessage buildVendorCommandWithId(int i, int i2, int i3, byte[] bArr) {
        byte[] bArr2 = new byte[bArr.length + 3];
        bArr2[0] = (byte) ((i3 >> 16) & 255);
        bArr2[1] = (byte) ((i3 >> 8) & 255);
        bArr2[2] = (byte) (i3 & 255);
        System.arraycopy(bArr, 0, bArr2, 3, bArr.length);
        return buildCommand(i, i2, 160, bArr2);
    }

    static HdmiCecMessage buildRecordOn(int i, int i2, byte[] bArr) {
        return buildCommand(i, i2, 9, bArr);
    }

    static HdmiCecMessage buildRecordOff(int i, int i2) {
        return buildCommand(i, i2, 11);
    }

    static HdmiCecMessage buildSetDigitalTimer(int i, int i2, byte[] bArr) {
        return buildCommand(i, i2, 151, bArr);
    }

    static HdmiCecMessage buildSetAnalogueTimer(int i, int i2, byte[] bArr) {
        return buildCommand(i, i2, 52, bArr);
    }

    static HdmiCecMessage buildSetExternalTimer(int i, int i2, byte[] bArr) {
        return buildCommand(i, i2, 162, bArr);
    }

    static HdmiCecMessage buildClearDigitalTimer(int i, int i2, byte[] bArr) {
        return buildCommand(i, i2, 153, bArr);
    }

    static HdmiCecMessage buildClearAnalogueTimer(int i, int i2, byte[] bArr) {
        return buildCommand(i, i2, 51, bArr);
    }

    static HdmiCecMessage buildClearExternalTimer(int i, int i2, byte[] bArr) {
        return buildCommand(i, i2, 161, bArr);
    }

    private static HdmiCecMessage buildCommand(int i, int i2, int i3) {
        return new HdmiCecMessage(i, i2, i3, HdmiCecMessage.EMPTY_PARAM);
    }

    private static HdmiCecMessage buildCommand(int i, int i2, int i3, byte[] bArr) {
        return new HdmiCecMessage(i, i2, i3, bArr);
    }

    private static byte[] physicalAddressToParam(int i) {
        return new byte[]{(byte) ((i >> 8) & 255), (byte) (i & 255)};
    }
}
