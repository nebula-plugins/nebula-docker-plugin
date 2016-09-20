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

import spock.lang.Specification

/**
 * Unit test for {@link NebulaDockerExtension}.
 *
 * @author ltudor
 */
class NebulaDockerExtensionTest extends Specification {
    def "environments is based on dockerRepo keyset"() {
        def x = new NebulaDockerExtension()
        x.dockerRepo = [a: 'b', c: 'd']

        when:
        def r = x.environments

        then:
        r == ['a', 'c'] as Set
    }

    def "environments returns an immutable set copy based on dockerRepo keyset"() {
        def x = new NebulaDockerExtension()
        x.dockerRepo = [a: 'b', c: 'd']
        def r = x.environments

        when:
        r.add 'x'

        then:
        thrown(UnsupportedOperationException)
    }

    def "environments returns null for empty/null dockerRepo"() {
        def x1 = new NebulaDockerExtension()
        x1.dockerRepo = [:]
        def x2 = new NebulaDockerExtension()

        when:
        def r1 = x1.environments
        def r2 = x2.environments

        then:
        r1 == null
        r2 == null
    }
}
