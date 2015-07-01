(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['$q', 'dataservice', 'logger'];
    /* @ngInject */
    function DashboardController($q, dataservice, logger) {
        var vm = this;
        vm.news = {
            title: 'Sequence Planner',
            description: 'Create a model and apply algorithms to it.'
        };
        vm.messageCount = 0;
        vm.models = [];
        vm.title = 'Dashboard';

        activate();

        function activate() {
            var promises = [getModels()];
            return $q.all(promises).then(function() {
                logger.info('Activated Dashboard View');
            });
        }

        function getModels() {
            return dataservice.getModels().then(function (data) {
                vm.models = data;
                return vm.models;
            });
        }
    }
})();
