/**
 * Created by daniel on 2015-07-21.
 */

(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('inlineEditingService', inlineEditingService);

    inlineEditingService.$inject = ['editableOptions'];
    /* @ngInject */
    function inlineEditingService(editableOptions) {
        var service = {
            initialize: initialize
        };

        return service;

        function initialize() {
            editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
        }

    }
})();
