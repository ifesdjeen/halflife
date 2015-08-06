package halflife.bus;

import halflife.bus.concurrent.LazyVar;
import halflife.bus.key.Key;
import halflife.bus.registry.DefaultingRegistry;
import halflife.bus.registry.KeyMissMatcher;
import halflife.bus.registry.Registration;
import halflife.bus.registry.Registry;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Dispatcher;
import reactor.core.processor.RingBufferProcessor;
import reactor.core.support.Assert;
import reactor.fn.Consumer;
import reactor.fn.timer.HashWheelTimer;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;


public class Firehose<K extends Key> {

  private final DefaultingRegistry<K>         consumerRegistry;
  private final Consumer<Throwable>           dispatchErrorHandler;
  private final Consumer<Throwable>           consumeErrorHandler;
  private final LazyVar<HashWheelTimer>       timer;
  private final RingBufferProcessor<Runnable> processor;

  public Firehose(Dispatcher dispatcher,
                  DefaultingRegistry<K> registry,
                  Consumer<Throwable> dispatchErrorHandler,
                  Consumer<Throwable> consumeErrorHandler) {
    this.consumerRegistry = registry;
    this.dispatchErrorHandler = dispatchErrorHandler;
    this.consumeErrorHandler = consumeErrorHandler;


    processor = RingBufferProcessor.create(Executors.newFixedThreadPool(2), 32);

    processor.subscribe(new Subscriber<Runnable>() {

      private volatile Subscription sub;

      @Override
      public void onSubscribe(Subscription subscription) {
        this.sub = subscription;
        subscription.request(1);
      }

      @Override
      public void onNext(Runnable runnable) {
        runnable.run();
        sub.request(1);
      }

      @Override
      public void onError(Throwable throwable) {
      }

      @Override
      public void onComplete() {
      }
    });


    this.timer = new LazyVar<>(() -> {
      return new HashWheelTimer(10); // TODO: configurable hash wheel size!
    });
  }

  public Firehose<K> withDispatcher(Dispatcher dispatcher) {
    return new Firehose<K>(dispatcher,
                           consumerRegistry,
                           dispatchErrorHandler,
                           consumeErrorHandler);
  }

  public <V> Firehose notify(K key, V ev) {
    Assert.notNull(key, "Key cannot be null.");
    Assert.notNull(ev, "Event cannot be null.");

    processor.onNext(() -> {
      for (Registration<K> reg : consumerRegistry.select(key)) {
        try {

          reg.getObject().accept(key, ev);
        } catch (Throwable e) {
          if (consumeErrorHandler != null) {
            consumeErrorHandler.accept(e);
          }
        }
      }
    });

    return this;
  }

  public <V> Firehose on(K key, KeyedConsumer<K, V> consumer) {
    consumerRegistry.register(key, consumer);
    return this;
  }

  public <V> Firehose on(K key, SimpleConsumer<V> consumer) {
    consumerRegistry.register(key, new KeyedConsumer<K, V>() {
      @Override
      public void accept(K key, V value) {
        consumer.accept(value);
      }
    });
    return this;
  }

  public Firehose miss(KeyMissMatcher<K> matcher,
                       Function<K, Map<K, KeyedConsumer>> supplier) {
    consumerRegistry.addKeyMissMatcher(matcher, supplier);
    return this;
  }

  public boolean unregister(K key) {
    return consumerRegistry.unregister(key);
  }

  public boolean unregister(Predicate<K> pred) {
    return consumerRegistry.unregister(pred);
  }

  public <V> Registry<K> getConsumerRegistry() {
    return this.consumerRegistry;
  }

  public HashWheelTimer getTimer() {
    return this.timer.get();
  }
}