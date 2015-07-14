package halflife.bus;

import halflife.bus.key.Key;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class AnonymousStream<V> {

  private final Key    rootKey;
  private final Key    upstream;
  private final Stream stream;

  public AnonymousStream(Key upstream, Stream stream) {
    this.upstream = upstream;
    this.rootKey = upstream;
    this.stream = stream;
  }

  public AnonymousStream(Key rootKey, Key upstream, Stream stream) {
    this.upstream = upstream;
    this.stream = stream;
    this.rootKey = rootKey;
  }

  @SuppressWarnings(value = {"unchecked"})
  public <V1> AnonymousStream<V1> map(Function<V, V1> mapper) {
    Key downstream = upstream.derive();

    stream.map(upstream, downstream, mapper);

    return new AnonymousStream<>(rootKey, downstream, stream);
  }


  @SuppressWarnings(value = {"unchecked"})
  public void consume(Consumer<V> consumer) {
    stream.consume(upstream, consumer);
  }

  @SuppressWarnings(value = {"unchecked"})
  public void notify(V v) {
    this.stream.notify(upstream, v);
  }

  public void unregister() {
    this.stream.unregister(new Predicate<Key>() {
      @Override
      public boolean test(Key k) {
        return k.isDerivedFrom(rootKey) || k.equals(rootKey);
      }
    });
  }
}
