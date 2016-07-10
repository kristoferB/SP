(function () {
    'use strict';

    angular
        .module('app.widgets')
        .directive('spWidget', spWidget);

    spWidget.$inject = [];
    /* @ngInject */
    function spWidget () {
        var directive = {
            restrict: 'E',
            templateUrl: 'app/widgets/sp-widget.html',
            controller: SPWidgetController,
            controllerAs: 'vm',
            scope: {},
            transclude: true,
            replace: true,
            bindToController: {
                widget: '=',
                dashboard: '=',
                /* Q: When is this used? /Patrik 150915
                   A: In certain widget controllers, to determine which dashboard they belong to. /Daniel 150922 */
                showCloseBtn: '='
            }
        };

        return directive;
    }

    SPWidgetController.$inject = ['$scope','settingsService'];

    function SPWidgetController($scope, settingsService) {
        var vm = this;
        vm.requestClose = requestClose;
        vm.title = angular.copy(vm.widget.title, '');
        vm.settingsService = settingsService;
        function requestClose() {
            $scope.$broadcast('closeRequest', vm.widget.id);
        }
    }

})();
