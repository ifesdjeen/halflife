package halflife.bus.key;

import java.util.Arrays;
import java.util.UUID;

public class Key {

  private final Object[] parts;
  private final boolean isDerived;

  public Key(Object[] parts) {
    this(parts, false);
  }

  protected Key(Object[] parts, boolean isDerived) {
    this.parts = parts;
    this.isDerived = isDerived;
  }

  public Key derive() {
    Object[] newKey = new Object[parts.length + 1];
    System.arraycopy(parts, 0, newKey, 0, parts.length);
    newKey[parts.length] = UUID.randomUUID();
    return new Key(newKey);
  }

  public Object getPart(int index) {
    if (index < parts.length - 1) {
      throw new RuntimeException(String.format("Can't get a part with index %d from %d long key",
                                               index,
                                               parts.length));
    } else {
      return parts[index];
    }
  }

  public boolean isDerived() {
    return isDerived;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Key key = (Key) o;

    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    if (!Arrays.equals(parts, key.parts))
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    return parts != null ? Arrays.hashCode(parts) : 0;
  }

  public static Key wrap(Object k) {
    return new Key(new Object[] { k } );
  }

  @Override
  public String toString() {
    return "Key{" +
           "parts=" + Arrays.toString(parts) +
           '}';
  }
}

