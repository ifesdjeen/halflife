package halflife.bus.registry;

import java.util.Map;
import java.util.function.Function;

public interface DefaultingRegistry<K,V> extends Registry<K, V> {

  public void addKeyMissMatcher(KeyMissMatcher<K> matcher,
                                Function<K, Map<K, ? extends V>> supplier);

}
