create table UserTable(
	UserID   integer not null auto_increment,
	UserName varchar(20) not null,
	Password varchar(10) not null,
	primary key(UserID),
	unique key(UserName)
) engine=innodb;

create table RoleTable(
	RoleID   integer not null auto_increment,
	RoleName varchar(20) not null,
	
	unique key(RoleName),
	primary key(RoleID)
) engine=innodb;

create table UserRoleTable(
	UserID integer not null,
	RoleID integer not null,
	
	primary key(UserID, RoleID),
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
	
	unique key(RightName),
	primary key(RightID)
) engine=innodb;

create table ResourceTable(
	ResourceID integer not null auto_increment,
	ResourceName varchar(256) not null ,
	
	unique key(ResourceName),
	primary key(ResourceID)
) engine=innodb;

create table ResourceACLTable(
	ResourceID  integer not null,
	RoleID		integer not null,
	RightID		integer not null,
	primary key(ResourceID, RoleID, RightID),
	foreign key(ResourceID)
		references ResourceTable(ResourceID)
		on delete cascade
		on update cascade,
	foreign key(RoleID)
		references RoleTable(RoleID)
		on delete cascade
		on update cascade,
	foreign key(RightID)
		references AccessRightTable(RightID)
		on delete cascade
		on update cascade
) engine=innodb;
