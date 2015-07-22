package halflife.bus;

import halflife.bus.concurrent.LazyVar;
import halflife.bus.registry.DefaultingRegistry;
import halflife.bus.registry.KeyMissMatcher;
import halflife.bus.registry.Registration;
import halflife.bus.registry.Registry;
import reactor.core.Dispatcher;
import reactor.core.support.Assert;
import reactor.fn.Consumer;
import reactor.fn.timer.HashWheelTimer;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;


public class Firehose<K, V> {

  private final Dispatcher                                 dispatcher;
  private final DefaultingRegistry<K, KeyedConsumer<K, V>> consumerRegistry;
  private final Consumer<Throwable>                        dispatchErrorHandler;
  private final Consumer<Throwable>                        consumeErrorHandler;
  private final LazyVar<HashWheelTimer>                    timer;

  public Firehose(Dispatcher dispatcher,
                  DefaultingRegistry<K, KeyedConsumer<K, V>> registry,
                  Consumer<Throwable> dispatchErrorHandler,
                  Consumer<Throwable> consumeErrorHandler) {
    this.dispatcher = dispatcher;
    this.consumerRegistry = registry;
    this.dispatchErrorHandler = dispatchErrorHandler;
    this.consumeErrorHandler = consumeErrorHandler;

    this.timer = new LazyVar<>(() -> {
      return new HashWheelTimer(10); // TODO: configurable hash wheel size!
    });
  }

  public Firehose<K, V> notify(K key, V ev) {
    Assert.notNull(key, "Key cannot be null.");
    Assert.notNull(ev, "Event cannot be null.");

    dispatcher.dispatch(ev, t -> {
      for (Registration<K, ? extends KeyedConsumer<K, V>> reg : consumerRegistry.select(key)) {
        try {
          reg.getObject().accept(key, t);
        } catch (Throwable e) {
          if (consumeErrorHandler != null) {
            consumeErrorHandler.accept(e);
          }
        }
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

  public Firehose<K, V> miss(KeyMissMatcher<K> matcher,
                             Function<K, Map<K, ? extends KeyedConsumer<K, V>>> supplier) {
    consumerRegistry.addKeyMissMatcher(matcher, supplier);
    return this;
  }

  public boolean unregister(K key) {
    return consumerRegistry.unregister(key);
  }

  public boolean unregister(Predicate<K> pred) {
    return consumerRegistry.unregister(pred);
  }

  public Registry<K, KeyedConsumer<K, V>> getConsumerRegistry() {
    return this.consumerRegistry;
  }

  public HashWheelTimer getTimer() {
    return this.timer.get();
  }
}