/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package nebula.plugin.docker

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.DockerTagImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Sync

/**
 * Nebula plugin which simplifies the process of creating docker images, tagging and pushing them to repo.
 *
 * @author ltudor
 */
class NebulaDockerPlugin implements Plugin<Project>, Strings, NebulaDockerSensibleDefaults {
    protected void createTasks(Project project, String envir) {
        NebulaDockerExtension nebulaDocker = project.nebulaDocker
        def capitalizedEnvir = lowerCaseCapitalize(envir)

        ["", "Latest"].each { tags ->
            String dependTaskName = (nebulaDocker.tagWithoutPush || !tags) ? "buildImage" : "pushImage${capitalizedEnvir}"
            String taggingVersion = "latest"
            if (!tags) {
                if (nebulaDocker.tagVersion) {
                    nebulaDocker.tagVersion.delegate = project
                    taggingVersion = nebulaDocker.tagVersion(project)
                } else {
                    taggingVersion = "${project.version}".toString()
                }
            }

            project.tasks.create(name: "dockerTagImage${capitalizedEnvir}${tags}", type: DockerTagImage) { task ->
                dependsOn project.tasks[dependTaskName]
                targetImageId { project.buildImage.imageId }
                def repo = nebulaDocker.dockerRepo[envir]
                if( repo instanceof Closure ){
                    repo = repo()
                }
                repository = repo
                task.conventionMapping.tag = { taggingVersion }
                logger.info "Using version $taggingVersion"
                force = true
            }

            project.tasks.create(name: "pushImage${capitalizedEnvir}${tags}", type: DockerPushImage) { task ->
                dependsOn project.tasks["dockerTagImage${capitalizedEnvir}${tags}"]
                task.conventionMapping.imageName = { project.tasks["dockerTagImage${capitalizedEnvir}${tags}"].getRepository() }
                task.conventionMapping.tag = { project.tasks["dockerTagImage${capitalizedEnvir}${tags}"].getTag() }
            }
        }
    }

    protected Task taskCreateDockerfile(Project project) {
        project.tasks.create(name: 'createDockerfile', type: Dockerfile) { task ->
            destFile = project.file(project.nebulaDocker.dockerFile)
            dependsOn project.tasks['nebulaDockerCopyDistResources']
            from "${project.nebulaDocker.dockerBase}"
            label(["maintainer": "${project.nebulaDocker.maintainerEmail}"])

            addFile "${project.distTar.archiveName}", "/"
            runCommand "ln -s '${-> project.nebulaDocker.appDir}' '${project.nebulaDocker.appDirLatest}'"
            entryPoint "${-> project.nebulaDocker.appDir}/bin/${project.applicationName}"
            if (project.nebulaDocker.dockerImage) {
                project.nebulaDocker.dockerImage.delegate = task
                project.nebulaDocker.dockerImage(task)
            }
        }
    }

    protected Task taskCopyDistResources(Project project) {
        project.tasks.create(name: 'nebulaDockerCopyDistResources', type: Sync) { task ->
            dependsOn project.tasks['dockerCopyDistResources']
            from "build/distributions/${project.distTar.archiveName}"
            into "build/docker/app-lib"
        }
    }

    protected Task taskBuildImage(Project project) {
        project.tasks.create(name: 'buildImage', type: DockerBuildImage) {
            dependsOn project.tasks['createDockerfile']
            inputDir = project.tasks['createDockerfile'].destFile.parentFile
        }
    }

    protected Task taskPushAllImages(Project project) {
        List<Task> taskArr = []
        project.nebulaDocker.environments.each { envir ->
            def capitalizedEnvir = lowerCaseCapitalize(envir)
            createTasks project, envir
            taskArr << project.tasks["pushImage${capitalizedEnvir}Latest"]
        }

        project.tasks.create(name: 'pushAllImages') {
            dependsOn taskArr
        }
    }

    protected void createAllTasks(Project project) {
        taskCopyDistResources project
        taskCreateDockerfile project
        taskBuildImage project
        taskPushAllImages project
    }

    void apply(Project project) {
        project.configurations.create('nebulaDocker')
                .setVisible(true)
                .setTransitive(true)
                .setDescription('The Nebula Docker libraries to be used for this project.')
        Configuration config = project.configurations['nebulaDocker']
        config.defaultDependencies { dependencies ->
            dependencies.add(project.dependencies.create("com.bmuschko:gradle-docker-plugin:3.6.0"))
            dependencies.add(project.dependencies.create("com.aries:docker-java-shaded:3.1.0-rc-3:cglib@jar"))
            dependencies.add(project.dependencies.create('org.slf4j:slf4j-simple:1.7.5'))
            dependencies.add(project.dependencies.create('javax.activation:activation:1.1.1'))
            dependencies.add(project.dependencies.create('cglib:cglib:3.2.0'))
        }
        project.configure(project) {
            apply plugin: 'com.bmuschko.docker-java-application'
            project.extensions['docker'].classpath = config
        }

        project.extensions.create("nebulaDocker", NebulaDockerExtension)
        def nebulaDockerExt = project.nebulaDocker

        project.afterEvaluate {
            assignDefaults project, nebulaDockerExt
            project.docker {
                url = nebulaDockerExt.dockerUrl
                javaApplication {
                    baseImage = nebulaDockerExt.dockerBase
                    maintainer = nebulaDockerExt.maintainerEmail
                }

                if(nebulaDockerExt.dockerRepoAuth) {
                    registryCredentials {
                        username = nebulaDockerExt.dockerRepoUsername
                        password = nebulaDockerExt.dockerRepoPassword
                        email = nebulaDockerExt.dockerRepoEmail
                    }
                }
            }

            createAllTasks project
        }
    }
}

