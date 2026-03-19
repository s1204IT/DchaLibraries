package sun.security.x509;

import java.util.Comparator;

class AVAComparator implements Comparator<AVA> {
    private static final Comparator<AVA> INSTANCE = new AVAComparator();

    private AVAComparator() {
    }

    static Comparator<AVA> getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(AVA ava, AVA ava2) {
        boolean zHasRFC2253Keyword = ava.hasRFC2253Keyword();
        boolean zHasRFC2253Keyword2 = ava2.hasRFC2253Keyword();
        if (zHasRFC2253Keyword) {
            if (zHasRFC2253Keyword2) {
                return ava.toRFC2253CanonicalString().compareTo(ava2.toRFC2253CanonicalString());
            }
            return -1;
        }
        if (zHasRFC2253Keyword2) {
            return 1;
        }
        int[] intArray = ava.getObjectIdentifier().toIntArray();
        int[] intArray2 = ava2.getObjectIdentifier().toIntArray();
        int i = 0;
        int length = intArray.length > intArray2.length ? intArray2.length : intArray.length;
        while (i < length && intArray[i] == intArray2[i]) {
            i++;
        }
        return i == length ? intArray.length - intArray2.length : intArray[i] - intArray2[i];
    }
}
