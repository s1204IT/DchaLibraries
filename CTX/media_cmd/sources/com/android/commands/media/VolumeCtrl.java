package com.android.commands.media;

import android.media.AudioSystem;
import android.media.IAudioService;
import android.os.ServiceManager;
import android.util.AndroidException;
import com.android.internal.os.BaseCommand;

public class VolumeCtrl {
    private static final String ADJUST_LOWER = "lower";
    private static final String ADJUST_RAISE = "raise";
    private static final String ADJUST_SAME = "same";
    static final String LOG_OK = "[ok]";
    static final String LOG_V = "[v]";
    static final String LOG_W = "[w]";
    private static final String TAG = "VolumeCtrl";
    public static final String USAGE = new String("the options are as follows: \n\t\t--stream STREAM selects the stream to control, see AudioManager.STREAM_*\n\t\t                controls AudioManager.STREAM_MUSIC if no stream is specified\n\t\t--set INDEX     sets the volume index value\n\t\t--adj DIRECTION adjusts the volume, use raise|same|lower for the direction\n\t\t--get           outputs the current volume\n\t\t--show          shows the UI during the volume change\n\texamples:\n\t\tadb shell media volume --show --stream 3 --set 11\n\t\tadb shell media volume --stream 0 --adj lower\n\t\tadb shell media volume --stream 3 --get\n");
    private static final int VOLUME_CONTROL_MODE_ADJUST = 2;
    private static final int VOLUME_CONTROL_MODE_SET = 1;

    public static void run(BaseCommand baseCommand) throws Exception {
        byte b;
        String str = null;
        int iIntValue = 3;
        int i = 0;
        boolean z = false;
        int i2 = 5;
        char c = 0;
        while (true) {
            String strNextOption = baseCommand.nextOption();
            byte b2 = 4;
            int i3 = -1;
            if (strNextOption == null) {
                if (c != VOLUME_CONTROL_MODE_ADJUST) {
                    i3 = VOLUME_CONTROL_MODE_SET;
                } else {
                    if (str == null) {
                        baseCommand.showError("Error: no valid volume adjustment (null)");
                        return;
                    }
                    int iHashCode = str.hashCode();
                    if (iHashCode == 3522662) {
                        if (str.equals(ADJUST_SAME)) {
                            b = VOLUME_CONTROL_MODE_SET;
                        }
                        switch (b) {
                        }
                    } else if (iHashCode != 103164673) {
                        b = (iHashCode == 108275692 && str.equals(ADJUST_RAISE)) ? (byte) 0 : (byte) -1;
                        switch (b) {
                            case 0:
                                break;
                            case VOLUME_CONTROL_MODE_SET:
                                i3 = 0;
                                break;
                            case VOLUME_CONTROL_MODE_ADJUST:
                                break;
                            default:
                                baseCommand.showError("Error: no valid volume adjustment, was " + str + ", expected " + ADJUST_LOWER + "|" + ADJUST_SAME + "|" + ADJUST_RAISE);
                                return;
                        }
                    } else {
                        if (str.equals(ADJUST_LOWER)) {
                            b = VOLUME_CONTROL_MODE_ADJUST;
                        }
                        switch (b) {
                        }
                    }
                }
                log(LOG_V, "Connecting to AudioService");
                IAudioService iAudioServiceAsInterface = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
                if (iAudioServiceAsInterface == null) {
                    System.err.println("Error type 2");
                    throw new AndroidException("Can't connect to audio service; is the system running?");
                }
                if (c == VOLUME_CONTROL_MODE_SET && (i2 > iAudioServiceAsInterface.getStreamMaxVolume(iIntValue) || i2 < iAudioServiceAsInterface.getStreamMinVolume(iIntValue))) {
                    baseCommand.showError(String.format("Error: invalid volume index %d for stream %d (should be in [%d..%d])", Integer.valueOf(i2), Integer.valueOf(iIntValue), Integer.valueOf(iAudioServiceAsInterface.getStreamMinVolume(iIntValue)), Integer.valueOf(iAudioServiceAsInterface.getStreamMaxVolume(iIntValue))));
                    return;
                }
                String name = baseCommand.getClass().getPackage().getName();
                if (c == VOLUME_CONTROL_MODE_SET) {
                    iAudioServiceAsInterface.setStreamVolume(iIntValue, i2, i, name);
                } else if (c == VOLUME_CONTROL_MODE_ADJUST) {
                    iAudioServiceAsInterface.adjustStreamVolume(iIntValue, i3, i, name);
                }
                if (z) {
                    log(LOG_V, "volume is " + iAudioServiceAsInterface.getStreamVolume(iIntValue) + " in range [" + iAudioServiceAsInterface.getStreamMinVolume(iIntValue) + ".." + iAudioServiceAsInterface.getStreamMaxVolume(iIntValue) + "]");
                    return;
                }
                return;
            }
            switch (strNextOption.hashCode()) {
                case 42995463:
                    if (!strNextOption.equals("--adj")) {
                        b2 = -1;
                    }
                    break;
                case 43001270:
                    b2 = !strNextOption.equals("--get") ? (byte) -1 : VOLUME_CONTROL_MODE_SET;
                    break;
                case 43012802:
                    b2 = !strNextOption.equals("--set") ? (byte) -1 : (byte) 3;
                    break;
                case 1333399709:
                    b2 = !strNextOption.equals("--show") ? (byte) -1 : (byte) 0;
                    break;
                case 1508023584:
                    b2 = !strNextOption.equals("--stream") ? (byte) -1 : VOLUME_CONTROL_MODE_ADJUST;
                    break;
                default:
                    b2 = -1;
                    break;
            }
            switch (b2) {
                case 0:
                    i = VOLUME_CONTROL_MODE_SET;
                    break;
                case VOLUME_CONTROL_MODE_SET:
                    log(LOG_V, "will get volume");
                    z = VOLUME_CONTROL_MODE_SET;
                    break;
                case VOLUME_CONTROL_MODE_ADJUST:
                    iIntValue = Integer.decode(baseCommand.nextArgRequired()).intValue();
                    log(LOG_V, "will control stream=" + iIntValue + " (" + streamName(iIntValue) + ")");
                    break;
                case 3:
                    int iIntValue2 = Integer.decode(baseCommand.nextArgRequired()).intValue();
                    log(LOG_V, "will set volume to index=" + iIntValue2);
                    i2 = iIntValue2;
                    c = VOLUME_CONTROL_MODE_SET;
                    break;
                case 4:
                    String strNextArgRequired = baseCommand.nextArgRequired();
                    log(LOG_V, "will adjust volume");
                    str = strNextArgRequired;
                    c = VOLUME_CONTROL_MODE_ADJUST;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument " + strNextOption);
            }
        }
    }

    static void log(String str, String str2) {
        System.out.println(str + " " + str2);
    }

    static String streamName(int i) {
        try {
            return AudioSystem.STREAM_NAMES[i];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "invalid stream";
        }
    }
}
