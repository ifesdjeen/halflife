package halflife.bus;

import halflife.bus.key.Key;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class MatchedStream<V> extends FinalizedMatchedStream<V> {

  public MatchedStream() {
    super(new LinkedList<>());
  }

  protected MatchedStream(List<MatchedStream.StreamSupplier> suppliers) {
    super(suppliers);
  }

  @SuppressWarnings(value = {"unchecked"})
  public <V1> MatchedStream<V1> map(Function<V, V1> mapper) {
    this.suppliers.add(new StreamSupplier<V>() {
      @Override
      public <SRC extends Key, DST extends Key> void get(SRC src, DST dst, Stream<V> stream) {
        stream.map(src, dst, mapper);
      }
    });
    return new MatchedStream<>(suppliers);
  }

  @SuppressWarnings(value = {"unchecked"})
  public FinalizedMatchedStream consume(Consumer<V> consumer) {
    this.suppliers.add(new StreamSupplier<V>() {
      @Override
      public <SRC extends Key, DST extends Key> void get(SRC src, DST dst, Stream<V> stream) {
        stream.consume(src, consumer);
      }
    });
    return new FinalizedMatchedStream(suppliers);
  }

  @FunctionalInterface
  public static interface StreamSupplier<V> {
    public <SRC extends Key, DST extends Key> void get(SRC src, DST dst, Stream<V> stream);
  }


}
