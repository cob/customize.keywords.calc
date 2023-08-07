package com.cultofbits.customizations.utils

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import org.apache.commons.lang.math.RandomUtils

class RecordmMsgBuilder {

    private List fields = [];

    static RecordmMsgBuilder aMessage() {
        return new RecordmMsgBuilder()
    }

    def field(Definition definition, Integer fieldDefinitionId, String value) {
        assert fieldDefinitionId != null

        def fieldDefinition = definition.getField(fieldDefinitionId)
        assert fieldDefinition != null

        return this.field(fieldDefinition, null, value)
    }

    def field(Definition definition, Integer fieldDefinitionId, Long id, String value) {
        assert fieldDefinitionId != null

        def fieldDefinition = definition.getField(fieldDefinitionId)
        assert fieldDefinition != null

        return this.field(fieldDefinition, id, value)
    }

    def field(FieldDefinition fieldDefinition, String value) {
        assert value != null

        return this.field(fieldDefinition, null, value)
    }

    def field(FieldDefinition fieldDefinition, Long id, String value) {
        return field([
                fieldDefinition: fieldDefinition,
                id             : id ?: RandomUtils.nextLong(),
                value          : value,
        ])
    }

    def field(Map field) {
        fields << field
        return this
    }

    RecordmMsg build() {
        new RecordmMsg([instance: [
                fields: fields
        ]])
    }
}
