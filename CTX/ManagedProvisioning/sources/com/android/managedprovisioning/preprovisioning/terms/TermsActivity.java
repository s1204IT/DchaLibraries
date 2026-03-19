package com.android.managedprovisioning.preprovisioning.terms;

import android.content.Intent;
import android.os.Bundle;
import android.util.ArraySet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toolbar;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.analytics.ProvisioningAnalyticsTracker;
import com.android.managedprovisioning.common.AccessibilityContextMenuMaker;
import com.android.managedprovisioning.common.ClickableSpanFactory;
import com.android.managedprovisioning.common.HtmlToSpannedParser;
import com.android.managedprovisioning.common.SetupLayoutActivity;
import com.android.managedprovisioning.common.StoreUtils;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.preprovisioning.WebActivity;
import com.android.managedprovisioning.preprovisioning.terms.TermsListAdapter;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TermsActivity extends SetupLayoutActivity {
    private final AccessibilityContextMenuMaker mContextMenuMaker;
    private final Set<Integer> mExpandedGroupsPosition;
    private final ProvisioningAnalyticsTracker mProvisioningAnalyticsTracker;
    private final TermsProvider mTermsProvider;

    public TermsActivity() {
        this(new StoreUtils.TextFileReader() {
            @Override
            public final String read(File file) {
                return StoreUtils.readString(file);
            }
        }, null);
    }

    TermsActivity(StoreUtils.TextFileReader textFileReader, AccessibilityContextMenuMaker accessibilityContextMenuMaker) {
        super(new Utils());
        this.mExpandedGroupsPosition = new ArraySet();
        this.mTermsProvider = new TermsProvider(this, textFileReader, this.mUtils);
        this.mProvisioningAnalyticsTracker = ProvisioningAnalyticsTracker.getInstance();
        this.mContextMenuMaker = accessibilityContextMenuMaker == null ? new AccessibilityContextMenuMaker(this) : accessibilityContextMenuMaker;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.terms_screen);
        setTitle(R.string.terms);
        setStatusBarColor(getColor(R.color.term_status_bar));
        final List<TermsDocument> terms = this.mTermsProvider.getTerms((ProvisioningParams) Preconditions.checkNotNull((ProvisioningParams) getIntent().getParcelableExtra("provisioningParams")), 0);
        final ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.terms_container);
        LayoutInflater layoutInflater = getLayoutInflater();
        AccessibilityContextMenuMaker accessibilityContextMenuMaker = new AccessibilityContextMenuMaker(this);
        HtmlToSpannedParser htmlToSpannedParser = new HtmlToSpannedParser(new ClickableSpanFactory(getColor(R.color.blue)), new HtmlToSpannedParser.UrlIntentFactory() {
            @Override
            public final Intent create(String str) {
                TermsActivity termsActivity = this.f$0;
                return WebActivity.createIntent(termsActivity, str, termsActivity.getWindow().getStatusBarColor());
            }
        });
        Objects.requireNonNull(expandableListView);
        expandableListView.setAdapter(new TermsListAdapter(terms, layoutInflater, accessibilityContextMenuMaker, htmlToSpannedParser, new TermsListAdapter.GroupExpandedInfo() {
            @Override
            public final boolean isGroupExpanded(int i) {
                return expandableListView.isGroupExpanded(i);
            }
        }));
        expandableListView.expandGroup(0);
        for (int i = 0; i < terms.size(); i++) {
            if (expandableListView.isGroupExpanded(i)) {
                this.mExpandedGroupsPosition.add(Integer.valueOf(i));
            }
        }
        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public final void onGroupExpand(int i2) {
                TermsActivity.lambda$onCreate$1(this.f$0, terms, expandableListView, i2);
            }
        });
        ((Toolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.finish();
            }
        });
        this.mProvisioningAnalyticsTracker.logNumberOfTermsDisplayed(this, terms.size());
    }

    public static void lambda$onCreate$1(TermsActivity termsActivity, List list, ExpandableListView expandableListView, int i) {
        termsActivity.mExpandedGroupsPosition.add(Integer.valueOf(i));
        for (int i2 = 0; i2 < list.size(); i2++) {
            if (i2 != i && expandableListView.isGroupExpanded(i2)) {
                expandableListView.collapseGroup(i2);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
        if (view instanceof TextView) {
            this.mContextMenuMaker.populateMenuContent(contextMenu, (TextView) view);
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
    public void onDestroy() {
        this.mProvisioningAnalyticsTracker.logNumberOfTermsRead(this, this.mExpandedGroupsPosition.size());
        super.onDestroy();
    }

    @Override
    protected int getMetricsCategory() {
        return 809;
    }
}
