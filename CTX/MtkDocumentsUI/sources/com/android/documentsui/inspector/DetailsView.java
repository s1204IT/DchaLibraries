package com.android.documentsui.inspector;

import android.content.Context;
import android.text.format.Formatter;
import android.util.AttributeSet;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.inspector.InspectorController;

public class DetailsView extends TableView implements InspectorController.DetailsDisplay {
    public DetailsView(Context context) {
        this(context, null);
    }

    public DetailsView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DetailsView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    public void accept(DocumentInfo documentInfo) {
        put(R.string.sort_dimension_file_type, DocumentsApplication.getFileTypeLookup(getContext()).lookup(documentInfo.mimeType));
        if (documentInfo.size >= 0 && !documentInfo.isDirectory()) {
            put(R.string.sort_dimension_size, Formatter.formatFileSize(getContext(), documentInfo.size));
        }
        if (documentInfo.lastModified > 0) {
            put(R.string.sort_dimension_date, DateUtils.formatDate(getContext(), documentInfo.lastModified));
        }
        if (documentInfo.isPartial() && documentInfo.summary != null) {
            put(R.string.sort_dimension_summary, documentInfo.summary);
        }
    }

    @Override
    public void setChildrenCount(int i) {
        put(R.string.directory_items, String.valueOf(i));
    }
}
