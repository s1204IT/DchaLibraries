package android.preference;

import android.animation.LayoutTransition;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public abstract class PreferenceActivity extends ListActivity implements PreferenceManager.OnPreferenceTreeClickListener, PreferenceFragment.OnPreferenceStartFragmentCallback {
    private static final String BACK_STACK_PREFS = ":android:prefs";
    private static final String CUR_HEADER_TAG = ":android:cur_header";
    public static final String EXTRA_NO_HEADERS = ":android:no_headers";
    private static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";
    private static final String EXTRA_PREFS_SET_NEXT_TEXT = "extra_prefs_set_next_text";
    private static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    private static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";
    public static final String EXTRA_SHOW_FRAGMENT = ":android:show_fragment";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":android:show_fragment_args";
    public static final String EXTRA_SHOW_FRAGMENT_SHORT_TITLE = ":android:show_fragment_short_title";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":android:show_fragment_title";
    private static final int FIRST_REQUEST_CODE = 100;
    private static final String HEADERS_TAG = ":android:headers";
    public static final long HEADER_ID_UNDEFINED = -1;
    private static final int MSG_BIND_PREFERENCES = 1;
    private static final int MSG_BUILD_HEADERS = 2;
    private static final String PREFERENCES_TAG = ":android:preferences";
    private static final String TAG = "PreferenceActivity";
    private CharSequence mActivityTitle;
    private Header mCurHeader;
    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private ViewGroup mHeadersContainer;
    private FrameLayout mListFooter;
    private Button mNextButton;
    private PreferenceManager mPreferenceManager;
    private ViewGroup mPrefsContainer;
    private Bundle mSavedInstanceState;
    private boolean mSinglePane;
    private final ArrayList<Header> mHeaders = new ArrayList<>();
    private int mPreferenceHeaderItemResId = 0;
    private boolean mPreferenceHeaderRemoveEmptyIcon = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            Header headerFindBestMatchingHeader;
            switch (message.what) {
                case 1:
                    PreferenceActivity.this.bindPreferences();
                    break;
                case 2:
                    ArrayList<Header> arrayList = new ArrayList<>(PreferenceActivity.this.mHeaders);
                    PreferenceActivity.this.mHeaders.clear();
                    PreferenceActivity.this.onBuildHeaders(PreferenceActivity.this.mHeaders);
                    if (PreferenceActivity.this.mAdapter instanceof BaseAdapter) {
                        ((BaseAdapter) PreferenceActivity.this.mAdapter).notifyDataSetChanged();
                    }
                    Header headerOnGetNewHeader = PreferenceActivity.this.onGetNewHeader();
                    if (headerOnGetNewHeader == null || headerOnGetNewHeader.fragment == null) {
                        if (PreferenceActivity.this.mCurHeader != null && (headerFindBestMatchingHeader = PreferenceActivity.this.findBestMatchingHeader(PreferenceActivity.this.mCurHeader, PreferenceActivity.this.mHeaders)) != null) {
                            PreferenceActivity.this.setSelectedHeader(headerFindBestMatchingHeader);
                            break;
                        }
                    } else {
                        Header headerFindBestMatchingHeader2 = PreferenceActivity.this.findBestMatchingHeader(headerOnGetNewHeader, arrayList);
                        if (headerFindBestMatchingHeader2 == null || PreferenceActivity.this.mCurHeader != headerFindBestMatchingHeader2) {
                            PreferenceActivity.this.switchToHeader(headerOnGetNewHeader);
                        }
                        break;
                    }
                    break;
            }
        }
    };

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        private LayoutInflater mInflater;
        private int mLayoutResId;
        private boolean mRemoveIconIfEmpty;

        private static class HeaderViewHolder {
            ImageView icon;
            TextView summary;
            TextView title;

            private HeaderViewHolder() {
            }
        }

        public HeaderAdapter(Context context, List<Header> list, int i, boolean z) {
            super(context, 0, list);
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mLayoutResId = i;
            this.mRemoveIconIfEmpty = z;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            HeaderViewHolder headerViewHolder;
            if (view == null) {
                view = this.mInflater.inflate(this.mLayoutResId, viewGroup, false);
                headerViewHolder = new HeaderViewHolder();
                headerViewHolder.icon = (ImageView) view.findViewById(16908294);
                headerViewHolder.title = (TextView) view.findViewById(16908310);
                headerViewHolder.summary = (TextView) view.findViewById(16908304);
                view.setTag(headerViewHolder);
            } else {
                headerViewHolder = (HeaderViewHolder) view.getTag();
            }
            Header item = getItem(i);
            if (this.mRemoveIconIfEmpty) {
                if (item.iconRes != 0) {
                    headerViewHolder.icon.setVisibility(0);
                    headerViewHolder.icon.setImageResource(item.iconRes);
                } else {
                    headerViewHolder.icon.setVisibility(8);
                }
            } else {
                headerViewHolder.icon.setImageResource(item.iconRes);
            }
            headerViewHolder.title.setText(item.getTitle(getContext().getResources()));
            CharSequence summary = item.getSummary(getContext().getResources());
            if (!TextUtils.isEmpty(summary)) {
                headerViewHolder.summary.setVisibility(0);
                headerViewHolder.summary.setText(summary);
            } else {
                headerViewHolder.summary.setVisibility(8);
            }
            return view;
        }
    }

    public static final class Header implements Parcelable {
        public static final Parcelable.Creator<Header> CREATOR = new Parcelable.Creator<Header>() {
            @Override
            public Header createFromParcel(Parcel parcel) {
                return new Header(parcel);
            }

            @Override
            public Header[] newArray(int i) {
                return new Header[i];
            }
        };
        public CharSequence breadCrumbShortTitle;
        public int breadCrumbShortTitleRes;
        public CharSequence breadCrumbTitle;
        public int breadCrumbTitleRes;
        public Bundle extras;
        public String fragment;
        public Bundle fragmentArguments;
        public int iconRes;
        public long id = -1;
        public Intent intent;
        public CharSequence summary;
        public int summaryRes;
        public CharSequence title;
        public int titleRes;

        public Header() {
        }

        public CharSequence getTitle(Resources resources) {
            if (this.titleRes != 0) {
                return resources.getText(this.titleRes);
            }
            return this.title;
        }

        public CharSequence getSummary(Resources resources) {
            if (this.summaryRes != 0) {
                return resources.getText(this.summaryRes);
            }
            return this.summary;
        }

        public CharSequence getBreadCrumbTitle(Resources resources) {
            if (this.breadCrumbTitleRes != 0) {
                return resources.getText(this.breadCrumbTitleRes);
            }
            return this.breadCrumbTitle;
        }

        public CharSequence getBreadCrumbShortTitle(Resources resources) {
            if (this.breadCrumbShortTitleRes != 0) {
                return resources.getText(this.breadCrumbShortTitleRes);
            }
            return this.breadCrumbShortTitle;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.id);
            parcel.writeInt(this.titleRes);
            TextUtils.writeToParcel(this.title, parcel, i);
            parcel.writeInt(this.summaryRes);
            TextUtils.writeToParcel(this.summary, parcel, i);
            parcel.writeInt(this.breadCrumbTitleRes);
            TextUtils.writeToParcel(this.breadCrumbTitle, parcel, i);
            parcel.writeInt(this.breadCrumbShortTitleRes);
            TextUtils.writeToParcel(this.breadCrumbShortTitle, parcel, i);
            parcel.writeInt(this.iconRes);
            parcel.writeString(this.fragment);
            parcel.writeBundle(this.fragmentArguments);
            if (this.intent != null) {
                parcel.writeInt(1);
                this.intent.writeToParcel(parcel, i);
            } else {
                parcel.writeInt(0);
            }
            parcel.writeBundle(this.extras);
        }

        public void readFromParcel(Parcel parcel) {
            this.id = parcel.readLong();
            this.titleRes = parcel.readInt();
            this.title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.summaryRes = parcel.readInt();
            this.summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.breadCrumbTitleRes = parcel.readInt();
            this.breadCrumbTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.breadCrumbShortTitleRes = parcel.readInt();
            this.breadCrumbShortTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.iconRes = parcel.readInt();
            this.fragment = parcel.readString();
            this.fragmentArguments = parcel.readBundle();
            if (parcel.readInt() != 0) {
                this.intent = Intent.CREATOR.createFromParcel(parcel);
            }
            this.extras = parcel.readBundle();
        }

        Header(Parcel parcel) {
            readFromParcel(parcel);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        CharSequence text;
        super.onCreate(bundle);
        TypedArray typedArrayObtainStyledAttributes = obtainStyledAttributes(null, R.styleable.PreferenceActivity, R.attr.preferenceActivityStyle, 0);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(0, R.layout.preference_list_content);
        this.mPreferenceHeaderItemResId = typedArrayObtainStyledAttributes.getResourceId(1, R.layout.preference_header_item);
        this.mPreferenceHeaderRemoveEmptyIcon = typedArrayObtainStyledAttributes.getBoolean(2, false);
        typedArrayObtainStyledAttributes.recycle();
        setContentView(resourceId);
        this.mListFooter = (FrameLayout) findViewById(R.id.list_footer);
        this.mPrefsContainer = (ViewGroup) findViewById(R.id.prefs_frame);
        this.mHeadersContainer = (ViewGroup) findViewById(R.id.headers);
        this.mSinglePane = onIsHidingHeaders() || !onIsMultiPane();
        String stringExtra = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        Bundle bundleExtra = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        int intExtra = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
        int intExtra2 = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, 0);
        this.mActivityTitle = getTitle();
        if (bundle != null) {
            ArrayList parcelableArrayList = bundle.getParcelableArrayList(HEADERS_TAG);
            if (parcelableArrayList != null) {
                this.mHeaders.addAll(parcelableArrayList);
                int i = bundle.getInt(CUR_HEADER_TAG, -1);
                if (i >= 0 && i < this.mHeaders.size()) {
                    setSelectedHeader(this.mHeaders.get(i));
                } else if (!this.mSinglePane && stringExtra == null) {
                    switchToHeader(onGetInitialHeader());
                }
            } else {
                showBreadCrumbs(getTitle(), null);
            }
        } else {
            if (!onIsHidingHeaders()) {
                onBuildHeaders(this.mHeaders);
            }
            if (stringExtra != null) {
                switchToHeader(stringExtra, bundleExtra);
            } else if (!this.mSinglePane && this.mHeaders.size() > 0) {
                switchToHeader(onGetInitialHeader());
            }
        }
        if (this.mHeaders.size() > 0) {
            setListAdapter(new HeaderAdapter(this, this.mHeaders, this.mPreferenceHeaderItemResId, this.mPreferenceHeaderRemoveEmptyIcon));
            if (!this.mSinglePane) {
                getListView().setChoiceMode(1);
            }
        }
        if (this.mSinglePane && stringExtra != null && intExtra != 0) {
            CharSequence text2 = getText(intExtra);
            if (intExtra2 != 0) {
                text = getText(intExtra2);
            } else {
                text = null;
            }
            showBreadCrumbs(text2, text);
        }
        if (this.mHeaders.size() == 0 && stringExtra == null) {
            setContentView(R.layout.preference_list_content_single);
            this.mListFooter = (FrameLayout) findViewById(R.id.list_footer);
            this.mPrefsContainer = (ViewGroup) findViewById(R.id.prefs);
            this.mPreferenceManager = new PreferenceManager(this, 100);
            this.mPreferenceManager.setOnPreferenceTreeClickListener(this);
            this.mHeadersContainer = null;
        } else if (this.mSinglePane) {
            if (stringExtra != null || this.mCurHeader != null) {
                this.mHeadersContainer.setVisibility(8);
            } else {
                this.mPrefsContainer.setVisibility(8);
            }
            ((ViewGroup) findViewById(R.id.prefs_container)).setLayoutTransition(new LayoutTransition());
        } else if (this.mHeaders.size() > 0 && this.mCurHeader != null) {
            setSelectedHeader(this.mCurHeader);
        }
        Intent intent = getIntent();
        if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {
            findViewById(R.id.button_bar).setVisibility(0);
            Button button = (Button) findViewById(R.id.back_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PreferenceActivity.this.setResult(0);
                    PreferenceActivity.this.finish();
                }
            });
            Button button2 = (Button) findViewById(R.id.skip_button);
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PreferenceActivity.this.setResult(-1);
                    PreferenceActivity.this.finish();
                }
            });
            this.mNextButton = (Button) findViewById(R.id.next_button);
            this.mNextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PreferenceActivity.this.setResult(-1);
                    PreferenceActivity.this.finish();
                }
            });
            if (intent.hasExtra(EXTRA_PREFS_SET_NEXT_TEXT)) {
                String stringExtra2 = intent.getStringExtra(EXTRA_PREFS_SET_NEXT_TEXT);
                if (TextUtils.isEmpty(stringExtra2)) {
                    this.mNextButton.setVisibility(8);
                } else {
                    this.mNextButton.setText(stringExtra2);
                }
            }
            if (intent.hasExtra(EXTRA_PREFS_SET_BACK_TEXT)) {
                String stringExtra3 = intent.getStringExtra(EXTRA_PREFS_SET_BACK_TEXT);
                if (TextUtils.isEmpty(stringExtra3)) {
                    button.setVisibility(8);
                } else {
                    button.setText(stringExtra3);
                }
            }
            if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_SKIP, false)) {
                button2.setVisibility(0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (this.mCurHeader != null && this.mSinglePane && getFragmentManager().getBackStackEntryCount() == 0 && getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT) == null) {
            this.mCurHeader = null;
            this.mPrefsContainer.setVisibility(8);
            this.mHeadersContainer.setVisibility(0);
            if (this.mActivityTitle != null) {
                showBreadCrumbs(this.mActivityTitle, null);
            }
            getListView().clearChoices();
            return;
        }
        super.onBackPressed();
    }

    public boolean hasHeaders() {
        return this.mHeadersContainer != null && this.mHeadersContainer.getVisibility() == 0;
    }

    public List<Header> getHeaders() {
        return this.mHeaders;
    }

    public boolean isMultiPane() {
        return !this.mSinglePane;
    }

    public boolean onIsMultiPane() {
        return getResources().getBoolean(R.bool.preferences_prefer_dual_pane);
    }

    public boolean onIsHidingHeaders() {
        return getIntent().getBooleanExtra(EXTRA_NO_HEADERS, false);
    }

    public Header onGetInitialHeader() {
        for (int i = 0; i < this.mHeaders.size(); i++) {
            Header header = this.mHeaders.get(i);
            if (header.fragment != null) {
                return header;
            }
        }
        throw new IllegalStateException("Must have at least one header with a fragment");
    }

    public Header onGetNewHeader() {
        return null;
    }

    public void onBuildHeaders(List<Header> list) {
    }

    public void invalidateHeaders() {
        if (!this.mHandler.hasMessages(2)) {
            this.mHandler.sendEmptyMessage(2);
        }
    }

    public void loadHeadersFromResource(int i, List<Header> list) throws Throwable {
        XmlResourceParser xml;
        int next;
        try {
            try {
                xml = getResources().getXml(i);
            } catch (Throwable th) {
                th = th;
                xml = null;
            }
        } catch (IOException e) {
            e = e;
        } catch (XmlPullParserException e2) {
            e = e2;
        }
        try {
            AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xml);
            do {
                next = xml.next();
                if (next == 1) {
                    break;
                }
            } while (next != 2);
            String name = xml.getName();
            if (!"preference-headers".equals(name)) {
                throw new RuntimeException("XML document must start with <preference-headers> tag; found" + name + " at " + xml.getPositionDescription());
            }
            int depth = xml.getDepth();
            Bundle bundle = null;
            while (true) {
                int next2 = xml.next();
                if (next2 == 1 || (next2 == 3 && xml.getDepth() <= depth)) {
                    break;
                }
                if (next2 != 3 && next2 != 4) {
                    if (Downloads.Impl.RequestHeaders.COLUMN_HEADER.equals(xml.getName())) {
                        Header header = new Header();
                        TypedArray typedArrayObtainStyledAttributes = obtainStyledAttributes(attributeSetAsAttributeSet, R.styleable.PreferenceHeader);
                        header.id = typedArrayObtainStyledAttributes.getResourceId(1, -1);
                        TypedValue typedValuePeekValue = typedArrayObtainStyledAttributes.peekValue(2);
                        if (typedValuePeekValue != null && typedValuePeekValue.type == 3) {
                            if (typedValuePeekValue.resourceId != 0) {
                                header.titleRes = typedValuePeekValue.resourceId;
                            } else {
                                header.title = typedValuePeekValue.string;
                            }
                        }
                        TypedValue typedValuePeekValue2 = typedArrayObtainStyledAttributes.peekValue(3);
                        if (typedValuePeekValue2 != null && typedValuePeekValue2.type == 3) {
                            if (typedValuePeekValue2.resourceId != 0) {
                                header.summaryRes = typedValuePeekValue2.resourceId;
                            } else {
                                header.summary = typedValuePeekValue2.string;
                            }
                        }
                        TypedValue typedValuePeekValue3 = typedArrayObtainStyledAttributes.peekValue(5);
                        if (typedValuePeekValue3 != null && typedValuePeekValue3.type == 3) {
                            if (typedValuePeekValue3.resourceId != 0) {
                                header.breadCrumbTitleRes = typedValuePeekValue3.resourceId;
                            } else {
                                header.breadCrumbTitle = typedValuePeekValue3.string;
                            }
                        }
                        TypedValue typedValuePeekValue4 = typedArrayObtainStyledAttributes.peekValue(6);
                        if (typedValuePeekValue4 != null && typedValuePeekValue4.type == 3) {
                            if (typedValuePeekValue4.resourceId != 0) {
                                header.breadCrumbShortTitleRes = typedValuePeekValue4.resourceId;
                            } else {
                                header.breadCrumbShortTitle = typedValuePeekValue4.string;
                            }
                        }
                        header.iconRes = typedArrayObtainStyledAttributes.getResourceId(0, 0);
                        header.fragment = typedArrayObtainStyledAttributes.getString(4);
                        typedArrayObtainStyledAttributes.recycle();
                        if (bundle == null) {
                            bundle = new Bundle();
                        }
                        int depth2 = xml.getDepth();
                        while (true) {
                            int next3 = xml.next();
                            if (next3 == 1 || (next3 == 3 && xml.getDepth() <= depth2)) {
                                break;
                            }
                            if (next3 != 3 && next3 != 4) {
                                String name2 = xml.getName();
                                if (name2.equals("extra")) {
                                    getResources().parseBundleExtra("extra", attributeSetAsAttributeSet, bundle);
                                    XmlUtils.skipCurrentTag(xml);
                                } else if (name2.equals("intent")) {
                                    header.intent = Intent.parseIntent(getResources(), xml, attributeSetAsAttributeSet);
                                } else {
                                    XmlUtils.skipCurrentTag(xml);
                                }
                            }
                        }
                        if (bundle.size() > 0) {
                            header.fragmentArguments = bundle;
                            bundle = null;
                        }
                        list.add(header);
                    } else {
                        XmlUtils.skipCurrentTag(xml);
                    }
                }
            }
            if (xml != null) {
                xml.close();
            }
        } catch (IOException e3) {
            e = e3;
            throw new RuntimeException("Error parsing headers", e);
        } catch (XmlPullParserException e4) {
            e = e4;
            throw new RuntimeException("Error parsing headers", e);
        } catch (Throwable th2) {
            th = th2;
            if (xml != null) {
                xml.close();
            }
            throw th;
        }
    }

    protected boolean isValidFragment(String str) {
        if (getApplicationInfo().targetSdkVersion >= 19) {
            throw new RuntimeException("Subclasses of PreferenceActivity must override isValidFragment(String) to verify that the Fragment class is valid! " + getClass().getName() + " has not checked if fragment " + str + " is valid.");
        }
        return true;
    }

    public void setListFooter(View view) {
        this.mListFooter.removeAllViews();
        this.mListFooter.addView(view, new FrameLayout.LayoutParams(-1, -2));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mPreferenceManager != null) {
            this.mPreferenceManager.dispatchActivityStop();
        }
    }

    @Override
    protected void onDestroy() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        super.onDestroy();
        if (this.mPreferenceManager != null) {
            this.mPreferenceManager.dispatchActivityDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        PreferenceScreen preferenceScreen;
        int iIndexOf;
        super.onSaveInstanceState(bundle);
        if (this.mHeaders.size() > 0) {
            bundle.putParcelableArrayList(HEADERS_TAG, this.mHeaders);
            if (this.mCurHeader != null && (iIndexOf = this.mHeaders.indexOf(this.mCurHeader)) >= 0) {
                bundle.putInt(CUR_HEADER_TAG, iIndexOf);
            }
        }
        if (this.mPreferenceManager != null && (preferenceScreen = getPreferenceScreen()) != null) {
            Bundle bundle2 = new Bundle();
            preferenceScreen.saveHierarchyState(bundle2);
            bundle.putBundle(PREFERENCES_TAG, bundle2);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        Bundle bundle2;
        PreferenceScreen preferenceScreen;
        if (this.mPreferenceManager != null && (bundle2 = bundle.getBundle(PREFERENCES_TAG)) != null && (preferenceScreen = getPreferenceScreen()) != null) {
            preferenceScreen.restoreHierarchyState(bundle2);
            this.mSavedInstanceState = bundle;
            return;
        }
        super.onRestoreInstanceState(bundle);
        if (!this.mSinglePane && this.mCurHeader != null) {
            setSelectedHeader(this.mCurHeader);
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (this.mPreferenceManager != null) {
            this.mPreferenceManager.dispatchActivityResult(i, i2, intent);
        }
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        if (this.mPreferenceManager != null) {
            postBindPreferences();
        }
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        if (!isResumed()) {
            return;
        }
        super.onListItemClick(listView, view, i, j);
        if (this.mAdapter != null) {
            Object item = this.mAdapter.getItem(i);
            if (item instanceof Header) {
                onHeaderClick((Header) item, i);
            }
        }
    }

    public void onHeaderClick(Header header, int i) {
        if (header.fragment != null) {
            switchToHeader(header);
        } else if (header.intent != null) {
            startActivity(header.intent);
        }
    }

    public Intent onBuildStartFragmentIntent(String str, Bundle bundle, int i, int i2) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(this, getClass());
        intent.putExtra(EXTRA_SHOW_FRAGMENT, str);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, i);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, i2);
        intent.putExtra(EXTRA_NO_HEADERS, true);
        return intent;
    }

    public void startWithFragment(String str, Bundle bundle, Fragment fragment, int i) {
        startWithFragment(str, bundle, fragment, i, 0, 0);
    }

    public void startWithFragment(String str, Bundle bundle, Fragment fragment, int i, int i2, int i3) {
        Intent intentOnBuildStartFragmentIntent = onBuildStartFragmentIntent(str, bundle, i2, i3);
        if (fragment == null) {
            startActivity(intentOnBuildStartFragmentIntent);
        } else {
            fragment.startActivityForResult(intentOnBuildStartFragmentIntent, i);
        }
    }

    public void showBreadCrumbs(CharSequence charSequence, CharSequence charSequence2) {
        if (this.mFragmentBreadCrumbs == null) {
            try {
                this.mFragmentBreadCrumbs = (FragmentBreadCrumbs) findViewById(16908310);
                if (this.mFragmentBreadCrumbs == null) {
                    if (charSequence != null) {
                        setTitle(charSequence);
                        return;
                    }
                    return;
                }
                if (this.mSinglePane) {
                    this.mFragmentBreadCrumbs.setVisibility(8);
                    View viewFindViewById = findViewById(R.id.breadcrumb_section);
                    if (viewFindViewById != null) {
                        viewFindViewById.setVisibility(8);
                    }
                    setTitle(charSequence);
                }
                this.mFragmentBreadCrumbs.setMaxVisible(2);
                this.mFragmentBreadCrumbs.setActivity(this);
            } catch (ClassCastException e) {
                setTitle(charSequence);
                return;
            }
        }
        if (this.mFragmentBreadCrumbs.getVisibility() != 0) {
            setTitle(charSequence);
        } else {
            this.mFragmentBreadCrumbs.setTitle(charSequence, charSequence2);
            this.mFragmentBreadCrumbs.setParentTitle(null, null, null);
        }
    }

    public void setParentTitle(CharSequence charSequence, CharSequence charSequence2, View.OnClickListener onClickListener) {
        if (this.mFragmentBreadCrumbs != null) {
            this.mFragmentBreadCrumbs.setParentTitle(charSequence, charSequence2, onClickListener);
        }
    }

    void setSelectedHeader(Header header) {
        this.mCurHeader = header;
        int iIndexOf = this.mHeaders.indexOf(header);
        if (iIndexOf >= 0) {
            getListView().setItemChecked(iIndexOf, true);
        } else {
            getListView().clearChoices();
        }
        showBreadCrumbs(header);
    }

    void showBreadCrumbs(Header header) {
        if (header != null) {
            CharSequence breadCrumbTitle = header.getBreadCrumbTitle(getResources());
            if (breadCrumbTitle == null) {
                breadCrumbTitle = header.getTitle(getResources());
            }
            if (breadCrumbTitle == null) {
                breadCrumbTitle = getTitle();
            }
            showBreadCrumbs(breadCrumbTitle, header.getBreadCrumbShortTitle(getResources()));
            return;
        }
        showBreadCrumbs(getTitle(), null);
    }

    private void switchToHeaderInner(String str, Bundle bundle) {
        int i;
        getFragmentManager().popBackStack(BACK_STACK_PREFS, 1);
        if (!isValidFragment(str)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: " + str);
        }
        Fragment fragmentInstantiate = Fragment.instantiate(this, str, bundle);
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        if (this.mSinglePane) {
            i = 0;
        } else {
            i = 4099;
        }
        fragmentTransactionBeginTransaction.setTransition(i);
        fragmentTransactionBeginTransaction.replace(R.id.prefs, fragmentInstantiate);
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        if (this.mSinglePane && this.mPrefsContainer.getVisibility() == 8) {
            this.mPrefsContainer.setVisibility(0);
            this.mHeadersContainer.setVisibility(8);
        }
    }

    public void switchToHeader(String str, Bundle bundle) {
        Header header;
        int i = 0;
        while (true) {
            if (i < this.mHeaders.size()) {
                if (!str.equals(this.mHeaders.get(i).fragment)) {
                    i++;
                } else {
                    header = this.mHeaders.get(i);
                    break;
                }
            } else {
                header = null;
                break;
            }
        }
        setSelectedHeader(header);
        switchToHeaderInner(str, bundle);
    }

    public void switchToHeader(Header header) {
        if (this.mCurHeader == header) {
            getFragmentManager().popBackStack(BACK_STACK_PREFS, 1);
        } else {
            if (header.fragment == null) {
                throw new IllegalStateException("can't switch to header that has no fragment");
            }
            switchToHeaderInner(header.fragment, header.fragmentArguments);
            setSelectedHeader(header);
        }
    }

    Header findBestMatchingHeader(Header header, ArrayList<Header> arrayList) {
        ArrayList arrayList2 = new ArrayList();
        for (int i = 0; i < arrayList.size(); i++) {
            Header header2 = arrayList.get(i);
            if (header == header2 || (header.id != -1 && header.id == header2.id)) {
                arrayList2.clear();
                arrayList2.add(header2);
                break;
            }
            if (header.fragment != null) {
                if (header.fragment.equals(header2.fragment)) {
                    arrayList2.add(header2);
                }
            } else if (header.intent != null) {
                if (header.intent.equals(header2.intent)) {
                    arrayList2.add(header2);
                }
            } else if (header.title != null && header.title.equals(header2.title)) {
                arrayList2.add(header2);
            }
        }
        int size = arrayList2.size();
        if (size == 1) {
            return (Header) arrayList2.get(0);
        }
        if (size > 1) {
            for (int i2 = 0; i2 < size; i2++) {
                Header header3 = (Header) arrayList2.get(i2);
                if (header.fragmentArguments != null && header.fragmentArguments.equals(header3.fragmentArguments)) {
                    return header3;
                }
                if (header.extras != null && header.extras.equals(header3.extras)) {
                    return header3;
                }
                if (header.title != null && header.title.equals(header3.title)) {
                    return header3;
                }
            }
            return null;
        }
        return null;
    }

    public void startPreferenceFragment(Fragment fragment, boolean z) {
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.replace(R.id.prefs, fragment);
        if (z) {
            fragmentTransactionBeginTransaction.setTransition(4097);
            fragmentTransactionBeginTransaction.addToBackStack(BACK_STACK_PREFS);
        } else {
            fragmentTransactionBeginTransaction.setTransition(4099);
        }
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    public void startPreferencePanel(String str, Bundle bundle, int i, CharSequence charSequence, Fragment fragment, int i2) {
        Fragment fragmentInstantiate = Fragment.instantiate(this, str, bundle);
        if (fragment != null) {
            fragmentInstantiate.setTargetFragment(fragment, i2);
        }
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.replace(R.id.prefs, fragmentInstantiate);
        if (i != 0) {
            fragmentTransactionBeginTransaction.setBreadCrumbTitle(i);
        } else if (charSequence != null) {
            fragmentTransactionBeginTransaction.setBreadCrumbTitle(charSequence);
        }
        fragmentTransactionBeginTransaction.setTransition(4097);
        fragmentTransactionBeginTransaction.addToBackStack(BACK_STACK_PREFS);
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    public void finishPreferencePanel(Fragment fragment, int i, Intent intent) {
        onBackPressed();
        if (fragment != null && fragment.getTargetFragment() != null) {
            fragment.getTargetFragment().onActivityResult(fragment.getTargetRequestCode(), i, intent);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        startPreferencePanel(preference.getFragment(), preference.getExtras(), preference.getTitleRes(), preference.getTitle(), null, 0);
        return true;
    }

    private void postBindPreferences() {
        if (this.mHandler.hasMessages(1)) {
            return;
        }
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    private void bindPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.bind(getListView());
            if (this.mSavedInstanceState != null) {
                super.onRestoreInstanceState(this.mSavedInstanceState);
                this.mSavedInstanceState = null;
            }
        }
    }

    @Deprecated
    public PreferenceManager getPreferenceManager() {
        return this.mPreferenceManager;
    }

    private void requirePreferenceManager() {
        if (this.mPreferenceManager == null) {
            if (this.mAdapter == null) {
                throw new RuntimeException("This should be called after super.onCreate.");
            }
            throw new RuntimeException("Modern two-pane PreferenceActivity requires use of a PreferenceFragment");
        }
    }

    @Deprecated
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        requirePreferenceManager();
        if (this.mPreferenceManager.setPreferences(preferenceScreen) && preferenceScreen != null) {
            postBindPreferences();
            CharSequence title = getPreferenceScreen().getTitle();
            if (title != null) {
                setTitle(title);
            }
        }
    }

    @Deprecated
    public PreferenceScreen getPreferenceScreen() {
        if (this.mPreferenceManager != null) {
            return this.mPreferenceManager.getPreferenceScreen();
        }
        return null;
    }

    @Deprecated
    public void addPreferencesFromIntent(Intent intent) {
        requirePreferenceManager();
        setPreferenceScreen(this.mPreferenceManager.inflateFromIntent(intent, getPreferenceScreen()));
    }

    @Deprecated
    public void addPreferencesFromResource(int i) {
        requirePreferenceManager();
        setPreferenceScreen(this.mPreferenceManager.inflateFromResource(this, i, getPreferenceScreen()));
    }

    @Override
    @Deprecated
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    @Deprecated
    public Preference findPreference(CharSequence charSequence) {
        if (this.mPreferenceManager == null) {
            return null;
        }
        return this.mPreferenceManager.findPreference(charSequence);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (this.mPreferenceManager != null) {
            this.mPreferenceManager.dispatchNewIntent(intent);
        }
    }

    protected boolean hasNextButton() {
        return this.mNextButton != null;
    }

    protected Button getNextButton() {
        return this.mNextButton;
    }
}
