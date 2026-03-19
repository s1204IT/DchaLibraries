package android.gesture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

class InstanceLearner extends Learner {
    private static final Comparator<Prediction> sComparator = new Comparator<Prediction>() {
        @Override
        public int compare(Prediction prediction, Prediction prediction2) {
            double d = prediction.score;
            double d2 = prediction2.score;
            if (d > d2) {
                return -1;
            }
            if (d < d2) {
                return 1;
            }
            return 0;
        }
    };

    InstanceLearner() {
    }

    @Override
    ArrayList<Prediction> classify(int i, int i2, float[] fArr) {
        double dSquaredEuclideanDistance;
        double d;
        ArrayList<Prediction> arrayList = new ArrayList<>();
        ArrayList<Instance> instances = getInstances();
        int size = instances.size();
        TreeMap treeMap = new TreeMap();
        for (int i3 = 0; i3 < size; i3++) {
            Instance instance = instances.get(i3);
            if (instance.vector.length == fArr.length) {
                if (i == 2) {
                    dSquaredEuclideanDistance = GestureUtils.minimumCosineDistance(instance.vector, fArr, i2);
                } else {
                    dSquaredEuclideanDistance = GestureUtils.squaredEuclideanDistance(instance.vector, fArr);
                }
                if (dSquaredEuclideanDistance == 0.0d) {
                    d = Double.MAX_VALUE;
                } else {
                    d = 1.0d / dSquaredEuclideanDistance;
                }
                Double d2 = (Double) treeMap.get(instance.label);
                if (d2 == null || d > d2.doubleValue()) {
                    treeMap.put(instance.label, Double.valueOf(d));
                }
            }
        }
        for (String str : treeMap.keySet()) {
            arrayList.add(new Prediction(str, ((Double) treeMap.get(str)).doubleValue()));
        }
        Collections.sort(arrayList, sComparator);
        return arrayList;
    }
}
