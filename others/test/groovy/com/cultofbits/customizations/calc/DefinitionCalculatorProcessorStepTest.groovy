package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import org.junit.Test

import static org.junit.Assert.assertEquals

class DefinitionCalculatorProcessorStepTest {

    @Test
    void validate_var_regex() {
        Definition definition = new Definition().with {
            it.name = "definition 1"
            it.version = 1
            it.fieldDefinitions = [
                    new FieldDefinition().with {
                        it.id = 1; it.name = "field1";
                        it.description = "\$var.field1 dummy text";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 2; it.name = "field2";
                        it.description = "\$number(2) \$var.field2 \$hint[some hint]";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 3; it.name = "field3";
                        it.description = "\$var.some.var.structure \$number(2) \$hint[some hint]";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 4; it.name = "field4";
                        it.description = "\$number(2) \$hint[some hint] \$var.some.var.structure ";
                        it
                    }
            ]
            it
        }

        def calculator = new DefinitionCalculator(definition)

        assertEquals(calculator.fdVarsByVarName["var.field1"].collect { it.id }.join(","), "1")
        assertEquals(calculator.fdVarsByVarName["var.field2"].collect { it.id }.join(","), "2")
        assertEquals(calculator.fdVarsByVarName["var.some.var.structure"].collect { it.id }.join(","), "3,4")
    }

    @Test
    void can_process_simple_relations_with_var_and_calc_expression() {
        Definition definition = new Definition().with {
            it.name = "definition 1"
            it.version = 1
            it.fieldDefinitions = [
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
                        it.description = "\$calc.multiply(var.field1,var.field2,10)";
                        it
                    }
            ]
            it
        }

        def calculator = new DefinitionCalculator(definition)
        assertEquals(calculator.defName, definition.name)
        assertEquals(calculator.defVersion, 1)

        assertEquals(calculator.fdVarsByVarName["var.field1"], [definition.fieldDefinitions[0]])
        assertEquals(calculator.fdVarsByVarName["var.field2"], [definition.fieldDefinitions[1]])

        assertEquals(calculator.fdCalcExprById[3].operation, "multiply")
        assertEquals(calculator.fdCalcExprById[3].args, ["var.field1", "var.field2", "10"])
    }

    @Test
    void can_process_simple_relations_with_var_and_calc_with_dependencies() {
        Definition definition = new Definition().with {
            it.name = "definition 1"
            it.version = 1
            it.fieldDefinitions = [
                    new FieldDefinition().with {
                        it.id = 1; it.name = "field1";
                        it.description = "\$var.field1";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 2; it.name = "field2";
                        it.description = "\$var.field2 \$calc.sum(var.field1,1000)";
                        it
                    },
                    new FieldDefinition().with {
                        it.id = 3; it.name = "field1";
                        it.description = "\$calc.multiply(var.field1,var.field2,10)";
                        it
                    }
            ]
            it
        }

        def calculator = new DefinitionCalculator(definition)
        assertEquals(calculator.defName, definition.name)
        assertEquals(calculator.defVersion, 1)

        assertEquals(calculator.fdVarsByVarName["var.field1"], [definition.fieldDefinitions[0]])
        assertEquals(calculator.fdVarsByVarName["var.field2"], [definition.fieldDefinitions[1]])

        assertEquals(calculator.fdCalcExprById[2].operation, "sum")
        assertEquals(calculator.fdCalcExprById[2].args, ["var.field1", "1000"])

        assertEquals(calculator.fdCalcExprById[3].operation, "multiply")
        assertEquals(calculator.fdCalcExprById[3].args, ["var.field1", "var.field2", "10"])
    }

}
