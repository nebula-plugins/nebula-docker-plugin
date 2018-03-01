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
 * Unit test for {@link Strings}.
 *
 * @author ltudor
 */
class StringsTest extends Specification {
    def "empty/null string returns original string in lowerCaseCapitalize"() {
        def x = new Object() as Strings

        when:
        def r1 = x.lowerCaseCapitalize(null)
        def r2 = x.lowerCaseCapitalize("")

        then:
        r1 == null
        r2 == ""
    }

    def "lowerCaseCapitalize lower cases the string and ensures capital letter"() {
        def x = new Object() as Strings

        when:
        def r1 = x.lowerCaseCapitalize("abc")
        def r2 = x.lowerCaseCapitalize("XYZ")
        def r3 = x.lowerCaseCapitalize("123")
        def r4 = x.lowerCaseCapitalize("oNE")

        then:
        r1 == "Abc"
        r2 == "Xyz"
        r3 == "123"
        r4 == "One"
    }

    def "parentOf returns null/empty for null/empty inputs"() {
        def x = new Object() as Strings

        when:
        def r1 = x.parentOf(null)
        def r2 = x.parentOf("")
        def r3 = x.parentOf('')

        then:
        notThrown(Throwable)
        r1 == null
        r2 == ''
        r3 == ''
    }

    def "parentOf returns the directory itself if no parent found"() {
        def x = new Object() as Strings

        when:
        def r1 = x.parentOf("/")
        def r2 = x.parentOf("xyz")

        then:
        r1 == "/"
        r2 == "xyz"
    }

    def "parentOf returns the parent directory"() {
        def x = new Object() as Strings

        when:
        def r1 = x.parentOf("/abc")
        def r2 = x.parentOf("/ab/")
        def r3 = x.parentOf("/ab/c")
        def r4 = x.parentOf("/ab/c/")
        def r5 = x.parentOf("/ab/c/d/e.png")

        then:
        r1 == "/"
        r2 == "/"
        r3 == "/ab"
        r4 == "/ab"
        r5 == "/ab/c/d"
    }
}
