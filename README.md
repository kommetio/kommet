# Kommet
Kommet is a platform for building and running Web applications.

Complete documentation and guides can be found at https://kommet.io

It is distributed under the dual license: AGPL and proprietary.

## Building

Kommet requires JDK 25 to compile and run. Use Maven 3.9 or newer with JDK 25
selected as `JAVA_HOME`, then run:

```shell
mvn clean test-compile
mvn test
```

Deploy the WAR to a Jakarta Servlet 6.0 container, such as Apache Tomcat 10.1.
