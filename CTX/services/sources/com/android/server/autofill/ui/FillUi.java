package com.android.server.autofill.ui;

import android.R;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutofillWindowPresenter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import com.android.server.autofill.ui.FillUi;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class FillUi {
    private static final String TAG = "FillUi";
    private static final int THEME_ID = 16974777;
    private static final TypedValue sTempTypedValue = new TypedValue();
    private final ItemsAdapter mAdapter;
    private AnnounceFilterResult mAnnounceFilterResult;
    private final Callback mCallback;
    private int mContentHeight;
    private int mContentWidth;
    private final Context mContext;
    private boolean mDestroyed;
    private String mFilterText;
    private final View mFooter;
    private final boolean mFullScreen;
    private final View mHeader;
    private final ListView mListView;
    private final int mVisibleDatasetsMaxCount;
    private final AnchoredWindow mWindow;
    private final Point mTempPoint = new Point();
    private final AutofillWindowPresenter mWindowPresenter = new AutofillWindowPresenter();

    interface Callback {
        void dispatchUnhandledKey(KeyEvent keyEvent);

        void onCanceled();

        void onDatasetPicked(Dataset dataset);

        void onDestroy();

        void onResponsePicked(FillResponse fillResponse);

        void requestHideFillUi();

        void requestShowFillUi(int i, int i2, IAutofillWindowPresenter iAutofillWindowPresenter);

        void startIntentSender(IntentSender intentSender);
    }

    public static boolean isFullScreen(Context context) {
        if (Helper.sFullScreenMode != null) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "forcing full-screen mode to " + Helper.sFullScreenMode);
            }
            return Helper.sFullScreenMode.booleanValue();
        }
        return context.getPackageManager().hasSystemFeature("android.software.leanback");
    }

    FillUi(Context context, final FillResponse fillResponse, AutofillId autofillId, String str, OverlayControl overlayControl, CharSequence charSequence, Drawable drawable, Callback callback) {
        ViewGroup viewGroup;
        RemoteViews.OnClickHandler onClickHandlerNewClickBlocker;
        Pattern pattern;
        boolean z;
        String lowerCase;
        LinearLayout linearLayout;
        this.mCallback = callback;
        this.mFullScreen = isFullScreen(context);
        this.mContext = new ContextThemeWrapper(context, 16974777);
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(this.mContext);
        RemoteViews header = fillResponse.getHeader();
        RemoteViews footer = fillResponse.getFooter();
        if (this.mFullScreen) {
            viewGroup = (ViewGroup) layoutInflaterFrom.inflate(R.layout.app_anr_dialog, (ViewGroup) null);
        } else if (header != null || footer != null) {
            viewGroup = (ViewGroup) layoutInflaterFrom.inflate(R.layout.app_error_dialog, (ViewGroup) null);
        } else {
            viewGroup = (ViewGroup) layoutInflaterFrom.inflate(R.layout.am_compat_mode_dialog, (ViewGroup) null);
        }
        ViewGroup viewGroup2 = viewGroup;
        TextView textView = (TextView) viewGroup2.findViewById(R.id.accessibility_button_target_label);
        if (textView != null) {
            textView.setText(this.mContext.getString(R.string.RestrictedOnNormalTitle, charSequence));
        }
        ImageView imageView = (ImageView) viewGroup2.findViewById(R.id.accessibility_button_prompt);
        if (imageView != null) {
            imageView.setImageDrawable(drawable);
        }
        if (this.mFullScreen) {
            Point point = this.mTempPoint;
            this.mContext.getDisplay().getSize(point);
            this.mContentWidth = -1;
            this.mContentHeight = point.y / 2;
            if (Helper.sVerbose) {
                Slog.v(TAG, "initialized fillscreen LayoutParams " + this.mContentWidth + "," + this.mContentHeight);
            }
        }
        viewGroup2.addOnUnhandledKeyEventListener(new View.OnUnhandledKeyEventListener() {
            @Override
            public final boolean onUnhandledKeyEvent(View view, KeyEvent keyEvent) {
                return FillUi.lambda$new$0(this.f$0, view, keyEvent);
            }
        });
        if (Helper.sVisibleDatasetsMaxCount > 0) {
            this.mVisibleDatasetsMaxCount = Helper.sVisibleDatasetsMaxCount;
            if (Helper.sVerbose) {
                Slog.v(TAG, "overriding maximum visible datasets to " + this.mVisibleDatasetsMaxCount);
            }
        } else {
            this.mVisibleDatasetsMaxCount = this.mContext.getResources().getInteger(R.integer.auto_data_switch_availability_stability_time_threshold_millis);
        }
        RemoteViews.OnClickHandler onClickHandler = new RemoteViews.OnClickHandler() {
            public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent intent) {
                if (pendingIntent != null) {
                    FillUi.this.mCallback.startIntentSender(pendingIntent.getIntentSender());
                    return true;
                }
                return true;
            }
        };
        if (fillResponse.getAuthentication() != null) {
            this.mHeader = null;
            this.mListView = null;
            this.mFooter = null;
            this.mAdapter = null;
            ViewGroup viewGroup3 = (ViewGroup) viewGroup2.findViewById(R.id.accessibility_button_target_icon);
            try {
                fillResponse.getPresentation().setApplyTheme(16974777);
                View viewApply = fillResponse.getPresentation().apply(this.mContext, viewGroup2, onClickHandler);
                viewGroup3.addView(viewApply);
                viewGroup3.setFocusable(true);
                viewGroup3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        this.f$0.mCallback.onResponsePicked(fillResponse);
                    }
                });
                if (!this.mFullScreen) {
                    Point point2 = this.mTempPoint;
                    resolveMaxWindowSize(this.mContext, point2);
                    viewApply.getLayoutParams().width = this.mFullScreen ? point2.x : -2;
                    viewApply.getLayoutParams().height = -2;
                    viewGroup2.measure(View.MeasureSpec.makeMeasureSpec(point2.x, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(point2.y, Integer.MIN_VALUE));
                    this.mContentWidth = viewApply.getMeasuredWidth();
                    this.mContentHeight = viewApply.getMeasuredHeight();
                }
                this.mWindow = new AnchoredWindow(viewGroup2, overlayControl);
                requestShowFillUi();
                return;
            } catch (RuntimeException e) {
                callback.onCanceled();
                Slog.e(TAG, "Error inflating remote views", e);
                this.mWindow = null;
                return;
            }
        }
        int size = fillResponse.getDatasets().size();
        if (Helper.sVerbose) {
            Slog.v(TAG, "Number datasets: " + size + " max visible: " + this.mVisibleDatasetsMaxCount);
        }
        if (header != null) {
            onClickHandlerNewClickBlocker = newClickBlocker();
            header.setApplyTheme(16974777);
            this.mHeader = header.apply(this.mContext, null, onClickHandlerNewClickBlocker);
            LinearLayout linearLayout2 = (LinearLayout) viewGroup2.findViewById(R.id.accessibility_button_chooser_grid);
            if (Helper.sVerbose) {
                Slog.v(TAG, "adding header");
            }
            linearLayout2.addView(this.mHeader);
            linearLayout2.setVisibility(0);
        } else {
            this.mHeader = null;
            onClickHandlerNewClickBlocker = null;
        }
        if (footer != null && (linearLayout = (LinearLayout) viewGroup2.findViewById(R.id.accessibility_autoclick_type_panel)) != null) {
            onClickHandlerNewClickBlocker = onClickHandlerNewClickBlocker == null ? newClickBlocker() : onClickHandlerNewClickBlocker;
            footer.setApplyTheme(16974777);
            this.mFooter = footer.apply(this.mContext, null, onClickHandlerNewClickBlocker);
            if (Helper.sVerbose) {
                Slog.v(TAG, "adding footer");
            }
            linearLayout.addView(this.mFooter);
            linearLayout.setVisibility(0);
        } else {
            this.mFooter = null;
        }
        ArrayList arrayList = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            Dataset dataset = (Dataset) fillResponse.getDatasets().get(i);
            int iIndexOf = dataset.getFieldIds().indexOf(autofillId);
            if (iIndexOf >= 0) {
                RemoteViews fieldPresentation = dataset.getFieldPresentation(iIndexOf);
                if (fieldPresentation == null) {
                    Slog.w(TAG, "not displaying UI on field " + autofillId + " because service didn't provide a presentation for it on " + dataset);
                } else {
                    try {
                        if (Helper.sVerbose) {
                            Slog.v(TAG, "setting remote view for " + autofillId);
                        }
                        fieldPresentation.setApplyTheme(16974777);
                        View viewApply2 = fieldPresentation.apply(this.mContext, null, onClickHandler);
                        Dataset.DatasetFieldFilter filter = dataset.getFilter(iIndexOf);
                        if (filter == null) {
                            AutofillValue autofillValue = (AutofillValue) dataset.getFieldValues().get(iIndexOf);
                            lowerCase = (autofillValue == null || !autofillValue.isText()) ? null : autofillValue.getTextValue().toString().toLowerCase();
                            pattern = null;
                            z = true;
                        } else {
                            Pattern pattern2 = filter.pattern;
                            if (pattern2 == null) {
                                if (Helper.sVerbose) {
                                    Slog.v(TAG, "Explicitly disabling filter at id " + autofillId + " for dataset #" + iIndexOf);
                                }
                                pattern = pattern2;
                                z = false;
                            } else {
                                pattern = pattern2;
                                z = true;
                            }
                            lowerCase = null;
                        }
                        arrayList.add(new ViewItem(dataset, pattern, z, lowerCase, viewApply2));
                    } catch (RuntimeException e2) {
                        Slog.e(TAG, "Error inflating remote views", e2);
                    }
                }
            }
        }
        this.mAdapter = new ItemsAdapter(arrayList);
        this.mListView = (ListView) viewGroup2.findViewById(R.id.accessibility_button_prompt_prologue);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setVisibility(0);
        this.mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public final void onItemClick(AdapterView adapterView, View view, int i2, long j) {
                FillUi fillUi = this.f$0;
                fillUi.mCallback.onDatasetPicked(fillUi.mAdapter.getItem(i2).dataset);
            }
        });
        if (str == null) {
            this.mFilterText = null;
        } else {
            this.mFilterText = str.toLowerCase();
        }
        applyNewFilterText();
        this.mWindow = new AnchoredWindow(viewGroup2, overlayControl);
    }

    public static boolean lambda$new$0(FillUi fillUi, View view, KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == 4 || keyCode == 66 || keyCode == 111) {
            return false;
        }
        switch (keyCode) {
            case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
            case 20:
            case BackupHandler.MSG_OP_COMPLETE:
            case WindowManagerService.H.REPORT_HARD_KEYBOARD_STATUS_CHANGE:
            case WindowManagerService.H.BOOT_TIMEOUT:
                return false;
            default:
                fillUi.mCallback.dispatchUnhandledKey(keyEvent);
                return true;
        }
    }

    void requestShowFillUi() {
        this.mCallback.requestShowFillUi(this.mContentWidth, this.mContentHeight, this.mWindowPresenter);
    }

    private RemoteViews.OnClickHandler newClickBlocker() {
        return new RemoteViews.OnClickHandler() {
            public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent intent) {
                if (Helper.sVerbose) {
                    Slog.v(FillUi.TAG, "Ignoring click on " + view);
                    return true;
                }
                return true;
            }
        };
    }

    private void applyNewFilterText() {
        final int count = this.mAdapter.getCount();
        this.mAdapter.getFilter().filter(this.mFilterText, new Filter.FilterListener() {
            @Override
            public final void onFilterComplete(int i) {
                FillUi.lambda$applyNewFilterText$3(this.f$0, count, i);
            }
        });
    }

    public static void lambda$applyNewFilterText$3(FillUi fillUi, int i, int i2) {
        if (fillUi.mDestroyed) {
            return;
        }
        if (i2 <= 0) {
            if (Helper.sDebug) {
                Slog.d(TAG, "No dataset matches filter with " + (fillUi.mFilterText != null ? fillUi.mFilterText.length() : 0) + " chars");
            }
            fillUi.mCallback.requestHideFillUi();
            return;
        }
        if (fillUi.updateContentSize()) {
            fillUi.requestShowFillUi();
        }
        if (fillUi.mAdapter.getCount() <= fillUi.mVisibleDatasetsMaxCount) {
            fillUi.mListView.setVerticalScrollBarEnabled(false);
        } else {
            fillUi.mListView.setVerticalScrollBarEnabled(true);
            fillUi.mListView.onVisibilityAggregated(true);
        }
        if (fillUi.mAdapter.getCount() != i) {
            fillUi.mListView.requestLayout();
        }
    }

    public void setFilterText(String str) {
        String lowerCase;
        throwIfDestroyed();
        if (this.mAdapter == null) {
            if (TextUtils.isEmpty(str)) {
                requestShowFillUi();
                return;
            } else {
                this.mCallback.requestHideFillUi();
                return;
            }
        }
        if (str == null) {
            lowerCase = null;
        } else {
            lowerCase = str.toLowerCase();
        }
        if (Objects.equals(this.mFilterText, lowerCase)) {
            return;
        }
        this.mFilterText = lowerCase;
        applyNewFilterText();
    }

    public void destroy(boolean z) {
        throwIfDestroyed();
        if (this.mWindow != null) {
            this.mWindow.hide(false);
        }
        this.mCallback.onDestroy();
        if (z) {
            this.mCallback.requestHideFillUi();
        }
        this.mDestroyed = true;
    }

    private boolean updateContentSize() {
        boolean zUpdateWidth;
        boolean z;
        if (this.mAdapter == null) {
            return false;
        }
        if (this.mFullScreen) {
            return true;
        }
        if (this.mAdapter.getCount() <= 0) {
            if (this.mContentWidth != 0) {
                this.mContentWidth = 0;
                z = true;
            } else {
                z = false;
            }
            if (this.mContentHeight == 0) {
                return z;
            }
            this.mContentHeight = 0;
            return true;
        }
        Point point = this.mTempPoint;
        resolveMaxWindowSize(this.mContext, point);
        this.mContentWidth = 0;
        this.mContentHeight = 0;
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(point.x, Integer.MIN_VALUE);
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(point.y, Integer.MIN_VALUE);
        int count = this.mAdapter.getCount();
        if (this.mHeader != null) {
            this.mHeader.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
            zUpdateWidth = updateWidth(this.mHeader, point) | false | updateHeight(this.mHeader, point);
        } else {
            zUpdateWidth = false;
        }
        for (int i = 0; i < count; i++) {
            View view = this.mAdapter.getItem(i).view;
            view.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
            zUpdateWidth |= updateWidth(view, point);
            if (i < this.mVisibleDatasetsMaxCount) {
                zUpdateWidth |= updateHeight(view, point);
            }
        }
        if (this.mFooter != null) {
            this.mFooter.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
            return updateWidth(this.mFooter, point) | zUpdateWidth | updateHeight(this.mFooter, point);
        }
        return zUpdateWidth;
    }

    private boolean updateWidth(View view, Point point) {
        int iMax = Math.max(this.mContentWidth, Math.min(view.getMeasuredWidth(), point.x));
        if (iMax != this.mContentWidth) {
            this.mContentWidth = iMax;
            return true;
        }
        return false;
    }

    private boolean updateHeight(View view, Point point) {
        int iMin = this.mContentHeight + Math.min(view.getMeasuredHeight(), point.y);
        if (iMin != this.mContentHeight) {
            this.mContentHeight = iMin;
            return true;
        }
        return false;
    }

    private void throwIfDestroyed() {
        if (this.mDestroyed) {
            throw new IllegalStateException("cannot interact with a destroyed instance");
        }
    }

    private static void resolveMaxWindowSize(Context context, Point point) {
        context.getDisplay().getSize(point);
        TypedValue typedValue = sTempTypedValue;
        context.getTheme().resolveAttribute(R.bool.allow_test_udfps, typedValue, true);
        point.x = (int) typedValue.getFraction(point.x, point.x);
        context.getTheme().resolveAttribute(R.bool.allow_clear_initial_attach_data_profile, typedValue, true);
        point.y = (int) typedValue.getFraction(point.y, point.y);
    }

    private static class ViewItem {
        public final Dataset dataset;
        public final Pattern filter;
        public final boolean filterable;
        public final String value;
        public final View view;

        ViewItem(Dataset dataset, Pattern pattern, boolean z, String str, View view) {
            this.dataset = dataset;
            this.value = str;
            this.view = view;
            this.filter = pattern;
            this.filterable = z;
        }

        public boolean matches(CharSequence charSequence) {
            if (TextUtils.isEmpty(charSequence)) {
                return true;
            }
            if (!this.filterable) {
                return false;
            }
            String lowerCase = charSequence.toString().toLowerCase();
            if (this.filter != null) {
                return this.filter.matcher(lowerCase).matches();
            }
            if (this.value == null) {
                return this.dataset.getAuthentication() == null;
            }
            return this.value.toLowerCase().startsWith(lowerCase);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ViewItem:[view=");
            sb.append(this.view.getAutofillId());
            String id = this.dataset == null ? null : this.dataset.getId();
            if (id != null) {
                sb.append(", dataset=");
                sb.append(id);
            }
            if (this.value != null) {
                sb.append(", value=");
                sb.append(this.value.length());
                sb.append("_chars");
            }
            if (this.filterable) {
                sb.append(", filterable");
            }
            if (this.filter != null) {
                sb.append(", filter=");
                sb.append(this.filter.pattern().length());
                sb.append("_chars");
            }
            sb.append(']');
            return sb.toString();
        }
    }

    private final class AutofillWindowPresenter extends IAutofillWindowPresenter.Stub {
        private AutofillWindowPresenter() {
        }

        public void show(final WindowManager.LayoutParams layoutParams, Rect rect, boolean z, int i) {
            if (Helper.sVerbose) {
                Slog.v(FillUi.TAG, "AutofillWindowPresenter.show(): fit=" + z + ", params=" + Helper.paramsToString(layoutParams));
            }
            UiThread.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    FillUi.this.mWindow.show(layoutParams);
                }
            });
        }

        public void hide(Rect rect) {
            Handler handler = UiThread.getHandler();
            final AnchoredWindow anchoredWindow = FillUi.this.mWindow;
            Objects.requireNonNull(anchoredWindow);
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    anchoredWindow.hide();
                }
            });
        }
    }

    final class AnchoredWindow {
        private final View mContentView;
        private final OverlayControl mOverlayControl;
        private WindowManager.LayoutParams mShowParams;
        private boolean mShowing;
        private final WindowManager mWm;

        AnchoredWindow(View view, OverlayControl overlayControl) {
            this.mWm = (WindowManager) view.getContext().getSystemService(WindowManager.class);
            this.mContentView = view;
            this.mOverlayControl = overlayControl;
        }

        public void show(WindowManager.LayoutParams layoutParams) {
            this.mShowParams = layoutParams;
            if (Helper.sVerbose) {
                Slog.v(FillUi.TAG, "show(): showing=" + this.mShowing + ", params=" + Helper.paramsToString(layoutParams));
            }
            try {
                layoutParams.packageName = PackageManagerService.PLATFORM_PACKAGE_NAME;
                layoutParams.setTitle("Autofill UI");
                if (!this.mShowing) {
                    layoutParams.accessibilityTitle = this.mContentView.getContext().getString(R.string.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK_SUCCESS);
                    this.mWm.addView(this.mContentView, layoutParams);
                    this.mOverlayControl.hideOverlays();
                    this.mShowing = true;
                    return;
                }
                this.mWm.updateViewLayout(this.mContentView, layoutParams);
            } catch (WindowManager.BadTokenException e) {
                if (Helper.sDebug) {
                    Slog.d(FillUi.TAG, "Filed with with token " + layoutParams.token + " gone.");
                }
                FillUi.this.mCallback.onDestroy();
            } catch (IllegalStateException e2) {
                Slog.e(FillUi.TAG, "Exception showing window " + layoutParams, e2);
                FillUi.this.mCallback.onDestroy();
            }
        }

        void hide() {
            hide(true);
        }

        void hide(boolean z) {
            try {
                try {
                    if (this.mShowing) {
                        this.mWm.removeView(this.mContentView);
                        this.mShowing = false;
                    }
                } catch (IllegalStateException e) {
                    Slog.e(FillUi.TAG, "Exception hiding window ", e);
                    if (z) {
                        FillUi.this.mCallback.onDestroy();
                    }
                }
            } finally {
                this.mOverlayControl.showOverlays();
            }
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mCallback: ");
        printWriter.println(this.mCallback != null);
        printWriter.print(str);
        printWriter.print("mFullScreen: ");
        printWriter.println(this.mFullScreen);
        printWriter.print(str);
        printWriter.print("mVisibleDatasetsMaxCount: ");
        printWriter.println(this.mVisibleDatasetsMaxCount);
        if (this.mHeader != null) {
            printWriter.print(str);
            printWriter.print("mHeader: ");
            printWriter.println(this.mHeader);
        }
        if (this.mListView != null) {
            printWriter.print(str);
            printWriter.print("mListView: ");
            printWriter.println(this.mListView);
        }
        if (this.mFooter != null) {
            printWriter.print(str);
            printWriter.print("mFooter: ");
            printWriter.println(this.mFooter);
        }
        if (this.mAdapter != null) {
            printWriter.print(str);
            printWriter.print("mAdapter: ");
            printWriter.println(this.mAdapter);
        }
        if (this.mFilterText != null) {
            printWriter.print(str);
            printWriter.print("mFilterText: ");
            Helper.printlnRedactedText(printWriter, this.mFilterText);
        }
        printWriter.print(str);
        printWriter.print("mContentWidth: ");
        printWriter.println(this.mContentWidth);
        printWriter.print(str);
        printWriter.print("mContentHeight: ");
        printWriter.println(this.mContentHeight);
        printWriter.print(str);
        printWriter.print("mDestroyed: ");
        printWriter.println(this.mDestroyed);
        if (this.mWindow != null) {
            printWriter.print(str);
            printWriter.print("mWindow: ");
            String str2 = str + "  ";
            printWriter.println();
            printWriter.print(str2);
            printWriter.print("showing: ");
            printWriter.println(this.mWindow.mShowing);
            printWriter.print(str2);
            printWriter.print("view: ");
            printWriter.println(this.mWindow.mContentView);
            if (this.mWindow.mShowParams != null) {
                printWriter.print(str2);
                printWriter.print("params: ");
                printWriter.println(this.mWindow.mShowParams);
            }
            printWriter.print(str2);
            printWriter.print("screen coordinates: ");
            if (this.mWindow.mContentView != null) {
                int[] locationOnScreen = this.mWindow.mContentView.getLocationOnScreen();
                printWriter.print(locationOnScreen[0]);
                printWriter.print("x");
                printWriter.println(locationOnScreen[1]);
                return;
            }
            printWriter.println("N/A");
        }
    }

    private void announceSearchResultIfNeeded() {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if (this.mAnnounceFilterResult == null) {
                this.mAnnounceFilterResult = new AnnounceFilterResult();
            }
            this.mAnnounceFilterResult.post();
        }
    }

    private final class ItemsAdapter extends BaseAdapter implements Filterable {
        private final List<ViewItem> mAllItems;
        private final List<ViewItem> mFilteredItems = new ArrayList();

        ItemsAdapter(List<ViewItem> list) {
            this.mAllItems = Collections.unmodifiableList(new ArrayList(list));
            this.mFilteredItems.addAll(list);
        }

        class AnonymousClass1 extends Filter {
            AnonymousClass1() {
            }

            @Override
            protected Filter.FilterResults performFiltering(final CharSequence charSequence) {
                List list = (List) ItemsAdapter.this.mAllItems.stream().filter(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return ((FillUi.ViewItem) obj).matches(charSequence);
                    }
                }).collect(Collectors.toList());
                Filter.FilterResults filterResults = new Filter.FilterResults();
                filterResults.values = list;
                filterResults.count = list.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
                int size = ItemsAdapter.this.mFilteredItems.size();
                ItemsAdapter.this.mFilteredItems.clear();
                if (filterResults.count > 0) {
                    ItemsAdapter.this.mFilteredItems.addAll((List) filterResults.values);
                }
                if (size != ItemsAdapter.this.mFilteredItems.size()) {
                    FillUi.this.announceSearchResultIfNeeded();
                }
                ItemsAdapter.this.notifyDataSetChanged();
            }
        }

        @Override
        public Filter getFilter() {
            return new AnonymousClass1();
        }

        @Override
        public int getCount() {
            return this.mFilteredItems.size();
        }

        @Override
        public ViewItem getItem(int i) {
            return this.mFilteredItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return getItem(i).view;
        }

        public String toString() {
            return "ItemsAdapter: [all=" + this.mAllItems + ", filtered=" + this.mFilteredItems + "]";
        }
    }

    private final class AnnounceFilterResult implements Runnable {
        private static final int SEARCH_RESULT_ANNOUNCEMENT_DELAY = 1000;

        private AnnounceFilterResult() {
        }

        public void post() {
            remove();
            FillUi.this.mListView.postDelayed(this, 1000L);
        }

        public void remove() {
            FillUi.this.mListView.removeCallbacks(this);
        }

        @Override
        public void run() {
            String quantityString;
            int count = FillUi.this.mListView.getAdapter().getCount();
            if (count <= 0) {
                quantityString = FillUi.this.mContext.getString(R.string.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_SUCCESS);
            } else {
                quantityString = FillUi.this.mContext.getResources().getQuantityString(R.plurals.pinpuk_attempts, count, Integer.valueOf(count));
            }
            FillUi.this.mListView.announceForAccessibility(quantityString);
        }
    }
}
