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

/**
 * Simple bean used for holding configuration information for the {@link NebulaDockerPlugin}.
 *
 * @author ltudor
 */
class NebulaDockerExtension {
    /**
     * Email address of the maintainer of the docker image.
     */
    def String maintainerEmail

    /**
     * Map of environment to docker repository.
     * This allows for the docker image to be deployed in different environments / repos.
     * For instance, if you have a different docker repo for 'test' and 'prod' you can define this as:
     * <code>project.nebulaDocker.dockerRepo = [test: 'http://url.for.test/path', prod: 'https://url.prod:port/path']</code>
     * Note that the keys of this map are used to generate the {@link #getEnvironments()} property.
     *
     * @see #getEnvironments()
     */
    def Map<String, String> dockerRepo

    /**
     * Docker daemon url.
     */
    def String dockerUrl

    /**
     * Docker base image.
     */
    def String dockerBase

    /**
     * Where in the project build structure is the Dockerfile.
     * Typically this will be in <code>./build/docker/Dockerfile</code>, but if you handcrafted your own Dockerfile, specify the path here.
     */
    def String dockerFile

    /**
     * Name of the directory to be created in the docker image to host the app.
     * Typically this is set to be <code>/applicationName-version.version</code> but if you want a different dir structure, specify it here.
     * Note that {@link #appDirLatest} will be symlink'd to this location.
     *
     * @see #appDirLatest
     */
    def String appDir

    /**
     * Name of the "latest" dir.
     * This will be symlink'd to the {@link #appDir} directory in the docker image and typically is set to
     * <code>/applicationName-latest</code> but if you want to change that name you can do so here.
     *
     * @see #appDir
     */
    def String appDirLatest

    /**
     * Closure to execute when building the docker image.
     * By default the code just creates the {@link #appDir} directory and symlinks {@link #appDirLatest} to it and sets the entry point
     * to the shell script in {@link #appDirLatest}/bin.
     * If you need any other files or symlinks or commands to be executed, specify them here.
     * If not set (or set to <code>null</code>) then no extra commands will be added to the Dockerfile.
     */
    def Closure dockerImage

    /**
     * Closure used to set the tag on the docker image.
     * Typically the code will set 2 tags: one with the application version and one with <code>latest</code>.
     * This closure allows you to define the tagging for the application version.
     * If not set, the application version will be set, as per above.
     */
    def Closure<String> tagVersion

    /**
     * Defines a property which returns all the environments defined in the {@link #dockerRepo}.
     * This is basically a set of all the keys present in the {@link #dockerRepo} set.
     *
     * @return Immutable set of all the environments present in {@link #dockerRepo} or <code>null</code> if the {@link #dockerRepo} is null/empty.
     * @see #dockerRepo
     */
    Set<String> getEnvironments() {
        if (!dockerRepo) return null
        Collections.unmodifiableSet(dockerRepo.keySet())
    }

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
                ", environments=" + getEnvironments() +
                '}';
    }
}