//(function() {
//    'use strict';
//
//    angular
//        .module('app')
//        .component('upgTopNavElements', upgTopNavElementsOptions());
//
//    /* @ngInject */
//    function upgTopNavElementsOptions() {
//        var options = {
//            scope: {}, // possibly unused since component-refactor
//            controller: UpgTopNavElementsController,
//            controllerAs: 'vm',
//            restrict: 'E', // must be set to 'E' for upgradability
//            templateUrl: 'app/upg-helpers/upg-top-nav-elements.html'
//        };
//
//        return options;
//    }
//
//    /* @ngInject */
//    UpgTopNavElementsController.$inject = ['modelService', 'dashboardService', '$state', '$uibModal','settingsService'];
//
//    function UpgTopNavElementsController(modelService, dashboardService, $state, $uibModal, settingsService) {
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
