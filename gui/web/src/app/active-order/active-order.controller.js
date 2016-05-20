/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.activeOrder')
      .controller('activeOrderController', activeOrderController);

    activeOrderController.$inject = ['$scope', 'dashboardService', 'spServicesService'];
    /* @ngInject */
    function activeOrderController($scope, dashboardService, spServicesService) {
        var vm = this;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.a = 3;
        vm.b = 7;
        vm.getColor = getColor;

        vm.ButtonColor = {
            kub: [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 4, 3, 0],
                [0, 1, 2, 0]
            ]
        };

        function getColor(int) {
                switch (int) {
                    case 1:
                        return "#ffff66";
                    case 2://GREEN = 2
                        return "#5cd65c";
                    case 3://RED = 3
                        return "#ff3333";
                    case 4://BLUE = 4
                        return "#0066ff";
                    default: //white by default
                        return "#FFFFFF";
                }
        }

        function resetColor() {
            vm.ButtonColor.kub = [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]
            ]
        }

        vm.spservice = spServicesService;
        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

    }
})();
