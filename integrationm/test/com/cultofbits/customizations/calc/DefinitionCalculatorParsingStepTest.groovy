package com.cultofbits.customizations.calc

import spock.lang.Specification

import static com.cultofbits.customizations.utils.DefinitionBuilder.aDefinition
import static com.cultofbits.customizations.utils.FieldDefinitionBuilder.aFieldDefinition
import static com.cultofbits.customizations.utils.FieldDefinitionBuilder.aNumberFieldDefinition


class DefinitionCalculatorParsingStepTest extends Specification {

    void "can detect field definitions with \$var"() {
        given:

        def definition = aDefinition()
                .fieldDefinitions(
                        aFieldDefinition().id(1).description("\$var.field1 dummy text").build(),
                        aNumberFieldDefinition(2).id(2).description("\$var.field2 \$hint[some hint]", true).build(),
                        aNumberFieldDefinition(2).id(3).description("\$var.some.var.structure \$hint[some hint]", true).build(),
                        aNumberFieldDefinition(2).id(4).description(" \$hint[some hint] \$var.some.var.structure", true).build(),
                ).build()

        when:
        def calculator = new DefinitionCalculator(definition)

        then:
        calculator.fdVarsMapByVarName["var.field1"].collect { it.id }.join(",") == "1"
        calculator.fdVarsMapByVarName["var.field2"].collect { it.id }.join(",") == "2"
        calculator.fdVarsMapByVarName["var.some.var.structure"].collect { it.id }.join(",") == "3,4"
    }

    void "can parse field definitions with var and calc expressions"() {
        given:

        def definition = aDefinition()
                .fieldDefinitions(
                        aFieldDefinition().id(1).description("\$var.field1 dummy text").build(),
                        aFieldDefinition().id(2).description("\$number(2) \$var.field2 \$hint[some hint]").build(),
                        aFieldDefinition().id(3).description("\$calc.multiply(var.field1,var.field2,10)").build(),
                        aFieldDefinition().id(4).description("\$var.field4 \$calc.multiply(var.field1,10)").build(),
                ).build()

        when:
        def calculator = new DefinitionCalculator(definition)

        then:
        calculator.defName == definition.name
        calculator.defVersion == 1

        calculator.fdVarsMapByVarName["var.field1"] == [definition.fieldDefinitions[0]]
        calculator.fdVarsMapByVarName["var.field2"] == [definition.fieldDefinitions[1]]
        calculator.fdVarsMapByVarName["var.field4"] == [definition.fieldDefinitions[3]]

        calculator.fdCalcExprMapById[3].operation == "multiply"
        calculator.fdCalcExprMapById[3].args == ["var.field1", "var.field2", "10"]

        calculator.fdCalcExprMapById[4].operation == "multiply"
        calculator.fdCalcExprMapById[4].args == ["var.field1", "10"]
    }

    void "can parse definition with 'previous' calc arg"() {
        given:
        def definition = aDefinition()
                .fieldDefinitions(
                        aFieldDefinition().id(1).build(),
                        aFieldDefinition().id(2).description("\$calc.multiply(previous,2)").build(),
                        aFieldDefinition().id(3).description("\$calc.multiply(previous,4)").build(),
                ).build()

        when:
        def calculator = new DefinitionCalculator(definition)

        then:
        calculator.fdCalcExprMapById[2].operation == "multiply"
        calculator.fdCalcExprMapById[2].args == ["previous:1", "2"]

        calculator.fdCalcExprMapById[3].operation == "multiply"
        calculator.fdCalcExprMapById[3].args == ["previous:2", "4"]
    }

    void "can process with subparts"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aFieldDefinition().id(1).description("\$var.total.chickens").build(),
                aFieldDefinition().id(2).description("\$var.total.dogs.small").build(),
                aFieldDefinition().id(3).description("\$var.total.dogs.big").build(),

                aFieldDefinition().id(4).description("\$calc.sum(var.total.dogs)").build(),
                aFieldDefinition().id(5).description("\$calc.sum(var.total)").build(),
        ).build()

        when:
        def calculator = new DefinitionCalculator(definition)

        then:
        calculator.fdCalcExprMapById[4].operation == "sum"
        calculator.fdCalcExprMapById[4].args == ["var.total.dogs.small", "var.total.dogs.big"]

        calculator.fdCalcExprMapById[5].operation == "sum"
        calculator.fdCalcExprMapById[5].args == ["var.total.chickens", "var.total.dogs.small", "var.total.dogs.big"]
    }

    void "collect field definitions with samve var"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aFieldDefinition().id(1).description("field1").build(),
                aFieldDefinition().id(2).description("\$var.field2").build(),
                aFieldDefinition().id(3).description("\$var.fieldwithsamename").build(),
                aFieldDefinition().id(4).description("\$var.fieldwithsamename").build(),
                aFieldDefinition().id(5).description("\$calc.multiply(var.field2,var.fieldwithsamename)").build(),
        ).build()

        when:
        def calculator = new DefinitionCalculator(definition)

        then:
        calculator.fdVarsMapByVarName["var.fieldwithsamename"].collect { it.id } == [3, 4]
    }

}