package androidx.slice;

import android.content.Context;
import android.text.TextUtils;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.ListContent;
import java.util.ArrayList;
import java.util.List;

public class SliceMetadata {
    private Context mContext;
    private long mExpiry;
    private SliceItem mHeaderItem;
    private long mLastUpdated;
    private ListContent mListContent;
    private SliceActionImpl mPrimaryAction;
    private Slice mSlice;
    private List<SliceAction> mSliceActions;
    private int mTemplateType;

    public static SliceMetadata from(Context context, Slice slice) {
        return new SliceMetadata(context, slice);
    }

    private SliceMetadata(Context context, Slice slice) {
        this.mSlice = slice;
        this.mContext = context;
        SliceItem ttlItem = SliceQuery.find(slice, "long", "ttl", (String) null);
        if (ttlItem != null) {
            this.mExpiry = ttlItem.getTimestamp();
        }
        SliceItem updatedItem = SliceQuery.find(slice, "long", "last_updated", (String) null);
        if (updatedItem != null) {
            this.mLastUpdated = updatedItem.getTimestamp();
        }
        this.mListContent = new ListContent(context, slice, null, 0, 0);
        this.mHeaderItem = this.mListContent.getHeaderItem();
        this.mTemplateType = this.mListContent.getHeaderTemplateType();
        this.mSliceActions = this.mListContent.getSliceActions();
        SliceItem action = this.mListContent.getPrimaryAction();
        if (action != null) {
            this.mPrimaryAction = new SliceActionImpl(action);
        }
    }

    public List<String> getSliceKeywords() {
        List<SliceItem> itemList;
        SliceItem keywordGroup = SliceQuery.find(this.mSlice, "slice", "keywords", (String) null);
        if (keywordGroup == null || (itemList = SliceQuery.findAll(keywordGroup, "text")) == null) {
            return null;
        }
        ArrayList<String> stringList = new ArrayList<>();
        for (int i = 0; i < itemList.size(); i++) {
            String keyword = (String) itemList.get(i).getText();
            if (!TextUtils.isEmpty(keyword)) {
                stringList.add(keyword);
            }
        }
        return stringList;
    }

    public int getLoadingState() {
        boolean hasHintPartial = SliceQuery.find(this.mSlice, (String) null, "partial", (String) null) != null;
        if (this.mListContent.isValid()) {
            return hasHintPartial ? 1 : 2;
        }
        return 0;
    }

    public long getExpiry() {
        return this.mExpiry;
    }

    public long getLastUpdatedTime() {
        return this.mLastUpdated;
    }

    public static List<SliceAction> getSliceActions(Slice slice) {
        SliceItem actionGroup = SliceQuery.find(slice, "slice", "actions", (String) null);
        String[] hints = {"actions", "shortcut"};
        List<SliceItem> items = actionGroup != null ? SliceQuery.findAll(actionGroup, "slice", hints, (String[]) null) : null;
        if (items == null) {
            return null;
        }
        List<SliceAction> actions = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            SliceItem item = items.get(i);
            actions.add(new SliceActionImpl(item));
        }
        return actions;
    }
}
