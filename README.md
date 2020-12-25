[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# Kotlin plugin for NetBeans IDE

The [original Netbeans Kotlin plugin](https://github.com/JetBrains/kotlin-netbeans) is no longer actively developed. See https://github.com/JetBrains/kotlin-netbeans/issues/122 for more information.

I tried to install the above on Netbeans 12.2 (running on Java 11) but it exited with a NullpointerException. I tried to build it but had to learn that it needs some dependencies I could not find.

Luckily I found a [fork of the above by the Github user mario-s - Thank you!](https://github.com/mario-s/kotlin-netbeans). He has solved the build problem but the NullpointerException remained.

I forked his work again to get it working.

## Branches
I removed the a existing branches except master. Further I added

- fix_nullpointer: Fix the NullpointerException but do not update dependencies.

## Build
The build works only on Java 8 due to dependencies to sun.misc.Unsafe. I prefer to

1. Download a JDK of version 8.x at https://adoptopenjdk.net/ as archive file and unpack it

2. Set JAVA_HOME to the path of the JDK

    export JAVA_HOME=/the/path/to/the/jdk

3. Build with /path/to/maven/bin/mvn clean package

4. I got an error when I run /path/to/maven/bin/mvn clean test

## Installing Kotlin plugin

### NetBeans Update Center

The plugin could be installed via NetBeans Update Center.

### Manual installation

1. Download the latest release: [0.2.0.1](https://github.com/JetBrains/kotlin-netbeans/releases/tag/v0.2.0.1)
2. Launch NetBeans IDE
3. Choose **Tools** and then **Plugins** from the main menu
4. Switch to **Downloaded** tab
5. On the **Downloaded** tab click **Add Plugins...** button
6. In the file chooser, navigate to the folder with downloaded plugin. Select the NBM file and click OK. The plugin will show up in the list of plugins to be installed.
7. Click **Install** button in the Plugins dialog
8. Complete the installation wizard by clicking **Next**, agreeing to the license terms and clicking **Install** button.


## Plugin feature set

1. Syntax highlighting
2. Semantics highlighting
3. Diagnostics
4. Code completion
5. Navigation in Source Code
6. Quick fixes
7. Intentions and Inspections
8. Occurrences finder
9. Code folding 
10. Unit testing support
11. Ant, Maven and Gradle support
12. Navigation by Kotlin class name
13. Debugging support
