package com.cultofbits.customizations.calc

import com.google.common.cache.*
import java.util.concurrent.TimeUnit

class CalculatorsDefinitionCache {

    private static Cache<String, DefinitionCalculator> cacheOfCalcFieldsForDefinition = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build()

    static DefinitionCalculator getCalculatorForDefinition(eventMsg) {
        String definitionName = eventMsg.type
        return cacheOfCalcFieldsForDefinition.getUnchecked(definitionName)?.with { defCalculator ->
            if (defCalculator?.defVersion == eventMsg.defVersion) {
                return defCalculator

            } else {
                cacheOfCalcFieldsForDefinition.invalidate(definitionName);
                return cacheOfCalcFieldsForDefinition.get(
                        definitionName,
                        { String name -> recordm.getDefinition(name)?.with { r -> new DefinitionCalculator(r.getBody()) } }
                )
            }
        }
    }
}