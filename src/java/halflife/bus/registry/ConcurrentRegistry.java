package halflife.bus.registry;

import halflife.bus.concurrent.Atom;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PVector;
import org.pcollections.TreePVector;
import reactor.fn.tuple.Tuple;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ConcurrentRegistry<K, V> implements DefaultingRegistry<K, V> {

  private final Atom<PMap<Object, PVector<Registration<K, ? extends V>>>> lookupMap;
  private final Map<KeyMissMatcher<K>, Function<K, Map<K, ? extends V>>>  keyMissMatchers;

  public ConcurrentRegistry() {
    this.lookupMap = new Atom<>(HashTreePMap.empty());
    this.keyMissMatchers = new ConcurrentHashMap<>();
  }

  @Override
  public void addKeyMissMatcher(KeyMissMatcher<K> matcher, Function<K, Map<K, ? extends V>> supplier) {
    this.keyMissMatchers.put(matcher, supplier);
  }

  @Override
  public Registration<K, V> register(K obj, V handler) {
    final PVector<Registration<K, ? extends V>> lookedUpArr = lookupMap.deref().get(obj);

    if (lookedUpArr == null) {
      final SimpleRegistration<K, V> reg = new SimpleRegistration<>(obj,
                                                                    handler,
                                                                    // TODO: FIX UNREGISTER
                                                                    reg1 -> lookupMap.deref()
                                                                                     .get(obj)
                                                                                     .remove(handler));
      final PVector<Registration<K, ? extends V>> emptyArr = TreePVector.singleton(reg);

      lookupMap.swap(old -> old.plus(obj, emptyArr));

      return reg;

    } else {
      final Registration<K, V> reg = new SimpleRegistration<>(obj,
                                                              handler,
                                                              // TODO: FIX REMOVES!!
                                                              reg1 -> lookupMap.deref()
                                                                               .get(obj)
                                                                               .remove(reg1));
      lookupMap.swap(old -> old.plus(obj, old.get(obj).plus(reg)));
      return reg;
    }

  }

  @Override
  public boolean unregister(K key) {
    return lookupMap.swapReturnOther((map) -> {
      PMap<Object, PVector<Registration<K, ? extends V>>> newv = map.minus(key);

      return Tuple.of(newv,
                      map.containsKey(key));
    });
  }

  @Override
  public List<Registration<K, ? extends V>> select(final K key) {
    return
      lookupMap.swap(old -> {
        if (old.containsKey(key)) {
          return old;
        } else {
          return keyMissMatchers.entrySet()
                                .stream()
                                .filter((m) -> {
                                  return m.getKey().test(key);
                                })
                                .map(
                                  (Map.Entry<KeyMissMatcher<K>, Function<K, Map<K, ? extends V>>> m) -> {
                                    return m.getValue();
                                  })
                                .flatMap((Function<K, Map<K, ? extends V>> m) -> {
                                  return m.apply(key).entrySet().stream();
                                })
                                .reduce(old,
                                        (PMap<Object, PVector<Registration<K, ? extends V>>> acc,
                                         Map.Entry<K, ? extends V> entry) -> {
                                          Registration<K, V> reg = new SimpleRegistration<K, V>(
                                            entry.getKey(),
                                            entry.getValue(),
                                            // TODO: Fix removes!
                                            null);

                                          if (acc.containsKey(entry.getKey())) {
                                            return acc.plus(entry.getKey(),
                                                            acc.get(entry.getValue()).plus(reg));
                                          } else {
                                            return acc.plus(entry.getKey(),
                                                            TreePVector.singleton(reg));
                                          }
                                        },
                                        PMap::plusAll);


        }
      }).getOrDefault(key, TreePVector.empty());

  }


  @Override
  public void clear() {
    //    this.lookupMap.clear();
    //    this.keyMissMatchers.clear();
  }


  @Override
  public Iterator<Registration<K, V>> iterator() {
    return null;
  }
}
