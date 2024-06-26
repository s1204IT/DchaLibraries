package android.support.v4.app;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
/* loaded from: a.zip:android/support/v4/app/BaseFragmentActivityHoneycomb.class */
abstract class BaseFragmentActivityHoneycomb extends BaseFragmentActivityEclair {
    @Override // android.app.Activity, android.view.LayoutInflater.Factory2
    public View onCreateView(View view, String str, Context context, AttributeSet attributeSet) {
        View dispatchFragmentsOnCreateView = dispatchFragmentsOnCreateView(view, str, context, attributeSet);
        return (dispatchFragmentsOnCreateView != null || Build.VERSION.SDK_INT < 11) ? dispatchFragmentsOnCreateView : super.onCreateView(view, str, context, attributeSet);
    }
}
