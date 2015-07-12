package com.instana.processor;

import halflife.bus.AnonymousStream;
import halflife.bus.Stream;
import halflife.bus.concurrent.AVar;
import halflife.bus.key.Key;
import org.junit.Test;

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

}
