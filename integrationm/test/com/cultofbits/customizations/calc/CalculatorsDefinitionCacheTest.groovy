package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.actionpack.RecordmActionPack
import com.cultofbits.integrationm.service.dictionary.ReusableResponse
import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import org.junit.Test
import spock.lang.Specification

class CalculatorsDefinitionCacheTest extends Specification {

    void setup() {
        CalculatorsDefinitionCache.cacheOfCalcFieldsForDefinition.invalidateAll()
    }

    @Test
    void "can load definition from recordm"() {
        given:
        def msg = new RecordmMsg([
                type               : "dummy-definition",
                "definitionVersion": 1
        ])

        def definition = new Definition().with {
            it.name = "dummy-definition"
            it.version = 1
            it
        }

        def reusableResponse = Mock(ReusableResponse.class)
        reusableResponse.getBody() >> definition

        def recordm = Mock(RecordmActionPack.class)
        recordm.getDefinition("dummy-definition") >> reusableResponse

        when:
        def calculator = CalculatorsDefinitionCache.getCalculatorForDefinition(msg, recordm)

        then:
        calculator.defName == msg.type
        calculator.defVersion == msg.definitionVersion
    }

    @Test
    void "can get latest version of definition from recordm"() {
        given:

        CalculatorsDefinitionCache.cacheOfCalcFieldsForDefinition.put(
                "dummy-definition",
                new DefinitionCalculator(new Definition().with {
                    it.name = "dummy-definition"
                    it.version = 1
                    it
                }))

        def msg = new RecordmMsg([
                type               : "dummy-definition",
                "definitionVersion": 2
        ])

        def definition = new Definition().with {
            it.name = "dummy-definition"
            it.version = 2
            it.fieldDefinitions = [
                    new FieldDefinition().with {
                        it.id = 0; it.name = "field0";
                        it
                    },
            ]
            it
        }

        def reusableResponse = Mock(ReusableResponse.class)
        reusableResponse.getBody() >> definition

        def recordm = Mock(RecordmActionPack.class)
        recordm.getDefinition("dummy-definition") >> reusableResponse

        when:
        def calculator = CalculatorsDefinitionCache.getCalculatorForDefinition(msg, recordm)

        then:
        calculator.defName == msg.type
        calculator.defVersion == 2
    }

}
