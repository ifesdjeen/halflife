package halflife.bus;


import halflife.bus.key.Key;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

class FinalizedMatchedStream<V> {

  protected final List<MatchedStream.StreamSupplier> suppliers;

  protected FinalizedMatchedStream(List<MatchedStream.StreamSupplier> suppliers) {
    this.suppliers = suppliers;
  }

  public Function<Key, List<KeyedConsumer<? extends Key, V>>> subscribers(Stream stream) {
    return new Function<Key, List<KeyedConsumer<? extends Key, V>>>() {
      @Override
      public List<KeyedConsumer<? extends Key, V>> apply(Key key) {
        List<KeyedConsumer<? extends Key, V>> consumers = new LinkedList<>();

        Key currentKey = key;
        for (MatchedStream.StreamSupplier supplier : suppliers) {
          Key nextKey = currentKey.derive();
          System.out.printf("Subscribing: %s %s %s\n", currentKey, nextKey, supplier);
          consumers.add(supplier.get(currentKey, nextKey, stream));
          currentKey = nextKey;
        }
        return consumers;
      }
    };


    //
    //        for (MatchedStream.StreamSupplier supplier : suppliers) {
    //          consumers.add(supplier.get(););
    //        }
    //               new KeyedConsumer<Key, V>() {
    //                @Override
    //                public void accept(Key k, V value) {
    //                  Key currentKey = k;
    //
    //                    Key nextKey = currentKey.derive();
    //                    System.out.printf("Subscribing: %s %s %s\n", currentKey, nextKey, supplier);
    //                    supplier.get(currentKey, nextKey, stream);
    //                    currentKey = nextKey;
    //                  }
    //                  stream.notify(k, value);
    //                };
    //              };
    //
    //


    //    return () -> {
    //      List<KeyedConsumer<Key, V>> consumers = new LinkedList<>();

    //       new KeyedConsumer<Key, V>() {
    //        @Override
    //        public void accept(Key k, V value) {
    //          Key currentKey = k;
    //          for (MatchedStream.StreamSupplier supplier : suppliers) {
    //            Key nextKey = currentKey.derive();
    //            System.out.printf("Subscribing: %s %s %s\n", currentKey, nextKey, supplier);
    //            supplier.get(currentKey, nextKey, stream);
    //            currentKey = nextKey;
    //          }
    //          stream.notify(k, value);
    //        };
    //      };

    //      return consumers;
    //    };
  }
}
