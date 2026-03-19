package android.media;

import java.util.ArrayList;
import java.util.List;

public class DecoderCapabilities {
    private static final native int native_get_audio_decoder_type(int i);

    private static final native int native_get_num_audio_decoders();

    private static final native int native_get_num_video_decoders();

    private static final native int native_get_video_decoder_type(int i);

    private static final native void native_init();

    public enum VideoDecoder {
        VIDEO_DECODER_WMV;

        public static VideoDecoder[] valuesCustom() {
            return values();
        }
    }

    public enum AudioDecoder {
        AUDIO_DECODER_WMA;

        public static AudioDecoder[] valuesCustom() {
            return values();
        }
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    public static List<VideoDecoder> getVideoDecoders() {
        List<VideoDecoder> decoderList = new ArrayList<>();
        int nDecoders = native_get_num_video_decoders();
        for (int i = 0; i < nDecoders; i++) {
            decoderList.add(VideoDecoder.valuesCustom()[native_get_video_decoder_type(i)]);
        }
        return decoderList;
    }

    public static List<AudioDecoder> getAudioDecoders() {
        List<AudioDecoder> decoderList = new ArrayList<>();
        int nDecoders = native_get_num_audio_decoders();
        for (int i = 0; i < nDecoders; i++) {
            decoderList.add(AudioDecoder.valuesCustom()[native_get_audio_decoder_type(i)]);
        }
        return decoderList;
    }

    private DecoderCapabilities() {
    }
}
