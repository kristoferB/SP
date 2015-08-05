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
            template:   '<div class="table-info">Showing ' +
                        '<span class="number-of-first-entry-on-page">{{numberOfFirstEntryOnPage()}}</span> to ' +
                        '<span class="number-of-last-entry-on-page">{{numberOfLastEntryOnPage()}}</span> of ' +
                        '<span class="total-number-of-entries">{{totalNoOfEntries()}}</span> entries</div>',
            link: link
        };
        return directive;

        function link(scope, element, attrs, ctrl) {

            scope.numberOfFirstEntryOnPage = numberOfFirstEntryOnPage;
            scope.numberOfLastEntryOnPage = numberOfLastEntryOnPage;
            scope.totalNoOfEntries = totalNoOfEntries;

            function totalNoOfEntries() {
                return ctrl.tableState().pagination.totalItemCount;
            }

            function numberOfFirstEntryOnPage() {
                return ctrl.tableState().pagination.start + 1;
            }

            function numberOfLastEntryOnPage() {
                var total = totalNoOfEntries();
                var last = numberOfFirstEntryOnPage() + ctrl.tableState().pagination.number - 1;
                if (last > total) {
                    return total;
                } else {
                    return last;
                }
            }
        }
    }
})();
