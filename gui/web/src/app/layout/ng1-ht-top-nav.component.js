// TODO fixa till test-procedurer för ng2, ta bort denna fil sen
//(function() {
//    'use strict';
//
//    angular
//        .module('app')
//        .component('htTopNav', htTopNavOptions());
//
//    /* @ngInject */
//    function htTopNavOptions() {
//        var options = {
//            scope: {}, // possibly unused since component-refactor
//            bindToController: { // possibly unused since component-refactor
//                'navline': '='
//            },
//            //controller: TopNavController,
//            //controllerAs: 'vm',
//            restrict: 'E', // must be set to 'E' for upgradability
//            templateUrl: 'app/layout/sp-top-nav.html'
//        };
//
//        return options;
//    }
//
//    /* @ngInject */
//    TopNavController.$inject = ['modelService', 'dashboardService', '$state', '$uibModal','settingsService'];
//
//    function TopNavController(modelService, dashboardService, $state, $uibModal, settingsService) {
//        var vm = this;
//        vm.modelService = modelService;
//        vm.dashboardService = dashboardService;
//        vm.settingsService = settingsService;
//        vm.$state = $state;
//        vm.createModel = createModel;
//
//        function createModel() {
//            var modalInstance = $uibModal.open({
//                templateUrl: '/app/models/createmodel.html',
//                controller: 'CreateModelController',
//                controllerAs: 'vm'
//            });
//
//            modalInstance.result.then(function(chosenName) {
//                modelService.createModel(chosenName);
//            });
//        }
//    }
//})();
