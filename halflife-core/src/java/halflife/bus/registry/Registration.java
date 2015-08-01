package halflife.bus.registry;

import halflife.bus.KeyedConsumer;
import halflife.bus.key.Key;

public interface Registration<K> {

  K getSelector(); // TODO: Rename to getKey, since we don't really have selectors in their old meaning

  <V> KeyedConsumer<K, V> getObject();

  // cancelAfterUse?
}
