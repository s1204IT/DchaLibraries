package com.android.internal.telephony.uicc;

import android.os.Build;

public class IccIoResult {
    private static final String UNKNOWN_ERROR = "unknown";
    public byte[] payload;
    public int sw1;
    public int sw2;

    private java.lang.String getErrorString() {
        r0 = r6.sw1;
        if (r0 != 152) {
            switch (r0) {
                case 98:
                    r0 = r6.sw2;
                    if (r0 != 0) {
                        switch (r0) {
                            case 129:
                                break;
                            case 130:
                                break;
                            case 131:
                                break;
                            case 132:
                                break;
                            default:
                                switch (r0) {
                                }
                        }
                    }
                case 99:
                    if ((r6.sw2 >> 4) == 12) {
                    } else {
                        switch (r6.sw2) {
                        }
                    }
                case 100:
                    if (r6.sw2 != 0) {
                    }
                case 101:
                    r0 = r6.sw2;
                    if (r0 != 0) {
                        if (r0 != 129) {
                        }
                    }
                default:
                    switch (r0) {
                        case 103:
                            if (r6.sw2 != 0) {
                            }
                        case 104:
                            r0 = r6.sw2;
                            if (r0 != 0) {
                                switch (r0) {
                                }
                            }
                        case 105:
                            r0 = r6.sw2;
                            if (r0 != 0) {
                                if (r0 != 137) {
                                    switch (r0) {
                                    }
                                }
                            }
                        case 106:
                            switch (r6.sw2) {
                            }
                        case 107:
                        default:
                            switch (r0) {
                                case 109:
                                case 110:
                                case 111:
                                    if (r6.sw2 != 0) {
                                    }
                                default:
                                    switch (r0) {
                                        case 144:
                                        case 145:
                                        case 146:
                                            if ((r6.sw2 >> 4) == 0) {
                                            } else {
                                                if (r6.sw2 != 64) {
                                                }
                                            }
                                        case 147:
                                            if (r6.sw2 != 0) {
                                            }
                                        case 148:
                                            r0 = r6.sw2;
                                            if (r0 != 0) {
                                                if (r0 != 2) {
                                                    if (r0 != 4) {
                                                        if (r0 != 8) {
                                                        }
                                                    }
                                                }
                                            }
                                        default:
                                            switch (r0) {
                                            }
                                    }
                            }
                    }
            }
            return null;
        } else {
            r0 = r6.sw2;
            if (r0 != 2) {
                if (r0 != 4) {
                    if (r0 != 8) {
                        if (r0 != 16) {
                            if (r0 != 64) {
                                if (r0 != 80) {
                                    if (r0 != 98) {
                                        switch (r0) {
                                        }
                                        return null;
                                    } else {
                                        return "authentication error, application specific";
                                    }
                                } else {
                                    return "increase cannot be performed, Max value reached";
                                }
                            } else {
                                return "unsuccessful CHV verification, no attempt left/unsuccessful UNBLOCK CHV verification, no attempt left/CHV blockedUNBLOCK CHV blocked";
                            }
                        } else {
                            return "in contradiction with invalidation status";
                        }
                    } else {
                        return "in contradiction with CHV status";
                    }
                } else {
                    return "access condition not fulfilled/unsuccessful CHV verification, at least one attempt left/unsuccessful UNBLOCK CHV verification, at least one attempt left/authentication failed";
                }
            } else {
                return "no CHV initialized";
            }
        }
    }

    public IccIoResult(int i, int i2, byte[] bArr) {
        this.sw1 = i;
        this.sw2 = i2;
        this.payload = bArr;
    }

    public IccIoResult(int i, int i2, String str) {
        this(i, i2, IccUtils.hexStringToBytes(str));
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("IccIoResult sw1:0x");
        sb.append(Integer.toHexString(this.sw1));
        sb.append(" sw2:0x");
        sb.append(Integer.toHexString(this.sw2));
        sb.append(" Payload: ");
        sb.append((Build.IS_DEBUGGABLE && Build.IS_ENG) ? this.payload : "*******");
        if (success()) {
            str = "";
        } else {
            str = " Error: " + getErrorString();
        }
        sb.append(str);
        return sb.toString();
    }

    public boolean success() {
        return this.sw1 == 144 || this.sw1 == 145 || this.sw1 == 158 || this.sw1 == 159;
    }

    public IccException getException() {
        if (success()) {
            return null;
        }
        if (this.sw1 == 148) {
            if (this.sw2 == 8) {
                return new IccFileTypeMismatch();
            }
            return new IccFileNotFound();
        }
        return new IccException("sw1:" + this.sw1 + " sw2:" + this.sw2);
    }
}
