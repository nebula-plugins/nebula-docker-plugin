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


