package halflife.bus.registry;

import java.util.List;

public interface Registry<K, V> extends Iterable<Registration<K, V>> {

  Registration<K, V> register(K sel, V obj);

  boolean unregister(K key);

  List<Registration<K, ? extends V>> select(K key);

  void clear();

}
