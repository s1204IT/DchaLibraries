package com.android.gallery3d.data;

import com.android.gallery3d.R;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class MediaDetails implements Iterable<Map.Entry<Integer, Object>> {
    private TreeMap<Integer, Object> mDetails = new TreeMap<>();
    private HashMap<Integer, Integer> mUnits = new HashMap<>();

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
        this.mUnits.put(Integer.valueOf(index), Integer.valueOf(unit));
    }

    public boolean hasUnit(int index) {
        return this.mUnits.containsKey(Integer.valueOf(index));
    }

    public int getUnit(int index) {
        return this.mUnits.get(Integer.valueOf(index)).intValue();
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
            Log.w("MediaDetails", "Could not find file to read exif: " + filePath, e);
        } catch (IOException e2) {
            Log.w("MediaDetails", "Could not read exif from file: " + filePath, e2);
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
}
