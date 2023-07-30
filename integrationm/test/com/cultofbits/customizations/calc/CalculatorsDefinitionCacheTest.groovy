package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.actionpack.RecordmActionPack
import com.cultofbits.integrationm.service.dictionary.ReusableResponse
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import spock.lang.Specification

import static com.cultofbits.customizations.utils.RmHelper.aDefinition
import static com.cultofbits.customizations.utils.RmHelper.aFieldDefinition

class CalculatorsDefinitionCacheTest extends Specification {

    void setup() {
        CalculatorsDefinitionCache.cacheOfCalcFieldsForDefinition.invalidateAll()
    }

    void "can load definition from recordm"() {
        given: "no definition is cached"
        def msg = new RecordmMsg([
                type               : "definition 1",
                "definitionVersion": 1
        ])

        def definition = aDefinition()

        def reusableResponse = Mock(ReusableResponse.class)
        reusableResponse.getBody() >> definition

        def recordm = Mock(RecordmActionPack.class)
        recordm.getDefinition("definition 1") >> reusableResponse

        def log = Object.class

        when:
        def calculator = CalculatorsDefinitionCache.getCalculatorForDefinition(msg, recordm, log)

        then:
        calculator.defName == msg.type
        calculator.defVersion == msg.definitionVersion
    }

    void "can get latest version of definition from recordm"() {
        given: "definition version is different from recordm message"

        CalculatorsDefinitionCache.cacheOfCalcFieldsForDefinition.put(
                "definition 1",
                new DefinitionCalculator(aDefinition()))

        def msg = new RecordmMsg([
                type             : "definition 1",
                definitionVersion: 2
        ])

        def definition = aDefinition(aFieldDefinition(0, "field0", null))
        definition.version = 2

        def reusableResponse = Mock(ReusableResponse.class)
        reusableResponse.getBody() >> definition

        def recordm = Mock(RecordmActionPack.class)
        recordm.getDefinition("definition 1") >> reusableResponse

        def log = Object.class

        when:
        def calculator = CalculatorsDefinitionCache.getCalculatorForDefinition(msg, recordm, log)

        then:
        calculator.defName == msg.type
        calculator.defVersion == 2
    }

}
