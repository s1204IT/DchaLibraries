package com.android.internal.telephony.uicc;

import android.R;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cat.BipUtils;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.UnsupportedEncodingException;

public class IccUtils {
    static final String LOG_TAG = "IccUtils";

    public static String bcdToString(byte[] data, int offset, int length) {
        int v;
        StringBuilder ret = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length && (v = data[i] & 15) <= 9; i++) {
            ret.append((char) (v + 48));
            int v2 = (data[i] >> 4) & 15;
            if (v2 != 15) {
                if (v2 > 9) {
                    break;
                }
                ret.append((char) (v2 + 48));
            }
        }
        return ret.toString();
    }

    public static String bchToString(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length; i++) {
            int v = data[i] & 15;
            ret.append("0123456789abcdef".charAt(v));
            int v2 = (data[i] >> 4) & 15;
            ret.append("0123456789abcdef".charAt(v2));
        }
        return ret.toString();
    }

    public static String cdmaBcdToString(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder(length);
        int count = 0;
        int i = offset;
        while (count < length) {
            int v = data[i] & 15;
            if (v > 9) {
                v = 0;
            }
            ret.append((char) (v + 48));
            int count2 = count + 1;
            if (count2 == length) {
                break;
            }
            int v2 = (data[i] >> 4) & 15;
            if (v2 > 9) {
                v2 = 0;
            }
            ret.append((char) (v2 + 48));
            count = count2 + 1;
            i++;
        }
        return ret.toString();
    }

    public static int gsmBcdByteToInt(byte b) {
        int ret = 0;
        if ((b & 240) <= 144) {
            ret = (b >> 4) & 15;
        }
        if ((b & 15) <= 9) {
            return ret + ((b & 15) * 10);
        }
        return ret;
    }

    public static int cdmaBcdByteToInt(byte b) {
        int ret = 0;
        if ((b & 240) <= 144) {
            ret = ((b >> 4) & 15) * 10;
        }
        if ((b & 15) <= 9) {
            return ret + (b & 15);
        }
        return ret;
    }

    public static String adnStringFieldToString(byte[] data, int offset, int length) {
        if (length == 0) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        if (length >= 1 && data[offset] == -128) {
            int ucslen = (length - 1) / 2;
            String ret = null;
            try {
                ret = new String(data, offset + 1, ucslen * 2, "utf-16be");
            } catch (UnsupportedEncodingException ex) {
                Rlog.e(LOG_TAG, "implausible UnsupportedEncodingException", ex);
            }
            if (ret != null) {
                int ucslen2 = ret.length();
                while (ucslen2 > 0 && ret.charAt(ucslen2 - 1) == 65535) {
                    ucslen2--;
                }
                return ret.substring(0, ucslen2);
            }
        }
        boolean isucs2 = false;
        char base = 0;
        int len = 0;
        if (length >= 3 && data[offset] == -127) {
            len = data[offset + 1] & 255;
            if (len > length - 3) {
                len = length - 3;
            }
            base = (char) ((data[offset + 2] & PplMessageManager.Type.INVALID) << 7);
            offset += 3;
            isucs2 = true;
        } else if (length >= 4 && data[offset] == -126) {
            len = data[offset + 1] & 255;
            if (len > length - 4) {
                len = length - 4;
            }
            base = (char) (((data[offset + 2] & PplMessageManager.Type.INVALID) << 8) | (data[offset + 3] & PplMessageManager.Type.INVALID));
            offset += 4;
            isucs2 = true;
        }
        if (isucs2) {
            StringBuilder ret2 = new StringBuilder();
            while (len > 0) {
                if (data[offset] < 0) {
                    ret2.append((char) ((data[offset] & 127) + base));
                    offset++;
                    len--;
                }
                int count = 0;
                while (count < len && data[offset + count] >= 0) {
                    count++;
                }
                ret2.append(GsmAlphabet.gsm8BitUnpackedToString(data, offset, count));
                offset += count;
                len -= count;
            }
            return ret2.toString();
        }
        Resources resource = Resources.getSystem();
        String defaultCharset = UsimPBMemInfo.STRING_NOT_SET;
        try {
            defaultCharset = resource.getString(R.string.config_systemVendorIntelligence);
        } catch (Resources.NotFoundException e) {
        }
        return GsmAlphabet.gsm8BitUnpackedToString(data, offset, length, defaultCharset.trim());
    }

    static int hexCharToInt(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 'A') + 10;
        }
        if (c < 'a' || c > 'f') {
            throw new RuntimeException("invalid hex char '" + c + "'");
        }
        return (c - 'a') + 10;
    }

    public static byte[] hexStringToBytes(String s) {
        if (s == null) {
            return null;
        }
        int sz = s.length();
        byte[] ret = new byte[sz / 2];
        for (int i = 0; i < sz; i += 2) {
            ret[i / 2] = (byte) ((hexCharToInt(s.charAt(i)) << 4) | hexCharToInt(s.charAt(i + 1)));
        }
        return ret;
    }

    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder ret = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            int b = (bytes[i] >> 4) & 15;
            ret.append("0123456789abcdef".charAt(b));
            int b2 = bytes[i] & 15;
            ret.append("0123456789abcdef".charAt(b2));
        }
        return ret.toString();
    }

    public static String networkNameToString(byte[] data, int offset, int length) {
        String ret;
        if ((data[offset] & BipUtils.TCP_STATUS_ESTABLISHED) != 128 || length < 1) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        switch ((data[offset] >>> 4) & 7) {
            case 0:
                int unusedBits = data[offset] & 7;
                int countSeptets = (((length - 1) * 8) - unusedBits) / 7;
                ret = GsmAlphabet.gsm7BitPackedToString(data, offset + 1, countSeptets);
                break;
            case 1:
                try {
                    ret = new String(data, offset + 1, length - 1, CharacterSets.MIMENAME_UTF_16);
                } catch (UnsupportedEncodingException ex) {
                    ret = UsimPBMemInfo.STRING_NOT_SET;
                    Rlog.e(LOG_TAG, "implausible UnsupportedEncodingException", ex);
                }
                break;
            default:
                ret = UsimPBMemInfo.STRING_NOT_SET;
                break;
        }
        if ((data[offset] & BipUtils.TCP_STATUS_LISTEN) != 0) {
        }
        return ret;
    }

    public static Bitmap parseToBnW(byte[] data, int length) {
        int valueIndex;
        int width = data[0] & 255;
        int valueIndex2 = 1 + 1;
        int height = data[1] & 255;
        int numOfPixels = width * height;
        int[] pixels = new int[numOfPixels];
        int bitIndex = 7;
        byte currentByte = 0;
        int pixelIndex = 0;
        while (pixelIndex < numOfPixels) {
            if (pixelIndex % 8 == 0) {
                valueIndex = valueIndex2 + 1;
                currentByte = data[valueIndex2];
                bitIndex = 7;
            } else {
                valueIndex = valueIndex2;
            }
            pixels[pixelIndex] = bitToRGB((currentByte >> bitIndex) & 1);
            bitIndex--;
            pixelIndex++;
            valueIndex2 = valueIndex;
        }
        if (pixelIndex != numOfPixels) {
            Rlog.e(LOG_TAG, "parse end and size error");
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private static int bitToRGB(int bit) {
        if (bit == 1) {
            return -1;
        }
        return -16777216;
    }

    public static Bitmap parseToRGB(byte[] data, int length, boolean transparency) {
        int[] resultArray;
        int width = data[0] & PplMessageManager.Type.INVALID;
        int valueIndex = 1 + 1;
        int height = data[1] & PplMessageManager.Type.INVALID;
        int valueIndex2 = valueIndex + 1;
        int bits = data[valueIndex] & PplMessageManager.Type.INVALID;
        int valueIndex3 = valueIndex2 + 1;
        int colorNumber = data[valueIndex2] & PplMessageManager.Type.INVALID;
        int valueIndex4 = valueIndex3 + 1;
        int i = (data[valueIndex3] & PplMessageManager.Type.INVALID) << 8;
        int valueIndex5 = valueIndex4 + 1;
        int clutOffset = i | (data[valueIndex4] & PplMessageManager.Type.INVALID);
        int[] colorIndexArray = getCLUT(data, clutOffset, colorNumber);
        if (transparency) {
            colorIndexArray[colorNumber - 1] = 0;
        }
        if (8 % bits == 0) {
            resultArray = mapTo2OrderBitColor(data, valueIndex5, width * height, colorIndexArray, bits);
        } else {
            resultArray = mapToNon2OrderBitColor(data, valueIndex5, width * height, colorIndexArray, bits);
        }
        return Bitmap.createBitmap(resultArray, width, height, Bitmap.Config.RGB_565);
    }

    private static int[] mapTo2OrderBitColor(byte[] data, int valueIndex, int length, int[] colorArray, int bits) {
        if (8 % bits != 0) {
            Rlog.e(LOG_TAG, "not event number of color");
            return mapToNon2OrderBitColor(data, valueIndex, length, colorArray, bits);
        }
        int mask = 1;
        switch (bits) {
            case 1:
                mask = 1;
                break;
            case 2:
                mask = 3;
                break;
            case 4:
                mask = 15;
                break;
            case 8:
                mask = 255;
                break;
        }
        int[] resultArray = new int[length];
        int resultIndex = 0;
        int run = 8 / bits;
        int valueIndex2 = valueIndex;
        while (resultIndex < length) {
            int valueIndex3 = valueIndex2 + 1;
            byte tempByte = data[valueIndex2];
            int runIndex = 0;
            int resultIndex2 = resultIndex;
            while (runIndex < run) {
                int offset = (run - runIndex) - 1;
                resultArray[resultIndex2] = colorArray[(tempByte >> (offset * bits)) & mask];
                runIndex++;
                resultIndex2++;
            }
            resultIndex = resultIndex2;
            valueIndex2 = valueIndex3;
        }
        return resultArray;
    }

    private static int[] mapToNon2OrderBitColor(byte[] data, int valueIndex, int length, int[] colorArray, int bits) {
        if (8 % bits == 0) {
            Rlog.e(LOG_TAG, "not odd number of color");
            return mapTo2OrderBitColor(data, valueIndex, length, colorArray, bits);
        }
        int[] resultArray = new int[length];
        return resultArray;
    }

    private static int[] getCLUT(byte[] rawData, int offset, int number) {
        if (rawData == null) {
            return null;
        }
        int[] result = new int[number];
        int endIndex = offset + (number * 3);
        int valueIndex = offset;
        int colorIndex = 0;
        while (true) {
            int colorIndex2 = colorIndex + 1;
            int valueIndex2 = valueIndex + 1;
            int i = ((rawData[valueIndex] & PplMessageManager.Type.INVALID) << 16) | (-16777216);
            int valueIndex3 = valueIndex2 + 1;
            int i2 = i | ((rawData[valueIndex2] & PplMessageManager.Type.INVALID) << 8);
            int valueIndex4 = valueIndex3 + 1;
            result[colorIndex] = i2 | (rawData[valueIndex3] & PplMessageManager.Type.INVALID);
            if (valueIndex4 >= endIndex) {
                return result;
            }
            colorIndex = colorIndex2;
            valueIndex = valueIndex4;
        }
    }

    public static String parseIccIdToString(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length; i++) {
            int v = data[i] & 15;
            if (v >= 0 && v <= 9) {
                ret.append((char) (v + 48));
            } else {
                ret.append((char) ((v + 97) - 10));
            }
            int v2 = (data[i] >> 4) & 15;
            if (v2 >= 0 && v2 <= 9) {
                ret.append((char) (v2 + 48));
            } else {
                ret.append((char) ((v2 + 97) - 10));
            }
        }
        return ret.toString();
    }

    public static String parsePlmnToStringForEfOpl(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder(length * 2);
        int v = data[offset] & 15;
        if (v >= 0 && v <= 9) {
            ret.append((char) (v + 48));
        } else {
            if (v == 13) {
                ret.append('d');
            }
            return ret.toString();
        }
        int v2 = (data[offset] >> 4) & 15;
        if (v2 >= 0 && v2 <= 9) {
            ret.append((char) (v2 + 48));
        } else {
            if (v2 == 13) {
                ret.append('d');
            }
            return ret.toString();
        }
        int v3 = data[offset + 1] & 15;
        if (v3 >= 0 && v3 <= 9) {
            ret.append((char) (v3 + 48));
        } else {
            if (v3 == 13) {
                ret.append('d');
            }
            return ret.toString();
        }
        int v4 = data[offset + 2] & 15;
        if (v4 >= 0 && v4 <= 9) {
            ret.append((char) (v4 + 48));
        } else {
            if (v4 == 13) {
                ret.append('d');
            }
            return ret.toString();
        }
        int v5 = (data[offset + 2] >> 4) & 15;
        if (v5 >= 0 && v5 <= 9) {
            ret.append((char) (v5 + 48));
        } else {
            if (v5 == 13) {
                ret.append('d');
            }
            return ret.toString();
        }
        int v6 = (data[offset + 1] >> 4) & 15;
        if (v6 >= 0 && v6 <= 9) {
            ret.append((char) (v6 + 48));
        } else if (v6 == 13) {
            ret.append('d');
        }
        return ret.toString();
    }

    public static String parseLanguageIndicator(byte[] rawData, int offset, int length) {
        if (rawData == null) {
            return null;
        }
        if (rawData.length < offset + length) {
            Rlog.e(LOG_TAG, "length is invalid");
            return null;
        }
        return GsmAlphabet.gsm8BitUnpackedToString(rawData, offset, length);
    }

    public static String parsePlmnToString(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder(length * 2);
        int v = data[offset] & 15;
        if (v <= 9) {
            ret.append((char) (v + 48));
            int v2 = (data[offset] >> 4) & 15;
            if (v2 <= 9) {
                ret.append((char) (v2 + 48));
                int v3 = data[offset + 1] & 15;
                if (v3 <= 9) {
                    ret.append((char) (v3 + 48));
                    int v4 = data[offset + 2] & 15;
                    if (v4 <= 9) {
                        ret.append((char) (v4 + 48));
                        int v5 = (data[offset + 2] >> 4) & 15;
                        if (v5 <= 9) {
                            ret.append((char) (v5 + 48));
                            int v6 = (data[offset + 1] >> 4) & 15;
                            if (v6 <= 9) {
                                ret.append((char) (v6 + 48));
                            }
                        }
                    }
                }
            }
        }
        return ret.toString();
    }
}
