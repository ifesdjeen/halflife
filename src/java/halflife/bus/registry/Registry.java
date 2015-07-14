package halflife.bus.registry;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface Registry<K, V> extends Iterable<Registration<K, ? extends V>> {

  Registration<K, V> register(K sel, V obj);

  boolean unregister(K key);

  boolean unregister(Predicate<K> key);

  List<Registration<K, ? extends V>> select(K key);

  void clear();

  public Stream<Registration<K, ? extends V>> stream();
}
