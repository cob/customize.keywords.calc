package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmMsg

import java.math.RoundingMode

class DefinitionCalculator {

    protected String defName;
    protected Integer defVersion;

    // a map with the key the var name (including var.) and the value the list of all fields
    protected Map<String, List<FieldDefinition>> fdVarsByMapVarName = [:]

    // All field definitions that have $calc.<operation>
    protected Map<Integer, CalcExpr> fdCalcExprMapById = [:]

    // log4j
    private Object log

    DefinitionCalculator(definition) {
        this.defName = definition.name
        this.defVersion = definition.version

        this.processDefinition(definition)
    }

    DefinitionCalculator(definition, log) {
        this.log = log
        this.defName = definition.name
        this.defVersion = definition.version

        this.processDefinition(definition)
    }

    protected processDefinition(Definition definition) {
        // Collect all field definitions with $var.*
        fdVarsByMapVarName = definition.allFields
                .findAll { fd -> fd.description != null && fd.description.indexOf("\$var.") != -1 }
                .inject([:] as Map<String, List<FieldDefinition>>) { map, fd ->
                    def words = fd.description =~ /([^\s]+)/
                    def varArg = (0..words.getCount() - 1)
                            .findResult { i -> words[i][0].indexOf("\$var") != -1 ? words[i][0] : null }
                            .substring(1) // remove the initial $

                    def varArgMapEntry = map[varArg]
                    if (varArgMapEntry != null) {
                        varArgMapEntry << fd
                    } else {
                        map << [(varArg): [fd]]
                    }

                    map
                }

        // Collect all field definitions with $calc.*
        fdCalcExprMapById = definition.allFields
                .findAll { fd -> fd.description =~ /[$]calc\./ }
                .inject([:] as Map<Integer, CalcExpr>) { map, fd ->
                    def op = (fd.description =~ /.*[$]calc.([^(]+)/).with { it[0][1] }
                    def args = (fd.description =~ /.*[$]calc.[^(]+\(([^(]+)\)/).with { it[0][1].tokenize(",") }

                    map << [(fd.id): new CalcExpr().with {
                        it.operation = op
                        it.args = args
                        it
                    }]

                    map
                }
    }

    private consoleLog(message) {
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
        def calcContext = new CalcContext(recordmMsg)

        recordmMsg.instance.getFields().inject([:] as Map<String, String>) { map, Map<String, Object> field ->
            def newValue = getFieldValue(field, null, calcContext, new ArrayList<StackEntry>())

            if (newValue != field.value) {
                map << [("id:${field.id}".toString()): newValue]
            }

            map
        } as Map<String, String>
    }

    /**
     * Calculate a single field value
     */
    String getFieldValue(field, parentField, calcContext, List<StackEntry> stack) {

        def calcExpr = fdCalcExprMapById[field.fieldDefinition.id]

        // There isn't anything to calculate, it is just a normal field
        if (calcExpr == null) {
            return field.value
        }

        if (calcExpr != null && calcContext.cache[field.fieldDefinition.id] != null) {
            return calcContext.cache[field.fieldDefinition.id]
        }

        def stackEntry = new StackEntry(field, parentField)
        if (stack.contains(stackEntry)) {
            // Adding to the stack so we can easily print the path for the cyrcular dependency
            stack << stackEntry
            throw new RuntimeException("Cyclic dependency detected, path: ${stack.collect { it.toString() }.join(" -> ")}")
        }

        stack << stackEntry

        // it is a $calc but it hasn't been calculated yet.
        def argValues = calcExpr.args.collect { arg ->
            !arg.startsWith("var") ? [arg] : fdVarsByMapVarName[arg].collect { fd ->
                calcContext.fieldMapByFieldDefId[fd.id]
                        ?.findAll { it != null } // only calculate fields that are part of the message
                        .collect { getFieldValue(it, field, calcContext, stack) }
            }.flatten()
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
                throw new IllegalArgumentException("Unknown operation ")
        }

        result = result.stripTrailingZeros().toPlainString()
        calcContext.cache[field.fieldDefinition.id] = result

        consoleLog("_calc instanceId=${calcContext.recordmMsg.instance.id} " +
                "fieldId=${field.id} fieldDefinitionName=${field.fieldDefinition.name} " +
                "operation=${calcExpr.operation} " +
                "args=${calcExpr.args} " +
                "argValues=${argValues} " +
                "flattenArgValues=${flattenArgValues} " +
                "result=${result}")

        result
    }

    /**
     * Auxiliary class that represents the operation to perform
     */
    static class CalcExpr {
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
        RecordmMsg recordmMsg
        Map<Integer, List<Map<String, Object>>> fieldMapByFieldDefId
        Map<Integer, BigDecimal> cache

        CalcContext(recordmMsg) {
            this.recordmMsg = recordmMsg
            this.cache = [:]
            this.fieldMapByFieldDefId = recordmMsg.instance.getFields().inject([:] as Map<Integer, List<Map<String, Object>>>) { map, field ->
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

    static class StackEntry {

        def field
        def parentField

        StackEntry(field, parentField) {
            this.field = field
            this.parentField = parentField
        }

        boolean equals(o) {
            if (this.is(o)) return true
            if (o == null || getClass() != o.class) return false

            StackEntry that = (StackEntry) o

            if (field != that.field) return false
            if (parentField != that.parentField) return false

            return true
        }

        int hashCode() {
            int result
            result = (field != null ? field.hashCode() : 0)
            result = 31 * result + (parentField != null ? parentField.hashCode() : 0)
            return result
        }


        @Override
        String toString() {
            return "fd:${field.fieldDefinition.id},${field.fieldDefinition.name}"
        }
    }

}