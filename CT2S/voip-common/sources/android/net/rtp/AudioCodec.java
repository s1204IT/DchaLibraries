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

    private AudioCodec(int type, String rtpmap, String fmtp) {
        this.type = type;
        this.rtpmap = rtpmap;
        this.fmtp = fmtp;
    }

    public static AudioCodec[] getCodecs() {
        return (AudioCodec[]) Arrays.copyOf(sCodecs, sCodecs.length);
    }

    public static AudioCodec getCodec(int type, String rtpmap, String fmtp) {
        if (type < 0 || type > 127) {
            return null;
        }
        AudioCodec hint = null;
        if (rtpmap != null) {
            String clue = rtpmap.trim().toUpperCase();
            AudioCodec[] arr$ = sCodecs;
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                AudioCodec codec = arr$[i$];
                if (!clue.startsWith(codec.rtpmap)) {
                    i$++;
                } else {
                    String channels = clue.substring(codec.rtpmap.length());
                    if (channels.length() == 0 || channels.equals("/1")) {
                        hint = codec;
                    }
                }
            }
        } else if (type < 96) {
            AudioCodec[] arr$2 = sCodecs;
            int len$2 = arr$2.length;
            int i$2 = 0;
            while (true) {
                if (i$2 >= len$2) {
                    break;
                }
                AudioCodec codec2 = arr$2[i$2];
                if (type != codec2.type) {
                    i$2++;
                } else {
                    hint = codec2;
                    rtpmap = codec2.rtpmap;
                    break;
                }
            }
        }
        if (hint == null) {
            return null;
        }
        if (hint == AMR && fmtp != null) {
            String clue2 = fmtp.toLowerCase();
            if (clue2.contains("crc=1") || clue2.contains("robust-sorting=1") || clue2.contains("interleaving=")) {
                return null;
            }
        }
        return new AudioCodec(type, rtpmap, fmtp);
    }
}
