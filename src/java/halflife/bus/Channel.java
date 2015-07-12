package halflife.bus;

import halflife.bus.concurrent.Atom;
import org.pcollections.PVector;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class Channel<T> {

  private final AnonymousStream<T> stream;
  private final Atom<PVector<T>>   state;
  private final AtomicBoolean      isDrained;

  Channel(AnonymousStream<T> stream,
          Atom<PVector<T>> state) {
    this.stream = stream;
    this.state = state;
    this.isDrained = new AtomicBoolean(false);
    stream.consume(e -> {
      if (!isDrained.get()) {
        state.swap(old -> old.plus(e));
      }
    });
  }

  public T get() {
    if (isDrained.get()) {
      throw new RuntimeException("Channel is already being drained by the stream.");
    }

    return state.swapReturnOther(new Function<PVector<T>, Tuple2<PVector<T>, T>>() {
      @Override
      public Tuple2<PVector<T>, T> apply(PVector<T> buffer) {
        if (buffer.size() == 0) {
          return Tuple.of(buffer,
                          null);
        }

        T t = buffer.get(0);
        if (t == null) {
          return null;
        } else {
          return Tuple.of(buffer.subList(1, buffer.size()),
                          t);
        }
      }
    });
  }

  public AnonymousStream<T> stream() {
    this.isDrained.set(true);
    return this.stream;
  }

  public void tell(T item) {
    stream.notify(item);
  }
}
