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
                dashboard: '=', /* When is this used? /Patrik 150915 */
                showCloseBtn: '='
            }
        };

        return directive;
    }

    SPWidgetController.$inject = ['$scope'];

    function SPWidgetController($scope) {
        var vm = this;
        vm.requestClose = requestClose;

        function requestClose() {
            $scope.$broadcast('closeRequest', vm.widget.id);
        }
    }

})();
