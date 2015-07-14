(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['$q', 'spTalker', 'logger'];
    /* @ngInject */
    function DashboardController($q, spTalker, logger) {
        var vm = this;
        vm.news = {
            title: 'Sequence Planner',
            description: 'Create a model and apply algorithms to it.'
        };
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
            return spTalker.getModels().then(function (data) {
                vm.models = data;
                return vm.models;
            });
        }
    }
})();
