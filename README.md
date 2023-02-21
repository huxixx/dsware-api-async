# PLEASE READ THIS DOC!

## SETUP - REQUIRED!
**You need to setup your project a little bit before it works.** <br>
Step 1. Change groupId and archiveId in pom.xml to your project ones.<br>
Step 2. Head to the maven-jar-plugin section and modify the "mainClass" to match your main class.<br>
Step 3. Next to the "make file" and "make folder" buttons at the top of the file list, click the three dots. then, click "Show Hidden Files" if you havent already.<br>
Step 4. Open your `.replit` file.<br>
Step 5. at the top, change the ***last argument*** of the "run" variable to match this: ```target/ARTIFACTID-VERSION.jar```<br>
Make sure to change version and artifactId to the same ones in pom.xml.

## TO ADD DEPENDENCIES
**in order to use maven, do <i>not</i> use the repl.it packager. use the pom.xml.** <br>
Step 1. Add dependency/repository to pom.<br>
Step 2. Run project to import.<br>
Step 3. Done!

## TO RUN PROJECT
Just do what the "setup" section says and hit run!