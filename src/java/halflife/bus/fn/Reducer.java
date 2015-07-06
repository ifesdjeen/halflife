package halflife.bus.fn;

import java.util.function.BiFunction;

/**
 * Java Reducer interface is much too confusing.
 */
public class Reducer {

  public static <T, R> R reduce(BiFunction<R, T, R> reducer, Iterable<T> input, R init) {
    R acc = init;
    for (T element : input) {
      acc = reducer.apply(acc, element);
    }
    return acc;
  }
}
