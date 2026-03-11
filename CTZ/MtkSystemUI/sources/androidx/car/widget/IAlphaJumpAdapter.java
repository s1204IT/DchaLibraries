package androidx.car.widget;

import java.util.Collection;

public interface IAlphaJumpAdapter {

    public interface Bucket {
        int getIndex();

        CharSequence getLabel();

        boolean isEmpty();
    }

    Collection<Bucket> getAlphaJumpBuckets();

    void onAlphaJumpEnter();

    void onAlphaJumpLeave(Bucket bucket);
}
