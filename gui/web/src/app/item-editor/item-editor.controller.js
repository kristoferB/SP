/**
 * Created by daniel on 2015-08-11.
 */
(function () {
    'use strict';

    angular
        .module('app.itemEditor')
        .controller('ItemEditorController', ItemEditorController);

    ItemEditorController.$inject = ['$timeout', 'itemService', '$scope', 'dialogs', 'dashboardService', 'spServicesService'];
    /* @ngInject */
    function ItemEditorController($timeout, itemService, $scope, dialogs, dashboardService, spServicesService) {
        var vm = this;
        vm.widget = $scope.$parent.widget;
        vm.editor = null;
        vm.editorLoaded = editorLoaded;
        vm.setMode = setMode;
        vm.modes = ['tree', 'code'];
        vm.search = function() {vm.editor.search(vm.editor.searchBox.dom.search.value);};
        vm.options = {
            mode: 'tree'
        };
        vm.numberOfErrors = 0;
        vm.itemService = itemService;
        vm.save = save;
        vm.change = change;
        vm.inSync = true;
        vm.unSync = unSync;
        vm.showDetail = false;
        vm.transformService = "";

        activate();

        function activate() {
            if(vm.widget.storage === undefined) {
                resetWidgetStorage();
            }
            $scope.$on('closeRequest', function() {
                if (vm.widget.storage.atLeastOneItemChanged) {
                    var dialog = dialogs.confirm('Confirm close','You have unsaved changes. Do you still want to close?');
                    dialog.result.then(function(){
                        dashboardService.closeWidget(vm.widget.id);
                    });
                } else {
                    dashboardService.closeWidget(vm.widget.id);
                }
            });
            listenToChanges();

            vm.widget.storage.data = {o1: {
                isa: "operation",
                name: "o1"
            }
            };
        }

        function resetWidgetStorage() {
            vm.widget.storage = {
                data: {},
                itemChanged: {},
                atLeastOneItemChanged: false
            };
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
                    if (vm.inSync && !vm.widget.storage.atLeastOneItemChanged) {
                        var isSame = (nowSelected.length == previouslySelected.length) && nowSelected.every(function (element, index) {
                              return element === previouslySelected[index];
                          });
                        if (!isSame) {
                            var selected = {};
                            for (var i = 0; i < nowSelected.length; i++) {
                                var item = nowSelected[i];
                                selected[item.name] = item;
                            }
                            vm.widget.storage.data = selected;
                        }
                    }
                }
            );
        }

        function change() {
            if (vm.inSync) {
                var keys = Object.keys(vm.widget.storage.data);
                var atLeastOneItemChanged = false;
                for (var i = 0; i < keys.length; i++) {
                    var key = keys[i];
                    if (vm.widget.storage.data.hasOwnProperty(key)) {
                        var editorItem = vm.widget.storage.data[key];
                        var centralItem = itemService.getItem(editorItem.id);
                        var equal = _.isEqual(editorItem, centralItem);
                        vm.widget.storage.itemChanged[editorItem.id] = !equal;
                        if (!equal) {
                            atLeastOneItemChanged = true;
                        }
                    }
                }
                vm.widget.storage.atLeastOneItemChanged = atLeastOneItemChanged;
            } else {

            }
        }
        function save() {
            if (vm.inSync) {
                var keys = Object.keys(vm.widget.storage.data);
                for (var i = 0; i < keys.length; i++) {
                    var key = keys[i];
                    if (vm.widget.storage.data.hasOwnProperty(key)) {
                        var editorItem = vm.widget.storage.data[key];
                        var centralItem = itemService.getItem(editorItem.id);
                        if (!_.isEqual(editorItem, centralItem)) {
                            //angular.extend(centralItem, editorItem);
                            itemService.saveItem(editorItem);
                        }
                    }
                }
                vm.widget.storage.atLeastOneItemChanged = false;
            } else {
                console.log("call service")
                spServicesService.callService(spServicesService.getService(vm.transformService), {data: vm.widget.storage.data}, response)
            }
            function response(event){
                vm.widget.storage.data = event;
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

        function listenToChanges() {
            $scope.$on('itemsFetch', function() {
                if (vm.inSync) {
                    resetWidgetStorage();
                }
            });
        }

        function unSync(){
            vm.widget.storage.atLeastOneItemChanged = true;
        }
    }
})();
