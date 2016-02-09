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
        vm.checkGuard = checkGuard;
        vm.checkAction = checkAction;
        vm.save = save;
        activate();

        function activate() {
            if(vm.widget.storage === undefined) {
                resetWidgetStorage();
            }
            $scope.$on('closeRequest', function() {
                var dialog = dialogs.confirm('Confirm close','You have unsaved changes. Do you still want to close?');
                dialog.result.then(function(){
                    dashboardService.closeWidget(vm.widget.id);
                });});

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
                                              guardParseError: '', actions: [] };
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
                    }
                }
            );
        }

        function resetWidgetStorage() {
            vm.widget.storage = {
                data: {},
                itemChanged: {},
                okToSave: true
            };
        }

        function setMode(mode) {
            vm.options.mode = mode;
            if (mode === 'code') {
            }
        }

        function save() {
            _.each(vm.widget.storage.operations, function(op) {
                // "reassemble" conditions, i.e. loop through and update
                var conds = _.map(op.conditions, function(c) {
                    return { guard: c.parsedGuard,
                             action: _.map(c.actions, function(a) { return a.parsedAction; })
                           };
                });

                var item = op.item;
                for(var i = 0; i<conds.length;++i){
                    item.conditions[i].guard = conds[i].guard;
                    item.conditions[i].action = conds[i].action;
                }

                var centralItem = itemService.getItem(item.id);
                itemService.saveItem(item);
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

        function checkAction(cond) {
            backendParseAction(cond);
        }
    }
})();
