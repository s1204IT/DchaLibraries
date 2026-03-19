package com.mediatek.internal.telephony.gsm;

import android.telecom.VideoProfile;
import android.util.Log;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class GsmVTProviderUtil {
    public static final int HIDE_ME_TYPE_DISABLE = 1;
    public static final int HIDE_ME_TYPE_FREEZE = 2;
    public static final int HIDE_ME_TYPE_NONE = 0;
    public static final int HIDE_ME_TYPE_PICTURE = 3;
    public static final int HIDE_YOU_TYPE_DISABLE = 0;
    public static final int HIDE_YOU_TYPE_ENABLE = 1;
    private static final String TAG = "GsmVTProviderUtil";
    public static final int TURN_OFF_CAMERA = -1;
    public static final int UI_MODE_DESTROY = 65536;
    private static ParameterSet mParamSet;
    private static GsmVTMessagePacker mPacker = new GsmVTMessagePacker();
    private static Map<String, Object> mProviderById = new HashMap();
    private static Map<String, Object> mSurfaceStatusById = new HashMap();

    public static class Size {
        public int height;
        public int width;

        public Size(int w, int h) {
            this.width = w;
            this.height = h;
        }

        public boolean equals(Object obj) {
            return (obj instanceof Size) && this.width == obj.width && this.height == obj.height;
        }

        public int hashCode() {
            return (this.width * 32713) + this.height;
        }
    }

    public class ParameterSet {
        public static final String ANTIBANDING_50HZ = "50hz";
        public static final String ANTIBANDING_60HZ = "60hz";
        public static final String ANTIBANDING_AUTO = "auto";
        public static final String ANTIBANDING_OFF = "off";
        public static final String CAPTURE_MODE_BEST_SHOT = "bestshot";
        public static final String CAPTURE_MODE_BURST_SHOT = "burstshot";
        public static final String CAPTURE_MODE_EV_BRACKET_SHOT = "evbracketshot";
        public static final String CAPTURE_MODE_NORMAL = "normal";
        public static final String CONTRAST_HIGH = "high";
        public static final String CONTRAST_LOW = "low";
        public static final String CONTRAST_MIDDLE = "middle";
        public static final String EFFECT_AQUA = "aqua";
        public static final String EFFECT_BLACKBOARD = "blackboard";
        public static final String EFFECT_MONO = "mono";
        public static final String EFFECT_NEGATIVE = "negative";
        public static final String EFFECT_NONE = "none";
        public static final String EFFECT_POSTERIZE = "posterize";
        public static final String EFFECT_SEPIA = "sepia";
        public static final String EFFECT_SOLARIZE = "solarize";
        public static final String EFFECT_WHITEBOARD = "whiteboard";
        public static final String FLASH_MODE_AUTO = "auto";
        public static final String FLASH_MODE_OFF = "off";
        public static final String FLASH_MODE_ON = "on";
        public static final String FLASH_MODE_RED_EYE = "red-eye";
        public static final String FLASH_MODE_TORCH = "torch";
        public static final String FOCUS_MODE_AUTO = "auto";
        public static final String FOCUS_MODE_EDOF = "edof";
        public static final String FOCUS_MODE_FIXED = "fixed";
        public static final String FOCUS_MODE_INFINITY = "infinity";
        public static final String FOCUS_MODE_MACRO = "macro";
        public static final String KEY_ANTIBANDING = "antibanding";
        public static final String KEY_BRIGHTNESS_MODE = "brightness";
        public static final String KEY_BURST_SHOT_NUM = "burst-num";
        public static final String KEY_CAPTURE_MODE = "cap-mode";
        public static final String KEY_CAPTURE_PATH = "capfname";
        public static final String KEY_CONTRAST_MODE = "contrast";
        public static final String KEY_EDGE_MODE = "edge";
        public static final String KEY_EFFECT = "effect";
        public static final String KEY_EXPOSURE = "exposure";
        public static final String KEY_EXPOSURE_COMPENSATION = "exposure-compensation";
        public static final String KEY_EXPOSURE_COMPENSATION_STEP = "exposure-compensation-step";
        public static final String KEY_EXPOSURE_METER = "exposure-meter";
        public static final String KEY_FD_MODE = "fd-mode";
        public static final String KEY_FLASH_MODE = "flash-mode";
        public static final String KEY_FOCAL_LENGTH = "focal-length";
        public static final String KEY_FOCUS_METER = "focus-meter";
        public static final String KEY_FOCUS_MODE = "focus-mode";
        public static final String KEY_GPS_ALTITUDE = "gps-altitude";
        public static final String KEY_GPS_LATITUDE = "gps-latitude";
        public static final String KEY_GPS_LONGITUDE = "gps-longitude";
        public static final String KEY_GPS_PROCESSING_METHOD = "gps-processing-method";
        public static final String KEY_GPS_TIMESTAMP = "gps-timestamp";
        public static final String KEY_HORIZONTAL_VIEW_ANGLE = "horizontal-view-angle";
        public static final String KEY_HUE_MODE = "hue";
        public static final String KEY_ISOSPEED_MODE = "iso-speed";
        public static final String KEY_JPEG_QUALITY = "jpeg-quality";
        public static final String KEY_JPEG_THUMBNAIL_HEIGHT = "jpeg-thumbnail-height";
        public static final String KEY_JPEG_THUMBNAIL_QUALITY = "jpeg-thumbnail-quality";
        public static final String KEY_JPEG_THUMBNAIL_SIZE = "jpeg-thumbnail-size";
        public static final String KEY_JPEG_THUMBNAIL_WIDTH = "jpeg-thumbnail-width";
        public static final String KEY_MAX_EXPOSURE_COMPENSATION = "max-exposure-compensation";
        public static final String KEY_MAX_ZOOM = "max-zoom";
        public static final String KEY_MIN_EXPOSURE_COMPENSATION = "min-exposure-compensation";
        public static final String KEY_PICTURE_FORMAT = "picture-format";
        public static final String KEY_PICTURE_SIZE = "picture-size";
        public static final String KEY_PREVIEW_FORMAT = "preview-format";
        public static final String KEY_PREVIEW_FRAME_RATE = "preview-frame-rate";
        public static final String KEY_PREVIEW_SIZE = "preview-size";
        public static final String KEY_ROTATION = "rotation";
        public static final String KEY_SATURATION_MODE = "saturation";
        public static final String KEY_SCENE_MODE = "scene-mode";
        public static final String KEY_SMOOTH_ZOOM_SUPPORTED = "smooth-zoom-supported";
        public static final String KEY_VERTICAL_VIEW_ANGLE = "vertical-view-angle";
        public static final String KEY_WHITE_BALANCE = "whitebalance";
        public static final String KEY_ZOOM = "zoom";
        public static final String KEY_ZOOM_RATIOS = "zoom-ratios";
        public static final String KEY_ZOOM_SUPPORTED = "zoom-supported";
        private static final String PIXEL_FORMAT_JPEG = "jpeg";
        private static final String PIXEL_FORMAT_RGB565 = "rgb565";
        private static final String PIXEL_FORMAT_YUV420SP = "yuv420sp";
        private static final String PIXEL_FORMAT_YUV422I = "yuv422i-yuyv";
        private static final String PIXEL_FORMAT_YUV422SP = "yuv422sp";
        public static final String SCENE_MODE_ACTION = "action";
        public static final String SCENE_MODE_AUTO = "auto";
        public static final String SCENE_MODE_BARCODE = "barcode";
        public static final String SCENE_MODE_BEACH = "beach";
        public static final String SCENE_MODE_CANDLELIGHT = "candlelight";
        public static final String SCENE_MODE_FIREWORKS = "fireworks";
        public static final String SCENE_MODE_LANDSCAPE = "landscape";
        public static final String SCENE_MODE_NIGHT = "night";
        public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";
        public static final String SCENE_MODE_PARTY = "party";
        public static final String SCENE_MODE_PORTRAIT = "portrait";
        public static final String SCENE_MODE_SNOW = "snow";
        public static final String SCENE_MODE_SPORTS = "sports";
        public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";
        public static final String SCENE_MODE_SUNSET = "sunset";
        public static final String SCENE_MODE_THEATRE = "theatre";
        public static final String SUPPORTED_VALUES_SUFFIX = "-values";
        public static final String WHITE_BALANCE_AUTO = "auto";
        public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
        public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
        public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
        public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
        public static final String WHITE_BALANCE_SHADE = "shade";
        public static final String WHITE_BALANCE_TWILIGHT = "twilight";
        public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
        private HashMap<String, String> mMap = new HashMap<>();

        public ParameterSet() {
        }

        public void dump() {
            Log.e(GsmVTProviderUtil.TAG, "dump: size=" + this.mMap.size());
            for (String k : this.mMap.keySet()) {
                Log.e(GsmVTProviderUtil.TAG, "dump: " + k + "=" + this.mMap.get(k));
            }
        }

        public String flatten() {
            StringBuilder flattened = new StringBuilder();
            for (String k : this.mMap.keySet()) {
                flattened.append(k);
                flattened.append("=");
                flattened.append(this.mMap.get(k));
                flattened.append(";");
            }
            flattened.deleteCharAt(flattened.length() - 1);
            return flattened.toString();
        }

        public void unflatten(String flattened) {
            this.mMap.clear();
            StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
            while (tokenizer.hasMoreElements()) {
                String kv = tokenizer.nextToken();
                int pos = kv.indexOf(61);
                if (pos != -1) {
                    String k = kv.substring(0, pos);
                    String v = kv.substring(pos + 1);
                    this.mMap.put(k, v);
                }
            }
        }

        public void remove(String key) {
            this.mMap.remove(key);
        }

        public void set(String key, String value) {
            if (key.indexOf(61) != -1 || key.indexOf(59) != -1) {
                Log.e(GsmVTProviderUtil.TAG, "Key \"" + key + "\" contains invalid character (= or ;)");
            } else if (value.indexOf(61) != -1 || value.indexOf(59) != -1) {
                Log.e(GsmVTProviderUtil.TAG, "Value \"" + value + "\" contains invalid character (= or ;)");
            } else {
                this.mMap.put(key, value);
            }
        }

        public void set(String key, int value) {
            this.mMap.put(key, Integer.toString(value));
        }

        public String get(String key) {
            return this.mMap.get(key);
        }

        public int getInt(String key, int defaultValue) {
            try {
                return Integer.parseInt(this.mMap.get(key));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public float getFloat(String key, float defaultValue) {
            try {
                return Float.parseFloat(this.mMap.get(key));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public Size getSize(String key) {
            return strToSize(get(key));
        }

        public List<String> getStrList(String key) {
            return split(get(key));
        }

        public List<Integer> getIntList(String key) {
            return splitInt(get(key));
        }

        public List<Size> getSizeList(String key) {
            return splitSize(get(key));
        }

        private ArrayList<String> split(String str) {
            if (str == null) {
                return null;
            }
            StringTokenizer tokenizer = new StringTokenizer(str, ",");
            ArrayList<String> substrings = new ArrayList<>();
            while (tokenizer.hasMoreElements()) {
                substrings.add(tokenizer.nextToken());
            }
            return substrings;
        }

        private ArrayList<Integer> splitInt(String str) {
            if (str == null) {
                return null;
            }
            StringTokenizer tokenizer = new StringTokenizer(str, ",");
            ArrayList<Integer> substrings = new ArrayList<>();
            while (tokenizer.hasMoreElements()) {
                String token = tokenizer.nextToken();
                substrings.add(Integer.valueOf(Integer.parseInt(token)));
            }
            if (substrings.size() == 0) {
                return null;
            }
            return substrings;
        }

        private ArrayList<Size> splitSize(String str) {
            if (str == null) {
                return null;
            }
            StringTokenizer tokenizer = new StringTokenizer(str, ",");
            ArrayList<Size> sizeList = new ArrayList<>();
            while (tokenizer.hasMoreElements()) {
                Size size = strToSize(tokenizer.nextToken());
                if (size != null) {
                    sizeList.add(size);
                }
            }
            if (sizeList.size() == 0) {
                return null;
            }
            return sizeList;
        }

        private Size strToSize(String str) {
            if (str == null) {
                return null;
            }
            int pos = str.indexOf(120);
            if (pos != -1) {
                String width = str.substring(0, pos);
                String height = str.substring(pos + 1);
                return new Size(Integer.parseInt(width), Integer.parseInt(height));
            }
            Log.e(GsmVTProviderUtil.TAG, "Invalid size parameter string=" + str);
            return null;
        }
    }

    public static class GsmVTMessagePacker {
        public String packFromVdoProfile(VideoProfile videoProfile) {
            StringBuilder flattened = new StringBuilder();
            flattened.append("mVideoState");
            flattened.append("=");
            flattened.append(UsimPBMemInfo.STRING_NOT_SET).append(videoProfile.getVideoState());
            flattened.append(";");
            flattened.append("mQuality");
            flattened.append("=");
            flattened.append(UsimPBMemInfo.STRING_NOT_SET).append(videoProfile.getQuality());
            flattened.append(";");
            flattened.deleteCharAt(flattened.length() - 1);
            Log.d(GsmVTProviderUtil.TAG, "[packFromVdoProfile] profile = " + flattened.toString());
            return flattened.toString();
        }

        public VideoProfile unPackToVdoProfile(String flattened) {
            Log.d(GsmVTProviderUtil.TAG, "[unPackToVdoProfile] flattened = " + flattened);
            StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
            int state = 3;
            int qty = 4;
            while (tokenizer.hasMoreElements()) {
                String kv = tokenizer.nextToken();
                int pos = kv.indexOf(61);
                if (pos != -1) {
                    String k = kv.substring(0, pos);
                    String v = kv.substring(pos + 1);
                    Log.d(GsmVTProviderUtil.TAG, "[unPackToVdoProfile] k = " + k + ", v = " + v);
                    if (k.equals("mVideoState")) {
                        state = Integer.valueOf(v).intValue();
                    } else if (k.equals("mQuality")) {
                        qty = Integer.valueOf(v).intValue();
                    }
                }
            }
            Log.d(GsmVTProviderUtil.TAG, "[unPackToVdoProfile] state = " + state + ", qty = " + qty);
            return new VideoProfile(state, qty);
        }
    }

    GsmVTProviderUtil() {
        mParamSet = new ParameterSet();
    }

    public static ParameterSet getSetting() {
        return mParamSet;
    }

    public static String packFromVdoProfile(VideoProfile VideoProfile) {
        return mPacker.packFromVdoProfile(VideoProfile);
    }

    public static VideoProfile unPackToVdoProfile(String flattened) {
        return mPacker.unPackToVdoProfile(flattened);
    }

    public static void surfaceSet(int Id, boolean isLocal, boolean isSet) {
        int statusInt;
        Integer status = (Integer) mSurfaceStatusById.get(UsimPBMemInfo.STRING_NOT_SET + Id);
        Log.d(TAG, "[surfaceSet] isLocal = " + isLocal + ", isSet = " + isSet);
        if (status != null) {
            int statusInt2 = status.intValue();
            Log.d(TAG, "[surfaceSet] state (before) = " + statusInt2);
            if (isLocal) {
                if (!isSet) {
                    statusInt = statusInt2 & (-2);
                } else {
                    statusInt = statusInt2 | 1;
                }
            } else if (!isSet) {
                statusInt = statusInt2 & (-3);
            } else {
                statusInt = statusInt2 | 2;
            }
        } else {
            Log.d(TAG, "[surfaceSet] state (before) = null");
            if (isLocal) {
                if (!isSet) {
                    statusInt = 0;
                } else {
                    statusInt = 1;
                }
            } else if (!isSet) {
                statusInt = 0;
            } else {
                statusInt = 2;
            }
        }
        Log.d(TAG, "[surfaceSet] state (after) = " + statusInt + ", Id = " + Id);
        mSurfaceStatusById.put(UsimPBMemInfo.STRING_NOT_SET + Id, new Integer(statusInt));
    }

    public static int surfaceGet(int Id) {
        Integer status = (Integer) mSurfaceStatusById.get(UsimPBMemInfo.STRING_NOT_SET + Id);
        if (status != null) {
            Log.d(TAG, "[surfaceGet] state = " + status.intValue() + ", Id = " + Id);
            return status.intValue();
        }
        Log.d(TAG, "[surfaceGet] state = 0, Id = " + Id);
        return 0;
    }

    public static void recordAdd(int Id, GsmVTProvider p) {
        Log.d(TAG, "recordAdd id = " + Id + ", size = " + recordSize());
        mProviderById.put(UsimPBMemInfo.STRING_NOT_SET + Id, p);
    }

    public static void recordRemove(int Id) {
        Log.d(TAG, "recordRemove id = " + Id + ", size = " + recordSize());
        mProviderById.remove(UsimPBMemInfo.STRING_NOT_SET + Id);
    }

    public static GsmVTProvider recordGet(int Id) {
        Log.d(TAG, "recordGet id = " + Id + ", size = " + recordSize());
        return (GsmVTProvider) mProviderById.get(UsimPBMemInfo.STRING_NOT_SET + Id);
    }

    public static int recordPopId() {
        if (mProviderById.size() != 0) {
            Iterator p$iterator = mProviderById.values().iterator();
            if (p$iterator.hasNext()) {
                Object p = p$iterator.next();
                return ((GsmVTProvider) p).getId();
            }
            return GsmVTProvider.VT_PROVIDER_INVALIDE_ID;
        }
        return GsmVTProvider.VT_PROVIDER_INVALIDE_ID;
    }

    public static boolean recordContain(int Id) {
        return mProviderById.containsKey(Integer.valueOf(Id));
    }

    public static int recordSize() {
        return mProviderById.size();
    }
}
