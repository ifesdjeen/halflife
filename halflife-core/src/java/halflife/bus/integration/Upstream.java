package halflife.bus.integration;

import halflife.bus.Firehose;

public abstract class Upstream<K, V> {

  private final Firehose firehose;

  public Upstream(Firehose firehose) {
    this.firehose = firehose;
  }

  public void publish(K key, V value) {
    this.firehose.notify(key, value);
  }
}
