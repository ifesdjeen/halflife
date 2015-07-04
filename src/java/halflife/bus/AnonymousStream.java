package halflife.bus;

import halflife.bus.key.Key;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class AnonymousStream<V> {

  private final List<Supplier<KeyedConsumer<? extends Key, V>>> suppliers;

  public AnonymousStream() {
    this(new LinkedList<>());
  }

  protected AnonymousStream(List<Supplier<KeyedConsumer<? extends Key, V>>> suppliers) {
    this.suppliers = suppliers;
  }

  public Supplier<KeyedConsumer<? extends Key, V>> subscriber() {
    return () -> {
      return new KeyedConsumer<Key, V>() {
        @Override
        public void accept(Key k, V value) {

        }
      };
    };
  }
}
