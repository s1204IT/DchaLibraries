package com.android.gallery3d.filtershow.filters;

import java.util.Iterator;
import java.util.Vector;

public abstract class FilterPointRepresentation extends FilterRepresentation {
    private Vector<FilterPoint> mCandidates;

    public FilterPointRepresentation(String str, int i, int i2) {
        super(str);
        this.mCandidates = new Vector<>();
        setFilterClass(ImageFilterRedEye.class);
        setFilterType(5);
        setTextId(i);
        setEditorId(i2);
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    public Vector<FilterPoint> getCandidates() {
        return this.mCandidates;
    }

    @Override
    public boolean isNil() {
        if (getCandidates() != null && getCandidates().size() > 0) {
            return false;
        }
        return true;
    }

    public Object getCandidate(int i) {
        return this.mCandidates.get(i);
    }

    public void addCandidate(FilterPoint filterPoint) {
        this.mCandidates.add(filterPoint);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (filterRepresentation instanceof FilterPointRepresentation) {
            this.mCandidates.clear();
            Iterator<FilterPoint> it = filterRepresentation.mCandidates.iterator();
            while (it.hasNext()) {
                this.mCandidates.add(it.next());
            }
        }
    }

    public void removeCandidate(RedEyeCandidate redEyeCandidate) {
        this.mCandidates.remove(redEyeCandidate);
    }

    public void clearCandidates() {
        this.mCandidates.clear();
    }

    public int getNumberOfCandidates() {
        return this.mCandidates.size();
    }
}
