package halflife.bus;

import halflife.bus.concurrent.Atom;
import halflife.bus.key.Key;
import halflife.bus.operation.PartitionOperation;
import halflife.bus.operation.SlidingWindowOperation;
import halflife.bus.registry.KeyMissMatcher;
import halflife.bus.state.DefaultStateProvider;
import halflife.bus.state.StateProvider;
import halflife.bus.state.StatefulSupplier;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.List;
import java.util.UUID;
import java.util.function.*;

public class Stream<V> {

  private final Firehose firehose;
  private final StateProvider stateProvider;

  public Stream(Firehose firehose) {
    this.firehose = firehose;
    this.stateProvider = new DefaultStateProvider();
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, DST extends Key> Stream<List<V>> partition(SRC source,
                                                                      DST destination,
                                                                      Predicate<List<V>> emit) {
    Atom<PVector<V>> buffer = stateProvider.makeAtom(source, TreePVector.empty());

    firehose.on(source, new PartitionOperation<SRC, DST, V>(firehose,
                                                            buffer,
                                                            emit,
                                                            destination));

    return new Stream<>(firehose);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, DST extends Key> Stream<List<V>> slide(SRC source,
                                                                  DST destination,
                                                                  UnaryOperator<List<V>> drop) {
    Atom<PVector<V>> buffer = stateProvider.makeAtom(source, TreePVector.empty());

    firehose.on(source, new SlidingWindowOperation<SRC, DST, V>(firehose,
                                                                buffer,
                                                                drop,
                                                                destination));

    return new Stream<>(firehose);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, DST extends Key, V1> Stream<V1> map(SRC source,
                                                               DST destination,
                                                               Function<V, V1> mapper) {
    firehose.on(source, new KeyedConsumer<SRC, V>() {
      @Override
      public void accept(SRC key, V value) {
        firehose.notify(destination, mapper.apply(value));
      }
    });

    return new Stream<>(firehose);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, DST extends Key, V1, ST> Stream<V1> map(SRC source,
                                                               DST destination,
                                                               BiFunction<Atom<ST>, V, V1> fn,
                                                               ST init) {
    Atom<ST> st = stateProvider.makeAtom(source, init);

    firehose.on(source, new KeyedConsumer<SRC, V>() {
      @Override
      public void accept(SRC key, V value) {
        firehose.notify(destination, fn.apply(st, value));
      }
    });

    return new Stream<>(firehose);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, DST extends Key, V1, ST> Stream<V1> map(SRC source,
                                                                   DST destination,
                                                                   StatefulSupplier<ST, Function<V, V1>> supplier,
                                                                   ST init) {
    Function<V, V1> mapper = supplier.get(stateProvider.makeAtom(source, init));

    firehose.on(source, new KeyedConsumer<SRC, V>() {
      @Override
      public void accept(SRC key, V value) {
        firehose.notify(destination, mapper.apply(value));
      }
    });

    return new Stream<>(firehose);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, DST extends Key> Stream<V> filter(SRC source,
                                                             DST destination,
                                                             Predicate<V> predicate) {
    firehose.on(source, new KeyedConsumer<SRC, V>() {
      @Override
      public void accept(SRC key, V value) {
        if(predicate.test(value)) {
          firehose.notify(destination, value);
        }
      }
    });

    return new Stream<>(firehose);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, V1> void consume(SRC source,
                                            Consumer<V1> consumer) {
    firehose.on(source, new KeyedConsumer<SRC, V1>() {
      @Override
      public void accept(SRC key_, V1 value) {
        consumer.accept(value);
      }
    });
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key> AnonymousStream<V> anonymous(SRC source) {
    return new AnonymousStream<>(source, this);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key> MatchedStream<V> matched(KeyMissMatcher<SRC> keyMatcher) {
    MatchedStream<V> downstream = new MatchedStream<>();
    firehose.miss(keyMatcher, downstream.subscribers(this));
    return downstream;
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key> Channel<V> channel() {
    Key k = new Key(new Object[] { UUID.randomUUID() });
    AnonymousStream<V> anonymousStream = new AnonymousStream<>(k,
                                                               this);
    return new Channel<V>(anonymousStream,
                          stateProvider.makeAtom(k, TreePVector.empty()));
  }

  public <SRC extends Key> void notify(SRC src, V v) {
    this.firehose.notify(src, v);
  }

}
