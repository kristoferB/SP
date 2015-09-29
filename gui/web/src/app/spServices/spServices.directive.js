/**
 * Created by patrik on 2015-09-22.
 */
(function () {
    'use strict';

    angular
        .module('app.spServices')
        .directive('spServicesForm', spServicesForm);

    spServicesForm.$inject = ['$compile'];
    /* @ngInject */
    function spServicesForm($compile) {
        var directive = {
            restrict: 'EA',
            templateUrl: 'app/spServices/spServices.directive.html',
            scope: {
                attributes: '=',
                structure: '=',
                key: '='
            },
            compile: compile,
          controller: spServicesFormController,
          controllerAs: 'vm',
          bindToController: true
        };

        return directive;

        function compile(tElement, tAttr, transclude) {
            var contents = tElement.contents().remove();
            var compiledContents;
            return function(scope, iElement, iAttr) {
                if(!compiledContents) {
                    compiledContents = $compile(contents, transclude);
                }
                compiledContents(scope, function(clone, scope) {
                    iElement.append(clone);
                });
            };
        }

    }

    spServicesFormController.$inject = ['modelService','itemService'];
    function spServicesFormController(modelService,itemService) {
        var vm = this;
        vm.isA = "";
        vm.reloadModelID = reloadModelID();
        vm.reloadSelectedItems = reloadSelectedItems();

        activate();

        function activate(){
            whatIsIt();
        }

        function whatIsIt(){
            var x = vm.structure;
            if (_.isUndefined(x)){
                vm.isA = ""
            } else if (!_.isUndefined(x.ofType)){

                if (x.ofType == "Option[ID]" && vm.key == "model") {
                    vm.isA = "Option[ID]Model";
                    vm.attributes = _.isUndefined(x.default) ? vm.reloadModelID : x.default;
                } else if (x.ofType == "List[ID]" && vm.key == "includeIDAbles") {
                    vm.isA = "List[ID]includeIDAbles";
                    vm.attributes = _.isUndefined(x.default) ? vm.reloadSelectedItems : x.default;
                } else if (x.ofType == "Boolean") {
                    vm.isA = "Boolean";
                    vm.attributes = _.isUndefined(x.default) ? false : x.default
                } else if (x.ofType == "List[ID]") {
                    vm.isA = "List[ID]";
                    vm.attributes = _.isUndefined(x.default) ? [] : x.default
                } else{
                    vm.isA = "KeyDef"; // för att testa
                    vm.attributes = _.isUndefined(x.default) ? "" : x.default
                }

//                vm.attributes = _.isUndefined(x.default) ? "" : x.default
            } else if (_.isObject(x)){
                vm.isA = "object";
                vm.attributes = {};
            } else {
                vm.isA = "something";
                vm.attributes = x;
            }
        }

        function reloadModelID() {
            var currentModelId = modelService.activeModel.id;
            return _.isUndefined(currentModelId) ? "" : currentModelId;
        }

        function reloadSelectedItems() {
            var toReturn = _.map(itemService.selected, function(item){
                return item.id;
            });
            return toReturn;
        }

    }

})();
