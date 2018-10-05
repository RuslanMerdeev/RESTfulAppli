Additional settings:
1. In <wildfly_root>\my.ini change parameter value to max_allowed_packet=32M or higher;

Application deploying:
1. Start server in standalone mode using standalone.bat in <wildfly_root>\bin;
2. Copy RESTfulAppli.war from <RESTfulAppli_root>\target(or from <RESTfulAppli_root>\docs) to <wildfly_root>\standalone\deployments;
FYI: application use mysql-connector-java-8.0.12.jar driver;

Initiate database:
1. Tag current state of database using tag.bat in <RESTfulAppli_root>\liquibase\db;
2. Update database using update.bat in <RESTfulAppli_root>\liquibase\db;
FYI: database should be previously created and named "distance_calculator";

Upload data to database:
1. Build cities.xml and distances.xml using run.bat in <RESTfulAppli_root>\xml_builder
2. Use cities.xml from <RESTfulAppli_root>\xml_builder\cities and distances.xml from <RESTfulAppli_root>\xml_builder\distances;
FYI: <python_root> should be previously changed to proper value in run.bat;