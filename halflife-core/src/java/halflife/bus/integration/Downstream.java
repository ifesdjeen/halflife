package halflife.bus.integration;

import halflife.bus.SimpleConsumer;
import halflife.bus.key.Key;

public interface Downstream<K, V> extends SimpleConsumer<StreamTuple<K,V>> {

}
