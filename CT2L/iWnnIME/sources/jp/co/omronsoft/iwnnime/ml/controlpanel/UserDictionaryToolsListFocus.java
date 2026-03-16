package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.content.Context;
import android.widget.TextView;

public class UserDictionaryToolsListFocus extends TextView {
    private UserDictionaryToolsListFocus mPairView;

    public UserDictionaryToolsListFocus(Context context) {
        super(context);
        this.mPairView = null;
    }

    public UserDictionaryToolsListFocus getPairView() {
        return this.mPairView;
    }

    public void setPairView(UserDictionaryToolsListFocus pairView) {
        this.mPairView = pairView;
    }
}
