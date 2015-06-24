#Sequence Planner

##In Production
Launch SP with
```
sbt launch/run
```

##During Development
Launch SP in manual restart mode
```
sbt  
project launch  
re-start
```

Stop SP with  
```
re-stop
```

Or launch SP in restart mode that triggers on code change with  
```
sbt  
project launch  
~re-start
```

Stop SP with
```
~re-stop
```
