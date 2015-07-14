package com.instana.processor;

import halflife.bus.KeyedConsumer;
import halflife.bus.concurrent.AVar;
import halflife.bus.key.Key;
import org.junit.Test;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
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
  public void doubleSubscriptionTest() throws InterruptedException {
    AVar<Integer> val = new AVar<>();
    AVar<Integer> val2 = new AVar<>();

    firehose.on(Key.wrap("key1"), (key, value) -> {
      return;
    });
    firehose.on(Key.wrap("key1"), val::set);

    firehose.on(Key.wrap("key2"), val2::set);
    firehose.on(Key.wrap("key2"), (key, value) -> {
      return;
    });

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

    assertThat(val.get(10, TimeUnit.MILLISECONDS),
               is(Tuple.of(Key.wrap("key1"), 1)));
  }

  @Test
  public void keyMissTest() throws InterruptedException {
    AVar<Tuple2> val = new AVar<>();

    firehose.miss((k_) -> true,
                  (k) -> {
                    return Collections.singletonMap(k, (key, value) -> {
                      val.set(Tuple.of(key, value));
                    });
                  });

    firehose.notify(Key.wrap("key1"), 1);
    assertThat(val.get(10, TimeUnit.MILLISECONDS), is(Tuple.of(Key.wrap("key1"), 1)));
  }

  @Test
  public void unsubscribeTest() throws InterruptedException {
    Key k = Key.wrap("key1");
    final CountDownLatch latch = new CountDownLatch(2);

    firehose.on(k, (i) -> {
      latch.countDown();
    });

    firehose.notify(k, 1);
    firehose.unregister(k);
    firehose.notify(k, 1);

    assertThat(latch.getCount(), is(1L));
  }

  @Test
  public void unsubscribeAllTest() throws InterruptedException {
    Key k1 = Key.wrap("key1");
    Key k2 = Key.wrap("key2");
    Key k3 = Key.wrap("_key3");

    final CountDownLatch latch = new CountDownLatch(2);
    final CountDownLatch latch2 = new CountDownLatch(1);

    firehose.on(k1, (i) -> {
      latch.countDown();
    });

    firehose.on(k2, (i) -> {
      latch.countDown();
    });

    firehose.on(k3, (i) -> {
      latch2.countDown();
    });

    firehose.unregister(k -> ((String)k.getPart(0)).startsWith("key"));
    firehose.notify(k1, 1);
    firehose.notify(k2, 1);
    firehose.notify(k3, 1);


    assertThat(latch2.getCount(), is(0L));
    assertThat(latch.getCount(), is(2L));
  }
}
