package halflife.bus.channel;

import halflife.bus.AnonymousStream;

public interface ConsumingChannel<T> {

  public T get();
  public AnonymousStream<T> stream();

}
