
JOML
====

Jukka's Obvious, Minimal Language for configuration, inspired by TOML.

By Jukka-Pekka Rahkonen

Objectives
----------

JOML aims to be a minimal configuration file format that's easy to
read and write due to it's obvious and simple semantics and syntax.

There should be nothing left to leave out. 

Key design decisions and features
-----------------------------------
- only key value pairs
- only one level of nesting
- one level of inheritance
- use of variables
- eval template


Example
-------

```joml
[server]
  host = 127.0.0.1
  port = 8080

[esb]
  url = http://foo.bar.com/esb
  userid = 11111
  passwd = xxxxx

[endpoint]
  foo = $(esb.url)/foo
  bar = http://$(server.host):$(server.port)

[cluster.s1]
  port = 9001

[cluster.s2]
  port = 9002
  
```

