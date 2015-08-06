package halflife.bus;

import halflife.bus.concurrent.LazyVar;
import halflife.bus.key.Key;
import halflife.bus.registry.*;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.processor.RingBufferProcessor;
import reactor.core.support.Assert;
import reactor.fn.Consumer;
import reactor.fn.timer.HashWheelTimer;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;


public class Firehose<K extends Key> {

  private final static int DEFAULT_RING_BUFFER_SIZE = 2048;

  private final DefaultingRegistry<K>         consumerRegistry;
  private final Consumer<Throwable>           errorHandler;
  private final LazyVar<HashWheelTimer>       timer;
  private final Processor<Runnable, Runnable> processor;

  public Firehose() {
    this(new ConcurrentRegistry<K>(),
         RingBufferProcessor.create(Executors.newFixedThreadPool(2), DEFAULT_RING_BUFFER_SIZE),
         throwable -> {
           System.out.println(throwable.getMessage());
           throwable.printStackTrace();
         });
  }

  public Firehose(Consumer<Throwable> errorHandler) {
    this(new ConcurrentRegistry<K>(),
         RingBufferProcessor.create(Executors.newFixedThreadPool(2), DEFAULT_RING_BUFFER_SIZE),
         errorHandler);
  }

  public Firehose(DefaultingRegistry<K> registry,
                  Processor<Runnable, Runnable> processor,
                  Consumer<Throwable> dispatchErrorHandler) {
    this.consumerRegistry = registry;
    this.errorHandler = dispatchErrorHandler;
    this.processor = processor;
    this.processor.subscribe(new Subscriber<Runnable>() {

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
        errorHandler.accept(throwable);
      }

      @Override
      public void onComplete() {
        this.sub.cancel();
      }
    });


    this.timer = new LazyVar<>(() -> {
      return new HashWheelTimer(10); // TODO: configurable hash wheel size!
    });
  }

  public Firehose<K> fork(ExecutorService executorService,
                          int ringBufferSize) {
    return new Firehose<K>(this.consumerRegistry,
                           RingBufferProcessor.create(executorService, ringBufferSize),
                           this.errorHandler);
  }

  public <V> Firehose notify(K key, V ev) {
    Assert.notNull(key, "Key cannot be null.");
    Assert.notNull(ev, "Event cannot be null.");

    processor.onNext(() -> {
      for (Registration<K> reg : consumerRegistry.select(key)) {
        reg.getObject().accept(key, ev);
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

  public void shutdown() {
    processor.onComplete();

  }
}