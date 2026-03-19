package com.android.server.hdmi;

import android.net.util.NetworkConstants;
import android.util.SparseArray;

public final class HdmiCecMessageValidator {
    private static final int DEST_ALL = 3;
    private static final int DEST_BROADCAST = 2;
    private static final int DEST_DIRECT = 1;
    static final int ERROR_DESTINATION = 2;
    static final int ERROR_PARAMETER = 3;
    static final int ERROR_PARAMETER_SHORT = 4;
    static final int ERROR_SOURCE = 1;
    static final int OK = 0;
    private static final int SRC_UNREGISTERED = 4;
    private static final String TAG = "HdmiCecMessageValidator";
    private final HdmiControlService mService;
    final SparseArray<ValidationInfo> mValidationInfo = new SparseArray<>();

    interface ParameterValidator {
        int isValid(byte[] bArr);
    }

    private static class ValidationInfo {
        public final int addressType;
        public final ParameterValidator parameterValidator;

        public ValidationInfo(ParameterValidator parameterValidator, int i) {
            this.parameterValidator = parameterValidator;
            this.addressType = i;
        }
    }

    public HdmiCecMessageValidator(HdmiControlService hdmiControlService) {
        this.mService = hdmiControlService;
        PhysicalAddressValidator physicalAddressValidator = new PhysicalAddressValidator();
        addValidationInfo(130, physicalAddressValidator, 6);
        addValidationInfo(157, physicalAddressValidator, 1);
        addValidationInfo(132, new ReportPhysicalAddressValidator(), 6);
        addValidationInfo(128, new RoutingChangeValidator(), 6);
        addValidationInfo(NetworkConstants.ICMPV6_ECHO_REPLY_TYPE, physicalAddressValidator, 6);
        addValidationInfo(NetworkConstants.ICMPV6_ROUTER_ADVERTISEMENT, physicalAddressValidator, 2);
        addValidationInfo(112, new SystemAudioModeRequestValidator(), 1);
        FixedLengthValidator fixedLengthValidator = new FixedLengthValidator(0);
        addValidationInfo(255, fixedLengthValidator, 1);
        addValidationInfo(159, fixedLengthValidator, 1);
        addValidationInfo(HdmiCecKeycode.UI_BROADCAST_DIGITAL_COMMNICATIONS_SATELLITE_2, fixedLengthValidator, 5);
        addValidationInfo(113, fixedLengthValidator, 1);
        addValidationInfo(143, fixedLengthValidator, 1);
        addValidationInfo(140, fixedLengthValidator, 5);
        addValidationInfo(70, fixedLengthValidator, 1);
        addValidationInfo(131, fixedLengthValidator, 5);
        addValidationInfo(125, fixedLengthValidator, 1);
        addValidationInfo(4, fixedLengthValidator, 1);
        addValidationInfo(192, fixedLengthValidator, 1);
        addValidationInfo(11, fixedLengthValidator, 1);
        addValidationInfo(15, fixedLengthValidator, 1);
        addValidationInfo(HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS, fixedLengthValidator, 1);
        addValidationInfo(HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_NEUTRAL, fixedLengthValidator, 1);
        addValidationInfo(HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_MINUS, fixedLengthValidator, 1);
        addValidationInfo(196, fixedLengthValidator, 1);
        addValidationInfo(NetworkConstants.ICMPV6_ROUTER_SOLICITATION, fixedLengthValidator, 6);
        addValidationInfo(54, fixedLengthValidator, 7);
        addValidationInfo(197, fixedLengthValidator, 1);
        addValidationInfo(13, fixedLengthValidator, 1);
        addValidationInfo(6, fixedLengthValidator, 1);
        addValidationInfo(5, fixedLengthValidator, 1);
        addValidationInfo(69, fixedLengthValidator, 1);
        addValidationInfo(139, fixedLengthValidator, 3);
        FixedLengthValidator fixedLengthValidator2 = new FixedLengthValidator(1);
        addValidationInfo(9, new VariableLengthValidator(1, 8), 1);
        addValidationInfo(10, fixedLengthValidator2, 1);
        addValidationInfo(158, fixedLengthValidator2, 1);
        addValidationInfo(50, new FixedLengthValidator(3), 2);
        VariableLengthValidator variableLengthValidator = new VariableLengthValidator(0, 14);
        addValidationInfo(NetworkConstants.ICMPV6_NEIGHBOR_SOLICITATION, new FixedLengthValidator(3), 2);
        addValidationInfo(137, new VariableLengthValidator(1, 14), 5);
        addValidationInfo(160, new VariableLengthValidator(4, 14), 7);
        addValidationInfo(138, variableLengthValidator, 7);
        addValidationInfo(100, variableLengthValidator, 1);
        addValidationInfo(71, variableLengthValidator, 1);
        addValidationInfo(141, fixedLengthValidator2, 1);
        addValidationInfo(142, fixedLengthValidator2, 1);
        addValidationInfo(68, new VariableLengthValidator(1, 2), 1);
        addValidationInfo(144, fixedLengthValidator2, 1);
        addValidationInfo(0, new FixedLengthValidator(2), 1);
        addValidationInfo(122, fixedLengthValidator2, 1);
        addValidationInfo(163, new FixedLengthValidator(3), 1);
        addValidationInfo(164, fixedLengthValidator2, 1);
        addValidationInfo(114, fixedLengthValidator2, 3);
        addValidationInfo(126, fixedLengthValidator2, 1);
        addValidationInfo(154, fixedLengthValidator2, 1);
        addValidationInfo(248, variableLengthValidator, 6);
    }

    private void addValidationInfo(int i, ParameterValidator parameterValidator, int i2) {
        this.mValidationInfo.append(i, new ValidationInfo(parameterValidator, i2));
    }

    int isValid(HdmiCecMessage hdmiCecMessage) {
        ValidationInfo validationInfo = this.mValidationInfo.get(hdmiCecMessage.getOpcode());
        if (validationInfo == null) {
            HdmiLogger.warning("No validation information for the message: " + hdmiCecMessage, new Object[0]);
            return 0;
        }
        if (hdmiCecMessage.getSource() == 15 && (validationInfo.addressType & 4) == 0) {
            HdmiLogger.warning("Unexpected source: " + hdmiCecMessage, new Object[0]);
            return 1;
        }
        if (hdmiCecMessage.getDestination() == 15) {
            if ((validationInfo.addressType & 2) == 0) {
                HdmiLogger.warning("Unexpected broadcast message: " + hdmiCecMessage, new Object[0]);
                return 2;
            }
        } else if ((validationInfo.addressType & 1) == 0) {
            HdmiLogger.warning("Unexpected direct message: " + hdmiCecMessage, new Object[0]);
            return 2;
        }
        int iIsValid = validationInfo.parameterValidator.isValid(hdmiCecMessage.getParams());
        if (iIsValid == 0) {
            return 0;
        }
        HdmiLogger.warning("Unexpected parameters: " + hdmiCecMessage, new Object[0]);
        return iIsValid;
    }

    private static class FixedLengthValidator implements ParameterValidator {
        private final int mLength;

        public FixedLengthValidator(int i) {
            this.mLength = i;
        }

        @Override
        public int isValid(byte[] bArr) {
            return bArr.length < this.mLength ? 4 : 0;
        }
    }

    private static class VariableLengthValidator implements ParameterValidator {
        private final int mMaxLength;
        private final int mMinLength;

        public VariableLengthValidator(int i, int i2) {
            this.mMinLength = i;
            this.mMaxLength = i2;
        }

        @Override
        public int isValid(byte[] bArr) {
            return bArr.length < this.mMinLength ? 4 : 0;
        }
    }

    private boolean isValidPhysicalAddress(byte[] bArr, int i) {
        if (!this.mService.isTvDevice()) {
            return true;
        }
        int iTwoBytesToInt = HdmiUtils.twoBytesToInt(bArr, i);
        return (iTwoBytesToInt != 65535 && iTwoBytesToInt == this.mService.getPhysicalAddress()) || this.mService.pathToPortId(iTwoBytesToInt) != -1;
    }

    static boolean isValidType(int i) {
        return i >= 0 && i <= 7 && i != 2;
    }

    private static int toErrorCode(boolean z) {
        return z ? 0 : 3;
    }

    private class PhysicalAddressValidator implements ParameterValidator {
        private PhysicalAddressValidator() {
        }

        @Override
        public int isValid(byte[] bArr) {
            if (bArr.length >= 2) {
                return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.this.isValidPhysicalAddress(bArr, 0));
            }
            return 4;
        }
    }

    private class SystemAudioModeRequestValidator extends PhysicalAddressValidator {
        private SystemAudioModeRequestValidator() {
            super();
        }

        @Override
        public int isValid(byte[] bArr) {
            if (bArr.length == 0) {
                return 0;
            }
            return super.isValid(bArr);
        }
    }

    private class ReportPhysicalAddressValidator implements ParameterValidator {
        private ReportPhysicalAddressValidator() {
        }

        @Override
        public int isValid(byte[] bArr) {
            if (bArr.length >= 3) {
                boolean z = false;
                if (HdmiCecMessageValidator.this.isValidPhysicalAddress(bArr, 0) && HdmiCecMessageValidator.isValidType(bArr[2])) {
                    z = true;
                }
                return HdmiCecMessageValidator.toErrorCode(z);
            }
            return 4;
        }
    }

    private class RoutingChangeValidator implements ParameterValidator {
        private RoutingChangeValidator() {
        }

        @Override
        public int isValid(byte[] bArr) {
            if (bArr.length < 4) {
                return 4;
            }
            boolean z = false;
            if (HdmiCecMessageValidator.this.isValidPhysicalAddress(bArr, 0) && HdmiCecMessageValidator.this.isValidPhysicalAddress(bArr, 2)) {
                z = true;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }
}
