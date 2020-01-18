# Getting Started

### Development environment installation

* Install the following tools
   - IntelliJ
   - Java 11.x
   - Gradle 6.x
   - Dockerk
* Run MySQL in Docker
   - docker pull mysql
   - docker run --name mysql-spexregister -p 3306:3306 -v <absolute path>/.mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=root -d mysql:latest
       * The directory .mysql must not exist the first time
   - docker exec -i mysql-spexregister sh -c 'exec mysql -uroot -p"root"' < <absolute path>/src/main//create-database.sql
* Run Solr in Docker
   - docker pull solr
   - docker run --name solr-spexregister -p 8983:8983 -t solr
