![OpenSMUS Logo](https://raw.githubusercontent.com/markhughes/opensmus/main/help/images/logo2_icone.gif)

## [OPENSMUS HELP](index.md): GETTING STARTED

**these are the original docs and have not been updated yet, you required JDK 1.8**

OpenSMUS is a server application that runs on Linux, Solaris, MacOSX, Windows, and almost any Java-enabled operational system. It was created originally in 2001 by Tabuleiro Producoes as the Nebulae MultiUserServer, a commercial product, and released as open-source software on January 1st, 2009, as a gift to the worldwide community of Director developers. It is 100% compatible with the Shockwave MultiUser protocol published by Macromedia, so movies authored for the Shockwave MultiUser Server version 2 and 3 can connect seamlessly to an OpenSMUS server, without modifications.

Java is considered one of the strongest platforms for server applications that need to service hundreds or thousands of users at the same time. OpenSMUS is 100% pure Java 2 code and works with JDK 1.5 or later. OpenSMUS provides an alternative to Shockwave developers that need to host multiuser movies in Unix systems, while preserving the investment made in learning the Shockwave MultiUser API.

All Shockwave MultiUser Server standard messages and commands can be used transparently with an OpenSMUS server, including DBObject functions. OpenSMUS includes a database engine so no database setup is required on the hosting machine, and database files are created and initialized automatically by the server. OpenSMUS also implements commands not available in the Shockwave MultiUser Server, like the ability to add banned user entries and IP addresses to the database, restart or shut down the server remotely, send email, and interface with standard SQL database engines.

The most important feature not supported by OpenSMUS in comparison with version 3 of the Shockwave MultiUser Server is server-side Lingo scripting, since a Lingo interpreter is not available for other platforms. The server-side scripting language used in OpenSMUS is Java: this is usually a more powerful solution for enterprise-level servers.

Please click [here](serversidescript.md) for more information about server-side scripting in OpenSMUS.

UDP protocol support is available when OpenSMUS is used with Director 8.5 or later and Shockwave applications.
