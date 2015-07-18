package halflife.kafka;

import halflife.bus.Firehose;

public interface Upstream<K, V> {

  public void startPublishing(Firehose<K, V> firehose);

}
