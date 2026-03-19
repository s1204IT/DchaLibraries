package com.android.documentsui.dirlist;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;
import com.android.documentsui.Model;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.EventListener;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.dirlist.DocumentsAdapter;
import java.util.ArrayList;
import java.util.List;

final class ModelBackedDocumentsAdapter extends DocumentsAdapter {
    static final boolean $assertionsDisabled = false;
    private final DocumentsAdapter.Environment mEnv;
    private final Lookup<String, String> mFileTypeLookup;
    private final IconHelper mIconHelper;
    private List<String> mModelIds = new ArrayList();
    private EventListener<Model.Update> mModelUpdateListener = new EventListener<Model.Update>() {
        @Override
        public void accept(Model.Update update) {
            if (update.hasException()) {
                ModelBackedDocumentsAdapter.this.onModelUpdateFailed(update.getException());
            } else {
                ModelBackedDocumentsAdapter.this.onModelUpdate(ModelBackedDocumentsAdapter.this.mEnv.getModel());
            }
        }
    };

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i, List list) {
        onBindViewHolder((DocumentHolder) viewHolder, i, (List<Object>) list);
    }

    public ModelBackedDocumentsAdapter(DocumentsAdapter.Environment environment, IconHelper iconHelper, Lookup<String, String> lookup) {
        this.mEnv = environment;
        this.mIconHelper = iconHelper;
        this.mFileTypeLookup = lookup;
    }

    @Override
    EventListener<Model.Update> getModelUpdateListener() {
        return this.mModelUpdateListener;
    }

    @Override
    public DocumentHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        DocumentHolder listDocumentHolder;
        switch (this.mEnv.getDisplayState().derivedMode) {
            case 1:
                listDocumentHolder = new ListDocumentHolder(this.mEnv.getContext(), viewGroup, this.mIconHelper, this.mFileTypeLookup);
                break;
            case 2:
                switch (i) {
                    case 1:
                        listDocumentHolder = new GridDocumentHolder(this.mEnv.getContext(), viewGroup, this.mIconHelper);
                        break;
                    case 2:
                        listDocumentHolder = new GridDirectoryHolder(this.mEnv.getContext(), viewGroup);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported layout type.");
                }
                break;
            default:
                throw new IllegalStateException("Unsupported layout mode.");
        }
        this.mEnv.initDocumentHolder(listDocumentHolder);
        return listDocumentHolder;
    }

    public void onBindViewHolder(DocumentHolder documentHolder, int i, List<Object> list) {
        if (list.contains("Selection-Changed")) {
            documentHolder.setSelected(this.mEnv.isSelected(this.mModelIds.get(i)), true);
        } else {
            onBindViewHolder(documentHolder, i);
        }
    }

    @Override
    public void onBindViewHolder(DocumentHolder documentHolder, int i) {
        String str;
        Cursor item;
        if ((this.mModelIds != null && this.mModelIds.size() == 0) || (item = this.mEnv.getModel().getItem((str = this.mModelIds.get(i)))) == null) {
            return;
        }
        documentHolder.bind(item, str);
        boolean zIsDocumentEnabled = this.mEnv.isDocumentEnabled(DocumentInfo.getCursorString(item, "mime_type"), DocumentInfo.getCursorInt(item, "flags"));
        this.mEnv.isSelected(str);
        if (!zIsDocumentEnabled) {
        }
        documentHolder.setEnabled(zIsDocumentEnabled);
        documentHolder.setSelected(this.mEnv.isSelected(str), false);
        this.mEnv.onBindDocumentHolder(documentHolder, item);
    }

    @Override
    public int getItemCount() {
        return this.mModelIds.size();
    }

    private void onModelUpdate(Model model) {
        String[] modelIds = model.getModelIds();
        this.mModelIds = new ArrayList(modelIds.length);
        for (String str : modelIds) {
            this.mModelIds.add(str);
        }
    }

    private void onModelUpdateFailed(Exception exc) {
        Log.w("ModelBackedDocuments", "Model update failed.", exc);
        this.mModelIds.clear();
    }

    @Override
    public String getStableId(int i) {
        if (i < 0) {
            Log.d("ModelBackedDocuments", "getModelId adapterPosition = " + i);
            return null;
        }
        return this.mModelIds.get(i);
    }

    @Override
    public int getAdapterPosition(String str) {
        return this.mModelIds.indexOf(str);
    }

    @Override
    public List<String> getStableIds() {
        return this.mModelIds;
    }

    @Override
    public int getPosition(String str) {
        int iIndexOf = this.mModelIds.indexOf(str);
        if (iIndexOf >= 0) {
            return iIndexOf;
        }
        return -1;
    }

    @Override
    public int getItemViewType(int i) {
        if (isDirectory(this.mEnv.getModel(), i)) {
            return 2;
        }
        return 1;
    }

    public static boolean isContentType(int i) {
        switch (i) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }
}
