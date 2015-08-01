package halflife.bus.integration;

import halflife.bus.SimpleConsumer;
import halflife.bus.key.Key;

public interface Downstream<K extends Key, V> extends SimpleConsumer<StreamTuple<K,V>> {

}
