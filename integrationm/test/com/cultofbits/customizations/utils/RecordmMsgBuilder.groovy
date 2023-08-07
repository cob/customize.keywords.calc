package com.cultofbits.customizations.utils

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import org.apache.commons.lang.math.RandomUtils

class RecordmMsgBuilder {

    private List fields = [];
    private Map diffs = [
            "ADDED"        : [],
            "VALUE_CHANGED": [],
            "VALUE_REMOVED": []
    ];

    static RecordmMsgBuilder aMessage() {
        return new RecordmMsgBuilder()
    }

    def newFieldValue(Definition definition, Integer fieldDefinitionId, String value) {
        assert fieldDefinitionId != null

        def fieldDefinition = definition.getField(fieldDefinitionId)
        assert fieldDefinition != null

        return this.newFieldValue(fieldDefinition, null, value)
    }

    def newFieldValue(Definition definition, Integer fieldDefinitionId, Long id, String value) {
        assert fieldDefinitionId != null

        def fieldDefinition = definition.getField(fieldDefinitionId)
        assert fieldDefinition != null

        return this.newFieldValue(fieldDefinition, id, value)
    }

    def newFieldValue(FieldDefinition fieldDefinition, String value) {
        assert value != null

        return this.newFieldValue(fieldDefinition, null, value)
    }

    def newFieldValue(FieldDefinition fieldDefinition, Long id, String value) {
        return newFieldValue([
                fieldDefinition: fieldDefinition,
                id             : id ?: RandomUtils.nextLong(),
                value          : value,
        ])
    }

    def newFieldValue(Map field) {
        fields << field
        diffs["ADDED"] << [name: field.fieldDefinition.name]
        return this
    }

    RecordmMsg build() {
        new RecordmMsg([
                instance   : [fields: fields],
                oldInstance: [],
                diff       : diffs,
        ])
    }
}
