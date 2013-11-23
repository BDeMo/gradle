/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.ide.visualstudio

import org.gradle.ide.visualstudio.fixtures.SolutionFile
import org.gradle.nativebinaries.language.cpp.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativebinaries.language.cpp.fixtures.app.CppHelloWorldApp
import org.gradle.nativebinaries.language.cpp.fixtures.app.ExeWithLibraryUsingLibraryHelloWorldApp

class VisualStudioSingleProjectIntegrationTest extends AbstractInstalledToolChainIntegrationSpec {

    def app = new CppHelloWorldApp()

    def setup() {
        buildFile << """
    apply plugin: 'cpp'
    apply plugin: 'visual-studio'

    buildTypes {
        debug {}
        release {}
    }

    targetPlatforms {
        win32 {
            architecture "i386"
        }
    }

"""
    }

    def "create visual studio solution for single executable"() {
        when:
        app.writeSources(file("src/main"))
        buildFile << """
    executables {
        main {}
    }
"""
        and:
        run "mainVisualStudio"

        then:
        solutionFile("visualStudio/main.sln").assertHasProjects("mainExe")
    }

    def "create visual studio solution for single library"() {
        when:
        app.library.writeSources(file("src/main"))
        buildFile << """
    libraries {
        main {}
    }
"""
        and:
        run "mainVisualStudio"

        then:
        solutionFile("visualStudio/main.sln").assertHasProjects("mainDll")
    }


    def "create visual studio solution for executable that depends on static library"() {
        when:
        app.executable.writeSources(file("src/main"))
        app.library.writeSources(file("src/hello"))
        buildFile << """
    libraries {
        hello {}
    }
    executables {
        main {}
    }
    sources.main.cpp.lib libraries.hello.static
"""
        and:
        run "mainVisualStudio"

        then:
        solutionFile("visualStudio/main.sln").assertHasProjects("mainExe", "helloLib")
    }

    def "create visual studio solution for executable that depends on shared library"() {
        when:
        app.executable.writeSources(file("src/main"))
        app.library.writeSources(file("src/hello"))
        buildFile << """
    libraries {
        hello {}
    }
    executables {
        main {}
    }
    sources.main.cpp.lib libraries.hello
"""
        and:
        run "mainVisualStudio"

        then:
        solutionFile("visualStudio/main.sln").assertHasProjects("mainExe", "helloDll")
    }

    def "create visual studio solution for executable that depends on library that depends on another library"() {
        given:
        def testApp = new ExeWithLibraryUsingLibraryHelloWorldApp()
        testApp.writeSources(file("src/main"), file("src/hello"), file("src/greetings"))

        buildFile << """
            apply plugin: "cpp"
            libraries {
                greetings {}
                hello {}
            }
            executables {
                main {}
            }
            sources {
                hello.cpp.lib libraries.greetings
                main.cpp.lib libraries.hello
            }
        """
        when:
        run "mainVisualStudio"

        then:
        solutionFile("visualStudio/main.sln").assertHasProjects("mainExe", "helloDll", "greetingsDll")
    }

    def "create visual studio solutions for 2 executables that depend on different linkages of the same library"() {
        when:
        app.executable.writeSources(file("src/main"))
        app.library.writeSources(file("src/hello"))
        buildFile << """
    libraries {
        hello {}
    }
    executables {
        main {}
        mainStatic {}
    }
    sources.main.cpp.lib libraries.hello
    sources.mainStatic.cpp.source.srcDirs "src/main/cpp"
    sources.mainStatic.cpp.lib libraries.hello.static
"""
        and:
        run "mainVisualStudio", "mainStaticVisualStudio"

        then:
        solutionFile("visualStudio/main.sln").assertHasProjects("mainExe", "helloDll")
        solutionFile("visualStudio/mainStatic.sln").assertHasProjects("mainStaticExe", "helloLib")
    }

    private SolutionFile solutionFile(String path) {
        return new SolutionFile(file(path))
    }
}
