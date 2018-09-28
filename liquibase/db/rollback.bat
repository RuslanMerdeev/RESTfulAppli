cd ../

liquibase ^
--changeLogFile=db/changelog/db.changelog-master.xml ^
--url="jdbc:mysql://localhost:3306/distance_calculator?useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC" ^
--username=root ^
--password=root ^
rollback testy

pause