package com.mediatek.gallerybasic.base;

import android.content.Context;
import android.database.Cursor;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.gallerybasic.util.MediaUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtFields {
    private static final String TAG = "MtkGallery2/ExtFields";
    private Object[] mImageFieldValues;
    private Object[] mVideoFieldValues;
    private static HashMap<String, Position> sImageFields = new HashMap<>();
    private static HashMap<String, Position> sVideoFields = new HashMap<>();
    private static List<String> sImageColumns = new ArrayList();
    private static List<String> sVideoColumns = new ArrayList();

    public ExtFields() {
    }

    public ExtFields(Cursor cursor, boolean z) {
        if (z && !sImageFields.isEmpty()) {
            this.mImageFieldValues = new Object[sImageFields.size()];
            setFieldsValue(cursor, sImageFields, this.mImageFieldValues);
        } else if (!z && !sVideoFields.isEmpty()) {
            this.mVideoFieldValues = new Object[sVideoFields.size()];
            setFieldsValue(cursor, sVideoFields, this.mVideoFieldValues);
        }
    }

    public static void registerFieldDefinition(IFieldDefinition[] iFieldDefinitionArr) {
        for (IFieldDefinition iFieldDefinition : iFieldDefinitionArr) {
            iFieldDefinition.onFieldDefine();
        }
    }

    public static void addImageFiled(String str) {
        if (sImageColumns != null && sImageColumns.contains(str)) {
            Log.d(TAG, "<addImageFiled> support [" + str + "] on current platform");
            addField(sImageFields, str);
            return;
        }
        Log.d(TAG, "<addImageFiled> not support [" + str + "] on current platform");
    }

    public static void addVideoFiled(String str) {
        if (sVideoColumns != null && sVideoColumns.contains(str)) {
            Log.d(TAG, "<addVideoFiled> support [" + str + "] on current platform");
            addField(sVideoFields, str);
            return;
        }
        Log.d(TAG, "<addVideoFiled> not support [" + str + "] on current platform");
    }

    public static String[] getImageProjection(String[] strArr) {
        return getProjection(sImageFields, strArr);
    }

    public static String[] getVideoProjection(String[] strArr) {
        return getProjection(sVideoFields, strArr);
    }

    public static void initColumns(Context context) {
        sImageColumns = MediaUtils.getImageColumns(context);
        sVideoColumns = MediaUtils.getVideoColumns(context);
    }

    private static void addField(HashMap<String, Position> map, String str) {
        if (map.containsKey(str)) {
            Log.d(TAG, "<addField> already has column[" + str + "], return");
            return;
        }
        map.put(str, new Position());
        Log.d(TAG, "<addField> add column[" + str + "], return");
    }

    private static String[] getProjection(HashMap<String, Position> map, String[] strArr) {
        if (map.isEmpty()) {
            return strArr;
        }
        String[] strArr2 = new String[strArr.length + map.size()];
        int i = 0;
        int i2 = 0;
        for (String str : strArr) {
            strArr2[i2] = strArr[i2];
            i2++;
        }
        for (Map.Entry<String, Position> entry : map.entrySet()) {
            strArr2[i2] = entry.getKey();
            entry.getValue().projectionIndex = i2;
            entry.getValue().valueIndex = i;
            i2++;
            i++;
        }
        return strArr2;
    }

    private static void setFieldsValue(Cursor cursor, HashMap<String, Position> map, Object[] objArr) {
        int i = 0;
        for (Map.Entry<String, Position> entry : map.entrySet()) {
            switch (cursor.getType(entry.getValue().projectionIndex)) {
                case 0:
                    objArr[i] = null;
                    break;
                case 1:
                    objArr[i] = Integer.valueOf(cursor.getInt(entry.getValue().projectionIndex));
                    break;
                case 2:
                    objArr[i] = Float.valueOf(cursor.getFloat(entry.getValue().projectionIndex));
                    break;
                case 3:
                    objArr[i] = cursor.getString(entry.getValue().projectionIndex);
                    break;
                case 4:
                    objArr[i] = cursor.getBlob(entry.getValue().projectionIndex);
                    break;
                default:
                    objArr[i] = null;
                    break;
            }
            i++;
        }
    }

    public Object getImageField(String str) {
        if (this.mImageFieldValues != null && sImageFields.containsKey(str)) {
            return this.mImageFieldValues[sImageFields.get(str).valueIndex];
        }
        return null;
    }

    public Object getVideoField(String str) {
        if (this.mVideoFieldValues != null && sVideoFields.containsKey(str)) {
            return this.mVideoFieldValues[sVideoFields.get(str).valueIndex];
        }
        return null;
    }

    private static class Position {
        public int projectionIndex;
        public int valueIndex;

        private Position() {
        }
    }
}
