package com.android.systemui.util.leak;

import android.os.Build;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.systemui.Dumpable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class LeakDetector implements Dumpable {
    public static final boolean ENABLED = Build.IS_DEBUGGABLE;
    private final TrackedCollections mTrackedCollections;
    private final TrackedGarbage mTrackedGarbage;
    private final TrackedObjects mTrackedObjects;

    @VisibleForTesting
    public LeakDetector(TrackedCollections trackedCollections, TrackedGarbage trackedGarbage, TrackedObjects trackedObjects) {
        this.mTrackedCollections = trackedCollections;
        this.mTrackedGarbage = trackedGarbage;
        this.mTrackedObjects = trackedObjects;
    }

    public <T> void trackInstance(T t) {
        if (this.mTrackedObjects != null) {
            this.mTrackedObjects.track(t);
        }
    }

    public <T> void trackCollection(Collection<T> collection, String str) {
        if (this.mTrackedCollections != null) {
            this.mTrackedCollections.track(collection, str);
        }
    }

    public void trackGarbage(Object obj) {
        if (this.mTrackedGarbage != null) {
            this.mTrackedGarbage.track(obj);
        }
    }

    TrackedGarbage getTrackedGarbage() {
        return this.mTrackedGarbage;
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println("SYSUI LEAK DETECTOR");
        indentingPrintWriter.increaseIndent();
        if (this.mTrackedCollections != null && this.mTrackedGarbage != null) {
            indentingPrintWriter.println("TrackedCollections:");
            indentingPrintWriter.increaseIndent();
            this.mTrackedCollections.dump(indentingPrintWriter, new Predicate() { // from class: com.android.systemui.util.leak.-$$Lambda$LeakDetector$pWx7s0HACocvPZyQWmuD0rk2VO8
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return LeakDetector.lambda$dump$0((Collection) obj);
                }
            });
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("TrackedObjects:");
            indentingPrintWriter.increaseIndent();
            this.mTrackedCollections.dump(indentingPrintWriter, new Predicate() { // from class: com.android.systemui.util.leak.-$$Lambda$oUbBhMkDSLCrT89WHUZWOlE1TKs
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return TrackedObjects.isTrackedObject((Collection) obj);
                }
            });
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.print("TrackedGarbage:");
            indentingPrintWriter.increaseIndent();
            this.mTrackedGarbage.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        } else {
            indentingPrintWriter.println("disabled");
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: NOT 
  (wrap:boolean:0x0000: INVOKE (r0v0 java.util.Collection) STATIC call: com.android.systemui.util.leak.TrackedObjects.isTrackedObject(java.util.Collection):boolean A[DONT_GENERATE, MD:(java.util.Collection<?>):boolean (m), REMOVE, WRAPPED] (LINE:113))
 */
    static /* synthetic */ boolean lambda$dump$0(Collection collection) {
        return !TrackedObjects.isTrackedObject(collection);
    }

    public static LeakDetector create() {
        if (ENABLED) {
            TrackedCollections trackedCollections = new TrackedCollections();
            return new LeakDetector(trackedCollections, new TrackedGarbage(trackedCollections), new TrackedObjects(trackedCollections));
        }
        return new LeakDetector(null, null, null);
    }
}
