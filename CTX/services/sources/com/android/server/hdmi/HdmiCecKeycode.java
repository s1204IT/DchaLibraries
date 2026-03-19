package com.android.server.hdmi;

import android.net.dhcp.DhcpPacket;
import android.net.util.NetworkConstants;
import com.android.server.NetworkManagementService;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.util.Arrays;
import libcore.util.EmptyArray;

final class HdmiCecKeycode {
    public static final int CEC_KEYCODE_ANGLE = 80;
    public static final int CEC_KEYCODE_BACKWARD = 76;
    public static final int CEC_KEYCODE_CHANNEL_DOWN = 49;
    public static final int CEC_KEYCODE_CHANNEL_UP = 48;
    public static final int CEC_KEYCODE_CLEAR = 44;
    public static final int CEC_KEYCODE_CONTENTS_MENU = 11;
    public static final int CEC_KEYCODE_DATA = 118;
    public static final int CEC_KEYCODE_DISPLAY_INFORMATION = 53;
    public static final int CEC_KEYCODE_DOT = 42;
    public static final int CEC_KEYCODE_DOWN = 2;
    public static final int CEC_KEYCODE_EJECT = 74;
    public static final int CEC_KEYCODE_ELECTRONIC_PROGRAM_GUIDE = 83;
    public static final int CEC_KEYCODE_ENTER = 43;
    public static final int CEC_KEYCODE_EXIT = 13;
    public static final int CEC_KEYCODE_F1_BLUE = 113;
    public static final int CEC_KEYCODE_F2_RED = 114;
    public static final int CEC_KEYCODE_F3_GREEN = 115;
    public static final int CEC_KEYCODE_F4_YELLOW = 116;
    public static final int CEC_KEYCODE_F5 = 117;
    public static final int CEC_KEYCODE_FAST_FORWARD = 73;
    public static final int CEC_KEYCODE_FAVORITE_MENU = 12;
    public static final int CEC_KEYCODE_FORWARD = 75;
    public static final int CEC_KEYCODE_HELP = 54;
    public static final int CEC_KEYCODE_INITIAL_CONFIGURATION = 85;
    public static final int CEC_KEYCODE_INPUT_SELECT = 52;
    public static final int CEC_KEYCODE_LEFT = 3;
    public static final int CEC_KEYCODE_LEFT_DOWN = 8;
    public static final int CEC_KEYCODE_LEFT_UP = 7;
    public static final int CEC_KEYCODE_MEDIA_CONTEXT_SENSITIVE_MENU = 17;
    public static final int CEC_KEYCODE_MEDIA_TOP_MENU = 16;
    public static final int CEC_KEYCODE_MUTE = 67;
    public static final int CEC_KEYCODE_MUTE_FUNCTION = 101;
    public static final int CEC_KEYCODE_NEXT_FAVORITE = 47;
    public static final int CEC_KEYCODE_NUMBERS_1 = 33;
    public static final int CEC_KEYCODE_NUMBERS_2 = 34;
    public static final int CEC_KEYCODE_NUMBERS_3 = 35;
    public static final int CEC_KEYCODE_NUMBERS_4 = 36;
    public static final int CEC_KEYCODE_NUMBERS_5 = 37;
    public static final int CEC_KEYCODE_NUMBERS_6 = 38;
    public static final int CEC_KEYCODE_NUMBERS_7 = 39;
    public static final int CEC_KEYCODE_NUMBERS_8 = 40;
    public static final int CEC_KEYCODE_NUMBERS_9 = 41;
    public static final int CEC_KEYCODE_NUMBER_0_OR_NUMBER_10 = 32;
    public static final int CEC_KEYCODE_NUMBER_11 = 30;
    public static final int CEC_KEYCODE_NUMBER_12 = 31;
    public static final int CEC_KEYCODE_NUMBER_ENTRY_MODE = 29;
    public static final int CEC_KEYCODE_PAGE_DOWN = 56;
    public static final int CEC_KEYCODE_PAGE_UP = 55;
    public static final int CEC_KEYCODE_PAUSE = 70;
    public static final int CEC_KEYCODE_PAUSE_PLAY_FUNCTION = 97;
    public static final int CEC_KEYCODE_PAUSE_RECORD = 78;
    public static final int CEC_KEYCODE_PAUSE_RECORD_FUNCTION = 99;
    public static final int CEC_KEYCODE_PLAY = 68;
    public static final int CEC_KEYCODE_PLAY_FUNCTION = 96;
    public static final int CEC_KEYCODE_POWER = 64;
    public static final int CEC_KEYCODE_POWER_OFF_FUNCTION = 108;
    public static final int CEC_KEYCODE_POWER_ON_FUNCTION = 109;
    public static final int CEC_KEYCODE_POWER_TOGGLE_FUNCTION = 107;
    public static final int CEC_KEYCODE_PREVIOUS_CHANNEL = 50;
    public static final int CEC_KEYCODE_RECORD = 71;
    public static final int CEC_KEYCODE_RECORD_FUNCTION = 98;
    public static final int CEC_KEYCODE_RESERVED = 79;
    public static final int CEC_KEYCODE_RESTORE_VOLUME_FUNCTION = 102;
    public static final int CEC_KEYCODE_REWIND = 72;
    public static final int CEC_KEYCODE_RIGHT = 4;
    public static final int CEC_KEYCODE_RIGHT_DOWN = 6;
    public static final int CEC_KEYCODE_RIGHT_UP = 5;
    public static final int CEC_KEYCODE_ROOT_MENU = 9;
    public static final int CEC_KEYCODE_SELECT = 0;
    public static final int CEC_KEYCODE_SELECT_AUDIO_INPUT_FUNCTION = 106;
    public static final int CEC_KEYCODE_SELECT_AV_INPUT_FUNCTION = 105;
    public static final int CEC_KEYCODE_SELECT_BROADCAST_TYPE = 86;
    public static final int CEC_KEYCODE_SELECT_MEDIA_FUNCTION = 104;
    public static final int CEC_KEYCODE_SELECT_SOUND_PRESENTATION = 87;
    public static final int CEC_KEYCODE_SETUP_MENU = 10;
    public static final int CEC_KEYCODE_SOUND_SELECT = 51;
    public static final int CEC_KEYCODE_STOP = 69;
    public static final int CEC_KEYCODE_STOP_FUNCTION = 100;
    public static final int CEC_KEYCODE_STOP_RECORD = 77;
    public static final int CEC_KEYCODE_SUB_PICTURE = 81;
    public static final int CEC_KEYCODE_TIMER_PROGRAMMING = 84;
    public static final int CEC_KEYCODE_TUNE_FUNCTION = 103;
    public static final int CEC_KEYCODE_UP = 1;
    public static final int CEC_KEYCODE_VIDEO_ON_DEMAND = 82;
    public static final int CEC_KEYCODE_VOLUME_DOWN = 66;
    public static final int CEC_KEYCODE_VOLUME_UP = 65;
    private static final KeycodeEntry[] KEYCODE_ENTRIES;
    public static final int NO_PARAM = -1;
    public static final int UI_BROADCAST_ANALOGUE = 16;
    public static final int UI_BROADCAST_ANALOGUE_CABLE = 48;
    public static final int UI_BROADCAST_ANALOGUE_SATELLITE = 64;
    public static final int UI_BROADCAST_ANALOGUE_TERRESTRIAL = 32;
    public static final int UI_BROADCAST_DIGITAL = 80;
    public static final int UI_BROADCAST_DIGITAL_CABLE = 112;
    public static final int UI_BROADCAST_DIGITAL_COMMNICATIONS_SATELLITE = 144;
    public static final int UI_BROADCAST_DIGITAL_COMMNICATIONS_SATELLITE_2 = 145;
    public static final int UI_BROADCAST_DIGITAL_SATELLITE = 128;
    public static final int UI_BROADCAST_DIGITAL_TERRESTRIAL = 96;
    public static final int UI_BROADCAST_IP = 160;
    public static final int UI_BROADCAST_TOGGLE_ALL = 0;
    public static final int UI_BROADCAST_TOGGLE_ANALOGUE_DIGITAL = 1;
    public static final int UI_SOUND_PRESENTATION_BASS_NEUTRAL = 178;
    public static final int UI_SOUND_PRESENTATION_BASS_STEP_MINUS = 179;
    public static final int UI_SOUND_PRESENTATION_BASS_STEP_PLUS = 177;
    public static final int UI_SOUND_PRESENTATION_SELECT_AUDIO_AUTO_EQUALIZER = 160;
    public static final int UI_SOUND_PRESENTATION_SELECT_AUDIO_AUTO_REVERBERATION = 144;
    public static final int UI_SOUND_PRESENTATION_SELECT_AUDIO_DOWN_MIX = 128;
    public static final int UI_SOUND_PRESENTATION_SOUND_MIX_DUAL_MONO = 32;
    public static final int UI_SOUND_PRESENTATION_SOUND_MIX_KARAOKE = 48;
    public static final int UI_SOUND_PRESENTATION_TREBLE_NEUTRAL = 194;
    public static final int UI_SOUND_PRESENTATION_TREBLE_STEP_MINUS = 195;
    public static final int UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS = 193;
    public static final int UNSUPPORTED_KEYCODE = -1;

    private HdmiCecKeycode() {
    }

    private static class KeycodeEntry {
        private final int mAndroidKeycode;
        private final byte[] mCecKeycodeAndParams;
        private final boolean mIsRepeatable;

        private KeycodeEntry(int i, int i2, boolean z, byte[] bArr) {
            this.mAndroidKeycode = i;
            this.mIsRepeatable = z;
            this.mCecKeycodeAndParams = new byte[bArr.length + 1];
            System.arraycopy(bArr, 0, this.mCecKeycodeAndParams, 1, bArr.length);
            this.mCecKeycodeAndParams[0] = (byte) (i2 & 255);
        }

        private KeycodeEntry(int i, int i2, boolean z) {
            this(i, i2, z, EmptyArray.BYTE);
        }

        private KeycodeEntry(int i, int i2, byte[] bArr) {
            this(i, i2, true, bArr);
        }

        private KeycodeEntry(int i, int i2) {
            this(i, i2, true, EmptyArray.BYTE);
        }

        private byte[] toCecKeycodeAndParamIfMatched(int i) {
            if (this.mAndroidKeycode == i) {
                return this.mCecKeycodeAndParams;
            }
            return null;
        }

        private int toAndroidKeycodeIfMatched(byte[] bArr) {
            if (Arrays.equals(this.mCecKeycodeAndParams, bArr)) {
                return this.mAndroidKeycode;
            }
            return -1;
        }

        private Boolean isRepeatableIfMatched(int i) {
            if (this.mAndroidKeycode == i) {
                return Boolean.valueOf(this.mIsRepeatable);
            }
            return null;
        }
    }

    private static byte[] intToSingleByteArray(int i) {
        return new byte[]{(byte) (i & 255)};
    }

    static {
        int i = 3;
        int i2 = 4;
        int i3 = -1;
        int i4 = 7;
        int i5 = 8;
        int i6 = 9;
        int i7 = 10;
        int i8 = 11;
        int i9 = 12;
        int i10 = 13;
        int i11 = 16;
        int i12 = 56;
        int i13 = 70;
        int i14 = 86;
        boolean z = true;
        KEYCODE_ENTRIES = new KeycodeEntry[]{new KeycodeEntry(23, 0), new KeycodeEntry(19, 1), new KeycodeEntry(20, 2), new KeycodeEntry(21, i), new KeycodeEntry(22, i2), new KeycodeEntry(i3, 5), new KeycodeEntry(i3, 6), new KeycodeEntry(i3, i4), new KeycodeEntry(i3, i5), new KeycodeEntry(i, i6), new KeycodeEntry(176, i7), new KeycodeEntry(256, i8, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, i9), new KeycodeEntry(i2, i10), new KeycodeEntry(NetworkManagementService.NetdResponseCode.TetherInterfaceListResult, i10), new KeycodeEntry(226, i11), new KeycodeEntry(UsbTerminalTypes.TERMINAL_USB_STREAMING, 17), new KeycodeEntry(234, 29), new KeycodeEntry(227, 30), new KeycodeEntry(228, 31), new KeycodeEntry(i4, 32), new KeycodeEntry(i5, 33), new KeycodeEntry(i6, 34), new KeycodeEntry(i7, 35), new KeycodeEntry(i8, 36), new KeycodeEntry(i9, 37), new KeycodeEntry(i10, 38), new KeycodeEntry(14, 39), new KeycodeEntry(15, 40), new KeycodeEntry(i11, 41), new KeycodeEntry(i12, 42), new KeycodeEntry(160, 43), new KeycodeEntry(28, 44), new KeycodeEntry(i3, 47), new KeycodeEntry(166, 48), new KeycodeEntry(167, 49), new KeycodeEntry(229, 50), new KeycodeEntry(i3, 51), new KeycodeEntry(UI_SOUND_PRESENTATION_BASS_NEUTRAL, 52), new KeycodeEntry(165, 53), new KeycodeEntry(i3, 54), new KeycodeEntry(92, 55), new KeycodeEntry(93, i12), new KeycodeEntry(26, 64, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(24, 65), new KeycodeEntry(25, 66), new KeycodeEntry(164, 67, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(126, 68), new KeycodeEntry(86, 69), new KeycodeEntry(127, i13), new KeycodeEntry(85, i13), new KeycodeEntry(130, 71), new KeycodeEntry(89, 72), new KeycodeEntry(90, 73), new KeycodeEntry(NetworkConstants.ICMPV6_ECHO_REPLY_TYPE, 74), new KeycodeEntry(87, 75), new KeycodeEntry(88, 76), new KeycodeEntry(i3, 77), new KeycodeEntry(i3, 78), new KeycodeEntry(i3, 79), new KeycodeEntry(i3, 80), new KeycodeEntry(175, 81), new KeycodeEntry(i3, 82), new KeycodeEntry(172, 83), new KeycodeEntry(258, 84), new KeycodeEntry(i3, 85), new KeycodeEntry(i3, 86), new KeycodeEntry(235, i14, z, intToSingleByteArray(16)), new KeycodeEntry(DhcpPacket.MIN_PACKET_LENGTH_BOOTP, i14, z, intToSingleByteArray(96)), new KeycodeEntry(238, i14, z, intToSingleByteArray(128)), new KeycodeEntry(UsbDescriptor.CLASSID_MISC, i14, z, intToSingleByteArray(144)), new KeycodeEntry(241, i14, z, intToSingleByteArray(1)), new KeycodeEntry(i3, 87), new KeycodeEntry(i3, 96, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, 97, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, 98, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, 99, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, 100, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, 101, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, CEC_KEYCODE_RESTORE_VOLUME_FUNCTION, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, CEC_KEYCODE_TUNE_FUNCTION, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, CEC_KEYCODE_SELECT_MEDIA_FUNCTION, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, CEC_KEYCODE_SELECT_AV_INPUT_FUNCTION, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, CEC_KEYCODE_SELECT_AUDIO_INPUT_FUNCTION, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, CEC_KEYCODE_POWER_TOGGLE_FUNCTION, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, CEC_KEYCODE_POWER_OFF_FUNCTION, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(i3, CEC_KEYCODE_POWER_ON_FUNCTION, (boolean) (0 == true ? 1 : 0)), new KeycodeEntry(186, 113), new KeycodeEntry(183, 114), new KeycodeEntry(184, CEC_KEYCODE_F3_GREEN), new KeycodeEntry(185, CEC_KEYCODE_F4_YELLOW), new KeycodeEntry(NetworkConstants.ICMPV6_NEIGHBOR_SOLICITATION, CEC_KEYCODE_F5), new KeycodeEntry(230, CEC_KEYCODE_DATA)};
    }

    static byte[] androidKeyToCecKey(int i) {
        for (int i2 = 0; i2 < KEYCODE_ENTRIES.length; i2++) {
            byte[] cecKeycodeAndParamIfMatched = KEYCODE_ENTRIES[i2].toCecKeycodeAndParamIfMatched(i);
            if (cecKeycodeAndParamIfMatched != null) {
                return cecKeycodeAndParamIfMatched;
            }
        }
        return null;
    }

    static int cecKeycodeAndParamsToAndroidKey(byte[] bArr) {
        for (int i = 0; i < KEYCODE_ENTRIES.length; i++) {
            int androidKeycodeIfMatched = KEYCODE_ENTRIES[i].toAndroidKeycodeIfMatched(bArr);
            if (androidKeycodeIfMatched != -1) {
                return androidKeycodeIfMatched;
            }
        }
        return -1;
    }

    static boolean isRepeatableKey(int i) {
        for (int i2 = 0; i2 < KEYCODE_ENTRIES.length; i2++) {
            Boolean boolIsRepeatableIfMatched = KEYCODE_ENTRIES[i2].isRepeatableIfMatched(i);
            if (boolIsRepeatableIfMatched != null) {
                return boolIsRepeatableIfMatched.booleanValue();
            }
        }
        return false;
    }

    static boolean isSupportedKeycode(int i) {
        return androidKeyToCecKey(i) != null;
    }

    public static int getMuteKey(boolean z) {
        return 67;
    }
}
