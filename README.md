# Nebula Docker Plugin

Plugin to help with assembling Docker images from Java apps. This is a very opinionated plugin (in the spirit of Nebula) regarding the layout of the docker image.

It uses the [docker gradle plugin](https://github.com/bmuschko/gradle-docker-plugin) by [Benjamin Muschko](https://github.com/bmuschko) to define a set of "opinionated" tasks for easily creating a docker image.

The plugin relies on the `application` plugin, which offers the `distTar` task; the `distTar` task will create a tarball using name format `applicationName-version.tar` (e.g. `application-1.0.1.tar`). This tarball will unpack in a directory which contains the version number (e.g. `application-1.0.1` in the above example) -- and as such when applying the `gradle-docker-plugin` the entry point to the docker image would be set in this case to `/application-1.0.1/bin/application`. Each incremental version will see a change in the tarball name and therefore in the docker image entry point. This has the implication of having to update other systems which need to start the docker image with the new version/path. 

To eliminate this issue, the plugin adds a symlink in the docker image : `appicationName-latest` -> `applicationName-version` directory and then uses the symlink to define the docker image entry point to be `application-latest/bin/application`. The `distTar` plugin will still generate a versioned tar and directory as per before, but simply adding the symlink in the docker image means that the docker image entry point is consistent across different versions of the docker image.

Also, using the `gradle-docker-plugin` requires a lot of boilerplate code to customize the docker image (add files, symlinks, execute commands etc) -- this plugin handles all of that and allows for a simple closure to be defined to handle constructing the docker image.

Tagging a docker image again requires boilerplat -- the `nebula.docker` plugin by default tags the docker image with the application version number _as well as_ `latest`. It also allows for a closure to be defined to set the tag/version of the docker image.

There are a few other customizations the plugin offers -- see the section below for more details on this.

# Quick Start

Simply apply the plugin in your `build.gradle`:

```
apply plugin: 'nebula.docker'
```

That's it! The plugin will build the docker image and publish it for you.

See the section below for customizing the plugin.

## Customization

The plugin exports an instance of [NebulaDockerExtension](src/main/groovy/nebula/plugin/docker/NebulaDockerExtension.groovy) via `project.nebulaDocker` -- which allows you to customize various aspects of the plugin execution as follows:

```
nebulaDocker {
    maintainerEmail = 'some@email.address.com'
    dockerUrl = 'http://docker.host.com'
    dockerBase = 'java:openjdk-8-jre'
    dockerFile = '/some/build/dir/Dockerfile'
    appDir = '/myapp-name'
    appDirLatest = '/myapp-latest
    dockerRepo = [test:'http://repo.for.test.com', prod: 'https://prod.repo.url/path', dev: 'http://localhost:port/some/path']
    dockerImage = {
        addFile 'my_properties.properties', '/path/etc'
        ...
    }
    tagVersion = {
        def x = computeX()
        def y = computeY()
        "some-version-i-want-set.${x}.${y}"
    }
}
```

where:


| Field             | Type               | Default Value (if not set)                       | Description                                                                                                       |
|-------------------|--------------------|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `maintainerEmail` | String             | `null` (not set)                                 | Email address of the maintainer of this docker image                                                              |
| `dockerUrl`       | String             | "http://localhost:4243"                          | URL used to communicate with the docker process to build the image etc                                            |              
| `dockerBase`      | String             | "java:openjdk-8-jre"                             | Base docker image to extend / build your docker image from                                                        |
| `dockerFile`      | String             | "./build/docker/Dockerfile"                      | Location of the `Dockerfile` in your build. If you have a handcrafted `Dockerfile` specify it here.               |
| `appDir`          | String             | "/${project.applicationName}-${project.version}" | Directory in the docker image where your app will be unpacked.                                                    |
| `appDirLatest`    | String             | "/${project.applicationName}-latest"             | Symlink directory which will be symlink to the `appDir` folder and be set to the entry point in the docker image. |
| `dockerRepo`      | Map<String,String> | [test: "titan-registry.main.us-east-1.dyntest.netflix.net:7001/${project.group}/${project.applicationName}", prod: "titan-registry.main.us-east-1.dynprod.netflix.net:7001/${project.group}/${project.applicationName}"] | Map of environment to docker repository. This allows for the docker image to be deployed in different environments / repos. For instance, if you have a different docker repo for 'test' and 'prod' you can define this as `project.nebulaDocker.dockerRepo = [test: 'http://url.for.test/path', prod: 'https://url.prod:port/path']`. _Note_ that the keys of this map are used to generate `environments` property on the `project.nebulaDocker`. |
| `dockerImage`     | Closure            | `null` (not set)                                 | Closure to execute when building the docker image. By default the code just creates the `appDir` directory and symlinks `appDirLatest` to it and sets the entry point to the shell script in `appDirLatest/bin`. If you need any other files or symlinks or commands to be executed, specify them here. |
| `tagVersion`      | Closure<String>    | `{ "${project.version}" }`                       | Closure used to set the tag on the docker image. Typically the code will set 2 tags: one with the application version and one with <code>latest</code>. This closure allows you to define the tagging for the application version. |

# Boilerplate Code

This is provided for reference, but this is the equivalent boilerplate code the plugin replaces:

```groovy
...
apply plugin: 'com.bmuschko.docker-java-application'
...
def dockerRepositoryProd = "repository.docker.test.net/${project.group}/${project.applicationName}"
def dockerRepositoryTest = "repository.docker.production.com/${project.group}/${project.applicationName}"
def dockerBase = 'java:openjdk-8-jre'
...
buildscript { dependencies { classpath "com.bmuschko:gradle-docker-plugin:$dockerVersion" } }
docker {
    url = 'http://docker.url.com:port'
    javaApplication {
        baseImage = dockerBase
        maintainer = 'email-address@maintainer.domain'
    }
}

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerTagImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile

afterEvaluate {
    def appDir = "/${project.applicationName}-${project.version}"
    def appDirLatest = "/${project.applicationName}-latest"

    task createDockerfile(type: Dockerfile) {
        destFile = project.file('./build/docker/Dockerfile')
        dependsOn distTar
        dependsOn dockerCopyDistResources
        from "$dockerBase"
        maintainer 'email-address@maintainer.domain'


        println "Using tar: ${distTar.archiveName}"
        addFile "${distTar.archiveName}", '/'
        runCommand "ln -s ${appDir} ${appDirLatest}"
        entryPoint "${appDir}/bin/${project.applicationName}"
    }

    task buildImage(type: DockerBuildImage) {
        dependsOn createDockerfile
        inputDir = createDockerfile.destFile.parentFile
    }

    // TEST
    task dockerTagImageTest(type: DockerTagImage) {
        dependsOn buildImage
        targetImageId { buildImage.imageId }
        repository = dockerRepositoryTest
        conventionMapping.tag = { project.version }
        force = true
    }

    task pushImageTest(type: DockerPushImage) {
        dependsOn dockerTagImageTest
        conventionMapping.imageName = { dockerTagImageTest.getRepository() }
        conventionMapping.tag = { dockerTagImageTest.getTag() }
    }

    task dockerTagImageTestLatest(type: DockerTagImage) {
        dependsOn pushImageTest
        targetImageId { buildImage.imageId }
        repository = dockerRepositoryTest
        conventionMapping.tag = { 'latest' }
        force = true
    }
    task pushImageTestLatest(type: DockerPushImage) {
        dependsOn dockerTagImageTestLatest
        conventionMapping.imageName = { dockerTagImageTest.getRepository() }
        conventionMapping.tag = { dockerTagImageTestLatest.getTag() }
    }

    // PROD
    task dockerTagImageProd(type: DockerTagImage) {
        dependsOn buildImage
        targetImageId { buildImage.imageId }
        repository = dockerRepositoryProd
        conventionMapping.tag = { project.version }
        force = true
    }

    task pushImageProd(type: DockerPushImage) {
        dependsOn dockerTagImageProd
        conventionMapping.imageName = { dockerTagImageProd.getRepository() }
        conventionMapping.tag = { dockerTagImageProd.getTag() }
    }

    task dockerTagImageProdLatest(type: DockerTagImage) {
        dependsOn pushImageProd
        targetImageId { buildImage.imageId }
        repository = dockerRepositoryProd
        conventionMapping.tag = { 'latest' }
        force = true
    }

    task pushImageProdLatest(type: DockerPushImage) {
        dependsOn dockerTagImageProdLatest
        conventionMapping.imageName = { dockerTagImageProd.getRepository() }
        conventionMapping.tag = { dockerTagImageProdLatest.getTag() }
    }

    task pushAllImages {
        dependsOn pushImageTestLatest, pushImageProdLatest
    }
}


```
