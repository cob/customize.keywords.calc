package com.cultofbits.customizations.utils

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg

class RmHelper {

    def static aDefinition(FieldDefinition... fieldDefinitions) {
        new Definition().with {
            it.name = "definition 1"
            it.version = 1
            it.fieldDefinitions = fieldDefinitions
            it
        }
    }

    def static aFieldDefinition(id, name, description) {
        new FieldDefinition().with {
            it.id = id
            it.name = name
            it.description = description
            it
        }
    }

    def static aFieldMap(Definition definition, Integer fieldDefinitionId, id, value) {
        return [
                fieldDefinition: definition.getField(fieldDefinitionId),
                id             : id,
                value          : value,
        ]
    }

    def static aRecordmMessage(Map... fields) {
        new RecordmMsg(
                [
                        instance: [
                                fields: fields.collect { it }
                        ]
                ])
    }

}
