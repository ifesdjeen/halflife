package halflife.bus.state;

import halflife.bus.concurrent.Atom;

public interface StateProvider {

  public <SRC, T> Atom<T> makeAtom(SRC src, T init);

}
