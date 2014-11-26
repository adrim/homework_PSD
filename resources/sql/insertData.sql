insert into AccessRightTable (RightName) values
	('NONE'),
	('READ'),
	('WRITE'),
	('READ_WRITE');
insert into UserTable(UserName, Password) values
	('root',  'student'),
	('alice', 'alice'),
	('bob',   'bob'),
	('mary',  'mary'),
	('lambda','math'),
	('delta', 'math');
insert into RoleTable(roleName) values
	('OWNER'),
	('READER'),
	('WRITER'),
	('DEFAULT'),
	('ALL_ACCESS');