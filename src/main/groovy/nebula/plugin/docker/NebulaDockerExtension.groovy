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
    def String maintainerEmail
    def Set<String> environments
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
                ", environments=" + environments +
                ", dockerRepo=" + dockerRepo +
                ", dockerUrl='" + dockerUrl + '\'' +
                ", dockerBase='" + dockerBase + '\'' +
                ", dockerFile='" + dockerFile + '\'' +
                ", appDir='" + appDir + '\'' +
                ", appDirLatest='" + appDirLatest + '\'' +
                '}';
    }
}