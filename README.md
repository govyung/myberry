# MyBerry Project

[![Build Status](https://travis-ci.com/govyung/myberry.svg?branch=master)](https://travis-ci.com/govyung/myberry)
[![codecov](https://codecov.io/gh/govyung/myberry/branch/master/graph/badge.svg)](https://codecov.io/gh/govyung/myberry)
![license](https://img.shields.io/github/license/govyung/myberry)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/govyung/myberry.svg)](http://isitmaintained.com/project/govyung/myberry "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/govyung/myberry.svg)](http://isitmaintained.com/project/govyung/myberry "Percentage of issues still open")

MyBerry is a distributed ID construction engine. You can customize the ID production rules as you
like.

## Official Site

[myberry.org](https://myberry.org)

## Features

* High extensibility
* Guaranteed unique
* Orchestration

## Building

Only supports Java 1.8

```bash
# mvn -Prelease-all -DskipTests clean install -U
```

### Maven dependency

```xml
<dependency>
	<groupId>org.myberry</groupId>
	<artifactId>myberry-client</artifactId>
	<version>1.1.0</version>
</dependency>
```

## License

MyBerry is under the MIT License. See the [LICENSE](https://myberry.org/license) file for details.
