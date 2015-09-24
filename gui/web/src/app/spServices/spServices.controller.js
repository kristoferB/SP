/**
 * Created by patrik on 2015-09-14.
 */
(function () {
    'use strict';

    angular
        .module('app.spServices')
        .controller('spServicesController', spServicesController);

    spServicesController.$inject = ['spServicesService', '$scope', 'dashboardService'];
    /* @ngInject */
    function spServicesController(spServicesService, $scope, dashboardService) {
        var vm = this;
        vm.widget = $scope.$parent.widget; //For GUI
        vm.registeredServices = spServicesService.spServices; //From RESTapi
        vm.displayedRegisteredServices = []; //For GUI
        vm.startSpService = spServicesService.startSpService; //To start a service. Name of service as parameter
        vm.buildHtmlFromJsonAttr = buildHtmlFromJsonAttr;
        vm.printToLogger = function(obj) {logger.info("I was asked to print this: "+obj);}

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

        function buildHtmlFromJsonAttr(jsonToParse) {
            var toReturn = '';
            function recBuild(obj) {
                var k;
                if (obj instanceof Object) {
                    for (k in obj){
                        if (obj.hasOwnProperty(k)){
                            toReturn += '<b>' + k + '</b>';
                            toReturn += '<ul>';
                            recBuild( obj[k] );
                            toReturn += '</ul>';
                        }
                    }
                } else {
                    toReturn += '<li><button ng-click="vm.printToLogger(\''+obj+'\')">' + obj+ '</button></li>';
                };
            };

            recBuild(jsonToParse);
            return toReturn;
        };

    }
})();
