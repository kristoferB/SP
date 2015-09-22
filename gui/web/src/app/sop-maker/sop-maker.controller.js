/**
 * Created by daniel on 2015-09-06.
 */
(function () {
    'use strict';

    angular
        .module('app.sopMaker')
        .controller('SOPMakerController', SOPMakerController);

    SOPMakerController.$inject = ['$element', '$scope', 'sopDrawer', 'itemService', 'logger', 'dashboardService', '$modal'];
    /* @ngInject */
    function SOPMakerController($element, $scope, sopDrawer, itemService, logger, dashboardService, $modal) {
        var vm = this, paper = null;
        var widgetModel = $scope.$parent.$parent.$parent.vm;
        vm.widget = widgetModel.widget;
        vm.addSop = addSop;
        vm.toggleDirection = toggleDirection;
        vm.save = save;
        vm.clearAndDrawFromScratch = clearAndDrawFromScratch;
        vm.sopSpecCopy = null;
        vm.saveButtonText = function() {return vm.widget.storage.sopSpecID ? 'Save' : 'Save As';};

        activate();

        function activate() {
            if (vm.widget.storage === undefined) {
                vm.widget.storage = {};
            }
            vm.widget.storage.editable = true;
            vm.widget.storage.viewAllConditions = true;
            var paperElement = $($element).find('.sop-paper');
            paper = Raphael(paperElement[0], 300, 300);
            if(itemService.itemsFetched) {
                loadAndDraw();
            } else {
                var listener = $scope.$on('itemsFetch', function () {
                    listener();
                    loadAndDraw();
                });
            }
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

        function addSop() {
            vm.sopSpecCopy.sop.push({
                    isa: 'Sequence',
                    sop: []
                }
            );
            reDraw();
        }

        function toggleDirection() {
            vm.sopSpecCopy.vertDir = !vm.sopSpecCopy.vertDir;
            reDraw();
        }

        function clearAndDrawFromScratch() {
            paper.clear();
            draw();
        }

        function draw() {
            sopDrawer.calcAndDrawSop($scope, paper, true, true);
        }

        function reDraw() {
            sopDrawer.calcAndDrawSop($scope, paper, true, false);
        }

        function loadAndDraw() {
            if (vm.widget.storage.sopSpecID) {
                var sopSpec = itemService.getItem(vm.widget.storage.sopSpecID);
                if (sopSpec === null) {
                    logger.error('The SOP Spec ' + vm.widget.storage.sopSpecID + ' associated with this SOP Maker instance is not available.');
                    return;
                } else {
                    vm.sopSpecCopy = angular.copy(sopSpec, {});
                    logger.info('SOP Spec Maker: SOP "' + vm.sopSpecCopy.name + '" loaded.');
                }
            } else if (vm.widget.storage.sopSpec) {
                vm.sopSpecCopy = angular.copy(vm.widget.storage.sopSpec, {});
                logger.info('SOP Spec Maker: Loaded a raw SOP structure that was supplied to the widget.');
            } else {
                vm.sopSpecCopy = {
                    name: 'Nameless SOP',
                    sop: []
                }
            }
            widgetModel.title = vm.sopSpecCopy.name;
            console.log($scope.$parent);
            vm.sopSpecCopy.vertDir = true;
            draw();
        }

        function save() {
            var sopSpec;
            if (vm.widget.storage.sopSpecID) {
                console.log(vm.widget.storage.sopSpecID);
                 sopSpec = itemService.getItem(vm.widget.storage.sopSpecID);
                if (sopSpec === null) {
                    logger.error('The SOP Spec associated with this SOP Maker instance is not available.');
                } else {
                    sopSpec.sop = extractSOPStructure(vm.sopSpecCopy.sop);
                    itemService.saveItem(sopSpec);
                }
            } else {
                sopSpec = {
                    isa: 'SOPSpec'
                };

                var saveAsModal = $modal.open({
                    templateUrl: '/app/sop-maker/save-as.html',
                    controller: 'SaveAsController',
                    controllerAs: 'vm',
                    resolve: {
                        item: function() {
                            return sopSpec;
                        }
                    }
                });

                saveAsModal.result.then(ifSaveIsConfirmed);
            }

            function newUUID() {
                return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                    var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
                    return v.toString(16);
                });
            }

            function ifSaveIsConfirmed(sopSpec) {
                sopSpec.id = newUUID();
                sopSpec.sop = extractSOPStructure(vm.sopSpecCopy.sop);
                itemService.saveItem(sopSpec).then(ifSaveSucceeds);

                function ifSaveSucceeds() {
                    vm.widget.storage.sopSpecID = sopSpec.id;
                    widgetModel.title = sopSpec.name;
                }
            }

        }

        function extractSOPStructure(obj) { // Handle the 3 simple types, and null or undefined
            var copy;
            if (null == obj || "object" != typeof obj) { // Handle null
                return obj;
            } else if (obj instanceof Date) { // Handle Date
                copy = new Date();
                copy.setTime(obj.getTime());
                return copy;
            } else if (obj instanceof Array) { // Handle Array
                copy = [];
                for (var i = 0, len = obj.length; i < len; i++) {
                    copy[i] = extractSOPStructure(obj[i]);
                }
                return copy;
            } if (obj instanceof Object) { // Handle Object
                copy = {};
                for (var attr in obj) {
                    if (obj.hasOwnProperty(attr) && attr !== 'clientSideAdditions') {
                        copy[attr] = extractSOPStructure(obj[attr]);
                    }
                }
                return copy;
            }
            throw new Error("Unable to copy obj! Its type isn't supported.");
        }


    }
})();
