package android.support.v4.widget;

import android.content.Context;
import android.view.View;
import android.widget.SearchView;
import com.android.common.speech.LoggingEvents;

class SearchViewCompatIcs {
    SearchViewCompatIcs() {
    }

    public static class MySearchView extends SearchView {
        public MySearchView(Context context) {
            super(context);
        }

        @Override
        public void onActionViewCollapsed() {
            setQuery(LoggingEvents.EXTRA_CALLING_APP_NAME, false);
            super.onActionViewCollapsed();
        }
    }

    public static View newSearchView(Context context) {
        return new MySearchView(context);
    }

    public static void setImeOptions(View searchView, int imeOptions) {
        ((SearchView) searchView).setImeOptions(imeOptions);
    }

    public static void setInputType(View searchView, int inputType) {
        ((SearchView) searchView).setInputType(inputType);
    }
}
