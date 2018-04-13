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
 * Trait which offers string manipulation utils.
 *
 * @author ltudor
 */
trait Strings {
    /**
     * Given a string ensures it's all lowercase and initial letter capital.
     *
     * @param s String to lowercase and capitalize.
     * @return Original string all lowercase and capitalize or null/empty if the original string is null/empty.
     */
    String lowerCaseCapitalize(String s) {
        if (!s) return s
        s.toLowerCase().capitalize()
    }

    String parentOf(String directory) {
        if (!directory) return directory
        def file = new File(directory)
        file.parent ?: directory
    }
}
