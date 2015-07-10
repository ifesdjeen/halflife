package halflife.bus;

import halflife.bus.registry.DefaultingRegistry;
import halflife.bus.registry.KeyMissMatcher;
import halflife.bus.registry.Registration;
import reactor.core.Dispatcher;
import reactor.core.support.Assert;
import reactor.fn.Consumer;

import java.util.Map;
import java.util.function.Function;


public class Firehose {

  private final Dispatcher                                      dispatcher;
  private final DefaultingRegistry<Object, KeyedConsumer<Object, Object>> consumerRegistry;
  private final Consumer<Throwable>                             dispatchErrorHandler;

  public Firehose(Dispatcher dispatcher,
                  DefaultingRegistry<Object, KeyedConsumer<Object, Object>> registry,
                  Consumer<Throwable> dispatchErrorHandler) {
    this.dispatcher = dispatcher;
    this.consumerRegistry = registry;
    this.dispatchErrorHandler = dispatchErrorHandler;
  }

  @SuppressWarnings({"unchecked"})
  public <K, V> Firehose notify(K key, V ev) {
    Assert.notNull(key, "Key cannot be null.");
    Assert.notNull(ev, "Event cannot be null.");

    dispatcher.dispatch(ev, t -> {
      for (Registration<Object, ? extends KeyedConsumer<?, ?>> reg : consumerRegistry.select(key)) {
        KeyedConsumer<K,V> cast = (KeyedConsumer<K,V>) reg.getObject();
        // TODO: add type checks
        System.out.println(t);
        cast.accept(key, t);
      }
    }, dispatchErrorHandler);

    return this;
  }

  public <K, V> Firehose on(K key, KeyedConsumer<K, V> consumer) {
    consumerRegistry.register(key, (KeyedConsumer<Object, Object>) consumer);
    return this;
  }

  @SuppressWarnings({"unchecked"})
  public <K, V> Firehose on(K key, SimpleConsumer<V> consumer) {
    consumerRegistry.register(key, (k_, value) -> consumer.accept((V) value));
    return this;
  }

  @SuppressWarnings({"unchecked"})
  public <K, V> Firehose miss(KeyMissMatcher<? extends K> matcher,
                              Function<K, Map<K, ? extends KeyedConsumer<K, V>>> supplier) {
    consumerRegistry.addKeyMissMatcher((KeyMissMatcher) matcher, (Function)supplier);
    return this;
  }
}