package com.android.quicksearchbox;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import com.android.quicksearchbox.google.GoogleSource;
import com.android.quicksearchbox.google.GoogleSuggestClient;
import com.android.quicksearchbox.google.SearchBaseUrlHelper;
import com.android.quicksearchbox.ui.DefaultSuggestionViewFactory;
import com.android.quicksearchbox.ui.SuggestionViewFactory;
import com.android.quicksearchbox.util.HttpHelper;
import com.android.quicksearchbox.util.JavaNetHttpHelper;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.PerNameExecutor;
import com.android.quicksearchbox.util.PriorityThreadFactory;
import com.android.quicksearchbox.util.SingleThreadNamedTaskExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ThreadFactory;

public class QsbApplication {
    private Config mConfig;
    private final Context mContext;
    private GoogleSource mGoogleSource;
    private HttpHelper mHttpHelper;
    private NamedTaskExecutor mIconLoaderExecutor;
    private Logger mLogger;
    private ThreadFactory mQueryThreadFactory;
    private SearchBaseUrlHelper mSearchBaseUrlHelper;
    private SearchSettings mSettings;
    private NamedTaskExecutor mSourceTaskExecutor;
    private SuggestionFormatter mSuggestionFormatter;
    private SuggestionViewFactory mSuggestionViewFactory;
    private SuggestionsProvider mSuggestionsProvider;
    private TextAppearanceFactory mTextAppearanceFactory;
    private Handler mUiThreadHandler;
    private int mVersionCode;
    private VoiceSearch mVoiceSearch;

    public QsbApplication(Context context) {
        this.mContext = new ContextThemeWrapper(context, R.style.Theme_QuickSearchBox);
    }

    public static QsbApplication get(Context context) {
        return ((QsbApplicationWrapper) context.getApplicationContext()).getApp();
    }

    protected Context getContext() {
        return this.mContext;
    }

    public int getVersionCode() {
        if (this.mVersionCode == 0) {
            try {
                PackageManager pm = getContext().getPackageManager();
                PackageInfo pkgInfo = pm.getPackageInfo(getContext().getPackageName(), 0);
                this.mVersionCode = pkgInfo.versionCode;
            } catch (PackageManager.NameNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
        return this.mVersionCode;
    }

    protected void checkThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Accessed Application object from thread " + Thread.currentThread().getName());
        }
    }

    protected void close() {
        checkThread();
        if (this.mConfig != null) {
            this.mConfig.close();
            this.mConfig = null;
        }
        if (this.mSuggestionsProvider != null) {
            this.mSuggestionsProvider.close();
            this.mSuggestionsProvider = null;
        }
    }

    public synchronized Handler getMainThreadHandler() {
        if (this.mUiThreadHandler == null) {
            this.mUiThreadHandler = new Handler(Looper.getMainLooper());
        }
        return this.mUiThreadHandler;
    }

    public synchronized NamedTaskExecutor getIconLoaderExecutor() {
        if (this.mIconLoaderExecutor == null) {
            this.mIconLoaderExecutor = createIconLoaderExecutor();
        }
        return this.mIconLoaderExecutor;
    }

    protected NamedTaskExecutor createIconLoaderExecutor() {
        ThreadFactory iconThreadFactory = new PriorityThreadFactory(10);
        return new PerNameExecutor(SingleThreadNamedTaskExecutor.factory(iconThreadFactory));
    }

    public void onStartupComplete() {
    }

    public synchronized Config getConfig() {
        if (this.mConfig == null) {
            this.mConfig = createConfig();
        }
        return this.mConfig;
    }

    protected Config createConfig() {
        return new Config(getContext());
    }

    public synchronized SearchSettings getSettings() {
        if (this.mSettings == null) {
            this.mSettings = createSettings();
            this.mSettings.upgradeSettingsIfNeeded();
        }
        return this.mSettings;
    }

    protected SearchSettings createSettings() {
        return new SearchSettingsImpl(getContext(), getConfig());
    }

    public NamedTaskExecutor getSourceTaskExecutor() {
        checkThread();
        if (this.mSourceTaskExecutor == null) {
            this.mSourceTaskExecutor = createSourceTaskExecutor();
        }
        return this.mSourceTaskExecutor;
    }

    protected NamedTaskExecutor createSourceTaskExecutor() {
        ThreadFactory queryThreadFactory = getQueryThreadFactory();
        return new PerNameExecutor(SingleThreadNamedTaskExecutor.factory(queryThreadFactory));
    }

    protected ThreadFactory getQueryThreadFactory() {
        checkThread();
        if (this.mQueryThreadFactory == null) {
            this.mQueryThreadFactory = createQueryThreadFactory();
        }
        return this.mQueryThreadFactory;
    }

    protected ThreadFactory createQueryThreadFactory() {
        int priority = getConfig().getQueryThreadPriority();
        return new ThreadFactoryBuilder().setNameFormat("QSB #%d").setThreadFactory(new PriorityThreadFactory(priority)).build();
    }

    protected SuggestionsProvider getSuggestionsProvider() {
        checkThread();
        if (this.mSuggestionsProvider == null) {
            this.mSuggestionsProvider = createSuggestionsProvider();
        }
        return this.mSuggestionsProvider;
    }

    protected SuggestionsProvider createSuggestionsProvider() {
        return new SuggestionsProviderImpl(getConfig(), getSourceTaskExecutor(), getMainThreadHandler(), getLogger());
    }

    public SuggestionViewFactory getSuggestionViewFactory() {
        checkThread();
        if (this.mSuggestionViewFactory == null) {
            this.mSuggestionViewFactory = createSuggestionViewFactory();
        }
        return this.mSuggestionViewFactory;
    }

    protected SuggestionViewFactory createSuggestionViewFactory() {
        return new DefaultSuggestionViewFactory(getContext());
    }

    public GoogleSource getGoogleSource() {
        checkThread();
        if (this.mGoogleSource == null) {
            this.mGoogleSource = createGoogleSource();
        }
        return this.mGoogleSource;
    }

    protected GoogleSource createGoogleSource() {
        return new GoogleSuggestClient(getContext(), getMainThreadHandler(), getIconLoaderExecutor(), getConfig());
    }

    public VoiceSearch getVoiceSearch() {
        checkThread();
        if (this.mVoiceSearch == null) {
            this.mVoiceSearch = createVoiceSearch();
        }
        return this.mVoiceSearch;
    }

    protected VoiceSearch createVoiceSearch() {
        return new VoiceSearch(getContext());
    }

    public Logger getLogger() {
        checkThread();
        if (this.mLogger == null) {
            this.mLogger = createLogger();
        }
        return this.mLogger;
    }

    protected Logger createLogger() {
        return new EventLogLogger(getContext(), getConfig());
    }

    public SuggestionFormatter getSuggestionFormatter() {
        if (this.mSuggestionFormatter == null) {
            this.mSuggestionFormatter = createSuggestionFormatter();
        }
        return this.mSuggestionFormatter;
    }

    protected SuggestionFormatter createSuggestionFormatter() {
        return new LevenshteinSuggestionFormatter(getTextAppearanceFactory());
    }

    public TextAppearanceFactory getTextAppearanceFactory() {
        if (this.mTextAppearanceFactory == null) {
            this.mTextAppearanceFactory = createTextAppearanceFactory();
        }
        return this.mTextAppearanceFactory;
    }

    protected TextAppearanceFactory createTextAppearanceFactory() {
        return new TextAppearanceFactory(getContext());
    }

    public synchronized HttpHelper getHttpHelper() {
        if (this.mHttpHelper == null) {
            this.mHttpHelper = createHttpHelper();
        }
        return this.mHttpHelper;
    }

    protected HttpHelper createHttpHelper() {
        return new JavaNetHttpHelper(new JavaNetHttpHelper.PassThroughRewriter(), getConfig().getUserAgent());
    }

    public synchronized SearchBaseUrlHelper getSearchBaseUrlHelper() {
        if (this.mSearchBaseUrlHelper == null) {
            this.mSearchBaseUrlHelper = createSearchBaseUrlHelper();
        }
        return this.mSearchBaseUrlHelper;
    }

    protected SearchBaseUrlHelper createSearchBaseUrlHelper() {
        return new SearchBaseUrlHelper(getContext(), getHttpHelper(), getSettings(), ((SearchSettingsImpl) getSettings()).getSearchPreferences());
    }

    public Help getHelp() {
        return new Help(getContext(), getConfig());
    }
}
