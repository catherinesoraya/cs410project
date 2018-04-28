CREATE DATABASE IF NOT EXISTS TaskMan;
USE TaskMan;
			
CREATE TABLE IF NOT EXISTS Task (
	taskId int NOT NULL AUTO_INCREMENT, 
	label VARCHAR(64) NOT NULL, 
	creationDate DATE, 
	dueDate DATE, 
	tag VARCHAR(128), 
	status VARCHAR(20), 
	PRIMARY KEY (taskId)
); 

