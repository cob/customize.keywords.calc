package com.cultofbits.customizations.calc

import com.cultofbits.customizations.utils.DefinitionBuilder
import com.cultofbits.customizations.utils.FieldDefinitionBuilder
import com.cultofbits.integrationm.service.actionpack.RecordmActionPack
import com.cultofbits.integrationm.service.dictionary.ReusableResponse
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import spock.lang.Specification

class CalculatorsDefinitionCacheTest extends Specification {

    void setup() {
        CalculatorsDefinitionCache.cacheOfCalcFieldsForDefinition.invalidateAll()
    }

    void "can load definition from recordm"() {
        given: "no definition is cached"

        def definition = DefinitionBuilder.aDefinition().build()

        def msg = new RecordmMsg([
                type               : definition.name,
                "definitionVersion": 1
        ])

        def reusableResponse = Mock(ReusableResponse.class)
        reusableResponse.getBody() >> definition

        def recordm = Mock(RecordmActionPack.class)
        recordm.getDefinition(definition.name) >> reusableResponse

        def log = Object.class

        when:
        def calculator = CalculatorsDefinitionCache.getCalculatorForDefinition(msg, recordm, log)

        then:
        calculator.defName == msg.type
        calculator.defVersion == msg.definitionVersion
    }

    void "can get latest version of definition from recordm"() {
        given: "definition version is different from recordm message"

        def rmActionPack = Mock(RecordmActionPack.class)

        def definitionName = "the definition"

        def definition = DefinitionBuilder.aDefinition().name(definitionName).build()

        CalculatorsDefinitionCache.cacheOfCalcFieldsForDefinition.put(
                definitionName,
                new DefinitionCalculator(definition, rmActionPack))

        def msg = new RecordmMsg([
                type             : definitionName,
                definitionVersion: 2
        ])

        def definitionV2 = DefinitionBuilder.aDefinition()
                .name(definitionName)
                .version(2)
                .fieldDefinitions(FieldDefinitionBuilder.aFieldDefinition().build())
                .build()

        def reusableResponse = Mock(ReusableResponse.class)
        reusableResponse.getBody() >> definitionV2

        rmActionPack.getDefinition(definitionName) >> reusableResponse

        def log = Object.class

        when:
        def calculator = CalculatorsDefinitionCache.getCalculatorForDefinition(msg, rmActionPack, log)

        then:
        calculator.defName == msg.type
        calculator.defVersion == 2
    }
}
