package android.app;

import android.view.View;

interface FragmentContainer {
    View findViewById(int i);

    boolean hasView();
}
