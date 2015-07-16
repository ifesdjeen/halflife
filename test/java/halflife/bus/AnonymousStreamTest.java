package halflife.bus;

import halflife.bus.concurrent.AVar;
import halflife.bus.key.Key;
import org.junit.Test;
import org.pcollections.TreePVector;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AnonymousStreamTest extends AbstractFirehoseTest {

  @Test
  public void testMap() throws InterruptedException {
    Stream<Integer> stream = new Stream<>(firehose);
    AVar<Integer> res = new AVar<>();

    stream.anonymous(Key.wrap("source"))
          .map((i) -> i + 1)
          .map(i -> i * 2)
          .consume(res::set);


    firehose.notify(Key.wrap("source"), 1);

    assertThat(res.get(1, TimeUnit.SECONDS), is(4));
  }

  @Test
  public void testFilter() throws InterruptedException {
    Stream<Integer> stream = new Stream<>(firehose);
    AVar<Integer> res = new AVar<>();

    stream.anonymous(Key.wrap("source"))
          .map(i -> i + 1)
          .filter(i -> i % 2 != 0)
          .map(i -> i * 2)

          .consume(res::set);


    firehose.notify(Key.wrap("source"), 1);
    firehose.notify(Key.wrap("source"), 2);

    assertThat(res.get(1, TimeUnit.SECONDS), is(6));
  }

  @Test
  public void testPartition() throws InterruptedException {
    Stream<Integer> stream = new Stream<>(firehose);
    AVar<List<Integer>> res = new AVar<>();

    stream.anonymous(Key.wrap("source"))
          .partition((i) -> {
            return i.size() == 5;
          })
          .consume(res::set);

    stream.notify(Key.wrap("source"), 1);
    stream.notify(Key.wrap("source"), 2);
    stream.notify(Key.wrap("source"), 3);
    stream.notify(Key.wrap("source"), 4);
    stream.notify(Key.wrap("source"), 5);
    stream.notify(Key.wrap("source"), 6);
    stream.notify(Key.wrap("source"), 7);

    assertThat(res.get(1, TimeUnit.SECONDS), is(TreePVector.from(Arrays.asList(1, 2, 3, 4, 5))));
  }

  @Test
  public void testSlide() throws InterruptedException {
    Stream<Integer> stream = new Stream<>(firehose);
    AVar<List<Integer>> res = new AVar<>(6);

    stream.anonymous(Key.wrap("source"))
          .slide(i -> {
            return i.subList(i.size() > 5 ? i.size() - 5 : 0,
                             i.size());
          })
          .consume(res::set);

    stream.notify(Key.wrap("source"), 1);
    stream.notify(Key.wrap("source"), 2);
    stream.notify(Key.wrap("source"), 3);
    stream.notify(Key.wrap("source"), 4);
    stream.notify(Key.wrap("source"), 5);
    stream.notify(Key.wrap("source"), 6);

    assertThat(res.get(1, TimeUnit.SECONDS), is(TreePVector.from(Arrays.asList(2,3,4,5,6))));
  }

  @Test
  public void testNotify() throws InterruptedException {
    Stream<Integer> stream = new Stream<>(firehose);
    AVar<Integer> res = new AVar<>();

    AnonymousStream<Integer> s = stream.anonymous(Key.wrap("source"));

    s.map((i) -> i + 1)
     .map(i -> i * 2)
     .consume(res::set);

    firehose.notify(Key.wrap("source"), 1);

    assertThat(res.get(1, TimeUnit.SECONDS), is(4));
  }

  @Test
  public void testUnregister() throws InterruptedException {
    Stream<Integer> stream = new Stream<>(firehose);
    CountDownLatch latch = new CountDownLatch(2);

    AnonymousStream<Integer> s = stream.anonymous(Key.wrap("source"));

    s.map((i) -> i + 1)
     .map(i -> i * 2)
     .consume(i -> latch.countDown());

    firehose.notify(Key.wrap("source"), 1);
    s.unregister();
    firehose.notify(Key.wrap("source"), 1);

    assertThat(latch.getCount(), is(1L));
    assertThat(firehose.getConsumerRegistry().stream().count(), is(0L));
  }

}
