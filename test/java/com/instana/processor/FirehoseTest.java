package com.instana.processor;

import halflife.bus.Firehose;
import halflife.bus.KeyedConsumer;
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
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FirehoseTest extends AbstractFirehoseTest {

  @Test
  public void simpleOnTest() throws InterruptedException {
    AVar<Integer> val = new AVar<>();
    AVar<Integer> val2 = new AVar<>();

    firehose.on(Key.wrap("key1"), val::set);
    firehose.on(Key.wrap("key2"), val2::set);

    firehose.notify(Key.wrap("key1"), 1);
    firehose.notify(Key.wrap("key2"), 2);

    assertThat(val.get(10, TimeUnit.MILLISECONDS), is(1));
    assertThat(val2.get(10, TimeUnit.MILLISECONDS), is(2));
  }

  @Test
  public void simpleOn2Test() throws InterruptedException {
    AVar<Tuple2> val = new AVar<>();
    firehose.on(Key.wrap("key1"), (key, value) -> {
      val.set(Tuple.of(key, value));
    });
    firehose.notify(Key.wrap("key1"), 1);

    assertThat(val.get(10, TimeUnit.MILLISECONDS), is(Tuple.of("key1", 1)));
  }

  @Test
  public void keyMissTest() throws InterruptedException {
    AVar<Tuple2> val = new AVar<>();

    firehose.miss((k_) -> true, () -> {
      return (key, value) -> {
        val.set(Tuple.of(key, value));
      };
    });

    firehose.notify(Key.wrap("key1"), 1);
    assertThat(val.get(10, TimeUnit.MILLISECONDS), is(Tuple.of("key1", 1)));
  }
}
