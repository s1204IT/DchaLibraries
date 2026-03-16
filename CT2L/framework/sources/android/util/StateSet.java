package android.util;

public class StateSet {
    public static final int[] WILD_CARD = new int[0];
    public static final int[] NOTHING = {0};

    public static boolean isWildCard(int[] stateSetOrSpec) {
        return stateSetOrSpec.length == 0 || stateSetOrSpec[0] == 0;
    }

    public static boolean stateSetMatches(int[] stateSpec, int[] stateSet) {
        boolean mustMatch;
        if (stateSet == null) {
            return stateSpec == null || isWildCard(stateSpec);
        }
        int stateSetSize = stateSet.length;
        for (int stateSpecState : stateSpec) {
            if (stateSpecState == 0) {
                return true;
            }
            if (stateSpecState > 0) {
                mustMatch = true;
            } else {
                mustMatch = false;
                stateSpecState = -stateSpecState;
            }
            boolean found = false;
            int j = 0;
            while (true) {
                if (j >= stateSetSize) {
                    break;
                }
                int state = stateSet[j];
                if (state == 0) {
                    if (mustMatch) {
                        return false;
                    }
                } else if (state != stateSpecState) {
                    j++;
                } else {
                    if (!mustMatch) {
                        return false;
                    }
                    found = true;
                }
            }
            if (mustMatch && !found) {
                return false;
            }
        }
        return true;
    }

    public static boolean stateSetMatches(int[] stateSpec, int state) {
        for (int stateSpecState : stateSpec) {
            if (stateSpecState == 0) {
                return true;
            }
            if (stateSpecState > 0) {
                if (state != stateSpecState) {
                    return false;
                }
            } else if (state == (-stateSpecState)) {
                return false;
            }
        }
        return true;
    }

    public static int[] trimStateSet(int[] states, int newSize) {
        if (states.length != newSize) {
            int[] trimmedStates = new int[newSize];
            System.arraycopy(states, 0, trimmedStates, 0, newSize);
            return trimmedStates;
        }
        return states;
    }

    public static String dump(int[] states) {
        StringBuilder sb = new StringBuilder();
        for (int i : states) {
            switch (i) {
                case 16842908:
                    sb.append("F ");
                    break;
                case 16842909:
                    sb.append("W ");
                    break;
                case 16842910:
                    sb.append("E ");
                    break;
                case 16842913:
                    sb.append("S ");
                    break;
                case 16842919:
                    sb.append("P ");
                    break;
            }
        }
        return sb.toString();
    }
}
