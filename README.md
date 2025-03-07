# th2 Gradle

This project contains Gradle plugins that simplifies development of th2 components.

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/com.exactpro.th2.gradle.base)](https://plugins.gradle.org/search?term=com.exactpro.th2.gradle)

## Available plugins

The following plugins are available:

- [_com.exactpro.th2.gradle.base_](#comexactproth2gradlebase) - adds th2 Maven BOM into dependencies and adds required plugins to the project
- [_com.exactpro.th2.gradle.grpc_](#comexactproth2gradlegrpc) - configures gRPC build for the project and add minimal set of required dependencies to create a th2 gRPC library
- [_com.exactpro.th2.gradle.publish_](#comexactproth2gradlepublish) - configures publication tasks for projects
- [_com.exactpro.th2.gradle.component_](#comexactproth2gradlecomponent) - prepares project to be built as Docker image

### com.exactpro.th2.gradle.base

This plugin **MUST** be applied to the root project.

It adds required configuration to the project to be ready for vulnerability scanning and building license report.
Also, it adds useful information from git into the JAR file.
If one of Java plugins is applied it also add maven BOM into dependencies.

To provide API key to access NVD vulnerability database use property `nvdApiKey`.
To provide delay in milliseconds between attempts to connect to NVD database use property `nvdDelay`.

Third-party plugins applied under the hood:

- [com.github.jk1.dependency-license-report](https://github.com/jk1/Gradle-License-Report)
- [com.gorylenko.gradle-git-properties](https://github.com/n0mer/gradle-git-properties)
- [org.owasp.dependencycheck](https://github.com/dependency-check/dependency-check-gradle)

#### Configuration properties

* `th2.licence.allow-licence-url` - provides a URL to download a file with allowed licenses for the `com.github.jk1.dependency-license-report` plugin.<br>
  _*NOTE: This property should be used only for debugging the file's content with allowed licenses.*_<br>
  By default, the file is downloaded from <https://raw.githubusercontent.com/th2-net/.github/main/license-compliance/gradle-license-report/allowed-licenses.json> URL.
* `th2.licence.license-normalizer-bundle-path` - provides a path to a file with license normalizer bundle for the `com.github.jk1.dependency-license-report` plugin.<br>
  _*NOTE: This property should only be used for debugging content of the license normalizer bundle.*_<br>
  By default, the `base` plugin downloads the file from <https://raw.githubusercontent.com/th2-net/.github/main/license-compliance/gradle-license-report/license-normalizer-bundle.json> URL and stores it in the project's build directory.

### com.exactpro.th2.gradle.grpc

This plugin prepares the project to be built as gRPC library for th2 component.
If this plugin is applied to the root project it also applies [base plugin](#comexactproth2gradlebase).
The `java-library` plugin **MUST** be also applied to the project with grpc plugin.
Plugin can be either configured via `th2Grpc` extension.

Plugin configures protobuf plugin to generate java stubs from `src/main/proto` source set.

The minimal set of required dependencies to protobuf and gRPC libraries is added automatically.

Third-party plugins applied under the hood:

- [com.google.protobuf](https://github.com/google/protobuf-gradle-plugin)

### com.exactpro.th2.gradle.publish

This plugin **MUST** be applied to the root project.

Plugin prepares the project to be published to maven repository.
It will prepare the project only if `maven-publish` plugin is applied to that project.
Plugin can be either configured via `th2Publish` extension or via properties.

Supported properties:

- Sonatype (only when both properties specified the project will be configured to publish to sonatype):
  - sonatypeUsername
  - sonatypePassword
- Nexus:
  - nexus_url
  - nexus_user
  - nexus_password
- Signature:
  - signingKey
  - signingPassword

Third-party plugins applied under the hood:

- [io.github.gradle-nexus.publish-plugin](https://github.com/gradle-nexus/publish-plugin)

### com.exactpro.th2.gradle.component

This plugin prepares the project to be packaged as Docker image.
The project **MUST** use `application` plugin to package the component.

## Apply from Sonatype repository

Until this plugin is not published to Gradle portal it can be applied from Sonatype repository.

### Apply plugins

Example:

```groovy
plugins {
  id 'java-library'
  id 'maven-publish'
  id 'com.exactpro.th2.gradle.grpc' version '<version>'
  id 'com.exactpro.th2.gradle.publish' version '<version>'
}
```


## License

This plugin is made available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).