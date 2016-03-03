/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.rasmus')
      .controller('rasmusController', rasmusController);

    rasmusController.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService'];
    /* @ngInject */
    function rasmusController($scope, dashboardService, eventService,spServicesService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what
        vm.a = 0;
        vm.b = 0;
        vm.picture;
        vm.result = 0;

        vm.calc = calc;
        if(vm.a == 0){
            vm.picture = "/images/Frame.jpg";
        } else {
            vm.picture = "/images/icon.png";
        }

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }
        function showImage() {

        }
        function calc (sign) {
            var mess = {"data": {"a":vm.a,"b":vm.b,"sign":sign}};

            spServicesService.callService(
                spServicesService.getService("Rasmus"),mess,
                function(resp) {

                    if(_.has(resp, "attributes.result")){
                        vm.result = resp.attributes.result;
                    }
                }
            )
        }

    }
})();
