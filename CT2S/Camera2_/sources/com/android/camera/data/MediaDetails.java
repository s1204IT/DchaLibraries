package com.android.camera.data;

import android.content.Context;
import android.util.SparseIntArray;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;
import com.android.camera2.R;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class MediaDetails implements Iterable<Map.Entry<Integer, Object>> {
    public static final int INDEX_APERTURE = 105;
    public static final int INDEX_DATETIME = 3;
    public static final int INDEX_DESCRIPTION = 2;
    public static final int INDEX_DURATION = 8;
    public static final int INDEX_EXPOSURE_TIME = 107;
    public static final int INDEX_FLASH = 102;
    public static final int INDEX_FOCAL_LENGTH = 103;
    public static final int INDEX_HEIGHT = 6;
    public static final int INDEX_ISO = 108;
    public static final int INDEX_LOCATION = 4;
    public static final int INDEX_MAKE = 100;
    public static final int INDEX_MIMETYPE = 9;
    public static final int INDEX_MODEL = 101;
    public static final int INDEX_ORIENTATION = 7;
    public static final int INDEX_PATH = 200;
    public static final int INDEX_SHUTTER_SPEED = 106;
    public static final int INDEX_SIZE = 10;
    public static final int INDEX_TITLE = 1;
    public static final int INDEX_WHITE_BALANCE = 104;
    public static final int INDEX_WIDTH = 5;
    private static final Log.Tag TAG = new Log.Tag("MediaDetails");
    private TreeMap<Integer, Object> mDetails = new TreeMap<>();
    private SparseIntArray mUnits = new SparseIntArray();

    public static class FlashState {
        private int mState;
        private static int FLASH_FIRED_MASK = 1;
        private static int FLASH_RETURN_MASK = 6;
        private static int FLASH_MODE_MASK = 24;
        private static int FLASH_FUNCTION_MASK = 32;
        private static int FLASH_RED_EYE_MASK = 64;

        public FlashState(int state) {
            this.mState = state;
        }

        public boolean isFlashFired() {
            return (this.mState & FLASH_FIRED_MASK) != 0;
        }
    }

    public void addDetail(int index, Object value) {
        this.mDetails.put(Integer.valueOf(index), value);
    }

    public Object getDetail(int index) {
        return this.mDetails.get(Integer.valueOf(index));
    }

    public int size() {
        return this.mDetails.size();
    }

    @Override
    public Iterator<Map.Entry<Integer, Object>> iterator() {
        return this.mDetails.entrySet().iterator();
    }

    public void setUnit(int index, int unit) {
        this.mUnits.put(index, unit);
    }

    public boolean hasUnit(int index) {
        return this.mUnits.indexOfKey(index) >= 0;
    }

    public int getUnit(int index) {
        return this.mUnits.get(index);
    }

    private static void setExifData(MediaDetails details, ExifTag tag, int key) {
        String value;
        if (tag != null) {
            int type = tag.getDataType();
            if (type == 5 || type == 10) {
                value = String.valueOf(tag.getValueAsRational(0L).toDouble());
            } else if (type == 2) {
                value = tag.getValueAsString();
            } else {
                value = String.valueOf(tag.forceGetValueAsLong(0L));
            }
            if (key == 102) {
                FlashState state = new FlashState(Integer.valueOf(value.toString()).intValue());
                details.addDetail(key, state);
            } else {
                details.addDetail(key, value);
            }
        }
    }

    public static void extractExifInfo(MediaDetails details, String filePath) {
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(filePath);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Could not find file to read exif: " + filePath, e);
        } catch (IOException e2) {
            Log.w(TAG, "Could not read exif from file: " + filePath, e2);
        }
        setExifData(details, exif.getTag(ExifInterface.TAG_FLASH), 102);
        setExifData(details, exif.getTag(ExifInterface.TAG_IMAGE_WIDTH), 5);
        setExifData(details, exif.getTag(ExifInterface.TAG_IMAGE_LENGTH), 6);
        setExifData(details, exif.getTag(ExifInterface.TAG_MAKE), 100);
        setExifData(details, exif.getTag(ExifInterface.TAG_MODEL), 101);
        setExifData(details, exif.getTag(ExifInterface.TAG_APERTURE_VALUE), 105);
        setExifData(details, exif.getTag(ExifInterface.TAG_ISO_SPEED_RATINGS), 108);
        setExifData(details, exif.getTag(ExifInterface.TAG_WHITE_BALANCE), 104);
        setExifData(details, exif.getTag(ExifInterface.TAG_EXPOSURE_TIME), 107);
        ExifTag focalTag = exif.getTag(ExifInterface.TAG_FOCAL_LENGTH);
        if (focalTag != null) {
            details.addDetail(103, Double.valueOf(focalTag.getValueAsRational(0L).toDouble()));
            details.setUnit(103, R.string.unit_mm);
        }
    }

    public static String formatDuration(Context context, long seconds) {
        long h = seconds / 3600;
        long m = (seconds - (3600 * h)) / 60;
        long s = seconds - ((3600 * h) + (60 * m));
        if (h == 0) {
            String durationValue = String.format(context.getString(R.string.details_ms), Long.valueOf(m), Long.valueOf(s));
            return durationValue;
        }
        String durationValue2 = String.format(context.getString(R.string.details_hms), Long.valueOf(h), Long.valueOf(m), Long.valueOf(s));
        return durationValue2;
    }
}
