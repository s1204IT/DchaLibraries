package android.test.mock;

import android.app.Application;
import android.content.res.Configuration;

@Deprecated
public class MockApplication extends Application {
    @Override
    public void onCreate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onTerminate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        throw new UnsupportedOperationException();
    }
}
