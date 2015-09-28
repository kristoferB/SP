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
                structure: '='
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

    spServicesFormController.$inject = ['$document'];
    function spServicesFormController($document) {
        var vm = this;
        vm.isA = "";

        activate();

        function activate(){
            whatIsIt();
        };

        function whatIsIt(){
            var x = vm.structure;
            if (_.isUndefined(x)){
                vm.isA = ""
            } else if (!_.isUndefined(x.ofType)){

                // använd denna för att matcha typer, ev förenkla typerna?
                //vm.isA = x.ofType;
                vm.isA = "KeyDef"; // för att testa

                vm.attributes = _.isUndefined(x.default) ? "" : x.default
            } else if (_.isObject(x)){
                vm.isA = "object";
                vm.attributes = {};
                //_.forOwn(x, function(value, key){
                //    vm.attributes[key] = "";
                //})
            } else {
                vm.isA = "something";
                vm.attributes = x;
            }
        }



    }



})();
