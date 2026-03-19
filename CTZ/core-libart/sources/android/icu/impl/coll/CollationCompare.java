package android.icu.impl.coll;

public final class CollationCompare {
    static final boolean $assertionsDisabled = false;

    public static int compareUpToQuaternary(CollationIterator collationIterator, CollationIterator collationIterator2, CollationSettings collationSettings) {
        long j;
        boolean z;
        long jReorder;
        long jReorder2;
        int i;
        int ce;
        int i2;
        long jReorder3;
        int i3;
        long jReorder4;
        int i4;
        int ce2;
        int i5;
        int i6;
        int ce3;
        int i7;
        int i8;
        int i9;
        int ce4;
        long j2;
        long j3;
        int i10 = collationSettings.options;
        long j4 = 0;
        if ((i10 & 12) != 0) {
            j = collationSettings.variableTop + 1;
        } else {
            j = 0;
        }
        boolean z2 = false;
        while (true) {
            long jNextCE = collationIterator.nextCE();
            char c = ' ';
            long j5 = jNextCE >>> 32;
            if (j5 >= j || j5 <= Collation.MERGE_SEPARATOR_PRIMARY) {
                z = z2;
                jReorder = j5;
            } else {
                do {
                    collationIterator.setCurrentCE(jNextCE & (-4294967296L));
                    while (true) {
                        jNextCE = collationIterator.nextCE();
                        j3 = jNextCE >>> 32;
                        if (j3 != 0) {
                            break;
                        }
                        collationIterator.setCurrentCE(0L);
                    }
                    if (j3 >= j) {
                        break;
                    }
                } while (j3 > Collation.MERGE_SEPARATOR_PRIMARY);
                jReorder = j3;
                z = true;
            }
            if (jReorder != 0) {
                do {
                    long jNextCE2 = collationIterator2.nextCE();
                    long j6 = jNextCE2 >>> 32;
                    if (j6 >= j || j6 <= Collation.MERGE_SEPARATOR_PRIMARY) {
                        jReorder2 = j6;
                    } else {
                        do {
                            collationIterator2.setCurrentCE(jNextCE2 & (-4294967296L));
                            while (true) {
                                jNextCE2 = collationIterator2.nextCE();
                                j2 = jNextCE2 >>> 32;
                                if (j2 != 0) {
                                    break;
                                }
                                collationIterator2.setCurrentCE(0L);
                            }
                            if (j2 >= j) {
                                break;
                            }
                        } while (j2 > Collation.MERGE_SEPARATOR_PRIMARY);
                        jReorder2 = j2;
                        z = true;
                    }
                } while (jReorder2 == 0);
                if (jReorder != jReorder2) {
                    if (collationSettings.hasReordering()) {
                        jReorder = collationSettings.reorder(jReorder);
                        jReorder2 = collationSettings.reorder(jReorder2);
                    }
                    return jReorder < jReorder2 ? -1 : 1;
                }
                if (jReorder != 1) {
                    z2 = z;
                } else {
                    if (CollationSettings.getStrength(i10) >= 1) {
                        if ((i10 & 2048) == 0) {
                            int i11 = 0;
                            int i12 = 0;
                            while (true) {
                                int i13 = i11 + 1;
                                int ce5 = ((int) collationIterator.getCE(i11)) >>> 16;
                                if (ce5 == 0) {
                                    i11 = i13;
                                } else {
                                    while (true) {
                                        i9 = i12 + 1;
                                        ce4 = ((int) collationIterator2.getCE(i12)) >>> 16;
                                        if (ce4 != 0) {
                                            break;
                                        }
                                        i12 = i9;
                                    }
                                    if (ce5 != ce4) {
                                        return ce5 < ce4 ? -1 : 1;
                                    }
                                    if (ce5 == 256) {
                                        break;
                                    }
                                    i11 = i13;
                                    i12 = i9;
                                }
                            }
                        } else {
                            int i14 = 0;
                            int i15 = 0;
                            while (true) {
                                int i16 = i14;
                                while (true) {
                                    long ce6 = collationIterator.getCE(i16) >>> c;
                                    if (ce6 <= Collation.MERGE_SEPARATOR_PRIMARY && ce6 != j4) {
                                        break;
                                    }
                                    i16++;
                                    j4 = 0;
                                    c = ' ';
                                }
                                int i17 = i15;
                                while (true) {
                                    long ce7 = collationIterator2.getCE(i17) >>> c;
                                    if (ce7 <= Collation.MERGE_SEPARATOR_PRIMARY && ce7 != j4) {
                                        break;
                                    }
                                    i17++;
                                    j4 = 0;
                                    c = ' ';
                                }
                                int i18 = i16;
                                int i19 = i17;
                                while (true) {
                                    int i20 = i18;
                                    int ce8 = 0;
                                    while (ce8 == 0 && i20 > i14) {
                                        i20--;
                                        ce8 = ((int) collationIterator.getCE(i20)) >>> 16;
                                    }
                                    int i21 = i19;
                                    int ce9 = 0;
                                    while (ce9 == 0 && i21 > i15) {
                                        i21--;
                                        ce9 = ((int) collationIterator2.getCE(i21)) >>> 16;
                                        ce8 = ce8;
                                    }
                                    int i22 = ce8;
                                    if (i22 != ce9) {
                                        return i22 < ce9 ? -1 : 1;
                                    }
                                    if (i22 == 0) {
                                        break;
                                    }
                                    i18 = i20;
                                    i19 = i21;
                                }
                                i14 = i16 + 1;
                                i15 = i17 + 1;
                                j4 = 0;
                                c = ' ';
                            }
                        }
                    }
                    if ((i10 & 1024) != 0) {
                        int strength = CollationSettings.getStrength(i10);
                        int i23 = 0;
                        int i24 = 0;
                        while (true) {
                            if (strength == 0) {
                                while (true) {
                                    i4 = i23 + 1;
                                    long ce10 = collationIterator.getCE(i23);
                                    ce2 = (int) ce10;
                                    if ((ce10 >>> 32) != 0 && ce2 != 0) {
                                        break;
                                    }
                                    i23 = i4;
                                }
                                i5 = ce2 & Collation.CASE_MASK;
                                while (true) {
                                    i6 = i24 + 1;
                                    long ce11 = collationIterator2.getCE(i24);
                                    i8 = (int) ce11;
                                    if ((ce11 >>> 32) != 0 && i8 != 0) {
                                        break;
                                    }
                                    i24 = i6;
                                }
                                i7 = i8 & Collation.CASE_MASK;
                            } else {
                                while (true) {
                                    i4 = i23 + 1;
                                    ce2 = (int) collationIterator.getCE(i23);
                                    if ((ce2 & (-65536)) != 0) {
                                        break;
                                    }
                                    i23 = i4;
                                }
                                i5 = ce2 & Collation.CASE_MASK;
                                while (true) {
                                    i6 = i24 + 1;
                                    ce3 = (int) collationIterator2.getCE(i24);
                                    if ((ce3 & (-65536)) != 0) {
                                        break;
                                    }
                                    i24 = i6;
                                }
                                i7 = ce3 & Collation.CASE_MASK;
                            }
                            if (i5 != i7) {
                                return (256 & i10) == 0 ? i5 < i7 ? -1 : 1 : i5 < i7 ? 1 : -1;
                            }
                            if ((ce2 >>> 16) == 256) {
                                break;
                            }
                            i23 = i4;
                            i24 = i6;
                        }
                    }
                    if (CollationSettings.getStrength(i10) <= 1) {
                        return 0;
                    }
                    int tertiaryMask = CollationSettings.getTertiaryMask(i10);
                    int i25 = 0;
                    int i26 = 0;
                    int i27 = 0;
                    while (true) {
                        int i28 = i25 + 1;
                        int ce12 = (int) collationIterator.getCE(i25);
                        i26 |= ce12;
                        int i29 = ce12 & tertiaryMask;
                        if (i29 != 0) {
                            while (true) {
                                i = i27 + 1;
                                ce = (int) collationIterator2.getCE(i27);
                                i26 |= ce;
                                i2 = ce & tertiaryMask;
                                if (i2 != 0) {
                                    break;
                                }
                                i27 = i;
                            }
                            if (i29 != i2) {
                                if (CollationSettings.sortsTertiaryUpperCaseFirst(i10)) {
                                    if (i29 > 256) {
                                        if ((ce12 & (-65536)) != 0) {
                                            i29 ^= Collation.CASE_MASK;
                                        } else {
                                            i29 += 16384;
                                        }
                                    }
                                    if (i2 > 256) {
                                        i2 = (ce & (-65536)) != 0 ? i2 ^ Collation.CASE_MASK : i2 + 16384;
                                    }
                                }
                                return i29 < i2 ? -1 : 1;
                            }
                            if (i29 != 256) {
                                i25 = i28;
                                i27 = i;
                            } else {
                                if (CollationSettings.getStrength(i10) <= 2) {
                                    return 0;
                                }
                                if (!z && (i26 & 192) == 0) {
                                    return 0;
                                }
                                int i30 = 0;
                                int i31 = 0;
                                while (true) {
                                    int i32 = i30 + 1;
                                    long ce13 = collationIterator.getCE(i30);
                                    long j7 = ce13 & 65535;
                                    if (j7 <= 256) {
                                        jReorder3 = ce13 >>> 32;
                                    } else {
                                        jReorder3 = 4294967103L | j7;
                                    }
                                    if (jReorder3 != 0) {
                                        while (true) {
                                            i3 = i31 + 1;
                                            long ce14 = collationIterator2.getCE(i31);
                                            long j8 = ce14 & 65535;
                                            jReorder4 = j8 <= 256 ? ce14 >>> 32 : j8 | 4294967103L;
                                            if (jReorder4 != 0) {
                                                break;
                                            }
                                            i31 = i3;
                                        }
                                        if (jReorder3 != jReorder4) {
                                            if (collationSettings.hasReordering()) {
                                                jReorder3 = collationSettings.reorder(jReorder3);
                                                jReorder4 = collationSettings.reorder(jReorder4);
                                            }
                                            return jReorder3 < jReorder4 ? -1 : 1;
                                        }
                                        if (jReorder3 != 1) {
                                            i31 = i3;
                                        } else {
                                            return 0;
                                        }
                                    }
                                    i30 = i32;
                                }
                            }
                        } else {
                            i25 = i28;
                        }
                    }
                }
            } else {
                z2 = z;
            }
        }
    }
}
