package android.support.v17.leanback.widget;

import android.view.View;

public interface GuidedActionAutofillSupport {

    public interface OnAutofillListener {
        void onAutofill(View view);
    }

    void setOnAutofillListener(OnAutofillListener onAutofillListener);
}
