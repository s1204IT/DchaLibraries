package android.ext.services.resolver;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.os.storage.StorageManager;
import android.service.resolver.ResolverRankerService;
import android.service.resolver.ResolverTarget;
import android.util.ArrayMap;
import android.util.Log;
import java.io.File;
import java.util.List;

public final class LRResolverRankerService extends ResolverRankerService {
    private float mBias;
    private ArrayMap<String, Float> mFeatureWeights;
    private SharedPreferences mParamSharedPref;

    public IBinder onBind(Intent intent) {
        initModel();
        return super.onBind(intent);
    }

    public void onPredictSharingProbabilities(List<ResolverTarget> list) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            ResolverTarget resolverTarget = list.get(i);
            resolverTarget.setSelectProbability(predict(getFeatures(resolverTarget)));
        }
    }

    public void onTrainRankingModel(List<ResolverTarget> list, int i) {
        int size = list.size();
        if (i < 0 || i >= size) {
            return;
        }
        ArrayMap<String, Float> features = getFeatures(list.get(i));
        float selectProbability = list.get(i).getSelectProbability();
        int size2 = list.size();
        for (int i2 = 0; i2 < size2; i2++) {
            if (i2 != i) {
                ArrayMap<String, Float> features2 = getFeatures(list.get(i2));
                float selectProbability2 = list.get(i2).getSelectProbability();
                if (selectProbability2 > selectProbability) {
                    update(features2, selectProbability2, false);
                    update(features, selectProbability, true);
                }
            }
        }
        commitUpdate();
    }

    private void initModel() {
        this.mParamSharedPref = getParamSharedPref();
        this.mFeatureWeights = new ArrayMap<>(4);
        if (this.mParamSharedPref == null || this.mParamSharedPref.getInt("version", 0) < 1) {
            this.mBias = -1.6568f;
            this.mFeatureWeights.put("launch", Float.valueOf(2.5543f));
            this.mFeatureWeights.put("timeSpent", Float.valueOf(2.8412f));
            this.mFeatureWeights.put("recency", Float.valueOf(0.269f));
            this.mFeatureWeights.put("chooser", Float.valueOf(4.2222f));
            return;
        }
        this.mBias = this.mParamSharedPref.getFloat("bias", 0.0f);
        this.mFeatureWeights.put("launch", Float.valueOf(this.mParamSharedPref.getFloat("launch", 0.0f)));
        this.mFeatureWeights.put("timeSpent", Float.valueOf(this.mParamSharedPref.getFloat("timeSpent", 0.0f)));
        this.mFeatureWeights.put("recency", Float.valueOf(this.mParamSharedPref.getFloat("recency", 0.0f)));
        this.mFeatureWeights.put("chooser", Float.valueOf(this.mParamSharedPref.getFloat("chooser", 0.0f)));
    }

    private ArrayMap<String, Float> getFeatures(ResolverTarget resolverTarget) {
        ArrayMap<String, Float> arrayMap = new ArrayMap<>(4);
        arrayMap.put("recency", Float.valueOf(resolverTarget.getRecencyScore()));
        arrayMap.put("timeSpent", Float.valueOf(resolverTarget.getTimeSpentScore()));
        arrayMap.put("launch", Float.valueOf(resolverTarget.getLaunchScore()));
        arrayMap.put("chooser", Float.valueOf(resolverTarget.getChooserScore()));
        return arrayMap;
    }

    private float predict(ArrayMap<String, Float> arrayMap) {
        if (arrayMap == null) {
            return 0.0f;
        }
        int size = arrayMap.size();
        float fFloatValue = 0.0f;
        for (int i = 0; i < size; i++) {
            fFloatValue += this.mFeatureWeights.getOrDefault(arrayMap.keyAt(i), Float.valueOf(0.0f)).floatValue() * arrayMap.valueAt(i).floatValue();
        }
        return (float) (1.0d / (Math.exp((-this.mBias) - fFloatValue) + 1.0d));
    }

    private void update(ArrayMap<String, Float> arrayMap, float f, boolean z) {
        if (arrayMap == null) {
            return;
        }
        int size = arrayMap.size();
        float f2 = z ? 1.0f - f : -f;
        for (int i = 0; i < size; i++) {
            String strKeyAt = arrayMap.keyAt(i);
            float fFloatValue = this.mFeatureWeights.getOrDefault(strKeyAt, Float.valueOf(0.0f)).floatValue();
            float f3 = 1.0E-4f * f2;
            this.mBias += f3;
            this.mFeatureWeights.put(strKeyAt, Float.valueOf((fFloatValue - (9.999999E-9f * fFloatValue)) + (f3 * arrayMap.valueAt(i).floatValue())));
        }
    }

    private void commitUpdate() {
        try {
            SharedPreferences.Editor editorEdit = this.mParamSharedPref.edit();
            editorEdit.putFloat("bias", this.mBias);
            int size = this.mFeatureWeights.size();
            for (int i = 0; i < size; i++) {
                editorEdit.putFloat(this.mFeatureWeights.keyAt(i), this.mFeatureWeights.valueAt(i).floatValue());
            }
            editorEdit.putInt("version", 1);
            editorEdit.apply();
        } catch (Exception e) {
            Log.e("LRResolverRankerService", "Failed to commit update" + e);
        }
    }

    private SharedPreferences getParamSharedPref() {
        return getSharedPreferences(new File(new File(Environment.getDataUserCePackageDirectory(StorageManager.UUID_PRIVATE_INTERNAL, getUserId(), getPackageName()), "shared_prefs"), "resolver_ranker_params.xml"), 0);
    }
}
