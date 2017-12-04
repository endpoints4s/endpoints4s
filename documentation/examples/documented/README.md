# Documented Example

This example demonstrates how to write the description of an HTTP API once and then
get for free both the HTTP server implementation and the OpenAPI documentation.

The application implements a counter whose value can be queried and updated. All the
code lives in a single file [Counter.scala](src/main/scala/counter/Counter.scala).

You can see it live here: http://documented-counter.herokuapp.com. You can play
with the API directly from the Swagger UI to query the counter value and change it.