package halflife.bus.state;

import halflife.bus.concurrent.Atom;

public interface StatefulSupplier<ST, T> {

  public T get(Atom<ST> state);

}
