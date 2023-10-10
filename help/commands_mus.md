![OpenSMUS Logo](https://raw.githubusercontent.com/markhughes/opensmus/main/help/images/logo2_icone.gif)

## [OPENSMUS HELP](index.md): MUS COMMAND LIST

OpenSMUS is 100% compatible with the standard Shockwave MultiUser Server commands as documented in the SMUS version 3 manual, "Using the Shockwave MultiUser Server and Xtra". Almost all multiuser movies authored to SMUS 3 can connect to an OpenSMUS server seamlessly, with no modification required. Below is a list of all standard MUS commands supported by OpenSMUS. We recommend using the standard MUS command syntax described in the Director manuals to keep your movies compatible with future versions of OpenSMUS and the SMUS server, so please refer to your Director documentation for more information about movie syntax. Please note that the older database commands for SMUS version 1 are NOT supported.

- system.server.getTime
- system.server.getVersion
- system.server.getMovieCount
- system.server.getMovies
- system.movie.getUserCount
- system.movie.getGroups
- system.movie.getGroupCount
- system.movie.enable
- system.movie.disable
- system.movie.delete
- system.group.join
- system.group.leave
- system.group.getUserCount
- system.group.getUsers
- system.group.enable
- system.group.disable
- system.group.delete
- system.group.createUniqueName
- system.group.setAttribute
- system.group.getAttribute
- system.group.getAttributeNames
- system.group.deleteAttribute
- system.user.changeMovie \*
- system.user.getGroups
- system.user.getGroupCount
- system.user.getAddress
- system.user.delete
- system.DBAdmin.createApplication
- system.DBAdmin.deleteApplication
- system.DBAdmin.declareAttribute
- system.DBAdmin.deleteAttribute
- system.DBAdmin.createApplicationData
- system.DBAdmin.deleteApplicationData
- system.DBAdmin.getUserCount \*\*
- system.DBAdmin.getUserNames \*\*
- system.DBApplication.setAttribute
- system.DBApplication.getAttribute
- system.DBApplication.getAttributeNames
- system.DBApplication.deleteAttribute
- system.DBApplication.getApplicationData
- system.DBUser.setAttribute
- system.DBUser.getAttribute
- system.DBUser.getAttributeNames
- system.DBUser.deleteAttribute
- system.DBPlayer.setAttribute
- system.DBPlayer.getAttribute
- system.DBPlayer.getAttributeNames
- system.DBPlayer.deleteAttribute

\*changeMovie is only documented in the Release Notes file for the SMUS 3 server

\*\*these commands are not listed in the command list summary for the SMUS 3 manual but appear in other sections of the document
