# Halflife - in-process challels and streams

Halflife is an evolvement and extension of our recent works and ideas on
[Reactor]() and [Meltdown]().

Main features include:

  * Named stream topologies
  * Anonymous stream topologies
  * Lazy topologies
  * Matched Lazy Streams
  * Uni- and Bi- directional Channels 
  * Independent Per-Entity Streams
  * Atomic State operations
  * Persistent-collection based handlers
  * Integration with Databases for persisting Stream information between restarts
  * Integration with Message Queues for distribution and fault-tolerance
  
And a little more description about each one of them:

## Named stream topologies

These are the "traditional" key/value subscriptions": you can subscribe to any named Stream.
As soon as the message with a certain Key is coming to the stream, it will be matched
and passed to all subscribed handlers.

## Anonymous stream topologies

Anonymous streams are chain of decoupled async stream operations that represent a single
logical operation. Anonymous Stream is subscribed to the particular stream, and will
create all the additional wiring between handlers in the Anonymous Stream automatically.

## Matched Lazy Streams

Since the flexible key subscription isn't supported and would be representing linear time,
but it is still necessary to be able to subscribe to the keys based on a certain logical
function, Matched Lazy Stream is the way to go: as soon as the first key/value pair is
sent to the handler, all Matchers are queired for subscription. Handlers that match
will be subscribed to the stream, and all the subsequent calls will be on average `O(1)`
lookup time.

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

# License

Copyright © 2014 Alex Petrov

Distributed under the Eclipse Public License, the same as Clojure.






