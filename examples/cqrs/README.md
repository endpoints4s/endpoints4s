# CQRS Example

This example demonstrates the following features:

 - [x] internal communication (between microservices) using multiplexed
       endpoints ;
 - [x] statically typed Scala.js and JVM communication ;
 - [ ] assets fetched by the Scala.js client can be cached for an infinite duration ;
 - [ ] HATEOAS ;
 - [ ] Human-readable generated documentation.

## Business Domain

The application allows users to monitor **meters** (like electricity meter, for instance). Users can register
meters, report their evolution over time, and visualize these data.

## Architecture

The project is broken down into the following sub-projects:

~~~
       +-----------------+  +--------------------+     +----------+
       | public-endpoints|  | commands-endpoints +-----+ commands |
       +----+--------+---+  +------+-----------+-+     +----------+
           /          \           /             \
          /            \         /               \
    +----+-------+    +-+-------+-----+           \
    | web-client |    | public-server |            \
    +------------+    +--------+------+             \
                                \                    \
                                 \                    \
                           +------+------------+     +-+-------+
                           | queries-endpoints +-----+ queries |
                           +-------------------+     +---------+
~~~

- `public-server` provides the public HTTP server ;
- `commands` and `queries` are internal microservices used by the public
  server ;
- `web-client` is a web based client for the service.

Each sub-project named `xxx-endpoints` provides a description of HTTP API
implemented by the sub-project `xxx`.