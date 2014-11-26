create table UserTable(
	UserID   integer not null,
	UserName varchar(20) not null,
	Password varchar(10) not null,
	primary key(UserID),
	unique key(UserName)
) engine=innodb;

create table RoleTable(
	RoleID   integer not null auto_increment,
	RoleName varchar(20) not null,
	
	primary key(RoleID)
) engine=innodb;

create table UserRoleTable(
	UserID integer,
	RoleID integer,
	
	foreign key(UserID)
		references UserTable(UserID)
		on delete cascade,
	foreign key(RoleID)
		references RoleTable(RoleID)
		on delete cascade
) engine=innodb;

create table AccessRightTable(
	RightID integer not null auto_increment,
	RightName varchar(20) not null,
	
	primary key(RightID)
) engine=innodb;

create table ResourceTable(
	ResourceID integer not null auto_increment,
	ResourceName varchar(20) not null,
	
	primary key(ResourceID)
) engine=innodb;


create table RoleAccessTable(
	RoleID 	integer,
	RightID integer,
	
	foreign key(RoleID)
		references RoleTable(RoleID)
		on delete cascade
		on update cascade,
	foreign key(RightID)
		references AccessRightTable(RightID)
		on delete cascade
) engine=innodb;

create table ResourceACLTable(
	ResourceID  integer,
	RoleID		integer,
	RightID		integer,
	foreign key(ResourceID)
		references ResourceTable(ResourceID)
		on delete cascade,
	foreign key(RoleID)
		references RoleTable(RoleID)
		on delete cascade,
	foreign key(RightID)
		references AccessRightTable(RightID)
		on delete cascade
) engine=innodb;

create trigger DefRightsInsert after insert on RoleTable
for each row
begin
	insert into RoleAccessTable(RoleID, RightID)
		select new.RoleID, RightID
		from (select RightID 
			  from AccessRightTable
			  where RightName = 'NONE')
end
create trigger DefRightsUpdate after update on RoleTable
for each row
begin
	insert into RoleAccessTable(RoleID, RightID)
		select new.RoleID, RightID
		from (select RightID 
			  from AccessRightTable
			  where RightName = 'NONE')
end 