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

    spServicesFormController.$inject = ['modelService','itemService','spServicesService'];
    function spServicesFormController(modelService,itemService,spServicesService) {
        var vm = this;
        vm.isA = "";
        vm.reloadModelID = spServicesService.reloadModelID;
        vm.reloadSelectedItems = spServicesService.reloadSelectedItems;
        vm.domainToSelectFrom = [];

        activate();

        function activate(){
            whatIsIt();
        }

        function whatIsIt(){
            var x = vm.structure;
            if (_.isUndefined(x)){
                vm.isA = ""
            } else if (!_.isUndefined(x.ofType)){
                //core>model
                if (x.ofType == "Option[ID]" && vm.key == "model") {
                    vm.isA = "Option[ID]Model";
                    vm.attributes = _.isUndefined(x.default) ? vm.reloadModelID() : x.default;
                //core>includeIDAbles
                } else if (x.ofType == "List[ID]" && vm.key == "includeIDAbles") {
                    vm.isA = "List[ID]includeIDAbles";
                    vm.attributes = _.isUndefined(x.default) ? vm.reloadSelectedItems() : x.default;
                //Boolean
                } else if (x.ofType == "Boolean") {
                    vm.isA = "Boolean";
                    vm.attributes = _.isUndefined(x.default) ? false : x.default;
                //String
                } else if (x.ofType == "String") {
                    vm.isA = "String";
                    vm.attributes = _.isUndefined(x.default) ? "" : x.default;
                    if(!_.isUndefined(x.domain) && x.domain.length > 0) {
                        vm.isA += "WithDomain";
                        vm.domainToSelectFrom = x.domain;
                    }
                //Int
                } else if (x.ofType == "Int") {
                    vm.isA = "Int";
                    vm.attributes = _.isUndefined(x.default) ? 0 : x.default;
                    if(!_.isUndefined(x.domain) && x.domain.length > 0) {
                        vm.isA += "WithDomain";
                        vm.domainToSelectFrom = x.domain;
                    }
                //ID
                } else if (x.ofType == "ID") {
                    vm.isA = "ID";
                    vm.attributes = _.isUndefined(x.default) ? vm.reloadSelectedItems()[0] : x.default;
                //List[ID] and List[String]
                } else if (x.ofType == "List[ID]" || x.ofType == "List[String]") {
                    vm.isA = "List[T]";
                    vm.attributes = _.isUndefined(x.default) ? [] : x.default;
                //The rest
                } else {
                    vm.isA = "KeyDef"; // för att testa
                    vm.attributes = _.isUndefined(x.default) ? "" : x.default;
                }

            } else if (_.isObject(x)){
                vm.isA = "object";
                vm.attributes = {};
            } else {
                vm.isA = "something";
                vm.attributes = x;
            }
        }
    }

})();
