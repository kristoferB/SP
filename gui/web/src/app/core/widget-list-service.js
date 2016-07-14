(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('widgetListService', widgetListService);

    // TODO add logger stuff
    widgetListService.$inject = ['$http', 'logger'];
    /* @ngInject */
    function widgetListService($http, logger) {
        var service = {list: list};
                return service;

        function list(callback) {
            $http.get('/widgetList.json', 'json').
                then(function(response) {
                    callback(response.data.widgetList);
                });
        }
    }
})();
