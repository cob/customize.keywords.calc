import com.cultofbits.customizations.calc.CalculatorsDefinitionCache

if (!(msg.user != "integrationm" && msg.product == "recordm" && msg.action =~ "add|update")) return

// ===================================================================================================

def maybeUpdated = CalculatorsDefinitionCache.getCalculatorForDefinition(msg)
        .calculate(msg)

if (maybeUpdated.isPresent()) {
    recordm.update(msg.type, msg.instance.id, maybeUpdated.get());
}
