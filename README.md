# customize.keyword.calc

## Depends on IntegrationM 14.3.0

## Install

```bash
cob-cli customize calc
```

## How to use:

```
Fields:
    field:
        name: Number of days
        description: $var.totaldays
        
    field:
        name: Cost per day
        description: $var.costperday    
       
    field:
        name: Total Cost
        description: $calc.multiply(var.totaldays,var.costperday)
```

For more information you can consult [this link](https://learning.cultofbits.com/docs/cob-platform/admins/managing-information/available-customizations/calc/)

## Build & test

```bash
./run-tests.sh
```

## Release

1. Update `costumize.js` and increment version
2. git commit && git push