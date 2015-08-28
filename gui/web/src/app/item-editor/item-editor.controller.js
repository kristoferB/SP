/**
 * Created by daniel on 2015-08-11.
 */
(function () {
    'use strict';

    angular
        .module('app.itemEditor')
        .controller('ItemEditorController', ItemEditorController);

    ItemEditorController.$inject = ['$timeout', 'itemService', '$scope'];
    /* @ngInject */
    function ItemEditorController($timeout, itemService, $scope) {
        var vm = this;
        vm.editor = null;
        vm.editorLoaded = editorLoaded;
        vm.setMode = setMode;
        vm.modes = ['tree', 'view', 'form', 'code', 'text'];
        vm.search = function() {vm.editor.search(vm.editor.searchBox.dom.search.value);};
        /*vm.data =  {
            'Array': [1, 2, 3],
            'Boolean': true,
            'Null': null,
            'Number': 123,
            'Object': {
                'a': 'b',
                'c': 'd'
            },
            'String': 'Hello World',
            'String2': 'Hello World 2',
            'String3': 'Hello World 3',
            'String4': 'Hello World 4'
        };*/
        /*vm.json = [
            {
                aString: 'hej'
            },
            {
                aString: 'hej2'
            }
        ];*/
        vm.data = {};
        vm.options = {
            mode: 'tree'
        };
        vm.numberOfErrors = 0;
        vm.itemService = itemService;
        vm.change = change;
        vm.editModel = editModel;

        activate();

        function activate() {
        }

        function editorLoaded(editorInstance) {
            vm.editor = editorInstance;
            editorInstance.setName('Selected items');
            $scope.$watch(
                function() {
                    return itemService.selected;
                },
                function(nowSelected) {
                    var selected = {};
                    //vm.data.splice(0, vm.data.length);
                    //vm.data = angular.extend([], nowSelected);
                    for(var i = 0; i < nowSelected.length; i++) {
                        var item = nowSelected[i];
                        selected[item.name] = item;
                    }
                    vm.data = selected;
                }, true
            );
            /*$scope.$watch(function() {
                return itemService.selected;
            }, function(selected) {
                console.log('Selected items changed.');
                angular.extend(vm.data, selected);
                for(var i = 0; i < selected.length; i++) {
                }
            }, true);*/
        }

        function editModel() {
            vm.data.Number = 164;
        }

        function change() {
            for(var i = 0; i < vm.data.length; i++) {
                var editorItem = vm.data[i];
                var centralItem = itemService.getItem(vm.data[i].id);
                angular.extend(centralItem, editorItem);
                itemService.saveItem(centralItem);
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
