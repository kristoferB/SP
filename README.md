#Sequence Planner 2
Sequence Planner (SP) is a micro service architecture for modeling and analyzing automation system. 
Initially, the focus was on supporting engineers in developing control
code for programmable logical controllers (PLCs). During the first years, 
algorithms to handle product and au- tomation system interaction, 
and to visualize complex operation sequences using multiple projections, 
was developed. Over the years, other use cases have been integrated, 
like formal verification and synthesis using Supremica, restart support, 
cycle time optimization, energy optimization and hybrid systems, 
online monitoring and control (the tweeting factory), 
as well as emergency department online planning support. 
 
##Frontend
check gui/README.md for instructions 

##Backend
###Setup
Download and install Simple Build Tool (SBT). Files and instructions for your platform are available at the project website, http://www.scala-sbt.org/.

###Run
SP is a set of micro services that communicates via json messages in an akka cluster. 

You have to start each service or group of services in a seperate terminal

Open a terminal and set location to the project root folder (SP/). Then launch SP core with  
```
sbt spcore/run
```
SPCore include the web-server, support services as well as some core services to handle models and key functionality

The open a new command window and set location to the project root folder (SP/). Then launch various services with
```
sbt labkit/run
sbt ModelTest/run
...
```
The service are locate in spservices

