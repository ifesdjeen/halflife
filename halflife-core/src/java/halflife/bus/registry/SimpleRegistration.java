package halflife.bus.registry;

import reactor.fn.Consumer;

public class SimpleRegistration<K, V> implements Registration<K, V> {

  private final K selector;
  private final V object;
  private final Consumer<Registration<K, V>> expireFn;

  public SimpleRegistration(K selector, V object, Consumer<Registration<K, V>> expireFn) {
    this.selector = selector;
    this.object = object;
    this.expireFn = expireFn;
  }

  @Override
  public K getSelector() {
    return selector;
  }

  @Override
  public V getObject() {
    return object;
  }
}
