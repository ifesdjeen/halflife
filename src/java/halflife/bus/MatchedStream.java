package halflife.bus;

import halflife.bus.key.Key;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MatchedStream<V> {

  private final List<Supplier<KeyedConsumer<? extends Key, V>>> suppliers;

  public MatchedStream() {
    this(new LinkedList<>());
  }

  @SuppressWarnings(value = {"unchecked"})
  public <V1> MatchedStream<V1> map(Function<V, V1> mapper) {
    this.suppliers.add(() -> {
      return new KeyedConsumer<Key, V>() {
        @Override
        public void accept(Key key, V value) {

        }
      };
    });
    return null;
  }

  @SuppressWarnings(value = {"unchecked"})
  public <V1> MatchedStream<V1> consume(Consumer<V1> consumer) {
    return null;
  }

  protected MatchedStream(List<Supplier<KeyedConsumer<? extends Key, V>>> suppliers) {
    this.suppliers = suppliers;
  }

  public Supplier<KeyedConsumer<? extends Key, V>> subscriber(Stream stream) {
    return () -> {
      return new KeyedConsumer<Key, V>() {
        @Override
        public void accept(Key k, V value) {

        }
      };
    };
  }
}
