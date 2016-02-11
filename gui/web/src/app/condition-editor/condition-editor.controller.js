/**
 * Created by martin on 2016-02-03.
 */
(function () {
    'use strict';

    angular
        .module('app.conditionEditor')
        .controller('ConditionEditorController', ConditionEditorController);

    ConditionEditorController.$inject = ['$timeout', 'itemService', '$scope', 'dialogs', 'dashboardService', 'spServicesService','restService','modelService'];
    /* @ngInject */
    function ConditionEditorController($timeout, itemService, $scope, dialogs, dashboardService, spServicesService, restService,modelService) {
        var vm = this;
        vm.widget = $scope.$parent.widget;
        vm.setMode = setMode;
        vm.modes = ['raw', 'transformations'];
        vm.options = {
            mode: 'raw'
        };
        vm.conditionTypes = [ { label: 'Precondition', isa: 'precondition' },
                              { label: 'Postcondition', isa: 'postcondition' }
                            ];
        vm.checkGuard = checkGuard;
        vm.checkAction = checkAction;
        vm.save = save;
        vm.newAction = newAction;
        vm.deleteAction = deleteAction;
        vm.deleteCondition = deleteCondition;
        vm.newCondition = newCondition;
        vm.sorter = sorter;
        activate();

        function activate() {
            if(vm.widget.storage === undefined) {
                resetWidgetStorage();
            }
            $scope.$on('closeRequest', function() {
                if(vm.widget.storage.okToSave) {
                    var dialog = dialogs.confirm('Confirm close','You have unsaved changes. Do you still want to close?');
                    dialog.result.then(function(){ dashboardService.closeWidget(vm.widget.id); });
                } else {
                    dashboardService.closeWidget(vm.widget.id);
                }
            });

            vm.widget.storage.operations = [];
            actOnSelectionChanges();
        }

        function actOnSelectionChanges() {
            $scope.$watchCollection(
                function() {
                    return itemService.selected;
                },
                function(nowSelected, previouslySelected) {
                    var isSame = (nowSelected.length == previouslySelected.length) && nowSelected.every(function (element, index) {
                        return element === previouslySelected[index];
                    });

                    if(!isSame) {
                        vm.widget.storage.operations = [];
                        for(var i = 0; i < nowSelected.length; i++) {
                            if(nowSelected[i].isa == 'Operation') {
                                var op = { item: nowSelected[i], conditions: [] };
                                _.each(op.item.conditions, function (cond) {
                                    var c = { kind: cond.attributes.kind, guard: '',
                                              parsedGuard: cond.guard,
                                              guardParseError: '', actions: [], deleted: false };
                                    backendPrintGuard(cond.guard, c);
                                    _.each(cond.action, function (act) {
                                        var a = { action: '', parsedAction: act, actionParseError: '' };
                                        backendPrintAction(act, a);
                                        c.actions.push(a);
                                    });
                                    op.conditions.push(c);
                                });
                                vm.widget.storage.operations.push(op);
                            }
                        }
                        updateOkToSave();
                    }
                }
            );
        }

        function resetWidgetStorage() {
            vm.widget.storage = {
                data: {},
                itemChanged: {},
                okToSave: false
            };
        }

        function updateOkToSave() {
            var ok = true;
            _.each(vm.widget.storage.operations, function(op) {
                // check for parse errors
                _.each(op.conditions, function(c) {
                    if(!c.deleted) {
                        if(c.guardParseError!='') ok = false;
                        _.each(c.actions, function(a) {
                            if(a.actionParseError!='') ok = false;
                        });
                    }
                });
            });

            if(ok) {
                // check for actual changes
                var changes = false;
                _.each(vm.widget.storage.operations, function(op) {
                    var item = getNewItem(op);
                    var centralItem = itemService.getItem(item.id);
                    if(!_.isEqual(item, centralItem)) {
                        changes = true;
                    }
                });
                ok = changes;
            }
            vm.widget.storage.okToSave = ok;
        }

        function setMode(mode) {
            vm.options.mode = mode;
            if (mode === 'code') {
            }
        }

        function sorter(c) {
            if(c.kind == 'precondition') return 0;
            if(c.kind == 'postcondition') return 1;
            return 2;
        }

        function newCondition(op,condType) {
            var cond = {isa:'PropositionCondition',guard:{},action:[],attributes:{kind:condType.isa}};
            op.item.conditions.push(cond);
            var c = { kind: cond.attributes.kind, guard: '',
                      parsedGuard: cond.guard,
                      guardParseError: '', actions: [], deleted: false };
            backendParseGuard(c);
            op.conditions.push(c);
        }

        function deleteCondition(cond) {
            cond.deleted=true;
            updateOkToSave();
        }

        function newAction(actions) {
            var a = { action: '', parsedAction: {}, actionParseError: '' };
            actions.push(a);
            backendParseAction(a);
        }

        function deleteAction(actions, act) {
            var i = actions.indexOf(act);
            if(i>=0) actions.splice(i,1);
            updateOkToSave();
        }

        function getNewItem(op) {
            // clone original item, loop through conditions and update (and remove)
            var item = JSON.parse(JSON.stringify(op.item));
            var new_conds = [];
            for(var i = 0; i<op.conditions.length;++i){
                if(!op.conditions[i].deleted) {
                    item.conditions[i].guard = op.conditions[i].parsedGuard;
                    item.conditions[i].action = _.map(op.conditions[i].actions, function(a) { return a.parsedAction; });
                    new_conds.push(item.conditions[i]);
                }
            }
            item.conditions = new_conds;
            return item;
        }

        function save() {
            if(!vm.widget.storage.okToSave) return; // possible to click disabled button

            _.each(vm.widget.storage.operations, function(op) {
                var item = getNewItem(op);
                var centralItem = itemService.getItem(item.id);
                if(!_.isEqual(item, centralItem)) {
                    itemService.saveItem(item);
                }
            });
        }

        function backendParseGuard(cond) {
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "command":"parseGuard",
                    "toParse":cond.guard,
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles": [],
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "PropositionParser");
            });

            return answerF.then(function(serviceAnswer){
                if(_.isUndefined(serviceAnswer.attributes.parseError)) {
                    cond.parsedGuard = serviceAnswer.attributes.proposition;
                    cond.guardParseError = '';
                } else {
                    cond.guardParseError = serviceAnswer.attributes.parseError;
                }
                updateOkToSave();
            });
        }

        function backendParseAction(act) {
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "command":"parseAction",
                    "toParse":act.action,
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles": [],
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "PropositionParser");
            });

            return answerF.then(function(serviceAnswer){
                if(_.isUndefined(serviceAnswer.attributes.parseError)) {
                    act.parsedAction = serviceAnswer.attributes.action;
                    act.actionParseError = '';
                } else {
                    act.actionParseError = serviceAnswer.attributes.parseError;
                }
                updateOkToSave();
            });
        }

        function backendPrintGuard(prop, obj) {
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "command":"printGuard",
                    "toPrint":prop,
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles": [],
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "PropositionParser");
            });

            return answerF.then(function(serviceAnswer){
                obj.guard = serviceAnswer.attributes.print;
            });
        }

        function backendPrintAction(action, obj) {
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "command":"printAction",
                    "toPrint":action,
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles": [],
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "PropositionParser");
            });

            return answerF.then(function(serviceAnswer){
                obj.action = serviceAnswer.attributes.print;
            });
        }

        function checkGuard(cond) {
            backendParseGuard(cond);
        }

        function checkAction(action) {
            backendParseAction(action);
        }
    }
})();
