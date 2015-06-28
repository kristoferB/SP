#Sequence Planner

##Setup
First, install nodejs according to instructions on the web. Then init the GUI dev environment by executing
```
cd gui/web  
npm install  
npm install -g yo //The yeoman dev environment. Run as root if you're on a Unix OS.
bower install //Fetch of web app dependencies.
npm install grunt-connect-proxy --save-dev
```

##Run
###Backend
Open a command window and set location to the project root folder (SP/). Then launch SP backend with  
```
sbt launch/run
```
###Frontend
Open another command window and set location to the web folder (SP/gui/web/). Then launch the GUI with  
```
grunt serve
```
