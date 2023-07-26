#!/usr/bin/env bash
## Usage: ./run-tests.sh [-a] [-t groovy|js]
## Summary: Run unit tests
##
## Options:
##    -a: the nunber of days ahead to generate activitiss
##    -t: apply to only this plan
##    -h: print help information
##
## Examples:
##
##     * Copy confm and recordm confs from cobdemo to /tmp
##     ./generate-activities.sh -d 1 -p 1594

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

all="false"
type=""

while getopts "d:p:rh" optname
do
   case "$optname" in
      "a")
         all="true"
         ;;
      "p")
         type="$OPTARG"
         ;;
      "h")
         print_help="true"
         ;;
      "?")
         echo "Unknown option $OPTARG"
         ;;
      ":")
         echo "No argument value for option $OPTARG"
         ;;
      *)
         # Should not occur
         echo "Unknown error while processing options"
         ;;
   esac
done

if [[ "$print_help" == "true" ]]; then
  cat "$SCRIPT_DIR/run-tests.sh" | grep "^##.*" | sed "s/## //g" | sed "s/##//g"
  exit 1
fi

if [[ "$all" == "false" ]] && [[ "$type" == "" ]]; then
  echo "Running all tests"
  all="true"
fi

if [[ "$all" == "true" ]] || [[ "$type" == "groovy" ]]; then
  if  [[ -f "integrationm/pom.xml" ]]; then
    (cd integrationm && mvn test)
  fi
fi