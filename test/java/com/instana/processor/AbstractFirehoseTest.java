package com.instana.processor;

import halflife.bus.Firehose;
import halflife.bus.KeyedConsumer;
import halflife.bus.registry.ConcurrentRegistry;
import halflife.bus.registry.DefaultingRegistry;
import org.junit.After;
import org.junit.Before;
import reactor.core.Dispatcher;
import reactor.core.dispatch.SynchronousDispatcher;
import reactor.fn.Consumer;

public class AbstractFirehoseTest {

  protected Dispatcher                                      dispatcher;
  protected DefaultingRegistry<Object, KeyedConsumer<Object, Object>> consumerRegistry;
  protected Consumer<Throwable>                             dispatchErrorHandler;
  protected Firehose                                        firehose;

  @Before
  public void setup() {
    this.dispatcher = new SynchronousDispatcher();
    this.consumerRegistry = new ConcurrentRegistry<>();
    this.dispatchErrorHandler = throwable -> {
      System.out.println(throwable.getMessage());
      throwable.printStackTrace();
    };

    this.firehose = new Firehose(dispatcher,
                                 consumerRegistry,
                                 dispatchErrorHandler);
  }

  @After
  public void teardown() {
    this.dispatcher.shutdown();
    this.firehose = null;
  }


}


