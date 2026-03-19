package android.net.rtp;

import java.util.Arrays;

public class AudioCodec {
    public final String fmtp;
    public final String rtpmap;
    public final int type;
    public static final AudioCodec PCMU = new AudioCodec(0, "PCMU/8000", null);
    public static final AudioCodec PCMA = new AudioCodec(8, "PCMA/8000", null);
    public static final AudioCodec GSM = new AudioCodec(3, "GSM/8000", null);
    public static final AudioCodec GSM_EFR = new AudioCodec(96, "GSM-EFR/8000", null);
    public static final AudioCodec AMR = new AudioCodec(97, "AMR/8000", null);
    private static final AudioCodec[] sCodecs = {GSM_EFR, AMR, GSM, PCMU, PCMA};

    private AudioCodec(int i, String str, String str2) {
        this.type = i;
        this.rtpmap = str;
        this.fmtp = str2;
    }

    public static AudioCodec[] getCodecs() {
        return (AudioCodec[]) Arrays.copyOf(sCodecs, sCodecs.length);
    }

    public static AudioCodec getCodec(int i, String str, String str2) {
        AudioCodec audioCodec;
        AudioCodec audioCodec2;
        if (i < 0 || i > 127) {
            return null;
        }
        int i2 = 0;
        if (str != null) {
            String upperCase = str.trim().toUpperCase();
            AudioCodec[] audioCodecArr = sCodecs;
            int length = audioCodecArr.length;
            while (true) {
                if (i2 >= length) {
                    break;
                }
                audioCodec2 = audioCodecArr[i2];
                if (!upperCase.startsWith(audioCodec2.rtpmap)) {
                    i2++;
                } else {
                    String strSubstring = upperCase.substring(audioCodec2.rtpmap.length());
                    if (strSubstring.length() != 0 && !strSubstring.equals("/1")) {
                        break;
                    }
                }
            }
            audioCodec = audioCodec2;
        } else if (i < 96) {
            AudioCodec[] audioCodecArr2 = sCodecs;
            int length2 = audioCodecArr2.length;
            while (i2 < length2) {
                audioCodec = audioCodecArr2[i2];
                if (i != audioCodec.type) {
                    i2++;
                } else {
                    str = audioCodec.rtpmap;
                    break;
                }
            }
            audioCodec = null;
        } else {
            audioCodec = null;
        }
        if (audioCodec == null) {
            return null;
        }
        if (audioCodec == AMR && str2 != null) {
            String lowerCase = str2.toLowerCase();
            if (lowerCase.contains("crc=1") || lowerCase.contains("robust-sorting=1") || lowerCase.contains("interleaving=")) {
                return null;
            }
        }
        return new AudioCodec(i, str, str2);
    }
}
