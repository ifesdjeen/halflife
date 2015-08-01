package halflife.bus.registry;

import halflife.bus.KeyedConsumer;
import reactor.fn.Consumer;

public class SimpleRegistration<K, V extends KeyedConsumer> implements Registration<K> {

  private final K selector;
  private final V object;
  private final Consumer<Registration<K>> expireFn;

  public SimpleRegistration(K selector, V object, Consumer<Registration<K>> expireFn) {
    this.selector = selector;
    this.object = object;
    this.expireFn = expireFn;
  }

  @Override
  public K getSelector() {
    return selector;
  }

  @Override
  public <V1> KeyedConsumer<K, V1> getObject() {
    return object;
  }


}
