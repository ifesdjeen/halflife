package halflife.bus;

@FunctionalInterface
public interface KeyedConsumer<K, V> {

  public void accept(K key, V value);

}
