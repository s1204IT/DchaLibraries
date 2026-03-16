package junit.runner;

import java.util.Vector;

public class Sorter {

    public interface Swapper {
        void swap(Vector vector, int i, int i2);
    }

    public static void sortStrings(Vector values, int left, int right, Swapper swapper) {
        String mid = (String) values.elementAt((left + right) / 2);
        while (true) {
            if (((String) values.elementAt(left)).compareTo(mid) < 0) {
                left++;
            } else {
                while (mid.compareTo((String) values.elementAt(right)) < 0) {
                    right--;
                }
                if (left <= right) {
                    swapper.swap(values, left, right);
                    left++;
                    right--;
                }
                if (left > right) {
                    break;
                }
            }
        }
        if (left < right) {
            sortStrings(values, left, right, swapper);
        }
        if (left < right) {
            sortStrings(values, left, right, swapper);
        }
    }
}
