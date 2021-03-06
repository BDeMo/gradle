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

package org.gradle.api.publish.internal;

import com.google.gson.stream.JsonWriter;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.ComponentWithVariants;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.ModuleMetadataParser;
import org.gradle.api.internal.component.SoftwareComponentInternal;
import org.gradle.api.internal.component.UsageContext;
import org.gradle.internal.scopeids.id.BuildInvocationScopeId;
import org.gradle.util.GUtil;
import org.gradle.util.GradleVersion;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Set;

public class ModuleMetadataFileGenerator {
    private final BuildInvocationScopeId buildInvocationScopeId;

    public ModuleMetadataFileGenerator(BuildInvocationScopeId buildInvocationScopeId) {
        this.buildInvocationScopeId = buildInvocationScopeId;
    }

    public void generateTo(ModuleVersionIdentifier coordinates, ComponentWithVariants component, Writer writer) throws IOException {
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setHtmlSafe(false);
        jsonWriter.setIndent("  ");
        writeComponentWithVariants(coordinates, component, jsonWriter);
        jsonWriter.flush();
        writer.append('\n');
    }

    private void writeComponentWithVariants(ModuleVersionIdentifier coordinates, ComponentWithVariants component, JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        Set<UsageContext> variants = new LinkedHashSet<UsageContext>();
        collectVariants(component, variants);
        writeFormat(jsonWriter);
        writeCreator(jsonWriter);
        writeVariants(coordinates, jsonWriter, variants);
        jsonWriter.endObject();
    }

    private void writeCreator(JsonWriter jsonWriter) throws IOException {
        jsonWriter.name("createdBy");
        jsonWriter.beginObject();
        jsonWriter.name("gradle");
        jsonWriter.beginObject();
        jsonWriter.name("version");
        jsonWriter.value(GradleVersion.current().getVersion());
        jsonWriter.name("buildId");
        jsonWriter.value(buildInvocationScopeId.getId().asString());
        jsonWriter.endObject();
        jsonWriter.endObject();
    }

    private void writeFormat(JsonWriter jsonWriter) throws IOException {
        jsonWriter.name("formatVersion");
        jsonWriter.value(ModuleMetadataParser.FORMAT_VERSION);
    }

    private void writeVariants(ModuleVersionIdentifier coordinates, JsonWriter jsonWriter, Set<UsageContext> variants) throws IOException {
        if (variants.isEmpty()) {
            return;
        }
        jsonWriter.name("variants");
        jsonWriter.beginArray();
        for (UsageContext variant : variants) {
            writeVariant(coordinates, jsonWriter, variant);
        }
        jsonWriter.endArray();
    }

    private void writeVariant(ModuleVersionIdentifier coordinates, JsonWriter jsonWriter, UsageContext variant) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("name");
        // TODO - give the variant a name
        jsonWriter.value(variant.getUsage().getName());
        jsonWriter.name("attributes");
        jsonWriter.beginObject();
        // TODO - include correct attributes
        jsonWriter.name(Usage.USAGE_ATTRIBUTE.getName());
        jsonWriter.value(variant.getUsage().getName());
        jsonWriter.endObject();

        writeDependencies(jsonWriter, variant);
        writeArtifacts(coordinates, jsonWriter, variant);

        jsonWriter.endObject();
    }

    private void writeArtifacts(ModuleVersionIdentifier coordinates, JsonWriter jsonWriter, UsageContext variant) throws IOException {
        if (variant.getArtifacts().isEmpty()) {
            return;
        }
        jsonWriter.name("files");
        jsonWriter.beginArray();
        for (PublishArtifact artifact : variant.getArtifacts()) {
            writeArtifact(coordinates, jsonWriter, artifact);
        }
        jsonWriter.endArray();
    }

    private void writeArtifact(ModuleVersionIdentifier coordinates, JsonWriter jsonWriter, PublishArtifact artifact) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("name");
        jsonWriter.value(artifact.getFile().getName());

        jsonWriter.name("url");
        // TODO - do not assume Maven layout
        StringBuilder artifactPath = new StringBuilder();
        artifactPath.append(coordinates.getName());
        artifactPath.append('-');
        artifactPath.append(coordinates.getVersion());
        if (GUtil.isTrue(artifact.getClassifier())) {
            artifactPath.append('-');
            artifactPath.append(artifact.getClassifier());
        }
        if (GUtil.isTrue(artifact.getExtension())) {
            artifactPath.append('.');
            artifactPath.append(artifact.getExtension());
        }
        jsonWriter.value(artifactPath.toString());

        jsonWriter.endObject();
    }

    private void writeDependencies(JsonWriter jsonWriter, UsageContext variant) throws IOException {
        if (variant.getDependencies().isEmpty()) {
            return;
        }
        jsonWriter.name("dependencies");
        jsonWriter.beginArray();
        for (ModuleDependency moduleDependency : variant.getDependencies()) {
            writeDependency(jsonWriter, moduleDependency);
        }
        jsonWriter.endArray();
    }

    private void writeDependency(JsonWriter jsonWriter, ModuleDependency moduleDependency) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("group");
        jsonWriter.value(moduleDependency.getGroup());
        jsonWriter.name("module");
        jsonWriter.value(moduleDependency.getName());
        jsonWriter.name("version");
        jsonWriter.value(moduleDependency.getVersion());
        jsonWriter.endObject();
    }

    private void collectVariants(ComponentWithVariants component, Set<UsageContext> dest) {
        if (component instanceof SoftwareComponentInternal) {
            SoftwareComponentInternal softwareComponentInternal = (SoftwareComponentInternal) component;
            dest.addAll(softwareComponentInternal.getUsages());
        }
    }
}
