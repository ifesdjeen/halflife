package halflife.bus;

import halflife.bus.channel.ConsumingChannel;
import halflife.bus.channel.PublishingChannel;
import halflife.bus.concurrent.AVar;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class ChannelTest extends AbstractStreamTest {

  @Test
  public void simpleChannelTest() throws InterruptedException {
    Stream<Integer> stream = new Stream<>();
    Channel<Integer> chan = stream.channel();

    chan.tell(1);
    chan.tell(2);

    assertThat(chan.get(LATCH_TIMEOUT, LATCH_TIME_UNIT), is(1));
    assertThat(chan.get(LATCH_TIMEOUT, LATCH_TIME_UNIT), is(2));
    assertTrue(chan.get() == null);

    chan.tell(3);
    assertThat(chan.get(LATCH_TIMEOUT, LATCH_TIME_UNIT), is(3));
  }

  @Test
  public void channelStreamTest() throws InterruptedException {
    AVar<Integer> res = new AVar<>();
    Stream<Integer> stream = new Stream<>();
    Channel<Integer> chan = stream.channel();

    chan.stream().consume((i) -> res.set(i));

    chan.tell(1);

    assertThat(res.get(LATCH_TIMEOUT, LATCH_TIME_UNIT), is(1));
  }

  @Test
  public void drainedChannelTest() throws InterruptedException {
    AVar<Integer> res = new AVar<>();
    Stream<Integer> stream = new Stream<>();
    Channel<Integer> chan = stream.channel();

    chan.stream().consume((i) -> res.set(i));

    chan.tell(1);

    assertThat(res.get(LATCH_TIMEOUT, LATCH_TIME_UNIT), is(1));
    Exception expectedException = null;
    try {
      chan.get();
    } catch (Exception e) {
      expectedException = e;
    }
    assertTrue(expectedException != null);
  }

  @Test
  public void consumingPublishingChannelsTest() throws InterruptedException {
    Stream<Integer> stream = new Stream<>();
    Channel<Integer> chan = stream.channel();

    PublishingChannel<Integer> publishingChannel = chan.publishingChannel();
    ConsumingChannel<Integer> consumingChannel = chan.consumingChannel();

    publishingChannel.tell(1);
    publishingChannel.tell(2);

    assertThat(consumingChannel.get(10, TimeUnit.SECONDS), is(1));
    assertThat(consumingChannel.get(10, TimeUnit.SECONDS), is(2));
    assertTrue(consumingChannel.get() == null);

    chan.tell(3);
    assertThat(consumingChannel.get(10, TimeUnit.SECONDS), is(3));
  }

  @Test
  public void timedGetTest() throws InterruptedException {
    Stream<Integer> stream = new Stream<>();
    Channel<Integer> chan = stream.channel();

    CountDownLatch latch = new CountDownLatch(1);
    new Thread(() -> {
      try {
        Thread.sleep(1000);
        chan.tell(1);
        latch.countDown();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();

    latch.await(LATCH_TIMEOUT, LATCH_TIME_UNIT);
    assertThat(stream.firehose().getConsumerRegistry().stream().count(), is(1L));
    assertThat(chan.get(2000, TimeUnit.MILLISECONDS), is(1));
  }

  @Test
  public void timedGetUnresolvedTest() throws InterruptedException {
    Stream<Integer> stream = new Stream<>();
    Channel<Integer> chan = stream.channel();

    assertThat(stream.firehose().getConsumerRegistry().stream().count(), is(1L));
    boolean caught = false;
    try {
      chan.get(100, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      caught = true;
    }
    assertThat(caught, is(true));
    assertThat(stream.firehose().getConsumerRegistry().stream().count(), is(0L));
  }

  @Test
  public void channelDisposeTest() throws InterruptedException {
    Stream<Integer> stream = new Stream<>();
    assertThat(stream.firehose().getConsumerRegistry().stream().count(), is(0L));
    Channel<Integer> chan = stream.channel();
    assertThat(stream.firehose().getConsumerRegistry().stream().count(), is(1L));
    chan.dispose();
    assertThat(stream.firehose().getConsumerRegistry().stream().count(), is(0L));
  }
}
