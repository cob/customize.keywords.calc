package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.actionpack.RecordmActionPack
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder

class CalculatorsDefinitionCache {

    protected static Cache<String, DefinitionCalculator> cacheOfCalcFieldsForDefinition = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build()

    static DefinitionCalculator getCalculatorForDefinition(RecordmMsg recordmMsg, RecordmActionPack recordmActionPack, log) {
        String definitionName = recordmMsg.type

        def calculator = getFromCache(definitionName, recordmActionPack, log)

        def sameDefVersion = calculator.defVersion == recordmMsg.definitionVersion
        // log.info("calculator.defVersion:", calculator?.defVersion)
        // log.info("recordmMsg.definitionVersion:", recordmMsg?.definitionVersion)

        if (sameDefVersion) {
            return calculator

        } else {
            cacheOfCalcFieldsForDefinition.invalidate(definitionName);
            return getFromCache(definitionName, recordmActionPack, log)
        }
    }

    private static DefinitionCalculator getFromCache(String definitionName, recordmActionPack, log) {
        cacheOfCalcFieldsForDefinition.get(
                definitionName,
                { recordmActionPack.getDefinition(definitionName)?.with { r -> new DefinitionCalculator(r.getBody(), recordmActionPack, log) } })
    }
}