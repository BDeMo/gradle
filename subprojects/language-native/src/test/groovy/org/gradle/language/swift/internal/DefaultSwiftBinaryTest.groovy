/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.language.swift.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.util.TestUtil
import spock.lang.Specification

class DefaultSwiftBinaryTest extends Specification {
    def implementation = Stub(Configuration)
    def compile = Stub(Configuration)
    def link = Stub(Configuration)
    def runtime = Stub(Configuration)
    def configurations = Stub(ConfigurationContainer)
    DefaultSwiftBinary binary

    def setup() {
        _ * configurations.maybeCreate("swiftImportDebug") >> compile
        _ * configurations.maybeCreate("nativeLinkDebug") >> link
        _ * configurations.maybeCreate("nativeRuntimeDebug") >> runtime

        binary = new DefaultSwiftBinary("mainDebug", TestUtil.objectFactory(), Stub(Provider), true, Stub(FileCollection),  configurations, implementation)
    }

    def "creates configurations for the binary"() {
        expect:
        binary.compileImportPath == compile
        binary.linkLibraries == link
        binary.runtimeLibraries == runtime
    }

}
