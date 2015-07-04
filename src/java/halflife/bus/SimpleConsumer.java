package halflife.bus;

import halflife.bus.key.Key;

@FunctionalInterface
public interface SimpleConsumer<V> {

  public void accept(V value);

  public static <T> KeyedConsumer<?, T> wrap(SimpleConsumer<T> consumer) {
    return new KeyedConsumer<Object, T>() {
      @Override
      public void accept(Object k, T value) {
        consumer.accept(value);
      }
    };
  }
}
