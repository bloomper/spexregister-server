# Getting Started

### Development environment installation

* Install the following tools
   - IntelliJ
   - Java 11.x
   - Gradle 6.x
   - Docker
* Run MySQL in Docker
   - docker pull mysql
   - docker run --name mysql-spexregister -p 3306:3306 -v <absolute path>/.mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=root -d mysql:latest
       * The directory .mysql must not exist the first time
   - docker exec -i mysql-spexregister sh -c 'exec mysql -uroot -p"root"' < <absolute path>/src/main//create-database.sql
* Run Elasticsearch in Docker
   - docker pull elasticsearch
   - docker run --name elasticsearch-spexregister -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.5.1
