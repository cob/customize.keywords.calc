package com.cultofbits.customizations.calc

import spock.lang.Specification

import static com.cultofbits.customizations.utils.RmHelper.*

class DefinitionCalculatorCalculationStepTest extends Specification {

    void "can calculate with simple definition"() {
        given:
        def definition = aDefinition(
                aFieldDefinition(1, "field1", "\$var.field1 dummy text"),
                aFieldDefinition(2, "field2", "\$number(2) \$var.field2 \$hint[some hint]"),
                aFieldDefinition(3, "field3", "\$var.some.var.structure \$number(2) \$hint[some hint]"),
                aFieldDefinition(4, "field4", "\$calc.sum(var.field1,var.field2,10,var.nullValue)"),
                aFieldDefinition(5, "nullValue", "\$var.nullValue")
        )

        def recordmMsg = aRecordmMessage(
                aFieldMap(definition, 1, 101, "1000"),
                aFieldMap(definition, 2, 102, "1"),
                aFieldMap(definition, 3, 103, "2"),
                aFieldMap(definition, 4, 104, null),
                aFieldMap(definition, 5, 105, null),
        )

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:104"] == "${1000 + 1 + 10}".toString()
    }

    void "ignore trailling zeros"() {
        given:
        def definition = aDefinition(
                aFieldDefinition(1, "field1", "\$number \$var.field1 dummy"),
                aFieldDefinition(2, "field2", "\$calc.multiply(var.field1,280,10)"),
        )

        def recordmMsg = aRecordmMessage(
                aFieldMap(definition, 1, 101, "0.4"),
                aFieldMap(definition, 2, 102, null),
        )

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:102"] == "1120"
    }

    void "can calculate chained calculations"() {
        given:
        def definition = aDefinition(
                aFieldDefinition(1, "field0", "field0"),
                aFieldDefinition(2, "field1", "\$var.field1"),
                aFieldDefinition(3, "field2", "\$calc.multiply(var.field1,2) \$var.field2"),
                aFieldDefinition(4, "field3", "\$calc.sum(var.field1,var.field2,10,var.nullValue)"),
                aFieldDefinition(5, "field4-nullValue", "\$var.nullValue")
        )

        def recordmMsg = aRecordmMessage(
                aFieldMap(definition, 1, 101, "1000"),
                aFieldMap(definition, 2, 102, 50),
                aFieldMap(definition, 3, 103, null),
                aFieldMap(definition, 4, 104, null),
                aFieldMap(definition, 5, 105, null),
        )

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:103"] == "${50 * 2}".toString()
        updateMap["id:104"] == "${50 + (50 * 2) + 10 + 0}".toString()
    }

    void "can calculate chained calculations with fields located after"() {
        given:
        def definition = aDefinition(
                aFieldDefinition(1, "field1", "field1"),
                aFieldDefinition(2, "field2", "\$var.field2"),
                aFieldDefinition(3, "field3", "\$calc.sum(var.field2,var.field4,10,var.nullValue)"),
                aFieldDefinition(4, "field4", "\$calc.multiply(var.field2,2) \$var.field4"),
                aFieldDefinition(5, "field5-nullValue", "\$var.nullValue")
        )

        def recordmMsg = aRecordmMessage(
                aFieldMap(definition, 1, 101, "1000"),
                aFieldMap(definition, 2, 102, 50),
                aFieldMap(definition, 3, 103, null),
                aFieldMap(definition, 4, 104, null),
                aFieldMap(definition, 5, 105, null),
        )

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:103"] == "${50 + (50 * 2) + 10}".toString()
        updateMap["id:104"] == "${50 * 2}".toString()
    }

    void "can handle circular dependencies by throwing an error"() {
        given:
        def definition = aDefinition(
                aFieldDefinition(1, "field1", "field1"),
                aFieldDefinition(2, "field2", "\$calc.multiply(var.field3,2) \$var.field2"),
                aFieldDefinition(3, "field3", "\$calc.multiply(var.field2,2) \$var.field3"),
        )

        def recordmMsg = aRecordmMessage(
                aFieldMap(definition, 1, 101, "1000"),
                aFieldMap(definition, 2, 102, 50),
                aFieldMap(definition, 3, 103, null),
        )

        when:
        def calculator = new DefinitionCalculator(definition)

        then:
        try {
            calculator.calculate(recordmMsg)
        } catch (RuntimeException e) {
            e.getMessage() == "Cyclic dependency detected, path: fd:1,field1 -> fd:2,field2 -> fd:1,field1 -> fd:2,field2"
        }
    }

    void "ignore invisible fields"() {
        given:
        def definition = aDefinition(
                aFieldDefinition(1, "field1", "field1"),
                aFieldDefinition(2, "field2", "\$var.field2"),
                aFieldDefinition(2, "field3", "\$var.fieldwithsamename"),
                aFieldDefinition(3, "field4", "\$var.fieldwithsamename \$hint[should not be parto of the message]"),
                aFieldDefinition(4, "field5", "\$calc.multiply(var.field1,var.fieldwithsamename)"),
        )

        def recordmMsg = aRecordmMessage(
                aFieldMap(definition, 1, 101, "1000"),
                aFieldMap(definition, 2, 102, "1"),
                aFieldMap(definition, 3, 103, "10"),
                aFieldMap(definition, 4, 104, null),
        )

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:104"] == "${1 * 10}".toString()
    }
}
