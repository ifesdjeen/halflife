package halflife.bus.registry;

import java.util.List;
import java.util.function.Function;

public interface DefaultingRegistry<K,V> extends Registry<K, V> {

  public void addKeyMissMatcher(KeyMissMatcher<K> matcher,
                                Function<K, List<? extends V>> supplier);

}
