package android.icu.impl.coll;

public final class CollationCompare {

    static final boolean f35assertionsDisabled;

    static {
        f35assertionsDisabled = !CollationCompare.class.desiredAssertionStatus();
    }

    public static int compareUpToQuaternary(CollationIterator left, CollationIterator right, CollationSettings settings) {
        long variableTop;
        long rightPrimary;
        long leftQuaternary;
        int rightIndex;
        long rightQuaternary;
        int leftIndex;
        int leftCase;
        int leftLower32;
        int leftCase2;
        int rightIndex2;
        int rightCase;
        int rightCase2;
        int leftIndex2;
        int leftCase3;
        int rightIndex3;
        int rightCase3;
        long p;
        int leftSecondary;
        int rightIndex4;
        int rightSecondary;
        int options = settings.options;
        if ((options & 12) == 0) {
            variableTop = 0;
        } else {
            variableTop = settings.variableTop + 1;
        }
        boolean anyVariable = false;
        while (true) {
            long ce = left.nextCE();
            long leftPrimary = ce >>> 32;
            if (leftPrimary < variableTop && leftPrimary > Collation.MERGE_SEPARATOR_PRIMARY) {
                anyVariable = true;
                do {
                    left.setCurrentCE((-4294967296L) & ce);
                    while (true) {
                        ce = left.nextCE();
                        leftPrimary = ce >>> 32;
                        if (leftPrimary != 0) {
                            break;
                        }
                        left.setCurrentCE(0L);
                    }
                    if (leftPrimary >= variableTop) {
                        break;
                    }
                } while (leftPrimary > Collation.MERGE_SEPARATOR_PRIMARY);
            }
            if (leftPrimary != 0) {
                do {
                    long ce2 = right.nextCE();
                    rightPrimary = ce2 >>> 32;
                    if (rightPrimary < variableTop && rightPrimary > Collation.MERGE_SEPARATOR_PRIMARY) {
                        anyVariable = true;
                        do {
                            right.setCurrentCE((-4294967296L) & ce2);
                            while (true) {
                                ce2 = right.nextCE();
                                rightPrimary = ce2 >>> 32;
                                if (rightPrimary != 0) {
                                    break;
                                }
                                right.setCurrentCE(0L);
                            }
                            if (rightPrimary >= variableTop) {
                                break;
                            }
                        } while (rightPrimary > Collation.MERGE_SEPARATOR_PRIMARY);
                    }
                } while (rightPrimary == 0);
                if (leftPrimary != rightPrimary) {
                    if (settings.hasReordering()) {
                        leftPrimary = settings.reorder(leftPrimary);
                        rightPrimary = settings.reorder(rightPrimary);
                    }
                    return leftPrimary < rightPrimary ? -1 : 1;
                }
                if (leftPrimary == 1) {
                    if (CollationSettings.getStrength(options) >= 1) {
                        if ((options & 2048) == 0) {
                            int leftIndex3 = 0;
                            int rightIndex5 = 0;
                            while (true) {
                                int leftIndex4 = leftIndex3 + 1;
                                int leftSecondary2 = ((int) left.getCE(leftIndex3)) >>> 16;
                                if (leftSecondary2 == 0) {
                                    leftIndex3 = leftIndex4;
                                } else {
                                    while (true) {
                                        rightIndex4 = rightIndex5 + 1;
                                        rightSecondary = ((int) right.getCE(rightIndex5)) >>> 16;
                                        if (rightSecondary != 0) {
                                            break;
                                        }
                                        rightIndex5 = rightIndex4;
                                    }
                                    if (leftSecondary2 != rightSecondary) {
                                        return leftSecondary2 < rightSecondary ? -1 : 1;
                                    }
                                    if (leftSecondary2 == 256) {
                                        break;
                                    }
                                    rightIndex5 = rightIndex4;
                                    leftIndex3 = leftIndex4;
                                }
                            }
                        } else {
                            int leftStart = 0;
                            int rightStart = 0;
                            while (true) {
                                int leftLimit = leftStart;
                                while (true) {
                                    long p2 = left.getCE(leftLimit) >>> 32;
                                    if (p2 <= Collation.MERGE_SEPARATOR_PRIMARY && p2 != 0) {
                                        break;
                                    }
                                    leftLimit++;
                                }
                                int rightLimit = rightStart;
                                while (true) {
                                    p = right.getCE(rightLimit) >>> 32;
                                    if (p <= Collation.MERGE_SEPARATOR_PRIMARY && p != 0) {
                                        break;
                                    }
                                    rightLimit++;
                                }
                                int leftIndex5 = leftLimit;
                                int rightIndex6 = rightLimit;
                                do {
                                    leftSecondary = 0;
                                    while (leftSecondary == 0 && leftIndex5 > leftStart) {
                                        leftIndex5--;
                                        leftSecondary = ((int) left.getCE(leftIndex5)) >>> 16;
                                    }
                                    int rightSecondary2 = 0;
                                    while (rightSecondary2 == 0 && rightIndex6 > rightStart) {
                                        rightIndex6--;
                                        rightSecondary2 = ((int) right.getCE(rightIndex6)) >>> 16;
                                    }
                                    if (leftSecondary != rightSecondary2) {
                                        return leftSecondary < rightSecondary2 ? -1 : 1;
                                    }
                                } while (leftSecondary != 0);
                                if (!f35assertionsDisabled) {
                                    if (!(left.getCE(leftLimit) == right.getCE(rightLimit))) {
                                        throw new AssertionError();
                                    }
                                }
                                if (p == 1) {
                                    break;
                                }
                                leftStart = leftLimit + 1;
                                rightStart = rightLimit + 1;
                            }
                        }
                    }
                    if ((options & 1024) != 0) {
                        int strength = CollationSettings.getStrength(options);
                        int leftIndex6 = 0;
                        int rightIndex7 = 0;
                        do {
                            if (strength == 0) {
                                while (true) {
                                    leftIndex2 = leftIndex6 + 1;
                                    long ce3 = left.getCE(leftIndex6);
                                    leftCase3 = (int) ce3;
                                    if ((ce3 >>> 32) != 0 && leftCase3 != 0) {
                                        break;
                                    }
                                    leftIndex6 = leftIndex2;
                                }
                                leftLower32 = leftCase3;
                                leftCase2 = leftCase3 & Collation.CASE_MASK;
                                while (true) {
                                    rightIndex3 = rightIndex7 + 1;
                                    long ce4 = right.getCE(rightIndex7);
                                    rightCase3 = (int) ce4;
                                    if ((ce4 >>> 32) != 0 && rightCase3 != 0) {
                                        break;
                                    }
                                    rightIndex7 = rightIndex3;
                                }
                                rightCase2 = rightCase3 & Collation.CASE_MASK;
                                rightIndex7 = rightIndex3;
                                leftIndex6 = leftIndex2;
                            } else {
                                while (true) {
                                    leftIndex = leftIndex6 + 1;
                                    leftCase = (int) left.getCE(leftIndex6);
                                    if (((-65536) & leftCase) != 0) {
                                        break;
                                    }
                                    leftIndex6 = leftIndex;
                                }
                                leftLower32 = leftCase;
                                leftCase2 = leftCase & Collation.CASE_MASK;
                                while (true) {
                                    rightIndex2 = rightIndex7 + 1;
                                    rightCase = (int) right.getCE(rightIndex7);
                                    if (((-65536) & rightCase) != 0) {
                                        break;
                                    }
                                    rightIndex7 = rightIndex2;
                                }
                                rightCase2 = rightCase & Collation.CASE_MASK;
                                rightIndex7 = rightIndex2;
                                leftIndex6 = leftIndex;
                            }
                            if (leftCase2 != rightCase2) {
                                return (options & 256) == 0 ? leftCase2 < rightCase2 ? -1 : 1 : leftCase2 < rightCase2 ? 1 : -1;
                            }
                        } while ((leftLower32 >>> 16) != 256);
                    }
                    if (CollationSettings.getStrength(options) <= 1) {
                        return 0;
                    }
                    int tertiaryMask = CollationSettings.getTertiaryMask(options);
                    int leftIndex7 = 0;
                    int rightIndex8 = 0;
                    int anyQuaternaries = 0;
                    while (true) {
                        int leftIndex8 = leftIndex7 + 1;
                        int leftLower322 = (int) left.getCE(leftIndex7);
                        anyQuaternaries |= leftLower322;
                        if (!f35assertionsDisabled) {
                            if (!((leftLower322 & Collation.ONLY_TERTIARY_MASK) != 0 || (49344 & leftLower322) == 0)) {
                                throw new AssertionError();
                            }
                        }
                        int leftTertiary = leftLower322 & tertiaryMask;
                        if (leftTertiary == 0) {
                            leftIndex7 = leftIndex8;
                        } else {
                            while (true) {
                                int rightIndex9 = rightIndex8 + 1;
                                int rightLower32 = (int) right.getCE(rightIndex8);
                                anyQuaternaries |= rightLower32;
                                if (!f35assertionsDisabled) {
                                    if (!((rightLower32 & Collation.ONLY_TERTIARY_MASK) != 0 || (49344 & rightLower32) == 0)) {
                                        throw new AssertionError();
                                    }
                                }
                                int rightTertiary = rightLower32 & tertiaryMask;
                                if (rightTertiary == 0) {
                                    rightIndex8 = rightIndex9;
                                } else {
                                    if (leftTertiary != rightTertiary) {
                                        if (CollationSettings.sortsTertiaryUpperCaseFirst(options)) {
                                            if (leftTertiary > 256) {
                                                if (((-65536) & leftLower322) != 0) {
                                                    leftTertiary ^= Collation.CASE_MASK;
                                                } else {
                                                    leftTertiary += 16384;
                                                }
                                            }
                                            if (rightTertiary > 256) {
                                                rightTertiary = ((-65536) & rightLower32) != 0 ? rightTertiary ^ Collation.CASE_MASK : rightTertiary + 16384;
                                            }
                                        }
                                        return leftTertiary < rightTertiary ? -1 : 1;
                                    }
                                    if (leftTertiary != 256) {
                                        rightIndex8 = rightIndex9;
                                        leftIndex7 = leftIndex8;
                                    } else {
                                        if (CollationSettings.getStrength(options) <= 2) {
                                            return 0;
                                        }
                                        if (!anyVariable && (anyQuaternaries & 192) == 0) {
                                            return 0;
                                        }
                                        int leftIndex9 = 0;
                                        int rightIndex10 = 0;
                                        while (true) {
                                            int leftIndex10 = leftIndex9 + 1;
                                            long ce5 = left.getCE(leftIndex9);
                                            long leftQuaternary2 = ce5 & 65535;
                                            if (leftQuaternary2 <= 256) {
                                                leftQuaternary = ce5 >>> 32;
                                            } else {
                                                leftQuaternary = leftQuaternary2 | 4294967103L;
                                            }
                                            if (leftQuaternary == 0) {
                                                leftIndex9 = leftIndex10;
                                            } else {
                                                while (true) {
                                                    rightIndex = rightIndex10 + 1;
                                                    long ce6 = right.getCE(rightIndex10);
                                                    long rightQuaternary2 = ce6 & 65535;
                                                    if (rightQuaternary2 <= 256) {
                                                        rightQuaternary = ce6 >>> 32;
                                                    } else {
                                                        rightQuaternary = rightQuaternary2 | 4294967103L;
                                                    }
                                                    if (rightQuaternary != 0) {
                                                        break;
                                                    }
                                                    rightIndex10 = rightIndex;
                                                }
                                                if (leftQuaternary != rightQuaternary) {
                                                    if (settings.hasReordering()) {
                                                        leftQuaternary = settings.reorder(leftQuaternary);
                                                        rightQuaternary = settings.reorder(rightQuaternary);
                                                    }
                                                    return leftQuaternary < rightQuaternary ? -1 : 1;
                                                }
                                                if (leftQuaternary == 1) {
                                                    return 0;
                                                }
                                                rightIndex10 = rightIndex;
                                                leftIndex9 = leftIndex10;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
