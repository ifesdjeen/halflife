package halflife.bus;

@FunctionalInterface
public interface SimpleConsumer<T> {

  public void accept(T value);

  public default void accept(Object k, T value) {
    accept(value);
  }

  public static <T> KeyedConsumer<T> wrap(SimpleConsumer<T> c) {
    return new KeyedConsumer<T>() {
      @Override
      public void accept(Object k, T value) {
        c.accept(value);
      }
    };
  }
}
