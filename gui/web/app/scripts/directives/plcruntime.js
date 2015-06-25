'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:plcRuntime
 * @description
 * # plcRuntime
 */
angular.module('spGuiApp')
  .directive('plcRuntime', function (sse, $modal, notificationService, $http, API_URL, spTalker) {
    return {
      templateUrl: 'views/plcruntime.html',
      restrict: 'E',
      scope: {
        ws: '=windowStorage'
      },
      link: function postLink(scope) {

        const runtimeURL = API_URL + '/runtimes/' + scope.ws.runtimeName;

        scope.spTalker = spTalker;
        scope.runtimeExists = true;
        scope.varStates = [];
        scope.opStates = [];
        scope.parseInt = function(string) { return parseInt(string) };
        scope.executing = function(op){
          return op.state === 1 || op.state === 2;
        };
        scope.executable = {
          state: 0,
          preTrue: true
        };
        scope.reverseOptions = [{value: false, label: 'Ascending'}, {value: true, label: 'Descending'}];
        scope.opSearch = {};
        scope.opSortOrder = 'name';
        scope.opReverse = false;
        scope.varSearch = {};
        scope.varSortOrder = 'name';
        scope.varReverse = false;
        scope.expandAll = false;

        function postToRuntime(data, successHandler, errorHandler) {
          $http({
            method: 'POST',
            url: runtimeURL,
            data: data
          }).success(function(data) {
              if(successHandler) successHandler(data);
            })
            .error(function(error, status) {
              if(status === 404)
                scope.runtimeExists = false;
              notificationService.error(error);
              if(errorHandler) errorHandler();
            });
        }

        scope.connect = function() { postToRuntime({action: 'connect'}) };
        scope.disconnect = function() { postToRuntime({action: 'disconnect'}) };
        scope.subscribe = function() { postToRuntime({action: 'subscribe'}) };
        scope.resetOp = function(id) { postToRuntime({action: 'opWrite', id: id, start: false, reset: true}) };
        scope.startOp = function(id) { postToRuntime({action: 'opWrite', id: id, start: true, reset: false}) };
        scope.writeVar = function(id, value) { postToRuntime( { action: 'varWrite', id: id, value: value } ) };

        scope.toggleOp = function(op) {
          if(op.state === 0)
            scope.startOp(op.id);
          else if(op.state === 2)
            scope.resetOp(op.id);
        };

        Object.byString = function(o, s) {
          s = s.replace(/\[(\w+)\]/g, '.$1'); // convert indexes to properties
          s = s.replace(/^\./, '');           // strip a leading dot
          var a = s.split('.');
          for (var i = 0, n = a.length; i < n; ++i) {
            var k = a[i];
            if (k in o) {
              o = o[k];
            } else {
              return;
            }
          }
          return o;
        };

        function itemProperty(id, path, defaultVal, propName) {
          const item = spTalker.getItemById(id);
          if(item) {
            try {
              return Object.byString(item, path);
            } catch(e) {
              notificationService.error(item.name + " misses a " + propName + " property.");
              return defaultVal
            }
          }
          notificationService.error(id + " is not part of your model.");
          return defaultVal
        }

        scope.rangeStart = function(id) { return itemProperty(id, "attributes.stateVariable.range.start", "0", "range start") };
        scope.rangeEnd = function(id) { return itemProperty(id, "attributes.stateVariable.range.end", "99", "range end") };
        scope.rangeStep = function(id) { return itemProperty(id, "attributes.stateVariable.range.step", "1", "range step") };
        scope.stateVarKind = function(id) { return itemProperty(id, "attributes.stateVariable.kind", "", "state variable kind") };
        scope.stateDescription = function(varState) { return itemProperty(varState.id, "attributes.stateVariable.domain[" + varState.value + "]", varState.value, varState.value + " state domain") };
        scope.domain = function(id) { return itemProperty(id, "attributes.stateVariable.domain", [], "domain") };

        scope.toggleVar = function(variable) {
          if(_.isBoolean(variable.value))
            scope.writeVar(variable.id, !variable.value);
          else if(_.isNumber(variable.value))
            scope.writeVar(variable.id, 1);
        };

        scope.isBoolean = function(value) {
          return _.isBoolean(value);
        };

        scope.isNumber = function(value) {
          return _.isNumber(value);
        };

        function setOPC(opcID) {
          if(opcID === "") {
            scope.opcSpec = false;
          } else {
            if(spTalker.items.hasOwnProperty(opcID)) {
              scope.opcSpec = spTalker.items[opcID];
            } else {
              notificationService.error('The runtime is set to an OPCSpec with ID ' + opcID
                + ' which is not part of your current model.');
            }
          }
        }

        function updateRuntimeState(data) {
          setOPC(data.opcSpecID);
          scope.connected = data.connected;
          angular.extend(scope.varStates, data.varStates);
          for(var i = 0; i < scope.varStates.length; i++) {
            scope.varStates[i].name = spTalker.getItemName(scope.varStates[i].id);
            scope.varStates[i].kind = scope.stateVarKind(scope.varStates[i].id);
          }
          angular.extend(scope.opStates, data.opStates);
          for(i = 0; i < scope.opStates.length; i++) scope.opStates[i].name = spTalker.getItemName(scope.opStates[i].id);
        }

        scope.getRuntimeState = function() { postToRuntime({action: 'getRuntimeState'}, updateRuntimeState); };
        scope.getRuntimeState();

        scope.chooseOPCModal = function() {
          $modal.open({
            templateUrl: 'views/chooseopc.html',
            controller: 'ChooseOPCCtrl',
            resolve: {
              runtimeName: function () {
                return scope.ws.runtimeName;
              },
              postToRuntime: function() {
                return postToRuntime;
              }
            }
          });
        };

        scope.chosenOPCText = function() {
          if(scope.opcSpec)
            return scope.opcSpec.name;
          else
            return "No OPC chosen";
        };

        scope.editTags = function() {
          var modal = $modal.open({
            templateUrl: 'views/opctagedit.html',
            controller: 'OPCTagEditCtrl',
            resolve: {
              opcSpec: function () {
                return scope.opcSpec;
              },
              runtimeName: function () {
                return scope.ws.runtimeName;
              }
            }
          });

          modal.result.then(function() {
            scope.subscribe();
          });

        };

        scope.sseEnabled = false;
        scope.sseOpen = false;
        var eventSource;

        scope.opcSpec = false;
        scope.connected = false;
        scope.connecting = false;
        scope.disconnecting = false;

        scope.$on('$destroy', function() {
          if(eventSource)
            eventSource.close();
        });

        function enableSSE() {
          eventSource = new EventSource(runtimeURL + "/sse");
          registerListeners();

          eventSource.onopen = function () {
            scope.$apply(function () {
              scope.sseEnabled = true;
              scope.sseOpen = true;
            });
          };

          eventSource.onerror = function () {
            scope.$apply(function () {
              scope.sseOpen = false;
            });
          };

          function registerListeners() {
            eventSource.onmessage = function(e) {
              console.log(e);
            };

            eventSource.addEventListener('CommandFailed', function(e) {
              scope.$apply(function () {
                scope.connected = false;
                scope.connecting = false;
              });
              const data = JSON.parse(e.data);
              notificationService.error('Failed to connect to ' + data.ip + ':' + data.port);
            });

            eventSource.addEventListener('Connecting', function() {
              scope.$apply(function () {
                scope.connecting = true;
              });
              notificationService.info('Connecting to ' + scope.opcSpec.name);
            });

            eventSource.addEventListener('Connected', function(e) {
              scope.$apply(function () {
                scope.connected = true;
                scope.connecting = false;
              });
              const data = JSON.parse(e.data);
              notificationService.success('Connection established to ' + data.ip + ':' + data.port);
            });

            eventSource.addEventListener('Disconnecting', function() {
              scope.$apply(function () {
                scope.disconnecting = true;
              });
              notificationService.info('Disconnecting from ' + scope.opcSpec.name);
            });

            eventSource.addEventListener('OPCChosen', function(e) {
              setOPC(e.data);
            });

            eventSource.addEventListener('NewOpState', function(e) {
              const newOpState = JSON.parse(e.data);
              const oldOpState = _.find(scope.opStates, function(opState){ return opState.id === newOpState.id; });
              if(_.isObject(oldOpState)) {
                oldOpState.preTrue = newOpState.preTrue;
                oldOpState.state = newOpState.state;
              }
              scope.$digest();
            });

            eventSource.addEventListener('NewVarState', function(e) {
              const newVarState = JSON.parse(e.data);
              const oldVarState = _.find(scope.varStates, function(varState){ return varState.id === newVarState.id; });
              if(_.isObject(oldVarState))
                oldVarState.value = newVarState.value;
              scope.$digest();
            });

            eventSource.addEventListener('Subscribed', function(e) {
              const runtimeState = JSON.parse(e.data);
              updateRuntimeState(runtimeState);
              scope.$digest();
              notificationService.info("Sent subscription request for " + Object.keys(runtimeState.varStates).length +
                " variables and " + Object.keys(runtimeState.opStates).length + " operations.");
            });

            eventSource.addEventListener('ConnectionClosed', function() {
              scope.$apply(function () {
                scope.connected = false;
                scope.disconnecting = false;
              });
              notificationService.success('The runtime was disconnected from the OPC.');
            });

            eventSource.addEventListener('Error', function(e) {
              notificationService.error(e.data);
            });
          }
        }
        enableSSE();

      }
    };
  });