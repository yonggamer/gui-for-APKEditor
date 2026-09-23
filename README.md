# APKEditor GUI wrapper

This is a Java Swing wrapper around the APKEditor CLI. It supports Build
(`b`), Decompile (`d`), Info (`info`), Merge (`m`), Protect (`p`), and Refactor
(`x`). Commands are launched with the selected external APKEditor jar, and
command output is displayed in the output area.

## Build

Requires Java 8 or newer. A JDK is needed to build the project. Building the
wrapper does not require an APKEditor jar, but running a command in the GUI
does require one downloaded separately.

```sh
./build.sh
```

The result is `output/APKEditor-GUI.jar`. The GUI wrapper does not include an
APKEditor jar. You must download one separately from
<https://github.com/REAndroid/APKEditor/releases>. `example/APKEditor-1.4.9.jar`
is provided only as an example and is not copied automatically. Select the
separately downloaded APKEditor jar in the GUI before running a command.

For example:

```sh
java -jar output/APKEditor-GUI.jar
```

After the GUI opens, choose the separately downloaded `APKEditor*.jar` in the
`APKEditor JAR` field. If one is in the same folder as the wrapper, it is
detected automatically at startup.

The selected external file must be named `APKEditor*.jar`.

## Run

```sh
java -jar output/APKEditor-GUI.jar
```

Run this command on a desktop session. A remote Codespace or SSH terminal without
an X11/Wayland display cannot show a Swing window.

The interface uses Java's system look-and-feel, which gives native controls on
each supported desktop OS while keeping the application in one OS-independent JAR.