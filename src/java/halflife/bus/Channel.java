package halflife.bus;

import halflife.bus.concurrent.Atom;
import org.pcollections.PVector;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;

import java.util.function.Consumer;
import java.util.function.Function;

public class Channel<T> {

  private final AnonymousStream<T> stream;
  private final Atom<PVector<T>> state;

  Channel(AnonymousStream<T> stream,
          Atom<PVector<T>> state) {
    this.stream = stream;
    this.state = state;
    stream.consume(e -> {
      state.swap(old -> old.plus(e));
    });
  }

  public T get() {
    return state.swapReturnOther(new Function<PVector<T>, Tuple2<PVector<T>, T>>() {
      @Override
      public Tuple2<PVector<T>, T> apply(PVector<T> buffer) {
        T t = buffer.get(0);
        if(t == null) {
          return null;
        } else {
          return Tuple.of(buffer.subList(1, buffer.size() - 1),
                          t);
        }

      }
    });
  }

//  public void tell(T item) {
//    stream.notify(item);
//  }
}
