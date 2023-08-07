package com.cultofbits.customizations.utils

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import org.apache.commons.lang.math.RandomUtils

class FieldDefinitionBuilder {

    private FieldDefinition fieldDefinition;

    static FieldDefinitionBuilder aFieldDefinition() {
        def builder = new FieldDefinitionBuilder()
        builder.fieldDefinition = new FieldDefinition()
        return builder
    }

    /**
     * Builds a number field definition
     * @param decimalPlaces if null will default to 2
     * @return
     */
    static FieldDefinitionBuilder aNumberFieldDefinition(Integer decimalPlaces) {
        def builder = new FieldDefinitionBuilder()
        builder.fieldDefinition = new FieldDefinition()
        builder.fieldDefinition.description = "\$number(${decimalPlaces ?: 2})"
        return builder
    }

    static FieldDefinitionBuilder aDateFieldDefinition() {
        def builder = new FieldDefinitionBuilder()
        builder.fieldDefinition = new FieldDefinition()
        builder.fieldDefinition.description = "\$date"
        return builder
    }

    static FieldDefinitionBuilder aDatetimeFieldDefinition() {
        def builder = new FieldDefinitionBuilder()
        builder.fieldDefinition = new FieldDefinition()
        builder.fieldDefinition.description = "\$datetime"
        return builder
    }

    static FieldDefinitionBuilder aFieldDefinition(id, name, description, boolean required, boolean duplicable, FieldDefinition... childFields) {
        def builder = new FieldDefinitionBuilder()
        builder.definition = new Definition().with {
            it.id = id
            it.name = name
            it.description = description
            it.required = required
            it.duplicable = duplicable
            it.fields = childFields.collect { cf -> cf.rootField = false; cf }
            it
        }
        return builder
    }

    def id(Integer id) {
        fieldDefinition.id = id
        return this
    }

    def name(String name) {
        fieldDefinition.name = name
        return this
    }

    def description(String description) {
        return this.description(description, false)
    }

    def description(String description, boolean append) {
        if (append) {
            fieldDefinition.description += " " + description
        } else {
            fieldDefinition.description = description
        }

        return this
    }

    def required(boolean required) {
        fieldDefinition.required = required
        return this
    }

    def duplicable(boolean duplicable) {
        fieldDefinition.duplicable = duplicable
        return this
    }

    def childFields(FieldDefinition... childFields) {
        fieldDefinition.fields = childFields
        return this
    }

    FieldDefinition build() {
        fieldDefinition.with {
            it.id = it.id ?: RandomUtils.nextInt()
            it.name = it.name ?: "field-definition-${it.id}"
            it.fields = it.fields ?: []
            it
        }
    }
}
