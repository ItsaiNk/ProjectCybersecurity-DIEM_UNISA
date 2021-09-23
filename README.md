# ProjectCybersecurity-DIEM_UNISA

This repository contains the proposed solution for the assigned final project for the course of Cybersecurity, master degree in Computer Engineering @ Università degli Studi di Salerno.

- [README](#ProjectCybersecurity-DIEM_UNISA)
- [About the project](#About-the-project)
- [Before starting](#Before-starting)
- [Launch the simulation](#Launch-the-simulation)
- [How the simulation works](#How-the-simulation-works)
- [Project tree](#Project-tree)

# About the project

The project concerns the simulation of a contact tracing application, in order to limit the spread of the SARS-COV2 virus.

# Before starting


Before starting, you need to generate some certificates necessary for operation. The project simulates connections to servers using the TLS protocol.

``` bash
keytool -genkeypair -alias INSERT_ALIAS -keyalg EC -keysize 256 -sigalg SHA256withECDSA  -validity 365 -storetype JKS -keystore ct_keystore.jks -storepass INSERT_PASS
``` 

``` bash
keytool -export -alias INSERT_ALIAS -storepass INSERT_PASS -file ct_server.cer -keystore ct_keystore.jks
```

``` bash
keytool -import -v -trustcacerts -alias INSERT_ALIAS -file ct_server.cer -keystore user_truststore.jks -keypass INSERT_PASS -storepass INSERT_PASS
``` 

``` bash
keytool -import -v -trustcacerts -alias INSERT_ALIAS -file ct_server.cer -keystore ha_truststore.jks -keypass INSERT_PASS -storepass INSERT_PASS
``` 

``` bash
keytool -genkeypair -alias INSERT_ALIAS -keyalg EC -keysize 256 -sigalg SHA256withECDSA  -validity 365 -storetype JKS -keystore ha_keystore.jks -storepass INSERT_PASS
``` 

``` bash
keytool -export -alias INSERT_ALIAS -storepass INSERT_PASS -file ha_server.cer -keystore ha_keystore.jks
``` 

``` bash
keytool -import -v -trustcacerts -alias INSERT_ALIAS -file ha_server.cer -keystore ct_truststore.jks -keypass INSERT_PASS -storepass INSERT_PASS
``` 

# Launch the simulation

In order to launch the simulation, you have to:
- insert the password chosen for the certificate in the HAServer.java files, line 12;
- compile all the java files;
- run these following two commands, in two separated console windows:

``` bash
java -Djavax.net.ssl.keyStore=ct_keystore.jks -Djavax.net.ssl.keyStorePassword=INSERT_PASS -Djavax.net.ssl.trustStore=ct_truststore.jks -Djavax.net.ssl.trustStorePassword=INSERT_PASS CTServer
```

``` bash
java -Djavax.net.ssl.trustStore=user_truststore.jks -Djavax.net.ssl.trustStorePassword=INSERT_PASS Simulation
```

# How the simulation works

To simulate the passage of time, in the Simulation class there is the variable t that
indicates the time expressed in minutes. Therefore, the execution cycle is divided into periods of one minute.

It is possible to set some parameters of the simulation in the Simulation.java file:

- RISK_CONTACT_TIME (default: 15): indicates the threshold, expressed in minutes.If a person is in contact with an infected person for more than the threshold value, a risk notification is generated;
- BLE_MAX_DISTANCE (default: 10): indicates the range of the BLE, expressed in meters;
- BLE_THRESHOLD (default: 2,5): indicates the threshold distance expressed in meters
within which the contact has to be considered as at risk;
- ID_GENERATION_PERIOD (default: 2): indicates the generation period of the ID expressed in minutes;
- RISK_INFECTION (default: 0.3): indicates the probability of contagion between 0 and 1;
- NUM_SMARTPHONE (default: 40): indicates the number of smartphones that has to be inserted in the simulation;
- SIM_TIME (default: 30 * DAY): indicates the simulation time, expressed in days.

The simulation creates a number of smartphones equal to NUM_SMARTPHONE with a random position within the space. Any smartphone for simplicity receives the list of all smartphones in the simulation. The simulation is scanned a 1-minute periods in which the simulateRoutine() method of the class is invoked Smartphone, for each smartphone of the simulation, which operates as follows:

- every day removes from the contact list those that occurred further than 20 days;
- every two days it re-initializes the seeds of the PRGs for the generation of the IDs;
- if the user is not infected:
    - every day it generates the string D;
    - every day removes generated IDs older than 14 days;
    - every day synchronizes with the server to download the list of positive IDs,
if the exposure notification is generated (indicated with the variable
needSwab). The procedure is started with the swabProcedure() method. It is possible that a smartphone will try to cheat the CT system to receive a free swab, with a probability of 1%;
    - every two minutes it generates the string Q;
    - every two minutes generate a new ID with the generateID() method;
    - every minute, for each smartphone in the aforementioned list, calculates the
real distance that separates the user from it. If the distance is less than
BLE_MAX_DISTANCE, the user ID is sent to that smartphone
via the receiveIDFromBLE () method;
    - every minute moves the user randomly with the randomMove() method of Position class;
- if the user is infected:
    - sends the list of its IDs to the CT system through the sendIDsToCT() method;
    - every 7 days, perform the swab procedure.

# Project tree

```
ProjectCybersecurity-DIEM_UNISA
├─ README.md
└─ src
   ├─ ByteArrayWrapper.java
   ├─ ClientUtils.java
   ├─ ContactTime.java
   ├─ CTServer.java
   ├─ HAServer.java
   ├─ Position.java
   ├─ Server.java
   ├─ Simulation.java
   ├─ Smartphone.java
   └─ Utils.java

```