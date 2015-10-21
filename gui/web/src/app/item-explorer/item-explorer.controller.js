/**
 * Created by daniel on 2015-08-11.
 */
(function () {
    'use strict';

    angular
        .module('app.itemExplorer')
        .controller('ItemExplorerController', ItemExplorerController);

    ItemExplorerController.$inject = ['$scope', 'logger', 'itemService', '$modal', 'dashboardService', '$rootScope', 'uuid4'];
    /* @ngInject */
    function ItemExplorerController($scope, logger, itemService, $modal, dashboardService, $rootScope, uuid4) {
        var vm = this;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.dashboard = $scope.$parent.$parent.$parent.vm.dashboard;
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
                check_callback: isNodeDroppable
            },
            dnd: {
                large_drop_target: true,
                large_drag_target: true
            },
            plugins: ['dnd', 'types', 'contextmenu', 'search'],
            search: {
                "show_only_matches" : true,
                "search_callback": searchCallback},
            types: {
                '#' : {
                    valid_children: ['Root', 'AllItemsRoot']
                },
                Root: {
                    icon : 'images/tree_icon.png',
                    valid_children: ['Operation', 'Thing', 'SPSpec', 'SOPSpec', 'Result']
                },
                AllItemsRoot: {
                    icon: 'images/all_items.png',
                    valid_children: []
                },
                Operation: {
                    icon: 'images/step_forward.png',
                    valid_children: ['Operation', 'Thing', 'SPSpec', 'SOPSpec', 'Result']
                },
                Thing: {
                    icon: 'images/robot.png',
                    valid_children: ['Operation', 'Thing', 'SPSpec', 'SOPSpec', 'Result']
                },
                SPSpec: {
                    icon: 'images/document.png',
                    valid_children: ['Operation', 'Thing', 'SPSpec', 'SOPSpec', 'Result']
                },
                SOPSpec: {
                    icon: 'images/hierarchy.png',
                    valid_children: ['Operation', 'Thing', 'SPSpec', 'SOPSpec', 'Result']
                },
                Result: {
                    icon: 'images/line_chart.png',
                    valid_children: ['Operation', 'Thing', 'SPSpec', 'SOPSpec', 'Result']
                }
            }
        };

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

        function onTreeReady() {
            updateTree();
            listenToChanges();
        }

        function isNodeDroppable(operation, draggedNode, targetParent, node_position, more) {
            return (
              draggedNode.id !== 'all-items' ||
              draggedNode.type === 'Root' ||
              targetParent.id !== 'all-items'
            );
        }

        function listenToChanges() {
            $scope.$on('itemsFetch', function() {
                updateTree();
            });
            $scope.$on('itemCreation', function(event, item) {
                updateTree();
            });
            $scope.$on('itemDeletion', function(event, item) {
                var node = vm.treeInstance.jstree(true).get_node(item.id);
                vm.treeInstance.jstree(true).delete_node(node);
                updateTree();
            });
            $scope.$on('itemUpdate', function(event, item) {
                updateTree();
            });
        }

        function searchCallback(str, node){
            return node.type.search(new RegExp(str, "i")) > -1 || node.text.search(new RegExp(str, "i")) > -1;
        }

        function onSelectionChange(e, data) {
            itemService.selected.splice(0, itemService.selected.length);
            for(var i = 0; i < data.selected.length; i++) {
                var nodeID = data.selected[i];
                if (nodeID !== 'all-items') {
                    var node = vm.treeInstance.jstree(true).get_node(nodeID);
                    var itemID = node.data.id;
                    var item = itemService.getItem(itemID);
                    itemService.selected.push(item);
                }
            }
        }






        function updateTree(){
            refreshRoots();
            refreshItemNodes();
        }


        function refreshRoots(){
            var roots = _.filter(itemService.items, function(i){return i.isa == 'HierarchyRoot'});
            _.forEach(roots, function(root){
                updateTreeRoot(root);
            });

            function updateTreeRoot(hierarchyRoot) {
                var children = createChildren(hierarchyRoot);
                var newRoot = createRoot(hierarchyRoot);
                newRoot.children = children;

                var oldRoot = vm.treeInstance.jstree(true).get_node(hierarchyRoot.id);

                vm.treeInstance.jstree(true).delete_node(oldRoot);

                vm.treeInstance.jstree(true).create_node('#', newRoot, 'last');

                function createChildren(node){
                    var x = _.map(node.children, function(c){
                        var newN = createNode(c);
                        newN.children = createChildren(c);
                        var treeN =  vm.treeInstance.jstree(true).get_node(c.id);
                        if (treeN){ newN.state = treeN.state}
                        return newN
                    });
                    return x;
                }

                function createRoot(hierarchyRoot) {
                    var oldRoot = vm.treeInstance.jstree(true).get_node(hierarchyRoot.id);
                    var root = {
                        id: hierarchyRoot.id,
                        text: hierarchyRoot.name,
                        type: 'Root',
                        children: [],
                        state: oldRoot.state ? oldRoot.state : {opened: true},
                        data: hierarchyRoot
                    };
                    return root;
                }

                function deleteChildrenInTree(node){
                    _.forEach(node.children, function(c){
                        deleteChildrenInTree(c);
                        var treeN =  vm.treeInstance.jstree(true).get_node(c.id);
                        if (treeN){
                            vm.treeInstance.jstree(true).delete_node(treeN);
                        }
                    })
                }


            }
        }



        function refreshItemNodes(){
            var items = _.filter(itemService.items, function(i){return i.isa !== 'HierarchyRoot'});

            var sorted = _.sortBy(items, 'name');
            var fixState = _.map(sorted, function(i){
                var item = createItemNode(i);
                var treeNode = vm.treeInstance.jstree(true).get_node(i.id);
                if (treeNode){
                    item.state = treeNode.state;
                }
                return item;
            });

            var root = vm.treeInstance.jstree(true).get_node('all-items');
            var newRoot = {
                id: 'all-items',
                text: 'All items',
                type: 'AllItemsRoot',
                children: fixState,
                state: root.state ? root.state : {opened: false}
            };

            vm.treeInstance.jstree(true).delete_node(root);
            vm.treeInstance.jstree(true).create_node('#',newRoot, 'last');
        }




        function createNode(hierarchyNode) {
            var item = itemService.getItem(hierarchyNode.item);
            if (item === null) {
                //logger.error('Item Explorer: A hierarchy node refers to an item that does not exist.');
                return {
                    id: hierarchyNode.id,
                    text: hierarchyNode.item,
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

        function createItemNode(item) {
            return {
                id: item.id,
                text: item.name,
                type: item.isa,
                children: [],
                data: item,
            };
        }

        function createHNode(item) {
            return {
                item: item.id,
                children: [],
                id: uuid4.generate()
            };
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

                // add to under selected node, if selected

            });
        }



// TODO: Refactor copy and move
        function onNodeCopy(e, data) {

            var newParentID = data.node.parent;
            var newRoot = getRoot(data.node);

            if(data.node.type !== 'Root' && data.node.type !== 'AllItemsRoot' && newRoot.type !== 'AllItemsRoot'){
                var id = data.original.data.id ? data.original.data.id : data.original.id;
                var item = itemService.getItem(id);

                var hNode = createHNode(item);
                var newH = itemService.getItem(newRoot.id);
                var newH2 = addHNodeToH(hNode, newParentID, newH);
                if (newH2) itemService.saveItem(newH2);


            } else {
                updateTree();
            }

        }


        function onNodeMove(e, data) {
            var newParentID = data.node.parent;
            var newRoot = getRoot(data.node);

            if(data.node.type !== 'Root' && data.node.type !== 'AllItemsRoot' && newRoot.type !== 'AllItemsRoot'){
                var oldParent = vm.treeInstance.jstree(true).get_node(data.old_parent);
                var oldRoot = getRoot(oldParent);

                var newH = itemService.getItem(newRoot.id);
                var oldH = itemService.getItem(oldRoot.id);
                var hNodeToAdd = getHNodeFromHRoot(oldH, data.node.id);
                var newH = addHNodeToH(hNodeToAdd, newParentID, newH);

                var sameP = newH && oldH && newH.id == oldH.id;
                oldH = sameP ? newH : oldH;
                var removeH = deleteNodeFromH(data.node.id, oldParent.id, oldH);

                if (newH && !sameP) itemService.saveItem(newH);
                if (removeH) itemService.saveItem(removeH);

                var newP = vm.treeInstance.jstree(true).get_node(newParentID);
                newP.state.opened = true;
            } else {
                updateTree();
            }

            function getHNodeFromHRoot(hRoot, id){
                if (hRoot){
                    return findHnodeInH(hRoot, id)
                } else {
                    return {
                        item: id,
                        children: [],
                        id: uuid4.generate()
                    }
                }
            }

        }

        function deleteNodeFromH(nodeID, parentID, hRoot){
            var hPar = findHnodeInH(hRoot, parentID)
            if (hPar){
                var index = _.findIndex(hPar.children, {id: nodeID});
                hPar.children.splice(index, 1)
                return hRoot;
            }
            return false;
        }

        function addHNodeToH(node, parentID, hRoot){
            var hPar = findHnodeInH(hRoot, parentID)
            if (hPar){
                hPar.children.push(node);
                return hRoot;
            }
            return false;
        }


        function findHnodeInH(node, id){
            if (!node) return false;
            if (node.id == id) return node;
            else {
                var found = false;
                _.forEach(node.children, function(c){
                    var res = findHnodeInH(c, id);
                    if (res){
                        found = res;
                    }
                });
                return found;
            }
        }

        function getRoot(node){
            if (node.type == 'Root') return node;
            if (node.type == 'AllItemsRoot') return false;

            var rootID = node.parents[node.parents.length - 2];
            return vm.treeInstance.jstree(true).get_node(rootID);
        }

        function getRootID(node) {
            var noOfParents = node.parents.length;
            return node.parents[noOfParents - 2];
        }

        function getHierarchyFromTree(node){
            return _.map(node.children, function(c){
                var treeChild = vm.treeInstance.jstree(true).get_node(c);
                return {
                    id: treeChild.id,
                    item: treeChild.item,
                    children: getHierarchyFromTree(treeChild)
                };
            });
        }


        function contextMenuItems(node) {
            var menuItems = {},
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
                menuItems[20] = rename;
                var rootID, item = itemService.getItem(node.data.id);
                if (node.type === 'Root') {
                    menuItems.delete = {
                        label: 'Delete root',
                        action: function(){
                            itemService.deleteItem(node.id)
                        }
                    };
                } else if (item === null) {
                    menuItems[30] = {
                        label: 'Delete node',
                        action: function() {
                            var root = getRoot(node);
                            var parID = node.parent;
                            var hRoot = itemService.getItem(root.id);
                            hRoot = deleteNodeFromH(node.id, parID, hRoot)
                            if (hRoot) itemService.saveItem(hRoot);
                        }
                    };
                    menuItems[40] = {
                        label: 'Delete item',
                        action: function() {
                            itemService.deleteItem(node.data.id);
                        }
                    };
                } else {
                    menuItems[30] = {
                        label: 'Delete item',
                        action: function(){
                            itemService.deleteItem(node.id);
                        }
                    };
                }

                if (node.data.isa === 'SOPSpec') {
                    menuItems[10] = {
                        label: 'Open with SOP Maker',
                        action: function() {
                            var widgetKind = _.find(dashboardService.widgetKinds, {title: 'SOP Maker'});
                            if (widgetKind === undefined) {
                                logger.error('Item Explorer: Open with SOP Maker failed. ' +
                                    'Could not find widgetKind "SOPMaker".')
                            }
                            dashboardService.addWidget(vm.dashboard, widgetKind, {sopSpecID: item.id});
                            $rootScope.$digest();
                        }
                    };
                }

            }

            return menuItems;

        }
    }
})();
