package halflife.bus;

import halflife.bus.concurrent.Atom;
import halflife.bus.key.Key;
import halflife.bus.operation.PartitionOperation;
import halflife.bus.operation.SlidingWindowOperation;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import javax.lang.model.type.NullType;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class MatchedStream<V> extends FinalizedMatchedStream<V> {

  public MatchedStream() {
    super(new LinkedList<>());
  }

  protected MatchedStream(List<MatchedStream.StreamSupplier> suppliers) {
    super(suppliers);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <V1> MatchedStream<V1> map(Function<V, V1> mapper) {
    this.suppliers.add(new StreamSupplier<V, V1>() {
      @Override
      public <SRC extends Key, DST extends Key> KeyedConsumer<SRC, V> get(SRC src,
                                                                          DST dst,
                                                                          Stream<V1> stream) {
        return (key, value) -> {
          stream.notify(dst, mapper.apply(value));
        };
      }
    });
    return new MatchedStream<>(suppliers);
  }

  @SuppressWarnings(value = {"unchecked"})
  public MatchedStream<V> filter(Predicate<V> predicate) {
    this.suppliers.add(new StreamSupplier<V, V>() {
      @Override
      public <SRC extends Key, DST extends Key> KeyedConsumer<SRC, V> get(SRC src,
                                                                          DST dst,
                                                                          Stream<V> stream) {
        return (key, value) -> {
          if (predicate.test(value)) {
            stream.notify(dst, value);
          }
        };
      }
    });

    return new MatchedStream<>(suppliers);
  }

  @SuppressWarnings(value = {"unchecked"})
  public MatchedStream<List<V>> slide(UnaryOperator<List<V>> drop) {
    this.suppliers.add(new StreamSupplier<V, V>() {
      @Override
      public <SRC extends Key, DST extends Key> KeyedConsumer<SRC, V> get(SRC src,
                                                                          DST dst,
                                                                          Stream<V> stream) {
        Atom<PVector<V>> buffer = stream.stateProvider().makeAtom(src, TreePVector.empty());

        return new SlidingWindowOperation<SRC, DST, V>(stream.firehose(),
                                                       buffer,
                                                       drop,
                                                       dst);
      }
    });

    return new MatchedStream<>(suppliers);
  }

  @SuppressWarnings(value = {"unchecked"})
  public MatchedStream<List<V>> partition(Predicate<List<V>> emit) {
    this.suppliers.add(new StreamSupplier<V, V>() {
      @Override
      public <SRC extends Key, DST extends Key> KeyedConsumer<SRC, V> get(SRC src,
                                                                          DST dst,
                                                                          Stream<V> stream) {
        Atom<PVector<V>> buffer = stream.stateProvider().makeAtom(src, TreePVector.empty());

        return new PartitionOperation<SRC, DST, V>(stream.firehose(),
                                                   buffer,
                                                   emit,
                                                   dst);
      }
    });

    return new MatchedStream<>(suppliers);
  }

  @SuppressWarnings(value = {"unchecked"})
  public FinalizedMatchedStream consume(Consumer<V> consumer) {
    this.suppliers.add(new StreamSupplier<V, NullType>() {
      @Override
      public <SRC extends Key, DST extends Key> KeyedConsumer<SRC, V> get(SRC src,
                                                                          DST dst,
                                                                          Stream<NullType> stream) {
        return (key, value) -> consumer.accept(value);
      }
    });
    return new FinalizedMatchedStream(suppliers);
  }

  @FunctionalInterface
  public static interface StreamSupplier<V, V1> {
    public <SRC extends Key, DST extends Key> KeyedConsumer<SRC, V> get(SRC src,
                                                                        DST dst,
                                                                        Stream<V1> stream);
  }


}
