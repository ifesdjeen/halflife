# Halflife - in-process channels and streams

Halflife is an evolvement and extension of our recent works and ideas on
[Reactor]() and [Meltdown]().

Main features include:

  * [Named stream topologies](https://github.com/ifesdjeen/halflife#named-stream-topologies)
  * [Anonymous stream topologies](https://github.com/ifesdjeen/halflife#anonymous-stream-topologies)
  * [Matched Lazy Streams](https://github.com/ifesdjeen/halflife#matched-lazy-streams)
  * [Uni- and Bi- directional Channels](https://github.com/ifesdjeen/halflife#uni--and-bi--directional-channels)
  * Independent Per-Entity Streams
  * Atomic State operations
  * Persistent-collection based handlers
  * Multiple dispatch strategies (use same topology to dispatch on Disruptor, ThreadPool or anything else)
  * Integration with Databases for persisting Stream information between restarts
  * Integration with Message Queues for distribution and fault-tolerance

## Terminology

`Stream` is a term coming from Reactive Programming. Stream looks a little like a collection
from the consumer perspective, with the only difference that if collection is a ready set
of events, stream is an infinite collection. If you do `map` operation on the stream,
`map` function will see each and every element coming to the stream.

`Publisher` (`generator` or `producer` in some terminologies) is a function or entity
that publishes items to the stream. `Consumer` (or `listener`, in some terminologies) is
a function that is subscribed to the stream, and will be asyncronously getting items that
the `publisher` publishes to the stream.

In many cases, function can simultaneously be a `consumer` and a `producer`. For example,
`map` is consumed to the events coming to the stream, and publishes modified events
back to the stream.

`Topology` is a stream with a chain of publishers and producers attached to it. For example,
you can have a stream that `maps` items, incrementing each one of them, then a `filter`
that picks up only even incremented numbers. Of course, in real life applications
topologies are much more complex.

`Upstream` / `downstream` are used to describe the order of functions in your topologies.
For example, if you have a stream that `maps` items, incrementing each one of them, it
serves as an upstream for the following `filter` function, that consumes events from it.
`Filter` serves as a `downstream` in this example.

`Named` and `anonymous` streams are just the means of explaning the wiring between
the parts of the topology. If each item within the `named` stream has to know where to
subscribe and where to publish the resulting events, in `anonymous` topologies the
connection between stream parts is implicit, e.g. parts are simply wired by the
systems with unique randomly generated keys.

And a little more description about each one of them:

## Named stream topologies

These are the "traditional" key/value subscriptions": you can subscribe to any named Stream.
As soon as the message with a certain Key is coming to the stream, it will be matched
and passed to all subscribed handlers.

```java
Stream<Integer> intStream = new Stream<>();

intStream.map(Key.wrap("key1"), // subscribe to
              Key.wrap("key2"), // downstream result to
              (i) -> i + 1);    // mapper function
              
intStream.consume(Key.wrap("key2"), // subscribe to result of mapper 
                  (i) -> System.out.println(i));

// send a couple of payloads
intStream.notify(Key.wrap("key1"), 1);
// => 2
intStream.notify(Key.wrap("key1"), 1);
// => 3
```

## Anonymous stream topologies

Anonymous streams are chain of decoupled async stream operations that represent a single
logical operation. Anonymous Stream is subscribed to the particular stream, and will
create all the additional wiring between handlers in the Anonymous Stream automatically.

```java
Stream<Integer> stream = new Stream<>();
AVar<Integer> res = new AVar<>();

stream.anonymous(Key.wrap("source"))       // create an anonymous stream subscribed to "source"
      .map(i -> i + 1)                     // add add 1 to each incoming value
      .map(i -> i * 2)                     // multiply all incoming values by 2
      .consume(i -> System.out.println(i)); // output every incoming value to stdout

firehose.notify(Key.wrap("source"), 1); 
// => 2

firehose.notify(Key.wrap("source"), 2);
// => 4
```

The main difference between Java streams and HalfLife streams is the dispatch flexibility
and combination of multiple streaming paradigms. You can pick the underlaying
dispatcher depending on whether your processing pipeline consists of short or long lived
functions and so on.

## Matched Lazy Streams

Since the flexible key subscription isn't supported and would be representing linear time,
but it is still necessary to be able to subscribe to the keys based on a certain logical
function, Matched Lazy Stream is the way to go: as soon as the first key/value pair is
sent to the handler, all Matchers are queired for subscription. Handlers that match
will be subscribed to the stream, and all the subsequent calls will be on average `O(1)`
lookup time.

```java
stream.matched(key -> key.getPart(0).equals("source")) // create an anonymous stream subscribed to "source"
      .map(i -> i + 1)                                 // add add 1 to each incoming value
      .map(i -> i * 2)                                 // multiply all incoming values by 2
      .consume(i -> System.out.println(i));            // output every incoming value to stdout

firehose.notify(Key.wrap("source", "first"), 1);
// => 2

firehose.notify(Key.wrap("source", "second"), 2);
// => 4
```

As you can see, streams will be created per-entity, which opens op a lot of opportunities
for independent stream processing, entity matching and storing per-entity state.

## Independent Per-Entity Streams

No more need to manage streams for multiple logical entities in the same handler,
we'll do that for you. This future is used together with Matched Lazy Streams, so
every entity stream will have it's own in-memory state.

## Atomic State operations

Since entity streams are independent and locks are expensive, it's important to
keep the operations lock-free. For that you're provided with an `Atom<T>` which
will ensure lock-free atomic updates to the state.

And since the state for entity are split, you're able to save and restore between
restarts of your processing topologies.

```java
Stream<Integer> intStream = new Stream<>(firehose);

intStream.map(Key.wrap("key1"), Key.wrap("key2"), (i) -> i + 1);             
intStream.map(Key.wrap("key2"), Key.wrap("key3"), (Atom<Integer> state) -> { // Use a supplier to capture state in closure 
                return (i) -> {                        // Return a function, just as a "regular" map would do
                  return state.swap(old -> old + i);   // Access internal state
                };
              },
              0);                                      // Pass the initial value for state
intStream.consume(Key.wrap("key3"), ());

intStream.notify(Key.wrap("key1"), 1);
// => 2
intStream.notify(Key.wrap("key1"), 2);
// => 5 
intStream.notify(Key.wrap("key1"), 3);
// => 9
```

## Persistent-collection based handlers

Window, batch and streaming grouping operations are saved in persistent collections.
This ensures a good no-copy memory footprint for all the shared state. Every
collection used is Persistent, and it's immutable internal entires will be
shared between instances.

## Uni- and Bi- directional Channels

Channels are much like a queue you can publish to and pull your changes from
the queue-like object. This feature is particularly useful in scenarios when
you don't need to have neither subscription nor publish hey, and you need
only to have async or sync uni- or bi- directional communication.

```java
Stream<Integer> stream = new Stream<>(firehose);
Channel<Integer> chan = stream.channel();

chan.tell(1);
chan.tell(2);

chan.get();
// => 1

chan.get();
// => 2

chan.get();
// => null
```

Channels can be consumed from streams, too. It is although important to remember
that in order to avoid unnecessary state accumulation Channels can either be
used as a stream or as a channel:

```java
Stream<Integer> stream = new Stream<>(firehose);
Channel<Integer> chan = stream.channel();

chan.stream()
    .map(i -> i + 1)
    .consume(i -> System.out.println(i));

chan.tell(1);
// => 2

chan.get();
// throws RuntimeException, since channel is already drained by the stream
```

Channels can also be split to publishing and consuming channels for type safety,
if you need to ensure that consuming part can't publish messages and publishing
part can't accidentally consume them:

```java
Stream<Integer> stream = new Stream<>(firehose);
Channel<Integer> chan = stream.channel();

PublishingChannel<Integer> publishingChannel = chan.publishingChannel();
ConsumingChannel<Integer> consumingChannel = chan.consumingChannel();

publishingChannel.tell(1);
publishingChannel.tell(2);

consumingChannel.get();
// => 1

consumingChannel.get();
// => 2
```

# License

Copyright © 2014 Alex Petrov

Distributed under the Eclipse Public License, the same as Clojure.






