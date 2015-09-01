/**
 * Created by daniel on 2015-08-11.
 */
(function () {
    'use strict';

    angular
        .module('app.itemExplorer')
        .controller('ItemExplorerController', ItemExplorerController);

    ItemExplorerController.$inject = ['$scope', 'logger', 'itemService', '$modal'];
    /* @ngInject */
    function ItemExplorerController($scope, logger, itemService, $modal) {
        var vm = this;
        vm.treeInstance = null;
        vm.searchText = '';
        vm.itemService = itemService;
        vm.search = function() {vm.treeInstance.jstree(true).search(vm.searchText);};
        vm.expandAll = function() {vm.treeInstance.jstree(true).open_all();};
        vm.collapseAll = function() {vm.treeInstance.jstree(true).close_all();};
        vm.selectAll = function() {vm.treeInstance.jstree(true).select_all();};
        vm.deselectAll = function() {vm.treeInstance.jstree(true).deselect_all();};
        vm.createItem = createItem;
        vm.onTreeReady = onTreeReady;
        vm.onNodeMove = onNodeMove;
        vm.onNodeCopy = onNodeCopy;
        vm.onSelectionChange = onSelectionChange;
        vm.itemKinds = [
            {value: 'Operation', label: 'Operation'},
            {value: 'Thing', label: 'Thing'},
            {value: 'SOPSpec', label: 'SOP Spec'},
            {value: 'SPSpec', label: 'SP Spec'},
            {value: 'HierarchyRoot', label: 'Hierarchy Root'}
        ];
        vm.treeConfig = {
            contextmenu: {
                items: contextMenuItems
            },
            core: {
                check_callback: checkCallback
            },
            plugins: ['dnd', 'types', 'contextmenu', 'search'],
            types: {
                '#' : {
                    valid_children: ['Root', 'AllItemsRoot']
                },
                Root: {
                    icon : 'images/tree_icon.png'
                },
                AllItemsRoot: {
                    icon: 'images/all_items.png'
                },
                Operation: {
                    icon: 'images/step_forward.png',
                    valid_children: []
                },
                Thing: {
                    icon: 'images/robot.png',
                    valid_children: ['operation']
                },
                SPSpec: {
                    icon: 'images/document.png',
                    valid_children: []
                },
                SOPSpec: {
                    icon: 'images/hierarchy.png',
                    valid_children: []
                },
                Result: {
                    icon: 'images/line_chart.png',
                    valid_children: []
                }
            }
        };

        activate();

        function activate() {
            $scope.$on('closeRequest', function(e, widgetID) {
                $scope.$emit('closeWidget', widgetID);
            });
        }

        function onTreeReady() {
            rebuildTree();
            listenToChanges();
        }

        function onSelectionChange(e, data) {
            itemService.selected.splice(0, itemService.selected.length);
            for(var i = 0; i < data.selected.length; i++) {
                var nodeID = data.selected[i];
                if (nodeID !== 'all-items') {
                    var node = vm.treeInstance.jstree(true).get_node(nodeID);
                    if (node.type !== 'Root') {
                        var itemID = node.data.id;
                        var item = itemService.getItem(itemID);
                        itemService.selected.push(item);
                    }
                }
            }
        }

        function createItem(itemKind) {
            var modalInstance = $modal.open({
                templateUrl: '/app/item-explorer/create-item.html',
                controller: 'CreateItemController',
                controllerAs: 'vm',
                resolve: {
                    itemKind: function () {
                        return itemKind;
                    }
                }
            });

            modalInstance.result.then(function(chosenName) {
                itemService.createItem(chosenName, itemKind.value);
            });
        }

        function getRootID(node) {
            var noOfParents = node.parents.length;
            return node.parents[noOfParents - 2];
        }

        function onNodeCopy(e, data) {
            data.node.data = angular.extend({}, data.original.data);
            var rootID = getRootID(data.node);
            updateHierarchyRoot(rootID);
        }

        function onNodeMove(e, data) {
            var oldParent = vm.treeInstance.jstree(true).get_node(data.old_parent);
            var oldRootID;
            if(oldParent.type === 'Root') {
                oldRootID = oldParent.id;
            } else {
                var noOfParentsOnOldPos = oldParent.parents.length;
                oldRootID = data.node.parents[noOfParentsOnOldPos - 2];
            }
            updateHierarchyRoot(oldRootID);

            var newRootID = getRootID(data.node);
            if (newRootID !== oldRootID) {
                updateHierarchyRoot(newRootID);
            }
        }

        function checkCallback(operation, node, node_parent, node_position, more) {
            if (operation === 'move_node' && node.id === 'all-items' ||
                operation === 'move_node' && node.parent === 'all-items' ||
                operation === 'move_node' && node_parent.id === 'all-items' ||
                operation === 'move_node' && node.type === 'Root' ||
                operation === 'copy_node' && node.id === 'all-items' ||
                operation === 'copy_node' && node_parent.id === 'all-items' ||
                operation === 'copy_node' && node.type === 'Root') {
                return false;
            }
        }

        function listenToChanges() {
            $scope.$on('itemsFetch', function() {
                rebuildTree();
            });
            $scope.$on('itemCreation', function(event, item) {
                if (item.isa === 'HierarchyRoot') {
                    addRoot(item);
                    logger.info('Item Explorer: Added a root to the tree.');
                } else {
                    var itemNode = allItemsNode(item);
                    var parent = vm.treeInstance.jstree(true).get_node('all-items');
                    vm.treeInstance.jstree(true).create_node(parent, itemNode, 'first');
                    logger.info('Item Explorer: Added a node to the "All items" root.');
                }
            });
            $scope.$on('itemDeletion', function(event, item) {
                var node = vm.treeInstance.jstree(true).get_node(item.id);
                removeTreeNode(node, true);
                logger.info('Item Explorer: Deleted item/root ' + item.name + ' from the tree.');
            });
            $scope.$on('itemUpdate', function(event, item) {
                if (item.isa === 'HierarchyRoot') {
                    updateTreeRoot(item);
                    logger.info('Item Explorer: Updated hierarchy root ' + item.name + '.');
                } else {
                    updateTreeNodeNames(item);
                }
            });
        }

        function updateTreeNodeNames(item) {
            var treeNodes = getTreeNodesConnectedToItem(item.id, true);
            for(var i = 0; i < treeNodes.length; i++) {
                vm.treeInstance.jstree(true).rename_node(treeNodes[i], item.name);
            }
        }

        function getTreeNodesConnectedToItem(itemID, includeAllItemsNode) {
            var hierarchyRoots = _.filter(itemService.items, {isa: 'HierarchyRoot'}),
                treeNodes = [];
            for(var i = 0; i < hierarchyRoots.length; i++) {
                loopHierarchy(hierarchyRoots[i]);
            }
            var allItemsNode = vm.treeInstance.jstree(true).get_node(itemID);
            if (includeAllItemsNode) {
                treeNodes.push(allItemsNode);
            }
            return treeNodes;

            function loopHierarchy(hierarchyParent) {
                for(var i = 0; i < hierarchyParent.children.length; i++) {
                    var hierarchyNode = hierarchyParent.children[i];
                    if (hierarchyNode.item === itemID) {
                        var treeNode = vm.treeInstance.jstree(true).get_node(hierarchyNode.id);
                        treeNodes.push(treeNode);
                    }
                    if (hierarchyNode.children.length > 0) {
                        loopHierarchy(hierarchyNode);
                    }
                }
            }
        }

        function updateTreeRoot(hierarchyRoot) {
            var treeRoot = vm.treeInstance.jstree(true).get_node(hierarchyRoot.id);
            loopTree(treeRoot, hierarchyRoot);
            loopHierarchy(treeRoot, hierarchyRoot);

            function loopTree(treeParent, hierarchyParent) {
                for(var i = 0; i < treeParent.children.length; i++) {
                    var treeChildID = treeParent.children[i];
                    var treeChild = vm.treeInstance.jstree(true).get_node(treeChildID);
                    var hierarchyChild = _.find(hierarchyParent.children, {id: treeChildID});
                    if (!hierarchyChild) {
                        removeTreeNode(treeChild, true);
                    }
                    if (treeChild.children.length > 0) {
                        loopTree(treeChild, hierarchyChild);
                    }
                }
            }

            function loopHierarchy(treeParent, hierarchyParent) {
                for(var i = 0; i < hierarchyParent.children.length; i++) {
                    var hierarchyChild = hierarchyParent.children[i];
                    var treeChildIndex = treeParent.children.indexOf(hierarchyChild.id);
                    if (treeChildIndex === -1) {
                        vm.treeInstance.jstree(true).create_node(treeParent, treeNode(hierarchyChild), 'last');
                    }
                    if (hierarchyChild.children && hierarchyChild.children.length > 0) {
                        var treeChild = vm.treeInstance.jstree(true).get_node(hierarchyChild.id);
                        loopHierarchy(treeChild, hierarchyChild);
                    }
                }
            }

        }

        function updateHierarchyRoot(rootID) {
            var treeRoot = vm.treeInstance.jstree(true).get_node(rootID);
            if(!treeRoot) {
                logger.error('Item Explorer: Remote root update failed. Could not find the tree root.');
                return;
            }
            var hierarchyRoot = itemService.getItem(treeRoot.id);
            if(hierarchyRoot === null) {
                logger.error('Item Explorer: Remote root update failed. Could not find the HierarchyRoot item.');
                return;
            }
            hierarchyRoot.children = [];
            loopTreeRoot(treeRoot, hierarchyRoot);
            itemService.saveItem(hierarchyRoot);

            function loopTreeRoot(treeParent, hierarchyParent) {
                for(var i = 0; i < treeParent.children.length; i++) {
                    var treeChildID = treeParent.children[i];
                    var treeChild = vm.treeInstance.jstree(true).get_node(treeChildID);
                    var hierarchyChild = {
                        id: treeChildID,
                        children: [],
                        item: treeChild.data.id
                    };
                    hierarchyParent.children.push(hierarchyChild);
                    if (treeChild.children.length > 0) {
                        loopTreeRoot(treeChild, hierarchyChild);
                    }
                }
            }
        }

        function addRoot(hierarchyRoot) {
            var root = {
                id: hierarchyRoot.id,
                text: hierarchyRoot.name,
                type: 'Root',
                children: [],
                state: {opened: true},
                data: hierarchyRoot
            };
            vm.treeInstance.jstree(true).create_node('#', root, 'last');
        }


        function allItemsNode(item) {
            return {
                id: item.id,
                text: item.name,
                type: item.isa,
                children: [],
                data: item
            };
        }

        function treeNode(hierarchyNode) {
            var item = itemService.getItem(hierarchyNode.item);
            if (item === null) {
                logger.error('Item Explorer: A hierarchy node refers to an item that does not exist.');
                return {
                    id: hierarchyNode.id,
                    text: 'Does not exist',
                    type: 'unknown',
                    children: [],
                    data: {
                        id: hierarchyNode.item
                    }
                };
            } else {
                return {
                    id: hierarchyNode.id,
                    text: item.name,
                    type: item.isa,
                    children: [],
                    data: item
                };
            }
        }

        function removeTreeNode(node, showError) {
            if(showError && !node) {
                logger.error('Item Explorer: Node deletion failed. Could not find any node with the supplied id.');
            } else {
                vm.treeInstance.jstree(true).delete_node(node);
            }
        }

        function rebuildTree() {
            var base = vm.treeInstance.jstree(true).get_node('#');
            var allItemsRoot = {
                id: 'all-items',
                text: 'All items',
                type: 'AllItemsRoot',
                children: [],
                state: {opened: true}

            };
            if (!base) {
                logger.error('Item Explorer: Failed to add items to the tree. The global root "#" was not present.');
            } else {
                for(var i = 0; i < base.children.length; i++) {
                    vm.treeInstance.jstree(true).delete_node(base.children[i]);
                }
                allItemsRoot.children.length = 0;
                var noOfCustomRoots = 0, noOfItems = 0;
                for(i = 0; i < itemService.items.length; i++) {
                    var item = itemService.items[i];
                    if (item.isa === 'HierarchyRoot' ) {
                        addRoot(item);
                        updateTreeRoot(item);
                        noOfCustomRoots++;
                    } else {
                        var itemNode = allItemsNode(item);
                        allItemsRoot.children.push(itemNode);
                        noOfItems++;
                    }
                }
                vm.treeInstance.jstree(true).create_node(base, allItemsRoot, 'first');
                logger.info('Item Explorer: Populated tree with ' + noOfItems + ' items and ' + noOfCustomRoots +
                    ' custom roots.');
            }
        }

        function contextMenuItems(node) {
            var items = {},
                rename = {
                    label: "Rename",
                    action: function() {
                        vm.treeInstance.jstree(true).edit(node, null, callback);

                        function callback(node, status, cancel) {
                            if(status && !cancel) {
                                var item = itemService.getItem(node.data.id);
                                if (node.text !== item.name) {
                                    item.name = node.text;
                                    itemService.saveItem(item);
                                }
                            }
                        }
                    }
                };
            /*create = {
                submenu: {
                    operation: {
                        label: "Operation",
                            action: function() {
                            itemService.createItem('New Operation', 'Operation');
                        }
                    },
                    thing: {
                        label: "Thing",
                            action: function() {
                            itemService.createItem('New Thing', 'Thing');
                        }
                    },
                    SPSpec: {
                        label: "SPSpec",
                            action: function() {
                            itemService.createItem('New SPSpec', 'SPSpec');
                        }
                    },
                    SOPSpec: {
                        label: "SOPSpec",
                            action: function() {
                            itemService.createItem('New SOPSpec', 'SOPSpec');
                        }
                    }
                },
                label: "Create"
            };
            if (node.id === 'all-items') {
                items.create = create;
            }*/
            if (node.id !== 'all-items') {
                items.rename = rename;
                var rootID, item = itemService.getItem(node.id);
                if (node.type === 'Root') {
                    items.delete = {
                        label: 'Delete root',
                        action: deleteItemAndItsNodes
                    };
                } else if (item === null) {
                    items.delete = {
                        label: 'Delete node',
                        action: function() {
                            removeTreeNode(node, true);
                            rootID = getRootID(node);
                            updateHierarchyRoot(rootID);
                        }
                    };
                } else {
                    items.delete = {
                        label: 'Delete item',
                        action: deleteItemAndItsNodes
                    };
                }
            }

            return items;

            function deleteItemAndItsNodes() {
                var localNodes = getTreeNodesConnectedToItem(item.id, false);
                for(var i = 0; i < localNodes.length; i++) {
                    rootID = getRootID(localNodes[i]);
                    removeTreeNode(localNodes[i], true);
                    updateHierarchyRoot(rootID);
                }
                itemService.deleteItem(item.id);
            }
        }
    }
})();
