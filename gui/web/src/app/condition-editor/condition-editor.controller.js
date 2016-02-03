/**
 * Created by martin on 2016-02-03.
 */
(function () {
    'use strict';

    angular
        .module('app.conditionEditor')
        .controller('ConditionEditorController', ConditionEditorController);

    ConditionEditorController.$inject = ['$timeout', 'itemService', '$scope', 'dialogs', 'dashboardService', 'spServicesService'];
    /* @ngInject */
    function ConditionEditorController($timeout, itemService, $scope, dialogs, dashboardService, spServicesService) {
        var vm = this;
        vm.widget = $scope.$parent.widget;
        vm.setMode = setMode;
        vm.modes = ['tree', 'code'];
        vm.options = {
            mode: 'tree'
        };
        vm.test = test;
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

            actOnSelectionChanges();
            vm.widget.storage.data = {};

            actOnSelectionChanges();
        }

        function actOnSelectionChanges() {
            $scope.$watchCollection(
                function() {
                    return itemService.selected;
                },
                function(nowSelected, previouslySelected) {
                    // do some checks here
                    vm.widget.storage.data = nowSelected;
                }
            );
        }        

        function resetWidgetStorage() {
            vm.widget.storage = {
                data: {},
                itemChanged: {},
                atLeastOneItemChanged: false
            };
        }

        function setMode(mode) {
            vm.options.mode = mode;
            if (mode === 'code') {
            }
        }

        function guardAsText(prop) {
            var operator;
            if(prop.isa === 'EQ' || prop.isa === 'NEQ' || prop.isa === 'GREQ' || prop.isa === 'LEEQ' || prop.isa === 'GR' || prop.isa === 'LE') {
                var left = handleProp(prop.left),
                    right = handleProp(prop.right);
                if(prop.isa === 'EQ') {
                    operator = ' == ';
                } else if(prop.isa === 'NEQ') {
                    operator = ' != ';
                } else if(prop.isa === 'GREQ') {
                    operator = ' >= ';
                } else if(prop.isa === 'LEEQ') {
                    operator = ' <= ';
                } else if(prop.isa === 'GR') {
                    operator = ' > ';
                } else { //prop.isa === 'LE')
                    operator = ' < ';
                }
                if(left === right) {
                    return '';
                } else {
                    return left + operator + right;
                }
            } else if(prop.isa === 'AND' || prop.isa === 'OR') {
                operator = ' ' + prop.isa + ' ';
                var line = '';
                for(var i = 0; i < prop.props.length; i++) {
                    if(i > 0) {
                        line = line + operator;
                    }
                    line = line + handleProp(prop.props[i]);
                }
                return line;
            } else if(prop.isa === 'NOT') {
                return '!' + handleProp(prop.p);
            } else {
                return '';
            }
        };

        function actionAsText(action) {
            var textLine = '';

            for(var i = 0; i < action.length; i++) {
                if(i > 0) {
                    textLine = textLine + '; ';
                }
                var actionValue = false;
                if (action[i].value.isa == "SVIDEval")
                    actionValue = action[i].value.isa.id;
                else actionValue = action[i].value.v;

                textLine = textLine + getNameFromId(action[i].id) + ' = ' + actionValue;
            }
            return textLine;
        };

        function test() {
            _.foreach(vm.widget.storage.data, function (item) {
                
            });
        }
    }
})();
