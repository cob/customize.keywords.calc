package com.cultofbits.customizations.utils

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import org.apache.commons.lang.math.RandomUtils

class DefinitionBuilder {

    private Definition definition;

    static DefinitionBuilder aDefinition() {
        def builder = new DefinitionBuilder()
        builder.definition = new Definition()
        return builder
    }

    static DefinitionBuilder aDefinition(id, name, description, version, FieldDefinition... fieldDefinitions) {
        def builder = new DefinitionBuilder()
        builder.definition = new Definition().with {
            it.id = id
            it.name = name
            it.version = version
            it.description = description
            it.fieldDefinitions = fieldDefinitions
            it
        }
        return builder
    }

    def id(Integer id) {
        definition.id = id
        this
    }

    def name(String name) {
        definition.name = name
        this
    }

    def description(String description) {
        definition.description = description
        this
    }

    def version(Integer version) {
        definition.version = version
        this
    }

    def fieldDefinitions(FieldDefinition... fieldDefinitions) {
        definition.fieldDefinitions = fieldDefinitions
        this
    }

    Definition build() {
        definition.with {
            it.id = it.id ?: RandomUtils.nextInt()
            it.name = it.name ?: "definition-${it.id}"
            it.version = it.version ?: 1
            it.fieldDefinitions = it.fieldDefinitions.collect { fd -> fd.rootField = true; fd } ?: []
            it
        }
    }
}
