package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class DefinitionCalculatorCalculationStepTest {

    @Test
    void can_process_simple_relations_with_var_and_calc_expression() {
        Definition definition = new Definition().with {
            it.name = "definition 1"
            it.version = 1
            it.fieldDefinitions = [
                    new FieldDefinition().with {
                        it.id = 0; it.name = "field0";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 1; it.name = "field1";
                        it.description = "\$var.field1";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 2; it.name = "field2";
                        it.description = "\$var.field2";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 3; it.name = "field1";
                        it.description = "\$calc.sum(var.field1,var.field2,10)";
                        it
                    }
            ]
            it
        }

        def recordmMsg = new RecordmMsg([
                instance: [
                        fields: [
                                buildFieldDefinition(100, definition.fieldDefinitions[0], "1000"),
                                buildFieldDefinition(101, definition.fieldDefinitions[1], "1"),
                                buildFieldDefinition(102, definition.fieldDefinitions[2], "2"),
                                buildFieldDefinition(103, definition.fieldDefinitions[3], null),
                        ]
                ]
        ])

        def calculator = new DefinitionCalculator(definition)
        def maybeUpdated = calculator.calculate(recordmMsg)

        assertTrue(maybeUpdated.isPresent())
        assertEquals(maybeUpdated.get()["id:103"], "13")
    }

    static def buildFieldDefinition(id, fieldDefinition, value) {
        return [
                id             : id,
                value          : value,
                fieldDefinition: fieldDefinition
        ]
    }

}
