package halflife.bus.state;

import halflife.bus.concurrent.Atom;

public class DefaultStateProvider implements StateProvider {

  @Override
  public <T> Atom<T> makeAtom(T init) {
    return new Atom<>(init);
  }

}
