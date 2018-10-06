# Guides

This part of the documentation provides in-depth guides for specific features.

## Extending *endpoints* with application-specific concepts

When you have application-specific aspects of your communication endpoints
that are not covered by *endpoints* you can extend the algebras and their
interpreters to include them.

Typically, each application has its own way of dealing with authentication.
The [custom authentication](guides/custom-authentication.md) guide shows how
to enrich the algebras with authentication-related vocabulary and how to
extend the client and server interpreters to consistently implement the
application-specific authentication mechanism.
