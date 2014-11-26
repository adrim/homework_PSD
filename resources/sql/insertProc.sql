delimiter //

create procedure addRights(
	in paramResourceName varchar(256),
	in paramRoleName 	 varchar(20),
	in paramRightName    varchar(20)
)
begin
	declare procResID int;
	insert into ResourceTable (ResourceName) values (paramResourceName)
		on duplicate key update id=last_insert_id(id), ResourceName=paramResourceName;
	
	select last_insert_id() into procResID;
		
	insert into ResourceACLTable values
		(procResID, (select RoleID from RoleTable where RoleName = paramRoleName),
		(select RightID from AccessRightTable where RightName = paramRightName));

end//
delimiter ;
