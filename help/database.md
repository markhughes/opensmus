![OpenSMUS Logo](https://raw.githubusercontent.com/markhughes/opensmus/main/help/images/logo2_icone.gif)

## [OPENSMUS HELP](index.md): DATABASE SUPPORT

**OpenSMUS** servers support all the DBObject commands used in Shockwave MultiUser Server 2 and 3, and some new ones.
These cover all DBAdmin, DBApplication, DBUser and DBPlayer functions. Older database commands used in SMUS 1 have been
deprecated by Macromedia, and are NOT supported by **OpenSMUS**. A list of all commands and their syntax is available in
the Command List section of this document.

The OpenSMUS download contains a package with [HSQL Database engine](http://sourceforge.net/projects/hsqldb/). HSQLDB is
free for redistribution and inclusion in commercial and non-commercial projects. All source code changes made by the
Tabuleiro team to the HSQL engine were available to the HSQLDB developer community as part of the 1.6.1 release, when we
joined the development team for the project briefly in late 2001. HSQLDB is a very fast in-memory 100% Java database
tool, and it runs in the same VM session of the OpenSMUS server for the best possible performance.

There is no configuration or installation needed to use databases in OpenSMUS. The server will automatically create and
initialize a default database the first time it is started. The following files will be created in the server directory:

- OpenSMUSDB.data
- OpenSMUSDB.backup
- OpenSMUSDB.script
- OpenSMUSDB.properties

These files contain the cached version of the database. It is recommended that at least one administrative user is
created in the database, to allow access to administrative database and server administration commands in the future.
This can be accomplished with the "CreateUser" directive in the OpenSMUS.cfg file:

```
CreateUser = admin,pass,100
```

This command will create a user named "admin", with password "pass" and user level 100 the next time the server is started. We have authored a Director tool called "DatabaseAdministrator" that is available in the DOWNLOAD section of the OpenSMUS site to make it easier to manipulate and administrate the database contents. But you can also use any other Director movie to interface with the database, as long as it uses the supported DBObject commands.
