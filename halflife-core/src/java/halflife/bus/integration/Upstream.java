package halflife.bus.integration;

import halflife.bus.Firehose;
import halflife.bus.key.Key;

public abstract class Upstream<K extends Key, V> {

  private final Firehose firehose;

  public Upstream(Firehose firehose) {
    this.firehose = firehose;
  }

  public void publish(StreamTuple<K, V> streamTuple) {
    this.firehose.notify(streamTuple.getKey(), streamTuple.getValue());
  }
}
