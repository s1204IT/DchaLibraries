package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.XmlRes;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.CustomDialogPreference;
import com.android.settings.CustomEditTextPreference;
import com.android.settings.CustomListPreference;
import com.android.settings.RestrictedListPreference;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.FloatingActionButton;
import com.android.settingslib.HelpUtils;
import java.util.UUID;

public abstract class SettingsPreferenceFragment extends InstrumentedPreferenceFragment implements DialogCreatable {
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    public static final String HELP_URI_RESOURCE_KEY = "help_uri_resource";
    private static final int ORDER_FIRST = -1;
    private static final int ORDER_LAST = 2147483646;
    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";
    private static final String TAG = "SettingsPreference";
    private HighlightablePreferenceGroupAdapter mAdapter;
    private boolean mAnimationAllowed;
    private ViewGroup mButtonBar;
    private ContentResolver mContentResolver;
    private RecyclerView.Adapter mCurrentRootAdapter;
    private SettingsDialogFragment mDialogFragment;
    private View mEmptyView;
    private FloatingActionButton mFloatingActionButton;
    private LayoutPreference mFooter;
    private LayoutPreference mHeader;
    private String mHelpUri;
    private LinearLayoutManager mLayoutManager;
    private ViewGroup mPinnedHeaderFrameLayout;
    private ArrayMap<String, Preference> mPreferenceCache;
    private String mPreferenceKey;
    private boolean mPreferenceHighlighted = false;
    private boolean mIsDataSetObserverRegistered = false;
    private RecyclerView.AdapterDataObserver mDataSetObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            SettingsPreferenceFragment.this.onDataSetChanged();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        int helpResource;
        super.onCreate(icicle);
        if (Utils.isMonkeyRunning()) {
            getActivity().finish();
        }
        if (icicle != null) {
            this.mPreferenceHighlighted = icicle.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(HELP_URI_RESOURCE_KEY)) {
            helpResource = arguments.getInt(HELP_URI_RESOURCE_KEY);
        } else {
            helpResource = getHelpResource();
        }
        if (helpResource == 0) {
            return;
        }
        this.mHelpUri = getResources().getString(helpResource);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        this.mPinnedHeaderFrameLayout = (ViewGroup) root.findViewById(R.id.pinned_header);
        this.mFloatingActionButton = (FloatingActionButton) root.findViewById(R.id.fab);
        this.mButtonBar = (ViewGroup) root.findViewById(R.id.button_bar);
        return root;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        checkAvailablePrefs(getPreferenceScreen());
    }

    private void checkAvailablePrefs(PreferenceGroup preferenceGroup) {
        if (preferenceGroup == null) {
            return;
        }
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);
            if ((preference instanceof SelfAvailablePreference) && !((SelfAvailablePreference) preference).isAvailable(getContext())) {
                preferenceGroup.removePreference(preference);
            } else if (preference instanceof PreferenceGroup) {
                checkAvailablePrefs((PreferenceGroup) preference);
            }
        }
    }

    public FloatingActionButton getFloatingActionButton() {
        return this.mFloatingActionButton;
    }

    public ViewGroup getButtonBar() {
        return this.mButtonBar;
    }

    public View setPinnedHeaderView(int layoutResId) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View pinnedHeader = inflater.inflate(layoutResId, this.mPinnedHeaderFrameLayout, false);
        setPinnedHeaderView(pinnedHeader);
        return pinnedHeader;
    }

    public void setPinnedHeaderView(View pinnedHeader) {
        this.mPinnedHeaderFrameLayout.addView(pinnedHeader);
        this.mPinnedHeaderFrameLayout.setVisibility(0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, this.mPreferenceHighlighted);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        this.mPreferenceKey = args.getString(":settings:fragment_args_key");
        highlightPreferenceIfNeeded();
    }

    @Override
    protected void onBindPreferences() {
        registerObserverIfNeeded();
    }

    @Override
    protected void onUnbindPreferences() {
        unregisterObserverIfNeeded();
    }

    public void showLoadingWhenEmpty() {
        View loading = getView().findViewById(R.id.loading_container);
        setEmptyView(loading);
    }

    public void setLoading(boolean loading, boolean animate) {
        View loading_container = getView().findViewById(R.id.loading_container);
        Utils.handleLoadingContainer(loading_container, getListView(), !loading, animate);
    }

    public void registerObserverIfNeeded() {
        if (this.mIsDataSetObserverRegistered) {
            return;
        }
        if (this.mCurrentRootAdapter != null) {
            this.mCurrentRootAdapter.unregisterAdapterDataObserver(this.mDataSetObserver);
        }
        this.mCurrentRootAdapter = getListView().getAdapter();
        this.mCurrentRootAdapter.registerAdapterDataObserver(this.mDataSetObserver);
        this.mIsDataSetObserverRegistered = true;
        onDataSetChanged();
    }

    public void unregisterObserverIfNeeded() {
        if (!this.mIsDataSetObserverRegistered) {
            return;
        }
        if (this.mCurrentRootAdapter != null) {
            this.mCurrentRootAdapter.unregisterAdapterDataObserver(this.mDataSetObserver);
            this.mCurrentRootAdapter = null;
        }
        this.mIsDataSetObserverRegistered = false;
    }

    public void highlightPreferenceIfNeeded() {
        if (!isAdded() || this.mPreferenceHighlighted || TextUtils.isEmpty(this.mPreferenceKey)) {
            return;
        }
        highlightPreference(this.mPreferenceKey);
    }

    protected void onDataSetChanged() {
        highlightPreferenceIfNeeded();
        updateEmptyView();
    }

    public LayoutPreference getHeaderView() {
        return this.mHeader;
    }

    public LayoutPreference getFooterView() {
        return this.mFooter;
    }

    protected void setHeaderView(int resource) {
        this.mHeader = new LayoutPreference(getPrefContext(), resource);
        addPreferenceToTop(this.mHeader);
    }

    protected void setHeaderView(View view) {
        this.mHeader = new LayoutPreference(getPrefContext(), view);
        addPreferenceToTop(this.mHeader);
    }

    private void addPreferenceToTop(LayoutPreference preference) {
        preference.setOrder(ORDER_FIRST);
        if (getPreferenceScreen() == null) {
            return;
        }
        getPreferenceScreen().addPreference(preference);
    }

    protected void setFooterView(int resource) {
        setFooterView(resource != 0 ? new LayoutPreference(getPrefContext(), resource) : null);
    }

    protected void setFooterView(View v) {
        setFooterView(v != null ? new LayoutPreference(getPrefContext(), v) : null);
    }

    private void setFooterView(LayoutPreference footer) {
        if (getPreferenceScreen() != null && this.mFooter != null) {
            getPreferenceScreen().removePreference(this.mFooter);
        }
        if (footer != null) {
            this.mFooter = footer;
            this.mFooter.setOrder(ORDER_LAST);
            if (getPreferenceScreen() == null) {
                return;
            }
            getPreferenceScreen().addPreference(this.mFooter);
            return;
        }
        this.mFooter = null;
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != null && !preferenceScreen.isAttached()) {
            preferenceScreen.setShouldUseGeneratedIds(this.mAnimationAllowed);
        }
        super.setPreferenceScreen(preferenceScreen);
        if (preferenceScreen == null) {
            return;
        }
        if (this.mHeader != null) {
            preferenceScreen.addPreference(this.mHeader);
        }
        if (this.mFooter == null) {
            return;
        }
        preferenceScreen.addPreference(this.mFooter);
    }

    private void updateEmptyView() {
        if (this.mEmptyView == null) {
            return;
        }
        if (getPreferenceScreen() == null) {
            this.mEmptyView.setVisibility(0);
        } else {
            boolean show = (getPreferenceScreen().getPreferenceCount() - (this.mHeader != null ? 1 : 0)) - (this.mFooter == null ? 0 : 1) <= 0;
            this.mEmptyView.setVisibility(show ? 0 : 8);
        }
    }

    public void setEmptyView(View v) {
        if (this.mEmptyView != null) {
            this.mEmptyView.setVisibility(8);
        }
        this.mEmptyView = v;
        updateEmptyView();
    }

    public View getEmptyView() {
        return this.mEmptyView;
    }

    private int canUseListViewForHighLighting(String key) {
        if (getListView() == null) {
            return ORDER_FIRST;
        }
        RecyclerView listView = getListView();
        RecyclerView.Adapter adapter = listView.getAdapter();
        return (adapter == null || !(adapter instanceof PreferenceGroupAdapter)) ? ORDER_FIRST : findListPositionFromKey((PreferenceGroupAdapter) adapter, key);
    }

    @Override
    public RecyclerView.LayoutManager onCreateLayoutManager() {
        this.mLayoutManager = new LinearLayoutManager(getContext());
        return this.mLayoutManager;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        this.mAdapter = new HighlightablePreferenceGroupAdapter(preferenceScreen);
        return this.mAdapter;
    }

    protected void setAnimationAllowed(boolean animationAllowed) {
        this.mAnimationAllowed = animationAllowed;
    }

    protected void cacheRemoveAllPrefs(PreferenceGroup group) {
        this.mPreferenceCache = new ArrayMap<>();
        int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference p = group.getPreference(i);
            if (!TextUtils.isEmpty(p.getKey())) {
                this.mPreferenceCache.put(p.getKey(), p);
            }
        }
    }

    protected Preference getCachedPreference(String key) {
        if (this.mPreferenceCache != null) {
            return this.mPreferenceCache.remove(key);
        }
        return null;
    }

    protected void removeCachedPrefs(PreferenceGroup group) {
        for (Preference p : this.mPreferenceCache.values()) {
            group.removePreference(p);
        }
    }

    protected int getCachedCount() {
        return this.mPreferenceCache.size();
    }

    private void highlightPreference(String key) {
        final int position = canUseListViewForHighLighting(key);
        if (position < 0) {
            return;
        }
        this.mPreferenceHighlighted = true;
        this.mLayoutManager.scrollToPosition(position);
        getView().postDelayed(new Runnable() {
            @Override
            public void run() {
                SettingsPreferenceFragment.this.mAdapter.highlight(position);
            }
        }, 600L);
    }

    private int findListPositionFromKey(PreferenceGroupAdapter adapter, String key) {
        int count = adapter.getItemCount();
        for (int n = 0; n < count; n++) {
            Preference preference = adapter.getItem(n);
            String preferenceKey = preference.getKey();
            if (preferenceKey != null && preferenceKey.equals(key)) {
                return n;
            }
        }
        return ORDER_FIRST;
    }

    protected void removePreference(String key) {
        Preference pref = findPreference(key);
        if (pref == null) {
            return;
        }
        getPreferenceScreen().removePreference(pref);
    }

    protected int getHelpResource() {
        return R.string.help_uri_default;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.mHelpUri == null || getActivity() == null) {
            return;
        }
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, this.mHelpUri, getClass().getName());
    }

    public final void finishFragment() {
        getActivity().onBackPressed();
    }

    public ContentResolver getContentResolver() {
        Context context = getActivity();
        if (context != null) {
            this.mContentResolver = context.getContentResolver();
        }
        return this.mContentResolver;
    }

    protected Object getSystemService(String name) {
        return getActivity().getSystemService(name);
    }

    protected PackageManager getPackageManager() {
        return getActivity().getPackageManager();
    }

    @Override
    public void onDetach() {
        if (isRemoving() && this.mDialogFragment != null) {
            this.mDialogFragment.dismiss();
            this.mDialogFragment = null;
        }
        super.onDetach();
    }

    protected void showDialog(int dialogId) {
        if (this.mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
            if (this.mDialogFragment.getDialogId() == dialogId) {
                return;
            } else {
                removeDialog(this.mDialogFragment.getDialogId());
            }
        }
        this.mDialogFragment = new SettingsDialogFragment(this, dialogId);
        this.mDialogFragment.show(getChildFragmentManager(), Integer.toString(dialogId));
    }

    public Dialog onCreateDialog(int dialogId) {
        return null;
    }

    protected void removeDialog(int dialogId) {
        if (this.mDialogFragment != null && this.mDialogFragment.getDialogId() == dialogId) {
            this.mDialogFragment.dismissAllowingStateLoss();
        }
        this.mDialogFragment = null;
    }

    protected void setOnCancelListener(DialogInterface.OnCancelListener listener) {
        if (this.mDialogFragment == null) {
            return;
        }
        this.mDialogFragment.mOnCancelListener = listener;
    }

    protected void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        if (this.mDialogFragment == null) {
            return;
        }
        this.mDialogFragment.mOnDismissListener = listener;
    }

    public void onDialogShowing() {
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment f;
        if (preference.getKey() == null) {
            preference.setKey(UUID.randomUUID().toString());
        }
        if (preference instanceof RestrictedListPreference) {
            f = RestrictedListPreference.RestrictedListPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof CustomListPreference) {
            f = CustomListPreference.CustomListPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof CustomDialogPreference) {
            f = CustomDialogPreference.CustomPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof CustomEditTextPreference) {
            f = CustomEditTextPreference.CustomPreferenceDialogFragment.newInstance(preference.getKey());
        } else {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        f.setTargetFragment(this, 0);
        f.show(getFragmentManager(), "dialog_preference");
        onDialogShowing();
    }

    public static class SettingsDialogFragment extends DialogFragment {
        private int mDialogId;
        private DialogInterface.OnCancelListener mOnCancelListener;
        private DialogInterface.OnDismissListener mOnDismissListener;
        private Fragment mParentFragment;

        public SettingsDialogFragment() {
        }

        public SettingsDialogFragment(DialogCreatable dialogCreatable, int dialogId) {
            this.mDialogId = dialogId;
            if (!(dialogCreatable instanceof Fragment)) {
                throw new IllegalArgumentException("fragment argument must be an instance of " + Fragment.class.getName());
            }
            this.mParentFragment = (Fragment) dialogCreatable;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if (this.mParentFragment == null) {
                return;
            }
            outState.putInt("key_dialog_id", this.mDialogId);
            outState.putInt("key_parent_fragment_id", this.mParentFragment.getId());
        }

        @Override
        public void onStart() {
            super.onStart();
            if (this.mParentFragment == null || !(this.mParentFragment instanceof SettingsPreferenceFragment)) {
                return;
            }
            ((SettingsPreferenceFragment) this.mParentFragment).onDialogShowing();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                this.mDialogId = savedInstanceState.getInt("key_dialog_id", 0);
                this.mParentFragment = getParentFragment();
                int mParentFragmentId = savedInstanceState.getInt("key_parent_fragment_id", SettingsPreferenceFragment.ORDER_FIRST);
                if (this.mParentFragment == null) {
                    this.mParentFragment = getFragmentManager().findFragmentById(mParentFragmentId);
                }
                if (!(this.mParentFragment instanceof DialogCreatable)) {
                    throw new IllegalArgumentException((this.mParentFragment != null ? this.mParentFragment.getClass().getName() : Integer.valueOf(mParentFragmentId)) + " must implement " + DialogCreatable.class.getName());
                }
                if (this.mParentFragment instanceof SettingsPreferenceFragment) {
                    ((SettingsPreferenceFragment) this.mParentFragment).mDialogFragment = this;
                }
            }
            return ((DialogCreatable) this.mParentFragment).onCreateDialog(this.mDialogId);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (this.mOnCancelListener == null) {
                return;
            }
            this.mOnCancelListener.onCancel(dialog);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (this.mOnDismissListener == null) {
                return;
            }
            this.mOnDismissListener.onDismiss(dialog);
        }

        public int getDialogId() {
            return this.mDialogId;
        }

        @Override
        public void onDetach() {
            super.onDetach();
            if (!(this.mParentFragment instanceof SettingsPreferenceFragment) || ((SettingsPreferenceFragment) this.mParentFragment).mDialogFragment != this) {
                return;
            }
            ((SettingsPreferenceFragment) this.mParentFragment).mDialogFragment = null;
        }
    }

    protected boolean hasNextButton() {
        return ((ButtonBarHandler) getActivity()).hasNextButton();
    }

    protected Button getNextButton() {
        return ((ButtonBarHandler) getActivity()).getNextButton();
    }

    public void finish() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            activity.finish();
        }
    }

    protected Intent getIntent() {
        if (getActivity() == null) {
            return null;
        }
        return getActivity().getIntent();
    }

    protected void setResult(int result, Intent intent) {
        if (getActivity() == null) {
            return;
        }
        getActivity().setResult(result, intent);
    }

    protected void setResult(int result) {
        if (getActivity() == null) {
            return;
        }
        getActivity().setResult(result);
    }

    public final Context getPrefContext() {
        return getPreferenceManager().getContext();
    }

    public boolean startFragment(Fragment caller, String fragmentClass, int titleRes, int requestCode, Bundle extras) {
        Activity activity = getActivity();
        if (activity instanceof SettingsActivity) {
            SettingsActivity sa = (SettingsActivity) activity;
            sa.startPreferencePanel(fragmentClass, extras, titleRes, null, caller, requestCode);
            return true;
        }
        Log.w(TAG, "Parent isn't SettingsActivity nor PreferenceActivity, thus there's no way to launch the given Fragment (name: " + fragmentClass + ", requestCode: " + requestCode + ")");
        return false;
    }

    public static class HighlightablePreferenceGroupAdapter extends PreferenceGroupAdapter {
        private int mHighlightPosition;

        public HighlightablePreferenceGroupAdapter(PreferenceGroup preferenceGroup) {
            super(preferenceGroup);
            this.mHighlightPosition = SettingsPreferenceFragment.ORDER_FIRST;
        }

        public void highlight(int position) {
            this.mHighlightPosition = position;
            notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            if (position != this.mHighlightPosition) {
                return;
            }
            View v = holder.itemView;
            if (v.getBackground() != null) {
                int centerX = v.getWidth() / 2;
                int centerY = v.getHeight() / 2;
                v.getBackground().setHotspot(centerX, centerY);
            }
            v.setPressed(true);
            v.setPressed(false);
            this.mHighlightPosition = SettingsPreferenceFragment.ORDER_FIRST;
        }
    }
}
