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

import nebula.test.ProjectSpec
import spock.lang.Specification

/**
 * Unit test for {@link NebulaDockerPlugin}
 *
 * @author ltudor
 */
class NebulaDockerPluginTest extends ProjectSpec {
    def "createTasks create 2 sets of tasks one for current version one for latest"() {
        def repoUrl = 'titan-registry.main.us-east-1.dynprod.netflix.net:7001'
        def x = new NebulaDockerPlugin()
        project.extensions.create("nebulaDocker", NebulaDockerExtension)
        project.configure(project) {
            apply plugin: 'com.bmuschko.docker-java-application'
        }
        project.tasks.create 'buildImage'
        project.nebulaDocker.dockerRepo = [test: repoUrl]

        when:
        x.createTasks project, "Test"
        def tagTest = project.tasks['dockerTagImageTest']
        def pushTest = project.tasks['pushImageTest']
        def tagTestLatest = project.tasks['dockerTagImageTestLatest']
        def pushTestLatest = project.tasks['pushImageTestLatest']

        then:
        tagTest
        tagTest.dependsOn.size() >= 1
        tagTest.dependsOn.find({ it.hasProperty('name') && it.name == 'buildImage' })
        tagTest.repository == repoUrl

        pushTest
        pushTest.dependsOn.size() >= 1
        pushTest.dependsOn.find({ it.hasProperty('name') && it.name == 'dockerTagImageTest' })

        tagTestLatest
        tagTestLatest.dependsOn.size() >= 1
        tagTestLatest.dependsOn.find({ it.hasProperty('name') && it.name == 'pushImageTest' })
        tagTestLatest.repository == repoUrl

        pushTestLatest
        pushTestLatest.dependsOn.size() >= 1
        pushTestLatest.dependsOn.find({ it.hasProperty('name') && it.name == 'dockerTagImageTestLatest' })
    }
}
