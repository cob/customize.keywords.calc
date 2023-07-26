package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.actionpack.RecordmActionPack
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg
import com.google.common.cache.*
import java.util.concurrent.TimeUnit

class CalculatorsDefinitionCache {

    private static Cache<String, DefinitionCalculator> cacheOfCalcFieldsForDefinition = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build()

    static DefinitionCalculator getCalculatorForDefinition(RecordmMsg recordmMsg, RecordmActionPack recordmActionPack) {
        String definitionName = recordmMsg.type
        return cacheOfCalcFieldsForDefinition.getUnchecked(definitionName)?.with { defCalculator ->
            if (defCalculator?.defVersion == recordmMsg.definitionVersion) {
                return defCalculator

            } else {
                cacheOfCalcFieldsForDefinition.invalidate(definitionName);
                return cacheOfCalcFieldsForDefinition.get(
                        definitionName,
                        { recordmActionPack.getDefinition(definitionName)?.with { r -> new DefinitionCalculator(r.getBody()) } }
                )
            }
        }
    }
}