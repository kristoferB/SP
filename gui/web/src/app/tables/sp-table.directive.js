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
            restrict: 'E',
            templateUrl: 'app/tables/sp-table.html',
            controller: SPTableController,
            controllerAs: 'vm',
            scope: {},
            bindToController: {
                rowCollection: '=',
                headerTemplate: '@',
                bodyTemplate: '@'
            }
        };
        return directive;
    }

    function SPTableController() {
        var vm = this;

        vm.itemsByPage = 15;
        vm.pageSizes = [2, 10, 15, 20, 50];
        vm.displayedCollection = [];
    }

})();
