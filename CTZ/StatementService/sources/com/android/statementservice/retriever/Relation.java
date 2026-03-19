package com.android.statementservice.retriever;

import java.util.regex.Pattern;

public final class Relation {
    private final String mDetail;
    private final String mKind;
    private static final Pattern KIND_PATTERN = Pattern.compile("^[a-z0-9_.]+$");
    private static final Pattern DETAIL_PATTERN = Pattern.compile("^([a-z0-9_.]+)$");

    private Relation(String str, String str2) {
        this.mKind = str;
        this.mDetail = str2;
    }

    public String getKind() {
        return this.mKind;
    }

    public String getDetail() {
        return this.mDetail;
    }

    public static Relation create(String str, String str2) throws AssociationServiceException {
        if (!KIND_PATTERN.matcher(str).matches() || !DETAIL_PATTERN.matcher(str2).matches()) {
            throw new AssociationServiceException("Relation not well formatted.");
        }
        return new Relation(str, str2);
    }

    public static Relation create(String str) throws AssociationServiceException {
        String[] strArrSplit = str.split("/", 2);
        if (strArrSplit.length != 2) {
            throw new AssociationServiceException("Relation not well formatted.");
        }
        return create(strArrSplit[0], strArrSplit[1]);
    }

    public boolean matches(Relation relation) {
        return getKind().equals(relation.getKind()) && getDetail().equals(relation.getDetail());
    }

    public String toString() {
        return getKind() + "/" + getDetail();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Relation relation = (Relation) obj;
        if (this.mDetail == null ? relation.mDetail != null : !this.mDetail.equals(relation.mDetail)) {
            return false;
        }
        if (this.mKind == null ? relation.mKind == null : this.mKind.equals(relation.mKind)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (this.mKind != null ? this.mKind.hashCode() : 0)) + (this.mDetail != null ? this.mDetail.hashCode() : 0);
    }
}
