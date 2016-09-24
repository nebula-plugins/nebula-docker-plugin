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

import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import nebula.test.ProjectSpec
import org.gradle.api.internal.file.UnionFileCollection
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
        tagTest.dependsOn.size() == 2
        tagTest.dependsOn.find({ it.hasProperty('name') && it.name == 'buildImage' })
        tagTest.repository == repoUrl

        pushTest
        pushTest.dependsOn.size() == 2
        pushTest.dependsOn.find({ it.hasProperty('name') && it.name == 'dockerTagImageTest' })

        tagTestLatest
        tagTestLatest.dependsOn.size() == 2
        tagTestLatest.dependsOn.find({ it.hasProperty('name') && it.name == 'pushImageTest' })
        tagTestLatest.repository == repoUrl

        pushTestLatest
        pushTestLatest.dependsOn.size() == 2
        pushTestLatest.dependsOn.find({ it.hasProperty('name') && it.name == 'dockerTagImageTestLatest' })
    }

    def "createTasks uses the tagVersion closure if set"() {
        def calls = 0
        def repoUrl = 'titan-registry.main.us-east-1.dynprod.netflix.net:7001'
        def x = new NebulaDockerPlugin()
        project.extensions.create("nebulaDocker", NebulaDockerExtension)
        project.configure(project) {
            apply plugin: 'com.bmuschko.docker-java-application'
        }
        project.tasks.create 'buildImage'
        project.nebulaDocker.dockerRepo = [test: repoUrl]
        project.nebulaDocker.tagVersion = { project ->
            calls++
        }

        when:
        x.createTasks project, "Test"

        then:
        calls == 1
    }

    def "createAllTasks creates the dockerfile task, buildImage and pushAllImages"() {
        def x = Spy(NebulaDockerPlugin)

        when:
        x.createAllTasks project

        then:
        1 * x.taskCreateDockerfile(project) >> {}
        1 * x.taskBuildImage(project) >> {}
        1 * x.taskPushAllImages(project) >> {}
    }

    def "taskBuildImage creates the task and set it to depend on createDockerfile"() {
        def x = new NebulaDockerPlugin()
        project.extensions.create("nebulaDocker", NebulaDockerExtension)
        project.configure(project) {
            apply plugin: 'com.bmuschko.docker-java-application'
        }

        def mockParent = Mock(File)

        def mockFile = Stub(File) {
            getParentFile() >> mockParent
        }
        def dockerFileTask = project.tasks.create('createDockerfile', Dockerfile)
        dockerFileTask.destFile = mockFile

        when:
        x.taskBuildImage(project)
        def task = project.tasks['buildImage']

        then:
        task.dependsOn.size() == 2
        task.dependsOn.find({ it.hasProperty('name') && it.name == 'createDockerfile' })
        task.inputDir.is(mockParent)
    }

    def "taskPushAllImages creates tasks for environments and then creates pushAllImages and makes it depend on all push... tasks"() {
        def x = Spy(NebulaDockerPlugin)
        project.extensions.create("nebulaDocker", NebulaDockerExtension)
        project.configure(project) {
            apply plugin: 'com.bmuschko.docker-java-application'
        }
        project.nebulaDocker.dockerRepo = [test: 'abc', prod: 'xyz']
        project.tasks.create 'pushImageTestLatest'
        project.tasks.create 'pushImageProdLatest'

        when:
        x.taskPushAllImages project
        def task = project.tasks['pushAllImages']

        then:
        1 * x.createTasks(project, 'Test') >> {}
        1 * x.createTasks(project, 'Prod') >> {}
        task.dependsOn.size() == 2
        def dpp = task.dependsOn.find({ !(it instanceof UnionFileCollection) })
        dpp.size() == 2
        dpp.find({ it.name == 'pushImageTestLatest' })
        dpp.find({ it.name == 'pushImageProdLatest' })
    }
}
