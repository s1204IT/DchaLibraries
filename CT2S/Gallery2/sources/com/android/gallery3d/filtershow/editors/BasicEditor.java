package com.android.gallery3d.filtershow.editors;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.controller.ParameterInteger;
import com.android.gallery3d.filtershow.filters.FilterBasicRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;

public class BasicEditor extends ParametricEditor implements ParameterInteger {
    public static int ID = R.id.basicEditor;
    private final String LOGTAG;

    public BasicEditor() {
        super(ID, R.layout.filtershow_default_editor, R.id.basicEditor);
        this.LOGTAG = "BasicEditor";
    }

    protected BasicEditor(int id, int layoutID, int viewID) {
        super(id, layoutID, viewID);
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
        FilterRepresentation tmpRep = getLocalRepresentation();
        if (tmpRep == null || !(tmpRep instanceof FilterBasicRepresentation)) {
            return null;
        }
        return (FilterBasicRepresentation) tmpRep;
    }

    @Override
    public int getMaximum() {
        FilterBasicRepresentation rep = getBasicRepresentation();
        if (rep == null) {
            return 0;
        }
        return rep.getMaximum();
    }

    @Override
    public int getMinimum() {
        FilterBasicRepresentation rep = getBasicRepresentation();
        if (rep == null) {
            return 0;
        }
        return rep.getMinimum();
    }

    @Override
    public int getValue() {
        FilterBasicRepresentation rep = getBasicRepresentation();
        if (rep == null) {
            return 0;
        }
        return rep.getValue();
    }

    @Override
    public void setValue(int value) {
        FilterBasicRepresentation rep = getBasicRepresentation();
        if (rep != null) {
            rep.setValue(value);
            commitLocalRepresentation();
        }
    }

    @Override
    public String getParameterName() {
        FilterBasicRepresentation rep = getBasicRepresentation();
        return this.mContext.getString(rep.getTextId());
    }

    @Override
    public String getParameterType() {
        return "ParameterInteger";
    }

    @Override
    public void setController(Control c) {
    }

    @Override
    public void setFilterView(FilterView editor) {
    }
}
