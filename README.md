eXo platform Pulse
=====================

Introduce
---------------
"exoplatform-pulse" that contains Activity Statistics Gadgets:
* Top Forum Participant gadget: Show Top Users of Forum Participant
* Activity Statistics gadget: Display eXo Platform statisic. They are: New Users, Login Count, Forum Active Users, New Forum Post, New users social connections, New posts on the activities stream, Total Platform Download.

Compatibility:
---------------
From eXo Platform 4.1

ScreenShot
---------------
* Top Forum Participant:<br>
<img src="resource/ScreenShots/TopForumParticipant.png" alt="Top Forum Participant">
<br>
* Activity Statistics (Maximum view):<br>
<img src="resource/ScreenShots/ActivityStatistics.png" alt="Activity Statistics maximum view">
<br>
* Activity Statistics (Minimum view):<br>
<img src="resource/ScreenShots/ActivitiyStatisticsMini.png" alt="Activity Statistics minimum view">
<br>
* Activity Statistics (Export data view):
<img src="resource/ScreenShots/ActivitiyStatisticsMini.png" alt="Activity Statistics: Export data view">
<br>

Build
---------------
Simply build it with :

	mvn clean install


Deploy to eXo
---------------
After build with this add-on:
* unzip exoplatform-pulse/bundle/target/exoplatform-pulse-bundle-1.0.x-SNAPSHOT.zip
* copy exoplatform-pulse/bundle/target/exoplatform-pulse-bundle-1.0.x-SNAPSHOT.zip/exoplatform-pulse-webapp.war to $Platform-Tomcat/webapps/
copy exoplatform-pulse/bundle/target/exoplatform-pulse-bundle-1.0.x-SNAPSHOT.zip/exoplatform-pulse-lib-1.0.x-SNAPSHOT.jar and exoplatform-pulse/bundle/target/exoplatform-pulse-bundle-1.0.x-SNAPSHOT.zip/exoplatform-pulse-config-1.0.x-SNAPSHOT.jar to $Platform-Tomcat/lib/
* Start tomcat. Done.

Deploy via addon-manager
---------------
run command ./addon install exo-pulse:1.0.x-SNAPSHOT 
