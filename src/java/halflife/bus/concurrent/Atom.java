package halflife.bus.concurrent;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Generic Atom
 */
public class  Atom<T> {

  private AtomicReference<T> ref;

  public Atom(T ref) {
    this.ref = new AtomicReference<T>(ref);
  }

  public T deref() {
    return ref.get();
  }

  public T swap(SwapFn<T> swapOp) {
    for (;;) {
      T old = ref.get();
      T newv = swapOp.swap(old);
      if(ref.compareAndSet(old, newv)) {
        return newv;
      }
    }
  }

  public T swapReturnOld(SwapFn<T> swapOp) {
    for (;;) {
      T old = ref.get();
      T newv = swapOp.swap(old);
      if(ref.compareAndSet(old, newv)) {
        return old;
      }
    }
  }

  public T reset(T newv) {
    for (;;) {
      T old = ref.get();
      if(ref.compareAndSet(old, newv)) {
        return newv;
      }
    }
  }


}

