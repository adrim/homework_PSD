create user 'admin'@'localhost' identified by 'student';
grant all on *.* to 'admin'@'localhost';
mysql=g:\Installed\wamp\bin\mysql\mysql5.6.17\bin\mysql.exe
mysql -u admin -p "student" ;
create database if not exists psd1;
use psd;
source sql/createTables.sql;
source sql/insertData.sql;

g:\Installed\wamp\bin\mysql\mysql5.6.17\bin\mysql.exe -u "admin" -p "student" ;
g:\Installed\wamp\bin\mysql\mysql5.6.17\bin\mysql.exe -u root;
