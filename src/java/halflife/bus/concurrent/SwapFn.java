package halflife.bus.concurrent;

public interface SwapFn<T> {
  public T swap(T old);
}
