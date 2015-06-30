package halflife.bus;

@FunctionalInterface
public interface KeyedConsumer<T> {

  public void accept(Object k, T value);

}
