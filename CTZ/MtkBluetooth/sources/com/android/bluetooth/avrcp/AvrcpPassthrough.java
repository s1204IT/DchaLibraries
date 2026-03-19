package com.android.bluetooth.avrcp;

import android.net.util.NetworkConstants;
import android.support.v4.media.subtitle.Cea708CCParser;
import android.support.v4.view.MotionEventCompat;
import com.android.bluetooth.avrcpcontroller.AvrcpControllerService;

class AvrcpPassthrough {
    AvrcpPassthrough() {
    }

    public static int toKeyCode(int i) {
        switch (i) {
            case 1:
                return 19;
            case 2:
                return 20;
            case 3:
                return 21;
            case 4:
                return 22;
            case 5:
                return 270;
            case 6:
                return 271;
            case 7:
                return 268;
            case 8:
                return 269;
            default:
                switch (i) {
                    case 32:
                        return Cea708CCParser.Const.CODE_C1_SPA;
                    case 33:
                        return Cea708CCParser.Const.CODE_C1_SPC;
                    case 34:
                        return Cea708CCParser.Const.CODE_C1_SPL;
                    case 35:
                        return 147;
                    case 36:
                        return 148;
                    case 37:
                        return 149;
                    case 38:
                        return 150;
                    case 39:
                        return 151;
                    case 40:
                        return Cea708CCParser.Const.CODE_C1_DF0;
                    case MotionEventCompat.AXIS_GENERIC_10:
                        return Cea708CCParser.Const.CODE_C1_DF1;
                    case MotionEventCompat.AXIS_GENERIC_11:
                        return Cea708CCParser.Const.CODE_C1_DF6;
                    case MotionEventCompat.AXIS_GENERIC_12:
                        return 160;
                    case MotionEventCompat.AXIS_GENERIC_13:
                        return 28;
                    default:
                        switch (i) {
                            case 48:
                                return 166;
                            case 49:
                                return 167;
                            case 50:
                                return 229;
                            default:
                                switch (i) {
                                    case 52:
                                        return 178;
                                    case NetworkConstants.DNS_SERVER_PORT:
                                        return 165;
                                    case 54:
                                        return 259;
                                    case 55:
                                        return 92;
                                    case 56:
                                        return 93;
                                    default:
                                        switch (i) {
                                            case 64:
                                                return 26;
                                            case AvrcpControllerService.PASS_THRU_CMD_ID_VOL_UP:
                                                return 24;
                                            case AvrcpControllerService.PASS_THRU_CMD_ID_VOL_DOWN:
                                                return 25;
                                            case NetworkConstants.DHCP4_SERVER_PORT:
                                                return 91;
                                            case 68:
                                                return 126;
                                            case AvrcpControllerService.PASS_THRU_CMD_ID_STOP:
                                                return 86;
                                            case 70:
                                                return 127;
                                            case 71:
                                                return 130;
                                            case AvrcpControllerService.PASS_THRU_CMD_ID_REWIND:
                                                return 89;
                                            case AvrcpControllerService.PASS_THRU_CMD_ID_FF:
                                                return 90;
                                            case 74:
                                                return 129;
                                            case AvrcpControllerService.PASS_THRU_CMD_ID_FORWARD:
                                                return 87;
                                            case AvrcpControllerService.PASS_THRU_CMD_ID_BACKWARD:
                                                return 88;
                                            default:
                                                switch (i) {
                                                    case 113:
                                                        return Cea708CCParser.Const.CODE_C1_CW3;
                                                    case 114:
                                                        return 132;
                                                    case 115:
                                                        return 133;
                                                    case 116:
                                                        return 134;
                                                    case 117:
                                                        return 135;
                                                    default:
                                                        return 0;
                                                }
                                        }
                                }
                        }
                }
        }
    }
}
