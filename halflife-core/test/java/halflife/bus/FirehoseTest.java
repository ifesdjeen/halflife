package halflife.bus;

import halflife.bus.concurrent.AVar;
import halflife.bus.key.Key;
import org.junit.Test;
import reactor.core.Dispatcher;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FirehoseTest extends AbstractFirehoseTest {

  @Test
  public void simpleOnTest() throws InterruptedException {

    AVar<Integer> val = new AVar<>();
    AVar<Integer> val2 = new AVar<>();

    firehose.on(Key.wrap("key1"), val::set);
    firehose.on(Key.wrap("key2"), val2::set);

    firehose.notify(Key.wrap("key1"), 1);
    firehose.notify(Key.wrap("key2"), 2);

    assertThat(val.get(10, TimeUnit.SECONDS), is(1));
    assertThat(val2.get(10, TimeUnit.SECONDS), is(2));

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
    final CountDownLatch latch2 = new CountDownLatch(2);
    final CountDownLatch latch1 = new CountDownLatch(1);

    firehose.on(k, (i) -> {
      latch2.countDown();
      latch1.countDown();
    });

    firehose.notify(k, 1);
    latch1.await(10, TimeUnit.SECONDS);
    firehose.unregister(k);
    firehose.notify(k, 1);

    latch2.await(1, TimeUnit.SECONDS);
    assertThat(latch2.getCount(), is(1L));
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

    firehose.unregister(k -> ((String) k.getPart(0)).startsWith("key"));
    firehose.notify(k1, 1);
    firehose.notify(k2, 1);
    firehose.notify(k3, 1);

    latch2.await(10, TimeUnit.SECONDS);

    assertThat(latch2.getCount(), is(0L));
    assertThat(latch.getCount(), is(2L));
  }

  @Test
  public void errorTest() throws InterruptedException {
    AVar<Throwable> caught = new AVar<>();
    Dispatcher asyncDispatcher = new ThreadPoolExecutorDispatcher(2, 100);
    Firehose<Key> asyncFirehose = new Firehose<>(throwable -> caught.set(throwable));
    Key k1 = Key.wrap("key1");

    asyncFirehose.on(k1, (Integer i) -> {
      int j = i / 0;
    });

    asyncFirehose.notify(k1, 1);

    assertTrue(caught.get(1, TimeUnit.MINUTES) instanceof ArithmeticException);

    asyncDispatcher.shutdown();
  }
}
