package com.android.server.hdmi;

import android.net.dhcp.DhcpPacket;
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

        KeycodeEntry(int androidKeycode, int cecKeycode, KeycodeEntry keycodeEntry) {
            this(androidKeycode, cecKeycode);
        }

        KeycodeEntry(int androidKeycode, int cecKeycode, boolean isRepeatable, KeycodeEntry keycodeEntry) {
            this(androidKeycode, cecKeycode, isRepeatable);
        }

        KeycodeEntry(int androidKeycode, int cecKeycode, boolean isRepeatable, byte[] cecParams, KeycodeEntry keycodeEntry) {
            this(androidKeycode, cecKeycode, isRepeatable, cecParams);
        }

        private KeycodeEntry(int androidKeycode, int cecKeycode, boolean isRepeatable, byte[] cecParams) {
            this.mAndroidKeycode = androidKeycode;
            this.mIsRepeatable = isRepeatable;
            this.mCecKeycodeAndParams = new byte[cecParams.length + 1];
            System.arraycopy(cecParams, 0, this.mCecKeycodeAndParams, 1, cecParams.length);
            this.mCecKeycodeAndParams[0] = (byte) (cecKeycode & DhcpPacket.MAX_OPTION_LEN);
        }

        private KeycodeEntry(int androidKeycode, int cecKeycode, boolean isRepeatable) {
            this(androidKeycode, cecKeycode, isRepeatable, EmptyArray.BYTE);
        }

        private KeycodeEntry(int androidKeycode, int cecKeycode, byte[] cecParams) {
            this(androidKeycode, cecKeycode, true, cecParams);
        }

        private KeycodeEntry(int androidKeycode, int cecKeycode) {
            this(androidKeycode, cecKeycode, true, EmptyArray.BYTE);
        }

        private byte[] toCecKeycodeAndParamIfMatched(int androidKeycode) {
            if (this.mAndroidKeycode == androidKeycode) {
                return this.mCecKeycodeAndParams;
            }
            return null;
        }

        private int toAndroidKeycodeIfMatched(byte[] cecKeycodeAndParams) {
            if (Arrays.equals(this.mCecKeycodeAndParams, cecKeycodeAndParams)) {
                return this.mAndroidKeycode;
            }
            return -1;
        }

        private Boolean isRepeatableIfMatched(int androidKeycode) {
            if (this.mAndroidKeycode == androidKeycode) {
                return Boolean.valueOf(this.mIsRepeatable);
            }
            return null;
        }
    }

    private static byte[] intToSingleByteArray(int value) {
        return new byte[]{(byte) (value & DhcpPacket.MAX_OPTION_LEN)};
    }

    static {
        int i = 86;
        int i2 = -1;
        KeycodeEntry keycodeEntry = null;
        KEYCODE_ENTRIES = new KeycodeEntry[]{new KeycodeEntry(23, 0, keycodeEntry), new KeycodeEntry(19, 1, keycodeEntry), new KeycodeEntry(20, 2, keycodeEntry), new KeycodeEntry(21, 3, keycodeEntry), new KeycodeEntry(22, 4, keycodeEntry), new KeycodeEntry(i2, 5, keycodeEntry), new KeycodeEntry(i2, 6, keycodeEntry), new KeycodeEntry(i2, 7, keycodeEntry), new KeycodeEntry(i2, 8, keycodeEntry), new KeycodeEntry(3, 9, keycodeEntry), new KeycodeEntry(176, 10, keycodeEntry), new KeycodeEntry(256, 11, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, 12, keycodeEntry), new KeycodeEntry(4, 13, keycodeEntry), new KeycodeEntry(111, 13, keycodeEntry), new KeycodeEntry(226, 16, keycodeEntry), new KeycodeEntry(257, 17, keycodeEntry), new KeycodeEntry(234, 29, keycodeEntry), new KeycodeEntry(227, 30, keycodeEntry), new KeycodeEntry(228, 31, keycodeEntry), new KeycodeEntry(7, 32, keycodeEntry), new KeycodeEntry(8, 33, keycodeEntry), new KeycodeEntry(9, 34, keycodeEntry), new KeycodeEntry(10, 35, keycodeEntry), new KeycodeEntry(11, 36, keycodeEntry), new KeycodeEntry(12, 37, keycodeEntry), new KeycodeEntry(13, 38, keycodeEntry), new KeycodeEntry(14, 39, keycodeEntry), new KeycodeEntry(15, 40, keycodeEntry), new KeycodeEntry(16, 41, keycodeEntry), new KeycodeEntry(56, 42, keycodeEntry), new KeycodeEntry(160, 43, keycodeEntry), new KeycodeEntry(28, 44, keycodeEntry), new KeycodeEntry(i2, 47, keycodeEntry), new KeycodeEntry(166, 48, keycodeEntry), new KeycodeEntry(167, 49, keycodeEntry), new KeycodeEntry(229, 50, keycodeEntry), new KeycodeEntry(i2, 51, keycodeEntry), new KeycodeEntry(UI_SOUND_PRESENTATION_BASS_NEUTRAL, 52, keycodeEntry), new KeycodeEntry(165, 53, keycodeEntry), new KeycodeEntry(i2, 54, keycodeEntry), new KeycodeEntry(92, 55, keycodeEntry), new KeycodeEntry(93, 56, keycodeEntry), new KeycodeEntry(26, 64, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(24, 65, keycodeEntry), new KeycodeEntry(25, 66, keycodeEntry), new KeycodeEntry(164, 67, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(126, 68, keycodeEntry), new KeycodeEntry(i, 69, keycodeEntry), new KeycodeEntry(127, 70, keycodeEntry), new KeycodeEntry(85, 70, keycodeEntry), new KeycodeEntry(130, 71, keycodeEntry), new KeycodeEntry(89, 72, keycodeEntry), new KeycodeEntry(90, 73, keycodeEntry), new KeycodeEntry(129, 74, keycodeEntry), new KeycodeEntry(87, 75, keycodeEntry), new KeycodeEntry(88, 76, keycodeEntry), new KeycodeEntry(i2, 77, keycodeEntry), new KeycodeEntry(i2, 78, keycodeEntry), new KeycodeEntry(i2, 79, keycodeEntry), new KeycodeEntry(i2, 80, keycodeEntry), new KeycodeEntry(175, 81, keycodeEntry), new KeycodeEntry(i2, 82, keycodeEntry), new KeycodeEntry(172, 83, keycodeEntry), new KeycodeEntry(258, 84, keycodeEntry), new KeycodeEntry(i2, 85, keycodeEntry), new KeycodeEntry(i2, i, keycodeEntry), new KeycodeEntry(235, i, 1 == true ? 1 : 0, intToSingleByteArray(16), keycodeEntry), new KeycodeEntry(DhcpPacket.MIN_PACKET_LENGTH_BOOTP, i, 1 == true ? 1 : 0, intToSingleByteArray(96), keycodeEntry), new KeycodeEntry(238, i, 1 == true ? 1 : 0, intToSingleByteArray(128), keycodeEntry), new KeycodeEntry(239, i, 1 == true ? 1 : 0, intToSingleByteArray(144), keycodeEntry), new KeycodeEntry(241, i, 1 == true ? 1 : 0, intToSingleByteArray(1), keycodeEntry), new KeycodeEntry(i2, 87, keycodeEntry), new KeycodeEntry(i2, 96, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, 97, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, 98, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, 99, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, 100, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, 101, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, CEC_KEYCODE_RESTORE_VOLUME_FUNCTION, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, CEC_KEYCODE_TUNE_FUNCTION, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, CEC_KEYCODE_SELECT_MEDIA_FUNCTION, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, CEC_KEYCODE_SELECT_AV_INPUT_FUNCTION, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, CEC_KEYCODE_SELECT_AUDIO_INPUT_FUNCTION, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, CEC_KEYCODE_POWER_TOGGLE_FUNCTION, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, CEC_KEYCODE_POWER_OFF_FUNCTION, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(i2, CEC_KEYCODE_POWER_ON_FUNCTION, (boolean) (0 == true ? 1 : 0), keycodeEntry), new KeycodeEntry(186, 113, keycodeEntry), new KeycodeEntry(183, 114, keycodeEntry), new KeycodeEntry(184, CEC_KEYCODE_F3_GREEN, keycodeEntry), new KeycodeEntry(185, CEC_KEYCODE_F4_YELLOW, keycodeEntry), new KeycodeEntry(135, CEC_KEYCODE_F5, keycodeEntry), new KeycodeEntry(230, CEC_KEYCODE_DATA, keycodeEntry)};
    }

    static byte[] androidKeyToCecKey(int keycode) {
        for (int i = 0; i < KEYCODE_ENTRIES.length; i++) {
            byte[] cecKeycodeAndParams = KEYCODE_ENTRIES[i].toCecKeycodeAndParamIfMatched(keycode);
            if (cecKeycodeAndParams != null) {
                return cecKeycodeAndParams;
            }
        }
        return null;
    }

    static int cecKeycodeAndParamsToAndroidKey(byte[] cecKeycodeAndParams) {
        for (int i = 0; i < KEYCODE_ENTRIES.length; i++) {
            int androidKey = KEYCODE_ENTRIES[i].toAndroidKeycodeIfMatched(cecKeycodeAndParams);
            if (androidKey != -1) {
                return androidKey;
            }
        }
        return -1;
    }

    static boolean isRepeatableKey(int androidKeycode) {
        for (int i = 0; i < KEYCODE_ENTRIES.length; i++) {
            Boolean isRepeatable = KEYCODE_ENTRIES[i].isRepeatableIfMatched(androidKeycode);
            if (isRepeatable != null) {
                return isRepeatable.booleanValue();
            }
        }
        return false;
    }

    static boolean isSupportedKeycode(int androidKeycode) {
        return androidKeyToCecKey(androidKeycode) != null;
    }

    public static int getMuteKey(boolean muting) {
        return 67;
    }
}
