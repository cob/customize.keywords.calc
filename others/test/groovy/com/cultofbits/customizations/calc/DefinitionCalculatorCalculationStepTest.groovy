package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import org.junit.Test

import static org.junit.Assert.assertEquals

class DefinitionCalculatorCalculationStepTest {

    @Test
    void can_calculate_no_dependencies() {
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
                        it.description = "\$calc.sum(var.field1,var.field2,10,var.nullValue)";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 4; it.name = "nullValue";
                        it.description = "\$var.nullValue";
                        it
                    },
            ]
            it
        }

        def recordmMsg = new RecordmMsg([
                instance: [
                        fields: [
                                buildField(100, definition.fieldDefinitions[0], "1000"),
                                buildField(101, definition.fieldDefinitions[1], "1"),
                                buildField(102, definition.fieldDefinitions[2], "2"),
                                buildField(103, definition.fieldDefinitions[3], null),
                                buildField(104, definition.fieldDefinitions[4], null),
                        ]
                ]
        ])

        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        assertEquals(updateMap["id:103"], "${1 + 2 + 10 + 0}".toString())
    }

    @Test
    void can_calculate_with_chained_dependencies() {
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
                        it.description = "\$calc.multiply(var.field1,2) \$var.field2";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 3; it.name = "field3";
                        it.description = "\$calc.sum(var.field1,var.field2,10,var.nullValue)";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 4; it.name = "field4-nullValue";
                        it.description = "\$var.nullValue";
                        it
                    },
            ]
            it
        }

        def recordmMsg = new RecordmMsg([
                instance: [
                        fields: [
                                buildField(100, definition.fieldDefinitions[0], "1000"),
                                buildField(101, definition.fieldDefinitions[1], 50),
                                buildField(102, definition.fieldDefinitions[2], null),
                                buildField(103, definition.fieldDefinitions[3], null),
                                buildField(104, definition.fieldDefinitions[4], null),
                        ]
                ]
        ])

        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        assertEquals(updateMap["id:102"], "${50 * 2}".toString())
        assertEquals(updateMap["id:103"], "${50 + (50 * 2) + 10 + 0}".toString())
    }

    @Test
    void can_calculate_with_reversed_dependencies() {
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
                        it.description = "\$calc.sum(var.field1,var.field3,10,var.nullValue)";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 3; it.name = "field3";
                        it.description = "\$calc.multiply(var.field1,2) \$var.field3";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 4; it.name = "field4-nullValue";
                        it.description = "\$var.nullValue";
                        it
                    },
            ]
            it
        }

        def recordmMsg = new RecordmMsg([
                instance: [
                        fields: [
                                buildField(100, definition.fieldDefinitions[0], "1000"),
                                buildField(101, definition.fieldDefinitions[1], 50),
                                buildField(102, definition.fieldDefinitions[2], null),
                                buildField(103, definition.fieldDefinitions[3], null),
                                buildField(104, definition.fieldDefinitions[4], null),
                        ]
                ]
        ])

        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        assertEquals(updateMap["id:102"], "${50 + (50 * 2) + 10 + 0}".toString())
        assertEquals(updateMap["id:103"], "${50 * 2}".toString())
    }

    @Test
    void can_handle_circular_dependencies_by_throwing_an_error() {
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
                        it.description = "\$calc.multiply(var.field2,2) \$var.field1";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 2; it.name = "field2";
                        it.description = "\$calc.multiply(var.field1,2) \$var.field2";
                        it
                    },
            ]
            it
        }

        def recordmMsg = new RecordmMsg([
                instance: [
                        fields: [
                                buildField(100, definition.fieldDefinitions[0], "1000"),
                                buildField(101, definition.fieldDefinitions[1], 50),
                                buildField(102, definition.fieldDefinitions[2], null),
                        ]
                ]
        ])

        def calculator = new DefinitionCalculator(definition)

        try {
            calculator.calculate(recordmMsg)
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Cyclic dependency detected, path: fd:1,field1 -> fd:2,field2 -> fd:1,field1")
        }
    }


    static def buildField(id, fieldDefinition, value) {
        return [
                id             : id,
                value          : value,
                fieldDefinition: fieldDefinition
        ]
    }

}
