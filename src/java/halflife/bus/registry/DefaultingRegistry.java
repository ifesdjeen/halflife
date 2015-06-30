package halflife.bus.registry;

import java.util.function.Supplier;

public interface DefaultingRegistry<K,V> extends Registry<K, V> {

  public void addKeyMissMatcher(KeyMissMatcher<K> matcher,
                                Supplier<? extends V> supplier);
}
