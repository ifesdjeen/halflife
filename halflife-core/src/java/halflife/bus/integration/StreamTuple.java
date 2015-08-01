package halflife.bus.integration;

import halflife.bus.key.Key;

public final class StreamTuple<K extends Key, V> {

  private final K key;
  private final V value;

  public StreamTuple(K key,
                     V value) {
    this.key = key;
    this.value = value;
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    StreamTuple that = (StreamTuple) o;

    if (key != null ? !key.equals(that.key) : that.key != null)
      return false;
    if (value != null ? !value.equals(that.value) : that.value != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = key != null ? key.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }
}
