
# Ambulare

### Demo

https://www.youtube.com/watch?v=2CYq9vNzst4

### Overview

Ambulare is a fun little side-scrolling engine that I built from scratch on top of LWJGL. The demo above shows off most of the capabilities, including a custom physics engine, a sound system, lighting systems, animations, and menus. All of the textures and sounds are homemade as well.

One capability not shown in the demo above is the custom readable serialization format I created in order to allow the idea of "stories". A story consists of multiple interconnected levels, such as Dank Cellar and Galen Forest shown in the demo above. By understanding the simple serialization format read in by the program, entire stories consisting of custom levels, blocks, textures, backgrounds, foliage, etc. can be created without touching any code at all.

In an alternate universe where I had unlimited time I would extend this project by adding many more levels, implementing enemies and combat, inventory and equipment systems, game saving/loading, and some sort of journal/quest system - all of which would be fully customizable for any aspiring custom story creator. I would also extend the soundscape to include homemade music and ambient noises.

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
