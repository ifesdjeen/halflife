package com.instana.processor;

import halflife.bus.Firehose;
import halflife.bus.KeyedConsumer;
import halflife.bus.Stream;
import halflife.bus.concurrent.AVar;
import halflife.bus.concurrent.Atom;
import halflife.bus.key.Key;
import halflife.bus.registry.ConcurrentRegistry;
import halflife.bus.registry.DefaultingRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pcollections.TreePVector;
import reactor.core.Dispatcher;
import reactor.core.dispatch.SynchronousDispatcher;
import reactor.fn.Consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class StreamTest extends AbstractFirehoseTest {

  private   Dispatcher                                      dispatcher;
  protected DefaultingRegistry<Object, KeyedConsumer<Object, Object>> consumerRegistry;
  protected Consumer<Throwable>                             dispatchErrorHandler;
  protected Firehose                                        firehose;

  @Before
  public void setup() {
    this.dispatcher = new SynchronousDispatcher();
    this.consumerRegistry = new ConcurrentRegistry<>();
    this.dispatchErrorHandler = throwable -> {
      System.out.println(throwable.getMessage());
      throwable.printStackTrace();
    };

    this.firehose = new Firehose(dispatcher,
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

    intStream.notify(Key.wrap("key1"), 1);

    assertThat(res.get(1, TimeUnit.SECONDS), is(2));
  }

  @Test
  public void streamStateSupplierTest() throws InterruptedException {
    AVar<Integer> res = new AVar<>(3);
    Stream<Integer> intStream = new Stream<>(firehose);

    intStream.map(Key.wrap("key1"), Key.wrap("key2"), (i) -> i + 1);
    intStream.map(Key.wrap("key2"), Key.wrap("key3"), (Atom<Integer> state) -> {
                    return (i) -> {
                      return state.swap(old -> old + i);
                    };
                  },
                  0);
    intStream.consume(Key.wrap("key3"), res::set);

    intStream.notify(Key.wrap("key1"), 1);
    intStream.notify(Key.wrap("key1"), 2);
    intStream.notify(Key.wrap("key1"), 3);

    assertThat(res.get(1, TimeUnit.SECONDS), is(9));
  }

  @Test
  public void streamStateFnTest() throws InterruptedException {
    AVar<Integer> res = new AVar<>(3);
    Stream<Integer> intStream = new Stream<>(firehose);

    intStream.map(Key.wrap("key1"), Key.wrap("key2"), (i) -> i + 1);
    intStream.map(Key.wrap("key2"), Key.wrap("key3"), (Atom<Integer> state, Integer i) -> {
                    return state.swap(old -> old + i);
                  },
                  0);
    intStream.consume(Key.wrap("key3"), res::set);

    intStream.notify(Key.wrap("key1"), 1);
    intStream.notify(Key.wrap("key1"), 2);
    intStream.notify(Key.wrap("key1"), 3);

    assertThat(res.get(1, TimeUnit.SECONDS), is(9));
  }

  @Test
  public void streamFilterTest() throws InterruptedException {
    AVar<Integer> res = new AVar<>(2);
    Stream<Integer> intStream = new Stream<>(firehose);

    intStream.filter(Key.wrap("key1"), Key.wrap("key2"), (i) -> i % 2 == 0);
    intStream.consume(Key.wrap("key2"), res::set);

    intStream.notify(Key.wrap("key1"), 1);
    intStream.notify(Key.wrap("key1"), 2);
    intStream.notify(Key.wrap("key1"), 3);
    intStream.notify(Key.wrap("key1"), 4);
    assertThat(res.get(1, TimeUnit.SECONDS), is(4));
  }

  @Test
  public void partitionTest() throws InterruptedException {
    AVar<List<Integer>> res = new AVar<>();
    Stream<Integer> intStream = new Stream<>(firehose);

    intStream.partition(Key.wrap("key1"), Key.wrap("key2"), (i) -> {
      // System.out.println(i);
      return i.size() == 5;
    });
    intStream.consume(Key.wrap("key2"), (List<Integer> a) -> {
      res.set(a);
    });

    intStream.notify(Key.wrap("key1"), 1);
    intStream.notify(Key.wrap("key1"), 2);
    intStream.notify(Key.wrap("key1"), 3);
    intStream.notify(Key.wrap("key1"), 4);
    intStream.notify(Key.wrap("key1"), 5);
    intStream.notify(Key.wrap("key1"), 6);
    intStream.notify(Key.wrap("key1"), 7);

    assertThat(res.get(1, TimeUnit.SECONDS), is(TreePVector.from(Arrays.asList(1, 2, 3, 4, 5))));
  }

}
