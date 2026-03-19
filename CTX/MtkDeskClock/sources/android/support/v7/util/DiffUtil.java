package android.support.v7.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class DiffUtil {
    private static final Comparator<Snake> SNAKE_COMPARATOR = new Comparator<Snake>() {
        @Override
        public int compare(Snake o1, Snake o2) {
            int cmpX = o1.x - o2.x;
            return cmpX == 0 ? o1.y - o2.y : cmpX;
        }
    };

    private DiffUtil() {
    }

    @NonNull
    public static DiffResult calculateDiff(@NonNull Callback cb) {
        return calculateDiff(cb, true);
    }

    @NonNull
    public static DiffResult calculateDiff(@NonNull Callback cb, boolean detectMoves) {
        int oldSize = cb.getOldListSize();
        int newSize = cb.getNewListSize();
        List<Snake> snakes = new ArrayList<>();
        List<Range> stack = new ArrayList<>();
        stack.add(new Range(0, oldSize, 0, newSize));
        int max = oldSize + newSize + Math.abs(oldSize - newSize);
        int[] forward = new int[max * 2];
        int[] backward = new int[max * 2];
        List<Range> rangePool = new ArrayList<>();
        while (true) {
            List<Range> rangePool2 = rangePool;
            if (!stack.isEmpty()) {
                Range range = stack.remove(stack.size() - 1);
                Snake snake = diffPartial(cb, range.oldListStart, range.oldListEnd, range.newListStart, range.newListEnd, forward, backward, max);
                if (snake != null) {
                    if (snake.size > 0) {
                        snakes.add(snake);
                    }
                    snake.x += range.oldListStart;
                    snake.y += range.newListStart;
                    Range left = rangePool2.isEmpty() ? new Range() : rangePool2.remove(rangePool2.size() - 1);
                    left.oldListStart = range.oldListStart;
                    left.newListStart = range.newListStart;
                    if (snake.reverse) {
                        left.oldListEnd = snake.x;
                        left.newListEnd = snake.y;
                    } else if (snake.removal) {
                        left.oldListEnd = snake.x - 1;
                        left.newListEnd = snake.y;
                    } else {
                        left.oldListEnd = snake.x;
                        left.newListEnd = snake.y - 1;
                    }
                    stack.add(left);
                    if (!snake.reverse) {
                        range.oldListStart = snake.x + snake.size;
                        range.newListStart = snake.y + snake.size;
                    } else if (snake.removal) {
                        range.oldListStart = snake.x + snake.size + 1;
                        range.newListStart = snake.y + snake.size;
                    } else {
                        range.oldListStart = snake.x + snake.size;
                        range.newListStart = snake.y + snake.size + 1;
                    }
                    stack.add(range);
                } else {
                    rangePool2.add(range);
                }
                rangePool = rangePool2;
            } else {
                Collections.sort(snakes, SNAKE_COMPARATOR);
                return new DiffResult(cb, snakes, forward, backward, detectMoves);
            }
        }
    }

    private static Snake diffPartial(Callback cb, int startOld, int endOld, int startNew, int endNew, int[] forward, int[] backward, int kOffset) {
        int i;
        int x;
        boolean z;
        int y;
        int i2;
        int x2;
        boolean z2;
        int y2;
        int y3;
        int oldSize;
        int newSize;
        int oldSize2 = endOld - startOld;
        int newSize2 = endNew - startNew;
        if (endOld - startOld >= 1 && endNew - startNew >= 1) {
            int delta = oldSize2 - newSize2;
            int dLimit = ((oldSize2 + newSize2) + 1) / 2;
            Arrays.fill(forward, (kOffset - dLimit) - 1, kOffset + dLimit + 1, 0);
            Arrays.fill(backward, ((kOffset - dLimit) - 1) + delta, kOffset + dLimit + 1 + delta, oldSize2);
            boolean checkInFwd = delta % 2 != 0;
            int d = 0;
            while (d <= dLimit) {
                int k = -d;
                while (k <= d) {
                    if (k == (-d)) {
                        i2 = 1;
                    } else {
                        if (k == d) {
                            i2 = 1;
                        } else {
                            i2 = 1;
                            if (forward[(kOffset + k) - 1] < forward[kOffset + k + 1]) {
                            }
                            y2 = x2 - k;
                            while (true) {
                                y3 = y2;
                                if (x2 >= oldSize2) {
                                    oldSize = oldSize2;
                                    newSize = newSize2;
                                    break;
                                }
                                if (y3 < newSize2) {
                                    oldSize = oldSize2;
                                    newSize = newSize2;
                                    if (!cb.areItemsTheSame(startOld + x2, startNew + y3)) {
                                        break;
                                    }
                                    x2++;
                                    y2 = y3 + 1;
                                    oldSize2 = oldSize;
                                    newSize2 = newSize;
                                } else {
                                    oldSize = oldSize2;
                                    newSize = newSize2;
                                    break;
                                }
                            }
                            forward[kOffset + k] = x2;
                            if (checkInFwd && k >= (delta - d) + 1 && k <= (delta + d) - 1 && forward[kOffset + k] >= backward[kOffset + k]) {
                                Snake outSnake = new Snake();
                                outSnake.x = backward[kOffset + k];
                                outSnake.y = outSnake.x - k;
                                outSnake.size = forward[kOffset + k] - backward[kOffset + k];
                                outSnake.removal = z2;
                                outSnake.reverse = false;
                                return outSnake;
                            }
                            k += 2;
                            oldSize2 = oldSize;
                            newSize2 = newSize;
                        }
                        x2 = forward[(kOffset + k) - i2] + i2;
                        z2 = i2;
                        y2 = x2 - k;
                        while (true) {
                            y3 = y2;
                            if (x2 >= oldSize2) {
                            }
                            x2++;
                            y2 = y3 + 1;
                            oldSize2 = oldSize;
                            newSize2 = newSize;
                        }
                        forward[kOffset + k] = x2;
                        if (checkInFwd) {
                        }
                        k += 2;
                        oldSize2 = oldSize;
                        newSize2 = newSize;
                    }
                    x2 = forward[kOffset + k + i2];
                    z2 = 0;
                    y2 = x2 - k;
                    while (true) {
                        y3 = y2;
                        if (x2 >= oldSize2) {
                        }
                        x2++;
                        y2 = y3 + 1;
                        oldSize2 = oldSize;
                        newSize2 = newSize;
                    }
                    forward[kOffset + k] = x2;
                    if (checkInFwd) {
                    }
                    k += 2;
                    oldSize2 = oldSize;
                    newSize2 = newSize;
                }
                int oldSize3 = oldSize2;
                int newSize3 = newSize2;
                for (int k2 = -d; k2 <= d; k2 += 2) {
                    int backwardK = k2 + delta;
                    if (backwardK == d + delta) {
                        i = 1;
                    } else {
                        if (backwardK == (-d) + delta) {
                            i = 1;
                        } else {
                            i = 1;
                            if (backward[(kOffset + backwardK) - 1] < backward[kOffset + backwardK + 1]) {
                            }
                            for (y = x - backwardK; x > 0 && y > 0 && cb.areItemsTheSame((startOld + x) - 1, (startNew + y) - 1); y--) {
                                x--;
                            }
                            backward[kOffset + backwardK] = x;
                            if (!checkInFwd && k2 + delta >= (-d) && k2 + delta <= d && forward[kOffset + backwardK] >= backward[kOffset + backwardK]) {
                                Snake outSnake2 = new Snake();
                                outSnake2.x = backward[kOffset + backwardK];
                                outSnake2.y = outSnake2.x - backwardK;
                                outSnake2.size = forward[kOffset + backwardK] - backward[kOffset + backwardK];
                                outSnake2.removal = z;
                                outSnake2.reverse = true;
                                return outSnake2;
                            }
                        }
                        x = backward[(kOffset + backwardK) + i] - i;
                        z = i;
                        while (x > 0) {
                            x--;
                        }
                        backward[kOffset + backwardK] = x;
                        if (!checkInFwd) {
                        }
                    }
                    x = backward[(kOffset + backwardK) - i];
                    z = 0;
                    while (x > 0) {
                    }
                    backward[kOffset + backwardK] = x;
                    if (!checkInFwd) {
                    }
                }
                d++;
                oldSize2 = oldSize3;
                newSize2 = newSize3;
            }
            throw new IllegalStateException("DiffUtil hit an unexpected case while trying to calculate the optimal path. Please make sure your data is not changing during the diff calculation.");
        }
        return null;
    }

    public static abstract class Callback {
        public abstract boolean areContentsTheSame(int i, int i2);

        public abstract boolean areItemsTheSame(int i, int i2);

        public abstract int getNewListSize();

        public abstract int getOldListSize();

        @Nullable
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return null;
        }
    }

    public static abstract class ItemCallback<T> {
        public abstract boolean areContentsTheSame(@NonNull T t, @NonNull T t2);

        public abstract boolean areItemsTheSame(@NonNull T t, @NonNull T t2);

        @Nullable
        public Object getChangePayload(@NonNull T oldItem, @NonNull T newItem) {
            return null;
        }
    }

    static class Snake {
        boolean removal;
        boolean reverse;
        int size;
        int x;
        int y;

        Snake() {
        }
    }

    static class Range {
        int newListEnd;
        int newListStart;
        int oldListEnd;
        int oldListStart;

        public Range() {
        }

        public Range(int oldListStart, int oldListEnd, int newListStart, int newListEnd) {
            this.oldListStart = oldListStart;
            this.oldListEnd = oldListEnd;
            this.newListStart = newListStart;
            this.newListEnd = newListEnd;
        }
    }

    public static class DiffResult {
        private static final int FLAG_CHANGED = 2;
        private static final int FLAG_IGNORE = 16;
        private static final int FLAG_MASK = 31;
        private static final int FLAG_MOVED_CHANGED = 4;
        private static final int FLAG_MOVED_NOT_CHANGED = 8;
        private static final int FLAG_NOT_CHANGED = 1;
        private static final int FLAG_OFFSET = 5;
        private final Callback mCallback;
        private final boolean mDetectMoves;
        private final int[] mNewItemStatuses;
        private final int mNewListSize;
        private final int[] mOldItemStatuses;
        private final int mOldListSize;
        private final List<Snake> mSnakes;

        DiffResult(Callback callback, List<Snake> snakes, int[] oldItemStatuses, int[] newItemStatuses, boolean detectMoves) {
            this.mSnakes = snakes;
            this.mOldItemStatuses = oldItemStatuses;
            this.mNewItemStatuses = newItemStatuses;
            Arrays.fill(this.mOldItemStatuses, 0);
            Arrays.fill(this.mNewItemStatuses, 0);
            this.mCallback = callback;
            this.mOldListSize = callback.getOldListSize();
            this.mNewListSize = callback.getNewListSize();
            this.mDetectMoves = detectMoves;
            addRootSnake();
            findMatchingItems();
        }

        private void addRootSnake() {
            Snake firstSnake = this.mSnakes.isEmpty() ? null : this.mSnakes.get(0);
            if (firstSnake == null || firstSnake.x != 0 || firstSnake.y != 0) {
                Snake root = new Snake();
                root.x = 0;
                root.y = 0;
                root.removal = false;
                root.size = 0;
                root.reverse = false;
                this.mSnakes.add(0, root);
            }
        }

        private void findMatchingItems() {
            int posOld = this.mOldListSize;
            int posNew = this.mNewListSize;
            for (int i = this.mSnakes.size() - 1; i >= 0; i--) {
                Snake snake = this.mSnakes.get(i);
                int endX = snake.x + snake.size;
                int endY = snake.y + snake.size;
                if (this.mDetectMoves) {
                    while (posOld > endX) {
                        findAddition(posOld, posNew, i);
                        posOld--;
                    }
                    while (posNew > endY) {
                        findRemoval(posOld, posNew, i);
                        posNew--;
                    }
                }
                for (int j = 0; j < snake.size; j++) {
                    int oldItemPos = snake.x + j;
                    int newItemPos = snake.y + j;
                    boolean theSame = this.mCallback.areContentsTheSame(oldItemPos, newItemPos);
                    int changeFlag = theSame ? 1 : 2;
                    this.mOldItemStatuses[oldItemPos] = (newItemPos << 5) | changeFlag;
                    this.mNewItemStatuses[newItemPos] = (oldItemPos << 5) | changeFlag;
                }
                posOld = snake.x;
                posNew = snake.y;
            }
        }

        private void findAddition(int x, int y, int snakeIndex) {
            if (this.mOldItemStatuses[x - 1] != 0) {
                return;
            }
            findMatchingItem(x, y, snakeIndex, false);
        }

        private void findRemoval(int x, int y, int snakeIndex) {
            if (this.mNewItemStatuses[y - 1] != 0) {
                return;
            }
            findMatchingItem(x, y, snakeIndex, true);
        }

        private boolean findMatchingItem(int x, int y, int snakeIndex, boolean removal) {
            int myItemPos;
            int curX;
            int curY;
            int changeFlag;
            if (removal) {
                myItemPos = y - 1;
                curX = x;
                curY = y - 1;
            } else {
                myItemPos = x - 1;
                curX = x - 1;
                curY = y;
            }
            int curY2 = curY;
            int curX2 = curX;
            for (int curX3 = snakeIndex; curX3 >= 0; curX3--) {
                Snake snake = this.mSnakes.get(curX3);
                int endX = snake.x + snake.size;
                int endY = snake.y + snake.size;
                if (removal) {
                    for (int pos = curX2 - 1; pos >= endX; pos--) {
                        if (this.mCallback.areItemsTheSame(pos, myItemPos)) {
                            boolean theSame = this.mCallback.areContentsTheSame(pos, myItemPos);
                            changeFlag = theSame ? 8 : 4;
                            this.mNewItemStatuses[myItemPos] = (pos << 5) | 16;
                            this.mOldItemStatuses[pos] = (myItemPos << 5) | changeFlag;
                            return true;
                        }
                    }
                } else {
                    for (int pos2 = curY2 - 1; pos2 >= endY; pos2--) {
                        if (this.mCallback.areItemsTheSame(myItemPos, pos2)) {
                            boolean theSame2 = this.mCallback.areContentsTheSame(myItemPos, pos2);
                            changeFlag = theSame2 ? 8 : 4;
                            this.mOldItemStatuses[x - 1] = (pos2 << 5) | 16;
                            this.mNewItemStatuses[pos2] = ((x - 1) << 5) | changeFlag;
                            return true;
                        }
                    }
                }
                curX2 = snake.x;
                curY2 = snake.y;
            }
            return false;
        }

        public void dispatchUpdatesTo(@NonNull RecyclerView.Adapter adapter) {
            dispatchUpdatesTo(new AdapterListUpdateCallback(adapter));
        }

        public void dispatchUpdatesTo(@NonNull ListUpdateCallback updateCallback) {
            BatchingListUpdateCallback batchingCallback;
            int endY;
            int snakeSize;
            ListUpdateCallback listUpdateCallback = updateCallback;
            if (listUpdateCallback instanceof BatchingListUpdateCallback) {
                batchingCallback = (BatchingListUpdateCallback) listUpdateCallback;
            } else {
                batchingCallback = new BatchingListUpdateCallback(listUpdateCallback);
                listUpdateCallback = batchingCallback;
            }
            BatchingListUpdateCallback batchingCallback2 = batchingCallback;
            List<PostponedUpdate> postponedUpdates = new ArrayList<>();
            int posOld = this.mOldListSize;
            int posNew = this.mNewListSize;
            int snakeIndex = this.mSnakes.size() - 1;
            int posOld2 = posOld;
            int posNew2 = posNew;
            while (true) {
                int snakeIndex2 = snakeIndex;
                if (snakeIndex2 >= 0) {
                    Snake snake = this.mSnakes.get(snakeIndex2);
                    int snakeSize2 = snake.size;
                    int endX = snake.x + snakeSize2;
                    int endY2 = snake.y + snakeSize2;
                    if (endX < posOld2) {
                        endY = endY2;
                        dispatchRemovals(postponedUpdates, batchingCallback2, endX, posOld2 - endX, endX);
                    } else {
                        endY = endY2;
                    }
                    if (endY < posNew2) {
                        snakeSize = snakeSize2;
                        dispatchAdditions(postponedUpdates, batchingCallback2, endX, posNew2 - endY, endY);
                    } else {
                        snakeSize = snakeSize2;
                    }
                    int i = snakeSize - 1;
                    while (true) {
                        int i2 = i;
                        if (i2 >= 0) {
                            if ((this.mOldItemStatuses[snake.x + i2] & 31) == 2) {
                                batchingCallback2.onChanged(snake.x + i2, 1, this.mCallback.getChangePayload(snake.x + i2, snake.y + i2));
                            }
                            i = i2 - 1;
                        }
                    }
                    posOld2 = snake.x;
                    posNew2 = snake.y;
                    snakeIndex = snakeIndex2 - 1;
                } else {
                    batchingCallback2.dispatchLastEvent();
                    return;
                }
            }
        }

        private static PostponedUpdate removePostponedUpdate(List<PostponedUpdate> updates, int pos, boolean removal) {
            for (int i = updates.size() - 1; i >= 0; i--) {
                PostponedUpdate update = updates.get(i);
                if (update.posInOwnerList == pos && update.removal == removal) {
                    updates.remove(i);
                    for (int j = i; j < updates.size(); j++) {
                        updates.get(j).currentPos += removal ? 1 : -1;
                    }
                    return update;
                }
            }
            return null;
        }

        private void dispatchAdditions(List<PostponedUpdate> postponedUpdates, ListUpdateCallback updateCallback, int start, int count, int globalIndex) {
            if (!this.mDetectMoves) {
                updateCallback.onInserted(start, count);
                return;
            }
            for (int i = count - 1; i >= 0; i--) {
                int status = this.mNewItemStatuses[globalIndex + i] & 31;
                if (status == 0) {
                    updateCallback.onInserted(start, 1);
                    for (PostponedUpdate update : postponedUpdates) {
                        update.currentPos++;
                    }
                } else if (status == 4 || status == 8) {
                    int pos = this.mNewItemStatuses[globalIndex + i] >> 5;
                    updateCallback.onMoved(removePostponedUpdate(postponedUpdates, pos, true).currentPos, start);
                    if (status == 4) {
                        updateCallback.onChanged(start, 1, this.mCallback.getChangePayload(pos, globalIndex + i));
                    }
                } else if (status == 16) {
                    postponedUpdates.add(new PostponedUpdate(globalIndex + i, start, false));
                } else {
                    throw new IllegalStateException("unknown flag for pos " + (globalIndex + i) + " " + Long.toBinaryString(status));
                }
            }
        }

        private void dispatchRemovals(List<PostponedUpdate> postponedUpdates, ListUpdateCallback updateCallback, int start, int count, int globalIndex) {
            if (!this.mDetectMoves) {
                updateCallback.onRemoved(start, count);
                return;
            }
            for (int i = count - 1; i >= 0; i--) {
                int status = this.mOldItemStatuses[globalIndex + i] & 31;
                if (status == 0) {
                    updateCallback.onRemoved(start + i, 1);
                    Iterator<PostponedUpdate> it = postponedUpdates.iterator();
                    while (it.hasNext()) {
                        it.next().currentPos--;
                    }
                } else if (status == 4 || status == 8) {
                    int pos = this.mOldItemStatuses[globalIndex + i] >> 5;
                    PostponedUpdate update = removePostponedUpdate(postponedUpdates, pos, false);
                    updateCallback.onMoved(start + i, update.currentPos - 1);
                    if (status == 4) {
                        updateCallback.onChanged(update.currentPos - 1, 1, this.mCallback.getChangePayload(globalIndex + i, pos));
                    }
                } else if (status == 16) {
                    postponedUpdates.add(new PostponedUpdate(globalIndex + i, start + i, true));
                } else {
                    throw new IllegalStateException("unknown flag for pos " + (globalIndex + i) + " " + Long.toBinaryString(status));
                }
            }
        }

        @VisibleForTesting
        List<Snake> getSnakes() {
            return this.mSnakes;
        }
    }

    private static class PostponedUpdate {
        int currentPos;
        int posInOwnerList;
        boolean removal;

        public PostponedUpdate(int posInOwnerList, int currentPos, boolean removal) {
            this.posInOwnerList = posInOwnerList;
            this.currentPos = currentPos;
            this.removal = removal;
        }
    }
}
