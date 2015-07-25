package halflife.bus;

import halflife.bus.concurrent.Atom;
import halflife.bus.key.Key;
import halflife.bus.operation.PartitionOperation;
import halflife.bus.operation.SlidingWindowOperation;
import halflife.bus.registry.ConcurrentRegistry;
import halflife.bus.registry.DefaultingRegistry;
import halflife.bus.registry.KeyMissMatcher;
import halflife.bus.registry.Registry;
import halflife.bus.state.DefaultStateProvider;
import halflife.bus.state.StateProvider;
import halflife.bus.state.StatefulSupplier;
import org.pcollections.PVector;
import org.pcollections.TreePVector;
import reactor.Environment;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

public class Stream<V> {

  private final Environment                          environment;
  private final Firehose                             firehose;
  private final StateProvider                        stateProvider;
  private final Registry<Key, KeyedConsumer<Key, V>> registry;

  @SuppressWarnings(value = {"unchecked"})
  public Stream(Environment environment) {
    this(environment,
         new Firehose(environment.getDispatcher("sync"),
                      new ConcurrentRegistry<Key, KeyedConsumer<Key, V>>(),
                      null,
                      null));
  }


  @SuppressWarnings(value = {"unchecked"})
  public Stream(Environment environment,
                Consumer<Throwable> dispatchErrorHandler,
                Consumer<Throwable> consumeErrorHandler) {
    this(environment,
         new Firehose(environment.getDispatcher("sync"),
                      new ConcurrentRegistry<Key, KeyedConsumer<Key, V>>(),
                      new reactor.fn.Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                          dispatchErrorHandler.accept(throwable);
                        }
                      },
                      new reactor.fn.Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                          consumeErrorHandler.accept(throwable);
                        }
                      }));
  }

  @SuppressWarnings(value = {"unchecked"})
  protected Stream(Environment environment,
                   Firehose firehose) {
    this.environment = environment;
    this.firehose = firehose;
    this.stateProvider = new DefaultStateProvider();
    this.registry = firehose.getConsumerRegistry();
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

    return new Stream<>(environment,
                        firehose);
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

    return new Stream<>(environment,
                        firehose);
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

    return new Stream<>(environment,
                        firehose);
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

    return new Stream<>(environment,
                        firehose);
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

    return new Stream<>(environment,
                        firehose);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, DST extends Key> Stream<V> filter(SRC source,
                                                             DST destination,
                                                             Predicate<V> predicate) {
    firehose.on(source, new KeyedConsumer<SRC, V>() {
      @Override
      public void accept(SRC key, V value) {
        if (predicate.test(value)) {
          firehose.notify(destination, value);
        }
      }
    });

    return new Stream<>(environment,
                        firehose);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, DST extends Key> Stream<V> debounce(SRC source,
                                                               DST destination,
                                                               int period,
                                                               TimeUnit timeUnit) {
    final Atom<V> debounced = stateProvider.makeAtom(source, null);

    firehose.getTimer().schedule(discarded_ -> {
      V currentDebounced = debounced.swapReturnOld(old_ -> null);
      if (currentDebounced != null) {
        firehose.notify(destination, currentDebounced);
      }
    }, period, timeUnit);

    firehose.on(source, new KeyedConsumer<SRC, V>() {
      @Override
      public void accept(SRC key, V value) {
        debounced.swap(discardedOld_ -> value);
      }
    });

    return new Stream<>(environment,
                        firehose);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, V1> void consume(SRC source,
                                            Consumer<V1> consumer) {
    this.consume(source,
                 new KeyedConsumer<SRC, V1>() {
                   @Override
                   public void accept(SRC key_, V1 value) {
                     consumer.accept(value);
                   }
                 });
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, V1> void consume(SRC source,
                                            KeyedConsumer<SRC, V1> consumer) {
    firehose.on(source, consumer);
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
  public Channel<V> channel() {
    Key k = new Key(new Object[]{UUID.randomUUID()});
    AnonymousStream<V> anonymousStream = new AnonymousStream<>(k,
                                                               this);
    return new Channel<V>(anonymousStream,
                          stateProvider.makeAtom(k, TreePVector.empty()));
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key> void notify(SRC src, V v) {
    this.firehose.notify(src, v);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key> void unregister(SRC src) {
    this.firehose.unregister(src);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key> void unregister(Predicate<SRC> pred) {
    this.firehose.unregister(pred);
  }

  public Firehose firehose() {
    return this.firehose;
  }

  public StateProvider stateProvider() {
    return this.stateProvider;
  }

  // TODO: last()
  // TODO: first()

}
