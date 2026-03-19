package java.util.stream;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.DistinctOps;
import java.util.stream.ReferencePipeline;
import java.util.stream.Sink;
import java.util.stream.StreamSpliterators;

final class DistinctOps {
    private DistinctOps() {
    }

    class AnonymousClass1<T> extends ReferencePipeline.StatefulOp<T, T> {
        AnonymousClass1(AbstractPipeline abstractPipeline, StreamShape streamShape, int i) {
            super(abstractPipeline, streamShape, i);
        }

        <P_IN> Node<T> reduce(PipelineHelper<T> pipelineHelper, Spliterator<P_IN> spliterator) {
            return Nodes.node((Collection) ReduceOps.makeRef(new Supplier() {
                @Override
                public final Object get() {
                    return new LinkedHashSet();
                }
            }, new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    ((LinkedHashSet) obj).add(obj2);
                }
            }, new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    ((LinkedHashSet) obj).addAll((LinkedHashSet) obj2);
                }
            }).evaluateParallel(pipelineHelper, spliterator));
        }

        @Override
        public <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<T[]> intFunction) {
            if (StreamOpFlag.DISTINCT.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return pipelineHelper.evaluate(spliterator, false, intFunction);
            }
            if (StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return reduce(pipelineHelper, spliterator);
            }
            final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            final ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
            ForEachOps.makeRef(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    DistinctOps.AnonymousClass1.lambda$opEvaluateParallel$0(atomicBoolean, concurrentHashMap, obj);
                }
            }, false).evaluateParallel(pipelineHelper, spliterator);
            Set setKeySet = concurrentHashMap.keySet();
            if (atomicBoolean.get()) {
                HashSet hashSet = new HashSet(setKeySet);
                hashSet.add(null);
                setKeySet = hashSet;
            }
            return Nodes.node(setKeySet);
        }

        static void lambda$opEvaluateParallel$0(AtomicBoolean atomicBoolean, ConcurrentHashMap concurrentHashMap, Object obj) {
            if (obj == null) {
                atomicBoolean.set(true);
            } else {
                concurrentHashMap.putIfAbsent(obj, Boolean.TRUE);
            }
        }

        @Override
        public <P_IN> Spliterator<T> opEvaluateParallelLazy(PipelineHelper<T> pipelineHelper, Spliterator<P_IN> spliterator) {
            if (StreamOpFlag.DISTINCT.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return pipelineHelper.wrapSpliterator(spliterator);
            }
            if (StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return reduce(pipelineHelper, spliterator).spliterator();
            }
            return new StreamSpliterators.DistinctSpliterator(pipelineHelper.wrapSpliterator(spliterator));
        }

        @Override
        public Sink<T> opWrapSink(int i, Sink<T> sink) {
            Objects.requireNonNull(sink);
            if (StreamOpFlag.DISTINCT.isKnown(i)) {
                return sink;
            }
            if (StreamOpFlag.SORTED.isKnown(i)) {
                return new Sink.ChainedReference<T, T>(sink) {
                    T lastSeen;
                    boolean seenNull;

                    @Override
                    public void begin(long j) {
                        this.seenNull = false;
                        this.lastSeen = null;
                        this.downstream.begin(-1L);
                    }

                    @Override
                    public void end() {
                        this.seenNull = false;
                        this.lastSeen = null;
                        this.downstream.end();
                    }

                    @Override
                    public void accept(T t) {
                        if (t == null) {
                            if (!this.seenNull) {
                                this.seenNull = true;
                                Sink<? super E_OUT> sink2 = this.downstream;
                                this.lastSeen = null;
                                sink2.accept((Object) null);
                                return;
                            }
                            return;
                        }
                        if (this.lastSeen == null || !t.equals(this.lastSeen)) {
                            Sink<? super E_OUT> sink3 = this.downstream;
                            this.lastSeen = t;
                            sink3.accept((Object) t);
                        }
                    }
                };
            }
            return new Sink.ChainedReference<T, T>(sink) {
                Set<T> seen;

                @Override
                public void begin(long j) {
                    this.seen = new HashSet();
                    this.downstream.begin(-1L);
                }

                @Override
                public void end() {
                    this.seen = null;
                    this.downstream.end();
                }

                @Override
                public void accept(T t) {
                    if (!this.seen.contains(t)) {
                        this.seen.add(t);
                        this.downstream.accept((Object) t);
                    }
                }
            };
        }
    }

    static <T> ReferencePipeline<T, T> makeRef(AbstractPipeline<?, T, ?> abstractPipeline) {
        return new AnonymousClass1(abstractPipeline, StreamShape.REFERENCE, StreamOpFlag.IS_DISTINCT | StreamOpFlag.NOT_SIZED);
    }
}
