package halflife.bus;

import halflife.bus.key.Key;
import halflife.bus.registry.ConcurrentRegistry;
import org.junit.After;
import org.junit.Before;
import reactor.core.processor.RingBufferProcessor;
import reactor.fn.Consumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AbstractFirehoseTest {

  protected Firehose<Key> firehose;
  protected ExecutorService executorService;

  @Before
  public void setup() {
    this.executorService = Executors.newFixedThreadPool(16);
    this.firehose = new Firehose<>(new ConcurrentRegistry<>(),
                                   RingBufferProcessor.create(executorService, 2048),
                                   new Consumer<Throwable>() {
                                     @Override
                                     public void accept(Throwable throwable) {
                                       throwable.printStackTrace();
                                     }
                                   });
  }

  @After
  public void teardown() {
  }

}


