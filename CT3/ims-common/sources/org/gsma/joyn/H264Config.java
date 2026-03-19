package org.gsma.joyn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class H264Config {
    public static final int BIT_RATE = 64000;
    public static final int CIF_HEIGHT = 288;
    public static final int CIF_WIDTH = 352;
    public static final int CLOCK_RATE = 90000;
    public static final String CODEC_NAME = "H264";
    public static final String CODEC_PARAMS = "profile-level-id=42900b;packetization-mode=1";
    public static final String CODEC_PARAM_PACKETIZATIONMODE = "packetization-mode";
    public static final String CODEC_PARAM_PROFILEID = "profile-level-id";
    public static final String CODEC_PARAM_SPROP_PARAMETER_SETS = "sprop-parameter-sets";
    public static final int FRAME_RATE = 15;
    public static final int QCIF_HEIGHT = 144;
    public static final int QCIF_WIDTH = 176;
    public static final int QVGA_HEIGHT = 240;
    public static final int QVGA_WIDTH = 320;
    public static final int VGA_HEIGHT = 480;
    public static final int VGA_WIDTH = 640;
    public static final int VIDEO_HEIGHT = 144;
    public static final int VIDEO_WIDTH = 176;

    public static int getCodecPacketizationMode(String codecParams) {
        String valPackMode = getParameterValue(CODEC_PARAM_PACKETIZATIONMODE, codecParams);
        if (valPackMode == null) {
            return 0;
        }
        try {
            int packetization_mode = Integer.parseInt(valPackMode);
            return packetization_mode;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getCodecProfileLevelId(String codecParams) {
        return getParameterValue(CODEC_PARAM_PROFILEID, codecParams);
    }

    private static String getParameterValue(String paramKey, String params) {
        if (params == null || params.length() <= 0) {
            return null;
        }
        try {
            Pattern p = Pattern.compile("(?<=" + paramKey + "=).*?(?=;|$)");
            Matcher m = p.matcher(params);
            if (!m.find()) {
                return null;
            }
            String value = m.group(0);
            return value;
        } catch (PatternSyntaxException e) {
            return null;
        }
    }
}
