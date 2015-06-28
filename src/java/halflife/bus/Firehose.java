package halflife.bus;

import halflife.bus.registry.Registration;
import halflife.bus.registry.Registry;
import reactor.core.Dispatcher;
import reactor.core.support.Assert;
import reactor.fn.Consumer;


public class Firehose<T> {

  private final Dispatcher                    dispatcher;
  private final Registry<Object, Consumer<T>> consumerRegistry;
  private final Consumer<Throwable>           dispatchErrorHandler;

  public Firehose(Dispatcher dispatcher,
                  Registry<Object, Consumer<T>> registry,
                  Consumer<Throwable> dispatchErrorHandler) {
    this.dispatcher = dispatcher;
    this.consumerRegistry = registry;
    this.dispatchErrorHandler = dispatchErrorHandler;
  }

  public Firehose<T> notify(Object key, T ev) {
    Assert.notNull(key, "Key cannot be null.");
    Assert.notNull(ev, "Event cannot be null.");

    dispatcher.dispatch(ev, new Consumer<T>() {
      @Override
      public void accept(T t) {
        for (Registration<Object, ? extends Consumer<T>> reg: consumerRegistry.select(key)) {
          reg.getObject().accept(t);
        }
      }
    }, dispatchErrorHandler);

    return this;
  }


}