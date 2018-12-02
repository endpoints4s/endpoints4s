# Guides

This part of the documentation provides in-depth guides for specific features.

## JSON codecs

When working with JSON entities, the large variety of algebras and interpreters
might make it a bit hard to see which ones you should use. The
[JSON codecs](/guides/json-codecs.md) guide helps you to choose the solution
that fits your need.

## `Tupler`

You might have seen implicit `Tupler` parameters in the type signature
of some algebra operations. The [Tupler](/guides/tupler.md) guide explains
what it is used for.

## Extending *endpoints* with application-specific concepts

When you have application-specific aspects of your communication endpoints
that are not covered by *endpoints* you can extend the algebras and their
interpreters to include them.

Typically, each application has its own way of dealing with authentication.
The [custom authentication](/guides/custom-authentication.md) guide shows how
to enrich the algebras with authentication-related vocabulary and how to
extend the client and server interpreters to consistently implement the
application-specific authentication mechanism.
