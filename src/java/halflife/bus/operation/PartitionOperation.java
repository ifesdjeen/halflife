package halflife.bus.operation;

import halflife.bus.Firehose;
import halflife.bus.KeyedConsumer;
import halflife.bus.concurrent.Atom;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.List;
import java.util.function.Predicate;

public class PartitionOperation<SRC, DST, V> implements KeyedConsumer<SRC, V> {

  private final Atom<PVector<V>>   buffer;
  private final Firehose           firehose;
  private final Predicate<List<V>> emit;
  private final DST                destination;

  public PartitionOperation(Firehose firehose,
                            Atom<PVector<V>> buffer,
                            Predicate<List<V>> emit,
                            DST destination) {
    this.buffer = buffer;
    this.firehose = firehose;
    this.emit = emit;
    this.destination = destination;
  }

  @Override
  @SuppressWarnings(value = {"unchecked"})
  public void accept(SRC key, V value) {
    PVector<V> newv = buffer.swap((old) -> old.plus(value));
    if (emit.test(newv)) {
      PVector<V> downstream = buffer.swapReturnOld((old) -> TreePVector.empty());
      firehose.notify(destination, downstream);
    }
  }
}
