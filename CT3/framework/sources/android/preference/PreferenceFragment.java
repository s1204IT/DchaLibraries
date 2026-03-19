package android.preference;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SuperNotCalledException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.android.internal.R;

public abstract class PreferenceFragment extends Fragment implements PreferenceManager.OnPreferenceTreeClickListener {
    private static final boolean DBG = "eng".equals(Build.TYPE);
    private static final int FIRST_REQUEST_CODE = 100;
    private static final int MSG_BIND_PREFERENCES = 1;
    private static final String PREFERENCES_TAG = "android:preferences";
    private static final String TAG = "PreferenceFragment";
    private boolean mHavePrefs;
    private boolean mInitDone;
    private ListView mList;
    private PreferenceManager mPreferenceManager;
    private int mLayoutResId = 17367222;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PreferenceFragment.this.bindPreferences();
                    break;
            }
        }
    };
    private final Runnable mRequestFocus = new Runnable() {
        @Override
        public void run() {
            PreferenceFragment.this.mList.focusableViewAvailable(PreferenceFragment.this.mList);
        }
    };
    private View.OnKeyListener mListOnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (PreferenceFragment.this.mList == null) {
                return false;
            }
            Object selectedItem = PreferenceFragment.this.mList.getSelectedItem();
            if (!(selectedItem instanceof Preference)) {
                return false;
            }
            View selectedView = PreferenceFragment.this.mList.getSelectedView();
            return ((Preference) selectedItem).onKey(selectedView, keyCode, event);
        }
    };

    public interface OnPreferenceStartFragmentCallback {
        boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) throws SuperNotCalledException {
        super.onCreate(savedInstanceState);
        if (DBG) {
            Log.d(TAG, "onCreate, this = " + this);
        }
        this.mPreferenceManager = new PreferenceManager(getActivity(), 100);
        this.mPreferenceManager.setFragment(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (DBG) {
            Log.d(TAG, "onCreateView, this = " + this);
        }
        TypedArray a = getActivity().obtainStyledAttributes(null, R.styleable.PreferenceFragment, android.R.attr.preferenceFragmentStyle, 0);
        this.mLayoutResId = a.getResourceId(0, this.mLayoutResId);
        a.recycle();
        return inflater.inflate(this.mLayoutResId, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Drawable divider;
        super.onViewCreated(view, savedInstanceState);
        TypedArray a = getActivity().obtainStyledAttributes(null, R.styleable.PreferenceFragment, android.R.attr.preferenceFragmentStyle, 0);
        ListView lv = (ListView) view.findViewById(android.R.id.list);
        if (lv != null && (divider = a.getDrawable(1)) != null) {
            lv.setDivider(divider);
        }
        a.recycle();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Bundle container;
        PreferenceScreen preferenceScreen;
        super.onActivityCreated(savedInstanceState);
        if (DBG) {
            Log.d(TAG, "onActivityCreated, mHavePrefs = " + this.mHavePrefs + ", this = " + this);
        }
        if (this.mHavePrefs) {
            bindPreferences();
        }
        this.mInitDone = true;
        if (savedInstanceState == null || (container = savedInstanceState.getBundle(PREFERENCES_TAG)) == null || (preferenceScreen = getPreferenceScreen()) == null) {
            return;
        }
        preferenceScreen.restoreHierarchyState(container);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DBG) {
            Log.d(TAG, "onStart, this = " + this);
        }
        this.mPreferenceManager.setOnPreferenceTreeClickListener(this);
    }

    @Override
    public void onStop() {
        if (DBG) {
            Log.d(TAG, "onStop, this = " + this);
        }
        super.onStop();
        this.mPreferenceManager.dispatchActivityStop();
        this.mPreferenceManager.setOnPreferenceTreeClickListener(null);
    }

    @Override
    public void onDestroyView() {
        if (this.mList != null) {
            this.mList.setOnKeyListener(null);
        }
        if (DBG) {
            Log.d(TAG, "onDestroyView, this = " + this);
        }
        this.mList = null;
        this.mHandler.removeCallbacks(this.mRequestFocus);
        this.mHandler.removeMessages(1);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (DBG) {
            Log.d(TAG, "onDestroy, this = " + this);
        }
        super.onDestroy();
        this.mPreferenceManager.dispatchActivityDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DBG) {
            Log.d(TAG, "onSaveInstanceState, this = " + this);
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) {
            return;
        }
        Bundle container = new Bundle();
        preferenceScreen.saveHierarchyState(container);
        outState.putBundle(PREFERENCES_TAG, container);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.mPreferenceManager.dispatchActivityResult(requestCode, resultCode, data);
    }

    public PreferenceManager getPreferenceManager() {
        return this.mPreferenceManager;
    }

    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (!this.mPreferenceManager.setPreferences(preferenceScreen) || preferenceScreen == null) {
            return;
        }
        onUnbindPreferences();
        this.mHavePrefs = true;
        if (!this.mInitDone) {
            return;
        }
        postBindPreferences();
    }

    public PreferenceScreen getPreferenceScreen() {
        return this.mPreferenceManager.getPreferenceScreen();
    }

    public void addPreferencesFromIntent(Intent intent) {
        if (DBG) {
            Log.d(TAG, "addPreferencesFromIntent, intent = " + intent + ", this = " + this);
        }
        requirePreferenceManager();
        setPreferenceScreen(this.mPreferenceManager.inflateFromIntent(intent, getPreferenceScreen()));
    }

    public void addPreferencesFromResource(int preferencesResId) {
        if (DBG) {
            Log.d(TAG, "addPreferencesFromResource, resId = " + preferencesResId + ", this = " + this);
        }
        requirePreferenceManager();
        setPreferenceScreen(this.mPreferenceManager.inflateFromResource(getActivity(), preferencesResId, getPreferenceScreen()));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getFragment() != null && (getActivity() instanceof OnPreferenceStartFragmentCallback)) {
            return ((OnPreferenceStartFragmentCallback) getActivity()).onPreferenceStartFragment(this, preference);
        }
        return false;
    }

    public Preference findPreference(CharSequence key) {
        if (this.mPreferenceManager == null) {
            return null;
        }
        return this.mPreferenceManager.findPreference(key);
    }

    private void requirePreferenceManager() {
        if (this.mPreferenceManager != null) {
        } else {
            throw new RuntimeException("This should be called after super.onCreate.");
        }
    }

    private void postBindPreferences() {
        if (DBG) {
            Log.d(TAG, "postBindPreferences, this = " + this);
        }
        if (this.mHandler.hasMessages(1)) {
            return;
        }
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    private void bindPreferences() {
        if (DBG) {
            Log.d(TAG, "bindPreferences, this = " + this);
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.bind(getListView());
        }
        onBindPreferences();
    }

    protected void onBindPreferences() {
    }

    protected void onUnbindPreferences() {
    }

    public ListView getListView() {
        ensureList();
        return this.mList;
    }

    public boolean hasListView() {
        if (this.mList != null) {
            return true;
        }
        View root = getView();
        if (root == null) {
            return false;
        }
        View rawListView = root.findViewById(android.R.id.list);
        if (!(rawListView instanceof ListView)) {
            return false;
        }
        this.mList = (ListView) rawListView;
        return this.mList != null;
    }

    private void ensureList() {
        if (this.mList != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        View rawListView = root.findViewById(android.R.id.list);
        if (!(rawListView instanceof ListView)) {
            throw new RuntimeException("Content has view with id attribute 'android.R.id.list' that is not a ListView class");
        }
        this.mList = (ListView) rawListView;
        if (this.mList == null) {
            throw new RuntimeException("Your content must have a ListView whose id attribute is 'android.R.id.list'");
        }
        this.mList.setOnKeyListener(this.mListOnKeyListener);
        this.mHandler.post(this.mRequestFocus);
    }
}
