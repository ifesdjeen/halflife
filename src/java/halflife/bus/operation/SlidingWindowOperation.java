package halflife.bus.operation;

import halflife.bus.Firehose;
import halflife.bus.KeyedConsumer;
import halflife.bus.concurrent.Atom;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.List;
import java.util.function.UnaryOperator;

public class SlidingWindowOperation<SRC, DST, V> implements KeyedConsumer<SRC, V> {

  private final Atom<PVector<V>>        buffer;
  private final Firehose                firehose;
  private final UnaryOperator<List<V>> drop;
  private final DST                     destination;

  public SlidingWindowOperation(Firehose firehose,
                                Atom<PVector<V>> buffer,
                                UnaryOperator<List<V>> drop,
                                DST destination) {
    this.buffer = buffer;
    this.firehose = firehose;
    this.drop = drop;
    this.destination = destination;
  }

  @Override
  public void accept(SRC key, V value) {
    PVector<V> newv = buffer.swap((old) -> {
      List<V> dropped = drop.apply(old.plus(value));
      return TreePVector.from(dropped);
    });

    firehose.notify(destination, newv);
  }
}
