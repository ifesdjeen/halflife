package halflife.bus;


import halflife.bus.key.Key;

import java.util.*;
import java.util.function.Function;

class FinalizedMatchedStream<V> {

  protected final List<MatchedStream.StreamSupplier> suppliers;

  protected FinalizedMatchedStream(List<MatchedStream.StreamSupplier> suppliers) {
    this.suppliers = suppliers;
  }

  public Function<Key, Map<Key, KeyedConsumer<? extends Key, V>>> subscribers(Stream stream) {
    return new Function<Key, Map<Key, KeyedConsumer<? extends Key, V>>>() {
      @Override
      public Map<Key, KeyedConsumer<? extends Key, V>> apply(Key key) {
        Map<Key, KeyedConsumer<? extends Key, V>> consumers = new LinkedHashMap<>();

        Key currentKey = key;
        for (MatchedStream.StreamSupplier supplier : suppliers) {
          Key nextKey = currentKey.derive();
          consumers.put(currentKey, supplier.get(currentKey, nextKey, stream));
          currentKey = nextKey;
        }
        return consumers;
      }
    };

  }
}
