package halflife.bus;

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

public class AbstractFirehoseTest {

  protected Dispatcher                                           dispatcher;
  protected DefaultingRegistry<Key, KeyedConsumer<Key, Integer>> consumerRegistry;
  protected Consumer<Throwable>                                  dispatchErrorHandler;
  protected Firehose<Key, Integer>                               firehose;

  @Before
  public void setup() {
    this.dispatcher = new SynchronousDispatcher();
    this.consumerRegistry = new ConcurrentRegistry<>();
    this.dispatchErrorHandler = throwable -> {
      System.out.println(throwable.getMessage());
      throwable.printStackTrace();
    };

    this.firehose = new Firehose<>(dispatcher,
                                   consumerRegistry,
                                   dispatchErrorHandler);
  }

  @After
  public void teardown() {
    this.dispatcher.shutdown();
    this.firehose = null;
  }


}


