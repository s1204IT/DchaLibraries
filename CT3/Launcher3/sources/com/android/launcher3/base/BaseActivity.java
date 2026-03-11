package com.android.launcher3.base;

import android.app.Activity;
import android.content.Context;

public class BaseActivity extends Activity {
    public Context getContext() {
        return this;
    }

    public Activity getActivity() {
        return this;
    }
}
