/**
 * Created by daniel on 2015-08-11.
 */
(function () {
    'use strict';

    angular
        .module('app.itemExplorer')
        .controller('ItemExplorerController', ItemExplorerController);

    ItemExplorerController.$inject = ['logger'];
    /* @ngInject */
    function ItemExplorerController(logger) {
        var vm = this;
        vm.treeInstance = null;
        vm.searchText = '';
        vm.search = function() {vm.treeInstance.jstree(true).search(vm.searchText);};
        vm.expandAll = function() {vm.treeInstance.jstree(true).open_all();};
        vm.collapseAll = function() {vm.treeInstance.jstree(true).close_all();};
        vm.treeConfig = {
            core: {
                check_callback: true
                //animation: 0
            },
            plugins: ['dnd', 'types', 'contextmenu', 'search', 'checkbox'],
            types: {
                '#' : {
                    valid_children: ['root']
                },
                root: {
                    icon : 'images/tree_icon.png'
                },
                operation: {
                    icon: 'images/step_forward.png',
                    valid_children: []
                },
                thing: {
                    icon: 'images/robot.png',
                    valid_children: ['operation']
                },
                spSpec: {
                    icon: 'images/document.png',
                    valid_children: []
                },
                sopSpec: {
                    icon: 'images/hierarchy.png',
                    valid_children: []
                },
                result: {
                    icon: 'images/line_chart.png',
                    valid_children: []
                }
            }
        };
        vm.treeData =  [
            {
                text: 'Root',
                type: 'root',
                state: {opened: true},
                children: [
                    {
                        text: 'PSL OPC Settings',
                        type: 'spSpec'
                    },
                    {
                        text: 'Robot R2',
                        type: 'thing',
                        children: [
                            {
                                text: 'Move to flexlink',
                                type: 'operation'
                            },
                            {
                                text: 'Move to home',
                                type: 'operation'
                            },
                            {
                                text: 'Pick part',
                                type: 'operation'
                            }
                        ]
                    },
                    {
                        text: 'Put together a car',
                        type: 'sopSpec'
                    },
                    {
                        text: 'Energy optimization',
                        type: 'result'
                    }
                ]
            }
        ];

        activate();

        function activate() {
            logger.info('Added an Item Explorer widget');
        }

    }
})();
