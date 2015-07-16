package halflife.bus.registry;

public interface Registration<K, V> {

  K getSelector();

  V getObject();

  // cancelAfterUse?
}
