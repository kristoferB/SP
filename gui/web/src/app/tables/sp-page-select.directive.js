/**
 * Created by daniel on 2015-07-24.
 */
(function () {
    'use strict';

    angular
        .module('app.tables')
        .directive('spPageSelect', spPageSelect);

    spPageSelect.$inject = [];
    /* @ngInject */
    function spPageSelect() {

        var directive = {
            restrict: 'E',
            template: '<input type="text" class="select-page" ng-model="inputPage" ng-change="selectPage(inputPage)">',
            link: link
        };
        return directive;

        function link(scope, element, attrs) {
            scope.$watch('currentPage', function(c) {
                scope.inputPage = c;
            });
        }
    }
})();
