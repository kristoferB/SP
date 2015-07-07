(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('dataservice', dataservice);

    dataservice.$inject = ['$http', '$q', 'logger'];
    /* @ngInject */
    function dataservice($http, $q, logger) {
        var service = {

            getModels: getModels
        };

        return service;

        function getModels() {
            return $http.get('/api/models')
                .then(success)
                .catch(fail);

            function success(response) {
                return response.data;
            }

            function fail(error) {
                var msg = 'query for models failed. ' + error.data.description;
                logger.error(msg);
                return $q.reject(msg);
            }
        }
    }
})();
