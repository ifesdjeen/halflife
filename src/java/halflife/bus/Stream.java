package halflife.bus;

import halflife.bus.key.Key;
import halflife.bus.registry.KeyMissMatcher;

import java.util.function.Consumer;
import java.util.function.Function;

public class Stream<V> {

  private final Firehose firehose;

  // Stream subscribes to some key
  public Stream(Firehose firehose) {
    this.firehose = firehose;
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
  public <SRC extends Key, V1> AnonymousStream<V1> map(SRC source,
                                                       Function<V, V1> mapper) {
    return map(new KeyMissMatcher<SRC>() {
      @Override
      public boolean test(SRC other) {
        return source.equals(other);
      }
    }, mapper);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key, V1> AnonymousStream<V1> map(KeyMissMatcher<SRC> keyMatcher,
                                                       Function<V, V1> mapper) {
    AnonymousStream<V1> downstream = new AnonymousStream<>();
    firehose.miss(keyMatcher, downstream.subscriber());
    return downstream;
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



}
