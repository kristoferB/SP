/**
 * Created by daniel on 2015-08-11.
 */
(function () {
    'use strict';

    angular
        .module('app.itemEditor')
        .controller('ItemEditorController', ItemEditorController);

    ItemEditorController.$inject = ['$timeout', 'itemService', '$scope', 'dialogs', '$sessionStorage'];
    /* @ngInject */
    function ItemEditorController($timeout, itemService, $scope, dialogs, $sessionStorage) {
        var vm = this;
        vm.editor = null;
        vm.editorLoaded = editorLoaded;
        vm.setMode = setMode;
        vm.modes = ['tree', 'view', 'form', 'code', 'text'];
        vm.search = function() {vm.editor.search(vm.editor.searchBox.dom.search.value);};
        vm.options = {
            mode: 'tree'
        };
        vm.storage = $sessionStorage.$default({
            data: {},
            itemChanged: {},
            atLeastOneItemChanged: false
        });
        vm.numberOfErrors = 0;
        vm.itemService = itemService;
        vm.save = save;
        vm.change = change;

        activate();

        function activate() {
            $scope.$on('closeRequest', function(e, widgetID) {
                if (vm.storage.atLeastOneItemChanged) {
                    var dialog = dialogs.confirm('Confirm close','You have unsaved changes. Do you still want to close?');
                    dialog.result.then(function(){
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
                function(nowSelected, previouslySelected) {
                    var isSame = (nowSelected.length == previouslySelected.length) && nowSelected.every(function(element, index) {
                            return element === previouslySelected[index];
                        });
                    if (!isSame) {
                        var selected = {};
                        for(var i = 0; i < nowSelected.length; i++) {
                            var item = nowSelected[i];
                            selected[item.name] = item;
                        }
                        vm.storage.data = selected;
                    }
                }
            );
        }

        function change() {
            var keys = Object.keys(vm.storage.data);
            var atLeastOneItemChanged = false;
            for(var i = 0; i < keys.length; i++) {
                var key = keys[i];
                if (vm.storage.data.hasOwnProperty(key)) {
                    var editorItem = vm.storage.data[key];
                    var centralItem = itemService.getItem(editorItem.id);
                    var equal = _.isEqual(editorItem, centralItem);
                    vm.storage.itemChanged[editorItem.id] = !equal;
                    if(!equal) {
                        atLeastOneItemChanged = true;
                    }
                }
            }
            vm.storage.atLeastOneItemChanged = atLeastOneItemChanged;
        }

        function save() {
            var keys = Object.keys(vm.storage.data);
            for(var i = 0; i < keys.length; i++) {
                var key = keys[i];
                if (vm.storage.data.hasOwnProperty(key)) {
                    var editorItem = vm.storage.data[key];
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
