package com.cultofbits.customizations.calc

import com.cultofbits.customizations.utils.RecordmMsgBuilder
import spock.lang.Specification

import static com.cultofbits.customizations.utils.DefinitionBuilder.aDefinition
import static com.cultofbits.customizations.utils.FieldDefinitionBuilder.aFieldDefinition
import static com.cultofbits.customizations.utils.FieldDefinitionBuilder.aNumberFieldDefinition


class DefinitionCalculatorCalculationStepTest extends Specification {

    void "can calculate with simple definition"() {
        given:
        def definition = aDefinition()
                .fieldDefinitions(
                        aFieldDefinition().id(1).description("\$var.field1 dummy text").build(),
                        aNumberFieldDefinition(2).id(2).description("\$var.field2 \$hint[some hint]", true).build(),
                        aNumberFieldDefinition(2).id(3).description("\$var.some.var.structure \$hint[some hint]", true).build(),
                        aFieldDefinition().id(4).description("\$calc.sum(var.field1,var.field2,10,var.nullValue)").build(),
                        aFieldDefinition().id(5).name("nullValue").description("\$var.nullValue").build(),
                ).build()


        def recordmMsg = RecordmMsgBuilder.aMessage()
                .newFieldValue(definition, 1, 101, "1000")
                .newFieldValue(definition, 2, 102, "1")
                .newFieldValue(definition, 3, 103, "2")
                .newFieldValue(definition, 4, 104, null)
                .newFieldValue(definition, 5, 105, null)
                .build()

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:104"] == "${1000 + 1 + 10}".toString()
    }

    void "can calculate chained calculations"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aFieldDefinition().id(1).description("field0").build(),
                aFieldDefinition().id(2).description("\$var.field1").build(),
                aFieldDefinition().id(3).description("\$calc.multiply(var.field1,2) \$var.field2").build(),
                aFieldDefinition().id(4).description("\$calc.sum(var.field1,var.field2,10,var.nullValue)").build(),
                aFieldDefinition().id(5).name("field4-nullValue").description("\$var.nullValue").build(),
        ).build()

        def recordmMsg = RecordmMsgBuilder.aMessage()
                .newFieldValue(definition, 1, 101, "1000")
                .newFieldValue(definition, 2, 102, "50")
                .newFieldValue(definition, 3, 103, null)
                .newFieldValue(definition, 4, 104, null)
                .newFieldValue(definition, 5, 105, null)
                .build()

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:103"] == "${50 * 2}".toString()
        updateMap["id:104"] == "${50 + (50 * 2) + 10 + 0}".toString()
    }

    void "can calculate chained calculations with fields located after"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aFieldDefinition().id(1).description("field1").build(),
                aFieldDefinition().id(2).description("\$var.field2").build(),
                aFieldDefinition().id(3).description("\$calc.sum(var.field2,var.field4,10,var.nullValue)").build(),
                aFieldDefinition().id(4).description("\$calc.multiply(var.field2,2) \$var.field4").build(),
                aFieldDefinition().id(5).name("field5-nullValue").description("\$var.nullValue").build(),
        ).build()

        def recordmMsg = RecordmMsgBuilder.aMessage()
                .newFieldValue(definition, 1, 101, "1000")
                .newFieldValue(definition, 2, 102, "50")
                .newFieldValue(definition, 3, 103, null)
                .newFieldValue(definition, 4, 104, null)
                .newFieldValue(definition, 5, 105, null)
                .build()

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:103"] == "${50 + (50 * 2) + 10}".toString()
        updateMap["id:104"] == "${50 * 2}".toString()
    }

    void "can handle circular dependencies by throwing an error"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aFieldDefinition().id(1).description("field1").build(),
                aFieldDefinition().id(2).description("\$calc.multiply(var.field3,2) \$var.field2").build(),
                aFieldDefinition().id(3).description("\$calc.multiply(var.field2,2) \$var.field3").build(),
        ).build()

        def recordmMsg = RecordmMsgBuilder.aMessage()
                .newFieldValue(definition, 1, 101, "1000")
                .newFieldValue(definition, 2, 102, "50")
                .newFieldValue(definition, 3, 103, null)
                .build()

        when:
        def calculator = new DefinitionCalculator(definition)
        calculator.calculate(recordmMsg)

        then:
        def e = thrown(RuntimeException)
        e.getMessage() == "Cyclic dependency detected, path: fd:2,field-definition-2 -> fd:3,field-definition-3 -> fd:2,field-definition-2"
    }

    void "can calculate with 'previous' calc arg"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aFieldDefinition().id(1).build(),
                aFieldDefinition().id(2).description("\$calc.multiply(previous,2)").build(),
                aFieldDefinition().id(3).description("\$calc.multiply(previous,2)").build(),
        ).build()

        def recordmMsg = RecordmMsgBuilder.aMessage()
                .newFieldValue(definition, 1, 101, "1000")
                .newFieldValue(definition, 2, 102, null)
                .newFieldValue(definition, 3, 103, null)
                .build()

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:102"] == "${1000 * 2}".toString()
        updateMap["id:103"] == "${(1000 * 2) * 2}".toString()
    }

    void "can calculate with subparts"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aFieldDefinition().id(1).description("\$var.total.chickens").build(),
                aFieldDefinition().id(2).description("\$var.total.dogs.small").build(),
                aFieldDefinition().id(3).description("\$var.total.dogs.big").build(),

                aFieldDefinition().id(4).description("\$calc.sum(var.total.dogs)").build(),
                aFieldDefinition().id(5).description("\$calc.sum(var.total)").build(),
        ).build()

        def recordmMsg = RecordmMsgBuilder.aMessage()
                .newFieldValue(definition, 1, 101, "10")
                .newFieldValue(definition, 2, 102, "5")
                .newFieldValue(definition, 3, 103, "2")
                // calcs
                .newFieldValue(definition, 4, 104, null)
                .newFieldValue(definition, 5, 105, null)
                .build()

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:104"] == "${5 + 2}".toString()
        updateMap["id:105"] == "${10 + 5 + 2}".toString()
    }

    void "throw error when calculating instance with definition in invalid state"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aFieldDefinition().id(2).description("\$calc.multiply(previous,2)").build(),
                aFieldDefinition().id(3).description("\$calc.multiply(previous,2)").build(),
        ).build()

        def recordmMsg = RecordmMsgBuilder.aMessage()
                .newFieldValue(definition, 2, 102, null)
                .newFieldValue(definition, 3, 103, null)
                .build()

        when:
        def calculator = new DefinitionCalculator(definition)
        calculator.calculate(recordmMsg)

        then:
        def e = thrown(IllegalStateException)
        e.getMessage() == "[_calc] instanceId=null definition is in invalid state to calculate {{errorMessage:No previous field available for field " +
                "FieldDefinition{id=2, name='field-definition-2', description='\$calc.multiply(previous,2)', duplicable=false, required=null} }}"
    }

    void "ignore invisible fields"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aFieldDefinition().id(1).description("field1").build(),
                aFieldDefinition().id(2).description("\$var.field2").build(),
                aFieldDefinition().id(3).description("\$var.fieldwithsamename").build(),
                aFieldDefinition().id(4).description("\$var.fieldwithsamename \$hint[should not be part of the message]").build(),
                aFieldDefinition().id(5).description("\$calc.multiply(var.field2,var.fieldwithsamename)").build(),
        ).build()

        def recordmMsg = RecordmMsgBuilder.aMessage()
                .newFieldValue(definition, 1, 101, "1000")
                .newFieldValue(definition, 2, 102, "1")
                .newFieldValue(definition, 3, 103, "10")
                .newFieldValue(definition, 5, 105, null)
                .build()

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:105"] == "${1 * 10}".toString()
    }

    void "ignore trailling zeros"() {
        given:
        def definition = aDefinition().fieldDefinitions(
                aNumberFieldDefinition(2).id(1).description("\$var.field1", true).build(),
                aFieldDefinition().id(2).description("\$calc.multiply(var.field1,280,10)").build(),
        ).build()

        def recordmMsg = RecordmMsgBuilder.aMessage()
                .newFieldValue(definition, 1, 101, "0.4")
                .newFieldValue(definition, 2, 102, null)
                .build()

        when:
        def calculator = new DefinitionCalculator(definition)
        def updateMap = calculator.calculate(recordmMsg)

        then:
        updateMap["id:102"] == "1120"
    }
}
