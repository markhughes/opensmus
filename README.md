# OpenSMUS

This is a fork of the original from source forge.

OpenSMUS is a server application that runs on Linux, Solaris, MacOSX, FreeBSD and almost any Java-enabled operational system. OpenSMUS is 100% compatible with the Shockwave MultiUser protocol published by Macromedia, so movies authored for the Shockwave MultiUserServer version 2 and 3 can connect seamlessly to an OpenSMUS server, without modifications.

## Building

You require maven!

You can use the build script `build.sh` to compile everything nicely.

```
mvn compile
mvn package
```

## About

Java is recognized as one of the strongest platform solutions for server applications that need to service hundreds or thousands of users at the same time. Macromedia, Adobe and other vendors offer server products that work on top of the Java 2 platform, like ColdFusion or JBoss. OpenSMUS is 100% pure Java code, including the database engine, and works with the any Java platform VM (JDK 1.5 or later.) OpenSMUS provides an alternative to Shockwave developers that need to host multiuser movies in Unix systems, while preserving the investment made in learning the Shockwave MultiUser API.

All Shockwave MultiUser Server messages and commands can be used transparently with an OpenSMUS server, including DBObject functions. OpenSMUS includes a database engine so no database setup is required on the hosting machine, and database files are created and initialized automatically by the server. OpenSMUS also implement commands not available in the Shockwave MultiUser Server, like the ability to add banned user entries and ip addresses to the database and restart or shutdown the server remotely.

OpenSMUS can work transparently with TCP or UDP connections as well, when used with Director 8.5 Shockwave movies that support UDP connections. No special Xtra is required for your Shockwave movies to connect with an OpenSMUS Server: Shockwave treats OpenSMUS connection just like regular Shockwave MultiUser Server 3 connections, using the same commands and protocols. The only command that returns a different result is "system.server.getVersion", and you can use this command to determine if extended OpenSMUS functions are available to your Shockwave movies.

The most important feature not supported by OpenSMUS in comparison with version 3 of the Shockwave MultiUser Server is server side Lingo scripting, since a Lingo interpreter is not available for other platforms. The server side scripting language used in OpenSMUS is Java: this is usually a more powerful solution for enterprise-level servers. More information about server side scripting is available in the OpenSMUS Online Help.

## History

Version 2.0 - 06/01/2011

- Rewritten network core. Now supports virtually unlimited connections.
- Movies can now have multiple server side scripts.
- Server side scripts can be dynamically reloaded and removed.
- Better server side script debugging.
- Improved security features. Including a completely new flood detection filter.

Version 1.0.1 - 04/15/2009

- Unicode support added.

Version 1.0 - 01/01/2009

- Initial open source release.

This program was developed originally by Mauricio Piacentini from Tabuleiro Producoes, as the Nebulae MultiUserServer. The original code was first created in 2001, and went through several revisions over the course of the years as a commercial project. At the end of 2008 the decision to create an open source version of the server was made, with the encouragement of several members of the Shockwave and Director game developers community. An initial version was created based on Nebulae 2.0.1, and the resulting program was renamed OpenSMUS, and released by the original author under a permissive open source license. Icons, documentation and the website design were contributed by Raquel Ravanini, also from Tabuleiro.
