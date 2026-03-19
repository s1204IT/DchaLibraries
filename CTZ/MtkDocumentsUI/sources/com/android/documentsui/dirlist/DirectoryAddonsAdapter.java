package com.android.documentsui.dirlist;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.android.documentsui.Model;
import com.android.documentsui.base.EventListener;
import com.android.documentsui.dirlist.DocumentsAdapter;
import com.android.documentsui.dirlist.Message;
import java.util.List;

final class DirectoryAddonsAdapter extends DocumentsAdapter {
    private int mBreakPosition = -1;
    private final DocumentsAdapter mDelegate;
    private final DocumentsAdapter.Environment mEnv;
    private final Message mHeaderMessage;
    private final Message mInflateMessage;
    private final EventListener<Model.Update> mModelUpdateListener;

    static int access$308(DirectoryAddonsAdapter directoryAddonsAdapter) {
        int i = directoryAddonsAdapter.mBreakPosition;
        directoryAddonsAdapter.mBreakPosition = i + 1;
        return i;
    }

    static int access$310(DirectoryAddonsAdapter directoryAddonsAdapter) {
        int i = directoryAddonsAdapter.mBreakPosition;
        directoryAddonsAdapter.mBreakPosition = i - 1;
        return i;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i, List list) {
        onBindViewHolder((DocumentHolder) viewHolder, i, (List<Object>) list);
    }

    DirectoryAddonsAdapter(DocumentsAdapter.Environment environment, DocumentsAdapter documentsAdapter) {
        this.mEnv = environment;
        this.mDelegate = documentsAdapter;
        this.mHeaderMessage = new Message.HeaderMessage(environment, new Runnable() {
            @Override
            public final void run() {
                this.f$0.onDismissHeaderMessage();
            }
        });
        this.mInflateMessage = new Message.InflateMessage(environment, new Runnable() {
            @Override
            public final void run() {
                this.f$0.onDismissHeaderMessage();
            }
        });
        this.mDelegate.registerAdapterDataObserver(new EventRelay());
        this.mModelUpdateListener = new EventListener() {
            @Override
            public final void accept(Object obj) {
                this.f$0.onModelUpdate((Model.Update) obj);
            }
        };
    }

    @Override
    EventListener<Model.Update> getModelUpdateListener() {
        return this.mModelUpdateListener;
    }

    @Override
    public GridLayoutManager.SpanSizeLookup createSpanSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int i) {
                if (DirectoryAddonsAdapter.this.getItemViewType(i) == Integer.MAX_VALUE || DirectoryAddonsAdapter.this.getItemViewType(i) == 2147483646 || DirectoryAddonsAdapter.this.getItemViewType(i) == 2147483645) {
                    return DirectoryAddonsAdapter.this.mEnv.getColumnCount();
                }
                return 1;
            }
        };
    }

    @Override
    public DocumentHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        DocumentHolder inflateMessageDocumentHolder;
        switch (i) {
            case 2147483645:
                inflateMessageDocumentHolder = new InflateMessageDocumentHolder(this.mEnv.getContext(), viewGroup);
                this.mEnv.initDocumentHolder(inflateMessageDocumentHolder);
                break;
            case 2147483646:
                inflateMessageDocumentHolder = new HeaderMessageDocumentHolder(this.mEnv.getContext(), viewGroup);
                this.mEnv.initDocumentHolder(inflateMessageDocumentHolder);
                break;
            case Integer.MAX_VALUE:
                TransparentDividerDocumentHolder transparentDividerDocumentHolder = new TransparentDividerDocumentHolder(this.mEnv.getContext());
                this.mEnv.initDocumentHolder(transparentDividerDocumentHolder);
                return transparentDividerDocumentHolder;
            default:
                return this.mDelegate.createViewHolder(viewGroup, i);
        }
        return inflateMessageDocumentHolder;
    }

    private void onDismissHeaderMessage() {
        this.mHeaderMessage.reset();
        if (this.mBreakPosition > 0) {
            this.mBreakPosition--;
        }
        notifyItemRemoved(0);
    }

    public void onBindViewHolder(DocumentHolder documentHolder, int i, List<Object> list) {
        switch (documentHolder.getItemViewType()) {
            case 2147483645:
                ((InflateMessageDocumentHolder) documentHolder).bind(this.mInflateMessage);
                break;
            case 2147483646:
                ((HeaderMessageDocumentHolder) documentHolder).bind(this.mHeaderMessage);
                break;
            case Integer.MAX_VALUE:
                ((TransparentDividerDocumentHolder) documentHolder).bind(this.mEnv.getDisplayState());
                break;
            default:
                this.mDelegate.onBindViewHolder(documentHolder, toDelegatePosition(i), list);
                break;
        }
    }

    @Override
    public void onBindViewHolder(DocumentHolder documentHolder, int i) {
        switch (documentHolder.getItemViewType()) {
            case 2147483645:
                ((InflateMessageDocumentHolder) documentHolder).bind(this.mInflateMessage);
                break;
            case 2147483646:
                ((HeaderMessageDocumentHolder) documentHolder).bind(this.mHeaderMessage);
                break;
            case Integer.MAX_VALUE:
                ((TransparentDividerDocumentHolder) documentHolder).bind(this.mEnv.getDisplayState());
                break;
            default:
                this.mDelegate.onBindViewHolder(documentHolder, toDelegatePosition(i));
                break;
        }
    }

    @Override
    public int getItemCount() {
        int i = (this.mHeaderMessage.shouldShow() ? 1 : 0) + (this.mInflateMessage.shouldShow() ? 1 : 0);
        if (this.mBreakPosition == -1) {
            return this.mDelegate.getItemCount() + i;
        }
        return this.mDelegate.getItemCount() + i + 1;
    }

    private void onModelUpdate(Model.Update update) {
        this.mDelegate.getModelUpdateListener().accept(update);
        this.mBreakPosition = -1;
        this.mInflateMessage.update(update);
        this.mHeaderMessage.update(update);
        if (update.hasException()) {
            return;
        }
        Model model = this.mEnv.getModel();
        for (int i = 0; i < model.getModelIds().length; i++) {
            if (!isDirectory(model, i)) {
                if (i > 0) {
                    this.mBreakPosition = i + (this.mHeaderMessage.shouldShow() ? 1 : 0);
                    return;
                }
                return;
            }
        }
    }

    @Override
    public int getItemViewType(int i) {
        if (i == 0 && this.mHeaderMessage.shouldShow()) {
            return 2147483646;
        }
        if (i == this.mBreakPosition) {
            return Integer.MAX_VALUE;
        }
        if (i == getItemCount() - 1 && this.mInflateMessage.shouldShow()) {
            return 2147483645;
        }
        return this.mDelegate.getItemViewType(toDelegatePosition(i));
    }

    private int toDelegatePosition(int i) {
        boolean zShouldShow = this.mHeaderMessage.shouldShow();
        if (this.mBreakPosition != -1 && i > this.mBreakPosition) {
            i--;
        }
        return i - (zShouldShow ? 1 : 0);
    }

    private int toViewPosition(int i) {
        int i2 = i + (this.mHeaderMessage.shouldShow() ? 1 : 0);
        return (this.mBreakPosition == -1 || i2 < this.mBreakPosition) ? i2 : i2 + 1;
    }

    @Override
    public List<String> getStableIds() {
        return this.mDelegate.getStableIds();
    }

    @Override
    public int getAdapterPosition(String str) {
        return toViewPosition(this.mDelegate.getAdapterPosition(str));
    }

    @Override
    public String getStableId(int i) {
        if (i == this.mBreakPosition) {
            return null;
        }
        if (i == 0 && this.mHeaderMessage.shouldShow()) {
            return null;
        }
        if (i == getItemCount() - 1 && this.mInflateMessage.shouldShow()) {
            return null;
        }
        return this.mDelegate.getStableId(toDelegatePosition(i));
    }

    @Override
    public int getPosition(String str) {
        return toViewPosition(this.mDelegate.getPosition(str));
    }

    private final class EventRelay extends RecyclerView.AdapterDataObserver {
        static final boolean $assertionsDisabled = false;

        private EventRelay() {
        }

        @Override
        public void onChanged() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onItemRangeChanged(int i, int i2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onItemRangeChanged(int i, int i2, Object obj) {
            DirectoryAddonsAdapter.this.notifyItemRangeChanged(DirectoryAddonsAdapter.this.toViewPosition(i), i2, obj);
        }

        @Override
        public void onItemRangeInserted(int i, int i2) {
            if (i < DirectoryAddonsAdapter.this.mBreakPosition) {
                DirectoryAddonsAdapter.access$308(DirectoryAddonsAdapter.this);
            }
            DirectoryAddonsAdapter.this.notifyItemRangeInserted(DirectoryAddonsAdapter.this.toViewPosition(i), i2);
        }

        @Override
        public void onItemRangeRemoved(int i, int i2) {
            if (i < DirectoryAddonsAdapter.this.mBreakPosition) {
                DirectoryAddonsAdapter.access$310(DirectoryAddonsAdapter.this);
            }
            DirectoryAddonsAdapter.this.notifyItemRangeRemoved(DirectoryAddonsAdapter.this.toViewPosition(i), i2);
        }
    }
}
