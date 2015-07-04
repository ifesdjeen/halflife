package halflife.bus;

import halflife.bus.registry.DefaultingRegistry;
import halflife.bus.registry.KeyMissMatcher;
import halflife.bus.registry.Registration;
import reactor.core.Dispatcher;
import reactor.core.support.Assert;
import reactor.fn.Consumer;

import java.util.function.Supplier;


public class Firehose<K, V> {

  private final Dispatcher                                 dispatcher;
  private final DefaultingRegistry<K, KeyedConsumer<K, V>> consumerRegistry;
  private final Consumer<Throwable>                        dispatchErrorHandler;

  public Firehose(Dispatcher dispatcher,
                  DefaultingRegistry<K, KeyedConsumer<K, V>> registry,
                  Consumer<Throwable> dispatchErrorHandler) {
    this.dispatcher = dispatcher;
    this.consumerRegistry = registry;
    this.dispatchErrorHandler = dispatchErrorHandler;
  }

  public Firehose<K, V> notify(K key, V ev) {
    Assert.notNull(key, "Key cannot be null.");
    Assert.notNull(ev, "Event cannot be null.");

    dispatcher.dispatch(ev, t -> {
      for (Registration<K, ? extends KeyedConsumer<K, V>> reg : consumerRegistry.select(key)) {
        reg.getObject().accept(key, t);
      }
    }, dispatchErrorHandler);

    return this;
  }

  public Firehose<K, V> on(K key, KeyedConsumer<K, V> consumer) {
    consumerRegistry.register(key, consumer);
    return this;
  }

  public Firehose<K, V> on(K key, SimpleConsumer<V> consumer) {
    consumerRegistry.register(key, (k_, value) -> consumer.accept(value));
    return this;
  }

  public Firehose<K, V> miss(KeyMissMatcher<K> matcher, Supplier<KeyedConsumer<K, V>> supplier) {
    consumerRegistry.addKeyMissMatcher(matcher, supplier);
    return this;
  }
}