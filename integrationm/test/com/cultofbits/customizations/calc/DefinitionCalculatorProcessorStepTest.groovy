package com.cultofbits.customizations.calc

import spock.lang.Specification

import static com.cultofbits.customizations.utils.RmHelper.aDefinition
import static com.cultofbits.customizations.utils.RmHelper.aFieldDefinition

class DefinitionCalculatorProcessorStepTest extends Specification {

    void "can detect field definitions with \$var"() {
        given:
        def definition = aDefinition(
                aFieldDefinition(1, "field1", "\$var.field1 dummy text"),
                aFieldDefinition(2, "field2", "\$number(2) \$var.field2 \$hint[some hint]"),
                aFieldDefinition(3, "field3", "\$var.some.var.structure \$number(2) \$hint[some hint]"),
                aFieldDefinition(4, "field4", "\$number(2) \$hint[some hint] \$var.some.var.structure")
        )

        when:
        def calculator = new DefinitionCalculator(definition)

        then:
        calculator.fdVarsByMapVarName["var.field1"].collect { it.id }.join(",") == "1"
        calculator.fdVarsByMapVarName["var.field2"].collect { it.id }.join(",") == "2"
        calculator.fdVarsByMapVarName["var.some.var.structure"].collect { it.id }.join(",") == "3,4"
    }

    void "can parse field definitions with var and calc expressions"() {
        given:
        def definition = aDefinition(
                aFieldDefinition(1, "field1", "\$var.field1 dummy text"),
                aFieldDefinition(2, "field2", "\$number(2) \$var.field2 \$hint[some hint]"),
                aFieldDefinition(3, "field3", "\$calc.multiply(var.field1,var.field2,10)"),
                aFieldDefinition(4, "field4", "\$var.field4 \$calc.multiply(var.field1,10)"),
        )

        when:
        def calculator = new DefinitionCalculator(definition)

        then:
        calculator.defName == definition.name
        calculator.defVersion == 1

        calculator.fdVarsByMapVarName["var.field1"] == [definition.fieldDefinitions[0]]
        calculator.fdVarsByMapVarName["var.field2"] == [definition.fieldDefinitions[1]]
        calculator.fdVarsByMapVarName["var.field4"] == [definition.fieldDefinitions[3]]

        calculator.fdCalcExprMapById[3].operation == "multiply"
        calculator.fdCalcExprMapById[3].args == ["var.field1", "var.field2", "10"]

        calculator.fdCalcExprMapById[4].operation == "multiply"
        calculator.fdCalcExprMapById[4].args == ["var.field1", "10"]
    }

}
