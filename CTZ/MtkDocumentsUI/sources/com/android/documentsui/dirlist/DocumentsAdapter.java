package com.android.documentsui.dirlist;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import com.android.documentsui.ActionHandler;
import com.android.documentsui.Model;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.EventListener;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.State;
import java.util.List;

public abstract class DocumentsAdapter extends RecyclerView.Adapter<DocumentHolder> {

    interface Environment {
        ActionHandler getActionHandler();

        int getColumnCount();

        Context getContext();

        State getDisplayState();

        Features getFeatures();

        Model getModel();

        void initDocumentHolder(DocumentHolder documentHolder);

        boolean isDocumentEnabled(String str, int i);

        boolean isInSearchMode();

        boolean isSelected(String str);

        void onBindDocumentHolder(DocumentHolder documentHolder, Cursor cursor);
    }

    public abstract int getAdapterPosition(String str);

    abstract EventListener<Model.Update> getModelUpdateListener();

    public abstract int getPosition(String str);

    public abstract String getStableId(int i);

    public abstract List<String> getStableIds();

    GridLayoutManager.SpanSizeLookup createSpanSizeLookup() {
        throw new UnsupportedOperationException();
    }

    static boolean isDirectory(Cursor cursor) {
        if (cursor == null) {
            return false;
        }
        return "vnd.android.document/directory".equals(DocumentInfo.getCursorString(cursor, "mime_type"));
    }

    boolean isDirectory(Model model, int i) {
        Cursor item;
        if ((getStableIds() == null || getStableIds().size() != 0) && (item = model.getItem(getStableIds().get(i))) != null) {
            return isDirectory(item);
        }
        return false;
    }
}
