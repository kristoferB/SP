/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.calculator')
      .controller('calculatorController', calculatorController);

    calculatorController.$inject = ['$scope', 'dashboardService', 'spServicesService'];
    /* @ngInject */
    function calculatorController($scope, dashboardService, spServicesService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;

        vm.a = 0;
        vm.b = 0;
        vm.result = 0;
        vm.calculating = false;

        vm.calc = calc;

        vm.spservice = spServicesService;


        activate();


        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

        function calc(sign){
            console.log('a n b' + vm.a + vm.b);
            var mess = {"data": {"a":vm.a, "b":vm.b, "sign":sign}};

              vm.spservice.callService(vm.spservice.getService("Calculator"),
              mess,
              function(resp){
                  if (_.has(resp, 'attributes.result')){
                      vm.result = resp.attributes.result;
                  }
              }
            )

        }
    }
})();
