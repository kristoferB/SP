/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.testWidget')
      .controller('testWidgetController', testWidgetController);

    testWidgetController.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService'];
    /* @ngInject */
    function testWidgetController($scope, dashboardService, eventService, spServicesService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what

        vm.a=0;
        vm.b=0;
        vm.result=0;

        vm.calc= calc;


        activate();


        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
    }
        function calc(sign){
            var mess ={"data":{"a":vm.a,"b":vm.b,"sign":sign}};
            spServicesService.callService(
                spServicesService.getService("testService"),mess,
                function(resp){
                    if(_.has(resp,'attributes.result')){
                        vm.result=resp.attributes.result;
                    }
                }
            )

            }

    }
})();
