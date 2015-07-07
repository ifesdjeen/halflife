package halflife.bus.state;

import halflife.bus.concurrent.Atom;

public interface StateProvider {

  public <T> Atom<T> makeAtom(T init);

}
