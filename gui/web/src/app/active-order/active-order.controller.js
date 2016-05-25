/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.activeOrder')
      .controller('activeOrderController', activeOrderController);

    activeOrderController.$inject = ['$scope', 'dashboardService', 'spServicesService','eventService'];
    /* @ngInject */
    function activeOrderController($scope, dashboardService, spServicesService, eventService) {
        var vm = this;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.a = 0;
        vm.b = 0;
        vm.getColor = getColor;
        vm.messages = '';
        var messages = '';

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
            ];
        }

        function reverseMessages() {
            return messages.split("\n").reverse().join("\n");
        }

        function onEvent(event) {
            if(!(_.isUndefined(event.service)) && event.service != "OrderHandler") return;
            if(_.isUndefined(event.isa)) return;
            if(event.isa != "Progress") return;
            if(_.isUndefined(event.attributes.status)) return;
            var attr = event.attributes;
            if(attr.status == 'reset') {
                vm.a = 0;
                vm.b = 0;
                messages = '';
            } else if(attr.status == 'new') {
                console.log('added');
                messages += 'Added new order: ' + attr.order.name + '\n';
                vm.b++;
            } else if(attr.status == 'completed') {
                console.log('finishing');
                messages += 'Order: ' + attr.order.name + ' -- Completed station ' + attr.station + '\n';
                if(attr.station == 'tower') {
                    // tower complete
                    vm.a++;
                    if(vm.a > vm.b) vm.a = vm.b;
                }
            } else if(attr.status == 'stationOrder') {
                console.log('starting');
                messages += 'Order: ' + attr.order.name + ' -- Starting station ' + attr.station + '\n';
                if(attr.station == 'tower') {
                    if(vm.a == vm.b) vm.a++;
                }
            }
            vm.messages = reverseMessages();
        }

        vm.spservice = spServicesService;
        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });

            eventService.addListener('Progress', onEvent);
        }

    }
})();
