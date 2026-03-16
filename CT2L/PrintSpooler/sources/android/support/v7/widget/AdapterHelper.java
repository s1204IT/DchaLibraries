package android.support.v7.widget;

import android.support.v4.util.Pools;
import android.support.v7.widget.OpReorderer;
import android.support.v7.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

class AdapterHelper implements OpReorderer.Callback {
    final Callback mCallback;
    final boolean mDisableRecycler;
    Runnable mOnItemProcessedCallback;
    final OpReorderer mOpReorderer;
    final ArrayList<UpdateOp> mPendingUpdates;
    final ArrayList<UpdateOp> mPostponedList;
    private Pools.Pool<UpdateOp> mUpdateOpPool;

    interface Callback {
        RecyclerView.ViewHolder findViewHolder(int i);

        void markViewHoldersUpdated(int i, int i2);

        void offsetPositionsForAdd(int i, int i2);

        void offsetPositionsForMove(int i, int i2);

        void offsetPositionsForRemovingInvisible(int i, int i2);

        void offsetPositionsForRemovingLaidOutOrNewView(int i, int i2);

        void onDispatchFirstPass(UpdateOp updateOp);

        void onDispatchSecondPass(UpdateOp updateOp);
    }

    AdapterHelper(Callback callback) {
        this(callback, false);
    }

    AdapterHelper(Callback callback, boolean disableRecycler) {
        this.mUpdateOpPool = new Pools.SimplePool(30);
        this.mPendingUpdates = new ArrayList<>();
        this.mPostponedList = new ArrayList<>();
        this.mCallback = callback;
        this.mDisableRecycler = disableRecycler;
        this.mOpReorderer = new OpReorderer(this);
    }

    void reset() {
        recycleUpdateOpsAndClearList(this.mPendingUpdates);
        recycleUpdateOpsAndClearList(this.mPostponedList);
    }

    void preProcess() {
        this.mOpReorderer.reorderOps(this.mPendingUpdates);
        int count = this.mPendingUpdates.size();
        for (int i = 0; i < count; i++) {
            UpdateOp op = this.mPendingUpdates.get(i);
            switch (op.cmd) {
                case 0:
                    applyAdd(op);
                    break;
                case 1:
                    applyRemove(op);
                    break;
                case 2:
                    applyUpdate(op);
                    break;
                case 3:
                    applyMove(op);
                    break;
            }
            if (this.mOnItemProcessedCallback != null) {
                this.mOnItemProcessedCallback.run();
            }
        }
        this.mPendingUpdates.clear();
    }

    void consumePostponedUpdates() {
        int count = this.mPostponedList.size();
        for (int i = 0; i < count; i++) {
            this.mCallback.onDispatchSecondPass(this.mPostponedList.get(i));
        }
        recycleUpdateOpsAndClearList(this.mPostponedList);
    }

    private void applyMove(UpdateOp op) {
        postponeAndUpdateViewHolders(op);
    }

    private void applyRemove(UpdateOp op) {
        int tmpStart = op.positionStart;
        int tmpCount = 0;
        int tmpEnd = op.positionStart + op.itemCount;
        int type = -1;
        int position = op.positionStart;
        while (position < tmpEnd) {
            boolean typeChanged = false;
            RecyclerView.ViewHolder vh = this.mCallback.findViewHolder(position);
            if (vh != null || canFindInPreLayout(position)) {
                if (type == 0) {
                    UpdateOp newOp = obtainUpdateOp(1, tmpStart, tmpCount);
                    dispatchAndUpdateViewHolders(newOp);
                    typeChanged = true;
                }
                type = 1;
            } else {
                if (type == 1) {
                    UpdateOp newOp2 = obtainUpdateOp(1, tmpStart, tmpCount);
                    postponeAndUpdateViewHolders(newOp2);
                    typeChanged = true;
                }
                type = 0;
            }
            if (typeChanged) {
                position -= tmpCount;
                tmpEnd -= tmpCount;
                tmpCount = 1;
            } else {
                tmpCount++;
            }
            position++;
        }
        if (tmpCount != op.itemCount) {
            recycleUpdateOp(op);
            op = obtainUpdateOp(1, tmpStart, tmpCount);
        }
        if (type == 0) {
            dispatchAndUpdateViewHolders(op);
        } else {
            postponeAndUpdateViewHolders(op);
        }
    }

    private void applyUpdate(UpdateOp op) {
        int tmpStart = op.positionStart;
        int tmpCount = 0;
        int tmpEnd = op.positionStart + op.itemCount;
        int type = -1;
        for (int position = op.positionStart; position < tmpEnd; position++) {
            RecyclerView.ViewHolder vh = this.mCallback.findViewHolder(position);
            if (vh != null || canFindInPreLayout(position)) {
                if (type == 0) {
                    UpdateOp newOp = obtainUpdateOp(2, tmpStart, tmpCount);
                    dispatchAndUpdateViewHolders(newOp);
                    tmpCount = 0;
                    tmpStart = position;
                }
                type = 1;
            } else {
                if (type == 1) {
                    UpdateOp newOp2 = obtainUpdateOp(2, tmpStart, tmpCount);
                    postponeAndUpdateViewHolders(newOp2);
                    tmpCount = 0;
                    tmpStart = position;
                }
                type = 0;
            }
            tmpCount++;
        }
        if (tmpCount != op.itemCount) {
            recycleUpdateOp(op);
            op = obtainUpdateOp(2, tmpStart, tmpCount);
        }
        if (type == 0) {
            dispatchAndUpdateViewHolders(op);
        } else {
            postponeAndUpdateViewHolders(op);
        }
    }

    private void dispatchAndUpdateViewHolders(UpdateOp op) {
        int positionMultiplier;
        if (op.cmd == 0 || op.cmd == 3) {
            throw new IllegalArgumentException("should not dispatch add or move for pre layout");
        }
        int tmpStart = updatePositionWithPostponed(op.positionStart, op.cmd);
        int tmpCnt = 1;
        int offsetPositionForPartial = op.positionStart;
        switch (op.cmd) {
            case 1:
                positionMultiplier = 0;
                break;
            case 2:
                positionMultiplier = 1;
                break;
            default:
                throw new IllegalArgumentException("op should be remove or update." + op);
        }
        for (int p = 1; p < op.itemCount; p++) {
            int pos = op.positionStart + (positionMultiplier * p);
            int updatedPos = updatePositionWithPostponed(pos, op.cmd);
            boolean continuous = false;
            switch (op.cmd) {
                case 1:
                    continuous = updatedPos == tmpStart;
                    break;
                case 2:
                    continuous = updatedPos == tmpStart + 1;
                    break;
            }
            if (continuous) {
                tmpCnt++;
            } else {
                UpdateOp tmp = obtainUpdateOp(op.cmd, tmpStart, tmpCnt);
                dispatchFirstPassAndUpdateViewHolders(tmp, offsetPositionForPartial);
                recycleUpdateOp(tmp);
                if (op.cmd == 2) {
                    offsetPositionForPartial += tmpCnt;
                }
                tmpStart = updatedPos;
                tmpCnt = 1;
            }
        }
        recycleUpdateOp(op);
        if (tmpCnt > 0) {
            UpdateOp tmp2 = obtainUpdateOp(op.cmd, tmpStart, tmpCnt);
            dispatchFirstPassAndUpdateViewHolders(tmp2, offsetPositionForPartial);
            recycleUpdateOp(tmp2);
        }
    }

    void dispatchFirstPassAndUpdateViewHolders(UpdateOp op, int offsetStart) {
        this.mCallback.onDispatchFirstPass(op);
        switch (op.cmd) {
            case 1:
                this.mCallback.offsetPositionsForRemovingInvisible(offsetStart, op.itemCount);
                return;
            case 2:
                this.mCallback.markViewHoldersUpdated(offsetStart, op.itemCount);
                return;
            default:
                throw new IllegalArgumentException("only remove and update ops can be dispatched in first pass");
        }
    }

    private int updatePositionWithPostponed(int pos, int cmd) {
        int start;
        int end;
        int count = this.mPostponedList.size();
        for (int i = count - 1; i >= 0; i--) {
            UpdateOp postponed = this.mPostponedList.get(i);
            if (postponed.cmd == 3) {
                if (postponed.positionStart < postponed.itemCount) {
                    start = postponed.positionStart;
                    end = postponed.itemCount;
                } else {
                    start = postponed.itemCount;
                    end = postponed.positionStart;
                }
                if (pos >= start && pos <= end) {
                    if (start == postponed.positionStart) {
                        if (cmd == 0) {
                            postponed.itemCount++;
                        } else if (cmd == 1) {
                            postponed.itemCount--;
                        }
                        pos++;
                    } else {
                        if (cmd == 0) {
                            postponed.positionStart++;
                        } else if (cmd == 1) {
                            postponed.positionStart--;
                        }
                        pos--;
                    }
                } else if (pos < postponed.positionStart) {
                    if (cmd == 0) {
                        postponed.positionStart++;
                        postponed.itemCount++;
                    } else if (cmd == 1) {
                        postponed.positionStart--;
                        postponed.itemCount--;
                    }
                }
            } else if (postponed.positionStart <= pos) {
                if (postponed.cmd == 0) {
                    pos -= postponed.itemCount;
                } else if (postponed.cmd == 1) {
                    pos += postponed.itemCount;
                }
            } else if (cmd == 0) {
                postponed.positionStart++;
            } else if (cmd == 1) {
                postponed.positionStart--;
            }
        }
        for (int i2 = this.mPostponedList.size() - 1; i2 >= 0; i2--) {
            UpdateOp op = this.mPostponedList.get(i2);
            if (op.cmd == 3) {
                if (op.itemCount == op.positionStart || op.itemCount < 0) {
                    this.mPostponedList.remove(i2);
                    recycleUpdateOp(op);
                }
            } else if (op.itemCount <= 0) {
                this.mPostponedList.remove(i2);
                recycleUpdateOp(op);
            }
        }
        return pos;
    }

    private boolean canFindInPreLayout(int position) {
        int count = this.mPostponedList.size();
        for (int i = 0; i < count; i++) {
            UpdateOp op = this.mPostponedList.get(i);
            if (op.cmd == 3) {
                if (findPositionOffset(op.itemCount, i + 1) == position) {
                    return true;
                }
            } else if (op.cmd == 0) {
                int end = op.positionStart + op.itemCount;
                for (int pos = op.positionStart; pos < end; pos++) {
                    if (findPositionOffset(pos, i + 1) == position) {
                        return true;
                    }
                }
            } else {
                continue;
            }
        }
        return false;
    }

    private void applyAdd(UpdateOp op) {
        postponeAndUpdateViewHolders(op);
    }

    private void postponeAndUpdateViewHolders(UpdateOp op) {
        this.mPostponedList.add(op);
        switch (op.cmd) {
            case 0:
                this.mCallback.offsetPositionsForAdd(op.positionStart, op.itemCount);
                return;
            case 1:
                this.mCallback.offsetPositionsForRemovingLaidOutOrNewView(op.positionStart, op.itemCount);
                return;
            case 2:
                this.mCallback.markViewHoldersUpdated(op.positionStart, op.itemCount);
                return;
            case 3:
                this.mCallback.offsetPositionsForMove(op.positionStart, op.itemCount);
                return;
            default:
                throw new IllegalArgumentException("Unknown update op type for " + op);
        }
    }

    boolean hasPendingUpdates() {
        return this.mPendingUpdates.size() > 0;
    }

    int findPositionOffset(int position) {
        return findPositionOffset(position, 0);
    }

    int findPositionOffset(int position, int firstPostponedItem) {
        int count = this.mPostponedList.size();
        for (int i = firstPostponedItem; i < count; i++) {
            UpdateOp op = this.mPostponedList.get(i);
            if (op.cmd == 3) {
                if (op.positionStart == position) {
                    position = op.itemCount;
                } else {
                    if (op.positionStart < position) {
                        position--;
                    }
                    if (op.itemCount <= position) {
                        position++;
                    }
                }
            } else if (op.positionStart > position) {
                continue;
            } else if (op.cmd == 1) {
                if (position < op.positionStart + op.itemCount) {
                    return -1;
                }
                position -= op.itemCount;
            } else if (op.cmd == 0) {
                position += op.itemCount;
            }
        }
        return position;
    }

    void consumeUpdatesInOnePass() {
        consumePostponedUpdates();
        int count = this.mPendingUpdates.size();
        for (int i = 0; i < count; i++) {
            UpdateOp op = this.mPendingUpdates.get(i);
            switch (op.cmd) {
                case 0:
                    this.mCallback.onDispatchSecondPass(op);
                    this.mCallback.offsetPositionsForAdd(op.positionStart, op.itemCount);
                    break;
                case 1:
                    this.mCallback.onDispatchSecondPass(op);
                    this.mCallback.offsetPositionsForRemovingInvisible(op.positionStart, op.itemCount);
                    break;
                case 2:
                    this.mCallback.onDispatchSecondPass(op);
                    this.mCallback.markViewHoldersUpdated(op.positionStart, op.itemCount);
                    break;
                case 3:
                    this.mCallback.onDispatchSecondPass(op);
                    this.mCallback.offsetPositionsForMove(op.positionStart, op.itemCount);
                    break;
            }
            if (this.mOnItemProcessedCallback != null) {
                this.mOnItemProcessedCallback.run();
            }
        }
        recycleUpdateOpsAndClearList(this.mPendingUpdates);
    }

    static class UpdateOp {
        int cmd;
        int itemCount;
        int positionStart;

        UpdateOp(int cmd, int positionStart, int itemCount) {
            this.cmd = cmd;
            this.positionStart = positionStart;
            this.itemCount = itemCount;
        }

        String cmdToString() {
            switch (this.cmd) {
                case 0:
                    return "add";
                case 1:
                    return "rm";
                case 2:
                    return "up";
                case 3:
                    return "mv";
                default:
                    return "??";
            }
        }

        public String toString() {
            return "[" + cmdToString() + ",s:" + this.positionStart + "c:" + this.itemCount + "]";
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UpdateOp op = (UpdateOp) o;
            if (this.cmd != op.cmd) {
                return false;
            }
            if (this.cmd == 3 && Math.abs(this.itemCount - this.positionStart) == 1 && this.itemCount == op.positionStart && this.positionStart == op.itemCount) {
                return true;
            }
            return this.itemCount == op.itemCount && this.positionStart == op.positionStart;
        }

        public int hashCode() {
            int result = this.cmd;
            return (((result * 31) + this.positionStart) * 31) + this.itemCount;
        }
    }

    @Override
    public UpdateOp obtainUpdateOp(int cmd, int positionStart, int itemCount) {
        UpdateOp op = this.mUpdateOpPool.acquire();
        if (op == null) {
            return new UpdateOp(cmd, positionStart, itemCount);
        }
        op.cmd = cmd;
        op.positionStart = positionStart;
        op.itemCount = itemCount;
        return op;
    }

    @Override
    public void recycleUpdateOp(UpdateOp op) {
        if (!this.mDisableRecycler) {
            this.mUpdateOpPool.release(op);
        }
    }

    void recycleUpdateOpsAndClearList(List<UpdateOp> ops) {
        int count = ops.size();
        for (int i = 0; i < count; i++) {
            recycleUpdateOp(ops.get(i));
        }
        ops.clear();
    }
}
