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

        public FlashState(int i) {
            this.mState = i;
        }

        public boolean isFlashFired() {
            return (this.mState & FLASH_FIRED_MASK) != 0;
        }
    }

    public void addDetail(int i, Object obj) {
        this.mDetails.put(Integer.valueOf(i), obj);
    }

    public Object getDetail(int i) {
        return this.mDetails.get(Integer.valueOf(i));
    }

    public int size() {
        return this.mDetails.size();
    }

    @Override
    public Iterator<Map.Entry<Integer, Object>> iterator() {
        return this.mDetails.entrySet().iterator();
    }

    public void setUnit(int i, int i2) {
        this.mUnits.put(Integer.valueOf(i), Integer.valueOf(i2));
    }

    public boolean hasUnit(int i) {
        return this.mUnits.containsKey(Integer.valueOf(i));
    }

    public int getUnit(int i) {
        return this.mUnits.get(Integer.valueOf(i)).intValue();
    }

    private static void setExifData(MediaDetails mediaDetails, ExifTag exifTag, int i) {
        String strValueOf;
        if (exifTag != null) {
            short dataType = exifTag.getDataType();
            if (dataType == 5 || dataType == 10) {
                strValueOf = String.valueOf(exifTag.getValueAsRational(0L).toDouble());
            } else if (dataType == 2) {
                strValueOf = exifTag.getValueAsString();
            } else {
                strValueOf = String.valueOf(exifTag.forceGetValueAsLong(0L));
            }
            if (i == 102) {
                mediaDetails.addDetail(i, new FlashState(Integer.valueOf(strValueOf.toString()).intValue()));
            } else {
                mediaDetails.addDetail(i, strValueOf);
            }
        }
    }

    public static void extractExifInfo(MediaDetails mediaDetails, String str) {
        ExifInterface exifInterface = new ExifInterface();
        try {
            exifInterface.readExif(str);
        } catch (FileNotFoundException e) {
            Log.w("Gallery2/MediaDetails", "Could not find file to read exif: " + str, e);
        } catch (IOException e2) {
            Log.w("Gallery2/MediaDetails", "Could not read exif from file: " + str, e2);
        }
        setExifData(mediaDetails, exifInterface.getTag(ExifInterface.TAG_FLASH), 102);
        setExifData(mediaDetails, exifInterface.getTag(ExifInterface.TAG_IMAGE_WIDTH), 5);
        setExifData(mediaDetails, exifInterface.getTag(ExifInterface.TAG_IMAGE_LENGTH), 6);
        setExifData(mediaDetails, exifInterface.getTag(ExifInterface.TAG_MAKE), 100);
        setExifData(mediaDetails, exifInterface.getTag(ExifInterface.TAG_MODEL), 101);
        setExifData(mediaDetails, exifInterface.getTag(ExifInterface.TAG_APERTURE_VALUE), 105);
        setExifData(mediaDetails, exifInterface.getTag(ExifInterface.TAG_ISO_SPEED_RATINGS), 108);
        setExifData(mediaDetails, exifInterface.getTag(ExifInterface.TAG_WHITE_BALANCE), 104);
        setExifData(mediaDetails, exifInterface.getTag(ExifInterface.TAG_EXPOSURE_TIME), 107);
        ExifTag tag = exifInterface.getTag(ExifInterface.TAG_FOCAL_LENGTH);
        if (tag != null) {
            mediaDetails.addDetail(103, Double.valueOf(tag.getValueAsRational(0L).toDouble()));
            mediaDetails.setUnit(103, R.string.unit_mm);
        }
    }

    public static void extractDNGExifInfo(MediaDetails mediaDetails, String str) {
        try {
            android.media.ExifInterface exifInterface = new android.media.ExifInterface(str);
            String attribute = exifInterface.getAttribute("Flash");
            if (attribute != null) {
                mediaDetails.addDetail(102, new FlashState(Integer.valueOf(attribute).intValue()));
            }
            String attribute2 = exifInterface.getAttribute("ImageWidth");
            if (attribute2 != null) {
                mediaDetails.addDetail(5, attribute2);
            }
            String attribute3 = exifInterface.getAttribute("ImageLength");
            if (attribute3 != null) {
                mediaDetails.addDetail(6, attribute3);
            }
            String attribute4 = exifInterface.getAttribute("Make");
            if (attribute4 != null) {
                mediaDetails.addDetail(100, attribute4);
            }
            String attribute5 = exifInterface.getAttribute("Model");
            if (attribute5 != null) {
                mediaDetails.addDetail(101, attribute5);
            }
            String attribute6 = exifInterface.getAttribute("ApertureValue");
            if (attribute6 != null) {
                mediaDetails.addDetail(105, attribute6);
            }
            String attribute7 = exifInterface.getAttribute("ISOSpeedRatings");
            if (attribute7 != null) {
                mediaDetails.addDetail(108, attribute7);
            }
            String attribute8 = exifInterface.getAttribute("WhiteBalance");
            if (attribute8 != null) {
                mediaDetails.addDetail(104, attribute8);
            }
            String attribute9 = exifInterface.getAttribute("ExposureTime");
            if (attribute9 != null) {
                mediaDetails.addDetail(107, attribute9);
            }
            double attributeDouble = exifInterface.getAttributeDouble("FocalLength", 0.0d);
            if (attributeDouble != 0.0d) {
                mediaDetails.addDetail(103, Double.valueOf(attributeDouble));
                mediaDetails.setUnit(103, R.string.unit_mm);
            }
        } catch (IOException e) {
            Log.e("Gallery2/MediaDetails", "<extractDNGExifInfo> Could not read exif from file: " + str, e);
        }
    }
}
