package halflife.bus;

import halflife.bus.key.Key;

import java.util.function.Consumer;
import java.util.function.Function;

public class AnonymousStream<V> {

  private final Key    upstream;
  private final Stream stream;

  public AnonymousStream(Key upstream, Stream stream) {
    this.upstream = upstream;
    this.stream = stream;
  }

  @SuppressWarnings(value = {"unchecked"})
  public <V1> AnonymousStream<V1> map(Function<V, V1> mapper) {
    Key downstream = upstream.derive();

    stream.map(upstream, downstream, mapper);

    return new AnonymousStream<>(downstream, stream);
  }


  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key> void consume(Consumer<V> consumer) {
    stream.consume(upstream, consumer);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <SRC extends Key> void notify(V v) {
    this.stream.notify(upstream, v);
  }
}
