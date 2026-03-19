package com.mediatek.galleryfeature.pq.filter;

import com.mediatek.gallerybasic.util.Log;
import com.mediatek.galleryportable.SystemPropertyUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Filter implements FilterInterface {
    public static final String CURRRENT_INDEX = "textViewCurrentIndex";
    public static final String GLOBAL_PQ_PROPERTY = "ro.globalpq.support";
    public static final boolean IS_GLOBALPQ_SUPPORT = SystemPropertyUtils.get(GLOBAL_PQ_PROPERTY).equals("1");
    public static final String MIN_VALUE = "textViewMinValue";
    public static final String RANGE = "textViewMaxValue";
    public static final String SEEKBAR_PROGRESS = "seekbarProgress";
    public static final String TAG = "MtkGallery2/Filter";
    protected int mCurrentIndex;
    protected int mDefaultIndex;
    protected int mRange;
    public Map<String, String> map = new HashMap();

    protected native int nativeGetContrastAdjIndex();

    protected native int nativeGetContrastAdjRange();

    protected native int nativeGetGrassToneHIndex();

    protected native int nativeGetGrassToneHRange();

    protected native int nativeGetGrassToneSIndex();

    protected native int nativeGetGrassToneSRange();

    protected native int nativeGetHueAdjIndex();

    protected native int nativeGetHueAdjRange();

    protected native int nativeGetSatAdjIndex();

    protected native int nativeGetSatAdjRange();

    protected native int nativeGetSharpAdjIndex();

    protected native int nativeGetSharpAdjRange();

    protected native int nativeGetSkinToneHIndex();

    protected native int nativeGetSkinToneHRange();

    protected native int nativeGetSkinToneSIndex();

    protected native int nativeGetSkinToneSRange();

    protected native int nativeGetSkyToneHIndex();

    protected native int nativeGetSkyToneHRange();

    protected native int nativeGetSkyToneSIndex();

    protected native int nativeGetSkyToneSRange();

    protected native int nativeGetXAxisIndex();

    protected native int nativeGetXAxisRange();

    protected native int nativeGetYAxisIndex();

    protected native int nativeGetYAxisRange();

    protected native boolean nativeSetContrastAdjIndex(int i);

    protected native boolean nativeSetGrassToneHIndex(int i);

    protected native boolean nativeSetGrassToneSIndex(int i);

    protected native boolean nativeSetHueAdjIndex(int i);

    protected native boolean nativeSetSatAdjIndex(int i);

    protected native boolean nativeSetSharpAdjIndex(int i);

    protected native boolean nativeSetSkinToneHIndex(int i);

    protected native boolean nativeSetSkinToneSIndex(int i);

    protected native boolean nativeSetSkyToneHIndex(int i);

    protected native boolean nativeSetSkyToneSIndex(int i);

    protected native boolean nativeSetXAxisIndex(int i);

    protected native boolean nativeSetYAxisIndex(int i);

    static {
        System.loadLibrary("PQjni");
    }

    public Map<String, String> getDate() {
        return this.map;
    }

    public Filter() {
        init();
        this.map.put("textViewMinValue", getMinValue());
        this.map.put("textViewMaxValue", getMaxValue());
        this.map.put("textViewCurrentIndex", getCurrentValue());
        this.map.put("seekbarProgress", getSeekbarProgressValue());
        Log.d(TAG, "<Filter> Create [" + getClass().getName() + " ]: MIN_VALUE=" + getMinValue() + " RANGE=" + getMaxValue() + " CURRRENT_INDEX=" + getCurrentValue() + "  SEEKBAR_PROGRESS=" + getSeekbarProgressValue());
    }

    public boolean addToList(ArrayList<FilterInterface> arrayList) {
        if (Integer.parseInt(getMaxValue()) > 0) {
            arrayList.add(this);
            Log.d(TAG, "<addToList>:::" + getClass().getName() + " has alread addToList! ");
            return true;
        }
        return false;
    }

    @Override
    public ArrayList<FilterInterface> getFilterList() {
        ArrayList<FilterInterface> arrayList = new ArrayList<>();
        if (!IS_GLOBALPQ_SUPPORT) {
            new FilterSharpAdj().addToList(arrayList);
        }
        new FilterSatAdj().addToList(arrayList);
        new FilterHueAdj().addToList(arrayList);
        new FilterSkinToneH().addToList(arrayList);
        new FilterSkinToneS().addToList(arrayList);
        new FilterSkyToneH().addToList(arrayList);
        new FilterSkyToneS().addToList(arrayList);
        new FilterGetXAxis().addToList(arrayList);
        new FilterGetYAxis().addToList(arrayList);
        new FilterGrassToneH().addToList(arrayList);
        new FilterGrassToneS().addToList(arrayList);
        new FilterContrastAdj().addToList(arrayList);
        return arrayList;
    }

    @Override
    public String getCurrentValue() {
        return Integer.toString(this.mCurrentIndex);
    }

    @Override
    public String getMaxValue() {
        return Integer.toString(this.mRange - 1);
    }

    @Override
    public String getMinValue() {
        return "0";
    }

    @Override
    public String getSeekbarProgressValue() {
        return Integer.toString(this.mCurrentIndex);
    }

    @Override
    public void init() {
    }

    @Override
    public void setCurrentIndex(int i) {
        this.mCurrentIndex = i;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public int getCurrentIndex() {
        return this.mCurrentIndex;
    }

    @Override
    public int getRange() {
        return this.mRange;
    }

    @Override
    public int getDefaultIndex() {
        return this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
    }
}
