package com.android.gallery3d.filtershow.editors;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.controller.ParameterInteger;
import com.android.gallery3d.filtershow.filters.FilterBasicRepresentation;

public class BasicEditor extends ParametricEditor implements ParameterInteger {
    public static int ID = R.id.basicEditor;
    private final String LOGTAG;

    public BasicEditor() {
        super(ID, R.layout.filtershow_default_editor, R.id.basicEditor);
        this.LOGTAG = "BasicEditor";
    }

    protected BasicEditor(int i, int i2, int i3) {
        super(i, i2, i3);
        this.LOGTAG = "BasicEditor";
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterBasicRepresentation)) {
            updateText();
        }
    }

    private FilterBasicRepresentation getBasicRepresentation() {
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation == 0 || !(localRepresentation instanceof FilterBasicRepresentation)) {
            return null;
        }
        return localRepresentation;
    }

    @Override
    public int getMaximum() {
        FilterBasicRepresentation basicRepresentation = getBasicRepresentation();
        if (basicRepresentation == null) {
            return 0;
        }
        return basicRepresentation.getMaximum();
    }

    @Override
    public int getMinimum() {
        FilterBasicRepresentation basicRepresentation = getBasicRepresentation();
        if (basicRepresentation == null) {
            return 0;
        }
        return basicRepresentation.getMinimum();
    }

    @Override
    public int getValue() {
        FilterBasicRepresentation basicRepresentation = getBasicRepresentation();
        if (basicRepresentation == null) {
            return 0;
        }
        return basicRepresentation.getValue();
    }

    @Override
    public void setValue(int i) {
        FilterBasicRepresentation basicRepresentation = getBasicRepresentation();
        if (basicRepresentation == null) {
            return;
        }
        basicRepresentation.setValue(i);
        commitLocalRepresentation();
    }

    @Override
    public String getParameterName() {
        return this.mContext.getString(getBasicRepresentation().getTextId());
    }

    @Override
    public String getParameterType() {
        return "ParameterInteger";
    }

    @Override
    public void setController(Control control) {
    }

    @Override
    public void setFilterView(FilterView filterView) {
    }
}
