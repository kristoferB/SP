/**
 * Created by daniel on 2015-07-24.
 */
(function () {
    'use strict';

    angular
        .module('app.tables')
        .directive('spTableInfo', spTableInfo);

    spTableInfo.$inject = [];
    /* @ngInject */
    function spTableInfo() {

        var directive = {
            restrict: 'E',
            require: '^stTable',
            template: '<div class="table-info">Showing {{indexOfFirstEntryOnPage()}} to ' +
                      '{{indexOfLastEntryOnPage()}} of {{totalNoOfEntries()}} entries</div>',
            link: link
        };
        return directive;

        function link(scope, element, attrs, ctrl) {

            scope.indexOfFirstEntryOnPage = indexOfFirstEntryOnPage;
            scope.indexOfLastEntryOnPage = indexOfLastEntryOnPage;
            scope.totalNoOfEntries = totalNoOfEntries;

            function totalNoOfEntries() {
                return ctrl.tableState().pagination.totalItemCount;
            }

            function indexOfFirstEntryOnPage() {
                return ctrl.tableState().pagination.start + 1;
            }

            function indexOfLastEntryOnPage() {
                var total = totalNoOfEntries();
                var last = indexOfFirstEntryOnPage() + ctrl.tableState().pagination.number - 1;
                if (last > total) {
                    return total;
                } else {
                    return last;
                }
            }
        }
    }
})();
