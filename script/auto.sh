#!/usr/bin/env bash

# auto helper script

# possible actions
_ACTIONS="help,opts,docker,setup,tests,all,clean"

# colors
_RED=""
_GREEN=""
_RESET=""
_YELLOW=""

# action: default start
_ACTION="tests"

# flags
_YES_FLAG=1

# default dbms
_DBMS=postgresql

# compose variables: file and service
_COMPOSE_FILE=./docker/docker-compose.yml
_COMPOSE_SERVICE_LIST=("postgresql" "mysql")
_COMPOSE_SERVICE=$_DBMS
_COMPOSE_PROFILE=$_DBMS

# default edn config file
_CONFIG=./config/test/config.edn

# sql script
_DB_USER=postgres
_DB_NAME=framework
_DB_SCRIPT=/sql/postgresql/test.sql

usage ()
{
    # print help message
    printf $_GREEN
    cat >&1 <<EOF

    [ AUTO HELPER SCRIPT ]

    usage: ${0} [-yxve] [-C {edn_config}] [-D [postgres|mysql]] {ACTION}

    ACTIONS:

    help                  show this message and exit
    opts                  show default options
    docker                setup selected database docker instance
    setup                 execute defined sql scripts
    tests                 run the local tests
    all                   execute docker, setup and tests: shortcut
    clean                 stop the docker instance

    OPTIONS:

    -x                    enables debug capability
    -v                    enables verbose capability
    -y                    answer 'YES' for all questions
    -e                    exit immediately if any untested command fails in
                          non-interactive mode (not recommended)

    -C EDN_CONFIG         set the edn configuration file
                          default: ./config/test/test.edn

    -D DBMS               set the dbms to use. Options: postgresql, mysql, all
                          default: postgres

EOF
    printf $_RESET
}

set_colors ()
{
    # True if the file whose file descriptor number is
    # file_descriptor is open and is associated with a terminal.
    # test(1)
    test -t 1 && {
        _RED="\033[31m"
        _GREEN="\033[32m"
        _YELLOW="\033[33m"
        _RESET="\033[m"
    }
}

# logs, printf wrapper
log () { printf "$@"; }

# error
log_error () { printf $_RED; log "$@"; printf $_RESET; }

# success
log_ok () { printf $_GREEN; log "$@"; printf $_RESET; }

# fatal, die: logs error and exit
die () { log_error "[-] Fatal: $@"; exit 1; }

# help message related to options
show_opts ()
{
    log "
[*] Options Values:

ACTION    = ${_ACTION}
CONFIG    = ${_CONFIG}
DB_USER   = ${_DB_USER}
DB_NAME   = ${_DB_NAME}
DB_SCRIPT = ${_DB_SCRIPT}
DBMS      = ${_DBMS}
\n"
}

# help message related to actions
show_actions ()
{
    # local field separator
    local IFS=","

    # help message
    log_ok "available actions: "

    # show available actions
    for _ in ${_ACTIONS}; do
        log "${_} "
    done

    # keep good and clean (layout)
    log "\n"
}

check_action ()
{
    # local field separator
    local IFS=","

    # find action
    for _ in ${_ACTIONS}; do
        # found
        test "${_}" = "${_ACTION}" && return 0
    done

    # logs and leave
    log_error "action not found\n"

    # help message and leave
    show_actions && exit 1
}

# parse user options
parse_opts ()
{
    # get options
    for opt in "$@"; do
        case $opt in
            # enable debugging
            -x) shift 1; set -x ;;

            # enable verbose
            -v) shift 1; set -v ;;

            # enable errexit
            -e) shift 1; set -e ;;

            # set YES to the answers
            -y) shift 1; _YES_FLAG=0 ;;

            # set edn config file
            -C) shift 1; _CONFIG=${1}; shift 1 ;;

            # set DBMS
            -D) shift 1; _DBMS=${2}; shift 1 ;;
        esac
    done

    # set action
    _ACTION=${1}

    # verify action argument
    check_action
}

# show values and operations that will be executed to the user
# before execute them
confirm_opts ()
{
    # if yes flag equal true (0), don't ask just leave
    test ${_YES_FLAG} -eq 0 && \
        return 0

    # show question
    log "[*] [h] help\n"
    log "[*] Can we proceed? [y/n/h] : "

    while read _answer;
    do
        case $_answer in
            # possible answers: (yes, no, help)
            y|yes) return 0 ;;

            # just leave
            n|no) log "\n"; exit 0 ;;

            # show help and leave
            h|help) usage; exit 0 ;;

            # undefined: asks again
            *) log "[*] Can we proceed? [y/n/h] : " ;;
        esac
    done
}

# handle information actions
handle_info ()
{
    # switch/case/try/catch equivalent
    case $_ACTION in
        # show usage (help) and force leave
        help) usage; exit 0 ;;

        # show options
        opts) show_opts ;;

        # not handled here, continue
        *) return 0 ;;
    esac

    # handled
    exit 0
}



_compose_up ()
{
    # up: create and instantiate postgre service
    COMPOSE_PROFILES=${_COMPOSE_PROFILE} docker-compose -f ${_COMPOSE_FILE} up -d
}

_select_init_command ()
{
  local _cs=$1
  case ${_cs} in
      "postgresql") return "psql -U $_DB_USER -f /sql/postgresql/init.sql" ;;
      "mysql") return "mysql -u $_DB_USER -p < /sql/mysql/init.sql" ;;
  esac
}

_select_test_command ()
{
  local _cs=$1
  case ${_cs} in
      "postgresql") return "psql -U $_DB_USER -f /sql/postgresql/test.sql" ;;
      "mysql") return "mysql -u $_DB_USER -p < /sql/mysql/test.sql" ;;
  esac
}

_compose_exec ()
{
    local _cmd=$1
    local _compose_service=$2

    docker-compose -f $_compose_service exec $_compose_service bash -c "$_cmd"
}

_db_init_script ()
{
    if [ "${_COMPOSE_SERVICE}" == "all" ]; then
        for _cs in ${_COMPOSE_SERVICE_LIST}; do
            _command=$_select_init_command ${_cs}
            _compose_exec $_command $_cs 
        done
    else
        _command=$_select_init_command ${_COMPOSE_SERVICE}
        _compose_exec $_command ${_COMPOSE_SERVICE}
    fi
}

_db_test_script ()
{
    if [ "${_COMPOSE_SERVICE}" == "all" ]; then
        for _cs in ${_COMPOSE_SERVICE_LIST}; do
            _command=$_select_test_command ${_cs}
            _compose_exec $_command $_cs 
        done
    else
        _command=$_select_test_command ${_COMPOSE_SERVICE}
        _compose_exec $_command ${_COMPOSE_SERVICE}
    fi
}

_setup ()
{
    _db_init_script && _db_test_script
}

_tests ()
{
    env XIANA_CONFIG=${_CONFIG} clj -M:test
}

_stops ()
{
    docker-compose -f ${_COMPOSE_FILE} stop
}

_all ()
{
    # execute setup cycle
    _compose_up && _setup && _tests
}

# select an action and execute it associated function
handle_action ()
{
    # switch/case/try/catch equivalent
    case $_ACTION in
        # create/instantiate postgres service
        docker) _compose_up ;;
        # execute the initialization scripts
        setup) _setup ;;
        # set the environment and call clj test
        tests) _tests ;;
        # run all
        all) _all ;;
        # stops the docker instance
        clean) _stops ;;
        # action not found (probably will never reach this portion of the code)
        # just leave it here to handle unexpected errors
        *) die "invalid action, $0 help for more information" ;;
    esac
}

# main entry point
main ()
{
    # get/set user options (command-line)
    parse_opts "$@"

    # handle information actions (opcodes)
    handle_info

    # show options
    show_opts

    # asks for confirmation
    confirm_opts

    # select build related actions and handle it
    handle_action
}

# setup colors variables
set_colors

# main routine
main "$@"
