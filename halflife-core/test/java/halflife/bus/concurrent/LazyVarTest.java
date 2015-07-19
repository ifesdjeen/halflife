package halflife.bus.concurrent;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class LazyVarTest {

  @Test
  public void testSupply() throws InterruptedException {
    AtomicInteger supplierCalls = new AtomicInteger();
    ExecutorService executor = Executors.newFixedThreadPool(16);

    LazyVar<Integer> lazyVar = new LazyVar<>(() -> {
      supplierCalls.incrementAndGet();
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      return 1;
    });

    List<Integer> reads = new LinkedList<>();
    CountDownLatch latch = new CountDownLatch(10);

    for (int i = 0; i < 10; i++) {
      executor.submit(() -> {
        try {
          reads.add(lazyVar.get());
          latch.countDown();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      });
    }

    latch.await(1, TimeUnit.SECONDS);

    for (int i = 0; i < 10; i++) {
      assertThat(reads.get(i), is(1));
    }

    assertThat(supplierCalls.get(), is(1));
    executor.shutdown();
  }
}
