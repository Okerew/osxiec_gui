A gui for osxiec https://github.com/Okerew/osxiec
<br>
It requires java
<br>
When executed a command make sure to close the process. 
![Screenshot 2024-07-24 at 12 05 58](https://github.com/user-attachments/assets/42d858e1-e4fd-4a82-b2e8-f86a7c35be38)

### Build java gui
Git clone the gui
```sh
git clone https://github.com/Okerew/osxiec_gui.git
```

Go to the directory
```sh
cd osxiec_gui
```
Build the class
```sh
javac OsxiecApp.java
```
Build the jar
```sh
jar -cvfe osxiec.jar OsxiecApp OsxiecApp.class
```

Copy jar into app bundle, remove the previous one
```sh
cp osxiec.jar osxiec.app/Contents/Resources
```
If using the one from release, delete the previous one

Copy the osxiec icon into Contents/Resources

Finally, copy the run_app_bundle.sh into the bundle as osxiec_gui
```sh
cp run_app_bundle.sh osxiec.app/Contents/MacOS/osxiec_gui
```
