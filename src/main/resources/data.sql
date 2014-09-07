insert into user(id,email,password,first_name,last_name) values (0,'rob@example.com','password','Rob','Winch');
insert into user(id,email,password,first_name,last_name) values (1,'luke@example.com','password','Luke','Taylor');

insert into message(id,created,to_id,from_id,summary,text) values (100,'2014-07-10 10:00:00',0,1,'Hello Rob','This message is for Rob');
insert into message(id,created,to_id,from_id,summary,text) values (101,'2014-07-10 14:00:00',0,1,'How are you Rob?','This message is for Rob');
insert into message(id,created,to_id,from_id,summary,text) values (102,'2014-07-11 22:00:00',0,1,'Is this secure?','This message is for Rob');

insert into message(id,created,to_id,from_id,summary,text) values (110,'2014-07-12 10:00:00',1,0,'Hello Luke','This message is for Luke');
insert into message(id,created,to_id,from_id,summary,text) values (111,'2014-07-12 10:00:00',1,0,'Greetings Luke','This message is for Luke');
insert into message(id,created,to_id,from_id,summary,text) values (112,'2014-07-12 10:00:00',1,0,'Is this secure?','This message is for Luke');
