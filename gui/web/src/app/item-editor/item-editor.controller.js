/**
 * Created by daniel on 2015-08-11.
 */
(function () {
    'use strict';

    angular
        .module('app.itemEditor')
        .controller('ItemEditorController', ItemEditorController);

    ItemEditorController.$inject = ['$timeout', 'itemService', '$scope', 'dialogs'];
    /* @ngInject */
    function ItemEditorController($timeout, itemService, $scope, dialogs) {
        var vm = this;
        vm.editor = null;
        vm.editorLoaded = editorLoaded;
        vm.setMode = setMode;
        vm.modes = ['tree', 'view', 'form', 'code', 'text'];
        vm.search = function() {vm.editor.search(vm.editor.searchBox.dom.search.value);};
        vm.data = {};
        vm.options = {
            mode: 'tree'
        };
        vm.numberOfErrors = 0;
        vm.itemService = itemService;
        vm.save = save;
        vm.change = change;
        vm.itemChanged = {};
        vm.atLeastOneItemChanged = false;

        activate();

        function activate() {
            $scope.$on('closeRequest', function(widgetID) {
                if (vm.atLeastOneItemChanged) {
                    var dialog = dialogs.confirm('Unsaved changes','You have unsaved changes. Do you still want to close?');
                    dialog.result.then(function(btn){
                        $scope.$emit('closeWidget', widgetID);
                    });
                } else {
                    $scope.$emit('closeWidget', widgetID);
                }
            });
        }

        function editorLoaded(editorInstance) {
            vm.editor = editorInstance;
            editorInstance.setName('Selected items');
            actOnSelectionChanges();
            $scope.$on('itemUpdate', function() {change();});
        }

        function actOnSelectionChanges() {
            $scope.$watchCollection(
                function() {
                    return itemService.selected;
                },
                function(nowSelected) {
                    var selected = {};
                    for(var i = 0; i < nowSelected.length; i++) {
                        var item = nowSelected[i];
                        selected[item.name] = item;
                    }
                    vm.data = selected;
                }
            );
        }

        function change() {
            var keys = Object.keys(vm.data);
            var atLeastOneItemChanged = false;
            for(var i = 0; i < keys.length; i++) {
                var key = keys[i];
                if (vm.data.hasOwnProperty(key)) {
                    var editorItem = vm.data[key];
                    var centralItem = itemService.getItem(editorItem.id);
                    var equal = _.isEqual(editorItem, centralItem);
                    vm.itemChanged[editorItem.id] = !equal;
                    if(!equal) {
                        atLeastOneItemChanged = true;
                    }
                }
            }
            vm.atLeastOneItemChanged = atLeastOneItemChanged;
        }

        function save() {
            var keys = Object.keys(vm.data);
            for(var i = 0; i < keys.length; i++) {
                var key = keys[i];
                if (vm.data.hasOwnProperty(key)) {
                    var editorItem = vm.data[key];
                    var centralItem = itemService.getItem(editorItem.id);
                    if (!_.isEqual(editorItem, centralItem)) {
                        angular.extend(centralItem, editorItem);
                        itemService.saveItem(centralItem);
                    }
                }
            }
        }

        function setMode(mode) {
            vm.options.mode = mode;
            if (mode === 'code') {
                $timeout(function() {
                    vm.editor.editor.setOptions({maxLines: Infinity});
                    vm.editor.editor.on('change', function() {
                        $timeout(function() {
                            vm.numberOfErrors = vm.editor.editor.getSession().getAnnotations().length;
                        }, 300);
                    });
                });
            }
        }
    }
})();
