package com.android.gallery3d.filtershow.filters;

import java.util.Vector;

public abstract class FilterPointRepresentation extends FilterRepresentation {
    private Vector<FilterPoint> mCandidates;

    public FilterPointRepresentation(String type, int textid, int editorID) {
        super(type);
        this.mCandidates = new Vector<>();
        setFilterClass(ImageFilterRedEye.class);
        setFilterType(5);
        setTextId(textid);
        setEditorId(editorID);
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    public Vector<FilterPoint> getCandidates() {
        return this.mCandidates;
    }

    @Override
    public boolean isNil() {
        return getCandidates() == null || getCandidates().size() <= 0;
    }

    public Object getCandidate(int index) {
        return this.mCandidates.get(index);
    }

    public void addCandidate(FilterPoint c) {
        this.mCandidates.add(c);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (a instanceof FilterPointRepresentation) {
            FilterPointRepresentation representation = (FilterPointRepresentation) a;
            this.mCandidates.clear();
            for (FilterPoint redEyeCandidate : representation.mCandidates) {
                this.mCandidates.add(redEyeCandidate);
            }
        }
    }

    public void removeCandidate(RedEyeCandidate c) {
        this.mCandidates.remove(c);
    }

    public void clearCandidates() {
        this.mCandidates.clear();
    }

    public int getNumberOfCandidates() {
        return this.mCandidates.size();
    }
}
