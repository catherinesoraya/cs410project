CS410 
Francesca Spezzano
Daniel Chavez, Soraya Yazdanpour
4-27-18


Our implementation basically amounts to a scanner based shell that parses user input for specified
comma separated commands e.g. (active, due soon). It then uses those to build sql strings to query the databse.

The database is connected to using a sandbox login and boise state login. 

The program asks for a kerberos login but this is arbitrary, you can just hit enter twice and the 
connection should be successful. 

Usage: java -jar TaskDestroyer.jar <BroncoUserid> <BroncoPassword> <sandboxUSerID> <sandbox password> <yourportnumber