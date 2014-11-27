delimiter //

create procedure addRights(
	in paramResourceName varchar(256),
	in paramRoleName 	 varchar(20),
	in paramRightName    varchar(20)
)
myLabel:begin
	declare procResID int;
	
	if (paramRightName = 'NONE') then
		delete from ResourceACLTable
		where RoleID = 
			  (select RoleID from RoleTable where RoleName = paramRoleName);
		
		leave myLabel;
	end if;
	
	insert into ResourceTable (ResourceName) values (paramResourceName)
		on duplicate key update ResourceID=last_insert_id(ResourceID), ResourceName=paramResourceName;
	
	select last_insert_id() into procResID;
		
	insert into ResourceACLTable values
		(procResID, (select RoleID from RoleTable where RoleName = paramRoleName),
		(select RightID from AccessRightTable where RightName = paramRightName));

end//
delimiter ;
