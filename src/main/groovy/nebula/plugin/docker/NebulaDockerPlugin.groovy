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

/**
 * Nebula plugin which simplifies the process of creating docker images, tagging and pushing them to repo.
 *
 * @author ltudor
 */
class NebulaDockerPlugin implements Plugin<Project>, Strings, NebulaDockerSensibleDefaults {
    private void createTasks(Project project, String envir) {
        NebulaDockerExtension nebulaDocker = project.nebulaDocker

        for (def tags in ["", "Latest"]) {
            project.tasks.create(name: "dockerTagImage${envir}${tags}", type: DockerTagImage) {
                if (tags == "") {
                    dependsOn project.tasks['buildImage']
                } else {
                    dependsOn project.tasks["pushImage${envir}"]
                }
                targetImageId { project.buildImage.imageId }
                repository = nebulaDocker.dockerRepo[envir.toLowerCase()]
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
        project.tasks.create(name: 'createDockerfile', type: Dockerfile) {
            destFile = project.file(project.nebulaDocker.dockerFile)
            dependsOn project.tasks['distTar']
            dependsOn project.tasks['dockerCopyDistResources']
            from "${project.nebulaDocker.dockerBase}"
            maintainer project.nebulaDocker.maintainerEmail

            addFile "${project.distTar.archiveName}", '/'
            runCommand "ln -s ${-> project.nebulaDocker.appDir} ${project.nebulaDocker.appDirLatest}"
            entryPoint "${-> project.nebulaDocker.appDir}/bin/${project.applicationName}"
        }

        project.tasks.create(name: 'buildImage', type: DockerBuildImage) {
            dependsOn project.tasks['createDockerfile']
            inputDir = project.tasks['createDockerfile'].destFile.parentFile
        }

        List<Task> taskArr = []
        for (def envir in project.nebulaDocker.environments) {
            envir = lowerCaseCapitalize(envir)
            createTasks project, envir
            taskArr << project.tasks["pushImage${envir}Latest"]
        }

        project.tasks.create(name: 'pushAllImages') {
            dependsOn taskArr
        }
    }

    void apply(Project project) {
        project.extensions.create("nebulaDocker", NebulaDockerExtension)
        def nebulaDockerExt = project.nebulaDocker

        project.configure(project) {
            apply plugin: 'com.bmuschko.docker-java-application'
        }

        assignDefaults project, nebulaDockerExt
        project.docker {
            url = nebulaDockerExt.dockerUrl
            javaApplication {
                baseImage = nebulaDockerExt.dockerBase
                maintainer = nebulaDockerExt.maintainerEmail
            }
        }

        project.afterEvaluate {
            if (!project.nebulaDocker.appDir) {
                project.nebulaDocker.appDir = "/${project.applicationName}-${-> project.version}"
            }
            createAllTasks project
        }
    }
}

