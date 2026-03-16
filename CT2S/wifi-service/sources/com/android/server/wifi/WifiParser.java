package com.android.server.wifi;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class WifiParser {
    private static final int IEEE_RSN_IE = 48;
    private static final int VENDOR_SPECIFIC_IE = 221;
    private static final int WPA_IE_VENDOR_TYPE = 5304833;

    class IE {
        byte[] data;
        int id;

        IE() {
        }
    }

    public static String parse_akm(IE[] full_IE, BitSet ieee_cap) {
        boolean error = false;
        if (ieee_cap == null || full_IE == null) {
            return null;
        }
        boolean privacy = ieee_cap.get(4);
        String capabilities = "";
        boolean rsne_found = false;
        int len$ = full_IE.length;
        int i$ = 0;
        while (true) {
            if (i$ < len$) {
                IE ie = full_IE[i$];
                if (ie.id == IEEE_RSN_IE) {
                    rsne_found = true;
                    ByteBuffer buf = ByteBuffer.wrap(ie.data);
                    int total_len = ie.data.length;
                    if (total_len - 2 < 2) {
                        error = true;
                    } else if (256 != buf.getShort(2)) {
                        error = true;
                    } else {
                        int offset = 2 + 2;
                        if (total_len - 4 < 4) {
                            error = true;
                        } else {
                            int offset2 = offset + 4;
                            if (total_len - 8 < 2) {
                                error = true;
                            } else {
                                int val = buf.getShort(offset2);
                                if (total_len - 8 < (val * 4) + 2) {
                                    error = true;
                                } else {
                                    int offset3 = (val * 4) + 2 + 8;
                                    if (total_len - offset3 < 2) {
                                        error = true;
                                    } else {
                                        int val2 = buf.getShort(offset3);
                                        if (total_len - offset3 < (val2 * 4) + 2) {
                                            error = true;
                                        } else {
                                            int offset4 = offset3 + 2;
                                            String security = val2 == 0 ? "[WPA2-EAP" : "[WPA2";
                                            for (int i = 0; i < val2; i++) {
                                                int akm = buf.getInt(offset4);
                                                switch (akm) {
                                                    case 28053248:
                                                        security = security + (0 != 0 ? "+" : "-EAP");
                                                        break;
                                                    case 44830464:
                                                        security = security + (0 != 0 ? "+" : "-PSK");
                                                        break;
                                                    case 61607680:
                                                        security = security + (0 != 0 ? "+" : "-FT/EAP");
                                                        break;
                                                    case 78384896:
                                                        security = security + (0 != 0 ? "+" : "-FT/PSK");
                                                        break;
                                                    case 95162112:
                                                        security = security + (0 != 0 ? "+" : "-EAP-SHA256");
                                                        break;
                                                    case 111939328:
                                                        security = security + (0 != 0 ? "+" : "-PSK-SHA256");
                                                        break;
                                                }
                                                offset4 += 4;
                                            }
                                            capabilities = capabilities + (security + "]");
                                            if (ie.id != VENDOR_SPECIFIC_IE) {
                                                int total_len2 = ie.data.length;
                                                if (total_len2 - 2 < 4) {
                                                    error = true;
                                                } else {
                                                    ByteBuffer buf2 = ByteBuffer.wrap(ie.data);
                                                    if (buf2.getInt(2) == 32657408) {
                                                        if (total_len2 - 2 < 2) {
                                                            error = true;
                                                        } else if (256 != buf2.getShort(2)) {
                                                            error = true;
                                                        } else {
                                                            int offset5 = 2 + 2;
                                                            if (total_len2 - 4 < 4) {
                                                                error = true;
                                                            } else {
                                                                int offset6 = offset5 + 4;
                                                                if (total_len2 - 8 < 2) {
                                                                    error = true;
                                                                } else {
                                                                    int val3 = buf2.getShort(offset6);
                                                                    if (total_len2 - 8 < (val3 * 4) + 2) {
                                                                        error = true;
                                                                    } else {
                                                                        int offset7 = (val3 * 4) + 2 + 8;
                                                                        if (total_len2 - offset7 < 2) {
                                                                            error = true;
                                                                        } else {
                                                                            int val4 = buf2.getShort(offset7);
                                                                            if (total_len2 - offset7 < (val4 * 4) + 2) {
                                                                                error = true;
                                                                            } else {
                                                                                int offset8 = offset7 + 2;
                                                                                String security2 = val4 == 0 ? "[WPA-EAP" : "[WPA";
                                                                                for (int i2 = 0; i2 < val4; i2++) {
                                                                                    int akm2 = buf2.getInt(offset8);
                                                                                    switch (akm2) {
                                                                                        case 32657408:
                                                                                            security2 = security2 + (0 != 0 ? "+" : "-EAP");
                                                                                            break;
                                                                                        case 49434624:
                                                                                            security2 = security2 + (0 != 0 ? "+" : "-PSK");
                                                                                            break;
                                                                                    }
                                                                                    offset8 += 4;
                                                                                }
                                                                                String str = security2 + "]";
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            i$++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (ie.id != VENDOR_SPECIFIC_IE) {
                    }
                    i$++;
                }
            }
        }
        if (!rsne_found && 0 == 0 && privacy) {
            capabilities = capabilities + "[WEP]";
        }
        if (error) {
            return null;
        }
        return capabilities;
    }
}
