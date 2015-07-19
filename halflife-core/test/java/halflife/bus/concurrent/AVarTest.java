package halflife.bus.concurrent;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AVarTest {

  @Test
  public void setTest() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(16);

    AVar<Integer> aVar = new AVar<>();

    int setters = 100;
    CountDownLatch latch = new CountDownLatch(setters);
    for(int i = 0; i < setters; i++) {
      final int finalI = i + 100;
      executor.submit(() -> {
        aVar.set(finalI);
        latch.countDown();
      });
    }

    latch.await(1, TimeUnit.SECONDS);

    assertThat(aVar.get(1, TimeUnit.SECONDS), is(100));
    executor.shutdown();
  }
}
