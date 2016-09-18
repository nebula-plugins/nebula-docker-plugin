package nebula.plugin.docker

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.DockerTagImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.api.Project
import org.gradle.api.Plugin

class NebulaDockerExtension {
    // main ones
    def String maintainerEmail = "ltudor@netflix.com"
    def Map<String, String> dockerRepo
    def String dockerUrl
    def String dockerBase
    def String dockerFile
    def String appDir
    def String appDirLatest


    @Override
    public String toString() {
        return "NebulaDockerExtension{" +
                "maintainerEmail='" + maintainerEmail + '\'' +
                ", dockerRepo=" + dockerRepo +
                ", dockerUrl='" + dockerUrl + '\'' +
                ", dockerBase='" + dockerBase + '\'' +
                ", dockerFile='" + dockerFile + '\'' +
                ", appDir='" + appDir + '\'' +
                ", appDirLatest='" + appDirLatest + '\'' +
                '}';
    }
}

class NebulaDockerPlugin implements Plugin<Project> {
    final String TITAN_TEST = "titan-registry.main.us-east-1.dyntest.netflix.net:7001"
    final String TITAN_PROD = "titan-registry.main.us-east-1.dynprod.netflix.net:7001"
    final String DOCKER_URL_LOCALHOST = "http://localhost:4243"
    final String DOCKER_BASE_OPEN_JRE = "java:openjdk-8-jre"

    final String DEF_DOCKER_FILE = "./build/docker/Dockerfile"

    private void assignDefaults(Project project, NebulaDockerExtension daDockerExt) {
        if (!daDockerExt.dockerUrl) {
            daDockerExt.dockerUrl = DOCKER_URL_LOCALHOST
        }

        if (!daDockerExt.dockerBase) {
            daDockerExt.dockerBase = DOCKER_BASE_OPEN_JRE
        }
        if (!daDockerExt.dockerFile) {
            daDockerExt.dockerFile = DEF_DOCKER_FILE
        }
        if (!daDockerExt.dockerRepo) {
            def groupAppName = "${project.group}/${project.applicationName}"
            daDockerExt.dockerRepo = [test: TITAN_TEST + "/$groupAppName", prod: TITAN_PROD + "/$groupAppName"]
        }
        if (!project.daDocker.appDirLatest) {
            project.daDocker.appDirLatest = "/${project.applicationName}-latest"
        }
    }

    private void createTasks(Project project, String envir) {
        NebulaDockerExtension daDocker = project.daDocker

        for (def tags in ["", "Latest"]) {
            project.tasks.create(name: "dockerTagImage${envir}${tags}", type: DockerTagImage) {
                if (tags == "") {
                    dependsOn project.tasks['buildImage']
                } else {
                    dependsOn project.tasks["pushImage${envir}"]
                }
                targetImageId { project.buildImage.imageId }
                repository = daDocker.dockerRepo[envir.toLowerCase()]
                if (tags == "") {
                    conventionMapping.tag = { "${-> project.version}".toString() }
                } else {
                    conventionMapping.tag = { "latest" }
                }
                force = true
            }

            project.tasks.create(name: "pushImage${envir}${tags}", type: DockerPushImage) {
                dependsOn project.tasks["dockerTagImage${envir}${tags}"]
                conventionMapping.imageName = { project.tasks["dockerTagImage${envir}${tags}"].getRepository() }
                conventionMapping.tag = { project.tasks["dockerTagImage${envir}${tags}"].getTag() }
            }
        }
    }

    private void createAllTasks(Project project) {
        project.tasks.create(name: 'testingDaDockerPlugin') << {
            println "*************************************************"
            println "* DADockerPlugin.testingDaDockerPlugin executed *"
            println "*************************************************"
        }

        project.tasks.create(name: 'createDockerfile', type: Dockerfile) {
            destFile = project.file(project.daDocker.dockerFile)
            dependsOn project.tasks['distTar']
            dependsOn project.tasks['dockerCopyDistResources']
            from "${project.daDocker.dockerBase}"
            maintainer project.daDocker.maintainerEmail

            addFile "${project.distTar.archiveName}", '/'
            runCommand "ln -s ${-> project.daDocker.appDir} ${project.daDocker.appDirLatest}"
            entryPoint "${-> project.daDocker.appDir}/bin/${project.applicationName}"
        }

        project.tasks.create(name: 'buildImage', type: DockerBuildImage) {
            dependsOn project.tasks['createDockerfile']
            inputDir = project.tasks['createDockerfile'].destFile.parentFile
        }

        for (def envir in ['Test', 'Prod']) {
            createTasks project, envir
        }

        project.tasks.create(name: 'pushAllImages') {
            dependsOn project.tasks["pushImageTestLatest"], project.tasks["pushImageProdLatest"]
        }
    }

    void apply(Project project) {
        project.extensions.create("daDocker", DaDockerExtension)
        def daDockerExt = project.daDocker

        project.configure(project) {
            apply plugin: 'com.bmuschko.docker-java-application'
        }

        assignDefaults project, daDockerExt
        project.docker {
            url = daDockerExt.dockerUrl
            javaApplication {
                baseImage = daDockerExt.dockerBase
                maintainer = daDockerExt.maintainerEmail
            }
        }

        project.afterEvaluate {
            if (!project.daDocker.appDir) {
                project.daDocker.appDir = "/${project.applicationName}-${-> project.version}"
            }
            createAllTasks project
        }
    }
}

