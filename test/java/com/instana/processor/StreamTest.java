package com.instana.processor;

import halflife.bus.Firehose;
import halflife.bus.KeyedConsumer;
import halflife.bus.Stream;
import halflife.bus.concurrent.AVar;
import halflife.bus.key.Key;
import halflife.bus.registry.ConcurrentRegistry;
import halflife.bus.registry.DefaultingRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.Dispatcher;
import reactor.core.dispatch.SynchronousDispatcher;
import reactor.fn.Consumer;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class StreamTest extends AbstractFirehoseTest {

  private Dispatcher                                           dispatcher;
  private DefaultingRegistry<Key, KeyedConsumer<Key, Integer>> consumerRegistry;
  private Consumer<Throwable>                                  dispatchErrorHandler;
  private Firehose<Key, Integer>                               firehose;

  @Before
  public void setup() {
    this.dispatcher = new SynchronousDispatcher();
    this.consumerRegistry = new ConcurrentRegistry<>();
    this.dispatchErrorHandler = throwable -> {
      System.out.println(throwable.getMessage());
      throwable.printStackTrace();
    };

    this.firehose = new Firehose<>(dispatcher,
                                   consumerRegistry,
                                   dispatchErrorHandler);
  }

  @After
  public void teardown() {
    this.dispatcher.shutdown();
    this.firehose = null;
  }

  @Test
  public void testMap() throws InterruptedException {
    AVar<Integer> res = new AVar<>();
    Stream<Integer> intStream = new Stream<>(firehose);

    intStream.map(Key.wrap("key1"), Key.wrap("key2"), (i) -> i + 1);
    intStream.consume(Key.wrap("key2"), res::set);

    firehose.notify(Key.wrap("key1"), 1);

    assertThat(res.get(1, TimeUnit.SECONDS), is(2));
  }

}
