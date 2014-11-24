insert into AccessRightTable (RightName) values
	('NONE'),
	('READ'),
	('WRITE');

insert into UserTable values
	(0, 'admin', md5('admin')),
	(1, 'alice', md5('alice')),
	(2, 'bob', 	 md5('bob')),
	(3, 'mary',  md5('mary')),
	(4, 'lambda',md5('math')),
	(5, 'delta', md5('math'));
insert into UserTable values
	(0, 'admin', 'admin'),
	(1, 'alice', 'alice'),
	(2, 'bob', 	 'bob'),
	(3, 'mary',  'mary'),
	(4, 'lambda','math'),
	(5, 'delta', 'math');	
insert into RoleTable(roleName) values
	('OWNER'),
	('READER'),
	('WRITER'),
	('DEFAULT'),
	('ALL_ACCESS');


insert into RoleAccessTable 
	select (RoleID, RightID)
	from RoleTable roles, AccessRightTable access
	where roles.RoleName = 'OWNER' and access.RightName = 'READ';
insert into RoleAccessTable 
	select (RoleID, RightID)
	from RoleTable roles, AccessRightTable access
	where roles.RoleName = 'OWNER' and access.RightName = 'WRITE';
insert into RoleAccessTable 
	select (RoleID, RightID)
	from RoleTable roles, AccessRightTable access
	where roles.RoleName = 'READER' and access.RightName = 'READ';
insert into RoleAccessTable 
	select (RoleID, RightID)
	from RoleTable roles, AccessRightTable access
	where roles.RoleName = 'WRITER' and access.RightName = 'WRITE';
insert into RoleAccessTable 
	select (RoleID, RightID)
	from RoleTable roles, AccessRightTable access
	where roles.RoleName = 'DEFAULT' and access.RightName = 'NONE';
insert into RoleAccessTable 
	select (RoleID, RightID)
	from RoleTable roles, AccessRightTable access
	where roles.RoleName = 'ALL_ACCESS' and access.RightName = 'READ';
insert into RoleAccessTable 
	select (RoleID, RightID)
	from RoleTable roles, AccessRightTable access
	where roles.RoleName = 'ALL_ACCESS' and access.RightName = 'WRITE';