# Build

* cd importservice &&  mvn clean compile install
* cd ../importworker && mvn clean compile install

# Run locally

* cd importservice && mvn clean spring-boot:run
* cd importworker && mvn exec:java

# Run dockerized

* [Build Service] (see above)
* [Build Worker] cd ../importworker && mvn clean compile assembly:single
* docker-compose build && docker-compose up
