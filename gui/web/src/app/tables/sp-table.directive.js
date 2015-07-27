/**
 * Created by daniel on 2015-07-26.
 */
(function () {
    'use strict';

    angular
        .module('app.tables')
        .directive('spTable', spTable);

    spTable.$inject = [];
    /* @ngInject */
    function spTable() {

        var directive = {
            restrict: 'A',
            templateUrl: 'app/tables/sp-table.html',
            controller: SPTableController,
            controllerAs: 'vm',
            link: linkFunction,
            scope: {},
            transclude: true,
            replace: true,
            bindToController: {
                rowCollection: '=',
                displayedCollection: '=',
                headerTemplate: '@'
            }
        };
        return directive;

        function linkFunction(scope, element, attrs, ctrl, transclude) {
            element.find('tbody').replaceWith(transclude());
        }
    }

    SPTableController.$inject = [];

    function SPTableController() {
        var vm = this;

        vm.itemsByPage = 15;
        vm.pageSizes = [2, 10, 15, 20, 50];
    }

})();
