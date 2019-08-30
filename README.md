
JOML
====

Jukka's Obvious, Minimal Language for configuration, inspired by TOML.

By Jukka-Pekka Rahkonen

Objectives
----------

JOML aims to be a minimal configuration file format that's easy to
read and write due to it's obvious and simple semantics and syntax.
Syntax is kept so simple that it needs only 100-200 lines of code and
an hour to write and test a parser and template evaluator in any
decent programming language.

There should be nothing left to leave out. 

Key design decisions and features
-----------------------------------
- only key value pairs
- only one level of nesting with prefix
- one level of inheritance (to be implemented)
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

# this is a comment line

[cluster.s1]          # end of line komment
  port = 9001
  tag = \#hashtag     # if value has # it must be escaped
  dollar = \$(sign)   # if value contains $( and its not variable
                      # it must be escaped 
  
[cluster.s2]
  port = 9002
  value1 = multi\
  	  line value
  value2 = multiline \
           value       # value1 == value2 

[win10]
  path  = C:\windows\system32\drivers   # only # and $ are escaped with '\'
  path2 = C:\\easy\to\write\windoze\paths  # no need to escape escape

#
# env. prefix is only reserved prefix keyword and it is used
# when configuration is evaluated for given environment
#

[env.local.1]
  port = 7777

[env.local.2]         # env. prefix is only special keyword 
  server.port = 8888  # this overrides server.port = 8080 above      
  usb.userid = 22222  # this overrides usb.userid = 11111 above

```

Syntax
-------
- all lines are terminated by \n or \r\n or \r
- all lines are trimmed (so indentions are optional)
- empty lines are ignored
- comment lines start with '#'
- end of line comment starts from first '#' that is not escaped with '\\'
- line can be either
  1. key = value
  or
  2. [prefix.a.b.c]
- prefix which starts with "env." are environments: for example
  "env.local.2" means that it overrides given settings
  when configuration is evaluated for envionment "local 2"
- values are strings from first '=' to end of line and trimmed
- keys are any strings that do not contain '='
- by escaping the line end by '\\' produces multiline values
- $(var.name) will be substituted with config var.name value when evaluated


