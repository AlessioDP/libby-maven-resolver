# Libby Maven Resolver
An utility and wrapper of `maven-resolver-supplier` and `maven-resolver-impl` used to download transitive dependencies for [Libby](https://github.com/AlessioDP/libby).

## Maven
```xml
<repository>
  <id>maven-snapshots</id>
  <url>https://s01.oss.sonatype.org/content/repositories/public/</url>
</repository>

<dependency>
    <groupId>com.alessiodp.libby.maven.resolver</groupId>
    <artifactId>libby-maven-resolver</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Gradle
```groovy
repositories {
    maven {
        url 'https://s01.oss.sonatype.org/content/repositories/public/'
    }
}
dependencies {
    implementation 'com.alessiodp.libby.maven.resolver:libby-maven-resolver:1.0.0'
}
```