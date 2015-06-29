package halflife.bus;

import reactor.fn.Consumer;

public interface KeyedConsumer<T> extends Consumer<T> {

  public default void accept(Object k, T value) {
    accept(value);
  }

}
