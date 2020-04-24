
# Ambulare

### Overview

Ambulare is a side-scrolling fantasy RPG. It runs in the JVM, and relies on LWJGL.

### Building

I use Maven to build the project and IntelliJ to develop it. The pom.xml file with all the necessary libraries listed is included in the repository. Simply import this repo into IntelliJ and make sure the natives in the pom.xml properties matches your system, and it should work right off the bat.

If you're running on MacOS, use the following JVM argument or you will probably crash:
```
-XstartOnFirstThread
```

### Other Notes

If you experience screen tearing on MacOS despite V-Sync being enabled, run the following console command:
```
defaults write -g NSRequiresAquaSystemAppearance -bool No
```
I have no idea how but this will magically fix it.
