import com.cultofbits.customizations.calc.CalculatorsDefinitionCache

if (!(msg.user != "integrationm" && msg.product == "recordm" && msg.action =~ "add|update")) return

// ===================================================================================================

def updateMap = CalculatorsDefinitionCache.getCalculatorForDefinition(msg, recordm)
        .calculate(msg)

if (updateMap.size() > 0) {
    recordm.update(msg.type, msg.instance.id, updateMap);
}
