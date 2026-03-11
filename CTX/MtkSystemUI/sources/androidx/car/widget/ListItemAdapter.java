package androidx.car.widget;

import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.car.R;
import androidx.car.utils.CarUxRestrictionsHelper;
import androidx.car.utils.ListItemBackgroundResolver;
import androidx.car.widget.ListItem;
import androidx.car.widget.PagedListView;
import java.util.function.Function;

public class ListItemAdapter extends RecyclerView.Adapter<ListItem.ViewHolder> implements PagedListView.DividerVisibilityManager, PagedListView.ItemCap {
    private int mBackgroundStyle;
    private Context mContext;
    private CarUxRestrictions mCurrentUxRestrictions;
    private final ListItemProvider mItemProvider;
    private int mListItemBackgroundColor;
    private int mListItemBodyTextAppearance;
    private int mListItemTitleTextAppearance;
    private final CarUxRestrictionsHelper mUxRestrictionsHelper;
    private final SparseIntArray mViewHolderLayoutResIds = new SparseIntArray();
    private final SparseArray<Function<View, ListItem.ViewHolder>> mViewHolderCreator = new SparseArray<>();
    private int mMaxItems = -1;

    public ListItemAdapter(Context context, ListItemProvider itemProvider, int backgroundStyle) {
        this.mContext = context;
        this.mItemProvider = itemProvider;
        this.mBackgroundStyle = backgroundStyle;
        registerListItemViewType(1, R.layout.car_list_item_text_content, new Function() {
            @Override
            public final Object apply(Object obj) {
                return TextListItem.createViewHolder((View) obj);
            }
        });
        registerListItemViewType(2, R.layout.car_list_item_seekbar_content, new Function() {
            @Override
            public final Object apply(Object obj) {
                return SeekbarListItem.createViewHolder((View) obj);
            }
        });
        registerListItemViewType(3, R.layout.car_list_item_subheader_content, new Function() {
            @Override
            public final Object apply(Object obj) {
                return SubheaderListItem.createViewHolder((View) obj);
            }
        });
        this.mUxRestrictionsHelper = new CarUxRestrictionsHelper(context, new CarUxRestrictionsManager.OnUxRestrictionsChangedListener() {
            @Override
            public final void onUxRestrictionsChanged(CarUxRestrictions carUxRestrictions) {
                ListItemAdapter.lambda$new$8(this.f$0, carUxRestrictions);
            }
        });
    }

    public static void lambda$new$8(ListItemAdapter listItemAdapter, CarUxRestrictions carUxRestrictions) {
        listItemAdapter.mCurrentUxRestrictions = carUxRestrictions;
        listItemAdapter.notifyDataSetChanged();
    }

    public void registerListItemViewType(int viewType, int layoutResId, Function<View, ListItem.ViewHolder> function) {
        if (this.mViewHolderLayoutResIds.get(viewType) != 0 || this.mViewHolderCreator.get(viewType) != null) {
            throw new IllegalArgumentException("View type is already registered.");
        }
        this.mViewHolderCreator.put(viewType, function);
        this.mViewHolderLayoutResIds.put(viewType, layoutResId);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.mContext = recyclerView.getContext();
        TypedArray a = this.mContext.getTheme().obtainStyledAttributes(R.styleable.ListItem);
        this.mListItemBackgroundColor = a.getColor(R.styleable.ListItem_listItemBackgroundColor, this.mContext.getColor(R.color.car_card));
        this.mListItemTitleTextAppearance = a.getResourceId(R.styleable.ListItem_listItemTitleTextAppearance, R.style.TextAppearance_Car_Body1);
        this.mListItemBodyTextAppearance = a.getResourceId(R.styleable.ListItem_listItemBodyTextAppearance, R.style.TextAppearance_Car_Body2);
        a.recycle();
    }

    @Override
    public ListItem.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (this.mViewHolderLayoutResIds.get(viewType) == 0 || this.mViewHolderCreator.get(viewType) == null) {
            throw new IllegalArgumentException("Unregistered view type.");
        }
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        View itemView = inflater.inflate(this.mViewHolderLayoutResIds.get(viewType), parent, false);
        ViewGroup container = createListItemContainer();
        container.addView(itemView);
        return this.mViewHolderCreator.get(viewType).apply(container);
    }

    private ViewGroup createListItemContainer() {
        if (this.mBackgroundStyle == 2) {
            CardView card = new CardView(this.mContext);
            RecyclerView.LayoutParams cardLayoutParams = new RecyclerView.LayoutParams(-1, -2);
            cardLayoutParams.bottomMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_1);
            card.setLayoutParams(cardLayoutParams);
            card.setRadius(this.mContext.getResources().getDimensionPixelSize(R.dimen.car_radius_1));
            card.setCardBackgroundColor(this.mListItemBackgroundColor);
            return card;
        }
        FrameLayout frameLayout = new FrameLayout(this.mContext);
        frameLayout.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        if (this.mBackgroundStyle != 1) {
            frameLayout.setBackgroundColor(this.mListItemBackgroundColor);
            return frameLayout;
        }
        return frameLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return this.mItemProvider.get(position).getViewType();
    }

    @Override
    public void onBindViewHolder(ListItem.ViewHolder holder, int position) {
        if (this.mBackgroundStyle == 3) {
            ListItemBackgroundResolver.setBackground(holder.itemView, position, this.mItemProvider.size());
        }
        if (this.mCurrentUxRestrictions != null) {
            holder.applyUxRestrictions(this.mCurrentUxRestrictions);
        }
        ListItem item = this.mItemProvider.get(position);
        item.setTitleTextAppearance(this.mListItemTitleTextAppearance);
        item.setBodyTextAppearance(this.mListItemBodyTextAppearance);
        item.bind(holder);
    }

    @Override
    public int getItemCount() {
        if (this.mMaxItems == -1) {
            return this.mItemProvider.size();
        }
        return Math.min(this.mItemProvider.size(), this.mMaxItems);
    }

    @Override
    public void setMaxItems(int maxItems) {
        this.mMaxItems = maxItems;
    }

    @Override
    public boolean shouldHideDivider(int position) {
        return position >= 0 && position < getItemCount() && this.mItemProvider.get(position).shouldHideDivider();
    }
}
