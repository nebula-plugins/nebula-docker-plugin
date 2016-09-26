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

import org.gradle.api.Project

/**
 * Trait which exposes a set of functionality for checking values of {@link NebulaDockerExtension}
 * and/or ensuring sensible default values where not set by the user.
 *
 * @author ltudor
 */
trait NebulaDockerSensibleDefaults {
    /* Default values here */
    final String TITAN_TEST = "titan-registry.main.us-east-1.dyntest.netflix.net:7001"
    final String TITAN_PROD = "titan-registry.main.us-east-1.dynprod.netflix.net:7001"
    final String DOCKER_URL_LOCALHOST = "http://localhost:4243"
    final String DOCKER_BASE_OPEN_JRE = "java:openjdk-8-jre"
    final String DEF_DOCKER_FILE = "./build/docker/Dockerfile"

    /**
     * Sets the default values for the execution of the plugin, where not set.
     * This traverses all the properties of {@link NebulaDockerExtension} instance and for ones which are not set (null)
     * sets the default values (see constants above).
     *
     * @param project Instance of the project this plugin has been applied to
     * @param nebulaDockerExtension Extension instance where properties are set
     */
    void assignDefaults(Project project, NebulaDockerExtension nebulaDockerExtension) {
        if (!nebulaDockerExtension.dockerUrl) {
            nebulaDockerExtension.dockerUrl = DOCKER_URL_LOCALHOST
        }

        if (!nebulaDockerExtension.dockerBase) {
            nebulaDockerExtension.dockerBase = DOCKER_BASE_OPEN_JRE
        }
        if (!nebulaDockerExtension.dockerFile) {
            nebulaDockerExtension.dockerFile = DEF_DOCKER_FILE
        }

        if (!nebulaDockerExtension.dockerRepo) {
            def groupAppName = "${project.group}/${project.applicationName}"
            nebulaDockerExtension.dockerRepo = [test: TITAN_TEST + "/$groupAppName", prod: TITAN_PROD + "/$groupAppName"]
        }

        if (!nebulaDockerExtension.appDirLatest) {
            nebulaDockerExtension.appDirLatest = "/${project.applicationName}-latest"
        }

        if (!nebulaDockerExtension.appDir) {
            nebulaDockerExtension.appDir = "/${project.applicationName}-${-> project.version}"
        }
    }
}