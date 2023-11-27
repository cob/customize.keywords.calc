package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.actionpack.RecordmActionPack
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import com.google.common.cache.*

class CalculatorsDefinitionCache {

    protected static Cache<String, DefinitionCalculator> cacheOfCalcFieldsForDefinition = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build()

    static DefinitionCalculator getCalculatorForDefinition(RecordmMsg recordmMsg, RecordmActionPack recordmActionPack, log) {
        String definitionName = recordmMsg.type

        def calculator = getFromCache(definitionName, recordmActionPack, log)
        if (calculator.defVersion == recordmMsg.definitionVersion) {
            return calculator

        } else {
            cacheOfCalcFieldsForDefinition.invalidate(definitionName);
            return getFromCache(definitionName, recordmActionPack, log)
        }
    }

    private static DefinitionCalculator getFromCache(String definitionName, recordmActionPack, log) {
        cacheOfCalcFieldsForDefinition.get(
                definitionName,
                { recordmActionPack.getDefinition(definitionName)?.with { r -> new DefinitionCalculator(r.getBody(), log) } })
    }
}