package com.android.setupwizardlib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.setupwizardlib.items.ItemGroup;
import com.android.setupwizardlib.items.ItemInflater;
import com.android.setupwizardlib.items.RecyclerItemAdapter;
import com.android.setupwizardlib.util.DrawableLayoutDirectionHelper;
import com.android.setupwizardlib.view.HeaderRecyclerView;

public class SetupWizardRecyclerLayout extends SetupWizardLayout {
    private RecyclerView.Adapter mAdapter;
    private Drawable mDefaultDivider;
    private Drawable mDivider;
    private DividerItemDecoration mDividerDecoration;
    private int mDividerInset;
    private View mHeader;
    private RecyclerView mRecyclerView;

    public SetupWizardRecyclerLayout(Context context) {
        this(context, 0, 0);
    }

    public SetupWizardRecyclerLayout(Context context, int template, int containerId) {
        super(context, template, containerId);
        init(context, null, 0);
    }

    public SetupWizardRecyclerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public SetupWizardRecyclerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.SuwSetupWizardRecyclerItemsLayout, defStyleAttr, 0);
        int xml = a.getResourceId(R$styleable.SuwSetupWizardRecyclerItemsLayout_android_entries, 0);
        if (xml != 0) {
            ItemGroup inflated = (ItemGroup) new ItemInflater(context).inflate(xml);
            this.mAdapter = new RecyclerItemAdapter(inflated);
            this.mAdapter.setHasStableIds(a.getBoolean(R$styleable.SuwSetupWizardRecyclerItemsLayout_suwHasStableIds, false));
            setAdapter(this.mAdapter);
        }
        int dividerInset = a.getDimensionPixelSize(R$styleable.SuwSetupWizardRecyclerItemsLayout_suwDividerInset, 0);
        if (dividerInset == 0) {
            dividerInset = getResources().getDimensionPixelSize(R$dimen.suw_items_icon_divider_inset);
        }
        setDividerInset(dividerInset);
        a.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (this.mDivider != null) {
            return;
        }
        updateDivider();
    }

    public RecyclerView.Adapter getAdapter() {
        return this.mAdapter;
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        this.mAdapter = adapter;
        getRecyclerView().setAdapter(adapter);
    }

    public RecyclerView getRecyclerView() {
        return this.mRecyclerView;
    }

    @Override
    protected ViewGroup findContainer(int containerId) {
        if (containerId == 0) {
            containerId = R$id.suw_recycler_view;
        }
        return super.findContainer(containerId);
    }

    @Override
    protected void onTemplateInflated() {
        initRecyclerView((RecyclerView) findViewById(R$id.suw_recycler_view));
    }

    protected void initRecyclerView(RecyclerView recyclerView) {
        this.mRecyclerView = recyclerView;
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (this.mRecyclerView instanceof HeaderRecyclerView) {
            this.mHeader = ((HeaderRecyclerView) this.mRecyclerView).getHeader();
        }
        this.mDividerDecoration = DividerItemDecoration.getDefault(getContext());
        this.mRecyclerView.addItemDecoration(this.mDividerDecoration);
    }

    @Override
    protected View onInflateTemplate(LayoutInflater inflater, int template) {
        if (template == 0) {
            template = R$layout.suw_recycler_template;
        }
        return super.onInflateTemplate(inflater, template);
    }

    @Override
    protected View findManagedViewById(int id) {
        View view;
        if (this.mHeader != null && (view = this.mHeader.findViewById(id)) != null) {
            return view;
        }
        return super.findViewById(id);
    }

    public void setDividerInset(int inset) {
        this.mDividerInset = inset;
        updateDivider();
    }

    private void updateDivider() {
        boolean shouldUpdate = true;
        if (Build.VERSION.SDK_INT >= 19) {
            shouldUpdate = isLayoutDirectionResolved();
        }
        if (!shouldUpdate) {
            return;
        }
        if (this.mDefaultDivider == null) {
            this.mDefaultDivider = this.mDividerDecoration.getDivider();
        }
        this.mDivider = DrawableLayoutDirectionHelper.createRelativeInsetDrawable(this.mDefaultDivider, this.mDividerInset, 0, 0, 0, this);
        this.mDividerDecoration.setDivider(this.mDivider);
    }
}
