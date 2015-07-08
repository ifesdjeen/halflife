package halflife.bus;

import halflife.bus.concurrent.Atom;
import halflife.bus.key.Key;
import halflife.bus.registry.KeyMissMatcher;
import halflife.bus.state.DefaultStateProvider;
import halflife.bus.state.StateProvider;
import halflife.bus.state.StatefulSupplier;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Stream<V> {

  private final Firehose firehose;
  private final StateProvider stateProvider;

  public Stream(Firehose firehose) {
    this.firehose = firehose;
    this.stateProvider = new DefaultStateProvider();
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
  public <SRC extends Key> void consume(SRC source,
                                        Consumer<V> consumer) {
    firehose.on(source, new KeyedConsumer<SRC, V>() {
      @Override
      public void accept(SRC key_, V value) {
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



  public <SRC extends Key> void notify(SRC src, V v) {
    this.firehose.notify(src, v);
  }

}
