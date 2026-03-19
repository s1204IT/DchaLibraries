package com.mediatek.galleryfeature.pq.dcfilter;

import com.mediatek.gallerybasic.util.Log;
import com.mediatek.galleryfeature.pq.filter.FilterInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DCFilter implements FilterInterface {
    public static final String CURRRENT_INDEX = "textViewCurrentIndex";
    public static final String MIN_VALUE = "textViewMinValue";
    public static final String RANGE = "textViewMaxValue";
    public static final String SEEKBAR_PROGRESS = "seekbarProgress";
    public static final String TAG = "MtkGallery2/DCFilter";
    protected int mCurrentIndex;
    protected int mDefaultIndex;
    protected String mName;
    protected int mRange;
    public Map<String, String> map = new HashMap();

    protected static native boolean nativeSetTuningMode(int i);

    protected native int nativeGetAdaptiveBlackEffectIndex();

    protected native int nativeGetAdaptiveBlackEffectRange();

    protected native int nativeGetAdaptiveWhiteEffectIndex();

    protected native int nativeGetAdaptiveWhiteEffectRange();

    protected native int nativeGetBlackEffectEnableIndex();

    protected native int nativeGetBlackEffectEnableRange();

    protected native int nativeGetBlackEffectLevelIndex();

    protected native int nativeGetBlackEffectLevelRange();

    protected native int nativeGetBlackEffectParam1Index();

    protected native int nativeGetBlackEffectParam1Range();

    protected native int nativeGetBlackEffectParam2Index();

    protected native int nativeGetBlackEffectParam2Range();

    protected native int nativeGetBlackEffectParam3Index();

    protected native int nativeGetBlackEffectParam3Range();

    protected native int nativeGetBlackEffectParam4Index();

    protected native int nativeGetBlackEffectParam4Range();

    protected native int nativeGetBlackRegionGain1Index();

    protected native int nativeGetBlackRegionGain1Range();

    protected native int nativeGetBlackRegionGain2Index();

    protected native int nativeGetBlackRegionGain2Range();

    protected native int nativeGetBlackRegionRangeIndex();

    protected native int nativeGetBlackRegionRangeRange();

    protected native int nativeGetContentSmooth1Index();

    protected native int nativeGetContentSmooth1Range();

    protected native int nativeGetContentSmooth2Index();

    protected native int nativeGetContentSmooth2Range();

    protected native int nativeGetContentSmooth3Index();

    protected native int nativeGetContentSmooth3Range();

    protected native int nativeGetContrastAdjust1Index();

    protected native int nativeGetContrastAdjust1Range();

    protected native int nativeGetContrastAdjust2Index();

    protected native int nativeGetContrastAdjust2Range();

    protected native int nativeGetDCChangeSpeedLevel2Index();

    protected native int nativeGetDCChangeSpeedLevel2Range();

    protected native int nativeGetDCChangeSpeedLevelIndex();

    protected native int nativeGetDCChangeSpeedLevelRange();

    protected native int nativeGetMiddleRegionGain1Index();

    protected native int nativeGetMiddleRegionGain1Range();

    protected native int nativeGetMiddleRegionGain2Index();

    protected native int nativeGetMiddleRegionGain2Range();

    protected native int nativeGetProtectRegionEffectIndex();

    protected native int nativeGetProtectRegionEffectRange();

    protected native int nativeGetProtectRegionWeightIndex();

    protected native int nativeGetProtectRegionWeightRange();

    protected native int nativeGetScenceChangeControlEnIndex();

    protected native int nativeGetScenceChangeControlEnRange();

    protected native int nativeGetScenceChangeControlIndex();

    protected native int nativeGetScenceChangeControlRange();

    protected native int nativeGetScenceChangeOnceEnIndex();

    protected native int nativeGetScenceChangeOnceEnRange();

    protected native int nativeGetScenceChangeTh1Index();

    protected native int nativeGetScenceChangeTh1Range();

    protected native int nativeGetScenceChangeTh2Index();

    protected native int nativeGetScenceChangeTh2Range();

    protected native int nativeGetScenceChangeTh3Index();

    protected native int nativeGetScenceChangeTh3Range();

    protected native int nativeGetStrongBlackEffectIndex();

    protected native int nativeGetStrongBlackEffectRange();

    protected native int nativeGetStrongWhiteEffectIndex();

    protected native int nativeGetStrongWhiteEffectRange();

    protected native int nativeGetWhiteEffectEnableIndex();

    protected native int nativeGetWhiteEffectEnableRange();

    protected native int nativeGetWhiteEffectLevelIndex();

    protected native int nativeGetWhiteEffectLevelRange();

    protected native int nativeGetWhiteEffectParam1Index();

    protected native int nativeGetWhiteEffectParam1Range();

    protected native int nativeGetWhiteEffectParam2Index();

    protected native int nativeGetWhiteEffectParam2Range();

    protected native int nativeGetWhiteEffectParam3Index();

    protected native int nativeGetWhiteEffectParam3Range();

    protected native int nativeGetWhiteEffectParam4Index();

    protected native int nativeGetWhiteEffectParam4Range();

    protected native int nativeGetWhiteRegionGain1Index();

    protected native int nativeGetWhiteRegionGain1Range();

    protected native int nativeGetWhiteRegionGain2Index();

    protected native int nativeGetWhiteRegionGain2Range();

    protected native int nativeGetWhiteRegionRangeIndex();

    protected native int nativeGetWhiteRegionRangeRange();

    protected native boolean nativeSetAdaptiveBlackEffectIndex(int i);

    protected native boolean nativeSetAdaptiveWhiteEffectIndex(int i);

    protected native boolean nativeSetBlackEffectEnableIndex(int i);

    protected native boolean nativeSetBlackEffectLevelIndex(int i);

    protected native boolean nativeSetBlackEffectParam1Index(int i);

    protected native boolean nativeSetBlackEffectParam2Index(int i);

    protected native boolean nativeSetBlackEffectParam3Index(int i);

    protected native boolean nativeSetBlackEffectParam4Index(int i);

    protected native boolean nativeSetBlackRegionGain1Index(int i);

    protected native boolean nativeSetBlackRegionGain2Index(int i);

    protected native boolean nativeSetBlackRegionRangeIndex(int i);

    protected native boolean nativeSetContentSmooth1Index(int i);

    protected native boolean nativeSetContentSmooth2Index(int i);

    protected native boolean nativeSetContentSmooth3Index(int i);

    protected native boolean nativeSetContrastAdjust1Index(int i);

    protected native boolean nativeSetContrastAdjust2Index(int i);

    protected native boolean nativeSetDCChangeSpeedLevel2Index(int i);

    protected native boolean nativeSetDCChangeSpeedLevelIndex(int i);

    protected native boolean nativeSetMiddleRegionGain1Index(int i);

    protected native boolean nativeSetMiddleRegionGain2Index(int i);

    protected native boolean nativeSetProtectRegionEffectIndex(int i);

    protected native boolean nativeSetProtectRegionWeightIndex(int i);

    protected native boolean nativeSetScenceChangeControlEnIndex(int i);

    protected native boolean nativeSetScenceChangeControlIndex(int i);

    protected native boolean nativeSetScenceChangeOnceEnIndex(int i);

    protected native boolean nativeSetScenceChangeTh1Index(int i);

    protected native boolean nativeSetScenceChangeTh2Index(int i);

    protected native boolean nativeSetScenceChangeTh3Index(int i);

    protected native boolean nativeSetStrongBlackEffectIndex(int i);

    protected native boolean nativeSetStrongWhiteEffectIndex(int i);

    protected native boolean nativeSetWhiteEffectEnableIndex(int i);

    protected native boolean nativeSetWhiteEffectLevelIndex(int i);

    protected native boolean nativeSetWhiteEffectParam1Index(int i);

    protected native boolean nativeSetWhiteEffectParam2Index(int i);

    protected native boolean nativeSetWhiteEffectParam3Index(int i);

    protected native boolean nativeSetWhiteEffectParam4Index(int i);

    protected native boolean nativeSetWhiteRegionGain1Index(int i);

    protected native boolean nativeSetWhiteRegionGain2Index(int i);

    protected native boolean nativeSetWhiteRegionRangeIndex(int i);

    static {
        System.loadLibrary("PQDCjni");
    }

    public Map<String, String> getDate() {
        return this.map;
    }

    public DCFilter(String str) {
        this.mName = str;
        initFilter();
    }

    public DCFilter() {
    }

    protected void initFilter() {
        init();
        this.map.put("textViewMinValue", getMinValue());
        this.map.put("textViewMaxValue", getMaxValue());
        this.map.put("textViewCurrentIndex", getCurrentValue());
        this.map.put("seekbarProgress", getSeekbarProgressValue() + "");
        Log.d(TAG, "<initFilter>Create [" + getClass().getName() + " ]: MIN_VALUE=" + getMinValue() + " RANGE=" + getMaxValue() + " CURRRENT_INDEX=" + getCurrentValue() + "  SEEKBAR_PROGRESS=" + getSeekbarProgressValue());
    }

    protected boolean addToList(ArrayList<FilterInterface> arrayList) {
        Log.d(TAG, "<addToList>this " + getClass().getName() + " Integer.parseInt(getMaxValue()=" + Integer.parseInt(getMaxValue()));
        if (Integer.parseInt(getMaxValue()) > 0) {
            arrayList.add(this);
            Log.d(TAG, "<addToList>:::" + getClass().getName() + " has alread addToList! ");
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "<onResume>: nativeSetTuningMode(1)");
        nativeSetTuningMode(1);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public ArrayList<FilterInterface> getFilterList() {
        ArrayList<FilterInterface> arrayList = new ArrayList<>();
        new DCFilterBlackEffectEnable("BlackEffectEnable").addToList(arrayList);
        new DCFilterWhiteEffectEnable("WhiteEffectEnable").addToList(arrayList);
        new DCFilterStrongBlackEffect("StrongBlackEffect").addToList(arrayList);
        new DCFilterStrongWhiteEffect("StrongWhiteEffect").addToList(arrayList);
        new DCFilterAdaptiveBlackEffect("AdaptiveBlackEffect").addToList(arrayList);
        new DCFilterAdaptiveWhiteEffect("AdaptiveWhiteEffect").addToList(arrayList);
        new DCFilterScenceChangeOnceEn("ScenceChangeOnceEn").addToList(arrayList);
        new DCFilterScenceChangeControlEn("ScenceChangeControlEn").addToList(arrayList);
        new DCFilterScenceChangeControl("ScenceChangeControl").addToList(arrayList);
        new DCFilterScenceChangeTh1("ScenceChangeTh1").addToList(arrayList);
        new DCFilterScenceChangeTh2("ScenceChangeTh2").addToList(arrayList);
        new DCFilterScenceChangeTh3("ScenceChangeTh3").addToList(arrayList);
        new DCFilterContentSmooth1("ContentSmooth1").addToList(arrayList);
        new DCFilterContentSmooth2("ContentSmooth2").addToList(arrayList);
        new DCFilterContentSmooth3("ContentSmooth3").addToList(arrayList);
        new DCFilterMiddleRegionGain1("MiddleRegionGain1").addToList(arrayList);
        new DCFilterMiddleRegionGain2("MiddleRegionGain").addToList(arrayList);
        new DCFilterBlackRegionGain1("BlackRegionGain1").addToList(arrayList);
        new DCFilterBlackRegionGain2("BlackRegionGain").addToList(arrayList);
        new DCFilterBlackRegionRange("BlackRegionRange").addToList(arrayList);
        new DCFilterBlackEffectLevel("BlackEffectLevel").addToList(arrayList);
        new DCFilterBlackEffectParam1("BlackEffectParam1").addToList(arrayList);
        new DCFilterBlackEffectParam2("BlackEffectParam2").addToList(arrayList);
        new DCFilterBlackEffectParam3("BlackEffectParam3").addToList(arrayList);
        new DCFilterBlackEffectParam4("BlackEffectParam4").addToList(arrayList);
        new DCFilterWhiteRegionGain1("WhiteRegionGain1").addToList(arrayList);
        new DCFilterWhiteRegionGain2("WhiteRegionGain").addToList(arrayList);
        new DCFilterWhiteRegionRange("WhiteRegionRange").addToList(arrayList);
        new DCFilterWhiteEffectLevel("WhiteEffectLevel").addToList(arrayList);
        new DCFilterWhiteEffectParam1("WhiteEffectParam1").addToList(arrayList);
        new DCFilterWhiteEffectParam2("WhiteEffectParam2").addToList(arrayList);
        new DCFilterWhiteEffectParam3("WhiteEffectParam3").addToList(arrayList);
        new DCFilterWhiteEffectParam4("WhiteEffectParam4").addToList(arrayList);
        new DCFilterContrastAdjust1("ContrastAdjust1").addToList(arrayList);
        new DCFilterContrastAdjust2("ContrastAdjust2").addToList(arrayList);
        new DCFilterDCChangeSpeedLevel("DCChangeSpeedLevel").addToList(arrayList);
        new DCFilterProtectRegionEffect("ProtectRegionEffect").addToList(arrayList);
        new DCFilterDCChangeSpeedLevel2("DCChangeSpeedLevel2").addToList(arrayList);
        new DCFilterProtectRegionWeight("ProtectRegionWeight").addToList(arrayList);
        return arrayList;
    }

    @Override
    public String getCurrentValue() {
        return getName() + Integer.toString(this.mCurrentIndex);
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
    public int getDefaultIndex() {
        return this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
    }

    @Override
    public int getCurrentIndex() {
        return this.mCurrentIndex;
    }

    @Override
    public int getRange() {
        return this.mRange;
    }

    protected String getName() {
        return this.mName + ":  ";
    }
}
