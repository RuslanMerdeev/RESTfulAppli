--liquibase formatted sql

--changeset ruslan.merdeev@mail.ru:create_table_city
create table city (
  id int not null auto_increment primary key,
  name varchar(50) not null,
  latitude float(8,4) not null,
  longitude float(9,4) not null
);
--rollback drop table city;

--changeset ruslan.merdeev@mail.ru:insert_city_togliatti
insert into city (name, latitude, longitude) values ('Togliatti', 53.5303, 49.3461);
--rollback delete from city where name='Togliatti';

--changeset ruslan.merdeev@mail.ru:insert_city_moscow
insert into city (name, latitude, longitude) values ('Moscow', 55.7522, 37.6156);
--rollback delete from city where name='Moscow';

--changeset ruslan.merdeev@mail.ru:insert_city_new_york
insert into city (name, latitude, longitude) values ('New-York', 40.7143, -74.0060);
--rollback delete from city where name='New-York';

--changeset ruslan.merdeev@mail.ru:create_distance
create table distance (
  id int not null auto_increment primary key,
  from_city varchar(50) not null,
  to_city varchar(50) not null,
  value float(7,1) not null
);
--rollback drop table distance;

--changeset ruslan.merdeev@mail.ru:insert_distance_togliatti_moscow
insert into distance (from_city, to_city, value) values ('Togliatti', 'Moscow', 800.0);
--rollback delete from distance where from_city='Togliatti' && to_city='Moscow';

--changeset ruslan.merdeev@mail.ru:insert_distance_togliatti_new_york
insert into distance (from_city, to_city, value) values ('Togliatti', 'New-York', 8250.0);
--rollback delete from distance where from_city='Togliatti' && to_city='New-York';

--changeset ruslan.merdeev@mail.ru:insert_distance_moscow_new_york
insert into distance (from_city, to_city, value) values ('Moscow', 'New-York', 7530.0);
--rollback delete from distance where from_city='Moscow' && to_city='New-York';

