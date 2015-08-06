package halflife.bus.channel;

import halflife.bus.AnonymousStream;

import java.util.concurrent.TimeUnit;

public interface ConsumingChannel<T> {

  public T get();
  public T get(long time, TimeUnit timeUnit) throws InterruptedException;
  public AnonymousStream<T> stream();

}
