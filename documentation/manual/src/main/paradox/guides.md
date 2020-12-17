# Guides

@@@ index
* [Tupler](guides/tupler.md)
* [Custom authentication](guides/custom-authentication.md)
@@@

This part of the documentation provides in-depth guides for specific features.

## `Tupler`

You might have seen implicit `Tupler` parameters in the type signature
of some algebra operations. The @ref[Tupler](guides/tupler.md) guide explains
what it is used for.

## Extending endpoints4s with application-specific concepts

When you have application-specific aspects of your communication endpoints
that are not covered by endpoints4s you can extend the algebras and their
interpreters to include them.

Typically, each application has its own way of dealing with authentication.
The @ref[custom authentication](guides/custom-authentication.md) guide shows how
to enrich the algebras with authentication-related vocabulary and how to
extend the client and server interpreters to consistently implement the
application-specific authentication mechanism.
