package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.actionpack.RecordmActionPack
import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmInstance
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg

import java.lang.reflect.Field
import java.math.RoundingMode

class DefinitionCalculator {

    private DEBUG = false

    protected String defName;
    protected Integer defVersion;

    // a map with the key the var name (including var.) and the value the list of all fields
    protected Map<String, List<FieldDefinition>> fdVarsMapByVarName = [:]

    // All field definitions that have $calc.<operation>
    protected Map<Integer, CalcExpr> fdCalcExprMapById = [:]

    // If the
    protected boolean invalidState = false;
    protected String invalidStateMsg = null;

    // log4j
    private Object log
    private final RecordmActionPack rmActionPack

    DefinitionCalculator(definition, RecordmActionPack rmActionPack) {
        this.defName = definition.name
        this.defVersion = definition.version

        this.rmActionPack = rmActionPack

        this.processDefinition(definition)
    }

    DefinitionCalculator(definition, RecordmActionPack rmActionPack, log) {
        this.defName = definition.name
        this.defVersion = definition.version

        this.log = log
        this.rmActionPack = rmActionPack

        this.processDefinition(definition)
    }

    protected processDefinition(Definition definition) {
        def allFields = definition.findFields { true }

        def previousFieldDefinition

        for (FieldDefinition fd : allFields) {
            def isVar = fd.description?.indexOf("\$var.") != -1
            if (isVar) {
                def words = fd.description =~ /([^\s]+)/
                def varArg = (0..words.getCount() - 1)
                        .findResult { i -> words[i][0].indexOf("\$var") != -1 ? words[i][0] : null }
                        ?.substring(1) // remove the initial $

                def varArgMapEntry = fdVarsMapByVarName[varArg]
                if (varArgMapEntry != null) {
                    varArgMapEntry << fd
                } else {
                    fdVarsMapByVarName << [(varArg): [fd]]
                }
            }

            def isCalc = fd.description =~ /[$]calc\./
            if (isCalc) {
                def op = (fd.description =~ /.*[$]calc.([^(]+)/).with { it[0][1] }
                def args = (fd.description =~ /.*[$]calc.[^(]+\(([^(]+)\)/).with { it[0][1].tokenize(",") }

                def finalArgs = []

                for (String arg : args) {
                    if (arg == "previous") {
                        if (previousFieldDefinition != null) {
                            finalArgs << "previous:${previousFieldDefinition.id}"

                        } else {
                            invalidState = true
                            invalidStateMsg = "No previous field available for field ${fd}"
                            return
                        }
                    } else if (arg.startsWith("var")) {
                        finalArgs.addAll(
                                definition.findFields { it.description =~ /.*[$]$arg.*/ ? true : false }
                                        .collect { it -> extractVar(it) }
                        )
                    } else {
                        finalArgs << arg.trim()
                    }
                }

                fdCalcExprMapById << [(fd.id): new CalcExpr().with {
                    it.fieldDefinition = fd
                    it.operation = op
                    it.args = finalArgs.unique()
                    it
                }]
            }

            previousFieldDefinition = fd
        }
    }

    private extractVar(FieldDefinition fd) {
        def words = fd.description =~ /([^\s]+)/
        def varArg = (0..words.getCount() - 1)
                .findResult { i -> words[i][0].indexOf("\$var") != -1 ? words[i][0] : null }
                ?.substring(1) // remove the initial $

        return varArg
    }

    private logMessage(message) {
        if (!DEBUG) return

        if (log != null) {
            log.info(message)

        } else {
            println(message)
        }
    }

    /**
     * Perform calculation in the necessary instance fields
     * @param recordmMsg the recordm event message
     * @return a Map with all the updated fields
     */
    Map<String, String> calculate(RecordmMsg recordmMsg) {
        if (invalidState) {
            throw new IllegalStateException("[_calc] instanceId=${recordmMsg.id} definition is in invalid state to calculate {{" +
                    "errorMessage:${invalidStateMsg} }}")
        }

        if (fdCalcExprMapById.isEmpty()) {
            return [:]
        }

        def instance = rmActionPack.get(recordmMsg.id).getBody()
        def calcContext = new CalcContext(instance)

        logMessage("[_calc] instanceId=${calcContext.recordmInstance.id} \n" +
                "fdVarsMapByVarName=\n    " + fdVarsMapByVarName.collect { k, v -> "${k} -> ${v.id}" }.join("\n    ") + "\n\n" +
                "fieldMapByFieldDefId=\n    " + calcContext.fieldMapByFieldDefId.collect { k, v -> "${k} -> ${v}" }.join("\n    ") + "\n" +
                "fdCalcExprMapById=\n    " + fdCalcExprMapById.collect { k, v -> "${k} -> ${v}" }.join("\n    ")
        )

        instance.getFields().inject([:] as Map<String, String>) { map, Map<String, Object> field ->
            def newValue = getFieldValue(field, calcContext, new ArrayList())

            if (newValue != field.value) {
                map << [("id:${field.id}".toString()): newValue]
            }

            map
        } as Map<String, String>
    }

    /**
     * Calculate a single field value
     */
    String getFieldValue(field, calcContext, List stack) {

        def calcExpr = fdCalcExprMapById[field.fieldDefinition.id]

        if (calcExpr == null) {
            return field.value
        }

        if (calcExpr != null && calcContext.cache[field.fieldDefinition.id] != null) {
            return calcContext.cache[field.fieldDefinition.id]
        }

        if (stack.contains(field)) {
            // Adding to the stack so we can easily print the path for the circular dependency
            stack << field
            throw new RuntimeException("Cyclic dependency detected, path: ${stack.collect { "fd:${it.fieldDefinition.id},${it.fieldDefinition.name}" }.join(" -> ")}")
        }

        stack << field

        // it is a $calc but it hasn't been calculated yet.
        def argValues = calcExpr.args.collect { arg ->
            def possibleValues

            if (arg =~ "previous:\\d+") {
                possibleValues = [calcContext.fieldMapByFieldDefId[arg.replaceAll("previous:", "").toInteger()]
                                          ?.findAll { it != null }
                                          .collect { getFieldValue(it, calcContext, stack) }]

            } else if (arg.startsWith("var")) {
                possibleValues = fdVarsMapByVarName[arg].collect { fd ->
                    calcContext.fieldMapByFieldDefId[fd.id]
                            ?.findAll { it != null } // only calculate fields that are part of the message
                            .collect { getFieldValue(it, calcContext, stack) }
                }
            } else {
                possibleValues = [arg]
            }

            possibleValues.flatten()
        }

        def result = new BigDecimal(0)
        def flattenArgValues = argValues
                .flatten()
                .findAll { it != null }
                .collect { new BigDecimal(it ?: 0) }

        switch (calcExpr.operation) {
            case "multiply":
                result = 1
                flattenArgValues.each { result = result.multiply(it) }
                break;
            case "divide":
                if (flattenArgValues.size() == 2 && (flattenArgValues[1] ?: 0 != 0)) {
                    result = flattenArgValues[0]
                    result = result.divide(flattenArgValues[1], 8, RoundingMode.HALF_UP)
                }
                break;
            case "sum":
                flattenArgValues.each { result = result + it }
                break;
            case "subtract":
                if (flattenArgValues.size() == 2) {
                    result = flattenArgValues[0]
                    result = result.subtract(flattenArgValues[1])
                }
                break;
            case "diffDays":
                if (flattenArgValues.size() == 2) {
                    result = flattenArgValues[0]
                    result = result.subtract(flattenArgValues[1])
                    result = result.divide(new BigDecimal(24 * 60 * 60 * 1000), 8, RoundingMode.HALF_UP)
                }
                break;
            case "diffHours":
                if (flattenArgValues.size() == 2) {
                    result = flattenArgValues[0]
                    result = result.subtract(flattenArgValues[1])
                    result = result.divide(new BigDecimal(60 * 60 * 1000), 8, RoundingMode.HALF_UP)

                }
                break;
            case "diifMinutes":
                if (flattenArgValues.size() == 2) {
                    result = flattenArgValues[0]
                    result = result.subtract(flattenArgValues[1])
                    result = result.divide(new BigDecimal(60 * 1000), 8, RoundingMode.HALF_UP)
                }
                break;
            default:
                throw new IllegalArgumentException("[_calc] Unknown operation instance instanceId=${calcContext.recordmMsg.instance.id} " +
                        "operation=${calcExpr.operation}")
        }

        result = result.stripTrailingZeros().toPlainString()
        calcContext.cache[field.fieldDefinition.id] = result

        logMessage("[_calc] instanceId=${calcContext.recordmInstance.id} \n" +
                "calcExpr=${calcExpr}" +
                "\n   fieldId=${field.id} fieldDefinitionName=${field.fieldDefinition.name} " +
                "\n   args=${calcExpr.args} " +
                "\n   fields=${calcExpr.args.collect { arg -> fdVarsMapByVarName[arg].collect { fd -> calcContext.fieldMapByFieldDefId[fd.id].collect { it.id }.join(',') } }} " +
                "\n   argValues=${argValues} " +
                "\n   flattenArgValues=${flattenArgValues} " +
                "\n   result=${result}")

        result
    }

    /**
     * Auxiliary class that represents the operation to perform
     */
    static class CalcExpr {
        FieldDefinition fieldDefinition;

        // one of: sum, multiply, subtract, divide, diffHours, diifMinutes, diffDays
        String operation;

        // All the arguments from the $calc expression like var.xxxx or any constant value
        List<String> args;

        @Override
        String toString() {
            return "calc.${operation}(${args.join(",")})"
        }
    }

    /**
     * Auxiliary class that holds instance information and provides methods to facilitate the calculation
     */
    static class CalcContext {
        Map<Integer, List<Map<String, Object>>> fieldMapByFieldDefId
        Map<Integer, BigDecimal> cache
        RecordmInstance recordmInstance

        CalcContext(recordmInstance) {
            this.cache = [:]
            this.recordmInstance = recordmInstance
            this.fieldMapByFieldDefId = recordmInstance.getFields().inject([:] as Map<Integer, List<Map<String, Object>>>) { map, field ->
                def fieldDefIdEntry = map[field.fieldDefinition.id]
                if (fieldDefIdEntry != null) {
                    fieldDefIdEntry << field
                } else {
                    map << [(field.fieldDefinition.id): [field]]
                }
                map
            }
        }
    }
}