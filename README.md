# Chat Room
A multi-threaded client-server chat application with text messages and file transfer. 
client connects to a listening socket on the specified server. 
The message sent in 3 ways 
1. publicly to all clients on server 
2. privately to specified user
3. exclude individual users 

The server listens through socket on the specified port. 
As it receives requests, it spawns a new "Threads".
This became an issue due to aws server only supporting 1 core and 1 thread with free version

Running the Application 
1 - I have provide the keys to my server as well as the destination if desired to run on cloud. You may also run it locally through "localhost"
2 - The server hosts the Server.java file just enter java Server into command prompt if not already running
3 - The Client is run locally and user must enter a user name into program arguments before a connection can be made

I have also attached images with configurations/properties/results of this project. 


