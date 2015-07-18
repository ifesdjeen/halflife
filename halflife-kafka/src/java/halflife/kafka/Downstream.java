package halflife.kafka;

import halflife.bus.Firehose;

public interface Downstream<K, V> {

  public void startConsuming(Firehose<K, V> firehose);

}
