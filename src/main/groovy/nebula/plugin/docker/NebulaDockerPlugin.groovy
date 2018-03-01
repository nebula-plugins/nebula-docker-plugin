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

/**
 * Nebula plugin which simplifies the process of creating docker images, tagging and pushing them to repo.
 *
 * @author ltudor
 */
class NebulaDockerPlugin implements Plugin<Project>, Strings, NebulaDockerSensibleDefaults {
    protected void createTasks(Project project, String envir) {
        NebulaDockerExtension nebulaDocker = project.nebulaDocker

        ["", "Latest"].each { tags ->
            String dependTaskName = (!tags) ? "buildImage" : "pushImage${envir}"
            String taggingVersion = "latest"
            if (!tags) {
                if (nebulaDocker.tagVersion) {
                    nebulaDocker.tagVersion.delegate = project
                    taggingVersion = nebulaDocker.tagVersion(project)
                } else {
                    taggingVersion = "${project.version}".toString()
                }
            }

            project.tasks.create(name: "dockerTagImage${envir}${tags}", type: DockerTagImage) { task ->
                dependsOn project.tasks[dependTaskName]
                targetImageId { project.buildImage.imageId }
                def repo = nebulaDocker.dockerRepo[envir.toLowerCase()]
                if( repo instanceof Closure ){
                    repo = repo()
                }
                repository = repo
                task.conventionMapping.tag = { taggingVersion }
                println "Using version $taggingVersion"
                force = true
            }

            project.tasks.create(name: "pushImage${envir}${tags}", type: DockerPushImage) { task ->
                dependsOn project.tasks["dockerTagImage${envir}${tags}"]
                task.conventionMapping.imageName = { project.tasks["dockerTagImage${envir}${tags}"].getRepository() }
                task.conventionMapping.tag = { project.tasks["dockerTagImage${envir}${tags}"].getTag() }
            }
        }
    }

    protected Task taskCreateDockerfile(Project project) {
        project.tasks.create(name: 'createDockerfile', type: Dockerfile) { task ->
            destFile = project.file(project.nebulaDocker.dockerFile)
            dependsOn project.tasks['distTar']
            dependsOn project.tasks['dockerCopyDistResources']
            from "${project.nebulaDocker.dockerBase}"
            maintainer project.nebulaDocker.maintainerEmail

            addFile "${project.distTar.archiveName}", '/'
            runCommand "ln -s '${-> project.nebulaDocker.appDir}' '${project.nebulaDocker.appDirLatest}'"
            entryPoint "${-> project.nebulaDocker.appDir}/bin/${project.applicationName}"
            if (project.nebulaDocker.dockerImage) {
                project.nebulaDocker.dockerImage.delegate = task
                project.nebulaDocker.dockerImage(task)
            }
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
            envir = lowerCaseCapitalize(envir)
            createTasks project, envir
            taskArr << project.tasks["pushImage${envir}Latest"]
        }

        project.tasks.create(name: 'pushAllImages') {
            dependsOn taskArr
        }
    }

    protected void createAllTasks(Project project) {
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
            dependencies.add(project.dependencies.create("com.bmuschko:gradle-docker-plugin:3.0.3"))
            dependencies.add(project.dependencies.create("com.github.docker-java:docker-java:3.0.3"))
            dependencies.add(project.dependencies.create('org.slf4j:slf4j-simple:1.7.5'))
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

